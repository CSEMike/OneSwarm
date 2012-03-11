/*
 * File    : TRHostTorrentRequestImpl.java
 * Created : 13-Dec-2003
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

import java.util.*;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRHostTorrentRequestImpl
	implements TRHostTorrentRequest
{
	protected TRHostTorrent				torrent;
	protected TRHostPeer				peer;
	protected TRTrackerServerRequest	request;
	
	protected
	TRHostTorrentRequestImpl(
		TRHostTorrent			_torrent,
		TRHostPeer				_peer,
		TRTrackerServerRequest	_request )
	{
		torrent		= _torrent;
		peer		= _peer;
		request		= _request;
	}
	
	public TRHostPeer
	getPeer()
	{
		return( peer );
	}

	public TRHostTorrent
	getTorrent()
	{
		return( torrent );
	}
	
	public int
	getRequestType()
	{
		if ( request.getType() == TRTrackerServerRequest.RT_ANNOUNCE ){
			
			return( RT_ANNOUNCE );
			
		}else if ( request.getType() == TRTrackerServerRequest.RT_SCRAPE ){
			
			return( RT_SCRAPE );
			
		}else{
			
			return( RT_FULL_SCRAPE );
		}
	}

	public String
	getRequest()
	{
		return( request.getRequest());
	}
	
	public Map
	getResponse()
	{
		return( request.getResponse());
	}
}
