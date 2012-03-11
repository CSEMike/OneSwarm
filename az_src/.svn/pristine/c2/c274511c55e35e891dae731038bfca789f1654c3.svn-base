/*
 * Created on 21-Jun-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.remote.tracker;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.remote.*;
import org.gudy.azureus2.pluginsimpl.remote.torrent.RPTorrent;


/**
 * @author parg
 *
 */

public class 
RPTrackerTorrent
	extends		RPObject
	implements 	TrackerTorrent
{
	protected transient TrackerTorrent		delegate;
	
		// don't change the names of these, they appear in XML serialisation

	public RPTorrent				torrent;

	public int		status;
	public long		total_uploaded;
	public long		total_downloaded;
	public long		average_uploaded;
	public long		average_downloaded;
	public long		total_left;
	public long 	completed_count;
	public long		total_bytes_in;
	public long		average_bytes_in;
	public long		total_bytes_out;
	public long 	average_bytes_out;
	public long		scrape_count;
	public long		average_scrape_count;
	public long		announce_count;
	public long		average_announce_count;
	public int		seed_count;
	public int		leecher_count;
	public int		bad_NAT_count;
	
	
	public static RPTrackerTorrent
	create(
		TrackerTorrent		_delegate )
	{
		RPTrackerTorrent	res =(RPTrackerTorrent)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPTrackerTorrent( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPTrackerTorrent(
		TrackerTorrent		_delegate )
	{
		super( _delegate );
		
		if ( delegate.getTorrent() != null ){
			
			torrent = (RPTorrent)_lookupLocal( delegate.getTorrent());
		
			if ( torrent == null ){
				
				torrent = RPTorrent.create( delegate.getTorrent());
			}
		}
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (TrackerTorrent)_delegate;
		
		status					= delegate.getStatus();
		total_uploaded			= delegate.getTotalUploaded();
		total_downloaded		= delegate.getTotalDownloaded();
		average_uploaded		= delegate.getAverageUploaded();
		average_downloaded		= delegate.getAverageDownloaded();
		total_left				= delegate.getTotalLeft();
		completed_count			= delegate.getCompletedCount();
		total_bytes_in			= delegate.getTotalBytesIn();
		average_bytes_in		= delegate.getAverageBytesIn();
		total_bytes_out			= delegate.getTotalBytesOut();
		average_bytes_out		= delegate.getAverageBytesOut();
		scrape_count			= delegate.getScrapeCount();
		average_scrape_count	= delegate.getAverageScrapeCount();
		announce_count			= delegate.getAnnounceCount();
		average_announce_count	= delegate.getAverageAnnounceCount();
		seed_count				= delegate.getSeedCount();
		leecher_count			= delegate.getLeecherCount();
		bad_NAT_count			= delegate.getBadNATCount();
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		Object res = _fixupLocal();
		
		if ( torrent != null ){
			
			torrent._setLocal();
		}
		
		return( res );
	}
	
	public void
	_setRemote(
		RPRequestDispatcher		dispatcher )
	{
		super._setRemote( dispatcher );
		
		if ( torrent != null ){
			
			torrent._setRemote( dispatcher );
		}
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String		method 	= request.getMethod();
		// Object[]	params	= request.getParams();			
		
		throw( new RPException( "Unknown method: " + method ));
	}

		//***************************************************************************8
	
	public void
	start()
	
		throws TrackerException
	{
		notSupported();
	}
	
	public void
	stop()
	
		throws TrackerException
	{
		notSupported();
	}
	
	public void
	remove()
	
		throws TrackerTorrentRemovalVetoException
	{
		notSupported();
		
	}
	
	public boolean
	canBeRemoved()
	
		throws TrackerTorrentRemovalVetoException
	{
		notSupported();
		
		return( false );	
	}
	
	
	public Torrent
	getTorrent()
	{
		return( torrent );	
	}
	
	public TrackerPeer[]
	getPeers()
	{
		notSupported();
		
		return( null );	
	}
	
	public int
	getStatus()
	{
		return( status );	
	}
	
	public long
	getTotalUploaded()
	{
		return( total_uploaded );	
	}
	
	public long
	getTotalDownloaded()
	{
		return( total_downloaded );	
	}
	
	public long
	getAverageUploaded()
	{
		return( average_uploaded );
	}
	
	public long
	getAverageDownloaded()
	{
		return( average_downloaded );	
	}
	
	public long
	getTotalLeft()
	{
		return( total_left );	
	}
	
	public long
	getCompletedCount()
	{
		return( completed_count );	
	}

	public long
	getTotalBytesIn()
	{
		return( total_bytes_in );	
	}	
	
	public long
	getAverageBytesIn()
	{
		return( average_bytes_in );	
	}
	
	public long
	getTotalBytesOut()
	{
		return( total_bytes_out );	
	}
	
	public long
	getAverageBytesOut()
	{
		return( average_bytes_out );	
	}	

	public long
	getScrapeCount()
	{
		return( scrape_count );	
	}
	
	public long
	getAverageScrapeCount()
	{
		return( average_scrape_count );
	}
	
	public long
	getAnnounceCount()
	{
		return( announce_count );	
	}
	
	public long
	getAverageAnnounceCount()
	{
		return( average_announce_count );
	}
	
	public int
	getSeedCount()
	{
		return( seed_count );	
	}	
	
	public int
	getLeecherCount()
	{
		return( leecher_count);	
	}
	
	public int
	getBadNATCount()
	{
		return( bad_NAT_count );
	}
	
	public void
	disableReplyCaching()
	{
		notSupported();
	}
	
	public boolean
	isPassive()
	{
		notSupported();
		
		return( false );
	}
	
	public long
	getDateAdded()
	{
		notSupported();
		
		return( 0 );		
	}
	
	public void
	addListener(
		TrackerTorrentListener	listener )
	{
		notSupported();
	}
	
	public void
	removeListener(
		TrackerTorrentListener	listener )
	{
		notSupported();
	}
	
	public void
	addRemovalListener(
		TrackerTorrentWillBeRemovedListener	listener )
	{
		notSupported();
	}
	
	
	public void
	removeRemovalListener(
		TrackerTorrentWillBeRemovedListener	listener )
	{
		notSupported();
	}
}
