/*
 * File    : Logger.java
 * Created : 6 avr. 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.gudy.azureus2.ui.swt.updater.snippets;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author Olivier Chalouhi
 *
 */
public class Logger {
    public static void log(String value) {
    FileOutputStream fos = null;
    try {
      String userPath = System.getProperty("user.dir") + System.getProperty("file.separator");
      File logFile = new File(userPath + "snippet.log");
      fos = new FileOutputStream(logFile,true);
      fos.write((value+"\n").getBytes());
      fos.close();
    } catch(Exception e) {
      //Do nothing
    }
  }
}
