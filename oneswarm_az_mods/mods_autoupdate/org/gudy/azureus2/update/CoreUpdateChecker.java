/*
 * Created on 20-May-2004
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

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.html.*;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.update.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

import com.aelitis.azureus.core.versioncheck.*;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;


public class 
CoreUpdateChecker
	implements Plugin, UpdatableComponent
{		
	public static final String KEY_ONE_SWARM_MOD_VERSION = "oneswarm_az_mod_version";
	public static final String KEY_ONE_SWARM_MOD_URL = "oneswarm_az_mod_filename";
	public static final String KEY_AZUREUS_VERSION = "version";
	public static final String KEY_AZUREUS_URL = "filename";
	
	public static final String	LATEST_VERSION_PROPERTY	= "latest_version";
	public static final String	MESSAGE_PROPERTY		= "message";
	
	public static final int	RD_GET_DETAILS_RETRIES	= 3;
	public static final int	RD_GET_MIRRORS_RETRIES	= 3;
	
	public static final int	RD_SIZE_RETRIES	= 3;
	public static final int	RD_SIZE_TIMEOUT	= 10000;

	protected static CoreUpdateChecker		singleton;
	
	protected PluginInterface				plugin_interface;
	protected ResourceDownloaderFactory 	rdf;
	protected LoggerChannel					log;
	protected ResourceDownloaderListener	rd_logger;
	
	protected boolean						first_check		= true;
	
	public static void
	doUsageStats()
	{
		singleton.doUsageStatsSupport();
	}
	
	public
	CoreUpdateChecker()
	{
		singleton	= this;
	}
	
	protected void
	doUsageStatsSupport()
	{
		try{
			Map decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
			  			first_check?VersionCheckClient.REASON_UPDATE_CHECK_START:VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);
    	  
			displayUserMessage( decoded );
			
		}finally{
			
			  first_check	= false;
		}
	}

	
	public void
	initialize(
		PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "Core Updater" );
		
		log	= plugin_interface.getLogger().getChannel("CoreUpdater");
		
		rd_logger =
			new ResourceDownloaderAdapter()
			{
				public void
				reportActivity(
					ResourceDownloader	downloader,
					String				activity )
				{
					log.log( activity );
				}
			};
			
		Properties	props = plugin_interface.getPluginProperties();
							
		props.setProperty( "plugin.version", plugin_interface.getAzureusVersion());
		
		rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
	
		plugin_interface.getUpdateManager().registerUpdatableComponent( this, true );
	}
	
	public String
	getName()
	{
		return( "Azureus Core" );
	}
	
	public int
	getMaximumCheckTime()
	{
		return( ( RD_SIZE_RETRIES * RD_SIZE_TIMEOUT )/1000);
	}	

	// ******************************************************
	/*
	 * EDIT, by isdal
	 * 
	 * added function to update our own mods as well, not only the Azureus2.jar file
	 * 
	 * 
	 */
	
	public void checkForUpdate(final UpdateChecker checker) {
		try{

		String currentAzVersion = plugin_interface.getAzureusVersion();
		String currentModsVersion = Constants.getOneSwarmAzureusModsVersion();
		log.log("Update check starts: current = " + currentAzVersion);

		Map decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
				first_check ? VersionCheckClient.REASON_UPDATE_CHECK_START
						: VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);

		String latestAzVersion = new String(
				(byte[]) decoded.get(KEY_AZUREUS_VERSION));
		String latestModsVersion = new String(
				(byte[]) decoded.get(KEY_ONE_SWARM_MOD_VERSION));

		System.err.println("Checking for updates: AZVersion=" + currentAzVersion
				+ " ModsVersion=" + currentModsVersion);

		boolean azUpdateRequired = shouldUpdate(currentAzVersion, latestAzVersion);
		boolean modsUpdateRequired = shouldUpdate(currentModsVersion,
				latestModsVersion);

		// do we need to update?
		if (!azUpdateRequired && !modsUpdateRequired) {
			System.err.println("no update required");
			return;
		}
		// ok, so if we only need to update the mods
		else if (!azUpdateRequired && modsUpdateRequired) {
			// update, and restart after
			System.err.println("updating OneSwarm Az mods");
			checkForOneSwarmModUpdate(checker, true);
		} else if(azUpdateRequired && modsUpdateRequired) {
			// update both az core and mods, no restart after mods update
			System.err.println("updating both azureus2.jar and mods");
			checkForOneSwarmModUpdate(checker, false);
			checkForAzureusUpdate(checker);
		} else {
			System.err.println("Strange!!!, only az core update?");
			checkForAzureusUpdate(checker);
		}
		
	}catch( Throwable e ){
		
		log.log( e );
		
		Debug.printStackTrace( e );
		
		checker.failed();
		
	}finally{
		
		checker.completed();
		
		first_check = false;
	}
		
		

	}

	private void checkForOneSwarmModUpdate(final UpdateChecker checker,
			boolean restart) throws Exception {
		String current_version = Constants.getOneSwarmAzureusModsVersion();

		Map decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
				first_check ? VersionCheckClient.REASON_UPDATE_CHECK_START
						: VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);

		String latest_version;
		String latest_file_name;

		byte[] b_version = (byte[]) decoded.get(KEY_ONE_SWARM_MOD_VERSION);
		if (b_version != null) {
			latest_version = new String(b_version);
		} else {
			throw (new Exception("No version found in reply"));
		}

		byte[] b_filename = (byte[]) decoded.get(KEY_ONE_SWARM_MOD_URL);
		if (b_filename != null) {
			latest_file_name = new String(b_filename);
		} else {
			throw (new Exception("No update file details in reply"));
		}

		URL full_download_url;
		if (latest_file_name.startsWith("http")) {
			try {
				full_download_url = new URL(latest_file_name);
			} catch (Throwable e) {
				full_download_url = null;
				log.log(e);
			}

			int pos = latest_file_name.lastIndexOf('/');
			latest_file_name = latest_file_name.substring(pos + 1);
		} else {
			full_download_url = null;
		}

		String msg = "Core: latest_version = '" + latest_version + "', file = '"
				+ latest_file_name + "'";

		checker.reportProgress(msg);

		log.log(msg);

		if (!shouldUpdate(current_version, latest_version)) {
			System.out.println("Mods update not requred");
			return;
		}

		final String f_latest_version = latest_version;

		ResourceDownloader top_downloader;
		ResourceDownloader full_rd = rdf.create( full_download_url );		
		full_rd = rdf.getSuffixBasedDownloader( full_rd );

		top_downloader = 
			rdf.getAlternateDownloader( 
					new ResourceDownloader[]
						{
							full_rd
						});
	
	top_downloader.addListener( rd_logger );
		// get size so it is cached
	top_downloader.getSize();		
	
	String[]	desc;	
	desc = new String[]{"OneSwarm Core Update" };
	
	int restartPolicy;
	if(restart){
		restartPolicy = Update.RESTART_REQUIRED_YES;
	} else {
		restartPolicy = Update.RESTART_REQUIRED_NO;
	}
	
	final Update update = 
		checker.addUpdate(
				"OneSwarm Core Update",
				desc,
				latest_version,
				top_downloader,
				restartPolicy );
	
	
	top_downloader.addListener( 
			new ResourceDownloaderAdapter()
			{
				public boolean
				completed(
					final ResourceDownloader	downloader,
					InputStream					data )
				{	
					installModsUpdate( checker, update, downloader, f_latest_version, data );
							
					return( true );
				}
			});
	}
	
	

	private void
	checkForAzureusUpdate(
		final UpdateChecker	checker ) throws Exception
	{
		try{			
			String	current_version = plugin_interface.getAzureusVersion();
			
			log.log( "Update check starts: current = " + current_version );
									
			Map	decoded = VersionCheckClient.getSingleton().getVersionCheckInfo(
		  			first_check?VersionCheckClient.REASON_UPDATE_CHECK_START:VersionCheckClient.REASON_UPDATE_CHECK_PERIODIC);

//			System.out.println("az update response:");
//			for( Object key : decoded.keySet() )
//			{
//				System.out.println( "key: " + key + " (" + key.getClass().getName() + ") -> " + decoded.get(key) + " (" + decoded.get(key).getClass().getName() + ")");
//			}
			
			displayUserMessage( decoded );
			
			// No point complaining later if we don't have any data in the map (which is
			// more likely due to network problems rather than the version check server
			// *actually* returning a map with nothing in it. 
			if (decoded.isEmpty()) {return;}
			
			String latest_version;
			String latest_file_name;
			
			byte[] b_version = (byte[])decoded.get("version");
			
			if ( b_version != null ){
			
				latest_version = new String( b_version );
				
				plugin_interface.getPluginProperties().setProperty( LATEST_VERSION_PROPERTY, latest_version );
				
			}else{
				
				throw( new Exception( "No version found in reply" ));
			}
			
			byte[] b_filename = (byte[]) decoded.get("filename");
			
			if ( b_filename != null ){
			
				latest_file_name = new String( b_filename );
				
			}else{
				
				throw( new Exception( "No update file details in reply" ));
			}
			
			//latest_version 		= "3.0.0.3";
			//latest_file_name	= "http://torrents.aelitis.com:88/torrents/Azureus2.5.0.0.jar.torrent";
			//latest_file_name	= "Azureus2.5.0.0.jar.torrent";
			
			String	msg = "Core: latest_version = '" + latest_version + "', file = '" + latest_file_name + "'";
			
			URL		full_download_url;
			
				// since 2501 we support a full download URL, falling back to SF mirrors if this
				// fails. 
					
			if ( latest_file_name.startsWith( "http" )){
				
				try{
					full_download_url	= new URL( latest_file_name );
					
				}catch( Throwable e ){
					
					full_download_url = null;
					
					log.log( e );
				}
				
				int	pos = latest_file_name.lastIndexOf( '/' );
				
				latest_file_name = latest_file_name.substring( pos+1 );
				
			}else{
				
				full_download_url	= null;
			}
			
			checker.reportProgress( msg );
			
			log.log( msg );
			
			if ( !shouldUpdate( current_version, latest_version )){
				
				return;
			}
				
			final String	f_latest_version	= latest_version;
//			final String	f_latest_file_name	= latest_file_name;
			
			ResourceDownloader	top_downloader;
//			
//			if ( full_download_url == null ){
//				
//				ResourceDownloader[]	primary_mirrors;
//					
//				primary_mirrors = getPrimaryDownloaders( latest_file_name );
//	
//					// the download hierarchy is primary mirrors first (randomised alternate)
//					// then backup mirrors (randomised alternate)
//				
//					// we don't want to load the backup mirrors until the primary mirrors fail
//				
//				ResourceDownloader		random_primary_mirrors = rdf.getRandomDownloader( primary_mirrors );
//				
//				ResourceDownloader		backup_downloader =
//					rdf.create(
//						new ResourceDownloaderDelayedFactory()
//						{
//							public ResourceDownloader
//							create()
//							{
//								ResourceDownloader[]	backup_mirrors = getBackupDownloaders( f_latest_file_name );
//							
//								return( rdf.getRandomDownloader( backup_mirrors ));
//							}
//						});
//								
//				top_downloader = 
//					rdf.getAlternateDownloader( 
//							new ResourceDownloader[]
//								{
//									random_primary_mirrors,
//									backup_downloader,
//								});
//
//			}else{
				
				ResourceDownloader full_rd = rdf.create( full_download_url );
				
				full_rd = rdf.getSuffixBasedDownloader( full_rd );
				
//				ResourceDownloader		primary_downloader =
//					rdf.create(
//						new ResourceDownloaderDelayedFactory()
//						{
//							public ResourceDownloader
//							create()
//							{
//								ResourceDownloader[]	primary_mirrors = getPrimaryDownloaders( f_latest_file_name );
//							
//								return( rdf.getRandomDownloader( primary_mirrors ));
//							}
//						});
//					
//				ResourceDownloader		backup_downloader =
//					rdf.create(
//						new ResourceDownloaderDelayedFactory()
//						{
//							public ResourceDownloader
//							create()
//							{
//								ResourceDownloader[]	backup_mirrors = getBackupDownloaders( f_latest_file_name );
//							
//								return( rdf.getRandomDownloader( backup_mirrors ));
//							}
//						});
				
				
				top_downloader = 
					rdf.getAlternateDownloader( 
							new ResourceDownloader[]
								{
									full_rd//,
//									primary_downloader,
//									backup_downloader,
								});
//			}
			
			top_downloader.addListener( rd_logger );
			
				// get size so it is cached
			
			top_downloader.getSize();		
							

//			byte[]	info_b = (byte[])decoded.get( "info" );
//			
//			String	info = null;
//			
//			if ( info_b != null ){
//			
//				try{
//					info = new String( info_b );
//				
//				}catch( Throwable e ){
//					
//					Debug.printStackTrace( e );
//				}
//			}
//			
//			byte[] info_url_bytes = (byte[])decoded.get("info_url");
//			
//			String info_url = null;
//			
//			if ( info_url_bytes != null ){
//				
//				try{
//					info_url = new String( info_url_bytes );
//					
//				}catch( Exception e ){
//					
//					Debug.out(e);
//				}
//			}
//			
//			if ( info != null || info_url != null ){
//				
//				String	check;
//				
//				if ( info == null ){
//					
//					check = info_url;
//					
//				}else if ( info_url == null ){
//					
//					check = info;
//					
//				}else{
//					
//					check = info + "|" + info_url;
//				}
//				
//				byte[]	sig = (byte[])decoded.get( "info_sig" );
//
//				boolean	ok = false;
//				
//				if ( sig == null ){
//				
//					Logger.log( new LogEvent( LogIDs.LOGGER, "info signature check failed - missing signature" ));
//
//				}else{
//					
//					try{
//						AEVerifier.verifyData( check, sig );
//
//						ok = true;
//						
//					}catch( Throwable e ){
//
//						Logger.log( new LogEvent( LogIDs.LOGGER, "info signature check failed", e  ));
//					}			
//				}
//				
//				if ( !ok ){
//					
//					info		= null;
//					info_url	= null;
//				}
//			}
			
			String[]	desc;
			
//			if ( info == null ){
				
				desc = new String[]{"OneSwarm is based on the Azureus BitTorrent client" };
				
//			}else{
//				
//				desc = new String[]{"Core Azureus Version", info };
//			}
			


			final Update update = 
				checker.addUpdate(
						"Azureus Lib",
						desc,
						latest_version,
						top_downloader,
						Update.RESTART_REQUIRED_YES );
			
//			if ( info_url != null ){
//				
//				update.setDescriptionURL(info_url);
//			}
			
			top_downloader.addListener( 
					new ResourceDownloaderAdapter()
					{
						public boolean
						completed(
							final ResourceDownloader	downloader,
							InputStream					data )
						{	
							installAzureusUpdate( checker, update, downloader, f_latest_version, data );
									
							return( true );
						}
					});
		}catch( Throwable e ){
			
			log.log( e );
			
			Debug.printStackTrace( e );
			
			checker.failed();
			
		}finally{
			
			checker.completed();
			
			first_check = false;
		}
	}
	
      
  
  
  /**
   * Log and display a user message if contained within reply.
   * @param reply from server
   */
  private void 
  displayUserMessage( 
	Map reply ) 
  {
	  //  pick up any user message in the reply

	  try{
		  Iterator it = reply.keySet().iterator();
		  
		  while( it.hasNext()){
			  
			  String	key = (String)it.next();
		  
			  	// support message + message_sig
			  	//		message_1 + message_sig_1   etc
			  
			  if ( key.startsWith( "message_sig" ) || !key.startsWith( "message" )){
				  
				  continue;
			  }
			  
			  byte[]  message_bytes = (byte[])reply.get( key );
	
			  if ( message_bytes != null && message_bytes.length > 0 ){
	
				  String  message = new String(message_bytes);
	
				  String sig_key;
				  
				  int	pos = key.indexOf('_');
				  
				  if ( pos == -1 ){
					  
					  sig_key = "message_sig";
					  
				  }else{
					  
					  sig_key = "message_sig" + key.substring( pos );
				  }
				  
				  String	last_message_key = "CoreUpdateChecker.last" + key;
				  
				  String  last = COConfigurationManager.getStringParameter( last_message_key, "" );
	
				  if ( !message.equals( last )){
	
					  byte[]	signature = (byte[])reply.get( sig_key );
	
					  if ( signature == null ){
	
						  Logger.log( new LogEvent( LogIDs.LOGGER, "Signature missing from message" ));
	
						  return;
					  }
	
					  try{
						  AEVerifier.verifyData( message, signature );
	
					  }catch( Throwable e ){
	
						  Logger.log( new LogEvent( LogIDs.LOGGER, "Message signature check failed", e  ));
	
						  return;
					  }
	
					  boolean	completed = false;
					  
					  if ( message.startsWith( "x:" )){
						  
						  	// emergency patch application
						  
						  try{
							  URL jar_url = new URL( message.substring(2));
							  
							  Logger.log( new LogEvent( LogIDs.LOGGER, "Patch application requsted: url=" + jar_url ));

							  File	temp_dir = AETemporaryFileHandler.createTempDir();
							  
							  File	jar_file = new File( temp_dir, "patch.jar" );
							  
							  InputStream is = rdf.create( jar_url ).download();
							  
							  try{
								  FileUtil.copyFile( is, jar_file );
								  
								  is = null;
								  
								  AEVerifier.verifyData( jar_file );
								  
								  ClassLoader cl = CoreUpdateChecker.class.getClassLoader();
								      		
								  if ( cl instanceof URLClassLoader ){

									  URL[]	old = ((URLClassLoader)cl).getURLs();

									  URL[]	new_urls = new URL[old.length+1];

									  System.arraycopy( old, 0, new_urls, 1, old.length );

									  new_urls[0]= jar_file.toURL();

									  cl = new URLClassLoader( new_urls, cl );

								  }else{

									  cl = new URLClassLoader( new URL[]{jar_file.toURL()}, cl );
								  }
	
								  Class cla = cl.loadClass( "org.gudy.azureus2.update.version.Patch" );
								  
								  cla.newInstance();
								  
								  completed = true;
								  
							  }finally{
								  
								  if ( is != null ){
								  
									  is.close();
								  }
							  }
						  }catch( Throwable e ){
							  
							  Logger.log( new LogEvent( LogIDs.LOGGER, "Patch application failed", e  ));
						  }
					  } else if ( message.startsWith("u:") && message.length() > 4 ) {
					  	try {
  					  	String type = message.substring(2, 3);
  					  	String url = message.substring(4);
  					  	UIFunctions uif = UIFunctionsManager.getUIFunctions();
  					  	if (uif != null) {
  					  		uif.viewURL(url, null, 0.9, 0.9, true, type.equals("1"));
  					  	}
					  	} catch (Throwable t) {
							  Logger.log( new LogEvent( LogIDs.LOGGER, "URL message failed", t  ));
					  	}
					  	// mark as complete even if errored
						  completed = true;
					  }else{
						  
						  int   alert_type    = LogAlert.AT_WARNING;
						  
						  String  alert_text    = message;
		
						  if ( alert_text.startsWith("i:" )){
		
							  alert_type = LogAlert.AT_INFORMATION;
		
							  alert_text = alert_text.substring(2);
						  }
		
						  plugin_interface.getPluginProperties().setProperty( MESSAGE_PROPERTY, alert_text );
		
						  Logger.log(new LogAlert(LogAlert.UNREPEATABLE, alert_type, alert_text));
						  
						  completed = true;
					  }
					  
					  if ( completed ){
						  
						  COConfigurationManager.setParameter( last_message_key, message );
	
						  COConfigurationManager.save();
					  }
				  }
			  }
		  }
	  }catch( Throwable e ){

		  Debug.printStackTrace( e );
	  }
  } 
	
	protected ResourceDownloader[]
	getPrimaryDownloaders(
		String		latest_file_name )
	{
		log.log( "Downloading primary mirrors" );
		
		List	res = new ArrayList();

		try{
			if ( latest_file_name == null ){
		
					// very old method, non-mirror based
				
				res.add( new URL( Constants.SF_WEB_SITE + "Azureus2.jar" ));
								
			}else{
		
				URL mirrors_url = new URL("http://prdownloads.sourceforge.net/azureus/" + latest_file_name + "?download");
				
				ResourceDownloader	rd = rdf.create( mirrors_url );
				
				rd = rdf.getRetryDownloader( rd, RD_GET_MIRRORS_RETRIES );
				
				rd.addListener( rd_logger );
				
				String	page = HTMLPageFactory.loadPage( rd.download()).getContent();
				
				String pattern = "/azureus/" + latest_file_name + "?use_mirror=";
	     
				int position = page.indexOf(pattern);
				
				while ( position > 0 ){
					
					int end = page.indexOf(">", position);
					
					if (end < 0) {
						
						position = -1;
						
					}else{
						
						String mirror = page.substring(position, end);
						
						if ( mirror.endsWith("\"")){
							
							mirror = mirror.substring(0,mirror.length()-1);
						}
						
						try{
							res.add( new URL( "http://prdownloads.sourceforge.net" + mirror ));
							
						}catch( Throwable e ){
							
							log.log( "Invalid URL read:" + mirror, e );
						}
	          
						position = page.indexOf(pattern, position + 1);
					}
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to read primary mirror list", e );
		}
		
		ResourceDownloader[]	dls = new ResourceDownloader[res.size()];
				
		for (int i=0;i<res.size();i++){
			
			URL	url =(URL)res.get(i);
			
			log.log( "    Primary mirror:" +url.toString());
			
			ResourceDownloader dl = rdf.create( url );
			
			dl = rdf.getMetaRefreshDownloader( dl );
			
				// add in a layer to do torrent based downloads if url ends with .torrent
			
			dl = rdf.getSuffixBasedDownloader( dl );
			
			dls[i] = dl;
		}
		
		return( dls );
	}
	
	protected ResourceDownloader[]
	getBackupDownloaders(
		String	latest_file_name )
	{
		List	res = new ArrayList();
	
		try{
			if ( latest_file_name != null ){
							
				log.log( "Downloading backup mirrors" );
				
				URL mirrors_url = new URL("http://azureus.sourceforge.net/mirrors.php");
				
				ResourceDownloader	rd = rdf.create( mirrors_url );
				
				rd = rdf.getRetryDownloader( rd, RD_GET_MIRRORS_RETRIES );
				
				rd.addListener( rd_logger );
				
				BufferedInputStream	data = new BufferedInputStream(rd.download());
				
				Map decoded = BDecoder.decode(data);
				
				data.close();
				
				List mirrors = (List)decoded.get("mirrors");
		
				for (int i=0;i<mirrors.size();i++){
					
					String mirror = new String( (byte[])mirrors.get(i));
					
					try{
						
						res.add( new URL( mirror + latest_file_name ));
						// res.add( new URL( "http://torrents.aelitis.com:88/torrents/Azureus2.4.0.2_signed.jar.torrent" ));
						
					}catch(Throwable e){
						
						log.log( "Invalid URL read:" + mirror, e );
					}
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to read backup mirror list", e );
		}
		
		ResourceDownloader[]	dls = new ResourceDownloader[res.size()];
		
		for (int i=0;i<res.size();i++){
			
			URL	url =(URL)res.get(i);
			
			log.log( "    Backup mirror:" +url.toString());
			
			ResourceDownloader dl = rdf.create( url );

				// add in .torrent decoder if appropriate
			
			dl = rdf.getSuffixBasedDownloader( dl );

			dls[i] = dl;
		}
		
		return( dls );
	}       	

	protected void
	installAzureusUpdate(
		UpdateChecker		checker,
		Update				update,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			data = update.verifyData( data, true );

			rd.reportActivity( "Data verified successfully" );
			
			String	temp_jar_name 	= "Azureus2_" + version + ".jar";
			String	target_jar_name	= "Azureus2.jar";
			
			UpdateInstaller	installer = checker.createInstaller();
			
			installer.addResource( temp_jar_name, data );
			
			// **************************************************
			/*
			 * EDIT: by isdal
			 * Ok, so we have slighly different paths, and they 
			 * should use the function in SystemProperties...
			 */
			//			if ( Constants.isOSX ){
			//				
			//				installer.addMoveAction( 
			//					temp_jar_name,
			//					installer.getInstallDir() + "/" + SystemProperties.getApplicationName() + ".app/Contents/Resources/Java/" + target_jar_name );        
			//			}else{
			//				
			//				installer.addMoveAction( 
			//					temp_jar_name,
			//					installer.getInstallDir() + File.separator + File.separator + target_jar_name );
			//			}
			installer.addMoveAction(temp_jar_name, SystemProperties.getAzureusJarPath());
			//*******************************************************
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
	
	protected void
	installModsUpdate(
		UpdateChecker		checker,
		Update				update,
		ResourceDownloader	rd,
		String				version,
		InputStream			data )
	{
		try{
			//System.out.println("We should verify our mods jar...");
			data = update.verifyData( data, true );

			rd.reportActivity( "Data verified successfully" );
			
			String	temp_jar_name 	= "OneSwarmAzMods" + version + ".jar";
				
			UpdateInstaller	installer = checker.createInstaller();
			installer.addResource( temp_jar_name, data );
			installer.addMoveAction(temp_jar_name, SystemProperties.getOneSwarmModsJar());
		}catch( Throwable e ){
			
			rd.reportActivity("Update install failed:" + e.getMessage());
		}
	}
	
	protected static boolean
	shouldUpdate(
		String	current_version,
		String	latest_version )
	{
		String	current_base	= Constants.getBaseVersion( current_version );
		int		current_inc		= Constants.getIncrementalBuild( current_version );

		String	latest_base	= Constants.getBaseVersion( latest_version );
		int		latest_inc	= Constants.getIncrementalBuild( latest_version );
			
			// currently we upgrade from, for example
			//  1) 2.4.0.0     -> 2.4.0.2
			//	2) 2.4.0.1_CVS -> 2.4.0.2
			//	3) 2.4.0.1_B12 -> 2.4.0.2  and 2.4.0.1_B14
		
			// but NOT
			//  1) 2.4.0.0 	   -> 2.4.0.1_CVS or 2.4.0.1_B23
			//  2) 2.4.0.1_CVS -> 2.4.0.1_B23
		
			// for inc values: 0 = not CVS, -1 = _CVS, > 0 = Bnn

		int	major_comp = Constants.compareVersions( current_base, latest_base );
		
		if ( major_comp < 0 && latest_inc == 0 ){
			
			return( true );		// latest is higher version and not CVS
		}
		
			// same version, both are B versions and latest B is more recent
		
		return( major_comp == 0 && current_inc > 0 && latest_inc > 0 && latest_inc > current_inc );
	}
	
	public static void
	main(
		String[]	args )
	{
		String[][]	tests = {
				{ "2.4.0.0", 		"2.4.0.2", 		"true" },
				{ "2.4.0.1_CVS", 	"2.4.0.2", 		"true" },
				{ "2.4.0.1_B12",	"2.4.0.2", 		"true" },
				{ "2.4.0.1_B12", 	"2.4.0.1_B34",	"true" },
				{ "2.4.0.1_B12", 	"2.4.0.1_B6",	"false" },
				{ "2.4.0.0", 		"2.4.0.1_CVS",	"false" },
				{ "2.4.0.0", 		"2.4.0.1_B12",	"false" },
				{ "2.4.0.0", 		"2.4.0.0"	,	"false" },
				{ "2.4.0.1_CVS", 	"2.4.0.1_CVS",	"false" },
				{ "2.4.0.1_B2", 	"2.4.0.1_B2",	"false" },
				{ "2.4.0.1_CVS", 	"2.4.0.1_B2",	"false" },
				{ "2.4.0.1_B2", 	"2.4.0.1_CVS",	"false" },

		};

		for (int i=0;i<tests.length;i++){
			
			System.out.println( shouldUpdate(tests[i][0],tests[i][1]) + " / " + tests[i][2] );
		}
		
		/*
		AEDiagnostics.startup();
		
		CoreUpdateChecker	checker = new CoreUpdateChecker();
		
		checker.log = new LoggerImpl(null).getTimeStampedChannel("");
		checker.rdf	= new ResourceDownloaderFactoryImpl();
		checker.rd_logger = 
			new ResourceDownloaderAdapter()
			{
				public void
				reportActivity(
					ResourceDownloader	downloader,
					String				activity )
				{
					System.out.println( activity );
				}
				
				public void
				reportPercentComplete(
					ResourceDownloader	downloader,
					int					percentage )
				{
					System.out.println( "    % = " + percentage );
				}
			};
			
		ResourceDownloader[]	primaries = checker.getPrimaryDownloaders( "Azureus-2.0.3.0.jar" );
		
		for (int i=0;i<primaries.length;i++){
			
			System.out.println( "primary: " + primaries[i].getName());
		}
		
		try{
			ResourceDownloader	rd = primaries[0];
			
			rd.addListener( checker.rd_logger );
			
			rd.download();
			
			System.out.println( "done" );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		*/
	}
}
