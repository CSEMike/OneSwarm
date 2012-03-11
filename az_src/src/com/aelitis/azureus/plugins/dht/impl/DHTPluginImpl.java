/*
 * Created on 24-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved
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

package com.aelitis.azureus.plugins.dht.impl;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.DHTStorageKeyStats;

import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncherAdapter;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;

import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginKeyStats;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;


/**
 * @author parg
 *
 */

public class 
DHTPluginImpl
{
	private static final String	SEED_ADDRESS_V4	= Constants.DHT_SEED_ADDRESS_V4;
	private static final String	SEED_ADDRESS_V6	= Constants.DHT_SEED_ADDRESS_V6;
	private static final int	SEED_PORT		= 6881;
		
	private static final long	MIN_ROOT_SEED_IMPORT_PERIOD	= 8*60*60*1000;
	
		
	private PluginInterface		plugin_interface;
	
	private int					status;
	private String				status_text;
	
	private ActionParameter		reseed_param;
	private BooleanParameter	warn_user_param;
	
	private DHT					dht;
	private int					port;
	private byte				protocol_version;
	private int					network;	
	private boolean				v6;
	private DHTTransportUDP		transport;

	private DHTPluginStorageManager storage_manager;

	private long				last_root_seed_import_time;
			
	private LoggerChannel		log;
	private DHTLogger			dht_log;
	
	private int					stats_ticks;
	
	public
	DHTPluginImpl(
		PluginInterface			_plugin_interface,
		DHTNATPuncherAdapter	_nat_adapter,
		DHTPluginImplAdapter	_adapter,
		byte					_protocol_version,
		int						_network,
		boolean					_v6,
		String					_ip,
		int						_port,
		ActionParameter			_reseed,
		BooleanParameter		_warn_user_param,
		boolean					_logging,
		LoggerChannel			_log,
		DHTLogger				_dht_log )
	{
		plugin_interface	= _plugin_interface;
		protocol_version	= _protocol_version;
		network				= _network;
		v6					= _v6;
		port				= _port;
		reseed_param		= _reseed;
		warn_user_param		= _warn_user_param;
		log					= _log;
		dht_log				= _dht_log;
		
		final DHTPluginImplAdapter	adapter = _adapter;
		
		try{
			storage_manager = new DHTPluginStorageManager( network, dht_log, getDataDir( _network ));
			
			final PluginConfig conf = plugin_interface.getPluginconfig();
			
			int	send_delay = conf.getPluginIntParameter( "dht.senddelay", 25 );
			int	recv_delay	= conf.getPluginIntParameter( "dht.recvdelay", 25 );
			
			boolean	bootstrap	= conf.getPluginBooleanParameter( "dht.bootstrapnode", false );
			
				// start off optimistic with reachable = true
			
			boolean	initial_reachable	= conf.getPluginBooleanParameter( "dht.reachable." + network, true );
			
			transport = 
				DHTTransportFactory.createUDP( 
						_protocol_version,
						_network,
						_v6,
						_ip,
						storage_manager.getMostRecentAddress(),
						_port, 
						3,
						1,
						10000, 	// udp timeout - tried less but a significant number of 
								// premature timeouts occurred
						send_delay, recv_delay, 
						bootstrap,
						initial_reachable,
						dht_log );
			
			transport.addListener(
				new DHTTransportListener()
				{
					public void
					localContactChanged(
						DHTTransportContact	local_contact )
					{
						storage_manager.localContactChanged( local_contact );
						
						if ( adapter != null ){
							
							adapter.localContactChanged( getLocalAddress());
						}
					}
					
					public void
					resetNetworkPositions()
					{
					}
					
					public void
					currentAddress(
						String		address )
					{
						storage_manager.recordCurrentAddress( address );
					}
					
					public void
					reachabilityChanged(
						boolean	reacheable )
					{
					}
				});
							
			Properties	props = new Properties();
			
			/*
			System.out.println( "FRIGGED REFRESH PERIOD" );
			
			props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 5*60*1000 ));
			*/
				
			if ( _network == DHT.NW_CVS ){
				
					// reduce network usage
				
				//System.out.println( "CVS DHT cache republish interval modified" );

				props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 1*60*60*1000 ));
			}
			
			dht = DHTFactory.create( 
						transport, 
						props,
						storage_manager,
						_nat_adapter,
						dht_log );
			
			plugin_interface.firePluginEvent(
				new PluginEvent()
				{
					public int
					getType()
					{
						return( DHTPlugin.EVENT_DHT_AVAILABLE );
					}
					
					public Object
					getValue()
					{
						return( dht );
					}
				});
			
			dht.setLogging( _logging );
			
			DHTTransportContact root_seed = importRootSeed();
			
			storage_manager.importContacts( dht );
			
			plugin_interface.getUtilities().createTimer( "DHTExport", true ).addPeriodicEvent(
					10*60*1000,
					new UTTimerEventPerformer()
					{
						public void
						perform(
							UTTimerEvent		event )
						{
							checkForReSeed(false);
							
							storage_manager.exportContacts( dht );
						}
					});

			integrateDHT( true, root_seed );
			
			status = DHTPlugin.STATUS_RUNNING;
			
			status_text = "Running";
												
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			log.log( "DHT integrtion fails", e );
			
			status_text = "DHT Integration fails: " + Debug.getNestedExceptionMessage( e );
			
			status	= DHTPlugin.STATUS_FAILED;
		}
	}

	public void
	updateStats(
		int		sample_stats_ticks )
	{
		stats_ticks++;
		
		if ( transport != null ){
				
			PluginConfig conf = plugin_interface.getPluginconfig();
			
			boolean current_reachable = transport.isReachable();
				
			if ( current_reachable != conf.getPluginBooleanParameter( "dht.reachable." + network, true )){
					
					// reachability has changed
				
				conf.setPluginParameter( "dht.reachable." + network, current_reachable );
				
				if ( !current_reachable ){
					
					String msg = "If you have a router/firewall, please check that you have port " + port + 
									" UDP open.\nDecentralised tracking requires this." ;

					int	warned_port = plugin_interface.getPluginconfig().getPluginIntParameter( "udp_warned_port", 0 );
					
					if ( warned_port == port || !warn_user_param.getValue() ){
						
						log.log( msg );
						
					}else{
						
						plugin_interface.getPluginconfig().setPluginParameter( "udp_warned_port", port );
						
						log.logAlert( LoggerChannel.LT_WARNING, msg );
					}
				}else{
					
					log.log( "Reachability changed for the better" );
				}
			}
			
			if ( stats_ticks % sample_stats_ticks == 0 ){

				logStats();
			}
		}	
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public String
	getStatusText()
	{
		return( status_text );
	}
	
	public boolean
	isReachable()
	{
		return( transport.isReachable());
	}
	
	public void
	setLogging(
		boolean		l )
	{
		dht.setLogging( l );
	}
	
	public void
	tick()
	{
	}
	
	public int
	getPort()
	{
		return( port );
	}
	
	public void
	setPort(
		int	new_port )
	{
		port	= new_port;
		
		try{
			transport.setPort( port );
			
		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	public long
	getClockSkew()
	{
		return( transport.getStats().getSkewAverage());
	}
	
	public void
	logStats()
	{
		DHTDBStats			d_stats	= dht.getDataBase().getStats();
		DHTControlStats		c_stats = dht.getControl().getStats();
		DHTRouterStats		r_stats = dht.getRouter().getStats();
		DHTTransportStats 	t_stats = transport.getStats();

		long[]	rs = r_stats.getStats();

		log.log( "DHT:ip=" + transport.getLocalContact().getAddress() + 
					",net=" + transport.getNetwork() +
					",prot=V" + transport.getProtocolVersion()+
					",reach=" + transport.isReachable());

		log.log( 	"Router" +
					":nodes=" + rs[DHTRouterStats.ST_NODES] +
					",leaves=" + rs[DHTRouterStats.ST_LEAVES] +
					",contacts=" + rs[DHTRouterStats.ST_CONTACTS] +
					",replacement=" + rs[DHTRouterStats.ST_REPLACEMENTS] +
					",live=" + rs[DHTRouterStats.ST_CONTACTS_LIVE] +
					",unknown=" + rs[DHTRouterStats.ST_CONTACTS_UNKNOWN] +
					",failing=" + rs[DHTRouterStats.ST_CONTACTS_DEAD]);

		log.log( 	"Transport" + 
					":" + t_stats.getString()); 
				
		int[]	dbv_details = d_stats.getValueDetails();
		
		log.log(    "Control:dht=" + c_stats.getEstimatedDHTSize() + 
				   	", Database:keys=" + d_stats.getKeyCount() +
				   	",vals=" + dbv_details[DHTDBStats.VD_VALUE_COUNT]+
				   	",loc=" + dbv_details[DHTDBStats.VD_LOCAL_SIZE]+
				   	",dir=" + dbv_details[DHTDBStats.VD_DIRECT_SIZE]+
				   	",ind=" + dbv_details[DHTDBStats.VD_INDIRECT_SIZE]+
				   	",div_f=" + dbv_details[DHTDBStats.VD_DIV_FREQ]+
				   	",div_s=" + dbv_details[DHTDBStats.VD_DIV_SIZE] );
	}
	
	protected File
	getDataDir(
		int		network )
	{
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" );
		
		if ( network != 0 ){
			
			dir = new File( dir, "net" + network );
		}
		
		FileUtil.mkdirs(dir);
		
		return( dir );
	}
	
	public void
	integrateDHT(
		boolean				first,
		DHTTransportContact	remove_afterwards )
	{
		try{
			reseed_param.setEnabled( false );						

			log.log( "DHT " + (first?"":"re-") + "integration starts" );
		
			long	start = SystemTime.getCurrentTime();
			
			dht.integrate( false );
			
			if ( remove_afterwards != null ){
				
				log.log( "Removing seed " + remove_afterwards.getString());
				
				remove_afterwards.remove();
			}
			
			long	end = SystemTime.getCurrentTime();
				
			log.log( "DHT " + (first?"":"re-") + "integration complete: elapsed = " + (end-start));
			
			dht.print( false );
			
		}finally{
			
			reseed_param.setEnabled( true );						
		}
	}
	
	public void
	checkForReSeed(
		boolean	force )
	{
		int	seed_limit = 32;
		
		try{
			
			long[]	router_stats = dht.getRouter().getStats().getStats();
		
			if ( router_stats[ DHTRouterStats.ST_CONTACTS_LIVE] < seed_limit || force ){
				
				if ( force ){
					
					log.log( "Reseeding" );
					
				}else{
					
					log.log( "Less than 32 live contacts, reseeding" );
				}
				
				int	peers_imported	= 0;

					// only try boostrapping off connected peers on the main network as it is unlikely
					// any of them are running CVS and hence the boostrap will fail
				
				if ( network == DHT.NW_MAIN || network == DHT.NW_MAIN_V6 ){
					
						// first look for peers to directly import
					
					Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
									
outer:
	
					for (int i=0;i<downloads.length;i++){
						
						Download	download = downloads[i];
						
						PeerManager pm = download.getPeerManager();
						
						if ( pm == null ){
							
							continue;
						}
						
						Peer[] 	peers = pm.getPeers();
						
						for (int j=0;j<peers.length;j++){
							
							Peer	p = peers[j];
							
							int	peer_udp_port = p.getUDPNonDataListenPort();
							
							if ( peer_udp_port != 0 ){
														
								if ( importSeed( p.getIp(), peer_udp_port ) != null ){
									
									peers_imported++;
																
									if ( peers_imported > seed_limit ){
										
										break outer;
									}
								}
							}	
						}
					}
				}
				
				DHTTransportContact	root_to_remove = null;
				
				if ( peers_imported == 0 ){
				
					root_to_remove = importRootSeed();
					
					if ( root_to_remove != null ){
						
						peers_imported++;
					}
				}
				
				if ( peers_imported > 0 ){
					
					integrateDHT( false, root_to_remove );
					
				}else{
					
					log.log( "No valid peers found to reseed from" );
				}
			}
			
		}catch( Throwable e ){
			
			log.log(e);
		}
	}
		
	protected DHTTransportContact
	importRootSeed()
	{
		try{
			long	 now = SystemTime.getCurrentTime();
			
			if ( now - last_root_seed_import_time > MIN_ROOT_SEED_IMPORT_PERIOD ){
		
				last_root_seed_import_time	= now;
				
				return( importSeed( getSeedAddress(), SEED_PORT ));
			
			}else{
				
				log.log( "    root seed imported too recently, ignoring" );
			}
		}catch( Throwable e ){
			
			log.log(e);
		}
		
		return( null );
	}
	
	public DHTTransportContact
	importSeed(
		String		ip,
		int			port )
	{
		try{
			
			return( importSeed( InetAddress.getByName( ip ), port ));
			
		}catch( Throwable e ){
			
			log.log(e);
			
			return( null );
		}
	}
	
	protected DHTTransportContact
	importSeed(
		InetAddress		ia,
		int				port )
	
	{
		try{
			return(
				transport.importContact( new InetSocketAddress(ia, port ), protocol_version ));
		
		}catch( Throwable e ){
			
			log.log(e);
			
			return( null );
		}
	}
	
	protected InetAddress
	getSeedAddress()
	{
		try{
			return( InetAddress.getByName( v6?SEED_ADDRESS_V6:SEED_ADDRESS_V4 ));
		}
		catch (java.net.UnknownHostException e) {
			Debug.out("Could not get DHT seed address: " + e);
			return null;
			
		}catch( Throwable e ){
				
			Debug.printStackTrace( e );
			
			return( null );
		}
	}
	

	public boolean
	isDiversified(
		byte[]		key )
	{
		return( dht.isDiversified( key ));
	}

	public void
	put(
		byte[]						key,
		String						description,
		byte[]						value,
		byte						flags,
		DHTPluginOperationListener	listener)
	{		
		put( key, description, value, flags, true, listener );
	}
	
	public void
	put(
		final byte[]						key,
		final String						description,
		final byte[]						value,
		final byte							flags,
		final boolean						high_priority,
		final DHTPluginOperationListener	listener)
	{		
		dht.put( 	key, 
					description,
					value,
					flags,
					high_priority,
					new DHTOperationListener()
					{
						private boolean started;
						
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							if ( listener != null ){
								
								synchronized( this ){
									
									if ( started ){
										
										return;
									}
									
									started = true;
								}
								
								listener.starts( key );
							}
						}

						public void
						diversified(
							String		desc )
						{
						}
						
						public void
						found(
							DHTTransportContact	contact,
							boolean				is_closest )
						{
						}

						public void
						read(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							Debug.out( "read operation not supported for puts" );
						}
						
						public void
						wrote(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							// log.log( "Put: wrote " + _value.getString() + " to " + _contact.getString());
							
							if ( listener != null ){
								
								listener.valueWritten( new DHTPluginContactImpl(DHTPluginImpl.this, _contact ), mapValue( _value ));
							}

						}
						
						public void
						complete(
							boolean				timeout )
						{
							// log.log( "Put: complete, timeout = " + timeout );
						
							if ( listener != null ){
								
								listener.complete( key, timeout );
							}
						}
					});
	}
	
	public DHTPluginValue
	getLocalValue(
		byte[]		key )
	{
		final DHTTransportValue	val = dht.getLocalValue( key );
		
		if ( val == null ){
			
			return( null );
		}
		
		return( mapValue( val ));
	}
	
	public List<DHTPluginValue>
	getValues()
	{
		DHTDB	db = dht.getDataBase();
		
		Iterator<HashWrapper>	keys = db.getKeys();
		
		List<DHTPluginValue>	vals = new ArrayList<DHTPluginValue>();
		
		while( keys.hasNext()){
			
			DHTDBValue val = db.getAnyValue( keys.next());
			
			if ( val != null ){
				
				vals.add( mapValue( val ));
			}
		}
		
		return( vals );
	}
	
	public void
	get(
		final byte[]								key,
		final String								description,
		final byte									flags,
		final int									max_values,
		final long									timeout,
		final boolean								exhaustive,
		final boolean								high_priority,
		final DHTPluginOperationListener			listener )
	{
		dht.get( 	key, description, flags, max_values, timeout, exhaustive, high_priority, 
					new DHTOperationListener()
					{
						private boolean	started = false;
						
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							if ( listener != null ){
								
								synchronized( this ){
									
									if ( started ){
										
										return;
									}
									
									started = true;
								}
								
								listener.starts( key );
							}
						}
						
						public void
						diversified(
							String		desc )
						{
							if ( listener != null ){
								
								listener.diversified();
							}
						}
						
						public void
						found(
							DHTTransportContact	contact,
							boolean				is_closest )
						{
						}

						public void
						read(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: read " + value.getString() + " from " + contact.getString() + ", originator = " + value.getOriginator().getString());
							
							if ( listener != null ){
								
								listener.valueRead( new DHTPluginContactImpl( DHTPluginImpl.this, value.getOriginator()), mapValue( value ));
							}
						}
						
						public void
						wrote(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: wrote " + value.getString() + " to " + contact.getString());
						}
						
						public void
						complete(
							boolean				_timeout )
						{
							// log.log( "Get: complete, timeout = " + _timeout );
							
							if ( listener != null ){
								
								listener.complete( key, _timeout );
							}
						}
					});
	}
	
	public void
	remove(
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		dht.remove( 	key,
						description,
						new DHTOperationListener()
						{
							private boolean started;
							
							public void
							searching(
								DHTTransportContact	contact,
								int					level,
								int					active_searches )
							{
								if ( listener != null ){
									
									synchronized( this ){
										
										if ( started ){
											
											return;
										}
										
										started = true;
									}
									
									listener.starts( key );
								}							
							}
		
							public void
							found(
								DHTTransportContact	contact,
								boolean				is_closest )
							{
							}

							public void
							diversified(
								String		desc )
							{
							}
							
							public void
							read(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: read " + value.getString() + " from " + contact.getString());
							}
							
							public void
							wrote(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: wrote " + value.getString() + " to " + contact.getString());
								if ( listener != null ){
									
									listener.valueWritten( new DHTPluginContactImpl( DHTPluginImpl.this, contact ), mapValue( value ));
								}
							}
							
							public void
							complete(
								boolean				timeout )
							{
								// log.log( "Remove: complete, timeout = " + timeout );
							
								if ( listener != null ){
								
									listener.complete( key, timeout );
								}
							}			
						});
	}
	
	public void
	remove(
		final DHTPluginContact[]			targets,
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		DHTTransportContact[]	t_contacts = new DHTTransportContact[ targets.length ];
		
		for (int i=0;i<targets.length;i++){
			
			t_contacts[i] = ((DHTPluginContactImpl)targets[i]).getContact();
		}
		
		dht.remove( 	t_contacts,
						key,
						description,
						new DHTOperationListener()
						{
							private boolean started;
							
							public void
							searching(
								DHTTransportContact	contact,
								int					level,
								int					active_searches )
							{
								if ( listener != null ){
									
									synchronized( this ){
										
										if ( started ){
											
											return;
										}
										
										started = true;
									}
									
									listener.starts( key );
								}
							}
							
							public void
							found(
								DHTTransportContact	contact,
								boolean				is_closest )
							{
							}

							public void
							diversified(
								String		desc )
							{
							}
							
							public void
							read(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: read " + value.getString() + " from " + contact.getString());
							}
							
							public void
							wrote(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: wrote " + value.getString() + " to " + contact.getString());
								if ( listener != null ){
									
									listener.valueWritten( new DHTPluginContactImpl( DHTPluginImpl.this, contact ), mapValue( value ));
								}
							}
							
							public void
							complete(
								boolean				timeout )
							{
								// log.log( "Remove: complete, timeout = " + timeout );
							
								if ( listener != null ){
								
									listener.complete( key, timeout );
								}
							}			
						});
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		return( new DHTPluginContactImpl( this, transport.getLocalContact()));
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		try{
			return( new DHTPluginContactImpl( this, transport.importContact( address, protocol_version )));
			
		}catch( DHTTransportException	e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address,
		byte							version )
	{
		try{
			return( new DHTPluginContactImpl( this, transport.importContact( address, version )));
			
		}catch( DHTTransportException	e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
	
		// direct read/write support
	
	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		dht.getTransport().registerTransferHandler( 
				handler_key,
				new DHTTransportTransferHandler()
				{
					public String
					getName()
					{
						return( handler.getName());
					}
					
					public byte[]
					handleRead(
						DHTTransportContact	originator,
						byte[]				key )
					{
						return( handler.handleRead( new DHTPluginContactImpl( DHTPluginImpl.this, originator ), key ));
					}
					
					public byte[]
					handleWrite(
							DHTTransportContact	originator,
						byte[]				key,
						byte[]				value )
					{
						handler.handleWrite( new DHTPluginContactImpl( DHTPluginImpl.this, originator ), key, value );
						
						return( null );
					}
				});
	}
	
	public byte[]
	read(
		final DHTPluginProgressListener	listener,
		DHTPluginContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	{
		try{
			return( dht.getTransport().readTransfer(
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						((DHTPluginContactImpl)target).getContact(), 
						handler_key, 
						key, 
						timeout ));
			
		}catch( DHTTransportException e ){
			
			throw( new RuntimeException( e ));
		}
	}

	public DHT
	getDHT()
	{
		return( dht );
	}
	
	public void
	closedownInitiated()
	{
		storage_manager.exportContacts( dht );
		
		dht.destroy();
	}

	public boolean
	isRecentAddress(
		String		address )
	{
		return( storage_manager.isRecentAddress( address ));
	}
	
	protected DHTPluginValue
	mapValue(
		final DHTTransportValue	value )
	{
		if ( value == null ){
			
			return( null );
		}
		
		return( new DHTPluginValueImpl(value));
	}
	
	
	public DHTPluginKeyStats
	decodeStats(
		DHTPluginValue	value )
	{
		if (( value.getFlags() & DHTPlugin.FLAG_STATS) == 0 ){
			
			return( null );
		}
		
		try{
			DataInputStream	dis = new DataInputStream( new ByteArrayInputStream( value.getValue()));
			
			final DHTStorageKeyStats stats = storage_manager.deserialiseStats( dis );
			
			return( 
				new DHTPluginKeyStats()
				{
					public int
					getEntryCount()
					{
						return( stats.getEntryCount());
					}
					
					public int
					getSize()
					{
						return( stats.getSize());
					}
					
					public int
					getReadsPerMinute()
					{
						return( stats.getReadsPerMinute());
					}
					
					public byte
					getDiversification()
					{
						return( stats.getDiversification());
					}
				});
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}
}
