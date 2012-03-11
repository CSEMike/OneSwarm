/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNATDevice;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;

public class 
NetworkAdminNATDeviceImpl 
	implements NetworkAdminNATDevice
{
	private UPnPPluginService		service;
	private InetAddress				external_address;
	private long					address_time;
	
	protected
	NetworkAdminNATDeviceImpl(
		UPnPPluginService		_service )
	{
		service	= _service;
	}
	
	public String
	getName()
	{
		return( service.getName());
	}
	
	public InetAddress
	getAddress()
	{
		try{
			
			return( InetAddress.getByName(service.getAddress()));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public int
	getPort()
	{
		return( service.getPort());
	}
	
	public InetAddress
	getExternalAddress()
	{
		long	now = SystemTime.getCurrentTime();
		
		if ( 	external_address != null &&
				now > address_time &&
				now - address_time < 60*1000 ){
			
			return( external_address );
		}
		
		try{		
			external_address = InetAddress.getByName(service.getExternalAddress());
			
			address_time = now;
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( external_address );
	}
	
	protected boolean
	sameAs(
		NetworkAdminNATDeviceImpl	other )
	{
		if ( 	!getAddress().equals( other.getAddress()) ||
				getPort() != other.getPort()){
			
			return( false );
		}
		
		InetAddress e1 = getExternalAddress();
		InetAddress e2 = other.getExternalAddress();
		
		if ( e1 == null && e2 == null ){
			
			return( true );
		}
		if ( e1 == null || e2 == null ){
			
			return( false );
		}
				
		return( e1.equals( e2 ));
	}
	
	public String
	getString()
	{
		String res = getName();
		
		res += ": address=" + service.getAddress() + ":" + service.getPort();
		
		InetAddress ext = getExternalAddress();
		
		if ( ext == null ){
			
			res += ", no public address available";
		}else{
			
			res += ", public address=" + ext.getHostAddress();
		}
		
		return( res );
	}
}
