/*
 * Created on 19 Jun 2006
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import java.net.InetSocketAddress;

import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpointFactory;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.udp.ProtocolEndpointUDP;

public class 
GenericMessageEndpointImpl 
	implements GenericMessageEndpoint
{
	private ConnectionEndpoint		ce;
	
	public
	GenericMessageEndpointImpl(
		ConnectionEndpoint		_ce )
	{
		ce		= _ce;
	}
	
	public
	GenericMessageEndpointImpl(
		InetSocketAddress		_ne )
	{
		ce		= new ConnectionEndpoint( _ne );
	}
	
	public InetSocketAddress
	getNotionalAddress()
	{
		return( ce.getNotionalAddress());
	}
	
	protected ConnectionEndpoint
	getConnectionEndpoint()
	{
		return( ce );
	}
	
	public void
	addTCP(
		InetSocketAddress	target )
	{
		ce.addProtocol( ProtocolEndpointFactory.createEndpoint( ProtocolEndpoint.PROTOCOL_TCP, target ));
	}
	
	public InetSocketAddress
	getTCP()
	{
		ProtocolEndpoint[]	pes = ce.getProtocols();
		
		for (int i=0;i<pes.length;i++){
			
			if ( pes[i] instanceof ProtocolEndpointTCP ){
				
				return( ((ProtocolEndpointTCP)pes[i]).getAddress());
			}
		}
			
		return( null );
	}
	
	public void
	addUDP(
		InetSocketAddress	target )
	{
		ce.addProtocol( ProtocolEndpointFactory.createEndpoint( ProtocolEndpoint.PROTOCOL_UDP, target ));
	}
	
	public InetSocketAddress
	getUDP()
	{
		ProtocolEndpoint[]	pes = ce.getProtocols();
		
		for (int i=0;i<pes.length;i++){
			
			if ( pes[i] instanceof ProtocolEndpointUDP ){
				
				return( ((ProtocolEndpointUDP)pes[i]).getAddress());
			}
		}
			
		return( null );
	}
}
