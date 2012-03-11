/*
 * Created on 28-Jan-2005
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

package com.aelitis.azureus.core.dht.db;

import java.util.Iterator;
import java.util.List;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportQueryStoreReply;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public interface 
DHTDB 
{
	public void
	setControl(
		DHTControl		control );
	
		/**
		 * Local store
		 * @param key
		 * @param value
		 * @param flags
		 * @return
		 */
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value,
		byte			flags,
		byte			life_hours,
		byte			replication_control );
	
		/**
		 * Remote store
		 * 
		 * @param sender
		 * @param key
		 * @param values
		 * @return diversification state
		 */
	
	public byte
	store(
		DHTTransportContact 	sender, 
		HashWrapper				key,
		DHTTransportValue[]		values );
	
	public DHTTransportQueryStoreReply
	queryStore(
		DHTTransportContact 		originating_contact, 
		int							header_len,
		List<Object[]>				keys );
	
		/**
		 * Internal lookup for locally originated values
		 * @param key
		 * @return
		 */
	
	public DHTDBValue
	get(
		HashWrapper		key );
	
		/**
		 * Returns a value for the given key (local or remote) if found
		 * @param key
		 * @return
		 */
	
	public DHTDBValue
	getAnyValue(
		HashWrapper		key );
	
	public boolean
	hasKey(
		HashWrapper		key );
	
	public DHTDBLookupResult
	get(
		DHTTransportContact		reader,
		HashWrapper				key,
		int						max_values,
		byte					flags,
		boolean					external_request );
		
		/**
		 * Local remove - returns a value suitable for putting in the DHT
		 * @param sender
		 * @param key
		 * @return
		 */
	
	public DHTDBValue
	remove(	
		DHTTransportContact 	sender,
		HashWrapper				key );
	
	public DHTStorageBlock
	keyBlockRequest(
		DHTTransportContact		direct_sender,
		byte[]					request,
		byte[]					signature );
	
	public DHTStorageBlock
	getKeyBlockDetails(
		byte[]			key );
	
	public boolean
	isKeyBlocked(
		byte[]			key );
	
	public DHTStorageBlock[]
	getDirectKeyBlocks();
	
	public boolean
	isEmpty();
		
		/**
		 * Returns an iterator over HashWrapper values denoting the snapshot of keys
		 * Thus by the time a key is used the entry may no longer exist
		 * @return
		 */
	
	public Iterator<HashWrapper>
	getKeys();
	
	public DHTDBStats
	getStats();
	
	public void
	print(
		boolean		full );
}
