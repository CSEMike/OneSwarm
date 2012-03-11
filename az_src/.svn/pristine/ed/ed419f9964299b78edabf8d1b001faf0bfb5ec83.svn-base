/*
 * File    : PRDownload.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.remote.download;

/**
 * @author parg
 *
 */

import java.io.File;
import java.util.Map;

import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.download.savelocation.*;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsimpl.remote.*;
import org.gudy.azureus2.pluginsimpl.remote.disk.RPDiskManagerFileInfo;
import org.gudy.azureus2.pluginsimpl.remote.torrent.*;


public class 
RPDownload
	extends		RPObject
	implements 	Download
{
	protected transient Download		delegate;

		// don't change these field names as they are visible on XML serialisation
	
	public RPTorrent				torrent;
	public RPDownloadStats			stats;
	public RPDownloadAnnounceResult	announce_result;
	public RPDownloadScrapeResult	scrape_result;
	
	public int						position;
	public boolean					force_start;
	
	public static RPDownload
	create(
		Download		_delegate )
	{
		RPDownload	res =(RPDownload)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDownload( _delegate );
		}
			
		return( res );
	}
	
	protected
	RPDownload(
		Download		_delegate )
	{
		super( _delegate );
			
			// torrent can be null if broken
		
		if ( delegate.getTorrent() != null ){
			
			torrent = (RPTorrent)_lookupLocal( delegate.getTorrent());
		
			if ( torrent == null ){
				
				torrent = RPTorrent.create( delegate.getTorrent());
			}
		}
		
		stats = (RPDownloadStats)_lookupLocal( delegate.getStats());
		
		if ( stats == null ){
			
			stats = RPDownloadStats.create( delegate.getStats());
		}
		
		announce_result = (RPDownloadAnnounceResult)_lookupLocal( delegate.getLastAnnounceResult());
		
		if ( announce_result == null ){
			
			announce_result = RPDownloadAnnounceResult.create( delegate.getLastAnnounceResult());
		}
		
		scrape_result = (RPDownloadScrapeResult)_lookupLocal( delegate.getLastScrapeResult());
		
		if ( scrape_result == null ){
			
			scrape_result = RPDownloadScrapeResult.create( delegate.getLastScrapeResult());
		}
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (Download)_delegate;
		
		position	= delegate.getPosition();
		force_start	= delegate.isForceStart();
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		Object res = _fixupLocal();
		
		if ( torrent != null ){
			
			torrent._setLocal();
		}
		
		stats._setLocal();
		
		announce_result._setLocal();
		
		scrape_result._setLocal();
		
		return( res );
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		dispatcher )
	{
		super._setRemote( dispatcher );
		
		if ( torrent != null ){
			
			torrent._setRemote( dispatcher );
		}
		
		stats._setRemote( dispatcher );
		
		announce_result._setRemote( dispatcher );
		
		scrape_result._setRemote( dispatcher );
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "initialize")){
			
			try{
				delegate.initialize();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "start")){
			
			try{
				delegate.start();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "restart")){
			
			try{
				delegate.restart();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "stop")){
			
			try{
				delegate.stop();
				
			}catch( DownloadException e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "remove")){
			
			try{
				delegate.remove();
				
			}catch( Throwable e ){
				
				return( new RPReply(e));
			}
			
			return( null );
			
		}else if ( method.equals( "setForceStart[boolean]")){
			
			boolean	b = ((Boolean)request.getParams()[0]).booleanValue();
			
			delegate.setForceStart( b );
			
			return( null );
			
		}else if ( method.equals( "setPosition[int]")){
			
			int	p = ((Integer)request.getParams()[0]).intValue();
			
			delegate.setPosition( p );
			
			return( null );
			
		}else if ( method.equals( "moveUp")){
						
			delegate.moveUp();
			
			return( null );
			
		}else if ( method.equals( "moveDown")){
			
			delegate.moveDown();
			
			return( null );
	
		}else if ( method.equals( "moveTo[int]")){
			
			int	p = ((Integer)request.getParams()[0]).intValue();
			
			delegate.setPosition( p );
			
			return( null );
			
		}else if ( method.equals( "setPriority[int]")){
			
			delegate.setPriority(((Integer)request.getParams()[0]).intValue());
			
			return( null );
			
		}else if ( method.equals( "requestTrackerAnnounce")){
			
			delegate.requestTrackerAnnounce();
			
			return( null );
		
		}else if ( method.equals( "getDiskManagerFileInfo")){
			
			DiskManagerFileInfo[] info = delegate.getDiskManagerFileInfo();
					
			RPDiskManagerFileInfo[] rp_info = new RPDiskManagerFileInfo[info.length];
			
			for (int i=0;i<rp_info.length;i++){
				
				rp_info[i] = RPDiskManagerFileInfo.create( info[i] );
			}
			
			return( new RPReply( rp_info ));
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
		// ***************************************************
	
	public int
	getState()
	{
		notSupported();
		
		return(0);
	}
	
	public int
	getSubState()
	{
		notSupported();
		
		return(0);
	}

	public String
	getErrorStateDetails()
	{
		notSupported();
		
		return( null );
	}
	
	public boolean
	getFlag(
		long	flag )
	{
		notSupported();
		
		return( false );
	}
	
	public long
	getFlags()
	{
		notSupported();
		
		return( 0 );
	}
	
	public int
	getIndex()
	{
		notSupported();
		
		return( 0 );
	}
	
	public Torrent
	getTorrent()
	{
		return( torrent );
	}
	
  
  public byte[] getDownloadPeerId() {
    return delegate.getDownloadPeerId();
  }
  
  
  public boolean isMessagingEnabled() {  return delegate.isMessagingEnabled();  }

  public void setMessagingEnabled( boolean enabled ) {
    delegate.setMessagingEnabled( enabled );
  }
  
  
  
	public void
	initialize()
	
		throws DownloadException	
	{
		try{
			_dispatcher.dispatch( new RPRequest( this, "initialize", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	start()
	
		throws DownloadException
	{
		try{
			_dispatcher.dispatch( new RPRequest( this, "start", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	stop()
	
		throws DownloadException
	{
		try{
			_dispatcher.dispatch( new RPRequest( this, "stop", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public void
	restart()
	
		throws DownloadException
	{
		try{
			_dispatcher.dispatch( new RPRequest( this, "restart", null )).getResponse();
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof DownloadException ){
				
				throw((DownloadException)e.getCause());
			}
			
			throw( e );
		}
	}
	
	public boolean
	isStartStopLocked()
	{
		notSupported();
		
		return( false );
	}
	
	public boolean
	isPaused()
	{
		notSupported();
		
		return( false );
	}
	
	public void
	pause()
	{
		notSupported();
	}
	
	public void
	resume()
	{
		notSupported();
	}
	
	public int
	getPriority()
	{
		//do nothing deprecated
	  return 0;
	}
	
	public void
	setPriority(
		int		priority )
	{
		//Do nothing, deprecated
	}
	
	/**
	 * @deprecated
	 */
	
	public boolean
	isPriorityLocked()
	{
	  	//	Do nothing, deprecated
		
		return( false );
	}
	
	public void
	remove()
	
		throws DownloadException, DownloadRemovalVetoException
	{
		try{
			_dispatcher.dispatch( new RPRequest( this, "remove", null )).getResponse();
			
		}catch( RPException e ){
			
			Throwable cause = e.getCause();
			
			if ( cause instanceof DownloadException ){
				
				throw((DownloadException)cause);
			}
			
			if ( cause instanceof DownloadRemovalVetoException ){
				
				throw((DownloadRemovalVetoException)cause);
			}
			
			throw( e );
		}
	}
	
	public void
	remove(
		boolean	delete_torrent,
		boolean	delete_data )
	
		throws DownloadException, DownloadRemovalVetoException
	{
		notSupported();
	}
	
	public boolean
	canBeRemoved()
	
		throws DownloadRemovalVetoException
	{
		notSupported();
		
		return( false );
	}
	
	public DownloadAnnounceResult
	getLastAnnounceResult()
	{
		return( announce_result );
	}
	
	public DownloadScrapeResult
	getLastScrapeResult()
	{
		return( scrape_result );
	}
	
	public DownloadStats
	getStats()
	{
		return( stats );
	}
	
	public void
	addListener(
		DownloadListener	l )
	{
		notSupported();
	}
	
	public void
	removeListener(
		DownloadListener	l )
	{
		notSupported();
	}
	
	public void
	addPropertyListener(
		DownloadPropertyListener	l )
	{
		notSupported();
	}
	
	public void
	removePropertyListener(
		DownloadPropertyListener	l )
	{
		notSupported();
	}
	
	public void
	addTrackerListener(
		DownloadTrackerListener	l )
	{
		notSupported();
	}
	
	public void
	removeTrackerListener(
		DownloadTrackerListener	l )
	{
		notSupported();
	}
	
	public void
	addDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}
	
	public void
	removeDownloadWillBeRemovedListener(
		DownloadWillBeRemovedListener	l )
	{
		notSupported();
	}
	
	public int 
	getPosition() 
	{	
		return( position );
	}
	
	public boolean 
	isForceStart()
	{	
		return( force_start );
	}
	
	public void 
	setForceStart(
		boolean _force_start ) 
	{
		force_start	= _force_start;
		
		_dispatcher.dispatch( new RPRequest( this, "setForceStart[boolean]", new Object[]{new Boolean(force_start )})).getResponse();
	}
	
	public void 
	setPosition(
		int new_position) 
	{
		_dispatcher.dispatch( new RPRequest( this, "setPosition[int]", new Object[]{new Integer(new_position )})).getResponse();
	}
	
	public void
	moveUp()
	{
		_dispatcher.dispatch( new RPRequest( this, "moveUp", null)).getResponse();
	}
	
	public void
	moveDown()
	{
		_dispatcher.dispatch( new RPRequest( this, "moveDown", null)).getResponse();
	}
	
	public void
	moveTo(
		int		position )
	{
		_dispatcher.dispatch( new RPRequest( this, "moveTo[int]", new Object[]{new Integer(position )})).getResponse();	
	}
	
	public void stopAndQueue() throws DownloadException {
		notSupported();
	}
	
	public void
	recheckData()
	
		throws DownloadException
	{
		notSupported();
	}
	
	public String getName() {
		notSupported();
		return ("");
	}
	
	public void
	addListener(
		DownloadPeerListener	l )
	{
		notSupported(l);
	}
	
	
	public void
	removeListener(
		DownloadPeerListener	l )
	{
		notSupported(l);
	}
	
	public void
	addPeerListener(
		DownloadPeerListener	l )
	{
		notSupported();
	}
	
	public void
	removePeerListener(
		DownloadPeerListener	l )
	{
		notSupported();
	}
  
  public String getTorrentFileName() {
 		notSupported();
		return ("");
 }
  
  public String
  getAttribute(
  	TorrentAttribute		attribute )
  {
	notSupported();
	return (null);  
  }
  
  public void
  setAttribute(
  	TorrentAttribute		attribute,
	String					value )
  {
  	notSupported();
  }
  
  public String[]
  getListAttribute(
		TorrentAttribute		attribute )
  {
	notSupported();
	return (null);   	
  }
  
  public void
  setMapAttribute(
	TorrentAttribute		attribute,
	Map						value )
  {
	  notSupported();
  }
  
  public Map
  getMapAttribute(
	TorrentAttribute		attribute )
  {
	notSupported();
	return( null );
  }
  
  public String getCategoryName() {
 		notSupported();
		return ("");
  }
  
  public void setCategory(String sName) {
 		notSupported();
  }
  
  public boolean 
  isPersistent() 
  {
 		notSupported();
		return false;
  }
  
	public void
	setMaximumDownloadKBPerSecond(
		int		kb )
 	{
		notSupported();
 	}
  
  public int getUploadRateLimitBytesPerSecond() {
    notSupported();
    return 0;
  }

  public void setUploadRateLimitBytesPerSecond( int max_rate_bps ) {  notSupported();  }
  
	public int getDownloadRateLimitBytesPerSecond() {
	   notSupported();
	    return 0;
  	}

  	public void setDownloadRateLimitBytesPerSecond( int max_rate_bps ) {
		notSupported();
  	}
  	
  	public int
	getMaximumDownloadKBPerSecond()
  	{
  		notSupported();
  		
  		return(0);
  	}
  	
 	public boolean
	isComplete()
 	{
 		notSupported();
 		
 		return( false );
 	}
 	
 	public boolean 
 	isComplete(boolean b)
 	{
		notSupported();
 		
 		return( false );	
 	}
 	
	public boolean
 	isChecking()
	{
 		notSupported();
 		
 		return( false );
 	}
	
	public PeerManager
	getPeerManager()
	{
		notSupported();
		
		return( null );
	}
	
	public DiskManager
	getDiskManager()
	{
		notSupported();
		
		return( null );
	}
	
	
	public DiskManagerFileInfo[]
	getDiskManagerFileInfo()
	{
		RPDiskManagerFileInfo[] resp = (RPDiskManagerFileInfo[])_dispatcher.dispatch( 
				new RPRequest( 
						this, 
						"getDiskManagerFileInfo", 
						null)).getResponse();

		for (int i=0;i<resp.length;i++){
			
			resp[i]._setRemote( _dispatcher );
		}
		
		return( resp );
	}

	public DiskManagerFileInfo
	getDiskManagerFileInfo(int index)
	{
		// TODO: Make it only return the index one

		RPDiskManagerFileInfo[] resp = (RPDiskManagerFileInfo[])_dispatcher.dispatch( 
				new RPRequest( 
						this, 
						"getDiskManagerFileInfo", 
						null)).getResponse();

		if (index >= 0 && index < resp.length) {
			resp[index]._setRemote( _dispatcher );
			return resp[index];
		}
		
		return( null );
	}
	
	public int 
	getDiskManagerFileCount() {
		notSupported();
		return 0;
	}

	public long
	getCreationTime()
	{
		notSupported();
		
		return( 0 );
	}
  
  public int getSeedingRank() {
		notSupported();
		
		return( 0 );
  }
  
 	public String
	getSavePath()
 	{
		notSupported();
		
		return( null );
	}
 	
 	public void
  	moveDataFiles(
  		File	new_parent_dir )
  	
  		throws DownloadException
  	{
 		notSupported();
  	}
 	
 	public void moveDataFiles(File new_parent_dir, String new_name) throws DownloadException {
 		notSupported();
 	}
  	
  	public void
  	moveTorrentFile(
  		File	new_parent_dir )
 	{
 		notSupported();
  	}
  	
 	public void
	requestTrackerAnnounce()
 	{
		_dispatcher.dispatch( new RPRequest( this, "requestTrackerAnnounce", null)).getResponse();	
 	}
 	
	public void
	requestTrackerAnnounce(
		boolean		immediate )
	{
		notSupported();
	}
	
	public void
	requestTrackerScrape(
		boolean		immediate )
	{
		notSupported();
	}
	
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		notSupported();
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
		notSupported();
	}
    
	public DownloadActivationEvent
	getActivationState()
	{
		notSupported();
		
		return( null );
	}
	
	public void
	addActivationListener(
		DownloadActivationListener		l )
	{
		notSupported();
	}
	
	public void
	removeActivationListener(
		DownloadActivationListener		l )
	{
		notSupported();
	}
    
		/* (non-Javadoc)
		 * @see org.gudy.azureus2.plugins.download.Download#setSeedingRank(int)
		 */
		public void setSeedingRank(int rank) {
			// TODO Auto-generated method stub
			
		}

		public void addTrackerListener(DownloadTrackerListener l, boolean immediateTrigger) {
			notSupported();
		}
		
		public void renameDownload(String new_name) {
			notSupported();
		}
		
		public File[] calculateDefaultPaths(boolean for_moving) {
			notSupported();
			return null;
		}
		
		public boolean isInDefaultSaveDir() {notSupported(); return false;}
		
	public boolean getBooleanAttribute(TorrentAttribute ta) {notSupported(); return false;}
	public int getIntAttribute(TorrentAttribute ta) {notSupported(); return 0;}
	public long getLongAttribute(TorrentAttribute ta) {notSupported(); return 0L;}
	public boolean hasAttribute(TorrentAttribute ta) {notSupported(); return false;}
	public void setBooleanAttribute(TorrentAttribute ta, boolean value) {notSupported();}
	public void setIntAttribute(TorrentAttribute ta, int value) {notSupported();}
	public void setListAttribute(TorrentAttribute ta, String[] value) {notSupported();}
	public void setLongAttribute(TorrentAttribute ta, long value) {notSupported();}
	public void setFlag(long flag, boolean set) {notSupported();}
	
	public void addAttributeListener(DownloadAttributeListener l, TorrentAttribute a, int e) {notSupported();}
	public void removeAttributeListener(DownloadAttributeListener l, TorrentAttribute a, int e) {notSupported();}
	
	public void addCompletionListener(DownloadCompletionListener l) {notSupported();}
	public void removeCompletionListener(DownloadCompletionListener l) {notSupported();}
	
	public boolean isRemoved() {notSupported();	return false; }
	public boolean canMoveDataFiles() {notSupported(); return false;}
	public SaveLocationChange calculateDefaultDownloadLocation() {notSupported(); return null;}
	
	public Object getUserData(Object key) {
		notSupported();
		return null;
	}
	
	public void setUserData(Object key, Object data) {
		notSupported();
	}
	
	public void startDownload(boolean force) {notSupported();}
	public void stopDownload() {notSupported();}
	public void changeLocation(SaveLocationChange slc) {notSupported();}
}