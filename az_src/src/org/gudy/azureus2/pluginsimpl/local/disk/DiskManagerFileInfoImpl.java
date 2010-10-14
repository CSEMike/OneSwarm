/*
 * Created : 2004/May/26
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.pluginsimpl.local.disk;

import java.io.File;

import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;


/**
 * @author TuxPaper
 *
 */

public class DiskManagerFileInfoImpl
       implements DiskManagerFileInfo 
{
	protected DownloadImpl										download;
	protected org.gudy.azureus2.core3.disk.DiskManagerFileInfo 	core;
	
	public
	DiskManagerFileInfoImpl(
		DownloadImpl										_download,
		org.gudy.azureus2.core3.disk.DiskManagerFileInfo 	coreFileInfo )
	{
	  core 		= coreFileInfo;
	  download	= _download;
	}

	public void setPriority(boolean b) {
	  core.setPriority(b);
	}
	
	public void setSkipped(boolean b) {
	  core.setSkipped(b);
	}
	 
	public void
	setDeleted(boolean b)
	{
		if ( b ){
						
			core.setStorageType( org.gudy.azureus2.core3.disk.DiskManagerFileInfo.ST_COMPACT );
			
		}else{
			
			core.setStorageType( org.gudy.azureus2.core3.disk.DiskManagerFileInfo.ST_LINEAR );
		}
	}
	
	public boolean
	isDeleted()
	{
		return( core.getStorageType() ==  org.gudy.azureus2.core3.disk.DiskManagerFileInfo.ST_COMPACT );
	}
	
	public void
	setLink(
		File	link_destination )
	{
		core.setLink( link_destination );
	}
		
	public File
	getLink()
	{
		return( core.getLink());
	}
	 	// get methods
	 	
	public int getAccessMode() {
	  return core.getAccessMode();
	}
	
	public long getDownloaded() {
	  return core.getDownloaded();
	}
	
	public long getLength() {
		  return core.getLength();
		}
	public File getFile() {
	  return core.getFile(false);
	}
	
	public int getFirstPieceNumber() {
	  return core.getFirstPieceNumber();
	}
	
	public int getNumPieces() {
	  return core.getNbPieces();
	}
		
	public boolean isPriority() {
	  return core.isPriority();
	}
	
	public boolean isSkipped() {
	  return core.isSkipped();
	}
	
	public int
	getIndex()
	{
		return( core.getIndex());
	}
	
	public Download getDownload()
         throws DownloadException
    {
		return DownloadManagerImpl.getDownloadStatic( core.getDownloadManager());
    }
	
	public DiskManagerChannel
	createChannel()
	 	throws DownloadException
	{
		if ( core.getDownloadManager().getTorrent() == null ){
			
			throw( new DownloadException( "Torrent invalid" ));
		}
		
		return( new DiskManagerChannelImpl( download, this ));
	}
	
	protected org.gudy.azureus2.core3.disk.DiskManagerFileInfo
	getCore()
	{
		return( core );
	}
}
