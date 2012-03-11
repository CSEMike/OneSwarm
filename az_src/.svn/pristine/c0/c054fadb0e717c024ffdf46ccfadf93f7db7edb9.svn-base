/*
 * Created on 12-Jul-2004
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

package org.gudy.azureus2.core3.global.impl;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.gudy.azureus2.core3.global.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;


class
GlobalManagerHostSupport
	implements 	TRHostTorrentFinder
{
	protected GlobalManager	gm;
	protected TRHost		host;
	
	protected
	GlobalManagerHostSupport(
		GlobalManager	_gm )
	{
		gm		= _gm;
		
	    host = TRHostFactory.getSingleton();
		  
		host.initialise( this );
	}
	
	public TOTorrent
	lookupTorrent(
		byte[]		hash )
	{
		List	managers = gm.getDownloadManagers();
		
		for (int i=0;i<managers.size();i++){
			
			DownloadManager	dm = (DownloadManager)managers.get(i);
			
			TOTorrent t = dm.getTorrent();
			
			if ( t != null ){
				
				try{
					if ( Arrays.equals( hash, t.getHash())){
						
						return( t );
					}
				}catch( TOTorrentException e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
		
		return( null );
	}
	
	protected void
	torrentRemoved(
		String			torrent_file_str,
		TOTorrent		torrent )
	{
		TRHostTorrent	host_torrent = host.getHostTorrent( torrent );
		
		if ( host_torrent != null ){
			
				// it we remove a torrent while it is hosted then we flip it into passive mode to
				// keep it around in a sensible state
			
				// we've got to ensure that the torrent's file location is available in the torrent itself
				// as we're moving from download-managed persistence to host managed :(
			
				// check file already exists - might have already been deleted as in the
				// case of shared resources
			
			File	torrent_file = new File( torrent_file_str );
			
			if ( torrent_file.exists()){
				
				try{
					TorrentUtils.writeToFile( host_torrent.getTorrent(), torrent_file, false );
				
					host_torrent.setPassive( true );
					
				}catch( Throwable e ){
					
					Debug.out( "Failed to make torrent '" + torrent_file_str + "' passive: " + Debug.getNestedExceptionMessage(e));
				}
			}
		}
	}
	
	protected void
	destroy()
	{			
		host.close();
	}
}