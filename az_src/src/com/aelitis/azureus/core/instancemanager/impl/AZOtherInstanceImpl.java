/*
 * Created on 20-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;


public class 
AZOtherInstanceImpl
	extends AZInstanceImpl
{
	protected static AZOtherInstanceImpl
	decode(
		InetAddress		internal_address,
		Map				map )
	{
		String	id			= new String((byte[])map.get( "id" ));
		String	int_ip		= new String((byte[])map.get( "iip" ));
		String	ext_ip		= new String((byte[])map.get( "eip" ));
		int		tcp			= ((Long)map.get("tp" )).intValue();
		int		udp			= ((Long)map.get("dp" )).intValue();
		
		Long	l_udp_other = (Long)map.get("dp2" );
		
		int		udp_other	= l_udp_other==null?udp:l_udp_other.intValue();
		
		byte[]	app_id_bytes = (byte[])map.get( "ai" );
		
		String app_id;
		
		if ( app_id_bytes == null ){
			
			app_id = SystemProperties.AZ_APP_ID + "_4.2.0.2";	// we dont know, but this is most likely
			
		}else{
			
			app_id = new String( app_id_bytes );
		}
		
		Map<String,Object>	props = (Map<String,Object>)map.get( "pr" );
		
		try{
			if ( !int_ip.equals("0.0.0.0")){
				
				internal_address = InetAddress.getByName( int_ip );
			}

			InetAddress	external_address = InetAddress.getByName( ext_ip );
			
				// ignore incompatible address mappings
			
			if ( internal_address instanceof Inet4Address == external_address instanceof Inet4Address ){
				
				return( new AZOtherInstanceImpl(id, app_id, internal_address, external_address, tcp, udp, udp_other, props ));
			}
			
			return( null );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( null );
	}
	
	private String					id;
	private String					app_id;
	private List					internal_addresses	= new ArrayList();
	private InetAddress				external_address;
	private int						tcp_port;
	private int						udp_port;
	private int						udp_non_data_port;
	private Map<String,Object>		props;
	
	private long	alive_time;


	protected
	AZOtherInstanceImpl(
		String					_id,
		String					_app_id,
		InetAddress				_internal_address,
		InetAddress				_external_address,
		int						_tcp_port,
		int						_udp_port,
		int						_udp_non_data_port,
		Map<String,Object>		_props )
	{
		id					= _id;
		app_id				= _app_id;
		
		internal_addresses.add( _internal_address );
		
		external_address	= _external_address;
		tcp_port			= _tcp_port;
		udp_port			= _udp_port;
		udp_non_data_port	= _udp_non_data_port;
	
		props				= _props;
		
		alive_time	= SystemTime.getCurrentTime();
	}
	
	protected boolean
	update(
		AZOtherInstanceImpl	new_inst )
	{		
		alive_time	= SystemTime.getCurrentTime();
		
		InetAddress	new_address = new_inst.getInternalAddress();
		
		boolean	same = true;
		
		if ( !internal_addresses.contains( new_address )){
			
			same	= false;
			
			List	new_addresses = new ArrayList( internal_addresses );
			
			new_addresses.add( 0, new_address );
			
			internal_addresses	= new_addresses;
		}
		
		same	 = 	same && 
					external_address.equals( new_inst.external_address ) &&
					tcp_port == new_inst.tcp_port  &&
					udp_port == new_inst.udp_port;
		
		
		external_address	= new_inst.external_address;
		tcp_port			= new_inst.tcp_port;
		udp_port			= new_inst.udp_port;
	
		return( !same );
	}
	
	public String
	getID()
	{
		return( id );
	}
	
	public String
	getApplicationID()
	{
		return( app_id );
	}
	
	public InetAddress
	getInternalAddress()
	{
		return((InetAddress)internal_addresses.get(0));
	}
	
	public List
	getInternalAddresses()
	{
		return( new ArrayList( internal_addresses ));
	}
	
	public InetAddress
	getExternalAddress()
	{
		return( external_address );
	}
	
	public int
	getTCPListenPort()
	{
		return( tcp_port );
	}
	
	public int
	getUDPListenPort()
	{
		return( udp_port );
	}
	
	public int 
	getUDPNonDataListenPort() 
	{
		return( udp_non_data_port );
	}
	
	public Map<String, Object> 
	getProperties() 
	{
		return( props );
	}
	
	protected long
	getAliveTime()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( now < alive_time ){
			
			alive_time	= now;
		}
		
		return( alive_time );
	}
}
