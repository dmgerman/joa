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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class DebFilter {

    public static void main(String[] args) throws IOException {

        FileInputStream fin = new FileInputStream(args[0]);
        InputStreamReader isr = new InputStreamReader(fin, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        int w, x, y, z;
        String prefix, suffix;
        while ((line = br.readLine()) != null) {

            x = line.indexOf(".deb/");
            if (x < 0) { System.out.println("INVALID: [" + line + "]"); continue; }
            prefix = line.substring(0,x);

            x = line.indexOf("data.tar.gz/");
            if (x < 0) {
                x = line.indexOf("data.tar.bz2/");
                if (x < 0) {
                    x = line.indexOf("data.tar.xz/");
                    if (x < 0) { System.out.println("INVALID: [" + line + "]"); continue; }
                }
            }

            suffix = line.substring(x+"data.tar.gz/".length());
            x = suffix.lastIndexOf('/');
            if (x >= 0) {
                suffix = suffix.substring(x+1);
            }

            w = prefix.lastIndexOf('/');
            if (x >= 0) {
                w = prefix.indexOf('_', w+1);
            }

            if (w < 0) {
                System.out.println("INVALID: [" + line + "]"); continue;
            }

            x = prefix.indexOf("+dfsg", w+1);
            y = prefix.indexOf(".dfsg", w+1);
            z = prefix.indexOf('-', w+1);

            int max = Math.max(x, Math.max(y, z));
            if (x < 0) { x = max; }
            if (y < 0) { y = max; }
            if (z < 0) { z = max; }
            x = Math.min(x, Math.min(y, z));

            if (x >= 0) {
                String version = prefix.substring(w+1, x) + ".jar";

                version = version.replace("~alpha", "-alpha-");
                version = version.replace("~beta", "-beta-");
                version = version.replace("+ds1.jar", ".jar");
                version = version.replace("+dak1.jar", ".jar");
                version = version.replace("~RC", "-RC");
                suffix = suffix.replace("-OOo31", "");

                suffix = removeDFSG(suffix, version);
                version = removeDFSG(version, suffix);


                if (suffix.contains(version)) {
                    System.out.println(line);
                }
            }

        }

    }

    private static String removeDFSG(String s, String target) {
        int y = s.indexOf("dfsg");
        if (y >= 0) {
            System.out.print("Fixing: [" + s);

            // Bug in debian package name:
            // [libhtmlparser-1.6.20060610.dfsg0.jar]
            if (s.charAt(y+4) == '0') {
                s = s.substring(0, y-1) + s.substring(y+5);
            } else {
                s = s.substring(0, y-1) + s.substring(y+4);
            }
            System.out.println("] to [" + s + "] target=[" + target + "]");
        }
        return s;
    }

}
