/*
 * File    : DownloadManagerImpl.java
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
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerInitialisationAdapter;
import org.gudy.azureus2.core3.download.impl.DownloadManagerDefaultPaths;
import org.gudy.azureus2.core3.download.impl.DownloadManagerMoveHandler;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadWillBeRemovedListener;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.download.savelocation.DefaultSaveLocationManager;
import org.gudy.azureus2.plugins.download.savelocation.SaveLocationManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.util.CopyOnWriteList;


public class 
DownloadManagerImpl
	implements org.gudy.azureus2.plugins.download.DownloadManager, DownloadManagerInitialisationAdapter
{
	protected static DownloadManagerImpl	singleton;
	protected static AEMonitor				class_mon	= new AEMonitor( "DownloadManager:class");
	
	public static DownloadManagerImpl
	getSingleton(
		AzureusCore	azureus_core )
	{
		try{
			class_mon.enter();
	
			if ( singleton == null ){
				
				singleton = new DownloadManagerImpl( azureus_core );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	//private AzureusCore				azureus_core;
	private GlobalManager			global_manager;
	private DownloadManagerStats	stats;
	private DownloadEventNotifierImpl global_dl_notifier;
	
	private List			listeners		= new ArrayList();
	private CopyOnWriteList	dwba_listeners	= new CopyOnWriteList();
	private AEMonitor		listeners_mon	= new AEMonitor( "DownloadManager:L");
	
	private List			downloads		= new ArrayList();
	private Map				pending_dls		= new HashMap();
	private Map				download_map	= new HashMap();
		
	protected
	DownloadManagerImpl(
		AzureusCore	_azureus_core )
	{
		//azureus_core	= _azureus_core;
		global_manager	= _azureus_core.getGlobalManager();
		
		stats = new DownloadManagerStatsImpl( global_manager );
		global_dl_notifier = new DownloadEventNotifierImpl(this);
		
		global_manager.addListener(
			new GlobalManagerListener()
			{
				public void
				downloadManagerAdded(
					DownloadManager	dm )
				{
					addDownloadManager( dm );
				}
				
				public void
				downloadManagerRemoved(
					DownloadManager	dm )
				{
					List			listeners_ref	= null;
					DownloadImpl	dl				= null;
					
					try{
						listeners_mon.enter();
						
						dl = (DownloadImpl)download_map.get( dm );
						
						if ( dl == null ){
							
							System.out.println( "DownloadManager:unknown manager removed");
							
						}else{
						
							downloads.remove( dl );
							
							download_map.remove( dm );
							
							pending_dls.remove( dm );
							
							dl.destroy();
						
							listeners_ref = listeners;
						}
						
					}finally{
						
						listeners_mon.exit();
					}
					
					if ( dl != null ){
							
						for (int i=0;i<listeners_ref.size();i++){
								
							((DownloadManagerListener)listeners_ref.get(i)).downloadRemoved( dl );
						}
					}
				}
				
				public void
				destroyInitiated()
				{
				}				
				
				public void
				destroyed()
				{	
				}
                
                
                public void seedingStatusChanged( boolean seeding_only_mode, boolean b ){
                  //TODO
                }           
			});
		
		global_manager.addDownloadWillBeRemovedListener(
			new GlobalManagerDownloadWillBeRemovedListener()
			{
				public void
				downloadWillBeRemoved(
					DownloadManager	dm,
					boolean remove_torrent,
					boolean remove_data )
				
					throws GlobalManagerDownloadRemovalVetoException
				{					
					DownloadImpl	download = (DownloadImpl)download_map.get( dm );
				
					if ( download != null ){
					
						try{ 
							download.isRemovable();
							
						}catch( DownloadRemovalVetoException e ){
													
							throw( new GlobalManagerDownloadRemovalVetoException( e.getMessage(),e.isSilent()));
						}		
					}
				}
			});
	}
	
	public void 
	addDownload(
		final File fileName ) 
	{
		UIManagerImpl.fireEvent( null, UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE, fileName );
	}

	public void 
	addDownload(
		final URL	url) 
	{
		addDownload(url,null,true,null);
	}
	
	public void 
	addDownload(
		URL		url,
		boolean	auto_download )
	
		throws DownloadException
	{
		addDownload(url,null,auto_download,null);	
	}
	
	public void 
	addDownload(
		final URL	url,
		final URL 	referrer) 
	{
		addDownload(url,referrer,true,null);
	}
	
	public void 
	addDownload(
		URL 		url, 
		Map 		request_properties ) 
	{
		addDownload(url,null,true,request_properties);
	}
	
	public void 
	addDownload(
		final URL	url,
		final URL 	referrer,
		boolean		auto_download,
		Map			request_properties )
	{
		UIManagerImpl.fireEvent( null, UIManagerEvent.ET_OPEN_TORRENT_VIA_URL, new Object[]{ url, referrer, new Boolean( auto_download ), request_properties });
	}
	

	protected void
	addDownloadManager(
		DownloadManager	dm )
	{
		List			listeners_ref 	= null;
		DownloadImpl	dl				= null;
		
		try{
			listeners_mon.enter();
			
			if ( download_map.get(dm) == null ){
	
				dl = (DownloadImpl)pending_dls.remove( dm );
				
				if ( dl == null ){
					
					dl = new DownloadImpl(dm);
				}
				
				downloads.add( dl );
				
				download_map.put( dm, dl );
				
				listeners_ref = listeners;
			}
		}finally{
			
			listeners_mon.exit();
		}
		
		if ( dl != null ){
			
			for (int i=0;i<listeners_ref.size();i++){
					
				try{
					((DownloadManagerListener)listeners_ref.get(i)).downloadAdded( dl );
					
				}catch( Throwable e ){
						
					Debug.printStackTrace( e );
				}
			}
		}
	}
	
	public Download
	addDownload(
		Torrent		torrent )
	
		throws DownloadException
	{	 
	    return( addDownload( torrent, null, null ));
	}
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{
		return( addDownload( torrent, torrent_file, data_location, getInitialState()));
	}
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location,
		int			initial_state )
	
		throws DownloadException
	{
		if ( torrent_file == null ){
			
		    String torrent_dir = null;
		    
		    if( COConfigurationManager.getBooleanParameter("Save Torrent Files")){
		    	
		      try{
		      	
		      	torrent_dir = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
		        
		      }catch(Exception egnore){}
		    }
		    
		    if ( torrent_dir == null || torrent_dir.length() == 0 ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: default torrent save directory must be configured" ));
		    }
		
		    torrent_file = new File( torrent_dir + File.separator + torrent.getName() + ".torrent" );
		    
		    try{
		    	torrent.writeToFile( torrent_file );
		    	
		    }catch( TorrentException e ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: failed to write torrent to '" + torrent_file.toString() + "'", e ));	    	
		    }
		}
		
		else {
			if (!torrent_file.exists()) {
				throw new DownloadException("DownloadManager::addDownload: torrent file does not exist - " + torrent_file.toString()); 
			}
			else if (!torrent_file.isFile()) {
				throw new DownloadException("DownloadManager::addDownload: torrent filepath given is not a file - " + torrent_file.toString());
			}
		}
		
		if ( data_location == null ){
			
		    String data_dir = COConfigurationManager.getStringParameter("Default save path");
		    
		    if ( data_dir == null || data_dir.length() == 0 ){
		    	
		    	throw( new DownloadException("DownloadManager::addDownload: default data save directory must be configured" ));
		    }
		    
		    data_location = new File(data_dir); 
		    
		    FileUtil.mkdirs(data_location);
		}

		byte[] hash = null;
		try {
			hash = torrent.getHash();
		} catch (Exception e) { }
		
		boolean	for_seeding = torrent.isComplete();
		
		DownloadManager dm = global_manager.addDownloadManager(
				torrent_file.toString(), hash, data_location.toString(),
				initial_state, true, for_seeding, null );
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed, download may already in the process of being added"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}

	public Download
	addDownloadStopped(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException
	{
		return( addDownload( torrent, torrent_location, data_location, DownloadManager.STATE_STOPPED ));
	}
	
	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_file,
		File		data_location )
	
		throws DownloadException
	{

		byte[] hash = null;
		try {
			hash = torrent.getHash();
		} catch (Exception e) { }

		DownloadManager dm = global_manager.addDownloadManager(
				torrent_file.toString(), hash, data_location.toString(),
				getInitialState(), false);
		
		if ( dm == null ){
			
			throw( new DownloadException( "DownloadManager::addDownload - failed"));
		}
		
		addDownloadManager( dm );
		
		return( getDownload( dm ));
	}
	
	protected int
	getInitialState()
	{
	  	boolean	default_start_stopped = COConfigurationManager.getBooleanParameter( "Default Start Torrents Stopped" );

        return( default_start_stopped?DownloadManager.STATE_STOPPED:DownloadManager.STATE_WAITING);
	}
	
	protected DownloadImpl
	getDownload(
		DownloadManager	dm )
	
		throws DownloadException
	{
		DownloadImpl dl = (DownloadImpl)download_map.get(dm);
		
		if ( dl == null ){
			
			throw( new DownloadException("DownloadManager::getDownload: download not found"));
		}
		
		return( dl );
	}
	
	public static DownloadImpl[] getDownloadStatic(DownloadManager[] dm) {
		ArrayList res = new ArrayList(dm.length);
		for (int i=0; i<dm.length; i++) {
			try {res.add(getDownloadStatic(dm[i]));}
			catch (DownloadException de) {}
		}
		return (DownloadImpl[])res.toArray(new DownloadImpl[res.size()]);
	}

	/**
	 * Retrieve the plugin Downlaod object related to the DownloadManager
	 * 
	 * @param dm DownloadManager to find
	 * @return plugin object
	 * @throws DownloadException
	 */
	public static DownloadImpl
	getDownloadStatic(
		DownloadManager	dm )
	
		throws DownloadException
	{
		if ( singleton != null ){
			
			return( singleton.getDownload( dm ));
		}
		
		throw( new DownloadException( "DownloadManager not initialised"));
	}
	
	public static Download
	getDownloadStatic(
		DiskManager	dm )
	
		throws DownloadException
	{
		if ( singleton != null ){
			
			return( singleton.getDownload( dm ));
		}
		
		throw( new DownloadException( "DownloadManager not initialised"));
	}
	
	public Download
	getDownload(
		DiskManager	dm )
	
		throws DownloadException
	{
		List	dls = global_manager.getDownloadManagers();

		for (int i=0;i<dls.size();i++){
			
			DownloadManager	man = (DownloadManager)dls.get(i);
			
			if ( man.getDiskManager() == dm ){
				
				return( getDownload( man.getTorrent()));
			}
		}
		
		return( null );
	}

	protected Download
	getDownload(
		TOTorrent	torrent )
	
		throws DownloadException
	{
		if ( torrent != null ){
			
			for (int i=0;i<downloads.size();i++){
				
				Download	dl = (Download)downloads.get(i);
				
				TorrentImpl	t = (TorrentImpl)dl.getTorrent();
				
					// can be null if broken torrent
				
				if ( t == null ){
					
					continue;
				}
				
				if ( t.getTorrent().hasSameHashAs( torrent )){
					
					return( dl );
				}
			}
		}
		
		throw( new DownloadException("DownloadManager::getDownload: download not found"));
	}
	
	public static Download
	getDownloadStatic(
		TOTorrent	torrent )
	
		throws DownloadException
	{
		if ( singleton != null ){
			
			return( singleton.getDownload( torrent ));
		}
		
		throw( new DownloadException( "DownloadManager not initialised"));
	}
	
	public Download
	getDownload(
		Torrent		_torrent )
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		try{
			return( getDownload( torrent.getTorrent()));
			
		}catch( DownloadException e ){
		}
		
		return( null );
	}
	
	public Download
	getDownload(
		byte[]	hash )
	{
		DownloadManager manager = global_manager.getDownloadManager(new HashWrapper(hash));
		if (manager != null) {
			try {
				return getDownload(manager);
			} catch (DownloadException e) {
			}
		}

		List	dls = global_manager.getDownloadManagers();
		
		for (int i=0;i<dls.size();i++){
			
			DownloadManager	man = (DownloadManager)dls.get(i);
			
				// torrent can be null if download manager torrent file read fails
			
			TOTorrent	torrent = man.getTorrent();
			
			if ( torrent != null ){
				
				try{
					if ( Arrays.equals( torrent.getHash(), hash )){
				
						return( getDownload( torrent ));
					}
				}catch( DownloadException e ){
					
						// not found
					
				}catch( TOTorrentException e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
		
		return( null );
	}
	
	public Download[]
	getDownloads()
	{
		Set	res_l = new LinkedHashSet();
	
		// we have to use the global manager's ordering as it
		// hold this

		List dms = global_manager.getDownloadManagers();

		try{
			listeners_mon.enter();

			for (int i=0;i<dms.size();i++){
			
				Object	dl = download_map.get( dms.get(i));
				
				if ( dl != null ){
					
					res_l.add( dl );
				}
			}
			
			if ( res_l.size() < downloads.size()){
				
					// now add in any external downloads 
				
				for (int i=0;i<downloads.size();i++){
					
					Download	download = (Download)downloads.get(i);
			
					if ( !res_l.contains( download )){
						
						res_l.add( download );
					}
				}
			}
		}finally{
			
			listeners_mon.exit();
		}
		
		Download[]	res = new Download[res_l.size()];
			
		res_l.toArray( res );
			
		return( res );
	}
	
	public Download[]
	getDownloads(boolean bSorted)
	{
		if (bSorted){
	  
			return getDownloads();
		}
	  
		try{
			listeners_mon.enter();
		
			Download[]	res = new Download[downloads.size()];
			
			downloads.toArray( res );
			
			return( res );
			
		}finally{
			
			listeners_mon.exit();
		}
	}

	public void
	pauseDownloads()
	{
		global_manager.pauseDownloads();
	}
	
	public boolean
	canPauseDownloads()
	{
		return global_manager.canPauseDownloads();
	}
		
	public void
	resumeDownloads()
	{
		global_manager.resumeDownloads();
	}
	
	public boolean
	canResumeDownloads()
	{
		return global_manager.canResumeDownloads();
	}
		
	public void
	startAllDownloads()
	{
		global_manager.startAllDownloads();
	}
		
	public void
	stopAllDownloads()
	{
		global_manager.stopAllDownloads();
	}
	
	public DownloadManagerStats
	getStats()
	{
		return( stats );
	}
	
	public boolean
	isSeedingOnly()
	{
		return( global_manager.isSeedingOnly());
	}
	
	public void addListener(DownloadManagerListener l) {addListener(l, true);}
	
	public void addListener(DownloadManagerListener l, boolean notify_of_current_downloads) {
		List downloads_copy = null;

		try {
			listeners_mon.enter();
			List new_listeners = new ArrayList(listeners);
			new_listeners.add(l);
			listeners = new_listeners;
			if (notify_of_current_downloads) {
				downloads_copy = new ArrayList(downloads);
				// randomize list so that plugins triggering dlm-state fixups don't lock each other by doing everything in the same order
				Collections.shuffle(downloads_copy);
			}
		}
		finally {
			listeners_mon.exit();
		}

		if (downloads_copy != null) {
			for (int i = 0; i < downloads_copy.size(); i++) {
				try {l.downloadAdded((Download) downloads_copy.get(i));}
				catch (Throwable e) {Debug.printStackTrace(e);}
			}
		}
	}
	
	public void removeListener(DownloadManagerListener l) {removeListener(l, false);}

	public void removeListener(DownloadManagerListener l, boolean notify_of_current_downloads) {
		List downloads_copy = null;
		
		try {
			listeners_mon.enter();
			List new_listeners = new ArrayList(listeners);
			new_listeners.remove(l);
			listeners = new_listeners;
			if (notify_of_current_downloads) {
				downloads_copy = new ArrayList(downloads);
			}
		}
		finally {
			listeners_mon.exit();
		}

		if (downloads_copy != null) {
			for (int i = 0; i < downloads_copy.size(); i++) {
				try {l.downloadRemoved((Download) downloads_copy.get(i));}
				catch (Throwable e) {Debug.printStackTrace(e);}
			}
		}
	
	}
	
	public void
	initialised(
		DownloadManager		manager )
	{
		DownloadImpl	dl;
		
		try{
			listeners_mon.enter();
			
			dl = new DownloadImpl( manager );
			
			pending_dls.put( manager, dl );
			
		}finally{
			
			listeners_mon.exit();
		}
		
		Iterator	it = dwba_listeners.iterator();
		
		while( it.hasNext()){
			
			try{
				((DownloadWillBeAddedListener)it.next()).initialised(dl);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addDownloadWillBeAddedListener(
		DownloadWillBeAddedListener		listener )
	{
		try{
			listeners_mon.enter();
			
			dwba_listeners.add( listener );
			
			if ( dwba_listeners.size() == 1 ){
				
				global_manager.addDownloadManagerInitialisationAdapter( this );
			}
			
		}finally{
			listeners_mon.exit();
		}	
	}
	
	public void
	removeDownloadWillBeAddedListener(
		DownloadWillBeAddedListener		listener )
	{
		try{
			listeners_mon.enter();
			
			dwba_listeners.remove( listener );
			
			if ( dwba_listeners.size() == 0 ){
				
				global_manager.removeDownloadManagerInitialisationAdapter( this );
			}
			
		}finally{
			listeners_mon.exit();
		}	
	}
	
	public void
	addExternalDownload(
		Download	download )
	{
		List			listeners_ref 	= null;
		
		try{
			listeners_mon.enter();
			
			if ( downloads.contains( download )){
	
				return;
			}
	
			downloads.add( download );
								
			listeners_ref = listeners;
			
		}finally{
			
			listeners_mon.exit();
		}
					
		for (int i=0;i<listeners_ref.size();i++){
				
			try{
				((DownloadManagerListener)listeners_ref.get(i)).downloadAdded( download );
				
			}catch( Throwable e ){
					
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	removeExternalDownload(
		Download	download )
	{
		List			listeners_ref 	= null;
		
		try{
			listeners_mon.enter();
			
			if ( !downloads.contains( download )){
	
				return;
			}
	
			downloads.remove( download );
								
			listeners_ref = listeners;
			
		}finally{
			
			listeners_mon.exit();
		}
					
		for (int i=0;i<listeners_ref.size();i++){
				
			try{
				((DownloadManagerListener)listeners_ref.get(i)).downloadRemoved( download );
				
			}catch( Throwable e ){
					
				Debug.printStackTrace( e );
			}
		}
	}
	
	public DownloadEventNotifier getGlobalDownloadEventNotifier() {
		return this.global_dl_notifier;
	}
	
	public void setSaveLocationManager(SaveLocationManager manager) {
		if (manager == null) {manager = getDefaultSaveLocationManager();}
		DownloadManagerMoveHandler.CURRENT_HANDLER = manager;				
	}
	
	public SaveLocationManager getSaveLocationManager() {
		return DownloadManagerMoveHandler.CURRENT_HANDLER;
	}	

	public DefaultSaveLocationManager getDefaultSaveLocationManager() {
		return DownloadManagerDefaultPaths.DEFAULT_HANDLER;
	}	
	
}
