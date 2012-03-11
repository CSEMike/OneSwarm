/*
 * File    : Started.java
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

import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * @author Olivier Chalouhi
 *
 */
public class Started {
    
    public static void main(String args[]) {
    	  //We wait untill the creating process has ended and released the
        // socket bind.
        boolean ok = false;
        try {
            while(!ok) {
              try{
                ServerSocket server = new ServerSocket(6880, 50, InetAddress.getByName("127.0.0.1"));
                ok = true;
                server.close();
              } catch(Exception e) {
                Logger.log("Exception while trying to bind on port 6880 : " + e);
                Thread.sleep(1000);
              }
            }
        } catch(Exception e) {
         Logger.log("Exception while running Started : " +e);   
        }
    }
}
