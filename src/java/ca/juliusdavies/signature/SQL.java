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

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class SQL {

    private final static boolean INCLUDE_PATH = true;

    public static String specialOrder = null;

    // Are we running in loose mode or tight mode?
    private static boolean noFQN = SimpleScan.noFQN;

    private final static Comparator<Artifact> BY_SIG = new Comparator<Artifact>() {
        public int compare(Artifact a1, Artifact a2) {
            String s1, s2;
            if (noFQN) {
                s1 = a1.toLooseSHA1();
                s2 = a2.toLooseSHA1();
            } else {
                s1 = a1.toTightSHA1();
                s2 = a2.toTightSHA1();
            }
            return NullSafe.compare(s1, s2);
        }
    };

    public final static int SAME_2_SAME = 0;
    public final static int SAME_2_OTHER = 1;
    public final static int FILE_SHA1 = 2;

    public static String buildQuery(Collection<Artifact> artifacts, int queryStyle) {

        TreeSet<Bin> bins = new TreeSet<Bin>(BY_SIG);
        TreeSet<Src> srcs = new TreeSet<Src>(BY_SIG);
        String binName = "*.jar";
        String srcName = "*.jar";
        for (Artifact a : artifacts) {
            try {
                if (a instanceof Bin) {
                    char c = a.toTightString().charAt(0);
                    if (c != 'E' && c != 'e') {
                        bins.add((Bin) a);
                        binName = extractName(binName, a.path, a);
                    }
                }
                if (a instanceof Src) {
                    char c = a.toTightString().charAt(0);
                    if (c != 'E' && c != 'e') {
                        srcs.add((Src) a);
                        srcName = extractName(srcName, a.path, a);
                    }
                }
            } catch (Exception e) {
                System.err.println("a signature failed when building SQL query: " + e);
            }
        }
        artifacts.clear();

        String q = "";
        if (!bins.isEmpty()) {
            q += binQuery(bins, binName, queryStyle) + "\n";
        }
        if (!srcs.isEmpty()) {
            // Throw in a blank line inbetween queries.
            if (!q.equals("")) { q += "\n"; }

            q += srcQuery(srcs, srcName, queryStyle) + "\n";
        }
        return q;
    }

    private static String extractName(String current, String path, Artifact artifact) {
        if ("[various...]".equals(current)) { return current; }

        int a = path.indexOf(".jar");
        int b = path.indexOf(".war");
        int c = path.indexOf(".ear");
        int d = path.indexOf(".zip");
        a = a == -1 ? Integer.MAX_VALUE : a;
        b = b == -1 ? Integer.MAX_VALUE : b;
        c = c == -1 ? Integer.MAX_VALUE : c;
        d = d == -1 ? Integer.MAX_VALUE : d;
        int x = Math.min(a, Math.min(b, Math.min(c, d)));

        if (x == Integer.MAX_VALUE && artifact.getTarName() != null) {
            return artifact.getTarName();
        }
        if (x > 0) {
            String name = path.substring(0, x + 4);
            x = name.lastIndexOf('/');
            if (x > 0) {
                name = name.substring(x + 1);
            }

            if ("*.jar".equals(current) || name.equals(current)) {
                return name;
            } else {
                return "[various...]";
            }
        } else {
            return current;
        }
    }

    private static String srcQuery(Set<Src> srcs, String srcFileName, int queryStyle) {
        String tallyColumn;
        if (SAME_2_SAME == queryStyle) {
            tallyColumn = "uniqjavasigre";
            srcFileName = srcFileName + "  (src2src)";
        } else {
            tallyColumn = "uniqclasssigre";
            srcFileName = srcFileName + "  (src2bin)";
        }
        return buildQuery((TreeSet<? extends Artifact>) srcs, srcFileName, tallyColumn, queryStyle);
    }

    private static String binQuery(Set<Bin> bins, String binFileName, int queryStyle) {
        String tallyColumn;
        if (SAME_2_OTHER == queryStyle) {
            tallyColumn = "uniqjavasigre";
            binFileName = binFileName + "  (bin2src)";
        } else {
            tallyColumn = "uniqclasssigre";
            binFileName = binFileName + "  (bin2bin)";
        }
        return buildQuery((TreeSet<? extends Artifact>) bins, binFileName, tallyColumn, queryStyle);
    }

    private static String buildQuery(
            TreeSet<? extends Artifact> sigs, String archiveName, String tallyColumn, int queryStyle
    ) {
        String suffix = "uniqjavasigre".equals(tallyColumn) ? ".java" : ".class";

        // Each sig in query requires 44 chars.
        int n = sigs.size();
        StringBuilder buf = new StringBuilder(2048 + (44 * n));
        buf.append("SELECT DISTINCT \n");
        buf.append("  ").append(n).append(" AS a,\n");
        buf.append("  ").append(tallyColumn).append(" AS b,\n");
        buf.append("  a_intersect_b,\n");
        buf.append("  ").append(n).append(" + ").append(tallyColumn).append(" - a_intersect_b AS a_union_b,\n");
        buf.append("  TO_CHAR(1.0 * a_intersect_b / (").append(n).append(" + ").append(tallyColumn).append(" - a_intersect_b), '0.999') AS jaccard,\n");
        buf.append("  TO_CHAR(1.0 * a_intersect_b /  ").append(n).append(", '0.999') AS inclusion,\n");
        buf.append("  TO_CHAR(1.0 * a_intersect_b /  ").append(tallyColumn).append(", '0.999') AS enclosion,\n");
        buf.append("  f.filesha1, ");
        buf.append("  basename || suffix AS \"a = ");
        buf.append(n).append(" = ").append(archiveName).append("\"");
        if (INCLUDE_PATH) {
            buf.append(",\n  path");
        }
        buf.append("\nFROM files f INNER JOIN (\n");

        if (queryStyle == FILE_SHA1) {
            // I don't think we actually need to "NATURAL JOIN sigs" in this case, but it keeps
            // things consistent in case we need to play with the general query template here.
            buf.append("  SELECT COUNT(DISTINCT filesha1)  AS a_intersect_b, infilesha1 FROM files ");
        } else {
            buf.append("  SELECT COUNT(DISTINCT sigsha1re) AS a_intersect_b, infilesha1 FROM files NATURAL JOIN sigs ");
        }

        buf.append(" WHERE suffix = '").append(suffix);

        if (queryStyle == FILE_SHA1) {
            buf.append("' AND filesha1 IN (");
        } else {
            buf.append("' AND sigs.sigsha1re IN (");
        }

        while (sigs.size() > 3) {
            Artifact a, b, c;
            a = sigs.pollFirst();
            b = sigs.pollFirst();
            c = sigs.pollFirst();
            buf.append("\n'");
            if (queryStyle == FILE_SHA1) {
                buf.append(a.getFileSHA1()).append("', '");
                buf.append(b.getFileSHA1()).append("', '");
                buf.append(c.getFileSHA1()).append("',");
            } else {
                if (noFQN) {
                    buf.append(a.toLooseSHA1()).append("', '");
                    buf.append(b.toLooseSHA1()).append("', '");
                    buf.append(c.toLooseSHA1()).append("',");
                } else {
                    buf.append(a.toTightSHA1()).append("', '");
                    buf.append(b.toTightSHA1()).append("', '");
                    buf.append(c.toTightSHA1()).append("',");
                }
            }
        }
        buf.append("\n");
        while (!sigs.isEmpty()) {
            Artifact a = sigs.pollFirst();
            if (queryStyle == FILE_SHA1) {
                buf.append("'").append(a.getFileSHA1()).append("', ");
            } else {
                if (noFQN) {
                    buf.append("'").append(a.toLooseSHA1()).append("', ");
                } else {
                    buf.append("'").append(a.toTightSHA1()).append("', ");
                }
            }
        }
        // remove final space and comma:
        buf.deleteCharAt(buf.length() - 1);
        buf.setCharAt(buf.length() - 1, '\n');

        buf.append(") GROUP BY infilesha1 ) t ON (f.filesha1 = t.infilesha1)");
        buf.append(" WHERE ").append(tallyColumn).append(" > 0 AND basename SIMILAR TO '%[0-9]%' ");
        buf.append(" ORDER BY ");
        if (specialOrder == null) {
            buf.append("jaccard DESC, inclusion DESC ");
        } else {
            buf.append(specialOrder);
        }
        buf.append(";\n");
        if (noFQN) {
            return buf.toString().replace("sigsha1re", "losesigsha1re");
        } else {
            return buf.toString();
        }
    }

    public static void byJaccard() { specialOrder = " jaccard DESC, inclusion DESC "; }
    public static void byInclusion() { specialOrder = " inclusion DESC, jaccard DESC "; }
    public static void byEnclosion() { specialOrder = " enclosion DESC, inclusion DESC, jaccard DESC "; }

}

