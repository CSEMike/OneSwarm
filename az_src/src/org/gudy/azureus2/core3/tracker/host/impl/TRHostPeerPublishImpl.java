/*
 * File    : TRHostPeerPublishImpl.java
 * Created : 12-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.tracker.host.*;

public class 
TRHostPeerPublishImpl
	implements TRHostPeer
{
	protected boolean	seed;
	
	protected
	TRHostPeerPublishImpl(
		boolean		_seed )
	{
		seed	=_seed;	
	}
	
	public boolean
	isSeed()
	{
		return( seed );
	}
	
	public long
	getUploaded()
	{
		return( 0 );
	}
	
	public long
	getDownloaded()
	{
		return( 0 );
	}
	
	public long
	getAmountLeft()
	{
		return( 0 );
	}
	
	public int
	getNumberOfPeers()
	{
		return( 0 );
	}
	
	public String
	getIP()
	{
		return("");
	}	
	
	public String
	getIPRaw()
	{
		return("");
	}
	
	public int
	getPort()
	{
		return( 0 );
	}
	
	public byte[]
	getPeerID()
	{
		return( null );
	}
}
