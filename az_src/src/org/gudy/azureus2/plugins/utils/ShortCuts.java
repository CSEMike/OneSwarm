/*
 * Created on 10-May-2004
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

package org.gudy.azureus2.plugins.utils;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.download.*;

public interface 
ShortCuts 
{
		/**
		 * A quick way of looking up a download given its hash
		 * @param hash
		 * @return
		 * @throws DownloadException
		 */
	
	public Download
	getDownload(
		byte[]		hash )
	
		throws DownloadException;
	
		/**
		 * A quick way of getting a download's statistics given its hash
		 * @param hash
		 * @return
		 * @throws DownloadException
		 */
		
	public DownloadStats
	getDownloadStats(
		byte[]		hash )
	
		throws DownloadException;
	
		/**
		 * A quick way of restarting a download given its hash
		 * @param hash
		 * @throws DownloadException
		 */
	
	public void
	restartDownload(
		byte[]		hash )
	
		throws DownloadException;
	
		/**
		 * A quick way of stopping a download given its hash
		 * @param hash
		 * @throws DownloadException
		 */
	
	public void
	stopDownload(
		byte[]		hash )
	
		throws DownloadException;
	
		/**
		 * A quick way of deleting a download given its hash
		 * @param hash
		 * @throws DownloadException
		 * @throws DownloadRemovalVetoException
		 */
	
	public void
	removeDownload(
		byte[]		hash )
	
		throws DownloadException, DownloadRemovalVetoException;
}
