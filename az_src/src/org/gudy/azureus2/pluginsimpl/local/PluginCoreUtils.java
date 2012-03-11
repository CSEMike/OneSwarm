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

import java.io.File;
import java.io.IOException;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.tracker.host.TRHostTorrent;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerTorrent;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.TrackerTorrent;

import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerImpl;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;
import org.gudy.azureus2.pluginsimpl.local.tracker.TrackerTorrentImpl;

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
	
	/**
	 * May return NULL if download not found (e.g. has been removed)
	 * @param dm
	 * @return may be null
	 */
	
	public static Download
	wrap(
		org.gudy.azureus2.core3.download.DownloadManager	dm )
	{
		try{
			return( DownloadManagerImpl.getDownloadStatic( dm ));
			
		}catch( Throwable e ){
			
			// Debug.printStackTrace( e );
			
			return( null );
		}
	}
	
	public static org.gudy.azureus2.plugins.disk.DiskManagerFileInfo
	wrap(
		DiskManagerFileInfo		info )
	
		throws DownloadException
	{
		if ( info == null ){
			
			return( null );
		}
		
		return( new DiskManagerFileInfoImpl( DownloadManagerImpl.getDownloadStatic( info.getDownloadManager()), info ));
	}
	
	public static DiskManagerFileInfo
	unwrap(
		final org.gudy.azureus2.plugins.disk.DiskManagerFileInfo		info )
	
		throws DownloadException
	{
		if ( info == null ){
			
			return( null );
		}

		try{
			Download dl = info.getDownload();
			
			if ( dl != null ){
				
				org.gudy.azureus2.core3.download.DownloadManager dm = unwrap( dl );
				
				return( dm.getDiskManagerFileInfo()[ info.getIndex()]);
			}
		}catch( Throwable e ){
		}
		
			// no underlying download, lash something up
		
		return(
			new DiskManagerFileInfo()
			{
				public void 
				setPriority(
					int b )
				{
					info.setPriority(b>0);
				}
				
				public void 
				setSkipped(
					boolean b)
				{
					info.setSkipped(b);
				}

				public boolean
				setLink(
					File	link_destination )
				{	
					info.setLink(link_destination);
					
					return( true );
				}
				
				public boolean 
				setLinkAtomic(
					File link_destination )
				{	
					info.setLink(link_destination);
					
					return( true );
				}
				
				public File
				getLink()
				{
					return( info.getLink());
				}
				
				public boolean 
				setStorageType(
					int type )
				{
					return( false );
				}
				
				public int
				getStorageType()
				{
					return( ST_LINEAR );
				}
				
				public int 
				getAccessMode()
				{
					return( info.getAccessMode());
				}
				
				public long 
				getDownloaded()
				{
					return( info.getDownloaded());
				}
				
				public String 
				getExtension()
				{
					return( "" );
				}
					
				public int 
				getFirstPieceNumber()
				{
					return( info.getFirstPieceNumber());
				}
			  
				public int 
				getLastPieceNumber()
				{
					return((int)(( info.getLength() + info.getPieceSize()-1 )/info.getPieceSize()));
				}
				
				public long 
				getLength()
				{
					return( info.getLength());
				}
					
				public int 
				getNbPieces()
				{
					return( info.getNumPieces());
				}
						
				public int 
				getPriority()
				{
					return( info.isPriority()?1:0);
				}
				
				public boolean 
				isSkipped()
				{
					return( info.isSkipped());
				}
				
				public int	
				getIndex()
				{
					return( info.getIndex());
				}
				
				public DownloadManager	
				getDownloadManager()
				{
					return( null );
				}
				
				public org.gudy.azureus2.core3.disk.DiskManager 
				getDiskManager()
				{
					return( null );
				}
				
				public File 
				getFile( boolean follow_link )
				{
					if ( follow_link ){
						
						return( info.getLink());
						
					}else{
						
						return( info.getFile());
					}
				}
				
				public TOTorrentFile
				getTorrentFile()
				{
					return( null );
				}
				
				public DirectByteBuffer
				read(
					long	offset,
					int		length )
				
					throws IOException
				{
					throw( new IOException( "unsupported" ));
				}
				
				public void
				flushCache()
				
					throws	Exception
				{	
				}
				
				public void
				close()
				{
				}
				
				public void
				addListener(
					DiskManagerFileInfoListener	listener )
				{
				}
				
				public void
				removeListener(
					DiskManagerFileInfoListener	listener )
				{
				}
			});
	}
	
	public static Object
	convert(
		Object datasource,
		boolean toCore)
	{
		try {
			if (toCore) {
				if (datasource instanceof org.gudy.azureus2.core3.download.DownloadManager) {
					return datasource;
				}
				if (datasource instanceof DownloadImpl) {
					return ((DownloadImpl) datasource).getDownload();
				}

				if (datasource instanceof org.gudy.azureus2.core3.disk.DiskManager) {
					return datasource;
				}
				if (datasource instanceof DiskManagerImpl) {
					return ((DiskManagerImpl) datasource).getDiskmanager();
				}

				if (datasource instanceof PEPeerManager) {
					return datasource;
				}
				if (datasource instanceof PeerManagerImpl) {
					return ((PeerManagerImpl) datasource).getDelegate();
				}
				
				if (datasource instanceof PEPeer) {
					return datasource;
				}
				if (datasource instanceof PeerImpl) {
					return ((PeerImpl)datasource).getPEPeer();
				}

				if (datasource instanceof org.gudy.azureus2.core3.disk.DiskManagerFileInfo) {
					return datasource;
				}
				if (datasource instanceof org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl) {
					return ((org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl) datasource).getCore();
				}

				if (datasource instanceof TRHostTorrent) {
					return datasource;
				}
				if (datasource instanceof TrackerTorrentImpl) {
					((TrackerTorrentImpl) datasource).getHostTorrent();
				}
			} else { // to PI
				if (datasource instanceof org.gudy.azureus2.core3.download.DownloadManager) {
					return wrap((org.gudy.azureus2.core3.download.DownloadManager) datasource);
				}
				if (datasource instanceof DownloadImpl) {
					return datasource;
				}

				if (datasource instanceof org.gudy.azureus2.core3.disk.DiskManager) {
					return wrap((org.gudy.azureus2.core3.disk.DiskManager) datasource);
				}
				if (datasource instanceof DiskManagerImpl) {
					return datasource;
				}

				if (datasource instanceof PEPeerManager) {
					return wrap((PEPeerManager) datasource);
				}
				if (datasource instanceof PeerManagerImpl) {
					return datasource;
				}

				if (datasource instanceof PEPeer) {
					return PeerManagerImpl.getPeerForPEPeer((PEPeer) datasource);
				}
				if (datasource instanceof Peer) {
					return datasource;
				}
				
				if (datasource instanceof org.gudy.azureus2.core3.disk.DiskManagerFileInfo) {
					DiskManagerFileInfo fileInfo = (org.gudy.azureus2.core3.disk.DiskManagerFileInfo) datasource;
					if (fileInfo != null) {
						try {
							return new org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl(
									DownloadManagerImpl.getDownloadStatic(fileInfo.getDownloadManager()),
									fileInfo);
						} catch (DownloadException e) { /* Ignore */
						}
					}
				}
				if (datasource instanceof org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerFileInfoImpl) {
					return datasource;
				}

				if (datasource instanceof TRHostTorrent) {
					TRHostTorrent item = (TRHostTorrent) datasource;
					return new TrackerTorrentImpl(item);
				}
				if (datasource instanceof TrackerTorrentImpl) {
					return datasource;
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
		}

		return datasource;
	}
	
	public static org.gudy.azureus2.core3.download.DownloadManager
	unwrap(
		Download		dm )
	{
		if ( dm instanceof DownloadImpl ){
			
			return(((DownloadImpl)dm).getDownload());
			
		}else{
			
			Debug.out( "Can't unwrap " + dm );
			
			return( null );
		}
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
	
	public static TRTrackerServerTorrent
	unwrap(
		TrackerTorrent		torrent )
	{
		return( ((TrackerTorrentImpl)torrent).getHostTorrent().getTrackerTorrent());
	}
	
	public static PEPeer
	unwrap(
		Peer		peer )
	{
		return(((PeerImpl)peer).getDelegate());
	}
	
	public static boolean
	isInitialisationComplete()
	{
		return( PluginInitializer.getDefaultInterface().getPluginState().isInitialisationComplete());
	}
}
