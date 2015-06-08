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

public class Names {


    public static String extractNameFromPath(String path) {
        int x = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return x >= 0 ? path.substring(x+1) : path;
    }

    public static String fixClassName(String s) {
        return s != null ? s.replace('/', '.').replace('$', '.') : s;
    }


    public static String stripGeneric(String name) {
        int x = name.indexOf('<');
        return x >= 0 ? name.substring(0,x) : name;
    }

    public static String fixTypeName(String typeName) {
        String s = fixClassName(typeName);
        if (s.indexOf('.') < 0) { return typeName; }
        s = removeOutterClassGeneric(s);

        StringBuilder buf = new StringBuilder(s);
        s = buf.reverse().toString();
        buf = new StringBuilder(typeName.length());
        boolean keepChars = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '.':
                    keepChars = false;
                    break;
                case ' ':
                case '<':
                case '>':
                case ',':
                case ']':
                case '[':
                    keepChars = true;
                    break;
            }
            if (keepChars) { buf.append(c); }
        }
        return buf.reverse().toString();
    }

    /*
    Sometimes we need to remove generic and strip to last dot
    in complex inner-class of generic outter FQN's.
    e.g., 'MyMap<T,V>.MyInner' becomes 'MyInner'.
    */
    public static String removeOutterClassGeneric(String s) {
        int y = s.indexOf(">.");
        while (y >= 0 && y+1 < s.length()) {
            int x = Names.findOpeningBracket(s, y-1);
            s = s.substring(0,x) + s.substring(y+1);
            y = s.indexOf(">.", x);
        }
        return s;
    }

    static int findOpeningBracket(String sig, int pos) {
        for (int i = pos; i >= 0; i--) {
            char c = sig.charAt(i);
            if (c == '>') {
                i = findOpeningBracket(sig, i-1);
            } else if (c == '<') {
                return i;
            }
        }
        throw new RuntimeException("Could not findOpeningBracket: [" + sig + "] / pos=" + pos);
    }

    public static int findClosingBracket(String sig, int pos) {
        for (int i = pos; i < sig.length(); i++) {
            char c = sig.charAt(i);
            if (c == '<') {
                i = findClosingBracket(sig, i+1);
            } else if (c == '>') {
                return i;
            }
        }
        throw new RuntimeException("Could not findClosingBracket: [" + sig + "] / pos=" + pos);
    }

}
