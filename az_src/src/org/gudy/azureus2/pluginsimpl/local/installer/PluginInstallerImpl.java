/*
 * Created on 28-Nov-2004
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

package org.gudy.azureus2.pluginsimpl.local.installer;

/**
 * @author parg
 *
 */

import java.io.File;
import java.io.InputStream;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.installer.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.pluginsimpl.local.FailedPlugin;
import org.gudy.azureus2.pluginsimpl.update.sf.*;

import org.gudy.azureus2.pluginsimpl.update.PluginUpdatePlugin;

public class 
PluginInstallerImpl
	implements PluginInstaller
{
	protected static PluginInstallerImpl	singleton;
	
	public static PluginInstallerImpl
	getSingleton(
		PluginManager	_manager )
	{
		if ( singleton == null ){
			
			singleton	= new PluginInstallerImpl( _manager );
		}
		
		return( singleton );
	}
	
	private PluginManager	manager;
	private List			listeners	 = new ArrayList();
	
	protected
	PluginInstallerImpl(
		PluginManager	_manager )
	{
		manager	= _manager;
	}
	
	protected PluginManager
	getPluginManager()
	{
		return( manager );
	}
	
	public StandardPlugin[]
	getStandardPlugins()
	
		throws PluginException
	{
		try{
			SFPluginDetailsLoader	loader = SFPluginDetailsLoaderFactory.getSingleton();
		
			SFPluginDetails[]	details = loader.getPluginDetails();

			List	res = new ArrayList();
				
			for (int i=0;i<details.length;i++){
				
				SFPluginDetails	detail = details[i];
				
				String	name 	= detail.getId();
				
				String	version = "";
				
				if ( Constants.isCVSVersion()){
					
					version = detail.getCVSVersion();
				}
				
				if ( version == null || version.length() == 0 || !Character.isDigit(version.charAt(0))){
					
					version = detail.getVersion();
					
				}else{
					
						// if cvs version and non-cvs version are the same then show the
						// non-cvs version
					
					String	non_cvs_version = detail.getVersion();
					
					if ( version.equals( non_cvs_version + "_CVS" )){
						
						version = non_cvs_version;
					}
				}
				
				if ( name.startsWith( "azplatform" ) || name.equals( "azupdater" )){
					
						// skip built in ones we don't want to let user install directly
						// not the cleanest of fixes, but it'll do for the moment
					
				}else if ( version == null || version.length() == 0 || !Character.isDigit(version.charAt(0))){
					
						// dodgy version
					
				}else if ( detail.getCategory().equalsIgnoreCase("hidden")){
					
						// not public
					
				}else{
					
					res.add( new StandardPluginImpl( this, details[i], version ));
				}
			}
			
			StandardPlugin[]	res_a = new StandardPlugin[res.size()];

			res.toArray( res_a );
			
			return( res_a );
			
		}catch( SFPluginDetailsException e ){
			
			throw( new PluginException("Failed to load standard plugin details", e ));
		}
	}
	
	public FilePluginInstaller
	installFromFile(
		File				file )
	
		throws PluginException
	{			
		return( new FilePluginInstallerImpl(this,file));
	}
	
	public void
	install(
		InstallablePlugin	installable_plugin,
		boolean				shared )
	
		throws PluginException
	{
		install( new InstallablePlugin[]{installable_plugin}, shared );
	}
	
	public void
	install(
		InstallablePlugin[]	plugins,
		boolean				shared )
	
		throws PluginException
	{
		PluginUpdatePlugin	pup = (PluginUpdatePlugin)manager.getPluginInterfaceByClass( PluginUpdatePlugin.class ).getPlugin();
		
		UpdateManager	uman = manager.getDefaultPluginInterface().getUpdateManager();
		
		UpdateCheckInstance	inst = 
			uman.createEmptyUpdateCheckInstance( 
					UpdateCheckInstance.UCI_INSTALL,
					"update.instance.install" );
		
		try{
			
			for (int i=0;i<plugins.length;i++){
				
				InstallablePlugin	plugin	= plugins[i];
				
				String	plugin_id = plugin.getId();
				
				PluginInterface	existing_plugin_interface = manager.getPluginInterfaceByID( plugin_id );
				
				Plugin			existing_plugin	= null;
				
				if ( existing_plugin_interface != null ){
					
					existing_plugin	= existing_plugin_interface.getPlugin();
					
						// try to check that the new version is higher than the old one!
					
					String	old_version = existing_plugin_interface.getPluginVersion();
					
					if ( old_version != null ){
						
						int	res = Constants.compareVersions( plugin.getVersion(), old_version );
						
						if ( res < 0 ){
							
							throw( new PluginException( "A higher version (" + old_version + ") of Plugin '" + plugin_id + "' is already installed" ));
							
						}else if ( res == 0 ){
							
							throw( new PluginException( "Version (" + old_version + ") of Plugin '" + plugin_id + "' is already installed" ));
						}
					}
				}
				
				String	target_dir;
				
				if ( shared ){
				    	    
					target_dir 	= FileUtil.getApplicationFile( "plugins" ).toString();
					
				}else{
					
					target_dir 	= FileUtil.getUserFile( "plugins" ).toString(); 
				}
				
				target_dir += File.separator + plugin_id;
		
					// this may fail on Vista but it doesn't matter as we recover this later
					// on. So *don't* check for success here
				
				new File( target_dir ).mkdir();
				
				if ( existing_plugin == null ){
					
						// create a dummy plugin at version 0.0 to trigger the "upgrade" to the new
						// installed version
					
					final FailedPlugin	dummy_plugin = new FailedPlugin( plugin_id, target_dir );
					
					PluginManager.registerPlugin( dummy_plugin, plugin_id );
				
					PluginInterface dummy_plugin_interface = manager.getPluginInterfaceByID( plugin_id );
					
					((InstallablePluginImpl)plugin).addUpdate( inst, pup, dummy_plugin, dummy_plugin_interface );
							
					inst.addListener(
						new UpdateCheckInstanceListener()
						{
							public void
							cancelled(
								UpdateCheckInstance		instance )
							{
								dummy_plugin.requestUnload();
							}
							
							public void
							complete(
								UpdateCheckInstance		instance )
							{
								dummy_plugin.requestUnload();
							}
						});
				}else{
					
					((InstallablePluginImpl)plugin).addUpdate( inst, pup, existing_plugin, existing_plugin_interface );
	
				}
			}
		
			inst.start();
			
		}catch( Throwable e ){
			
			inst.cancel();
			
			if ( e instanceof PluginException ){
				
				throw((PluginException)e);
			}else{
				
				throw( new PluginException( "Failed to create installer", e ));
			}
		}
	}
	
	public void
	uninstall(
		InstallablePlugin		standard_plugin )
	
		throws PluginException
	{
		PluginInterface	pi = standard_plugin.getAlreadyInstalledPlugin();
		
		if ( pi == null ){
			
			throw( new PluginException(" Plugin '" + standard_plugin.getId() + "' is not installed"));
		}
		
		pi.uninstall();
	}
	
	public void
	uninstall(
		final PluginInterface		pi )
	
		throws PluginException
	{
		uninstall( new PluginInterface[]{ pi });
	}
	
	public void
	uninstall(
		final PluginInterface[]		pis )
	
		throws PluginException
	{
		for (int i=0;i<pis.length;i++){
			
			PluginInterface	pi = pis[i];
			
			if ( pi.isMandatory()){
				
				throw( new PluginException( "Plugin '" + pi.getPluginID() + "' is mandatory, can't uninstall" ));
			}
			
			if ( pi.isBuiltIn()){
				
				throw( new PluginException( "Plugin '" + pi.getPluginID() + "' is built-in, can't uninstall" ));
			}
			
			String	plugin_dir = pi.getPluginDirectoryName();
			
			if ( plugin_dir == null || !new File(plugin_dir).exists()){
	
				throw( new PluginException( "Plugin '" + pi.getPluginID() + "' is not loaded from the file system, can't uninstall" ));
			}
		}
		
		try{
			UpdateManager	uman = manager.getDefaultPluginInterface().getUpdateManager();
			
			UpdateCheckInstance	inst = 
				uman.createEmptyUpdateCheckInstance(
						UpdateCheckInstance.UCI_UNINSTALL,
						"update.instance.uninstall");

			for (int i=0;i<pis.length;i++){
				
				final PluginInterface	pi = pis[i];
				
				final String	plugin_dir = pi.getPluginDirectoryName();

				inst.addUpdatableComponent(
					new UpdatableComponent()
					{
						public String
						getName()
						{
							return( pi.getPluginName());
						}
					
						public int
						getMaximumCheckTime()
						{
							return( 0 );
						}
						
						public void
						checkForUpdate(
							final UpdateChecker	checker )
						{
							try{
								ResourceDownloader rd = 
									manager.getDefaultPluginInterface().getUtilities().getResourceDownloaderFactory().create( new File( plugin_dir ));
								
								rd.addListener(
									new ResourceDownloaderAdapter()
									{
										public boolean
										completed(
											ResourceDownloader	downloader,
											InputStream			data )
										{
											try{
												if ( pi.isUnloadable()){
											
													pi.unload();
													
													FileUtil.recursiveDelete( new File( plugin_dir ));
												
												}
																
												UpdateInstaller installer = checker.createInstaller();
													
												installer.addRemoveAction( new File( plugin_dir ).getCanonicalPath());
											
												
											}catch( Throwable e ){
												
												Debug.printStackTrace(e);
												Logger.log(new LogAlert(LogAlert.REPEATABLE,
														"Plugin uninstall failed", e));
											}
											
												// don't close the stream as we process it later
												
											return( true );
										}
										
										public void
										failed(
											ResourceDownloader			downloader,
											ResourceDownloaderException e )
										{
											if ( !downloader.isCancelled()){
												Logger.log(new LogAlert(LogAlert.REPEATABLE,
														"Plugin uninstall failed", e));
											}
										}
									});
	
									// the plugin may have > 1 plugin interfaces, make the name up appropriately
								
								String	update_name = "";
									
								PluginInterface[]	ifs = manager.getPluginInterfaces();
								
							    Arrays.sort( 
							    		ifs,
									  	new Comparator()
										{
								      		public int 
											compare(
												Object o1, 
												Object o2)
								      		{
								      			return(((PluginInterface)o1).getPluginName().compareTo(((PluginInterface)o2).getPluginName()));
								      		}
										});
							    
								for (int i=0;i<ifs.length;i++){
									
									if ( ifs[i].getPluginID().equals(pi.getPluginID())){
									
										update_name += (update_name.length()==0?"":",") + ifs[i].getPluginName();
									}
								}
								
								checker.addUpdate(
									update_name,
									new String[]{ "Uninstall: " + plugin_dir},
									pi.getPluginVersion(),
									rd,
									pi.isUnloadable()?Update.RESTART_REQUIRED_NO:Update.RESTART_REQUIRED_YES );
									
							}finally{
								
								checker.completed();
							}
								
						}
					}, false );
			}

			inst.start();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected PluginInterface
	getAlreadyInstalledPlugin(
		String	id )
	{
		return( getPluginManager().getPluginInterfaceByID(id));
	}
	
	
	public void
	requestInstall(
		String				reason,
		InstallablePlugin 	plugin )
	
		throws PluginException
	{
		for (int i=0;i<listeners.size();i++){
			
			if (((PluginInstallerListener)listeners.get(i)).installRequest( reason, plugin )){
				
				return;
			}
		}
		
		throw( new PluginException( "No listeners registered to perform installation of '" + plugin.getName() +" (" + reason + ")" ));
	}
	
	public void
	addListener(
		PluginInstallerListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		PluginInstallerListener		l )
	{
		listeners.remove( l );
	}
}
