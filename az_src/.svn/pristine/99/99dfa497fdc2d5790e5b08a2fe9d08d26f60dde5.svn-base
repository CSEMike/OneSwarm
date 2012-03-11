/*
 * Created on 14-Jan-2005
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

package org.gudy.azureus2.core3.util;

import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.*;

/**
 * @author parg
 *
 */

public class 
AENetworkClassifier 
{
		// DON'T change these constants as they get serialised!!!!
		// (obviously you can add new networks to them).
		// If you add to them remember to update the configuration item default for
		// "Network Selection Default.<name>" and
		// "Tracker Network Selection Default.<name>
	
	public static final String	AT_PUBLIC		= "Public";
	public static final String	AT_I2P			= "I2P";
	public static final String	AT_TOR			= "Tor";
	
	public static final String[]	AT_NETWORKS =
		{ AT_PUBLIC, AT_I2P, AT_TOR };
	
	private static List	listeners = new ArrayList();
	
	public static String
	categoriseAddress(
		String	str )
	{
		int	last_dot = str.lastIndexOf('.');
		
		if ( last_dot == -1 ){
			
			return( AT_PUBLIC );	// no idea really, treat as normal
		}
		
		String	dom = str.substring(last_dot+1).toLowerCase();
		
		if ( dom.equals( "i2p" )){
			
			return( AT_I2P );
			
		}else if ( dom.equals( "onion" )){
			
			return( AT_TOR );
		}
		
		return( AT_PUBLIC );
	}
	
	public static String[]
	getNetworks(
		TOTorrent	torrent,
		String		display_name )
	{
			// go through all the announce URL and find all networks
		
		List	urls = new ArrayList();
		
		urls.add( torrent.getAnnounceURL());
		
		TOTorrentAnnounceURLSet[] sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
		
		for (int i=0;i<sets.length;i++){
			
			URL[]	u = sets[i].getAnnounceURLs();
			
			for (int j=0;j<u.length;j++){
				
				urls.add( u[j] );
			}
		}
		
		List	available_networks = new ArrayList();
		
		for (int i=0;i<urls.size();i++){
			
			URL	u = (URL)urls.get(i);
					
			String	network = categoriseAddress( u.getHost());
			
			if ( !available_networks.contains( network )){
				
				available_networks.add( network );
			}
		}
		
		if ( available_networks.size() == 1 && available_networks.get(0) == AT_PUBLIC ){
			
			return( new String[]{ AT_PUBLIC });
		}
		
		
		boolean	prompt = COConfigurationManager.getBooleanParameter( "Network Selection Prompt" );
		
		List	res = new ArrayList();

		if ( prompt && listeners.size() > 0 ){

			String[]	t_nets = new String[available_networks.size()];
			
			available_networks.toArray( t_nets );

			for (int i=0;i<listeners.size();i++){
				
				try{
					String[]	selected = ((AENetworkClassifierListener)listeners.get(i)).selectNetworks(
											display_name,
											t_nets );
					
					if ( selected != null ){
						
						for (int j=0;j<selected.length;j++){
							
							res.add( selected[j] );
						}
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
		}else{
				// use enabled defaults to proceed
			
			
			for (int i=0;i<available_networks.size();i++){
				
				if ( COConfigurationManager.getBooleanParameter( "Network Selection Default." + available_networks.get(i))){
			
					res.add( available_networks.get(i));
				}
			}
		}
		
		String[]	x = new String[res.size()];
		
		res.toArray( x );
		
		return( x );
	}
	
	public static void
	addListener(
		AENetworkClassifierListener	l )
	{
		listeners.add(l);
	}
	
	public static void
	removeListener(
		AENetworkClassifierListener	l )
	{
		listeners.remove(l);
	}
}
