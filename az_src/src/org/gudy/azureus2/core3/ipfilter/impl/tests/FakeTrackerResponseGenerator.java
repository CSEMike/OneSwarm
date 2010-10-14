/*
 * Created on 30 sept. 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * 
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.core3.ipfilter.impl.tests;

/**
 * @author Olivier Chalouhi
 *
 */
public class FakeTrackerResponseGenerator {
	
	//This will generate a fake announce with loads of bad IPs in it.
	public static void main(String args[]) {
		String baseRange = "195.68.236.";
		String basePeerId = "-AZ2104-0VR73lDzLejd";
		System.out.print("d8:intervali10e5:peersl");
		for(int i = 100 ; i < 200 ; i++) {
			String iStr = "" + i;
			int iStrLength  = iStr.length();
			String ip = baseRange + iStr;
			String peerId = basePeerId.substring(0,20-iStrLength) + iStr;
			System.out.print("d2:ip" + ip.length() + ":" + ip);
			System.out.print("7:peer id20:" + peerId);
			System.out.print("4:porti3003ee");
		}
		System.out.print("ee");		
	}
}
