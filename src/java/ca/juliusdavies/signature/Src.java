/*

Copyright 2011, 2012 Julius Davies and Daniel M German

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

*/
package ca.juliusdavies.signature;

import ca.phonelist.util.NullSafe;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Src extends Artifact implements Cloneable {

    public static final Comparator<Src> COMPARE_INNERS = new Comparator<Src>() {
        public int compare(Src s1, Src s2) {
            return NullSafe.compare(s1.classLine, s2.classLine, SignatureLine.BY_LINE);
        }
    };

    private static final int TYPE_CLASS = 1;
    private static final int TYPE_METHOD = 2;
    private static final int TYPE_FIELD = 3;
    private String tightString;
    private String fqn;
    private String name;
    private String pkg;
    private ScanConfig config = ScanConfig.DEFAULT_CONFIG;

    private SignatureLine classLine;

    public Src(String path) throws IOException { this(new File(path)); }

    public Src(File f) throws IOException {
        super(f.getName(), f.getPath(), f.lastModified(), null, null);
    }

    public Src(
            NameAndBytes nab, ScanConfig config, String parentSha1
    ) {
        super(nab.name, nab.path, nab.lastModified, nab.bytes, parentSha1);
        this.config = config;
    }

    public String getPackageName() { return pkg; }
    public String getClassName() { return name; }
    public String getFQN() { return fqn; }
    public String toTightString() { return tightString; }
    public int getMinLine() { return -1; }
    public int getMaxLine() { return -1; }
    public int getMethodCount() { return -1; }

    public java.util.List<Src> scan() {
        java.util.List<Src> artifacts = new ArrayList<Src>();
        try {
            Context context = new Context();
            context.put(DiagnosticListener.class, DO_NOTHING);
            Log log = Log.instance(context);
            log.dumpOnError = false;

            JavaFileObject jObj;
            try {
                jObj = new InMemoryFileObject(fileName, path, date, bytes); // fm.getFileForInput(f);
            } catch (URISyntaxException use) {
                throw new RuntimeException(use);
            }

            JavaCompiler comp = JavaCompiler.instance(context);
            comp.verbose = false;
            comp.verboseCompilePolicy = false;
            JCTree.JCCompilationUnit compUnit = comp.parse(jObj);
            Enter enter = Enter.instance(context);
            enter.main(List.of(compUnit));

            String pkg = compUnit.packge.toString();
            if ("unnamed package".equals(pkg)) {
                pkg = "";
            }
            this.pkg = pkg;

            for (JCTree t : compUnit.defs) {
                if (t instanceof JCTree.JCImport) {
                    // ignore imports
                } else if (t instanceof JCTree.JCClassDecl) {

                    // Java spec allows non-public classes to sit in the same file
                    // as the main one.   It's a pain for us!
                    // Don't confuse these with inner-classes... that's a different problem!
                    Src src = buildClassSig(pkg, (JCTree.JCClassDecl) t, 0);
                    artifacts.add(src);

                } else {
                    // System.out.println("E;couldn't handle: " + t.getClass());
                }
            }
        } catch (Throwable t) {
            this.tightString = "e;Failed to parse Java. [" + t + "]";
            artifacts.add(this);
        }

        return artifacts;
    }

    public Src buildClassSig(String classPkg, JCTree.JCClassDecl c, int level) {
        String className = c.name.toString();
        String classFQN = !"".equals(classPkg) ? classPkg + "." + className : className;
        long classAccess = c.mods.flags;
        StringBuilder buf = new StringBuilder();

        // Enums and Interfaces are always static, so let's not include that in
        // signature, to make things more consistent.
        if (is(Flags.ENUM, classAccess) || is(Flags.INTERFACE, classAccess)) {
            classAccess = classAccess & ~Flags.STATIC;
        }

        // Developers sometimes write 'public abstract interface', but
        // interfaces are always abstract, so let's not include that in
        // signature, to make things more consistent.
        if (is(Flags.INTERFACE, classAccess)) {
            classAccess = classAccess & ~Flags.ABSTRACT;
        }

        // public class NAME
        buf.append("c;").append(level).append(";");
        Artifact.indentForCurrentLevel(level, buf);
        buf.append(extractModifiers(TYPE_CLASS, classAccess));
        buf.append(is(Flags.INTERFACE, classAccess) ? "interface " : "class ");
        buf.append(classFQN);

        // <GENERIC>
        List<JCTree.JCTypeParameter> typeParams = c.getTypeParameters();
        if (typeParams != null && typeParams.size() > 0) {
            buf.append('<');
            for (JCTree.JCTypeParameter type : c.getTypeParameters()) {
                buf.append(type.toString().replace(", ", ","));
                buf.append(',');
            }
            buf.setCharAt(buf.length() - 1, '>');
        }

        // extends...
        String extendsInfo = null;
        if (c.getExtendsClause() != null) {
            JCTree extendsClause = c.getExtendsClause();
            String type = extendsClause.toString();
            String fixedType = Names.fixTypeName(type);
            extendsInfo = fixedType.replace(", ", ",");

            // If developer explicitly extends 'Object' we need to ignore it.
            if ("Object".equals(extendsInfo)) {
                extendsInfo = null;
            }
        }
        if (extendsInfo != null) {
            buf.append(" extends ").append(extendsInfo);
        }

        // implements...
        boolean moreThanOneImplements = false;
        for (JCTree.JCExpression expr : c.getImplementsClause()) {
            if (moreThanOneImplements) {
                buf.append(",");
            } else {
                buf.append(" implements ");
            }
            String type = expr.toString();
            buf.append(Names.fixTypeName(type).replace(", ", ","));
            moreThanOneImplements = true;
        }

        SignatureLine classLine = new SignatureLine(className, buf.toString());

        // Fields:
        ArrayList<SignatureLine> fieldLines = buildFieldSigs(c, level, classAccess);
        if (config.sortFields) {
            Collections.sort(fieldLines, SignatureLine.BY_LINE);
        }

        // Methods:
        ArrayList<SignatureLine> methodLines = buildMethodSigs(c, level, classAccess);
        if (config.sortMethods) {
            Collections.sort(methodLines, SignatureLine.BY_LINE);
        }

        // Inner classes:
        String innerSigs = buildInnerClassSigs(c, level + 1);

        // Build the sig:
        buf = new StringBuilder(512);
        buf.append(classLine.line).append('\n');
        for (SignatureLine sigLine : fieldLines) {
            buf.append(sigLine.line).append('\n');
        }
        for (SignatureLine sigLine : methodLines) {
            buf.append(sigLine.line).append('\n');
        }
        buf.append(innerSigs);
        this.tightString = buf.toString();

        Src src;
        try {
            src = (Src) this.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("impossible");
        }
        src.classLine = classLine;
        src.path = path + ";" + className;
        src.pkg = classPkg;
        src.name = className;
        src.fqn = classFQN;

        return src;
    }

    private String buildInnerClassSigs(JCTree.JCClassDecl c, int level) {
        // Build list of inner classes to build signatures for.
        java.util.List<JCTree.JCClassDecl> innerClasses = new ArrayList<JCTree.JCClassDecl>();
        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCClassDecl) {
                innerClasses.add((JCTree.JCClassDecl) t);
            }
        }

        ArrayList<Src> inners = new ArrayList<Src>();

        // Now build the signatures.
        for (JCTree.JCClassDecl innerClass : innerClasses) {
            Src src = buildClassSig("", innerClass, level);
            src.config = config;
            inners.add(src);
        }

        if (config.sortInnerClasses) {
            Collections.sort(inners, COMPARE_INNERS);
        }

        StringBuilder buf = new StringBuilder(128);
        for (Src src : inners) {
            buf.append(src.toTightString());
        }
        return buf.toString();
    }

    private static ArrayList<SignatureLine> buildFieldSigs(JCTree.JCClassDecl c, int level, long classAccess) {
        ArrayList<SignatureLine> sigLines = new ArrayList<SignatureLine>(64);
        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) t;
                long fieldAccess = var.mods.flags;

                // All fields in an Interface are public final static.
                if (is(Flags.INTERFACE, classAccess)) {
                    fieldAccess = fieldAccess | Flags.PUBLIC | Flags.FINAL | Flags.STATIC;
                }

                StringBuilder buf = new StringBuilder(64);
                String fieldName = var.getName().toString();

                buf.append("f;").append(level).append(";");
                Artifact.indentForCurrentLevel(level + 1, buf);
                buf.append(extractModifiers(TYPE_FIELD, fieldAccess));
                buf.append(Names.fixTypeName(var.getType().toString().replace(", ", ",")));
                buf.append(' ');
                buf.append(fieldName);
                buf.append(';');
                sigLines.add(new SignatureLine(fieldName, buf.toString()));
            }
        }
        return sigLines;
    }

    private static ArrayList<SignatureLine> buildMethodSigs(JCTree.JCClassDecl c, int level, long classAccess) {
        ArrayList<SignatureLine> sigLines = new ArrayList<SignatureLine>(64);
        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCMethodDecl) {
                JCTree.JCMethodDecl m = (JCTree.JCMethodDecl) t;
                long methodAccess = m.mods.flags;

                StringBuilder paramsBuf = new StringBuilder();
                paramsBuf.append("(");
                List<JCTree.JCVariableDecl> params = m.getParameters();
                if (!params.isEmpty()) {
                    for (JCTree.JCVariableDecl var : params) {
                        String type = var.getType().toString();
                        String fixedType = Names.fixTypeName(type);
                        paramsBuf.append(fixedType.replace(", ", ",")).append(',');
                    }
                    paramsBuf.setCharAt(paramsBuf.length() - 1, ')');
                } else {
                    paramsBuf.append(")");
                }
                String paramsList = paramsBuf.toString();

                // Various enum issues we are having:
                if (is(Flags.ENUM, classAccess)) {

                    // enum constructors are always private.
                    if ("<init>".equals(m.name.toString())) {
                        methodAccess = methodAccess | Flags.PRIVATE;

                        // javac inserts "private <init>(String,int)" into enum's,
                        // but we are having some problems with these.
                        if ("()".equals(paramsList) || "(String,int)".equals(paramsList)) {
                            continue;
                        }
                    }

                    // enum's are sometimes abstract, but our source parser cannot
                    // always see that
                    classAccess = classAccess & ~Flags.ABSTRACT;
                }

                // If the class is already 'strictfp' then its redundant to make the
                // method 'strictfp'.
                //
                // And javac makes all methods strictfp in the bytecode when the class is strictfp,
                // so we need to do this to keep our bytecode analyzer's output consistent.
                if (is(Flags.STRICTFP, classAccess) && is(Flags.STRICTFP, methodAccess)) {
                    methodAccess = methodAccess & ~Flags.STRICTFP;
                }

                // All interface methods are always public.  Let's force this here
                // so that our bytecode analyzer is consistent.  Also, sometimes
                // programmers mark interface methods as abstract, but this is redundant.
                if (is(Flags.INTERFACE, classAccess)) {
                    methodAccess = (methodAccess | Flags.PUBLIC) & (~Flags.ABSTRACT);
                }

                StringBuilder buf = new StringBuilder(64);
                String methodName = m.name.toString();

                buf.append("m;").append(level).append(";");
                Artifact.indentForCurrentLevel(level + 1, buf);
                buf.append(extractModifiers(TYPE_METHOD, methodAccess));
                if (m.restype != null) {
                    JCTree returnType = m.getReturnType();
                    buf.append(Names.fixTypeName(returnType.toString()).replace(", ", ","));
                    buf.append(" ");
                }
                buf.append(methodName);
                buf.append(paramsList);
                if (!m.getThrows().isEmpty()) {
                    buf.append(" throws ");
                    for (JCTree.JCExpression expr : m.getThrows()) {
                        buf.append(Names.fixTypeName(expr.toString())).append(',');
                    }
                    // Delete last comma.
                    buf.deleteCharAt(buf.length() - 1);
                }

                sigLines.add(new SignatureLine(methodName, buf.toString()));
            }
        }
        return sigLines;
    }

    public static String extractModifiers(int type, long access) {
        // ignore:
        // SUPER
        // VARARGS
        // INTERFACE

        // Also ignore: DEPRECATED
        // ( deprecated doesn't seem to showup during the phases of javac
        //   we are invoking ).

        StringBuilder buf = new StringBuilder();
        if (is(Flags.SYNTHETIC, access)) buf.append("synthetic ");
        if (type != TYPE_FIELD && is(Flags.BRIDGE, access)) buf.append("bridge ");
        if (is(Flags.PUBLIC, access)) buf.append("public ");
        if (is(Flags.PROTECTED, access)) buf.append("protected ");
        if (is(Flags.PRIVATE, access)) buf.append("private ");
        if (is(Flags.FINAL, access)) buf.append("final ");
        if (is(Flags.ABSTRACT, access)) buf.append("abstract ");
        if (is(Flags.STATIC, access)) buf.append("static ");
        if (is(Flags.NATIVE, access)) buf.append("native ");
        if (type == TYPE_METHOD && is(Flags.SYNCHRONIZED, access)) buf.append("synchronized ");
        if (is(Flags.ENUM, access)) buf.append("enum ");
        // if (type == TYPE_METHOD && is(Flags.DEPRECATED, access)) buf.append("deprecated ");
        if (is(Flags.STRICTFP, access)) buf.append("strictfp ");
        if (type == TYPE_FIELD && is(Flags.TRANSIENT, access)) buf.append("transient ");
        if (type == TYPE_FIELD && is(Flags.VOLATILE, access)) buf.append("volatile ");
        return buf.toString();
    }

    private static boolean is(long flag, long var) { return (flag & var) != 0; }

    private final static DiagnosticListener DO_NOTHING = new DoNothingDiagnosticListener();

    public static class DoNothingDiagnosticListener implements DiagnosticListener<JavaFileObject> {
        public void report(Diagnostic<? extends JavaFileObject> d) {

            // Do nothing.

            /*
            String code = d.getCode();
            if ("compiler.err.cant.resolve.location".equals(code)) {

            } else if ("compiler.err.doesnt.exist".equals(code)) {

            } else {
                // System.out.println(d.getCode() + " [" + d.getMessage(Locale.ENGLISH) + "]");
            }
            */
        }
    }

}
