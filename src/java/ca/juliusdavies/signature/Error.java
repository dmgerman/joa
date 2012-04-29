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

public class Error extends Artifact {

    private String msg;

    public Error(String msg) { this.msg = msg; }

    public String getPackageName() { return ""; }
    public String getClassName() { return ""; }
    public String toTightString() { return msg; }
    public int getMinLine() { return -1; }
    public int getMaxLine() { return -1; }
    public int getMethodCount() { return -1; }

}
