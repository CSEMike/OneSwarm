/*
 * Created on Jul 16, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.lws;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerManagerFactory;
import org.gudy.azureus2.core3.peer.PEPeerManagerListener;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerListener;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponsePeer;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistrationAdapter;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;



public class 
LightWeightSeed 
	extends LogRelation
	implements PeerManagerRegistrationAdapter
{
	private static final byte	ACT_NONE					= 0;
	private static final byte	ACT_HAS_PEERS				= 2;
	private static final byte	ACT_HAS_POTENTIAL_PEERS		= 3;
	private static final byte	ACT_INCOMING				= 4;
	private static final byte	ACT_NO_PM					= 5;
	private static final byte	ACT_TIMING_OUT				= 6;
	private static final byte	ACT_TRACKER_ANNOUNCE		= 7;
	private static final byte	ACT_TRACKER_SCRAPE			= 8;
	
	private static final int DEACTIVATION_TIMEOUT					= 5*60*1000;
	private static final int DEACTIVATION_WITH_POTENTIAL_TIMEOUT	= 15*60*1000;

	private LightWeightSeedManager		manager;
	private LightWeightSeedAdapter		adapter;
	private String						name;
	private HashWrapper					hash;
	private URL							announce_url;
	private File						data_location;
	
	private PeerManagerRegistration		peer_manager_registration;

	private PEPeerManager				peer_manager;
	private LWSDiskManager				disk_manager;
	private LWSDownload					pseudo_download;
	private LWSTorrent					torrent_facade;
	private TRTrackerAnnouncer			announcer;
	
	private TOTorrent					actual_torrent;
	
	private boolean		is_running;
	private long		last_activity_time;
	private int			activation_state		= ACT_NONE;
	
	protected 
	LightWeightSeed(
		LightWeightSeedManager	_manager,
		String					_name,
		HashWrapper				_hash,
		URL						_announce_url,
		File					_data_location,
		LightWeightSeedAdapter	_adapter )
	{
		manager			= _manager;
		name			= _name;
		hash			= _hash;
		announce_url	= _announce_url;
		data_location	= _data_location;
		adapter			= _adapter;
	}
	
	protected String
	getName()
	{
		return( name + "/" + ByteFormatter.encodeString( hash.getBytes()));
	}
	
	protected Torrent
	getTorrent()
	{
		return( new TorrentImpl( getTOTorrent( false )));
	}
	
	protected TOTorrent
	getTOTorrent(
		boolean		actual )
	{
		if ( actual ){
			
			synchronized( this ){
				
				if ( actual_torrent == null ){
					
					try{
					
						actual_torrent = adapter.getTorrent( hash.getBytes(), announce_url, data_location );
						
					}catch( Throwable e ){
						
						log( "Failed to get torrent", e );
					}
					
					if ( actual_torrent == null ){
						
						throw( new RuntimeException( "Torrent not available" ));
					}
				}
				
				return( actual_torrent );
			}
		}else{
			
			return( torrent_facade );
		}
	}
	
	public HashWrapper
	getHash()
	{
		return( hash );
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	public File
	getDataLocation()
	{
		return( data_location );
	}
	
	protected long
	getSize()
	{
		return( data_location.length());
	}
	
	public boolean
	isPeerSourceEnabled(
		String		peer_source )
	{
		return( true );
	}
	
	public boolean 
	manualRoute(
		NetworkConnection connection )
	{
		return false;
	}
	
	public byte[][] 
	getSecrets() 
	{
		return( new byte[][]{ hash.getBytes()});
	}
	
	public boolean
	activateRequest(
		InetSocketAddress		remote_address )
	{
		ensureActive( "Incoming[" + remote_address.getAddress().getHostAddress() + "]", ACT_INCOMING );
		
		return( true );
	}
	
	public void
	deactivateRequest(
		InetSocketAddress		remote_address )
	{
		
	}
	
	public String
	getDescription()
	{
		return( "LWS: " + getName());
	}
	
	protected synchronized void
	start()
	{
		log( "Start" );
				
		if ( is_running ){
			
			log( "Start of '" + getString() + "' failed - already running" );

			return;
		}
		
		if ( peer_manager_registration != null ){
			
			log( "Start of '" + getString() + "' failed - router already registered" );

			return;
		}
		
		if ( pseudo_download != null ){
			
			log( "Start of '" + getString() + "' failed - pseudo download already registered" );

			return;
		}
		
		if ( disk_manager != null ){
			
			log( "Start of '" + getString() + "' failed - disk manager already started" );
			
			return;
		}
	
		if ( peer_manager != null ){
			
			log( "Start of '" + getString() + "' failed - peer manager already started" );
			
			return;
		}
		
		try{
			if ( torrent_facade == null ){
				
				torrent_facade = new LWSTorrent( this );
			}
			
			peer_manager_registration = PeerManager.getSingleton().registerLegacyManager( hash, this );
			
			announcer = createAnnouncer();
				
			pseudo_download = new LWSDownload( this, announcer );
				
			manager.addToDHTTracker( pseudo_download );
		
			is_running	= true;

			last_activity_time = SystemTime.getMonotonousTime();
			
		}catch( Throwable e ){
						
			log( "Start of '" + getString() + "' failed", e );
			
		}finally{
			
			if ( is_running ){
										
				log( "Started " + getString());

			}else{
				
				stop();
			}
		}
	}
	
	protected synchronized void
	stop()
	{
		log( "Stop" );
		
		try{
			if ( disk_manager != null ){
				
				disk_manager.stop( false );
			
				disk_manager	= null;		
			}
			
			if ( peer_manager != null ){
				
				peer_manager.stopAll();
				
				peer_manager = null;
			}

			if ( pseudo_download != null ){
				
				manager.removeFromDHTTracker( pseudo_download );
				
				pseudo_download = null;
			}

			if ( announcer != null ){
				
				announcer.stop( false );
				
				announcer.destroy();
				
				announcer = null;
			}
			
			if ( peer_manager_registration != null ){
				
				peer_manager_registration.unregister();
				
				peer_manager_registration	= null;
			}
						
		}finally{
			
			is_running 	= false;
			
			activation_state 	= ACT_NONE;
			
			log( "Stopped " + getString());
		}
	}
	
	protected synchronized void
	activate(
		String		reason_str,
		byte		activation_reason )
	{
		log( "Activate: " + activation_reason + "/" + reason_str );
		
		if ( activation_state != ACT_NONE ){
			
			return;
		}
		
		try{			
			disk_manager = new LWSDiskManager( this, data_location );
			
			disk_manager.start();
			
			if ( disk_manager.getState() != DiskManager.READY ){
									
				log( "Start of '" + getString() + "' failed, disk manager failed = " + disk_manager.getErrorMessage());
				
			}else{
				
				peer_manager = 
					PEPeerManagerFactory.create( 
								announcer.getPeerId(), 
								new LWSPeerManagerAdapter( 
										this, 
										peer_manager_registration ), 
								disk_manager );
	
				peer_manager.addListener(
					new PEPeerManagerListener()
					{
						public void 
						peerAdded( 
							final PEPeerManager 	manager, 
							final PEPeer 			peer )
						{
							last_activity_time = SystemTime.getMonotonousTime();
						}
						  

						public void 
						peerRemoved( 
							PEPeerManager 	manager, 
							PEPeer 			peer )
						{
							last_activity_time = SystemTime.getMonotonousTime();
						}
						
						public void 
						peerDiscovered(
							PEPeerManager manager,
							PeerItem peer, 
							PEPeer finder) 
						{
						}
						  
						public void 
						pieceAdded( 
							PEPeerManager 	manager, 
							PEPiece 		peice, 
							PEPeer 			for_peer )
						{
						}
						  
						public void 
						pieceRemoved( 
							PEPeerManager 	manager, 
							PEPiece 		peice )
						{
						}

						public void 
						peerSentBadData(
							PEPeerManager 	manager,
							PEPeer 			peer, 
							int 			pieceNumber) 
						{							
						}
						public void
						destroyed()
						{
						}
					});
				
				peer_manager.start();
			
				announcer.update( true );
											
				activation_state	= activation_reason;
				
				last_activity_time = SystemTime.getMonotonousTime();
			}
			
		}catch( Throwable e ){
						
			log( "Activation of '" + getString() + "' failed", e );
			
		}finally{
			
			if ( activation_state != ACT_NONE ){
					

			}else{
				
				deactivate();
			}
		}
	}
	
	protected synchronized void
	deactivate()
	{
		log( "Deactivate" );

		try{
			if ( disk_manager != null ){
				
				disk_manager.stop( false );
			
				disk_manager	= null;		
			}
			
			if ( peer_manager != null ){
				
				peer_manager.stopAll();
				
				peer_manager = null;
			}
		}finally{
			
			activation_state = ACT_NONE;
		}
	}
	
	protected synchronized TRTrackerAnnouncer
	createAnnouncer()		
		throws TRTrackerAnnouncerException 
	{
		
			// use a facade here to delay loading the actual torrent until the 
			// download is activated
	
		TRTrackerAnnouncer result = TRTrackerAnnouncerFactory.create( torrent_facade, true );
		
		result.addListener(
				new TRTrackerAnnouncerListener()
				{
					public void
					receivedTrackerResponse(
						TRTrackerAnnouncerResponse	response )
					{
						TRTrackerAnnouncerResponsePeer[] peers = response.getPeers();
						
							// tracker shouldn't return seeds to seeds to we can assume
							// that if peers returned this means we have someone to talk to
					
						if ( peers != null && peers.length > 0 ){
						
							ensureActive( "Tracker[" + peers[0].getAddress()+ "]", ACT_TRACKER_ANNOUNCE );
							
						}else if ( response.getScrapeIncompleteCount() > 0 ){
							
							ensureActive( "Tracker[scrape]", ACT_TRACKER_SCRAPE );
						}
						
						PEPeerManager	pm = peer_manager;
						
						if ( pm != null ){
															
							pm.processTrackerResponse( response );
						}
					}
	
					public void
					urlChanged(
						TRTrackerAnnouncer	announcer,
						URL					old_url,
						URL					new_url,
						boolean				explicit )
					{
					}
						
					public void
					urlRefresh()
					{	
					}
				});
			
		result.setAnnounceDataProvider(
				new TRTrackerAnnouncerDataProvider()
				{
					public String
					getName()
					{
						return( LightWeightSeed.this.getName());
					}
					
					public long
					getTotalSent()
					{
						return( 0 );
					}
					
					public long
					getTotalReceived()
					{
						return( 0 );
					}
					
	    			public long 
	    			getFailedHashCheck() 
	    			{
	    				return( 0 );
	    			}
	    			
					public long
					getRemaining()
					{
						return( 0 );
					}
					
					public String
					getExtensions()
					{
						return( null );
					}
					
					public int
					getMaxNewConnectionsAllowed()
					{
						PEPeerManager	pm = peer_manager;

						if ( pm == null ){
							
								// got to ask for at least one to trigger activation!
							
							return( 8 );
						}
						
						return( PeerUtils.numNewConnectionsAllowed( pm.getPeerIdentityDataID(),0));
					}
					
					public int 
					getPendingConnectionCount() 
					{
						PEPeerManager	pm = peer_manager;

						if ( pm == null ){
							
							return( 0 );
						}
						
						return( pm.getPendingPeerCount());
					}
					
					public int
					getConnectedConnectionCount()
					{
						PEPeerManager	pm = peer_manager;

						if ( pm == null ){
							
							return( 0 );
						}
						
						return( pm.getNbPeers() + pm.getNbSeeds());
					}
					
					public int 
					getUploadSpeedKBSec(
						boolean estimate) 
					{
						return 0;
					}
					
					public int 
					getCryptoLevel() 
					{
						return( NetworkManager.CRYPTO_OVERRIDE_NONE );
					}
					
					public boolean
					isPeerSourceEnabled(
						String		peer_source )
					{
						return( true );
					}
					
					public void
					setPeerSources(
						String[]	sources )
					{
					}
				});
		
		return( result );
	}
	
	protected synchronized void
	ensureActive(
		String	reason,
		byte	a_reason )
	{
		if ( is_running && activation_state == ACT_NONE ){
			
			activate( reason, a_reason );
		}
	}
	
	protected synchronized void
	checkDeactivation()
	{
		if ( activation_state == ACT_NONE ){
			
			return;
		}
		
		if ( peer_manager == null ){
			
			activation_state	= ACT_NO_PM;
			
			return;
		}

		
		if ( peer_manager.getNbPeers() > 0 ){
			
			activation_state	= ACT_HAS_PEERS;

			return;
		}
		
		long	now = SystemTime.getMonotonousTime();
				
		long	millis_since_last_act = now - last_activity_time;
		
		if ( peer_manager.hasPotentialConnections()){
			
			if ( millis_since_last_act < DEACTIVATION_WITH_POTENTIAL_TIMEOUT ){
				
				activation_state	= ACT_HAS_POTENTIAL_PEERS;

				return;
			}
		}
		
		if ( millis_since_last_act >= DEACTIVATION_TIMEOUT ){
			
			deactivate();
			
		}else{
			
			activation_state	= ACT_TIMING_OUT;
		}
	}
	
	public void 
	enqueueReadRequest( 
		PEPeer							peer,
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener 	listener )
	{
		LWSDiskManager	dm = disk_manager;
		
		if ( dm == null ){
			
			listener.readFailed( request, new Throwable( "download is stopped" ));
			
		}else{
		
			dm.enqueueReadRequest( request, listener );
		}
	}
	
	public void
	remove()
	{
		manager.remove( this );
	}
	
	public String 
	getRelationText() 
	{
		return "LWS: '" + getName() + "'";
	}

	public Object[] 
	getQueryableInterfaces() 
	{
		return new Object[]{};
	}
	
	public LogRelation
	getRelation()
	{
		return( this );
	}
	
	protected String
	getString()
	{
		return( getName());
	}
	
	protected void
	log(
		String		str )
	{
		Logger.log(new LogEvent(this, LogIDs.CORE, str ));
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		Logger.log(new LogEvent(this, LogIDs.CORE, str, e ));
	}
}
