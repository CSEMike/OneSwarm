/*
 * File    : DownloadManagerStatsImpl.java
 * Created : 24-Oct-2003
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
 
package org.gudy.azureus2.core3.download.impl;

/**
 * @author parg
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.disk.*;

public class 
DownloadManagerStatsImpl 
	implements DownloadManagerStats
{
	private DownloadManagerImpl	download_manager;
		
		//Completed (used for auto-starting purposes)
		
	private int completed;
	private int downloadCompleted;
	
		// saved downloaded and uploaded
	private long saved_data_bytes_downloaded;
	private long saved_protocol_bytes_downloaded;
  
	private long saved_data_bytes_uploaded;
	private long saved_protocol_bytes_uploaded;
  
	private long saved_discarded = 0;
	private long saved_hashfails = 0;
	
	private long saved_SecondsDownloading = 0;
	private long saved_SecondsOnlySeeding = 0;
	
	private int saved_SecondsSinceDownload	= 0;
	private int saved_SecondsSinceUpload	= 0;
	
	private int max_upload_rate_bps = 0;  //0 for unlimited
	private int max_download_rate_bps = 0;  //0 for unlimited
  
  

	protected
	DownloadManagerStatsImpl(
		DownloadManagerImpl	dm )
	{
		download_manager = dm;
	}

	public long 
	getDataReceiveRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataReceiveRate();
	  	}
	  	
	  	return 0;
	}
  
  
  public long 
  getProtocolReceiveRate() 
  {
    PEPeerManager pm = download_manager.getPeerManager();
    
      if (pm != null){
      
        return pm.getStats().getProtocolReceiveRate();
      }
      
      return 0;
  }
  
  
  
	public long 
	getDataSendRate() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
	  	if (pm != null){
	  	
			return pm.getStats().getDataSendRate();
	  	}
	  	
	  	return 0;
	}
  
	public long 
	getProtocolSendRate() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
	    
	    if (pm != null){
	      
	        return pm.getStats().getProtocolSendRate();
	    }
	      
	    return 0;
	}
  
  
	
	public long 
	getETA()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
	  
			return pm.getETA();
		}
	  
		return -1;   //return exactly -1 if ETA is unknown
	}

	public int 
	getCompleted() 
	{
	  DiskManager	dm = download_manager.getDiskManager();
		
	  if (dm == null) {
	    int state = download_manager.getState();
	    if (state == DownloadManager.STATE_ALLOCATING ||
	        state == DownloadManager.STATE_CHECKING ||
	        state == DownloadManager.STATE_INITIALIZING)
	      return completed;
	    else
	      return downloadCompleted;
	  }
	  if (dm.getState() == DiskManager.ALLOCATING || 
	      dm.getState() == DiskManager.CHECKING || 
	      dm.getState() == DiskManager.INITIALIZING)
      return dm.getPercentDone();
	  else {
      long total = dm.getTotalLength();
      return total == 0 ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);
	  }
	}

	public void setCompleted(int _completed) {
	  completed = _completed;
	}

	public int 
	getDownloadCompleted(
		boolean bLive ) 
	{
		DiskManager	dm = download_manager.getDiskManager();
		
			// no disk manager -> not running -> use stored value
		
		if ( dm == null ){
			
		   return downloadCompleted;
		}
		
	    int state = dm.getState();

	    boolean	transient_state = 
	    		state == DiskManager.INITIALIZING ||
	            state == DiskManager.ALLOCATING   ||
	            state == DiskManager.CHECKING;
	    
	    long total = dm.getTotalLength();
	    
	    int computed_completion = (total == 0) ? 0 : (int) ((1000 * (total - dm.getRemaining())) / total);

	    	// use non-transient values to update the record of download completion
	    
	    if ( !transient_state ){
	    	
	    	downloadCompleted = computed_completion;
	    }
	    
	    if ( bLive ){
	    
	    		// return the transient completion level
	    	
	    	return computed_completion;
	    	
	    }else{
	    	
	    		// return the non-transient one
	    	
	    	return( downloadCompleted );
	    }
	}
  
	public void setDownloadCompleted(int _completed) {
		downloadCompleted = _completed;
	}

	public String getElapsedTime() {
	  PEPeerManager	pm = download_manager.getPeerManager();
		
	  if (pm != null){
		return pm.getElapsedTime();
	  }
	  
	  return "";
	}
	
	public long 
	getTimeStarted() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
			return pm.getTimeStarted();
		}
		
		return -1;
	}
	
	public long 
	getTimeStartedSeeding() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
		 
			return pm.getTimeStartedSeeding();
		}
		
		return -1;
	}

	public long 
	getTotalDataBytesReceived() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null) {
			return saved_data_bytes_downloaded + pm.getStats().getTotalDataBytesReceived();
		}
		return(saved_data_bytes_downloaded);
	}	
  
  
	public long 
	getTotalGoodDataBytesReceived() 
	{
		long downloaded	= getTotalDataBytesReceived();
       
		downloaded -= ( getHashFailBytes() + getDiscarded());
		
		if ( downloaded < 0 ){
			
			downloaded = 0;
		}
		
		return( downloaded );
	}
	
	public long 
	getTotalProtocolBytesReceived() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
    
		if (pm != null) {
			return saved_protocol_bytes_downloaded + pm.getStats().getTotalProtocolBytesReceived();
		}
    
		return(saved_protocol_bytes_downloaded);
	} 
  
	public void 
	resetTotalBytesSentReceived(
		long 	new_sent,
		long	new_received )
	{
		boolean running = download_manager.getPeerManager() != null;
		
		if ( running ){
			
			download_manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
		}
		
		
		if ( new_sent >= 0 ){
			
			saved_data_bytes_uploaded		= new_sent;
			saved_protocol_bytes_uploaded	= 0;
		}
		
		if ( new_received >= 0 ){
			
			saved_data_bytes_downloaded			= new_received;
			saved_protocol_bytes_downloaded		= 0;
		}

		if ( running ){
			
			download_manager.setStateWaiting();
		}
	}
	
	public long 
	getTotalDataBytesSent() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
	
		if (pm != null) {
			return saved_data_bytes_uploaded + pm.getStats().getTotalDataBytesSent();
		}
	  
		return( saved_data_bytes_uploaded );
	}
  
  
	public long 
	getTotalProtocolBytesSent() 
	{
		PEPeerManager pm = download_manager.getPeerManager();
		
		if (pm != null) {
			
			return saved_protocol_bytes_uploaded + pm.getStats().getTotalProtocolBytesSent();
		}
		
		return( saved_protocol_bytes_uploaded );
	}
 
	public long 
	getRemaining()
	{
		DiskManager disk_manager = download_manager.getDiskManager();
		
	    if ( disk_manager == null ){
	    	
	    	return download_manager.getSize() - 
		             ((long)getCompleted() * download_manager.getSize() / 1000L);
		
	    }else{
		     
	    	return disk_manager.getRemainingExcludingDND();
		}
	}
	
	public long 
	getDiscarded()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
			
			return saved_discarded + pm.getStats().getTotalDiscarded();
		}
		
		return( saved_discarded );
	}
  

	public long 
	getHashFailCount()
	{
		TOTorrent	t = download_manager.getTorrent();
		
		if ( t == null ){
			
			return(0);
		}
		
		long	total 	= getHashFailBytes();
		
		long	res = total / t.getPieceLength();
		
		if ( res == 0 && total > 0 ){
			
			res = 1;
		}
		
		return( res );
	}
  
	public long
	getHashFailBytes()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		if (pm != null){
			
			return saved_hashfails + pm.getStats().getTotalHashFailBytes();
		}
		
		return( saved_hashfails );
	}

	public long 
	getTotalAverage() 
	{
		PEPeerManager	pm = download_manager.getPeerManager();
	
		if (pm != null){
			
			return pm.getStats().getTotalAverage();
		}
		
		return( 0 );
	}
      
	public long
	getTotalAveragePerPeer()
	{
		int div = download_manager.getNbPeers() + (download_manager.isDownloadComplete(false) ? 0 : 1);  //since total speed includes our own speed when downloading
	    
	    long average = div < 1 ? 0 : getTotalAverage() / div;

	    return( average );
	}
	
	public int 
	getShareRatio() 
	{
		long downloaded	= getTotalGoodDataBytesReceived();
		long uploaded	= getTotalDataBytesSent();
        
		if ( downloaded <= 0 ){
		  
			return( -1 );
		}

		return (int) ((1000 * uploaded) / downloaded);
	}
	
  
	public long 
	getSecondsDownloading() 
	{
	  long lTimeStartedDL = getTimeStarted();
	  if (lTimeStartedDL >= 0) {
  	  long lTimeEndedDL = getTimeStartedSeeding();
  	  if (lTimeEndedDL == -1) {
  	    lTimeEndedDL = SystemTime.getCurrentTime();
  	  }
  	  if (lTimeEndedDL > lTimeStartedDL) {
    	  return saved_SecondsDownloading + ((lTimeEndedDL - lTimeStartedDL) / 1000);
    	}
  	}
	  return saved_SecondsDownloading;
	}

	public long 
	getSecondsOnlySeeding() 
	{
	  long lTimeStarted = getTimeStartedSeeding();
	  if (lTimeStarted >= 0) {
	    return saved_SecondsOnlySeeding + 
	           ((SystemTime.getCurrentTime() - lTimeStarted) / 1000);
	  }
	  return saved_SecondsOnlySeeding;
	}
	
	public float
	getAvailability()
	{
	    PEPeerManager  pm = download_manager.getPeerManager();

	    if ( pm == null ){
	    	
	    	return( -1 );
	    }
		
	    return( pm.getMinAvailability());
	}
	 
  
	public int 
	getUploadRateLimitBytesPerSecond() 
	{  
		return max_upload_rate_bps;  
	}

	public void 
	setUploadRateLimitBytesPerSecond( 
		int max_rate_bps ) 
	{  
		max_upload_rate_bps = max_rate_bps;  
	}
  
	public int 
	getDownloadRateLimitBytesPerSecond() 
	{  
		return max_download_rate_bps;  
	}
  
	public void 
	setDownloadRateLimitBytesPerSecond( 
		int max_rate_bps ) 
	{  
		max_download_rate_bps = max_rate_bps;  
	}
    
	public int 
	getTimeSinceLastDataReceivedInSeconds()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		int	res = saved_SecondsSinceDownload;
		
		if ( pm != null ){
			
			int	current = pm.getStats().getTimeSinceLastDataReceivedInSeconds();
			
			if ( current >= 0 ){
			
					// activity this session, use this value
				
				res = current;
				
			}else{
				
					// no activity this session. If ever has been activity add in session
					// time
				
				if ( res >= 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long	elapsed = now - pm.getTimeStarted();
					
					if ( elapsed < 0 ){
						
						elapsed = 0;
					}
					
					res += elapsed/1000;
				}
			}
		}
		
		return( res );	
	}
	
	public int 
	getTimeSinceLastDataSentInSeconds()
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		
		int	res = saved_SecondsSinceUpload;
		
		if ( pm != null ){
			
			int	current = pm.getStats().getTimeSinceLastDataSentInSeconds();
			
			if ( current >= 0 ){
			
					// activity this session, use this value
				
				res = current;
				
			}else{
				
					// no activity this session. If ever has been activity add in session
					// time
				
				if ( res >= 0 ){
					
					long	now = SystemTime.getCurrentTime();
					
					long	elapsed = now - pm.getTimeStarted();
					
					if ( elapsed < 0 ){
						
						elapsed = 0;
					}
					
					res += elapsed/1000;
				}
			}
		}
		
		return( res );	
	}
	
	public long
	getAvailWentBadTime()
	{
		PEPeerManager	pm = download_manager.getPeerManager();

		if ( pm != null ){
			
			long	bad_time = pm.getAvailWentBadTime();
			
			if ( bad_time > 0 ){
				
					// valid last bad time
				
				return( bad_time );
			}
			
			if ( pm.getMinAvailability() >= 1.0 ){
				
					// we can believe the fact that it isn't bad (we want to ignore 0 results from
					// downloads that never get to a 1.0 availbility)
				
				return( 0 );
			}
		}
		
		DownloadManagerState state = download_manager.getDownloadState();

		return( state.getLongAttribute( DownloadManagerState.AT_AVAIL_BAD_TIME ));
	}
	
	protected void
	saveSessionTotals()
	{
		  	// re-base the totals from current totals and session totals
		  
		saved_data_bytes_downloaded 	= getTotalDataBytesReceived();
		saved_data_bytes_uploaded		= getTotalDataBytesSent();
	  
		saved_discarded				= getDiscarded();
		saved_hashfails				= getHashFailBytes();
	
		saved_SecondsDownloading 		= getSecondsDownloading();
		saved_SecondsOnlySeeding		= getSecondsOnlySeeding();
		
		saved_SecondsSinceDownload		= getTimeSinceLastDataReceivedInSeconds();
		saved_SecondsSinceUpload		= getTimeSinceLastDataSentInSeconds();
		
		DownloadManagerState state = download_manager.getDownloadState();

		state.setIntAttribute( DownloadManagerState.AT_TIME_SINCE_DOWNLOAD, saved_SecondsSinceDownload );
		state.setIntAttribute( DownloadManagerState.AT_TIME_SINCE_UPLOAD, saved_SecondsSinceUpload );
		
		state.setLongAttribute( DownloadManagerState.AT_AVAIL_BAD_TIME, getAvailWentBadTime());
	}
	
 	protected void
  	setSavedDownloadedUploaded(
  		long	d,
  		long	u )
  	{
		saved_data_bytes_downloaded	= d;
		saved_data_bytes_uploaded	= u;		
  	}
 	
	public void
	restoreSessionTotals(
		long		_saved_data_bytes_downloaded,
		long		_saved_data_bytes_uploaded,
		long		_saved_discarded,
		long		_saved_hashfails,
		long		_saved_SecondsDownloading,
		long		_saved_SecondsOnlySeeding )
	{
		saved_data_bytes_downloaded	= _saved_data_bytes_downloaded;
		saved_data_bytes_uploaded	= _saved_data_bytes_uploaded;
		saved_discarded				= _saved_discarded;
		saved_hashfails				= _saved_hashfails;
		saved_SecondsDownloading	= _saved_SecondsDownloading;
		saved_SecondsOnlySeeding	= _saved_SecondsOnlySeeding;
		
		DownloadManagerState state = download_manager.getDownloadState();
		
		saved_SecondsSinceDownload	= state.getIntAttribute( DownloadManagerState.AT_TIME_SINCE_DOWNLOAD );
		saved_SecondsSinceUpload	= state.getIntAttribute( DownloadManagerState.AT_TIME_SINCE_UPLOAD );
	}
	
	protected void 
	generateEvidence(
		IndentWriter writer) 
	{
		writer.println( "DownloadManagerStats" );
		
		try{
			writer.indent();
			
			writer.println( 
				"recv_d=" + getTotalDataBytesReceived() + ",recv_p=" + getTotalProtocolBytesReceived() + ",recv_g=" + getTotalGoodDataBytesReceived() + 
				",sent_d=" + getTotalDataBytesSent() + ",sent_p=" + getTotalProtocolBytesSent() + 
				",discard=" + getDiscarded() + ",hash_fails=" + getHashFailCount() + "/" + getHashFailBytes() +
				",comp=" + getCompleted() 
				+ "[live:" + getDownloadCompleted(true) + "/" + getDownloadCompleted( false) 
				+ "],dl_comp=" + downloadCompleted
				+ ",remaining=" + getRemaining());
	
		}finally{
			
			writer.exdent();
		}
	}
}
