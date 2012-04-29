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
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

public class Src extends Artifact {
    public static int test = 1;

    static {
        test = 2;
    }

    class Y {
        class YY { protected class YYY { YYY(YY y){} }
            class ZZ1 {}
            class ZZ2 { ZZ2(int i) {} }
            class ZZ3 { ZZ3(int a, ZZ3 z, double d) {} }
            class ZZ4 { public String toString() { return tightString; } }
        }
    }


    private static class OhYeah {
        public String toString() { return "OhYeah"; }
    }

    static class Z {
        class ZZ {}
        class ZZ1 {}
        class ZZ2 {}
        class ZZ3 {}
        class ZZ4 {}
    }


    private static final int TYPE_CLASS = 1;
    private static final int TYPE_METHOD = 2;
    private static final int TYPE_FIELD = 3;
    private String tightString;
    private boolean firstClassDone = false;

    public Src(String path) throws IOException { this(new File(path)); }

    public Src(File f) throws IOException { this(new NameAndBytes(NameAndBytes.SOURCE, f)); }

    public Src(
        NameAndBytes nab
    ) {
        super(nab.name, nab.path, nab.lastModified, nab.bytes);
    }


    public static String blah() {
        Class c = Src.D.Inner_D.Inner_Inner_D.class;
        return c.toString();
    }

    public String getPackageName() { return ""; }
    public String getClassName() { return ""; }
    public String toTightString() { return tightString; }
    public String toLooseString() { return ""; }
    public int getMinLine() { return -1; }
    public int getMaxLine() { return -1; }
    public int getMethodCount() { return -1; }
    public boolean isEnum() { return false; }
    public boolean isInterface() { return false; }
    public boolean isInnerClass() { return false; }

    public static class B extends HashSet<Number> {}

    public static class C<T> extends LinkedList<T> {}

    public abstract static strictfp class F<A,B extends TreeMap<C,D>,E> extends ArrayList<A> implements Comparable<B>, Comparator<E> {
        volatile transient A a;
        static Src.B b;
        private double getFive() { return 5; }
        B bb;
        C c;
        final static D d = null;
        F<A,TreeMap<C,D>,E> e;
        private static strictfp double getSix() { return 6; }
        ArrayList<? extends Number> list;
    }

    public static class D implements DiagnosticListener<JavaFileObject> {

        public void report(Comparable<? extends JavaFileObject[]> d) {

        }

        public void report(Diagnostic<? extends JavaFileObject> d) {
            String code = d.getCode();
            if ("compiler.err.cant.resolve.location".equals(code)) {

            } else if ("compiler.err.doesnt.exist".equals(code)) {

            } else {
                // System.out.println(d.getCode() + " [" + d.getMessage(Locale.ENGLISH) + "]");
            }

        }

        private static class Inner_D extends TreeMap<String, ArrayList<Integer>> implements Serializable, Comparable<Map<Map<Map<Integer, Set<String>>, Double>, Object>>, Cloneable {

            public int compareTo(Map<Map<Map<Integer, Set<String>>, Double>, Object> i) {
                return 5;
            }

            public static class Inner_Inner_D extends FilterInputStream {

                public Inner_Inner_D() {
                    super(null);
                }

                public Inner_Inner_D(InputStream in) {
                    super(in);
                }

                public String toString() {
                    Serializable s = new Serializable() {
                        public int doNothing() { return 5; }
                    };
                    return "This is fun! " + s;
                }

            }

        }
    }

    public static void main(String[] args) throws Exception {
        for (String s : args) {
            long start = System.currentTimeMillis();
            Src src = new Src(s);
            src.scan();
            long delay = System.currentTimeMillis() - start;
            System.out.println("Took: " + delay + " ms ");
        }
    }


    public void scan() {

        Comparator c = new Comparator<String>() {
            public int compare(String s1, String s2) {
                return 5;
            }
        };

        Context context = new Context();
        context.put(DiagnosticListener.class, new D());
        Log log = Log.instance(context);
        log.dumpOnError = false;

        // Options options = Options.instance(context);
        //options.put("-attrparseonly", "-attrparseonly");
        //options.put("-source", "1.5");

        // DefaultFileManager fm = new DefaultFileManager(context, false, Charset.defaultCharset());
        JavaFileObject jObj;
        try {
            jObj = new InMemoryFileObject(fileName, path, date, bytes); // fm.getFileForInput(f);
        } catch (URISyntaxException use) {
            throw new RuntimeException(use);
        }
        JavaCompiler comp = new JavaCompiler(context);
        comp.verbose = false;
        comp.verboseCompilePolicy = false;
        // comp.attrParseOnly = true;

        comp.initProcessAnnotations(null);

        /*
        List<JCTree.JCCompilationUnit> compUnits = comp.parseFiles(List.of(jObj));
        JavaCompiler jc = comp.processAnnotations(
                comp.enterTrees(compUnits),
                List.<String>nil());

        jc.flow(jc.attribute(jc.todo));


        JCTree.JCCompilationUnit compUnit = compUnits.head;
        */

        JCTree.JCCompilationUnit compUnit = comp.parse(jObj);
        Enter enter = Enter.instance(context);
        enter.main(List.of(compUnit));

        String pkg = compUnit.packge.toString();
        if ("unnamed package".equals(pkg)) {
            pkg = "";
        }

        StringBuilder buf = new StringBuilder();
        for (JCTree t : compUnit.defs) {
            if (t instanceof JCTree.JCImport) {

                // ignore imports

            } else if (t instanceof JCTree.JCClassDecl) {

                buildClassSig(pkg, (JCTree.JCClassDecl) t, 0, buf);

            } else {

                System.out.println("couldn't handle: " + t.getClass());

            }
        }

        // buf.append("\n");
        tightString = buf.toString().trim();

        // System.out.println(buf.toString().substring(1));
    }

    private native int getBlah();

    public void buildClassSig(java.lang.String pkg, JCTree.JCClassDecl c, int level, StringBuilder buf) {
        if (firstClassDone && level == 0) {
            buf.append("\n\nn;[virtual]/").append(pkg.replace('.', '/'));
            buf.append("/").append(c.name.toString()).append(".java");
        }

        long classAccess = c.mods.flags;

        // Class line
        buf.append("\nc;").append(level).append(";");
        indentForCurrentLevel(level, buf);

        buf.append(extractModifiers(TYPE_CLASS, classAccess));
        buf.append(is(Flags.INTERFACE, classAccess) ? "interface " : "class ");
        buf.append(pkg);
        if (!"".equals(pkg)) buf.append('.');
        buf.append(c.name.toString());

        List<JCTree.JCTypeParameter> typeParams = c.getTypeParameters();
        if (typeParams != null && typeParams.size() > 0) {
            buf.append('<');
            for (JCTree.JCTypeParameter type : c.getTypeParameters()) {
                buf.append(type.toString().replace(", ", ","));
                buf.append(',');
            }
            buf.setCharAt(buf.length()-1, '>');
        }

        if (c.getExtendsClause() != null) {
            buf.append(" extends ");
            JCTree extendsClause = c.getExtendsClause();
            String type = extendsClause.toString();
            String fixedType = Names.fixTypeName(type);
            buf.append(fixedType.replace(", ", ","));
        }

        boolean moreThanOne = false;
        for (JCTree.JCExpression expr : c.getImplementsClause()) {
            if (moreThanOne) {
                buf.append(",");
            } else {
                buf.append(" implements ");
            }
            String type = expr.toString();
            buf.append(Names.fixTypeName(type).replace(", ", ","));
            moreThanOne = true;
        }

        buildFieldSigs(c, level, buf);
        buildMethodSigs(c, level, buf, classAccess);
        buildInnerClassSigs(c, level+1, buf);
        if (level == 0) {
            firstClassDone = true;
        }

    }


    private void buildInnerClassSigs(JCTree.JCClassDecl c, int level, StringBuilder buf) {

        // Build list of inner classes to build signatures for.
        java.util.List<JCTree.JCClassDecl> innerClasses = new ArrayList<JCTree.JCClassDecl>();
        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCClassDecl) {
                innerClasses.add((JCTree.JCClassDecl) t);
            }
        }

        // Now build the signatures.
        for (JCTree.JCClassDecl innerClass : innerClasses) {
            buildClassSig("", innerClass, level, buf);
        }
    }


    private static void buildFieldSigs(JCTree.JCClassDecl c, int level, StringBuilder buf) {

        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCVariableDecl) {
                JCTree.JCVariableDecl var = (JCTree.JCVariableDecl) t;

                buf.append("\nf;").append(level).append(";");
                indentForCurrentLevel(level+1, buf);
                buf.append(extractModifiers(TYPE_FIELD, var.mods.flags));
                buf.append(Names.fixTypeName(var.getType().type.toString()));
                buf.append(' ');
                buf.append(var.getName());
                buf.append(';');
            }
        }
    }

    private static void buildMethodSigs(JCTree.JCClassDecl c, int level, StringBuilder buf, long classAccess) {
        for (JCTree t : c.getMembers()) {
            if (t instanceof JCTree.JCMethodDecl) {
                JCTree.JCMethodDecl m = (JCTree.JCMethodDecl) t;

                buf.append("\nm;").append(level).append(";");
                indentForCurrentLevel(level+1, buf);

                long methodAccess = m.mods.flags;

                // If the class is already 'strictfp' then its redundant to make the
                // method 'strictfp'.
                //
                // And javac makes all methods strictfp in the bytecode when the class is strictfp,
                // so we need to do this to keep our bytecode analyzer's output consistent.
                if (is(Flags.STRICTFP, classAccess) && is(Flags.STRICTFP, methodAccess)) {
                    methodAccess = methodAccess & ~Flags.STRICTFP;
                }

                // All interface methods are always public.  Let's force this here
                // so that our bytecode analyzer is consistent.
                if (is(Flags.INTERFACE, classAccess)) {
                    methodAccess = methodAccess | Flags.PUBLIC;
                }

                buf.append(extractModifiers(TYPE_METHOD, methodAccess));
                if (m.restype != null) {
                    buf.append(Names.fixTypeName(m.restype.type.toString()));
                    buf.append(" ");
                }
                buf.append(m.name);
                buf.append("(");
                List<JCTree.JCVariableDecl> params = m.getParameters();
                if (!params.isEmpty()) {
                    for (JCTree.JCVariableDecl var : params) {
                        String type = var.getType().toString();
                        String fixedType = Names.fixTypeName(type);
                        buf.append(fixedType.replace(", ", ",")).append(',');
                    }
                    buf.setCharAt(buf.length() - 1, ')');
                } else {
                    buf.append(")");
                }
                if (!m.getThrows().isEmpty()) {
                    buf.append(" throws ");
                    buf.append(m.getThrows());
                }
            }
        }
    }

    public static String extractModifiers(int type, long access) {

        StringBuilder buf = new StringBuilder();

        // ignore:
        // ACC_SUPER
        // ACC_VARARGS


        if (is(Flags.SYNTHETIC, access)) buf.append("synthetic ");

        if (type != TYPE_FIELD && is(Flags.BRIDGE, access)) buf.append("bridge ");

        if (is(Flags.PUBLIC, access)) buf.append("public ");
        if (is(Flags.PROTECTED, access)) buf.append("protected ");
        if (is(Flags.PRIVATE, access)) buf.append("private ");

        if (is(Flags.FINAL, access)) buf.append("final ");
        if (is(Flags.ABSTRACT, access)) buf.append("abstract ");
        if (is(Flags.STATIC, access)) buf.append("static ");
        // if (is(Flags.INTERFACE, access)) buf.append("interface ");

        if (is(Flags.NATIVE, access)) buf.append("native ");
        if (type == TYPE_METHOD && is(Flags.SYNCHRONIZED, access)) buf.append("synchronized ");
        if (is(Flags.ENUM, access)) buf.append("enum ");
        if (is(Flags.DEPRECATED, access)) buf.append("deprecated ");

        if (is(Flags.STRICTFP, access)) buf.append("strictfp ");
        if (is(Flags.TRANSIENT, access)) buf.append("transient ");

        if (type == TYPE_FIELD && is(Flags.VOLATILE, access)) buf.append("volatile ");

        return buf.toString();
    }

    private static strictfp boolean is(long flag, long var) {
        return (flag & var) != 0;
    }

    private static void indentForCurrentLevel(int level, StringBuilder buf) {
        for (int i = 0; i < level; i++) { buf.append("  "); }
    }


    static {
        try {
            Object o = null;
            String s = o.toString();
        } catch (NullPointerException npe) {
            test = 99;
        }
    }

    strictfp enum yeah implements Cloneable, Comparator<Map<String,String>> {
        one, two, three;

        public int compare(Map<String,String> m1, Map<String,String> m2) {
            return 5;
        }
    }
}
