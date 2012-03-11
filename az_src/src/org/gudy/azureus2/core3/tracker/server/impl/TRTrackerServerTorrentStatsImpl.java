/*
 * File    : TRTrackerServerStatsImpl.java
 * Created : 31-Oct-2003
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
 
package org.gudy.azureus2.core3.tracker.server.impl;

/**
 * @author parg
 */

import org.gudy.azureus2.core3.tracker.server.*;

public class 
TRTrackerServerTorrentStatsImpl
	implements TRTrackerServerTorrentStats 
{
	private TRTrackerServerTorrentImpl		torrent;
	private long							announce_count;
	private long							scrape_count;
	private long							completed_count;
	
	private long							uploaded;
	private long							downloaded;
	private long							left;
	
	private long							biased_uploaded;
	private long							biased_downloaded;

	private long							bytes_in;
	private long							bytes_out;
	
	protected
	TRTrackerServerTorrentStatsImpl(
		TRTrackerServerTorrentImpl 	_torrent )
	{
		torrent	= _torrent;
	}
		
	protected void
	addAnnounce(
		long		ul_diff,
		long		dl_diff,
		long		le_diff,
		boolean		biased_peer )
	{
		announce_count++;
		
		uploaded	+= ul_diff<0?0:ul_diff;	// should always be +ve
		downloaded	+= dl_diff<0?0:dl_diff;
		left		+= le_diff;	// can be +ve at start (0->x) then neg after
		
		if ( left < 0 ){
			
			left	= 0;
		}
		
		if ( biased_peer ){
			
			biased_uploaded		+= ul_diff<0?0:ul_diff;	// should always be +ve
			biased_downloaded	+= dl_diff<0?0:dl_diff;
		}
	}
	
	protected void
	removeLeft(
		long	_left )
	{
		left	-= _left;
		
		if ( left < 0 ){
			
			left	= 0;
		}
	}
	
	public long
	getAnnounceCount()
	{
		return( announce_count );
	}
	
	protected void
	addScrape()
	{
		scrape_count++;
	}
	
	public long
	getScrapeCount()
	{
		return( scrape_count );
	}
	
	protected void
	addCompleted()
	{
		completed_count++;
	}

	public long
	getCompletedCount()
	{
		return( completed_count );
	}
	
	public long
	getUploaded()
	{
		return( uploaded );
	}
	
	public long
	getDownloaded()
	{
		return( downloaded );
	}
	
	public long
	getBiasedUploaded()
	{
		return( biased_uploaded );
	}
	
	public long
	getBiasedDownloaded()
	{
		return( biased_downloaded );
	}

	public long
	getAmountLeft()
	{
		return( left );
	}
		
	protected void
	addXferStats(
		int		in,
		int		out )
	{
		bytes_in	+= in;
		bytes_out	+= out;
	}
	
	public long
	getBytesIn()
	{
		return( bytes_in );
	}
	
	public long
	getBytesOut()
	{
		return( bytes_out );
	}
	
	public int
	getSeedCount()
	{
		return( torrent.getSeedCount());
	}
	
	public int
	getLeecherCount()
	{
		return( torrent.getLeecherCount());
	}
	
	public int
	getQueuedCount()
	{
		return( torrent.getQueuedCount());
	}
	
	public int
	getBadNATPeerCount()
	{
		return( torrent.getBadNATPeerCount());
	}
	
	public String
	getString()
	{
		return( "an=" + announce_count + ",sc=" + scrape_count + ",co=" + completed_count +",le=" + getLeecherCount() + ",se=" + getSeedCount() + ",q=" + getQueuedCount() + ",bi=" + bytes_in + ",bo=" + bytes_out );
	}
}
