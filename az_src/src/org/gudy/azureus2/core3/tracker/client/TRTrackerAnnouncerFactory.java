/*
 * File    : TRTrackerClientFactory.java
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

package org.gudy.azureus2.core3.tracker.client;


import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerAnnouncerFactoryImpl;

public class 
TRTrackerAnnouncerFactory 
{
	public static TRTrackerAnnouncer
	create(
		TOTorrent		torrent )
		
		throws TRTrackerAnnouncerException
	{
		return( create( torrent, null ));
	}
	
	public static TRTrackerAnnouncer
	create(
		TOTorrent		torrent,
		boolean			manual )
		
		throws TRTrackerAnnouncerException
	{
		return( TRTrackerAnnouncerFactoryImpl.create( torrent, null, manual ));
	}
	
	public static TRTrackerAnnouncer
	create(
		TOTorrent		torrent,
		String[]		networks )
		
		throws TRTrackerAnnouncerException
	{
		return( TRTrackerAnnouncerFactoryImpl.create( torrent, networks, false ));
	}
	
	public static void
	addListener(
		TRTrackerAnnouncerFactoryListener	l )
	{
		TRTrackerAnnouncerFactoryImpl.addListener(l);	
	}
		
	public static void
	removeListener(
		TRTrackerAnnouncerFactoryListener	l )
	{
		TRTrackerAnnouncerFactoryImpl.removeListener(l);	
	}
}
