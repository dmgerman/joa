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

import javax.tools.*;
import java.io.*;
import java.util.*;
import java.net.*;


public class InMemoryFileObject extends SimpleJavaFileObject {
  private byte[] bytes;
  private long lastModified;
  private String path;
  private String name;

  public InMemoryFileObject(String name, String path, long date, byte[] bytes) throws URISyntaxException {

    super(new URI("file:///dev/null/" +       path.replaceAll(" ", "%20")  + "/" + name), JavaFileObject.Kind.SOURCE);

    try {
        // UTF-8 is probably not a good choice here, either.  Oh well.
        String s = new String(bytes, "UTF-8");

        if (s.contains("Enumeration enum")) {
            // We're screwed, since enum is a reserved word, but this might help:
            s = s.replace("enum", "en");
            bytes = s.getBytes("UTF-8");
        }
    } catch (UnsupportedEncodingException uee) {

    }

    this.bytes = bytes;
    this.lastModified = date;
    this.name = name;
    this.path = path;
  }

  public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
    return new String(bytes, 0, bytes.length, "UTF-8");
  }

  public long getLastModified() { return lastModified; }

  public String getName() { return name; }

  public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
    if (JavaFileObject.Kind.SOURCE.equals(kind)) {
        String s = name.substring(0, Math.min(name.length(), simpleName.length()));
        // System.out.println(name + "] vs. [" + simpleName + "] " +  simpleName.equalsIgnoreCase(s) );
        return name.equalsIgnoreCase(s);
    } else {
        throw new UnsupportedOperationException("don't support that kind: " + kind);
    }
  }

  public InputStream openInputStream() throws IOException { return new ByteArrayInputStream(bytes); }

  public Reader openReader() throws IOException { return new InputStreamReader(openInputStream(), "UTF-8"); }

  public String toString() { return name; }


}
