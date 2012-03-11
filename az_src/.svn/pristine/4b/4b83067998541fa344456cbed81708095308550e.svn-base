/*
 * File    : TrackerPeerImpl.java
 * Created : 08-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.tracker;

/**
 * @author parg
 *
 */
 
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.plugins.tracker.*;

public class 
TrackerPeerImpl
	implements TrackerPeer
{
	protected TRHostPeer		peer;
	
	protected
	TrackerPeerImpl(
		TRHostPeer		_peer )
	{
		peer	= _peer;
	}
	
	public boolean
	isSeed()
	{
		return( peer.isSeed());
	}	
	
	public long
	getAmountLeft()
	{
		return( peer.getAmountLeft());
	}	
	
	public long
	getDownloaded()
	{
		return( peer.getDownloaded());
	}	
	
	public long
	getUploaded()
	{
		return( peer.getUploaded());
	}
	
	public String
	getIP()
	{
		return( peer.getIP());
	}
	
	public String
	getIPRaw()
	{
		return( peer.getIPRaw());
	}
	
	public int
	getPort()
	{
		return( peer.getPort());
	}
	
	public byte[]
	getPeerID()
	{
		return( peer.getPeerID());
	}
}
