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

import ca.phonelist.util.Strings;

import java.io.*;
import java.util.Locale;

/**
 * To assemble list of Debian java packages:
 *
 * grep -i java list | grep -iv 'virtual package\|\-dev' > java-list
 *
 *
 *
 */
public class DebDownloader {

    private final static String ROOT_URL = "http://mirror.peer1.net/debian/";

    public static void main(String[] args) throws IOException {

        File filenames = new File(args[1]);
        if (!filenames.exists() || !filenames.canRead() || !filenames.isFile()) {
            System.err.println(filenames.getPath() + " is not a readable file.");
            System.exit(1);
        }

        FileInputStream fin = new FileInputStream(args[0]);
        InputStreamReader isr = new InputStreamReader(fin, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if ("".equals(line)) { continue; }

            String[] toks = line.split("\\s+");
            String name = toks[0];

            String debianPath = Unpacker.grep(name, filenames).trim();

            if (!"".equals(debianPath)) {
                // Each line starts with 'Filename: '
                debianPath = ROOT_URL + debianPath.substring(10);
                System.out.println(debianPath);
            }
        }
    }
}
