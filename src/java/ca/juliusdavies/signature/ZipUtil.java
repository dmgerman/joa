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

import ca.phonelist.util.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    // SHA1 + "/" + size.  To avoid duplicate processing.
    private final static HashSet<String> PROCESSED_ZIPS = new HashSet<String>();

    public static ScanConfig scanConfig;
    public static SimpleScan.ArgScanner argScanner;

    /**
     * An interface that allows us to re-read a ZipInputStream as many times as we want.
     */
    private static interface Zipper {
        public ZipInputStream getFreshZipStream();
        public void close();
        public String getSHA1();
    }

    private static void extractClassesRecursive(
            final String zipPath, final String target, final Zipper zipper, final ArrayList<Artifact> list
    ) throws IOException {
        ZipEntry ze;
        ZipInputStream zin;

        // 1st pass... look for archives inside the archive
        zin = zipper.getFreshZipStream();
        if (zin == null) { return; }

        while ((ze = zin.getNextEntry()) != null) {
            if (ze.isDirectory()) { continue; }
            final String path = ze.getName();
            final String fullPath = zipPath + "!/" + path;
            if (target != null && !target.startsWith(fullPath)) {
                continue;
            }

            String PATH = path.toUpperCase(Locale.ENGLISH);
            if (PATH.endsWith(".ZIP") || PATH.endsWith(".WAR") || PATH.endsWith(".EAR") || PATH.endsWith(".JAR")) {

                final NameAndBytes nab = extract(zipper.getSHA1(), zipPath, zin, ze);
                Zipper recursiveZipper = new Zipper() {
                    private String sha1;
                    public ZipInputStream getFreshZipStream() {
                        if (this.sha1 == null) {
                            this.sha1 = Hash.sha1(nab.bytes);

                            if (SimpleScan.hashOutput) {
                                Src a = new Src(nab, scanConfig, zipper.getSHA1());
                                StringBuilder buf = new StringBuilder(256);
                                buf.append("F;");
                                buf.append(a.getRelativePath());
                                buf.append(';');
                                buf.append(a.getBaseName());
                                buf.append(';');
                                buf.append(a.getSuffix());
                                buf.append(';');
                                buf.append(a.getZipLevel());
                                buf.append(';');
                                buf.append(a.getParentFileSHA1());
                                buf.append(';');
                                buf.append(sha1);
                                buf.append(";\\N;\\N;\\N;\\N");
                                if (!SimpleScan.buildQueryOutput) {
                                    System.out.println(buf.toString());
                                }

                                String key = sha1 + "/" + a.getBaseName() + "/" + nab.bytes.length;
                                if (PROCESSED_ZIPS.contains(key)) {
                                    if (!SimpleScan.buildQueryOutput) {
                                        System.out.println("I;Skipping [" + nab.path + "]: identical file already processed.");
                                    }
                                    // Abort processing!
                                    close();
                                    return null;
                                } else {
                                    // Disabled for now.
                                    // PROCESSED_ZIPS.add(key);
                                }
                            }
                        }

                        ByteArrayInputStream bin = new ByteArrayInputStream(nab.bytes);
                        return new ZipInputStream(bin);
                    }
                    public String getSHA1() { return sha1; }
                    public void close() {}
                };
                extractClassesRecursive(nab.path, target, recursiveZipper, list);
            }
        }

        extractClasses(zipPath, target, zipper, list);
    }

    private static void extractClasses(
            final String zipPath, final String target, final Zipper zipper, final ArrayList<Artifact> list
    ) throws IOException {
        TreeMap<String, NameAndBytes> pathToNab = new TreeMap<String, NameAndBytes>();
        TreeMap<NameAndBytes, List<NameAndBytes>> innerClasses = new TreeMap<NameAndBytes, List<NameAndBytes>>();
        ZipEntry ze;
        ZipInputStream zin;

        // 1st pass... get all the parent classes done.
        zin = zipper.getFreshZipStream();
        if (zin == null) { return; }
        while ((ze = zin.getNextEntry()) != null) {
            if (ze.isDirectory()) { continue; }
            final String path = ze.getName();
            final String PATH = path.toUpperCase(Locale.ENGLISH);
            final String fullPath = zipPath + "!/" + path;
            if (target != null && !target.startsWith(fullPath)) {
                continue;
            }

            if (PATH.endsWith("PACKAGE-INFO.JAVA") || PATH.endsWith("PACKAGE-INFO.CLASS")) {
                continue;
            }

            String S = PATH;
            if (S.endsWith(".TAR") || S.endsWith(".TAR.BZ2") || S.endsWith(".TAR.GZ")
                || S.endsWith(".TAR.LZMA") || S.endsWith(".TAR.XZ") || S.endsWith(".TGZ")
            ) {

                if (!SimpleScan.recursiveZip) {
                    continue;
                }

                String zipName = zipPath;
                int x = zipPath.lastIndexOf('/');
                if (x >= 0) {
                    zipName = zipPath.substring(x+1);
                }

                Src a = new Src(fullPath);
                String tarPath = SimpleScan.TAR_PATH + zipper.getSHA1() + "-lvl" + (a.getZipLevel()-1);
                File f = new File(tarPath + "/" + zipName + "/" + path);
                f.getParentFile().mkdirs();
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(f);
                    Streams.copy(zin, fout);
                    fout = null;
                } finally {
                    Util.close(fout);
                }


                TreeSet<String> entries = argScanner.tars.get(tarPath);
                if (entries == null) {
                    entries = new TreeSet<String>();
                    argScanner.tars.put(tarPath, entries);
                }
                entries.add(f.getPath());
            }

            // Must be parent class:
            else if (PATH.endsWith(".JAVA") || PATH.endsWith(".CLASS") && !InnerClass.isChildClass(path)) {

                // Extract can fail (IOException).
                NameAndBytes nab = extract(zipper.getSHA1(), zipPath, zin, ze);
                if (nab != null) {
                    innerClasses.put(nab, new ArrayList<NameAndBytes>());
                    if (pathToNab.put(path, nab) != null) {
                        System.err.println("WARN: " + zipPath + "/!/" + path + " already scanned!");
                    }
                }
            }
        }

        // 2nd pass... inner-classes.
        zin = zipper.getFreshZipStream();
        while ((ze = zin.getNextEntry()) != null) {
            final String path = ze.getName();

            if (InnerClass.isChildClass(path)) {
                // It's an inner-class, and we know its parent.
                final String parent = InnerClass.ultimateParent(path);
                final String fullPath = zipPath + "!/" + parent;
                if (target != null && !target.startsWith(fullPath)) {
                    continue;
                }


                String innerName = path.substring(0, path.length()-6);
                int x = innerName.lastIndexOf('$');
                if (x >= 0) {
                    innerName = innerName.substring(x + 1);
                }
                if (Numbers.isLong(innerName)) {
                    // We skip anonymous inner-classes.
                    continue;
                }

                // Associate it with its parent.
                NameAndBytes parentNab = pathToNab.get(parent);
                if (parentNab != null) {
                    List<NameAndBytes> childClasses = innerClasses.get(parentNab);
                    if (childClasses != null) {
                        // Extract can fail (IOException).
                        NameAndBytes nab = extract(zipper.getSHA1(), zipPath, zin, ze);
                        if (nab != null) {
                            childClasses.add(nab);
                        }
                    }
                }
            }
        }

        for (Map.Entry<NameAndBytes, List<NameAndBytes>> entry : innerClasses.entrySet()) {
            NameAndBytes parent = entry.getKey();
            List<NameAndBytes> inners = entry.getValue();
            if (parent.isSrc) {
                Src src = new Src(parent, scanConfig, parent.parentSHA1);
                for (Src s : src.scan()) {
                    if (SimpleScan.sortOutput) {
                        list.add(s);
                    } else {
                        SimpleScan.print(s);
                    }
                }
            } else {
                Bin bin = new Bin(parent, inners, scanConfig, parent.parentSHA1);
                if (SimpleScan.sortOutput) {
                    list.add(bin);
                } else {
                    SimpleScan.print(bin);
                }
            }
        }
    }

    private static NameAndBytes extract(String parentSHA1, String zipPath, ZipInputStream zin, ZipEntry ze) {
        String path = ze.getName();
        String fullPath = zipPath + "!/" + path;
        try {
            String name = Names.extractNameFromPath(path);
            byte[] bytes = Bytes.streamToBytes(zin, false);
            boolean isSrc = name.endsWith(".java");
            return new NameAndBytes(
                    isSrc, name, fullPath, ze.getTime(), bytes, parentSHA1
            );
        } catch (IOException ioe) {
            System.err.println("E;" + fullPath + " " + ioe);
            return null;
        }
    }

    public static void scan(
            final File zipFile, boolean recursive, ScanConfig scanConfig, ArrayList<Artifact> list
    ) throws IOException {
        scan(zipFile, recursive, scanConfig, list, null);
    }

    private static void scan(
        final File zipFile, boolean recursive, ScanConfig scanConfig, ArrayList<Artifact> list, String target
    ) throws IOException {
        ZipUtil.scanConfig = scanConfig;
        Zipper myZipper = new Zipper() {
            private FileInputStream fin;
            private BufferedInputStream bin;
            private ZipInputStream zin;
            private String sha1;

            public ZipInputStream getFreshZipStream() {
                Util.close(zin, bin, fin);
                try {
                    if (this.sha1 == null) {
                        fin = new FileInputStream(zipFile);
                        this.sha1 = Hash.sha1(fin);
                        fin.close();

                        if (SimpleScan.hashOutput) {
                            Src a = new Src(zipFile);
                            String parentSHA1 = a.getParentFileSHA1();
                            StringBuilder buf = new StringBuilder(256);
                            buf.append("F;");
                            buf.append(a.getRelativePath());
                            buf.append(';');
                            buf.append(a.getBaseName());
                            buf.append(';');
                            buf.append(a.getSuffix());
                            buf.append(';');
                            buf.append(a.getZipLevel());
                            buf.append(';');
                            buf.append(parentSHA1 != null ? parentSHA1 : "\\N");
                            buf.append(';');
                            buf.append(sha1);
                            buf.append(";\\N;\\N;\\N;\\N");

                            if (!SimpleScan.buildQueryOutput) {
                                System.out.println(buf.toString());
                            }

                            long zipSize = zipFile.length();
                            String key = sha1 + "/" + a.getBaseName() + "/" + zipSize;
                            if (PROCESSED_ZIPS.contains(key)) {
                                if (!SimpleScan.buildQueryOutput) {
                                    System.out.println("I;Skipping [" + zipFile.getPath() + "]: identical file already processed.");
                                }
                                // Abort processing!
                                close();
                                return null;
                            } else {
                                // Disabled for now.
                                // PROCESSED_ZIPS.add(key);
                            }
                        }
                    }

                    fin = new FileInputStream(zipFile);
                    bin = new BufferedInputStream(fin);
                    zin = new ZipInputStream(bin);
                    return zin;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public String getSHA1() { return sha1; }
            public void close() { Util.close(zin, bin, fin); }
        };

        try {
            if (recursive) {
                ZipUtil.extractClassesRecursive(zipFile.getPath(), target, myZipper, list);
            } else {
                ZipUtil.extractClasses(zipFile.getPath(), target, myZipper, list);
            }
        } finally {
            myZipper.close();
        }
    }

    public static Artifact extractTarget(
            final String path, final String className, ScanConfig scanConfig
    ) {
        int x = path.indexOf('!');
        int y = path.lastIndexOf('!');

        String zipPath = path.substring(0, x);
        File zipFile = new File(zipPath);
        try {
            ArrayList<Artifact> results = new ArrayList<Artifact>();
            scan(zipFile, x != y, scanConfig, results, path);

            // TODO: This is stupid, needs work:
            return results.get(0);

        } catch (IOException ioe) {
            throw new RuntimeException("failed: " + ioe, ioe);
        }
    }

}