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

import java.io.File;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TreeSet;

public class DirScanner {
    private final static TreeSet<String> WE_LIKE_THESE = new TreeSet<String>();

    static {
        WE_LIKE_THESE.add(".gz");
        WE_LIKE_THESE.add(".bz2");
        WE_LIKE_THESE.add(".xz");
        WE_LIKE_THESE.add(".lzma");
        WE_LIKE_THESE.add(".tgz");
        WE_LIKE_THESE.add(".tar");
        WE_LIKE_THESE.add(".jar");
        WE_LIKE_THESE.add(".ear");
        WE_LIKE_THESE.add(".war");
        WE_LIKE_THESE.add(".zip");
        WE_LIKE_THESE.add(".class");
        WE_LIKE_THESE.add(".java");
    }

    public static void scan(File dir, TreeSet<String> list) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                scan(f, list);
            }
        } else if (dir.isFile() && dir.canRead() && dir.length() > 0) {
            String path = dir.getPath();
            int x = path.lastIndexOf('.');
            if (x >= 0) {
                String suffix = path.substring(x).toLowerCase(Locale.ENGLISH);
                if (WE_LIKE_THESE.contains(suffix)) {
                    list.add(path);
                }
            }
        }
    }

}
