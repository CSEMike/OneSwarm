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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.proxy.AEProxyFactory;

public class 
AddressUtils 
{
	public static final byte LAN_LOCAL_MAYBE	= 0;
	public static final byte LAN_LOCAL_YES		= 1;
	public static final byte LAN_LOCAL_NO		= 2;
	
	private static AZInstanceManager	instance_manager;
	
	private static Map	host_map = null;
	
	public static URL
	adjustURL(
		URL		url )
	{
		url = AEProxyFactory.getAddressMapper().internalise( url );

		if ( host_map != null ){
			
			String	rewrite = (String)host_map.get( url.getHost());
			
			if ( rewrite != null ){
		
				String str = url.toExternalForm();
				
				try{
					int pos = str.indexOf( "//" ) + 2;
					
					int pos2 = str.indexOf( "/", pos );
					
					String	host_bit = str.substring( pos, pos2 );
					
					int	pos3 = host_bit.indexOf(':');
					
					String	port_bit;
					
					if ( pos3 == -1 ){
						
						port_bit = "";
						
					}else{
						
						port_bit = host_bit.substring(pos3);
					}
					
					String new_str = str.substring(0,pos) + rewrite + port_bit + str.substring( pos2 );
										
					url = new URL( new_str );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
		
		return( url );
	}
	
	public static synchronized void
	addHostRedirect(
		String	from_host,
		String	to_host )
	{
		System.out.println( "AddressUtils::addHostRedirect - " + from_host + " -> " + to_host );
		
		Map	new_map;
		
		if ( host_map == null ){
			
			new_map = new HashMap();
		}else{
			
			new_map = new HashMap( host_map );
		}
		
		new_map.put( from_host, to_host );
		
		host_map = new_map;
	}
	
	public static InetSocketAddress
	adjustTCPAddress(
		InetSocketAddress	address,
		boolean				ext_to_lan )
	{
		return( adjustAddress( address, ext_to_lan, AZInstanceManager.AT_TCP ));
	}
	
	public static InetSocketAddress
	adjustUDPAddress(
		InetSocketAddress	address,
		boolean				ext_to_lan )
	{
		return( adjustAddress( address, ext_to_lan, AZInstanceManager.AT_UDP ));
	}
	
	public static InetSocketAddress
	adjustDHTAddress(
		InetSocketAddress	address,
		boolean				ext_to_lan )
	{
		return( adjustAddress( address, ext_to_lan, AZInstanceManager.AT_UDP_NON_DATA ));
	}
	
	private static InetSocketAddress
	adjustAddress(
		InetSocketAddress	address,
		boolean				ext_to_lan,
		int					port_type )
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
			
			adjusted_address	= instance_manager.getLANAddress( address, port_type );
			
		}else{

			adjusted_address	= instance_manager.getExternalAddress( address, port_type );
		}
		
		if ( adjusted_address == null ){
			
			adjusted_address	= address;
			
		}else{
		
			// System.out.println( "adj: " + address + "/" + ext_to_lan + " -> " + adjusted_address );
		}
		
		return( adjusted_address );
	}
	
	public static List
	getLANAddresses(
		String		address )
	{
		List	result = new ArrayList();
		
		result.add( address );
		
		try{
			InetAddress ad = InetAddress.getByName( address );

			if ( isLANLocalAddress( address ) != LAN_LOCAL_NO ){

				if ( instance_manager == null ){
					
					try{
						instance_manager = AzureusCoreFactory.getSingleton().getInstanceManager();
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
				
				if ( instance_manager == null || !instance_manager.isInitialized()){
					
					return( result );
				}
			
				AZInstance[] instances = instance_manager.getOtherInstances();
				
				for (int i=0;i<instances.length;i++){
					
					AZInstance instance = instances[i];
					
					List addresses = instance.getInternalAddresses();
					
					if ( addresses.contains( ad )){
						
						for ( int j=0; j<addresses.size();j++){
							
							InetAddress ia = (InetAddress)addresses.get(j);
							
							String str = ia.getHostAddress();
							
							if ( !result.contains( str )){
								
								result.add( str );
							}
						}
					}
				}
			}
		}catch( Throwable e ){
			
		}
		
		return( result );
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
			
		}catch( UnknownHostException e ){
			
		}catch( Throwable t ){
			
			t.printStackTrace();  
		}
		
		return is_lan_local;
	}
	
	/**
	 * checks if the provided address is a global-scope ipv6 unicast address
	 */
	public static boolean isGlobalAddressV6(InetAddress addr) {
		return addr instanceof Inet6Address && !addr.isAnyLocalAddress() && !addr.isLinkLocalAddress() && !addr.isLoopbackAddress() && !addr.isMulticastAddress() && !addr.isSiteLocalAddress() && !((Inet6Address)addr).isIPv4CompatibleAddress();
	}
	
	public static boolean isTeredo(InetAddress addr)
	{
		if(!(addr instanceof Inet6Address))
			return false;
		byte[] bytes = addr.getAddress();
		// check for the 2001:0000::/32 prefix, i.e. teredo
		return bytes[0] == 0x20 && bytes[1] == 0x01 && bytes[2] == 0x00 && bytes[3] == 0x00;
	}
	
	public static boolean is6to4(InetAddress addr)
	{
		if(!(addr instanceof Inet6Address))
			return false;
		byte[] bytes = addr.getAddress();
		// check for the 2002::/16 prefix, i.e. 6to4
		return bytes[0] == 0x20 && bytes[1] == 0x02;
	}
	
	/**
	 * picks 1 global-scoped address out of a list based on the heuristic
	 * "true" ipv6/tunnel broker > 6to4 > teredo
	 * 
	 * @return null if no proper v6 address is found, best one otherwise
	 */
	public static InetAddress pickBestGlobalV6Address(List<InetAddress> addrs)
	{
		InetAddress bestPick = null;
		int currentRanking = 0;
		for(InetAddress addr : addrs)
		{
			if(!isGlobalAddressV6(addr))
				continue;
			int ranking = 3;
			if(isTeredo(addr))
				ranking = 1;
			else if(is6to4(addr))
				ranking = 2;
			
			if(ranking > currentRanking)
			{
				bestPick = addr;
				currentRanking = ranking;
			}
		}
		
		return bestPick;
	}
	
}
