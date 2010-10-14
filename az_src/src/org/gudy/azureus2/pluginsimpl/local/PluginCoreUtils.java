/*
 * Created on Apr 17, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.pluginsimpl.local;

import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

public class 
PluginCoreUtils 
{
	public static Torrent
	wrap(
		TOTorrent	t )
	{
		return( new TorrentImpl( t ));
	}
	
	public static TOTorrent
	unwrap(
		Torrent		t )
	{
		return(((TorrentImpl)t).getTorrent());
	}
	
	public static DiskManager
	wrap(
		org.gudy.azureus2.core3.disk.DiskManager	dm )
	{
		return( new DiskManagerImpl( dm ));
	}
	
	public static org.gudy.azureus2.core3.disk.DiskManager
	unwrap(
		DiskManager		dm )
	{
		return(((DiskManagerImpl)dm).getDiskmanager());
	}
	
	public static Download
	wrap(
		org.gudy.azureus2.core3.download.DownloadManager	dm )
	{
		try{
			return( DownloadManagerImpl.getDownloadStatic( dm ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
	}
	
	public static org.gudy.azureus2.core3.download.DownloadManager
	unwrap(
		Download		dm )
	{
		return(((DownloadImpl)dm).getDownload());
	}
	
	public static PeerManager
	wrap(
		PEPeerManager	pm )
	{
		return( PeerManagerImpl.getPeerManager( pm ));
	}
	
	public static PEPeerManager
	unwrap(
		PeerManager		pm )
	{
		return(((PeerManagerImpl)pm).getDelegate());
	}
	
}
