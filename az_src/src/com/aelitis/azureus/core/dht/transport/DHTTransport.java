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

import java.io.*;

public interface 
DHTTransport 
{	
	public byte
	getProtocolVersion();
	
	public int
	getNetwork();

	public boolean
	isIPV6();
	
		/**
		 * Gives access to the node ID for this transport 
		 * @return
		 */
	
	public DHTTransportContact
	getLocalContact();
	
	public int
	getPort();
	
	public void
	setPort(
		int	port )
	
		throws DHTTransportException;
	
	public long
	getTimeout();
	
	public void
	setTimeout(
		long		millis );
	
	public DHTTransportContact
	importContact(
		DataInputStream		is )
	
		throws IOException, DHTTransportException;
	
		/**
		 * Set the handler for incoming requests
		 * @param receiver
		 */
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	receiver );
	
	public DHTTransportStats
	getStats();
	
		// direct contact-contact communication
	
	public void
	registerTransferHandler(
		byte[]						handler_key,
		DHTTransportTransferHandler	handler );
	
	public byte[]
	readTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	
		throws DHTTransportException;
	
	public void
	writeTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							key,
		byte[]							data,
		long							timeout )
	
		throws DHTTransportException;
	
	public byte[]
	writeReadTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							data,
		long							timeout )	
	
		throws DHTTransportException;

	public boolean
	supportsStorage();
	
	public boolean
	isReachable();
	
	public DHTTransportContact[]
	getReachableContacts();
	
	public DHTTransportContact[]
	getRecentContacts();
	
	public void
	addListener(
		DHTTransportListener	l );
	
	public void
	removeListener(
		DHTTransportListener	l );
}
