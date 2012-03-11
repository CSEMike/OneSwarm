/*
 * File    : PluginInterface.java
 * Created : 2 nov. 2003 18:48:47
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
 
package org.gudy.azureus2.plugins;

import java.util.Properties;

import org.gudy.azureus2.plugins.sharing.ShareManager;
import org.gudy.azureus2.plugins.sharing.ShareException;
import org.gudy.azureus2.plugins.tracker.Tracker;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.logging.Logger;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.ipfilter.IPFilter;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.torrent.TorrentManager;
import org.gudy.azureus2.plugins.platform.PlatformManager;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.update.UpdateManager;
import org.gudy.azureus2.plugins.utils.ShortCuts;
import org.gudy.azureus2.plugins.clientid.*;
import org.gudy.azureus2.plugins.dht.mainline.MainlineDHTManager;


/**
 * Defines the communication interface between Azureus and Plugins
 * @author Olivier
 */
public interface PluginInterface {  
	
	/**
	 * Retrieve the name of the application.
     *
     * @return the Application's name
     *
     * @since 2.1.0.0
     */
	public String getAzureusName();
	
	/**
	 * Returns the name of the application that the user sees - if you need to
	 * display the name of the program, you should use this method.
	 * 
	 * @return 3.0.5.3
	 */
	public String getApplicationName();
	
	/** Retrieve the Application's version as a string.
	 *
	 * @return Application's version.  Typically in the following formats (regexp):<br>
	 *         [0-9]+\.[0-9]+\.[0-9]+\.[0-9]+<br>
	 *         [0-9]+\.[0-9]+\.[0-9]+\.[0-9]+_CVS<br>
	 *         [0-9]+\.[0-9]+\.[0-9]+\.[0-9]+_B[0-9]+
   *
   * @since 2.1.0.0
	 */
	public String
	getAzureusVersion();
	
  /**
   * adds a tab under the 'plugins' tab in the config view.<br>
   * Use {@link #getPluginConfigUIFactory()} to get the 
   * {@link PluginConfigUIFactory} class, from which you can create different
   * types of parameters.
   *
   * @param parameters the Parameter(s) to be edited
   * @param displayName the under which it should display.<br>
   * Azureus will look-up for ConfigView.section.plugins.<i>displayName</i>; into the lang files
   * in order to find the localized displayName. (see i18n)
   *
   * @since 2.0.6.0
   * @deprecated Use of this is discouraged - use {@link UIManager#getBasicPluginViewModel(String)}
   *     to get a <tt>BasicPluginViewModel</tt> instance, and then use the methods on that to add
   *     parameters.
   */
  public void addConfigUIParameters(Parameter[] parameters, String displayName);

  /**
   * adds a ConfigSection to the config view.<p>
   * In contrast to addConfigUIParameters, this gives you total control over
   * a tab.  Please be kind and use localizable text.<BR>
   *
   * @param section ConfigSection to be added to the Config view
   *
   * @since 2.0.8.0
   * @deprecated Use {@link UIManager#createBasicPluginConfigModel(String)} instead.
   */
	public void addConfigSection(ConfigSection section);

	/**
	 * 
	 * @param section
	 * @since 2.3.0.5
	 */
	
	public void removeConfigSection( ConfigSection section );

  /**
   * 
   * @return List of ConfigSections for this plugin
   * 
   * @since 2.5.0.1
   */
	ConfigSection[] getConfigSections();

  /**
   * Gives access to the tracker functionality
   * @return The tracker
   *
   * @since 2.0.6.0
   */
  public Tracker getTracker();
  
  /**
   * Gives access to the logger
   * @return The logger
   *
   * @since 2.0.7.0
   */
  public Logger getLogger();
  
  /**
   * Gives access to the IP filter
   * @return An object that allows access to IP Filtering
   *
   * @since 2.0.8.0
   */
  public IPFilter
  getIPFilter();
  
  /**
   * Gives access to the download manager
   * @return An object that allows management of downloads
   *
   * @since 2.0.7.0
   */
  public DownloadManager
  getDownloadManager();
  
  /**
   * Gives access to the sharing functionality
   * @return
   *
   * @since 2.0.7.0
   */
  public ShareManager
  getShareManager()
  
  	throws ShareException;
  
  /**
   * Gives access to the torrent manager
   * @return An object to manage torrents
   *
   * @since 2.0.8.0
   */
  public TorrentManager
  getTorrentManager();
  
  /**
   * access to various utility functions
   * @return
   *
   * @since 2.1.0.0
   */
  public Utilities
  getUtilities();
  
  /**
   * access to a set of convenience routines for doing things in a quicker, although less
   * structured, fashion
   * @return
   *
   * @since 2.1.0.0
   */
  public ShortCuts
  getShortCuts();
  
  /**
   * access to UI extension features 
   * @return
   *
   * @since 2.1.0.0
   */
  public UIManager
  getUIManager();
  
  /**
   * access to the update manager used to update plugins. required for non-Azureus SF hosted
   * plugins (SF ones are managed automatically)
   * @return
   *
   * @since 2.1.0.0
   */
  public UpdateManager
  getUpdateManager();
  
  /**
   * opens a torrent file given its name
   * @param fileName The Name of the file that azureus must open
   *
   * @since 2.0.4.0
   *
   * @deprecated Use {@link DownloadManager#addDownload}
   */
  public void openTorrentFile(String fileName);
  
  /**
   * opens a torrent file given the url it's at
   * @param url The String representation of the url pointing to a torrent file
   *
   * @since 2.0.4.0
   *
   * @deprecated Use {@link DownloadManager#addDownload}
   */
  public void openTorrentURL(String url);
  
  /**
   * gives access to the plugin properties
   * @return the properties from the file plugin.properties
   *
   * @since 2.0.4.0
   */
  public Properties getPluginProperties();
  
  /**
   * Gives access to the plugin installation path - note, if you want to use this
   * path to store data files in, it would be better for you to use
   * {@link PluginConfig#getPluginUserFile(String)} instead.
   * @return the full path the plugin is installed in
   *
   * @since 2.0.4.0
   */
  public String getPluginDirectoryName();
  
  /**
   * gives access to the per-user plugin directory. Useful for shared plugins that need to store
   * per-user state. Will be same as getPluginDirectoryName for per-user installed plugins
   * directory may not yet exist 
   * @return
   */
  
  public String getPerUserPluginDirectoryName();
  
  /**
   * Returns the value of "plugin.name" if it exists in the properties file, otherwise the directory name is returned.
   * @since 2.1.0.0
   */
  public String getPluginName();
  
  /**
   * Returns the version number of the plugin it if can be deduced from either the name of
   * the jar file it is loaded from or the properties file. null otherwise
   *
   * @return Version number as a string, or null
   *
   * @since 2.1.0.0
   */
  public String
  getPluginVersion();
  
  /**
   * Returns an identifier used to identify this particular plugin 
   * @return
   *
   * @since 2.1.0.0
   */
  public String
  getPluginID();
  
  	/**
  	 * Whether or not this is a mandatory plugin. Mandatory plugins take priority over update checks, for example,
  	 * over optional ones.
  	 * 
  	 * @deprecated Use {@link PluginState#isMandatory()}.
  	 */
  
  public boolean
  isMandatory();
  
  	/**
  	 * Built-in plugins are those used internally by Azureus, for example the UPnP plugin
  	 * @deprecated Use {@link PluginState#isBuiltIn()}.
  	 */ 
  public boolean
  isBuiltIn();
  
  /**
   * gives access to the plugin config interface
   * @return the PluginConfig object associated with this plugin
   */
  public PluginConfig getPluginconfig();
  
  
  /**
   * gives access to the plugin Config UI Factory
   * @return the PluginConfigUIFactory associated with this plugin
   * 
   * @deprecated Use of this is discouraged - use {@link UIManager#getBasicPluginViewModel(String)}
   *     to get a <tt>BasicPluginViewModel</tt> instance, and then use the methods on that to add
   *     parameters.
   */
  public PluginConfigUIFactory getPluginConfigUIFactory();
  
  /**
   * gives access to the ClassLoader used to load the plugin
   * @return
   *
   * @since 2.0.8.0
   */
  public ClassLoader
  getPluginClassLoader();
  
	/**
	 * Returns an initialised plugin instance with its own scope (e.g. for config params). 
	 * Designed for loading secondary plugins directly from a primary one. 
	 * Note - ensure that the bundled secondary plugins do *not* contain a plugin.properties as
	 * this will cause no end of problems.
	 * @param plugin	must implement Plugin
	 * @param id        the unique id of this plugin (used to scope config params etc)
	 * @return
	 */

  public PluginInterface
  getLocalPluginInterface(
	Class		plugin,
	String		id )
  
  	throws PluginException;
  
  	/**
  	 * get the inter-plugin-communcations interface for this plugin
  	 * @return
  	 */
  
  public IPCInterface
  getIPC ();
  
  /**
   * Gives access to the plugin itself
   * @return
   *
   * @since 2.1.0.0
   */
  public Plugin
  getPlugin();
  
  /**
   * Returns <tt>true</tt> if the plugin is running, returns <tt>false</tt> if the
   * plugin isn't running for some reason.
   *
   * @since 2.1.0.0
   * @deprecated Use {@link PluginState#isOperational()}.
   */
  public boolean
  isOperational();
  
  /**
   * Returns <tt>true</tt> if the plugin has been marked as disabled, and prevented
   * from initialising.
   * 
   * @deprecated Use {@link PluginState#isDisabled}.
   */
  public boolean
  isDisabled();
  
  /**
   * Sets whether the plugin can be loaded or not. If you are trying to affect if the plugin
   * can be loaded at startup - use {@link #setLoadedAtStartup(boolean)} instead. This needs
   * to be called prior to a plugin's initialisation to take effect.
   * 
   * @since 2.3.0.1
   * @param disabled
   * @deprecated Use {@link PluginState#setDisabled(boolean)}.
   */
  public void setDisabled(boolean disabled);


  /**
   * @deprecated Use {@link PluginState#isUnloadable()}.
   * @since 2.1.0.0
   */
  public boolean
  isUnloadable();

  

  
  /**
   * @since 2503/3005
   * @deprecated Use {@link PluginState#isShared()}.
   */
  
  public boolean
  isShared();
  
  /**
   * @deprecated Use {@link PluginState#unload()}.
   * @since 2.1.0.0
   */  
  public void
  unload()
  
  	throws PluginException;

  /**
   * @deprecated Use {@link PluginState#reload()}.
   * @since 2.1.0.0
   */
  public void
  reload()
  
  	throws PluginException;
  
  	/**
  	 * Uninstall this plugin if it has been loaded from a plugin directory. Deletes the
  	 * plugin directory
     * @deprecated Use {@link PluginState#uninstall()}. 
  	 * @throws PluginException
  	 */
  
  public void
  uninstall()
  
  	throws PluginException;
  
  	/**
  	 * Indicates whether or not the current thread is the one responsible for running
  	 * plugin initialisation
  	 * @return
  	 */
  
  public boolean
  isInitialisationThread();
  
  /**
   * gives access to the plugin manager
   * @return
   *
   * @since 2.1.0.0
   */
  public PluginManager
  getPluginManager();
  
  	/**
  	 * 
  	 * @return
  	 * @since 2.2.0.3
  	 */
  
  public ClientIDManager
  getClientIDManager();
  
  
  /**
   * Get the connection manager.
   * @since 2.2.0.3
   * @return manager
   */
  public ConnectionManager getConnectionManager(); 
  
  
  /**
   * Get the peer messaging manager.
   * @since 2.2.0.3
   * @return manager
   */
  public MessageManager getMessageManager();
  
  
  /**
   * Get the distributed database
   * @since 2.2.0.3
   * @return
   */
  public DistributedDatabase
  getDistributedDatabase();
  
  /**
   * Gets the platform manager that gives access to native functionality
   * @return
   */
  
  public PlatformManager
  getPlatformManager();
  
  /**
   *
   * @since 2.0.7.0
   */
  public void
  addListener(
  	PluginListener	l );
  
  /**
   *
   * @since 2.0.7.0
   */
  public void
  removeListener(
  	PluginListener	l );
  
  	/**
  	 * Fire a plugin-specific event. See PluginEvent for details of type values to use 
  	 * @since 2403
  	 * @param event
  	 */
  
  public void
  firePluginEvent(
	PluginEvent		event );
  
  /**
   *
   * @since 2.0.8.0
   */
  public void
  addEventListener(
  	PluginEventListener	l );
  
  /**
   *
   * @since 2.0.8.0
   */
  public void
  removeEventListener(
  	PluginEventListener	l );
  
  /**
   * Returns the manager object for registering plugins that connect to the
   * Mainline DHT.
   *
   * @since 3.0.4.3
   */
  public MainlineDHTManager getMainlineDHTManager();
  
  /**
   * Returns an object that provides information the current state of the plugin,
   * and provides various mechanisms to query and control plugins and their
   * integration with Azureus at a low-level.
   * 
   * @since 3.1.1.1
   */
  public PluginState getPluginState();
}
