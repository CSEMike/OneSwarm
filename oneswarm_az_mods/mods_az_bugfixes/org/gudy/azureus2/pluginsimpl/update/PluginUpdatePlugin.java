/*
 * Created on 28-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.update;

/**
 * @author parg
 *
 */

import java.util.*;
import java.util.zip.*;
import java.net.URL;
import java.io.*;

import org.gudy.azureus2.core3.html.HTMLUtils;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.PluginInstaller;
import org.gudy.azureus2.plugins.installer.StandardPlugin;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.pluginsimpl.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.update.CorePatchChecker;

import com.aelitis.azureus.core.util.DeleteFileOnCloseInputStream;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
PluginUpdatePlugin
	implements Plugin
{
	private static final String PLUGIN_CONFIGSECTION_ID = "plugins.update";

	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;
		
	protected PluginInterface		plugin_interface;
	protected SFPluginDetailsLoader	loader;
	protected LoggerChannel 		log;
		
	private String	last_id_info	= "";
		
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Plugin Updater" );

		log = plugin_interface.getLogger().getChannel("Plugin Update");

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( 
					"Plugin Update");
		
		final PluginConfig	plugin_config = plugin_interface.getPluginconfig();
		
		boolean enabled = plugin_config.getPluginBooleanParameter( "enable.update", true );

		model.setConfigSectionID(PLUGIN_CONFIGSECTION_ID);
		model.getStatus().setText( enabled?"Running":"Optional checks disabled" );
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
			new LoggerChannelListener()
			{
				public void
				messageLogged(
					int		type,
					String	message )
				{
					model.getLogArea().appendText( message+"\n");
				}
				
				public void
				messageLogged(
					String		str,
					Throwable	error )
				{
					model.getLogArea().appendText( error.toString()+"\n");
				}
			});
		
		loader = SFPluginDetailsLoaderFactory.getSingleton();
		
		loader.addListener( 
			new SFPluginDetailsLoaderListener()
			{
				public void
				log(
					String	str )
				{
					log.log( LoggerChannel.LT_INFORMATION, "[" + str + "]" );
					
				}
			});
				
		BasicPluginConfigModel config = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, PLUGIN_CONFIGSECTION_ID);
		
		config.addBooleanParameter2( "enable.update", "Plugin.pluginupdate.enablecheck", true );
				
		UpdateManager	update_manager = plugin_interface.getUpdateManager();
		
		update_manager.addListener(
			new UpdateManagerListener()
			{
				public void
				checkInstanceCreated(
					UpdateCheckInstance	inst )
				{
					SFPluginDetailsLoaderFactory.getSingleton().reset();
				}
				
			});
		
		final PluginManager	plugin_manager = plugin_interface.getPluginManager();
		
		PluginInterface[]	plugins = plugin_manager.getPlugins();
			
		int mandatory_count 	= 0;
		int non_mandatory_count	= 0;
		
		for (int i=0;i<plugins.length;i++){
			
			PluginInterface	pi = plugins[i];
						
			boolean	pi_mandatory = pi.isMandatory();
						
			if ( pi_mandatory ){
					
				mandatory_count++;
					
			}else{
					
				non_mandatory_count++;
			}
		}
		
		final int f_non_mandatory_count	= non_mandatory_count;
		final int f_mandatory_count		= mandatory_count;
		
		update_manager.registerUpdatableComponent( 
			new UpdatableComponent()
			{
				public String
				getName()
				{
					return( "Non-mandatory plugins" );
				}
				
				public int
				getMaximumCheckTime()
				{
					return( f_non_mandatory_count * (( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000));
				}	
				
				public void
				checkForUpdate(
					UpdateChecker	checker )
				{
					if ( checkForUpdateSupport( checker, null, false ) == 0 ){
						
						String[] rps = VersionCheckClient.getSingleton(). getRecommendedPlugins();
						
						boolean	found_one = false;
						
						for (int i=0;i<rps.length;i++){
							
							String	rp_id = rps[i];
							
							if ( plugin_manager.getPluginInterfaceByID( rp_id ) != null ){
								
									// already installed
								
								continue;
							}
							
							final String	config_key = "recommended.processed." + rp_id;
							
							if ( !plugin_config.getPluginBooleanParameter( config_key, false )){
								
								try{
									final PluginInstaller installer = plugin_interface.getPluginManager().getPluginInstaller();
									
									StandardPlugin[]	sps = installer.getStandardPlugins();
									
									for (int j=0;j<sps.length;j++){
										
										final StandardPlugin	sp = sps[j];
										
										if ( sp.getId().equals( rp_id )){
										
											found_one = true;
											
											checker.getCheckInstance().addListener(
												new UpdateCheckInstanceListener()
												{
													public void
													cancelled(
														UpdateCheckInstance		instance )
													{													
													}
													
													public void
													complete(
														UpdateCheckInstance		instance )
													{
														if ( instance.getUpdates().length == 0 ){
															
															installRecommendedPlugin( installer, sp );
															
															plugin_config.setPluginParameter( config_key, true );
														}
													}
												});
											
											break;
										}
									}
									
								}catch( Throwable e ){
									
								}
							}
							
							if ( found_one ){
								
								break;
							}
						}
					}
				}
				
			}, false );
		
		update_manager.registerUpdatableComponent( 
				new UpdatableComponent()
				{
					public String
					getName()
					{
						return( "Mandatory plugins" );
					}
					
					public int
					getMaximumCheckTime()
					{
						return( f_mandatory_count * (( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000));
					}
					
					public void
					checkForUpdate(
						UpdateChecker	checker )
					{
						checkForUpdateSupport( checker, null, true );
					}			
				}, true );
		
		update_manager.addListener(
			new UpdateManagerListener()
			{
				public void
				checkInstanceCreated(
					UpdateCheckInstance	instance )
				{
					log.log( LoggerChannel.LT_INFORMATION, "**** Update check starts ****" );
				}
			});
	}
	
	protected void
	installRecommendedPlugin(
		PluginInstaller	installer,
		StandardPlugin	plugin )
	{	
		try{
			installer.requestInstall( MessageText.getString("plugin.installer.recommended.plugin"), plugin );
			
		}catch( Throwable e ){
			
			log.log(e);
		}
	}
	
	public UpdatableComponent
	getCustomUpdateableComponent(
		final String		id,
		final boolean		mandatory )
	{
		return(
			new UpdatableComponent()
			{
				public String
				getName()
				{
					return( "Installation of '" + id + "'" );
				}
				
				public int
				getMaximumCheckTime()
				{
					return( ( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000 );
				}
				
				public void
				checkForUpdate(
					UpdateChecker	checker )
				{
					checkForUpdateSupport( checker, new String[]{ id }, mandatory );
				}			
			});
	}
	
	protected  int
	checkForUpdateSupport(
		UpdateChecker	checker,
		String[]		ids_to_check,	// explicit ids or null for all
		boolean			mandatory )
	{
		int	num_updates_found = 0;
		
		try{
			if ( 	(!mandatory) &&
					(ids_to_check == null ) && 	// allow custom actions through
					(!plugin_interface.getPluginconfig().getPluginBooleanParameter( "enable.update", true ))){
								
				return( num_updates_found );
			}
			
			PluginInterface[]	plugins = plugin_interface.getPluginManager().getPlugins();
				
			List	plugins_to_check 			= new ArrayList();
			List	plugins_to_check_ids		= new ArrayList();
			Map		plugins_to_check_names		= new HashMap();
			
			for (int i=0;i<plugins.length;i++){
				
				PluginInterface	pi = plugins[i];
				
				if ( pi.isDisabled()){
					
					continue;
				}
				
				String	mand = pi.getPluginProperties().getProperty( "plugin.mandatory");
				
				boolean	pi_mandatory = mand != null && mand.trim().toLowerCase().equals("true");
				
				if ( pi_mandatory != mandatory ){
					
					continue;
				}
				
				String	id 		= pi.getPluginID();
				String	version = pi.getPluginVersion();
				String	name	= pi.getPluginName();
				
				if ( ids_to_check != null ){
				
					boolean	id_selected = false;
					
					for (int j=0;j<ids_to_check.length;j++){
						
						if ( ids_to_check[j].equals( id )){
							
							id_selected = true;
							
							break;
						}
					}
					
					if ( !id_selected ){
						
						continue;
					}
				}
				
				if ( version != null ){
					
					if ( plugins_to_check_ids.contains( id )){
						
						String	s = (String)plugins_to_check_names.get(id);
						
						if ( !name.equals( id )){
							
							plugins_to_check_names.put( id, s+","+name);
						}						
					}else{
						plugins_to_check_ids.add( id );
						
						plugins_to_check.add( pi );
						
						plugins_to_check_names.put( id, name.equals(id)?"":name);
					}
				}
				
				String location = pi.getPluginDirectoryName();
				
				log.log( LoggerChannel.LT_INFORMATION, (mandatory?"*":"-") + pi.getPluginName() + ", id = " + id + (version==null?"":(", version = " + pi.getPluginVersion())) + (location==null?"":( ", loc = " + location)));
			}
			
			String[]	ids = loader.getPluginIDs();
			
			String	id_info = "";
			
			for (int i=0;i<ids.length;i++){
				
				String	id = ids[i];
				
				SFPluginDetails	details = loader.getPluginDetails( id );
				
				id_info += (i==0?"":",") + ids[i] + "=" + details.getVersion()+"/"+details.getCVSVersion();
			}
			
			if ( !id_info.equals( last_id_info )){
				
				last_id_info	= id_info;
				
				log.log( LoggerChannel.LT_INFORMATION, "Downloaded plugin info = " + id_info );
			}
			
			for ( int i=0;i<plugins_to_check.size();i++){
				
				if ( checker.getCheckInstance().isCancelled()){
					
					throw( new Exception( "Update check cancelled" ));
				}
				
				final PluginInterface	pi_being_checked 	= (PluginInterface)plugins_to_check.get(i);
				final String			plugin_id 			= pi_being_checked.getPluginID();
					
				checker.reportProgress( "Loading details for " + plugin_id + "/" + pi_being_checked.getPluginName());

				boolean	found	= false;
				
				for (int j=0;j<ids.length;j++){
					
					if ( ids[j].equalsIgnoreCase( plugin_id )){
						
						found	= true;
						
						break;
					}
				}
				
				if ( !found ){
					
					if ( !pi_being_checked.isBuiltIn()){
						
						log.log( LoggerChannel.LT_INFORMATION, "Skipping " + plugin_id + " as not listed on web site");
					}

					continue;
				}
				
				String			plugin_names		= (String)plugins_to_check_names.get( plugin_id );
				//final boolean	plugin_unloadable 	= ((Boolean)plugins_to_check_unloadable.get( plugin_id )).booleanValue();
				
				log.log( LoggerChannel.LT_INFORMATION, "Checking " + plugin_id);
				
				try{
					
					SFPluginDetails	details = loader.getPluginDetails( plugin_id );
	
					if ( plugin_names.length() == 0 ){
						
						plugin_names = details.getName();
					}
					
					boolean az_cvs = plugin_interface.getUtilities().isCVSVersion();
					
					String pi_version_info = pi_being_checked.getPluginProperties().getProperty( "plugin.version.info" );
					
					String az_plugin_version	= pi_being_checked.getPluginVersion();
					
					String sf_plugin_version	= details.getVersion();
					
					String sf_comp_version		= sf_plugin_version;
					
					if ( az_cvs ){
						
						String	sf_cvs_version = details.getCVSVersion();
						
						if ( sf_cvs_version.length() > 0 ){
							
								// sf cvs version ALWAYS ends in _CVS
							
							sf_plugin_version	= sf_cvs_version;
							
							sf_comp_version = sf_plugin_version.substring(0,sf_plugin_version.length()-4);
						}
					}
					
					if (	 sf_comp_version.length() == 0 ||
							!Character.isDigit(sf_comp_version.charAt(0))){
						
						log.log( LoggerChannel.LT_INFORMATION, "Skipping " + plugin_id + " as no valid version to check");

						continue;					
					}
					
					// 	System.out.println("comp version = " + sf_comp_version );
					
					int	comp = PluginUtils.comparePluginVersions( az_plugin_version, sf_comp_version );
					
						// if they're the same version and latest is CVS then stick a _CVS on
						// the end of current to avoid confusion
					
					log.log( LoggerChannel.LT_INFORMATION, 
								"    Current: " + az_plugin_version + 
								(comp==0&&sf_plugin_version.endsWith( "_CVS")?"_CVS":"")+
								", Latest: " + sf_plugin_version + (pi_version_info==null?"":" [" + pi_version_info + "]"));
					
					if ( comp < 0 && ! ( pi_being_checked.getPlugin() instanceof UpdatableComponent)){
													
							// only update if newer verison + plugin itself doesn't handle
							// the update
						
						String sf_plugin_download	= details.getDownloadURL();
						
						if ( az_cvs ){
							
							String	sf_cvs_version = details.getCVSVersion();
							
							if ( sf_cvs_version.length() > 0 ){
								
								sf_plugin_download	= details.getCVSDownloadURL();
							}
						}

						log.log( LoggerChannel.LT_INFORMATION, "    Description:" );
						
						List	update_desc = new ArrayList();
						
						List	desc_lines = HTMLUtils.convertHTMLToText( "", details.getDescription());
						
						logMultiLine( "        ", desc_lines );
						
						update_desc.addAll( desc_lines );
						
						log.log( LoggerChannel.LT_INFORMATION, "    Comment:" );
						
						List	comment_lines = HTMLUtils.convertHTMLToText( "    ", details.getComment());

						logMultiLine( "    ", comment_lines );
						
						update_desc.addAll( comment_lines );
						
						String msg =   "A newer version (version " + sf_plugin_version + ") of plugin '" + 
										plugin_id + "' " +
										(plugin_names.length()==0?"":"(" + plugin_names + ") " ) +
										"is available. ";
						
						log.log( LoggerChannel.LT_INFORMATION, "" );
						
						log.log( 	LoggerChannel.LT_INFORMATION, "        " + msg + "Download from "+
									sf_plugin_download);
						
						ResourceDownloaderFactory rdf =  plugin_interface.getUtilities().getResourceDownloaderFactory();
						
						ResourceDownloader direct_rdl = rdf.create( new URL( sf_plugin_download ));

							// work out what the torrent download will be, if it exists 
							// sf_plugin_download will be something like ../plugins/safepeer_2.4.zip
							//     torrent is safepeer_2.4.zip.torrent
						

						String	torrent_download = Constants.AELITIS_TORRENTS;
						
						int	slash_pos = sf_plugin_download.lastIndexOf("/");
						
						if ( slash_pos == -1 ){
							
							torrent_download += sf_plugin_download;
							
						}else{
							
							torrent_download += sf_plugin_download.substring( slash_pos + 1 );
						}
						
						torrent_download	+= ".torrent";
						
						ResourceDownloader torrent_rdl = rdf.create( new URL( torrent_download ));

						torrent_rdl	= rdf.getSuffixBasedDownloader( torrent_rdl );
						
							// create an alternate downloader with torrent attempt first
						
						ResourceDownloader alternate_rdl = rdf.getAlternateDownloader( new ResourceDownloader[]{ torrent_rdl, direct_rdl });
						
							// get size so it is cached
						
						rdf.getTimeoutDownloader(rdf.getRetryDownloader(alternate_rdl,RD_SIZE_RETRIES),RD_SIZE_TIMEOUT).getSize();
																			
						String[]	update_d = new String[update_desc.size()];
						
						update_desc.toArray( update_d );
							
						num_updates_found++;
						
							// see if unloadable
						
						boolean	plugin_unloadable 	= true;
						
						for (int j=0;j<plugins.length;j++){
							
							PluginInterface pi = plugins[j];
							
							if ( pi.getPluginID().equals( plugin_id )){
								
								plugin_unloadable &= pi.isUnloadable();
							}
						}
						
						if ( plugin_unloadable ){
							
							checker.reportProgress( "Plugin is unloadable" );
						}
						
						Update update = 
							addUpdate( 
								pi_being_checked,
								checker,
								plugin_id + "/" + plugin_names,
								update_d,
								sf_plugin_version,
								alternate_rdl,
								sf_plugin_download.toLowerCase().endsWith(".jar"),
								plugin_unloadable?Update.RESTART_REQUIRED_NO:Update.RESTART_REQUIRED_YES,
								true );
			
						update.setRelativeURLBase( details.getRelativeURLBase());
						update.setDescriptionURL(details.getInfoURL());
					}
				}catch( Throwable e ){
					
					log.log("    Plugin check failed", e ); 
				}
			}
						
		}catch( Throwable e ){
			
			log.log("Failed to load plugin details", e );
			
			checker.failed();
			
		}finally{
			
				// any prior failure will take precedence
			
			checker.completed();
		}
		
		return( num_updates_found );
	}
	
	public Update
	addUpdate(
		final PluginInterface			pi_for_update,
		final UpdateChecker				checker,
		final String					update_name,
		final String[]					update_details,
		final String					version,
		final ResourceDownloader		resource_downloader,
		final boolean					is_jar,
		final int						restart_type,
		final boolean					verify )
	{
		final Update update = checker.addUpdate(
				update_name,
				update_details,
				version,
				resource_downloader,
				restart_type );

		update.setUserObject( pi_for_update );
		
		resource_downloader.addListener( 
			new ResourceDownloaderAdapter()
			{
				public boolean
				completed(
					final ResourceDownloader	downloader,
					InputStream					data )
				{	
						// during the update phase report any messages
						// to the downloader
					
					LoggerChannelListener	list = 
						new LoggerChannelListener()
						{
						public void
							messageLogged(
								int		type,
								String	content )
							{
								downloader.reportActivity( content );
							}
							
							public void
							messageLogged(
								String		str,
								Throwable	error )
							{
								downloader.reportActivity( str );
							}
						};
			
					try{
						
						log.addListener(list);
							
						installUpdate( 
								checker,
								update,
								pi_for_update,
								restart_type == Update.RESTART_REQUIRED_NO,
								is_jar,
								version, 
								data,
								verify );
						
						return( true );
					}finally{
						
						log.removeListener( list );
					}
				}
			});	
		
		return( update );
	}
	
	
	protected void
	installUpdate(
		UpdateChecker		checker,
		Update				update,
		PluginInterface		plugin,		// note this will be first one if > 1 defined
		boolean				unloadable,
		boolean				is_jar,		// false -> zip 
		String				version,
		InputStream			data,
		boolean				verify )
	{
		log.log( LoggerChannel.LT_INFORMATION,
				 "Installing plugin " + plugin.getPluginID() + ", version " + version );

		String	target_version = version.endsWith("_CVS")?version.substring(0,version.length()-4):version;

		UpdateInstaller	installer	= null;
		
		try{
		
			InputStream orig_data = data;
			data = update.verifyData( data, verify );
			
			log.log( "    Data verification stage complete" );
			
			boolean update_txt_found	= false;

			String plugin_dir_name = plugin.getPluginDirectoryName();
			
			if ( plugin_dir_name == null || plugin_dir_name.length() == 0 ){
				
					// update to a built-in plugin
				
				log.log( LoggerChannel.LT_INFORMATION, "    This is a built-in plugin, updating core" );

				CorePatchChecker.patchAzureus2( update.getCheckInstance(), data, plugin.getPluginID() + "_" + version, log );
				
					// always need to restart for this
				
				update.setRestartRequired( Update.RESTART_REQUIRED_YES );
				
			}else{

				final File	plugin_dir	= new File( plugin_dir_name );
				final File	user_dir	= new File( plugin_interface.getUtilities().getAzureusUserDir());
				final File	prog_dir	= new File( plugin_interface.getUtilities().getAzureusProgramDir());
	
					// .jar files get copied straight in with the right version number
					// .zip files need to be unzipped. There are various possibilities for
					// target dir depending on the contents of the zip file. Basically we
					// need to remove any zip paths to ensure it ends up in the right place
					// There's also the issue of overwriting stuff like "plugin.properties"
					// and any other config files....
					
				boolean	force_indirect_install = false;
				
					// for windows Vista we may not have write access to the plugin directory and have
					// to use an installer + restart to copy the files (as the restart can elevate
					// permissions)
				
				if ( Constants.isWindowsVistaOrHigher ){
					
						// test with .dll as this will fail write to virtual-store as required
					
					File	test_file = new File( plugin_dir, "_aztest45.dll" );
					
					boolean	ok = false;
					
					try{
						if ( test_file.exists()){
							
							test_file.delete();
						}

						FileOutputStream os = new FileOutputStream(test_file);
						os.write(32);
						os.close();

						ok = test_file.delete();
					}catch( Throwable e ){					
					}
					
					if ( !ok ){
						
						log.log( "Can't write directly to the plugin directroy, installing indirectly" );
						
						force_indirect_install = true;				
					}
				}
				
				File	target_plugin_dir;
				File	target_prog_dir;
				File	target_user_dir;
				
				if ( force_indirect_install ){
					
					File temp_dir = AETemporaryFileHandler.createTempDir();
					
					target_plugin_dir	= new File( temp_dir, "plugin" );
					target_user_dir		= new File( temp_dir, "user" );
					target_prog_dir		= new File( temp_dir, "prog" );
					
					target_plugin_dir.mkdirs();
					target_user_dir.mkdirs();
					target_prog_dir.mkdirs();
					
					installer = update.getCheckInstance().createInstaller();

					update.setRestartRequired( Update.RESTART_REQUIRED_YES );

				}else{
					
					target_plugin_dir	= plugin_dir;
					target_user_dir		= user_dir;
					target_prog_dir		= prog_dir;
				}
				
				File	target_jar_zip = new File( target_plugin_dir, plugin.getPluginID() + "_" + target_version + (is_jar?".jar":".zip"));
				

				FileUtil.copyFile( data, new FileOutputStream(target_jar_zip));
			
				if ( !is_jar ){
					
					/**
					 * PIAMOD: BUGFIX this never gets removed if zip file. 
					 * 
					 * But apparently vista needs it (since the updater is separate from the app), so leave for now. 
					 */
					//target_jar_zip.deleteOnExit();
					
					ZipInputStream	zis = 
						new ZipInputStream( 
								new BufferedInputStream( new FileInputStream( target_jar_zip ) ));
					
					
						// first look for a common dir prefix and platform-specific stuff
					
					String	common_prefix = null;
				
					String	selected_platform		= null;
					List	selected_sub_platforms	= new ArrayList();
					
					try{
						while( true ){
							
							ZipEntry	entry = zis.getNextEntry();
								
							if ( entry == null ){
								
								break;
							}
							
							String	name = entry.getName();
							
							if ( !( name.equals( "azureus.sig" ) || name.endsWith("/"))){
								
								if ( common_prefix == null ){
									
									common_prefix = name;
									
								}else{
									int	len = 0;
									
									for (int i=0;i<Math.min(common_prefix.length(), name.length());i++){
										
										if ( common_prefix.charAt(i) == name.charAt(i)){
											
											len++;
											
										}else{
											
											break;
										}
									}
									
									common_prefix = common_prefix.substring(0,len);
								}
								
								int	plat_pos = name.indexOf( "platform/" );
								
								if ( plat_pos != -1 ){
									
									plat_pos += 9;
									
									int	plat_end_pos = name.indexOf( "/", plat_pos );
									
									if ( plat_end_pos != -1 ){
										
										String	platform 		= name.substring( plat_pos, plat_end_pos );
										String	sub_platform 	= null;
										
										int	sub_plat_pos = platform.indexOf("_");
										
										if ( sub_plat_pos != -1 ){
											
											sub_platform = platform.substring( sub_plat_pos+1 );
											
											platform	= platform.substring(0,sub_plat_pos);
										}
																	
										if ( 	(Constants.isWindows && platform.equalsIgnoreCase( "windows" )) ||
												(Constants.isLinux && platform.equalsIgnoreCase( "linux" ))	||
												(Constants.isUnix && platform.equalsIgnoreCase( "unix" ))	||
												(Constants.isFreeBSD && platform.equalsIgnoreCase( "freebsd" ))	||
												(Constants.isSolaris && platform.equalsIgnoreCase( "solaris" ))	||
												(Constants.isOSX && platform.equalsIgnoreCase( "osx" ))){
											
										
											selected_platform = platform;
											
											if ( sub_platform != null ){
												
												if ( !selected_sub_platforms.contains( sub_platform )){
												
													selected_sub_platforms.add( sub_platform );
												}
											}
										}
									}
								}
							}
																		
							byte[]	buffer = new byte[65536];
							
							while( true ){
							
								int	len = zis.read( buffer );
								
								if ( len <= 0 ){
									
									break;
								}
							}
						}
					}finally{
						
						zis.close();
					}
	
					if ( selected_platform != null ){
						
						String[]	options = new String[selected_sub_platforms.size()];
						
						selected_sub_platforms.toArray( options );

						if ( options.length == 1 ){
							
							selected_platform += "_" + options[0];
							
							log.log( LoggerChannel.LT_INFORMATION,
									"platform is '" + selected_platform +"'" );

						}else if ( options.length > 1 ){
													
							String selected_sub_platform = (String) 
								update.getDecision(
										UpdateManagerDecisionListener.DT_STRING_ARRAY_TO_STRING,
										"Select Platform",
										"Multiple platform options exist for this plugin, please select required one",
										options );
							
							if ( selected_sub_platform == null ){
								
								throw( new Exception( "Valid sub-platform selection not selected" ));
								
							}else{
								
								selected_platform += "_" + selected_sub_platform;
								
								log.log( LoggerChannel.LT_INFORMATION,
										"platform is '" + selected_platform +"'" );
							}
						}
					}
	
					if ( common_prefix != null ){
						
						int	pos = common_prefix.lastIndexOf("/");
						
						if ( pos == -1 ){
							
							common_prefix = "";
						}else{
							
							common_prefix = common_prefix.substring(0,pos+1);
						}
									
						zis = new ZipInputStream( 
									new BufferedInputStream( new FileInputStream( target_jar_zip ) ));
											
						try{
							while( true ){
									
								ZipEntry	entry = zis.getNextEntry();
										
								if ( entry == null ){
										
									break;
								}
								
								String	name = entry.getName();
								
								OutputStream	entry_os = null;
								
								File			origin					= null;
								File			initial_target			= null;
								File			final_target			= null;
								boolean			is_plugin_properties 	= false;
								
								try{
									if ( 	name.length() >= common_prefix.length() &&
											!( name.equals( "azureus.sig") || name.endsWith("/"))){
										
										boolean	skip_file = false;
										
										String	file_name = entry.getName().substring( common_prefix.length());
										
										if ( selected_platform != null ){
											
											if ( file_name.indexOf( "platform/" ) != -1 ){
												
												String	bit_to_remove = "platform/" + selected_platform;
											
												int	pp = file_name.indexOf( bit_to_remove );
											
												if ( pp != -1 ){
												
													file_name = file_name.substring(0,pp) + file_name.substring(pp+bit_to_remove.length()+1);
																								
												}else{
												
														// stuff from other platform, ignore it
												
													skip_file	= true;
												}
											}
										}
										
										File	install_root;
										File	origin_root;
										
										if ( file_name.startsWith( "shared/lib" )){
											
												// updating shared resources, force restart
											
											update.setRestartRequired( Update.RESTART_REQUIRED_YES );

												// indicate that we can't go ahead and load the plugin
												// later in the code
											
											unloadable	= false;
											
											if ( plugin.isShared()){
												
												origin_root		= prog_dir;
												install_root 	= target_prog_dir;
											
											}else{
												
												origin_root		= user_dir;
												install_root 	= target_user_dir;
											}
										}else{
											
											origin_root		= plugin_dir;
											install_root 	= target_plugin_dir;
										}
										
										origin 			= new File( origin_root, file_name );
										initial_target 	= new File( install_root, file_name );
										
										final_target	= initial_target;
														
										if ( origin.exists()){
											
											if ( 	file_name.toLowerCase().endsWith(".properties") ||
													file_name.toLowerCase().endsWith(".config" )){
												
												is_plugin_properties	= file_name.toLowerCase().equals("plugin.properties");
												
												String	old_file_name = file_name;
												
												file_name = file_name + "_" + target_version;
												
												final_target = new File( install_root, file_name );
												
												log.log( LoggerChannel.LT_INFORMATION,
															"saving new file '" + old_file_name + "'as '" + file_name +"'" );
											}else{
												
													// if it is a versioned artifact then we don't need to do anything as the update contains
													// the same version of the file
												
												if ( isVersioned( file_name )){
													
													log.log( LoggerChannel.LT_INFORMATION,
															"Version '" + file_name +"' already present, skipping" );
													
													skip_file	= true;
													
												}else{
						
													log.log( LoggerChannel.LT_INFORMATION,
															"overwriting '" + file_name +"'" );
													
													File	backup = new File( origin.getParentFile(), origin.getName() + ".bak" );

														// back up just in case

													if ( force_indirect_install ){
														
														if ( backup.exists()){
														
															installer.addRemoveAction( backup.getAbsolutePath());
														}
														
														installer.addMoveAction( origin.getAbsolutePath(), backup.getAbsolutePath());
														
													}else{
														
														if ( backup.exists()){
															
															backup.delete();
														}
														
														if ( !initial_target.renameTo( backup )){
															
															log.log( LoggerChannel.LT_INFORMATION,
																	"    failed to backup '" + file_name +"', deferring until restart" );
		
															if ( installer == null ){
																
																update.setRestartRequired( Update.RESTART_REQUIRED_YES );
																
																installer = update.getCheckInstance().createInstaller();
															}
															
															File	tmp = new File( initial_target.getParentFile(), initial_target.getName() + ".tmp" );
		
															tmp.delete();
															
															installer.addMoveAction( tmp.getAbsolutePath(), initial_target.getAbsolutePath());
															
															final_target = tmp;
														}
													}
												}
											}
										}
										
										if ( !skip_file ){
																						
											FileUtil.mkdirs(final_target.getParentFile());
											
											entry_os = new FileOutputStream( final_target );
										}
									}
																		
									byte[]	buffer = new byte[65536];
									
									while( true ){
									
										int	len = zis.read( buffer );
										
										if ( len <= 0 ){
											
											break;
										}
																				
										if ( entry_os != null ){
											
											entry_os.write( buffer, 0, len );
										}
									}
								}finally{
									
									if ( entry_os != null ){
										
										entry_os.close();
									}
								}
								
								if ( is_plugin_properties ){
									
										// we've got to modify the plugin.version in the existing
										// file (if it exists) otherwise we keep downloading the new 
										// version! Or, if the key doesn't exist, add it!
									
										// if we were smarter we could merge values from the
										// old one into the new one, however this is too much like
										// hard work
															
										// hmm, we really need to at least merge in the new
										// predefined values such as 
										//	plugin.name, plugin.names
										//	plugin.class, plugin.classes
										//  plugin.langfile
									
									Properties	old_props 	= new Properties();
									Properties	new_props	= new Properties();
									
									List	props_to_delete		= new ArrayList();
									Map		props_to_replace	= new HashMap();
									Map		props_to_insert		= new HashMap();
									
									try{
										FileInputStream fis = new FileInputStream( origin );
										
										old_props.load( fis );
										
										try{
											fis.close();
											
										}catch( Throwable e ){
										}
										
										fis = new FileInputStream( final_target );
										
										new_props.load( fis );
								
										try{
											fis.close();
											
										}catch( Throwable e ){
										}
										
									}catch( Throwable e ){
										
										Debug.printStackTrace( e );
									}
									
									new_props.put( "plugin.version", target_version );
									
									String[]	prop_names = 
										{ 	"plugin.name", "plugin.names",
											"plugin.class", "plugin.classes",
											"plugin.version",
											"plugin.langfile"
										};
									
									for (int z=0;z<prop_names.length;z++){
										
										String	prop_name = prop_names[z];
										
										String	old_name = old_props.getProperty( prop_name );
										String	new_name = new_props.getProperty( prop_name );
											
										if ( new_name != null ){
											
											if ( prop_name.equals( "plugin.name")){
												props_to_delete.add( "plugin.names" );
											}else if ( prop_name.equals( "plugin.names")){
												props_to_delete.add( "plugin.name" );
											}else if ( prop_name.equals( "plugin.class")){
												props_to_delete.add( "plugin.classes" );
											}else if ( prop_name.equals( "plugin.classes")){
												props_to_delete.add( "plugin.class" );
											}
		
											if ( old_name == null ){
												
												props_to_insert.put( prop_name, new_name );
												
											}else if ( !new_name.equals( old_name )){
												
												props_to_replace.put( prop_name, new_name );									
											}
										}
									}
									
									File	tmp_file;
									
									if ( force_indirect_install ){
										
											// install into temp dir so we don't need to create a temp file and then rename
											// later as this installer will do this
										
										tmp_file = initial_target;
										
									}else{
										
										tmp_file = new File( initial_target.getParentFile(), initial_target.getName() + ".tmp" );
									}
									
									LineNumberReader	lnr = null;
									
									PrintWriter			tmp = null;
									
									try{
										lnr = new LineNumberReader(new FileReader( origin ));
									
										tmp = new PrintWriter(new FileWriter( tmp_file ));			
									
										Iterator	it = props_to_insert.keySet().iterator();
										
										while( it.hasNext()){
											
											String	pn = (String)it.next();
											
											String	pv = (String)props_to_insert.get(pn);
										
											log.log("    Inserting property:" + pn + "=" + pv );
											
											tmp.println( pn + "=" + pv );
										}	
									
										while(true){
											
											String	line = lnr.readLine();
											
											if ( line == null ){
												
												break;
											}
											
											int	ep = line.indexOf('=');
											
											if( ep != -1 ){
												
												String	pn = line.substring(0,ep).trim();
		
												if ( props_to_delete.contains(pn)){
													
													log.log("    Deleting property:" + pn );
													
												}else{
													
													String rv = (String)props_to_replace.get( pn );
													
													if ( rv != null ){
														
														log.log("    Replacing property:" + pn + " with " + rv );
													
														tmp.println( pn + "=" + rv );
														
													}else{
														
														tmp.println( line );
													}
												}
											}else{
		
												tmp.println( line );
											}
										}
									}finally{
										
										lnr.close();
										
										if ( tmp != null ){
											
											tmp.close();
										}
									}
									
									File	bak_file	= new File( origin.getParentFile(), origin.getName() + ".bak" );

									if ( force_indirect_install ){
										
										if ( bak_file.exists()){

											installer.addRemoveAction( bak_file.getAbsolutePath());
										}
										
										installer.addMoveAction( origin.getAbsolutePath(), bak_file.getAbsolutePath());
										
									}else{
										
										if ( bak_file.exists()){
											
											bak_file.delete();
										}
										
										if ( !initial_target.renameTo( bak_file)){
											
											throw( new IOException( "Failed to rename '" + initial_target.toString() + "' to '" + bak_file.toString() + "'" ));
										}
										
										if ( !tmp_file.renameTo( initial_target )){
											
											bak_file.renameTo( initial_target );
											
											throw( new IOException( "Failed to rename '" + tmp_file.toString() + "' to '" + initial_target.toString() + "'" ));
										}
										
										bak_file.delete();
									}
									
								}else if ( final_target != null && final_target.getName().equalsIgnoreCase( "update.txt" )){
									
									update_txt_found	= true;
									
									LineNumberReader lnr = null;
	
									try{
										lnr = new LineNumberReader( new FileReader( final_target ));
																			
										while(true){
											
											String	line = lnr.readLine();
											
											if (line == null ){
												
												break;
											}
											
											log.log( LoggerChannel.LT_INFORMATION, line );
										}
										
									}catch( Throwable e ){
										
										Debug.printStackTrace( e );
										
									}finally{
										
										if ( lnr != null ){
											
											lnr.close();
										}
									}
								}
							}
						}finally{
								
							zis.close();
							
						}
					}
				} // if ( !is_jar )
				
				if ( unloadable ){
					
						// 	check unloadability AGAIN in case changed during activities
					
					String	plugin_id = plugin.getPluginID();
					
					PluginInterface[]	plugins = plugin.getPluginManager().getPlugins();
					
					boolean	plugin_unloadable 	= true;
					
					for (int j=0;j<plugins.length;j++){
						
						PluginInterface pi = plugins[j];
						
						if ( pi.getPluginID().equals( plugin_id )){
							
							plugin_unloadable &= pi.isUnloadable();
						}
					}
					
					if ( !plugin_unloadable ){
						
						log.log( "Switching unloadability for " + plugin_id + " as changed during update" );
						
						update.setRestartRequired( Update.RESTART_REQUIRED_YES );
						
						unloadable = false;
					}
				}
				
				if ( force_indirect_install ){
					
						// create installation move actions for the files that have been installed
						// into temp location
					
					addInstallationActions( installer, target_plugin_dir, plugin_dir );
					addInstallationActions( installer, target_prog_dir, prog_dir );
					addInstallationActions( installer, target_user_dir, user_dir );					
				
						// don't delete temp store, it'll get deleted on restart
					
				}else if ( unloadable ){
					
					log.log( "Plugin initialising, please wait... " );
					
					plugin.reload();	// this will reload all if > 1 defined
					
					log.log( "... initialisation complete." );
	
				}	
			}
			
			String msg =   "Version " + version + " of plugin '" + 
							plugin.getPluginID() + "' " +
							"installed successfully";

			if ( update_txt_found ){
				
				msg += " - See update log for details";
			}
			
			log.logAlertRepeatable( update_txt_found?LoggerChannel.LT_WARNING:LoggerChannel.LT_INFORMATION, msg );			

		}catch( Throwable e ){
					
			String msg =   "Version " + version + " of plugin '" + 
							plugin.getPluginID() + "' " +
							"failed to install - " + (e.getMessage());
		
			log.logAlertRepeatable( LoggerChannel.LT_ERROR, msg );
			
		}finally{
			
			update.complete();
		}
	}
	
	protected void
	addInstallationActions(
		UpdateInstaller		installer,
		File				from_file,
		File				to_file )
	
		throws UpdateException
	{
		if ( from_file.isDirectory()){
			
			File[]	files = from_file.listFiles();
			
			if ( files != null ){
				
				for (int i=0;i<files.length;i++){
					
					addInstallationActions( installer, files[i], new File( to_file, files[i].getName()));
				}
			}
		}else{
			
			installer.addMoveAction( from_file.getAbsolutePath(), to_file.getAbsolutePath());
		}
	}
	
	protected boolean
	isVersioned(
		String	name )
	{
			// assumption: versioned names are of the form <prefix>_<version>.<extension>
			// where version is mixture of digits and .s
		
		int	pos = name.lastIndexOf('_');
		
		if ( pos == -1 || name.endsWith("_")){
			
			return( false );
		}
		
			// remove everything up to _
		
		String	rem = name.substring(pos+1);
		
		pos = rem.lastIndexOf( '.' );
		
			// remove extension (e.g. .jar)
		
		if ( pos != -1 ){
			
			rem = rem.substring(0,pos);
		}
		
		for (int i=0;i<rem.length();i++){
			
			char	c = rem.charAt(i);
			
			if ( c != '.' && !Character.isDigit( c )){
				
				return( false );
			}
		}
		
		return( true );
	}
	
	protected void
	logMultiLine(
		String		indent,
		List		lines )
	{
		for (int i=0;i<lines.size();i++){
			
			log.log( LoggerChannel.LT_INFORMATION, indent + (String)lines.get(i) );
		}
	}
}
