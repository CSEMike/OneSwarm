/*
 * Created on 15-Jan-2005
 * Created by Paul Gardner
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

package com.aelitis.azureus.core.dht.control.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.gudy.azureus2.core3.util.ByteFormatter;


/**
 * @author parg
 *
 */

public class 
Test 
{
	public static void
	main(
		String[]	args )
	{
		//   :AFC97EA1 :lookup complete for B6589FC6 
		//    :AFC97EA1 :    queried = {91032AD7 ,AC3478D6 ,ACCA5B51 ,B6692EA5 ,ACF10F2C ,F66B7DCD ,B3F0C7F6 ,A8A2B30F ,972A67C4 ,AE7B4B79 ,887309D0 ,8383DA94 ,9E6A55B6 ,902BA3CD ,AFC97EA1 ,BD307A3E ,B1D57811 ,8665243E ,9EE0DF7C ,B6589FC6 ,BC33EA4E }
		//    :AFC97EA1 :    to query = {F6E1126C ,F22FE10D ,F0483F25 ,F1F836CB ,F1ABD670 ,FE5DBBCE ,FA35E192 ,FBDD85A9 ,FBEA31C7 ,EAEF5296 ,D435A6CD ,D051BF1D ,DA4B9237 ,C1DFD96E ,CB4E5208 ,356A192B ,17BA0791 ,1574BDDB ,1B645389 ,0716D970 ,0ADE7C2C ,77DE68DA ,7B52009B ,4D134BC0 }

		// final byte[]	target 	= { (byte)0xB6, (byte)0x58, (byte)0x9f, (byte)0xc6 };

		//  :9A79BE61:lookup complete for 31017A72
		//    :9A79BE61:    queried = {77DE68DA,1B645389,31BD9B9F,356A192B,310B86E0}
		//    :9A79BE61:    to query = {6FB84AED,B6589FC6,812ED456,DA4B9237}
		//    :9A79BE61:    ok = {77DE68DA,1B645389,356A192B,31BD9B9F,310B86E0}
		
		final byte[]	target 	= { (byte)0x35, (byte)0x6a, (byte)0x19, (byte)0x28 };
		final byte[]	t1 		= { (byte)0x76, (byte)0x1f, (byte)0x22, (byte)0xb2 };
		final byte[]	t2 		= { (byte)0x47, (byte)0x2b, (byte)0x07, (byte)0xb9 };
				   		
		byte[]	d1 = DHTControlImpl.computeDistance2( t1, target );
		byte[]	d2 = DHTControlImpl.computeDistance2( t2, target );
		
		System.out.println( "d1 = " + ByteFormatter.nicePrint( d1 ));
		System.out.println( "d2 = " + ByteFormatter.nicePrint( d2 ));
		
		System.out.println( "comp1 = " + DHTControlImpl.compareDistances2( d1, d2 ));
		System.out.println( "comp2 = " + DHTControlImpl.computeAndCompareDistances2( t1, t2, target ));
		
		final Set			set = 
			new TreeSet(
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{						
						byte[] d1 = DHTControlImpl.computeDistance2( (byte[])o1, target );
						byte[] d2 = DHTControlImpl.computeDistance2( (byte[])o2, target );
						
						System.out.println( "dist:" + ByteFormatter.nicePrint((byte[])o1) + " -> " + ByteFormatter.nicePrint(d1));
						System.out.println( "dist:" + ByteFormatter.nicePrint((byte[])o2) + " -> " + ByteFormatter.nicePrint(d2));
						return( DHTControlImpl.compareDistances2( d1, d2 ));
					}
				});
		
		set.add( t1 );
		set.add( t2 );
		
		//set.add( new byte[]{ (byte)0xF0, (byte)0x48 ,(byte)0x3F, (byte)0x25 });
		//set.add( new byte[]{ (byte)0xF1, (byte)0xF8, (byte)0x36, (byte)0xCB });
		//set.add( new byte[]{ (byte)0xF2, (byte)0x2F, (byte)0xE1, (byte)0x0D });
	
		
		Iterator it = set.iterator();
		
		while( it.hasNext()){
			
			byte[]	val = (byte[])it.next();
			
			System.out.println( ByteFormatter.nicePrint( val ));
		}
	}
}
