/*
 * Created on 29-Jul-2005
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

package org.gudy.azureus2.core3.download.impl;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.ForceRecheckListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerDataProvider;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.network.ConnectionManager;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistration;
import com.aelitis.azureus.core.peermanager.PeerManagerRegistrationAdapter;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPeer;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;

public class 
DownloadManagerController 
	extends LogRelation
	implements PEPeerManagerAdapter, PeerManagerRegistrationAdapter
{
	private static long STATE_FLAG_HASDND = 0x01;
	private static long STATE_FLAG_COMPLETE_NO_DND = 0x02;
	
	private static long skeleton_builds;
	
	private static ExternalSeedPlugin	ext_seed_plugin;
	private static boolean				ext_seed_plugin_tried;
	
	protected static ExternalSeedPlugin
	getExternalSeedPlugin()
	{
		if ( !ext_seed_plugin_tried ){
			
			ext_seed_plugin_tried	= true;
			
			try {
				org.gudy.azureus2.plugins.PluginInterface ext_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(ExternalSeedPlugin.class);
				if (ext_pi != null) {
					ext_seed_plugin = (ExternalSeedPlugin)ext_pi.getPlugin();
				}
	
			}catch (Throwable e){
	
				Debug.printStackTrace( e );
			}
		}
		
		return( ext_seed_plugin );
	}
	
		// DISK listeners
	
	private static final int LDT_DL_ADDED		= 1;
	private static final int LDT_DL_REMOVED		= 2;

	private static ListenerManager	disk_listeners_agregator 	= ListenerManager.createAsyncManager(
			"DMC:DiskListenAgregatorDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerDiskListener	listener = (DownloadManagerDiskListener)_listener;
					
					if ( type == LDT_DL_ADDED ){
						
						listener.diskManagerAdded((DiskManager)value);
						
					}else if ( type == LDT_DL_REMOVED ){
						
						listener.diskManagerRemoved((DiskManager)value);
					}
				}
			});	
	
	private ListenerManager	disk_listeners 	= ListenerManager.createManager(
			"DMC:DiskListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		listener,
					int			type,
					Object		value )
				{
					disk_listeners_agregator.dispatch( listener, type, value );
				}
			});
	
	private AEMonitor	disk_listeners_mon	= new AEMonitor( "DownloadManagerController:DL" );
	
	private AEMonitor	control_mon		= new AEMonitor( "DownloadManagerController" );
	private AEMonitor	state_mon		= new AEMonitor( "DownloadManagerController:State" );
	private AEMonitor	facade_mon		= new AEMonitor( "DownloadManagerController:Facade" );
	
	private DownloadManagerImpl			download_manager;
	private DownloadManagerStatsImpl	stats;
	
		// these are volatile as we want to ensure that if a state is read it is always the
		// most up to date value available (as we don't synchronize state read - see below
		// for comments)
	
	private volatile int		state_set_by_method = DownloadManager.STATE_START_OF_DAY;
	private volatile int		substate;
	private volatile boolean 	force_start;

		// to try and ensure people don't start using disk_manager without properly considering its
		// access implications we've given it a silly name
	
	private volatile DiskManager 			disk_manager_use_accessors;
	private DiskManagerListener				disk_manager_listener_use_accessors;
	
	private FileInfoFacadeSet		fileFacadeSet = new FileInfoFacadeSet();
	private boolean					files_facade_destroyed;
	
	private boolean					cached_complete_excluding_dnd;
	private boolean					cached_has_dnd_files;
	private boolean         cached_values_set;
	
	private PeerManagerRegistration	peer_manager_registration;
	private PEPeerManager 			peer_manager;
	
	private List	external_rate_limiters_cow;
	
	private String errorDetail;

	private GlobalManagerStats		global_stats;
	
	private boolean bInitialized = false;
	
	private long data_send_rate_at_close;
	
	private static final int			ACTIVATION_REBUILD_TIME		= 10*60*1000;
	private static final int			BLOOM_SIZE					= 64;
	private volatile BloomFilter		activation_bloom;
	private volatile long				activation_bloom_create_time	= SystemTime.getCurrentTime();
	private volatile int				activation_count;
	private volatile long				activation_count_time;
	
	private boolean	 piece_checking_enabled	= true;
	
	private long		priority_connection_count;
	
	private static final int				HTTP_SEEDS_MAX	= 64;
	private LinkedList<ExternalSeedPeer>	http_seeds = new LinkedList<ExternalSeedPeer>();
	
	private int	md_info_dict_size;
	private volatile WeakReference<byte[]>	md_info_dict_ref = new WeakReference<byte[]>( null );
	
	private static final int MD_INFO_PEER_HISTORY_MAX 		= 128;

	private Map<String,int[]>	md_info_peer_history = 
		new LinkedHashMap<String,int[]>(MD_INFO_PEER_HISTORY_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry<String,int[]> eldest) 
			{
				return size() > MD_INFO_PEER_HISTORY_MAX;
			}
		};
	
	
	
	protected
	DownloadManagerController(
		DownloadManagerImpl	_download_manager )
	{
		download_manager = _download_manager;
		
		GlobalManager	gm = download_manager.getGlobalManager();
					
		global_stats = gm.getStats();
		
		stats	= (DownloadManagerStatsImpl)download_manager.getStats();

		cached_values_set = false;
	}
	
	protected void
	setInitialState(
		int	initial_state )
	{
			// only take note if there's been no errors
		
		bInitialized = true;
		
		if ( getState() == DownloadManager.STATE_START_OF_DAY ){
			
			setState( initial_state, true );
		}
		
		DownloadManagerState state = download_manager.getDownloadState();

		TOTorrent torrent = download_manager.getTorrent();

		if (torrent != null) {
			
			try{
				peer_manager_registration = PeerManager.getSingleton().registerLegacyManager( torrent.getHashWrapper(), this );
				
				md_info_dict_size = state.getIntAttribute( DownloadManagerState.AT_MD_INFO_DICT_SIZE );
					
				if ( md_info_dict_size == 0 ){
					
					try{
						md_info_dict_size = BEncoder.encode((Map)torrent.serialiseToMap().get( "info" )).length;
					
					}catch( Throwable e ){
						
						md_info_dict_size = -1;
					}
					
					state.setIntAttribute( DownloadManagerState.AT_MD_INFO_DICT_SIZE, md_info_dict_size );
				}
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace(e);
			}
		}
			
		if (state.parameterExists(DownloadManagerState.PARAM_DND_FLAGS)) {
			long flags = state.getLongParameter(DownloadManagerState.PARAM_DND_FLAGS);
			cached_complete_excluding_dnd = (flags & STATE_FLAG_COMPLETE_NO_DND) != 0;
			cached_has_dnd_files = (flags & STATE_FLAG_HASDND) != 0;
			cached_values_set = true;
		}
	}

	public void 
	startDownload(
		TRTrackerAnnouncer	tracker_client ) 
	{
		DiskManager	dm;
		
		try{
			control_mon.enter();
		
			if ( getState() != DownloadManager.STATE_READY ){
			
				Debug.out( "DownloadManagerController::startDownload state must be ready, " + getState());
				
				setFailed( "Inconsistent download state: startDownload, state = " + getState());

				return;
			}
			
	 		if ( tracker_client == null ){
	  			
	  			Debug.out( "DownloadManagerController:startDownload: tracker_client is null" );
	  			
	  				// one day we should really do a proper state machine for this. In the meantime...
	  				// probably caused by a "stop" during initialisation, I've reproduced it once or twice
	  				// in my life... Tidy things up so we don't sit here in a READ state that can't
	  				// be started.
	  			
	  			stopIt( DownloadManager.STATE_STOPPED, false, false );
	  			
	  			return;
	  		}
	  			
			if ( peer_manager != null ){
				
				Debug.out( "DownloadManagerController::startDownload: peer manager not null" );
				
					// try and continue....
				
				peer_manager.stopAll();
				
				DownloadManagerRateController.removePeerManager( peer_manager );
				
				peer_manager	= null;
			}
			
			dm	= getDiskManager();
			
			if ( dm == null ){
				
				Debug.out( "DownloadManagerController::startDownload: disk manager is null" );
				
				return;
			}
		
			setState( DownloadManager.STATE_DOWNLOADING, false );
		
		}finally{
			
			control_mon.exit();
	
		}
		
			// make sure it is started before making it "visible"
		
		final PEPeerManager temp = PEPeerManagerFactory.create( tracker_client.getPeerId(), this, dm );
		
		download_manager.informWillBeStarted( temp );
		
		temp.start();
	
		   //The connection to the tracker
		
		tracker_client.setAnnounceDataProvider(
	    		new TRTrackerAnnouncerDataProvider()
	    		{
	    			private long	last_reported_total_received;
	    			private long	last_reported_total_received_data;
	    			private long	last_reported_total_received_discard;
	    			private long	last_reported_total_received_failed;
	    			
	    			public String
					getName()
	    			{
	    				return( getDisplayName());
	    			}
	    			
	    			public long
	    			getTotalSent()
	    			{
	    				return(temp.getStats().getTotalDataBytesSentNoLan());
	    			}
	    			public long
	    			getTotalReceived()
	    			{
	    				long received 	= temp.getStats().getTotalDataBytesReceivedNoLan();
	    				long discarded 	= temp.getStats().getTotalDiscarded();
	    				long failed		= temp.getStats().getTotalHashFailBytes();
	    				
	    				long verified = received - ( discarded + failed );

	    				verified -= temp.getHiddenBytes();
	    				
	    					// ensure we don't go backwards. due to lack of atomicity of updates and possible
	    					// miscounting somewhere we have seen this occur
	    				
	    				if ( verified < last_reported_total_received ){
	    					
	    					verified = last_reported_total_received;
	    					
	    						// use -1 as indicator that we've reported this event
	    					
	    					if ( last_reported_total_received_data != -1 ){
	    							
	    						/*
	    						Debug.out( 
	    								getDisplayName() + ": decrease in overall downloaded - " +
	    								"data=" + received + "/" + last_reported_total_received_data +
	    								",discard=" + discarded + "/" + last_reported_total_received_discard +
	    								",fail=" + failed + "/" + last_reported_total_received_failed );
	    						*/
	    						
	    						last_reported_total_received_data = -1;
	    					}
	    				}else{
	    					
	    					last_reported_total_received = verified;
	    				
	    					last_reported_total_received_data		= received;
	    					last_reported_total_received_discard	= discarded;
	    					last_reported_total_received_failed		= failed;
	    				}
	    				
	    				return( verified < 0?0:verified );
	    			}
	    			
	    			public long
	    			getRemaining()
	    			{
	    				return( Math.max( temp.getRemaining(), temp.getHiddenBytes()));
	    			}
					
	    			public long 
	    			getFailedHashCheck() 
	    			{
	    				return( temp.getStats().getTotalHashFailBytes());
	    			}
	    			
					public String
					getExtensions()
					{
						return( getTrackerClientExtensions());
					}
					
					public int
					getMaxNewConnectionsAllowed()
					{
						return( temp.getMaxNewConnectionsAllowed());
					}
					
					public int 
					getPendingConnectionCount() 
					{
						return( temp.getPendingPeerCount());
					}
					
					public int
					getConnectedConnectionCount()
					{
						return( temp.getNbPeers() + temp.getNbSeeds());
					}
					
					public int 
					getUploadSpeedKBSec(
						boolean estimate ) 
					{
						long	current_local = stats.getDataSendRate();
						
						if ( estimate ){
							
								// see if we have an old value from previous stop/start
							
							current_local = data_send_rate_at_close;
							
							if ( current_local == 0 ){
								
								int current_global 	= global_stats.getDataSendRate();
								
								int	old_global		= global_stats.getDataSendRateAtClose();
							
								if ( current_global < old_global ){
									
									current_global = old_global;
								}
						
								List managers = download_manager.getGlobalManager().getDownloadManagers();
								
								int	num_dls = 0;
								
									// be optimistic and share out the bytes between non-seeds
								
								for (int i=0;i<managers.size();i++){
									
									DownloadManager	dm = (DownloadManager)managers.get(i);
									
									if ( dm.getStats().getDownloadCompleted( false ) == 1000 ){
										
										continue;
									}
									
									int	state = dm.getState();
									
									if ( 	state != DownloadManager.STATE_ERROR &&
											state != DownloadManager.STATE_STOPPING &&
											state != DownloadManager.STATE_STOPPED ){
										
										num_dls++;
									}
								}
								
								if ( num_dls == 0 ){
									
									current_local = current_global;
								}else{
									
									current_local = current_global/num_dls;
								}
							}
						}
												
						return((int)((current_local+1023)/1024 ));
					}
					
					public int 
					getCryptoLevel() 
					{
						return( download_manager.getCryptoLevel());
					}
					
					public void
					setPeerSources(
						String[]	allowed_sources )
					{
						DownloadManagerState	dms = download_manager.getDownloadState();
						
						String[]	sources = PEPeerSource.PS_SOURCES;
						
						for (int i=0;i<sources.length;i++){
							
							String	s = sources[i];
							
							boolean	ok = false;
							
							for (int j=0;j<allowed_sources.length;j++){
								
								if ( s.equals( allowed_sources[j] )){
									
									ok = true;
									
									break;
								}
							}
							
							if ( !ok ){
								
								dms.setPeerSourcePermitted( s, false );
							}
						}
						
						PEPeerManager pm = getPeerManager();
						
						if ( pm != null ){
							
							Set<String>	allowed = new HashSet<String>();
							
							allowed.addAll( Arrays.asList( allowed_sources ));
							
							Iterator<PEPeer> it = pm.getPeers().iterator();
						
							while( it.hasNext()){
								
								PEPeer peer = it.next();
								
								if ( !allowed.contains( peer.getPeerSource())){
									
									pm.removePeer( peer, "Peer source not permitted" );
								}
							}
						}
					}
					
					public boolean
					isPeerSourceEnabled(
						String		peer_source )
					{
						return( DownloadManagerController.this.isPeerSourceEnabled( peer_source ));
					}
	    		});
	    
		
		List	limiters;
		
		try{
			control_mon.enter();
		
			peer_manager = temp;

			DownloadManagerRateController.addPeerManager( peer_manager );
			
			limiters = external_rate_limiters_cow;
			
		}finally{
			
			control_mon.exit();
		}
		
		if ( limiters != null ){
			
			for (int i=0;i<limiters.size();i++){
				
				Object[]	entry = (Object[])limiters.get(i);
				
				temp.addRateLimiter((LimitedRateGroup)entry[0],((Boolean)entry[1]).booleanValue());
			}
		}
		
		// Inform only after peer_manager.start(), because it 
		// may have switched it to STATE_SEEDING (in which case we don't need to
		// inform).
		
		if (getState() == DownloadManager.STATE_DOWNLOADING) {
			
			download_manager.informStateChanged();
		}

		download_manager.informStarted( temp );
	}
  
  
  

	public void 
	initializeDiskManager(
		final boolean	open_for_seeding )
	{
		initializeDiskManagerSupport(
			DownloadManager.STATE_INITIALIZED,
			new DiskManagerListener()
	  			{
	  				public void
	  				stateChanged(
	  					int 	oldDMState,
	  					int		newDMState )
	  				{
	  					DiskManager	dm;
	  					
	  					try{
	  						control_mon.enter();
	  					
		  					dm = getDiskManager();

		  					if ( dm == null ){
	  						
	  								// already been cleared down
	  							
		  						return;
		  					}
		  					
	  					}finally{
	  						control_mon.exit();
	  					}
	  					
	  					try{
			  				if ( newDMState == DiskManager.FAULTY ){
			  					
			  					setFailed( dm.getErrorMessage());						
			   				}
			  					
			  				if ( oldDMState == DiskManager.CHECKING && newDMState != DiskManager.CHECKING ){
			  						
			  						// good time to trigger minimum file info fixup as the disk manager's
		  							// files are now in a good state

			  					fileFacadeSet.makeSureFilesFacadeFilled(true);

			  					stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			  						
			  					download_manager.setAssumedComplete(isDownloadComplete(false));
			  				}
			  					  
			  				if ( newDMState == DiskManager.READY ){
			  					
			  					int	completed = stats.getDownloadCompleted(false);
			  								  					
			  					if ( 	stats.getTotalDataBytesReceived() == 0 &&
			  							stats.getTotalDataBytesSent() == 0 &&
			  							stats.getSecondsDownloading() == 0 ){

			  						if ( completed < 1000 ){
		  							
			  							if ( open_for_seeding ){
			  								
			  								setFailed( "File check failed" );
			  								
			  								download_manager.getDownloadState().clearResumeData();
			  								
			  							}else{
			  								
					  						// make up some sensible "downloaded" figure for torrents that have been re-added to Azureus
					  						// and resumed 
					  				
					  									  										 
				  								// assume downloaded = uploaded, optimistic but at least results in
				  								// future share ratios relevant to amount up/down from now on
				  								// see bug 1077060 
				  								
				  							long	amount_downloaded = (completed*dm.getTotalLength())/1000;
				  								
				 							stats.setSavedDownloadedUploaded( amount_downloaded, amount_downloaded );
			  							}
			  						}else{		  					
			  								// see GlobalManager for comment on this
			  							
			  							int	dl_copies = COConfigurationManager.getIntParameter("StartStopManager_iAddForSeedingDLCopyCount");
			  		              
										if ( dl_copies > 0 ){
											
			  								stats.setSavedDownloadedUploaded( download_manager.getSize()*dl_copies, stats.getTotalDataBytesSent());
			  							}
										
							        	download_manager.getDownloadState().setFlag( DownloadManagerState.FLAG_ONLY_EVER_SEEDED, true );
			  						}
			  		        	}
			  					
			  					/* all initialization should be done here (Disk- and DownloadManager).
			  					 * assume this download is complete and won't recieve any modifications until it is stopped again
			  					 * or the user fiddles on the knobs
			  					 * discard fluff once tentatively, will save memory for many active, seeding torrent-cases
			  					 */
			  					if(completed == 1000) 			  					
			  						download_manager.getDownloadState().discardFluff();
			  				}
	  					}finally{
	  							  						
	  						download_manager.informStateChanged();
	  					}
	  				}

	                public void 
					filePriorityChanged(
						DiskManagerFileInfo	file ) 
	                {  
	                	download_manager.informPriorityChange( file );
	                }
	                
	               	public void
	            	pieceDoneChanged(
	            		DiskManagerPiece	piece )
	            	{           		
	            	}
	               	
	            	public void
	            	fileAccessModeChanged(
	            		DiskManagerFileInfo		file,
	            		int						old_mode,
	            		int						new_mode )
	            	{
	            	}
	  			});
	}
	
	protected void 
	initializeDiskManagerSupport(
		int						initialising_state,
		DiskManagerListener		listener ) 
	{
		try{
			control_mon.enter();
		
			int	entry_state = getState();
				
			if ( 	entry_state != DownloadManager.STATE_WAITING &&
					entry_state != DownloadManager.STATE_STOPPED &&
					entry_state != DownloadManager.STATE_QUEUED &&
					entry_state != DownloadManager.STATE_ERROR ){
					
				Debug.out( "DownloadManagerController::initializeDiskManager: Illegal initialize state, " + entry_state );
				
				setFailed( "Inconsistent download state: initSupport, state = " + entry_state );
				
				return;
			}
	
			DiskManager	old_dm = getDiskManager();
			 
			if ( old_dm != null ){
				
				Debug.out( "DownloadManagerController::initializeDiskManager: disk manager is not null" );
				
					// we shouldn't get here but try to recover the situation
				
				old_dm.stop( false );
				
				setDiskManager( null, null );
			}
		
			errorDetail	= "";
					
			setState( initialising_state, false );
				  		
		  	DiskManager dm = DiskManagerFactory.create( download_manager.getTorrent(), download_manager);
	  	      
	  	  	setDiskManager( dm, listener );
	  	  			  	  	
		}finally{
			
			control_mon.exit();
		
			download_manager.informStateChanged();
		}
	}
	  	  
	public boolean 
	canForceRecheck() 
	{
	  	int state = getState();
	  	  	
	  		// gotta check error + disk manager state as error can be generated by both
	  		// an overall error or a running disk manager in faulty state
	  	
	  	return(		(state == DownloadManager.STATE_STOPPED ) ||
	  	           	(state == DownloadManager.STATE_QUEUED ) ||
	  	           	(state == DownloadManager.STATE_ERROR && getDiskManager() == null));
	}

	public void 
	forceRecheck(final ForceRecheckListener l) 
	{
		try{
			control_mon.enter();
		
			if ( getDiskManager() != null || !canForceRecheck() ){
				
				Debug.out( "DownloadManagerController::forceRecheck: illegal entry state" );
				
				return;
			}
			
			final int start_state = DownloadManagerController.this.getState();
	  								
				// remove resume data
			
	  		download_manager.getDownloadState().clearResumeData();
	  					
	  			// For extra protection from a plugin stopping a checking torrent,
	  			// fake a forced start. 
	  					
	  		final boolean wasForceStarted = force_start;
	  					
	  		force_start = true;
			
				// if a file has been deleted we want this recheck to recreate the file and mark
				// it as 0%, not fail the recheck. Otherwise the only way of recovering is to remove and
				// re-add the torrent
  					
	  		download_manager.setDataAlreadyAllocated( false );
	  					
	  		initializeDiskManagerSupport( 
	  			DownloadManager.STATE_CHECKING,
	  			new forceRecheckDiskManagerListener(wasForceStarted, start_state, l));
	  		
		}finally{
			
			control_mon.exit();
		}
	}  	  
  
 	public void 
  	setPieceCheckingEnabled(
  		boolean enabled )
 	{
 		piece_checking_enabled	= enabled;
 		
 		DiskManager dm = getDiskManager();
 	
 		if ( dm != null ){
 			
 			dm.setPieceCheckingEnabled( enabled );
 		}
 	}
 	
	public void 
	stopIt(
		int 				_stateAfterStopping, 
		final boolean 		remove_torrent, 
		final boolean 		remove_data )
	{	  
		long	current_up = stats.getDataSendRate();
		
		if ( current_up != 0 ){
			
			data_send_rate_at_close = current_up;
		}
		
		boolean closing = _stateAfterStopping == DownloadManager.STATE_CLOSED;
		
		if ( closing ){
			
			_stateAfterStopping = DownloadManager.STATE_STOPPED;
		}
		
		final int stateAfterStopping	= _stateAfterStopping;
		
		try{
			control_mon.enter();
		
			int	state = getState();
		  
			if ( 	state == DownloadManager.STATE_STOPPED ||
					( state == DownloadManager.STATE_ERROR && getDiskManager() == null )) {
	    
				//already in stopped state, just do removals if necessary
	    	
				if( remove_data ){
				  
					download_manager.deleteDataFiles();
				}
	      
				if( remove_torrent ){
					
					download_manager.deleteTorrentFile();
				}
	      
				setState( _stateAfterStopping, false );
	      
				return;
			}

    
			if ( state == DownloadManager.STATE_STOPPING){
    
				return;
			}
    
			setSubState( _stateAfterStopping );
			
			setState( DownloadManager.STATE_STOPPING, false );


				// this will run synchronously but on a non-daemon thread so that it will under
  				// normal circumstances complete, even if we're closing
  	

			final	AESemaphore nd_sem = new AESemaphore( "DM:DownloadManager.NDTR" );
			
			NonDaemonTaskRunner.runAsync(
				new NonDaemonTask()
				{
					public Object
					run()
					{
						nd_sem.reserve();
						
						return( null );
					}
					
					public String
					getName()
					{
						return( "Stopping '" + getDisplayName() + "'" );
					}
					
				});
						
			try{				
				try{
		  								
					if ( peer_manager != null ){
						
					  peer_manager.stopAll(); 
					  
					  stats.saveSessionTotals();
					  
					  DownloadManagerRateController.removePeerManager( peer_manager );
					}
					
						// do this even if null as it also triggers tracker actions
					
					download_manager.informStopped( peer_manager, stateAfterStopping==DownloadManager.STATE_QUEUED );
											
					peer_manager	= null;

					DiskManager	dm = getDiskManager();
					
					if ( dm != null ){
						
						boolean went_async = dm.stop( closing );

						if ( went_async ){
							
							int	wait_count = 0;

							// Delay by 10ms, hoping for really short stop
							Thread.sleep(10);

							while( !dm.isStopped()){
							
								wait_count++;
								
								if ( wait_count > 2*60*10 ){
									
									Debug.out( "Download stop took too long to complete" );
									
									break;
									
								}else if ( wait_count % 200 == 0 ){
									
									Debug.out( "Waiting for download to stop - elapsed=" + wait_count + " sec" );
								}
								
								Thread.sleep(100);
							}
						}
						
						stats.setCompleted(stats.getCompleted());
						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
			      
					  		// we don't want to update the torrent if we're seeding
					  
						if ( !download_manager.getAssumedComplete()){
							download_manager.getDownloadState().save();
						}			  					  
					  							  
						setDiskManager( null, null );
					}
				
				 }finally{
							  
				   force_start = false;
         
				   if( remove_data ){
				   
				   		download_manager.deleteDataFiles();
				   }
				   
				   if( remove_torrent ){
				   	
					   download_manager.deleteTorrentFile();
				   }
         
				   List<ExternalSeedPeer> to_remove = new ArrayList<ExternalSeedPeer>();

				   synchronized( http_seeds ){

					   to_remove.addAll( http_seeds );
					   
					   http_seeds.clear();
				   }
					
				   for ( ExternalSeedPeer peer: to_remove ){
					   
					   peer.remove();
				   }
				   
				   		// only update the state if things haven't gone wrong
				   
				   if ( getState() == DownloadManager.STATE_STOPPING ){
					   
					   setState( stateAfterStopping, true );
				   }
				 }
			}finally{
				
				nd_sem.release();
			}
			
		}catch( Throwable e ){
  		
			Debug.printStackTrace( e );
		
		}finally{
		
			control_mon.exit();
			
			download_manager.informStateChanged();
		}
	}

	protected void
	setStateWaiting()
	{
		setState(DownloadManager.STATE_WAITING, true );
	}
  
  	public void
  	setStateFinishing()
  	{
  		setState(DownloadManager.STATE_FINISHING, true);
  	}

	public void 
	setStateDownloading() 
	{	
		if (getState() == DownloadManager.STATE_SEEDING) {
			setState(DownloadManager.STATE_DOWNLOADING, true);
		} else if (getState() != DownloadManager.STATE_DOWNLOADING) {
			Logger.log(new LogEvent(this, LogIDs.CORE, LogEvent.LT_WARNING,
					"Trying to set state to downloading when state is not seeding"));
		}
	}
	

  	public void
  	setStateSeeding(
  		boolean	never_downloaded )
	{
		// should already be finishing, but make sure (if it already is, there
		// won't be a trigger)
		setStateFinishing();

		download_manager.downloadEnded(never_downloaded);

		setState(DownloadManager.STATE_SEEDING, true);
	}
  
  	public boolean
  	isStateSeeding()
  	{
  		return( getState() == DownloadManager.STATE_SEEDING );
  	}
  	
  	protected void
  	setStateQueued()
  	{
  		setState(DownloadManager.STATE_QUEUED, true);
  	}
  
  	public int 
  	getState() 
  	{
  		if ( state_set_by_method != DownloadManager.STATE_INITIALIZED ){
		
  			return( state_set_by_method );
  		}
	
  			// we don't want to synchronize here as there are potential deadlock problems
  			// regarding the DownloadManager::addListener call invoking this method while
  			// holding the listeners monitor.
  			// 
  		DiskManager	dm = getDiskManager();
   		
	  	if ( dm == null){
			
	  		return DownloadManager.STATE_INITIALIZED;
	  	}
		
  		int diskManagerState = dm.getState();

		if (diskManagerState == DiskManager.INITIALIZING){
		
			return DownloadManager.STATE_INITIALIZED;
			
		}else if (diskManagerState == DiskManager.ALLOCATING){
		  
			return DownloadManager.STATE_ALLOCATING;
			
		}else if (diskManagerState == DiskManager.CHECKING){
		  
			return DownloadManager.STATE_CHECKING;
			
		}else if (diskManagerState == DiskManager.READY){
		  
			return DownloadManager.STATE_READY;
			
		}else if (diskManagerState == DiskManager.FAULTY){
		  
			return DownloadManager.STATE_ERROR;
		}
  		
		return DownloadManager.STATE_ERROR;
  	}
  
	protected int
  	getSubState()
  	{
		if ( state_set_by_method == DownloadManager.STATE_STOPPING ){
			
			return( substate );
		}else{
			
			return( getState());
		}
  	}

	private void
	setSubState(
		int	ss )
	{
		substate	= ss;
	}
	
	/**
	 * @param _state
	 * @param _inform_changed trigger informStateChange (which may not trigger
	 *                        listeners if state hasn't changed since last trigger)
	 */
  	private void 
  	setState(
  		int 		_state,
  		boolean		_inform_changed )
  	{   
  			// we bring this call out of the monitor block to prevent a potential deadlock whereby we chain
  			// state_mon -> control_mon (there exist numerous dependencies control_mon -> state_mon...
  		
  		boolean	call_filesExist	= false;
  		
   		try{
  			state_mon.enter();
  		
	  		int	old_state = state_set_by_method;
		  
			// note: there is a DIFFERENCE between the state held on the DownloadManager and
		    // that reported via getState as getState incorporated DiskManager states when
		    // the DownloadManager is INITIALIZED
		  	//System.out.println( "DM:setState - " + _state );
		  
	  		if ( old_state != _state ){
	    	
	  			state_set_by_method = _state;
	      	      
	  			if ( state_set_by_method != DownloadManager.STATE_QUEUED ){
	  				
	  					// only maintain this while queued
	  				
	  				activation_bloom = null;
	  				
	  				if ( state_set_by_method == DownloadManager.STATE_STOPPED ){
	  				
	  					activation_count = 0;
	  				}
	  			}
	  			
	  			if (state_set_by_method == DownloadManager.STATE_QUEUED ){
	        

	  				// don't pick up errors regarding missing data while queued. 
	  				// We'll do that when the torrent starts.  Saves time at startup
	  				// pick up any errors regarding missing data for queued SEEDING torrents
//	  				if (  download_manager.getAssumedComplete()){
//	    		  
//	  					call_filesExist	= true;
//	  				}
	    	  
	  			}else if ( state_set_by_method == DownloadManager.STATE_ERROR ){
	      
		      		// the process of attempting to start the torrent may have left some empty
		      		// directories created, some users take exception to this.
		      		// the most straight forward way of remedying this is to delete such empty
		      		// folders here
	      	
	  				TOTorrent	torrent = download_manager.getTorrent();
	    	
	  				if ( torrent != null && !torrent.isSimpleTorrent()){
	
	  					File	save_dir_file	= download_manager.getAbsoluteSaveLocation();
	
	  					if ( save_dir_file != null && save_dir_file.exists() && save_dir_file.isDirectory()){
		      		
	  						TorrentUtils.recursiveEmptyDirDelete( save_dir_file, false );
	  					}
	  				}
	  			}
	  		}
  		}finally{
  			
  			state_mon.exit();
  		}
	      
  		if ( call_filesExist ){
  			
  			filesExist( true );
  		}
  		
  		if ( _inform_changed ){
  			
  			download_manager.informStateChanged();
  		}
  	}
  
	 /**
	   * Stops the current download, then restarts it again.
	   */
	  
	public void 
	restartDownload(boolean forceRecheck)
	{
		boolean	was_force_start = isForceStart();
			    
		stopIt( DownloadManager.STATE_STOPPED, false, false );
	    
		if (forceRecheck) {
			download_manager.getDownloadState().clearResumeData();
		}

		download_manager.initialize();
	    
		if ( was_force_start ){
	    	
			setForceStart(true);
		}
	}  
	 
	protected void
	destroy()
	{
		if ( peer_manager_registration != null ){
			
			peer_manager_registration.unregister();
			
			peer_manager_registration	= null;
		}
		
		fileFacadeSet.destroyFileInfo();
	}
	
	public boolean
	isPeerSourceEnabled(
		String		peer_source )
	{
		return( download_manager.getDownloadState().isPeerSourceEnabled( peer_source ));
	}
	
		// secrets for inbound connections, support all
	
	public byte[][]
	getSecrets()
	{
		TOTorrent	torrent = download_manager.getTorrent();
				
		try{
			byte[]	secret1 = torrent.getHash();
			
			try{
				
				byte[]	secret2	 = getSecret2( torrent );
			
				return( new byte[][]{ secret1, secret2 });
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
				
				return( new byte[][]{ secret1 } );
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( new byte[0][] );
		}
	}
	
		// secrets for outbound connections, based on level of target
	
	public byte[][]
  	getSecrets(
  		int	crypto_level )
	{
		TOTorrent	torrent = download_manager.getTorrent();

		try{
			byte[]	secret;
			
			if ( crypto_level == PeerItemFactory.CRYPTO_LEVEL_1 ){
				
				secret = torrent.getHash();
				
			}else{
				
				secret = getSecret2( torrent );
			}
			
			return( new byte[][]{ secret });
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( new byte[0][] );
		}
	}
	
	protected byte[]
	getSecret2(
		TOTorrent	torrent )
	
		throws TOTorrentException
	{
		Map	secrets_map = download_manager.getDownloadState().getMapAttribute( DownloadManagerState.AT_SECRETS );
		
		if ( secrets_map == null ){
			
			secrets_map = new HashMap();
			
		}else{
			
				// clone as we can't just update the returned values
			
			secrets_map = new LightHashMap(secrets_map);
		}
		
		if ( secrets_map.size() == 0 ){
		
			secrets_map.put( "p1", torrent.getPieces()[0] );
				
			download_manager.getDownloadState().setMapAttribute( DownloadManagerState.AT_SECRETS, secrets_map );
		}
		
		return((byte[])secrets_map.get( "p1" ));
	}
	
	public boolean 
	manualRoute(
		NetworkConnection connection )
	{
		return false;
	}
	
	public long 
	getRandomSeed() 
	{
		return( download_manager.getDownloadState().getLongParameter( DownloadManagerState.PARAM_RANDOM_SEED ));
	}
	
	public void
	addRateLimiter(
		LimitedRateGroup	group,
		boolean				upload )
	{
		PEPeerManager	pm;
		
		try{
			control_mon.enter();
			
			ArrayList	new_limiters = new ArrayList( external_rate_limiters_cow==null?1:external_rate_limiters_cow.size()+1);
			
			if ( external_rate_limiters_cow != null ){
				
				new_limiters.addAll( external_rate_limiters_cow );
			}
			
			new_limiters.add( new Object[]{ group, new Boolean( upload )});
			
			external_rate_limiters_cow = new_limiters;
			
			pm	= peer_manager;
			
		}finally{
			
			control_mon.exit();
		}	
		
		if ( pm != null ){
			
			pm.addRateLimiter(group, upload);
		}
	}
	
	public void
	removeRateLimiter(
		LimitedRateGroup	group,
		boolean				upload )
	{
		PEPeerManager	pm;
		
		try{
			control_mon.enter();
			
			if ( external_rate_limiters_cow != null ){
				
				ArrayList	new_limiters = new ArrayList( external_rate_limiters_cow.size()-1);
				
				for (int i=0;i<external_rate_limiters_cow.size();i++){
					
					Object[]	entry = (Object[])external_rate_limiters_cow.get(i);
					
					if ( entry[0] != group ){
						
						new_limiters.add( entry );
					}
				}
				
				if ( new_limiters.size() == 0 ){
					
					external_rate_limiters_cow = null;
					
				}else{
				
					external_rate_limiters_cow = new_limiters;
				}
			}
			
			pm	= peer_manager;
			
		}finally{
			
			control_mon.exit();
		}	
	
		if ( pm != null ){
			
			pm.removeRateLimiter(group, upload);
		}
	}
	
	public void 
	enqueueReadRequest( 
		PEPeer							peer,
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener 	listener )
	{
		getDiskManager().enqueueReadRequest( request, listener );
	}
	
	public boolean
	activateRequest(
		InetSocketAddress	address )
	{
		if ( getState() == DownloadManager.STATE_QUEUED ){
			
			BloomFilter	bloom = activation_bloom;
			
			if ( bloom == null ){
				
				activation_bloom = bloom = BloomFilterFactory.createAddRemove4Bit( BLOOM_SIZE );
			}
			
			byte[]	address_bytes = address.getAddress().getAddress();
					
			int	hit_count = bloom.add( address_bytes );
			
			if ( hit_count > 5 ){
				
				Logger.log(
						new LogEvent(
							this, 
							LogIDs.CORE, 
							LogEvent.LT_WARNING,
							"Activate request for " + getDisplayName() + " from " + address + " denied as too many recently received" ));

				return( false );
			}
			
			Logger.log(new LogEvent(this, LogIDs.CORE, "Activate request for " + getDisplayName() + " from " + address ));

			long	now = SystemTime.getCurrentTime();

				// we don't really care about the bloom filter filling up and giving false positives
				// as activation events should be fairly rare
			
			if ( now < activation_bloom_create_time || now - activation_bloom_create_time > ACTIVATION_REBUILD_TIME ){
				
				activation_bloom = BloomFilterFactory.createAddRemove4Bit( BLOOM_SIZE );
				
				activation_bloom_create_time	= now;
			}
			
			activation_count = bloom.getEntryCount();
			
			activation_count_time = now;
			
			return( download_manager.activateRequest( activation_count ));
		}
		
		return( false );
	}
	
	public void
	deactivateRequest(
		InetSocketAddress	address )
	{
		BloomFilter	bloom = activation_bloom;
		
		if ( bloom != null ){
		
			byte[]	address_bytes = address.getAddress().getAddress();

			int	count = bloom.count( address_bytes);
			
			for (int i=0;i<count;i++){
				
				bloom.remove( address_bytes );
			}
			
			activation_count = bloom.getEntryCount();
		}
	}
	
	public int
	getActivationCount()
	{
			// in the absence of any new activations we persist the last count for the activation rebuild
			// period
		
		long	now = SystemTime.getCurrentTime();

		if ( now < activation_count_time ){
			
			activation_count_time = now;
			
		}else if ( now - activation_count_time > ACTIVATION_REBUILD_TIME ){
			
			activation_count = 0;
		}

		return( activation_count );
	}
	
	public PeerManagerRegistration
	getPeerManagerRegistration()
	{
		return( peer_manager_registration );
	}
	
  	public boolean 
  	isForceStart() 
  	{
	    return( force_start );
	}

	public void 
	setForceStart(
		boolean _force_start) 
	{
		try{
			state_mon.enter();
		
			if ( force_start != _force_start ){
		    	
				force_start = _force_start;
		      
				int	state = getState();
				
				if (	force_start && 
						(	state == DownloadManager.STATE_STOPPED || 
							state == DownloadManager.STATE_QUEUED )) {
					
						// Start it!  (Which will cause a stateChanged to trigger)
					
					setState(DownloadManager.STATE_WAITING, false );		    	  
				}
		    }
		}finally{
			
			state_mon.exit();
		}
		
			// "state" includes the force-start setting
		
		download_manager.informStateChanged();
	}
	
	protected void
	setFailed(
		String		reason )
	{
		if ( reason != null ){
  		
			errorDetail = reason;
		}
  	
		stopIt( DownloadManager.STATE_ERROR, false, false );
	}

	
	public boolean 
	filesExist(
		boolean	expected_to_be_allocated )
	{
		if ( !expected_to_be_allocated ){
			
			if ( !download_manager.isDataAlreadyAllocated()){
				
				return( false );
			}
		}
		
		DiskManager dm = getDiskManager();

		if (dm != null) {
			return dm.filesExist();
		}

		fileFacadeSet.makeSureFilesFacadeFilled(false);
		
		DiskManagerFileInfo[] files = fileFacadeSet.getFiles();

		for (int i = 0; i < files.length; i++) {
			DiskManagerFileInfo fileInfo = files[i];
			if (!fileInfo.isSkipped()) {
				File file = fileInfo.getFile(true);
				try {
					if (!file.exists()) {
						
						// For multi-file torrents, complain if the save directory is missing.
						if (!this.download_manager.getTorrent().isSimpleTorrent()) {
							File save_path = this.download_manager.getAbsoluteSaveLocation();
							if (FileUtil.isAncestorOf(save_path, file) && !save_path.exists()) {
								file = save_path; // We're going to abort very soon, so it's OK to overwrite this.
							}
						}
														
						setFailed(MessageText.getString("DownloadManager.error.datamissing")
								+ " " + file);
						return false;
						
					} else if (fileInfo.getLength() < file.length()) { 
						
							// file may be incremental creation - don't complain if too small
						
							// don't bitch if the user is happy with this
						
						if ( !COConfigurationManager.getBooleanParameter("File.truncate.if.too.large")){
							
							setFailed(MessageText.getString("DownloadManager.error.badsize")
									+ " " + file + "(" + fileInfo.getLength() + "/" + file.length() + ")");
							
							
							return false;
						}
					}
				} catch (Exception e) {
					setFailed(e.getMessage());
					return false;
				}
			}
		}

		return true;
	}
	
	public DiskManagerFileInfoSet getDiskManagerFileInfoSet()
	{
		fileFacadeSet.makeSureFilesFacadeFilled(false);
		
		return fileFacadeSet;
	}
	
	/**
	 * @deprecated use getDiskManagerFileInfoSet() instead 
	 */
   	public DiskManagerFileInfo[]
    getDiskManagerFileInfo()
   	{
   		fileFacadeSet.makeSureFilesFacadeFilled(false);
  		
   		return( fileFacadeSet.getFiles() );
   	}
	
	protected void
	fileInfoChanged()
	{
		fileFacadeSet.makeSureFilesFacadeFilled(true);
	}
	
	protected void
	filePrioritiesChanged(List files)
	{
		if (!cached_values_set) {
			fileFacadeSet.makeSureFilesFacadeFilled(false);
		}

		// no need to calculate completeness if there are no DND files and the
		// file being changed is not DND
		if (!cached_has_dnd_files && files.size() == 1 && !((DiskManagerFileInfo)files.get(0)).isSkipped()){
			return;
		}
		// if it's more than one file just do the scan anyway
		fileFacadeSet.makeSureFilesFacadeFilled(false);
		calculateCompleteness( fileFacadeSet.facadeFiles );
	}
	
	protected void
	calculateCompleteness(
		DiskManagerFileInfo[]	active )
	{
		boolean complete_excluding_dnd = true;

		boolean has_dnd_files = false;

		for (int i = 0; i < active.length; i++) {

			DiskManagerFileInfo file = active[i];

			if ( file.isSkipped()){

				has_dnd_files = true;
				
			}else if (file.getDownloaded() != file.getLength()) {

				complete_excluding_dnd = false;
				
			}
			
			if(has_dnd_files && !complete_excluding_dnd)
				break; // we can bail out early
		}

		cached_complete_excluding_dnd = complete_excluding_dnd;
		cached_has_dnd_files = has_dnd_files;
		cached_values_set = true;
		DownloadManagerState state = download_manager.getDownloadState();
		long flags = (cached_complete_excluding_dnd ? STATE_FLAG_COMPLETE_NO_DND : 0) |
								 (cached_has_dnd_files ? STATE_FLAG_HASDND : 0);
		state.setLongParameter(DownloadManagerState.PARAM_DND_FLAGS, flags);
	}
	
	/**
	 * Determine if the download is complete, excluding DND files.  This
	 * function is mostly cached when there is a DiskManager.
	 * 
	 * @return completion state
	 */
	protected boolean isDownloadComplete(boolean bIncludeDND) {
		if (!cached_values_set) {
			fileFacadeSet.makeSureFilesFacadeFilled(false);
		}

		// The calculate from stats doesn't take into consideration DND
		// So, if we have no DND files, use calculation from stats, which
		// remembers things like whether the file was once complete
		if (!cached_has_dnd_files) {
			return stats.getDownloadCompleted(false) == 1000;
		}

		// We have DND files.  If we have an existing diskmanager, then it
		// will have better information than the stats object.
		DiskManager dm = getDiskManager();

		//System.out.println(dm + ":" + (dm == null ? "null" : dm.getState() + ";" + dm.getRemainingExcludingDND()));
		if (dm != null) {

			int dm_state = dm.getState();

			if (dm_state == DiskManager.READY) {
				long remaining = bIncludeDND ? dm.getRemaining()
						: dm.getRemainingExcludingDND();
				return remaining == 0;
			}
		}

		// No DiskManager or it's in a bad state for us.
		// Assumed: We have DND files
		if (bIncludeDND) {
			// Want to include DND files in calculation, which there some, which
			// means completion MUST be false
			return false;
		}

		// Have DND files, bad DiskManager, and we don't want to include DND files
		return cached_complete_excluding_dnd;
	}
	
	protected PEPeerManager
	getPeerManager()
	{
		return( peer_manager );
	}
	
	protected DiskManager
	getDiskManager()
	{
		return( disk_manager_use_accessors );
	}
	
	protected String
	getErrorDetail()
	{
		return( errorDetail );
	}
	
 	protected void
  	setDiskManager(
  		DiskManager			new_disk_manager,
  		DiskManagerListener	new_disk_manager_listener )
  	{
 		if ( new_disk_manager != null ){
 			
 			new_disk_manager.setPieceCheckingEnabled( piece_checking_enabled );
 		}
 		
  	 	try{
	  		disk_listeners_mon.enter();
	  		
	  		DiskManager	old_disk_manager = disk_manager_use_accessors;
	  		
	  			// remove any old listeners in case the diskmanager is still running async
	  		
	  		if ( old_disk_manager != null && disk_manager_listener_use_accessors != null ){
	  			
	  			old_disk_manager.removeListener( disk_manager_listener_use_accessors );
	  		}
	  		
	  		disk_manager_use_accessors			= new_disk_manager;
	  		disk_manager_listener_use_accessors	= new_disk_manager_listener;
	  		
			if ( new_disk_manager != null ){
	 			
	 			new_disk_manager.addListener( new_disk_manager_listener );
	 		}
	  		
	  			// whether going from none->active or the other way, indicate that the file info
	  			// has changed
	  		
	  		fileInfoChanged();
	  		
	  		if ( new_disk_manager == null && old_disk_manager != null ){
	  				  			
	  			disk_listeners.dispatch( LDT_DL_REMOVED, old_disk_manager );
	  			
	  		}else if ( new_disk_manager != null && old_disk_manager == null ){
	  			
	  			disk_listeners.dispatch( LDT_DL_ADDED, new_disk_manager );
	  			
	  		}else{
	  		
	  			Debug.out( "inconsistent DiskManager state - " + new_disk_manager + "/" + old_disk_manager  );
	  		}
	  		
	  	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}	
  	}
  	
	public void
	addDiskListener(
		DownloadManagerDiskListener	listener )
	{
	 	try{
	  		disk_listeners_mon.enter();
	  		
	  		disk_listeners.addListener( listener );
	  		
	  		DiskManager	dm = getDiskManager();
	  		
			if ( dm != null ){
		
				disk_listeners.dispatch( listener, LDT_DL_ADDED, dm );
			}
	  	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}		
	}
		
	public void
	removeDiskListener(
		DownloadManagerDiskListener	listener )
	{
	 	try{
	  		disk_listeners_mon.enter();

	  		disk_listeners.removeListener( listener );
	  		
	 	}finally{
	  		
	  		disk_listeners_mon.exit();
	  	}
	}
	
	public long getDiskListenerCount() {
		return disk_listeners.size();
	}
	
	public String
	getDisplayName()
	{
		return( download_manager.getDisplayName());
	}
	
	public int
	getUploadRateLimitBytesPerSecond()
	{
		return( download_manager.getEffectiveUploadRateLimitBytesPerSecond());
	}
	
	public int
	getDownloadRateLimitBytesPerSecond()
	{
		return( stats.getDownloadRateLimitBytesPerSecond());
	}
	
		// these per-download rates are not easy to implement as we either have per-peer limits or global limits, with the download-limits being implemented
		// by adding them to all peers as peer-limits. So for the moment we stick with global (non-lan) limits 
	
	public int 
	getPermittedBytesToReceive()
	{
		return( NetworkManager.getSingleton().getRateHandler( false, false ).getCurrentNumBytesAllowed());
	}
	
	public void 
	permittedReceiveBytesUsed( 
		int bytes )
	{
		NetworkManager.getSingleton().getRateHandler( false, false ).bytesProcessed( bytes );
	}
	
	public int 
	getPermittedBytesToSend()
	{
		return( NetworkManager.getSingleton().getRateHandler( true, false ).getCurrentNumBytesAllowed());
	}
	
	public void	
	permittedSendBytesUsed(	
		int bytes )
	{
		NetworkManager.getSingleton().getRateHandler( true, false ).bytesProcessed( bytes );
	}
	
	public int
	getMaxUploads()
	{
		return( download_manager.getEffectiveMaxUploads());
	}
	
	public int
	getMaxConnections()
	{
		int	result;
		
		if ( download_manager.isMaxConnectionsWhenSeedingEnabled() && isStateSeeding()){
			
			result = download_manager.getMaxConnectionsWhenSeeding();
			
		}else{
			
			result = download_manager.getMaxConnections();
		}
		
		return( result );
	}
	
	public int
	getMaxSeedConnections()
	{
		return( download_manager.getMaxSeedConnections());
	}
	
	public boolean
	isExtendedMessagingEnabled()
	{
		return( download_manager.isExtendedMessagingEnabled());
	}
	
	public boolean
	isPeerExchangeEnabled()
	{
		return( download_manager.getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_OTHER_PEER ));
	}
	
	public int 
	getCryptoLevel() 
	{
		return( download_manager.getCryptoLevel());
	}
	
	public boolean
	isPeriodicRescanEnabled()
	{
		return( download_manager.getDownloadState().getFlag( DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES ));
	}
	
	public TRTrackerScraperResponse
	getTrackerScrapeResponse()
	{
		return( download_manager.getTrackerScrapeResponse());
	}
	
	public String
	getTrackerClientExtensions()
	{
		return( download_manager.getDownloadState().getTrackerClientExtensions());
	}
	
	public void
	setTrackerRefreshDelayOverrides(
		int	percent )
	{
		download_manager.setTrackerRefreshDelayOverrides( percent );
	}
	
	public boolean
	isNATHealthy()
	{
		return( download_manager.getNATStatus() == ConnectionManager.NAT_OK );
	}
	
	public int 
	getTorrentInfoDictSize() 
	{
		return( md_info_dict_size );
	}
	
	public byte[]
	getTorrentInfoDict(
		PEPeer		peer )		
	{
		try{
			String ip = peer.getIp();
			
			synchronized( md_info_peer_history ){
			
				int	now_secs = (int)( SystemTime.getMonotonousTime()/1000 );
				
				int[]	stats = md_info_peer_history.get( ip );
				
				if ( stats == null ){
					
					stats = new int[]{ now_secs, 0 };
					
					md_info_peer_history.put( ip, stats );
				}
				
				if ( now_secs - stats[0] > 5*60 ){
					
					stats[1] = 16*1024;
					
				}else{
					
					int	bytes = stats[1];
					
					if ( bytes >= md_info_dict_size*3 ){
						
						return( null );
					}
					
					stats[1] = bytes + 16*1024;
				}
			}
			
			byte[] data = md_info_dict_ref.get();
			
			if ( data == null ){
			
				TOTorrent torrent = download_manager.getTorrent();
		
				data = BEncoder.encode((Map)torrent.serialiseToMap().get( "info" ));
			
				md_info_dict_ref = new WeakReference<byte[]>( data );
			}
			
			return( data );
			
		}catch( Throwable e ){
			
			return( null );
		}
	}
	
	public void
	addPeer(
		PEPeer	peer )
	{
		download_manager.addPeer( peer );
	}
	
	public void
	removePeer(
		PEPeer	peer )
	{	
		download_manager.removePeer( peer );
	}
	
	public void
	addPiece(
		PEPiece	piece )
	{
		download_manager.addPiece( piece );
	}
	
	public void
	removePiece(
		PEPiece	piece )
	{
		download_manager.removePiece( piece );
	}
	
	public void
	discarded(
		PEPeer		peer,
		int			bytes )
	{
		if ( global_stats != null ){
			
			global_stats.discarded( bytes );
		}
	}
	
	public void
	protocolBytesReceived(
		PEPeer		peer,
		int			bytes )
	{
		if ( global_stats != null ){
			
			global_stats.protocolBytesReceived( bytes, peer.isLANLocal());
		}
	}
	
	public void
	dataBytesReceived(
		PEPeer		peer,
		int			bytes )
	{
		if ( global_stats != null ){
			
			global_stats.dataBytesReceived( bytes, peer.isLANLocal());
		}
	}
	
	public void
	protocolBytesSent(
		PEPeer		peer,
		int			bytes )
	{
		if ( global_stats != null ){
			
			global_stats.protocolBytesSent( bytes, peer.isLANLocal());
		}
	}
	
	public void
	dataBytesSent(
		PEPeer		peer,
		int			bytes )
	{
		if ( global_stats != null ){
			
			global_stats.dataBytesSent( bytes, peer.isLANLocal());
		}
	}
	
	public int getPosition() {
		return download_manager.getPosition();
	}
	
	public void 
	statsRequest(
		PEPeer 		originator, 
		Map 		request,
		Map			reply )
	{	
		GlobalManager	gm = download_manager.getGlobalManager();

		gm.statsRequest( request, reply );
		
		Map	info = new HashMap();
		
		reply.put( "dl", info );
		
		try{
			info.put( "u_lim", new Long( getUploadRateLimitBytesPerSecond()));
			info.put( "d_lim", new Long( getDownloadRateLimitBytesPerSecond()));
			
			info.put( "u_rate", new Long( stats.getProtocolSendRate() + stats.getDataSendRate()));
			info.put( "d_rate", new Long( stats.getProtocolReceiveRate() + stats.getDataReceiveRate()));
			
			info.put( "u_slot", new Long( getMaxUploads()));
			info.put( "c_max", new Long( getMaxConnections()));
			
			info.put( "c_leech", new Long( download_manager.getNbPeers()));
			info.put( "c_seed", new Long( download_manager.getNbSeeds()));
			
			PEPeerManager pm = peer_manager;
			
			if ( pm != null ){
				
				info.put( "c_rem", pm.getNbRemoteTCPConnections());
				info.put( "c_rem_utp", pm.getNbRemoteUTPConnections());
				info.put( "c_rem_udp", pm.getNbRemoteUDPConnections());
				
				List<PEPeer> peers = pm.getPeers();
				
				List<Long>	slot_up = new ArrayList<Long>();
				
				info.put( "slot_up", slot_up );
				
				for ( PEPeer p: peers ){
					
					if ( !p.isChokedByMe()){
						
						long up = p.getStats().getDataSendRate() + p.getStats().getProtocolSendRate();
						
						slot_up.add( up );
					}
				}
			}
		}catch( Throwable e ){
		}
	}
	
	public void
	addHTTPSeed(
		String	address,
		int		port )
	{
		ExternalSeedPlugin	plugin = getExternalSeedPlugin();

		try{
			if ( plugin != null ){
				
				Map config = new HashMap();
	
				List urls = new ArrayList();
				
				String	seed_url = "http://" + UrlUtils.convertIPV6Host(address) + ":" + port + "/webseed";
				
				urls.add( seed_url.getBytes());
	
				config.put("httpseeds", urls);
	
				Map params = new HashMap();
	
				params.put( "supports_503", new Long(0));
				params.put( "transient", new Long(1));
	
				config.put("httpseeds-params", params);
	
				List<ExternalSeedPeer> new_seeds = plugin.addSeed( org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl.getDownloadStatic( download_manager ), config);
				
				if ( new_seeds.size() > 0 ){
					
					List<ExternalSeedPeer> to_remove = new ArrayList<ExternalSeedPeer>();
					
					synchronized( http_seeds ){
						
						http_seeds.addAll( new_seeds );
						
						while( http_seeds.size() > HTTP_SEEDS_MAX ){
							
							ExternalSeedPeer x = http_seeds.removeFirst();
							
							to_remove.add( x );
						}
					}
					
					for (ExternalSeedPeer peer: to_remove ){
						
						peer.remove();
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public void
	priorityConnectionChanged(
		boolean	added )
	{
		synchronized( this ){
			
			if ( added ){
				
				priority_connection_count++;
				
			}else{
				
				priority_connection_count--;
			}
		}
	}
	
	public boolean
	hasPriorityConnection()
	{
		synchronized( this ){
			
			return( priority_connection_count > 0 ); 
		}
	}
	
	public String 
	getDescription() 
	{
		return( download_manager.getDisplayName());
	}
	
	public LogRelation
	getLogRelation()
	{
		return( this );
	}
	
	public String
	getRelationText() 
	{
		return( download_manager.getRelationText());
	}

	public Object[] 
	getQueryableInterfaces() 
	{
		List	interfaces = new ArrayList();
		
		Object[]	intf = download_manager.getQueryableInterfaces();
		
		for (int i=0;i<intf.length;i++){
			
			interfaces.add( intf[i] );
		}
		
		interfaces.add( download_manager );
		
		DiskManager	dm = getDiskManager();
		
		if ( dm != null ){
			
			interfaces.add( dm );
		}
		
		return( interfaces.toArray());
	}
	
	protected class FileInfoFacadeSet implements DiskManagerFileInfoSet {

		DiskManagerFileInfoSet delegate;
		fileInfoFacade[] facadeFiles = new fileInfoFacade[0];	// default before torrent avail

		public DiskManagerFileInfo[] getFiles() {
			return facadeFiles;
		}

		public int nbFiles() {
			if (delegate == null) {
				return 0;
			}
			return delegate.nbFiles();
		}

		public void setPriority(int[] toChange) {
			delegate.setPriority(toChange);
		}

		public void setSkipped(boolean[] toChange, boolean setSkipped) {
			delegate.setSkipped(toChange, setSkipped);
		}

		public boolean[] setStorageTypes(boolean[] toChange, int newStroageType) {
			return delegate.setStorageTypes(toChange, newStroageType);
		}

		/** XXX Don't call me, call makeSureFilesFacadeFilled() */
		protected void fixupFileInfo(fileInfoFacade[] info) {

			// too early in initialisation sequence to action this - it'll get reinvoked later anyway
			if (info.length == 0) return;
			
			final List delayed_prio_changes = new ArrayList(0);
			
			try {
				facade_mon.enter();
				if (files_facade_destroyed)	return;

				DiskManager dm = DownloadManagerController.this.getDiskManager();
				DiskManagerFileInfoSet active = null;
				
				if (dm != null) {
					int dm_state = dm.getState();
					// grab the live file info if available
					if (dm_state == DiskManager.CHECKING || dm_state == DiskManager.READY)
						active = dm.getFileSet();

				}
				if (active == null) {
					final boolean[] initialising = { true };
					// chance of recursion with this listener as the file-priority-changed is triggered
					// synchronously during construction and this can cause a listener to reenter the
					// incomplete fixup logic here + instantiate new skeletons.....
					try {
						skeleton_builds++;
						if (skeleton_builds % 1000 == 0) {
							Debug.outNoStack("Skeleton builds: " + skeleton_builds);
						}
						active = DiskManagerFactory.getFileInfoSkeleton(download_manager, new DiskManagerListener() {
							public void stateChanged(int oldState, int newState) {}

							public void filePriorityChanged(DiskManagerFileInfo file) {
								if (initialising[0]) {
									delayed_prio_changes.add(file);
								} else {
									download_manager.informPriorityChange(file);
								}
							}

							public void pieceDoneChanged(DiskManagerPiece piece) {}

							public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode, int new_mode) {}
						});
					} finally {
						initialising[0] = false;
					}
					calculateCompleteness(active.getFiles());
				}
				
				DiskManagerFileInfo[] activeFiles = active.getFiles();
				
				for (int i = 0; i < info.length; i++)
					info[i].setDelegate(activeFiles[i]);
				
				delegate = active;
				
			} finally {
				facade_mon.exit();
			}
			
			fileFacadeSet.facadeFiles = info;
			download_manager.informPrioritiesChange(delayed_prio_changes);

			delayed_prio_changes.clear();
		}
		
		private void makeSureFilesFacadeFilled(boolean refresh) {
			if (!bInitialized) return; // too early

			if (facadeFiles.length == 0) {
				fileInfoFacade[] newFacadeFiles = new fileInfoFacade[download_manager.getTorrent() == null
						? 0 : download_manager.getTorrent().getFiles().length];

				for (int i = 0; i < newFacadeFiles.length; i++)
					newFacadeFiles[i] = new fileInfoFacade();
				
				// no need to set facadeFiles, it gets set to newFacadeFiles in fixup
				fileFacadeSet.fixupFileInfo(newFacadeFiles);
			} else if (refresh) {
				fixupFileInfo(facadeFiles);
			}
		}
		
		
		protected void destroyFileInfo() {
			try {
				facade_mon.enter();
				if (fileFacadeSet == null || files_facade_destroyed)
					return;

				files_facade_destroyed = true;

				for (int i = 0; i < facadeFiles.length; i++)
					facadeFiles[i].close();
			} finally {
				facade_mon.exit();
			}
		}
	}
	
	protected class
	fileInfoFacade
		implements DiskManagerFileInfo
	{
		private DiskManagerFileInfo		delegate;
		
		private List					listeners;
		
		protected 
		fileInfoFacade()
		{
		}
		
		protected void
		setDelegate(
			DiskManagerFileInfo		_delegate )
		{
			if ( _delegate == delegate ){
				
				return;
			}
			
			if ( delegate != null ){
				
				delegate.close();
			}

	 		delegate = _delegate;
	   		
	 			// transfer any existing listeners across
	 		
	   		if ( listeners != null ){
	   			
	   			for (int i=0;i<listeners.size();i++){
	   				
	   				delegate.addListener((DiskManagerFileInfoListener)listeners.get(i));
	   			}
	   		}
		}

		public void 
		setPriority(
			int b )
		{
			delegate.setPriority(b);
		}
		
		public void 
		setSkipped(
			boolean b)
		{
			delegate.setSkipped(b);
		}
		 
		
		public boolean
		setLink(
			File	link_destination )
		{
			return( delegate.setLink( link_destination ));
		}

		public boolean
		setLinkAtomic(
			File	link_destination )
		{
			return( delegate.setLinkAtomic( link_destination ));
		}
		
		public File
		getLink()
		{
			return( delegate.getLink());
		}
		
		public boolean
		setStorageType(
			int		type )
		{
			return( delegate.setStorageType( type ));
		}
		
		public int
		getStorageType()
		{
			return( delegate.getStorageType());
		}
		
		 	
		public int 
		getAccessMode()
		{
			return( delegate.getAccessMode());
		}
		
		public long 
		getDownloaded()
		{
			return( delegate.getDownloaded());
		}
		
		public String 
		getExtension()
		{
			return( delegate.getExtension());
		}
			
		public int 
		getFirstPieceNumber()
		{
			return( delegate.getFirstPieceNumber());
		}
	  
		public int 
		getLastPieceNumber()
		{
			return( delegate.getLastPieceNumber());
		}
		
		public long 
		getLength()
		{
			return( delegate.getLength());
		}
			
		public int 
		getNbPieces()
		{
			return( delegate.getNbPieces());
		}
				
		public int 
		getPriority()
		{
			return( delegate.getPriority());
		}
		
		public boolean 
		isSkipped()
		{
			return( delegate.isSkipped());
		}
		
		public int	
		getIndex()
		{
			return( delegate.getIndex());
		}
		
		public DiskManager 
		getDiskManager()
		{
			return( delegate.getDiskManager());
		}
		
		public DownloadManager	
		getDownloadManager()
		{
			return( download_manager );
		}
		
		public File 
		getFile( boolean follow_link )
		{
			return( delegate.getFile( follow_link ));
		}
		
		public TOTorrentFile
		getTorrentFile()
		{
			return( delegate.getTorrentFile());
		}
		
		public void
		flushCache()
		
			throws	Exception
		{
			try{
				facade_mon.enter();
				
				delegate.flushCache();
				
			}finally{
				
				facade_mon.exit();
			}
		}
		
		public DirectByteBuffer
		read(
			long	offset,
			int		length )
		
			throws IOException
		{
			try{
				facade_mon.enter();
			
				return( delegate.read( offset, length ));
				
			}finally{
				
				facade_mon.exit();
			}
		}
		
		public void
		close()
		{
			try{
				facade_mon.enter();

				delegate.close();
				
			}finally{
				
				facade_mon.exit();
			}
		}
		
		public void
		addListener(
			DiskManagerFileInfoListener	listener )
		{
			if ( listeners == null ){
				
				listeners = new ArrayList();
			}
			
			listeners.add( listener );
			
			delegate.addListener( listener );
		}
		
		public void
		removeListener(
			DiskManagerFileInfoListener	listener )
		{
			listeners.remove( listener );
			
			delegate.removeListener( listener );
		}
	}
	
	public void generateEvidence(IndentWriter writer) {
		writer.println("DownloadManager Controller:");
		
		writer.indent();
		try {
			writer.println("cached info: complete w/o DND="
					+ cached_complete_excluding_dnd + "; hasDND? " + cached_has_dnd_files);

			writer.println("Complete w/DND? " + isDownloadComplete(true)
					+ "; w/o DND? " + isDownloadComplete(false));

			writer.println("filesFacade length: " + fileFacadeSet.nbFiles());

			if (force_start) {
				writer.println("Force Start");
			}

			writer.println("FilesExist? " + filesExist(download_manager.isDataAlreadyAllocated()));
			
		} finally {
			writer.exdent();
		}
	}
	
	public class forceRecheckDiskManagerListener
		implements DiskManagerListener
	{
		private final boolean wasForceStarted;

		private final int start_state;

		private final ForceRecheckListener l;

		public forceRecheckDiskManagerListener(boolean wasForceStarted,
				int start_state, ForceRecheckListener l) {
			this.wasForceStarted = wasForceStarted;
			this.start_state = start_state;
			this.l = l;
		}

		public void stateChanged(int oldDMState, int newDMState) {
			try {
				control_mon.enter();

				if (getDiskManager() == null) {

					// already closed down via stop

					download_manager.setAssumedComplete(false);

					if (l != null) {
						l.forceRecheckComplete(download_manager);
					}

					return;
				}
			} finally {

				control_mon.exit();
			}

			
			if (newDMState == DiskManager.CHECKING) {

				fileFacadeSet.makeSureFilesFacadeFilled(true);
			}

			if (newDMState == DiskManager.READY || newDMState == DiskManager.FAULTY) {

				force_start = wasForceStarted;

				stats.setDownloadCompleted(stats.getDownloadCompleted(true));

				if (newDMState == DiskManager.READY) {

					try {
						boolean only_seeding = false;
						boolean update_only_seeding = false;

						try {
							control_mon.enter();

							DiskManager dm = getDiskManager();

							if (dm != null) {

								dm.stop(false);

								only_seeding = dm.getRemainingExcludingDND() == 0;

								update_only_seeding = true;

								setDiskManager(null, null);

								if (start_state == DownloadManager.STATE_ERROR) {

									setState(DownloadManager.STATE_STOPPED, false);

								} else {

									setState(start_state, false);
								}
							}
						} finally {

							control_mon.exit();

							download_manager.informStateChanged();
						}

						// careful here, don't want to update seeding while holding monitor
						// as potential deadlock

						if (update_only_seeding) {

							download_manager.setAssumedComplete(only_seeding);
						}

					} catch (Exception e) {

						setFailed("Resume data save fails: "
								+ Debug.getNestedExceptionMessage(e));
					}
				} else { // Faulty

					try {
						control_mon.enter();

						DiskManager dm = getDiskManager();

						if (dm != null) {

							dm.stop(false);

							setDiskManager(null, null);

							setFailed(dm.getErrorMessage());
						}
					} finally {

						control_mon.exit();
					}

					download_manager.setAssumedComplete(false);

				}
				if (l != null) {
					l.forceRecheckComplete(download_manager);
				}
			}
		}

		public void filePriorityChanged(DiskManagerFileInfo file) {
			download_manager.informPriorityChange(file);
		}

		public void pieceDoneChanged(DiskManagerPiece piece) {
		}

		public void fileAccessModeChanged(DiskManagerFileInfo file, int old_mode,
				int new_mode) {
		}
	}
}
