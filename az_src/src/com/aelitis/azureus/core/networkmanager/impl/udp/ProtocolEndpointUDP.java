/*
 * Created on 21 Jun 2006
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

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.AddressUtils;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpointHandler;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpointFactory;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.Transport.ConnectListener;

public class 
ProtocolEndpointUDP 
	implements ProtocolEndpoint
{
	public static void
	register()
	{
		ProtocolEndpointFactory.registerHandler(
			new ProtocolEndpointHandler()
			{
				public int
				getType()
				{
					return( ProtocolEndpoint.PROTOCOL_UDP );
				}
				
				public ProtocolEndpoint
				create(
					InetSocketAddress		address )
				{
					return( new ProtocolEndpointUDP( address ));
				}
				
				public ProtocolEndpoint
				create(
					ConnectionEndpoint		connection_endpoint,
					InetSocketAddress		address )
				{
					return( new ProtocolEndpointUDP( connection_endpoint, address ));
				}
			});
	}
	
	private ConnectionEndpoint		ce;
	private InetSocketAddress		address;
	
	private
	ProtocolEndpointUDP(
		ConnectionEndpoint		_ce,
		InetSocketAddress		_address )
	{
		ce		= _ce;
		address	= _address;
		
		ce.addProtocol( this );
	}
	
	private
	ProtocolEndpointUDP(
		InetSocketAddress		_address )
	{
		ce		= new ConnectionEndpoint(_address );
		address	= _address;
		
		ce.addProtocol( this );
	}
	
	public void
	setConnectionEndpoint(
		ConnectionEndpoint		_ce )
	{
		ce	= _ce;
		
		ce.addProtocol( this );
	}
	
	public int
	getType()
	{
		return( PROTOCOL_UDP );
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public InetSocketAddress 
	getAdjustedAddress(
		boolean to_lan )
	{
		return( AddressUtils.adjustUDPAddress( address, to_lan ));
	}
	
	public ConnectionEndpoint
	getConnectionEndpoint()
	{
		return( ce );
	}
	
	public Transport
	connectOutbound(
		boolean				connect_with_crypto, 
		boolean 			allow_fallback, 
		byte[][]			shared_secrets,
		ByteBuffer			initial_data,
		int					priority,
		ConnectListener 	listener )
	{
		UDPTransport t = new UDPTransport( this, shared_secrets );
		
		t.connectOutbound( initial_data, listener, priority );
		
		return( t );
	}
	
	public String
	getDescription()
	{
		return( address.toString());
	}
}
