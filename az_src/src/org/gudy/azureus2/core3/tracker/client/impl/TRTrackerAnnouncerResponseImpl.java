/*
 * File    : TRTrackerResponseImpl.java
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

package org.gudy.azureus2.core3.tracker.client.impl;

import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.HashWrapper;

public class 
TRTrackerAnnouncerResponseImpl
	implements TRTrackerAnnouncerResponse 
{
	private URL				url;
	private HashWrapper		hash;
	private int				status;
	private long			time_to_wait;
	private String			failure_reason;
	
	private int				scrape_complete		= -1;
	private int				scrape_incomplete	= -1;
	
	protected TRTrackerAnnouncerResponsePeer[]	peers;
	
	protected Map						extensions;
	
	public
	TRTrackerAnnouncerResponseImpl(
		URL			_url,
		HashWrapper	_hash,
		int			_status,
		long		_time_to_wait  )
	{
		url				= _url;
		hash			= _hash;
		status			= _status;	
		time_to_wait	= _time_to_wait;
	}
	
	public
	TRTrackerAnnouncerResponseImpl(
		URL			_url,
		HashWrapper	_hash,
		int			_status,
		long		_time_to_wait,
		String		_failure_reason )
	{
		url				= _url;
		hash			= _hash;
		status			= _status;	
		time_to_wait	= _time_to_wait;
		failure_reason	= _failure_reason;
	}
	
	public
	TRTrackerAnnouncerResponseImpl(
		URL									_url,
		HashWrapper							_hash,
		int									_status,
		long								_time_to_wait,
		TRTrackerAnnouncerResponsePeer[]	_peers )
	{
		url				= _url;
		hash			= _hash;
		status			= _status;	
		time_to_wait	= _time_to_wait;
		peers			= _peers;
	}
	
	public HashWrapper
	getHash()
	{
		return( hash );
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public String
	getStatusString()
	{
		String	str = "";
		
		if ( status == ST_OFFLINE ){
		
			str = "Offline";
			
		}else if  (status == ST_ONLINE ){
			
			str = "OK";
		}else{
			
			str = "Failed";
		}
		
		if ( failure_reason != null && failure_reason.length() > 0 ){
			
			str += " - " + failure_reason;
		}
		
		return( str );
	}
	
	public void setFailurReason(String reason) {
		failure_reason = reason;
	}
	
	public long
	getTimeToWait()
	{
		return( time_to_wait );
	}
	
	public String
	getAdditionalInfo()
	{
		return( failure_reason );
	}
	
	public void
	setPeers(
		TRTrackerAnnouncerResponsePeer[]		_peers )
	{
		peers	= _peers;
	}
	
	public TRTrackerAnnouncerResponsePeer[]
	getPeers()
	{
		return( peers );
	}
	
	public void
	setExtensions(
		Map		_extensions )
	{
		extensions = _extensions;
	}
	
	public Map
	getExtensions()
	{
		return( extensions );
	}
	
	public URL
	getURL()
	{
		return( url );
	}
	
	public int
	getScrapeCompleteCount()
	{
		return( scrape_complete );
	}
	
	public int
	getScrapeIncompleteCount()
	{
		return( scrape_incomplete );
	}
	
	public void
	setScrapeResult(
		int		_complete,
		int		_incomplete )
	{
		scrape_complete		= _complete;
		scrape_incomplete	= _incomplete;
	}
	
	public void
	print()
	{
		System.out.println( "TRTrackerResponse::print");
		System.out.println( "\tstatus = " + getStatus());
		System.out.println( "\tfail msg = " + getAdditionalInfo());
		System.out.println( "\tpeers:" );
		
		if ( peers != null ){
					
			for (int i=0;i<peers.length;i++){
				
				TRTrackerAnnouncerResponsePeer	peer = peers[i];
				
				System.out.println( "\t\t" + peer.getAddress() + ":" + peer.getPort());
			}
		}
	}
		
	public String
	getString()
	{
		String	str = "url=" + url + ", status=" + getStatus();
		
		if ( getStatus() != ST_ONLINE ){
			
			str +=", error=" + getAdditionalInfo();
		}
		
		str += ", time_to_wait=" + time_to_wait;
		
		str += ", scrape_comp=" + scrape_complete + ", scrape_incomp=" + scrape_incomplete;
		
		return( str );
	}
}
