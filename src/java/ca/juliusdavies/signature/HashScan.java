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
import java.util.HashMap;

public class HashScan {

    public static void main(String[] args) throws IOException {
        String arg0 = args[0];
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, newArgs.length);

        HashMap<String, HashMap<String, String>> skipOutput = new HashMap<String, HashMap<String, String>>();
        FileInputStream fin = new FileInputStream(arg0);
        InputStreamReader isr = new InputStreamReader(fin, "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();

            // Skip some lines:
            if ("".equals(line)) { continue; }
            char c = Character.toUpperCase(line.charAt(0));
            char d = line.charAt(1);
            if (d == ';' && c == 'E' || c == 'N') { continue; }


            String[] toks = line.split(";");
            if (toks.length > 2) {
                String fqn = toks[0].trim();
                String hash = toks[1].trim();
                String path = toks[2].trim();

                HashMap<String, String> hashes = skipOutput.get(fqn);
                if (hashes == null) {
                    hashes = new HashMap<String, String>();
                    skipOutput.put(fqn, hashes);
                }
                hashes.put(hash, path);
            }
        }

        SimpleScan.skipOutput = skipOutput;
        SimpleScan.main(newArgs);
    }
}
