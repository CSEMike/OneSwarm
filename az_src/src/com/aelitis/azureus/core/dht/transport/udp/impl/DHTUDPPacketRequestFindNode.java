/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.packethandler.DHTUDPPacketNetworkHandler;


/**
 * @author parg
 *
 */

public class 
DHTUDPPacketRequestFindNode 
	extends DHTUDPPacketRequest
{
	private byte[]		id;
	
	private int			node_status;
	private int			estimated_dht_size;
	
	public
	DHTUDPPacketRequestFindNode(
		DHTTransportUDPImpl				_transport,
		long							_connection_id,
		DHTTransportUDPContactImpl		_local_contact,
		DHTTransportUDPContactImpl		_remote_contact )
	{
		super( _transport, DHTUDPPacketHelper.ACT_REQUEST_FIND_NODE, _connection_id, _local_contact, _remote_contact );
	}
	
	protected
	DHTUDPPacketRequestFindNode(
		DHTUDPPacketNetworkHandler		network_handler,
		DataInputStream					is,
		long							con_id,
		int								trans_id )
	
		throws IOException
	{
		super( network_handler, is, DHTUDPPacketHelper.ACT_REQUEST_FIND_NODE, con_id, trans_id );
		
		id = DHTUDPUtils.deserialiseByteArray( is, 64 );
		
		if ( getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_MORE_NODE_STATUS ){

			node_status 		= is.readInt();
			estimated_dht_size 	= is.readInt();
		}
		
		super.postDeserialise(is);
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		DHTUDPUtils.serialiseByteArray( os, id, 64 );
		
		if ( getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_MORE_NODE_STATUS ){

			 os.writeInt( node_status );
			
			 os.writeInt( estimated_dht_size );
		}
		
		super.postSerialise( os );
	}
	
	protected void
	setID(
		byte[]		_id )
	{
		id	= _id;
	}
	
	protected byte[]
	getID()
	{
		return( id );
	}
	
	protected void
	setNodeStatus(
		int		ns )
	{
		node_status	= ns;
	}
	
	protected int
	getNodeStatus()
	{
		return( node_status );
	}
		
	protected void
	setEstimatedDHTSize(
		int	s )
	{
		estimated_dht_size	= s;
	}
	
	protected int
	getEstimatedDHTSize()
	{
		return( estimated_dht_size );
	}
	
	public String
	getString()
	{
		return( super.getString());
	}
}