/*
 * File    : TorrentDownloader2.java
 * Created : 27-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.plugins.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.InputStream;

public interface 
ResourceDownloader 
{
	public static final String	PR_STRING_CONTENT_TYPE		= "ContentType";
	
		/**
		 * Get a sensible name for the download based on its details (e.g. URL)
		 * @return
		 */
	
	public String
	getName();
	
		/**
		 * Synchronously download. Events are still reported to listeners
		 * @return
		 * @throws ResourceDownloaderException
		 */
	
	public InputStream
	download()
	
		throws ResourceDownloaderException;
	
		/**
		 * Asynchronously download.
		 *
		 */
	
	public void
	asyncDownload();
	
		/**
		 * attempts to get the size of the download. Returns -1 if the size can't be determined
		 * @return
		 * @throws ResourceDownloaderException
		 */
	
	public long
	getSize()
	
		throws ResourceDownloaderException;

	public Object
	getProperty(
		String		name )
	
		throws ResourceDownloaderException;
	
		/**
		 * Cancel the download. 
		 */
	
	public void
	cancel();
	
	public boolean
	isCancelled();
	
	public void
	reportActivity(
		String				activity );

	public void
	addListener(
		ResourceDownloaderListener	l );
	
	public void
	removeListener(
		ResourceDownloaderListener	l );
}
