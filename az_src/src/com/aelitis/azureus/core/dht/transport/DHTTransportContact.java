/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.transport;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.InetSocketAddress;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

public interface 
DHTTransportContact
{
	public int
	getMaxFailForLiveCount();
	
	public int
	getMaxFailForUnknownCount();
	
	public int
	getInstanceID();
	
	public byte[]
	getID();
	
	public byte
	getProtocolVersion();
	
	public long
	getClockSkew();
	
	public void
	setRandomID(
		int	id );
	
	public int
	getRandomID();
	
	public String
	getName();
	
	public InetSocketAddress
	getAddress();
	
	public InetSocketAddress
	getExternalAddress();
	
	public boolean
	isAlive(
		long		timeout );

	public void
	isAlive(
		DHTTransportReplyHandler	handler,
		long						timeout );
	
	public boolean
	isValid();
	
	public void
	sendPing(
		DHTTransportReplyHandler	handler );
	
	public void
	sendImmediatePing(
		DHTTransportReplyHandler	handler,
		long						timeout );

	public void
	sendStats(
		DHTTransportReplyHandler	handler );
	
	public void
	sendStore(
		DHTTransportReplyHandler	handler,
		byte[][]					keys,
		DHTTransportValue[][]		value_sets,
		boolean						immediate );
	
	public void
	sendQueryStore(
		DHTTransportReplyHandler	handler,
		int							header_length,
		List<Object[]>				key_details );
	
	public void
	sendFindNode(
		DHTTransportReplyHandler	handler,
		byte[]						id );
		
	public void
	sendFindValue(
		DHTTransportReplyHandler	handler,
		byte[]						key,
		int							max_values,
		byte						flags );
		
	public void
	sendKeyBlock(
		DHTTransportReplyHandler	handler,
		byte[]						key_block_request,
		byte[]						key_block_signature );

	public DHTTransportFullStats
	getStats();
	
	public void
	exportContact(
		DataOutputStream	os )
	
		throws IOException, DHTTransportException;
	
	public void
	remove();
	
	public void
	createNetworkPositions(
		boolean		is_local );
			
	public DHTNetworkPosition[]
	getNetworkPositions();
	
	public DHTNetworkPosition
	getNetworkPosition(
		byte	position_type );

	public DHTTransport
	getTransport();
	
	public String
	getString();
}
