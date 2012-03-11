/*
 * Created on Oct 9, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.rssgen;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageGenerator;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.HyperlinkParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.webplugin.WebPlugin;


public class 
RSSGeneratorPlugin
	extends WebPlugin
{
	public static final String	PLUGIN_NAME		= "Local RSS etc.";
	public static final int 	DEFAULT_PORT    = 6905;
	public static final String	DEFAULT_ACCESS	= "all";
	
	private static volatile RSSGeneratorPlugin		singleton;
	
	private static boolean	loaded;
	
	private static Properties defaults = new Properties();
	    
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

		synchronized( RSSGeneratorPlugin.class ){
			
			if ( loaded ){
				
				return;
			}
			
			loaded = true;
		}
				
		File	root_dir = new File( SystemProperties.getUserPath() + "rss" );
		
		if ( !root_dir.exists()){
			
			root_dir.mkdir();
		}
		
		Integer	rss_port;
		String	rss_access;
		
		if ( COConfigurationManager.getBooleanParameter( "rss.internal.migrated", false )){
			
			rss_port 	= COConfigurationManager.getIntParameter( "rss.internal.config.port", DEFAULT_PORT );
			rss_access 	= COConfigurationManager.getStringParameter( "rss.internal.config.access", DEFAULT_ACCESS );
			
		}else{
			
				// migrate from when the RSS feed was tied to devices
			
			int		port 	= COConfigurationManager.getIntParameter( "Plugin.default.device.rss.port", DEFAULT_PORT );
			
			rss_port 	= port;

			if ( port != DEFAULT_PORT ){
				
				COConfigurationManager.setParameter( "rss.internal.config.port", port );
			}
			
			boolean	local 	= COConfigurationManager.getBooleanParameter( "Plugin.default.device.rss.localonly", true );
			
			rss_access	= local?"local":"all";
			
			if ( !rss_access.equals( DEFAULT_ACCESS )){
				
				COConfigurationManager.setParameter( "rss.internal.config.access", rss_access );
			}
			
			COConfigurationManager.setParameter( "rss.internal.migrated", true );	
		}
		
		defaults.put( WebPlugin.PR_ENABLE, new Boolean( COConfigurationManager.getBooleanParameter( "Plugin.default.device.rss.enable", false )));
		defaults.put( WebPlugin.PR_DISABLABLE, new Boolean( true ));
	    defaults.put( WebPlugin.PR_PORT, rss_port );
	    defaults.put( WebPlugin.PR_ACCESS, rss_access );
	    defaults.put( WebPlugin.PR_ROOT_DIR, root_dir.getAbsolutePath());
	    defaults.put( WebPlugin.PR_ENABLE_KEEP_ALIVE, new Boolean(true));
	    defaults.put( WebPlugin.PR_HIDE_RESOURCE_CONFIG, new Boolean(true));
	    defaults.put( WebPlugin.PR_PAIRING_SID, "rss" );
	    
	    defaults.put( WebPlugin.PR_CONFIG_MODEL_PARAMS, new String[]{ ConfigSection.SECTION_ROOT, "rss" });
	}
	
	public static RSSGeneratorPlugin
	getSingleton()
	{
		return( singleton );
	}
	
	
	private static Map<String,Provider>	providers = new TreeMap<String, Provider>();
	
	private HyperlinkParameter		test_param;
	
	public
	RSSGeneratorPlugin()
	{
		super( defaults );		
	}
	
	public String
	getURL()
	{
		return( getProtocol() + "://127.0.0.1:" + getPort() + "/" );
	}
	
	@Override
	protected void
	setupServer()
	{
		super.setupServer();
		
		if ( test_param != null ){
		
			test_param.setEnabled( isPluginEnabled());
		
			test_param.setHyperlink( getURL());
		}
	}
	
	public static void
	registerProvider(
		String				name,
		Provider			provider )
	{
		synchronized( providers ){
		
			providers.put( name, provider );
		}
	}
	
	public static void
	unregisterProvider(
		String				name )
	{
		synchronized( providers ){
		
			providers.remove( name );
		}
	}
	
	public void
	initialize(
		PluginInterface		pi )
	
		throws PluginException
	{
		singleton = this;

		pi.getPluginProperties().setProperty( "plugin.name", PLUGIN_NAME );
		
		super.initialize( pi );
	}
	
	@Override
	protected void
	initStage(
		int	num )
	{
		if ( num == 1 ){
			
			BasicPluginConfigModel  config = getConfigModel();
			
			test_param = config.addHyperlinkParameter2( "rss.internal.test.url", "" );
			
			test_param.setEnabled( isPluginEnabled());
		}
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String url = request.getURL();
		
		if ( url.startsWith( "/" )){
			
			url = url.substring( 1 );
		}
	
		if ( url.length() == 0 || url.charAt(0) == '?' ){
			
			response.setContentType( "text/html; charset=UTF-8" );
			
			PrintWriter pw = new PrintWriter(new OutputStreamWriter( response.getOutputStream(), "UTF-8" ));

			pw.println( "<HTML><HEAD><TITLE>Vuze Feeds etc.</TITLE></HEAD><BODY>" );
			
			synchronized( providers ){
				
				for ( Map.Entry<String,Provider> entry: providers.entrySet()){
			
					Provider provider = entry.getValue();
					
					if ( !provider.isEnabled()){
						
						continue;
					}

					String	name = entry.getKey();
								
					pw.println( "<LI><A href=\"" + URLEncoder.encode( name, "UTF-8" ) + "\">" + name + "</A></LI>" );
				}
			}
			
			pw.println( "</BODY></HTML>" );
			
			pw.flush();
			
			return( true );
			
		}else{
			
			int	pos = url.indexOf( '/' );
			
			if ( pos != -1 ){
				
				url = url.substring( 0, pos );
			}
			
			Provider provider;
			
			synchronized( providers ){
				
				provider = providers.get( url );
			}
			
			if ( provider != null && provider.isEnabled()){
				
				if ( provider.generate(request, response)){
					
					return( true );
				}
			}
		}
		
		response.setReplyStatus( 404 );
		
		return( true );
	}
	
	public interface
	Provider
		extends TrackerWebPageGenerator
	{
		public boolean
		isEnabled();
	}
}
