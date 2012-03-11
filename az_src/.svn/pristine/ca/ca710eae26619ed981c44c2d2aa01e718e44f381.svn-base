/*
 * Created on May 27, 2008
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


package com.aelitis.azureus.plugins.net.buddy.tracker;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.core3.util.SHA1;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerEvent;
import org.gudy.azureus2.plugins.peers.PeerListener2;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerListener;
import org.gudy.azureus2.plugins.peers.PeerStats;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.CopyOnWriteSet;
import com.aelitis.azureus.plugins.net.buddy.*;

public class 
BuddyPluginTracker 
	implements BuddyPluginListener, DownloadManagerListener, BuddyPluginAZ2TrackerListener, DownloadPeerListener
{
	private  static final Object	PEER_KEY		= new Object();		// maps to Download object
	
	private static final Object	PEER_STATS_KEY	= new Object();
	
	public static final int BUDDY_NETWORK_IDLE		= 1;
	public static final int BUDDY_NETWORK_OUTBOUND	= 2;
	public static final int BUDDY_NETWORK_INBOUND	= 3;
	
	private static final int	TRACK_CHECK_PERIOD		= 15*1000;
	private static final int	TRACK_CHECK_TICKS		= TRACK_CHECK_PERIOD/BuddyPlugin.TIMER_PERIOD;

	private static final int	PEER_CHECK_PERIOD		= 60*1000;
	private static final int	PEER_CHECK_TICKS		= PEER_CHECK_PERIOD/BuddyPlugin.TIMER_PERIOD;
	
	private static final int	PEER_RECHECK_PERIOD		= 120*1000;
	private static final int	PEER_RECHECK_TICKS		= PEER_RECHECK_PERIOD/BuddyPlugin.TIMER_PERIOD;

	private static final int	TRACK_INTERVAL			= 10*60*1000;
	
	private static final int	SHORT_ID_SIZE			= 4;
	private static final int	FULL_ID_SIZE			= 20;
	
	private static final int	REQUEST_TRACKER_SUMMARY	= 1;
	private static final int	REPLY_TRACKER_SUMMARY	= 2;
	private static final int	REQUEST_TRACKER_STATUS	= 3;
	private static final int	REPLY_TRACKER_STATUS	= 4;
	private static final int	REQUEST_TRACKER_CHANGE	= 5;
	private static final int	REPLY_TRACKER_CHANGE	= 6;
	private static final int	REQUEST_TRACKER_ADD		= 7;
	private static final int	REPLY_TRACKER_ADD		= 8;
	
	private static final int	RETRY_SEND_MIN			= 5*60*1000;
	private static final int	RETRY_SEND_MAX			= 60*60*1000;
	
	private static final int	BUDDY_NO		= 0;
	private static final int	BUDDY_MAYBE		= 1;
	private static final int	BUDDY_YES		= 2;
	
	private BuddyPlugin		plugin;
	
	private boolean			plugin_enabled;
	private boolean			tracker_enabled;
	private boolean			seeding_only;
	
	private boolean			old_plugin_enabled;
	private boolean			old_tracker_enabled;
	private boolean			old_seeding_only;
	
	private int				network_status = BUDDY_NETWORK_IDLE;
	
	private Set				online_buddies 			= new HashSet();
	private Map				online_buddy_ips		= new HashMap();
	
	private Set				tracked_downloads		= new HashSet();
	private int				download_set_id;
	
	private Set				last_processed_download_set;
	private int				last_processed_download_set_id;
	
	private Map				short_id_map	= new HashMap();
	private Map				full_id_map		= new HashMap();
	
	private Set				actively_tracking	= new HashSet();
		
	private CopyOnWriteSet	buddy_peers	= new CopyOnWriteSet();
	
	private CopyOnWriteList	listeners = new CopyOnWriteList();
	
	private TimerEventPeriodic	buddy_stats_timer;
	
	private Average buddy_receive_speed = Average.getInstance(1000, 10);
	
	private Average buddy_send_speed 	= Average.getInstance(1000, 10);
	
	public
	BuddyPluginTracker(
		BuddyPlugin					_plugin,
		BasicPluginConfigModel		_config )
	{
		plugin		= _plugin;
		
		final BooleanParameter te = _config.addBooleanParameter2("azbuddy.tracker.enabled", "azbuddy.tracker.enabled", true );
		
		tracker_enabled = te.getValue();
		
		te.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					tracker_enabled = te.getValue();
					
					checkEnabledState();
				}
			});
		
		// Assumed if we already have a plugin reference, that the
		// Azureus Core is available
		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		
		gm.addListener(
			new GlobalManagerAdapter()
			{
				public void 
				seedingStatusChanged( 
					boolean seeding_only_mode,
					boolean potentially_seeding_only )
				{
					seeding_only = potentially_seeding_only;
					
					checkEnabledState();
				}
			}, false );
		
		seeding_only = gm.isPotentiallySeedingOnly();
		
		checkEnabledState();
	}
	
	public void
	initialise()
	{
		plugin_enabled = plugin.isEnabled();
		
		checkEnabledState();
		
		List buddies = plugin.getBuddies();
		
		for (int i=0;i<buddies.size();i++){
			
			buddyAdded((BuddyPluginBuddy)buddies.get(i));
		}
		
		plugin.addListener( this );
		
		plugin.getAZ2Handler().addTrackerListener( this );

		plugin.getPluginInterface().getDownloadManager().addListener( this, true );
	}
	
	public void
	tick(
		int		tick_count )
	{
		if ( tick_count % TRACK_CHECK_TICKS == 0 ){
			
			checkTracking();
		}
		
		if ( ( tick_count-1 ) % TRACK_CHECK_TICKS == 0 ){
			
			doTracking();
		}
		
		if ( tick_count % PEER_CHECK_TICKS == 0 ){
			
			checkPeers();
		}
		
		if ( tick_count % PEER_RECHECK_TICKS == 0 ){
			
			recheckPeers();
		}
	}
	
	public int
	getNetworkStatus()
	{
		return( network_status );
	}
	
	public long
	getNetworkReceiveBytesPerSecond()
	{
		return( buddy_receive_speed.getAverage());
	}
	
	public long
	getNetworkSendBytesPerSecond()
	{
		return( buddy_send_speed.getAverage());
	}
	
	protected void
	doTracking()
	{
		if ( !( plugin_enabled && tracker_enabled )){
			
			return;
		}

		Map	to_do = new HashMap();
		
		Set active_set = new HashSet();
		
		synchronized( online_buddies ){

			Iterator it = online_buddies.iterator();
				
			while( it.hasNext()){
				
				BuddyPluginBuddy	buddy = (BuddyPluginBuddy)it.next();
				
				buddyData buddy_data = getBuddyData( buddy );
				
				Map active = buddy_data.getDownloadsToTrack();
				
				if ( active.size() > 0 ){
					
					Iterator it2 = active.entrySet().iterator();
					
					List track_now = new ArrayList();
					
					while( it2.hasNext()){
						
						Map.Entry entry = (Map.Entry)it2.next();
						
						Download 	dl 	= (Download)entry.getKey();
						boolean		now = ((Boolean)entry.getValue()).booleanValue();
						
						if ( now ){
							
							track_now.add( dl );
						}
						
						active_set.add( dl );
					}
					
					if( track_now.size() > 0 ){
					
						to_do.put( buddy, track_now );
					}
				}
			}
		}
		
		synchronized( actively_tracking ){
			
			Iterator it = active_set.iterator();
			
			while( it.hasNext()){
				
				Download dl = (Download)it.next();
				
				if ( !actively_tracking.contains( dl )){
					
					actively_tracking.add( dl );
					
					trackPeers( dl );
				}
			}
			
			it = actively_tracking.iterator();
			
			while( it.hasNext()){
				
				Download dl = (Download)it.next();
				
				if ( !active_set.contains( dl )){
					
					it.remove();
					
					untrackPeers( dl );
				}
			}
		}
		
		Iterator it = to_do.entrySet().iterator();
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
					
			BuddyPluginBuddy buddy = (BuddyPluginBuddy)entry.getKey();
			
			if ( !buddy.isOnline( false )){
				
				continue;
			}
			
			InetAddress ip 			= buddy.getAdjustedIP();
			
			if ( ip == null ){
				
				continue;
			}
			
			int			tcp_port	= buddy.getTCPPort();
			int			udp_port	= buddy.getUDPPort();
			
			List	downloads = (List)entry.getValue();
			
			for (int i=0;i<downloads.size();i++){
				
				Download	download = (Download)downloads.get(i);
				
				PeerManager pm = download.getPeerManager();
				
				if ( pm == null ){
					
					continue;
				}
				
				Peer[] existing_peers = pm.getPeers( ip.getHostAddress());
			
				boolean	connected = false;
				
				for (int j=0;j<existing_peers.length;j++){
					
					Peer peer = existing_peers[j];
					
					if ( 	peer.getTCPListenPort() == tcp_port ||
							peer.getUDPListenPort() == udp_port ){
						
						connected = true;
						
						break;
					}	
				}
				
				if ( connected ){
					
					log( download.getName() + " - peer " + ip.getHostAddress() + " already connected" );
					
					continue;
				}
				
				log( download.getName() + " - connecting to peer " + ip.getHostAddress());

				PEPeerManager c_pm = PluginCoreUtils.unwrap( pm ); 
				
				Map	user_data = new LightHashMap();
				
				user_data.put( PEER_KEY, download );
				
				user_data.put( Peer.PR_PRIORITY_CONNECTION, new Boolean( true ));
				
				c_pm.addPeer( ip.getHostAddress(), tcp_port, udp_port, true, user_data );
			}
		}
	}
	
	protected void
	checkTracking()
	{
		if ( !( plugin_enabled && tracker_enabled )){
			
			return;
		}
		
		List	online;
		
		synchronized( online_buddies ){

			online = new ArrayList( online_buddies );
		}
		
		Set			downloads;
		int			downloads_id;
		
		synchronized( tracked_downloads ){
			
			boolean downloads_changed = last_processed_download_set_id != download_set_id;
			
			if ( downloads_changed ){
				
				last_processed_download_set 	= new HashSet( tracked_downloads );
				last_processed_download_set_id	= download_set_id;
			}
			
			downloads 		= last_processed_download_set;
			downloads_id	= last_processed_download_set_id;
		}
		
		Map	diff_map = new HashMap();
		
		for (int i=0;i<online.size();i++){
			
			BuddyPluginBuddy	buddy = (BuddyPluginBuddy)online.get(i);
			
			buddyData buddy_data = getBuddyData( buddy );
			
			buddy_data.updateLocal( downloads, downloads_id, diff_map );
		}
	}		
	
	public void
	initialised(
		boolean		available )
	{	
	}
	
	public void
	buddyAdded(
		BuddyPluginBuddy	buddy )
	{
		buddyChanged( buddy );
	}
	
	public void
	buddyRemoved(
		BuddyPluginBuddy	buddy )
	{
		buddyChanged( buddy );
	}

	public void
	buddyChanged(
		BuddyPluginBuddy	buddy )
	{	
		if ( buddy.isOnline( false )){
			
			addBuddy( buddy );
			
		}else{
			
			removeBuddy( buddy );
		}
	}
	
	protected buddyData
	getBuddyData(
		BuddyPluginBuddy		buddy )
	{
		synchronized( online_buddies ){
			
			buddyData buddy_data = (buddyData)buddy.getUserData( BuddyPluginTracker.class );

			if ( buddy_data == null ){
				
				buddy_data = new buddyData( buddy );
				
				buddy.setUserData( BuddyPluginTracker.class, buddy_data );
			}
			
			return( buddy_data );
		}
	}
	
	protected buddyData
	addBuddy(
		BuddyPluginBuddy		buddy )
	{
		synchronized( online_buddies ){
			
			if ( !online_buddies.contains( buddy )){
				
				online_buddies.add( buddy );
			}
			
			buddyData bd = getBuddyData( buddy );
			
			if ( bd.hasIPChanged()){
				
				String	ip = bd.getIP();
				
				if ( ip != null ){
					
					List	l = (List)online_buddy_ips.get( ip );
					
					if ( l != null ){
						
						l.remove( buddy );
						
						if ( l.size() == 0 ){
							
							online_buddy_ips.remove( ip );
						}
					}
				}
				
				bd.updateIP();
				
				ip = bd.getIP();
				
				if ( ip != null ){
					
					List l = (List)online_buddy_ips.get( ip );
					
					if ( l == null ){
						
						l = new ArrayList();
						
						online_buddy_ips.put( ip, l );
					}
				
					l.add( buddy );
				}
			}
			
			return( bd );
		}
	}
		
	protected void
	removeBuddy(
		BuddyPluginBuddy		buddy )
	{		
		synchronized( online_buddies ){

			if ( online_buddies.contains( buddy )){
				
				buddyData bd = getBuddyData( buddy );

				online_buddies.remove( buddy );
				
				String	ip = bd.getIP();
				
				if ( ip != null ){
					
					List	l = (List)online_buddy_ips.get( ip );
					
					if ( l != null ){
						
						l.remove( buddy );
						
						if ( l.size() == 0 ){
							
							online_buddy_ips.remove( ip );
						}
					}
				}
			}
		}
	}
	
	protected int
	isBuddy(
		Peer		peer )
	{
		String	peer_ip = peer.getIp();
		
		List ips = AddressUtils.getLANAddresses( peer_ip );

		synchronized( online_buddies ){
			
			int	result = BUDDY_NO;
		
			String	tested = "";
outer:	
			for (int i=0;i<ips.size();i++){

				String ip = (String)ips.get(i);
				
				tested += ip;
				
				List buddies =(List)online_buddy_ips.get( ip  );
			
				if ( buddies != null ){
									
					if ( peer.getTCPListenPort() == 0 && peer.getUDPListenPort() == 0 ){
						
						result = BUDDY_MAYBE;
						
					}else{
					
						for (int j=0;j<buddies.size();j++){
								
							BuddyPluginBuddy	buddy = (BuddyPluginBuddy)buddies.get(j);
								
							if (	buddy.getTCPPort() == peer.getTCPListenPort() &&
									buddy.getTCPPort() != 0 ){
								
								result =  BUDDY_YES;
								
								break outer;
							}
									
							if (	buddy.getUDPPort() == peer.getUDPListenPort() &&
									buddy.getUDPPort() != 0 ){
								
								result =  BUDDY_YES;
								
								break outer;
							}
						}
					}
				}
			}
			
			// log( "isBuddy: " + peer_ip + " -> " + result + ",tested=" + tested );
			
			return( result );
		}
	}
	
	public void
	messageLogged(
		String		str,
		boolean		error )
	{	
	}
	
	public void
	enabledStateChanged(
		boolean 	_enabled )
	{
		plugin_enabled = _enabled;
		
		checkEnabledState();
	}
	
	public boolean
	isEnabled()
	{
		synchronized( this ){
			
			return( plugin_enabled && tracker_enabled );
		}
	}
	
	protected void
	checkEnabledState()
	{
		boolean	seeding_change 	= false;
		boolean	enabled_change	= false;
		
		synchronized( this ){
			
			boolean	old_enabled = old_plugin_enabled && old_tracker_enabled;
			
			if ( plugin_enabled != old_plugin_enabled ){
				
				log( "Plugin enabled state changed to " + plugin_enabled );
				
				old_plugin_enabled = plugin_enabled;
			}
			
			if ( tracker_enabled != old_tracker_enabled ){
				
				log( "Tracker enabled state changed to " + tracker_enabled );
				
				old_tracker_enabled = tracker_enabled;
			}
			
			if ( seeding_only != old_seeding_only ){
				
				log( "Seeding-only state changed to " + seeding_only );
				
				old_seeding_only = seeding_only;
				
				seeding_change = true;
			}
			
			enabled_change = old_enabled != ( plugin_enabled && tracker_enabled );
		}
		
		if ( seeding_change ){
			
			updateSeedingMode();
		}
		
		if ( enabled_change ){
			
			fireEnabledChanged( isEnabled());
		}
	}
	
	protected void
	updateSeedingMode()
	{
		updateNetworkStatus();
		
		List	online;
		
		synchronized( online_buddies ){

			online = new ArrayList( online_buddies );
		}
		
		for (int i=0;i<online.size();i++){
			
			buddyData buddy_data = getBuddyData((BuddyPluginBuddy)online.get(i));
			
			if ( buddy_data.hasDownloadsInCommon()){
				
				buddy_data.updateStatus();
			}
		}
	}
	
	public void
	downloadAdded(
		final Download	download )
	{
		Torrent t = download.getTorrent();
		
		if ( t == null ){
			
			return;
		}
		
		if ( t.isPrivate()){
			
			download.addTrackerListener(
				new DownloadTrackerListener()
				{
					public void 
					scrapeResult(
						DownloadScrapeResult result )
					{					
					}
	
					public void 
					announceResult(
						DownloadAnnounceResult result)
					{
						if ( okToTrack( download )){
							
							trackDownload( download );
							
						}else{
							
							untrackDownload( download );
						}						
					}
				}, 
				false );
		}
		
		if ( okToTrack( download )){
			
			trackDownload( download );
		}
		
		download.addListener(
			new DownloadListener()
			{
				public void
				stateChanged(
					Download		download,
					int				old_state,
					int				new_state )
				{
					if ( okToTrack( download )){
						
						trackDownload( download );
						
					}else{
						
						untrackDownload( download );
					}
				}

				public void
				positionChanged(
					Download	download, 
					int 		oldPosition,
					int 		newPosition )
				{
				}
			});
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		untrackDownload( download );
	}
	
	protected void
	trackDownload(
		Download	download )
	{
		synchronized( tracked_downloads ){
			
			if ( tracked_downloads.contains( download )){
				
				return;
			}
							
			downloadData download_data = new downloadData( download );
				
			download.setUserData( BuddyPluginTracker.class, download_data );
			
			HashWrapper	full_id		= download_data.getID();
			
			HashWrapper short_id 	= new HashWrapper( full_id.getHash(), 0, 4 );
			
			full_id_map.put( full_id, download );
			
			List	dls = (List)short_id_map.get( short_id );
			
			if ( dls == null ){
				
				dls = new ArrayList();
				
				short_id_map.put( short_id, dls );
			}
			
			dls.add( download );
			
			tracked_downloads.add( download );
			
			download_set_id++;
		}
	}
	
	protected void
	untrackDownload(
		Download	download )
	{
		synchronized( tracked_downloads ){
			
			if ( tracked_downloads.remove( download )){
				
				download_set_id++;
				
				downloadData download_data = (downloadData)download.getUserData( BuddyPluginTracker.class );
								
				download.setUserData( BuddyPluginTracker.class, null );
				
				HashWrapper	full_id		= download_data.getID();
				
				full_id_map.remove( full_id );
				
				HashWrapper short_id 	= new HashWrapper( full_id.getHash(), 0, SHORT_ID_SIZE );
				
				List	dls = (List)short_id_map.get( short_id );

				if ( dls != null ){
					
					dls.remove( download );
					
					if ( dls.size() == 0 ){
						
						short_id_map.remove( short_id );
					}
				}
			}
		}
		
		synchronized( online_buddies ){

			Iterator it = online_buddies.iterator();
				
			while( it.hasNext()){
				
				BuddyPluginBuddy	buddy = (BuddyPluginBuddy)it.next();
				
				buddyData buddy_data = getBuddyData( buddy );
				
				buddy_data.resetTracking( download );
			}
		}
		
		synchronized( actively_tracking ){

			actively_tracking.remove( download );
		}
	}
	
	protected void
	trackPeers(
		final Download		download )
	{
		PeerManager pm = download.getPeerManager();
		
			// not running
		
		if ( pm == null ){

			synchronized( actively_tracking ){

				actively_tracking.remove( download );
			}
		}else{
			
			log( "Tracking peers for " + download.getName());
						
			download.addPeerListener( this );
		}
	}
	
	public void
	peerManagerAdded(
		Download		download,
		PeerManager		peer_manager )
	{
		trackPeers( download, peer_manager );
	}
	
	public void
	peerManagerRemoved(
		Download		download,
		PeerManager		peer_manager )
	{
		synchronized( actively_tracking ){

			actively_tracking.remove( download );
		}
		
		download.removePeerListener( this );
	}
	
	protected void
	trackPeers(
		final Download	download,
		PeerManager		pm )
	{
		pm.addListener(
			new PeerManagerListener()
			{
				public void
				peerAdded(
					PeerManager		manager,
					Peer			peer )
				{
					synchronized( actively_tracking ){
						
						if ( !actively_tracking.contains( download )){
							
							manager.removeListener( this );
							
							return;
						}
					}
				
					trackPeer( download, peer );
				}
				
				public void
				peerRemoved(
					PeerManager	manager,
					Peer		peer )
				{
				}
			});
		
		Peer[] peers = pm.getPeers();
		
		for (int i=0;i<peers.length;i++){
			
			trackPeer( download, peers[i] );
		}
	}
	
	protected void
	trackPeer(
		final Download	download,
		final Peer		peer )
	{
		int type = isBuddy( peer );
		
		if ( type == BUDDY_YES ){
			
			markBuddyPeer( download, peer );
			
		}else if ( type == BUDDY_MAYBE ){
			
				// mark as peer early so that we get optimistic disconnect if needed
			
			markBuddyPeer( download, peer );
			
			PeerListener2 listener = 
				new PeerListener2()
				{
					public void 
					eventOccurred(
						PeerEvent event )
					{
						if ( event.getType() == PeerEvent.ET_STATE_CHANGED ){
							
							if (((Integer)event.getData()).intValue() == Peer.TRANSFERING ){
								
								peer.removeListener( this );
								
									// withdraw buddy marker if it turns out our earlier optimism
									// was misplaced
								
								if ( isBuddy( peer ) != BUDDY_YES ){
									
									unmarkBuddyPeer( peer );
								}
							}
						}
					}
				};
				
			peer.addListener( listener );

			if ( peer.getState() == Peer.TRANSFERING ){
				
				peer.removeListener( listener );
				
					// withdraw buddy marker if it turns out our earlier optimism
					// was misplaced
			
				if ( isBuddy( peer ) != BUDDY_YES ){
					
					unmarkBuddyPeer( peer );
				}
			}
		}
	}
	
	protected void
	untrackPeers(
		Download		download )
	{
		log( "Not tracking peers for " + download.getName());
		
		download.removePeerListener( this );
		
		PeerManager pm = download.getPeerManager();
		
		if ( pm != null ){

			Peer[] peers = pm.getPeers();
			
			for (int i=0;i<peers.length;i++){
				
				Peer	peer = peers[i];
				
				unmarkBuddyPeer( peer );
			}
		}
	}
	
	protected void
	markBuddyPeer(
		final Download		download,
		final Peer			peer )
	{
		boolean	state_changed 	= false;
		
		synchronized( buddy_peers ){
			
			if ( !buddy_peers.contains( peer )){
				
				log( "Adding buddy peer " + peer.getIp());
				
				if ( buddy_peers.size() == 0 ){
					
					if ( buddy_stats_timer == null ){
						
						buddy_stats_timer = 
							SimpleTimer.addPeriodicEvent(
								"BuddyTracker:stats",
								1000,
								new TimerEventPerformer()
								{
									public void 
									perform(
										TimerEvent event )
									{
										Iterator it = buddy_peers.iterator();
										
										long	total_sent		= 0;
										long	total_received	= 0;
										
										while( it.hasNext()){
											
											Peer	p = (Peer)it.next();
											
											PeerStats ps = p.getStats();
											
											long sent		= ps.getTotalSent();
											long received 	= ps.getTotalReceived();
											
											long[]	last = (long[])p.getUserData( PEER_STATS_KEY );
											
											if ( last != null ){
												
												total_sent 		+= sent - last[0];
												total_received	+= received - last[1];
											}
											
											p.setUserData( PEER_STATS_KEY, new long[]{ sent, received });
										}
										
										buddy_receive_speed.addValue( total_received );
										buddy_send_speed.addValue( total_sent );
									}
								});
					}
					
					state_changed 	= true;
				}
				
				buddy_peers.add( peer );
				
				peer.setUserData( PEER_KEY, download );
				
				peer.setPriorityConnection( true );
								
				log( download.getName() + ": adding buddy peer " + peer.getIp());

				peer.addListener(
					new PeerListener2()
					{ 
						public void 
						eventOccurred(
							PeerEvent event )
						{
							if ( event.getType() == PeerEvent.ET_STATE_CHANGED ){
								
								int	state = ((Integer)event.getData()).intValue();
								
								if ( state == Peer.CLOSING || state == Peer.DISCONNECTED ){
									
									peer.removeListener( this );
									
									unmarkBuddyPeer( peer );
								}
							}	
						}
					});
			}
		}
		
		if ( peer.getState() == Peer.CLOSING || peer.getState() == Peer.DISCONNECTED ){
			
			unmarkBuddyPeer( peer );
		}
		
		if ( state_changed ){
			
			updateNetworkStatus();
		}
	}
	
	protected void
	unmarkBuddyPeer(
		Peer		peer )
	{
		boolean	state_changed = false;
		
		synchronized( buddy_peers ){

			Download download = (Download)peer.getUserData( PEER_KEY );
				
			if ( download == null ){
				
				return;
			}

			if ( buddy_peers.remove( peer )){
				
				if ( buddy_peers.size() == 0 ){
					
					state_changed = true;
					
					if ( buddy_stats_timer != null ){
						
						buddy_stats_timer.cancel();
						
						buddy_stats_timer = null;
					}
				}
				
				log( download.getName() + ": removing buddy peer " + peer.getIp());
			}
			
			peer.setUserData( PEER_KEY, null );
			
			peer.setPriorityConnection( false );
		}	
		
		if ( state_changed ){
			
			updateNetworkStatus();
		}
	}
	
	protected void
	checkPeers()
	{
		List	to_unmark = new ArrayList();
		
		synchronized( buddy_peers ){

			Iterator	it = buddy_peers.iterator();
			
			while( it.hasNext()){
				
				Peer	peer = (Peer)it.next();
				
				if ( peer.getState() == Peer.CLOSING || peer.getState() == Peer.DISCONNECTED ){
					
					to_unmark.add( peer );
				}
			}
		}
		
		for (int i=0;i<to_unmark.size();i++){
			
			unmarkBuddyPeer((Peer)to_unmark.get(i));
		}
	}
	
	protected void
	recheckPeers()
	{
			// go over peers for active torrents to see if we've missed and. can really only
			// happen with multi-homed LAN setups where a new (and utilised) route is found
			// after we start tracking 
		
		synchronized( actively_tracking ){
			
			Iterator it = actively_tracking.iterator();
			
			while( it.hasNext()){
				
				Download download = (Download)it.next();
				
				PeerManager pm = download.getPeerManager();
				
				if ( pm != null ){
					
					Peer[] peers = pm.getPeers();
					
					for (int i=0;i<peers.length;i++){
						
						trackPeer( download, peers[i] );
					}
				}
			}
		}
	}
	
	protected void
	updateNetworkStatus()
	{	
		int		new_status;
		boolean	changed = false;
		
		synchronized( buddy_peers ){
	
			if ( buddy_peers.size() == 0 ){
				
				new_status 	= BUDDY_NETWORK_IDLE;
				
			}else{
				
				new_status	= seeding_only?BUDDY_NETWORK_OUTBOUND:BUDDY_NETWORK_INBOUND;
			}
			
			if ( new_status != network_status ){
				
				network_status	= new_status;
				
				changed	= true;
			}
		}
		
		if ( changed ){
		
			fireStateChange( new_status );
		}
	}
	
	public void
	addListener(
		BuddyPluginTrackerListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		BuddyPluginTrackerListener		l )
	{
		listeners.remove( l );
	}
	
	protected void
	fireStateChange(
		int		state )
	{
		Iterator	it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((BuddyPluginTrackerListener)it.next()).networkStatusChanged( this, state );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	fireEnabledChanged(
		boolean	enabled )
	{
		Iterator	it = listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((BuddyPluginTrackerListener)it.next()).enabledStateChanged( this, enabled );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
	
	protected void
	sendMessage(
		BuddyPluginBuddy	buddy,
		int					type,
		Map					body )
	{
		Map	msg = new HashMap();
		
		msg.put( "type", new Long( type ));
		msg.put( "msg", body );
		
		plugin.getAZ2Handler().sendAZ2TrackerMessage(
				buddy, 
				msg, 
				BuddyPluginTracker.this );
	}
	
	public Map
	messageReceived(
		BuddyPluginBuddy	buddy,
		Map					message )
	{
		buddyData buddy_data = buddyAlive( buddy );
		
		int type = ((Long)message.get( "type" )).intValue();
		
		Map msg = (Map)message.get( "msg" );
		
		return( buddy_data.receiveMessage( type, msg ));
	}
	
	public void
	messageFailed(
		BuddyPluginBuddy	buddy,
		Throwable			cause )
	{
		log( "Failed to send message to " + buddy.getName(), cause );
		
		buddyDead( buddy );
	}
	
	protected buddyData
	buddyAlive(
		BuddyPluginBuddy		buddy )
	{
		buddyData buddy_data = addBuddy( buddy );
		
		buddy_data.setAlive( true );
		
		return( buddy_data );
	}
	
	protected void
	buddyDead(
		BuddyPluginBuddy		buddy )
	{
		buddyData buddy_data = getBuddyData( buddy );

		if ( buddy_data != null ){
			
			buddy_data.setAlive( false );
		}
	}
	
	protected boolean
	okToTrack(
		Download	d )
	{
		Torrent	t = d.getTorrent();
		
		if ( t == null ){
			
			return( false );
		}
		
			// only track private torrents if we have successfully received peers from tracker
			// which means we have the torrent legitimately. As this rule is enforced by both
			// ends of the tracking operation it means we will only track between peers that
			// both have a legitimate copy of the torrent.
				
		if ( t.isPrivate()){
			
			DownloadAnnounceResult announce = d.getLastAnnounceResult();

			if ( 	announce == null ||
					announce.getResponseType() != DownloadAnnounceResult.RT_SUCCESS ||
					announce.getPeers().length < 2 ){
				
				return( false );
			}
		}
		
		int state = d.getState();
		
		return( 	state != Download.ST_ERROR && 
					state != Download.ST_STOPPING && 
					state != Download.ST_STOPPED );
	}
	
	protected void
	log(
		String		str )
	{
		plugin.log( "Tracker: " + str );
	}
	
	protected void
	log(
		String		str,
		boolean		verbose )
	{
		if ( verbose ){
			
			if ( Constants.isCVSVersion()){
			
				log( str );
			}
		}else{
			
			log( str );
		}
	}
	
	protected void
	log(
		String		str,
		Throwable 	e )
	{
		plugin.log( "Tracker: " + str, e );
	}
	
	private class
	buddyData
	{
		private BuddyPluginBuddy		buddy;
		
		private Set	downloads_sent;
		private int	downloads_sent_id;
		
		private Map		downloads_in_common;
		private boolean	buddy_seeding_only;
		
		private int		consecutive_fails;
		private long	last_fail;
		
		private String	current_ip;
		
		protected
		buddyData(
			BuddyPluginBuddy		_buddy )
		{
			buddy	= _buddy;
		}
		
		protected void
		updateIP()
		{
			current_ip	= buddy.getAdjustedIP().getHostAddress();
			
			log( "IP set to " + current_ip );
		}
			
		protected boolean
		hasIPChanged()
		{
			InetAddress	latest_ip = buddy.getAdjustedIP();
			
			if ( latest_ip == null && current_ip == null ){
				
				return( false );
				
			}else if ( latest_ip == null || current_ip == null ){
				
				return( true );
				
			}else{
			
				return(	!current_ip.equals( latest_ip.getHostAddress()));
			}
		}
		
		protected String
		getIP()
		{
			return( current_ip );
		}
		
		protected boolean
		hasDownloadsInCommon()
		{
			synchronized( this ){
			
				return( downloads_in_common != null );
			}
		}
		
		protected void
		setAlive(
			boolean		alive )
		{
			synchronized( this ){
				
				if ( alive ){
					
					consecutive_fails		= 0;
					last_fail				= 0;

				}else{
					
					consecutive_fails++;
					
					last_fail	= SystemTime.getMonotonousTime();
				}
			}
		}
		
		protected void
		updateLocal(
			Set		downloads,
			int		id,
			Map		diff_map )
		{
			if ( consecutive_fails > 0 ){
				
				long	retry_millis = RETRY_SEND_MIN;
				
				for (int i=0;i<consecutive_fails-1;i++){
					
					retry_millis <<= 2;
					
					if ( retry_millis > RETRY_SEND_MAX ){
						
						retry_millis = RETRY_SEND_MAX;
						
						break;
					}
				}
				
				long	now = SystemTime.getMonotonousTime();
				
				if ( now - last_fail >= retry_millis ){
					
					last_fail			= now;	// assume we're going to fail so we avoid
												// falling through here multiple times before
												// actuallt failing again
					
					downloads_sent 		= null;
					downloads_sent_id	= 0;
				}
			}
			
				// first check to see if completion state changed for any common downloads
			
			List	comp_changed = new ArrayList();
			
			synchronized( this ){
				
				if ( downloads_in_common != null ){
					
					Iterator it = downloads_in_common.entrySet().iterator();
					
					while( it.hasNext()){
						
						Map.Entry	entry = (Map.Entry)it.next();
						
						Download d = (Download)entry.getKey();

						buddyDownloadData	bdd = (buddyDownloadData)entry.getValue();
						
						boolean	local_complete = d.isComplete( false );
						
						if ( local_complete != bdd.isLocalComplete()){
							
							bdd.setLocalComplete( local_complete );
							
							comp_changed.add( d );
						}
					}
				}
			}
			
			if ( comp_changed.size() > 0 ){
				
				byte[][] change_details = exportFullIDs( comp_changed );
				
				if( change_details[0].length > 0 ){
					
					Map	msg = new HashMap();
										
					msg.put( "seeding", new Long( seeding_only?1:0 ));
					
					msg.put( "change", 		change_details[0] );
					msg.put( "change_s", 	change_details[1] );

					sendMessage( buddy, REQUEST_TRACKER_CHANGE, msg );
				}
			}
			
			if ( id == downloads_sent_id ){
				
				return;
			}
			
			Long	key = new Long(((long)id) << 32 | (long)downloads_sent_id);
				
			Object[]	diffs = (Object[])diff_map.get( key );
			
			boolean	incremental = downloads_sent != null;
			
			byte[]	added_bytes;
			byte[]	removed_bytes;
			
			if ( diffs == null ){
				
				List	added;
				List	removed	= new ArrayList();
				

				if ( downloads_sent == null ){
					
					added 	= new ArrayList( downloads );
					
				}else{
					
					added	= new ArrayList();

					Iterator	it1 = downloads.iterator();
					
					while( it1.hasNext()){
					
						Download download = (Download)it1.next();
						
						if ( okToTrack( download )){
							
							if ( !downloads_sent.contains( download )){
								
								added.add( download );
							}
						}
					}
					
					Iterator	it2 = downloads_sent.iterator();
					
					while( it2.hasNext()){
					
						Download download = (Download)it2.next();
						
						if ( !downloads.contains( download )){
							
							removed.add( download );
						}
					}
				}
				
				added_bytes 	= exportShortIDs( added );
				removed_bytes	= exportFullIDs( removed )[0];
				
				diff_map.put( key, new Object[]{ added_bytes, removed_bytes });
			}else{
				
				added_bytes 	= (byte[])diffs[0];
				removed_bytes 	= (byte[])diffs[1];
			}
				
			downloads_sent 		= downloads;
			downloads_sent_id	= id;
			
			if ( added_bytes.length == 0 && removed_bytes.length == 0 ){
				
				return;
			}
			
			Map	msg = new HashMap();
			
			if ( added_bytes.length > 0 ){
				
				msg.put( "added", 	added_bytes );
			}
			
			if ( removed_bytes.length > 0 ){
				
				msg.put( "removed", removed_bytes );
			}
			
			msg.put( "inc", 	new Long( incremental?1:0 ));
			msg.put( "seeding", new Long( seeding_only?1:0 ));
			
			sendMessage( buddy, REQUEST_TRACKER_SUMMARY, msg );
		}	
		
		protected Map
		updateRemote(
			Map		msg )
		{			
			List	added 	= importShortIDs((byte[])msg.get( "added" ));
			
			Map	reply = new HashMap();
			
			byte[][] add_details = exportFullIDs( added );
			
			if( add_details[0].length > 0 ){
			
				reply.put( "added", 	add_details[0] );
				reply.put( "added_s", 	add_details[1] );
			}
			
			synchronized( this ){

				if ( downloads_in_common != null ){

					Map removed = importFullIDs( (byte[])msg.get( "removed" ), null );
					
					Iterator it = removed.keySet().iterator();
					
					while( it.hasNext()){
						
						Download d = (Download)it.next();
						
						if ( downloads_in_common.remove( d ) != null ){
							
							log( "Removed " + d.getName() + " common download", false, true );
						}
					}
				
					if ( downloads_in_common.size() == 0 ){
						
						downloads_in_common = null;
					}
				}
			}
			
			return( reply );
		}
		
		protected void
		updateCommonDownloads(
			Map			downloads,
			boolean		incremental )
		{
			synchronized( this ){

				if ( downloads_in_common == null ){
					
					downloads_in_common = new HashMap();
					
				}else{
					
						// if not incremental then remove any downloads that no longer
						// are in common
					
					if ( !incremental ){
						
						Iterator it = downloads_in_common.keySet().iterator();
						
						while( it.hasNext()){
							
							Download download = (Download)it.next();
							
							if ( !downloads.containsKey( download )){
								
								log( "Removing " + download.getName() + " from common downloads", false, true );

								it.remove();
							}
						}
					}
				}
				
				Iterator it = downloads.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry	entry = (Map.Entry)it.next();
			
					Download d = (Download)entry.getKey();

					buddyDownloadData	bdd = (buddyDownloadData)entry.getValue();
					
					buddyDownloadData existing = (buddyDownloadData)downloads_in_common.get( d );
					
					if ( existing == null ){
						
						log( "Adding " + d.getName() + " to common downloads (bdd=" + bdd.getString() + ")", false, true );
						
						downloads_in_common.put( d, bdd );
						
					}else{
						
						boolean	old_rc = existing.isRemoteComplete();
						boolean	new_rc = bdd.isRemoteComplete();
						
						if ( old_rc != new_rc ){
						
							existing.setRemoteComplete( new_rc ); 
							
							log( "Changing " + d.getName() + " common downloads (bdd=" + existing.getString() + ")", false, true );
						}				
					}
				}
				
				if ( downloads_in_common.size() == 0 ){
					
					downloads_in_common = null;
				}
			}
		}
		
		protected void
		updateStatus()
		{
			Map	msg = new HashMap();
			
			msg.put( "seeding", new Long( seeding_only?1:0 ));
			
			sendMessage( buddy, REQUEST_TRACKER_STATUS, msg );
		}
		
		protected Map
		receiveMessage(
			int			type,
			Map			msg_in )
		{
			int	reply_type	= -1;
			Map	msg_out		= null;

			Long	l_seeding = (Long)msg_in.get( "seeding" );
			
			if( l_seeding != null ){
				
				boolean old = buddy_seeding_only;
				
				buddy_seeding_only = l_seeding.intValue() == 1;
				
				if ( old != buddy_seeding_only ){
					
					log( "Seeding only changed to " + buddy_seeding_only );
				}
			}
			
			if ( type == REQUEST_TRACKER_SUMMARY ){
		
				reply_type	= REPLY_TRACKER_SUMMARY;
				
				msg_out = updateRemote( msg_in );
				
				msg_out.put( "inc", msg_in.get( "inc" ));
								
			}else if ( type == REQUEST_TRACKER_STATUS ){
				
				reply_type	= REPLY_TRACKER_STATUS;
				
			}else if ( type == REQUEST_TRACKER_CHANGE ){

				reply_type	= REPLY_TRACKER_STATUS;
									
				Map downloads = importFullIDs( (byte[])msg_in.get( "changed" ), (byte[])msg_in.get( "changed_s" ) );

				updateCommonDownloads( downloads, true );
								
			}else if ( type == REQUEST_TRACKER_ADD ){

				reply_type	= REPLY_TRACKER_ADD;
										
				Map downloads = importFullIDs( (byte[])msg_in.get( "added" ), (byte[])msg_in.get( "added_s" ) );

				updateCommonDownloads( downloads, true );

			}else if ( type == REPLY_TRACKER_SUMMARY ){
				
					// full hashes on reply
				
				byte[]	possible_matches 		= (byte[])msg_in.get( "added" );
				byte[]	possible_match_states 	= (byte[])msg_in.get( "added_s" );

				boolean	incremental = ((Long)msg_in.get( "inc" )).intValue() == 1;
				
				if ( possible_matches != null && possible_match_states != null ){
							
					Map downloads = importFullIDs( possible_matches, possible_match_states );
						
					if ( downloads.size() > 0 ){
												
						updateCommonDownloads( downloads, incremental );
						
						byte[][] common_details = exportFullIDs( new ArrayList( downloads.keySet()));
						
						if( common_details[0].length > 0 ){
							
							Map	msg = new HashMap();
												
							msg.put( "seeding", new Long( seeding_only?1:0 ));
							
							msg.put( "added", 	common_details[0] );
							msg.put( "added_s", common_details[1] );

							sendMessage( buddy, REQUEST_TRACKER_ADD, msg );
						}
					}
				}
								
			}else if ( 	type == REPLY_TRACKER_CHANGE ||
						type == REPLY_TRACKER_STATUS ||
						type == REPLY_TRACKER_ADD ){
				
					// nothing interesting in reply for these
			}else{
				
				log( "Unrecognised type " + type );
			}
			
			if ( reply_type != -1 ){
				
				Map	reply = new HashMap();
			
				reply.put( "type", new Long( reply_type ));
		
				if ( msg_out == null ){
					
					msg_out = new HashMap();
				}
				
				msg_out.put( "seeding", new Long( seeding_only?1:0 ));
			
				reply.put( "msg", msg_out );

				return( reply );
			}
			
			return( null );
		}
		
		protected byte[]
		exportShortIDs(
			List	downloads )
		{
			byte[]	res = new byte[ SHORT_ID_SIZE * downloads.size() ];
			
			for (int i=0;i<downloads.size();i++ ){
				
				Download download = (Download)downloads.get(i);
				
				downloadData download_data = (downloadData)download.getUserData( BuddyPluginTracker.class );
				
				if ( download_data != null ){

					System.arraycopy(
						download_data.getID().getBytes(),
						0,
						res,
						i * SHORT_ID_SIZE,
						SHORT_ID_SIZE );
				}
			}
			
			return( res );
		}
		
		protected List
		importShortIDs(
			byte[]		ids )
		{
			List	res = new ArrayList();
			
			if ( ids != null ){
				
				synchronized( tracked_downloads ){

					for (int i=0;i<ids.length;i+= SHORT_ID_SIZE ){
					
						List dls = (List)short_id_map.get( new HashWrapper( ids, i, SHORT_ID_SIZE ));
						
						if ( dls != null ){
							
							res.addAll( dls );
						}
					}
				}
			}
			
			return( res );
		}
		
		protected byte[][]
   		exportFullIDs(
   			List	downloads )
   		{
   			byte[]	hashes 	= new byte[ FULL_ID_SIZE * downloads.size() ];
   			byte[] 	states	= new byte[ downloads.size()];
   			
   			for (int i=0;i<downloads.size();i++ ){
   				
   				Download download = (Download)downloads.get(i);
   				
   				downloadData download_data = (downloadData)download.getUserData( BuddyPluginTracker.class );
   				
   				if ( download_data != null ){

   					System.arraycopy(
   						download_data.getID().getBytes(),
   						0,
   						hashes,
   						i * FULL_ID_SIZE,
   						FULL_ID_SIZE );
   					
   					states[i] = download.isComplete( false )?(byte)0x01:(byte)0x00;
   				}
   			}
   			
   			return( new byte[][]{ hashes, states });
   		}
		
		protected Map
		importFullIDs(
			byte[]		ids,
			byte[]		states )
		{
			Map	res = new HashMap();
			
			if ( ids != null ){
				
				synchronized( tracked_downloads ){

					for (int i=0;i<ids.length;i+= FULL_ID_SIZE ){
					
						Download dl = (Download)full_id_map.get( new HashWrapper( ids, i, FULL_ID_SIZE ));
						
						if ( dl != null ){
							
							buddyDownloadData bdd = new buddyDownloadData( dl );
							
							if ( states != null ){
							
								bdd.setRemoteComplete(( states[i/FULL_ID_SIZE] & 0x01 ) != 0 );
							}
							
							res.put( dl, bdd );
						}
					}
				}
			}
			
			return( res );
		}
		
		protected Map
		getDownloadsToTrack()
		{
			Map	res = new HashMap();

			if ( seeding_only == buddy_seeding_only ){
				
				log( "Not tracking, buddy and me both " + (seeding_only?"seeding":"downloading"), true, false );

				return( res );
			}			
		
			long	now = SystemTime.getMonotonousTime();
			
			synchronized( this ){

				if ( downloads_in_common == null ){
					
					log( "Not tracking, buddy has nothing in common", true, false );

					return( res );
				}
				
				Iterator it = downloads_in_common.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry	entry = (Map.Entry)it.next();
			
					Download d = (Download)entry.getKey();

					buddyDownloadData	bdd = (buddyDownloadData)entry.getValue();
					
					if ( d.isComplete( false ) && bdd.isRemoteComplete()){
						
							// both complete, nothing to do!
						
						log( d.getName() + " - not tracking, both complete", true, true );
						
					}else{
						
						long	last_track = bdd.getTrackTime();
						
						if ( 	last_track == 0 || 
								now - last_track >= TRACK_INTERVAL ){
							
							log( d.getName() + " - tracking", false, true );

							bdd.setTrackTime( now );
							
							res.put( d, new Boolean( true ));
							
						}else{
							
							res.put( d, new Boolean( false ));
						}
					}
				}
			}
			
			return( res );
		}
		
		protected void
		resetTracking(
			Download		download )
		{
			synchronized( this ){
				
				if ( downloads_in_common == null ){
					
					return;
				}
				
				buddyDownloadData bdd = (buddyDownloadData)downloads_in_common.get( download );
				
				if ( bdd != null ){
					
					bdd.resetTrackTime();
				}
			}
		}
		
		protected void
		log(
			String	str )
		{
			BuddyPluginTracker.this.log( buddy.getName() + ": " + str );
		}
		
		protected void
		log(
			String	str,
			boolean	verbose,
			boolean	no_buddy )
		{
			BuddyPluginTracker.this.log( (no_buddy?"":( buddy.getName() + ": ")) + str, verbose );
		}
	}
	
	private static class
	buddyDownloadData
	{
		private boolean	local_is_complete;
		private boolean	remote_is_complete;
		private long	last_track;
		
		protected
		buddyDownloadData(
			Download		download )
		{
			local_is_complete = download.isComplete( false );
		}
		
		protected void
		setLocalComplete(
			boolean		b )
		{
			local_is_complete	= b;
		}
	
		protected boolean
		isLocalComplete()
		{
			return( local_is_complete );
		}
		
		protected void
		setRemoteComplete(
			boolean		b )
		{
			remote_is_complete	= b;
		}
	
		protected boolean
		isRemoteComplete()
		{
			return( remote_is_complete );
		}
		
		protected void
		setTrackTime(
			long	time )
		{
			last_track	= time;
		}
		
		protected long
		getTrackTime()
		{
			return( last_track );
		}
		
		protected void
		resetTrackTime()
		{
			last_track	= 0;
		}
		
		protected String
		getString()
		{
			return( "lic=" + local_is_complete + ",ric=" + remote_is_complete + ",lt=" + last_track );
		}
	}
	
	private static class
	downloadData
	{
		private static final byte[]	IV = {(byte)0x7A, (byte)0x7A, (byte)0xAD, (byte)0xAB, (byte)0x8E, (byte)0xBF, (byte)0xCD, (byte)0x39, (byte)0x87, (byte)0x0, (byte)0xA4, (byte)0xB8, (byte)0xFE, (byte)0x40, (byte)0xA2, (byte)0xE8 }; 
			
		private HashWrapper	id;
		
		protected
		downloadData(
			Download	download )
		{
			Torrent t = download.getTorrent();
			
			if ( t != null ){
				
				byte[]	hash = t.getHash();
				
				SHA1	sha1 = new SHA1();
			
				sha1.update( ByteBuffer.wrap( IV ));
				sha1.update( ByteBuffer.wrap( hash ));
				
				id = new HashWrapper( sha1.digest() );
			}
		}
		
		protected HashWrapper
		getID()
		{
			return( id );
		}
	}
}
