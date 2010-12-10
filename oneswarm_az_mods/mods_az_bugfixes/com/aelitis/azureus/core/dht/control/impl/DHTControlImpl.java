/*
 * Created on 12-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

package com.aelitis.azureus.core.dht.control.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.core3.util.ThreadPoolTask;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationAdapter;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.DHTStorageBlock;
import com.aelitis.azureus.core.dht.control.DHTControl;
import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.control.DHTControlAdapter;
import com.aelitis.azureus.core.dht.control.DHTControlListener;
import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBFactory;
import com.aelitis.azureus.core.dht.db.DHTDBLookupResult;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;
import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPositionManager;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.router.DHTRouterAdapter;
import com.aelitis.azureus.core.dht.router.DHTRouterContact;
import com.aelitis.azureus.core.dht.router.DHTRouterFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFindValueReply;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportReplyHandlerAdapter;
import com.aelitis.azureus.core.dht.transport.DHTTransportRequestHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportStoreReply;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

/**
 * @author parg
 *
 */

public class
DHTControlImpl
	implements DHTControl, DHTTransportRequestHandler
{
	private static final int EXTERNAL_LOOKUP_CONCURRENCY	= 32; // orig 16
	private static final int EXTERNAL_PUT_CONCURRENCY		= 8;

	private static final int RANDOM_QUERY_PERIOD			= 5*60*1000;

	private static final int INTEGRATION_TIME_MAX			= 15*1000;


	private final DHTControlAdapter		adapter;
	private final DHTTransport			transport;
	private DHTTransportContact		local_contact;

	private DHTRouter		router;

	private final DHTDB			database;

	private final DHTControlStatsImpl	stats;

	private final DHTLogger	logger;

	private final	int			node_id_byte_count;
	private final int			search_concurrency;
	private final int			lookup_concurrency;
	private final int			cache_at_closest_n;
	private final int			K;
	private final int			B;
	private final int			max_rep_per_node;

	private long		router_start_time;
	private int			router_count;

	private final ThreadPool	internal_lookup_pool;
	private final ThreadPool	external_lookup_pool;
	private final ThreadPool	internal_put_pool;
	private final ThreadPool	external_put_pool;

	private final Map			imported_state	= new HashMap();

	private long		last_lookup;


	private final ListenerManager	listeners 	= ListenerManager.createAsyncManager(
			"DHTControl:listenDispatcher",
			new ListenerManagerDispatcher()
			{
				@Override
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DHTControlListener	target = (DHTControlListener)_listener;

					target.activityChanged((DHTControlActivity)value, type );
				}
			});

	private final List		activities		= new ArrayList();
	private final AEMonitor	activity_mon	= new AEMonitor( "DHTControl:activities" );

	protected AEMonitor	estimate_mon		= new AEMonitor( "DHTControl:estimate" );
	private long		last_dht_estimate_time;
	private long		local_dht_estimate;
	private long		combined_dht_estimate;

	private static final int	LOCAL_ESTIMATE_HISTORY	= 32;

	private final Map	local_estimate_values =
		new LinkedHashMap(LOCAL_ESTIMATE_HISTORY,0.75f,true)
		{
			@Override
			protected boolean
			removeEldestEntry(
		   		Map.Entry eldest)
			{
				return( size() > LOCAL_ESTIMATE_HISTORY );
			}
		};

	private static final int	REMOTE_ESTIMATE_HISTORY	= 128;

	private final List	remote_estimate_values = new LinkedList();

	protected AEMonitor	spoof_mon		= new AEMonitor( "DHTControl:spoof" );

	private Cipher 			spoof_cipher;
	private SecretKey		spoof_key;

	public
	DHTControlImpl(
		DHTControlAdapter	_adapter,
		DHTTransport		_transport,
		int					_K,
		int					_B,
		int					_max_rep_per_node,
		int					_search_concurrency,
		int					_lookup_concurrency,
		int					_original_republish_interval,
		int					_cache_republish_interval,
		int					_cache_at_closest_n,
		DHTLogger 			_logger )
	{
		adapter		= _adapter;
		transport	= _transport;
		logger		= _logger;

		K								= _K;
		B								= _B;
		max_rep_per_node				= _max_rep_per_node;
		search_concurrency				= _search_concurrency;
		lookup_concurrency				= _lookup_concurrency;
		cache_at_closest_n				= _cache_at_closest_n;

			// set this so we don't do initial calculation until reasonably populated

		last_dht_estimate_time	= SystemTime.getCurrentTime();

		database = DHTDBFactory.create(
						adapter.getStorageAdapter(),
						_original_republish_interval,
						_cache_republish_interval,
						logger );

		internal_lookup_pool 	= new ThreadPool("DHTControl:internallookups", lookup_concurrency );
		internal_put_pool 		= new ThreadPool("DHTControl:internalputs", lookup_concurrency );

			// external pools queue when full ( as opposed to blocking )

		external_lookup_pool 	= new ThreadPool("DHTControl:externallookups", EXTERNAL_LOOKUP_CONCURRENCY, true );
		external_put_pool 		= new ThreadPool("DHTControl:puts", EXTERNAL_PUT_CONCURRENCY, true );

		createRouter( transport.getLocalContact());

		node_id_byte_count	= router.getID().length;

		stats = new DHTControlStatsImpl( this );

			// don't bother computing anti-spoof stuff if we don't support value storage

		if ( transport.supportsStorage()){

			try{
				spoof_cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");

				KeyGenerator keyGen = KeyGenerator.getInstance("DESede");

				spoof_key = keyGen.generateKey();

			}catch( Throwable e ){

				Debug.printStackTrace( e );

				logger.log( e );
			}
		}

		transport.setRequestHandler( this );

		transport.addListener(
			new DHTTransportListener()
			{
				public void
				localContactChanged(
					DHTTransportContact	new_local_contact )
				{
					logger.log( "Transport ID changed, recreating router" );

					List	old_contacts = router.findBestContacts( 0 );

					byte[]	old_router_id = router.getID();

					createRouter( new_local_contact );

						// sort for closeness to new router id

					Set	sorted_contacts = new sortedTransportContactSet( router.getID(), true ).getSet();

					for (int i=0;i<old_contacts.size();i++){

						DHTRouterContact	contact = (DHTRouterContact)old_contacts.get(i);

						if ( !Arrays.equals( old_router_id, contact.getID())){

							if ( contact.isAlive()){

								DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getTransportContact();

								sorted_contacts.add( t_contact );
							}
						}
					}

						// fill up with non-alive ones to lower limit in case this is a start-of-day
						// router change and we only have imported contacts in limbo state

					for (int i=0;sorted_contacts.size() < 32 && i<old_contacts.size();i++){

						DHTRouterContact	contact = (DHTRouterContact)old_contacts.get(i);

						if ( !Arrays.equals( old_router_id, contact.getID())){

							if ( !contact.isAlive()){

								DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getTransportContact();

								sorted_contacts.add( t_contact );
							}
						}
					}

					Iterator	it = sorted_contacts.iterator();

					int	added = 0;

						// don't add them all otherwise we can skew the smallest-subtree. better
						// to seed with some close ones and then let the normal seeding process
						// populate it correctly

					while( it.hasNext() && added < 128 ){

						DHTTransportContact	contact = (DHTTransportContact)it.next();

						router.contactAlive( contact.getID(), new DHTControlContactImpl( contact ));

						added++;
					}

					seed( false );
				}

				public void
				currentAddress(
					String		address )
				{
				}

				public void
				reachabilityChanged(
					boolean	reacheable )
				{
				}
			});
	}

	protected void
	createRouter(
		DHTTransportContact		_local_contact)
	{
		router_start_time	= SystemTime.getCurrentTime();
		router_count++;

		local_contact	= _local_contact;

		if ( router != null ){

			router.destroy();
		}

		router	= DHTRouterFactory.create(
					K, B, max_rep_per_node,
					local_contact.getID(),
					new DHTControlContactImpl( local_contact ),
					logger);

		router.setAdapter(
			new DHTRouterAdapter()
			{
				public void
				requestPing(
					DHTRouterContact	contact )
				{
					DHTControlImpl.this.requestPing( contact );
				}

				public void
				requestLookup(
					byte[]		id,
					String		description )
				{
					lookup( internal_lookup_pool, false,
							id,
							description,
							(byte)0,
							false,
							0,
							search_concurrency,
							1,
							router.getK(),	// (parg - removed this) decrease search accuracy for refreshes
							new lookupResultHandler(new DHTOperationAdapter())
							{
								@Override
								public void
								diversify(
									DHTTransportContact	cause,
									byte				diversification_type )
								{
								}

								@Override
								public void
								closest(
									List		res )
								{
								}
							});
				}

				public void
				requestAdd(
					DHTRouterContact	contact )
				{
					nodeAddedToRouter( contact );
				}
			});

		database.setControl( this );
	}

	public long
	getRouterUptime()
	{
		long	now = SystemTime.getCurrentTime();

		if ( now < router_start_time ){

			router_start_time	= now;
		}

		return(  now - router_start_time );
	}

	public int
	getRouterCount()
	{
		return( router_count );
	}

	public DHTControlStats
	getStats()
	{
		return( stats );
	}

	public DHTTransport
	getTransport()
	{
		return( transport );
	}

	public DHTRouter
	getRouter()
	{
		return( router );
	}

	public DHTDB
	getDataBase()
	{
		return( database );
	}

	public void
	contactImported(
		DHTTransportContact	contact )
	{
		router.contactKnown( contact.getID(), new DHTControlContactImpl(contact));
	}

	public void
	contactRemoved(
		DHTTransportContact	contact )
	{
			// obviously we don't want to remove ourselves

		if ( !router.isID( contact.getID())){

			router.contactDead( contact.getID(), true );
		}
	}

	public void
	exportState(
		DataOutputStream	daos,
		int					max )

		throws IOException
	{
			/*
			 * We need to be a bit smart about exporting state to deal with the situation where a
			 * DHT is started (with good import state) and then stopped before the goodness of the
			 * state can be re-established. So we remember what we imported and take account of this
			 * on a re-export
			 */

			// get all the contacts

		List	contacts = router.findBestContacts( 0 );

			// give priority to any that were alive before and are alive now

		List	to_save 	= new ArrayList();
		List	reserves	= new ArrayList();

		//System.out.println( "Exporting" );

		for (int i=0;i<contacts.size();i++){

			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);

			Object[]	imported = (Object[])imported_state.get( new HashWrapper( contact.getID()));

			if ( imported != null ){

				if ( contact.isAlive()){

						// definitely want to keep this one

					to_save.add( contact );

				}else if ( !contact.isFailing()){

						// dunno if its still good or not, however its got to be better than any
						// new ones that we didn't import who aren't known to be alive

					reserves.add( contact );
				}
			}
		}

		//System.out.println( "    initial to_save = " + to_save.size() + ", reserves = " + reserves.size());

			// now pull out any live ones

		for (int i=0;i<contacts.size();i++){

			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);

			if ( contact.isAlive() && !to_save.contains( contact )){

				to_save.add( contact );
			}
		}

		//System.out.println( "    after adding live ones = " + to_save.size());

			// now add any reserve ones

		for (int i=0;i<reserves.size();i++){

			DHTRouterContact	contact = (DHTRouterContact)reserves.get(i);

			if ( !to_save.contains( contact )){

				to_save.add( contact );
			}
		}

		//System.out.println( "    after adding reserves = " + to_save.size());

			// now add in the rest!

		for (int i=0;i<contacts.size();i++){

			DHTRouterContact	contact = (DHTRouterContact)contacts.get(i);

			if (!to_save.contains( contact )){

				to_save.add( contact );
			}
		}

			// and finally remove the invalid ones

		Iterator	it = to_save.iterator();

		while( it.hasNext()){

			DHTRouterContact	contact	= (DHTRouterContact)it.next();

			DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getTransportContact();

			if ( !t_contact.isValid()){

				it.remove();
			}
		}

		//System.out.println( "    finally = " + to_save.size());

		int	num_to_write = Math.min( max, to_save.size());

		daos.writeInt( num_to_write );

		for (int i=0;i<num_to_write;i++){

			DHTRouterContact	contact = (DHTRouterContact)to_save.get(i);

			//System.out.println( "export:" + contact.getString());

			daos.writeLong( contact.getTimeAlive());

			DHTTransportContact	t_contact = ((DHTControlContactImpl)contact.getAttachment()).getTransportContact();

			try{

				t_contact.exportContact( daos );

			}catch( DHTTransportException e ){

					// shouldn't fail as for a contact to make it to the router
					// it should be valid...

				Debug.printStackTrace( e );

				throw( new IOException( e.getMessage()));
			}
		}

		daos.flush();
	}

	public void
	importState(
		DataInputStream		dais )

		throws IOException
	{
		int	num = dais.readInt();

		for (int i=0;i<num;i++){

			try{

				long	time_alive = dais.readLong();

				DHTTransportContact	contact = transport.importContact( dais );

				imported_state.put( new HashWrapper( contact.getID()), new Object[]{ new Long( time_alive ), contact });

			}catch( DHTTransportException e ){

				Debug.printStackTrace( e );
			}
		}
	}

	public void
	seed(
		final boolean		full_wait )
	{
		final AESemaphore	sem = new AESemaphore( "DHTControl:seed" );

		lookup( internal_lookup_pool, false,
				router.getID(),
				"Seeding DHT",
				(byte)0,
				false,
				0,
				search_concurrency*4,
				1,
				router.getK(),
				new lookupResultHandler(new DHTOperationAdapter())
				{
					@Override
					public void
					diversify(
						DHTTransportContact	cause,
						byte				diversification_type )
					{
					}

					@Override
					public void
					closest(
						List		res )
					{
						if ( !full_wait ){

							sem.release();
						}

						try{

							router.seed();

						}finally{

							if ( full_wait ){

								sem.release();
							}
						}
					}
				});

			// we always wait at least a minimum amount of time before returning

		long	start = SystemTime.getCurrentTime();

		sem.reserve( INTEGRATION_TIME_MAX );

		long	now = SystemTime.getCurrentTime();

		if ( now < start ){

			start	= now;
		}

		long	remaining = INTEGRATION_TIME_MAX - ( now - start );

		if ( remaining > 500 && !full_wait ){

			logger.log( "Initial integration completed, waiting " + remaining + " ms for second phase to start" );

			try{
				Thread.sleep( remaining );

			}catch( Throwable e ){

				Debug.out(e);
			}
		}
	}

	protected void
	poke()
	{
		long	now = SystemTime.getCurrentTime();

		if ( 	now < last_lookup ||
				now - last_lookup > RANDOM_QUERY_PERIOD ){

			last_lookup	= now;

				// we don't want this to be blocking as it'll stuff the stats

			external_lookup_pool.run(
				new task(external_lookup_pool)
				{
					private byte[]	target = {};

					@Override
					public void
					runSupport()
					{
						target = router.refreshRandom();
					}

					@Override
					public byte[]
					getTarget()
					{
						return( target );
					}

					@Override
					public String
					getDescription()
					{
						return( "Random Query" );
					}
				});
		}
	}

	public void
	put(
		byte[]					_unencoded_key,
		String					_description,
		byte[]					_value,
		byte					_flags,
		DHTOperationListener	_listener )
	{
			// public entry point for explicit publishes

		if ( _value.length == 0 ){

				// zero length denotes value removal

			throw( new RuntimeException( "zero length values not supported"));
		}

		byte[]	encoded_key = encodeKey( _unencoded_key );

		DHTLog.log( "put for " + DHTLog.getString( encoded_key ));

		DHTDBValue	value = database.store( new HashWrapper( encoded_key ), _value, _flags );

		put( 	external_put_pool,
				encoded_key,
				_description,
				value,
				0,
				true,
				new HashSet(),
				_listener instanceof DHTOperationListenerDemuxer?
						(DHTOperationListenerDemuxer)_listener:
						new DHTOperationListenerDemuxer(_listener));
	}

	public void
	putEncodedKey(
		byte[]				encoded_key,
		String				description,
		DHTTransportValue	value,
		long				timeout,
		boolean				original_mappings )
	{
		put( 	internal_put_pool,
				encoded_key,
				description,
				value,
				timeout,
				original_mappings,
				new HashSet(),
				new DHTOperationListenerDemuxer( new DHTOperationAdapter()));
	}


	protected void
	put(
		ThreadPool					thread_pool,
		byte[]						initial_encoded_key,
		String						description,
		DHTTransportValue			value,
		long						timeout,
		boolean						original_mappings,
		Set							keys_written,
		DHTOperationListenerDemuxer	listener )
	{
		put( 	thread_pool,
				initial_encoded_key,
				description,
				new DHTTransportValue[]{ value },
				timeout,
				original_mappings,
				keys_written,
				listener );
	}

	protected void
	put(
		final ThreadPool					thread_pool,
		final byte[]						initial_encoded_key,
		final String						description,
		final DHTTransportValue[]			values,
		final long							timeout,
		final boolean						original_mappings,
		final Set							keys_written,
		final DHTOperationListenerDemuxer	listener )
	{

			// get the initial starting point for the put - may have previously been diversified

		byte[][]	encoded_keys	=
			adapter.diversify(
					null,
					true,
					true,
					initial_encoded_key,
					DHT.DT_NONE,
					original_mappings );

			// may be > 1 if diversification is replicating (for load balancing)

		for (int i=0;i<encoded_keys.length;i++){

			final byte[]	encoded_key	= encoded_keys[i];

			HashWrapper	hw = new HashWrapper( encoded_key );

			if ( keys_written.contains( hw )){

				// System.out.println( "put: skipping key as already written" );

				continue;
			}

			keys_written.add( hw );

			final String	this_description =
				Arrays.equals( encoded_key, initial_encoded_key )?
						description:
						("Diversification of [" + description + "]" );

			lookup( thread_pool, false,
					encoded_key,
					this_description,
					(byte)0,
					false,
					timeout,
					search_concurrency,
					1,
					router.getK(),
					new lookupResultHandler(listener)
					{
						@Override
						public void
						diversify(
							DHTTransportContact	cause,
							byte				diversification_type )
						{
							Debug.out( "Shouldn't get a diversify on a lookup-node" );
						}

						@Override
						public void
						closest(
							List				_closest )
						{
							put( 	thread_pool,
									new byte[][]{ encoded_key },
									"Store of [" + this_description + "]",
									new DHTTransportValue[][]{ values },
									_closest,
									timeout,
									listener,
									true,
									keys_written );
						}
					});
		}
	}

	public void
	putDirectEncodedKeys(
		byte[][]				encoded_keys,
		String					description,
		DHTTransportValue[][]	value_sets,
		List					contacts )
	{
			// we don't consider diversification for direct puts (these are for republishing
			// of cached mappings and we maintain these as normal - its up to the original
			// publisher to diversify as required)

		put( 	internal_put_pool,
				encoded_keys,
				description,
				value_sets,
				contacts,
				0,
				new DHTOperationListenerDemuxer( new DHTOperationAdapter()),
				false,
				new HashSet());
	}

	protected void
	put(
		final ThreadPool						thread_pool,
		byte[][]								initial_encoded_keys,
		final String							description,
		final DHTTransportValue[][]				initial_value_sets,
		final List								contacts,
		final long								timeout,
		final DHTOperationListenerDemuxer		listener,
		final boolean							consider_diversification,
		final Set								keys_written )
	{
		boolean[]	ok = new boolean[initial_encoded_keys.length];
		int	failed = 0;

		for (int i=0;i<initial_encoded_keys.length;i++){

			if ( ! (ok[i] = !database.isKeyBlocked( initial_encoded_keys[i]))){

				failed++;
			}
		}

			// if all failed then nothing to do

		if ( failed == ok.length ){

			listener.incrementCompletes();

			listener.complete( false );

			return;
		}

		final byte[][] 				encoded_keys 	= failed==0?initial_encoded_keys:new byte[ok.length-failed][];
		final DHTTransportValue[][] value_sets 		= failed==0?initial_value_sets:new DHTTransportValue[ok.length-failed][];

		if ( failed > 0 ){

			int	pos = 0;

			for (int i=0;i<ok.length;i++){

				if ( ok[i] ){

					encoded_keys[ pos ] = initial_encoded_keys[i];
					value_sets[ pos ] 	= initial_value_sets[i];

					pos++;
				}
			}
		}

			// only diversify on one hit as we're storing at closest 'n' so we only need to
			// do it once for each key

		final boolean[]	diversified = new boolean[encoded_keys.length];

		for (int i=0;i<contacts.size();i++){

			DHTTransportContact	contact = (DHTTransportContact)contacts.get(i);

			if ( router.isID( contact.getID())){

					// don't send to ourselves!

			}else{

				try{

					for (int j=0;j<value_sets.length;j++){

						for (int k=0;k<value_sets[j].length;k++){

							listener.wrote( contact, value_sets[j][k] );
						}
					}

						// each store is going to report its complete event

					listener.incrementCompletes();

					contact.sendStore(
						new DHTTransportReplyHandlerAdapter()
						{
							@Override
							public void
							storeReply(
								DHTTransportContact _contact,
								byte[]				_diversifications )
							{
								try{
									DHTLog.log( "Store OK " + DHTLog.getString( _contact ));

									router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));

										// can be null for old protocol versions

									if ( consider_diversification && _diversifications != null ){

										for (int j=0;j<_diversifications.length;j++){

											if ( _diversifications[j] != DHT.DT_NONE && !diversified[j] ){

												diversified[j]	= true;

												byte[][]	diversified_keys =
													adapter.diversify( _contact, true, false, encoded_keys[j], _diversifications[j], false );

												for (int k=0;k<diversified_keys.length;k++){

													put( 	thread_pool,
															diversified_keys[k],
															"Diversification of [" + description + "]",
															value_sets[j],
															timeout,
															false,
															keys_written,
															listener );
												}
											}
										}
									}
								}finally{

									listener.complete( false );
								}
							}

							public void
							failed(
								DHTTransportContact 	_contact,
								Throwable 				_error )
							{
								try{
									DHTLog.log( "Store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());

									router.contactDead( _contact.getID(), false );

								}finally{

									listener.complete( true );
								}
							}

							@Override
							public void
							keyBlockRequest(
								DHTTransportContact		contact,
								byte[]					request,
								byte[]					key_signature )
							{
								DHTStorageBlock	key_block = database.keyBlockRequest( null, request, key_signature );

								if ( key_block != null ){

										// remove this key for any subsequent publishes. Quickest hack
										// is to change it into a random key value - this will be rejected
										// by the recipient as not being close enough anyway

									for (int i=0;i<encoded_keys.length;i++){

										if ( Arrays.equals( encoded_keys[i], key_block.getKey())){

											byte[]	dummy = new byte[encoded_keys[i].length];

											new Random().nextBytes( dummy );

											encoded_keys[i] = dummy;
										}
									}
								}
							}
						},
						encoded_keys,
						value_sets );

				}catch( Throwable e ){

					Debug.printStackTrace(e);

				}
			}
		}
	}

	public DHTTransportValue
	getLocalValue(
		byte[]		unencoded_key )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "getLocalValue for " + DHTLog.getString( encoded_key ));

		DHTDBValue	res = database.get( new HashWrapper( encoded_key ));

		if ( res == null ){

			return( null );
		}

		return( res );
	}

	public void
	get(
		byte[]						unencoded_key,
		String						description,
		byte						flags,
		int							max_values,
		long						timeout,
		boolean						exhaustive,
		boolean						high_priority,
		final DHTOperationListener	get_listener )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "get for " + DHTLog.getString( encoded_key ));

		getSupport( encoded_key, description, flags, max_values, timeout, exhaustive, high_priority, new DHTOperationListenerDemuxer( get_listener ));
	}

	public boolean
	isDiversified(
		byte[]		unencoded_key )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		return( adapter.isDiversified( encoded_key ));
	}

	public boolean
   	lookup(
   		byte[]							unencoded_key,
   		long							timeout,
   		final DHTOperationListener		lookup_listener )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "lookup for " + DHTLog.getString( encoded_key ));

		final AESemaphore	sem = new AESemaphore( "DHTControl:lookup" );

		final	boolean[]	diversified = { false };

		DHTOperationListener	delegate =
			new DHTOperationListener()
			{
				public void
				searching(
					DHTTransportContact	contact,
					int					level,
					int					active_searches )
				{
					lookup_listener.searching( contact, level, active_searches );
				}

				public void
				found(
					DHTTransportContact	contact )
				{
				}

				public void
				diversified()
				{
					lookup_listener.diversified();
				}

				public void
				read(
					DHTTransportContact	contact,
					DHTTransportValue	value )
				{
				}

				public void
				wrote(
					DHTTransportContact	contact,
					DHTTransportValue	value )
				{
				}

				public void
				complete(
					boolean				timeout )
				{
					lookup_listener.complete( timeout );

					sem.release();
				}
			};

		lookup( 	external_lookup_pool, false,
					encoded_key,
					"lookup",
					(byte)0,
					false,
					timeout,
					search_concurrency,
					1,
					router.getK(),
					new lookupResultHandler( delegate )
					{
						@Override
						public void
						diversify(
							DHTTransportContact	cause,
							byte				diversification_type )
						{
							diversified();

							diversified[0] = true;
						}

						@Override
						public void
						closest(
							List	closest )
						{
							for (int i=0;i<closest.size();i++){

								lookup_listener.found((DHTTransportContact)closest.get(i));
							}
						}
					});

		sem.reserve();

		return( diversified[0] );
	}

	protected void
	getSupport(
		final byte[]						initial_encoded_key,
		final String						description,
		final byte							flags,
		final int							max_values,
		final long							timeout,
		final boolean						exhaustive,
		final boolean						high_priority,
		final DHTOperationListenerDemuxer	get_listener )
	{
			// get the initial starting point for the get - may have previously been diversified

		byte[][]	encoded_keys	= adapter.diversify( null, false, true, initial_encoded_key, DHT.DT_NONE, exhaustive );

		for (int i=0;i<encoded_keys.length;i++){

			final boolean[]	diversified = { false };

			final byte[]	encoded_key	= encoded_keys[i];

			boolean	div = !Arrays.equals( encoded_key, initial_encoded_key );

			if ( div ){

				get_listener.diversified();
			}

			final String	this_description =
				div?("Diversification of [" + description + "]" ):description;

			lookup( external_lookup_pool,
					high_priority,
					encoded_key,
					this_description,
					flags,
					true,
					timeout,
					search_concurrency,
					max_values,
					router.getK(),
					new lookupResultHandler( get_listener )
					{
						private final List	found_values	= new ArrayList();

						@Override
						public void
						diversify(
							DHTTransportContact	cause,
							byte				diversification_type )
						{
							diversified();

								// we only want to follow one diversification

							if ( !diversified[0]){

								diversified[0] = true;

								int	rem = max_values==0?0:( max_values - found_values.size());

								if ( max_values == 0 || rem > 0 ){

									byte[][]	diversified_keys = adapter.diversify( cause, false, false, encoded_key, diversification_type, exhaustive );

										// should return a max of 1 (0 if diversification refused)
										// however, could change one day to search > 1

									for (int j=0;j<diversified_keys.length;j++){

										getSupport( diversified_keys[j], "Diversification of [" + this_description + "]", flags, rem,  timeout, exhaustive, high_priority, get_listener );
									}
								}
							}
						}

						@Override
						public void
						read(
							DHTTransportContact	contact,
							DHTTransportValue	value )
						{
							found_values.add( value );

							super.read( contact, value );
						}

						@Override
						public void
						closest(
							List	closest )
						{
							/* we don't use teh cache-at-closest kad feature
							if ( found_values.size() > 0 ){

								DHTTransportValue[]	values = new DHTTransportValue[found_values.size()];

								found_values.toArray( values );

									// cache the values at the 'n' closest seen locations

								for (int k=0;k<Math.min(cache_at_closest_n,closest.size());k++){

									DHTTransportContact	contact = (DHTTransportContact)(DHTTransportContact)closest.get(k);

									for (int j=0;j<values.length;j++){

										wrote( contact, values[j] );
									}

									contact.sendStore(
											new DHTTransportReplyHandlerAdapter()
											{
												public void
												storeReply(
													DHTTransportContact _contact,
													byte[]				_diversifications )
												{
														// don't consider diversification for cache stores as we're not that
														// bothered

													DHTLog.log( "Cache store OK " + DHTLog.getString( _contact ));

													router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
												}

												public void
												failed(
													DHTTransportContact 	_contact,
													Throwable 				_error )
												{
													DHTLog.log( "Cache store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());

													router.contactDead( _contact.getID(), false );
												}
											},
											new byte[][]{ encoded_key },
											new DHTTransportValue[][]{ values });
								}
							}
							*/
						}
					});
		}
	}

	public byte[]
	remove(
		byte[]					unencoded_key,
		String					description,
		DHTOperationListener	listener )
	{
		final byte[]	encoded_key = encodeKey( unencoded_key );

		DHTLog.log( "remove for " + DHTLog.getString( encoded_key ));

		DHTDBValue	res = database.remove( local_contact, new HashWrapper( encoded_key ));

		if ( res == null ){

				// not found locally, nothing to do

			return( null );

		}else{

				// we remove a key by pushing it back out again with zero length value

			put( 	external_put_pool,
					encoded_key,
					description,
					res,
					0,
					true,
					new HashSet(),
					new DHTOperationListenerDemuxer( listener ));

			return( res.getValue());
		}
	}

		/**
		 * The lookup method returns up to K closest nodes to the target
		 * @param lookup_id
		 * @return
		 */

	protected void
	lookup(
		ThreadPool					thread_pool,
		boolean						high_priority,
		final byte[]				lookup_id,
		final String				description,
		final byte					flags,
		final boolean				value_search,
		final long					timeout,
		final int					concurrency,
		final int					max_values,
		final int					search_accuracy,
		final lookupResultHandler	handler )
	{
		thread_pool.run(
			new task(thread_pool)
			{
				@Override
				public void
				runSupport()
				{
					try{
						lookupSupportSync( lookup_id, flags, value_search, timeout, concurrency, max_values, search_accuracy, handler );

					}catch( Throwable e ){

						Debug.printStackTrace(e);
					}
				}

				@Override
				public byte[]
				getTarget()
				{
					return( lookup_id );
				}

				@Override
				public String
				getDescription()
				{
					return( description );
				}
			}, high_priority );
	}

	public double calculateBudgetInUse(List l){
		double res = 0;
		long now = System.currentTimeMillis();
		for(int where = 0; where < l.size(); where++){
			res += probOfResponse(now, (DHTTransportReplyHandlerAdapter)l.get(where));
		}
		return res;
	}

	public double calculateProbOfActiveSearchesBeingCloserThanNode(List active, Set closest, byte[] lookup_id){
		double res = 0;
		long now = System.currentTimeMillis();

		if(closest.size() == 0)
			return 100;
		DHTTransportContact dtc = (DHTTransportContact) closest.iterator().next();

		for(int where = 0; where < active.size(); where++){
			DHTTransportReplyHandlerAdapter adapt = ((DHTTransportReplyHandlerAdapter)active.get(where));
			byte[] activeID = adapt.getTgt().getID();
			//is it closer than anyone else?
			boolean closer = false;
			List close = new ArrayList(closest);
			//for(int c = 0; c < close.size(); c++){
			//	DHTTransportContact dtc = (DHTTransportContact) close.get(c);
				int distance = computeAndCompareDistances(dtc.getID(), activeID, lookup_id);
				if (distance > 0) {
					//logger.log(DHTLog.getString(activeID) + " is closer than " + DHTLog.getFullString(dtc.getID()) + " to " +DHTLog.getFullString(lookup_id) );
					closer = true;
				}
			//}
			if(closer){
				res+=probOfResponse(now, adapt);
			}
		}

		/*
		for(int where = 0; where < active.size(); where++){
			DHTTransportReplyHandlerAdapter adapt = ((DHTTransportReplyHandlerAdapter)active.get(where));
			DHTTransportContact tg = adapt.tgt;

			int distance = computeAndCompareDistances(node.getID(), tg.getID(), lookup_id);
			if (distance > 0) {
				res += probOfResponse(now, adapt);
			}
		}*/
		return res;
	}

	private static final double [] respProbTable = {
		0.5305003758504375,
		0.5248907429686456,
		0.5064232752978594,
		0.47965272461186714,
		0.45212130681374313,
		0.4251841319938128,
		0.4003241514720664,
		0.37852486081381603,
		0.35924434097707225,
		0.34220312589514035,
		0.32732455249493514,
		0.31433238372506844,
		0.3029768743479038,
		0.29291755421186855,
		0.2838894357382553,
		0.27571592998244876,
		0.2681279870561623,
		0.26101165396760917,
		0.25426575490385,
		0.24785987462393255,
		0.2417096420827496,
		0.2357991205273364,
		0.2300118918036828,
		0.22443274360403245,
		0.2189934846804056,
		0.21367588672058774,
		0.20847227647315142,
		0.20334703060794027,
		0.1983461886335189,
		0.19345732057698684,
		0.1886408449603925,
		0.1838896093412288,
		0.17922180731111562,
		0.17463743887005298,
		0.17010969971893677,
		0.16562963194433594,
		0.16122789383136554,
		0.1568792435076828,
		0.1526446156169763,
		0.148429500700488,
		0.14429313209276648,
		0.14022089242344543,
		0.13620229607292714,
		0.13221894112602361,
		0.12828593104142688,
		0.12442427177892702,
		0.12062597760174436,
		0.11688632650900045,
		0.11322788688724661,
		0.1096367357780104,
		0.10610492216510685,
		0.10264515378619404,
		0.09923583443137214,
		0.09592883666911446,
		0.09270666131969509,
		0.08952486602190522,
		0.08642233784180256,
		0.08337654311343044,
		0.08038571108645949,
		0.07744709883390882,
		0.07458389971303428,
		0.07179548875313137,
		0.06910092756068725,
		0.06645119065599356,
		0.0638981853281183,
		0.06143284950184627,
		0.059061294001843664,
		0.05677716495928142,
		0.054552963663161626,
		0.05242382735531494,
		0.05035430630855845,
		0.048376586514173814,
		0.04644692002284615,
		0.04459992326748573,
		0.04281028493456036,
		0.04110723976480269,
		0.03947464268167985,
		0.03791065349367304,
		0.03640766839054419,
		0.03496870806403172,
		0.033584016027026514,
		0.03223918323273041,
		0.030950424198865678,
		0.029733849281370493,
		0.028556439194690527,
		0.0274530881364936,
		0.02638286052563508,
		0.025376241044256606,
		0.024401703392375705,
		0.023455844951712334,
		0.02256574778612808,
		0.021712280847946363,
		0.02089766625522762,
		0.020107911608310026,
		0.019349787423158984,
		0.01863936933511797,
		0.017968706328001988,
		0.017326132281993725,
		0.016715292859536098,
		0.01613170910391353,
		0.015584408369746557,
		0.01506616877333875,
		0.01456924762207327,
		0.014101352887972264,
		0.013660158291191212,
		0.013240941830851678,
		0.012861376289653089,
		0.012494553206707325,
		0.012151583172316575,
		0.01182621647943586,
		0.01151727262784557,
		0.011225862676575925,
		0.01095188246384284,
		0.010682936737340437,
		0.010419511585394435,
		0.010175529966477265,
		0.009943040864403925,
		0.009721488749659304,
		0.009506255783149054,
		0.009299876568285861,
		0.00910356632588403,
		0.008915623746803535,
		0.008735909948665601,
		0.008563383313629396,
		0.008396585576717758,
		0.008237669414801736,
		0.008083683577332313,
		0.00794528728688065,
		0.00781227268860661,
		0.007677001251677439,
		0.00754846561011897,
		0.007421665998295219,
		0.0073029215644405555,
		0.0071908087641725125,
		0.007079425096393051,
		0.006977415989181062,
		0.00687672626456746,
		0.0067793697170445135,
		0.0066857282731538625,
		0.0065929895647252644,
		0.0064976120910998966,
		0.006412164707557111,
		0.0063288005596959865,
		0.006247623809300606,
		0.0061678358826929985,
		0.006090478397713996,
		0.00601805123718159,
		0.0059443741352401885,
		0.005870037341999593,
		0.005795665828164305,
		0.005726467682938473,
		0.005660394391235133,
		0.005597376511864895,
		0.005537171000664899,
		0.005481548607964557,
		0.005428877465813235,
		0.005373324514302281,
		0.005320965857503208,
		0.005267635024052692,
		0.005215797176174033,
		0.005165660637435398,
		0.005115176892749819,
		0.005066255574825485,
		0.005020632713397115,
		0.004978447190843485,
		0.004937928256835183,
		0.004898763426019961,
		0.00485873058033738,
		0.004817100587298859,
		0.004776546932695863,
		0.004736409925229199,
		0.004698460315228279,
		0.004660337102253887,
		0.004623949919014213,
		0.004590513986323558,
		0.004557737744932095,
		0.004526836415654128,
		0.00449617813053902,
		0.004463887977473278,
		0.004432639442248367,
		0.004401633951186316,
		0.004373718593052062,
		0.0043453518671867815,
		0.0043151449498027,
		0.004284417223498204,
		0.004255286644549648,
		0.0042304961399378845,
		0.004205219547000401,
		0.0041806026453621095,
		0.004156055184913207,
		0.0041322368569528855,
		0.004110119838132587,
		0.0040864056719563495,
		0.004065781638707908,
		0.004045018723080689,
		0.004022623939502836,
		0.004001479097333979,
		0.003978424622456934,
		0.003958321398128907,
		0.003936239099903303,
		0.003917455258173662,
		0.003898081166334217,
		0.0038782557067637453,
		0.003857353908757749,
		0.00383978528784241,
		0.003820376475408271,
		0.003801939839625573,
		0.0037826351889755166,
		0.0037640596708140416,
		0.003745657755626038,
		0.0037260058990290384,
		0.0037084372781136994,
		0.003692049157417968,
		0.0036776748312145092,
		0.0036618769606285816,
		0.003647294310856956,
		0.0036333019111951348,
		0.003619344232128008,
		0.003603511640947386,
		0.0035883734616606513,
		0.0035718464585861426,
		0.003557958220708404,
		0.0035440352622359717,
		0.0035302859067370108,
		0.0035171962425372422,
		0.003503932975364002,
		0.003491086355327094,
		0.003478343897074269,
		0.003464282056223059,
		0.003451365994996762,
		0.0034400818017211,
		0.0034274782258470523,
		0.003413485826185231,
		0.0034009169709058778,
		0.0033874106595697773,
		0.003373348818718567,
		0.0033592522572726624,
		0.003345259857610841,
		0.0033322396346004615,
		0.0033196013381317194,
		0.0033073102476099212,
		0.003294567789357096,
		0.0032823808606193806,
		0.00326807597560531,
		0.003255923767462289,
		0.0032431118680200754,
		0.0032313415864186922,
		0.003219779628385475,
		0.0032072802142955104,
		0.003195093285557795
	};

	public double probOfResponse(long now, DHTTransportReplyHandlerAdapter adapt){
		long diff = Math.max(now-adapt.getCreation(),0);
		int index = (int)(diff / 100);

		if( diff >= respProbTable.length ) {
			return 0;
		}

		// table lookup -- much faster.
		return respProbTable[index];

//		if(diff<=0) return 0.5305003758504375;
//		if(diff<=100) return 0.5248907429686456;
//		if(diff<=200) return 0.5064232752978594;
//		if(diff<=300) return 0.47965272461186714;
//		if(diff<=400) return 0.45212130681374313;
//		if(diff<=500) return 0.4251841319938128;
//		if(diff<=600) return 0.4003241514720664;
//		if(diff<=700) return 0.37852486081381603;
//		if(diff<=800) return 0.35924434097707225;
//		if(diff<=900) return 0.34220312589514035;
//		if(diff<=1000) return 0.32732455249493514;
//		if(diff<=1100) return 0.31433238372506844;
//		if(diff<=1200) return 0.3029768743479038;
//		if(diff<=1300) return 0.29291755421186855;
//		if(diff<=1400) return 0.2838894357382553;
//		if(diff<=1500) return 0.27571592998244876;
//		if(diff<=1600) return 0.2681279870561623;
//		if(diff<=1700) return 0.26101165396760917;
//		if(diff<=1800) return 0.25426575490385;
//		if(diff<=1900) return 0.24785987462393255;
//		if(diff<=2000) return 0.2417096420827496;
//		if(diff<=2100) return 0.2357991205273364;
//		if(diff<=2200) return 0.2300118918036828;
//		if(diff<=2300) return 0.22443274360403245;
//		if(diff<=2400) return 0.2189934846804056;
//		if(diff<=2500) return 0.21367588672058774;
//		if(diff<=2600) return 0.20847227647315142;
//		if(diff<=2700) return 0.20334703060794027;
//		if(diff<=2800) return 0.1983461886335189;
//		if(diff<=2900) return 0.19345732057698684;
//		if(diff<=3000) return 0.1886408449603925;
//		if(diff<=3100) return 0.1838896093412288;
//		if(diff<=3200) return 0.17922180731111562;
//		if(diff<=3300) return 0.17463743887005298;
//		if(diff<=3400) return 0.17010969971893677;
//		if(diff<=3500) return 0.16562963194433594;
//		if(diff<=3600) return 0.16122789383136554;
//		if(diff<=3700) return 0.1568792435076828;
//		if(diff<=3800) return 0.1526446156169763;
//		if(diff<=3900) return 0.148429500700488;
//		if(diff<=4000) return 0.14429313209276648;
//		if(diff<=4100) return 0.14022089242344543;
//		if(diff<=4200) return 0.13620229607292714;
//		if(diff<=4300) return 0.13221894112602361;
//		if(diff<=4400) return 0.12828593104142688;
//		if(diff<=4500) return 0.12442427177892702;
//		if(diff<=4600) return 0.12062597760174436;
//		if(diff<=4700) return 0.11688632650900045;
//		if(diff<=4800) return 0.11322788688724661;
//		if(diff<=4900) return 0.1096367357780104;
//		if(diff<=5000) return 0.10610492216510685;
//		if(diff<=5100) return 0.10264515378619404;
//		if(diff<=5200) return 0.09923583443137214;
//		if(diff<=5300) return 0.09592883666911446;
//		if(diff<=5400) return 0.09270666131969509;
//		if(diff<=5500) return 0.08952486602190522;
//		if(diff<=5600) return 0.08642233784180256;
//		if(diff<=5700) return 0.08337654311343044;
//		if(diff<=5800) return 0.08038571108645949;
//		if(diff<=5900) return 0.07744709883390882;
//		if(diff<=6000) return 0.07458389971303428;
//		if(diff<=6100) return 0.07179548875313137;
//		if(diff<=6200) return 0.06910092756068725;
//		if(diff<=6300) return 0.06645119065599356;
//		if(diff<=6400) return 0.0638981853281183;
//		if(diff<=6500) return 0.06143284950184627;
//		if(diff<=6600) return 0.059061294001843664;
//		if(diff<=6700) return 0.05677716495928142;
//		if(diff<=6800) return 0.054552963663161626;
//		if(diff<=6900) return 0.05242382735531494;
//		if(diff<=7000) return 0.05035430630855845;
//		if(diff<=7100) return 0.048376586514173814;
//		if(diff<=7200) return 0.04644692002284615;
//		if(diff<=7300) return 0.04459992326748573;
//		if(diff<=7400) return 0.04281028493456036;
//		if(diff<=7500) return 0.04110723976480269;
//		if(diff<=7600) return 0.03947464268167985;
//		if(diff<=7700) return 0.03791065349367304;
//		if(diff<=7800) return 0.03640766839054419;
//		if(diff<=7900) return 0.03496870806403172;
//		if(diff<=8000) return 0.033584016027026514;
//		if(diff<=8100) return 0.03223918323273041;
//		if(diff<=8200) return 0.030950424198865678;
//		if(diff<=8300) return 0.029733849281370493;
//		if(diff<=8400) return 0.028556439194690527;
//		if(diff<=8500) return 0.0274530881364936;
//		if(diff<=8600) return 0.02638286052563508;
//		if(diff<=8700) return 0.025376241044256606;
//		if(diff<=8800) return 0.024401703392375705;
//		if(diff<=8900) return 0.023455844951712334;
//		if(diff<=9000) return 0.02256574778612808;
//		if(diff<=9100) return 0.021712280847946363;
//		if(diff<=9200) return 0.02089766625522762;
//		if(diff<=9300) return 0.020107911608310026;
//		if(diff<=9400) return 0.019349787423158984;
//		if(diff<=9500) return 0.01863936933511797;
//		if(diff<=9600) return 0.017968706328001988;
//		if(diff<=9700) return 0.017326132281993725;
//		if(diff<=9800) return 0.016715292859536098;
//		if(diff<=9900) return 0.01613170910391353;
//		if(diff<=10000) return 0.015584408369746557;
//		if(diff<=10100) return 0.01506616877333875;
//		if(diff<=10200) return 0.01456924762207327;
//		if(diff<=10300) return 0.014101352887972264;
//		if(diff<=10400) return 0.013660158291191212;
//		if(diff<=10500) return 0.013240941830851678;
//		if(diff<=10600) return 0.012861376289653089;
//		if(diff<=10700) return 0.012494553206707325;
//		if(diff<=10800) return 0.012151583172316575;
//		if(diff<=10900) return 0.01182621647943586;
//		if(diff<=11000) return 0.01151727262784557;
//		if(diff<=11100) return 0.011225862676575925;
//		if(diff<=11200) return 0.01095188246384284;
//		if(diff<=11300) return 0.010682936737340437;
//		if(diff<=11400) return 0.010419511585394435;
//		if(diff<=11500) return 0.010175529966477265;
//		if(diff<=11600) return 0.009943040864403925;
//		if(diff<=11700) return 0.009721488749659304;
//		if(diff<=11800) return 0.009506255783149054;
//		if(diff<=11900) return 0.009299876568285861;
//		if(diff<=12000) return 0.00910356632588403;
//		if(diff<=12100) return 0.008915623746803535;
//		if(diff<=12200) return 0.008735909948665601;
//		if(diff<=12300) return 0.008563383313629396;
//		if(diff<=12400) return 0.008396585576717758;
//		if(diff<=12500) return 0.008237669414801736;
//		if(diff<=12600) return 0.008083683577332313;
//		if(diff<=12700) return 0.00794528728688065;
//		if(diff<=12800) return 0.00781227268860661;
//		if(diff<=12900) return 0.007677001251677439;
//		if(diff<=13000) return 0.00754846561011897;
//		if(diff<=13100) return 0.007421665998295219;
//		if(diff<=13200) return 0.0073029215644405555;
//		if(diff<=13300) return 0.0071908087641725125;
//		if(diff<=13400) return 0.007079425096393051;
//		if(diff<=13500) return 0.006977415989181062;
//		if(diff<=13600) return 0.00687672626456746;
//		if(diff<=13700) return 0.0067793697170445135;
//		if(diff<=13800) return 0.0066857282731538625;
//		if(diff<=13900) return 0.0065929895647252644;
//		if(diff<=14000) return 0.0064976120910998966;
//		if(diff<=14100) return 0.006412164707557111;
//		if(diff<=14200) return 0.0063288005596959865;
//		if(diff<=14300) return 0.006247623809300606;
//		if(diff<=14400) return 0.0061678358826929985;
//		if(diff<=14500) return 0.006090478397713996;
//		if(diff<=14600) return 0.00601805123718159;
//		if(diff<=14700) return 0.0059443741352401885;
//		if(diff<=14800) return 0.005870037341999593;
//		if(diff<=14900) return 0.005795665828164305;
//		if(diff<=15000) return 0.005726467682938473;
//		if(diff<=15100) return 0.005660394391235133;
//		if(diff<=15200) return 0.005597376511864895;
//		if(diff<=15300) return 0.005537171000664899;
//		if(diff<=15400) return 0.005481548607964557;
//		if(diff<=15500) return 0.005428877465813235;
//		if(diff<=15600) return 0.005373324514302281;
//		if(diff<=15700) return 0.005320965857503208;
//		if(diff<=15800) return 0.005267635024052692;
//		if(diff<=15900) return 0.005215797176174033;
//		if(diff<=16000) return 0.005165660637435398;
//		if(diff<=16100) return 0.005115176892749819;
//		if(diff<=16200) return 0.005066255574825485;
//		if(diff<=16300) return 0.005020632713397115;
//		if(diff<=16400) return 0.004978447190843485;
//		if(diff<=16500) return 0.004937928256835183;
//		if(diff<=16600) return 0.004898763426019961;
//		if(diff<=16700) return 0.00485873058033738;
//		if(diff<=16800) return 0.004817100587298859;
//		if(diff<=16900) return 0.004776546932695863;
//		if(diff<=17000) return 0.004736409925229199;
//		if(diff<=17100) return 0.004698460315228279;
//		if(diff<=17200) return 0.004660337102253887;
//		if(diff<=17300) return 0.004623949919014213;
//		if(diff<=17400) return 0.004590513986323558;
//		if(diff<=17500) return 0.004557737744932095;
//		if(diff<=17600) return 0.004526836415654128;
//		if(diff<=17700) return 0.00449617813053902;
//		if(diff<=17800) return 0.004463887977473278;
//		if(diff<=17900) return 0.004432639442248367;
//		if(diff<=18000) return 0.004401633951186316;
//		if(diff<=18100) return 0.004373718593052062;
//		if(diff<=18200) return 0.0043453518671867815;
//		if(diff<=18300) return 0.0043151449498027;
//		if(diff<=18400) return 0.004284417223498204;
//		if(diff<=18500) return 0.004255286644549648;
//		if(diff<=18600) return 0.0042304961399378845;
//		if(diff<=18700) return 0.004205219547000401;
//		if(diff<=18800) return 0.0041806026453621095;
//		if(diff<=18900) return 0.004156055184913207;
//		if(diff<=19000) return 0.0041322368569528855;
//		if(diff<=19100) return 0.004110119838132587;
//		if(diff<=19200) return 0.0040864056719563495;
//		if(diff<=19300) return 0.004065781638707908;
//		if(diff<=19400) return 0.004045018723080689;
//		if(diff<=19500) return 0.004022623939502836;
//		if(diff<=19600) return 0.004001479097333979;
//		if(diff<=19700) return 0.003978424622456934;
//		if(diff<=19800) return 0.003958321398128907;
//		if(diff<=19900) return 0.003936239099903303;
//		if(diff<=20000) return 0.003917455258173662;
//		if(diff<=20100) return 0.003898081166334217;
//		if(diff<=20200) return 0.0038782557067637453;
//		if(diff<=20300) return 0.003857353908757749;
//		if(diff<=20400) return 0.00383978528784241;
//		if(diff<=20500) return 0.003820376475408271;
//		if(diff<=20600) return 0.003801939839625573;
//		if(diff<=20700) return 0.0037826351889755166;
//		if(diff<=20800) return 0.0037640596708140416;
//		if(diff<=20900) return 0.003745657755626038;
//		if(diff<=21000) return 0.0037260058990290384;
//		if(diff<=21100) return 0.0037084372781136994;
//		if(diff<=21200) return 0.003692049157417968;
//		if(diff<=21300) return 0.0036776748312145092;
//		if(diff<=21400) return 0.0036618769606285816;
//		if(diff<=21500) return 0.003647294310856956;
//		if(diff<=21600) return 0.0036333019111951348;
//		if(diff<=21700) return 0.003619344232128008;
//		if(diff<=21800) return 0.003603511640947386;
//		if(diff<=21900) return 0.0035883734616606513;
//		if(diff<=22000) return 0.0035718464585861426;
//		if(diff<=22100) return 0.003557958220708404;
//		if(diff<=22200) return 0.0035440352622359717;
//		if(diff<=22300) return 0.0035302859067370108;
//		if(diff<=22400) return 0.0035171962425372422;
//		if(diff<=22500) return 0.003503932975364002;
//		if(diff<=22600) return 0.003491086355327094;
//		if(diff<=22700) return 0.003478343897074269;
//		if(diff<=22800) return 0.003464282056223059;
//		if(diff<=22900) return 0.003451365994996762;
//		if(diff<=23000) return 0.0034400818017211;
//		if(diff<=23100) return 0.0034274782258470523;
//		if(diff<=23200) return 0.003413485826185231;
//		if(diff<=23300) return 0.0034009169709058778;
//		if(diff<=23400) return 0.0033874106595697773;
//		if(diff<=23500) return 0.003373348818718567;
//		if(diff<=23600) return 0.0033592522572726624;
//		if(diff<=23700) return 0.003345259857610841;
//		if(diff<=23800) return 0.0033322396346004615;
//		if(diff<=23900) return 0.0033196013381317194;
//		if(diff<=24000) return 0.0033073102476099212;
//		if(diff<=24100) return 0.003294567789357096;
//		if(diff<=24200) return 0.0032823808606193806;
//		if(diff<=24300) return 0.00326807597560531;
//		if(diff<=24400) return 0.003255923767462289;
//		if(diff<=24500) return 0.0032431118680200754;
//		if(diff<=24600) return 0.0032313415864186922;
//		if(diff<=24700) return 0.003219779628385475;
//		if(diff<=24800) return 0.0032072802142955104;
//		if(diff<=24900) return 0.003195093285557795;
//
//		return 0;
		}

	protected void
	lookupSupportSync(
		final byte[]				lookup_id,
		byte						flags,
		boolean						value_search,
		long						timeout,
		int							concurrency,
		int							max_values,
		final int					search_accuracy,
		final lookupResultHandler	result_handler )
	{
		boolean		timeout_occurred	= false;

		logger.log("***** our lookupSupportSync");

		DHTLog.logging_on = true;

		last_lookup	= SystemTime.getCurrentTime();

		result_handler.incrementCompletes();

		try{
			DHTLog.log( "lookup for " + DHTLog.getString( lookup_id ));

			if ( value_search ){

				if ( database.isKeyBlocked( lookup_id )){

					DHTLog.log( "lookup: terminates - key blocked" );

						// bail out and pretend everything worked with zero results

					return;
				}
			}

				// keep querying successively closer nodes until we have got responses from the K
				// closest nodes that we've seen. We might get a bunch of closer nodes that then
				// fail to respond, which means we have reconsider further away nodes

				// we keep a list of nodes that we have queried to avoid re-querying them

				// we keep a list of nodes discovered that we have yet to query

				// we have a parallel search limit of A. For each A we effectively loop grabbing
				// the currently closest unqueried node, querying it and adding the results to the
				// yet-to-query-set (unless already queried)

				// we terminate when we have received responses from the K closest nodes we know
				// about (excluding failed ones)

				// Note that we never widen the root of our search beyond the initial K closest
				// that we know about - this could be relaxed


				// contacts remaining to query
				// closest at front

			final Set		contacts_to_query	= getClosestContactsSet( lookup_id, false );

			final AEMonitor	contacts_to_query_mon	= new AEMonitor( "DHTControl:ctq" );

			final Map	level_map			= new LightHashMap();

			Iterator	it = contacts_to_query.iterator();

			while( it.hasNext()){

				DHTTransportContact	contact	= (DHTTransportContact)it.next();

				result_handler.found( contact );

				level_map.put( contact , new Integer(0));
			}

				// record the set of contacts we've queried to avoid re-queries

			final Map			contacts_queried = new LightHashMap();

				// record the set of contacts that we've had a reply from
				// furthest away at front

			final Set			ok_contacts = new sortedTransportContactSet( lookup_id, false ).getSet();


				// this handles the search concurrency

			final AESemaphore	search_sem = new AESemaphore( "DHTControl:search", concurrency );

			final int[]	idle_searches	= { 0 };
//			final int[]	active_searches	= { 0 };

			final int[]	values_found	= { 0 };
			final int[]	value_replies	= { 0 };
			final Set	values_found_set	= new HashSet();

			final boolean[]	key_blocked	= { false };

			long	start = SystemTime.getCurrentTime();

			// PIAMODs
			boolean lastRoundQueriedSomeoneNew = true;
			boolean running = true;

			final double maxBudget = 1;
			final double threshold = .2;
			double allocatedBudget = 0;

			final List active_searches = new ArrayList();

			while( running ){

				int activeSize = 0;
				try{
					//get state for the next round
					contacts_to_query_mon.enter();

					activeSize = active_searches.size();

					allocatedBudget = calculateBudgetInUse(active_searches);

					running = (ok_contacts.size() < search_accuracy //we need some way to bootstrap the first contacts without rewriting this whole thing
							|| threshold < allocatedBudget);
				} finally {
					contacts_to_query_mon.exit();
				}

//				logger.log(activeSize + " RPCs running; allocated budget: " + allocatedBudget + " budget in unreplied closer to K "+budgetInUnrepliedRequestsToNodesInNearestK);

				//are we even going to query anyone??
				lastRoundQueriedSomeoneNew = allocatedBudget < maxBudget;

//				System.out.println("active searches: " + active_searches.size() + " budget: " + allocatedBudget + " max: " + maxBudget + " queried new: " + lastRoundQueriedSomeoneNew);

				if ( timeout > 0 ){

					long	now = SystemTime.getCurrentTime();

						// check for clock being set back

					if ( now < start ){

//						start	= now;
					}

					long remaining = timeout - ( now - start );

					if ( remaining <= 0 ){

						DHTLog.log( "lookup: terminates - timeout" );

						timeout_occurred	= true;

						break;

					}
						// get permission to kick off another search

//					if ( !search_sem.reserve( remaining )){
//
//						DHTLog.log( "lookup: terminates - timeout" );
//
//						timeout_occurred	= true;
//
//						break;
//					}
				}else{

//					search_sem.reserve();
				}

				try{
					contacts_to_query_mon.enter();

					if ( 	values_found[0] >= max_values ||
							value_replies[0]>= 2 ){	// all hits should have the same values anyway...

						break;
					}

						// if we've received a key block then easiest way to terminate the query is to
						// dump any outstanding targets

					if ( key_blocked[0] ){

						contacts_to_query.clear();
					}

						// if nothing pending then we need to wait for the results of a previous
						// search to arrive. Of course, if there are no searches active then
						// we've run out of things to do

					if ( contacts_to_query.size() == 0 ){

						if ( active_searches.size() == 0 ){

							DHTLog.log( "lookup: terminates - no contacts left to query" );

							break;
						}

						lastRoundQueriedSomeoneNew = false;
						idle_searches[0]++;

						continue;
					}

						// select the next contact to search

					DHTTransportContact	closest	= (DHTTransportContact)contacts_to_query.iterator().next();

						// if the next closest is further away than the furthest successful hit so
						// far and we have K hits, we're done

					if ( ok_contacts.size() == search_accuracy ){

						DHTTransportContact	furthest_ok = (DHTTransportContact)ok_contacts.iterator().next();

						int	distance = computeAndCompareDistances( furthest_ok.getID(), closest.getID(), lookup_id );

						if ( distance <= 0 ){

							DHTLog.log( "lookup: terminates - we've searched the closest " + search_accuracy + " contacts" );

							break;
						}
					}

					// we optimise the first few entries based on their Vivaldi distance. Only a few
					// however as we don't want to start too far away from the target.

					if ( contacts_queried.size() < concurrency ){

						DHTNetworkPosition[]	loc_nps = local_contact.getNetworkPositions();

						DHTTransportContact	vp_closest = null;

						Iterator vp_it = contacts_to_query.iterator();

						int	vp_count_limit = (concurrency*2) - contacts_queried.size();

						int	vp_count = 0;

						float	best_dist = Float.MAX_VALUE;

						while( vp_it.hasNext() && vp_count < vp_count_limit ){

							vp_count++;

							DHTTransportContact	entry	= (DHTTransportContact)vp_it.next();

							DHTNetworkPosition[]	rem_nps = entry.getNetworkPositions();

							float	dist = DHTNetworkPositionManager.estimateRTT( loc_nps, rem_nps );

							if ( (!Float.isNaN(dist)) && dist < best_dist ){

								best_dist	= dist;

								vp_closest	= entry;

								// System.out.println( start + ": lookup for " + DHTLog.getString2( lookup_id ) + ": vp override (dist = " + dist + ")");
							}

							if ( vp_closest != null ){

									// override ID closest with VP closes

								closest = vp_closest;
							}
						}
					}

					if(lastRoundQueriedSomeoneNew){

					contacts_to_query.remove( closest );

					contacts_queried.put( new HashWrapper( closest.getID()), closest );

						// never search ourselves!

					if ( router.isID( closest.getID())){

//						search_sem.release();

						continue;
					}

					final int	search_level = ((Integer)level_map.get(closest)).intValue();

//					active_searches[0]++;

					DHTTransportReplyHandlerAdapter	handler =
						new DHTTransportReplyHandlerAdapter()
						{
							private boolean	value_reply_received	= false;

							@Override
							public void
							findNodeReply(
								DHTTransportContact 	target_contact,
								DHTTransportContact[]	reply_contacts )
							{
								try{

									DHTLog.log( "findNodeReply: " + DHTLog.getString( reply_contacts ));

									router.contactAlive( target_contact.getID(), new DHTControlContactImpl(target_contact));

									for (int i=0;i<reply_contacts.length;i++){

										DHTTransportContact	contact = reply_contacts[i];

											// ignore responses that are ourselves

										if ( compareDistances( router.getID(), contact.getID()) == 0 ){

											continue;
										}

											// dunno if its alive or not, however record its existance

										router.contactKnown( contact.getID(), new DHTControlContactImpl(contact));
									}

									try{
										contacts_to_query_mon.enter();

										ok_contacts.add( target_contact );

										if ( ok_contacts.size() > search_accuracy ){

												// delete the furthest away

											Iterator ok_it = ok_contacts.iterator();

											ok_it.next();

											ok_it.remove();
										}

										for (int i=0;i<reply_contacts.length;i++){

											DHTTransportContact	contact = reply_contacts[i];

												// ignore responses that are ourselves

											if ( compareDistances( router.getID(), contact.getID()) == 0 ){

												continue;
											}

											if (	contacts_queried.get( new HashWrapper( contact.getID())) == null &&
													(!contacts_to_query.contains( contact ))){

												DHTLog.log( "    new contact for query: " + DHTLog.getString( contact ));

												contacts_to_query.add( contact );

												result_handler.found( contact );

												level_map.put( contact, new Integer( search_level+1));

												if ( idle_searches[0] > 0 ){

													idle_searches[0]--;

//													search_sem.release();
												}
											}else{

												// DHTLog.log( "    already queried: " + DHTLog.getString( contact ));
											}
										}
									}finally{

										contacts_to_query_mon.exit();
									}
								}finally{

									try{
										contacts_to_query_mon.enter();

//										active_searches[0]--;
										active_searches.remove(this);

									}finally{

										contacts_to_query_mon.exit();
									}

//									search_sem.release();
								}
							}

							@Override
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportValue[]		values,
								byte					diversification_type,
								boolean					more_to_come )
							{
								DHTLog.log( "findValueReply: " + DHTLog.getString( values ) + ",mtc=" + more_to_come + ", dt=" + diversification_type );

								try{
									if ( !key_blocked[0] ){

										if ( diversification_type != DHT.DT_NONE ){

												// diversification instruction

											result_handler.diversify( contact, diversification_type );
										}
									}

									value_reply_received	= true;

									router.contactAlive( contact.getID(), new DHTControlContactImpl(contact));

									int	new_values = 0;

									if ( !key_blocked[0] ){

										for (int i=0;i<values.length;i++){

											DHTTransportValue	value = values[i];

											DHTTransportContact	originator = value.getOriginator();

												// can't just use originator id as this value can be DOSed (see DB code)

											byte[]	originator_id 	= originator.getID();
											byte[]	value_bytes		= value.getValue();

											byte[]	value_id = new byte[originator_id.length + value_bytes.length];

											System.arraycopy( originator_id, 0, value_id, 0, originator_id.length );

											System.arraycopy( value_bytes, 0, value_id, originator_id.length, value_bytes.length );

											HashWrapper	x = new HashWrapper( value_id );

											if ( !values_found_set.contains( x )){

												new_values++;

												values_found_set.add( x );

												result_handler.read( contact, values[i] );
											}
										}
									}

									try{
										contacts_to_query_mon.enter();

										if ( !more_to_come ){

											value_replies[0]++;
										}

										values_found[0] += new_values;

									}finally{

										contacts_to_query_mon.exit();
									}
								}finally{

									if ( !more_to_come ){

										try{
											contacts_to_query_mon.enter();

//											active_searches[0]--;

										}finally{

											contacts_to_query_mon.exit();
										}

//										search_sem.release();
									}
								}
							}

							@Override
							public void
							findValueReply(
								DHTTransportContact 	contact,
								DHTTransportContact[]	contacts )
							{
								findNodeReply( contact, contacts );
							}

							public void
							failed(
								DHTTransportContact 	target_contact,
								Throwable 				error )
							{
								try{
										// if at least one reply has been received then we
										// don't treat subsequent failure as indication of
										// a contact failure (just packet loss)

									if ( !value_reply_received ){

										DHTLog.log( "findNode/findValue " + DHTLog.getString( target_contact ) + " -> failed: " + error.getMessage());

										router.contactDead( target_contact.getID(), false );
									}

								}finally{

									try{
										contacts_to_query_mon.enter();

//										active_searches[0]--;
										active_searches.remove(this);

									}finally{

										contacts_to_query_mon.exit();
									}

//									search_sem.release();
								}
							}

							@Override
							public void
							keyBlockRequest(
								DHTTransportContact		contact,
								byte[]					request,
								byte[]					key_signature )
							{
									// we don't want to kill the contact due to this so indicate that
									// it is ok by setting the flag

								if ( database.keyBlockRequest( null, request, key_signature ) != null ){

									key_blocked[0]	= true;
								}
							}
						};

					// PIAMOD
					handler.setCreation(System.currentTimeMillis());
					handler.setTgt(closest);
					active_searches.add(handler);
					result_handler.searching( closest, search_level, active_searches.size() );
					// --

					router.recordLookup( lookup_id );

					if ( value_search ){

						int	rem = max_values - values_found[0];

						if ( rem <= 0 ){

							Debug.out( "eh?" );

							rem = 1;
						}

						closest.sendFindValue( handler, lookup_id, rem, flags );

					}else{

						closest.sendFindNode( handler, lookup_id );
					}
				}
				}finally{

					contacts_to_query_mon.exit();

					if(!lastRoundQueriedSomeoneNew){
						//don't spin
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			}

				// maybe unterminated searches still going on so protect ourselves
				// against concurrent modification of result set

			List	closest_res = null;

			try{
				contacts_to_query_mon.enter();

				if ( DHTLog.isOn()){
					DHTLog.log( "lookup complete for " + DHTLog.getString( lookup_id ));

					DHTLog.log( "    queried = " + DHTLog.getString( contacts_queried ));
					DHTLog.log( "    to query = " + DHTLog.getString( contacts_to_query ));
					DHTLog.log( "    ok = " + DHTLog.getString( ok_contacts ));
				}

				closest_res	= new ArrayList( ok_contacts );

					// we need to reverse the list as currently closest is at
					// the end

				Collections.reverse( closest_res );

				if ( timeout <= 0 && !value_search ){

						// we can use the results of this to estimate the DHT size

					estimateDHTSize( lookup_id, contacts_queried, search_accuracy );
				}

			}finally{

				contacts_to_query_mon.exit();
			}

			result_handler.closest( closest_res );

		}finally{

			result_handler.complete( timeout_occurred );
		}
	}


		// Request methods

	public void
	pingRequest(
		DHTTransportContact originating_contact )
	{
		DHTLog.log( "pingRequest from " + DHTLog.getString( originating_contact.getID()));

		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));
	}

	public void
	keyBlockRequest(
		DHTTransportContact originating_contact,
		byte[]				request,
		byte[]				sig )
	{
		DHTLog.log( "keyBlockRequest from " + DHTLog.getString( originating_contact.getID()));

		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));

		database.keyBlockRequest( originating_contact, request, sig );
	}

	public DHTTransportStoreReply
	storeRequest(
		DHTTransportContact 	originating_contact,
		byte[][]				keys,
		DHTTransportValue[][]	value_sets )
	{
		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));

		DHTLog.log( "storeRequest from " + DHTLog.getString( originating_contact )+ ", keys = " + keys.length );

		byte[]	diverse_res = new byte[ keys.length];

		Arrays.fill( diverse_res, DHT.DT_NONE );

		if ( keys.length != value_sets.length ){

			Debug.out( "DHTControl:storeRequest - invalid request received from " + originating_contact.getString() + ", keys and values length mismatch");

			return( new DHTTransportStoreReplyImpl(  diverse_res ));
		}

		// System.out.println( "storeRequest: received " + originating_contact.getRandomID() + " from " + originating_contact.getAddress());

		DHTStorageBlock	blocked_details	= null;

		for (int i=0;i<keys.length;i++){

			HashWrapper			key		= new HashWrapper( keys[i] );

			DHTTransportValue[]	values 	= value_sets[i];

			DHTLog.log( "    key=" + DHTLog.getString(key) + ", value=" + DHTLog.getString(values));

			diverse_res[i] = database.store( originating_contact, key, values );

			if ( blocked_details == null ){

				blocked_details = database.getKeyBlockDetails( keys[i] );
			}
		}

			// fortunately we can get away with this as diversifications are only taken note of by initial, single value stores
			// and not by the multi-value cache forwards...

		if ( blocked_details == null ){

			return( new DHTTransportStoreReplyImpl( diverse_res ));

		}else{

			return( new DHTTransportStoreReplyImpl( blocked_details.getRequest(), blocked_details.getCertificate()));
		}
	}

	public DHTTransportContact[]
	findNodeRequest(
		DHTTransportContact originating_contact,
		byte[]				id )
	{
		DHTLog.log( "findNodeRequest from " + DHTLog.getString( originating_contact.getID()));

		router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));

		List	l;

		if ( id.length == router.getID().length ){

			l = getClosestKContactsList( id, false );

		}else{

				// this helps both protect against idiot queries and also saved bytes when we use findNode
				// to just get a random ID prior to cache-forwards

			l = new ArrayList();
		}

		final DHTTransportContact[]	res = new DHTTransportContact[l.size()];

		l.toArray( res );

		int	rand = generateSpoofID( originating_contact );

		originating_contact.setRandomID( rand );

		return( res );
	}

	public DHTTransportFindValueReply
	findValueRequest(
		DHTTransportContact originating_contact,
		byte[]				key,
		int					max_values,
		byte				flags )
	{
		DHTLog.log( "findValueRequest from " + DHTLog.getString( originating_contact.getID()));

		DHTDBLookupResult	result	= database.get( originating_contact, new HashWrapper( key ), max_values, flags, true );

		if ( result != null ){

			router.contactAlive( originating_contact.getID(), new DHTControlContactImpl(originating_contact));

			DHTStorageBlock	block_details = database.getKeyBlockDetails( key );

			if ( block_details == null ){

				return( new DHTTransportFindValueReplyImpl( result.getDiversificationType(), result.getValues()));

			}else{

				return( new DHTTransportFindValueReplyImpl( block_details.getRequest(), block_details.getCertificate()));

			}
		}else{

			return( new DHTTransportFindValueReplyImpl( findNodeRequest( originating_contact, key )));
		}
	}

	public DHTTransportFullStats
	statsRequest(
		DHTTransportContact	contact )
	{
		return( stats );
	}

	protected void
	requestPing(
		DHTRouterContact	contact )
	{
		((DHTControlContactImpl)contact.getAttachment()).getTransportContact().sendPing(
				new DHTTransportReplyHandlerAdapter()
				{
					@Override
					public void
					pingReply(
						DHTTransportContact _contact )
					{
						DHTLog.log( "ping OK " + DHTLog.getString( _contact ));

						router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
					}

					public void
					failed(
						DHTTransportContact 	_contact,
						Throwable				_error )
					{
						DHTLog.log( "ping " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());

						router.contactDead( _contact.getID(), false );
					}
				});
	}

	protected void
	nodeAddedToRouter(
		DHTRouterContact	new_contact )
	{
			// ignore ourselves

		if ( router.isID( new_contact.getID())){

			return;
		}

		// when a new node is added we must check to see if we need to transfer
		// any of our values to it.

		Map	keys_to_store	= new HashMap();

		DHTStorageBlock[]	direct_key_blocks = database.getDirectKeyBlocks();

		if ( database.isEmpty() && direct_key_blocks.length == 0 ){

			// nothing to do, ping it if it isn't known to be alive

			if ( !new_contact.hasBeenAlive()){

				requestPing( new_contact );
			}

			return;
		}

			// see if we're one of the K closest to the new node

		List	closest_contacts = getClosestKContactsList( new_contact.getID(), false );

		boolean	close	= false;

		for (int i=0;i<closest_contacts.size();i++){

			if ( router.isID(((DHTTransportContact)closest_contacts.get(i)).getID())){

				close	= true;

				break;
			}
		}

		if ( !close ){

			if ( !new_contact.hasBeenAlive()){

				requestPing( new_contact );
			}

			return;
		}

			// ok, we're close enough to worry about transferring values

		Iterator	it = database.getKeys();

		while( it.hasNext()){

			HashWrapper	key		= (HashWrapper)it.next();

			byte[]	encoded_key		= key.getHash();

			if ( database.isKeyBlocked( encoded_key )){

				continue;
			}

			DHTDBLookupResult	result = database.get( null, key, 0, (byte)0, false );

			if ( result == null  ){

					// deleted in the meantime

				continue;
			}

				// even if a result has been diversified we continue to maintain the base value set
				// until the original publisher picks up the diversification (next publish period) and
				// publishes to the correct place

			DHTDBValue[]	values = result.getValues();

			List	values_to_store = new ArrayList();

			for (int i=0;i<values.length;i++){

				DHTDBValue	value = values[i];

				// we don't consider any cached further away than the initial location, for transfer
				// however, we *do* include ones we originate as, if we're the closest, we have to
				// take responsibility for xfer (as others won't)

				List		sorted_contacts	= getClosestKContactsList( encoded_key, false );

					// if we're closest to the key, or the new node is closest and
					// we're second closest, then we take responsibility for storing
					// the value

				boolean	store_it	= false;

				if ( sorted_contacts.size() > 0 ){

					DHTTransportContact	first = (DHTTransportContact)sorted_contacts.get(0);

					if ( router.isID( first.getID())){

						store_it = true;

					}else if ( Arrays.equals( first.getID(), new_contact.getID()) && sorted_contacts.size() > 1 ){

						store_it = router.isID(((DHTTransportContact)sorted_contacts.get(1)).getID());

					}
				}

				if ( store_it ){

					values_to_store.add( value );
				}
			}

			if ( values_to_store.size() > 0 ){

				keys_to_store.put( key, values_to_store );
			}
		}

		final DHTTransportContact	t_contact = ((DHTControlContactImpl)new_contact.getAttachment()).getTransportContact();

		final boolean[]	anti_spoof_done	= { false };

		if ( keys_to_store.size() > 0 ){

			it = keys_to_store.entrySet().iterator();

			final byte[][]				keys 		= new byte[keys_to_store.size()][];
			final DHTTransportValue[][]	value_sets 	= new DHTTransportValue[keys.length][];

			int		index = 0;

			while( it.hasNext()){

				Map.Entry	entry = (Map.Entry)it.next();

				HashWrapper	key		= (HashWrapper)entry.getKey();

				List		values	= (List)entry.getValue();

				keys[index] 		= key.getHash();
				value_sets[index]	= new DHTTransportValue[values.size()];


				for (int i=0;i<values.size();i++){

					value_sets[index][i] = ((DHTDBValue)values.get(i)).getValueForRelay( local_contact );
				}

				index++;
			}

				// move to anti-spoof for cache forwards. we gotta do a findNode to update the
				// contact's latest random id

			t_contact.sendFindNode(
					new DHTTransportReplyHandlerAdapter()
					{
						@Override
						public void
						findNodeReply(
							DHTTransportContact 	contact,
							DHTTransportContact[]	contacts )
						{
							// System.out.println( "nodeAdded: pre-store findNode OK" );

							anti_spoof_done[0]	= true;

							t_contact.sendStore(
									new DHTTransportReplyHandlerAdapter()
									{
										@Override
										public void
										storeReply(
											DHTTransportContact _contact,
											byte[]				_diversifications )
										{
											// System.out.println( "nodeAdded: store OK" );

												// don't consider diversifications for node additions as they're not interested
												// in getting values from us, they need to get them from nodes 'near' to the
												// diversification targets or the originator

											DHTLog.log( "add store ok" );

											router.contactAlive( _contact.getID(), new DHTControlContactImpl(_contact));
										}

										public void
										failed(
											DHTTransportContact 	_contact,
											Throwable				_error )
										{
											// System.out.println( "nodeAdded: store Failed" );

											DHTLog.log( "add store failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());

											router.contactDead( _contact.getID(), false);
										}

										@Override
										public void
										keyBlockRequest(
											DHTTransportContact		contact,
											byte[]					request,
											byte[]					signature )
										{
											database.keyBlockRequest( null, request, signature );
										}
									},
									keys,
									value_sets );
						}

						public void
						failed(
							DHTTransportContact 	_contact,
							Throwable				_error )
						{
							// System.out.println( "nodeAdded: pre-store findNode Failed" );

							DHTLog.log( "pre-store findNode failed " + DHTLog.getString( _contact ) + " -> failed: " + _error.getMessage());

							router.contactDead( _contact.getID(), false);
						}
					},
					t_contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF2?new byte[0]:new byte[20] );

		}else{

			if ( !new_contact.hasBeenAlive()){

				requestPing( new_contact );
			}
		}

			// finally transfer any key-blocks

		if ( t_contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_BLOCK_KEYS ){

			for (int i=0;i<direct_key_blocks.length;i++){

				final DHTStorageBlock	key_block = direct_key_blocks[i];

				List	contacts = getClosestKContactsList( key_block.getKey(), false );

				boolean	forward_it = false;

					// ensure that the key is close enough to us

				for (int j=0;j<contacts.size();j++){

					final DHTTransportContact	contact = (DHTTransportContact)contacts.get(j);

					if ( router.isID( contact.getID())){

						forward_it	= true;

						break;
					}
				}

				if ( !forward_it || key_block.hasBeenSentTo( t_contact )){

					continue;
				}

				final Runnable task =
					new Runnable()
					{
						public void
						run()
						{
							t_contact.sendKeyBlock(
									new DHTTransportReplyHandlerAdapter()
									{
										@Override
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

				if ( anti_spoof_done[0] ){

					task.run();

				}else{

					t_contact.sendFindNode(
							new DHTTransportReplyHandlerAdapter()
							{
								@Override
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
							t_contact.getProtocolVersion() >= DHTTransportUDP.PROTOCOL_VERSION_ANTI_SPOOF2?new byte[0]:new byte[20] );
				}
			}
		}
	}

	protected Set
	getClosestContactsSet(
		byte[]		id,
		boolean		live_only )
	{
		List	l = router.findClosestContacts( id, live_only );

		Set	sorted_set	= new sortedTransportContactSet( id, true ).getSet();

		for (int i=0;i<l.size();i++){

			sorted_set.add(((DHTControlContactImpl)((DHTRouterContact)l.get(i)).getAttachment()).getTransportContact());
		}

		return( sorted_set );
	}

	public List
	getClosestKContactsList(
		byte[]		id,
		boolean		live_only )
	{
		Set	sorted_set	= getClosestContactsSet( id, live_only );

		List	res = new ArrayList(K);

		Iterator	it = sorted_set.iterator();

		while( it.hasNext() && res.size() < K ){

			res.add( it.next());
		}

		return( res );
	}

	protected byte[]
	encodeKey(
		byte[]		key )
	{
		byte[]	temp = new SHA1Simple().calculateHash( key );

		byte[]	result =  new byte[node_id_byte_count];

		System.arraycopy( temp, 0, result, 0, node_id_byte_count );

		return( result );
	}

	public int
	computeAndCompareDistances(
		byte[]		t1,
		byte[]		t2,
		byte[]		pivot )
	{
		return( computeAndCompareDistances2( t1, t2, pivot ));
	}

	protected static int
	computeAndCompareDistances2(
		byte[]		t1,
		byte[]		t2,
		byte[]		pivot )
	{
		for (int i=0;i<t1.length;i++){

			byte d1 = (byte)( t1[i] ^ pivot[i] );
			byte d2 = (byte)( t2[i] ^ pivot[i] );

			int diff = (d1&0xff) - (d2&0xff);

			if ( diff != 0 ){

				return( diff );
			}
		}

		return( 0 );
	}

	public byte[]
	computeDistance(
		byte[]		n1,
		byte[]		n2 )
	{
		return( computeDistance2( n1, n2 ));
	}

	protected static byte[]
	computeDistance2(
		byte[]		n1,
		byte[]		n2 )
	{
		byte[]	res = new byte[n1.length];

		for (int i=0;i<res.length;i++){

			res[i] = (byte)( n1[i] ^ n2[i] );
		}

		return( res );
	}

		/**
		 * -ve -> n1 < n2
		 * @param n1
		 * @param n2
		 * @return
		 */

	public int
	compareDistances(
		byte[]		n1,
		byte[]		n2 )
	{
		return( compareDistances2( n1,n2 ));
	}

	protected static int
	compareDistances2(
		byte[]		n1,
		byte[]		n2 )
	{
		for (int i=0;i<n1.length;i++){

			int diff = (n1[i]&0xff) - (n2[i]&0xff);

			if ( diff != 0 ){

				return( diff );
			}
		}

		return( 0 );
	}

	public void
	addListener(
		DHTControlListener	l )
	{
		try{
			activity_mon.enter();

			listeners.addListener( l );

			for (int i=0;i<activities.size();i++){

				listeners.dispatch( DHTControlListener.CT_ADDED, activities.get(i));
			}

		}finally{

			activity_mon.exit();
		}
	}

	public void
	removeListener(
		DHTControlListener	l )
	{
		listeners.removeListener( l );
	}

	public DHTControlActivity[]
	getActivities()
	{
		List	res;

		try{

			activity_mon.enter();

			res = new ArrayList( activities );

		}finally{

			activity_mon.exit();
		}

		DHTControlActivity[]	x = new DHTControlActivity[res.size()];

		res.toArray( x );

		return( x );
	}

	public void
	setTransportEstimatedDHTSize(
		int						size )
	{
		if ( size > 0 ){

			try{
				estimate_mon.enter();

				remote_estimate_values.add( new Integer( size ));

				if ( remote_estimate_values.size() > REMOTE_ESTIMATE_HISTORY ){

					remote_estimate_values.remove(0);
				}
			}finally{

				estimate_mon.exit();
			}
		}
		// System.out.println( "estimated dht size: " + size );
	}

	public int
	getTransportEstimatedDHTSize()
	{
		return((int)local_dht_estimate );
	}

	public int
	getEstimatedDHTSize()
	{
			// public method, trigger actual computation periodically

		long	now = SystemTime.getCurrentTime();

		long	diff = now - last_dht_estimate_time;

		if ( diff < 0 || diff > 60*1000 ){

			estimateDHTSize( router.getID(), null, router.getK());
		}

		return((int)combined_dht_estimate );
	}

	protected void
	estimateDHTSize(
		byte[]	id,
		Map		contacts,
		int		contacts_to_use )
	{
			// if called with contacts then this is in internal estimation based on lookup values

		long	now = SystemTime.getCurrentTime();

		long	diff = now - last_dht_estimate_time;

			// 5 second limiter here

		if ( diff < 0 || diff > 5*1000 ){

			try{
				estimate_mon.enter();

				last_dht_estimate_time	= now;

				List	l;

				if ( contacts == null ){

					l = getClosestKContactsList( id, false );

				}else{

					Set	sorted_set	= new sortedTransportContactSet( id, true ).getSet();

					sorted_set.addAll( contacts.values());

					l = new ArrayList( sorted_set );

					if ( l.size() > 0 ){

							// algorithm works relative to a starting point in the ID space so we grab
							// the first here rather than using the initial lookup target

						id = ((DHTTransportContact)l.get(0)).getID();
					}

					/*
					String	str = "";
					for (int i=0;i<l.size();i++){
						str += (i==0?"":",") + DHTLog.getString2( ((DHTTransportContact)l.get(i)).getID());
					}
					System.out.println( "trace: " + str );
					*/
				}

					// can't estimate with less than 2

				if ( l.size() > 2 ){


					/*
					<Gudy> if you call N0 yourself, N1 the nearest peer, N2 the 2nd nearest peer ... Np the pth nearest peer that you know (for example, N1 .. N20)
					<Gudy> and if you call D1 the Kad distance between you and N1, D2 between you and N2 ...
					<Gudy> then you have to compute :
					<Gudy> Dc = sum(i * Di) / sum( i * i)
					<Gudy> and then :
					<Gudy> NbPeers = 2^160 / Dc
					*/

					BigInteger	sum1 = new BigInteger("0");
					BigInteger	sum2 = new BigInteger("0");

						// first entry should be us

					for (int i=1;i<Math.min( l.size(), contacts_to_use );i++){

						DHTTransportContact	node = (DHTTransportContact)l.get(i);

						byte[]	dist = computeDistance( id, node.getID());

						BigInteger b_dist = IDToBigInteger( dist );

						BigInteger	b_i = new BigInteger(""+i);

						sum1 = sum1.add( b_i.multiply(b_dist));

						sum2 = sum2.add( b_i.multiply( b_i ));
					}

					byte[]	max = new byte[id.length+1];

					max[0] = 0x01;

					long this_estimate;

					if ( sum1.compareTo( new BigInteger("0")) == 0 ){

						this_estimate = 0;

					}else{

						this_estimate = IDToBigInteger(max).multiply( sum2 ).divide( sum1 ).longValue();
					}

						// there's always us!!!!

					if ( this_estimate < 1 ){

						this_estimate	= 1;
					}

					local_estimate_values.put( new HashWrapper( id ), new Long( this_estimate ));

					long	new_estimate	= 0;

					Iterator	it = local_estimate_values.values().iterator();

					String	sizes = "";

					while( it.hasNext()){

						long	estimate = ((Long)it.next()).longValue();

						sizes += (sizes.length()==0?"":",") + estimate;

						new_estimate += estimate;
					}

					local_dht_estimate = new_estimate/local_estimate_values.size();

					// System.out.println( "getEstimatedDHTSize: " + sizes + "->" + dht_estimate + " (id=" + DHTLog.getString2(id) + ",cont=" + (contacts==null?"null":(""+contacts.size())) + ",use=" + contacts_to_use );
				}

				List rems = new ArrayList(new TreeSet( remote_estimate_values ));

				// ignore largest and smallest few values

				long	rem_average = local_dht_estimate;
				int		rem_vals	= 1;

				for (int i=3;i<rems.size()-3;i++){

					rem_average += ((Integer)rems.get(i)).intValue();

					rem_vals++;
				}

				combined_dht_estimate = rem_average / rem_vals;

				// System.out.println( "estimateDHTSize: loc =" + local_dht_estimate + ", comb = " + combined_dht_estimate + " [" + remote_estimate_values.size() + "]");
			}finally{

				estimate_mon.exit();
			}
		}
	}

	protected BigInteger
	IDToBigInteger(
		byte[]		data )
	{
		String	str_key = "";

		for (int i=0;i<data.length;i++){

			String	hex = Integer.toHexString( data[i]&0xff );

			while( hex.length() < 2 ){

				hex = "0" + hex;
			}

			str_key += hex;
		}

		BigInteger	res		= new BigInteger( str_key, 16 );

		return( res );
	}

	protected int
	generateSpoofID(
		DHTTransportContact	contact )
	{
		if ( spoof_cipher == null  ){

			return( 0 );
		}

		try{
			spoof_mon.enter();

			spoof_cipher.init(Cipher.ENCRYPT_MODE, spoof_key );

			byte[]	address = contact.getAddress().getAddress().getAddress();

			byte[]	data_out = spoof_cipher.doFinal( address );

			int	res =  	(data_out[0]<<24)&0xff000000 |
						(data_out[1] << 16)&0x00ff0000 |
						(data_out[2] << 8)&0x0000ff00 |
						data_out[3]&0x000000ff;

			// System.out.println( "anti-spoof: generating " + res + " for " + contact.getAddress());

			return( res );

		}catch( Throwable e ){

			logger.log(e);

		}finally{

			spoof_mon.exit();
		}

		return( 0 );
	}

	public boolean
	verifyContact(
		DHTTransportContact 	c,
		boolean					direct )
	{
		boolean	ok = c.getRandomID() == generateSpoofID( c );

		if ( DHTLog.CONTACT_VERIFY_TRACE ){

			System.out.println( "    net " + transport.getNetwork() +"," + (direct?"direct":"indirect") + " verify for " + c.getName() + " -> " + ok + ", version = " + c.getProtocolVersion());
		}

		return( ok );
	}

	public List
	getContacts()
	{
		List	contacts = router.getAllContacts();

		List	res = new ArrayList( contacts.size());

		for (int i=0;i<contacts.size();i++){

			DHTRouterContact	rc = (DHTRouterContact)contacts.get(i);

			res.add( rc.getAttachment());
		}

		return( res );
	}

	public void
	print()
	{
		DHTNetworkPosition[]	nps = transport.getLocalContact().getNetworkPositions();

		String	np_str = "";

		for (int j=0;j<nps.length;j++){
			np_str += (j==0?"":",") + nps[j];
		}

		logger.log( "DHT Details: external IP = " + transport.getLocalContact().getAddress() +
						", network = " + transport.getNetwork() +
						", protocol = V" + transport.getProtocolVersion() +
						", nps = " + np_str );

		router.print();

		database.print();

		/*
		List	c = getContacts();

		for (int i=0;i<c.size();i++){

			DHTControlContact	cc = (DHTControlContact)c.get(i);

			System.out.println( "    " + cc.getTransportContact().getVivaldiPosition());
		}
		*/
	}

	public List
	sortContactsByDistance(
		List		contacts )
	{
		Set	sorted_contacts = new sortedTransportContactSet( router.getID(), true ).getSet();

		sorted_contacts.addAll( contacts );

		return( new ArrayList( sorted_contacts ));
	}

	protected static class
	sortedTransportContactSet
	{
		private TreeSet	tree_set;

		private byte[]	pivot;
		private boolean	ascending;

		protected
		sortedTransportContactSet(
			byte[]		_pivot,
			boolean		_ascending )
		{
			pivot		= _pivot;
			ascending	= _ascending;

			tree_set = new TreeSet(
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
							// this comparator ensures that the closest to the key
							// is first in the iterator traversal

						DHTTransportContact	t1 = (DHTTransportContact)o1;
						DHTTransportContact t2 = (DHTTransportContact)o2;

						int	distance = computeAndCompareDistances2( t1.getID(), t2.getID(), pivot );

						if ( ascending ){

							return( distance );

						}else{

							return( -distance );
						}
					}
				});
		}

		public Set
		getSet()
		{
			return( tree_set );
		}
	}

	protected static class
	DHTOperationListenerDemuxer
		implements DHTOperationListener
	{
		private final AEMonitor	this_mon = new AEMonitor( "DHTOperationListenerDemuxer" );

		private final DHTOperationListener	delegate;

		private boolean		complete_fired;
		private boolean		complete_included_ok;

		private int			complete_count	= 0;

		protected
		DHTOperationListenerDemuxer(
			DHTOperationListener	_delegate )
		{
			delegate	= _delegate;

			if ( delegate == null ){

				Debug.out( "invalid: null delegate" );
			}
		}

		public void
		incrementCompletes()
		{
			try{
				this_mon.enter();

				complete_count++;

			}finally{

				this_mon.exit();
			}
		}

		public void
		searching(
			DHTTransportContact	contact,
			int					level,
			int					active_searches )
		{
			delegate.searching( contact, level, active_searches );
		}

		public void
		diversified()
		{
			delegate.diversified();
		}

		public void
		found(
			DHTTransportContact	contact )
		{
			delegate.found( contact );
		}

		public void
		read(
			DHTTransportContact	contact,
			DHTTransportValue	value )
		{
			delegate.read( contact, value );
		}

		public void
		wrote(
			DHTTransportContact	contact,
			DHTTransportValue	value )
		{
			delegate.wrote( contact, value );
		}

		public void
		complete(
			boolean				timeout )
		{
			boolean	fire	= false;

			try{
				this_mon.enter();

				if ( !timeout ){

					complete_included_ok	= true;
				}

				complete_count--;

				if (complete_count <= 0 && !complete_fired ){

					complete_fired	= true;
					fire			= true;
				}
			}finally{

				this_mon.exit();
			}

			if ( fire ){

				delegate.complete( !complete_included_ok );
			}
		}
	}

	abstract static class
	lookupResultHandler
		extends DHTOperationListenerDemuxer
	{
		protected
		lookupResultHandler(
			DHTOperationListener	delegate )
		{
			super( delegate );
		}

		public abstract void
		closest(
			List		res );

		public abstract void
		diversify(
			DHTTransportContact	cause,
			byte				diversification_type );

	}


	protected static class
	DHTTransportFindValueReplyImpl
		implements DHTTransportFindValueReply
	{
		private byte					dt = DHT.DT_NONE;
		private DHTTransportValue[]		values;
		private DHTTransportContact[]	contacts;
		private byte[]					blocked_key;
		private byte[]					blocked_sig;

		protected
		DHTTransportFindValueReplyImpl(
			byte				_dt,
			DHTTransportValue[]	_values )
		{
			dt		= _dt;
			values	= _values;
		}

		protected
		DHTTransportFindValueReplyImpl(
			DHTTransportContact[]	_contacts )
		{
			contacts	= _contacts;
		}

		protected
		DHTTransportFindValueReplyImpl(
			byte[]		_blocked_key,
			byte[]		_blocked_sig )
		{
			blocked_key	= _blocked_key;
			blocked_sig	= _blocked_sig;
		}

		public byte
		getDiversificationType()
		{
			return( dt );
		}

		public boolean
		hit()
		{
			return( values != null );
		}

		public boolean
		blocked()
		{
			return( blocked_key != null );
		}

		public DHTTransportValue[]
		getValues()
		{
			return( values );
		}

		public DHTTransportContact[]
		getContacts()
		{
			return( contacts );
		}

		public byte[]
		getBlockedKey()
		{
			return( blocked_key );
		}

		public byte[]
		getBlockedSignature()
		{
			return( blocked_sig );
		}
	}

	protected static class
	DHTTransportStoreReplyImpl
		implements DHTTransportStoreReply
	{
		private byte[]	divs;
		private byte[]	block_request;
		private byte[]	block_sig;

		protected
		DHTTransportStoreReplyImpl(
			byte[]		_divs )
		{
			divs	= _divs;
		}

		protected
		DHTTransportStoreReplyImpl(
			byte[]		_bk,
			byte[]		_bs )
		{
			block_request	= _bk;
			block_sig		= _bs;
		}

		public byte[]
		getDiversificationTypes()
		{
			return( divs );
		}

		public boolean
		blocked()
		{
			return( block_request != null );
		}

		public byte[]
		getBlockRequest()
		{
			return( block_request );
		}

		public byte[]
		getBlockSignature()
		{
			return( block_sig );
		}
	}

	protected abstract class
	task
		extends ThreadPoolTask
	{
		private final controlActivity	activity;

		protected
		task(
			ThreadPool	thread_pool )
		{
			activity = new controlActivity( thread_pool, this );

			try{

				activity_mon.enter();

				activities.add( activity );

				listeners.dispatch( DHTControlListener.CT_ADDED, activity );

				// System.out.println( "activity added:" + activities.size());

			}finally{

				activity_mon.exit();
			}
		}

		@Override
		public void
		taskStarted()
		{
			listeners.dispatch( DHTControlListener.CT_CHANGED, activity );

			//System.out.println( "activity changed:" + activities.size());
		}

		@Override
		public void
		taskCompleted()
		{
			try{
				activity_mon.enter();

				activities.remove( activity );

				listeners.dispatch( DHTControlListener.CT_REMOVED, activity );

				// System.out.println( "activity removed:" + activities.size());

			}finally{

				activity_mon.exit();
			}
		}

		@Override
		public void
		interruptTask()
		{
		}

		public abstract byte[]
		getTarget();

		public abstract String
		getDescription();
	}

	protected class
	controlActivity
		implements DHTControlActivity
	{
		protected ThreadPool	tp;
		protected task			task;
		protected int			type;

		protected
		controlActivity(
			ThreadPool	_tp,
			task		_task )
		{
			tp		= _tp;
			task	= _task;

			if ( _tp == internal_lookup_pool ){

				type	= DHTControlActivity.AT_INTERNAL_GET;

			}else if ( _tp == external_lookup_pool ){

				type	= DHTControlActivity.AT_EXTERNAL_GET;

			}else if ( _tp == internal_put_pool ){

				type	= DHTControlActivity.AT_INTERNAL_PUT;

			}else{

				type	= DHTControlActivity.AT_EXTERNAL_PUT;
			}
		}

		public byte[]
		getTarget()
		{
			return( task.getTarget());
		}

		public String
		getDescription()
		{
			return( task.getDescription());
		}

		public int
		getType()
		{
			return( type );
		}

		public boolean
		isQueued()
		{
			return( tp.isQueued( task ));
		}

		public String
		getString()
		{
			return( type + ":" + DHTLog.getString( getTarget()) + "/" + getDescription() + ", q = " + isQueued());
		}
	}
}
