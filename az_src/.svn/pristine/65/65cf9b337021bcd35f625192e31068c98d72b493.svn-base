/*
 * File    : Main.java
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
import java.net.InetAddress;
import java.net.ServerSocket;

import org.gudy.azureus2.core3.util.Debug;

/**
 * @author Olivier Chalouhi
 *
 */
public class Main {
    
    private static String classToStart = "org.gudy.azureus2.ui.swt.updater.snippets.Started";
    
    public static void main(String args[]) {
        try {
        	ServerSocket server = new ServerSocket(6880, 50, InetAddress.getByName("127.0.0.1"));
          spawnStarted();
          server.close();
        } catch(Exception e) {
        	Debug.printStackTrace( e );
        }
    }
    
    public static void spawnStarted() throws Exception {
        String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$     
        String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
        String javaPath = System.getProperty("java.home")
          + System.getProperty("file.separator")
          + "bin"
          + System.getProperty("file.separator");
                 
        
        String exec = "\"" + javaPath + "java\" -classpath \"" + classPath
        + "\" " + classToStart;
        
        
        Logger.log("Main is about to execute : " + exec);    
                        
        File userDir = new File(userPath);
        String[] env = {"user.dir=" + userPath};
        Runtime.getRuntime().exec(exec);
    }
}
