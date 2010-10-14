/*
 * Created on 04-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.proxy.AEProxyFactory;

public class 
AddressUtils 
{
	public static final byte LAN_LOCAL_MAYBE	= 0;
	public static final byte LAN_LOCAL_YES		= 1;
	public static final byte LAN_LOCAL_NO		= 2;
	
	private static AZInstanceManager	instance_manager;
	
	public static URL
	adjustURL(
		URL		url )
	{
		url = AEProxyFactory.getAddressMapper().internalise( url );

		return( url );
	}
	
	public static InetSocketAddress
	adjustDHTAddress(
		InetSocketAddress	address,
		boolean				ext_to_lan )
	{
		if ( instance_manager == null ){
			
			try{
				instance_manager = AzureusCoreFactory.getSingleton().getInstanceManager();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( instance_manager == null || !instance_manager.isInitialized()){
			
			return( address );
		}
		
		InetSocketAddress	adjusted_address;
		
		if ( ext_to_lan ){
			
			adjusted_address	= instance_manager.getLANAddress( address, AZInstanceManager.AT_UDP_NON_DATA );
			
		}else{

			adjusted_address	= instance_manager.getExternalAddress( address, AZInstanceManager.AT_UDP_NON_DATA );
		}
		
		if ( adjusted_address == null ){
			
			adjusted_address	= address;
			
		}else{
		
			// System.out.println( "adj: " + address + "/" + ext_to_lan + " -> " + adjusted_address );
		}
		
		return( adjusted_address );
	}
	
	public static byte
	isLANLocalAddress(
		InetSocketAddress	socket_address )
	{
		InetAddress address = socket_address.getAddress();
		
		return( isLANLocalAddress( address ));
	}
	
	public static byte
	isLANLocalAddress(
		InetAddress	address )

	{
			// if someone passes us an unresolved address then handle sensibly
		
		if ( address == null ){
			
			return( LAN_LOCAL_NO );
		}
		
		if ( instance_manager == null ){
			
			if ( AzureusCoreFactory.isCoreAvailable()){
				
				try{
					instance_manager = AzureusCoreFactory.getSingleton().getInstanceManager();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		if ( instance_manager == null || !instance_manager.isInitialized()){
			
			return( LAN_LOCAL_MAYBE );
		}
		
		return( instance_manager.isLANAddress( address )? LAN_LOCAL_YES:LAN_LOCAL_NO);
	}
	
	
	public static byte 
	isLANLocalAddress( 
		String address ) 
	{
		byte is_lan_local = LAN_LOCAL_MAYBE;
		
		try {
			is_lan_local = isLANLocalAddress( HostNameToIPResolver.syncResolve( address ));
			
		}catch( Throwable t ){
			
			t.printStackTrace();  
		}
		
		return is_lan_local;
	}
	
}
