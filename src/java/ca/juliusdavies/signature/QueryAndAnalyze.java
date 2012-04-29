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
import ca.phonelist.util.Pad;

import java.io.*;
import java.util.Locale;
import java.util.TreeMap;

public class QueryAndAnalyze {

    private final static TreeMap<String, String> JAR_SHA1_GROUND_TRUTH = new TreeMap<String, String>();
    private final static TreeMap<String, String> CLASS_SHA1_GROUND_TRUTH = new TreeMap<String, String>();

    // bitmaps
    public final static int ONE_POINT_OH = 1;
    public final static int SINGLE_MATCH = 2;
    public final static int PERFECT_MATCH = 4;
    public final static int BERTILLONAGE_MATCH = 8;

    // Common values.
    public final static int SINGLE_PERFECT_1 = ONE_POINT_OH | SINGLE_MATCH | PERFECT_MATCH;
    public final static int SINGLE_CORRECT_1 = ONE_POINT_OH | SINGLE_MATCH | BERTILLONAGE_MATCH;
    public final static int SINGLE_WRONG_1 = ONE_POINT_OH | SINGLE_MATCH;

    public final static int MULTI_PERFECT_1 = ONE_POINT_OH | PERFECT_MATCH;
    public final static int MULTI_CORRECT_1 = ONE_POINT_OH | BERTILLONAGE_MATCH;
    public final static int MULTI_WRONG_1 = ONE_POINT_OH;

    public final static int SINGLE_PERFECT_0 = SINGLE_MATCH | PERFECT_MATCH;
    public final static int SINGLE_CORRECT_0 = SINGLE_MATCH | BERTILLONAGE_MATCH;
    public final static int SINGLE_WRONG_0 = SINGLE_MATCH;

    public final static int MULTI_PERFECT_0 = PERFECT_MATCH;
    public final static int MULTI_CORRECT_0 = BERTILLONAGE_MATCH;
    public final static int MULTI_WRONG_0 = 0;

    private final static String SQL_JAR_SHA1
            = " SELECT DISTINCT basename || suffix AS \"a = _FILENAME_ (bin2bin)\" "
            + " FROM files WHERE filesha1 = '_SHA1_' ; ";

    private static PrintStream realOut = System.out;

    public static void main(final String[] args) throws IOException {

        FileInputStream fin = new FileInputStream(args[3]);
        InputStreamReader isr = new InputStreamReader(fin, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) { continue; }
            if ("".equals(line.trim())) { continue; }

            String[] toks = line.split(";");
            String type = toks[0];
            String subject = toks[2].toLowerCase(Locale.ENGLISH);
            String truth = subject;
            if (toks.length > 3 && !"".equals(toks[3].trim())) {
                truth = toks[3].toLowerCase(Locale.ENGLISH);
            }

            if ("jar-sha1".equals(type)) {
                JAR_SHA1_GROUND_TRUTH.put(subject, truth);
            } else if ("class-sha1".equals(type)) {
                CLASS_SHA1_GROUND_TRUTH.put(subject, truth);
            }
        }
        br.close();
        isr.close();
        fin.close();

        // args[0] should be '-qs' or '-qo' or '-qh' or '-qj'.
        String[] scanArgs = {args[0], "-si", null};
        String sortBy = args[1];
        if (".i".equals(sortBy)) {
            SQL.byInclusion();
        } else if (".j".equals(sortBy)) {
            SQL.byJaccard();
        } else if (".e".equals(sortBy)) {
            SQL.byEnclosion();
        } else {
            throw new IllegalArgumentException("2nd arg must be one of: .i / .j / .e");
        }

        fin = new FileInputStream(args[2]);
        isr = new InputStreamReader(fin, "UTF-8");
        br = new BufferedReader(isr);

        FileOutputStream fout;
        // File f;

        int count = 1;
        while ((line = br.readLine()) != null) {
            String[] toks = line.split(";");
            String lineNumber = toks[0];
            if (toks.length > 1) {
                line = toks[1];
            } else {
                line = lineNumber;
            }

            scanArgs[scanArgs.length - 1] = line;

            String query;

            long seconds, delay, start = System.currentTimeMillis();
            if ("-qj".equals(args[0])) {

                File myF = new File(line);
                if (line.startsWith("Fixing")) {
                    continue;
                }
                FileInputStream myFin = new FileInputStream(myF);

                String sha1 = Hash.sha1(myFin);

                myFin.close();
                query = SQL_JAR_SHA1.replace("_SHA1_", sha1);
                query = query.replace("_FILENAME_", myF.getName());

                realOut.println(query);

            } else {

                // We use this to intercept SimpleScan's stdout.
                ByteArrayOutputStream bout = new ByteArrayOutputStream(8192);
                PrintStream ps = new PrintStream(bout, false, "UTF-8");
                System.setOut(ps);

                // Now we call SimpleScan's main().
                SimpleScan.main(scanArgs);

                ps.flush();
                ps.close();
                bout.close();
                query = bout.toString("UTF-8");
            }

            int x = query.indexOf("inclusion DESC ;");
            if (x >= 0) {
                query = query.substring(0, x + "inclusion DESC ;".length());
            }
            if (query.contains("(bin")) {

                delay = System.currentTimeMillis() - start;
                seconds = delay / 1000;
                String timeToGenerateSQL = seconds + "." + Pad.leftWithZeroes(delay % 1000, 3);

                start = System.currentTimeMillis();
                final String result = Unpacker.psql(query).trim();
                delay = System.currentTimeMillis() - start;
                seconds = delay / 1000;
                String timeToExecuteSQL = seconds + "." + Pad.leftWithZeroes(delay % 1000, 3);

                String[] lines = result.split("\n");
                String firstLine = lines[0];
                toks = firstLine.split("=");
                String subject = toks[toks.length - 1];
                x = subject.indexOf('(');
                if (x >= 0) {
                    subject = subject.substring(0, x).trim();
                }
                boolean noData = lines[2].startsWith("(0 rows)");

                // String dir = "result" + args[0];
                String dir = args[2] + args[0] + args[1];

                int analysis = 0;

                // final File f = new File(dir + "/query-" + lineNumber + ".txt");
                File f;

                String subj = subject.toLowerCase(Locale.ENGLISH);
                if (JAR_SHA1_GROUND_TRUTH.containsKey(subj)) {
                    dir = dir + "/JAR-SHA1-TRUTH";
                } else if (CLASS_SHA1_GROUND_TRUTH.containsKey(subj)) {
                    dir = dir + "/CLASS-SHA1-TRUTH";
                } else {
                    dir = dir + "/NEW-ANALYSIS";
                }

                int[] artifactSize = {0};
                int[] topCount = {0};
                String[] topJaccard = {null};
                if (noData) {
                    f = new File(dir + "/NO_MATCH/" + line + ".txt");
                } else {
                    if ("-qj".equals(args[0])) {
                        boolean multiple = lines.length > 4;
                        if (multiple) {
                            analysis = MULTI_PERFECT_1;
                        } else {
                            analysis = SINGLE_PERFECT_1;
                        }
                    } else {
                        analysis = analyze(subject, result, topCount, topJaccard, artifactSize);
                    }
                    f = getFile(dir, line, analysis);
                }
                f.getParentFile().mkdirs();
                fout = new FileOutputStream(f);

                StringBuilder buf = new StringBuilder();
                buf.append(artifactSize[0]);
                buf.append(",  ,");
                buf.append(timeToGenerateSQL).append(',').append(timeToExecuteSQL);
                buf.append(",  ,");
                buf.append(topCount[0]);
                buf.append(",  ,");
                buf.append(topJaccard[0]);
                buf.append(",  ,");
                buf.append(subject).append(',');
                if (noData) {
                    buf.append(" NO DATA");
                } else {
                    if ((analysis & ONE_POINT_OH) == ONE_POINT_OH) {
                        buf.append("    1.000  ");
                    } else {
                        buf.append(" (0-0.999) ");
                    }
                    if ((analysis & SINGLE_MATCH) == SINGLE_MATCH) {
                        buf.append(" SINGLE ");
                    } else {
                        buf.append(" MULTI  ");
                    }
                    if ((analysis & PERFECT_MATCH) == PERFECT_MATCH) {
                        buf.append(" PERFECT ");
                    } else {
                        if ((analysis & BERTILLONAGE_MATCH) == BERTILLONAGE_MATCH) {
                            buf.append(" CORRECT ");
                        } else {
                            buf.append(" WRONG ");
                        }
                    }
                }
                buf.append('\n');
                buf.append(result);
                buf.append('\n');
                fout.write(buf.toString().getBytes("UTF-8"));
                fout.close();

                count++;
            }

        }
    }

    public static File getFile(String dir, String line, int analysis) {
        switch (analysis) {
            case SINGLE_PERFECT_1:
                return new File(dir + "/SINGLE/PERFECT/1.000/" + line + ".txt");
            case SINGLE_CORRECT_1:
                return new File(dir + "/SINGLE/CORRECT/1.000/" + line + ".txt");
            case SINGLE_WRONG_1:
                return new File(dir + "/SINGLE/WRONG/1.000/" + line + ".txt");
            case MULTI_PERFECT_1:
                return new File(dir + "/MULTI/PERFECT/1.000/" + line + ".txt");
            case MULTI_CORRECT_1:
                return new File(dir + "/MULTI/CORRECT/1.000/" + line + ".txt");
            case MULTI_WRONG_1:
                return new File(dir + "/MULTI/WRONG/1.000/" + line + ".txt");
            case SINGLE_PERFECT_0:
                return new File(dir + "/SINGLE/PERFECT/0.999/" + line + ".txt");
            case SINGLE_CORRECT_0:
                return new File(dir + "/SINGLE/CORRECT/0.999/" + line + ".txt");
            case SINGLE_WRONG_0:
                return new File(dir + "/SINGLE/WRONG/0.999/" + line + ".txt");
            case MULTI_PERFECT_0:
                return new File(dir + "/MULTI/PERFECT/0.999/" + line + ".txt");
            case MULTI_CORRECT_0:
                return new File(dir + "/MULTI/CORRECT/0.999/" + line + ".txt");
            case MULTI_WRONG_0:
                return new File(dir + "/MULTI/WRONG/0.999/" + line + ".txt");
            default:
                return new File(dir + "/UNKNOWN/" + line + ".txt");
        }
    }

    public static int analyze(String jar, String result, int[] topCount, String[] topJaccard, int[] size) {

        jar = jar.toLowerCase(Locale.ENGLISH);
        result = result.toLowerCase(Locale.ENGLISH);
        String family = jar;

        if (JAR_SHA1_GROUND_TRUTH.containsKey(jar)) {
            jar = JAR_SHA1_GROUND_TRUTH.get(jar);
        } else if (CLASS_SHA1_GROUND_TRUTH.containsKey(jar)) {
            jar = CLASS_SHA1_GROUND_TRUTH.get(jar);
        }

        // Ideally, "family" is the substring up to the last dash before
        // version numbering.
        int x = jar.indexOf('.');
        if (x >= 0) {
            family = jar.substring(0, x);
            x = family.lastIndexOf('-');
            if (x >= 0) {
                family = family.substring(0, x);
            }
        }

        jar = stripPathAndSuffix(jar);

        int targetPosition = -1;
        int relatedPosition = -1;
        String bestJaccard = "";
        boolean matchedMultiple = false;

        String targetJaccard = "blah";
        String relatedJaccard = "blah";

        String[] rows = result.split("\n");

        boolean bin2src = rows[0].contains("src)");

        for (int i = 2; i < rows.length - 1; i++) {
            String row = rows[i];
            String[] toks = row.split("\\|");

            String artifact = toks[toks.length - 1].trim();
            artifact = stripPathAndSuffix(artifact);


            if (i == 2) {
                bestJaccard = toks[2].trim(); // a_intersect_b doesn't have round-off problems.
                topCount[0] = 1;
                topJaccard[0] = bestJaccard;
                size[0] = Integer.parseInt(toks[0].trim());
            } else if (i > 2) {
                if (toks[2].trim().equals(bestJaccard)) {
                    topCount[0] = topCount[0] + 1;
                } else {
                    // We're only interested in top-matches.
                    break;
                }
            }

            if (i >= 2) {
                boolean isEqual = jar.equals(artifact);
                if (!isEqual && bin2src) {
                    isEqual = (jar + "-sources").equals(artifact) ||
                            (jar + "-src").equals(artifact) ||
                            (jar + "-source-release").equals(artifact);
                }
                if (isEqual) {
                    targetPosition = (i - 1);
                    targetJaccard = toks[4].trim();
                } else if (relatedPosition == -1 && artifact.contains(family)) {
                    relatedPosition = (i - 1);
                    relatedJaccard = toks[4].trim();
                }
            }
        }

        String before = "";
        String after = "";
        if (targetPosition != -1) {
            if (targetPosition > 1) {
                before = rows[targetPosition].split("\\|")[4].trim();
            }
            if (targetPosition + 3 < rows.length) {
                after = rows[targetPosition + 2].split("\\|")[4].trim();
            }
            matchedMultiple = targetJaccard.equals(before) || targetJaccard.equals(after);
        } else if (relatedPosition != -1) {
            if (relatedPosition > 1) {
                String row = rows[relatedPosition];
                before = row.split("\\|")[4].trim();
            }
            if (relatedPosition + 3 < rows.length) {
                String row = rows[relatedPosition + 2];
                after = row.split("\\|")[4].trim();
            }
            matchedMultiple = relatedJaccard.equals(before) || relatedJaccard.equals(after);
        }

        int analysis = 0;
        if ("1.000".equals(bestJaccard)) {
            analysis |= ONE_POINT_OH;
        }
        if (targetPosition != -1) {
            if (bestJaccard.equals(targetJaccard)) {
                analysis |= PERFECT_MATCH;
            } else {
                analysis |= BERTILLONAGE_MATCH;
            }
        }
        if (!matchedMultiple) {
            analysis |= SINGLE_MATCH;
        }
        if (relatedPosition != -1 && !relatedJaccard.equals(targetJaccard)) {
            analysis |= BERTILLONAGE_MATCH;
        }
        return analysis;
    }

    private static String normalizeName(final String n) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (Character.isDigit(c) || Character.isLetter(c) || c == '.') {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private static String stripPathAndSuffix(final String suppliedPath) {
        String path = suppliedPath;
        int x = path.lastIndexOf('/');
        if (x >= 0) {
            path = path.substring(x + 1);
        }

        x = path.lastIndexOf(".tar.");
        if (x >= 0) {
            // Worst case:  .tar.lzma
            if (x + 8 > path.length()) {
                path = path.substring(0, x);
                return path;
            }
        }

        x = path.lastIndexOf('.');
        if (x >= 0) {
            String suffix = path.substring(x + 1);
            if ("jar".equals(suffix) || "zip".equals(suffix) || "tgz".equals(suffix) || "war".equals(suffix) || "ear".equals(suffix)) {
                path = path.substring(0, x);
                return path;
            }
        }
        return path;
    }

}
