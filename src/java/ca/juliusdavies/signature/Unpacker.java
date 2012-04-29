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

import ca.phonelist.util.Bytes;
import ca.phonelist.util.Strings;
import ca.phonelist.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Julius Davies
 * @since Mar 28, 2010
 */
public abstract class Unpacker {

    private static class ExecResult {
        public final String output;
        public final int exitStatus;

        public ExecResult(String output, int exit) {
            this.output = output;
            this.exitStatus = exit;
        }
    }

    private static String SOURCE_DIR = ".";
    private static String TARGET_DIR = ".";
    public static boolean INCLUDE_ZIPS = false;
    public static boolean SHOW_OUTPUT = false;

    /*

   .zip
   .jar
   .tar.bz2
   .tar.gz
   .tgz
   .jar.pack.gz

    */

    public static void main(String[] args) throws IOException {
        INCLUDE_ZIPS = true;
        SHOW_OUTPUT = true;

        if (args.length > 0) {
            TARGET_DIR = args[0];
        }
        if (args.length > 1) {
            SOURCE_DIR = args[0];
            TARGET_DIR = args[1];
        }
        SOURCE_DIR = new File(SOURCE_DIR).getCanonicalPath();
        TARGET_DIR = new File(TARGET_DIR).getCanonicalPath();
        if (SOURCE_DIR.equals(TARGET_DIR)) {
            System.err.println("Source and target must be different: [" + SOURCE_DIR + "]");
            System.exit(1);
        }
        scan(new File(SOURCE_DIR));
    }

    public static void unpack(String source, String target) throws IOException {
        File src = new File(source).getCanonicalFile();
        if (src.isDirectory()) {
            SOURCE_DIR = src.getCanonicalPath();
        } else {
            SOURCE_DIR = src.getParentFile().getCanonicalPath();
        }

        TARGET_DIR = new File(target).getCanonicalPath();
        scan(src);
    }

    private static void scan(File d) throws IOException {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                scan(f);
            }
        } else if (d.isFile() && d.canRead() && d.length() > 0) {
            extract(d);
        }
    }

    static void extract(File f) throws IOException {
        String name = f.getName().toLowerCase(Locale.ENGLISH);
        boolean isTar
                =  name.endsWith(".tar")
                || name.endsWith(".tar.bz2")
                || name.endsWith(".tar.gz")
                || name.endsWith(".tar.lzma")
                || name.endsWith(".tar.xz")
                || name.endsWith(".tgz")
                || name.endsWith(".deb")
/*
                || name.endsWith(".rpm")
                || name.endsWith(".Z")
                || name.endsWith(".rar")
                || name.endsWith(".ar")
                || name.endsWith(".cpio")
                || name.endsWith(".jar.pack")
                || name.endsWith(".jar.pack.gz")
*/
                || (INCLUDE_ZIPS && name.endsWith(".zip"))
                || (INCLUDE_ZIPS && name.endsWith(".jar"));

        if (!isTar) {
            return;
        }


        String filePath = f.getCanonicalPath();
        String dir = f.getParentFile().getCanonicalPath();
        String t;
        File T;
        if (dir.startsWith(SOURCE_DIR)) {
            t = dir.replace(SOURCE_DIR, TARGET_DIR);
            T = new File(t + "/" + f.getName());
            if (!T.exists()) {
                T.mkdirs();
            }
            t = T.getCanonicalPath();
        } else {
            throw new RuntimeException("sourceDir not present: [" + dir + "] vs [" + SOURCE_DIR + "]");
        }

        String archive = f.getCanonicalPath();
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/bin/bash");
        cmd.add("-c");
        if (name.endsWith(".tar.bz2")) {
            cmd.add("bunzip2 -c " + archive + " | tar -x -C " + t);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            cmd.add("gunzip -c " + archive + " | tar -x -C " + t);
        } else if (name.endsWith(".tar.lzma") || name.endsWith(".tgz")) {
            cmd.add("unlzma -c " + archive + " | tar -x -C " + t);
        } else if (name.endsWith(".tar.xz") || name.endsWith(".tgz")) {
            cmd.add("unxz -c " + archive + " | tar -x -C " + t);
        } else if (name.endsWith(".tar")) {
            cmd.add("tar -x -C " + t + " -f " + archive);
        } else if (name.endsWith(".deb")) {
            cmd.add("cd " + t + "; ar x " + archive);
        } else if (name.endsWith("jar.pack.gz") || name.endsWith("jar.pack")) {
            int charsAfterJar = name.endsWith("jar.pack.gz") ? 8 : 5;
            int endPos = archive.length() - charsAfterJar;

            cmd.add("unpack200 " + archive + " " + archive.substring(0, endPos));
            exec(cmd, null);

            File jar = new File(archive.substring(0, archive.length() - 8));
            unzip(new ZipFile(jar), t);
            jar.delete();
            return;
        } else if (name.endsWith(".zip") || name.endsWith(".jar")) {
            unzip(new ZipFile(f), t);
            return;
        } else {
            return;
        }

        exec(cmd, null);

    }

    public static String psql(String query) throws IOException {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/bin/bash");
        cmd.add("-c");
        cmd.add("psql maven");

        return exec(cmd, query).output;
    }

    public static String grep(String pkg, File f) throws IOException {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/bin/bash");
        cmd.add("-c");

        // We're not interested in 'dev' packages.
        cmd.add("grep '/" + pkg + "' " + f.getPath() + " | grep -v '/" + pkg + "\\-\\|/lib" + pkg + "' ");

        String grepLine = exec(cmd, null).output;
        int lines = Strings.countChar(grepLine, '\n');

        // Try again with a more aggressive grep pattern:
        if (lines > 1) {
            cmd.remove(cmd.size()-1);
            cmd.add("grep '/" + pkg + "/" + pkg + "' " + f.getPath() + " | grep -v '/" + pkg + "\\-\\|/lib" + pkg + "' ");

            grepLine = exec(cmd, null).output;
            lines = Strings.countChar(grepLine, '\n');
        }

        if (lines > 1) {
            System.out.println("DOH: " + lines + " [ " + cmd.get(cmd.size()-1) + " ] ");
            return "DOH";
        } else {
            return grepLine.trim();
        }

    }

    public static int wget(String url) throws IOException {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/bin/bash");
        cmd.add("-c");
        cmd.add("wget -r --continue " + url);


        int result = exec(cmd, null).exitStatus;
        System.out.print(cmd.get(cmd.size() - 1) + "(" + result + ")");
        return result;
    }

    public static int rmrf(String dir) throws IOException {
        File f = new File(dir);
        String path = f.getCanonicalPath();

        // We only rm -rf paths in our TAR_PATH directory.
        if (!path.startsWith(SimpleScan.TAR_PATH)) {
            return -9;
        }

        if (f.exists()) {
            ArrayList<String> cmd = new ArrayList<String>();
            cmd.add("/bin/bash");
            cmd.add("-c");
            cmd.add("rm -rf " + path);
            return exec(cmd, null).exitStatus;
        } else {
            return 0;
        }
    }

    public static ExecResult exec(ArrayList<String> cmd, String input) throws IOException {
        if (SHOW_OUTPUT) { System.out.print(cmd.get(cmd.size() - 1)); }
        String[] exec = cmd.toArray(new String[cmd.size()]);
        Process p = null;
        int returnVal = -1;
        String output = null;
        try {
            Thread.sleep(1);
            p = Runtime.getRuntime().exec(exec);
            InputStream in = p.getInputStream();
            OutputStream out = p.getOutputStream();
            InputStream err = p.getErrorStream();

            if (input != null) {
                try {

                    out.write(input.getBytes("UTF-8"));
                    out.flush();
                    out.close();
                    out = null;

                } catch (Exception e) {

                    System.out.println("E;exec failed: " + e + " " + cmd);

                } finally {
                    Util.close(out);
                }
            }

            try {

                byte[] b = Bytes.streamToBytes(in);
                output = new String(b, "UTF-8");

                b = Bytes.streamToBytes(err);
                if (b.length > 0) {
                    System.err.println(new String(b, "UTF-8"));
                }

                if (out != null) { out.close(); }

            } catch (Exception e) {
                System.out.println("E;untarring failed: " + e + " " + cmd);
            } finally {
                returnVal = p.waitFor();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } finally {
            if (p != null) { p.destroy(); }
        }
        return new ExecResult(output, returnVal);
    }

    public static void unzip(ZipFile zf, String target) throws IOException {
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement();
            File f = new File(target, ze.getName());
            f.getParentFile().mkdirs();
            if (!ze.isDirectory()) {
                InputStream in;
                FileOutputStream fout;
                in = zf.getInputStream(ze);
                fout = new FileOutputStream(f);

                try {
                    byte[] buf = new byte[4096];
                    int c = in.read(buf);
                    while (c >= 0) {
                        if (c > 0) {
                            fout.write(buf, 0, c);
                        }
                        c = in.read(buf);
                    }
                } finally {
                    fout.close();
                    in.close();
                }

            }
        }
        if (SHOW_OUTPUT) { System.out.println("Unzipped " + zf.getName() + " to " + target); }
    }
}
