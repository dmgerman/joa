package ca.juliusdavies.signature.testgarbage;

import java.io.File;
import java.io.Serializable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Enumeration;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Julius Davies
 * @since Mar 28, 2010
 */
public abstract class B implements Serializable,Comparable < Map < Map < String,
    List < Integer > >,
    Number
    >
    >, Cloneable {

    private static String CURRENT_DIR = "";
    private static String TARGET_DIR = "";
    private static boolean INCLUDE_ZIPS = false;

    /*

   .zip
   .tar.bz2
   .tar.gz
   .tgz

    */

    abstract void f();

    public static void main(
        String[] args
    ) throws
        Exception,
        RuntimeException,
        NumberFormatException,
        NullPointerException

    {
        CURRENT_DIR = args[1];
        TARGET_DIR = args[2];
        INCLUDE_ZIPS = args.length > 3 && "zips".equalsIgnoreCase(args[3]);
        System.out.println("Including zips: " + INCLUDE_ZIPS);
        
        scan(new File(args[0]));
    }

    protected synchronized static void scan(File d) throws Exception {
        if (d.isDirectory()) {
            for (File f : d.listFiles()) {
                scan(f);
            }
        } else if (d.isFile() && d.canRead() && d.length() > 0) {
            extract(d);
        }
    }

    static void extract(File f) throws Exception {
        String name = f.getName().toLowerCase(Locale.ENGLISH);
        boolean isTar
            = name.endsWith(".tar.gz")
            || name.endsWith(".tgz")
            || name.endsWith(".tar.bz2")
            || name.endsWith(".tar")
            || (INCLUDE_ZIPS && name.endsWith(".zip"))
            || (INCLUDE_ZIPS && name.endsWith(".jar")); 

        if (!isTar) {
            return;
        }

        String dir = f.getParentFile().getCanonicalPath();
        String t = dir;
        File T = null;
        if (dir.startsWith(CURRENT_DIR)) {
            t = dir.replace(CURRENT_DIR, TARGET_DIR);
            T = new File(t + "/" + f.getName());
            if (!T.exists()) {
                T.mkdirs();
            }
            t = T.getCanonicalPath();
        } else {
            throw new RuntimeException("currentDir not present: [" + dir + "] vs [" + CURRENT_DIR + "]");
        }

        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/bin/bash");
        cmd.add("-c");
        if (name.endsWith(".tar.bz2")) {
            cmd.add("bunzip2 -c " + f.getCanonicalPath() + " | tar -x -C " + t);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            cmd.add("gunzip -c " + f.getCanonicalPath() + " | tar -x -C " + t);
        } else if (name.endsWith(".tar")) {
            cmd.add("tar -x -C " + t + " -f " + f.getCanonicalPath());
        } else if (name.endsWith(".zip") || name.endsWith(".jar")) {
            unzip(new ZipFile(f), t);
            return;
        } else {
            return;
        }

        System.out.print(cmd.get(cmd.size() - 1));
        String[] exec = cmd.toArray(new String[cmd.size()]);
        Process p = null;
        try {
            Thread.sleep(1);
            p = Runtime.getRuntime().exec(exec);
            InputStream in = p.getInputStream();
            OutputStream out = p.getOutputStream();
            try {
                out.close();
                in.close();
            } catch (Exception e) {
                System.out.println("!!! " + e);
            } finally {
                System.out.println(" exec(): " + p.waitFor());
            }
        } finally {
            if (p != null) { p.destroy(); }
        }

    }

    public int compareTo(Map < Map < String, List < Integer > >, Number > t) {
        return 0;
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
        System.out.println("Unzipped " + zf.getName() + " to " + target);
    }
}
