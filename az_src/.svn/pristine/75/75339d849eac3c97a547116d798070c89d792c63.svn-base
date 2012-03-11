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
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseTTTorrent;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreLifecycleAdapter;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;


public class 
LightWeightSeedManager 
{
	private static LightWeightSeedManager singleton = new LightWeightSeedManager();
	
	public static LightWeightSeedManager
	getSingleton()
	{
		return( singleton );
	}
	
	
	private Map lws_map = new HashMap();
	
	private boolean			started;
	private Set				dht_add_queue = new HashSet();
	
	private boolean				borked;
	private DHTTrackerPlugin	dht_tracker_plugin;
	private DDBaseTTTorrent		tttorrent;
	
	private TimerEventPeriodic	timer;
	
	private AESemaphore			init_sem = new AESemaphore( "LWSM" );
	
	
	protected
	LightWeightSeedManager()
	{
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				startUp();
			}
		});
	}
	
	protected void
	startUp()
	{
		synchronized( this ){
			
			if ( started ){
				
				return;
			}
			
			started = true;
		}
	
		boolean	release_now = true;
		
		try{
			PluginInterface  pi  = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass( DHTTrackerPlugin.class );
	
			if ( pi != null ){
				
				final DHTTrackerPlugin plugin = (DHTTrackerPlugin)pi.getPlugin();
				
				new AEThread2( "LWS:waitForPlug", true )
				{
					public void
					run()
					{	
						try{
							plugin.waitUntilInitialised();
							
							if ( plugin.isRunning()){
							
								tttorrent = DDBaseImpl.getSingleton(AzureusCoreFactory.getSingleton()).getTTTorrent();
							}
							
							Set	to_add;
							
							synchronized( this ){
				
								dht_tracker_plugin = plugin;
								
								to_add = new HashSet( dht_add_queue );
								
								dht_add_queue.clear();
							}
							
							Iterator it = to_add.iterator();
							
							while( it.hasNext()){
								
								addDownload( (Download)it.next());
							}
						}finally{
							
							init_sem.releaseForever();
						}
					}
				}.start();
				
				release_now = false;
				
			}else{
				
				synchronized( this ){
					
					borked = true;
					
					dht_add_queue.clear();
				}
			}
		}finally{
			
			if ( release_now ){
				
				init_sem.releaseForever();
			}
		}
	}
	
	public LightWeightSeed
	add(
		String					name,
		HashWrapper				hash,
		URL						url,
		File					data_location,
		LightWeightSeedAdapter	adapter )
	
		throws Exception
	{
		if ( !TorrentUtils.isDecentralised( url )){
			
			throw( new Exception( "Only decentralised torrents supported" ));
		}
		
		LightWeightSeed lws;
		
		synchronized( this ){
			
			if ( lws_map.containsKey( hash )){
				
				throw( new Exception( "Seed for hash '" + ByteFormatter.encodeString( hash.getBytes()) + "' already added" ));
			}
			
			lws = new LightWeightSeed(  this, name, hash, url, data_location, adapter );
			
			lws_map.put( hash, lws );
			
			if ( timer == null ){
				
				timer = SimpleTimer.addPeriodicEvent(
							"LWSManager:timer",
							60*1000,
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event )
								{
									processTimer();
								}
							});
			}
			
			log( "Added LWS: " + name + ", " + UrlUtils.getMagnetURI( hash.getBytes()));
		}
		
		lws.start();
		
		return( lws );
	}
	
	public LightWeightSeed	
	get(
		HashWrapper		hw )
	{
		synchronized( this ){

			return((LightWeightSeed)lws_map.get( hw ));
		}
	}
	
	protected void
	processTimer()
	{
		List	to_process;
	
		synchronized( this ){

			to_process = new ArrayList( lws_map.values());
		}
		
		for ( int i=0;i<to_process.size();i++){
			
			try{
				((LightWeightSeed)to_process.get(i)).checkDeactivation();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	remove(
		LightWeightSeed		lws )
	{
		lws.stop();
		
		synchronized( this ){

			lws_map.remove( lws.getHash());
			
			if ( lws_map.size() == 0 ){
				
				if ( timer != null ){
					
					timer.cancel();
					
					timer = null;
				}
			}
		}
		
		log( "Added LWS: " + lws.getName() + ", " + UrlUtils.getMagnetURI( lws.getHash().getBytes()));
	}
	
	protected void
	addToDHTTracker(
		Download		download )
	{
		synchronized( dht_add_queue ){
			
			if ( borked ){
				
				return;
			}
			
			if ( dht_tracker_plugin == null ){
				
				dht_add_queue.add( download );
				
				return;
			}
		}
		
		init_sem.reserve();

		addDownload( download );
	}
	
	protected void
	removeFromDHTTracker(
		Download		download )
	{
		synchronized( dht_add_queue ){
			
			if ( borked ){
				
				return;
			}
			
			if ( dht_tracker_plugin == null ){
				
				dht_add_queue.remove( download );
				
				return;
			}
		}
		
		init_sem.reserve();
		
		removeDownload( download );
	}
	
	protected void
	addDownload(
		Download		download )
	{
		dht_tracker_plugin.addDownload( download );
		
		if ( tttorrent != null ){
			
			tttorrent.addDownload( download );
		}
	}
	
	protected void
	removeDownload(
		Download		download )
	{
		dht_tracker_plugin.removeDownload( download );
		
		if ( tttorrent != null ){
			
			tttorrent.removeDownload( download );
		}
	}
	
	protected void
	log(
		String		str )
	{
		Logger.log(new LogEvent(LogIDs.CORE, str ));
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		Logger.log(new LogEvent(LogIDs.CORE, str, e ));
	}
}
