/*
 * File    : PluginInterfaceImpl.java
 * Created : 12 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.pluginsimpl.local;

import java.util.*;
import java.io.File;
import java.net.URL; 

import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.dht.mainline.*;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.pluginsimpl.local.dht.mainline.*;
import org.gudy.azureus2.pluginsimpl.local.clientid.ClientIDManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.installer.PluginInstallerImpl;
import org.gudy.azureus2.pluginsimpl.local.ipc.IPCInterfaceImpl;
import org.gudy.azureus2.pluginsimpl.local.ipfilter.IPFilterImpl;
import org.gudy.azureus2.pluginsimpl.local.logging.LoggerImpl;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.network.ConnectionManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.protocol.*;
import org.gudy.azureus2.pluginsimpl.local.sharing.ShareManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.*;
import org.gudy.azureus2.pluginsimpl.local.ui.*;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ParameterRepository;
import org.gudy.azureus2.pluginsimpl.local.ui.config.PluginConfigUIFactoryImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.*;
import org.gudy.azureus2.pluginsimpl.local.update.*;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.tables.peers.PluginPeerItemFactory;
import org.gudy.azureus2.plugins.ui.tables.mytorrents.PluginMyTorrentsItemFactory;
import org.gudy.azureus2.plugins.peers.protocol.*;
import org.gudy.azureus2.plugins.platform.PlatformManager;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.clientid.ClientIDManager;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.update.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;



/**
 * @author Olivier
 *
 */
public class 
PluginInterfaceImpl 
	implements PluginInterface 
{
	private static final LogIDs LOGID = org.gudy.azureus2.core3.logging.LogIDs.PLUGIN;

  private Plugin				plugin;
  private PluginInitializer		initialiser;
  private Object				initialiser_key;
  private ClassLoader			class_loader;
  private List					listeners 		= new ArrayList();
  private List					event_listeners	= new ArrayList();
  private String				key;
  private String 				pluginConfigKey;
  private Properties 			props;
  private String 				pluginDir;
  private PluginConfig 			config;
  private String				plugin_id;
  private String				plugin_version;
  private boolean				operational;
  private boolean				disabled;
  private Logger				logger;
  private IPCInterfaceImpl		ipc_interface;
  private List					children		= new ArrayList();
  private List configSections = new ArrayList();
  
  public 
  PluginInterfaceImpl(
  		Plugin				_plugin,
  		PluginInitializer	_initialiser,
		Object				_initialiser_key,
		ClassLoader			_class_loader,
		String 				_key,
		Properties 			_props,
		String 				_pluginDir,
		String				_plugin_id,
		String				_plugin_version ) 
  {
  	plugin				= _plugin;
  	initialiser			= _initialiser;
  	initialiser_key		= _initialiser_key;
  	class_loader		= _class_loader;
  	key					= _key;
  	pluginConfigKey 	= "Plugin." + _key;
    props 				= new propertyWrapper(_props );
    pluginDir 			= _pluginDir;
    config 				= new PluginConfigImpl(this,pluginConfigKey);
    plugin_id			= _plugin_id;
    plugin_version		= _plugin_version;
    ipc_interface		= new IPCInterfaceImpl( initialiser, plugin );
  }
  
  	public Plugin
	getPlugin()
	{
  		return( plugin );
	}
  
  	protected void
	setOperational(
		boolean	b )
	{
  		operational	= b;
  	}
  	
    public boolean
    isOperational()
	{
    	return( operational );
    }

  	public Object
	getInitializerKey()
	{
  		return( initialiser_key );
  	}
  	
  	public PluginManager
	getPluginManager()
	{
  		return( initialiser.getPluginManager());
  	}
  	
	public String
	getAzureusName()
	{
		return( Constants.AZUREUS_NAME );
	}
	
	public String
	getAzureusVersion()
	{
		return( Constants.AZUREUS_VERSION );
	}
	

	/**
	 * @deprecated
	 */
  public void addView(PluginView view)
  {
    getUIManager().getSWTManager().addView(view);
  } 
  
  public void addConfigSection(ConfigSection section)
  {
  	ConfigSectionRepository.getInstance().addConfigSection(section);
  	configSections.add(section);
  }

  public void removeConfigSection(ConfigSection section)
  {
  	ConfigSectionRepository.getInstance().removeConfigSection(section);
  	configSections.remove(section);
  }
  
  public ConfigSection[] getConfigSections() {
  	return (ConfigSection[]) configSections.toArray(new ConfigSection[0]);
  }
  
  /**
   * @deprecated
   */
  public void openTorrentFile(String fileName) {
	  try{
		  getDownloadManager().addDownload( new File(fileName));
	  }catch( DownloadException e ){
		  throw( new RuntimeException(e));
	  }
  }

  /**
   * @deprecated
   */
  public void openTorrentURL(String url) {
	  try{
		  getDownloadManager().addDownload( new URL( url ));
	  }catch( Throwable e ){
		  throw( new RuntimeException(e));
	  } 
  }
      
  public void
  setPluginName(
  	String	name )
  {
  	props.put( "plugin.name", name );
  }
  
  public String getPluginName()
  {
  	String	name = null;
  	
  	if ( props != null ){
  		
  		name = (String)props.get( "plugin.name");
  	}
  	
  	if ( name == null ){
  		
  		try{
  			
  			name = new File(pluginDir).getName();
  			
  		}catch( Throwable e ){
  			
  		}
  	}
  	
  	if ( name == null || name.length() == 0 ){
  		
  		name = plugin.getClass().getName();
  	}
  	
  	return( name );
  }

  public void
  setPluginVersion(
  	String	version )
  {
	props.put( "plugin.version", version );
  }
  
  public String
  getPluginVersion()
  {
	String	version = (String)props.get("plugin.version");
  	
  	if ( version == null ){
  		
  		version = plugin_version;
  	}
  	
  	return( version );
  }

  public String
  getPluginID()
  {
  	String	id = (String)props.get("plugin.id");
  	
  	if ( id == null ){
  		
  		id = plugin_id;
  	}
  	
  	return( id==null?"<none>":id );
  }

  public boolean
  isMandatory()
  {
	String	mand = getPluginProperties().getProperty( "plugin.mandatory");
	
	return( mand != null && mand.trim().toLowerCase().equals("true"));
  }
  
  public boolean
  isBuiltIn()
  {
	  String	dir = getPluginDirectoryName();
	  
	  if ( dir == null ){
		  
		  return( PluginInitializer.isLoadingBuiltin());
	  }
	  
  		return( dir.length() == 0 || getPluginID().equals( "azupdater" ));
  }
  
	public void
	setDisabled(
		boolean	_disabled )
	{
		disabled	= _disabled;
	}
	  
	public boolean
	isDisabled()
	{
		return( disabled );
	}
	
  public Properties getPluginProperties() 
  {
    return(props);
  }
  
  public String getPluginDirectoryName() {
    return pluginDir;
  }

  public void
  setPluginDirectoryName(
  	String		name )
  {
  	initialiser_key	= new File(name);
  	
  	pluginDir	= name;
  }
  
  public void addConfigUIParameters(Parameter[] parameters, String displayName) {
  	ParameterRepository.getInstance().addPlugin(parameters, displayName);
  }


  public PluginConfig getPluginconfig() {
    return config;
  }


  public PluginConfigUIFactory getPluginConfigUIFactory() {
    return new PluginConfigUIFactoryImpl(config,pluginConfigKey);
  }
  
  public String
  getPluginConfigKey()
  {
  	return( pluginConfigKey );
  }
  
  /** @deprecated Use getUIManager().getTableManager().createColumn */
  public void addColumnToPeersTable(String columnName, PluginPeerItemFactory item) {
    Debug.out( "Method PluginInterface::addColumnToPeersTable deprecated. Use getUIManager().getTableManager().createColumn" );
  }
  
  /** @deprecated Use getUIManager().getTableManager().createColumn */
  public void addColumnToMyTorrentsTable(String columnName, PluginMyTorrentsItemFactory item) {
	   Debug.out( "Method PluginInterface::addColumnToMyTorrentsTable deprecated. Use getUIManager().getTableManager().createColumn" );
 }

  public Tracker getTracker() {
  	return( TrackerImpl.getSingleton());
  }
  
  public ShareManager
  getShareManager()
  
  	throws ShareException
  {
  	return( ShareManagerImpl.getSingleton());
  }
  
  public DownloadManager
  getDownloadManager()
  {
  	return( DownloadManagerImpl.getSingleton(initialiser.getAzureusCore()));
  }
  
  public MainlineDHTManager getMainlineDHTManager() {
	  return new MainlineDHTManagerImpl(initialiser.getAzureusCore());
  }
  
  public TorrentManager
  getTorrentManager()
  {
  	return( TorrentManagerImpl.getSingleton().specialise( this ));
  }
  
  public Logger getLogger() 
  {
  	if ( logger == null ){
  		
  		logger = new LoggerImpl( this );
  	}
  	
  	return( logger );
  }
  
  public IPFilter
  getIPFilter()
  {
  	return( new IPFilterImpl());
  }
  
  public Utilities
  getUtilities()
  {
  	return( new UtilitiesImpl( initialiser.getAzureusCore(), this ));
  }
  
  public ShortCuts
  getShortCuts()
  {
  	return( new ShortCutsImpl(this));
  }
  
  public UIManager
  getUIManager()
  {
  	return( new UIManagerImpl( this ));
  }
  
  public UpdateManager
  getUpdateManager()
  {
  	return( UpdateManagerImpl.getSingleton( initialiser.getAzureusCore()));
  }

  public PeerProtocolManager
  getPeerProtocolManager()
  {
  	return( PeerProtocolManagerImpl.getSingleton());
  }
  
  public boolean
  isUnloadable()
  {
  	String dir = getPluginDirectoryName();
  	
  		// mechanism to override unloadability 
  	
   	boolean	disable_unload = getPluginProperties().getProperty( "plugin.unload.disabled", "" ).equalsIgnoreCase( "true" );
  	
  	if ( disable_unload ){
  		
  		return( false );
  	}
  	
		// if not dir based then just test this one
  	
  	if ( dir == null || dir.length() == 0 ){
  		
  		return(getPlugin() instanceof UnloadablePlugin );
  	}
  	
 	List	pis = PluginInitializer.getPluginInterfaces();
  	
  	for (int i=0;i<pis.size();i++){
  		
  		PluginInterface	pi = (PluginInterface)pis.get(i);
  		
  		String other_dir = pi.getPluginDirectoryName();
  		
  		if ( other_dir == null || other_dir.length() == 0 ){
  			
  			continue;
  		}
  		
  		if ( dir.equals( other_dir )){
  			
  			if ( !(pi.getPlugin() instanceof UnloadablePlugin )){
  		
  				return( false );
  			}  
  		}
  	}
  	
  	for (int i=0;i<children.size();i++){
  		
  		if ( !((PluginInterface)children.get(i)).isUnloadable()){
  			
  			return( false );
  		}
  	}
  	
  	return( true );
  }
  
  public void
  unload()
  
  	throws PluginException
  {
  	if ( !isUnloadable()){
  		
  		throw( new PluginException( "Plugin isn't unloadable" ));
  	}
  	
 	String dir = getPluginDirectoryName();
  	
  		// if not dir based then just test this one
  	
  	if ( dir == null || dir.length() == 0 ){
  		
		((UnloadablePlugin)getPlugin()).unload();
			
		initialiser.unloadPlugin( this );
		
  	}else{
  		
  			// we must copy the list here as when we unload interfaces they will be
  			// removed from the original list
  		
		List	pis = new ArrayList(PluginInitializer.getPluginInterfaces());
		 
		for (int i=0;i<pis.size();i++){
	  		
	  		PluginInterfaceImpl	pi = (PluginInterfaceImpl)pis.get(i);
	  		
			String other_dir = pi.getPluginDirectoryName();
	  		
	  		if ( other_dir == null || other_dir.length() == 0 ){
	  			
	  			continue;
	  		}
	  		
	  		if ( dir.equals( other_dir )){
		  			
	  			((UnloadablePlugin)pi.getPlugin()).unload();
	  			
	  			initialiser.unloadPlugin( pi );
	  		}
		}
  	}
  	
  	for (int i=0;i<children.size();i++){
  		
  		((PluginInterface)children.get(i)).unload();
  	}
  	
  	setOperational(false);

  	class_loader = null;
  }
  
  protected void
  unloadSupport()
  {
	  ipc_interface.unload();
  }
  
  public void
  reload()
  
  	throws PluginException
  {
	  	// we use the "reload" method to load disabled plugins regardless of whether they are
	  	// unloadable. If currently disabled then no unloading to do anyway
	  
	if ( isUnloadable() || isOperational()){
		
	  unload();
	}
	  
  	initialiser.reloadPlugin( this );
  }
  
	public void
	uninstall()
	
		throws PluginException
	{
		PluginInstallerImpl.getSingleton(getPluginManager()).uninstall( this );
	}
	
	public boolean
	isInitialisationThread()
	{
		return( initialiser.isInitialisationThread());
	}
	
	 public ClientIDManager
	 getClientIDManager()
	 {
	 	return( ClientIDManagerImpl.getSingleton());
	 }
	 
   
   public ConnectionManager getConnectionManager() {
     return ConnectionManagerImpl.getSingleton( initialiser.getAzureusCore());
   }
   
   public MessageManager getMessageManager() {
     return MessageManagerImpl.getSingleton( initialiser.getAzureusCore() );
   }
   
   
   public DistributedDatabase
   getDistributedDatabase()
   {
   	return( DDBaseImpl.getSingleton(initialiser.getAzureusCore()));
   }
   
   public PlatformManager
   getPlatformManager()
   {
	   return( PlatformManagerFactory.getPlatformManager());
   }
   
  protected void
  initialisationComplete()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).initializationComplete();
  			
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  		}
  	}
  	
  	for (int i=0;i<children.size();i++){
  		
  		((PluginInterfaceImpl)children.get(i)).initialisationComplete();
  	}
  }
  
  protected void
  closedownInitiated()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).closedownInitiated();
  			
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  		}
  	}
  	
  	for (int i=0;i<children.size();i++){
  		
  		((PluginInterfaceImpl)children.get(i)).closedownInitiated();
  	}
  }
  
  protected void
  closedownComplete()
  {
  	for (int i=0;i<listeners.size();i++){
  		
  		try{
  			((PluginListener)listeners.get(i)).closedownComplete();
  			
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  		}
  	}
  	
 	for (int i=0;i<children.size();i++){
  		
  		((PluginInterfaceImpl)children.get(i)).closedownComplete();
  	}
  }
  
  public void
  firePluginEvent(
	PluginEvent		event )
  {
  	for (int i=0;i<event_listeners.size();i++){
  		
  		try{
  			((PluginEventListener)event_listeners.get(i)).handleEvent( event );
  			
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  		}
  	} 
  	
	for (int i=0;i<children.size();i++){
  		
  		((PluginInterfaceImpl)children.get(i)).firePluginEvent(event);
  	}
  }
  
  public ClassLoader
  getPluginClassLoader()
  {
  	return( class_loader );
  }
  
	public PluginInterface
	getLocalPluginInterface(
		Class		plugin_class,
		String		id )
	
		throws PluginException
	{
		try{
			Plugin	p = (Plugin)plugin_class.newInstance();
	
			PluginInterfaceImpl pi =
				new PluginInterfaceImpl(
			  		p,
			  		initialiser,
					initialiser_key,
					class_loader,
					key + "." + id,
					props,
					pluginDir,
					plugin_id + "." + id,
					plugin_version ); 
			
			p.initialize( pi );
			
			children.add( pi );
			
			return( pi );
			
		}catch( Throwable e ){
			
			if ( e instanceof PluginException ){
				
				throw((PluginException)e);
			}
			
			throw( new PluginException( "Local initialisation fails", e ));
		}
	}
	
	 public IPCInterface 
	 getIPC() 
	 {
		 return( ipc_interface );
	 }
	 
	public boolean
	isShared()
	{
		String shared_dir 	= FileUtil.getApplicationFile( "plugins" ).toString(); 
		   
		String	plugin_dir = getPluginDirectoryName();
				
		return( plugin_dir.startsWith( shared_dir ));
	}
	
  public void
  addListener(
  	PluginListener	l )
  {
  	listeners.add(l);
  	
  	if ( initialiser.isInitialisationComplete()){
  		
  		l.initializationComplete();
  	}
  }
  
  public void
  removeListener(
  	PluginListener	l )
  {
  	listeners.remove(l);
  }
  
  public void
  addEventListener(
  	PluginEventListener	l )
  {
  	event_listeners.add(l);
  }
  
  public void
  removeEventListener(
  	PluginEventListener	l )
  {
  	event_listeners.remove(l);
  }
  
	protected void
	generateEvidence(
		IndentWriter		writer )
	{
		writer.println( getPluginName());

		try{
			writer.indent();
			
			writer.println( "id:" + getPluginID() + ",version:" + getPluginVersion());
			
			String user_dir 	= FileUtil.getUserFile( "plugins" ).toString(); 
			String shared_dir 	= FileUtil.getApplicationFile( "plugins" ).toString(); 
			   
			String	plugin_dir = getPluginDirectoryName();
			
			String	type;
			
			if ( plugin_dir.startsWith( shared_dir )){
				
				type = "shared";
			
			}else	if ( plugin_dir.startsWith( user_dir )){
					
				type = "per-user";	

			}else{
				
				type = "built-in";
			}
			
			writer.println( "type:" + type + ",enabled:" + !isDisabled() + ",operational:" + isOperational());
			
		}finally{
			
			writer.exdent();
		}
	}
  
  
  
  	// unfortunately we need to protect ourselves against the plugin itself trying to set
  	// plugin.version and plugin.id as this screws things up if they get it "wrong".
  	// They should be setting these things in the plugin.properties file
  	// currently the RSSImport plugin does this (version 1.1 sets version as 1.0)
  
  protected class
  propertyWrapper
  	extends Properties
  {
  	protected boolean	initialising	= true;
  	
  	protected
	propertyWrapper(
		Properties	_props )
	{
  		Iterator it = _props.keySet().iterator();
  		
  		while( it.hasNext()){
  			
  			Object	key = it.next();
  			
  			put( key, _props.get(key));
  		}
  		
  		initialising	= false;
  	}
  	
  	public Object
	setProperty(
		String		str,
		String		val )
	{
  			// if its us then we probably know what we're doing :P
  		
  		if ( ! ( plugin.getClass().getName().startsWith( "org.gudy") || plugin.getClass().getName().startsWith( "com.aelitis."))){
  			
	  		if ( str.equalsIgnoreCase( "plugin.id" ) || str.equalsIgnoreCase("plugin.version" )){
	  		 			
	  			if (org.gudy.azureus2.core3.logging.Logger.isEnabled())
						org.gudy.azureus2.core3.logging.Logger
								.log(new LogEvent(LOGID, LogEvent.LT_WARNING, "Plugin '"
										+ getPluginName() + "' tried to set property '" + str
										+ "' - action ignored"));
	  			
	  			return( null );
	  		}
  		}
  		
  		return( super.setProperty( str, val ));
  	}
  	
  	public Object
	put(
		Object	key,
		Object	value )
	{
			// if its us then we probably know what we're doing :P
  		
  		if ( ! ( plugin.getClass().getName().startsWith( "org.gudy") || plugin.getClass().getName().startsWith( "com.aelitis."))){
  			
	 		if ((!initialising ) && key instanceof String ){
	  			
	  			String	k_str = (String)key;
	  			
	  	 		if ( k_str.equalsIgnoreCase( "plugin.id" ) || k_str.equalsIgnoreCase("plugin.version" )){
	  	 	  		
	  	 			if (org.gudy.azureus2.core3.logging.Logger.isEnabled())
							org.gudy.azureus2.core3.logging.Logger.log(new LogEvent(LOGID,
									LogEvent.LT_WARNING, "Plugin '" + getPluginName()
											+ "' tried to set property '" + k_str
											+ "' - action ignored"));
	  	 		 
	  	 			return( null );
	  	 	  	}
	  		}
  		}
  		
  		return( super.put( key, value ));
  	}
  	
  	public Object
	get(
		Object	key )
	{
  		return( super.get(key));
  	}
  }
}
