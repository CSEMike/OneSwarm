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

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.*;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;

import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadActivationEvent;
import org.gudy.azureus2.plugins.download.DownloadActivationListener;
import org.gudy.azureus2.plugins.download.DownloadAttributeListener;
import org.gudy.azureus2.plugins.download.DownloadListener;
import org.gudy.azureus2.plugins.download.DownloadPeerListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyListener;
import org.gudy.azureus2.plugins.download.DownloadPropertyEvent;
import org.gudy.azureus2.plugins.download.DownloadTrackerListener;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.download.DownloadStats;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.plugins.download.session.SessionAuthenticator;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogRelation;

import com.aelitis.azureus.core.util.CopyOnWriteList;

public class 
DownloadImpl
	extends LogRelation
	implements 	Download, DownloadManagerListener, 
				DownloadManagerTrackerListener, DownloadManagerPeerListener,
				DownloadManagerStateListener, DownloadManagerActivationListener
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
	private List		peer_listeners			= new ArrayList();
	private AEMonitor	peer_listeners_mon		= new AEMonitor( "Download:PL");
	
	private DownloadPropertyListener attribute_listener_bridge = null;
	private AEMonitor attr_listener_mon = new AEMonitor("Download:ATTR");
	private Map read_attribute_listeners = new HashMap();
	private Map write_attribute_listeners = new HashMap();
	
	private CopyOnWriteList	activation_listeners	= new CopyOnWriteList();
	private DownloadActivationEvent	activation_state;
	
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
	
		if ( prev_state != latest_state || latest_forcedStart != curr_forcedStart ){
			
			latest_forcedStart = curr_forcedStart;
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					long startTime = SystemTime.getCurrentTime();
					DownloadListener listener = (DownloadListener)listeners.get(i);

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
		try {
			this.attr_listener_mon.enter();
			if (this.attribute_listener_bridge == null) {
				this.attribute_listener_bridge = new DownloadAttributeListenerBridge();
				this.addPropertyListener(this.attribute_listener_bridge);
			}
			Map attr_map = this.getAttributeMapForType(event_type);
			List listener_list = (List)attr_map.get(attr);
			if (listener_list == null) {
				listener_list = new ArrayList();
				attr_map.put(attr, listener_list);
			}
			listener_list.add(listener);
		}
		finally {this.attr_listener_mon.exit();}
	}

	public void removeAttributeListener(DownloadAttributeListener listener, TorrentAttribute attr, int event_type) {
		try {
			this.attr_listener_mon.enter();
			Map attr_map = this.getAttributeMapForType(event_type);
			List listener_list = (List)attr_map.get(attr);
			
			// Remove the listener, and clear up the mapping list if need be.
			if (listener_list != null) {
				listener_list.remove(listener);
				if (listener_list.isEmpty()) {attr_map.remove(attr);}
			}
			
			// If both mappings are empty, destroy the bridge.
			if (attribute_listener_bridge != null && read_attribute_listeners.isEmpty() && write_attribute_listeners.isEmpty()) {
				this.removePropertyListener(attribute_listener_bridge);
				this.attribute_listener_bridge = null;
			}
		}
		finally {this.attr_listener_mon.exit();}
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
	
		last_scrape_result.setContent( response );
		
		return( last_scrape_result );
	}
	
	
	public void
	scrapeResult(
		TRTrackerScraperResponse	response )
	{
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
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		download_manager.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
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
		DownloadPeerListener	l )
	{
		try{
			peer_listeners_mon.enter();
		
			List	new_peer_listeners	= new ArrayList( peer_listeners );
			
			new_peer_listeners.add( l );
			
			peer_listeners	= new_peer_listeners;
			
			if ( peer_listeners.size() == 1 ){
				
				download_manager.addPeerListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
		}
	}
	
	
	public void
	removePeerListener(
		DownloadPeerListener	l )
	{
		try{
			peer_listeners_mon.enter();

			List	new_peer_listeners	= new ArrayList( peer_listeners );
			
			new_peer_listeners.remove( l );
			
			peer_listeners	= new_peer_listeners;
			
			if ( peer_listeners.size() == 0 ){
				
				download_manager.removePeerListener( this );
			}
		}finally{
			
			peer_listeners_mon.exit();
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
	
	public void
	peerManagerWillBeAdded(
		PEPeerManager	manager )
	{
	}
	
	public void
	peerManagerAdded(
		PEPeerManager	manager )
	{
		if ( peer_listeners.size() > 0 ){
			
			PeerManager pm = PeerManagerImpl.getPeerManager( manager);
		
			for (int i=0;i<peer_listeners.size();i++){
		
				((DownloadPeerListener)peer_listeners.get(i)).peerManagerAdded( this, pm );
			}
		}
	}
	
	public void
	peerManagerRemoved(
		PEPeerManager	manager )
	{
		if ( peer_listeners.size() > 0 ){
			
			PeerManager pm = PeerManagerImpl.getPeerManager( manager);
		
			for (int i=0;i<peer_listeners.size();i++){
		
				((DownloadPeerListener)peer_listeners.get(i)).peerManagerRemoved( this, pm );
			}
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
	peerAdded(
		PEPeer 	peer )
	{
		
	}
		
	public void
	peerRemoved(
		PEPeer	peer )
	{
		
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
  	
  	public File[] calculateDefaultPaths(boolean for_moving) {
  		return download_manager.calculateDefaultPaths(for_moving);
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
  
  
  public void setSessionAuthenticator( SessionAuthenticator auth ) {
    //TODO
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
	
	private class DownloadAttributeListenerBridge implements DownloadPropertyListener {
		public void propertyChanged(Download d, DownloadPropertyEvent e) {
			Map attr_listener_map = getAttributeMapForType(e.getType());
			List listeners = (List)attr_listener_map.get(e.getData());
			if (listeners == null) {return;}
			try {
				attr_listener_mon.enter();
				ArrayList listener_ref = new ArrayList(listeners);
				for (int i=0; i<listener_ref.size(); i++) {
					DownloadAttributeListener dal = (DownloadAttributeListener)listener_ref.get(i);
					try {
						dal.attributeEventOccurred(d, (TorrentAttribute)e.getData(), e.getType());
					}
					catch (Throwable t) {Debug.printStackTrace(t);}
				}
			}
			finally {attr_listener_mon.exit();}
		}
	}
	
	private Map getAttributeMapForType(int event_type) {
		return event_type == DownloadPropertyEvent.PT_TORRENT_ATTRIBUTE_WILL_BE_READ ? read_attribute_listeners : write_attribute_listeners;
	}
	
}
