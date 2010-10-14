/*
 * File    : WebPlugin.java
 * Created : 23-Jan-2004
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

package org.gudy.azureus2.ui.webplugin;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ipfilter.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;

import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

public class 
WebPlugin
	implements Plugin, TrackerWebPageGenerator
{
	public static final String	PR_PORT						= "Port";						// Integer
	public static final String	PR_BIND_IP					= "Bind IP";					// String
	public static final String	PR_ROOT_RESOURCE			= "Root Resource";				// String
	public static final String	PR_LOG						= "DefaultLoggerChannel";		// LoggerChannel
	public static final String	PR_CONFIG_MODEL				= "DefaultConfigModel";			// BasicPluginConfigModel
	public static final String	PR_VIEW_MODEL				= "DefaultViewModel";			// BasicPluginViewModel
	public static final String	PR_HIDE_RESOURCE_CONFIG		= "DefaultHideResourceConfig";	// Boolean
	
	
	public static final String	PROPERTIES_MIGRATED		= "Properties Migrated";
	public static final String	CONFIG_MIGRATED			= "Config Migrated";
	
	public static final String	CONFIG_PASSWORD_ENABLE			= "Password Enable";
	public        final boolean	CONFIG_PASSWORD_ENABLE_DEFAULT	= false;
	
	public static final String	CONFIG_USER				= "User";
	public        final String	CONFIG_USER_DEFAULT		= "";
	
	public static final String	CONFIG_PASSWORD			= "Password";
	public        final byte[]	CONFIG_PASSWORD_DEFAULT	= {};
	
	public static final String 	CONFIG_PORT						= PR_PORT;
	public int			 		CONFIG_PORT_DEFAULT				= 8089;
	
	public static final String 	CONFIG_BIND_IP					= PR_BIND_IP;
	public String		 		CONFIG_BIND_IP_DEFAULT			= "";

	public static final String 	CONFIG_PROTOCOL					= "Protocol";
	public 		  final String 	CONFIG_PROTOCOL_DEFAULT			= "HTTP";

	public static final String	CONFIG_UPNP_ENABLE				= "UPnP Enable";
	public    	  final boolean	CONFIG_UPNP_ENABLE_DEFAULT		= true;

	public static final String 	CONFIG_HOME_PAGE				= "Home Page";
	public        final String 	CONFIG_HOME_PAGE_DEFAULT		= "index.html";
	
	public static final String 	CONFIG_ROOT_DIR					= "Root Dir";
	public        final String 	CONFIG_ROOT_DIR_DEFAULT			= "";
	
	public static final String 	CONFIG_ROOT_RESOURCE			= PR_ROOT_RESOURCE;
	public              String 	CONFIG_ROOT_RESOURCE_DEFAULT	= "";
	
	public static final String 	CONFIG_MODE						= "Mode";
	public static final String 	CONFIG_MODE_FULL				= "full";
	public        final String 	CONFIG_MODE_DEFAULT				= CONFIG_MODE_FULL;
	
	public static final String 	CONFIG_ACCESS					= "Access";
	public        final String 	CONFIG_ACCESS_DEFAULT			= "all";
	
	protected static final String	NL			= "\r\n";
	
	protected static final String[]		welcome_pages = {"index.html", "index.htm", "index.php", "index.tmpl" };
	protected static File[]				welcome_files;
	
	protected PluginInterface			plugin_interface;	// unfortunately this is accessed by webui - fix sometime
	private LoggerChannel			log;
	private Tracker					tracker;
	private BasicPluginViewModel 	view_model;
	private BasicPluginConfigModel	config_model;
	
	private String				home_page;
	private String				file_root;
	private String				resource_root;
	
	private boolean				ip_range_all	= false;
	private IPRange				ip_range;
	
	private Properties	properties;
	
	public 
	WebPlugin()
	{
		properties	= new Properties();
	}
	
	public 
	WebPlugin(
		Properties		defaults )
	{	
		properties	= defaults;
	}
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		plugin_interface	= _plugin_interface;
		
		Integer	pr_port = (Integer)properties.get(PR_PORT);
		
		if ( pr_port != null ){
		
			CONFIG_PORT_DEFAULT	= pr_port.intValue();
		}
		
		String	pr_bind_ip = (String)properties.get(PR_BIND_IP);
		
		if ( pr_bind_ip != null ){
		
			CONFIG_BIND_IP_DEFAULT	= pr_bind_ip.trim();
		}
		
		String	pr_root_resource = (String)properties.get( PR_ROOT_RESOURCE );
		
		if( pr_root_resource != null ){
			
			CONFIG_ROOT_RESOURCE_DEFAULT	= pr_root_resource;
		}
		
		Boolean	pr_hide_resource_config = (Boolean)properties.get( PR_HIDE_RESOURCE_CONFIG );
		
		log = (LoggerChannel)properties.get( PR_LOG );
		
		if ( log == null ){
			
			log = plugin_interface.getLogger().getChannel("WebPlugin");
		}
		
		UIManager	ui_manager = plugin_interface.getUIManager();
		
		view_model = (BasicPluginViewModel)properties.get( PR_VIEW_MODEL );

		if ( view_model == null ){
			
			view_model = ui_manager.createBasicPluginViewModel( plugin_interface.getPluginName());
		}
		
		String sConfigSectionID = "plugins." + plugin_interface.getPluginID();
		
		view_model.setConfigSectionID(sConfigSectionID);
		view_model.getStatus().setText( "Running" );
		view_model.getActivity().setVisible( false );
		view_model.getProgress().setVisible( false );
		
		log.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	message )
				{
					view_model.getLogArea().appendText( message+"\n");
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					view_model.getLogArea().appendText( str + "\n" );
					view_model.getLogArea().appendText( error.toString() + "\n" );
				}
			});
		
		PluginConfig	plugin_config = plugin_interface.getPluginconfig();
		
		config_model = (BasicPluginConfigModel)properties.get( PR_CONFIG_MODEL );

		if ( config_model == null ){
			
			config_model = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, sConfigSectionID);
		}
		
		boolean	save_needed = false;
		
		if ( !plugin_config.getPluginBooleanParameter( CONFIG_MIGRATED, false )){
			
			plugin_config.setPluginParameter( CONFIG_MIGRATED, true );
			
			save_needed	= true;
			
			plugin_config.setPluginParameter(
					CONFIG_PASSWORD_ENABLE,
					plugin_config.getBooleanParameter(
							"Tracker Password Enable Web", CONFIG_PASSWORD_ENABLE_DEFAULT ));
			
			plugin_config.setPluginParameter(
					CONFIG_USER,
					plugin_config.getStringParameter(
							"Tracker Username", CONFIG_USER_DEFAULT ));
			
			plugin_config.setPluginParameter(
					CONFIG_PASSWORD,
					plugin_config.getByteParameter(
							"Tracker Password", CONFIG_PASSWORD_DEFAULT ));
					
		}
		
		if ( !plugin_config.getPluginBooleanParameter( PROPERTIES_MIGRATED, false )){
			
			plugin_config.setPluginParameter( PROPERTIES_MIGRATED, true );
						
			Properties	props = plugin_interface.getPluginProperties();
			
				// make sure we've got an old properties file too
			
			if ( props.getProperty( "port", "" ).length() > 0 ){
				
				save_needed = true;
				
				String	prop_port		= props.getProperty( "port",			""+CONFIG_PORT_DEFAULT );
				String	prop_protocol	= props.getProperty( "protocol", 		CONFIG_PROTOCOL_DEFAULT );
				String	prop_home		= props.getProperty( "homepage", 		CONFIG_HOME_PAGE_DEFAULT );
				String	prop_rootdir	= props.getProperty( "rootdir", 		CONFIG_ROOT_DIR_DEFAULT );
				String	prop_rootres	= props.getProperty( "rootresource", 	CONFIG_ROOT_RESOURCE_DEFAULT );
				String	prop_mode		= props.getProperty( "mode", 			CONFIG_MODE_DEFAULT );
				String	prop_access		= props.getProperty( "access", 			CONFIG_ACCESS_DEFAULT );
	
				int	prop_port_int = CONFIG_PORT_DEFAULT;
				
				try{
					prop_port_int	= Integer.parseInt( prop_port );
					
				}catch( Throwable e ){
				}
		
				plugin_config.setPluginParameter(CONFIG_PORT, prop_port_int );
				plugin_config.setPluginParameter(CONFIG_PROTOCOL, prop_protocol );
				plugin_config.setPluginParameter(CONFIG_HOME_PAGE, prop_home );
				plugin_config.setPluginParameter(CONFIG_ROOT_DIR, prop_rootdir );
				plugin_config.setPluginParameter(CONFIG_ROOT_RESOURCE, prop_rootres );
				plugin_config.setPluginParameter(CONFIG_MODE, prop_mode );
				plugin_config.setPluginParameter(CONFIG_ACCESS, prop_access );
								
				File	props_file = new File( plugin_interface.getPluginDirectoryName(), "plugin.properties" );
				
				PrintWriter pw = null;
				
				try{
					File	backup = new File( plugin_interface.getPluginDirectoryName(), "plugin.properties.bak" );
					
					props_file.renameTo( backup );
					
					pw = new PrintWriter( new FileWriter( props_file ));
	
					pw.println( "plugin.class=" + props.getProperty( "plugin.class" ));
					pw.println( "plugin.name=" + props.getProperty( "plugin.name" ));
					pw.println( "plugin.version=" + props.getProperty( "plugin.version" ));
					pw.println( "plugin.id=" + props.getProperty( "plugin.id" ));
					pw.println( "" );
					pw.println( "# configuration has been migrated to plugin config - see view->config->plugins" );
					pw.println( "# in the SWT user interface" );
					
					log.logAlert( 	LoggerChannel.LT_INFORMATION, 
							plugin_interface.getPluginName() + " - plugin.properties settings migrated to plugin configuration." );
			
				}catch( Throwable  e ){
					
					Debug.printStackTrace( e );
					
					log.logAlert( 	LoggerChannel.LT_ERROR, 
									plugin_interface.getPluginName() + " - plugin.properties settings migration failed." );
					
				}finally{
					
					if ( pw != null ){
						
						pw.close();
					}
				}	
			}
		}
		
		if ( save_needed ){
			
			plugin_config.save();
		}
		
		config_model.addLabelParameter2( "webui.restart.info" );

		IntParameter	param_port = config_model.addIntParameter2(		CONFIG_PORT, "webui.port", CONFIG_PORT_DEFAULT );
		StringParameter	param_bind = config_model.addStringParameter2(	CONFIG_BIND_IP, "webui.bindip", CONFIG_BIND_IP_DEFAULT );
		
		StringListParameter	param_protocol = 
			config_model.addStringListParameter2(
					CONFIG_PROTOCOL, "webui.protocol", new String[]{ "http", "https" }, CONFIG_PROTOCOL_DEFAULT );
		
		
		final BooleanParameter	upnp_enable = 
			config_model.addBooleanParameter2( 
							CONFIG_UPNP_ENABLE, 
							"webui.upnpenable",
							CONFIG_UPNP_ENABLE_DEFAULT );

		StringParameter	param_home 		= config_model.addStringParameter2(	CONFIG_HOME_PAGE, "webui.homepage", CONFIG_HOME_PAGE_DEFAULT );
		StringParameter	param_rootdir 	= config_model.addStringParameter2(	CONFIG_ROOT_DIR, "webui.rootdir", CONFIG_ROOT_DIR_DEFAULT );
		StringParameter	param_rootres	= config_model.addStringParameter2(	CONFIG_ROOT_RESOURCE, "webui.rootres", CONFIG_ROOT_RESOURCE_DEFAULT );
		
		if ( pr_hide_resource_config != null && pr_hide_resource_config.booleanValue()){
			
			param_home.setVisible( false );
			param_rootdir.setVisible( false );
			param_rootres.setVisible( false );
		}
		
		config_model.addLabelParameter2( "webui.mode.info" ); 
		config_model.addStringListParameter2(	
					CONFIG_MODE, "webui.mode", new String[]{ "full", "view" }, CONFIG_MODE_DEFAULT );
		
		
		config_model.addLabelParameter2( "webui.access.info" );
		StringParameter	param_access	= config_model.addStringParameter2(	CONFIG_ACCESS, "webui.access", CONFIG_ACCESS_DEFAULT );
		
		
		final BooleanParameter	pw_enable = 
			config_model.addBooleanParameter2( 
							CONFIG_PASSWORD_ENABLE, 
							"webui.passwordenable",
							CONFIG_PASSWORD_ENABLE_DEFAULT );
		
		final StringParameter		user_name = 
			config_model.addStringParameter2( 
							CONFIG_USER, 
							"webui.user",
							CONFIG_USER_DEFAULT );
		
		final PasswordParameter	password = 
			config_model.addPasswordParameter2( 
							CONFIG_PASSWORD, 
							"webui.password",
							PasswordParameter.ET_SHA1,
							CONFIG_PASSWORD_DEFAULT );
		
		pw_enable.addEnabledOnSelection( user_name );
		pw_enable.addEnabledOnSelection( password );
		
		tracker = plugin_interface.getTracker();
		
		home_page = param_home.getValue().trim();
		
		if ( home_page.length() == 0 ){
				
			home_page = null;
				
		}else if (!home_page.startsWith("/" )){
			
			home_page = "/" + home_page;
		}
		
		resource_root = param_rootres.getValue().trim();
				
		if ( resource_root.length() == 0 ){
				
			resource_root = null;
				
		}else if ( resource_root.startsWith("/" )){
			
			resource_root = resource_root.substring(1);
		}
		
		String	root_dir	= param_rootdir.getValue().trim();
		
		if ( root_dir.length() == 0 ){
			
			file_root = plugin_interface.getPluginDirectoryName();
			
			if ( file_root == null ){
				
				file_root = SystemProperties.getUserPath() + "web";
			}
		}else{
			
				// absolute or relative
			
			if ( root_dir.startsWith(File.separator) || root_dir.indexOf(":") != -1 ){
				
				file_root = root_dir;
				
			}else{
				
				file_root = SystemProperties.getUserPath() + "web" + File.separator + root_dir;
				
			}
		}

		File	f_root = new File( file_root );
		
		if ( !f_root.exists()){
	
			String	error = "WebPlugin: root dir '" + file_root + "' doesn't exist";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}

		if ( !f_root.isDirectory()){
			
			String	error = "WebPlugin: root dir '" + file_root + "' isn't a directory";
			
			log.log( LoggerChannel.LT_ERROR, error );
			
			throw( new PluginException( error ));
		}
		
		welcome_files = new File[welcome_pages.length];
		
		for (int i=0;i<welcome_pages.length;i++){
			
			welcome_files[i] = new File( file_root + File.separator + welcome_pages[i] );
		}
		
					
		final int port	= param_port.getValue();

		String	protocol_str = param_protocol.getValue().trim();
		
		String bind_ip_str = param_bind.getValue().trim();
		
		InetAddress	bind_ip = null;
		
		if ( bind_ip_str.length() > 0 ){
			
			try{
				bind_ip = InetAddress.getByName( bind_ip_str );
				
			}catch( Throwable  e ){
				
				log.log( LoggerChannel.LT_ERROR, "Bind IP parameter '" + bind_ip_str + "' is invalid" );
	
			}
		}
		
		int	protocol = protocol_str.equalsIgnoreCase( "HTTP")?Tracker.PR_HTTP:Tracker.PR_HTTPS;
	
		log.log( 	LoggerChannel.LT_INFORMATION, 
					"Initialisation: port = " + port +
					(bind_ip == null?"":(", bind = " + bind_ip_str + ")")) +
					", protocol = " + protocol_str + (root_dir.length()==0?"":(", root = " + root_dir )));
		
		String	access_str = param_access.getValue().trim();
		
		if ( access_str.length() > 7 && Character.isDigit(access_str.charAt(0))){
			
			ip_range	= plugin_interface.getIPFilter().createRange(true);
			
			int	sep = access_str.indexOf("-");
				
			if ( sep == -1 ){
				
				ip_range.setStartIP( access_str );
				
				ip_range.setEndIP( access_str );
				
			}else{				
				
				ip_range.setStartIP( access_str.substring(0,sep).trim());
				
				ip_range.setEndIP( access_str.substring( sep+1 ).trim());
			}
			
			ip_range.checkValid();
			
			if (!ip_range.isValid()){
			
				log.log( LoggerChannel.LT_ERROR, "Access parameter '" + access_str + "' is invalid" );
			
				ip_range	= null;
			}
		}else{
			
			if ( access_str.equalsIgnoreCase( "all" )){
								
				ip_range_all	= true;				
			}
		}
		
		log.log( 	LoggerChannel.LT_INFORMATION, 
					"acceptable IP range = " +
						( ip_range==null?
							(ip_range_all?"all":"local"):
							(ip_range.getStartIP() + " - " + ip_range.getEndIP())));
				
							
		try{
			TrackerWebContext	context = 
				tracker.createWebContext(
						plugin_interface.getAzureusName() + " - " + plugin_interface.getPluginName(), 
						port, protocol, bind_ip );
		
			context.addPageGenerator( this );
	
			context.addAuthenticationListener(
				new TrackerAuthenticationAdapter()
				{
					String	last_pw		= "";
					byte[]	last_hash	= {};
					
					AEMonitor	this_mon = new AEMonitor( "WebPlugin:auth" );
					
					public boolean
					authenticate(
						URL			resource,
						String		user,
						String		pw )
					{
						try{
							this_mon.enter();
						
							if ( !pw_enable.getValue()){
								
								return( true );
							}
							
							if ( !user.equals(user_name.getValue())){
								
								return( false );
							}
							
							byte[]	hash = last_hash;
							
							if (  !last_pw.equals( pw )){
															
								hash = plugin_interface.getUtilities().getSecurityManager().calculateSHA1( pw.getBytes());
								
								last_pw		= pw;
								last_hash	= hash;
							}
							
							return( Arrays.equals( hash, password.getValue()));
							
						}finally{
							
							this_mon.exit();
						}
					}
				});
			
		}catch( TrackerException e ){
			
			log.log( "Plugin Initialisation Fails", e );
		}
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
					
					if ( pi_upnp == null ){
						
						log.log( "No UPnP plugin available, not attempting port mapping");
						
					}else{
						
						if ( upnp_enable.getValue()){
							
							((UPnPPlugin)pi_upnp.getPlugin()).addMapping( plugin_interface.getPluginName(), true, port, true );
							
						}else{
							
							log.log( "UPnP disabled for the plugin, not attempting port mapping");
							
						}
					}
				}
				
				public void
				closedownInitiated()
				{
				}
				
				public void
				closedownComplete()
				{	
				}
			});
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		return( false );
	}
	
	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		if ( !ip_range_all ){
		
			String	client = request.getClientAddress();
			
			// System.out.println( "client = " + client );
			
			try{
				InetAddress ia = InetAddress.getByName( client );
				
				if ( ip_range == null ){
					
					if ( !ia.isLoopbackAddress()){
				
						log.log( LoggerChannel.LT_ERROR, "Client '" + client + "' is not local, rejecting" );
						
						return( false );
					}
				}else{
					
					if ( !ip_range.isInRange( ia.getHostAddress())){
						
						log.log( LoggerChannel.LT_ERROR, "Client '" + client + "' (" + ia.getHostAddress() + ") is not in range, rejecting" );
						
						return( false );
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
				
				return( false );
			}
		}
		
		if ( request.getURL().toString().endsWith(".class")){
			
			System.out.println( "WebPlugin::generate:" + request.getURL());
		}
			
		if ( generateSupport( request, response )){
			
			return(true);
		}
		
		OutputStream os = response.getOutputStream();
		
		String	url = request.getURL();
		
		if (url.equals("/")){
			
			if (home_page != null ){
				
				url = home_page;
				
			}else{
			
				for (int i=0;i<welcome_files.length;i++){
					
					if ( welcome_files[i].exists()){
						
						url = "/" + welcome_pages[i];
						
						break;
					}
				}	
			}
		}
	
			// first try file system for data
		
		if ( response.useFile( file_root, url )){
			
			return( true );
		}
		
				// now try jars		
			
		String	resource_name = url;
		
		if (resource_name.startsWith("/")){
			
			resource_name = resource_name.substring(1);
		}
					
		int	pos = resource_name.lastIndexOf(".");
		
		if ( pos != -1 ){
			
			String	type = resource_name.substring( pos+1 );
		
			ClassLoader	cl = plugin_interface.getPluginClassLoader();
			
			InputStream is = cl.getResourceAsStream( resource_name );
		
			if ( is == null ){
				
				// failed absolute load, try relative
				
				if ( resource_root != null ){ 
					
					resource_name = resource_root + "/" + resource_name;
					
					is = cl.getResourceAsStream( resource_name );	
				}
			}
			
			// System.out.println( resource_name + "->" + is + ", url = " + url );
		
			if (is != null ){
			
				try{
					response.useStream( type, is );
				
				}finally{
				
					is.close();
				}
			
				return( true );
			}
		}
		
		return( false );
	}
	
	protected BasicPluginConfigModel
	getConfigModel()
	{
		return( config_model );
	}
	
	protected BasicPluginViewModel getViewModel() {
		return this.view_model;
	}
}
