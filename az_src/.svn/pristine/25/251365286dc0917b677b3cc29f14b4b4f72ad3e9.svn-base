/*
 * File    : DownloadManagerStats.java
 * Created : 22-Oct-2003
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

package org.gudy.azureus2.core3.download;

/**
 * @author parg
 *
 */
public interface 
DownloadManagerStats 
{	
  /** Find out percentage done of current state
   * Use getDownloadCompleted() if you wish to find out a torrents download completion level
   *
   * @return 0 to 1000, 0% to 100% respectively
   *         When state is Allocating, Checking, or Initializing, this
   *         returns the % done of that task
   *         Any other state MAY return getDownloadCompleted()
   */
	public int
	getCompleted();

  /** Retrieve the level of download completion.
   * 
   * To understand the bLive parameter, you must know a bit about the
   * Torrent activation process:
   * 1) Torrent goes into ST_WAITING
   * 2) Torrent moves to ST_PREPARING
   * 3) Torrent moves to ST_DOWNLOADING or ST_SEEDING
   *
   * While in ST_PREPARING, Completion Level is rebuilt (either via Fast Resume
   * or via piece checking). Quite often, the download completion level before
   * ST_PREPARING and after ST_PREPARING are identical.
   *
   * Before going into ST_PREPARING, we store the download completion level.
   * If you wish to retrieve this value instead of the live "building" one,
   * pass false for the parameter.
   *
   * @param bLive true - Always returns the known completion level of the torrent
   *               false - In the case of ST_PREPARING, return completion level 
   *                       before of the torrent ST_PREPARING started.  
   *                       Otherwise, same as true.
   * @return 0 - 1000
   */
	public int
	getDownloadCompleted(boolean bLive);
	
	public void 
	setDownloadCompleted(int completed);
				
  
  /**
   * Get the total number of bytes ever downloaded.
   * @return total bytes downloaded
   */
	public long	getTotalDataBytesReceived();
	
	/**
	 * data bytes received minus discards and hashfails
	 * @return
	 */
	
	public long	getTotalGoodDataBytesReceived();
	
	public long getTotalProtocolBytesReceived();
	
  
  /**
   * Get the total number of bytes ever uploaded.
   * @return total bytes uploaded
   */
	public long	getTotalDataBytesSent();
  	
	public long getTotalProtocolBytesSent();
	
	/*
	 * Resets the total bytes sent/received - will stop and start the download if it is running
	 */
	public void resetTotalBytesSentReceived( long sent, long received );

	public long getRemaining();
	
	public long
	getDiscarded();
  	
	public long
	getHashFailBytes();
	
	public long
	getHashFailCount();

	/**
	 * Gives the share ratio of the torrent in 1000ths (i.e. 1000 = share ratio of 1)
	 */
	public int
	getShareRatio();
	
 
	public long getDataReceiveRate();
  
	public long getProtocolReceiveRate();
  
		
	public long getDataSendRate();

	public long getProtocolSendRate();
  
	/**
	 * Swarm speed
	 * @return
	 */
	public long
	getTotalAverage();
	
		/**
		 * Average for a peer in the swarm
		 * @return
		 */
	public long
	getTotalAveragePerPeer();
	
	public String
	getElapsedTime();
	
	// in ms
	public long
	getTimeStarted();

  /* -1 if not seeding */		
	public long
	getTimeStartedSeeding();

	public long
	getETA();
	
	public float
	getAvailability();
		

	public long 
	getSecondsDownloading();

	public long 
	getSecondsOnlySeeding();
	
	public void
	setCompleted(
		int		c );	 
  
  /**
   * Get the max upload rate allowed for this download.
   * @return upload rate in bytes per second, 0 for unlimited, -1 for upload disabled
   */
  public int getUploadRateLimitBytesPerSecond();
  
  /**
   * Set the max upload rate allowed for this download.
   * @param max_rate_bps limit in bytes per second, 0 for unlimited, -1 for upload disabled
   */
  public void setUploadRateLimitBytesPerSecond( int max_rate_bps );
  
  
  /**
   * Get the max download rate allowed for this download.
   * @return download rate in bytes per second, 0 for unlimited, -1 for download disabled
   */
  public int getDownloadRateLimitBytesPerSecond();
  
  /**
   * Set the max download rate allowed for this download.
   * @param max_rate_bps limit in bytes per second, 0 for unlimited, -1 for download disabled
   */
  public void setDownloadRateLimitBytesPerSecond( int max_rate_bps );
  
	public int getTimeSinceLastDataReceivedInSeconds();
	public int getTimeSinceLastDataSentInSeconds();
	
	public long getAvailWentBadTime();
	
	/*
	public long getEstimatedDownloaded();
	public long getEstimatedUploaded();
	*/
	
	public void
	restoreSessionTotals(
		long		_saved_data_bytes_downloaded,
		long		_saved_data_bytes_uploaded,
		long		_saved_discarded,
		long		_saved_hashfails,
		long		_saved_SecondsDownloading,
		long		_saved_SecondsOnlySeeding );
}
