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

package com.aelitis.azureus.core.networkmanager;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.AddressUtils;

import com.aelitis.azureus.core.networkmanager.Transport.ConnectListener;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.udp.ProtocolEndpointUDP;

public class 
ConnectionEndpoint 
{
	private InetSocketAddress	notional_address;
	private ProtocolEndpoint[]	protocols;
	
	public
	ConnectionEndpoint(
		InetSocketAddress	_notional_address )
	{
		notional_address	= _notional_address;
	}
	
	public InetSocketAddress
	getNotionalAddress()
	{
		return( notional_address );
	}
	
	public boolean
	matchIP(
		String	ip )
	{
		if ( matchIP( ip, notional_address )){
			
			return( true );
		}
		
		ProtocolEndpoint[]	peps = getProtocols();
		
		for (int i=0;i<peps.length;i++){
			
			ProtocolEndpoint	pep = peps[i];
			
			if ( pep.getType() == ProtocolEndpoint.PROTOCOL_TCP ){
				
				ProtocolEndpointTCP	tcp = (ProtocolEndpointTCP)pep;
				
				if ( matchIP( ip, tcp.getAddress())){
					
					return( true );
				}
			}else{
				
				ProtocolEndpointUDP	tcp = (ProtocolEndpointUDP)pep;
				
				if ( matchIP( ip, tcp.getAddress())){
					
					return( true );
				}				
			}
		}
		
		return( false );
	}
	
	protected boolean
	matchIP(
		String				ip,
		InetSocketAddress	address )
	{
		return( ip.equals( address.getAddress().getHostAddress()));
	}
	
	public ProtocolEndpoint[]
	getProtocols()
	{
		if ( protocols == null ){
			
			return( new ProtocolEndpoint[0] );
		}
		
		return( protocols );
	}
	
	public void
	addProtocol(
		ProtocolEndpoint	ep )
	{
		if ( protocols == null ){
			
			protocols = new ProtocolEndpoint[]{ ep };
			
		}else{
		
			for (int i=0;i<protocols.length;i++){
				
				if ( protocols[i] == ep ){
					
					return;
				}
			}
			
			ProtocolEndpoint[]	new_ep = new ProtocolEndpoint[ protocols.length + 1 ];
			
			System.arraycopy( protocols, 0, new_ep, 0, protocols.length );
			
			new_ep[ protocols.length ] = ep;
			
			protocols	= new_ep;
		}
		
		ep.setConnectionEndpoint( this );
	}
	
	public ConnectionEndpoint
	getLANAdjustedEndpoint()
	{
		ConnectionEndpoint	result = new ConnectionEndpoint( notional_address );
		
		for (int i=0;i<protocols.length;i++){

			ProtocolEndpoint ep = protocols[i];
			
			InetSocketAddress address = ep.getAdjustedAddress( true );
			
			ProtocolEndpointFactory.createEndpoint( ep.getType(), result, address );
		}
		
		return( result );
	}
	
	public ConnectionAttempt
	connectOutbound(
		boolean				connect_with_crypto, 
		boolean 			allow_fallback, 
		byte[][]			shared_secrets,
		ByteBuffer			initial_data,
		int					priority,
		ConnectListener 	listener )
	{
		ProtocolEndpoint	protocol = protocols[0];
		
		final Transport transport = protocol.connectOutbound( connect_with_crypto, allow_fallback, shared_secrets, initial_data, priority, listener );
		
		return( 
			new ConnectionAttempt()
			{
				public void 
				abandon() 
				{
					if ( transport != null ){
						
						transport.close( "Connection attempt abandoned" );
					}
				}
			});
	}  
	
	public String
	getDescription()
	{
		String	str = "[";
		
		for (int i=0;i<protocols.length;i++){
			
			str += (i==0?"":",") + protocols[i].getDescription();
		}
		
		return( str + "]" );
	}
}
