/*
 * File    : PEPeerManagerFactory.java
 * Created : 15-Oct-2003
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

package org.gudy.azureus2.core3.peer;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.peer.impl.*;

public class 
PEPeerManagerFactory 
{
	public static PEPeerManager
	create(
		byte[]					peer_id,
		PEPeerManagerAdapter	adapter,
	  	DiskManager 			diskManager )
	{
  		return( create( peer_id, adapter, diskManager, 0 ));
	}
	
	public static PEPeerManager
	create(
		byte[]					peer_id,
		PEPeerManagerAdapter	adapter,
	  	DiskManager 			diskManager,
	  	int						id )
	{
  		return( PEPeerControlFactory.create( peer_id, adapter, diskManager, id ));
	}
}
