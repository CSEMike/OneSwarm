/*
 * Created on Jan 29, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.plugins.tracker.peerauth;

import java.io.BufferedInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.*;
import java.util.*;


import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.ThreadPool;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResultPeer;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerListener;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;

import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;

public class 
TrackerPeerAuthPlugin 
	implements Plugin, DownloadManagerListener
{
	private static final String	PLUGIN_NAME	= "Tracker Peer Auth";
	private static final String PLUGIN_CONFIGSECTION_ID = "Plugin.trackerpeerauth.name";

	private static final int DEFAULT_CHECK_PERIOD	= 30*1000;
	
	private static final String	STATE_ENABLED	= "enabled";
	private static final String	STATE_DISABLED	= "disabled";
	
	private static final int	TIMER_PERIOD	= 10*1000;
	
	private PluginInterface		plugin_interface;
	
	private TorrentAttribute 	ta_state ;
	private LoggerChannel 		log;
		
	private Map					dt_map	= new HashMap();
	
	private ThreadPool			thread_pool = new ThreadPool("TrackerPeerAuthPlugin",8, true );
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );
	}
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface = _plugin_interface;
		
		ta_state 	= plugin_interface.getTorrentManager().getPluginAttribute( "state" );
		
		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( ConfigSection.SECTION_PLUGINS, PLUGIN_CONFIGSECTION_ID );

		config.addLabelParameter2( "Plugin.trackerpeerauth.info" );		
		
		final BasicPluginViewModel	view_model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "Plugin.trackerpeerauth.name" );

		view_model.setConfigSectionID(PLUGIN_CONFIGSECTION_ID);
		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	content )
					{
						view_model.getLogArea().appendText( content + "\n" );
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						if ( str.length() > 0 ){
							view_model.getLogArea().appendText( str + "\n" );
						}
						
						StringWriter sw = new StringWriter();
						
						PrintWriter	pw = new PrintWriter( sw );
						
						error.printStackTrace( pw );
						
						pw.flush();
						
						view_model.getLogArea().appendText( sw.toString() + "\n" );
					}
				});
		
		System.out.println( "**** tracker peer auth disabled ****" );
		
		/*

		plugin_interface.getDownloadManager().addListener( this );
		
		SimpleTimer.addPeriodicEvent(
			"TrackerPeerAuthPlugin:checker",
			TIMER_PERIOD,
			new TimerEventPerformer()
			{
				private long	tick_count = 0;
				
				public void
				perform(
					TimerEvent	event )
				{
					tick_count++;
					
					synchronized( dt_map ){
						
						Iterator	it = dt_map.values().iterator();
						
						while( it.hasNext()){
							
							((DownloadTracker)it.next()).checkPeers( tick_count );
						}
					}
				}
			});
			*/
	}
	
	public void
	downloadAdded(
		final Download	download )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent != null && torrent.isPrivate()){
			
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
						DownloadAnnounceResult result )
					{
						if ( result.getResponseType() == DownloadAnnounceResult.RT_SUCCESS ){
							
							Map	ext = result.getExtensions();
							
							boolean	enabled = true;
							
							int		check_period	= DEFAULT_CHECK_PERIOD;
							
							if ( ext != null ){
								
								// TODO: detect enabled state
								
								// TODO: get check period
							}
							
							download.setAttribute( ta_state, enabled?STATE_ENABLED:STATE_DISABLED );

							setState( download, enabled, check_period );
						}
					}
				});
			
			String state = download.getAttribute( ta_state );
			

			if ( state != null ){
				
				boolean	enabled = state.equals( STATE_ENABLED );
				
				setState( download, enabled, DEFAULT_CHECK_PERIOD );
			}
		}
	}
	
	public void
	downloadRemoved(
		Download	download )
	{
		synchronized( dt_map ){
		
			dt_map.remove( download );
		}
	}
	
	protected void
	setState(
		Download	download,
		boolean		enabled,
		int			check_period )
	{
		synchronized( dt_map ){

			if ( enabled ){
						
				DownloadTracker existing = (DownloadTracker)dt_map.get( download );
			
				if ( existing == null ){
			
					DownloadTracker	dt = new DownloadTracker( download, check_period );
				
					dt_map.put( download, dt );
					
				}else{
					
					existing.setCheckPeriod( check_period );
				}
			}else{
							
				dt_map.remove( download );
			}
		}
	}
	
	protected void
	log(
		Download	download,
		String		str )
	{
		log.log( "Download '" + download.getName() + "' - " + str );
	}
	
	protected class
	DownloadTracker
		implements DownloadPeerListener, PeerManagerListener, DownloadTrackerListener
	{
		private static final int 	BACKOFF_TICK_COUNT	= 60*1000 / TIMER_PERIOD;	// TODO:
		
		private static final int 	MAX_PEERS_PER_QUERY	= 100;
		
		private static final int	OK_BLOOM_INITIAL	= 16*1024;
		private static final int	OK_BLOOM_INC		= 16*1024;
		private static final int	OK_BLOOM_MAX		= 128*1024;
		
		private static final int	BAD_BLOOM_INITIAL	= 4*1024;
		private static final int	BAD_BLOOM_INC		= 4*1024;
		private static final int	BAD_BLOOM_MAX		= 64*1024;
		
		private Download		download;
		
		private BloomFilter		ok_bloom 	= BloomFilterFactory.createAddOnly( OK_BLOOM_INITIAL );
		private BloomFilter		bad_bloom 	= BloomFilterFactory.createAddOnly( BAD_BLOOM_INITIAL );
			
		private long			pending_check_peer_count	= 0; 
		
		private boolean			check_running;
		private int				check_tick_count;
		private int				backoff_tick_count;
		
		protected
		DownloadTracker(
			Download	_download,
			int			_min_check_period )
		{
			download	= _download;
			
			download.addTrackerListener( this );
			
			download.addPeerListener( this );
			
			setCheckPeriod( _min_check_period );
			
			log( "enabled, check period=" + _min_check_period );
		}
		
		protected void
		setCheckPeriod(
			int			_min_check_period )
		{
			check_tick_count	= _min_check_period / TIMER_PERIOD;
		}
		
		protected void
		recordPeer(
			String		source,
			byte[]		id,
			String		ip,
			int			port,
			boolean		ok )
		{
			if ( id == null ){
				
				return;
			}
			
			byte[]	key = getKey( id, ip );
			
			synchronized( this ){
				
				if ( ok ){
					
					int	entries = ok_bloom.getEntryCount();
					
					if ( entries > 0 && ( ok_bloom.getSize() / entries < 10 )){
						
						int	new_size = ok_bloom.getSize() + OK_BLOOM_INC;
						
						if ( new_size > OK_BLOOM_MAX ){
							
							new_size = OK_BLOOM_MAX;
						}
						
						log( "Expanding ok bloom to " + new_size + " entries" );
						
						BloomFilter new_ok_bloom 	= BloomFilterFactory.createAddOnly( new_size );
	
						PeerManager pm = download.getPeerManager();
						
						if ( pm != null ){
							
							Peer[] peers = pm.getPeers();
							
							for (int i=0;i<peers.length;i++){
								
								byte[]	peer_key = getKey( peers[i] );
								
								if ( peer_key != null && ok_bloom.contains( peer_key )){
									
									new_ok_bloom.add( peer_key );
								}
							}
						}
						
						ok_bloom = new_ok_bloom;
						
							// need to drop the bad bloom here too. we rely on the ok bloom 
							// to filter false positives on the bad bloom 
						
						bad_bloom 	= BloomFilterFactory.createAddOnly( bad_bloom.getSize());
					}
					
					ok_bloom.add( key );
					
				}else{
					
					int	entries = bad_bloom.getEntryCount();
					
					if ( entries > 0 && ( bad_bloom.getSize() / entries < 10 )){
						
						int	new_size = bad_bloom.getSize() + BAD_BLOOM_INC;
						
						if ( new_size > BAD_BLOOM_MAX ){
							
							new_size = BAD_BLOOM_MAX;
						}
						
						log( "Expanding bad bloom to " + new_size + " entries" );
						
						bad_bloom 	= BloomFilterFactory.createAddOnly( new_size );
					}
					
					bad_bloom.add( key );
				}
			}
		}
		
		protected void
		checkPeers(
			long	tick_count )
		{
			if ( backoff_tick_count > 0 ){
				
				backoff_tick_count--;
				
				return;
			}
			
			if ( tick_count % check_tick_count == 0 ){
				
				synchronized( this ){
					
					if ( pending_check_peer_count > 0 && !check_running){
						
						pending_check_peer_count	= 0;
						
						check_running				= true;
						
					}else{
						
						return;
					}
				}
				
				boolean	gone_async = false;
				
				try{
					
					PeerManager pm = download.getPeerManager();
					
					if ( pm != null ){
						
						Peer[] peers = pm.getPeers();
						
						final List	to_check = new ArrayList();
						
						for (int i=0;i<peers.length;i++){
							
							Peer	peer = peers[i];
							
							byte[]	peer_key = getKey( peer );
							
							if ( peer_key != null ){
								
								if ( ok_bloom.contains( peer_key )){
									
								}else if ( bad_bloom.contains( peer_key )){
									
									removePeer( peer );
									
								}else{
									
									to_check.add( peer );
								}
							}
						}
						
						if ( to_check.size() > 0 ){
																
							thread_pool.run(
								new AERunnable()
								{
									public void
									runSupport()
									{
										try{
											
											check( to_check );
											
										}finally{
											
											synchronized( DownloadTracker.this ){
											
												check_running	= false;
											}
										}
									}
								});
							
							gone_async = true;
						}
					}
				}finally{
					
					if ( !gone_async ){
						
						synchronized( this ){

							check_running = false;
						}
					}
				}
			}
		}
		
		protected void
		check(
			List		peers )
		{
			DownloadAnnounceResult	an = download.getLastAnnounceResult();
			
			URL	target = an==null?null:an.getURL();
			
			if ( target == null ){
				
				target = download.getTorrent().getAnnounceURL();
			}
							
			OutputStreamWriter 		out	= null;
			BufferedInputStream		in	= null;
			
			try{
				String	url_str = target.toString();
				
				int	pos = url_str.indexOf( "announce" );
				
				if ( pos == -1 ){
				
						// TODO: this should be logged once and checked earlier
					
					log( "announce URL '" + url_str + "' is non-conformant" );
					
					return;
				}
				
				url_str = url_str.substring(0,pos) + "testauth" + url_str.substring( pos + 8 );
				
				target = new URL( url_str );
			
				Map	map = new HashMap();
				
				String	peer_str = "";
				
				for (int i=0;i<peers.size() && i < MAX_PEERS_PER_QUERY; i++ ){
					
					Peer	peer = (Peer)peers.get(i);
					
					List	peer_data = new ArrayList();
					
					peer_data.add( download.getTorrent().getHash());
					peer_data.add( peer.getId());
					peer_data.add( peer.getIp());
					
					map.put( "peer" + i, peer_data );
					
					peer_str += (i==0?"":",") + peer.getIp();
				}

				log( "Checking " + url_str + " : peers=" + peer_str );
				

				byte[]	encoded = BEncoder.encode( map, true );				

				HttpURLConnection connection = (HttpURLConnection)target.openConnection();
				
			    String data = "authpeers=" + new String(encoded, "ISO-8859-1" );
			    
			    System.out.println( "sending '" + data + "'" );
			    
			    connection.setDoOutput(true);
			    
			    connection.setRequestMethod("POST");
			    
			    connection.setRequestProperty("User-Agent", Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION);     
				  
			    connection.setRequestProperty( "Connection", "close" );

			    connection.addRequestProperty( "Accept-Encoding", "gzip" );

			    out = new OutputStreamWriter(connection.getOutputStream());
			    
			    out.write(data);
			    
			    out.flush();
			    
			    in = new BufferedInputStream(connection.getInputStream());

			    Map	result_map = BDecoder.decode( in );
			    
				for (int i=0;i<peers.size() && i < MAX_PEERS_PER_QUERY; i++ ){

					Peer	peer = (Peer)peers.get(i);

					Long	enabled = (Long)result_map.get( "peer" + i );
					
					if ( enabled == null ){
						
						log( "No response for peer '" + peer.getIp() + "'" );
						
					}else{
						
						boolean	ok = enabled.longValue() != 0;
						
						recordPeer( "auth check", peer.getId(), peer.getIp(), peer.getPort(), ok );
				
						if ( !ok ){
							
							removePeer( peer );
						}
					}
				}
				
			}catch( Throwable e ){
				
				backoff_tick_count = BACKOFF_TICK_COUNT;
				
				e.printStackTrace();
				
			}finally{
				
				if ( out != null ){
					
					try{
						out.close();
						
					}catch( Throwable e ){
						
					}
				}
				
				if ( in != null ){
					
					try{
						in.close();
						
					}catch( Throwable e ){
						
					}
				}
			}
		}
		
		protected byte[]
		getKey(
			Peer		peer )
		{
			byte[]	peer_id = peer.getId();
			
			if ( peer_id == null ){
							
				return( null );
			}
						
			return( getKey( peer_id, peer.getIp()));
		}
		
		protected byte[]
		getKey(
			byte[]		peer_id,
			String		ip_str )
		{
			byte[]	ip = ip_str.getBytes();
			
			byte[]	key = new byte[peer_id.length + ip.length ];
			
			System.arraycopy( peer_id, 0, key, 0, peer_id.length );
			System.arraycopy( ip, 0, key, peer_id.length, ip.length );
			
			return( key );
		}
		
		protected boolean
		knownToBeOK(
			Peer		peer )
		{
			byte[]	peer_id = peer.getId();
			
			if ( peer_id == null ){
			
					// only happens on outbound connectas we don't retain the peer-id pending
					// outbound connect
				
				return( true );
			}
						
			byte[]	key = getKey( peer_id, peer.getIp());
			
			return( ok_bloom.contains( key ));
		}
		
		protected boolean
		knownToBeBad(
			Peer		peer )
		{
			byte[]	peer_id = peer.getId();
			
			if ( peer_id == null ){
				
					// shouldn't get here as we should check for OK first
				
				return( true );
			}
						
			byte[]	key = getKey( peer_id, peer.getIp());
			
			return( bad_bloom.contains( key ));
		}
		
		protected void
		peerMightBeBad(
			Peer		peer )
		{
			if ( knownToBeOK( peer )){
				
			}else if ( knownToBeBad( peer )){
				
				removePeer( peer );
				
			}else{
				
				// we just leave it here and pick up for checking periodically
				
				pending_check_peer_count++;
			}
		}
		
		protected void
		removePeer(
			Peer		peer )
		{
			log( "Disconnecting peer " + peer.getIp() + "/" + peer.getPort() + ": not authorized" );
			
			peer.close( "Tracker peer authorization failure", false, false );
		}
		
		public void 
		scrapeResult(
			DownloadScrapeResult result )
		{
			
		}


		public void 
		announceResult(
			DownloadAnnounceResult result )
		{
			DownloadAnnounceResultPeer[] peers = result.getPeers();
			
			if ( peers != null ){
				
				for (int i=0;i<peers.length;i++){
					
					DownloadAnnounceResultPeer	peer = peers[i];
					
					recordPeer( "Tracker", peer.getPeerID(), peer.getAddress(), peer.getPort(), true );
				}
			}
		}
		
		public void
		peerAdded(
			PeerManager	manager,
			Peer		peer )
		{
				// assume all outgoing connections are valid as we got them from the tracker
			
			if ( peer.isIncoming()){
				
				peerMightBeBad( peer );

			}else{
				
				recordPeer( "Outgoing", peer.getId(), peer.getIp(), peer.getPort(), true );
			}
		}
		
		public void
		peerRemoved(
			PeerManager	manager,
			Peer		peer )
		{
		}
		
		public void
		peerManagerAdded(
			Download		download,
			PeerManager		peer_manager )
		{
			peer_manager.addListener( this );
		}
		
		public void
		peerManagerRemoved(
			Download		download,
			PeerManager		peer_manager )
		{
			peer_manager.removeListener( this );
		}
		
		protected void
		log(
			String	str )
		{
			TrackerPeerAuthPlugin.this.log( download, str );
		}
	}
}
