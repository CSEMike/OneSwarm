/*
 * File    : DownloadManagerImpl.java
 * Created : 19-Oct-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.download.impl;
/*
 * Created on 30 juin 2003
 *
 */
 
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFactory;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.disk.impl.DiskManagerImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerUtil;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentListener;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationTask;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.control.PeerControlSchedulerFactory;
import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.core.tracker.TrackerPeerSourceAdapter;
import com.aelitis.azureus.core.util.CaseSensitiveFileMap;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;
import com.aelitis.azureus.plugins.tracker.local.LocalTrackerPlugin;

/**
 * @author Olivier
 * 
 */

public class 
DownloadManagerImpl 
	extends LogRelation
	implements DownloadManager
{
	private final static long SCRAPE_DELAY_ERROR_TORRENTS = 1000 * 60 * 60 * 2;// 2 hrs
	private final static long SCRAPE_DELAY_STOPPED_TORRENTS = 1000 * 60 * 60;  // 1 hr

	private final static long SCRAPE_INITDELAY_ERROR_TORRENTS = 1000 * 60 * 10;
	private final static long SCRAPE_INITDELAY_STOPPED_TORRENTS = 1000 * 60 * 3;

	private static int	upload_when_busy_min_secs;
	
	static{
		COConfigurationManager.addAndFireParameterListener(
			"max.uploads.when.busy.inc.min.secs",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					upload_when_busy_min_secs = COConfigurationManager.getIntParameter( name );
				}
			});
	}
	
	private static final String CFG_MOVE_COMPLETED_TOP = "Newly Seeding Torrents Get First Priority";
		// DownloadManager listeners
	
	private static final int LDT_STATECHANGED			= 1;
	private static final int LDT_DOWNLOADCOMPLETE		= 2;
	private static final int LDT_COMPLETIONCHANGED 		= 3;
	private static final int LDT_POSITIONCHANGED 		= 4;
	private static final int LDT_FILEPRIORITYCHANGED 	= 5;
	
	
	private AEMonitor	listeners_mon	= new AEMonitor( "DM:DownloadManager:L" );

	private static ListenerManager	listeners_aggregator 	= ListenerManager.createAsyncManager(
			"DM:ListenAggregatorDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		_value )
				{
					DownloadManagerListener	listener = (DownloadManagerListener)_listener;
					
					Object[]	value = (Object[])_value;
					
					DownloadManagerImpl	dm = (DownloadManagerImpl)value[0];
					
					if ( type == LDT_STATECHANGED ){
						
						listener.stateChanged(dm, ((Integer)value[1]).intValue());
						
					}else if ( type == LDT_DOWNLOADCOMPLETE ){
						
						listener.downloadComplete(dm);

					}else if ( type == LDT_COMPLETIONCHANGED ){
						
						listener.completionChanged(dm, ((Boolean)value[1]).booleanValue());

					}else if ( type == LDT_FILEPRIORITYCHANGED ){
						
						listener.filePriorityChanged(dm, (DiskManagerFileInfo)value[1]);

					}else if ( type == LDT_POSITIONCHANGED ){
												
						listener.positionChanged( dm, ((Integer)value[1]).intValue(), ((Integer)value[2]).intValue());
						                         
					}
				}
			});		
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"DM:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		listener,
					int			type,
					Object		value )
				{
					listeners_aggregator.dispatch( listener, type, value );
				}
			});	
	
		// TrackerListeners
	
	private static final int LDT_TL_ANNOUNCERESULT		= 1;
	private static final int LDT_TL_SCRAPERESULT		= 2;
	
	private ListenerManager	tracker_listeners 	= ListenerManager.createManager(
			"DM:TrackerListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerTrackerListener	listener = (DownloadManagerTrackerListener)_listener;
					
					if ( type == LDT_TL_ANNOUNCERESULT ){
						
						listener.announceResult((TRTrackerAnnouncerResponse)value);
						
					}else if ( type == LDT_TL_SCRAPERESULT ){
						
						listener.scrapeResult((TRTrackerScraperResponse)value);
					}
				}
			});	

	// PeerListeners
	
	private static final int LDT_PE_PEER_ADDED		= 1;
	private static final int LDT_PE_PEER_REMOVED	= 2;
	private static final int LDT_PE_PM_ADDED		= 5;
	private static final int LDT_PE_PM_REMOVED		= 6;
	
		// one static async manager for them all
	
	private static ListenerManager<DownloadManagerPeerListener>	peer_listeners_aggregator 	= ListenerManager.createAsyncManager(
			"DM:PeerListenAggregatorDispatcher",
			new ListenerManagerDispatcher<DownloadManagerPeerListener>()
			{
				public void
				dispatch(
					DownloadManagerPeerListener		listener,
					int								type,
					Object							value )
				{
					if ( type == LDT_PE_PEER_ADDED ){
						
						listener.peerAdded((PEPeer)value);
						
					}else if ( type == LDT_PE_PEER_REMOVED ){
						
						listener.peerRemoved((PEPeer)value);
						
					}else if ( type == LDT_PE_PM_ADDED ){
						
						listener.peerManagerAdded((PEPeerManager)value);
						
					}else if ( type == LDT_PE_PM_REMOVED ){
						
						listener.peerManagerRemoved((PEPeerManager)value);
					}			
				}
			});

	private static Object TPS_Key = new Object();
	
	private ListenerManager<DownloadManagerPeerListener>	peer_listeners 	= ListenerManager.createManager(
			"DM:PeerListenDispatcher",
			new ListenerManagerDispatcher<DownloadManagerPeerListener>()
			{
				public void
				dispatch(
					DownloadManagerPeerListener		listener,
					int			type,
					Object		value )
				{
					peer_listeners_aggregator.dispatch( listener, type, value );
				}
			});	
	
	private AEMonitor	peer_listeners_mon	= new AEMonitor( "DM:DownloadManager:PeerL" );
	
	private List	current_peers 	= new ArrayList();
	
		// PieceListeners
	
	private static final int LDT_PE_PIECE_ADDED		= 3;
	private static final int LDT_PE_PIECE_REMOVED	= 4;
	
		// one static async manager for them all
	
	private static ListenerManager	piece_listeners_aggregator 	= ListenerManager.createAsyncManager(
			"DM:PieceListenAggregatorDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerPieceListener	listener = (DownloadManagerPieceListener)_listener;
					
					if ( type == LDT_PE_PIECE_ADDED ){
						
						listener.pieceAdded((PEPiece)value);
						
					}else if ( type == LDT_PE_PIECE_REMOVED ){
						
						listener.pieceRemoved((PEPiece)value);
					}
				}
			});

	private ListenerManager	piece_listeners 	= ListenerManager.createManager(
			"DM:PieceListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		listener,
					int			type,
					Object		value )
				{
					piece_listeners_aggregator.dispatch( listener, type, value );
				}
			});	
	
	private List<DownloadManagerTPSListener>	tps_listeners;
	
	private AEMonitor	piece_listeners_mon	= new AEMonitor( "DM:DownloadManager:PeiceL" );
	
	private List	current_pieces	= new ArrayList();
  
	private DownloadManagerController	controller;
	private DownloadManagerStatsImpl	stats;

	protected AEMonitor					this_mon = new AEMonitor( "DM:DownloadManager" );
	
	private boolean		persistent;

	/**
	 * Pretend this download is complete while not running, 
	 * even if it has no data.  When the torrent starts up, the real complete
	 * level will be checked (probably by DiskManager), and if the torrent
	 * actually does have missing data at that point, the download will be thrown
	 * into error state.
	 * <p>
	 * Only a forced-recheck should clear this flag.
	 * <p>
	 * Current Implementation:<br>
	 * - implies that the user completed the download at one point<br>
	 * - Checks if there's Data Missing when torrent is done (or torrent load)
	 */
	private boolean assumedComplete;
	
	/**
	 * forceStarted torrents can't/shouldn't be automatically stopped
	 */
	
	private int			last_informed_state	= STATE_START_OF_DAY;
	private boolean		latest_informed_force_start;

	private GlobalManager globalManager;
	private String torrentFileName;
	
	private boolean	open_for_seeding;
	
	private String	display_name	= "";
	private String	internal_name	= "";
	
		// for simple torrents this refers to the torrent file itself. For non-simple it refers to the
		// folder containing the torrent's files
	
	private File	torrent_save_location;	
  
	// Position in Queue
	private int position = -1;
	
	private Object[]					read_torrent_state;
	private	DownloadManagerState		download_manager_state;
	
	private TOTorrent		torrent;
	private String 			torrent_comment;
	private String 			torrent_created_by;
	
	private TRTrackerAnnouncer 				tracker_client;
	private TRTrackerAnnouncerListener		tracker_client_listener = 
			new TRTrackerAnnouncerListener() 
			{
				public void 
				receivedTrackerResponse(
					TRTrackerAnnouncerResponse	response) 
				{
					PEPeerManager pm = controller.getPeerManager();
      
					if ( pm != null ) {
        
						pm.processTrackerResponse( response );
					}

					tracker_listeners.dispatch( LDT_TL_ANNOUNCERESULT, response );
				}

				public void 
				urlChanged(
					final TRTrackerAnnouncer	announcer,	
					final URL	 				old_url,
					URL							new_url,
					boolean 					explicit ) 
				{
					if ( explicit ){
						
							// flush connected peers on explicit url change
						
						if ( torrent.getPrivate()){
						
							final List	peers;
							
							try{
								peer_listeners_mon.enter();
					 	
								peers = new ArrayList( current_peers );
					 
							}finally{
								
								peer_listeners_mon.exit();
							}
															
							new AEThread2( "DM:torrentChangeFlusher", true )
							{
								public void
								run()
								{
									for (int i=0;i<peers.size();i++){
										
										PEPeer	peer = (PEPeer)peers.get(i);
										
										peer.getManager().removePeer( peer, "Private torrent: tracker changed" );
									}
								}
							}.start();
						}
						
						requestTrackerAnnounce( true );
					}
				}

				public void 
				urlRefresh() 
				{
					requestTrackerAnnounce( true );
				}
			};
	
				// a second listener used to catch and propagate the "stopped" event
				
	private TRTrackerAnnouncerListener		stopping_tracker_client_listener = 
		new TRTrackerAnnouncerListener() 
		{
			public void 
			receivedTrackerResponse(
				TRTrackerAnnouncerResponse	response) 
			{
				if(tracker_client == null)
					response.setPeers(new TRTrackerAnnouncerResponsePeer[0]);
				tracker_listeners.dispatch( LDT_TL_ANNOUNCERESULT, response );
			}

			public void 
			urlChanged(
				TRTrackerAnnouncer	announcer,
				URL 				old_url,
				URL					new_url,
				boolean 			explicit ) 
			{
			}

			public void 
			urlRefresh() 
			{
			}
		};
		
		
	private CopyOnWriteList	activation_listeners = new CopyOnWriteList();
	
	private long						scrape_random_seed	= SystemTime.getCurrentTime();

	private Map		data;
  
	private boolean data_already_allocated = false;
  
	private long	creation_time	= SystemTime.getCurrentTime();
  
	private int iSeedingRank;

	private boolean az_messaging_enabled = true;
   
	private boolean	dl_identity_obtained;
	private byte[]	dl_identity;
    private int 	dl_identity_hashcode;

    private int		max_uploads	= DownloadManagerState.MIN_MAX_UPLOADS;
    private int		max_connections;
    private int		max_connections_when_seeding;
    private boolean	max_connections_when_seeding_enabled;
    private int		max_seed_connections;
    private int		max_uploads_when_seeding	= DownloadManagerState.MIN_MAX_UPLOADS;
    private boolean	max_uploads_when_seeding_enabled;
    
    private int		max_upload_when_busy_bps;
    private int		current_upload_when_busy_bps;
    private long	last_upload_when_busy_update;
    private long	last_upload_when_busy_dec_time;
    
    private int		crypto_level = NetworkManager.CRYPTO_OVERRIDE_NONE;
    
	// Only call this with STATE_QUEUED, STATE_WAITING, or STATE_STOPPED unless you know what you are doing
	
	private volatile boolean	destroyed;
	
	public 
	DownloadManagerImpl(
		GlobalManager 							_gm,
		byte[]									_torrent_hash,
		String 									_torrentFileName, 
		String 									_torrent_save_dir,
		String									_torrent_save_file,
		int   									_initialState,
		boolean									_persistent,
		boolean									_recovered,
		boolean									_open_for_seeding,
		boolean									_has_ever_been_started,
		List									_file_priorities,
		DownloadManagerInitialisationAdapter	_initialisation_adapter ) 
	{
		if ( 	_initialState != STATE_WAITING &&
				_initialState != STATE_STOPPED &&
				_initialState != STATE_QUEUED ){
			
			Debug.out( "DownloadManagerImpl: Illegal start state, " + _initialState );
		}
		
		persistent			= _persistent;
		globalManager 		= _gm;
		open_for_seeding	= _open_for_seeding;

			// TODO: move this to download state!
		
    	if ( _file_priorities != null ){
    		
    		setData( "file_priorities", _file_priorities );
    	}
    	
		stats = new DownloadManagerStatsImpl( this );
  	
		controller	= new DownloadManagerController( this );
	 	
		torrentFileName = _torrentFileName;
		
		while( _torrent_save_dir.endsWith( File.separator )){
			
			_torrent_save_dir = _torrent_save_dir.substring(0, _torrent_save_dir.length()-1 );
		}
		
			// readTorrent adjusts the save dir and file to be sensible values
			
		readTorrent( 	_torrent_save_dir, _torrent_save_file, _torrent_hash, 
						persistent && !_recovered, _open_for_seeding, _has_ever_been_started,
						_initialState );		

		if ( torrent != null && _initialisation_adapter != null ){
			
			try{
				_initialisation_adapter.initialised( this );
				
			}catch( Throwable e ){
					
				Debug.printStackTrace(e);
			}
		}
	}


	private void 
	readTorrent(
		String		torrent_save_dir,
		String		torrent_save_file,
		byte[]		torrent_hash,		// can be null for initial torrents
		boolean		new_torrent,		// probably equivalend to (torrent_hash == null)????
		boolean		for_seeding,
		boolean		has_ever_been_started,
		int			initial_state )
	{		
		try{
			display_name				= torrentFileName;	// default if things go wrong decoding it
			internal_name				= "";
			torrent_comment				= "";
			torrent_created_by			= "";
			
			try{
	
					// this is the first thing we do and most likely to go wrong - hence its
					// existence is used below to indicate success or not
				
				 download_manager_state	= 
					 	DownloadManagerStateImpl.getDownloadState(
					 			this, 
					 			torrentFileName, 
					 			torrent_hash, 
					 			initial_state == DownloadManager.STATE_STOPPED || 
					 			initial_state == DownloadManager.STATE_QUEUED );
				 
				 readParameters();
				 
					// establish any file links
				 DownloadManagerStateAttributeListener attr_listener = new DownloadManagerStateAttributeListener() {
					 public void attributeEventOccurred(DownloadManager dm, String attribute_name, int event_type) {
						 if (attribute_name.equals(DownloadManagerState.AT_FILE_LINKS)) {
							 setFileLinks();
						 }
						 else if (attribute_name.equals(DownloadManagerState.AT_PARAMETERS)) {
							 readParameters();
						 }
					 }
				 };

				 download_manager_state.addListener(attr_listener, DownloadManagerState.AT_FILE_LINKS, DownloadManagerStateAttributeListener.WRITTEN);
				 download_manager_state.addListener(attr_listener, DownloadManagerState.AT_PARAMETERS, DownloadManagerStateAttributeListener.WRITTEN);
						
				 torrent	= download_manager_state.getTorrent();
				 
				 setFileLinks();
				 
				 	// We can't have the identity of this download changing as this will screw up
				 	// anyone who tries to maintain a unique set of downloads (e.g. the GlobalManager)
				 	//
				 
				 if ( !dl_identity_obtained ){
					 
					 	// flag set true below
					 
					 dl_identity			= torrent_hash==null?torrent.getHash():torrent_hash;
	                 
	                 this.dl_identity_hashcode = new String( dl_identity ).hashCode();		 
				 }
					 
				 if ( !Arrays.equals( dl_identity, torrent.getHash())){
						 
					 torrent	= null;	// prevent this download from being used
					 
					 	// set up some kinda default else things don't work wel...
					 
					 torrent_save_location = new File( torrent_save_dir, torrentFileName );
					 
					 throw( new NoStackException( "Download identity changed - please remove and re-add the download" ));
				 }
				 
				 read_torrent_state	= null;	// no longer needed if we saved it
	
				 LocaleUtilDecoder	locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );
						 
				 	// if its a simple torrent and an explicit save file wasn't supplied, use
				 	// the torrent name itself

				 display_name = FileUtil.convertOSSpecificChars(TorrentUtils.getLocalisedName(torrent),false);
				 
				 internal_name = ByteFormatter.nicePrint(torrent.getHash(),true);
	
				 	// now we know if its a simple torrent or not we can make some choices about
				 	// the save dir and file. On initial entry the save_dir will have the user-selected
				 	// save location and the save_file will be null
				 
				 File	save_dir_file	= new File( torrent_save_dir );
				 
				 // System.out.println( "before: " + torrent_save_dir + "/" + torrent_save_file );
				 
				 	// if save file is non-null then things have already been sorted out
				 
				 if ( torrent_save_file == null ){
				 		 	
				 		// make sure we're working off a canonical save dir if possible
				 	
				 	try{
				 		if ( save_dir_file.exists()){
				 			
				 			save_dir_file = save_dir_file.getCanonicalFile();
				 		}
				 	}catch( Throwable e ){
				 			
				 		Debug.printStackTrace(e);
				 	}
		
				 	if ( torrent.isSimpleTorrent()){
				 		
				 			// if target save location is a directory then we use that as the save
				 			// dir and use the torrent display name as the target. Otherwise we
				 			// use the file name
				 		
				 		if ( save_dir_file.exists()){
				 			
				 			if ( save_dir_file.isDirectory()){
				 				
				 				torrent_save_file	= display_name;
				 				
				 			}else{
				 				
				 				torrent_save_dir	= save_dir_file.getParent().toString();
				 				
				 				torrent_save_file	= save_dir_file.getName();
				 			}
				 		}else{
				 			
				 				// doesn't exist, assume it refers directly to the file
				 			
				 			if ( save_dir_file.getParent() == null ){
				 				
				 				throw( new NoStackException( "Data location '" + torrent_save_dir + "' is invalid" ));
	
				 			}
				 			
			 				torrent_save_dir	= save_dir_file.getParent().toString();
			 				
			 				torrent_save_file	= save_dir_file.getName(); 			
				 		}
				 		
				 	}else{
				 	
				 			// torrent is a folder. It is possible that the natural location
				 			// for the folder is X/Y and that in fact 'Y' already exists and
				 			// has been selected. If ths is the case the select X as the dir and Y
				 			// as the file name
				 		
				 		if ( save_dir_file.exists()){
				 			
				 			if ( !save_dir_file.isDirectory()){
				 				
				 				throw( new NoStackException( "'" + torrent_save_dir + "' is not a directory" ));
				 			}
				 			
				 			if ( save_dir_file.getName().equals( display_name )){
				 				torrent_save_dir	= save_dir_file.getParent().toString();
				 			}
				 		}
				 		
				 		torrent_save_file	= display_name;		
				 	}
				 }
	
				 torrent_save_location = new File( torrent_save_dir, torrent_save_file );
				 
				 	// final validity test must be based of potentially linked target location as file
				 	// may have been re-targetted
	
				 File	linked_target = getSaveLocation();
				 
				 if ( !linked_target.exists()){
				 	
				 		// if this isn't a new torrent then we treat the absence of the enclosing folder
				 		// as a fatal error. This is in particular to solve a problem with the use of
				 		// externally mounted torrent data on OSX, whereby a re-start with the drive unmounted
				 		// results in the creation of a local diretory in /Volumes that subsequently stuffs
				 		// up recovery when the volume is mounted
				 	
				 		// changed this to only report the error on non-windows platforms 
				 	
				 	if ( !(new_torrent || Constants.isWindows )){
				 		
							// another exception here - if the torrent has never been started then we can
							// fairly safely continue as its in a stopped state
						
						if ( has_ever_been_started ){
				 		
							throw (new NoStackException(
									MessageText.getString("DownloadManager.error.datamissing")
											+ " " + Debug.secretFileName(linked_target.toString())));
						}
				 	}
				 }	
				 
				 	// if this is a newly introduced torrent trash the tracker cache. We do this to
				 	// prevent, say, someone publishing a torrent with a load of invalid cache entries
				 	// in it and a bad tracker URL. This could be used as a DOS attack
	
				 if ( new_torrent ){
				 	
					 System.out.println("");
					download_manager_state.setLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME, SystemTime.getCurrentTime());
					 
				 		// propagate initial properties from torrent to download
					 
					boolean	low_noise = TorrentUtils.getFlag( torrent, TorrentUtils.TORRENT_FLAG_LOW_NOISE );
					
					if ( low_noise ){
						
						download_manager_state.setFlag( DownloadManagerState.FLAG_LOW_NOISE, true );
					}
							
					Map peer_cache = TorrentUtils.getPeerCache( torrent );
					
					if ( peer_cache != null ){
						
						try{
							download_manager_state.setTrackerResponseCache( peer_cache );
							
						}catch( Throwable e ){
						
							Debug.out( e );
							
							download_manager_state.setTrackerResponseCache( new HashMap());
						}
					}else{
					
						download_manager_state.setTrackerResponseCache( new HashMap());
					}
				 	
				 		// also remove resume data incase someone's published a torrent with resume
				 		// data in it
				 	
				 	if ( for_seeding ){
				 		
				 		DiskManagerFactory.setTorrentResumeDataNearlyComplete(download_manager_state);
				 		
				 		// Prevent download being considered for on-completion moving - it's considered complete anyway.
				 		download_manager_state.setFlag(DownloadManagerState.FLAG_MOVE_ON_COMPLETION_DONE, true);
	
				 	}else{
				 		
				 		download_manager_state.clearResumeData();
				 	}
				 }else{
					 
					 long	add_time = download_manager_state.getLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME );

					 if ( add_time == 0 ){

						 // grab an initial value from torrent file - migration only

						 try{
							 add_time = new File( torrentFileName ).lastModified();

						 }catch( Throwable e ){
						 }

						 if ( add_time == 0 ){

							 add_time = SystemTime.getCurrentTime();
						 }

						 download_manager_state.setLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME, add_time );
					 }
				 }
				 
		         
				 //trackerUrl = torrent.getAnnounceURL().toString();
	         
				torrent_comment = StringInterner.intern(locale_decoder.decodeString(torrent.getComment()));
	         
				if ( torrent_comment == null ){
					
				   torrent_comment	= "";
				}
				
				torrent_created_by = locale_decoder.decodeString(torrent.getCreatedBy());
	         
				if ( torrent_created_by == null ){
					
					torrent_created_by	= "";
				}
				 			 
				 	// only restore the tracker response cache for non-seeds
		   
				 if ( download_manager_state.isResumeDataComplete() || for_seeding ){
				 	
					 	// actually, can't think of a good reason not to restore the
					 	// cache for seeds, after all if the tracker's down we still want
					 	// to connect to peers to upload to
					 
					  // download_manager_state.clearTrackerResponseCache();
						
					  stats.setDownloadCompleted(1000);
				  
					  setAssumedComplete(true);
				  
				 }else{
				 					 
					 setAssumedComplete(false);
				}
			}catch( TOTorrentException e ){
			
				//Debug.printStackTrace( e );
				       		 			
				setFailed( TorrentUtils.exceptionToText( e ));
	 			
			}catch( UnsupportedEncodingException e ){
			
				Debug.printStackTrace( e );
				       					
				setFailed( MessageText.getString("DownloadManager.error.unsupportedencoding"));
				
			}catch( NoStackException e ){
				
				Debug.outNoStack( e.getMessage());
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
				   					
				setFailed( e );
				
			}finally{
				
				 dl_identity_obtained	= true;			 
			}
			
			if ( download_manager_state == null ){
				read_torrent_state = 
					new Object[]{ 	
						torrent_save_dir, torrent_save_file, torrent_hash,
						new Boolean(new_torrent), new Boolean( for_seeding ), new Boolean( has_ever_been_started ),
						new Integer( initial_state )
					};
	
					// torrent's stuffed - create a dummy "null object" to simplify use
					// by other code
				
				download_manager_state	= DownloadManagerStateImpl.getDownloadState( this );
				
					// make up something vaguely sensible for save location
				
				if ( torrent_save_file == null ){
					
					torrent_save_location = new File( torrent_save_dir );
					
				}else{
					
					torrent_save_location = new File( torrent_save_dir, torrent_save_file );
				}
				
			}else{
				
				
					// make up something vaguely sensible for save location if we haven't got one
			
				if ( torrent_save_file == null ){
				
					torrent_save_location = new File( torrent_save_dir );
				}
				
					// make sure we know what networks to use for this download
				
				if ( download_manager_state.getNetworks().length == 0 && torrent != null){
					
					String[] networks = AENetworkClassifier.getNetworks( torrent, display_name );
					
					download_manager_state.setNetworks( networks );
				}
			}			
		}finally{
			
			if ( torrent_save_location != null ){
				
				try{
					torrent_save_location = torrent_save_location.getCanonicalFile();
					
				}catch( Throwable e ){
					
					torrent_save_location = torrent_save_location.getAbsoluteFile();
				}
				
				// update cached stuff in case something changed
				getSaveLocation();
			}
			
				// must be after torrent read, so that any listeners have a TOTorrent
				// not that if things have failed above this method won't override a failed
				// state with the initial one
			
			controller.setInitialState( initial_state );
		}
	}

	protected void
	readTorrent()
	{
		if ( read_torrent_state == null ){
			
			return;
		}
		
		readTorrent(
				(String)read_torrent_state[0],
				(String)read_torrent_state[1],
				(byte[])read_torrent_state[2],
				((Boolean)read_torrent_state[3]).booleanValue(),
				((Boolean)read_torrent_state[4]).booleanValue(),
				((Boolean)read_torrent_state[5]).booleanValue(),
				((Integer)read_torrent_state[6]).intValue());

	}
	
	protected void
	readParameters()
	{
		max_connections							= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_PEERS );
		max_connections_when_seeding_enabled	= getDownloadState().getBooleanParameter( DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING_ENABLED );
		max_connections_when_seeding			= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_PEERS_WHEN_SEEDING );
		max_seed_connections					= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_SEEDS );
		max_uploads						 		= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_UPLOADS );
		max_uploads_when_seeding_enabled 		= getDownloadState().getBooleanParameter( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING_ENABLED );
		max_uploads_when_seeding 				= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_UPLOADS_WHEN_SEEDING );
		max_upload_when_busy_bps				= getDownloadState().getIntParameter( DownloadManagerState.PARAM_MAX_UPLOAD_WHEN_BUSY ) * 1024;

		max_uploads = Math.max( max_uploads, DownloadManagerState.MIN_MAX_UPLOADS );
		max_uploads_when_seeding = Math.max( max_uploads_when_seeding, DownloadManagerState.MIN_MAX_UPLOADS );
	}
	
	protected int
	getMaxConnections()
	{
		return( max_connections );
	}
	
	protected int
	getMaxConnectionsWhenSeeding()
	{
		return( max_connections_when_seeding );
	}
	
	protected boolean
	isMaxConnectionsWhenSeedingEnabled()
	{
		return( max_connections_when_seeding_enabled );
	}
	
	protected int
	getMaxSeedConnections()
	{
		return( max_seed_connections );
	}
	
	protected boolean
	isMaxUploadsWhenSeedingEnabled()
	{
		return( max_uploads_when_seeding_enabled );
	}
	
	protected int
	getMaxUploadsWhenSeeding()
	{
		return( max_uploads_when_seeding );
	}
	
	public int
	getMaxUploads()
	{
		return( max_uploads );
	}
	
	public void
	setMaxUploads(
		int	max )
	{
		download_manager_state.setIntParameter( DownloadManagerState.PARAM_MAX_UPLOADS, max );
	}
	
	public int
	getEffectiveMaxUploads()
	{
		if ( isMaxUploadsWhenSeedingEnabled() && getState() == DownloadManager.STATE_SEEDING ){
			
			return( getMaxUploadsWhenSeeding());
			
		}else{
			
			return( max_uploads );
		}
	}
	
	public int
	getEffectiveUploadRateLimitBytesPerSecond()
	{
		int	local_max_bps	= stats.getUploadRateLimitBytesPerSecond();
		int	rate			= local_max_bps;
		
		if ( max_upload_when_busy_bps != 0 ){
			
			long	now = SystemTime.getCurrentTime();
			
			if ( now < last_upload_when_busy_update || now - last_upload_when_busy_update > 5000 ){
				
				last_upload_when_busy_update	= now;
				
					// might need to impose the limit
				
				String key = TransferSpeedValidator.getActiveUploadParameter( globalManager );
				
				int	global_limit_bps = COConfigurationManager.getIntParameter( key )*1024;
								
				if ( global_limit_bps > 0 && max_upload_when_busy_bps < global_limit_bps ){
				
						// we have a global limit and a valid busy limit
				
					local_max_bps = local_max_bps==0?global_limit_bps:local_max_bps;

					GlobalManagerStats gm_stats = globalManager.getStats();
	
					int	actual = gm_stats.getDataSendRateNoLAN() + gm_stats.getProtocolSendRateNoLAN();
				
					int	move_by = ( local_max_bps - max_upload_when_busy_bps ) / 10;
					
					if ( move_by < 1024 ){
						
						move_by = 1024;
					}
					
					if ( global_limit_bps - actual <= 2*1024 ){
				
							// close enough to impose the busy limit downwards
						
						
						if ( current_upload_when_busy_bps == 0 ){
							
							current_upload_when_busy_bps = local_max_bps;
						}
						
						int	prev_upload_when_busy_bps = current_upload_when_busy_bps;
						
						current_upload_when_busy_bps -= move_by;
						
						if ( current_upload_when_busy_bps < max_upload_when_busy_bps ){
							
							current_upload_when_busy_bps = max_upload_when_busy_bps;
						}
						
						if ( current_upload_when_busy_bps < prev_upload_when_busy_bps ){
							
							last_upload_when_busy_dec_time = now;
						}
					}else{
						
							// not hitting limit, increase
						
						if ( current_upload_when_busy_bps != 0 ){
							
								// only try increment if sufficient time passed
							
							if ( 	upload_when_busy_min_secs == 0 ||
									now < last_upload_when_busy_dec_time ||
									now - last_upload_when_busy_dec_time >=  upload_when_busy_min_secs*1000 ){
									
								current_upload_when_busy_bps += move_by;
								
								if ( current_upload_when_busy_bps >= local_max_bps ){
									
									current_upload_when_busy_bps	= 0;
								}
							}
						}
					}
					
					if ( current_upload_when_busy_bps > 0 ){
						
						rate = current_upload_when_busy_bps;
					}
				}else{
					
					current_upload_when_busy_bps = 0;
				}
			}else{
				
				if ( current_upload_when_busy_bps > 0 ){
				
					rate = current_upload_when_busy_bps;
				}
			}
		}
		
		return( rate );
	}
	
	protected void
	setFileLinks()
	{
			// invalidate the cache info in case its now wrong
		
		cached_save_location	= null;
		
		DiskManagerFactory.setFileLinks( this, download_manager_state.getFileLinks());
		
		controller.fileInfoChanged();
	}
	
	protected void
	clearFileLinks()
	{
		download_manager_state.clearFileLinks();
	}
	
	protected void updateFileLinks(File old_save_path, File new_save_path) {
		try {old_save_path = old_save_path.getCanonicalFile();}
		catch (IOException ioe) {old_save_path = old_save_path.getAbsoluteFile();}
		try {new_save_path = new_save_path.getCanonicalFile();}
		catch (IOException ioe) {new_save_path = new_save_path.getAbsoluteFile();}
		
		String old_path = old_save_path.getPath();
		String new_path = new_save_path.getPath();
		
		CaseSensitiveFileMap links = download_manager_state.getFileLinks();
		Iterator it = links.keySetIterator();
		
		while(it.hasNext()){
			File	from 	= (File)it.next();
			File	to		= (File)links.get(from);
			String  from_s  = (from == null) ? null : from.getAbsolutePath();
			String  to_s    = (to == null) ? null : to.getAbsolutePath();
		
			try {
				updateFileLink(old_path, new_path, from_s, to_s);
			}
			catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}
	}
	
	// old_path -> Full location of old torrent (inclusive of save name)
	// from_loc -> Old unmodified location of file within torrent.
	// to_loc -> Old modified location of file (where the link points to).
	//
	// We have to update from_loc and to_loc.
	// We should always be modifying from_loc. Only modify to_loc if it sits within
	// the old path.
	protected void updateFileLink(String old_path, String new_path, String from_loc, String to_loc) {
		
		if (to_loc == null) return;
		if (this.torrent.isSimpleTorrent()) {
			if (!old_path.equals(from_loc)) {throw new RuntimeException("assert failure: old_path=" + old_path + ", from_loc=" + from_loc);}
			download_manager_state.setFileLink(new File(old_path), null );
			download_manager_state.setFileLink(new File(new_path), new File(new_path)); // Or should the second bit be null?
			return;
		}
			
		String from_loc_to_use = FileUtil.translateMoveFilePath(old_path, new_path, from_loc);
		if (from_loc_to_use == null) return;
		
		String to_loc_to_use = FileUtil.translateMoveFilePath(old_path, new_path, to_loc);
		if (to_loc_to_use == null) {to_loc_to_use = to_loc;}
		
		download_manager_state.setFileLink(new File(from_loc), null);
		download_manager_state.setFileLink(new File(from_loc_to_use), new File(to_loc_to_use));
		
	}
	
	// Superceded by updateFileLinks(String, String).
	/*
	protected void
	updateFileLinks(
		String		_old_dir,
		String		_new_dir,
		File		_old_save_dir )
	{
		try{
			String	old_dir 		= new File( _old_dir ).getCanonicalPath();
			String	new_dir 		= new File( _new_dir ).getCanonicalPath();
			String	old_save_dir 	= _old_save_dir.getCanonicalPath();
			
			CaseSensitiveFileMap	links = download_manager_state.getFileLinks();
			Iterator	it = links.keySetIterator();
			
			while( it.hasNext()){
				
				File	from 	= (File)it.next();
				File	to		= (File)links.get(from);
				
				if ( to == null ){
					
					continue;
					
				}
				
				String	from_str = from.getCanonicalPath();
				
				if ( from_str.startsWith( old_save_dir )){
					
					String	new_from_str;
					
					String	from_suffix = from_str.substring( old_dir.length());
					
					if ( from_suffix.startsWith( File.separator )){
						
						new_from_str = new_dir + from_suffix;
						
					}else{
						
						new_from_str = new_dir + File.separator + from_suffix;
					}
					
					String	to_str = to.getCanonicalPath();

					if ( to_str.startsWith( old_save_dir )){

						String	new_to_str;
						
						String	to_suffix = to_str.substring( old_dir.length());
						
						if ( to_suffix.startsWith( File.separator )){
							
							new_to_str = new_dir + to_suffix;
							
						}else{
							
							new_to_str = new_dir + File.separator + to_suffix;
						}
						
						to	= new File( new_to_str );
					}
					
					// System.out.println( "Updating file link:" + from + "->" + to + ":" + new_from_str );
					
					download_manager_state.setFileLink( from, null );
					download_manager_state.setFileLink( new File( new_from_str), to ); 
				}
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	*/
	
	/**
	 * @deprecated
	 */
	
	public boolean 
	filesExist()
	{
		return( filesExist( true ));
	}
	
	public boolean 
	filesExist(
		boolean expected_to_be_allocated )
	{
		return( controller.filesExist( expected_to_be_allocated ));
	}
	
	
	public boolean
	isPersistent()
	{
		return( persistent );
	}
  
	public String 
	getDisplayName() 
	{
		DownloadManagerState dms = this.getDownloadState();
		if (dms != null) {
			String result = dms.getDisplayName();
			if (result != null) {return result;}
		}
		return( display_name );
	}	
	
 	public String
	getInternalName()
  	{
 		return( internal_name );
  	}
 	
	public String 
	getErrorDetails() 
	{
		return( controller.getErrorDetail());
	}

	public long 
	getSize() 
	{
		if( torrent != null){
		
			return torrent.getSize();
		}
	  
		return 0;
	}

	protected void
	setFailed()
	{
		setFailed((String)null );
	}
  
	protected void
	setFailed(
		Throwable 	e )
	{
		setFailed( Debug.getNestedExceptionMessage(e));
	}
  
	protected void
	setFailed(
		String	str )
	{
		controller.setFailed( str );
	}
  
	protected void
	setTorrentInvalid(
		String	str )
	{
		setFailed( str );
		
		torrent	= null;
	}
  

	public void
	saveResumeData()
	{
		if ( getState() == STATE_DOWNLOADING) {

			try{
				getDiskManager().saveResumeData( true );
    		
			}catch( Exception e ){
    		
				setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
			}
		}
    
		// we don't want to update the torrent if we're seeding
	  
		if ( !assumedComplete  ){
	  	
			download_manager_state.save();
		}
	}
  
  	public void
  	saveDownload()
  	{
  		DiskManager disk_manager = controller.getDiskManager();
    
  		if ( disk_manager != null ){
    	
  			disk_manager.saveState();
  		}
    
  		download_manager_state.save();
  	}
  
  
	public void 
	initialize() 
	{
	  	// entry:  valid if waiting, stopped or queued
	  	// exit: error, ready or on the way to error
	  
		if ( torrent == null ) {

				// have a go at re-reading the torrent in case its been recovered
			
			readTorrent();
		}
		
		if ( torrent == null ) {

			setFailed();
      
			return;
		}
		         	
		// If a torrent that is assumed complete, verify that it actually has the
		// files, before we create the diskManager, which will try to allocate
		// disk space.
		if (assumedComplete && !filesExist( true )) {
			// filesExist() has set state to error for us

			// If the user wants to re-download the missing files, they must
			// do a re-check, which will reset the flag.
			return;
		}
   
		download_manager_state.setActive( true );
		
		try{
			try{
				this_mon.enter();
			
				if ( tracker_client != null ){
	
					Debug.out( "DownloadManager: initialize called with tracker client still available" );
					
					tracker_client.destroy();
				}
	
				tracker_client = TRTrackerAnnouncerFactory.create( torrent, download_manager_state.getNetworks());
	    
				tracker_client.setTrackerResponseCache( download_manager_state.getTrackerResponseCache());
					
				tracker_client.addListener( tracker_client_listener );
				
			}finally{
				
				this_mon.exit();
			}
     	
      		// we need to set the state to "initialized" before kicking off the disk manager
      		// initialisation as it should only report its status while in the "initialized"
      		// state (see getState for how this works...)
      	      
			try{
				controller.initializeDiskManager( open_for_seeding );
				
			}finally{
				
					// only supply this the very first time the torrent starts so the controller can check 
					// that things are ok. Subsequent restarts are under user control
				
				open_for_seeding	= false;
			}
			
		}catch( TRTrackerAnnouncerException e ){
 		
			setFailed( e ); 
		}
	}
  
  
	public void
	setStateWaiting()
	{
		controller.setStateWaiting();
	}
  
  	public void
  	setStateFinishing()
  	{
  		controller.setStateFinishing();
  	}
  
  	public void
  	setStateQueued()
  	{
  		controller.setStateQueued();
  	}
  
  	public int
  	getState()
  	{
  		return( controller.getState());
  	}
 
  	public int
  	getSubState()
  	{
  		return( controller.getSubState());
  	}
  	
  	public boolean
  	canForceRecheck()
  	{
		if ( getTorrent() == null ){
  	  		
  				// broken torrent, can't force recheck
  	  		
			return( false );
	  	}

  		return( controller.canForceRecheck());
  	}
  
  	public void
  	forceRecheck()
  	{
  		controller.forceRecheck(null);
  	}

  	public void
  	forceRecheck(final ForceRecheckListener l)
  	{
  		controller.forceRecheck(l);
  	}

  	public void 
  	setPieceCheckingEnabled(
  		boolean enabled )
  	{
  		controller.setPieceCheckingEnabled( enabled );
  	}
  	
    public void
    resetFile(
    	DiskManagerFileInfo		file )
    {
		int	state = getState();
  		
	  	if ( 	state == DownloadManager.STATE_STOPPED ||
	  			state == DownloadManager.STATE_ERROR ){
	  			  		
	  		DiskManagerFactory.clearResumeData( this, file );
	  		
	  	}else{
	  		
	  		Debug.out( "Download not stopped" );
	  	}
    }
    
    public void
    recheckFile(
    	DiskManagerFileInfo		file )
    {
		int	state = getState();
  		
	  	if ( 	state == DownloadManager.STATE_STOPPED ||
	  			state == DownloadManager.STATE_ERROR ){

	  		DiskManagerFactory.recheckFile( this, file );

	  	}else{
	  		
	  		Debug.out( "Download not stopped" );
	  	}
	  }
      
  	public void
  	startDownload()
  	{
 		controller.startDownload( getTrackerClient() ); 
  	}
  	
	public void 
	stopIt(
			int state_after_stopping, 
			boolean remove_torrent,
			boolean remove_data) 
	{
		try {
			boolean closing = state_after_stopping == DownloadManager.STATE_CLOSED;
			int curState = getState();
			boolean alreadyStopped = curState == STATE_STOPPED
					|| curState == STATE_STOPPING || curState == STATE_ERROR;
			boolean skipSetTimeStopped = alreadyStopped
					|| (closing && curState == STATE_QUEUED);

			if (!skipSetTimeStopped) {
				download_manager_state.setLongAttribute(
						DownloadManagerState.AT_TIME_STOPPED, SystemTime.getCurrentTime());
			}

			controller.stopIt(state_after_stopping, remove_torrent, remove_data);

		} finally {

			download_manager_state.setActive(false);
		}
	}
  	
	public boolean
	pause()
	{
		return( globalManager.pauseDownload( this ));
	}
	
	public boolean
	isPaused()
	{
		return( globalManager.isPaused( this ));
	}
	
	public void
	resume()
	{
		globalManager.resumeDownload( this );
	}
	
	public boolean getAssumedComplete() {
		return assumedComplete;
	}

	public boolean requestAssumedCompleteMode() {
		boolean bCompleteNoDND = controller.isDownloadComplete(false);

		setAssumedComplete(bCompleteNoDND);
		return bCompleteNoDND;
	}

	// Protected: Use requestAssumedCompleteMode outside of scope
	protected void setAssumedComplete(boolean _assumedComplete) {
		if (_assumedComplete) {
			long completedOn = download_manager_state.getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedOn <= 0) {
				download_manager_state.setLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME,
						SystemTime.getCurrentTime());
			}
		}
		
		if (assumedComplete == _assumedComplete) {
			return;
		}

		//Logger.log(new LogEvent(this, LogIDs.CORE, "setAssumedComplete("
		//		+ _assumedComplete + ") was " + assumedComplete));

		assumedComplete = _assumedComplete;

		if (!assumedComplete) {
			controller.setStateDownloading();
		}

		// NOTE: We don't set "stats.setDownloadCompleted(1000)" anymore because
		//       we can be in seeding mode with an unfinished torrent

		if (position != -1) {
			// we are in a new list, move to the top of the list so that we continue 
			// seeding.
			// -1 position means it hasn't been added to the global list.  We
			// shouldn't touch it, since it'll get a position once it's adding is
			// complete

			DownloadManager[] dms = { DownloadManagerImpl.this };

			// pretend we are at the bottom of the new list
			// so that move top will shift everything down one

			position = globalManager.getDownloadManagers().size() + 1;

			if (COConfigurationManager.getBooleanParameter(CFG_MOVE_COMPLETED_TOP)) {

				globalManager.moveTop(dms);

			} else {

				globalManager.moveEnd(dms);
			}

			// we left a gap in incomplete list, fixup

			globalManager.fixUpDownloadManagerPositions();
		}

		listeners.dispatch(LDT_COMPLETIONCHANGED, new Object[] {
				this,
				new Boolean(_assumedComplete) });
	}
  
  
  public int 
  getNbSeeds() 
  {
	  PEPeerManager peerManager = controller.getPeerManager();
	  
	  if (peerManager != null){
		  
		  return peerManager.getNbSeeds();
	  }
	  
	  return 0;
  }

  public int
  getNbPeers() 
  {
	  PEPeerManager peerManager = controller.getPeerManager();

	  if (peerManager != null){
		
		  return peerManager.getNbPeers();
	  }
	  
	  return 0;
  }

  

  	public String 
  	getTrackerStatus() 
  	{
  		TRTrackerAnnouncer tc = getTrackerClient();
  		
  		if (tc != null){
  			
  			return tc.getStatusString();
  		}
    
  			// no tracker, return scrape
  		
  		if (torrent != null ) {
  			
  			TRTrackerScraperResponse response = getTrackerScrapeResponse();
      
  			if (response != null) {
  				return response.getStatusString();
  				
  			}
  		}

  		return "";
  	}
  
  	public TRTrackerAnnouncer 
  	getTrackerClient() 
  	{
  		return( tracker_client );
  	}
 
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		TRTrackerAnnouncer	cl = getTrackerClient();
		
		if ( cl == null ){
			
			// this can happen due to timing issues - not work debug spew for
			// Debug.out( "setAnnounceResult called when download not running" );
			
			return;
		}
		
		cl.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
		if ( torrent != null && result != null ){
			
			TRTrackerScraper	scraper = globalManager.getTrackerScraper();
		
			TRTrackerScraperResponse current_resp = getTrackerScrapeResponse();
			
			URL	target_url;
			
			if ( current_resp != null ){
				
				target_url = current_resp.getURL();
				
			}else{
				
				target_url = torrent.getAnnounceURL();
			}
			
			scraper.setScrape( torrent, target_url, result );
		}
	}
	
	public int 
	getNbPieces() 
	{		
		if ( torrent == null ){
			
			return(0);
		}
		
		return( torrent.getNumberOfPieces());
	}


	public int 
	getTrackerTime() 
	{
		TRTrackerAnnouncer tc = getTrackerClient();
		
		if ( tc != null){
			
			return( tc.getTimeUntilNextUpdate());
		}
		
			// no tracker, return scrape
			
		if ( torrent != null ) {
				
			TRTrackerScraperResponse response = getTrackerScrapeResponse();
				
			if (response != null) {
					
				if (response.getStatus() == TRTrackerScraperResponse.ST_SCRAPING){
          
					return( -1 );
				}
					
				return (int)((response.getNextScrapeStartTime() - SystemTime.getCurrentTime()) / 1000);
			}
		}
		
		return( TRTrackerAnnouncer.REFRESH_MINIMUM_SECS );
	}

 
  	public TOTorrent
  	getTorrent() 
  	{
  		return( torrent );
  	}

 	private File	cached_save_location;
	private File	cached_save_location_result;
 	  	
  	public File 
	getSaveLocation()
  	{	  
  			// this can be called quite often - cache results for perf reasons
  		
  		File	save_location	= torrent_save_location;
  		
  		if ( save_location == cached_save_location  ){
  			
  			return( cached_save_location_result );
  		}
  			  			 			 			
 		File	res = download_manager_state.getFileLink( save_location );
 			
 		if ( res == null || res.equals(save_location) ){
 				
 			res	= save_location;
 		}else{
 			
 			try{
				res = res.getCanonicalFile();
				
			}catch( Throwable e ){
				
				res = res.getAbsoluteFile();
			}
 		}
 		
 		cached_save_location		= save_location;
 		cached_save_location_result	= res;
 		
 		return( res );
 	}
	
  	public File
  	getAbsoluteSaveLocation()
  	{
  		return( torrent_save_location );
  	}
  	
	public void setTorrentSaveDir(String new_dir) {
		setTorrentSaveDir(new_dir, this.getAbsoluteSaveLocation().getName());
	}

	public void setTorrentSaveDir(String new_dir, String dl_name) {
		File old_location = torrent_save_location;
		File new_location = new File(new_dir, dl_name);
		
		if (new_location.equals(old_location)){
			return;
		}

  		// assumption here is that the caller really knows what they are doing. You can't
  		// just change this willy nilly, it must be synchronised with reality. For example,
  		// the disk-manager calls it after moving files on completing
  		// The UI can call it as long as the torrent is stopped.
  		// Calling it while a download is active will in general result in unpredictable behaviour!
 
		updateFileLinks( old_location, new_location);

		torrent_save_location = new_location;

		try{
			torrent_save_location = torrent_save_location.getCanonicalFile();
			
		}catch( Throwable e ){
			
			torrent_save_location = torrent_save_location.getAbsoluteFile();
		}
		
		Logger.log(new LogEvent(this, LogIDs.CORE, "Torrent save directory changing from \"" + old_location.getPath() + "\" to \"" + new_location.getPath()));

		// Trying to fix a problem where downloads are being moved into the program
		// directory on my machine, and I don't know why...
		//Debug.out("Torrent save directory changing from \"" + old_location.getPath() + "\" to \"" + new_location.getPath());
		
		controller.fileInfoChanged();
	}

	public String 
	getPieceLength()
	{
		if ( torrent != null ){
			return( DisplayFormatters.formatByteCountToKiBEtc(torrent.getPieceLength()));
		}
		
		return( "" );
	}

	public String 
	getTorrentFileName() 
	{
		return torrentFileName;
	}

	public void 
	setTorrentFileName(
		String string) 
	{
		torrentFileName = string;
	}

		// this is called asynchronously when a response is received
	  
 	public void
 	setTrackerScrapeResponse(
  	TRTrackerScraperResponse	response )
 	{
  			// this is a reasonable place to pick up the change in active url caused by this scrape
  			// response and update the torrent's url accordingly
		
		Object[] res = getActiveScrapeResponse();
  		
		URL	active_url = (URL)res[1];

		if ( active_url != null && torrent != null ){
			
			torrent.setAnnounceURL( active_url );
		}
		
		if (response != null) {
			int state = getState();
			if (state == STATE_ERROR || state == STATE_STOPPED) {
				long minNextScrape;
				if (response.getStatus() == TRTrackerScraperResponse.ST_INITIALIZING) {
					minNextScrape = SystemTime.getCurrentTime()
							+ (state == STATE_ERROR ? SCRAPE_INITDELAY_ERROR_TORRENTS
									: SCRAPE_INITDELAY_STOPPED_TORRENTS);
				} else {
					minNextScrape = SystemTime.getCurrentTime()
							+ (state == STATE_ERROR ? SCRAPE_DELAY_ERROR_TORRENTS
									: SCRAPE_DELAY_STOPPED_TORRENTS);
				}
				if (response.getNextScrapeStartTime() < minNextScrape) {
					response.setNextScrapeStartTime(minNextScrape);
				}
			} else if (!response.isValid() && response.getStatus() == TRTrackerScraperResponse.ST_INITIALIZING) {
				long minNextScrape;
				// Spread the scrapes out a bit.  This is extremely helpfull on large
				// torrent lists, and trackers that do not support multi-scrapes.
				// For trackers that do support multi-scrapes, it will really delay
				// the scrape for all torrent in the tracker to the one that has
				// the lowest share ratio.
				int sr = getStats().getShareRatio();
				minNextScrape = SystemTime.getCurrentTime()
						+ ((sr > 10000 ? 10000 : sr + 1000) * 60);

				if (response.getNextScrapeStartTime() < minNextScrape) {
					response.setNextScrapeStartTime(minNextScrape);
				}
			}
			
			// Need to notify listeners, even if scrape result is not valid, in
			// case they parse invalid scrapes 
			
			if ( response.isValid() && response.getStatus() == TRTrackerScraperResponse.ST_ONLINE ){
			
				long cache = ((((long)response.getSeeds())&0x00ffffffL)<<32)|(((long)response.getPeers())&0x00ffffffL);
				
				download_manager_state.setLongAttribute( DownloadManagerState.AT_SCRAPE_CACHE, cache );
			}
			
			tracker_listeners.dispatch(LDT_TL_SCRAPERESULT, response);
		}
	}
  	
	public TRTrackerScraperResponse 
	getTrackerScrapeResponse() 
	{
		Object[] res = getActiveScrapeResponse();
		
		return((TRTrackerScraperResponse)res[0]);
	}
  
		/**
		 * Returns the "first" online scrape response found, and its active URL, otherwise one of the failing
		 * scrapes
		 * @return
		 */
	
	protected Object[]
	getActiveScrapeResponse()
	{
		TRTrackerScraperResponse 	response	= null;
       	URL							active_url	= null;
       	
		TRTrackerScraper	scraper = globalManager.getTrackerScraper();
	
		TRTrackerAnnouncer tc = getTrackerClient();
		
		if ( tc != null ){
  	
			response = scraper.scrape( tc );
		}
  
		if ( response == null && torrent != null){
      	
				// torrent not running. For multi-tracker torrents we need to behave sensibly
      			// here
      	
			TRTrackerScraperResponse	non_null_response = null;
    	
			TOTorrentAnnounceURLSet[]	sets;
			try {
				sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
			} catch (Exception e) {
				return( new Object[]{ scraper.scrape(torrent), active_url } );
			}
    	
			if ( sets.length == 0 ){
    	
				response = scraper.scrape(torrent);
   
			}else{
    			   
				URL							backup_url 		= null;
				TRTrackerScraperResponse	backup_response = null;
				
					// we use a fixed seed so that subsequent scrapes will randomise
    				// in the same order, as required by the spec. Note that if the
    				// torrent's announce sets are edited this all works fine (if we
    				// cached the randomised URL set this wouldn't work)
    		
				Random	scrape_random = new Random(scrape_random_seed);
    		
				for (int i=0;response==null && i<sets.length;i++){
    			
					TOTorrentAnnounceURLSet	set = sets[i];
    			
					URL[]	urls = set.getAnnounceURLs();
    			
					List	rand_urls = new ArrayList();
    							 	
					for (int j=0;j<urls.length;j++ ){
			  		
						URL url = urls[j];
					            									
						int pos = (int)(scrape_random.nextDouble() *  (rand_urls.size()+1));
					
						rand_urls.add(pos,url);
					}
			 	
					for (int j=0;response==null && j<rand_urls.size();j++){
						
						URL url = (URL)rand_urls.get(j);
						
						response = scraper.scrape(torrent, url);
			 		
						if ( response!= null ){
							
							int status = response.getStatus();
							
								// Exit if online
							
							if (status == TRTrackerScraperResponse.ST_ONLINE) {
								
								if ( response.isDHTBackup()){
								
										// we'll use this if we don't find anything better
									
									backup_url		= url;
									backup_response	= response;
									
									response = null;
									
									continue;
								}else{
									
									active_url	= url;

									break;
								}
							}

								// Scrape 1 at a time to save on outgoing connections
							
							if (	status == TRTrackerScraperResponse.ST_INITIALIZING || 
									status == TRTrackerScraperResponse.ST_SCRAPING) {
								
								break;
							}
								
								// treat bad scrapes as missing so we go on to 
			 					// the next tracker
			 			
							if ( (!response.isValid()) || status == TRTrackerScraperResponse.ST_ERROR ){
			 				
								if ( non_null_response == null ){
			 					
									non_null_response	= response;
								}
			 				
								response	= null;
							}					
						}
					}
				}
    		
				if ( response == null ){
    			
					if ( backup_response != null ){
						
						response 	= backup_response;
						active_url	= backup_url;
						
					}else{
						
						response = non_null_response;
					}
				}
			}
		}
		
		return( new Object[]{ response, active_url } );
	}
	
	public void
	requestTrackerAnnounce(
		boolean	force )
	{
		TRTrackerAnnouncer tc = getTrackerClient();
		
		if ( tc != null)
	
			tc.update( force );
	}

	public void
	requestTrackerScrape(
		boolean	force )
	{
		if ( torrent != null ){
	    	
			TRTrackerScraper	scraper = globalManager.getTrackerScraper();
 
			scraper.scrape( torrent, force );
		}
	}
	
	protected void
	setTrackerRefreshDelayOverrides(
		int	percent )
	{
		TRTrackerAnnouncer tc = getTrackerClient();
		
		if ( tc != null ){
			
			tc.setRefreshDelayOverrides( percent );
		}
	}
	
	protected boolean
	activateRequest(
		int		count )
	{
			// activation request for a queued torrent
				
		for (Iterator it = activation_listeners.iterator();it.hasNext();){
			
			DownloadManagerActivationListener	listener = (DownloadManagerActivationListener)it.next();
			
			try{
				
				if ( listener.activateRequest( count )){
					
					return( true );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( false );
	}
	
	public int
	getActivationCount()
	{
		return( controller.getActivationCount());
	}
	
	public String 
	getTorrentComment() 
	{
		return torrent_comment;
	}	
  
	public String 
	getTorrentCreatedBy() 
	{
		return torrent_created_by;
	}
  
	public long 
	getTorrentCreationDate() 
	{
		if (torrent==null){
			return(0);
		}
  	
		return( torrent.getCreationDate());
	}
  

	public GlobalManager
	getGlobalManager()
	{
		return( globalManager );
	}
	
	public DiskManager
	getDiskManager()
	{
		return( controller.getDiskManager());
	}
	
	public DiskManagerFileInfoSet getDiskManagerFileInfoSet()
	{
		return controller.getDiskManagerFileInfoSet();
	}
  
	/**
	 * @deprecated use getDiskManagerFileInfoSet() instead 
	 */
	public DiskManagerFileInfo[]
   	getDiskManagerFileInfo()
	{
		return( controller.getDiskManagerFileInfo());
	}
	
	public int getNumFileInfos() {
		// probably faster than controller.getDiskManagerFileInfoSet().nbFiles()
		// (especially when the facade isn't filled yet)
		return torrent == null ? 0 : torrent.isSimpleTorrent() ? 1
				: torrent.getFiles().length;
	}
	
	public PEPeerManager
	getPeerManager()
	{
		return( controller.getPeerManager());
	}

	public boolean isDownloadComplete(boolean bIncludeDND) {
		if (!bIncludeDND) {
			return assumedComplete;
		}
		
		return controller.isDownloadComplete(bIncludeDND);
	}
  	
	public void
	addListener(
		DownloadManagerListener	listener )
	{
		addListener(listener, true);
	}

	public void
	addListener(
		DownloadManagerListener	listener,
		boolean triggerStateChange )
	{
		if (listener == null) {
			Debug.out("Warning: null listener");
			return;
		}

		try{
			listeners_mon.enter();

			listeners.addListener(listener);
				
			if (triggerStateChange) {
				listener.stateChanged( this, getState());
			}

				// we DON'T dispatch a downloadComplete event here as this event is used to mark the
				// transition between downloading and seeding, NOT purely to inform of seeding status
			
		} catch (Throwable t) {
			
			Debug.out("adding listener", t);
			
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void
	removeListener(
		DownloadManagerListener	listener )
	{
		try{
			listeners_mon.enter();

			listeners.removeListener(listener);
			
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	/**
	 * Doesn't not inform if state didn't change from last inform call
	 */
	protected void
	informStateChanged()
	{
			// whenever the state changes we'll get called 
		try{
			listeners_mon.enter();
			
			int		new_state 		= controller.getState();
			boolean new_force_start	= controller.isForceStart();

			if ( 	new_state != last_informed_state ||
					new_force_start != latest_informed_force_start ){
				
				last_informed_state	= new_state;
				
				latest_informed_force_start	= new_force_start;
				
				listeners.dispatch( LDT_STATECHANGED, new Object[]{ this, new Integer( new_state )});
			}
			
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	protected void
	informDownloadEnded()
	{
		try{
			listeners_mon.enter();

			listeners.dispatch( LDT_DOWNLOADCOMPLETE, new Object[]{ this });
		
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	 void informPrioritiesChange(
			List	files )
		{
			controller.filePrioritiesChanged(files);
			
			try{
				listeners_mon.enter();

				for(int i=0;i<files.size();i++)
					listeners.dispatch( LDT_FILEPRIORITYCHANGED, new Object[]{ this, (DiskManagerFileInfo)files.get(i) });
			
			}finally{
				
				listeners_mon.exit();
			}
			
			requestAssumedCompleteMode();
		}
	
	
	protected void
	informPriorityChange(
		DiskManagerFileInfo	file )
	{
		informPrioritiesChange(Collections.singletonList(file));
	}
	
	protected void
	informPositionChanged(
		int new_position )
	{
		try{
			listeners_mon.enter();
			
			int	old_position = position;
			
			if ( new_position != old_position ){
				
				position = new_position;
				
				listeners.dispatch( 
					LDT_POSITIONCHANGED, 
					new Object[]{ this, new Integer( old_position ), new Integer( new_position )});
				
				// an active torrent changed its position, scheduling needs to be updated
				if(getState() == DownloadManager.STATE_SEEDING || getState() == DownloadManager.STATE_DOWNLOADING)
					PeerControlSchedulerFactory.updateScheduleOrdering();
			}
		}finally{
			
			listeners_mon.exit();
		}
	}

	public void
	addPeerListener(
		DownloadManagerPeerListener	listener )
	{
		addPeerListener(listener, true);
	}

	public void
	addPeerListener(
		DownloadManagerPeerListener	listener,
		boolean bDispatchForExisting )
	{
		try{
			peer_listeners_mon.enter();
			
			peer_listeners.addListener( listener );
			
			if (!bDispatchForExisting)
				return; // finally will call
  		
			for (int i=0;i<current_peers.size();i++){
  			
				peer_listeners.dispatch( listener, LDT_PE_PEER_ADDED, current_peers.get(i));
			}
				
			PEPeerManager	temp = controller.getPeerManager();
		
			if ( temp != null ){
	
				peer_listeners.dispatch( listener, LDT_PE_PM_ADDED, temp );
			}
  	
		}finally{

			peer_listeners_mon.exit();
		}
	}
		
	public void
	removePeerListener(
		DownloadManagerPeerListener	listener )
	{
		peer_listeners.removeListener( listener );
	}	
 
	public void
	addPieceListener(
		DownloadManagerPieceListener	listener )
	{
		addPieceListener(listener, true);
	}

	public void
	addPieceListener(
		DownloadManagerPieceListener	listener,
		boolean 						bDispatchForExisting )
	{
		try{
			piece_listeners_mon.enter();
			
			piece_listeners.addListener( listener );
			
			if (!bDispatchForExisting)
				return; // finally will call
		
			for (int i=0;i<current_pieces.size();i++){
  			
				piece_listeners.dispatch( listener, LDT_PE_PIECE_ADDED, current_pieces.get(i));
			}
  	
		}finally{

			piece_listeners_mon.exit();
		}
	}
		
	public void
	removePieceListener(
		DownloadManagerPieceListener	listener )
	{
		piece_listeners.removeListener( listener );
	}	
 
 
	
	public void
	addPeer(
		PEPeer 		peer )
	{
		try{
			peer_listeners_mon.enter();
 	
			current_peers.add( peer );
  		
			peer_listeners.dispatch( LDT_PE_PEER_ADDED, peer );
  		
		}finally{
		
			peer_listeners_mon.exit();
		}
	}
		
	public void
	removePeer(
		PEPeer		peer )
	{
		try{
			peer_listeners_mon.enter();
    	
			current_peers.remove( peer );
    	
			peer_listeners.dispatch( LDT_PE_PEER_REMOVED, peer );
    	
		}finally{
    	
			peer_listeners_mon.exit();
		}
		
			// if we're a seed and they're a seed then no point in keeping in the announce cache
			// if it happens to be there - avoid seed-seed connections in the future
		
		if ( (peer.isSeed() || peer.isRelativeSeed()) && isDownloadComplete( false )){
	
			TRTrackerAnnouncer	announcer = tracker_client;
			
			if ( announcer != null ){
				
				announcer.removeFromTrackerResponseCache( peer.getIp(), peer.getTCPListenPort());
			}
		}
	}
		
	public PEPeer[] 
	getCurrentPeers() 
	{
		try{
			peer_listeners_mon.enter();

			return (PEPeer[])current_peers.toArray(new PEPeer[current_peers.size()]);
			
		}finally{
			
			peer_listeners_mon.exit();

		}
	}

	public void
	addPiece(
		PEPiece 	piece )
	{
		try{
			piece_listeners_mon.enter();
  		
			current_pieces.add( piece );
  		
			piece_listeners.dispatch( LDT_PE_PIECE_ADDED, piece );
  		
		}finally{
  		
			piece_listeners_mon.exit();
		}
	}
		
	public void
	removePiece(
		PEPiece		piece )
	{
		try{
			piece_listeners_mon.enter();
  		
			current_pieces.remove( piece );
  		
			piece_listeners.dispatch( LDT_PE_PIECE_REMOVED, piece );
  		
		}finally{
  		
			piece_listeners_mon.exit();
		}
	}

	public PEPiece[] 
	getCurrentPieces() 
	{
		try{
			piece_listeners_mon.enter();

			return (PEPiece[])current_pieces.toArray(new PEPiece[current_pieces.size()]);
			
		}finally{
			
			piece_listeners_mon.exit();

		}	
	}

	protected void
	informWillBeStarted(
		PEPeerManager	pm )
	{
			// hack I'm afraid - sometimes we want synchronous notification of a peer manager's
			// creation before it actually starts
		
		List l = peer_listeners.getListenersCopy();
		
		for (int i=0;i<l.size();i++){
			
			try{
				((DownloadManagerPeerListener)l.get(i)).peerManagerWillBeAdded( pm );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
  	protected void
  	informStarted(
		PEPeerManager	pm )
  	{
		try{
			peer_listeners_mon.enter();
			
			peer_listeners.dispatch( LDT_PE_PM_ADDED, pm );
		}finally{
		
			peer_listeners_mon.exit();
		}
	
		TRTrackerAnnouncer tc = getTrackerClient();
		
		if ( tc != null ){
			
			tc.update( true );
		}
  	}
  
  	protected void
  	informStopped(
		PEPeerManager	pm,
		boolean			for_queue )	// can be null if controller was already stopped....
  	{
  		if ( pm != null ){
		  
  			try{
  				peer_listeners_mon.enter();
			  
  				peer_listeners.dispatch( LDT_PE_PM_REMOVED, pm );
			  	
  			}finally{
			  	
  				peer_listeners_mon.exit();
  			}
  		}
				
  		try{
  			this_mon.enter();
	  
  			if ( tracker_client != null ){
			
				tracker_client.addListener( stopping_tracker_client_listener );

  				tracker_client.removeListener( tracker_client_listener );
		
 				download_manager_state.setTrackerResponseCache(	tracker_client.getTrackerResponseCache());
 				
 				// we have serialized what we need -> can destroy retained stuff now
 				tracker_client.getLastResponse().setPeers(new TRTrackerAnnouncerResponsePeer[0]); 				
 				
 				// currently only report this for complete downloads...
 				
 				tracker_client.stop( for_queue && isDownloadComplete( false ));
 				
  				tracker_client.destroy();
				
  				tracker_client = null;
  			}
		}finally{
			
			this_mon.exit();
		}
  	}
  
	public DownloadManagerStats
	getStats()
	{
		return( stats );
	}

	public boolean 
	isForceStart() 
	{
		return( controller.isForceStart());
	}	

	public void 
	setForceStart(
			boolean forceStart) 
	{
		controller.setForceStart( forceStart );
	}

	  /**
	   * Is called when a download is finished.
	   * Activates alerts for the user.
	   *
	   * @param never_downloaded true indicates that we never actually downloaded
	   *                         anything in this session, but we determined that
	   *                         the download is complete (usually via
	   *                         startDownload())
	   *
	   * @author Rene Leonhardt
	   */
	
	protected void 
	downloadEnded(
		boolean	never_downloaded )
	{
	    if ( !never_downloaded ){
		
	    	if (isForceStart()){
    	
	    		setForceStart(false);
	    	}

	    	setAssumedComplete(true);
	
	    	informDownloadEnded();
	    }
	    
	    TRTrackerAnnouncer	tc = tracker_client;
	    
	    if ( tc != null ){
	    	
	    	DiskManager	dm = getDiskManager();
	    	
	    		// only report "complete" if we really are complete, not a dnd completion event
	    	
	    	if ( dm != null && dm.getRemaining() == 0 ){
	    		
	    		tc.complete( never_downloaded );
	    	}
	    }
	}

 
	public void
	addDiskListener(
		DownloadManagerDiskListener	listener )
	{
		controller.addDiskListener( listener );
	}
		
	public void
	removeDiskListener(
		DownloadManagerDiskListener	listener )
	{
		controller.removeDiskListener( listener );
	}
  
	public void
    addActivationListener(
    	DownloadManagerActivationListener listener )
	{
		activation_listeners.add( listener );
	}

    public void
    removeActivationListener(
    	DownloadManagerActivationListener listener )
    {
    	activation_listeners.remove( listener );
    }
    
	public int 
	getHealthStatus() 
	{
		int	state = getState();
	  
		PEPeerManager	peerManager	 = controller.getPeerManager();
	  
		TRTrackerAnnouncer tc = getTrackerClient();
	  
		if( tc != null && peerManager != null && (state == STATE_DOWNLOADING || state == STATE_SEEDING)) {
		  
			int nbSeeds = getNbSeeds();
			int nbPeers = getNbPeers();
			int nbRemotes = peerManager.getNbRemoteTCPConnections() + peerManager.getNbRemoteUTPConnections();
			
			TRTrackerAnnouncerResponse	announce_response = tc.getLastResponse();
			
			int trackerStatus = announce_response.getStatus();
			
			boolean isSeed = (state == STATE_SEEDING);
      
			if( (nbSeeds + nbPeers) == 0) {
    	  
				if( isSeed ){
        	
					return WEALTH_NO_TRACKER;	// not connected to any peer and seeding
				}
        
				return WEALTH_KO;        // not connected to any peer and downloading
			}
      
      			// read the spec for this!!!!
      			// no_tracker =
      			//	1) if downloading -> no tracker
      			//	2) if seeding -> no connections		(dealt with above)
      
			if ( !isSeed ){
    	  
				if( 	trackerStatus == TRTrackerAnnouncerResponse.ST_OFFLINE || 
						trackerStatus == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR){
	    	  
					return WEALTH_NO_TRACKER;
				}
			}
      
			if( nbRemotes == 0 ){
       
				TRTrackerScraperResponse scrape_response = getTrackerScrapeResponse();
				
				if ( scrape_response != null && scrape_response.isValid()){
					
						// if we're connected to everyone then report OK as we can't get
						// any incoming connections!
					
					if ( 	nbSeeds == scrape_response.getSeeds() &&
							nbPeers == scrape_response.getPeers()){
						
						return WEALTH_OK;
					}
				}
				
				return WEALTH_NO_REMOTE;
			}
      
			return WEALTH_OK;
      
		} else if (state == STATE_ERROR) {
			return WEALTH_ERROR;
		}else{
    	
			return WEALTH_STOPPED;
		}
	}
  
	public int 
	getNATStatus() 
	{
		int	state = getState();
	  
		PEPeerManager	peerManager	 = controller.getPeerManager();
	  
		TRTrackerAnnouncer tc = getTrackerClient();
	  
		if ( tc != null && peerManager != null && (state == STATE_DOWNLOADING || state == STATE_SEEDING)) {
		  			
			if ( peerManager.getNbRemoteTCPConnections() > 0 || peerManager.getNbRemoteUTPConnections() > 0 ){
				
				return( ConnectionManager.NAT_OK );
			}
			
			long	last_good_time = peerManager.getLastRemoteConnectionTime();
		
			if ( last_good_time > 0 ){
				
					// half an hour's grace
				
				if ( SystemTime.getCurrentTime() - last_good_time < 30*60*1000 ){
				
					return( ConnectionManager.NAT_OK );
					
				}else{
					
					return( ConnectionManager.NAT_PROBABLY_OK );
				}
			}
			
			TRTrackerAnnouncerResponse	announce_response = tc.getLastResponse();
			
			int trackerStatus = announce_response.getStatus();
			
			if( 	trackerStatus == TRTrackerAnnouncerResponse.ST_OFFLINE || 
					trackerStatus == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR){
	    	  
				return ConnectionManager.NAT_UNKNOWN;
			}
			
				// tracker's ok but no remotes - give it some time
			
			if ( SystemTime.getCurrentTime() - peerManager.getTimeStarted() < 3*60*1000 ){
				
				return ConnectionManager.NAT_UNKNOWN;
			}
			
			TRTrackerScraperResponse scrape_response = getTrackerScrapeResponse();
				
			if ( scrape_response != null && scrape_response.isValid()){
					
					// if we're connected to everyone then report OK as we can't get
					// any incoming connections!
					
				if ( 	peerManager.getNbSeeds() == scrape_response.getSeeds() &&
						peerManager.getNbPeers() == scrape_response.getPeers()){
						
					return ConnectionManager.NAT_UNKNOWN;
				}
				
					// can't expect incoming if we're seeding and there are no peers
				
				if ( state == STATE_SEEDING  && scrape_response.getPeers() == 0 ){
					
					return ConnectionManager.NAT_UNKNOWN;
				}
			}else{
				
					// no scrape and we're seeding - don't use this as sign of badness as
					// we can't determine
				
				if ( state == STATE_SEEDING ){
					
					return ConnectionManager.NAT_UNKNOWN;
				}
			}
				
			return ConnectionManager.NAT_BAD;
	
		}else{
    	
			return ConnectionManager.NAT_UNKNOWN;
		}
	}
  
	public int 
	getPosition() 
	{
		return position;
	}

	public void 
	setPosition(
		int new_position ) 
	{
		informPositionChanged( new_position );
	}

	public void
	addTrackerListener(
		DownloadManagerTrackerListener	listener )
	{  		
		tracker_listeners.addListener( listener );
	}
  
	public void
	removeTrackerListener(
		DownloadManagerTrackerListener	listener )
	{
  		tracker_listeners.removeListener( listener );
	}
  
	protected void 
	deleteDataFiles() 
	{
		DownloadManagerState state = getDownloadState();
		
		DiskManagerFactory.deleteDataFiles(
			torrent, 
			torrent_save_location.getParent(), 
			torrent_save_location.getName(),
			( 	state.getFlag( DownloadManagerState.FLAG_LOW_NOISE ) ||
				state.getFlag( DownloadManagerState.FLAG_FORCE_DIRECT_DELETE )));
		
		// Attempted fix for bug 1572356 - apparently sometimes when we perform removal of a download's data files,
		// it still somehow gets processed by the move-on-removal rules. I'm making the assumption that this method
		// is only called when a download is about to be removed.
		
		state.setFlag(DownloadManagerState.FLAG_DISABLE_AUTO_FILE_MOVE, true);
	}
  
	protected void 
	deleteTorrentFile() 
	{
		if ( torrentFileName != null ){
  		
			TorrentUtils.delete( new File(torrentFileName),getDownloadState().getFlag( DownloadManagerState.FLAG_LOW_NOISE ));
		}
	}
  

	public DownloadManagerState 
	getDownloadState()
	{	
		return( download_manager_state );
	}
  
	public Object getData (String key){ return( getUserData( key ));}

	    
	public void setData (String key, Object value){ setUserData( key, value ); }
	
  /** To retreive arbitrary objects against a download. */
  public Object getUserData (Object key) {
  	if (data == null) return null;
    return data.get(key);
  }

  /** To store arbitrary objects against a download. */
  public void setUserData (Object key, Object value) {
  	try{
  		peer_listeners_mon.enter();
  	
	  	if (data == null) {
	  	  data = new LightHashMap();
	  	}
	    if (value == null) {
	      if (data.containsKey(key))
	        data.remove(key);
	    } else {
	      data.put(key, value);
	    }
  	}finally{
  		
  		peer_listeners_mon.exit();
  	}
  }
  
  
  public boolean 
  isDataAlreadyAllocated() 
  {  
  	return data_already_allocated;  
  }
  
  public void 
  setDataAlreadyAllocated( 
  	boolean already_allocated ) 
  {
    data_already_allocated = already_allocated;
  }
    
  public void setSeedingRank(int rank) {
    iSeedingRank = rank;
  }
  
  public int getSeedingRank() {
    return iSeedingRank;
  }

  public long
  getCreationTime()
  {
  	return( creation_time );
  }

  public void
  setCreationTime(
  	long		t )
  {
  	creation_time	= t;
  }
  
  
  public boolean isExtendedMessagingEnabled() {  return az_messaging_enabled;  }
  
  public void 
  setAZMessagingEnabled( 
	boolean enable ) 
  {
    az_messaging_enabled = enable;
  }
  
  public void
  setCryptoLevel(
	int		level )
  {
	  crypto_level = level;
  }
  
  public int
  getCryptoLevel()
  {
	  return( crypto_level );
  }
  
  public void
  moveDataFiles(
	File	new_parent_dir )
  
  	throws DownloadManagerException
  {
	  this.moveDataFiles(new_parent_dir, null);
  }
  
  public void renameDownload(String new_name) throws DownloadManagerException {
      this.moveDataFiles(null, new_name);
  }
    
  public void 
  moveDataFiles(
	final File 		destination, 
	final String    new_name) 
  
  	throws DownloadManagerException 
  {
	  if (destination == null && new_name == null) {
		  throw new NullPointerException("destination and new name are both null");
	  }
	  
	  if (!canMoveDataFiles()) {
		  throw new DownloadManagerException("canMoveDataFiles is false!");
	  }

	  
	  /**
	   * Test to see if the download is to be moved somewhere where it already
	   * exists. Perhaps you might think it is slightly unnecessary to check this,
	   * but I would prefer this test left in - we want to prevent pausing
	   * unnecessarily pausing a running torrent (it fires off listeners, which
	   * might try to move the download).
	   * 
	   * This helps us avoid a situation with AzCatDest...
	   */
	  SaveLocationChange slc = new SaveLocationChange();
	  slc.download_location = destination;
	  slc.download_name = new_name;
	  
	  File current_location = getSaveLocation();
	  if (slc.normaliseDownloadLocation(current_location).equals(current_location)) {
		  return;
	  }
	  
	  try{
		  FileUtil.runAsTask(
				new AzureusCoreOperationTask()
				{
					public void 
					run(
						AzureusCoreOperation operation) 
					{
						try{
							moveDataFilesSupport( destination, new_name );
							
						}catch( DownloadManagerException e ){
							
							throw( new RuntimeException( e ));
						}
					}
				});
	  }catch( RuntimeException e ){
		  
		  Throwable cause = e.getCause();
		  
		  if ( cause instanceof DownloadManagerException ){
			  
			  throw((DownloadManagerException)cause);
		  }
		  
		  throw( e );
	  }
  }
  
  private void 
  moveDataFilesSupport(
	File new_parent_dir, 
	String new_filename) 
  
  	throws DownloadManagerException 
  	{
	  boolean is_paused = this.pause();
	  try {moveDataFilesSupport0(new_parent_dir, new_filename);}
	  finally {if (is_paused) {this.resume();}}
  	}
  
  private void moveDataFilesSupport0(
			File new_parent_dir, 
			String new_filename) throws DownloadManagerException {
	  if (!canMoveDataFiles()){
		  throw new DownloadManagerException("canMoveDataFiles is false!");
	  }
	  
	  if (new_filename != null) {new_filename = FileUtil.convertOSSpecificChars(new_filename,false);}
	  		  
			// old file will be a "file" for simple torrents, a dir for non-simple

	  File	old_file = getSaveLocation();
		  
	  try{
		  old_file = old_file.getCanonicalFile();
		  if (new_parent_dir != null) {new_parent_dir = new_parent_dir.getCanonicalFile();}
			  
	  }catch( Throwable e ){
		  Debug.printStackTrace(e);
		  throw( new DownloadManagerException( "Failed to get canonical paths", e ));
	  }

	  File current_save_location = old_file;
	  File new_save_location = new File(
			  (new_parent_dir == null) ? old_file.getParentFile() : new_parent_dir,
			  (new_filename == null) ? old_file.getName() : new_filename
	  );
	  
	  if (current_save_location.equals(new_save_location)) {
		  	// null operation
		  return;
	  }

	  DiskManager	dm = getDiskManager();
	  
	  if ( dm == null || dm.getFiles() == null){

		  if ( !old_file.exists()){
				  
		  	// files not created yet
				  
		  	FileUtil.mkdirs(new_save_location.getParentFile());
				  
			  setTorrentSaveDir(new_save_location.getParent().toString(), new_save_location.getName());
			  
			  return;
		  }
			  
		  try{
			  new_save_location	= new_save_location.getCanonicalFile();
			  
		  }catch( Throwable e ){
			  
			  Debug.printStackTrace(e);
		  }
		  
		  if ( old_file.equals( new_save_location )){
			  
			  // nothing to do
			  
		  } else if (torrent.isSimpleTorrent()) {
			  // Have to keep the file name in sync if we're renaming.
			  if (controller.getDiskManagerFileInfo()[0].setLinkAtomic(new_save_location)) {
				  setTorrentSaveDir( new_save_location.getParentFile().toString(), new_save_location.getName());
			  } else {throw new DownloadManagerException( "rename operation failed");}
			  
		  }else{

			  if (FileUtil.isAncestorOf(old_file, new_save_location)) {
		    		
		            Logger.logTextResource(new LogAlert(this, LogAlert.REPEATABLE,
							LogAlert.AT_ERROR, "DiskManager.alert.movefilefails"),
							new String[] {old_file.toString(), "Target is sub-directory of files" });
		            
		            throw( new DownloadManagerException( "rename operation failed" ));
				}
			  
			  // The files we move must be limited to those mentioned in the torrent.
			  final HashSet files_to_move = new HashSet();

              // Required for the adding of parent directories logic.
              files_to_move.add(null);
              DiskManagerFileInfo[] info_files = controller.getDiskManagerFileInfo();
              for (int i=0; i<info_files.length; i++) {
                  File f = info_files[i].getFile(true);
                  try {f = f.getCanonicalFile();}
                  catch (IOException ioe) {f = f.getAbsoluteFile();}
                  boolean added_entry = files_to_move.add(f);

                  /**
                   * Start adding all the parent directories to the
                   * files_to_move list. Doesn't matter if we include
                   * files which are outside of the file path, the
                   * renameFile call won't try to move those directories
                   * anyway.
                   */
                  while (added_entry) {
                      f = f.getParentFile();
                      added_entry = files_to_move.add(f);
                  }
              }
			  FileFilter ff = new FileFilter() {
				  public boolean accept(File f) {return files_to_move.contains(f);}
			  };
			  
			  if ( FileUtil.renameFile( old_file, new_save_location, false, ff )){
		  			  
				  setTorrentSaveDir( new_save_location.getParentFile().toString(), new_save_location.getName());
			  
			  }else{
				  
				  throw( new DownloadManagerException( "rename operation failed" ));
			  }
		  }
	  }else{
		  dm.moveDataFiles( new_save_location.getParentFile(), new_save_location.getName());
	  }
  }
  
  public void moveTorrentFile(File new_parent_dir) throws DownloadManagerException {
	  this.moveTorrentFile(new_parent_dir, null);
  }

  public void moveTorrentFile(File new_parent_dir, String new_name) throws DownloadManagerException {
	  SaveLocationChange slc = new SaveLocationChange();
	  slc.torrent_location = new_parent_dir;
	  slc.torrent_name = new_name;
	  
	  File torrent_file_now = new File(getTorrentFileName());
	  if (!slc.isDifferentTorrentLocation(torrent_file_now)) {return;}

	  boolean is_paused = this.pause();
	  try {moveTorrentFile0(new_parent_dir, new_name);}
	  finally {if (is_paused) {this.resume();}}
  }
  
  
  private void moveTorrentFile0(
	File	new_parent_dir,
	String  new_name)
  
	throws DownloadManagerException
  {
	    
	  if ( !canMoveDataFiles()){
		  
		  throw( new DownloadManagerException( "Cannot move torrent file" ));
	  }
	  
	  setTorrentFile(new_parent_dir, new_name);
  }
  
  public void setTorrentFile(File new_parent_dir, String new_name) throws DownloadManagerException {
		  
	  File	old_file = new File( getTorrentFileName() );
		  
	  if ( !old_file.exists()){
		  Debug.out( "torrent file doesn't exist!" );
		  return;
	  }
	  
	  if (new_parent_dir == null) {new_parent_dir = old_file.getParentFile();}
	  if (new_name == null) {new_name = old_file.getName();}
	  File new_file = new File(new_parent_dir, new_name);
	  
	  try{
		  old_file = old_file.getCanonicalFile();
		  new_file = new_file.getCanonicalFile();
		  
	  }catch( Throwable e ){
		  
		  Debug.printStackTrace(e);
		  
		  throw( new DownloadManagerException( "Failed to get canonical paths", e ));
	  }
	  
	  // Nothing to do.
	  if ( new_file.equals(old_file)) {return;}
	  
	  if (TorrentUtils.move( old_file, new_file )){
		  setTorrentFileName( new_file.toString());
	  			  
	  }else{
		  throw( new DownloadManagerException( "rename operation failed" ));
	  }
  }
  
  public boolean isInDefaultSaveDir() {
	  return DownloadManagerDefaultPaths.isInDefaultDownloadDir(this);
  }
  
  public boolean
  seedPieceRecheck()
  {
	  PEPeerManager pm = controller.getPeerManager();
	  
	  if ( pm != null ){
		  
		  return( pm.seedPieceRecheck());
	  }
	  
	  return( false );
  }
  
  public void
  addRateLimiter(
	  LimitedRateGroup	group,
	  boolean			upload )
  {
	  controller.addRateLimiter( group, upload );
  }

  public void
  removeRateLimiter(
	  LimitedRateGroup	group,
	  boolean				upload )
  {
	  controller.removeRateLimiter( group, upload );  
  }
	
  public boolean
  isTrackerError()
  {
		TRTrackerAnnouncer announcer = getTrackerClient();
		
		if ( announcer != null ){
			
			TRTrackerAnnouncerResponse resp = announcer.getLastResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
					
					return( true );
				}
			}
		}else{
			
			TRTrackerScraperResponse resp = getTrackerScrapeResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerScraperResponse.ST_ERROR ){
					
					return( true );
				}
			}
		}  
		
		return( false );
  }
  
  public boolean
  isUnauthorisedOnTracker()
  {
		TRTrackerAnnouncer announcer = getTrackerClient();

		String	status_str = null;
		
		if ( announcer != null ){
			
			TRTrackerAnnouncerResponse resp = announcer.getLastResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR ){
					
					status_str = resp.getStatusString();
				}
			}
		}else{
			
			TRTrackerScraperResponse resp = getTrackerScrapeResponse();
			
			if ( resp != null ){
				
				if ( resp.getStatus() == TRTrackerScraperResponse.ST_ERROR ){
					
					status_str = resp.getStatusString();
				}
			}
		}
		
		if ( status_str != null ){
			
			status_str = status_str.toLowerCase();
			
			if ( 	status_str.contains( "not authorised" ) ||
					status_str.contains( "not authorized" )){
				
				return( true );
			}
		}
		
		return( false );
  }
  
  	public List<TrackerPeerSource> 
  	getTrackerPeerSources() 
  	{	  
  		try{
  			this_mon.enter();

  			Object[] tps_data = (Object[])getUserData( TPS_Key );

  			List<TrackerPeerSource>	tps;
  			
  			if ( tps_data == null ){

  				tps = new ArrayList<TrackerPeerSource>();

  				TOTorrentListener tol =
  					new TOTorrentListener()
					{
						public void 
						torrentChanged(
							TOTorrent	torrent,
							int 		type ) 
						{
							if ( type == TOTorrentListener.CT_ANNOUNCE_URLS ){

								List<DownloadManagerTPSListener>	to_inform = null;
								
						 		try{
						  			this_mon.enter();

						  			torrent.removeListener( this );
						  			
						  			setUserData( TPS_Key, null );

						  			if ( tps_listeners != null ){
						  				
						  				to_inform = new ArrayList<DownloadManagerTPSListener>( tps_listeners );
						  			}
						  		}finally{

						  			this_mon.exit();
						  		}
						  		
						  		if ( to_inform != null ){
						  			
						  			for ( DownloadManagerTPSListener l: to_inform ){
						  				
						  				try{
						  				
						  					l.trackerPeerSourcesChanged();
						  					
						  				}catch( Throwable e ){
						  					
						  					Debug.out(e);
						  				}
						  			}
						  		}
							}
						}
					};
					
  				setUserData( TPS_Key, new Object[]{ tps, tol });

  					// tracker peer sources
  				
  				TOTorrent t = getTorrent();

  				if ( t != null ){

  					t.addListener( tol );
  					
  					TOTorrentAnnounceURLSet[] sets = t.getAnnounceURLGroup().getAnnounceURLSets();
  					
  					if ( sets.length == 0 ){
  						
  						sets = new TOTorrentAnnounceURLSet[]{ t.getAnnounceURLGroup().createAnnounceURLSet( new URL[]{ torrent.getAnnounceURL()})};
  					}
  					  
  						// source per set
  					
					for ( final TOTorrentAnnounceURLSet set: sets ){
						
						final URL[] urls = set.getAnnounceURLs();
						
						if ( urls.length == 0 || TorrentUtils.isDecentralised( urls[0] )){
							
							continue;
						}
						
						tps.add( 
							new TrackerPeerSource()
							{
								private TrackerPeerSource _delegate;
								
			 					private TRTrackerAnnouncer		ta;
			  					private long					ta_fixup;

			  					private long		last_scrape_time;
			  					private int[]		last_scrape;
			  					
								private TrackerPeerSource
								fixup()
								{
									long	now = SystemTime.getMonotonousTime();
									
									if ( now - ta_fixup > 1000 ){
										
										TRTrackerAnnouncer current_ta = getTrackerClient();
										
										if ( current_ta == ta ){
											
											if ( current_ta != null && _delegate == null ){
												
												_delegate = current_ta.getTrackerPeerSource( set );
											}
										}else{
											
											if ( current_ta == null ){
												
												_delegate = null;
												
											}else{
												
												_delegate = current_ta.getTrackerPeerSource( set );
											}
											
											ta = current_ta;
										}
										
										ta_fixup	= now;
									}
									
									return( _delegate );
								}
								
								protected int[]
								getScrape()
								{
									long now = SystemTime.getMonotonousTime();
									
									if ( now - last_scrape_time > 30*1000 || last_scrape == null ){

										TRTrackerScraper	scraper = globalManager.getTrackerScraper();

										int	max_peers = -1;
										int max_seeds = -1;
										
										for ( URL u: urls ){
										
											TRTrackerScraperResponse resp = scraper.peekScrape(torrent, u );
											
											if ( resp != null ){
												
												if ( !resp.isDHTBackup()){
													
													int peers = resp.getPeers();
													int seeds = resp.getSeeds();
													
													if ( peers > max_peers ){
													
														max_peers = peers;
													}
													
													if ( seeds > max_seeds ){
														
														max_seeds = seeds;
													}
												}
											}
										}
										
										last_scrape = new int[]{ max_seeds, max_peers }; 
										
										last_scrape_time = now;
									}
									
									return( last_scrape );
								}
								
								public int
								getType()
								{
									return( TrackerPeerSource.TP_TRACKER );
								}
								
								public String
								getName()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( urls[0].toExternalForm());
									}
									
									return( delegate.getName());
								}
								
								public int 
								getStatus()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( ST_STOPPED );
									}
									
									return( delegate.getStatus());
								}
								
								public String 
								getStatusString()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( null );
									}
									
									return( delegate.getStatusString());
								}
								
								public int
								getSeedCount()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( getScrape()[0] );
									}
									
									int seeds = delegate.getSeedCount();
									
									if ( seeds < 0 ){
										
										seeds = getScrape()[0];
									}
									
									return( seeds );
								}
								
								public int
								getLeecherCount()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( getScrape()[1] );
									}
									
									int leechers = delegate.getLeecherCount();
									
									if ( leechers < 0 ){
										
										leechers = getScrape()[1];
									}
									
									return( leechers );						
								}
	
								public int
								getPeers()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( -1 );
									}
									
									return( delegate.getPeers());							
								}

								public int
								getInterval()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( -1 );
									}
									
									return( delegate.getInterval());							
								}
								
								public int
								getMinInterval()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( -1 );
									}
									
									return( delegate.getMinInterval());							
								}
								
								public boolean
								isUpdating()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( false );
									}
									
									return( delegate.isUpdating());							
								}
								
								public int
								getSecondsToUpdate()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( -1 );
									}
									
									return( delegate.getSecondsToUpdate());
								}
								
								public boolean
								canManuallyUpdate()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
										
										return( false );
									}
									
									return( delegate.canManuallyUpdate());
								}
								
								public void
								manualUpdate()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate != null ){
										
										delegate.manualUpdate();
									}
								}
							});
					}
					
						// cache peer source
					
					tps.add( 
							new TrackerPeerSourceAdapter()
							{
								private TrackerPeerSource _delegate;
								
			 					private TRTrackerAnnouncer		ta;
			 					private boolean					enabled;
			  					private long					ta_fixup;

								private TrackerPeerSource
								fixup()
								{
									long	now = SystemTime.getMonotonousTime();
									
									if ( now - ta_fixup > 1000 ){
										
										TRTrackerAnnouncer current_ta = getTrackerClient();
										
										if ( current_ta == ta ){
											
											if ( current_ta != null && _delegate == null ){
												
												_delegate = current_ta.getCacheTrackerPeerSource();
											}
										}else{
											
											if ( current_ta == null ){
												
												_delegate = null;
												
											}else{
												
												_delegate = current_ta.getCacheTrackerPeerSource();
											}
											
											ta = current_ta;
										}
										
										enabled = controller.isPeerSourceEnabled( PEPeerSource.PS_BT_TRACKER );
										
										ta_fixup	= now;
									}
									
									return( _delegate );
								}
								
								public int
								getType()
								{
									return( TrackerPeerSource.TP_TRACKER );
								}
								
								public String
								getName()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null ){
									
										return( MessageText.getString( "tps.tracker.cache" ));
									}
									
									return( delegate.getName());
								}
								
								public int 
								getStatus()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( !enabled ){
										
										return( ST_DISABLED );
									}
									
									if ( delegate == null ){
									
										return( ST_STOPPED );
									}
									
									return( ST_ONLINE );
								}
	
								public int
								getPeers()
								{
									TrackerPeerSource delegate = fixup();
									
									if ( delegate == null || !enabled ){
									
										return( -1 );
									}
									
									return( delegate.getPeers());							
								}
							});
  				}
  			
	  				// http seeds
	  			
	  			try{
	  				ExternalSeedPlugin esp = DownloadManagerController.getExternalSeedPlugin();
	  				
	  				if ( esp != null ){
	  					  					
						tps.add( esp.getTrackerPeerSource( PluginCoreUtils.wrap( this ) ));
	 				}
	  			}catch( Throwable e ){
	  			}
	  			
	  				// dht
	  			
	  			try{
	  				
					PluginInterface dht_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(DHTTrackerPlugin.class);
	
	  			    if ( dht_pi != null ){
	  			    	
	  			    	tps.add(((DHTTrackerPlugin)dht_pi.getPlugin()).getTrackerPeerSource( PluginCoreUtils.wrap( this )));
	  			    }
				}catch( Throwable e ){
	  			}
	  			
					// LAN
				
				try{
					
					PluginInterface lt_pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByClass(LocalTrackerPlugin.class);
	
					if ( lt_pi != null ){
						
	  			    	tps.add(((LocalTrackerPlugin)lt_pi.getPlugin()).getTrackerPeerSource( PluginCoreUtils.wrap( this )));
					
					}
					
				}catch( Throwable e ){
					
				}
				
					// Plugin
				
				try{
				
					tps.add(((DownloadImpl)PluginCoreUtils.wrap( this )).getTrackerPeerSource());
					
				}catch( Throwable e ){
					
				}
				
					// PEX...
				
				tps.add(
					new TrackerPeerSourceAdapter()
					{
						private PEPeerManager		_pm;
						private TrackerPeerSource	_delegate;
						
						private TrackerPeerSource 
						fixup()
						{
							PEPeerManager pm = getPeerManager();
							
							if ( pm == null ){
								
								_delegate 	= null;
								_pm			= null;
								
							}else if ( pm != _pm ){
								
								_pm	= pm;
								
								_delegate = pm.getTrackerPeerSource();
							}
							
							return( _delegate );
						}
						
						public int
						getType()
						{
							return( TP_PEX );
						}
						
						public int
						getStatus()
						{
							TrackerPeerSource delegate = fixup();
							
							if ( delegate == null ){
								
								return( ST_STOPPED );
								
							}else{
								
								return( delegate.getStatus());
							}
						}
						
						public String 
						getName() 
						{
							TrackerPeerSource delegate = fixup();
							
							if ( delegate == null ){
								
								return( "" );
								
							}else{
								
								return( delegate.getName());
							}	
						}
						
						public int 
						getPeers() 
						{
							TrackerPeerSource delegate = fixup();
							
							if ( delegate == null ){
								
								return( -1 );
								
							}else{
								
								return( delegate.getPeers());
							}	
						}
					});
				
					// incoming
				
				tps.add(
						new TrackerPeerSourceAdapter()
						{
							private long				fixup_time;
						
							private PEPeerManager		_pm;
							private int					tcp;
							private int					udp;
							private int					utp;
							private int					total;
							private boolean				enabled;
							
							private PEPeerManager 
							fixup()
							{
								long	now = SystemTime.getMonotonousTime();
								
								if ( now - fixup_time > 1000 ){
	
									PEPeerManager pm = _pm = getPeerManager();
									
									if ( pm != null ){
										
										tcp 	= pm.getNbRemoteTCPConnections();
										udp		= pm.getNbRemoteUDPConnections();
										utp		= pm.getNbRemoteUTPConnections();
										total	= pm.getStats().getTotalIncomingConnections();
									}
									
									enabled = controller.isPeerSourceEnabled( PEPeerSource.PS_INCOMING );
									
									fixup_time = now;
								}
								
								return( _pm );
							}
							
							public int
							getType()
							{
								return( TP_INCOMING );
							}
							
							public int
							getStatus()
							{
								PEPeerManager delegate = fixup();
								
								if ( delegate == null ){
									
									return( ST_STOPPED );
									
								}else if ( !enabled ){
									
									return( ST_DISABLED );
										
								}else{
								
									return( ST_ONLINE );
								}
							}
							
							public String 
							getName() 
							{
								PEPeerManager delegate = fixup();
								
								if ( delegate == null || !enabled ){
									
									return( "" );
									
								}else{
									
									return( 
										MessageText.getString( 
											"tps.incoming.details",
											new String[]{ String.valueOf( tcp ), String.valueOf( udp + utp ), String.valueOf( total )} ));
								}	
							}
							
							public int 
							getPeers() 
							{
								PEPeerManager delegate = fixup();
								
								if ( delegate == null || !enabled ){
									
									return( -1 );
									
								}else{
									
									return( tcp + udp );
								}	
							}
						});
				
 			}else{
  				
  				tps = (List<TrackerPeerSource>)tps_data[0];
  			}
 
  			return( tps );

  		}finally{

  			this_mon.exit();
  		}
  	}

    public void
    addTPSListener(
    	DownloadManagerTPSListener		listener )
    {
    	try{
    		this_mon.enter();
    		
    		if ( tps_listeners == null ){
    			
    			tps_listeners = new ArrayList<DownloadManagerTPSListener>(1);
    		}
    		
    		tps_listeners.add( listener );
    		
    	}finally{
    		
    		this_mon.exit();
    	}
    }	
    
    public void
    removeTPSListener(
    	DownloadManagerTPSListener		listener )
    {
       	try{
    		this_mon.enter();
    		
    		if ( tps_listeners != null ){
    		 		
    			tps_listeners.remove( listener );
    			
    			if ( tps_listeners.size() == 0 ){
    				
    				tps_listeners = null;
    				
    	  			Object[] tps_data = (Object[])getUserData( TPS_Key );

    	  			if ( tps_data != null ){
		  			
    	 				TOTorrent t = getTorrent();

    	 				if ( t != null ){
    	 					
    	 					t.removeListener( (TOTorrentListener)tps_data[1] );
    	 				}
    	 				
    	  				setUserData( TPS_Key, null );
    	  			}
    			}
    		}
    	}finally{
    		
    		this_mon.exit();
    	}
    }
    
  private byte[]
  getIdentity()
  {
 	  return( dl_identity );
  }
   
   /** @retun true, if the other DownloadManager has the same hash 
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public boolean equals(Object obj)
   {
 		// check for object equivalence first!
   		
 	if ( this == obj ){
   		
 		return( true );
 	}
   	
 	if( obj instanceof DownloadManagerImpl ) {
     	
 	  DownloadManagerImpl other = (DownloadManagerImpl) obj;
           
 	  byte[] id1 = getIdentity();
 	  byte[] id2 = other.getIdentity();
       
 	  if ( id1 == null || id2 == null ){
       	
 		return( false );	// broken torrents - treat as different so shown
 							// as broken
 	  }
       
 	  return( Arrays.equals( id1, id2 ));
 	}
     
 	return false;
   }
   
   
   public int 
   hashCode() 
   {  
	   return dl_identity_hashcode;  
   }


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getLogRelationText()
	 */
	public String getRelationText() {
		return "TorrentDLM: '" + getDisplayName() + "'";
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#queryForClass(java.lang.Class)
	 */
	public Object[] getQueryableInterfaces() {
		return new Object[] { tracker_client };
	}
	
	public String toString() {
		String hash = "<unknown>";

		try {
			hash = ByteFormatter.encodeString(torrent.getHash());

		} catch (Throwable e) {
		}

		String status = DisplayFormatters.formatDownloadStatus(this);
		if (status.length() > 10) {
			status = status.substring(0, 10);
		}
		return "DownloadManagerImpl#" + getPosition()
				+ (getAssumedComplete() ? "s" : "d") + "@"
				+ Integer.toHexString(hashCode()) + "/"
				+ status + "/"
				+ hash;
	}
	
	protected static class
	NoStackException
		extends Exception
	{
		protected
		NoStackException(
			String	str )
		{
			super( str );
		}
	}
	
	public void
	generateEvidence(
		IndentWriter		writer )
	{
		writer.println(toString());

		PEPeerManager pm = getPeerManager();

		try {
			writer.indent();

			writer.println("Save Dir: "
					+ Debug.secretFileName(getSaveLocation().toString()));
			
			if (current_peers.size() > 0) {
				writer.println("# Peers: " + current_peers.size());
			}
			
			if (current_pieces.size() > 0) {
				writer.println("# Pieces: " + current_pieces.size());
			}
			
			writer.println("Listeners: DownloadManager=" + listeners.size() + "; Disk="
				+ controller.getDiskListenerCount() + "; Peer=" + peer_listeners.size()
				+ "; Tracker=" + tracker_listeners.size());
			
			writer.println("SR: " + iSeedingRank);
			
			
			String sFlags = "";
			if (open_for_seeding) {
				sFlags += "Opened for Seeding; ";
			}
			
			if (data_already_allocated) {
				sFlags += "Data Already Allocated; ";
			}
			
			if (assumedComplete) {
				sFlags += "onlySeeding; ";
			}
			
			if (persistent) {
				sFlags += "persistent; ";
			}
			
			if (sFlags.length() > 0) {
				writer.println("Flags: " + sFlags);
			}

			stats.generateEvidence( writer );
			
			download_manager_state.generateEvidence( writer );

			if (pm != null) {
				pm.generateEvidence(writer);
			}
			
				// note, PeerManager generates DiskManager evidence
			
			controller.generateEvidence(writer);

			TRTrackerAnnouncer announcer = tracker_client;
			
			if ( announcer != null ){
				
				announcer.generateEvidence( writer );
			}
			
			TRTrackerScraperResponse scrape = getTrackerScrapeResponse();
			
			if ( scrape == null ){
				
				writer.println( "Scrape: null" );
			}else{
				
				writer.println( "Scrape: " + scrape.getString());
			}
		} finally {

			writer.exdent();
		}
	}

	public void
	destroy(
		boolean	is_duplicate )
	{
		destroyed	= true;
		
		if ( is_duplicate ){
	
				// minimal tear-down
			
			controller.destroy();
			
		}else{
		
			try{
		   	// Data files don't exist, so we just don't do anything.
		    	if (!getSaveLocation().exists()) {return;}
		    	
		    	DiskManager dm = this.getDiskManager();
		    	if (dm != null) {
		    		dm.downloadRemoved();
		    		return;
		    	}
		    	    	
		    	SaveLocationChange move_details;
		    	move_details = DownloadManagerMoveHandler.onRemoval(this);
		    	if (move_details == null) {
		    		return;
		    	}
		    	
		    	boolean can_move_torrent = move_details.hasTorrentChange();
		    	
		    	try {
		    		if (move_details.hasDownloadChange()) {
		    			this.moveDataFiles(move_details.download_location, move_details.download_name);
		    		}
		    	}
		    	catch (Exception e) {
		    		can_move_torrent = false;
		    		Logger.log(new LogAlert(this, true,
							"Problem moving files to removed download directory", e));
		    	}
		    	
		    	// This code will silently fail if the torrent file doesn't exist.
		    	if (can_move_torrent) {
		  		    try {
			    		this.moveTorrentFile(move_details.torrent_location, move_details.torrent_name);
			    	}
			    	catch (Exception e) {
			    		Logger.log(new LogAlert(this, true, 
			    				"Problem moving torrent to removed download directory", e));
			    	}
		    	}
			}finally{
				
				clearFileLinks();
				
				controller.destroy(); 
			}
		}
	}
	
	public boolean 
	isDestroyed() 
	{
		return( destroyed );
	}
	
	public int[] getStorageType(DiskManagerFileInfo[] info) {
		String[] types = DiskManagerImpl.getStorageTypes(this);
		int[] result = new int[info.length];
		for (int i=0; i<info.length; i++) {
			result[i] = DiskManagerUtil.convertDMStorageTypeFromString( types[info[i].getIndex()]);
		}
		return result;
	}
	
	public boolean canMoveDataFiles() {
		if (!isPersistent()) {return false;}
		return true;
	}
	
	public void rename(String name) throws DownloadManagerException {
		boolean paused = this.pause();
		try {
			this.renameDownload(name);
			this.getDownloadState().setAttribute(DownloadManagerState.AT_DISPLAY_NAME, name);
			this.renameTorrentSafe(name);
		}
		finally {
			if (paused) {this.resume();}
		}
	}
	
	public void renameTorrent(String name) throws DownloadManagerException {
		this.moveTorrentFile(null, name);
	}

	public void renameTorrentSafe(String name) throws DownloadManagerException {
		String torrent_parent = new File(this.getTorrentFileName()).getParent();
		String torrent_name = name;
		
		File new_path = new File(torrent_parent, torrent_name + ".torrent");
		if (new_path.exists()) {new_path = null;}
			
		for (int i=1; i<10; i++) {
			if (new_path != null) {break;}
			new_path = new File(torrent_parent, torrent_name + "(" + i + ").torrent");
			if (new_path.exists()) {new_path = null;}
		}
		
		if (new_path == null) {
			throw new DownloadManagerException("cannot rename torrent file - file already exists");
		}
		
		this.renameTorrent(new_path.getName());
	}

}
