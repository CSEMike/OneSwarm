/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht;

import java.io.*;

import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.speed.DHTSpeedTester;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public interface 
DHT 
{
		// all property values are Integer values
	
	public static final String	PR_CONTACTS_PER_NODE					= "EntriesPerNode";
	public static final String	PR_NODE_SPLIT_FACTOR					= "NodeSplitFactor";
	public static final String	PR_SEARCH_CONCURRENCY					= "SearchConcurrency";
	public static final String	PR_LOOKUP_CONCURRENCY					= "LookupConcurrency";
	public static final String	PR_MAX_REPLACEMENTS_PER_NODE			= "ReplacementsPerNode";
	public static final String	PR_CACHE_AT_CLOSEST_N					= "CacheClosestN";
	public static final String	PR_ORIGINAL_REPUBLISH_INTERVAL			= "OriginalRepublishInterval";
	public static final String	PR_CACHE_REPUBLISH_INTERVAL				= "CacheRepublishInterval";

	public static final byte		FLAG_SINGLE_VALUE		= 0x00;
	public static final byte		FLAG_DOWNLOADING		= 0x01;
	public static final byte		FLAG_SEEDING			= 0x02;
	public static final byte		FLAG_MULTI_VALUE		= 0x04;
	public static final byte		FLAG_STATS				= 0x08;
	public static final byte		FLAG_ANON				= 0x10;
	public static final byte		FLAG_PRECIOUS			= 0x20;
	public static final byte		FLAG_PUT_AND_FORGET		= 0x40;				// local only
	public static final byte		FLAG_OBFUSCATE_LOOKUP	= (byte)0x80;		// local only

	public static final int 	MAX_VALUE_SIZE		= 512;

	public static final byte	REP_FACT_NONE			= 0;
	public static final byte	REP_FACT_DEFAULT		= (byte)0xff;
	
		// diversification types, don't change as serialised!!!!
	
	public static final byte	DT_NONE			= 1;
	public static final byte	DT_FREQUENCY	= 2;
	public static final byte	DT_SIZE			= 3;
	
	public static final String[]	DT_STRINGS = { "", "None", "Freq", "Size" };
	
	public static final int		NW_MAIN			= 0;
	public static final int		NW_CVS			= 1;
	public static final int		NW_MAIN_V6		= 3;
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		DHTOperationListener	listener );
	
		/**
		 * default is HIGH PRIORITY. if you change to low priority then do so consistently as
		 * operations can get out of order otherwise
		 * @param key
		 * @param description
		 * @param value
		 * @param flags
		 * @param high_priority
		 * @param listener
		 */
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		boolean					high_priority,
		DHTOperationListener	listener );
	
	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		byte					life_hours,
		boolean					high_priority,
		DHTOperationListener	listener );

	public void
	put(
		byte[]					key,
		String					description,
		byte[]					value,
		byte					flags,
		byte					life_hours,
		byte					replication_control,	// 4 bits 1->14 republish hours; 0=vuze default | 4 bits 0->15 maintain replicas; [ff=no replication control-use default]
		boolean					high_priority,
		DHTOperationListener	listener );
	
		/**
		 * Returns value if originated from here for key
		 * @param key
		 * @return
		 */
	
	public DHTTransportValue
	getLocalValue(
		byte[]		key );
	
	
		/**
		 * @param key
		 * @param max_values
		 * @param timeout
		 * @param listener
		 */
	
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
	
	public boolean
	isDiversified(
		byte[]		key );
	
	public int
	getIntProperty(
		String		name );
	
	public DHTTransport
	getTransport();
	
	public DHTRouter
	getRouter();
	
	public DHTControl
	getControl();
	
	public DHTDB
	getDataBase();
	
	public DHTNATPuncher
	getNATPuncher();
	
	public DHTSpeedTester
	getSpeedTester();
	
	public DHTStorageAdapter
	getStorageAdapter();
	
		/**
		 * externalises information that allows the DHT to be recreated at a later date
		 * and populated via the import method
		 * @param os
		 * @param max  maximum to export, 0 -> all
		 * @throws IOException
		 */
	
	public void
	exportState(
		DataOutputStream	os,
		int					max )
	
		throws IOException;
	
		/**
		 * populate the DHT with previously exported state 
		 * @param is
		 * @throws IOException
		 */
	
	public void
	importState(
		DataInputStream		is )
	
		throws IOException;
	
		/**
		 * Integrate the node into the DHT
		 * Can be invoked more than once if additional state is imported
		 */
	
	public void
	integrate(
		boolean		full_wait );

	public void
	destroy();
	
	public void
	setLogging(
		boolean	on );

	public DHTLogger
	getLogger();
	
	public void
	print(
		boolean	full );
}
