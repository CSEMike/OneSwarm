/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht.router.impl;

/**
 * @author parg
 *
 */


import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.dht.router.*;


public class 
Test 
{
	public static void
	main(
		String[]	args )
	{
		randomTest();
	}
	
	protected static void
	simpleTest()
	{
		DHTRouter	router = DHTRouterFactory.create( 1, 1,1,  new byte[]{ 0 }, null, com.aelitis.azureus.core.dht.impl.Test.getLogger());
		
		router.setAdapter( 
			new DHTRouterAdapter()
			{
				public void requestAdd(DHTRouterContact contact) {
					// TODO Auto-generated method stub
					
				}
				public void requestLookup(byte[] id, String description) {
					// TODO Auto-generated method stub
					
				}
				public void requestPing(DHTRouterContact contact) {
					// TODO Auto-generated method stub
					
				}
			});
		
		byte[][]	node_ids ={ 
				{ toByte( "11111111" ) },		
				{ toByte( "01111111" ) },		
				{ toByte( "00101111" ) },		
				{ toByte( "00100111" ) },		
				//{ toByte( "00111111" ) },		
		};
		
		for (int i=0;i<node_ids.length;i++){
						
			router.contactKnown( node_ids[i], null );
		}

			// byte[]	node_id = new byte[]{ 1,1,1,1 }; //new SHA1Hasher().calculateHash( (""+i).getBytes());

		router.print();
	}
	
	protected static void
	randomTest()
	{
		DHTRouter	router = DHTRouterFactory.create( 20, 5, 5, getSHA1(), null, com.aelitis.azureus.core.dht.impl.Test.getLogger());
		
		router.setAdapter( 
				new DHTRouterAdapter()
				{
					public void requestAdd(DHTRouterContact contact) {
						// TODO Auto-generated method stub
						
					}
					public void requestLookup(byte[] id, String description) {
						// TODO Auto-generated method stub
						
					}
					public void requestPing(DHTRouterContact contact) {
						// TODO Auto-generated method stub
						
					}
				});
		
		for (int i=0;i<1000000;i++){
			
			byte[]	id = getSHA1();
			
			DHTRouterContact cont = router.contactKnown( id, null);
		}
		
		router.print();
	}
	
	protected static long next_sha1_seed = 0;
	
	protected static byte[]
	getSHA1()
	{
		return( new SHA1Simple().calculateHash( ( "" + ( next_sha1_seed++ )).getBytes()));
	}
	
	protected static byte
	toByte(
		String	str )
	{
		int	res = 0;
		
		for (int i=0;i<8;i++){
		
			if ( str.charAt(i) == '1' ){
				
				res += 1<<(7-i);
			}
		}
		
		return((byte)res);
	}
}
