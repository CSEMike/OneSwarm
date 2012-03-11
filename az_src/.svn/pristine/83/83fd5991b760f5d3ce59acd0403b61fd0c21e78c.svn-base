/*
 * File    : ShareManagerImpl.java
 * Created : 30-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.sharing;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.AzureusCoreFactory;

public class 
ShareManagerImpl
	implements ShareManager, TOTorrentProgressListener, ParameterListener, AEDiagnosticsEvidenceGenerator
{
	private static final LogIDs LOGID = LogIDs.PLUGIN;
	public static final String		TORRENT_STORE 		= "shares";
	public static final String		TORRENT_SUBSTORE	= "cache";
	
	public static final int			MAX_FILES_PER_DIR	= 1000;
	public static final int			MAX_DIRS			= 1000;
	
	protected static ShareManagerImpl	singleton;
	private static AEMonitor			class_mon	= new AEMonitor( "ShareManager:class" );

	protected AEMonitor				this_mon	= new AEMonitor( "ShareManager" );

	protected TOTorrentCreator		to_creator;
	
	public static ShareManagerImpl
	getSingleton()
	
		throws ShareException
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new ShareManagerImpl();
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	
	private volatile boolean	initialised;
	private volatile boolean	initialising;
	
	private File				share_dir;
	
	private URL[]				announce_urls;
	private ShareConfigImpl		config;
	
	private Map<String,ShareResourceImpl>	shares 		= new HashMap<String, ShareResourceImpl>();
	
	private shareScanner		current_scanner;
	private boolean				scanning;
	
	private List<ShareManagerListener>				listeners	= new ArrayList<ShareManagerListener>();
	
	protected
	ShareManagerImpl()
	
		throws ShareException
	{
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					announce_urls	= null;
				}
			});
		
		AEDiagnostics.addEvidenceGenerator( this );
	}
	
	public void
	initialise()
		throws ShareException
	{
		try{
			this_mon.enter();
		
			if ( !initialised ){
			
				try{
					initialising	= true;
					
					initialised		= true;
					
					share_dir = FileUtil.getUserFile( TORRENT_STORE );
					
					FileUtil.mkdirs(share_dir);
									
					config = new ShareConfigImpl();
					
					try{
						config.suspendSaving();
					
						config.loadConfig(this);
										
					}finally{
					
						Iterator<ShareResourceImpl> it = shares.values().iterator();
						
						while(it.hasNext()){
						
							ShareResourceImpl	resource = it.next();
							
							if ( resource.getType() == ShareResource.ST_DIR_CONTENTS ){
					
								for (int i=0;i<listeners.size();i++){
									
									try{
										
										listeners.get(i).resourceAdded( resource );
										
									}catch( Throwable e ){
										
										Debug.printStackTrace( e );
									}
								}
							}
						}
						
						config.resumeSaving();
					}
					
					readAZConfig();
					
				}finally{
					
					initialising	= false;
					
					new AEThread2( "ShareManager:initScan", true )
					{
						public void
						run()
						{
							try{
								scanShares();
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
							}
						}
					}.start();
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	isInitialising()
	{
		return( initialising );
	}
	
	protected void
	readAZConfig()
	{
		COConfigurationManager.addParameterListener( "Sharing Rescan Enable", this );	
		
		readAZConfigSupport();
	}
	
	public void
	parameterChanged(
		String	name )
	{
		readAZConfigSupport();
	}
	
	protected void
	readAZConfigSupport()
	{
		try{
			this_mon.enter();
		
			boolean	scan_enabled	= COConfigurationManager.getBooleanParameter( "Sharing Rescan Enable" );
						
			if ( !scan_enabled ){
			
				current_scanner	= null;
				
			}else if ( current_scanner == null ){
				
				current_scanner = new shareScanner();
			}
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected ShareConfigImpl
	getShareConfig()
	{
		return( config );
	}
	
	protected void
	checkConsistency()
	
		throws ShareException
	{
			// copy set for iteration as consistency check can delete resource
		
		Iterator<ShareResourceImpl>	it = new HashSet<ShareResourceImpl>(shares.values()).iterator();
		
		while(it.hasNext()){
			
			ShareResourceImpl	resource = it.next();
			
			try{
				resource.checkConsistency();
				
			}catch( ShareException e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	deserialiseResource(
		Map					map )
	{
		try{
			ShareResourceImpl	new_resource = null;
			
			int	type = ((Long)map.get("type")).intValue();
			
			if ( 	type == ShareResource.ST_FILE ||
					type == ShareResource.ST_DIR ){
				
				new_resource = ShareResourceFileOrDirImpl.deserialiseResource( this, map, type );
				
			}else{
				
				new_resource = ShareResourceDirContentsImpl.deserialiseResource( this, map );
			}
			
			if ( new_resource != null ){
				
				ShareResourceImpl	old_resource = shares.get(new_resource.getName());
				
				if ( old_resource != null ){
					
					old_resource.delete(true);
				}
				
				shares.put( new_resource.getName(), new_resource );
				
					// we delay the reporting of dir_contents until all recovery is complete so that
					// the resource reported is initialised correctly
				
				if ( type != ShareResource.ST_DIR_CONTENTS ){
					
					for (int i=0;i<listeners.size();i++){
						
						try{
						
							((ShareManagerListener)listeners.get(i)).resourceAdded( new_resource );
							
						}catch( Throwable e ){
						
							Debug.printStackTrace( e );
						}
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected String
	getNewTorrentLocation()
	
		throws ShareException
	{
		Random rand = new Random(SystemTime.getCurrentTime());
		
		for (int i=1;i<=MAX_DIRS;i++){
			
			String	cache_dir_str = share_dir + File.separator + TORRENT_SUBSTORE + i;
			
			File	cache_dir = new File(cache_dir_str);
			
			if ( !cache_dir.exists()){
				
				FileUtil.mkdirs(cache_dir);
			}
			
			if ( cache_dir.listFiles().length < MAX_FILES_PER_DIR ){
				
				for (int j=0;j<MAX_FILES_PER_DIR;j++){
					
					long	file = Math.abs(rand.nextLong());
			
					File	file_name = new File(cache_dir_str + File.separator + file + ".torrent");
					
					if ( !file_name.exists()){
						
							// return path relative to cache_dir to save space 
						
						return( TORRENT_SUBSTORE + i + File.separator + file + ".torrent" );
					}
				}
			}
		}
		
		throw( new ShareException( "ShareManager: Failed to allocate cache file"));
	}
	
	protected void
	writeTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		try{
			item.getTorrent().writeToFile( getTorrentFile(item ));
			
		}catch( TorrentException e ){
			
			throw( new ShareException( "ShareManager: Torrent write fails", e ));
		}
	}
	
	protected void
	readTorrent(
		ShareItemImpl		item )
	
		throws ShareException
	{
		try{
			TOTorrent torrent = TOTorrentFactory.deserialiseFromBEncodedFile( getTorrentFile(item ));
			
			item.setTorrent(new TorrentImpl(torrent));
			
		}catch( TOTorrentException e ){
			
			throw( new ShareException( "ShareManager: Torrent read fails", e ));
		}
	}
	
	protected void
	deleteTorrent(
		ShareItemImpl		item )
	{
		File	torrent_file = getTorrentFile(item);
				
		torrent_file.delete();
	}
	
	protected boolean
	torrentExists(
		ShareItemImpl		item )
	{		
		return( getTorrentFile(item).exists());
	}
	
	protected File
	getTorrentFile(
		ShareItemImpl		item )
	{
		return( new File(share_dir+File.separator+item.getTorrentLocation()));
	}
	
	protected URL[]
	getAnnounceURLs()
	
		throws ShareException
	{
		if ( announce_urls == null ){
						
			String	protocol = COConfigurationManager.getStringParameter( "Sharing Protocol" );
			
			if ( protocol.equalsIgnoreCase( "DHT" )){
				
				announce_urls	= new URL[]{ TorrentUtils.getDecentralisedEmptyURL()};
				
			}else{
			
				URL[][]	tracker_url_sets = TRTrackerUtils.getAnnounceURLs();
				
				if ( tracker_url_sets.length == 0 ){
					
					throw( new ShareException( "ShareManager: Tracker must be configured"));
				}
			
				for (int i=0;i<tracker_url_sets.length;i++){
				
					URL[]	tracker_urls = tracker_url_sets[i];
			
					if ( tracker_urls[0].getProtocol().equalsIgnoreCase( protocol )){
					
						announce_urls = tracker_urls;
						
						break;
					}
				}
				
				if ( announce_urls == null ){
					
					throw( new ShareException( "ShareManager: Tracker must be configured for protocol '" + protocol + "'" ));
				}
			}
		}
		
		return( announce_urls );
	}
	
	protected boolean
	getAddHashes()
	{
		return( COConfigurationManager.getBooleanParameter( "Sharing Add Hashes" ));
	}
	
	public ShareResource[]
	getShares()
	{
		ShareResource[]	res = new ShareResource[shares.size()];
		
		shares.values().toArray( res );
		
		return( res );
	}
	
	protected ShareResourceImpl
	getResource(
		File		file )
	
		throws ShareException
	{
		try{
			return((ShareResourceImpl)shares.get(file.getCanonicalFile().toString()));
			
		}catch( IOException e ){
			
			throw( new ShareException( "getCanonicalFile fails", e ));
		}
	}
	
	public ShareResource
	getShare(
		File	file_or_dir )
	{
		try{
			return( getResource( file_or_dir ));
			
		}catch( ShareException e ){
						
			return( null );
		}
	}
	
	private boolean
	getBooleanProperty(
		Map<String,String>	properties,
		String				name )
	{
		if ( properties == null ){
			
			return( false );
		}
		
		String	value = properties.get( name );
		
		if ( value == null ){
			
			return( false );
		}
		
		return( value.equalsIgnoreCase( "true" ));
	}
	
	public ShareResourceFile
	addFile(
		File	file )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		return( addFile( file, null ));
	}
	
	public ShareResourceFile
	addFile(
		File				file,
		Map<String,String>	properties )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		return( addFile( null, file, getBooleanProperty( properties, PR_PERSONAL )));
	}
	
	protected ShareResourceFile
	addFile(
		ShareResourceDirContentsImpl	parent,
		File							file,
		boolean							personal )

		throws ShareException, ShareResourceDeletionVetoException
	{
		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "ShareManager: addFile '"
					+ file.toString() + "'"));

		try{
			return( (ShareResourceFile)addFileOrDir( parent, file, ShareResource.ST_FILE, personal ));
			
		}catch( ShareException e ){
			
			reportError(e);
			
			throw(e);
		}
	}
	
	public ShareResourceFile
	getFile(
		File	file )
	
		throws ShareException
	{
		return( (ShareResourceFile)ShareResourceFileImpl.getResource( this, file ));
	}
	
	public ShareResourceDir
	addDir(
		File				dir )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		return( addDir( dir, null ));
	}
	
	public ShareResourceDir
	addDir(
		File				dir,
		Map<String,String>	properties )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		return( addDir( null, dir, getBooleanProperty( properties, PR_PERSONAL )));
	}
	
	public ShareResourceDir
	addDir(
		ShareResourceDirContentsImpl	parent,
		File							dir,
		boolean							personal )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "ShareManager: addDir '" + dir.toString()
					+ "'"));

		try{
			this_mon.enter();
			
			return( (ShareResourceDir)addFileOrDir( parent, dir, ShareResource.ST_DIR, personal ));
			
		}catch( ShareException e ){
			
			reportError(e);
			
			throw(e);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public ShareResourceDir
	getDir(
		File	file )
	
		throws ShareException
	{
		return( (ShareResourceDir)ShareResourceDirImpl.getResource( this, file ));
	}
	
	protected ShareResource
	addFileOrDir(
		ShareResourceDirContentsImpl	parent,
		File							file,
		int								type,
		boolean							personal )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		try{
			this_mon.enter();
		
			String	name = file.getCanonicalFile().toString();
			
			ShareResourceImpl	old_resource = shares.get(name);
			
			boolean	modified = old_resource != null;
			
			if ( modified ){
		
				old_resource.delete( true, false );
			}
			
			ShareResourceImpl new_resource;
			
			if ( type == ShareResource.ST_FILE ){
		
				reportCurrentTask( "Adding file '" + name + "'");
				
				new_resource = new ShareResourceFileImpl( this, parent, file, personal );
				
			}else{
				
				reportCurrentTask( "Adding dir '" + name + "'");
				
				new_resource = new ShareResourceDirImpl( this, parent, file, personal );
			}
			
			shares.put(name, new_resource );
			
			config.saveConfig();
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					
					if ( modified ){
						
						((ShareManagerListener)listeners.get(i)).resourceModified( old_resource, new_resource );
					
					}else{
						
						((ShareManagerListener)listeners.get(i)).resourceAdded( new_resource );				
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			
			return( new_resource );
			
		}catch( IOException e ){
			
			throw( new ShareException( "getCanoncialFile fails", e ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public ShareResourceDirContents
	addDirContents(
		File				dir,
		boolean				recursive )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		return( addDirContents( dir, recursive, null ));
	}
	
	public ShareResourceDirContents
	addDirContents(
		File				dir,
		boolean				recursive,
		Map<String,String>	properties )
	
		throws ShareException, ShareResourceDeletionVetoException
	{
		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "ShareManager: addDirContents '"
					+ dir.toString() + "'"));

		try{
			this_mon.enter();
			
			String	name = dir.getCanonicalFile().toString();
			
			reportCurrentTask( "Adding dir contents '" + name + "', recursive = " + recursive );
	
			ShareResource	old_resource = (ShareResource)shares.get( name );
			
			if ( old_resource != null ){
				
				old_resource.delete( true );
			}

			ShareResourceDirContentsImpl new_resource = new ShareResourceDirContentsImpl( this, dir, recursive, getBooleanProperty( properties, PR_PERSONAL ), true );
						
			shares.put( name, new_resource );
			
			config.saveConfig();
			
			for (int i=0;i<listeners.size();i++){
				
				try{
					
					((ShareManagerListener)listeners.get(i)).resourceAdded( new_resource );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			
			return( new_resource );
			
		}catch( IOException e ){
			
			reportError(e);
			
			throw( new ShareException( "getCanoncialFile fails", e ));
			
		}catch( ShareException e ){
			
			reportError(e);
			
			throw(e);
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	protected void
	delete(
		ShareResourceImpl	resource,
		boolean				fire_listeners )
	
		throws ShareException
	{
		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "ShareManager: resource '"
					+ resource.getName() + "' deleted"));
		
		try{
			this_mon.enter();
		
			shares.remove(resource.getName());
			
			resource.deleteInternal();
			
			config.saveConfig();
			
			if ( fire_listeners ){
				
				for (int i=0;i<listeners.size();i++){
					
					try{
						
						((ShareManagerListener)listeners.get(i)).resourceDeleted( resource );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	scanShares()
	
		throws ShareException
	{
		try{
			this_mon.enter();

			if ( scanning ){
				
				return;
			}
			
			scanning = true;
			
		}finally{
			
			this_mon.exit();
		}
		
		try{
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID,
						"ShareManager: scanning resources for changes"));
	
			checkConsistency();
			
		}finally{
			
			try{
				this_mon.enter();

				scanning = false;
				
			}finally{
				
				this_mon.exit();
			}
		}
	}
	
		// bit of a hack this, but to do it properly would require extensive rework to decouple the
		// process of saying "share file" and then actually doing it 
	
	protected  void
	setTorrentCreator(
		TOTorrentCreator	_to_creator )
	{
		to_creator	= _to_creator;
	}
	
	public void
	cancelOperation()
	{
		TOTorrentCreator	temp = to_creator;
		
		if ( temp != null ){
			
			temp.cancel();
		}
	}
	
	public void
	reportProgress(
		int		percent_complete )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				((ShareManagerListener)listeners.get(i)).reportProgress( percent_complete );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}		
	}
	
	public void
	reportCurrentTask(
		String	task_description )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				
				((ShareManagerListener)listeners.get(i)).reportCurrentTask( task_description );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}			
	}

	protected void
	reportError(
		Throwable e )
	{
		String	message = e.getMessage();
		
		if ( message != null ){
			
			reportCurrentTask( Debug.getNestedExceptionMessage(e));
			
		}else{
			
			reportCurrentTask( e.toString());
		}
	}
	public void
	addListener(
		ShareManagerListener		l )
	{
		listeners.add(l);	
	}
	
	public void
	removeListener(
		ShareManagerListener		l )
	{
		listeners.remove(l);
	}
	
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "Shares" );
			
		try{
			writer.indent();

			ShareResource[]	shares = getShares();
			
			HashSet	share_map = new HashSet();
						
			for ( int i=0;i<shares.length;i++ ){
				
				ShareResource	share = shares[i];
									
				if ( share instanceof ShareResourceDirContents ){
					
					share_map.add( share );
					
				}else if ( share.getParent() != null ){
					
				}else{
					
					writer.println( getDebugName( share ));
				}
			}
			
			Iterator	it = share_map.iterator();
	
			// We don't need GlobalManager, so isCoreRunning isn't needed
			// Hopefully all the things we need are avail on core create
			if (!AzureusCoreFactory.isCoreAvailable()) {
				// could probably log some stuff below, but for now
				// be safe and lazy and just exit
				writer.println("No Core");
				return;
			}
			
			TorrentManager tm = PluginInitializer.getDefaultInterface().getTorrentManager();

			TorrentAttribute	category_attribute 	= tm.getAttribute( TorrentAttribute.TA_CATEGORY );
			TorrentAttribute	props_attribute 	= tm.getAttribute( TorrentAttribute.TA_SHARE_PROPERTIES );
			
			while( it.hasNext()){
				
				ShareResourceDirContents	root = (ShareResourceDirContents)it.next();
				
				String	cat 	= root.getAttribute( category_attribute );
				String	props 	= root.getAttribute( props_attribute );
				
				String	extra = cat==null?"":(",cat=" + cat );
				
				extra += props==null?"":(",props=" + props );
				
				extra += ",rec=" + root.isRecursive();
				
				writer.println( root.getName() + extra );
				
				generate( writer, root );
			}
		}finally{
			
			writer.exdent();
		}
	}
	
	protected void
	generate(
		IndentWriter				writer,		
		ShareResourceDirContents	node )
	{
		try{
			writer.indent();

			ShareResource[]	kids = node.getChildren();
			
			for (int i=0;i<kids.length;i++){
				
				ShareResource	kid = kids[i];
				
				writer.println( getDebugName( kid ));
	
				if ( kid instanceof ShareResourceDirContents ){
					
					generate( writer, (ShareResourceDirContents)kid );
				}
			}
		}finally{
			
			writer.exdent();
		}
	}
	
	protected String
	getDebugName(
		ShareResource	_share )
	{
		Torrent	torrent = null;
		
		try{
			if ( _share instanceof ShareResourceFile ){
				
				ShareResourceFile share = (ShareResourceFile)_share;
				
				torrent = share.getItem().getTorrent();
				
			}else if ( _share instanceof ShareResourceDir ){
				
				ShareResourceDir share = (ShareResourceDir)_share;
				
				torrent = share.getItem().getTorrent();
			}
		}catch( Throwable e ){			
		}
		
		if ( torrent == null ){
			
			return(	Debug.secretFileName( _share.getName()));
			
		}else{
			
			return( Debug.secretFileName( torrent.getName() ) + "/" + ByteFormatter.encodeString( torrent.getHash()));
		}
	}
	
	
	protected class
	shareScanner
	{
		boolean	run = true;
		
		protected
		shareScanner()
		{
			current_scanner	= this;
			
			new AEThread2( "ShareManager::scanner", true )
			{
				public void
				run()
				{
					while( current_scanner == shareScanner.this ){
					
						try{
							
							int		scan_period		= COConfigurationManager.getIntParameter( "Sharing Rescan Period" );

							if ( scan_period < 1 ){
								
								scan_period	= 1;
							}
							
							Thread.sleep( scan_period * 1000 );
							
							if ( current_scanner == shareScanner.this ){
								
								scanShares();
							}

						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}
			}.start();
		}
	}
}
