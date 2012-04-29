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

import java.util.zip.*;
import java.io.*;
import java.util.*;



public class InnerClass {

  public static boolean isChildClass(String name) {
    return name.endsWith(".class") && name.indexOf('$') >= 0;
  }

  public static String ultimateParent(String child) {
    int x = child.indexOf('$');
    if (x >= 0) {
      return child.substring(0, x) + ".class";
    } else {
      return null;
    }
  }

  public static String immediateParent(String child) {
    int x = child.lastIndexOf('$');
    if (x >= 0) {
      return child.substring(0, x) + ".class";    
    } else {
      return null;
    }
  }
  
  
  public static void main(String[] args) throws Exception {
    File f = new File(args[0]);
    scan(f);
  }
  
  private static void scan(File file) throws Exception {
    if (file.isDirectory()) {
      for (File f : file.listFiles()) {
        scan(f);
      }
    } else if (file.isFile()) {
      String name = file.getName();      
      if (isChildClass(name)) {
        System.out.println(ultimateParent(name) + "]\t[" + immediateParent(name) + "]\t[" + name + "]");
      }
    }
  }

}