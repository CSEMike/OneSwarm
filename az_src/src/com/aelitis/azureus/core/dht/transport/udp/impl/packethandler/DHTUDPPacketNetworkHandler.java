/*
 * Created on 12-Jun-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl.packethandler;

import java.io.IOException;

import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacket;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketReply;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTUDPPacketRequest;
import com.aelitis.net.udp.uc.PRUDPPacketRequest;
import com.aelitis.net.udp.uc.PRUDPRequestHandler;

public class 
DHTUDPPacketNetworkHandler 
	implements PRUDPRequestHandler
{
	private DHTUDPPacketHandlerFactory		factory;
	private int								port;
	
	protected
	DHTUDPPacketNetworkHandler(
		DHTUDPPacketHandlerFactory		_factory,
		int								_port )
	{
		factory		= _factory;
		port		= _port;
	}
	
	
	public DHTTransportUDPImpl
	getTransport(
		DHTUDPPacket		packet )
	
		throws IOException
	{
		if ( packet instanceof DHTUDPPacketRequest ){
			
			return( factory.getTransport( port, ((DHTUDPPacketRequest)packet).getNetwork()));
		
		}else{
			
			return( factory.getTransport( port, ((DHTUDPPacketReply)packet).getNetwork()));
		}	
	}
	
	public void
	process(
		PRUDPPacketRequest	_request )
	{
		if ( _request instanceof DHTUDPPacketRequest ){
		
			DHTUDPPacketRequest	request = (DHTUDPPacketRequest)_request;
		
			factory.process( port, request );
		}
	}
}
