/*
 * File    : IPBanned.java
 * Created : 08-Jan-2007
 * By      : jstockall
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 *  
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.pluginsimpl.local.ipfilter;

/**
 * @author jstockall
 *
 */

import org.gudy.azureus2.core3.ipfilter.BannedIp;
import org.gudy.azureus2.plugins.ipfilter.IPBanned;

public class 
IPBannedImpl 
	implements IPBanned
{
	protected BannedIp	banned;
	
	protected
	IPBannedImpl(
		BannedIp	_blocked )
	{
		banned	= _blocked;
	}
	 
	public String 
	getBannedIP()
	{
		return( banned.getIp());
	}
	 
	public String
	getBannedTorrentName()
	{
		return(banned.getTorrentName());
	}
	
	public long 
	getBannedTime()
	{
		return( banned.getBanningTime());
	}
}
