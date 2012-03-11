/*
 * File    : DownloadManagerFactory.java
 * Created : 19-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.download;

/**
 * @author parg
 *
 */

import java.util.List;

import org.gudy.azureus2.core3.download.impl.*;
import org.gudy.azureus2.core3.global.*;

public class 
DownloadManagerFactory 
{	
		// new downloads
	
	public static DownloadManager
	create(
		GlobalManager 							gm, 
		byte[]									torrent_hash,
		String 									torrentFileName, 
		String 									savePath,
		String									saveFile,
		int      								initialState,
		boolean									persistent,
		boolean									for_seeding,
		List									file_priorities,
		DownloadManagerInitialisationAdapter 	adapter )
	{
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, savePath, saveFile, initialState, persistent, false, for_seeding, false, file_priorities, adapter ));
	}
	
		// recovery method
	
	public static DownloadManager
	create(
		GlobalManager 	gm, 
		byte[]			torrent_hash,
		String 			torrentFileName, 
		String 			torrent_save_dir,
		String			torrent_save_file, 
		int      		initialState,
		boolean			persistent,
		boolean			recovered,
		boolean			has_ever_been_started,
		List			file_priorities )
	{
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, torrent_save_dir, torrent_save_file, initialState, persistent, recovered, false, has_ever_been_started, file_priorities, null ));
	}
}
