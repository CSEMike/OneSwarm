/*
 * Created on 15-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.extseed;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadManagerStats;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.utils.*;

import com.aelitis.azureus.plugins.extseed.impl.getright.ExternalSeedReaderFactoryGetRight;
import com.aelitis.azureus.plugins.extseed.impl.webseed.ExternalSeedReaderFactoryWebSeed;

public class 
ExternalSeedPlugin
	implements Plugin, DownloadManagerListener, DownloadListener
{
	private static ExternalSeedReaderFactory[]	factories = {
		new ExternalSeedReaderFactoryGetRight(),
		new ExternalSeedReaderFactoryWebSeed(),
	};
	
	private PluginInterface			plugin_interface;
	private DownloadManagerStats	dm_stats;
	private LoggerChannel			log;
	
	private 		Random	random = new Random();
	
	private Map		download_map	= new HashMap();
	private Monitor	download_mon;
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		dm_stats = plugin_interface.getDownloadManager().getStats();
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"External Seed" );
				
		log	= plugin_interface.getLogger().getTimeStampedChannel( "External Seeds" );
		
		final BasicPluginViewModel	view_model = 
			plugin_interface.getUIManager().createBasicPluginViewModel( "Plugin.extseed.name" );

		
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
		
		download_mon	= plugin_interface.getUtilities().getMonitor();
		
		Utilities utilities = plugin_interface.getUtilities();
		
		// XXX Would be better if we fired this off after (any) UI is complete,
		//     instead of a timer
		
		new DelayedEvent( 
			"ExternalSeedInitialise", 
			15000,
			new AERunnable()
			{
				public void 
				runSupport() 
				{
					AEThread2 t = 
						new AEThread2( "ExternalSeedInitialise", true )
						{
							public void 
							run() 
							{
								plugin_interface.getDownloadManager().addListener(
										ExternalSeedPlugin.this);
							}
						};
					
					t.setPriority( Thread.MIN_PRIORITY );
					
					t.start();
				}
			});
		
		UTTimer timer = utilities.createTimer("ExternalPeerScheduler", true);
		
		timer.addPeriodicEvent(
				5000,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						try{
							Iterator	it = download_map.values().iterator();
							
							while( it.hasNext()){
								
								List	peers = randomiseList((List)it.next());
								
								for (int i=0;i<peers.size();i++){
									
										// bail out early if the state changed for this peer
										// so one peer at a time gets a chance to activate
									
									if (((ExternalSeedPeer)peers.get(i)).checkConnection()){
										
										break;
									}
								}
							}
							
						}catch( Throwable e ){
							// we do this without holding the monitor as doing so causes potential
							// deadlock between download_mon and the connection's connection_mon
							
							// so ignore possible errors here that may be caused by concurrent
							// modification to the download_map ans associated lists. We are only
							// reading the data so errors will only be transient
						}
					}
				});
		
	}
	
	public void
	downloadAdded(
		Download	download )
	{
		Torrent	torrent = download.getTorrent();
		download.addListener(this);
		if ( torrent == null ){
			
			return;
		}
		
		addExtPeers(download);
	}

	private void addExtPeers(Download download) {
		if(!networkEnabled(download)){
			return;
		}
			
		List	peers = new ArrayList();
		
		for (int i=0;i<factories.length;i++){
			
			ExternalSeedReader[]	x = factories[i].getSeedReaders( this, download );
			
			for (int j=0;j<x.length;j++){
				
				ExternalSeedReader	reader = x[j];
				
				ExternalSeedPeer	peer = new ExternalSeedPeer( this, download, reader );
				
				peers.add( peer );
			}
		}
		log.log(LoggerChannel.LT_INFORMATION,"adding peers: " + peers.size());
		addPeers( download, peers );
	}
	
	
	// **********************************
	// edit, by isdal
	// require http seeds to have http seed network enabled
	// **********************************
	private boolean networkEnabled(Download download) {
		log.log(LoggerChannel.LT_INFORMATION, "checking networks: " + download.getName());
		byte[] infohash = download.getTorrent().getHash();
		if (infohash == null) {
			Debug.out("infohash=null");
			return false;
		}
		DownloadManager dm = TorrentUtils.getDownloadManager(new HashWrapper(
				infohash));

		if (dm == null) {
			Debug.out("downloadmanager=null");
			return false;
		}
		DownloadManagerState state = dm.getDownloadState();
		if (state == null) {
			return false;
		}

		boolean networkEnabled = false;
		for (String network : state.getNetworks()) {
			if (network.equals(AENetworkClassifier.AT_WEBSEED)) {
				networkEnabled = true;
				break;
			}
		}
		if (!networkEnabled) {
			log.log(LoggerChannel.LT_INFORMATION, "http seed network not enabled");
			return false;
		}
		return true;
	}

	//**********************************
	
	public void
	addSeed(
		Download	download,
		Map			config )
	{
		Torrent	torrent = download.getTorrent();
		
		if ( torrent == null ){
			
			return;
		}
	 
		addExtPeers(download);
	}
	
	protected void
	addPeers(
		final Download	download,
		List			_peers )
	{
		//***************************************
		if(!networkEnabled(download)){
			return;
		}
		//*************************************
		final List peers = new ArrayList();
		
		peers.addAll( _peers );
	
		if ( peers.size() > 0 ){
			
			boolean	add_listener = false;
			
			try{
				download_mon.enter();
				
				List	existing_peers = (List)download_map.get( download );
				
				if ( existing_peers == null ){
					
					add_listener	= true;
					
					existing_peers = new ArrayList();
					
					download_map.put( download, existing_peers );
				}
	
				Iterator	it = peers.iterator();
					
				while( it.hasNext()){
					
					ExternalSeedPeer	peer = (ExternalSeedPeer)it.next();
					
					boolean	skip = false;
					
					for (int j=0;j<existing_peers.size();j++){
						
						ExternalSeedPeer	existing_peer = (ExternalSeedPeer)existing_peers.get(j);
						
						if ( existing_peer.sameAs( peer )){
							
							skip	= true;
							
							break;
						}
					}

					if ( skip ){
						
						it.remove();
						
					}else{
						
						log( download.getName() + " found seed " + peer.getName());
						

						existing_peers.add( peer );
					}
				}
				
				
			}finally{
				
				download_mon.exit();
			}
			
			if ( add_listener ){
			
				download.addPeerListener(
					new DownloadPeerListener()
					{
						public void
						peerManagerAdded(
							Download		download,
							PeerManager		peer_manager )
						{
							List	existing_peers = getPeers();
	
							if ( existing_peers== null ){
								
								return;
							}
							
							for (int i=0;i<existing_peers.size();i++){
								
								ExternalSeedPeer	peer = (ExternalSeedPeer)existing_peers.get(i);
								
								peer.setManager( peer_manager );
							}
						}
						
						public void
						peerManagerRemoved(
							Download		download,
							PeerManager		peer_manager )
						{
							List	existing_peers = getPeers();
							
							if ( existing_peers== null ){
								
								return;
							}
							
							for (int i=0;i<existing_peers.size();i++){
								
								ExternalSeedPeer	peer = (ExternalSeedPeer)existing_peers.get(i);
								
								peer.setManager( null );
							}
						}
						
						protected List
						getPeers()
						{
							List	existing_peers = null;
							
							try{
								download_mon.enter();
								
								List	temp = (List)download_map.get( download );
								
								if ( temp != null ){
									
									existing_peers = new ArrayList( temp.size());
									
									existing_peers.addAll( temp );
								}
							}finally{
								
								download_mon.exit();
							}
							
							return( existing_peers );
						}
					});
			}else{
				
					// fix up newly added peers to current peer manager
				
				PeerManager	existing_pm = download.getPeerManager();
				
				if ( existing_pm != null ){
					
					for (int i=0;i<peers.size();i++){
						
						ExternalSeedPeer	peer = (ExternalSeedPeer)peers.get(i);

						if ( peer.getManager() == null ){
							
							peer.setManager( existing_pm );
						}
					}
				}
			}
		}
	}
	
	protected void
	removePeer(
		ExternalSeedPeer	peer )
	{
		Download	download = peer.getDownload();
		
		try{
			download_mon.enter();
		
			List	existing_peers = (List)download_map.get( download );

			if ( existing_peers != null ){
				
				if ( existing_peers.remove( peer )){
					
					log( download.getName() + " removed seed " + peer.getName());
				}
			}
		}finally{
			
			download_mon.exit();
		}
	}

	public void
	downloadRemoved(
		Download	download )
	{
		try{
			download_mon.enter();
			
			download_map.remove( download );
			
		}finally{
			
			download_mon.exit();
		}	
	}
	
	public ExternalSeedManualPeer[]
	getManualWebSeeds(
		Download	download )
	{
		try{
			download_mon.enter();
		
			List	peers = (List)download_map.get( download );
	
			ExternalSeedManualPeer[]	result = new ExternalSeedManualPeer[peers.size()];
			
			for (int i=0;i<peers.size();i++){
				
				result[i] = new ExternalSeedManualPeer((ExternalSeedPeer)peers.get(i));
			}
			
			return( result );
			
		}finally{
			
			download_mon.exit();
		}	
	}
	
	public int
	getGlobalDownloadRateBytesPerSec()
	{
		return( dm_stats.getDataAndProtocolReceiveRate());
	}
	
	public void
	log(
		String		str )
	{
		log.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable 	e )
	{
		log.log( str, e );
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	protected List
	randomiseList(
		List	l )
	{
		if ( l.size() < 2 ){
			
			return(l);
		}
		
		List	new_list = new ArrayList();
		
		for (int i=0;i<l.size();i++){
			
			new_list.add( random.nextInt(new_list.size()+1), l.get(i));
		}
		
		return( new_list );
	}

	public void positionChanged(Download download, int oldPosition,
			int newPosition) {
	}

	public void stateChanged(Download download, int old_state, int new_state) {
		if(new_state == Download.ST_DOWNLOADING){
			log.log(LoggerChannel.LT_INFORMATION,"download started: "+download.getName());
			this.addExtPeers(download);
		}
	}
}