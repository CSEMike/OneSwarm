/*
 * Created on 27-Apr-2004
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

package org.gudy.azureus2.pluginsimpl.update.sf.impl2;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.html.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.logging.*;

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader, ResourceDownloaderListener
{
	private static final LogIDs LOGID = LogIDs.CORE;

	//***********************************************************************
	/*
	 * EDIT, by isdal
	 * use our plugin page, so we can add our own plugins
	 */
	//private static final String	site_prefix = "http://azureus.sourceforge.net/";
	//private static final String	site_prefix = "http://update.oneswarm.org/";
	private static final String	site_prefix = "http://" + Constants.VERSION_SERVER_V4 + "/";
	//***********************************************************************
	private static String	base_url_params;
	
	static{
		
		base_url_params = "version=" + Constants.AZUREUS_VERSION + "&app=" + SystemProperties.getApplicationName();
		
		try{
			base_url_params += "&os=" + URLEncoder.encode(System.getProperty( "os.name"),"UTF-8" );
			
			base_url_params += "&arch=" + URLEncoder.encode(System.getProperty( "os.arch"),"UTF-8" );
			
			base_url_params += "&ui=" + URLEncoder.encode(COConfigurationManager.getStringParameter("ui"),"UTF-8" );
			
			base_url_params += "&java=" + URLEncoder.encode(System.getProperty( "java.version" ),"UTF-8" );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	private static String	page_url 	= site_prefix + "update/pluginlist3.php?type=&" + base_url_params;

	
	static{
		try{
			PlatformManager pm = PlatformManagerFactory.getPlatformManager();
			
			if ( pm.hasCapability( PlatformManagerCapabilities.GetVersion )){
				
				page_url += "&pmv=" + pm.getVersion();
			}
			
		}catch( Throwable e ){
			
		}
	}
	
	private static SFPluginDetailsLoaderImpl		singleton;
	private static AEMonitor		class_mon		= new AEMonitor( "SFPluginDetailsLoader:class" );

	private static final int		RELOAD_MIN_TIME	= 60*60*1000;
	
	public static SFPluginDetailsLoader
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton	= new SFPluginDetailsLoaderImpl();
			}
			
			return( singleton );
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected boolean	plugin_ids_loaded;
	protected long		plugin_ids_loaded_at;
	
	protected List		plugin_ids;
	protected Map		plugin_map;
	
	protected List		listeners			= new ArrayList();
	
	protected ResourceDownloaderFactory rd_factory = ResourceDownloaderFactoryImpl.getSingleton();

	protected AEMonitor		this_mon		= new AEMonitor( "SFPluginDetailsLoader" );

	protected
	SFPluginDetailsLoaderImpl()
	{
		reset();
	}
	
	protected String
	getRelativeURLBase()
	{
		return( site_prefix );
	}
	
	protected void
	loadPluginList()
	
		throws SFPluginDetailsException
	{
		try{			
			ResourceDownloader dl = rd_factory.create( new URL(page_url));

			dl = rd_factory.getRetryDownloader( dl, 5 );
			
			dl.addListener( this );
			
			Properties	details = new Properties();
			
			InputStream is = dl.download();
			
			details.load( is );
			
			is.close();
			
			Iterator it = details.keySet().iterator();
			
			while( it.hasNext()){
				
				String	plugin_id 	= (String)it.next();
				
				String	data			= (String)details.get(plugin_id);

				int	pos = 0;
				
				List	bits = new ArrayList();
				
				while( pos < data.length()){
					
					int	p1 = data.indexOf(';',pos);
					
					if ( p1 == -1 ){
						
						bits.add( data.substring(pos).trim());
					
						break;
					}else{
						
						bits.add( data.substring(pos,p1).trim());
						
						pos = p1+1;
					}
				}
				
				if (bits.size() < 3) {
					Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
							"SF loadPluginList failed for plugin '" + plugin_id
									+ "'.  Details array is " + bits.size() + " (3 min)"));
				} else {
					String version = (String) bits.get(0);
					String cvs_version = (String) bits.get(1);
					String name = (String) bits.get(2);
					String category = "";

					if (bits.size() > 3) {
						category = (String) bits.get(3);
					}

					plugin_ids.add(plugin_id);

					plugin_map.put(plugin_id.toLowerCase(), new SFPluginDetailsImpl(this,
							plugin_id, version, cvs_version, name, category));
				}
			}
			
			plugin_ids_loaded	= true;
			
			plugin_ids_loaded_at	= SystemTime.getCurrentTime();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new SFPluginDetailsException( "Plugin list load failed", e ));
		}
	}
	
	protected void
	loadPluginDetails(
		SFPluginDetailsImpl		details )
	
		throws SFPluginDetailsException
	{
		try{
			ResourceDownloader p_dl = rd_factory.create( new URL( site_prefix + "plugin_details.php?plugin=" + details.getId() + "&" + base_url_params ));
		
			p_dl = rd_factory.getRetryDownloader( p_dl, 5 );
		
			p_dl.addListener( this );

			HTMLPage	plugin_page = HTMLPageFactory.loadPage( p_dl.download());
			
			if ( !processPluginPage( details, plugin_page )){
							
				throw( new SFPluginDetailsException( "Plugin details load fails for '" + details.getId() + "': data not found" ));
			}
					
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			throw( new SFPluginDetailsException( "Plugin details load fails", e ));
		}
	}
	
	protected boolean
	processPluginPage(
		SFPluginDetailsImpl		details,
		HTMLPage				page )
	
		throws SFPluginDetailsException
	{
		HTMLTable[]	tables = page.getTables();
		
		// dumpTables("", tables );
		
		return( processPluginPage( details, tables ));
	}
	
	protected boolean
	processPluginPage(
		SFPluginDetailsImpl		details,
		HTMLTable[]				tables )
	
		throws SFPluginDetailsException
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	table = tables[i];
			
			HTMLTableRow[]	rows = table.getRows();
		
			if ( rows.length == 10 ){
				
				HTMLTableCell[]	cells = rows[0].getCells();
				
				if ( cells.length == 6 &&
						cells[0].getContent().trim().equals("Name") &&
						cells[5].getContent().trim().equals("Contact")){
				
					
					// got the plugin details table
				
					HTMLTableCell[]	detail_cells = rows[2].getCells();
					
					//String	plugin_name			= detail_cells[0].getContent();
					//String	plugin_version		= detail_cells[1].getContent();
					String	plugin_auth			= detail_cells[4].getContent();
					
					String[]	dl_links = detail_cells[2].getLinks();
					
					String	plugin_download;
					
					if ( dl_links.length == 0 ){
						
						plugin_download	= "<unknown>";
						
					}else{
						
						plugin_download = site_prefix + dl_links[0];
					}
					
					HTMLTableCell[]	cvs_detail_cells = rows[3].getCells();

					// String	plugin_cvs_version		= cvs_detail_cells[1].getContent();

					String[]	cvs_dl_links 		= cvs_detail_cells[2].getLinks();
					
					String	plugin_cvs_download;
					
					if ( cvs_dl_links.length == 0 ){
						
						plugin_cvs_download	= "<unknown>";
						
					}else{
						
						plugin_cvs_download = site_prefix + cvs_dl_links[0];
					}
					
					String info_url = null;
					if (rows[9].getCells().length > 1) {
						info_url = rows[9].getCells()[1].getContent();
					}


					// System.out.println( "got plugin:" + plugin_name + "/" + plugin_version + "/" + plugin_download + "/" + plugin_auth );
					
					details.setDetails(
									plugin_download,
									plugin_auth,
									plugin_cvs_download,
									rows[6].getCells()[0].getContent(),
									rows[9].getCells()[0].getContent(),
									info_url);
					
					return( true );
				}
			}
			
			HTMLTable[]	sub_tables = table.getTables();
			
			boolean	res = processPluginPage( details, sub_tables );
			
			if( res ){
				
				return( res );
			}
		}
		
		return( false );
	}
	
	protected void
	dumpTables(
		String			indent,
		HTMLTable[]		tables )
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	tab = tables[i];
			
			System.out.println( indent + "tab:" + tab.getContent());
			
			HTMLTableRow[] rows = tab.getRows();
			
			for (int j=0;j<rows.length;j++){
				
				HTMLTableRow	row = rows[j];
				
				System.out.println( indent + "  row[" + j + "]: " + rows[j].getContent());
				
				HTMLTableCell[]	cells = row.getCells();
				
				for (int k=0;k<cells.length;k++){
					
					System.out.println( indent + "    cell[" + k + "]: " + cells[k].getContent());
					
				}
			}
			
			dumpTables( indent + "  ", tab.getTables());
		}
	}
	
	public String[]
	getPluginIDs()
		
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
			if ( !plugin_ids_loaded ){
				
				loadPluginList();
			}
			
			String[]	res = new String[plugin_ids.size()];
			
			plugin_ids.toArray( res );
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException
	{
		try{
			this_mon.enter();
		
				// make sure details are loaded
			
			getPluginIDs();
			
			SFPluginDetails details = (SFPluginDetails)plugin_map.get(name.toLowerCase()); 
			
			if ( details == null ){
				
				throw( new SFPluginDetailsException( "Plugin '" + name + "' not found" ));
			}
			
			return( details );
			
		}finally{
			this_mon.exit();
		}
	}
	
	public SFPluginDetails[]
	getPluginDetails()
	
		throws SFPluginDetailsException	
	{
		String[]	plugin_ids = getPluginIDs();
		
		SFPluginDetails[]	res = new SFPluginDetails[plugin_ids.length];
	
		for (int i=0;i<plugin_ids.length;i++){
			
			res[i] = getPluginDetails(plugin_ids[i]);
		}
		
		return( res );
	}
	
	public void
	reportPercentComplete(
		ResourceDownloader	downloader,
		int					percentage )
	{
	}
	
	public void
	reportAmountComplete(
		ResourceDownloader	downloader,
		long				amount )
	{
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		informListeners( activity );
	}
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		informListeners( "Error: " + e.getMessage());
	}

	protected void
	informListeners(
		String		log )
	{
		for (int i=0;i<listeners.size();i++){
			
			((SFPluginDetailsLoaderListener)listeners.get(i)).log( log );
		}
	}
	
	public void
	reset()
	{
		try{
			this_mon.enter();
		
			long	now = SystemTime.getCurrentTime();
			
				// handle backward time changes
			
			if ( now < plugin_ids_loaded_at ){
				
				plugin_ids_loaded_at	= 0;
			}
			
			if ( now - plugin_ids_loaded_at > RELOAD_MIN_TIME ){
				
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"SFPluginDetailsLoader: resetting values"));
				
				plugin_ids_loaded	= false;
			
				plugin_ids		= new ArrayList();
				plugin_map			= new HashMap();
				
			}else{
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
							"SFPluginDetailsLoader: not resetting, " + "cache still valid"));
			}
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SFPluginDetailsLoaderListener		l )
	{
		listeners.remove(l);
	}
}
