package ca.juliusdavies.signature.testgarbage;

import ca.juliusdavies.signature.Src;
import com.sun.tools.javac.tree.JCTree;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class A {

    protected static interface I {}

    public static int test = 1;

    static {
        test = 2;
    }

    class Y {
        class YY { protected class YYY { YYY(YY y){} }
            class ZZ1 {}
            class ZZ2 { ZZ2(int i) {} }
            class ZZ3 { ZZ3(int a, ZZ3 z, double d) {} }
            class ZZ4 { public String toString() { return "zz4"; } }
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

    public static String blah() {
        Class c = A.D.Inner_D.Inner_Inner_D.class;
        return c.toString();
    }


    public static class B extends HashSet<Number> {}

    public static class C<T> extends LinkedList<T> {}

    public abstract static strictfp class F<A,B extends TreeMap<C,D>,E> extends ArrayList<A> implements Comparable<B>, Comparator<E> {
        volatile transient A a;
        static ca.juliusdavies.signature.testgarbage.A.B b;
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

    private native int getBlah();

    private static strictfp boolean is(long flag, long var) {
         return (flag & var) != 0;
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

    enum AnotherEnum {
        BIG, SMALL
    }

    public Src buildClassSig(java.lang.String classPkg, JCTree.JCClassDecl c, int level) {
        return null;
    }

}
