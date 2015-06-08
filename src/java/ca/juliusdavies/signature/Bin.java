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

import ca.phonelist.util.Hash;
import ca.phonelist.util.Hex;
import ca.phonelist.util.NullSafe;
import ca.phonelist.util.Strings;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.security.MessageDigest;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class Bin extends Artifact {
    public static final Comparator<Bin> BY_TOP_LINE = new Comparator<Bin>(){
        public int compare(Bin b1, Bin b2) {
            return NullSafe.compare(b1.classLine, b2.classLine, SignatureLine.BY_LINE);
        }
    };

    private static final MessageDigest SHA1;

    static {
        MessageDigest MD = null;
        try {
            MD = MessageDigest.getInstance("sha1");
        } catch (Exception e) {
            throw new RuntimeException("End of the world: " + e, e);
        } finally {
            SHA1 = MD;
        }
    }

    public static final int TYPE_CLASS = 1;
    public static final int TYPE_METHOD = 2;
    public static final int TYPE_FIELD = 3;
    public static final String JAVA_LANG_OBJECT = "java/lang/Object";
    public static final String JAVA_LANG_ANNOTATION = "java/lang/annotation/Annotation";
    public static final String CONSTRUCTOR_NAME = "<init>";

    /* raw data to help us reassemble the inner classes */
    private transient Map<String,NameAndBytes> allKnownInnerClasses = new TreeMap<String,NameAndBytes>();
    private Bin ultimateParent;

    private int level;
    private String fqn;
    private String name;
    private String pkg;
    private String tightSig;
    private String parent;
    private int myAccess;
    private LinkedHashMap<String, Integer> myInnerClasses = new LinkedHashMap<String, Integer>();
    private ScanConfig config = ScanConfig.DEFAULT_CONFIG;

    private ArrayList<SignatureLine> fieldLines = new ArrayList<SignatureLine>(64);
    private ArrayList<SignatureLine> methodLines = new ArrayList<SignatureLine>(64);
    private SignatureLine classLine;

    public Bin(NameAndBytes nab) { this(nab, null, ScanConfig.DEFAULT_CONFIG, null); }

    private Bin(NameAndBytes nab, Bin ultimateParent, String parent, int myAccess) {
        this(nab);
        this.ultimateParent = ultimateParent;
        this.myAccess = myAccess;
        this.parent = parent;
        this.config = ultimateParent.config;
    }

    public Bin(NameAndBytes nab, List<NameAndBytes> knownInnerClasses, ScanConfig config, String parentSha1) {
        super(nab.name, nab.path, nab.lastModified, nab.bytes, parentSha1);

        // We need to append the inner-class bytes to the SHA1 of the file.
        if (knownInnerClasses != null) {
            Collections.sort(knownInnerClasses);
            synchronized (SHA1) {
                SHA1.reset();
                SHA1.update(bytes);
                for (NameAndBytes inner : knownInnerClasses) {
                    SHA1.update(inner.bytes);
                }
                this.fileSHA1 = Hex.encode(SHA1.digest());
            }
        } else {
            this.fileSHA1 = Hash.sha1(bytes);
        }

        this.config = config;
        try {
            if (knownInnerClasses != null) {
                this.ultimateParent = this;
                for (NameAndBytes innerNab : knownInnerClasses) {
                    innerNab.cr = new ClassReader(innerNab.bytes);
                    String fqn = Names.fixClassName(innerNab.cr.getClassName());
                    allKnownInnerClasses.put(fqn, innerNab);
                }
                scan(new ClassReader(bytes));

                // only outter class needs a path
                this.path = path + ";" + this.name;
            } else {
                // postpone scan for inner-classes... we need more info
            }
        } catch (Throwable t) {
            this.tightSig = "e;Could not parse bytecode. [" + t + "]";
        }
    }

    private void initSignature() {
        if (tightSig == null) {
            try {
                assembleSignature();
            } catch (MissingInnerClassException mice) {
                this.tightSig = "e;-1;Failed to parse [" + fqn + "]. Cannot find inner-class bytecode: [" + mice.fqn + "]";
            }
        }
    }

    private void assembleSignature() {
        if (tightSig == null) {

            // Fields:
            if (config.sortFields) { Collections.sort(fieldLines, SignatureLine.BY_LINE); }

            // Methods:
            if (config.sortMethods) { Collections.sort(methodLines, SignatureLine.BY_LINE); }

            StringBuilder buf = new StringBuilder();
            buf.append(classLine.line).append('\n');
            for (SignatureLine sigLine : fieldLines) {
                buf.append(sigLine.line).append('\n');
            }
            for (SignatureLine sigLine : methodLines) {
                buf.append(sigLine.line).append('\n');
            }

            ArrayList<String> innerClasses = new ArrayList<String>(myInnerClasses.keySet());

            // For some reason asm.jar lists inner-classes in reverse order.
            Collections.reverse(innerClasses);
            ArrayList<Bin> inners = new ArrayList<Bin>();
            for (String innerFQN : innerClasses) {
                Integer innerAccess = myInnerClasses.get(innerFQN);
                NameAndBytes nab = ultimateParent.allKnownInnerClasses.get(innerFQN);
                if (nab == null) {
                    throw new MissingInnerClassException(innerFQN);
                }
                ClassReader cr = nab.cr;
                Bin b = new Bin(nab, ultimateParent, this.name, innerAccess);
                b.scan(cr);
                b.assembleSignature();
                inners.add(b);
            }

            if (config.sortInnerClasses) {
                Collections.sort(inners, BY_TOP_LINE);
            }

            for (Bin b : inners) {
                String innerSig = b.toTightString();
                buf.append(innerSig);
            }

            tightSig = buf.toString();
        }
    }

    public String getPackageName() { return pkg; }
    public String getClassName() { return name; }
    public String toTightString() { initSignature(); return tightSig; }
    public int getMinLine() { return -1; }
    public int getMaxLine() { return -1; }
    public int getMethodCount() { return -1; }

    private void scan(ClassReader cr) {
        int flags = ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG;
        cr.accept(this, flags);
    }

    public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
        boolean isInnerClass = this != ultimateParent;
        if (!isInnerClass) {
            this.myAccess = access;
        }

        int x = name.lastIndexOf('/');
        this.level = Strings.countChar(name.substring(x >= 0 ? x : 0), '$');
        this.fqn = Names.fixClassName(name);
        x = fqn.lastIndexOf('.');
        if (x >= 0) {
            this.pkg = fqn.substring(0, x);
            this.name = fqn.substring(x + 1);
        } else {
            this.pkg = "";
            this.name = fqn;
        }

        String genericExtendsString = superName != null ? Names.fixTypeName(superName) : "";
        String genericImplementsString = null;
        String generic = null;
        if (sig != null) {
            sig = sig.replace("::", ":");
            if (sig.charAt(0) == '<') {
                x = Names.findClosingBracket(sig, 1);
                generic = sig.substring(0, x+1);
                sig = sig.substring(x+1);
            }
            generic = genericParser(generic);
            sig = signatureParser(sig);
            if (interfaces != null && interfaces.length > 0) {
                x = firstComma(sig, 0);
                genericExtendsString = sig.substring(0, x);
                genericImplementsString = sig.substring(x+1);
            } else {
                genericExtendsString = sig;
            }
        }

        // Interfaces are always abstract and static in bytecode, but source
        // representation doesn't bother specifying this redundant info.
        if (is(ACC_INTERFACE, myAccess)) {
            myAccess = myAccess & ~ACC_ABSTRACT & ~ACC_STATIC;
        }
        // enum's are always final, static in bytecode, but source
        // representation doesn't bother mentioning this.
        // Also, source representation might not mention abstract, either.
        if (is(ACC_ENUM, myAccess)) {
            myAccess = myAccess & ~ACC_FINAL & ~ACC_STATIC & ~ACC_ABSTRACT;
        }

        StringBuilder buf = new StringBuilder(64);
        buf.append("c;").append(level).append(";");
        Artifact.indentForCurrentLevel(level, buf);
        buf.append(extractModifiers(TYPE_CLASS, myAccess));
        buf.append(is(ACC_INTERFACE, myAccess) ? "interface " : "class ");
        buf.append(isInnerClass ? Names.fixTypeName(fqn) : fqn);
        if (generic != null) {
            buf.append(generic);
        }
        if (superName != null && !JAVA_LANG_OBJECT.equals(superName) && !is(ACC_ENUM, myAccess)) {
            buf.append(" extends ").append(genericExtendsString);
        }

        // Weird quirk:  interfaces CANNOT implement, but that's
        // how the @interface tag is dealt with.  Not visible to our
        // source processor, so we need to omit this.
        if (interfaces != null && interfaces.length == 1) {
            if (JAVA_LANG_ANNOTATION.equals(interfaces[0]) && is(ACC_INTERFACE, myAccess)) {
                interfaces = null;
            }
        }

        if (interfaces != null && interfaces.length > 0) {
            buf.append(" implements ");
            if (genericImplementsString != null) {
                // The generic "implements" clause (when applicable).
                buf.append(genericImplementsString);
            } else {
                // Non-generic version.
                for (String s : interfaces) {
                    buf.append(Names.fixTypeName(s)).append(",");
                }
                // Delete final comma.
                buf.deleteCharAt(buf.length()-1);
            }
        }

        this.classLine = new SignatureLine(this.name, buf.toString().trim());
    }

    public void visitInnerClass(String fullName, String outerName, String innerName, int access) {
        if (outerName != null && innerName != null && fqn.equals(Names.fixClassName(outerName))) {
            myInnerClasses.put(fqn + "." + innerName, access);
        }
    }

    public FieldVisitor visitField(int access, final String name, String desc, String sig, Object value) {
        if (is(ACC_SYNTHETIC, access)) { return null; }

        StringBuilder buf = new StringBuilder(64);
        buf.append("f;").append(level).append(";");
        Artifact.indentForCurrentLevel(level + 1, buf);
        buf.append(extractModifiers(TYPE_FIELD, access));
        if (sig != null) {
            buf.append(signatureParser(sig));
        } else {
            String type = Names.fixTypeName(Type.getType(desc).getClassName());
            buf.append(type);
        }
        buf.append(" ").append(name).append(';');
        fieldLines.add(new SignatureLine(name, buf.toString()));
        return null;
    }

    public MethodVisitor visitMethod(int access, final String name, String desc, String sig, String[] exceptions) {

        if (is(ACC_SYNTHETIC, access) || is(ACC_BRIDGE, access)) {
            return null;
        }

        // for some reason our source parser cannot see static initializers
        if ("<clinit>".equals(name)) {
            return null;
        }

        // javac inserts "values()" and "valueOf(String)" methods into enum's,
        // but our source parser cannot see them, so we must watch out for them.
        if (is(ACC_ENUM, myAccess) && is(ACC_STATIC, access)) {
            if ("values".equals(name) && desc != null && desc.startsWith("()")) {
                return null;
            }
            if ("valueOf".equals(name) && desc != null && desc.startsWith("(Ljava/lang/String;)")) {
                return null;
            }
        }

        // javac inserts "private <init>(String,int)" into enum's,
        // but we are having some problems with these.
        if (is(ACC_ENUM, myAccess) && is(ACC_PRIVATE, access)) {
            if ("<init>".equals(name) && desc != null && desc.startsWith("()")) {
                return null;
            }
            if ("<init>".equals(name) && desc != null && desc.startsWith("(Ljava/lang/String;I)")) {
                return null;
            }
            if ("<init>".equals(name) && desc != null && desc.startsWith("(Ljava/lang/String;ILjava/lang/String;I)")) {
                return null;
            }
        }

        StringBuilder buf = new StringBuilder(64);
        buf.append("m;").append(level).append(";");
        Artifact.indentForCurrentLevel(level + 1, buf);

        // If the class is strictfp, it's redundant to mark the method as strictfp.
        if (is(ACC_STRICT, myAccess)) {
            access = access & ~ACC_STRICT;
        }

        // If the class is an interface, it's redundant to mark the method as abstract.
        if (is(ACC_INTERFACE, myAccess)) {
            access = access & ~ACC_ABSTRACT;
        }

        buf.append(extractModifiers(TYPE_METHOD, access));

        String[] s = methodSig(sig, desc);
        String returnSig = s[0];
        String methodSig = s[1];

        if (!CONSTRUCTOR_NAME.equals(name)) {
            buf.append(returnSig).append(" ");
        }
        buf.append(name).append("(");
        if (!is(ACC_ENUM, myAccess)) {
            if ("<init>".equals(name) && ultimateParent != this && !is(ACC_STATIC, myAccess) && methodSig.startsWith(parent)) {
                methodSig = methodSig.substring(parent.length());
                if (!"".equals(methodSig) && methodSig.charAt(0) == ',') {
                    methodSig = methodSig.substring(1);
                }
            }
        }
        buf.append(methodSig);
        buf.append(")");
        if (exceptions != null && exceptions.length > 0) {
            buf.append(" throws ");
            for (String x : exceptions) {
                buf.append(Names.fixTypeName(x)).append(',');
            }
            // Delete the final comma.
            buf.deleteCharAt(buf.length()-1);
        }
        methodLines.add(new SignatureLine(name, buf.toString()));
        return null;
    }

    public static String[] methodSig(String sig, String desc) {
        return methodSig(sig, desc, true);
    }

    public static String[] methodSig(String sig, String desc, boolean strip) {
        if (sig == null) {
            sig = desc;
        }
        String returnSig = "";
        String methodSig = sig;
        if (sig != null) {
            int x = sig.indexOf('(');
            int y = sig.indexOf(')');
            if (x >= 0) {
                methodSig = sig.substring(x + 1, y);
                returnSig = sig.substring(y + 1);
            }

            returnSig = signatureParser(returnSig, strip);
            methodSig = signatureParser(methodSig, strip);
        }
        return new String[]{returnSig, methodSig};
    }

    public static String extractModifiers(int type, int access) {
        // ignore:
        // ACC_SUPER
        // ACC_VARARGS
        // ACC_INTERFACE

        // Also ignore: DEPRECATED
        // ( deprecated doesn't seem to showup during the phases of javac
        //   we are invoking in the *.java scanner ).

        StringBuilder buf = new StringBuilder();
        if (is(ACC_SYNTHETIC, access)) buf.append("synthetic ");
        if (type != TYPE_FIELD && is(ACC_BRIDGE, access)) buf.append("bridge ");
        if (is(ACC_PUBLIC, access)) buf.append("public ");
        if (is(ACC_PROTECTED, access)) buf.append("protected ");
        if (is(ACC_PRIVATE, access)) buf.append("private ");
        if (is(ACC_FINAL, access)) buf.append("final ");
        if (is(ACC_ABSTRACT, access)) buf.append("abstract ");
        if (is(ACC_STATIC, access)) buf.append("static ");
        if (is(ACC_NATIVE, access)) buf.append("native ");
        if (type == TYPE_METHOD && is(ACC_SYNCHRONIZED, access)) buf.append("synchronized ");
        if (is(ACC_ENUM, access)) buf.append("enum ");
        // if (type == TYPE_METHOD && is(ACC_DEPRECATED, access)) buf.append("deprecated ");
        if (is(ACC_STRICT, access)) buf.append("strictfp ");
        if (type == TYPE_FIELD && is(ACC_TRANSIENT, access)) buf.append("transient ");
        if (type == TYPE_FIELD && is(ACC_VOLATILE, access)) buf.append("volatile ");
        return buf.toString();
    }

    public static boolean is(int flag, int var) { return (flag & var) != 0; }

    public static boolean any(int flag1, int flag2, int var) { return any(flag1, flag2, 0, var); }

    public static boolean any(int flag1, int flag2, int flag3, int var) {
        return ((flag1 | flag2 | flag3) & var) != 0;
    }

    public static String genericParser(String generic) {
        if (generic != null) {
            StringBuilder buf = new StringBuilder();
            generic = generic.substring(1, generic.length()-1);
            String[] toks = generic.split(":");
            for (int i = 0; i < toks.length; i++) {
                String t = toks[i];
                int x = t.lastIndexOf(';');
                if (x > 0) {
                    toks[i-1] = toks[i-1] + ":" + t.substring(0, x+1);
                    toks[i] = t.substring(x+1);
                }
            }

            buf.append('<');
            for (String t : toks) {
                if ("".equals(t)) {
                    continue;
                }
                int x = t.indexOf(':');
                String sig = t.substring(x+1);
                sig = signatureParser(sig);
                t = x >= 0 ? t.substring(0,x) : t;
                if ("Object".equals(sig)) {
                    buf.append(t).append(',');
                } else {
                    buf.append(t).append(" extends ").append(sig).append(',');
                }
            }
            buf.setCharAt(buf.length()-1, '>');
            return buf.toString();
        }
        return null;
    }

    public static String signatureParser(String sig) {
        return signatureParser(sig, true);
    }

    public static String signatureParser(String sig, boolean strip) {
        if (sig == null || "".equals(sig)) {
            return "";
        }

        if (!sig.endsWith(";")) {
            sig = sig + ";";
        }
        sig = sig.replace(";>", ">");
        sig = sig.replace(">", ";>");
        sig = Names.removeOutterClassGeneric(sig);
        int y = sig.indexOf(">.");
        while (y >= 0 && y+1 < sig.length()) {
            int x = Names.findOpeningBracket(sig, y-1);
            sig = sig.substring(0,x) + sig.substring(y+1);
            y = sig.indexOf(">.", x);
        }

        StringBuilder buf = new StringBuilder(sig.length());
        signatureParser(sig, 0, buf, strip);
        return buf.toString();
    }

    public static int firstComma(String sig, int pos) {
        for (int i = pos; i < sig.length(); i++) {
            char c = sig.charAt(i);
            if (c == '<') {
                i = Names.findClosingBracket(sig, i+1);
            } else if (c == ',') {
                return i;
            }
        }
        throw new RuntimeException("Could not find firstComma: [" + sig + "]");
    }

    private static int signatureParser(String sig, int pos, StringBuilder buf, boolean strip) {
        for (int i = pos; i < sig.length(); i++) {
            char c = sig.charAt(i);
            switch (c) {
                case ';': {
                    int bracketCount = toJavaType(sig.substring(pos, i), buf, strip);
                    appendBrackets(bracketCount, buf);
                    if (sig.length() > i + 1 && sig.charAt(i + 1) != '>') {
                        buf.append(',');
                    }
                    pos = i;
                }
                break;
                case '<': {
                    int bracketCount = toJavaType(sig.substring(pos, i), buf, strip);
                    buf.append('<');
                    i = signatureParser(sig, i+1, buf, strip);
                    buf.append('>');
                    appendBrackets(bracketCount, buf);
                    pos = i;
                }
                break;
                case '>':
                    return i;
            }
        }

        return sig.length();
    }

    private static int toJavaType(String t, StringBuilder buf, boolean strip) {
        boolean isFirstToken = true;
        int bracketCount = 0;
        boolean hasGenericWildcard = false;
        boolean hasGenericSuper = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            String tok = null;
            switch (c) {
                case '[':
                    bracketCount++;
                    break;
                case '*':
                    tok = "?";
                    break;
                case 'T':
                    int x = t.indexOf(';', i+2);
                    x = x < 0 ? t.length() : x;
                    tok = t.substring(i+1, x);
                    i = x+1;
                    break;
                case 'Z': tok = "boolean"; break;
                case 'B': tok = "byte"; break;
                case 'S': tok = "short"; break;
                case 'C': tok = "char"; break;
                case 'I': tok = "int"; break;
                case 'J': tok = "long"; break;
                case 'F': tok = "float"; break;
                case 'D': tok = "double"; break;
                case 'V': tok = "void"; break;
                case '+':
                    hasGenericWildcard = true;
                    break;
                case '-':
                    hasGenericSuper = true;
                    break;
                case 'L':
                    x = t.indexOf(';', i);
                    if (x < 0) { x = t.length(); }
                    tok = t.substring(i + 1, x);
                    if (strip) {
                        tok = Names.fixTypeName(tok);
                    } else {
                        tok = Names.fixClassName(tok);
                    }
                    i = x+1;
                    break;
                default:
                    break;
            }

            if (tok != null) {
                if (hasGenericWildcard) {
                    buf.append("? extends ");
                }
                if (hasGenericSuper) {
                    buf.append("? super ");
                }
                if (!isFirstToken) {
                    buf.append(',');
                }
                isFirstToken = false;
                buf.append(tok);

                if (i < t.length()) {
                    appendBrackets(bracketCount, buf);
                    bracketCount = 0;
                }
            }

        }
        return bracketCount;
    }

    private static void appendBrackets(int bracketCount, StringBuilder buf) {
        for (int j = 0; j < bracketCount; j++) {
            buf.append("[]");
        }
    }

    public String toString() { return getFQN(); }

}
