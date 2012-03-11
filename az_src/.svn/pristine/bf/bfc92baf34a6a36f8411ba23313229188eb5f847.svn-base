/*
 * File    : RPPluginInterface.java
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

package org.gudy.azureus2.pluginsimpl.remote;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTManager;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.platform.PlatformManager;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;

import org.gudy.azureus2.core3.util.Constants;

import org.gudy.azureus2.pluginsimpl.remote.download.*;
import org.gudy.azureus2.pluginsimpl.remote.ipfilter.*;
import org.gudy.azureus2.pluginsimpl.remote.torrent.*;
import org.gudy.azureus2.pluginsimpl.remote.utils.*;
import org.gudy.azureus2.pluginsimpl.remote.tracker.*;

public class 
RPPluginInterface
	extends		RPObject
	implements 	PluginInterface
{
	protected static long		connection_id_next		= new Random().nextLong();

	protected transient PluginInterface		delegate;
	protected transient long				request_id_next;
		
	// don't change these field names as they are visible on XML serialisation
	
	public String			azureus_name		= Constants.AZUREUS_NAME;
	public String			azureus_version		= Constants.AZUREUS_VERSION;

	// **** Don't try using AEMOnitor for synchronisations here as this object is serialised
	
	public static RPPluginInterface
	create(
		PluginInterface		_delegate )
	{
		RPPluginInterface	res =(RPPluginInterface)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPPluginInterface( _delegate );
		}
		
		return( res );
	}	
	
	public long	_connection_id;
	
	protected
	RPPluginInterface(
		PluginInterface		_delegate )
	{
		super( _delegate );
		
		synchronized( RPPluginInterface.class ){
			
			_connection_id = connection_id_next++;
			
				// avoid 0 as it has special meaning (-> no connection for singleton calls);
			
			if ( _connection_id == 0 ){
				
				_connection_id = connection_id_next++;
			}
		}
	}
	
	protected long
	_getConectionId()
	{
		return( _connection_id );
	}
	
	protected long
	_getNextRequestId()
	{
		synchronized( this ){
		
			return( request_id_next++ );
		}
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (PluginInterface)_delegate;
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "getPluginProperties")){
				
				// must copy properties as actual return is subtype + non serialisable
			
			Properties p = new Properties();
			
			Properties x = delegate.getPluginProperties();
			
			Iterator	it = x.keySet().iterator();
			
			while(it.hasNext()){
				
				Object	key = it.next();
				
				p.put( key, x.get(key));
			}
			
			return( new RPReply( p ));
			
		}else if ( method.equals( "getDownloadManager")){
			
			return( new RPReply( RPDownloadManager.create(delegate.getDownloadManager())));
		
		}else if ( method.equals( "getTorrentManager")){
			
			return( new RPReply( RPTorrentManager.create(delegate.getTorrentManager())));
		
		}else if ( method.equals( "getPluginconfig")){
			
			return( new RPReply( RPPluginConfig.create(delegate.getPluginconfig())));
			
		}else if ( method.equals( "getIPFilter")){
			
			return( new RPReply( RPIPFilter.create(delegate.getIPFilter())));
			
		}else if ( method.equals( "getShortCuts")){
			
			return( new RPReply( RPShortCuts.create(delegate.getShortCuts())));
			
		}else if ( method.equals( "getTracker")){
			
			return( new RPReply( RPTracker.create(delegate.getTracker())));
		}
			
		throw( new RPException( "Unknown method: " + method ));
	}
	
		// ******************************************
	
	public PluginManager
	getPluginManager()
	{
		notSupported();
		
		return( null );
	}
	
	public Plugin
	getPlugin()
	{
		notSupported();
		
		return( null );
	}
	
	public String
	getAzureusName()
	{
		return( azureus_name );
	}
	
	public String
	getAzureusVersion()
	{
		return( azureus_version );
	}
	
  	public String getApplicationName() {
  		return Constants.APP_NAME;
  	}
	
	public void addConfigUIParameters(Parameter[] parameters, String displayName)
	{
		notSupported();
	}

	public void addConfigSection(ConfigSection tab)
	{
		notSupported();
	}
	
	public void removeConfigSection(ConfigSection tab)
	{
		notSupported();
	}
	
	public Tracker 
	getTracker()
	{
		RPTracker	res = (RPTracker)_dispatcher.dispatch( new RPRequest( this, "getTracker", null )).getResponse();
		
		res._setRemote( _dispatcher );
			
		return( res );	
	}
	
	public Logger getLogger()
	{
		notSupported();
		
		return( null );
	}
	
	public IPFilter 
	getIPFilter()
	{
		RPIPFilter	res = (RPIPFilter)_dispatcher.dispatch( new RPRequest( this, "getIPFilter", null )).getResponse();
		
		res._setRemote( _dispatcher );
			
		return( res );	
	}
	
	public DownloadManager
	getDownloadManager()
	{
		RPDownloadManager	res = (RPDownloadManager)_dispatcher.dispatch( new RPRequest( this, "getDownloadManager", null )).getResponse();
	
		res._setRemote( _dispatcher );
		
		return( res );
	}
	
	
	public ShareManager
	getShareManager()
	
		throws ShareException
	{
		notSupported();
		
		return( null );
	}
	
	public Utilities
	getUtilities()
	{
		notSupported();
		
		return( null );
	}
	  
	public ShortCuts
	getShortCuts()
	{
		RPShortCuts	res = (RPShortCuts)_dispatcher.dispatch( new RPRequest( this, "getShortCuts", null )).getResponse();
		
		res._setRemote( _dispatcher );
			
		return( res );		
	}
	
	 public UIManager
	 getUIManager()
	 {
		notSupported();
		
		return( null );	 	
	 }
	 
	 public TorrentManager
	 getTorrentManager()
	 {
		RPTorrentManager	res = (RPTorrentManager)_dispatcher.dispatch( new RPRequest( this, "getTorrentManager", null )).getResponse();
		
		res._setRemote( _dispatcher );
			
		return( res );
	 }
	 
	/**
	 * @deprecated
	 */
	
	public void openTorrentFile(String fileName)
	{
		notSupported();
	}
	
	/**
	 * @deprecated
	 */
	
	public void openTorrentURL(String url)
	{
		notSupported();
	}
	
	public Properties getPluginProperties()
	{
		return((Properties)_dispatcher.dispatch( new RPRequest( this, "getPluginProperties", null )).getResponse());
	}
	
	public String getPluginDirectoryName()
	{
		notSupported();
		
		return( null );
	}
	
	public String getPerUserPluginDirectoryName()
	{
		notSupported();
		
		return( null );
	}

	public boolean
	isShared()
	{
		notSupported();
		
		return( false );
	}
	
    public String getPluginName()
	{
		notSupported();
		
		return( null );
	}
    
    public String getPluginID()
	{
		notSupported();
		
		return( null );
	}
    
    public boolean isMandatory()
	{
		notSupported();
		
		return( false );
	}
    
    public boolean
	isBuiltIn()
	{
		notSupported();
		
		return( false );
	}
    
    public boolean
	isSigned()
	{
		notSupported();
		
		return( false );
	}
    
    public boolean isOperational()
	{
		notSupported();
		
		return( false );
	}
    
	public void
	setDisabled(
		boolean	disabled )
	{
		notSupported();
	}
	  
	public boolean
	isDisabled()
	{
		notSupported();
		
		return( false );
	}
	
    public String getPluginVersion()
	{
		notSupported();
		
		return( null );
	}
    
	public PluginConfig getPluginconfig()
	{
		RPPluginConfig	res = (RPPluginConfig)_dispatcher.dispatch( new RPRequest( this, "getPluginconfig", null )).getResponse();
		
		res._setRemote( _dispatcher );
			
		return( res );
	}
	
	public PluginConfigUIFactory getPluginConfigUIFactory()
	{
		notSupported();
		
		return( null );
	}
	
	public ClassLoader
	getPluginClassLoader()
	{
		notSupported();
		
		return( null );
	}
	
	public PluginInterface
	getLocalPluginInterface(
		Class		plugin,
		String		id )
	{
		notSupported();
		
		return( null );
	}
	
	public IPCInterface
	getIPC ()
	{
		notSupported();
		
		return( null );
	}
	
	public UpdateManager
	getUpdateManager()
	{
		notSupported();
		
		return( null );
	}

	
	public boolean
	isUnloadable()
	{
		notSupported();
		
		return( false );
	}
	
	public void
	unload()
	  
		throws PluginException
	{
		notSupported();
	}
	
	public void
	reload()
	  
		throws PluginException
	{
		notSupported();
	}
	
	public void
	uninstall()
	
		throws PluginException
	{
		notSupported();
	}
	
	public boolean
	isInitialisationThread()
	{
		notSupported();
		
		return( false );
	}

	 public ClientIDManager
	 getClientIDManager()
	 {
	 	notSupported();
	 	
	 	return( null );
	 }
   
   
   public ConnectionManager getConnectionManager() {
     notSupported();
     return null; 
   }
   
   public MessageManager getMessageManager() {
     notSupported();
     return null; 
   }
   
   
   public DistributedDatabase
   getDistributedDatabase()
   {
    notSupported();
    return null; 
   }
   public PlatformManager
   getPlatformManager()
   {
    notSupported();
    return null; 
   }
   
	public void
	addListener(
			PluginListener	l )
	{
		notSupported();
	}
	
	public void
	removeListener(
			PluginListener	l )	
	{
		notSupported();
	}
	
	public void
	firePluginEvent(
		PluginEvent		event )
	{
	  notSupported();
	}
	  
	public void
	addEventListener(
		PluginEventListener	l )
	{
		notSupported();
	}
	
	public void
	removeEventListener(
		PluginEventListener	l )	
	{
		notSupported();
	}

	public ConfigSection[] getConfigSections() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public MainlineDHTManager getMainlineDHTManager() {notSupported(); return null;}
	public PluginState getPluginState() {notSupported(); return null;}
	
}
