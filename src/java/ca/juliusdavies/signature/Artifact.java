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
import ca.phonelist.util.Strings;
import org.objectweb.asm.commons.EmptyVisitor;

import java.util.Date;
import java.util.Locale;

/**
 * @author Julius Davies
 * @since Apr 18, 2010
 */
public abstract class Artifact extends EmptyVisitor implements Comparable<Artifact> {

    protected long date;
    protected String tarName;
    protected String fileName;
    protected String path;
    protected String sigSHA1;
    protected String looseSig;
    protected String looseSigSHA1;
    protected String parentFileSHA1;
    protected byte[] bytes;
    protected String fileSHA1;
    protected int zipLevel = -2;

    Artifact() {}

    public Artifact(String fileName, String path, long date, byte[] bytes, String parentFileSHA1) {
        this.date = date;
        this.fileName = fileName;
        this.path = path;
        this.bytes = bytes;

        String[] sha1AndLevel = SimpleScan.extractSHA1(path);
        if (sha1AndLevel != null) {
            this.parentFileSHA1 = sha1AndLevel[0];
            this.zipLevel = Integer.parseInt(sha1AndLevel[1]);
            this.path = sha1AndLevel[2];
            this.tarName = sha1AndLevel[3];
        }

        // ParentFileSHA1 supplied as parameter trumps the one in the path.
        if (parentFileSHA1 != null) {
            this.parentFileSHA1 = parentFileSHA1;
        }

        if (bytes != null && !(this instanceof Bin)) {
            fileSHA1 = Hash.sha1(bytes);
        }
    }

    public abstract String getPackageName();
    public abstract String getClassName();
    public abstract String toTightString();
    public abstract int getMinLine();
    public abstract int getMaxLine();
    public abstract int getMethodCount();

    public final String getTarName() { return tarName; }

    private void initStrings() {
        if (sigSHA1 == null) {
            String s = toTightString().trim();
            sigSHA1 = Hash.sha1(s + "\n");

            // Might be an error.
            char c = s.charAt(0);
            if (c == 'e' || c =='E') {
                looseSig = s;
                looseSigSHA1 = sigSHA1;
                return;
            }

            int x = s.indexOf('\n');
            if (x == -1) { x = s.length(); }

            String classLine = s.substring(0, x);
            String[] toks = classLine.substring(4).split(" ");
            StringBuilder buf = new StringBuilder();
            buf.append(classLine.substring(0,4));
            String prevTok = "";
            boolean done = false;
            for (String tok : toks) {
                if (!done && ("class".equals(prevTok) || "interface".equals(prevTok))) {
                    buf.append(Names.fixTypeName(tok));
                    done = true;
                } else {
                    buf.append(tok);
                }
                buf.append(' ');
                prevTok = tok;
            }

            this.looseSig = buf.toString().trim() + s.substring(x);
            this.looseSigSHA1 = Hash.sha1(looseSig + "\n");
        }
    }

    public final String toLooseString() {
        initStrings();
        return looseSig;
    }

    public String toTightSHA1() {
        initStrings();
        return sigSHA1;
    }

    public String toLooseSHA1() {
        initStrings();
        return looseSigSHA1;
    }

    public final byte[] getBytes() { return bytes; }

    public final String getFileSHA1() { return fileSHA1; }

    public String getFQN() {
        String pkg = getPackageName();
        return "".equals(pkg) ? getClassName() : pkg + "." + getClassName();
    }

    public final String getFileName() { return fileName; }
    public final String getPath() { return path; }

    public final String getRelativePath() {
        String s = path;

        int x = path.lastIndexOf("!/");
        if (x >= 0) { s = s.substring(x+2); }

        x = s.lastIndexOf('/');
        if (x >= 0) { s = s.substring(0, x+1); }

        x = s.lastIndexOf(';');
        if (x >= 0) { s = s.substring(0, x); }

        return s;
    }

    public final String getBaseName() {
        int x = fileName.lastIndexOf('.');
        if (x >= 0) {
            if (x > 4) {
                if (".tar".equalsIgnoreCase(fileName.substring(x-4,x))) {
                    x = x - 4;
                }
            }
            return fileName.substring(0, x);
        } else {
            return fileName;
        }
    }

    public final String getSuffix() {
        int x = fileName.lastIndexOf('.');
        if (x >= 0) {
            if (x > 4) {
                if (".tar".equalsIgnoreCase(fileName.substring(x-4,x))) {
                    x = x - 4;
                }
            }
            return fileName.substring(x).toLowerCase(Locale.ENGLISH);
        } else {
            return "";
        }
    }

    public final int getZipLevel() {
        int lvl = Strings.countChar(path, '!');
        return zipLevel == -2 ? lvl-1 : zipLevel + lvl + 1;
    }

    public Date getDate() { return new Date(date); }
    public long getSize() { return bytes.length; }

    public final String getParentFileSHA1() { return parentFileSHA1; }

    public int compareTo(Artifact a) {
        String s1 = getPackageName();
        String s2 = a.getPackageName();
        int c = s1.compareTo(s2);
        if (c == 0) {
            s1 = getClassName();
            s2 = a.getClassName();
            c = s1.compareTo(s2);
        }
        return c;
    }


    public static String extractVersion(String zipName, String path) {
        String version = "";
        String[] S = path.split("/");
        for (int i = S.length - 1; i >= 0; i--) {
            String s = S[i].trim();
            if (s.length() > 0) {
                char c = s.charAt(0);
                if (Character.isDigit(c)) {
                    version = s;
                    break;
                }
            }
        }
        if ("".equals(version) && zipName != null) {
            S = zipName.split("-");
            for (int i = 0; i < S.length; i++) {
                String s = S[i].trim();
                if (s.length() > 0) {
                    char c = s.charAt(0);
                    if (Character.isDigit(c)) {
                        i++;
                        for (; i < S.length; i++) {
                            s += "-" + S[i];
                        }
                        int x = s.lastIndexOf('.');
                        if (x >= 0) {
                            s = s.substring(0, x);
                        }
                        version = "*" + s;
                        break;
                    }
                }
            }

            if ("".equals(version)) {
                int x = zipName.lastIndexOf('-');
                if (x >= 0) {
                    x++;
                    int y = zipName.lastIndexOf('.');
                    if (y < 0) {
                        y = zipName.length();
                    }
                    if (x < zipName.length()) {
                        version = "**" + zipName.substring(x, y);
                    }
                }
            }
        }
        return version;
    }


    public static void indentForCurrentLevel(int level, StringBuilder buf) {
        for (int i = 0; i < level; i++) { buf.append("  "); }
    }

}

