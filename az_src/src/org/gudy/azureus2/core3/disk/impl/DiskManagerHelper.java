/*
 * Created on 31-Jul-2004
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

package org.gudy.azureus2.core3.disk.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;

import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;

public interface 
DiskManagerHelper 
	extends DiskManager
{
	public DiskAccessController
	getDiskAccessController();
	
	public DMPieceList
	getPieceList(
		int	piece_number );
		
	public byte[]
	getPieceHash(
		int	piece_number )
	
		throws TOTorrentException;
		
	/**
	 * Stops the disk manager and informs the download manager that things have gone
	 * wrong. 
	 * @param reason
	 */

	public void
	setFailed(
		String		reason );
	
	public void
	setFailed(
		DiskManagerFileInfo	file,
		String				reason );
	
	public long
	getAllocated();
	
	public void
	setAllocated(
		long		num );
	
	public void
	setPercentDone(
		int			num );
		
	public void
	setPieceDone(
		DiskManagerPieceImpl	piece,
		boolean					done );
	
	public TOTorrent
	getTorrent();
	
	public String[]
	getStorageTypes();
	
	public String getStorageType(int fileIndex);
	
	public void
	accessModeChanged(
		DiskManagerFileInfoImpl		file,
		int							old_mode,
		int							new_mode );
	
	public void
    skippedFileSetChanged(
	    	DiskManagerFileInfo	file );
	
	public void 
	priorityChanged(
		DiskManagerFileInfo	file );
	
	public String
	getInternalName();
	
	public DownloadManagerState
	getDownloadState();
	
	public DiskManagerRecheckScheduler
	getRecheckScheduler();

}
