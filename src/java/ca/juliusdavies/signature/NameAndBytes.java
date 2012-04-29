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
import ca.phonelist.util.NullSafe;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class NameAndBytes implements Serializable, Comparable<NameAndBytes> {
    public static final boolean BINARY = false;
    public static final boolean SOURCE = true;

    public final boolean isSrc;
    public final String name;
    public final String path;
    public final long lastModified;
    public final byte[] bytes;
    public final String parentSHA1;
    public transient ClassReader cr; // just a holder to help with inner-classes

    public NameAndBytes(boolean isSrc, File f) throws IOException {
        this(
                isSrc, f.getName(), f.getPath(), f.lastModified(), Bytes.fileToBytes(f), null
        );
    }

    public NameAndBytes(
            boolean isSrc, String name, String path, long lastModified, byte[] bytes, String parentSHA1
    ) {
        this.isSrc = isSrc;
        this.name = name;
        this.path = path;
        this.lastModified = lastModified;
        this.bytes = bytes;
        this.parentSHA1 = parentSHA1;
    }

    public int compareTo(NameAndBytes other) {
        return NullSafe.compareIgnoreCase(path, other.path);
    }

    public String toString() { return path; }

}
