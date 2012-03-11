/*
 * File    : PRDownloadStats.java
 * Created : 30-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.remote.download;

/**
 * @author parg
 *
 */
import org.gudy.azureus2.plugins.download.*;

import org.gudy.azureus2.pluginsimpl.remote.*;


public class 
RPDownloadStats
	extends		RPObject
	implements 	DownloadStats
{
	protected transient DownloadStats		delegate;

		// don't change these field names as they are visible on XML serialisation

	public long				downloaded;
	public long				uploaded;
	public int				completed;
	public int				downloadCompletedLive;
	public int				downloadCompletedStored;
	public String			status;
	public String			status_localised;
	public long				upload_average;
	public long				download_average;
	public String			eta;
	public int				share_ratio;
	public float			availability;
	public int				health;
	
	public static RPDownloadStats
	create(
		DownloadStats		_delegate )
	{
		RPDownloadStats	res =(RPDownloadStats)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDownloadStats( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPDownloadStats(
		DownloadStats		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (DownloadStats)_delegate;
		
		downloaded					= delegate.getDownloaded();
		uploaded					= delegate.getUploaded();
		completed					= delegate.getCompleted();
		downloadCompletedLive		= delegate.getDownloadCompleted(true);
		downloadCompletedStored		= delegate.getDownloadCompleted(false);
		status						= delegate.getStatus();
		status_localised			= delegate.getStatus(true);
		upload_average				= delegate.getUploadAverage();
		download_average			= delegate.getDownloadAverage();
		eta							= delegate.getETA();
		share_ratio					= delegate.getShareRatio();
		availability				= delegate.getAvailability();
		health						= delegate.getHealth();
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();	
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	
		// ***************************************************
	
	public String
	getStatus()
	{
		return( status );
	}
	
	public String
	getStatus(boolean localised)
	{
		return (localised)? status_localised : status;
	}
	
	public String
	getDownloadDirectory()
	{
		notSupported();
		
		return( null );
	}	
	
	public String
	getTargetFileOrDir()
	{
		notSupported();
		
		return( null );
	}	
	
	public String
	getTrackerStatus()
	{
		notSupported();
		
		return( null );
	}
	
	public int
	getCompleted()
	{
		return( completed );
	}
	
	public int
	getDownloadCompleted(boolean bLive)
	{
		return( bLive ? downloadCompletedLive : downloadCompletedStored );
	}

	
	public int
	getCheckingDoneInThousandNotation()
	{
		notSupported();
		
		return( 0 );
	}

	public void
	resetUploadedDownloaded(
		long l1, 
		long l2 )
	{
		notSupported();
	}
	
	public long
	getDownloaded()
	{
		return( downloaded );
	}
	
	public long
	getUploaded()
	{
		return( uploaded );
	}
	
	public long
	getRemaining()
	{
		notSupported();
		
		return( 0 );
	}
	
	public long
	getDiscarded()
	{
		notSupported();
		
		return( 0 );
	}
	
	public long
	getDownloadAverage()
	{
		return( download_average );
	}
	
	public long
	getUploadAverage()
	{
		return( upload_average );
	}
	
	public long
	getTotalAverage()
	{
		notSupported();
		
		return( 0 );
	}
	
	public String
	getElapsedTime()
	{
		notSupported();
		
		return( null );
	}	
	
	public String
	getETA()
	{
		return( eta );
	}
	
	public long
	getETASecs()
	{
		notSupported();
		return(0);
	}
	
	public long
	getHashFails()
	{
		notSupported();
		
		return( 0 );
	}
	
	public int
	getShareRatio()
	{
		return( share_ratio );
	}
	
	public long 
	getTimeStarted() 
	{
		 notSupported();
		 return ( 0 );
	}
	
	public float
	getAvailability()
	{
		return( availability );
	}
  
  public long getSecondsDownloading() {
		 notSupported();
		 return ( 0 );
  }
  
  public long getSecondsOnlySeeding() {
		 notSupported();
		 return ( 0 );
  }
  
  public long getTimeStartedSeeding() {
		 notSupported();
		 return ( 0 );
  }
  
	public long
	getSecondsSinceLastDownload()
	{
		notSupported();
		return ( 0 );	
	}
	
	public long
	getSecondsSinceLastUpload()
	{
		notSupported();
		return ( 0 );	
	}
	
	public int
	getHealth()
	{
		return( health );
	}
}