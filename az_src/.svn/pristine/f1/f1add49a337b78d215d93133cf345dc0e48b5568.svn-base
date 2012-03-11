/*
 * File    : TRHostTorrentPublishImpl.java
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

import java.util.*;

import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerTorrent;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

public class 
TRHostTorrentPublishImpl
	implements TRHostTorrent 
{
	private TRHostImpl		host;
	private TOTorrent		torrent;

	private long			date_added;
	
	private int				status	= TS_PUBLISHED;
	private boolean			persistent;
	
	private int					seed_count;
	private int					peer_count;
	private TRHostPeer[]		peers = new TRHostPeer[0];
	
	private List				listeners_cow		= new ArrayList();
	private List				removal_listeners	= new ArrayList();
	
	private HashMap data;

	protected AEMonitor this_mon 	= new AEMonitor( "TRHostTorrentPublish" );

	protected
	TRHostTorrentPublishImpl(
		TRHostImpl		_host,
		TOTorrent		_torrent,
		long			_date_added )
	{
		host		= _host;
		torrent		= _torrent;
		date_added	= _date_added;
	}

	public void
	start()
	{
	}

	public void
	stop()
	{
	}

	public void
	remove()
	
		throws TRHostTorrentRemovalVetoException
	{
		try{
			this_mon.enter();
			
			canBeRemoved();
		
			host.remove( this );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	canBeRemoved()
	
		throws TRHostTorrentRemovalVetoException
	{
		for (int i=0;i<removal_listeners.size();i++){
			
			((TRHostTorrentWillBeRemovedListener)removal_listeners.get(i)).torrentWillBeRemoved( this );
		}
		
		return( true );
	}
	public int
	getStatus()
	{
		return( status );
	}

	public boolean
	isPersistent()
	{
		return( persistent );
	}
	
	public void
	setPersistent(
		boolean		_persistent )
	{
		persistent	= _persistent;
	}
	
	public boolean
	isPassive()
	{
		return( false );
	}
	
	public void
	setPassive(
		boolean		passive )
	{
	}
	
	public long
	getDateAdded()
	{
		return( date_added );
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}

	public TRTrackerServerTorrent 
	getTrackerTorrent() 
	{
		return( null );
	}
	
	public int
	getPort()
	{
		return( -1 );
	}
	
	public TRHostPeer[]
	getPeers()
	{
		try{
			this_mon.enter();
		
			return( peers );
		}finally{
			
			this_mon.exit();
		}
	}	

	public long
	getAnnounceCount()
	{
		return( 0 );
	}
	
	public long
	getAverageAnnounceCount()
	{
		return( 0 );
	}
	
	public long
	getScrapeCount()
	{
		return( 0 );
	}
	
	public long
	getAverageScrapeCount()
	{
		return( 0 );
	}
	
	public long
	getCompletedCount()
	{
		return( 0 );
	}
	
	protected void
	updateStats()
	{		
		TRTrackerScraperResponse resp = null;
		
		TRTrackerAnnouncer tc = host.getTrackerClient( this );
		
		if ( tc != null ){
			
			resp = TRTrackerScraperFactory.getSingleton().scrape( tc );
		}
		
		if ( resp == null ){
			
			resp = TRTrackerScraperFactory.getSingleton().scrape( torrent );
		}
				
		try{
			this_mon.enter();
		
			if ( resp != null && resp.isValid()){
						
				peer_count 	= resp.getPeers();
				seed_count	= resp.getSeeds();
				
				peers = new TRHostPeer[ peer_count + seed_count ];
				
				for (int i=0;i<peers.length;i++){
					
					peers[i] = new TRHostPeerPublishImpl( i<seed_count );
				}
			}else{
				
				peers = new TRHostPeer[0];
			}
		}finally{
			
			this_mon.exit();
		}
	}

	public int
	getSeedCount()
	{
		return( seed_count );
	}
	
	public int
	getLeecherCount()
	{
		return( peer_count );
	}
	
	public int
	getBadNATCount()
	{
		return( 0 );
	}
	
	public long
	getTotalUploaded()
	{
		return( 0 );
	}

	public long
	getTotalDownloaded()
	{
		return( 0 );
	}	

	public long
	getTotalLeft()
	{
		return( 0 );
	}

	public long
	getAverageUploaded()
	{
		return( 0 );
	}

	public long
	getAverageDownloaded()
	{
		return( 0 );
	}
	
	public long
	getTotalBytesIn()
	{
		return( 0 );
	}
	
	public long
	getTotalBytesOut()
	{
		return( 0 );
	}
	
	public long
	getAverageBytesIn()
	{
		return( 0 );
	}
	
	public long
	getAverageBytesOut()
	{
		return( 0 );
	}
	
	public void
	disableReplyCaching()
	{
	}
	
	protected void
	preProcess(
		TRHostTorrentRequest	req )
	
		throws TRHostException
	{
		List	listeners_ref = listeners_cow;
	
		for (int i=0;i<listeners_ref.size();i++){
		
			try{
				((TRHostTorrentListener)listeners_ref.get(i)).preProcess(req);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	postProcess(
		TRHostTorrentRequest	req )
	
		throws TRHostException
	{
		List	listeners_ref = listeners_cow;
	
		for (int i=0;i<listeners_ref.size();i++){
		
			try{
				((TRHostTorrentListener)listeners_ref.get(i)).postProcess(req);
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	addListener(
		TRHostTorrentListener	l )
	{
		try{
			this_mon.enter();
	
			List	new_listeners = new ArrayList( listeners_cow );
			
			new_listeners.add(l);
			
			listeners_cow	= new_listeners;
			
		}finally{
			
			this_mon.exit();
		}
		
		host.torrentListenerRegistered();
	}
	
	public void
	removeListener(
		TRHostTorrentListener	l )
	{
		try{
			this_mon.enter();
		
			List	new_listeners = new ArrayList( listeners_cow );
			
			new_listeners.remove(l);
			
			listeners_cow	= new_listeners;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	addRemovalListener(
		TRHostTorrentWillBeRemovedListener	l )
	{
		try{
			this_mon.enter();
			
			removal_listeners.add(l);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeRemovalListener(
		TRHostTorrentWillBeRemovedListener	l )
	{
		try{
			this_mon.enter();
			
			removal_listeners.remove(l);
		}finally{
			
			this_mon.exit();
		}
	}

  /** To retreive arbitrary objects against this object. */
  public Object getData (String key) {
  	if (data == null) return null;
    return data.get(key);
  }

  /** To store arbitrary objects against this object. */
  public void setData (String key, Object value) {
	try{
		this_mon.enter();

	  	if (data == null) {
	  	  data = new HashMap();
	  	}
	    if (value == null) {
	      if (data.containsKey(key))
	        data.remove(key);
	    } else {
	      data.put(key, value);
	    }
	}finally{
		
		this_mon.exit();
	}
  }
}
