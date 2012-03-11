/*
 * File    : TOTorrentAnnounceURLGroup.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent;

import java.net.*;

public interface 
TOTorrentAnnounceURLGroup 
{
	 /**
	  * Gets the current sets defined for this group, 0 length if none defined
	  * @return
	  */
	 
	public TOTorrentAnnounceURLSet[]
	getAnnounceURLSets();
	
	 /**
	  * Sets the group's URL sets to the supplied values.
	  * @param sets
	  */
	public void
	setAnnounceURLSets(
		TOTorrentAnnounceURLSet[]	sets );
		
		/**
		 * This method will create a new set. It is not added into the current set, this 
		 * must be done by the caller inserting the newly created set into an array as
		 * required and calling the above "set" method.
		 *  
		 * @param urls the URLs for the new set
		 * @return	the newly created set
		 */
		
	public TOTorrentAnnounceURLSet
	createAnnounceURLSet(
		URL[]	urls );
}
