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

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.clientid.ClientIDManagerImpl;
import org.gudy.azureus2.core3.peer.util.PeerUtils;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author parg
 *
 */

public class 
ClientIDPlugin
{
	private static boolean		send_os;
	
	public static void initialize() {
		final String	param = "Tracker Client Send OS and Java Version";
		
		COConfigurationManager.addAndFireParameterListener(param, new ParameterListener() {
			public void parameterChanged(String param) {
				send_os = COConfigurationManager.getBooleanParameter(param);
			}
		});

		ClientIDManagerImpl.getSingleton().setGenerator( 
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
					doHTTPProperties( properties );
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

	protected static void
	doHTTPProperties(
		Properties			properties )
	{
		Boolean	raw = (Boolean)properties.get( ClientIDGenerator.PR_RAW_REQUEST );
		
		if ( raw != null && raw ){
			
			return;
		}
		
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
