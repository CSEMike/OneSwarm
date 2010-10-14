/*
 * Created on 16 Jun 2006
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

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.Transport.ConnectListener;

public class 
ProtocolEndpointTCP 
	implements ProtocolEndpoint
{
	private ConnectionEndpoint		ce;
	private InetSocketAddress		address;
	
	public
	ProtocolEndpointTCP(
		ConnectionEndpoint		_ce,
		InetSocketAddress		_address )
	{
		ce		= _ce;
		address	= _address;
		
		ce.addProtocol( this );
	}
	
	public
	ProtocolEndpointTCP(
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
		return( PROTOCOL_TCP );
	}
	
	public ConnectionEndpoint
	getConnectionEndpoint()
	{
		return( ce );
	}
	
	public InetSocketAddress
	getAddress()
	{
		return( address );
	}
	
	public Transport
	connectOutbound(
		boolean				connect_with_crypto, 
		boolean 			allow_fallback, 
		byte[][]			shared_secrets,
		ByteBuffer			initial_data,
		boolean				high_priority,
		ConnectListener 	listener )
	{
		TCPTransportImpl t = new TCPTransportImpl( this, connect_with_crypto, allow_fallback, shared_secrets );
					
		t.connectOutbound( initial_data, listener, high_priority );
		
		return( t );
	}
	
	public Transport
	connectLightWeight(
		SocketChannel		sc )
	{
		return new LightweightTCPTransport( this, TCPTransportHelperFilterFactory.createTransparentFilter( sc ) );
	}
	
	public String
	getDescription()
	{
		return( address.toString());
	}
}
