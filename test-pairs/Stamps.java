/* Julius Davies, CSC 330, October 7 2010, Assignment 1, "Stamps" */

package csc330;
import ca.phonelist.util.Compare;
import org.apache.commons.ssl.KeyMaterial;
import org.apache.commons.ssl.SSL;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

/**
 * A class that calculates the maximum contiguous value, starting from zero, that we can
 * cover with various stamp demoninations and a given limit of stamps.
 */
public class Stamps<BBBB> {

    private int bob;

    /**
     * Returns the maximum contiguous value, starting from zero, that we can
     * cover with the supplied stamp demoninations, and the supplied maximum
     * number of stamps.
     *
     * @param maxStamps maximum number of stamps allowed
     * @param stamps    a set denominations in REVERSE order -
     *                  most expensive to least expensive.
     * @return the maximum continguous value that can be covered using the
     *         supplied constraints.
     */
    public static int coverage(int maxStamps, TreeSet<Integer> stamps) {
        int target = 75;
        while (canCoverRecusive(target, maxStamps, stamps)) { target++; }
        return --target;
    }

    /**
     * A recursive method to see if the supplied stamps can reach a desired target
     * value.  First we try the greedy method.  If that doesn't work we try
     * every possible combination of stamps in a recursive fashion.
     *
     * @param target    target value we want our stamps to cover
     * @param maxStamps maximum number of stamps we're allowed to use
     * @param stamps    a set of stamp denominations in REVERSE ORDER
     *                  (most expensive - least expensive).
     * @return true if we can reach the target value, false if we cannot.
     */
    private static boolean canCoverRecusive(
        int target, int maxStamps, TreeSet<Integer> stamps
    ) {
        // First we try our simple greedy algorithm:
        boolean ok = canCoverSimple(target, maxStamps, stamps);

        // If the greedy algorithm didn't work, we brute-force by trying
        // every possible combination of stamps:
        if (!ok) {

            // Remove most expensive stamp from list of stamps.
            Integer largest = stamps.pollFirst();
            try {

                // Can we even achieve the desired postage given the
                // max value and the number of slots still left?
                if (largest == null || largest * maxStamps < target) {
                    return false;
                }

                // Let's try 1 of the most expensive, then 2, then 3, etc...
                // We recursively call canCoverRecursive() with the remaining
                // stamp values.
                for (int howMany = 0; howMany <= maxStamps; howMany++) {
                    int difference = largest * howMany;
                    if (difference <= target) {
                        int newTarget = target - largest * howMany;

                        // Recursive call using the altered set of denominations:
                        ok = canCoverRecusive(
                            newTarget, maxStamps - howMany, stamps
                        );

                        // Short circuit.
                        if (ok) { break; }
                    } else {
                        break;
                    }
                }
            } finally {
                // Put the most-expensive stamp back into the list of possible
                // stamp values.
                if (largest != null) { stamps.add(largest); }
            }
        }
        return ok;
    }

    /**
     * Simple greedy method to see if the supplied stamps can reach a desired target
     * value:  we use as many of the most expensive stamps as possible, and then
     * we use as many of the next-most-expensive stamps, and so on.
     *
     * @param target    target value we want our stamps to cover
     * @param slots     maximum number of stamps we're allowed to use
     * @param stamps    a set of stamp denominations in REVERSE ORDER
     *                  (most expensive - least expensive).
     * @return true if we can reach the target value, false if we cannot.
     */
    private static boolean canCoverSimple(
        int target, int slots, TreeSet<Integer> stamps
    ) {
        if (stamps == null || stamps.isEmpty()) { return false; }
        int stampCount = 0;
        for (Integer stamp : stamps) {
            while (stamp <= target && stampCount < slots) {
                target -= stamp;
                stampCount++;
            }
        }
        return target == 0;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: [input file] [output file]");
            System.exit(1);
        }

        FileOutputStream fout = new FileOutputStream(args[1]);
        PrintStream out = new PrintStream(fout, false, "UTF-8");

        FileInputStream fin = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        ArrayList<String> lines = new ArrayList<String>(1024);
        try {
            fin = new FileInputStream(args[0]);
            isr = new InputStreamReader(fin, "UTF-8");
            br = new BufferedReader(isr);

            String line = br.readLine();
            while (line != null) {
                line = line.trim().toLowerCase(Locale.ENGLISH);
                lines.add(line);
                line = br.readLine();
            }
        } finally {
            br.close();
            isr.close();
            fin.close();
        }

        Iterator<String> it = lines.iterator();
        while (it.hasNext()) {
            String line = it.next();
            if ("".equals(line)) { continue; }
            try {
                if (line.indexOf(' ') < 0) {
                    // This line is a single digit:  # of stamps allowed.
                    int maxStamps = Integer.parseInt(line);
                    if (maxStamps == 0) {
                        // Input file is terminated by a single line with the
                        // number 0.
                        return;
                    }
                    if (maxStamps > 10) {
                        throw new NumberFormatException("integer M must be 10 or less: [" + maxStamps + "]");
                    }

                    if (it.hasNext()) {
                        line = it.next();
                        TreeSet<Integer> stamps = new TreeSet<Integer>(
                            Collections.reverseOrder()
                        );
                        String[] toks = line.split("\\s");
                        if (toks.length > 1) {
                            int numberOfToks = Integer.parseInt(toks[0]);
                            if (numberOfToks > 10) {
                                throw new NumberFormatException("number of stamps must be 10 or less: [" + numberOfToks + "]");
                            }
                            if (numberOfToks >= toks.length) {
                                throw new NumberFormatException("line said it had [" + numberOfToks + "] stamps, but it only had: [" + (toks.length - 1) + "]");
                            }
                            for (int i = 1; i <= numberOfToks; i++) {
                                Integer stamp = new Integer(toks[i]);
                                if (stamp > 120) {
                                    throw new NumberFormatException("stamp denomination must be 120 or less: [" + stamp + "]");
                                }
                                stamps.add(stamp);
                            }
                            if (!stamps.isEmpty()) {
                                StringBuilder buf = new StringBuilder();
                                buf.append("max coverage = ");
                                buf.append(coverage(maxStamps, stamps));
                                buf.append(" :");
                                for (Integer i : stamps.descendingSet()) {
                                    buf.append(' ').append(i);
                                }
                                out.println(buf);
                            }
                        }
                    }
                }
            } catch (NumberFormatException nfe) {
                System.err.println("Bad input line: [" + line + "] -> " + nfe);
            }
        }

        out.close();
        fout.close();
    }


    public static class A implements Comparable<SSL> {
        public String toString() { return "Okay"; }

        public int compareTo(SSL other) { return 5; }

        private static void blah(org.apache.commons.ssl.KeyMaterial km) {

        }
    }

    public volatile BBBB frog;

    private transient volatile static String simon;

    private enum abc {a, b, c}


    private Map<String, String> getMap(Set<Integer> s, List<Map<String,Integer>> l) {
        return null;
    }

    private static void testMethod(
        String target, int slots, int blah2, TreeSet<Integer> stamps,

        boolean bb, byte b, short s, char c, int i, long l, float f, double d,

        boolean[] bbs, byte[] bs, short[] ss, char[] cs, int[] is, long[] ls, float[] fs, double[] ds,

        boolean[][] bbss, byte[][] bss, short[][] sss, char[][] css, int[][] iss, long[][] lss, float[][] fss, double[][] dss,

        boolean[][][] bbsss, byte[][][] bsss, short[][][] ssss, char[][][] csss, int[][][] isss, long[][][] lsss, float[][][] fsss, double[][][] dsss,

        Set<Integer>[] sets, Set<Integer>[][] setss, Set<Integer>[][][] setsss,

        Set<Integer[]>[] setsi, Set<Integer[][]>[][] setssi, Set<Integer[][][]>[][][] setsssi,

        Map<Long[][][][][],Map<String[],Character[][]>[]>[][][][][][][][][] m

    ) {
        return;
    }


    private static int five() { return 5; }
}
