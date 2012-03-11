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

package com.aelitis.azureus.core.dht.control;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public interface 
DHTControl 
{
	public static final int		K_DEFAULT								= 20;
	public static final int		B_DEFAULT								= 4;
	public static final int		MAX_REP_PER_NODE_DEFAULT				= 5;
	public static final int		SEARCH_CONCURRENCY_DEFAULT				= 5;
	public static final int		LOOKUP_CONCURRENCY_DEFAULT				= 10;
	public static final int		CACHE_AT_CLOSEST_N_DEFAULT				= 1;
	public static final int		ORIGINAL_REPUBLISH_INTERVAL_DEFAULT		= 8*60*60*1000;
	public static final int		CACHE_REPUBLISH_INTERVAL_DEFAULT		=   30*60*1000; 
	
	public void
	seed(
		boolean		full_wait );
		
	public boolean
	isSeeded();
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		byte					life_hours,
		byte					replication_control,
		boolean					high_priority,
		DHTOperationListener	listener );
	
	public boolean
	isDiversified(
		byte[]		key );
	
	public DHTTransportValue
	getLocalValue(
		byte[]		key );
		
	public void
	get(
		byte[]					key,
		String					description,
		byte					flags,
		int						max_values,
		long					timeout,
		boolean					exhaustive,
		boolean					high_priority,
		DHTOperationListener	listener );
		
	public byte[]
	remove(
		byte[]					key,
		String					description,
		DHTOperationListener	listener );
	
	public byte[]
	remove(
		DHTTransportContact[]	contacts,
		byte[]					key,
		String					description,
		DHTOperationListener	listener );
	
	public DHTControlStats
	getStats();
	
	public DHTTransport
	getTransport();
	
	public DHTRouter
	getRouter();
	
	public DHTDB
	getDataBase();
	
	public DHTControlActivity[]
	getActivities();
	
	public void
	exportState(
		DataOutputStream	os,
		int				max )
		
		throws IOException;
		
	public void
	importState(
		DataInputStream		is )
		
		throws IOException;
	
		// support methods for DB
	
	public List<DHTTransportContact>
	getClosestKContactsList(
		byte[]		id,
		boolean		live_only );
	
	public List<DHTTransportContact>
	getClosestContactsList(
		byte[]		id,
		int			num_to_return,
		boolean		live_only );
	
	public void
	putEncodedKey(
		byte[]				key,
		String				description,
		DHTTransportValue	value,
		long				timeout,
		boolean				original_mappings );
	
	public void
	putDirectEncodedKeys(
		byte[][]					keys,
		String						description,
		DHTTransportValue[][]		value_sets,
		List<DHTTransportContact>	contacts );
	
	public void
	putDirectEncodedKeys(
		byte[][]					keys,
		String						description,
		DHTTransportValue[][]		value_sets,
		DHTTransportContact			contact,
		DHTOperationListener		listener );
	
	public int
	computeAndCompareDistances(
		byte[]		n1,
		byte[]		n2,
		byte[]		pivot );
	
	public byte[]
	computeDistance(
		byte[]		n1,
		byte[]		n2 );
	
	public int
	compareDistances(
		byte[]		n1,
		byte[]		n2 );
	
	public boolean
	verifyContact(
		DHTTransportContact c,
		boolean				direct );
	
	public boolean
	lookup(
		byte[]					id,
		String					description,
		long					timeout,
		DHTOperationListener	listener );
	
	public boolean
	lookupEncoded(
		byte[]					id,
		String					description,
		long					timeout,
		boolean					high_priority,
		DHTOperationListener	listener );
	
	public byte[]
	getObfuscatedKey(
		byte[]		plain_key );
	
	
	public List<DHTControlContact>
	getContacts();
	
		// debug method only
	
	public void
	pingAll();
	
	public void
	addListener(
		DHTControlListener	l );
	
	public void
	removeListener(
		DHTControlListener	l );
	
	public void
	print(
		boolean	full );
}
