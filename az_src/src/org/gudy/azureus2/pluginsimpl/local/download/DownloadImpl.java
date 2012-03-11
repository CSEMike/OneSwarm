/*
 * File    : DownloadImpl.java
 * Created : 06-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.download;

/**
 * @author parg
 *
 */

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.impl.DownloadManagerMoveHandler;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationChange;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.pluginsimpl.local.deprecate.PluginDeprecation;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;

import com.aelitis.azureus.core.tracker.TrackerPeerSource;
import com.aelitis.azureus.core.tracker.TrackerPeerSourceAdapter;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.core.util.CopyOnWriteMap;

public class 
DownloadImpl
	extends LogRelation
	implements 	Download, DownloadManagerListener, 
				DownloadManagerTrackerListener,
				DownloadManagerStateListener, DownloadManagerActivationListener,
				DownloadManagerStateAttributeListener
{
	private DownloadManager		download_manager;
	private DownloadStatsImpl		download_stats;
	
	private int		latest_state		= ST_STOPPED;
	private boolean 	latest_forcedStart;
	
	private DownloadAnnounceResultImpl	last_announce_result 	= new DownloadAnnounceResultImpl(this,null);
	private DownloadScrapeResultImpl		last_scrape_result		= new DownloadScrapeResultImpl( this, null );
    private TorrentImpl torrent = null;
	
	private List		listeners 				= new ArrayList();
	private AEMonitor	listeners_mon			= new AEMonitor( "Download:L");
	private List		property_listeners		= new ArrayList();
	private List		tracker_listeners		= new ArrayList();
	private AEMonitor	tracker_listeners_mon	= new AEMonitor( "Download:TL");
	private List		removal_listeners 		= new ArrayList();
	private AEMonitor	removal_listeners_mon	= new AEMonitor( "Download:RL");
	private Map			peer_listeners			= new HashMap();
	private AEMonitor	peer_listeners_mon		= new AEMonitor( "Download:PL");
	
	private CopyOnWriteList completion_listeners     = new CopyOnWriteList();
	
	private CopyOnWriteMap read_attribute_listeners_map_cow  = new CopyOnWriteMap();
	private CopyOnWriteMap write_attribute_listeners_map_cow = new CopyOnWriteMap();
	
	private CopyOnWriteList	activation_listeners = new CopyOnWriteList();
	private DownloadActivationEvent	activation_state;
	
	private HashMap property_to_attribute_map = null;
	
	private Map<String,int[]>	announce_response_map;
	
	protected
	DownloadImpl(
		DownloadManager		_dm )
	{
		download_manager	= _dm;
		download_stats		= new DownloadStatsImpl( download_manager );
		
		activation_state = 
			new DownloadActivationEvent()
			{
				public Download
				getDownload()
				{
					return( DownloadImpl.this );
				}
				
				public int
				getActivationCount()
				{
					return( download_manager.getActivationCount());
				}
			};
			
		download_manager.addListener( this );
		
		latest_forcedStart = download_manager.isForceStart();
	}
	
	// Not available to plugins
	public DownloadManager
	getDownload()
	{
		return( download_manager );
	}

	public int
	getState()
	{
		return( convertState( download_manager.getState()) );
	}
	
	public int
	getSubState()
	{
		int	state = getState();
		
		if ( state == ST_STOPPING ){
			
			int	substate = download_manager.getSubState();
			
			if ( substate == DownloadManager.STATE_QUEUED ){
				
				return( ST_QUEUED );
				
			}else if ( substate == DownloadManager.STATE_STOPPED ){
				
				return( ST_STOPPED );
				
			}else if ( substate == DownloadManager.STATE_ERROR ){
				
				return( ST_ERROR );
			}
		}
		
		return( state );
	}
	
	protected int
	convertState(
		int		dm_state )
	{	
		// dm states: waiting -> initialising -> initialized -> 
		//		disk states: allocating -> checking -> ready ->
		// dm states: downloading -> finishing -> seeding -> stopping -> stopped
		
		// "initialize" call takes from waiting -> initialising -> waiting (no port) or initialized (ok)
		// if initialized then disk manager runs through to ready
		// "startdownload" takes ready -> dl etc.
		// "stopIt" takes to stopped which is equiv to ready
		
		int	our_state;
		
		switch( dm_state ){
			case DownloadManager.STATE_WAITING:
			{
				our_state	= ST_WAITING;
				
				break;
			}		
			case DownloadManager.STATE_INITIALIZING:
			case DownloadManager.STATE_INITIALIZED:
			case DownloadManager.STATE_ALLOCATING:
			case DownloadManager.STATE_CHECKING:
			{
				our_state	= ST_PREPARING;
					
				break;
			}
			case DownloadManager.STATE_READY:
			{
				our_state	= ST_READY;
					
				break;
			}
			case DownloadManager.STATE_DOWNLOADING:
			case DownloadManager.STATE_FINISHING:		// finishing download - transit to seeding
			{
				our_state	= ST_DOWNLOADING;
					
				break;
			}
			case DownloadManager.STATE_SEEDING:
			{
				our_state	= ST_SEEDING;
				
				break;
			}
			case DownloadManager.STATE_STOPPING:
			{
				our_state	= ST_STOPPING;
				
				break;
			}
			case DownloadManager.STATE_STOPPED:
			{
				our_state	= ST_STOPPED;
					
				break;
			}
			case DownloadManager.STATE_QUEUED:
			{
				our_state	= ST_QUEUED;
					
				break;
			}
			case DownloadManager.STATE_ERROR:
			{
				our_state	= ST_ERROR;
				
				break;
			}
			default:
			{
				our_state	= ST_ERROR;
			}
		}
		
		return( our_state );
	}
	
	public String
	getErrorStateDetails()
	{
		return( download_manager.getErrorDetails());
	}
	
	public long
	getFlags()
	{
		return( download_manager.getDownloadState().getFlags());
	}
	
	public boolean
	getFlag(
		long		flag )
	{
		return( download_manager.getDownloadState().getFlag( flag ));
	}
	
	public void setFlag(long flag, boolean set) {
		download_manager.getDownloadState().setFlag(flag, set);
	}
	
	public int
	getIndex()
	{
		GlobalManager globalManager = download_manager.getGlobalManager();
		return globalManager.getIndexOf(download_manager);
	}
	
    public Torrent
    getTorrent()
    {
    	if (this.torrent != null) {return this.torrent;}
    	
        TOTorrent torrent = download_manager.getTorrent();
        if (torrent == null) {return null;}
        this.torrent = new TorrentImpl(torrent);
        return this.torrent;
    }
    
	public void
	initialize()
	
		throws DownloadException
	{
		int	state = download_manager.getState();
		
		if ( state == DownloadManager.STATE_WAITING ){
			
			download_manager.initialize();
			
		}else{
			
			throw( new DownloadException( "Download::initialize: download not waiting (state=" + state + ")" ));
		}
	}
	
	public void
	start()
	
		throws DownloadException
	{
		int	state = download_manager.getState();
		
		if ( state == DownloadManager.STATE_READY ){
						
			download_manager.startDownload();
										
		}else{
			
			throw( new DownloadException( "Download::start: download not ready (state=" + state + ")" ));
		}
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		int	state = download_manager.getState();
		
		if ( 	state == DownloadManager.STATE_STOPPED ||
				state == DownloadManager.STATE_QUEUED ){
			
			download_manager.setStateWaiting();
			
		}else{
			
			throw( new DownloadException( "Download::restart: download already running (state=" + state + ")" ));
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_STOPPED){
			
			download_manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
			
		}else{
			
			throw( new DownloadException( "Download::stop: download already stopped" ));
		}
	}
	
	public void
	stopAndQueue()
	
		throws DownloadException
	{
		if ( download_manager.getState() != DownloadManager.STATE_QUEUED){
						
			download_manager.stopIt( DownloadManager.STATE_QUEUED, false, false );
			
		}else{
			
			throw( new DownloadException( "Download::stopAndQueue: download already queued" ));
		}
	}
	
	public void
	recheckData()
	
		throws DownloadException
	{
		if ( !download_manager.canForceRecheck()){
			
			throw( new DownloadException( "Download::recheckData: download must be stopped, queued or in error state" ));
		}
		
		download_manager.forceRecheck();
	}
	
	public boolean
	isStartStopLocked()
	{
		return( download_manager.getState() == DownloadManager.STATE_STOPPED );
	}
	
	public boolean
	isForceStart()
	{
		return download_manager.isForceStart();
	}
	
	public void
	setForceStart(boolean forceStart)
	{
		download_manager.setForceStart(forceStart);
	}
	
	public boolean
	isPaused()
	{
		return( download_manager.isPaused());
	}
	
	public void
	pause()
	{
		download_manager.pause();
	}
	
	public void
	resume()
	{
		download_manager.resume();
	}
	
	public int
	getPosition()
	{
		return download_manager.getPosition();
	}
	
	public long
	getCreationTime()
	{
		return( download_manager.getCreationTime());
	}
	
	public void
	setPosition(int newPosition)
	{
		download_manager.setPosition(newPosition);
	}
	
	public void
	moveUp()
	{
		download_manager.getGlobalManager().moveUp(download_manager);
	}
	
	public void
	moveDown()
	{
		download_manager.getGlobalManager().moveDown(download_manager);
	}
	
	public void
	moveTo(
		int	pos )
	{
		download_manager.getGlobalManager().moveTo( download_manager, pos );
	}
	
	public String 
	getName()
	{
		return download_manager.getDisplayName();
	}

  public String getTorrentFileName() {
    return download_manager.getTorrentFileName();
  }
  
  public String getCategoryName() {
    Category category = download_manager.getDownloadState().getCategory();
    if (category == null)
      category = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);

    if (category == null)
      return null;
    return category.getName();
  }
    
  
  public String
  getAttribute(
  	TorrentAttribute		attribute )
  {
  	String	name = convertAttribute( attribute );
  	
  	if ( name != null ){
  		
  		return( download_manager.getDownloadState().getAttribute( name ));
  	}
  	
  	return( null );
  }
  
  public String[]
  getListAttribute(
  	TorrentAttribute		attribute )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
	  		
	  		return( download_manager.getDownloadState().getListAttribute( name ));
	  	}
	  	
	  	return( null );
  }
  
  public void 
  setListAttribute(
	TorrentAttribute attribute, 
	String[] value) 
  {
	  String name = convertAttribute(attribute);
	  
	  if (name != null) {
		  download_manager.getDownloadState().setListAttribute(name, value);
	  }
  }
  
  public void
  setMapAttribute(
	TorrentAttribute		attribute,
	Map						value )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
			
	  			// gotta clone before updating in case user has read values and then just
	  			// updated them - setter code optimises out sets of the same values...
	  		
			download_manager.getDownloadState().setMapAttribute( name, BEncoder.cloneMap( value ));
	  	}
  }
  
  public Map
  getMapAttribute(
	TorrentAttribute		attribute )
  {
	  	String	name = convertAttribute( attribute );
	  	
	  	if ( name != null ){
	  		
	  		return( download_manager.getDownloadState().getMapAttribute( name ));
	  	}
	  	
	  	return( null );
  }
  
  public void
  setAttribute(
  	TorrentAttribute		attribute,
	String					value )
  {
 	String	name = convertAttribute( attribute );
  	
  	if ( name != null ){

  		download_manager.getDownloadState().setAttribute( name, value );
  	}
  }
  
  public boolean hasAttribute(TorrentAttribute attribute) {
	  String name = convertAttribute(attribute);
	  if (name == null) {return false;}
	  return download_manager.getDownloadState().hasAttribute(name);
  }
  
  public boolean getBooleanAttribute(TorrentAttribute attribute) {
	  String name = convertAttribute(attribute);
	  if (name == null) {return false;} // Default value
	  return download_manager.getDownloadState().getBooleanAttribute(name);
  }
  
  public void setBooleanAttribute(TorrentAttribute attribute, boolean value) {
	  String name = convertAttribute(attribute);
	  if (name != null) {
		  download_manager.getDownloadState().setBooleanAttribute(name, value);
	  }
  }

  public int getIntAttribute(TorrentAttribute attribute) {
	  String name = convertAttribute(attribute);
	  if (name == null) {return 0;} // Default value
	  return download_manager.getDownloadState().getIntAttribute(name);
  }
  
  public void setIntAttribute(TorrentAttribute attribute, int value) {
	  String name = convertAttribute(attribute);
	  if (name != null) {
		  download_manager.getDownloadState().setIntAttribute(name, value);
	  }
  }
  
  public long getLongAttribute(TorrentAttribute attribute) {
	  String name = convertAttribute(attribute);
	  if (name == null) {return 0L;} // Default value
	  return download_manager.getDownloadState().getLongAttribute(name);
  }
  
  public void setLongAttribute(TorrentAttribute attribute, long value) {
	  String name = convertAttribute(attribute);
	  if (name != null) {
		  download_manager.getDownloadState().setLongAttribute(name, value);
	  }
  }
  
  protected String
  convertAttribute(
  	TorrentAttribute		attribute )
  {
 	if ( attribute.getName() == TorrentAttribute.TA_CATEGORY ){
  		
  		return( DownloadManagerState.AT_CATEGORY );
  		
 	}else if ( attribute.getName() == TorrentAttribute.TA_NETWORKS ){
  		
		return( DownloadManagerState.AT_NETWORKS );
		
 	}else if ( attribute.getName() == TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS ){
  		
		return( DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS );
	  		
	}else if ( attribute.getName() == TorrentAttribute.TA_PEER_SOURCES ){
  		
		return( DownloadManagerState.AT_PEER_SOURCES );

	}else if ( attribute.getName() == TorrentAttribute.TA_DISPLAY_NAME ){
		
		return( DownloadManagerState.AT_DISPLAY_NAME );
		
	}else if ( attribute.getName() == TorrentAttribute.TA_USER_COMMENT ){
		
		return( DownloadManagerState.AT_USER_COMMENT );	

	}else if ( attribute.getName() == TorrentAttribute.TA_RELATIVE_SAVE_PATH ){
		
		return( DownloadManagerState.AT_RELATIVE_SAVE_PATH );	
		
	}else if ( attribute.getName() == TorrentAttribute.TA_SHARE_PROPERTIES ){
  		
			// this is a share-level attribute only, not propagated to individual downloads
		
		return( null );
  		
	}else if ( attribute.getName().startsWith( "Plugin." )){
  		
		return( attribute.getName());
  		
  	}else{
  		
  		Debug.out( "Can't convert attribute '" + attribute.getName() + "'" );
  		
  		return( null );
  	}
  }
  
  protected TorrentAttribute
  convertAttribute(
  	String			name )
  {
 	if ( name.equals( DownloadManagerState.AT_CATEGORY )){
  		
  		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY ));
  		
	}else if ( name.equals( DownloadManagerState.AT_NETWORKS )){
	  		
	  	return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_NETWORKS ));
	  		
	}else if ( name.equals( DownloadManagerState.AT_PEER_SOURCES )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_PEER_SOURCES ));
		
	}else if ( name.equals( DownloadManagerState.AT_TRACKER_CLIENT_EXTENSIONS )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_TRACKER_CLIENT_EXTENSIONS ));
	
	}else if ( name.equals ( DownloadManagerState.AT_DISPLAY_NAME)){
		
		return ( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_DISPLAY_NAME ));
		
	}else if ( name.equals ( DownloadManagerState.AT_USER_COMMENT)){
		
		return ( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_USER_COMMENT ));

	}else if ( name.equals ( DownloadManagerState.AT_RELATIVE_SAVE_PATH)){
		
		return ( TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_RELATIVE_SAVE_PATH ));
		
	}else if ( name.startsWith( "Plugin." )){
  		
		return( TorrentManagerImpl.getSingleton().getAttribute( name ));
	  		
  	}else{
  		
  		return( null );
  	}
  }
  
  public void setCategory(String sName) {
    Category category = CategoryManager.getCategory(sName);
    if (category == null)
      category = CategoryManager.createCategory(sName);
    download_manager.getDownloadState().setCategory(category);
  }

  public boolean isPersistent() {
    return download_manager.isPersistent();
  }

	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		remove( false, false );
	}
	
	public void
	remove(
		boolean	delete_torrent,
		boolean	delete_data )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		int	dl_state = download_manager.getState();
		
		if ( 	dl_state == DownloadManager.STATE_STOPPED 	|| 
				dl_state == DownloadManager.STATE_ERROR 	||
				dl_state == DownloadManager.STATE_QUEUED ){
			
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				
				globalManager.removeDownloadManager(download_manager, delete_torrent, delete_data);
				
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				
				throw( new DownloadRemovalVetoException( e.getMessage()));
			}
			
		}else{
			
			throw( new DownloadRemovalVetoException( MessageText.getString("plugin.download.remove.veto.notstopped")));
		}
	}
	
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException
	{
		int	dl_state = download_manager.getState();
		
		if ( 	dl_state == DownloadManager.STATE_STOPPED 	|| 
				dl_state == DownloadManager.STATE_ERROR 	||
				dl_state == DownloadManager.STATE_QUEUED ){
						
			GlobalManager globalManager = download_manager.getGlobalManager();
			
			try{
				globalManager.canDownloadManagerBeRemoved(download_manager, false, false);
				
			}catch( GlobalManagerDownloadRemovalVetoException e ){
				
				throw( new DownloadRemovalVetoException( e.getMessage(),e.isSilent()));
			}
			
		}else{
			
			throw( new DownloadRemovalVetoException( MessageText.getString("plugin.download.remove.veto.notstopped")));
		}
		
		return( true );
	}
	
	public DownloadStats
	getStats()
	{
		return( download_stats );
	}
	
 	public boolean
	isComplete()
 	{
 		return download_manager.isDownloadComplete(false);
 	}

 	public boolean isComplete(boolean bIncludeDND) {
 		return download_manager.isDownloadComplete(bIncludeDND);
 	}

 	public boolean
 	isChecking()
 	{
 		return( download_stats.getCheckingDoneInThousandNotation() != -1 );
 	}

	protected void
	isRemovable()
		throws DownloadRemovalVetoException
	{
			// no sync required, see update code
		
		for (int i=0;i<removal_listeners.size();i++){
			
			try{
				((DownloadWillBeRemovedListener)removal_listeners.get(i)).downloadWillBeRemoved(this);
				
			}catch( DownloadRemovalVetoException e ){
				
				throw( e );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	destroy()
	{
		download_manager.removeListener( this );
	}
	

	// DownloadManagerListener methods
	
	public void
	stateChanged(
		DownloadManager manager,
		int				state )
	{
		int	prev_state 	= latest_state;
		
		latest_state	= convertState(state);
		
		// System.out.println("Plug: dl = " + getName() + ", prev = " + prev_state + ", curr = " + latest_state + ", signalled state = " + state);
		
		boolean curr_forcedStart = isForceStart();
		
		// Copy reference in case any attempts to remove or add listeners are tried.
		List listeners_to_use = listeners;
		
		if ( prev_state != latest_state || latest_forcedStart != curr_forcedStart ){
			
			latest_forcedStart = curr_forcedStart;
			
			for (int i=0;i<listeners_to_use.size();i++){
				
				try{
					long startTime = SystemTime.getCurrentTime();
					DownloadListener listener = (DownloadListener)listeners_to_use.get(i);

					listener.stateChanged( this, prev_state, latest_state );
					
					long diff = SystemTime.getCurrentTime() - startTime;
					if (diff > 1000) {
						System.out.println("Plugin should move long processes (" + diff
								+ "ms) off of Download's stateChanged listener trigger. "
								+ listener);
					}
				
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
	}
	
	public void
	downloadComplete(DownloadManager manager)
	{	
		if (this.completion_listeners.isEmpty()) {return;}
		Iterator itr = this.completion_listeners.iterator();
		DownloadCompletionListener dcl;
		while (itr.hasNext()) {
			dcl = (DownloadCompletionListener)itr.next();
			long startTime = SystemTime.getCurrentTime();
			try {dcl.onCompletion(this);}
			catch (Throwable t) {Debug.printStackTrace(t);}
			long diff = SystemTime.getCurrentTime() - startTime;
			if (diff > 1000) {
				System.out.println("Plugin should move long processes (" + diff + "ms) off of Download's onCompletion listener trigger. " + dcl);
			}
		}
	}

	public void 
	completionChanged(
		DownloadManager 	manager, 
		boolean 			bCompleted) 
	{
	}

	public void
	filePriorityChanged( DownloadManager download, org.gudy.azureus2.core3.disk.DiskManagerFileInfo file )
	{	  
	}
	  
  public void 
  positionChanged(
  	DownloadManager download, 
    int oldPosition, 
	int newPosition) 
  {	
	for (int i = 0; i < listeners.size(); i++) {
		try {
			long startTime = SystemTime.getCurrentTime();
			DownloadListener listener = (DownloadListener)listeners.get(i);

			listener.positionChanged(this, oldPosition, newPosition);

			long diff = SystemTime.getCurrentTime() - startTime;
			if (diff > 1000) {
				System.out.println("Plugin should move long processes (" + diff
						+ "ms) off of Download's positionChanged listener trigger. "
						+ listener);
			}
		} catch (Throwable e) {
			Debug.printStackTrace( e );
		}
	}
  }

	public void
	addListener(
		DownloadListener	l )
	{
		try{
			listeners_mon.enter();
			
			List	new_listeners = new ArrayList( listeners );
			
			new_listeners.add(l);
			
			listeners	= new_listeners;
		}finally{
			
			listeners_mon.exit();
		}
	}
	

	public void
	removeListener(
		DownloadListener	l )
	{
		try{
			listeners_mon.enter();
			
			List	new_listeners	= new ArrayList(listeners);
			
			new_listeners.remove(l);
			
			listeners	= new_listeners;
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void addAttributeListener(DownloadAttributeListener listener, TorrentAttribute attr, int event_type) {
		String attribute = convertAttribute(attr);
		if (attribute == null) {return;}
		
		CopyOnWriteMap attr_map = this.getAttributeMapForType(event_type);
		CopyOnWriteList listener_list = (CopyOnWriteList)attr_map.get(attribute);
		boolean add_self = false;
		
		if (listener_list == null) {
			listener_list = new CopyOnWriteList();
			attr_map.put(attribute, listener_list);
		}
		add_self = listener_list.isEmpty();
		
		listener_list.add(listener);
		if (add_self) {
			download_manager.getDownloadState().addListener(this, attribute, event_type);
		}
	}

	public void removeAttributeListener(DownloadAttributeListener listener, TorrentAttribute attr, int event_type) {
		String attribute = convertAttribute(attr);
		if (attribute == null) {return;}
		
		CopyOnWriteMap attr_map = this.getAttributeMapForType(event_type);
		CopyOnWriteList listener_list = (CopyOnWriteList)attr_map.get(attribute);
		boolean remove_self = false;
			
		if (listener_list != null) {
			listener_list.remove(listener);
			remove_self = listener_list.isEmpty();
		}
		
		if (remove_self) {
			download_manager.getDownloadState().removeListener(this, attribute, event_type);
		}
		
	}
	
	public DownloadAnnounceResult
	getLastAnnounceResult()
	{
		TRTrackerAnnouncer tc = download_manager.getTrackerClient();
		
		if ( tc != null ){
			
			last_announce_result.setContent( tc.getLastResponse());
		}
		
		return( last_announce_result );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{		
		TRTrackerScraperResponse response = download_manager.getTrackerScrapeResponse();

		if ( response != null ){
			
				// don't notify plugins of intermediate (initializing, scraping) states as they would be picked up as errors
			
			if ( response.getStatus() == TRTrackerScraperResponse.ST_ERROR || response.getStatus() == TRTrackerScraperResponse.ST_ONLINE ){
				
				last_scrape_result.setContent( response );
			}
		}
		
		return( last_scrape_result );
	}
	
	
	public void
	scrapeResult(
		TRTrackerScraperResponse	response )
	{
		// don't notify plugins of intermediate (initializing, scraping) states as they would be picked up as errors 
		if(response.getStatus() != TRTrackerScraperResponse.ST_ERROR && response.getStatus() != TRTrackerScraperResponse.ST_ONLINE)
			return;
		
		last_scrape_result.setContent( response );
		
		for (int i=0;i<tracker_listeners.size();i++){
			
			try{						
				((DownloadTrackerListener)tracker_listeners.get(i)).scrapeResult( last_scrape_result );

			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	// Used by DownloadEventNotifierImpl.
	void announceTrackerResultsToListener(DownloadTrackerListener l) {
		l.announceResult(last_announce_result);
		l.scrapeResult(last_scrape_result);
	}
	
	public void
	announceResult(
		TRTrackerAnnouncerResponse			response )
	{
		last_announce_result.setContent( response );
		
		List	tracker_listeners_ref = tracker_listeners;
		
		for (int i=0;i<tracker_listeners_ref.size();i++){
			
			try{						
				((DownloadTrackerListener)tracker_listeners_ref.get(i)).announceResult( last_announce_result );

			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public TrackerPeerSource
	getTrackerPeerSource()
	{
		return( 
			new TrackerPeerSourceAdapter()
			{
				private long	fixup;
				private int		state;
				private String 	details = "";
				private int		seeds;
				private int		leechers;
				private int		peers;
				
				private void
				fixup()
				{
					long	now = SystemTime.getCurrentTime();
					
					if ( now - fixup > 1000 ){
					
						if ( !download_manager.getDownloadState().isPeerSourceEnabled( PEPeerSource.PS_PLUGIN )){
							
							state = ST_DISABLED;
							
						}else{
						
							int s = getState();
							
							if ( s == ST_DOWNLOADING || s == ST_SEEDING ){
								
								state = ST_ONLINE;
								
							}else{
								
								state = ST_STOPPED;
							}
						}
						
						if ( state == ST_ONLINE ){
							
							try{
								peer_listeners_mon.enter();
		
								int	s	= 0;
								int	l	= 0;
								int p 	= 0;
								
								String	str = "";
								
								if ( announce_response_map != null ){
								
									for (Map.Entry<String, int[]> entry: announce_response_map.entrySet()){
										
										String 	cn 		= entry.getKey();
										int[]	data 	= entry.getValue();
										
										str += (str.length()==0?"":", ") + cn;
										
										str += " " + data[0] + "/" + data[1] + "/" + data[2];
										
										s += data[0];
										l += data[1];
										p += data[2];
									}
								}
								
								details 	= str;
								seeds		= s;
								leechers	= l;
								peers		= p;
								
							}finally{
								
								peer_listeners_mon.exit();
							}
						}else{
							
							details = "";
						}
						
						fixup = now;
					}
				}
				
				public int
				getType()
				{
					return( TP_PLUGIN );
				}
				
				public int
				getStatus()
				{
					fixup();
					
					return( state );
				}
				
				public String
				getName()
				{
					fixup();
				
					if ( state == ST_ONLINE ){
						
						return( details );
					}
					
					return( "" );
				}
				
				public int
				getSeedCount()
				{
					fixup();
					
					if ( state == ST_ONLINE ){
						
						return( seeds );
					}
					
					return( -1 );
				}
				
				public int
				getLeecherCount()
				{
					fixup();
					
					if ( state == ST_ONLINE ){
						
						return( leechers );
					}
					
					return( -1 );
				}
				
				public int
				getPeers()
				{
					fixup();
					
					if ( state == ST_ONLINE ){
						
						return( peers );
					}
					
					return( -1 );
				}
			});
	}
	
	private String
	getTrackingName(
		Object		obj )
	{
		String	name = obj.getClass().getName();
		
		int	pos = name.lastIndexOf( '.' );
		
		name = name.substring( pos+1 );
		
		pos = name.indexOf( '$' );
		
		if ( pos != -1 ){
			
			name = name.substring( 0, pos );
		}
		
			// hack alert - could use classloader to find plugin I guess
		
		if ( name.equals( "DHTTrackerPlugin" )){
			
			name = null;
			
		}else if ( name.equals( "DHTAnnounceResult")){
			
			name = "mlDHT";
		}
		
		return( name );
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		String class_name = getTrackingName( result );
		
		if ( class_name != null ){
			
			int	seeds 		= result.getSeedCount();
			int	leechers 	= result.getNonSeedCount();
			
			DownloadAnnounceResultPeer[] peers = result.getPeers();
			
			int	peer_count = peers==null?0:peers.length;
			
			try{
				peer_listeners_mon.enter();
				
				if ( announce_response_map == null ){
					
					announce_response_map = new HashMap<String, int[]>();
					
				}else{
					
					if ( announce_response_map.size() > 32 ){
						
						Debug.out( "eh?" );
						
						announce_response_map.clear();
					}
				}
				
				int[]	data = (int[])announce_response_map.get( class_name );
				
				if ( data == null ){
					
					data = new int[3];
					
					announce_response_map.put( class_name, data );
				}
				
				data[0]	= seeds;
				data[1]	= leechers;
				data[2]	= peer_count;
				
			}finally{
				
				peer_listeners_mon.exit();
			}
		}
		
		download_manager.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
		String class_name = getTrackingName( result );
		
		if ( class_name != null ){
			
			int	seeds 		= result.getSeedCount();
			int	leechers 	= result.getNonSeedCount();
					
			try{
				peer_listeners_mon.enter();
				
				if ( announce_response_map == null ){
					
					announce_response_map = new HashMap<String, int[]>();
					
				}else{
					
					if ( announce_response_map.size() > 32 ){
						
						Debug.out( "eh?" );
						
						announce_response_map.clear();
					}
				}
				
				int[]	data = (int[])announce_response_map.get( class_name );
				
				if ( data == null ){
					
					data = new int[3];
					
					announce_response_map.put( class_name, data );
				}
				
				data[0]	= seeds;
				data[1]	= leechers;
				
			}finally{
				
				peer_listeners_mon.exit();
			}
		}
		
		download_manager.setScrapeResult( result );
	}
	
	public void
	stateChanged(
		DownloadManagerState			state,
		DownloadManagerStateEvent		event )
	{
		final int type = event.getType();
		
		if ( 	type == DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN ||
				type == DownloadManagerStateEvent.ET_ATTRIBUTE_WILL_BE_READ 	){
			
			String	name = (String)event.getData();
			
			List	property_listeners_ref = property_listeners;
			
			final TorrentAttribute	attr = convertAttribute( name );
			
			if ( attr != null ){
				
				for (int i=0;i<property_listeners_ref.size();i++){
					
					try{						
						((DownloadPropertyListener)property_listeners_ref.get(i)).propertyChanged(
								this,
								new DownloadPropertyEvent()
								{
									public int
									getType()
									{
										return( type==DownloadManagerStateEvent.ET_ATTRIBUTE_WRITTEN
													?DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WRITTEN
													:DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WILL_BE_READ	);
									}
									
									public Object
									getData()
									{
										return( attr );
									}
								});

					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
			}			
		}
	}
	
	public void
	addPropertyListener(
		DownloadPropertyListener	l )
	{
		
		// Compatibility for the autostop plugin.
		if ("com.aimedia.stopseeding.core.RatioWatcher".equals(l.getClass().getName())) {
			
			// Looking at the source code, this method doesn't actually appear to do anything,
			// so we can avoid doing anything for now.
			return;
			/*
			if (property_to_attribute_map == null) {property_to_attribute_map = new HashMap(1);}
			DownloadAttributeListener dal = new PropertyListenerBridge(l);
			property_to_attribute_map.put(l, dal);
			this.addAttributeListener(dal, attr, event_type);
			*/
		}
		
		PluginDeprecation.call("property listener", l);
		try{
			tracker_listeners_mon.enter();
	
			List	new_property_listeners = new ArrayList( property_listeners );
			
			new_property_listeners.add( l );
			
			property_listeners	= new_property_listeners;
			
			if ( property_listeners.size() == 1 ){
				
				download_manager.getDownloadState().addListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}	
	}
	
	public void
	removePropertyListener(
		DownloadPropertyListener	l )
	{
		
		// Compatibility for the autostop plugin.
		if ("com.aimedia.stopseeding.core.RatioWatcher".equals(l.getClass().getName())) {
			
			// Looking at the source code, this method doesn't actually appear to do anything,
			// so we can avoid doing anything for now.
			return;
		}
		
		try{
			tracker_listeners_mon.enter();
			
			List	new_property_listeners	= new ArrayList( property_listeners );
			
			new_property_listeners.remove( l );
			
			property_listeners	= new_property_listeners;
			
			if ( property_listeners.size() == 0 ){
				
				download_manager.getDownloadState().removeListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}		
	}
	
	public void
	torrentChanged()
	{
		TRTrackerAnnouncer	client = download_manager.getTrackerClient();
		
		if ( client != null ){
			
			client.resetTrackerUrl(true);
		}
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
		addTrackerListener(l, true);
	}

	public void
	addTrackerListener(
		DownloadTrackerListener	l,
		boolean immediateTrigger )
	{
		try{
			tracker_listeners_mon.enter();
	
			List	new_tracker_listeners = new ArrayList( tracker_listeners );
			
			new_tracker_listeners.add( l );
			
			tracker_listeners	= new_tracker_listeners;
			
			if ( tracker_listeners.size() == 1 ){
				
				download_manager.addTrackerListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}
		
		if (immediateTrigger) {this.announceTrackerResultsToListener(l);}
	}
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
		try{
			tracker_listeners_mon.enter();
			
			List	new_tracker_listeners	= new ArrayList( tracker_listeners );
			
			new_tracker_listeners.remove( l );
			
			tracker_listeners	= new_tracker_listeners;
			
			if ( tracker_listeners.size() == 0 ){
				
				download_manager.removeTrackerListener( this );
			}
		}finally{
			
			tracker_listeners_mon.exit();
		}
	}
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		try{
			removal_listeners_mon.enter();
			
			List	new_removal_listeners	= new ArrayList( removal_listeners );
			
			new_removal_listeners.add(l);
			
			removal_listeners	= new_removal_listeners;
			
		}finally{
			
			removal_listeners_mon.exit();
		}
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l ) 
	{
		try{
			removal_listeners_mon.enter();
			
			List	new_removal_listeners	= new ArrayList( removal_listeners );
			
			new_removal_listeners.remove(l);
			
			removal_listeners	= new_removal_listeners;
			
		}finally{
			
			removal_listeners_mon.exit();
		}
	}
	
	public void
	addPeerListener(
		final DownloadPeerListener	listener )
	{
		DownloadManagerPeerListener delegate =
			new DownloadManagerPeerListener()
			{
				
				public void
				peerManagerAdded(
					PEPeerManager	manager )
				{
					PeerManager pm = PeerManagerImpl.getPeerManager( manager);
					
					listener.peerManagerAdded( DownloadImpl.this, pm );
				}
				
				public void
				peerManagerRemoved(
					PEPeerManager	manager )
				{
					PeerManager pm = PeerManagerImpl.getPeerManager( manager);
					
					listener.peerManagerRemoved( DownloadImpl.this, pm );

				}
				
				public void
				peerManagerWillBeAdded(
					PEPeerManager	manager )
				{
				}

				public void
				peerAdded(
					PEPeer 	peer )
				{				
				}
					
				public void
				peerRemoved(
					PEPeer	peer )
				{	
				}
			};
		
		try{
			peer_listeners_mon.enter();
							
			peer_listeners.put( listener, delegate );
		
		}finally{
			
			peer_listeners_mon.exit();
		}
		
		download_manager.addPeerListener( delegate );
	}
	
	
	public void
	removePeerListener(
		DownloadPeerListener	listener )
	{
		DownloadManagerPeerListener delegate;
		
		try{
			peer_listeners_mon.enter();
			
			delegate = (DownloadManagerPeerListener)peer_listeners.remove( listener );

		}finally{
			
			peer_listeners_mon.exit();
		}
		
		if ( delegate == null ){
			
			Debug.out( "Listener not found for removal" );
			
		}else{
			
			download_manager.removePeerListener( delegate );
		}
	}
	
	public boolean
	activateRequest(
		final int		count )
	{
		DownloadActivationEvent event = 
			new DownloadActivationEvent()
		{
			public Download 
			getDownload() 
			{
				return( DownloadImpl.this );
			}
			
			public int
			getActivationCount()
			{
				return( count );
			}
		};
		
		for (Iterator it=activation_listeners.iterator();it.hasNext();){	
				
			try{
				DownloadActivationListener	listener = (DownloadActivationListener)it.next();
				
				if ( listener.activationRequested( event )){
					
					return( true );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( false );
	}
	
	public DownloadActivationEvent
	getActivationState()
	{
		return( activation_state );
	}
	
	public void
	addActivationListener(
		DownloadActivationListener		l )
	{
		try{
			peer_listeners_mon.enter();
		
			activation_listeners.add( l );
			
			if ( activation_listeners.size() == 1 ){
				
				download_manager.addActivationListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
		}
	}
	
	public void
	removeActivationListener(
		DownloadActivationListener		l )
	{
		try{
			peer_listeners_mon.enter();

			activation_listeners.remove( l );
			
			if ( activation_listeners.size() == 0 ){
				
				download_manager.removeActivationListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
		}
	}

	public void	addCompletionListener(DownloadCompletionListener l) {
		try {
			listeners_mon.enter();
			this.completion_listeners.add(l);
		}
		finally{
			listeners_mon.exit();
		}
	}
	
	public void	removeCompletionListener(DownloadCompletionListener l) {
		try {
			listeners_mon.enter();
			this.completion_listeners.remove(l);
		}
		finally{
			listeners_mon.exit();
		}
	}
	
 	public PeerManager
	getPeerManager()
 	{
 		PEPeerManager	pm = download_manager.getPeerManager();
 		
 		if ( pm == null ){
 			
 			return( null );
 		}
 		
 		return( PeerManagerImpl.getPeerManager( pm));
 	}
 	
	public DiskManager
	getDiskManager()
	{
		PeerManager	pm = getPeerManager();
		
		if ( pm != null ){
			
			return( pm.getDiskManager());
		}
		
		return( null );
	}
	
	public int getDiskManagerFileCount() {
		return download_manager.getNumFileInfos();
	}
	
	public DiskManagerFileInfo getDiskManagerFileInfo(int index) {
		org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] info = download_manager.getDiskManagerFileInfo();

		if (info == null) {
			return null;
		}
		if (index < 0 || index >= info.length) {
			return null;
		}

		return new DiskManagerFileInfoImpl(this, info[index]);
	}
	
	public DiskManagerFileInfo[]
	getDiskManagerFileInfo()
	{
		org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] info = download_manager.getDiskManagerFileInfo();
		
		if ( info == null ){
			
			return( new DiskManagerFileInfo[0] );
		}
		
		DiskManagerFileInfo[]	res = new DiskManagerFileInfo[info.length];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = new DiskManagerFileInfoImpl( this, info[i] );
		}
		
		return( res );
	}
	
 	public void
	setMaximumDownloadKBPerSecond(
		int		kb )
 	{
         if(kb==-1){
            Debug.out("setMaximiumDownloadKBPerSecond got value (-1) ZERO_DOWNLOAD. (-1)"+
                "does not work through this method, use getDownloadRateLimitBytesPerSecond() instead.");
         }//if

         download_manager.getStats().setDownloadRateLimitBytesPerSecond( kb < 0 ? 0 : kb*1024 );
 	}
  	
  	public int
	getMaximumDownloadKBPerSecond()
  	{
  		return( download_manager.getStats().getDownloadRateLimitBytesPerSecond() /1024 );
  	}
    
  	public int getUploadRateLimitBytesPerSecond() {
      return download_manager.getStats().getUploadRateLimitBytesPerSecond();
  	}

  	public void setUploadRateLimitBytesPerSecond( int max_rate_bps ) {
      download_manager.getStats().setUploadRateLimitBytesPerSecond( max_rate_bps );
  	}

  	public int getDownloadRateLimitBytesPerSecond() {
  		return download_manager.getStats().getDownloadRateLimitBytesPerSecond();
  	}

  	public void setDownloadRateLimitBytesPerSecond( int max_rate_bps ) {
  		download_manager.getStats().setDownloadRateLimitBytesPerSecond( max_rate_bps );
  	}

  public int getSeedingRank() {
    return download_manager.getSeedingRank();
  }
    
	public void setSeedingRank(int rank) {
		download_manager.setSeedingRank(rank);
	}
  	
	public String
	getSavePath()
 	{
		return( download_manager.getSaveLocation().toString());
 	}
	
	public void
  	moveDataFiles(
  		File	new_parent_dir )
  	
  		throws DownloadException
  	{
 		try{
 			download_manager.moveDataFiles( new_parent_dir );
 			
 		}catch( DownloadManagerException e ){
 			
 			throw( new DownloadException("move operation failed", e ));
 		}
  	}

	public void moveDataFiles(File new_parent_dir, String new_name)
  	
  		throws DownloadException
  	{
 		try{
 			download_manager.moveDataFiles( new_parent_dir, new_name );
 			
 		}catch( DownloadManagerException e ){
 			
 			throw( new DownloadException("move / rename operation failed", e ));
 		}
  	}
	
	public void renameDownload(String new_name) throws DownloadException {
		try {download_manager.renameDownload(new_name);}
		catch (DownloadManagerException e) {
			throw new DownloadException("rename operation failed", e);
		}
	}
  	
  	public void
  	moveTorrentFile(
  		File	new_parent_dir )
  	
  		throws DownloadException
 	{
		try{
 			download_manager.moveTorrentFile( new_parent_dir );
 			
 		}catch( DownloadManagerException e ){
 			
 			throw( new DownloadException("move operation failed", e ));
 		}  	
 	}
  	
  	/**
  	 * @deprecated
  	 */
  	public File[] calculateDefaultPaths(boolean for_moving) {
  	  SaveLocationChange slc = this.calculateDefaultDownloadLocation(); 
	  if (slc == null) {return null;}
	  return new File[] {slc.download_location, slc.torrent_location};
  	}
  	
  	public boolean isInDefaultSaveDir() {
  		return download_manager.isInDefaultSaveDir();
  	}
  	
 	public void
	requestTrackerAnnounce()
 	{
 		download_manager.requestTrackerAnnounce( false );
 	}
 	
	public void
	requestTrackerAnnounce(
		boolean		immediate )
	{
		download_manager.requestTrackerAnnounce( immediate );
	}
	
	public void
	requestTrackerScrape(
		boolean		immediate )
	{
		download_manager.requestTrackerScrape( immediate );
	}
	
  public byte[] getDownloadPeerId() {
    TRTrackerAnnouncer announcer = download_manager.getTrackerClient();
    if(announcer == null) return null;
    return announcer.getPeerId();
  }
  
  
  public boolean isMessagingEnabled() {  return download_manager.isExtendedMessagingEnabled();  }

  public void setMessagingEnabled( boolean enabled ) {
	  throw new RuntimeException("setMessagingEnabled is in the process of being removed - if you are seeing this error, let the Azureus developers know that you need this method to stay!");
    //download_manager.setAZMessagingEnabled( enabled );
  }
  
  
 	// Deprecated methods

  public int getPriority() {
    return 0;
  }
  
  public boolean isPriorityLocked() {
    return false;
  }  

  public void setPriority(int priority) {
  }

  public boolean isRemoved() {
	return( download_manager.isDestroyed());
  }
  // Pass LogRelation off to core objects

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getLogRelationText()
	 */
	public String getRelationText() {
		return propogatedRelationText(download_manager);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getQueryableInterfaces()
	 */
	public Object[] getQueryableInterfaces() {
		return new Object[] { download_manager };
	}
	
	private CopyOnWriteMap getAttributeMapForType(int event_type) {
		return event_type == DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WILL_BE_READ ? read_attribute_listeners_map_cow : write_attribute_listeners_map_cow;
	}
	
	public boolean canMoveDataFiles() {
		return download_manager.canMoveDataFiles();
	}
	
	public void attributeEventOccurred(DownloadManager download, String attribute, int event_type) {
		CopyOnWriteMap attr_listener_map = getAttributeMapForType(event_type);

		TorrentAttribute attr = convertAttribute(attribute);
		if (attr == null) {return;}
		
		List listeners = null;
		listeners = ((CopyOnWriteList)attr_listener_map.get(attribute)).getList();

		if (listeners == null) {return;}
	
		for (int i=0; i<listeners.size(); i++) {
			DownloadAttributeListener dal = (DownloadAttributeListener)listeners.get(i);
			try {dal.attributeEventOccurred(this, attr, event_type);}
			catch (Throwable t) {Debug.printStackTrace(t);}
		}
	}
	
	public SaveLocationChange calculateDefaultDownloadLocation() {
		return DownloadManagerMoveHandler.recalculatePath(this.download_manager);
	}
	
	 public Object getUserData( Object key ){
		 return( download_manager.getUserData(key));
	 }

	 public void setUserData( Object key, Object data ){
		 download_manager.setUserData(key, data);
	 }
	 
	 public void startDownload(boolean force) {
		if (force) {
			this.setForceStart(true);
			return;
		}
		this.setForceStart(false);
		
		int state = this.getState();
		if (state == DownloadManager.STATE_STOPPED ||	state == DownloadManager.STATE_QUEUED) {
			download_manager.setStateWaiting();
		}
		
	 }
	 
	 public void stopDownload() {
		 if (download_manager.getState() == DownloadManager.STATE_STOPPED) {return;}
		 download_manager.stopIt(DownloadManager.STATE_STOPPED, false, false);
	 }
	 
	 public void changeLocation(SaveLocationChange slc) throws DownloadException {
		 
		 // No change in the file.
		 boolean has_change = slc.hasDownloadChange() || slc.hasTorrentChange();
		 if (!has_change) {return;}
		 
		 // Test that one of the locations is actually different.
		 has_change = slc.isDifferentDownloadLocation(new File(this.getSavePath()));
		 if (!has_change) {
			 has_change = slc.isDifferentTorrentLocation(new File(this.getTorrentFileName()));
		 }
		 
		 if (!has_change) {return;}

		 boolean try_to_resume = !this.isPaused();
		 try {
			 try {
				 if (slc.hasDownloadChange()) {download_manager.moveDataFiles(slc.download_location, slc.download_name);}
				 if (slc.hasTorrentChange()) {download_manager.moveTorrentFile(slc.torrent_location, slc.torrent_name);}
			 }
			 catch (DownloadManagerException e) {
				 throw new DownloadException(e.getMessage(), e);
			 }
		 }
		 finally {
			 if (try_to_resume) {this.resume();}
		 }
			 
	 }
	 
	 private static class PropertyListenerBridge implements DownloadAttributeListener {
		 private DownloadPropertyListener l;
		 public PropertyListenerBridge(DownloadPropertyListener l) {this.l = l;}
		 public void attributeEventOccurred(Download d, final TorrentAttribute attr, final int event_type) {
			 l.propertyChanged(d, new DownloadPropertyEvent() {
				 public int getType() {return event_type;}
				 public Object getData() {return attr;}
			 });
		 }
	 }
	 
}