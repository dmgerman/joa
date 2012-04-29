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
import ca.phonelist.util.Numbers;
import ca.phonelist.util.Util;

import java.io.*;
import java.util.*;

public class SimpleScan {

    private final static boolean CLEANUP_TARS = true;

    private final static long START_TIME = System.currentTimeMillis();
    public final static String TAR_PATH = "/tmp/java-tars" + START_TIME + "/";
    private static String prevFileLine = "";
    private final static TreeSet<String> VALID_ARGS = new TreeSet<String>();

    // SHA1 + "/" + size.  To avoid duplicate processing.
    private final static HashSet<String> PROCESSED_TARS = new HashSet<String>();

    static {
        VALID_ARGS.add("--stdin"); VALID_ARGS.add("-in");
        VALID_ARGS.add("--recursiveZip"); VALID_ARGS.add("-rz");
        VALID_ARGS.add("--hashOutput"); VALID_ARGS.add("-ho");
        VALID_ARGS.add("--sortOutput"); VALID_ARGS.add("-so");
        VALID_ARGS.add("--sortInnerClasses"); VALID_ARGS.add("-si");
        VALID_ARGS.add("--sortMethods"); VALID_ARGS.add("-sm");
        VALID_ARGS.add("--sortFields"); VALID_ARGS.add("-sf");
        VALID_ARGS.add("--querySame"); VALID_ARGS.add("-qs");
        VALID_ARGS.add("--queryOther"); VALID_ARGS.add("-qo");
        VALID_ARGS.add("--queryFileHash"); VALID_ARGS.add("-qh");
        VALID_ARGS.add("--noFQN"); VALID_ARGS.add("-nf");
    }

    // skipOutput:  a map of FQN-to-hashes.
    // When processing, if we find a match to one of these, we
    // skip its output.  Useful for finding non-matching signatures.
    static HashMap<String, HashMap<String, String>> skipOutput =
            new HashMap<String, HashMap<String, String>>();
    static HashSet<String> matchCount = new HashSet<String>();

    /* Don't adjust these:  main() sets them. */
    static boolean querySame;
    static boolean queryOther;
    static boolean queryFileHash;
    static boolean buildQueryOutput;
    static boolean recursiveZip;
    static boolean hashOutput;
    static boolean useStdIn;
    static boolean sortOutput;
    static boolean noFQN;
    static ScanConfig scanConfig = ScanConfig.DEFAULT_CONFIG;

    // Are we using command-line args, or a list of filename on stdin?
    // ArgScanner deals with this distinction.
    public static class ArgScanner {
        private BufferedReader br;
        private int pos = 0;
        private String[] args;

        public final LinkedHashMap<String,TreeSet<String>> tars = new LinkedHashMap<String,TreeSet<String>>();

        public ArgScanner(BufferedReader br) { this.br = br; }
        public ArgScanner(String[] args) { this.args = args; }

        public String nextArg() throws IOException {
            while (!tars.isEmpty()) {
                Map.Entry<String, TreeSet<String>> newestEntry = null;
                for (Map.Entry<String, TreeSet<String>> entry : tars.entrySet()) {
                    newestEntry = entry;
                }

                TreeSet<String> tarEntries = newestEntry.getValue();
                if (!tarEntries.isEmpty()) {
                    return tarEntries.pollFirst();
                } else {
                    if (CLEANUP_TARS) {
                        String expandedTar = newestEntry.getKey();
                        int returnVal = Unpacker.rmrf(expandedTar);
                        tars.remove(expandedTar);
                        if (hashOutput && !buildQueryOutput) {
                            System.out.println("I;Tar Cleanup: rm -rf " + expandedTar + " (" + returnVal + ")");
                        }
                    }
                }
            }
            if (br == null) {
                return pos < args.length ? args[pos++] : null;
            } else {
                return br.readLine();
            }
        }
    }

    private static void printHelp() {
        System.err.println();
        System.err.println("Joa: extracts signatures from *.class and *.java.");
        System.err.println("by Julius Davies and Daniel M. German, April 2012.");
        System.err.println();
        System.err.println("Usage: [flags] [paths-to-examine...] ");
        System.err.println();
        System.err.println("  --stdin            / -in   Reads paths from stdin.");
        System.err.println("  --recursiveZip     / -rz   Process zips inside zips.");
        System.err.println("  --hashOutput       / -ho   Each output line is: SHA1;FQN;PATH");
        System.err.println("  --noFQN            / -nf   Class signature should not include FQN.");
        System.err.println();
        System.err.println("  --sortOutput       / -so   Sorts output (all signatures) by FQN.");
        System.err.println("  --sortInnerClasses / -si   Sorts inner-classes by name within each signature.");
        System.err.println("  --sortMethods      / -sm   Sorts methods by name within each signature.");
        System.err.println("  --sortFields       / -sf   Sorts fields by name within each signature.");
        System.err.println();
        System.err.println("  --querySame        / -qs   Generates bin_2_bin / src_2_src SQL");
        System.err.println("  --queryOther       / -qo   Generates bin_2_src / src_2_bin SQL.");
        System.err.println("  --queryFileHashes  / -qh   Generates SQL based on file SHA1's.");
        System.err.println();
        System.err.println("  Note: The '-queryOther/-qo' option takes precedence over '-querySame/-qs'.");
        System.err.println();
        System.err.println("  If data is supplied on STDIN, the extractor assumes this contains a list");
        System.err.println("  of paths separated by newlines (LF).  Paths supplied on the command-line");
        System.err.println("  are ignored when STDIN has data.");
        System.err.println();
        System.err.println("  Sorting the output ( -so / --sortOutput ) can require a lot of RAM.");
        System.err.println();

        double m = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
        long mem = Math.round(m);
        String mb = "" + mem;
        if (m > 1000.0) {
            long remainder = mem % 1000;
            mem = mem / 1000;
            mb = mem + "," + remainder;
        }
        System.err.println("Java thinks it's allowed to use at most " + mb + "MB of RAM right now.");
    }

    /*

c ; level ; name ; signature
m ; etc............

     */
    public static void main(String[] args) throws IOException {

        // Make sure assertions are disabled.
        assert false;
        boolean processMoreZips = true;
        boolean hasFiles = false;

System.err.println("Signature extractor version $Id: SimpleScan.java 248 2012-04-29 05:45:47Z dmg $");

        // The first 10 args might be command-line args.
        TreeSet<String> argSet = new TreeSet<String>();
        for (int i = 0; i < 10; i++) {
            if (i < args.length) {
                String arg = args[i];
                if (arg.startsWith("-")) {
                    if (VALID_ARGS.contains(arg)) {
                        argSet.add(arg);
                    } else {
                        System.err.println("\nINVALID ARG: [" + arg + "]\n");
                        System.exit(1);
                        return;
                    }
                } else {
                    hasFiles = true;
                }
            }
        }

        useStdIn = argSet.contains("--stdin") || argSet.contains("-in");
        hasFiles = hasFiles || useStdIn;

        if (!hasFiles) {
            printHelp();
            System.exit(1);
            return;
        }

        // SQL generation modes, for quering our database:
        querySame = argSet.contains("--querySame") || argSet.contains("-qs");
        queryOther = argSet.contains("--queryOther") || argSet.contains("-qo");
        queryFileHash = argSet.contains("--queryFileHash") || argSet.contains("-qh");
        if (queryOther) { querySame = false; }
        buildQueryOutput = querySame || queryOther || queryFileHash;

        // General options.
        noFQN = argSet.contains("--noFQN") || argSet.contains("-nf");
        recursiveZip = argSet.contains("--recursiveZip") || argSet.contains("-rz");
        hashOutput = argSet.contains("--hashOutput") || argSet.contains("-ho");
        sortOutput = argSet.contains("--sortOutput") || argSet.contains("-so") || buildQueryOutput;
        boolean sortInnerClasses = argSet.contains("--sortInnerClasses") || argSet.contains("-si");
        boolean sortMethods = argSet.contains("--sortMethods") || argSet.contains("-sm");
        boolean sortFields = argSet.contains("--sortFields") || argSet.contains("-sf");
        scanConfig = new ScanConfig(sortInnerClasses, sortMethods, sortFields);

        ArrayList<Artifact> artifacts = new ArrayList<Artifact>(16384);
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            ArgScanner scanner;
            if (useStdIn) {
                isr = new InputStreamReader(System.in, "UTF-8");
                br = new BufferedReader(isr);
                scanner = new ArgScanner(br);
            } else {
                scanner = new ArgScanner(args);
            }

            ZipUtil.argScanner = scanner;

            String s;
            while ((s = scanner.nextArg()) != null) {
                String S = s.toUpperCase(Locale.ENGLISH);

                // Skip inner classe files.
                if (InnerClass.isChildClass(s)) { continue; }

                // Skip package-info
                if (S.endsWith("PACKAGE-INFO.CLASS") || S.endsWith("PACKAGE-INFO.JAVA")) { continue; }

                File f = null;
                boolean isSrc = false;
                boolean isZip = false;
                boolean isTar = false;
                if (S.endsWith(".ZIP") || S.endsWith(".WAR") || S.endsWith(".EAR") || S.endsWith(".JAR")) {

                    f = new File(s);
                    isZip = true;

                } else if (S.endsWith(".TAR") || S.endsWith(".TAR.BZ2") || S.endsWith(".TAR.GZ")
                        || S.endsWith(".TAR.LZMA") || S.endsWith(".TAR.XZ") || S.endsWith(".TGZ")
                    ) {

                    f = new File(s);
                    isTar = true;

                } else if (S.endsWith(".CLASS") || S.endsWith(".JAVA")) {

                    f = new File(s);
                    isSrc = S.endsWith(".JAVA");

                }

                if (f == null) {
                    if (!argSet.contains(s) && !buildQueryOutput) {
                        System.out.println("E;Ignored: [" + s + "] Not a known file type.");
                    }
                } else if (!f.exists() || !f.canRead() || f.isDirectory()) {
                    if (f.isDirectory() && !buildQueryOutput) {
                        System.out.println("E;Skipped: [" + s + "] It's a directory.");
                    } else if (!buildQueryOutput) {
                        System.out.println("E;Problem: [" + s + "] Can't read file.");
                    }
                } else if (!processMoreZips && (isZip || isTar)) {
                    if (!buildQueryOutput) {
                        System.out.println("E;Skipped: [" + s + "].  recursiveZips is set to false.");
                    }
                } else if (f.isFile()) {
                    if (hashOutput && !buildQueryOutput) {
                        System.out.println("I;Processing: " + f.getPath());
                    }
                    try {

                        if (isTar) {

                            if (!recursiveZip) { processMoreZips = false; }

                            FileInputStream fin = new FileInputStream(f);
                            String sha1 = Hash.sha1(fin);

                            String parentSHA1 = null;
                            int level = -1;
                            String path = f.getPath();
                            String relPath = path;
                            if (path.startsWith(TAR_PATH)) {
                                String[] shaAndLevel = extractSHA1(path);
                                parentSHA1 = shaAndLevel[0];
                                level = Integer.parseInt(shaAndLevel[1]) + 1;
                                relPath = shaAndLevel[2];
                            }

                            Src a = new Src(relPath);
                            StringBuilder buf = new StringBuilder(256);
                            buf.append("F;");
                            buf.append(a.getRelativePath());
                            buf.append(';');
                            buf.append(a.getBaseName());
                            buf.append(';');
                            buf.append(a.getSuffix());
                            buf.append(';');
                            buf.append(level);
                            buf.append(';');
                            buf.append(parentSHA1 != null ? parentSHA1 : "\\N");
                            buf.append(';');
                            buf.append(sha1);
                            buf.append(";\\N;\\N;\\N;\\N");
                            if (!buildQueryOutput) {
                                System.out.println(buf.toString());
                            }

                            long zipSize = f.length();
                            String key = sha1 + "/" + a.getBaseName() + "/" + zipSize;
                            if (PROCESSED_TARS.contains(key)) {
                                if (!buildQueryOutput) {
                                    System.out.println("I;Skipping [" + f.getPath() + "]: identical file already processed.");
                                }
                                // Abort processing!
                                continue;
                            } else {
                                // Disabled for now.
                                // PROCESSED_TARS.add(key);
                            }


                            String tarPath = TAR_PATH + sha1 + "-lvl" + level;
                            Unpacker.unpack(f.getPath(), tarPath);

                            // TODO:  If we could get zips and tars to go last in this
                            // TODO:  TreeSet, that would fix a problem where a zip or tar
                            // TODO:  interrupts the current collection of signatures.
                            TreeSet<String> newPaths = new TreeSet<String>();
                            DirScanner.scan(new File(tarPath), newPaths);

                            if (scanner.tars.put(tarPath, newPaths) != null) {
                                System.out.println("E;EEK - " + tarPath + " clobbered!");
                            }

                        } else if (isZip) {

                            if (!recursiveZip) { processMoreZips = false; }

                            // Process the contents of the zip file in memory.
                            ZipUtil.scan(f, recursiveZip, scanConfig, artifacts);
                            System.gc();

                            if (buildQueryOutput) {
                                int queryStyle = SQL.SAME_2_SAME;
                                if (queryFileHash) {
                                    queryStyle = SQL.FILE_SHA1;
                                } else if (queryOther) {
                                    queryStyle = SQL.SAME_2_OTHER;
                                }

                                String sql = SQL.buildQuery(artifacts, queryStyle);
                                System.out.println(sql);
                                System.out.println();
                                System.out.println();
                                artifacts.clear();
                            }

                        } else if (isSrc) {
                            Src src = toSrc(f);
                            if (src != null) {
                                // We must see if the Src actually emcompasses many classes.
                                // (those annoying extra non-inner top-level classes)
                                artifacts.addAll(src.scan());
                            }
                        } else {
                            Bin bin = toBin(f);
                            if (bin != null) {
                                artifacts.add(bin);
                            }
                        }
                    } catch (Throwable t) {
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        try {
                            PrintStream ps = new PrintStream(bout, false, "UTF-8");
                            t.printStackTrace(ps);
                            ps.flush();
                            ps.close();
                            System.out.println("E;Failed on: [" + f.getPath() + "] Reason: " + t);
                            System.err.println("E;Failed on: [" + f.getPath() + "] Reason: " + t);
                            System.err.println(bout.toString("UTF-8"));
                            // System.exit(1);
                        } catch (UnsupportedEncodingException uee) {
                            // ignore, since UTF-8 is always supported.
                        }
                    }
                    if (!sortOutput) {
                        for (Artifact a : artifacts) { print(a); }
                        artifacts.clear();
                    }
                }

            }
        } finally {
            Util.close(br, isr);
        }

        if (!sortOutput) {
            if (!artifacts.isEmpty()) {
                System.err.println("ERROR:  artifacts should be empty if we're not sorting!");
            }
        }

        if (!artifacts.isEmpty()) {
            if (buildQueryOutput) {
                int queryStyle = SQL.SAME_2_SAME;
                if (queryFileHash) {
                    queryStyle = SQL.FILE_SHA1;
                } else if (queryOther) {
                    queryStyle = SQL.SAME_2_OTHER;
                }

                String sql = SQL.buildQuery(artifacts, queryStyle);
                System.out.println(sql);
                System.out.println();
                System.out.println();
                artifacts.clear();
            } else {
                // Sort them so that we can get 'appended' classes back into the right order.
                Collections.sort(artifacts);
                for (Artifact a : artifacts) {
                    print(a);
                }
                if (!matchCount.isEmpty()) {
                    System.out.println(matchCount.size() + " matches");
                }
            }
        }
    }

    public static NameAndBytes toNab(boolean isSrc, File f) {
        try {
            return new NameAndBytes(isSrc, f);
        } catch (IOException ioe) {
            System.err.println("E;Failed to load file [" + f + "] - " + ioe);
            return null;
        }
    }

    public static Artifact toArtifact(String target) {

        // Pull out the class name (for Java files that contain multiple top-level classes).
        int z = target.lastIndexOf(';');
        final String className = z >= 0 ? target.substring(z+1) : "";
        String path = target.substring(0, target.length() - className.length());

        if (target.indexOf('!') >= 0) {
            return ZipUtil.extractTarget(path, className, scanConfig);
        } else {
            String TARGET = target.toUpperCase(Locale.ENGLISH);
            if (TARGET.endsWith(".JAVA")) {
                List<Src> srcs = toSrc(new File(target)).scan();
                for (Src s : srcs) {
                    if (className.equals(s.getClassName())) {
                        return s;
                    }
                }
                throw new RuntimeException("ugh could not find desired class: [" + target + "]");
            } else {
                return toBin(new File(path));
            }
        }
    }

    public static Src toSrc(File f) {
        // *.java is a little tricky:  might include 'appended' non-public classes.
        NameAndBytes nab = toNab(true, f);
        return nab != null ? new Src(nab, scanConfig, null) : null;
    }

    public static Bin toBin(File f) {
        // *.class is harder:  need to find related inner classes (if applicable).
        NameAndBytes nab = toNab(true, f);
        if (nab == null) { return null; }

        List<NameAndBytes> relatedClasses = new ArrayList<NameAndBytes>();
        String n = f.getName();
        String prefix = n.substring(0, n.length() - 6) + "$";

        // find related inner classes
        File d = f.getParentFile();
        File[] siblings = d != null ? d.listFiles() : null;
        if (siblings != null) {
            for (File sibling : siblings) {

                String name = sibling.getName();
                String NAME = sibling.getName().toUpperCase(Locale.ENGLISH);
                if (NAME.endsWith(".CLASS") && name.startsWith(prefix)) {

                    String innerName = name.substring(prefix.length(), name.length() - 6);
                    int x = innerName.lastIndexOf('$');
                    if (x >= 0) {
                        innerName = innerName.substring(x + 1);
                    }

                    // Skip anonymous-inner classes (e.g. File$1.class, File$Inner$2.class, etc...)
                    if (!Numbers.isLong(innerName)) {
                        try {
                            relatedClasses.add(new NameAndBytes(NameAndBytes.BINARY, sibling));
                        } catch (IOException ioe) {
                            System.err.println("load inner-class [" + name + "] failed: " + ioe);
                        }
                    }
                }
            }
        }
        return new Bin(nab, relatedClasses, scanConfig, null);
    }

    public static void print(Artifact a) {
        String fqn = a.getFQN();
        String path = a.getPath();
        String tightSig = a.toTightString().trim();
        String looseSig = a.toLooseString().trim();
        String tightSHA1 = a.toTightSHA1();
        String looseSHA1 = a.toLooseSHA1();
        String fileSha1 = a.getFileSHA1();
        char c = tightSig.charAt(0);

        HashMap<String, String> skipHashes = skipOutput.get(fqn);
        boolean hasMatch = skipHashes != null && skipHashes.containsKey(tightSHA1);

        if (hasMatch) {
            matchCount.add(tightSHA1);
        } else {
            StringBuilder buf = new StringBuilder(256);
            if (hashOutput) {

                // Errors
                if (c == 'e' || c == 'E' || a instanceof Error) {

                    buf.append("E;");
                    buf.append(a.getPath());
                    buf.append(";");
                    if (noFQN) {
                        buf.append(a.toLooseString().replace('\n', ' '));
                    } else {
                        buf.append(a.toTightString().replace('\n', ' '));
                    }

                } else {

                    String parentSha1 = a.getParentFileSHA1();
                    buf.append("F;");
                    buf.append(a.getRelativePath());
                    buf.append(';');
                    buf.append(a.getBaseName());
                    buf.append(';');
                    buf.append(a.getSuffix());
                    buf.append(';');
                    buf.append(a.getZipLevel());
                    buf.append(';');
                    buf.append(parentSha1 != null ? parentSha1 : "\\N");
                    buf.append(';');
                    buf.append(fileSha1);
                    buf.append(";\\N;\\N;\\N;\\N");
                    String s = buf.toString();
                    if (prevFileLine.equals(s)) {
                        // No point printing it.
                    } else {
                        System.out.println(s);
                        prevFileLine = s;
                    }


                    // This hash includes both tight (fqn) and loose (no fqn).
                    buf = new StringBuilder(256);
                    buf.append("S;");
                    buf.append(fileSha1);
                    buf.append(';');
                    buf.append(a.getClassName());
                    buf.append(';');
                    buf.append(tightSHA1);
                    buf.append(';');
                    buf.append(looseSHA1);

                }


            } else {
                buf = new StringBuilder(path.length() + tightSig.length() + 16);
                buf.append("\nn;").append(path).append("\n").append(noFQN ? looseSig : tightSig);
            }

            if (hashOutput && !skipOutput.isEmpty()) {
                if (skipHashes == null) {
                    buf.append(";NO-FQN");
                } else {
                    buf.append(";MISMATCH");
                    for (String mismatchTarget : skipOutput.get(fqn).values()) {
                        Artifact b = toArtifact(mismatchTarget);
                        if (b instanceof Src) {
                            buf.append("\n").append(diff(b, a));
                        } else {
                            buf.append("\n").append(diff(a, b));
                        }
                    }
                }
            }
            System.out.println(buf.toString());
        }
    }

    private static String diff(Artifact a, Artifact b) {
        StringBuilder buf = new StringBuilder(1024);
        buf.append("- ").append(a.getPath()).append("\n");
        buf.append("+ ").append(b.getPath()).append("\n");
        buf.append("---\n");
        String s1 = a.toTightString().trim();
        String s2 = b.toTightString().trim();
        String[] toks1 = s1.split("[\n|\r]+");
        String[] toks2 = s2.split("[\n|\r]+");
        int i = 0;
        for (; i < Math.min(toks1.length, toks2.length); i++) {
            s1 = toks1[i];
            s2 = toks2[i];
            if (!s1.equals(s2)) {
                buf.append("- ").append(s1).append("\n");
                buf.append("+ ").append(s2).append("\n");
            }
        }
        for (; i < Math.max(toks1.length, toks2.length); i++) {
            String s = i < toks1.length ? toks1[i] : toks2[i];
            buf.append(i < toks1.length ? "- " : "+ ").append(s).append("\n");
        }
        return buf.toString();
    }

    public static String[] extractSHA1(String path) {
        if (path.startsWith(TAR_PATH)) {
            path = path.substring(TAR_PATH.length());
            int x = path.indexOf('/');
            int y = path.indexOf('/', x+1);
            if (y >= 0) {
                String[] toks = path.substring(0, x).split("-lvl");
                return new String[]{
                    toks[0],
                    toks[1],
                    path.substring(y+1),
                    path.substring(x+1, y)
                };
            }
        }
        return null;
    }


}
