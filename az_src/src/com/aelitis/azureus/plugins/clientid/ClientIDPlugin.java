/*
 * Created on 29-Dec-2004
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

package com.aelitis.azureus.plugins.clientid;

import java.util.Properties;

import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.torrent.Torrent;

/**
 * @author parg
 *
 */

public class 
ClientIDPlugin 
	implements Plugin
{
	private PluginInterface		plugin_interface;
	
	private static boolean		send_os;
	
	public static void
	load(
		final PluginInterface	plugin_interface )
	{
		final String	param = "Tracker Client Send OS and Java Version";
		
		send_os = plugin_interface.getPluginconfig().getBooleanParameter( param );

		plugin_interface.getPluginconfig().addListener(
			new PluginConfigListener()
			{
				public void 
				configSaved() 
				{
					send_os = plugin_interface.getPluginconfig().getBooleanParameter( param );				
				}
			});
		
		plugin_interface.getClientIDManager().setGenerator( 
			new ClientIDGenerator()
			{
				public byte[]
				generatePeerID(
					Torrent		torrent,
					boolean		for_tracker )
				{
					return( PeerUtils.createPeerID());
				}
							
				public void
				generateHTTPProperties(
					Properties	properties )
				{
					doHTTPProperties( plugin_interface, properties );
				}
				
				public String[]
				filterHTTP(
					String[]	lines_in )
				{
					return( lines_in );
				}
			},
			false );
	}
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Client ID" );
	}
	

	protected static void
	doHTTPProperties(
		PluginInterface		plugin_interface,
		Properties			properties )
	{
		String	version = Constants.AZUREUS_VERSION;
		
			// trim of any _Bnn or _CVS suffix as unfortunately some trackers can't cope with this
			// (well, apparently can't cope with B10)
			// its not a big deal anyway
		
		int	pos = version.indexOf('_');
		
		if ( pos != -1 ){
			
			version = version.substring(0,pos);
		}
		
		String	agent = Constants.AZUREUS_NAME + " " + version;
				
		if ( send_os ){
							
			agent += ";" + Constants.OSName;
		
			agent += ";Java " + Constants.JAVA_VERSION;
		}
		
		properties.put( ClientIDGenerator.PR_USER_AGENT, agent );
	}
	

}
