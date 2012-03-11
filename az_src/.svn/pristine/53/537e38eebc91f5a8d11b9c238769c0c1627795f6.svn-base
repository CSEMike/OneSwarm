/*
 * File    : DownloadStatsImpl.java
 * Created : 08-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.local.download;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.download.DownloadStats;

public class 
DownloadStatsImpl 
	implements DownloadStats
{
	protected DownloadManager		dm;
	protected DownloadManagerStats	dm_stats;
	
	protected
	DownloadStatsImpl(
		DownloadManager	_dm )
	{
		dm 			= _dm;
		dm_stats	= dm.getStats();
	}
	
	public String
	getStatus()
	{
		return( DisplayFormatters.formatDownloadStatusDefaultLocale( dm));
	}
	
	public String
	getStatus(boolean localised)
	{
		return (localised)? DisplayFormatters.formatDownloadStatus( dm ) : getStatus();
			
	}
	
	public String
	getDownloadDirectory()
	{
		return( dm.getSaveLocation().getParent());
	}
	
	public String
	getTargetFileOrDir()
	{		
		return( dm.getSaveLocation().toString());
	}
	
	public String
	getTrackerStatus()
	{
		return(dm.getTrackerStatus());
	}
	
	public int
	getCompleted()
	{
		return( dm_stats.getCompleted());
	}

	public int
	getDownloadCompleted(boolean bLive)
	{
		return( dm_stats.getDownloadCompleted(bLive) );
	}
	
	
	public int
	getCheckingDoneInThousandNotation()
	{
		org.gudy.azureus2.core3.disk.DiskManager	disk = dm.getDiskManager();
 		
 		if ( disk != null ){
 			
 			return( disk.getCompleteRecheckStatus());
 		}
 		
 		return( -1 );
	}

	public void
	resetUploadedDownloaded(
		long 	new_up, 
		long 	new_down )
	{
		dm_stats.resetTotalBytesSentReceived( new_up, new_down );
	}
	
	public long
	getDownloaded()
	{
		return( dm_stats.getTotalDataBytesReceived());
	}
	
	public long
	getRemaining()
	{
		return( dm_stats.getRemaining());
	}
	
	public long
	getUploaded()
	{
		return( dm_stats.getTotalDataBytesSent());
	}
	
	public long
	getDiscarded()
	{
		return( dm_stats.getDiscarded());
	}
	
	public long
	getDownloadAverage()
	{
		return( dm_stats.getDataReceiveRate());
	}
	
	public long
	getUploadAverage()
	{
		return( dm_stats.getDataSendRate());
	}
	
	public long
	getTotalAverage()
	{
		return( dm_stats.getTotalAverage());
	}
	
	public String
	getElapsedTime()
	{
		return( dm_stats.getElapsedTime());
	}
	
	public String
	getETA()
	{
		return(DisplayFormatters.formatETA(dm_stats.getETA()));
	}
	
	public long
	getETASecs()
	{
		return( dm_stats.getETA());
	}
	
	public long
	getHashFails()
	{		
		return( dm_stats.getHashFailCount());
	}
	
	public int
	getShareRatio()
	{
		return( dm_stats.getShareRatio());
	}
	
	// in ms
	public long
	getTimeStarted()
	{
		return (dm_stats.getTimeStarted());
	}
	
	public float
	getAvailability()
	{
		return( dm_stats.getAvailability());
	}

	public long getSecondsOnlySeeding() {
		return dm_stats.getSecondsOnlySeeding();
	}

	public long getSecondsDownloading() {
	  return dm_stats.getSecondsDownloading();
	}

	public long getTimeStartedSeeding() {
	  return dm_stats.getTimeStartedSeeding();
	}
	
	public long
	getSecondsSinceLastDownload()
	{
		return(dm_stats.getTimeSinceLastDataReceivedInSeconds());
	}
	
	public long
	getSecondsSinceLastUpload()
	{
		return(dm_stats.getTimeSinceLastDataSentInSeconds());
	}
	
	public int
	getHealth()
	{
		switch( dm.getHealthStatus()){
		
			case DownloadManager.WEALTH_STOPPED:
			{
				return( DownloadStats.HEALTH_STOPPED );
			
			}
			case DownloadManager.WEALTH_NO_TRACKER:
			{
				return( DownloadStats.HEALTH_NO_TRACKER );
			
			}
			case DownloadManager.WEALTH_NO_REMOTE:
			{
				return( DownloadStats.HEALTH_NO_REMOTE );
			
			}
			case DownloadManager.WEALTH_OK:
			{
				return( DownloadStats.HEALTH_OK );
			
			}
			case DownloadManager.WEALTH_KO:
			{
				return( DownloadStats.HEALTH_KO );
			
			}
			case DownloadManager.WEALTH_ERROR:
			{
				return( DownloadStats.HEALTH_ERROR );
			}
			default:
			{
				Debug.out( "Invalid health status" );
				
				return( dm.getHealthStatus());
			
			}
		}
	}
}
