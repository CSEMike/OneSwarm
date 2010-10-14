/*
 * Created on Apr 11, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 * 
 */
package org.gudy.azureus2.core3.util.test;

import org.gudy.azureus2.core3.util.Debug;


public class SystemClockSpeedup {
	public static void main(String[] args) {
		for (int i=0; i < 20; i++) {
      new tester().start();
		}
	}
    
   
    
  public static class tester extends Thread {
    public void run() {
        try {
            int count = 0;
            while (true) {
                System.currentTimeMillis();
                count++;
                if (count == 30000) {
                    count = 0;
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
        	Debug.printStackTrace( e );
        }
    }
  }

    
    
    
    
}
