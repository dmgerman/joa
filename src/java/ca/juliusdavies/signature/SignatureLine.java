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

import java.util.Comparator;

public class SignatureLine implements Comparable<SignatureLine> {
    public static final Comparator<SignatureLine> BY_LINE = new Comparator<SignatureLine>() {
        public int compare(SignatureLine s1, SignatureLine s2) {
            int c = NullSafe.compareIgnoreCase(s1.line, s2.line);
            if (c == 0) {
                // fall-back to case-sensitive
                c = NullSafe.compare(s1.line, s2.line);
            }
            return c;
        }
    };

    public static final Comparator<SignatureLine> BY_NAME = new Comparator<SignatureLine>() {
        public int compare(SignatureLine s1, SignatureLine s2) {
            int c = NullSafe.compareIgnoreCase(s1.name, s2.name);
            if (c == 0) {
                // fall-back to case-sensitive
                c = NullSafe.compare(s1.name, s2.name);
            }
            return c;
        }
    };

    public final String name;
    public final String line;

    public SignatureLine(String name, String line) {
        this.name = name;
        this.line = line;
    }

    public int compareTo(SignatureLine other) { return BY_NAME.compare(this, other); }

}
