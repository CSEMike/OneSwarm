/*
 * File    : IPBlocked.java
 * Created : 05-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.ipfilter;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.ipfilter.*;

import org.gudy.azureus2.core3.ipfilter.*;

public class 
IPBlockedImpl 
	implements IPBlocked
{
	protected IPFilter	filter;
	protected BlockedIp	blocked;
	
	protected
	IPBlockedImpl(
		IPFilter	_filter,
		BlockedIp	_blocked )
	{
		filter	= _filter;
		blocked	= _blocked;
	}
	 
	public String 
	getBlockedIP()
	{
		return( blocked.getBlockedIp());
	}
	 
	public String
	getBlockedTorrentName()
	{
		return(blocked.getTorrentName());
	}
	
	public long 
	getBlockedTime()
	{
		return( blocked.getBlockedTime());
	}
	 
	public IPRange
	getBlockingRange()
	{
	 	return( new IPRangeImpl( filter, blocked.getBlockingRange()));
	}
}
