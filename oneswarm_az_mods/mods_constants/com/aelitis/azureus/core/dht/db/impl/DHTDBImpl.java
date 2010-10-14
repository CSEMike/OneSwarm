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

package com.aelitis.azureus.core.dht.db.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;


import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTStorageAdapter;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.DHTStorageKey;
import com.aelitis.azureus.core.dht.DHTStorageKeyStats;
import com.aelitis.azureus.core.dht.db.*;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;

/**
 * @author parg
 *
 */

public class 
DHTDBImpl
	implements DHTDB, DHTDBStats
{	
	private int			original_republish_interval;
	
		// the grace period gives the originator time to republish their data as this could involve
		// some work on their behalf to find closest nodes etc. There's no real urgency here anyway
	
	private int			ORIGINAL_REPUBLISH_INTERVAL_GRACE	= 60*60*1000;
	
	private int			cache_republish_interval;
	
	private long		MIN_CACHE_EXPIRY_CHECK_INTERVAL		= 60000;
	private long		last_cache_expiry_check;
	
	private static final long	IP_BLOOM_FILTER_REBUILD_PERIOD		= 15*60*1000;
	private static final int	IP_COUNT_BLOOM_SIZE_INCREASE_CHUNK	= 1000;
	
	private BloomFilter	ip_count_bloom_filter = BloomFilterFactory.createAddRemove8Bit( IP_COUNT_BLOOM_SIZE_INCREASE_CHUNK );
	
	private static final int	VALUE_VERSION_CHUNK = 128;
	private int	next_value_version;
	private int next_value_version_left;
	
	
	private Map			stored_values = new HashMap();
	
	private DHTControl				control;
	private DHTStorageAdapter		adapter;
	private DHTRouter				router;
	private DHTTransportContact		local_contact;
	private DHTLogger				logger;
	
	// PIAMOD -- save memory by reducing this from 4 -> 1
	private static final long	MAX_TOTAL_SIZE	= 1*1024*1024;
	
	private long		total_size;
	private long		total_values;
	private long		total_keys;
	
	private boolean force_original_republish;
	
	private IpFilter	ip_filter	= IpFilterManagerFactory.getSingleton().getIPFilter();

	private AEMonitor	this_mon	= new AEMonitor( "DHTDB" );

	public
	DHTDBImpl(
		DHTStorageAdapter	_adapter,
		int					_original_republish_interval,
		int					_cache_republish_interval,
		DHTLogger			_logger )
	{
		adapter							= _adapter==null?null:new adapterFacade( _adapter );
		original_republish_interval		= _original_republish_interval;
		cache_republish_interval		= _cache_republish_interval;
		logger							= _logger;
				
		
		
		SimpleTimer.addPeriodicEvent(
			"DHTDB:op",
			original_republish_interval,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					logger.log( "Republish of original mappings starts" );
					
					long	start 	= SystemTime.getCurrentTime();
					
					int	stats = republishOriginalMappings();
					
					long	end 	= SystemTime.getCurrentTime();

					logger.log( "Republish of original mappings completed in " + (end-start) + ": " +
								"values = " + stats );

				}
			});
					
				// random skew here so that cache refresh isn't very synchronised, as the optimisations
				// regarding non-republising benefit from this 
			
		SimpleTimer.addPeriodicEvent(
				"DHTDB:cp",
				cache_republish_interval + 10000 - (int)(Math.random()*20000),
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						logger.log( "Republish of cached mappings starts" );
						
						long	start 	= SystemTime.getCurrentTime();
						
						int[]	stats = republishCachedMappings();		
						
						long	end 	= SystemTime.getCurrentTime();

						logger.log( "Republish of cached mappings completed in " + (end-start) + ": " +
									"values = " + stats[0] + ", keys = " + stats[1] + ", ops = " + stats[2]);
						
						if ( force_original_republish ){
							
							force_original_republish	= false;
							
							logger.log( "Force republish of original mappings due to router change starts" );
							
							start 	= SystemTime.getCurrentTime();
							
							int stats2 = republishOriginalMappings();
							
							end 	= SystemTime.getCurrentTime();

							logger.log( "Force republish of original mappings due to router change completed in " + (end-start) + ": " +
										"values = " + stats2 );
						}
					}
				});
		
	
		
		SimpleTimer.addPeriodicEvent(
				"DHTDB:bloom",
				IP_BLOOM_FILTER_REBUILD_PERIOD,
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						try{
							this_mon.enter();
							
							rebuildIPBloomFilter( false );
							
						}finally{
							
							this_mon.exit();
						}
					}
				});
						
	}
	
	
	public void
	setControl(
		DHTControl		_control )
	{
		control			= _control;
		
			// trigger an "original value republish" if router has changed
		
		force_original_republish = router != null;
		
		router			= control.getRouter();
		local_contact	= control.getTransport().getLocalContact(); 
	
			// our ID has changed - amend the originator of all our values
		
		try{
			this_mon.enter();
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				mapping.updateLocalContact( local_contact );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBValue
	store(
		HashWrapper		key,
		byte[]			value,
		byte			flags )
	{
			// local store
		
		try{
			this_mon.enter();
				
				// don't police max check for locally stored data
				// only that received
			
			DHTDBMapping	mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping == null ){
				
				mapping = new DHTDBMapping( this, key, true );
				
				stored_values.put( key, mapping );
			}
			
			DHTDBValueImpl res =	
				new DHTDBValueImpl( 
						SystemTime.getCurrentTime(), 
						value, 
						getNextValueVersion(),
						local_contact, 
						local_contact,
						true,
						flags );
	
			mapping.add( res );
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public byte
	store(
		DHTTransportContact 	sender, 
		HashWrapper				key,
		DHTTransportValue[]		values )
	{
			// allow 4 bytes per value entry to deal with overhead (prolly should be more but we're really
			// trying to deal with 0-length value stores)
		
		if ( total_size + ( total_values*4 ) > MAX_TOTAL_SIZE ){
			
			DHTLog.log( "Not storing " + DHTLog.getString2(key.getHash()) + " as maximum storage limit exceeded" );

			return( DHT.DT_SIZE );
		}
		
			// remote store for cache values
		
			// Make sure that we only accept values for storing that are reasonable.
			// Assumption is that the caller has made a reasonable effort to ascertain
			// the correct place to store a value. Part of this will in general have 
			// needed them to query us for example. Therefore, limit values to those
			// that are at least as close to us
		
		List closest_contacts = control.getClosestKContactsList( key.getHash(), true );
		
		boolean	store_it	= false;
		
		for (int i=0;i<closest_contacts.size();i++){
			
			if ( router.isID(((DHTTransportContact)closest_contacts.get(i)).getID())){
				
				store_it	= true;
				
				break;
			}		
		}
		
		if ( !store_it ){
			
			DHTLog.log( "Not storing " + DHTLog.getString2(key.getHash()) + " as key too far away" );

			return( DHT.DT_NONE );
		}
		
			// next, for cache forwards (rather then values coming directly from 
			// originators) we ensure that the contact sending the values to us is
			// close enough. If any values are coming indirect then we can safely assume
			// that they all are
		
		boolean	cache_forward = false;
		
		for (int i=0;i<values.length;i++){
			
			if (!Arrays.equals( sender.getID(), values[i].getOriginator().getID())){
				
				cache_forward	= true;
				
				break;
			}
		}
		
		
		if ( cache_forward ){
			
				// get the closest contacts to me
				
			byte[]	my_id	= local_contact.getID();
			
			closest_contacts = control.getClosestKContactsList( my_id, true );
			
			DHTTransportContact	furthest = (DHTTransportContact)closest_contacts.get( closest_contacts.size()-1);
						
			if ( control.computeAndCompareDistances( furthest.getID(), sender.getID(), my_id ) < 0 ){

				store_it	= false;
			}
		}
		
		if ( !store_it ){
			
			DHTLog.log( "Not storing " + DHTLog.getString2(key.getHash()) + " as cache forward and sender too far away" );
			
			return( DHT.DT_NONE );
		}
		
		try{
			this_mon.enter();
						
			checkCacheExpiration( false );
				
			DHTDBMapping	mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping == null ){
				
				mapping = new DHTDBMapping( this, key, false );
				
				stored_values.put( key, mapping );
			}
			
			boolean contact_checked = false;
			boolean	contact_ok		= false;
			
				// we carry on an update as its ok to replace existing entries
				// even if diversified
			
			for (int i=0;i<values.length;i++){
				
				DHTTransportValue	t_value = values[i];
								
					// last check, verify that the contact is who they say they are, only for non-forwards
					// as cache forwards are only accepted if they are "close enough" and we can't 
					// rely on their identify due to the way that cache republish works (it doesn't
					// guarantee a "lookup_node" prior to "store".

				DHTTransportValue	value = values[i];
				
				boolean	ok_to_store = false;
				
				boolean	direct =Arrays.equals( sender.getID(), value.getOriginator().getID());
								
				if ( !contact_checked ){
						
					contact_ok =  control.verifyContact( sender, direct );
						
					if ( !contact_ok ){
						
						logger.log( "DB: verification of contact '" + sender.getName() + "' failed for store operation" );
					}
					
					contact_checked	= true;
				}
			
				ok_to_store	= contact_ok;

				if ( ok_to_store ){
					
					DHTDBValueImpl mapping_value	= new DHTDBValueImpl( sender, value, false );
			
					mapping.add( mapping_value );
				}
			}
			
			return( mapping.getDiversificationType());
	
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBLookupResult
	get(
		DHTTransportContact		reader,
		HashWrapper				key,
		int						max_values,	// 0 -> all
		byte					flags,
		boolean					external_request )	
	{
		try{
			this_mon.enter();
			
			checkCacheExpiration( false );
					
			final DHTDBMapping mapping = (DHTDBMapping)stored_values.get(key);
			
			if ( mapping == null ){
				
				return( null );
			}
			
			if ( external_request ){
				
				mapping.addHit();
			}
			
			final DHTDBValue[]	values = mapping.get( reader, max_values, flags );
						
			return(
				new DHTDBLookupResult()
				{
					public DHTDBValue[]
					getValues()
					{
						return( values );
					}
					
					public byte
					getDiversificationType()
					{
						return( mapping.getDiversificationType());
					}
				});
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBValue
	get(
		HashWrapper				key )
	{
			// local remove
		
		try{
			this_mon.enter();
		
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping != null ){
				
				return( mapping.get( local_contact ));
			}
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTDBValue
	remove(
		DHTTransportContact 	originator,
		HashWrapper				key )
	{
			// local remove
		
		try{
			this_mon.enter();
		
			DHTDBMapping mapping = (DHTDBMapping)stored_values.get( key );
			
			if ( mapping != null ){
				
				DHTDBValueImpl	res = mapping.remove( originator );
				
				if ( res != null ){
					
					return( res.getValueForDeletion( getNextValueVersion()));
				}
				
				return( null ); 
			}
			
			return( null );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public DHTStorageBlock
	keyBlockRequest(
		DHTTransportContact		direct_sender,
		byte[]					request,
		byte[]					signature )
	{
		if ( adapter == null ){
			
			return( null );
		}
		
			// for block requests sent to us (as opposed to being returned from other operations)
			// make sure that the key is close enough to us
		
		if ( direct_sender != null ){
			
			byte[]	key = adapter.getKeyForKeyBlock( request );
			
			List closest_contacts = control.getClosestKContactsList( key, true );
			
			boolean	process_it	= false;
			
			for (int i=0;i<closest_contacts.size();i++){
				
				if ( router.isID(((DHTTransportContact)closest_contacts.get(i)).getID())){
					
					process_it	= true;
					
					break;
				}		
			}
		
			if ( !process_it ){
			
				DHTLog.log( "Not processing key block for  " + DHTLog.getString2(key) + " as key too far away" );

				return( null );
			}
			
			if ( ! control.verifyContact( direct_sender, true )){
				
				DHTLog.log( "Not processing key block for  " + DHTLog.getString2(key) + " as verification failed" );

				return( null );
			}
		}
		
		return( adapter.keyBlockRequest( direct_sender, request, signature ));
	}
	
	public DHTStorageBlock
	getKeyBlockDetails(
		byte[]		key )
	{
		if ( adapter == null ){
			
			return( null );
		}
		
		return( adapter.getKeyBlockDetails( key ));
	}
	
	public boolean
	isKeyBlocked(
		byte[]		key )
	{
		return( getKeyBlockDetails(key) != null );
	}
	
	public DHTStorageBlock[]
	getDirectKeyBlocks()
	{
		if ( adapter == null ){
			
			return( new DHTStorageBlock[0] );
		}
		
		return( adapter.getDirectKeyBlocks());
	}
	
	public boolean
	isEmpty()
	{
		return( total_keys == 0 );
	}
	
	public int
	getKeyCount()
	{
		return( (int)total_keys );
	}
	
	public int[]
	getValueDetails()
	{
		try{
			this_mon.enter();
			
			int[]	res = new int[6];
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
				
				res[DHTDBStats.VD_VALUE_COUNT] += mapping.getValueCount();
				res[DHTDBStats.VD_LOCAL_SIZE] += mapping.getLocalSize();
				res[DHTDBStats.VD_DIRECT_SIZE] += mapping.getDirectSize();
				res[DHTDBStats.VD_INDIRECT_SIZE] += mapping.getIndirectSize();
				
				int	dt = mapping.getDiversificationType();
				
				if ( dt == DHT.DT_FREQUENCY ){
					
					res[DHTDBStats.VD_DIV_FREQ]++;
					
				}else if ( dt == DHT.DT_SIZE ){
					
					res[DHTDBStats.VD_DIV_SIZE]++;
				}
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public int
	getKeyBlockCount()
	{
		if ( adapter == null ){
		
			return( 0 );
		}
		
		return( adapter.getDirectKeyBlocks().length );
	}
	
	public Iterator
	getKeys()
	{
		try{
			this_mon.enter();
			
			return( new ArrayList( stored_values.keySet()).iterator());
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected int
	republishOriginalMappings()
	{
		int	values_published	= 0;

		Map	republish = new HashMap();
		
		try{
			this_mon.enter();
			
			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper		key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping	= (DHTDBMapping)entry.getValue();
				
				Iterator	it2 = mapping.getValues();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();
				
					if ( value != null && value.isLocal()){
						
						// we're republising the data, reset the creation time
						
						value.setCreationTime();

						values.add( value );
					}
				}
				
				if ( values.size() > 0 ){
					
					republish.put( key, values );
					
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		Iterator	it = republish.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			HashWrapper			key		= (HashWrapper)entry.getKey();
			
			List		values	= (List)entry.getValue();
			
				// no point in worry about multi-value puts here as it is extremely unlikely that
				// > 1 value will locally stored, or > 1 value will go to the same contact
			
			for (int i=0;i<values.size();i++){
				
				values_published++;
				
				control.putEncodedKey( key.getHash(), "Republish", (DHTDBValueImpl)values.get(i), 0, true );
			}
		}
		
		return( values_published );
	}
	
	protected int[]
	republishCachedMappings()
	{		
			// first refresh any leaves that have not performed at least one lookup in the
			// last period
		
		router.refreshIdleLeaves( cache_republish_interval );
		
		final Map	republish = new HashMap();
		
		long	now = System.currentTimeMillis();
		
		try{
			this_mon.enter();
			
			checkCacheExpiration( true );

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				DHTDBMapping		mapping	= (DHTDBMapping)entry.getValue();
				
					// assume that if we've diversified then the other k-1 locations are under similar
					// stress and will have done likewise - no point in republishing cache values to them
					// New nodes joining will have had stuff forwarded to them regardless of diversification
					// status
				
				if ( mapping.getDiversificationType() != DHT.DT_NONE ){
					
					continue;
				}
				
				Iterator	it2 = mapping.getValues();
				
				List	values = new ArrayList();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();
				
					if ( !value.isLocal()){
						
							// if this value was stored < period ago then we assume that it was
							// also stored to the other k-1 locations at the same time and therefore
							// we don't need to re-store it
						
						if ( now < value.getStoreTime()){
							
								// deal with clock changes
							
							value.setStoreTime( now );
							
						}else if ( now - value.getStoreTime() <= cache_republish_interval ){
							
							// System.out.println( "skipping store" );
							
						}else{
								
							values.add( value );
						}
					}
				}

				if ( values.size() > 0 ){
					
					republish.put( key, values );
					
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		final int[]	values_published	= {0};
		final int[]	keys_published		= {0};
		final int[]	republish_ops		= {0};
		
		final HashSet	anti_spoof_done	= new HashSet();
		
		if ( republish.size() > 0 ){
			
			// System.out.println( "cache replublish" );
			
				// The approach is to refresh all leaves in the smallest subtree, thus populating the tree with
				// sufficient information to directly know which nodes to republish the values
				// to.
			
				// However, I'm going to rely on the "refresh idle leaves" logic above
				// (that's required to keep the DHT alive in general) to ensure that all
				// k-buckets are reasonably up-to-date
					
			Iterator	it = republish.entrySet().iterator();
			
			List	stop_caching = new ArrayList();
			
				// build a map of contact -> list of keys to republish
			
			Map	contact_map	= new HashMap();
			
			while( it.hasNext()){
				
				Map.Entry	entry = (Map.Entry)it.next();
				
				HashWrapper			key		= (HashWrapper)entry.getKey();
				
				byte[]	lookup_id	= key.getHash();
				
					// just use the closest contacts - if some have failed then they'll
					// get flushed out by this operation. Grabbing just the live ones
					// is a bad idea as failures may rack up against the live ones due
					// to network problems and kill them, leaving the dead ones!
				
				List	contacts = control.getClosestKContactsList( lookup_id, false );
							
					// if we are no longer one of the K closest contacts then we shouldn't
					// cache the value
				
				boolean	keep_caching	= false;
				
				for (int j=0;j<contacts.size();j++){
				
					if ( router.isID(((DHTTransportContact)contacts.get(j)).getID())){
						
						keep_caching	= true;
						
						break;
					}
				}
				
				if ( !keep_caching ){
					
					DHTLog.log( "Dropping cache entry for " + DHTLog.getString( lookup_id ) + " as now too far away" );
					
					stop_caching.add( key );
					
						// we carry on and do one last publish
					
				}
				
				for (int j=0;j<contacts.size();j++){
					
					DHTTransportContact	contact = (DHTTransportContact)contacts.get(j);
					
					if ( router.isID( contact.getID())){
						
						continue;	// ignore ourselves
					}
					
					Object[]	data = (Object[])contact_map.get( new HashWrapper(contact.getID()));
					
					if ( data == null ){
						
						data	= new Object[]{ contact, new ArrayList()};
						
						contact_map.put( new HashWrapper(contact.getID()), data );
					}
					
					((List)data[1]).add( key );
				}
			}
		
			it = contact_map.values().iterator();
			
			while( it.hasNext()){
				
				final Object[]	data = (Object[])it.next();
				
				final DHTTransportContact	contact = (DHTTransportContact)data[0];
				
					// move to anti-spoof on cache forwards - gotta do a find-node first
					// to get the random id
				
				final AESemaphore	sem = new AESemaphore( "DHTDB:cacheForward" );
				
				contact.sendFindNode(
						new DHTTransportReplyHandlerAdapter()
						{
							public void
							findNodeReply(
								DHTTransportContact 	_contact,
								DHTTransportContact[]	_contacts )
							{	
								anti_spoof_done.add( _contact );
							
								try{
									// System.out.println( "cacheForward: pre-store findNode OK" );
								
									List				keys	= (List)data[1];
										
									byte[][]				store_keys 		= new byte[keys.size()][];
									DHTTransportValue[][]	store_values 	= new DHTTransportValue[store_keys.length][];
									
									keys_published[0] += store_keys.length;
									
									for (int i=0;i<store_keys.length;i++){
										
										HashWrapper	wrapper = (HashWrapper)keys.get(i);
										
										store_keys[i] = wrapper.getHash();
										
										List		values	= (List)republish.get( wrapper );
										
										store_values[i] = new DHTTransportValue[values.size()];
							
										values_published[0] += store_values[i].length;
										
										for (int j=0;j<values.size();j++){
										
											DHTDBValueImpl	value	= (DHTDBValueImpl)values.get(j);
												
												// we reduce the cache distance by 1 here as it is incremented by the
												// recipients
											
											store_values[i][j] = value.getValueForRelay(local_contact);
										}
									}
										
									List	contacts = new ArrayList();
									
									contacts.add( contact );
									
									republish_ops[0]++;
									
									control.putDirectEncodedKeys( 
											store_keys, 
											"Republish cache",
											store_values,
											contacts );
								}finally{
									
									sem.release();
								}
							} 
							
							public void
							failed(
								DHTTransportContact 	_contact,
								Throwable				_error )
							{
								try{
									// System.out.println( "cacheForward: pre-store findNode Failed" );
	
									DHTLog.log( "cacheForward: pre-store findNode failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
																			
									router.contactDead( _contact.getID(), false);
									
								}finally{
									
									sem.release();
								}
							}
						},
						contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF2?new byte[0]:new byte[20] );
				
				sem.reserve();
			}
			
			try{
				this_mon.enter();
				
				for (int i=0;i<stop_caching.size();i++){
					
					DHTDBMapping	mapping = (DHTDBMapping)stored_values.remove( stop_caching.get(i));
					
					if ( mapping != null ){
						
						mapping.destroy();
					}
				}
			}finally{
				
				this_mon.exit();
			}
		}
		
		DHTStorageBlock[]	direct_key_blocks = getDirectKeyBlocks();

		if ( direct_key_blocks.length > 0 ){
					
			for (int i=0;i<direct_key_blocks.length;i++){
			
				final DHTStorageBlock	key_block = direct_key_blocks[i];
				
				List	contacts = control.getClosestKContactsList( key_block.getKey(), false );

				boolean	forward_it = false;
				
					// ensure that the key is close enough to us 
				
				for (int j=0;j<contacts.size();j++){

					final DHTTransportContact	contact = (DHTTransportContact)contacts.get(j);

					if ( router.isID( contact.getID())){
						
						forward_it	= true;
						
						break;
					}
				}
					
				for (int j=0; forward_it && j<contacts.size();j++){
					
					final DHTTransportContact	contact = (DHTTransportContact)contacts.get(j);
					
					if ( key_block.hasBeenSentTo( contact )){
						
						continue;
					}
					
					if ( contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_BLOCK_KEYS ){
						
						final Runnable task = 
							new Runnable()
							{
								public void
								run()
								{
									contact.sendKeyBlock(
										new DHTTransportReplyHandlerAdapter()
										{
											public void
											keyBlockReply(
												DHTTransportContact 	_contact )
											{
												DHTLog.log( "key block forward ok " + DHTLog.getString( _contact ));
												
												key_block.sentTo( _contact );
											}
											
											public void
											failed(
												DHTTransportContact 	_contact,
												Throwable				_error )
											{
												DHTLog.log( "key block forward failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
											}
										},
										key_block.getRequest(),
										key_block.getCertificate());
								}
							};
						
							if ( anti_spoof_done.contains( contact )){
								
								task.run();
								
							}else{
								
								contact.sendFindNode(
										new DHTTransportReplyHandlerAdapter()
										{
											public void
											findNodeReply(
												DHTTransportContact 	contact,
												DHTTransportContact[]	contacts )
											{	
												task.run();
											}
											public void
											failed(
												DHTTransportContact 	_contact,
												Throwable				_error )
											{
												// System.out.println( "nodeAdded: pre-store findNode Failed" );

												DHTLog.log( "pre-kb findNode failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());
																						
												router.contactDead( _contact.getID(), false);
											}
										},
										contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF2?new byte[0]:new byte[20] );
							}
					}
				}
			}
		}
		
		return( new int[]{ values_published[0], keys_published[0], republish_ops[0] });
	}
		
	protected void
	checkCacheExpiration(
		boolean		force )
	{
		long	 now = SystemTime.getCurrentTime();
		
		if ( !force ){
			
			long elapsed = now - last_cache_expiry_check;
			
			if ( elapsed > 0 && elapsed < MIN_CACHE_EXPIRY_CHECK_INTERVAL ){
				
				return;
			}
		}
			
		try{
			this_mon.enter();
			
			last_cache_expiry_check	= now;
			
			Iterator	it = stored_values.values().iterator();
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();
	
				if ( mapping.getValueCount() == 0 ){
					
					mapping.destroy();
					
					it.remove();
										
				}else{
					
					Iterator	it2 = mapping.getValues();
					
					while( it2.hasNext()){
						
						DHTDBValueImpl	value = (DHTDBValueImpl)it2.next();				
						
						if ( !value.isLocal()){
							
								// distance 1 = initial store location. We use the initial creation date
								// when deciding whether or not to remove this, plus a bit, as the 
								// original publisher is supposed to republish these
							
							if ( now - value.getCreationTime() > original_republish_interval + ORIGINAL_REPUBLISH_INTERVAL_GRACE ){
								
								DHTLog.log( "removing cache entry (" + value.getString() + ")" );
								
								it2.remove();
							}	
						}
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	protected DHTStorageAdapter
	getAdapter()
	{
		return( adapter );
	}
	
	protected void
	log(
		String	str )
	{
		logger.log( str );
	}
	
	public DHTDBStats
	getStats()
	{
		return( this );
	}
	
	public void
	print()
	{
		Map	count = new TreeMap();
		
		try{
			this_mon.enter();
			
			logger.log( "Stored keys = " + stored_values.size() + ", values = " + getValueDetails()[DHTDBStats.VD_VALUE_COUNT]); 

			Iterator	it = stored_values.entrySet().iterator();
			
			while( it.hasNext()){
						
				Map.Entry		entry = (Map.Entry)it.next();
				
				HashWrapper		value_key	= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping = (DHTDBMapping)entry.getValue();
				
				DHTDBValue[]	values = mapping.get(null,0,(byte)0);
					
				for (int i=0;i<values.length;i++){
					
					DHTDBValue	value = values[i];
					
					Integer key = new Integer( value.isLocal()?0:1);
					
					Object[]	data = (Object[])count.get( key );
									
					if ( data == null ){
						
						data = new Object[2];
						
						data[0] = new Integer(1);
						
						data[1] = "";
									
						count.put( key, data );
	
					}else{
						
						data[0] = new Integer(((Integer)data[0]).intValue() + 1 );
					}
				
					String	s = (String)data[1];
					
					s += (s.length()==0?"":", ") + "key=" + DHTLog.getString2(value_key.getHash()) + ",val=" + value.getString();
					
					data[1]	= s;
				}
			}
			
			it = count.keySet().iterator();
			
			while( it.hasNext()){
				
				Integer	k = (Integer)it.next();
				
				Object[]	data = (Object[])count.get(k);
				
				logger.log( "    " + k + " -> " + data[0] + " entries" ); // ": " + data[1]);
			}
			
			it = stored_values.entrySet().iterator();
			
			String	str 		= "    ";
			int		str_entries	= 0;
			
			while( it.hasNext()){
						
				Map.Entry		entry = (Map.Entry)it.next();
				
				HashWrapper		value_key	= (HashWrapper)entry.getKey();
				
				DHTDBMapping	mapping = (DHTDBMapping)entry.getValue();
				
				if ( str_entries == 16 ){
					
					logger.log( str );
					
					str = "    ";
					
					str_entries	= 0;
				}
				
				str_entries++;
				
				str += (str_entries==1?"":", ") + DHTLog.getString2(value_key.getHash()) + " -> " + mapping.getValueCount() + "/" + mapping.getHits()+"["+mapping.getLocalSize()+","+mapping.getDirectSize()+","+mapping.getIndirectSize() + "]";
			}
			
			if ( str_entries > 0 ){
				
				logger.log( str );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	banContact(
		final DHTTransportContact	contact,
		final String				reason )
	{
		new AEThread( "DHTDBImpl:delayed flood delete", true )
		{
			public void
			runSupport()
			{
					// delete their data on a separate thread so as not to 
					// interfere with the current action
				
				try{
					this_mon.enter();
					
					Iterator	it = stored_values.values().iterator();
												
					while( it.hasNext()){
						
						DHTDBMapping	mapping = (DHTDBMapping)it.next();

						Iterator	it2 = mapping.getDirectValues();
						
						while( it2.hasNext()){
							
							DHTDBValueImpl	val = (DHTDBValueImpl)it2.next();
							
							if ( !val.isLocal()){
								
								if ( Arrays.equals( val.getOriginator().getID(), contact.getID())){
									
									it.remove();
								}
							}
						}
					}

				}finally{
					
					this_mon.exit();
					
				}
			}
		}.start();
	
		logger.log( "Banning " + contact.getString() + " due to store flooding (" + reason + ")" );
		
		ip_filter.ban( 
				contact.getAddress().getAddress().getHostAddress(),
				"DHT: Sender stored excessive entries at this node (" + reason + ")", false );		
	}
	
	protected void
	incrementValueAdds(
		DHTTransportContact	contact )
	{
			// assume a node stores 1000 values at 20 (K) locations -> 20,000 values
			// assume a DHT size of 100,000 nodes
			// that is, on average, 1 value per 5 nodes
			// assume NAT of up to 30 ports per address
			// this gives 6 values per address
			// with a factor of 10 error this is still only 60 per address
		
		int	hit_count = ip_count_bloom_filter.add( contact.getAddress().getAddress().getAddress());
		
		if ( DHTLog.GLOBAL_BLOOM_TRACE ){
		
			System.out.println( "direct add from " + contact.getAddress() + ", hit count = " + hit_count );
		}

			// allow up to 10% bloom filter utilisation
		
		if ( ip_count_bloom_filter.getSize() / ip_count_bloom_filter.getEntryCount() < 10 ){
			
			rebuildIPBloomFilter( true );
		}
		
		if ( hit_count > 64 ){
			
			// obviously being spammed, drop all data originated by this IP and ban it
			
			banContact( contact, "global flood" );
		}
	}
	
	protected void
	decrementValueAdds(
		DHTTransportContact	contact )
	{
		int	hit_count = ip_count_bloom_filter.remove( contact.getAddress().getAddress().getAddress());

		if ( DHTLog.GLOBAL_BLOOM_TRACE ){
			
			System.out.println( "direct remove from " + contact.getAddress() + ", hit count = " + hit_count );
		}
	}

	protected void
	rebuildIPBloomFilter(
		boolean	increase_size )
	{
		BloomFilter	new_filter;
		
		if ( increase_size ){
			
			new_filter = BloomFilterFactory.createAddRemove8Bit( ip_count_bloom_filter.getSize() + IP_COUNT_BLOOM_SIZE_INCREASE_CHUNK );
			
		}else{
			
			new_filter = BloomFilterFactory.createAddRemove8Bit( ip_count_bloom_filter.getSize());
			
		}
		
		try{
			
			//Map		sender_map	= new HashMap();
			//List	senders		= new ArrayList();
			
			Iterator	it = stored_values.values().iterator();
			
			int	max_hits = 0;
			
			while( it.hasNext()){
				
				DHTDBMapping	mapping = (DHTDBMapping)it.next();

				mapping.rebuildIPBloomFilter( false );
				
				Iterator	it2 = mapping.getDirectValues();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	val = (DHTDBValueImpl)it2.next();
					
					if ( !val.isLocal()){
						
						// logger.log( "    adding " + val.getOriginator().getAddress());
						
						int	hits = new_filter.add( val.getOriginator().getAddress().getAddress().getAddress());
						
						if ( hits > max_hits ){
							
							max_hits = hits;
						}
					}
				}
				
					// survey our neighbourhood
				
				/*
				 * its is non-trivial to do anything about nodes that get "close" to us and then
				 * spam us with crap. Ultimately, of course, to take a key out you "just" create
				 * the 20 closest nodes to the key and then run nodes that swallow all registrations
				 * and return nothing.  
				 * Protecting against one or two such nodes that flood crap requires crap to be
				 * identified. Tracing shows a large disparity between number of values registered
				 * per neighbour (factors of 100), so an approach based on number of registrations
				 * is non-trivial (assuming future scaling of the DHT, what do we consider crap?)
				 * A further approach would be to query the claimed originators of values (obviously
				 * a low bandwith approach, e.g. query 3 values from the contact with highest number
				 * of forwarded values). This requires originators to support long term knowledge of
				 * what they've published (we don't want to blacklist a neighbour because an originator
				 * has deleted a value/been restarted). We also then have to consider how to deal with
				 * non-responses to queries (assuming an affirmative Yes -> value has been forwarded
				 * correnctly, No -> probably crap). We can't treat non-replies as No. Thus a bad
				 * neighbour only has to forward crap with originators that aren't AZ nodes (very
				 * easy to do!) to break this aproach. 
				 * 
				 * 
				it2 = mapping.getIndirectValues();
				
				while( it2.hasNext()){
					
					DHTDBValueImpl	val = (DHTDBValueImpl)it2.next();
					
					DHTTransportContact sender = val.getSender();
					
					HashWrapper	hw = new HashWrapper( sender.getID());
					
					Integer	sender_count = (Integer)sender_map.get( hw );
					
					if ( sender_count == null ){
						
						sender_count = new Integer(1);
						
						senders.add( sender );
						
					}else{
						
						sender_count = new Integer( sender_count.intValue() + 1 );						
					}
					
					sender_map.put( hw, sender_count );
				}	
				*/
			}
			
			logger.log( "Rebuilt global IP bloom filter, size = " + new_filter.getSize() + ", entries =" + new_filter.getEntryCount()+", max hits = " + max_hits );
				
			/*
			senders = control.sortContactsByDistance( senders );
			
			for (int i=0;i<senders.size();i++){
				
				DHTTransportContact	sender = (DHTTransportContact)senders.get(i);
				
				System.out.println( i + ":" + sender.getString() + " -> " + sender_map.get(new HashWrapper(sender.getID())));	
			}
			*/
			
		}finally{
			
			ip_count_bloom_filter	= new_filter;
		}
	}
	
	protected void
	reportSizes(
		String	op )
	{
		/*
		if ( !this_mon.isHeld()){
			
			Debug.out( "Monitor not held" );
		}
		
		int	actual_keys 	= stored_values.size();
		int	actual_values 	= 0;
		int actual_size		= 0;
		
		Iterator it = stored_values.values().iterator();
		
		while( it.hasNext()){
		
			DHTDBMapping	mapping = (DHTDBMapping)it.next();
			
			int	reported_size = mapping.getLocalSize() + mapping.getDirectSize() + mapping.getIndirectSize();
			
			actual_values += mapping.getValueCount();
			
			Iterator	it2 = mapping.getValues();
			
			int	sz = 0;
			
			while( it2.hasNext()){
				
				DHTDBValue	val = (DHTDBValue)it2.next();
				
				sz += val.getValue().length;
			}
			
			if ( sz != reported_size ){
				
				Debug.out( "Reported mapping size != actual: " + reported_size + "/" + sz );
			}
			
			actual_size += sz;
		}
		
		if ( actual_keys != total_keys ){
			
			Debug.out( "Actual keys != total: " + actual_keys + "/" + total_keys );
		}
		
		if ( actual_values != total_values ){
			
			Debug.out( "Actual values != total: " + actual_values + "/" + total_values );
		}
		
		if ( actual_size != total_size ){
			
			Debug.out( "Actual size != total: " + actual_size + "/" + total_size );
		}
		
		System.out.println( "DHTDB: " + op + " - keys=" + total_keys + ", values=" + total_values + ", size=" + total_size );
		*/
	}
	
	protected int
	getNextValueVersion()
	{
		try{
			this_mon.enter();
			
			if ( next_value_version_left == 0 ){
				
				next_value_version_left = VALUE_VERSION_CHUNK;
				
				if ( adapter == null ){
					
						// no persistent manager, just carry on incrementing
					
				}else{
					
					next_value_version = adapter.getNextValueVersions( VALUE_VERSION_CHUNK );
				}
				
				//System.out.println( "next chunk:" + next_value_version );
			}
			
			next_value_version_left--;
			
			int	res = next_value_version++;
			
			//System.out.println( "next value version = " + res );
			
			return( res  );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected class
	adapterFacade
		implements DHTStorageAdapter
	{
		private DHTStorageAdapter		delegate;
		
		protected
		adapterFacade(
			DHTStorageAdapter	_delegate )
		{
			delegate = _delegate;
		}
		
		public DHTStorageKey
		keyCreated(
			HashWrapper		key,
			boolean			local )
		{
				// report *before* incrementing as this occurs before the key is locally added
			
			reportSizes( "keyAdded" );
			
			total_keys++;
			
			return( delegate.keyCreated( key, local ));
		}
		
		public void
		keyDeleted(
			DHTStorageKey	adapter_key )
		{
			total_keys--;
			
			reportSizes( "keyDeleted" );
			
			delegate.keyDeleted( adapter_key );
		}
		
		public void
		keyRead(
			DHTStorageKey			adapter_key,
			DHTTransportContact		contact )
		{
			reportSizes( "keyRead" );
			
			delegate.keyRead( adapter_key, contact );
		}
		
		public DHTStorageKeyStats
		deserialiseStats(
			DataInputStream			is )
		
			throws IOException
		{
			return( delegate.deserialiseStats( is ));
		}
		
		public void
		valueAdded(
			DHTStorageKey		key,
			DHTTransportValue	value )
		{
			total_values++;
			total_size += value.getValue().length;
			
			reportSizes( "valueAdded");
			
			if ( !value.isLocal() ){
				
				DHTDBValueImpl	val = (DHTDBValueImpl)value;
				
				boolean	direct = Arrays.equals( value.getOriginator().getID(), val.getSender().getID());
				
				if ( direct ){
					
					incrementValueAdds( value.getOriginator());
				}
			}
				
			delegate.valueAdded( key, value );
		}
		
		public void
		valueUpdated(
			DHTStorageKey		key,
			DHTTransportValue	old_value,
			DHTTransportValue	new_value )
		{
			total_size += (new_value.getValue().length - old_value.getValue().length );
			
			reportSizes("valueUpdated");
			
			delegate.valueUpdated( key, old_value, new_value );
		}
		
		public void
		valueDeleted(
			DHTStorageKey		key,
			DHTTransportValue	value )
		{
			total_values--;
			total_size -= value.getValue().length;
		
			reportSizes("valueDeleted");
			
			if ( !value.isLocal() ){
				
				DHTDBValueImpl	val = (DHTDBValueImpl)value;
				
				boolean	direct = Arrays.equals( value.getOriginator().getID(), val.getSender().getID());
				
				if ( direct ){
					
					decrementValueAdds( value.getOriginator());
				}
			}

			delegate.valueDeleted( key, value );
		}
		
			// local lookup/put operations
		
		public boolean
		isDiversified(
			byte[]		key )
		{
			return( delegate.isDiversified( key ));
		}
		
		public byte[][]
		getExistingDiversification(
			byte[]			key,
			boolean			put_operation,
			boolean			exhaustive_get )
		{
			return( delegate.getExistingDiversification( key, put_operation, exhaustive_get ));
		}
		
		public byte[][]
		createNewDiversification(
			DHTTransportContact	cause,
			byte[]				key,
			boolean				put_operation,
			byte				diversification_type,
			boolean				exhaustive_get )
		{
			return( delegate.createNewDiversification( cause, key, put_operation, diversification_type, exhaustive_get ));
		}
		
		public int
		getNextValueVersions(
			int		num )
		{
			return( delegate.getNextValueVersions(num));
		}
		
		public DHTStorageBlock
		keyBlockRequest(
			DHTTransportContact		direct_sender,
			byte[]					request,
			byte[]					signature )
		{
			return( delegate.keyBlockRequest( direct_sender, request, signature ));
		}
		
		public DHTStorageBlock
		getKeyBlockDetails(
			byte[]		key )
		{
			return( delegate.getKeyBlockDetails(key));
		}
		
		public DHTStorageBlock[]
		getDirectKeyBlocks()
		{
			return( delegate.getDirectKeyBlocks());
		}
		
		public byte[]
    	getKeyForKeyBlock(
    		byte[]	request )
		{
			return( delegate.getKeyForKeyBlock( request ));
		}
		
		public void
		setStorageForKey(
			String	key,
			byte[]	data )
		{
			delegate.setStorageForKey( key, data );
		}
		
		public byte[]
		getStorageForKey(
			String	key )
		{
			return( delegate.getStorageForKey(key));
		}
	}
}
