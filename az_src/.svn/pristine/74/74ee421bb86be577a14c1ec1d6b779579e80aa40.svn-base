/*
 * File    : TRTrackerScraperImpl.java
 * Created : 09-Oct-2003
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
 
package org.gudy.azureus2.core3.tracker.client.impl;

/**
 * @author parg
 *
 */

import java.net.URL;

import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.tracker.client.impl.bt.TRTrackerBTScraperImpl;
import org.gudy.azureus2.core3.tracker.client.impl.dht.TRTrackerDHTScraperImpl;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

public class 
TRTrackerScraperImpl
	implements TRTrackerScraper 
{
	private static TRTrackerScraperImpl		singleton;
	private static AEMonitor 				class_mon 	= new AEMonitor( "TRTrackerScraper" );

	private TRTrackerBTScraperImpl		bt_scraper;
	private TRTrackerDHTScraperImpl		dht_scraper;
	
	private TRTrackerScraperClientResolver		client_resolver;
	
	// DiskManager listeners
	
	private static final int LDT_SCRAPE_RECEIVED		= 1;
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"TrackerScraper:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					TRTrackerScraperListener	listener = (TRTrackerScraperListener)_listener;
					
					listener.scrapeReceived((TRTrackerScraperResponse)value);
				}
			});	
	
	public static TRTrackerScraperImpl
	create()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
			
				singleton =  new TRTrackerScraperImpl();
			}
			
			return( singleton );
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected
	TRTrackerScraperImpl()
	{
		bt_scraper 	= TRTrackerBTScraperImpl.create( this );
		
		dht_scraper	= TRTrackerDHTScraperImpl.create( this );
	}

	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent )
	{
		return( scrape( torrent, false ));
	}
	
	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent,
		URL				target_url )
	{
		return( scrape( torrent, target_url, false ));
	}
	
	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent,
		boolean			force )
	{
		return( scrape( torrent, null, force ));
	}
	
	public void
	setScrape(
		TOTorrent				torrent,
		URL						url,
		DownloadScrapeResult	result )
	{
		if ( torrent != null ){
		
			if ( TorrentUtils.isDecentralised( torrent )){
				
				dht_scraper.setScrape( torrent, url, result );
				
			}else{
				
				bt_scraper.setScrape( torrent, url, result );
			}
		}
	}
	
	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent,
		URL				target_url,
		boolean			force )
	{
		if (torrent == null ){
			
			return null;
		}

		if ( TorrentUtils.isDecentralised( torrent )){
			
			return( dht_scraper.scrape( torrent, target_url, force ));
			
		}else{
			
			return( bt_scraper.scrape( torrent, target_url, force ));
		}
	}
		
	public TRTrackerScraperResponse
	peekScrape(
		TOTorrent		torrent,
		URL				target_url )
	{
		if ( torrent == null ){
			
			return null;
		}

		if ( TorrentUtils.isDecentralised( torrent )){
			
			return( dht_scraper.peekScrape( torrent, target_url ));
			
		}else{
			
			return( bt_scraper.peekScrape( torrent, target_url ));
		}
	}
	
	public TRTrackerScraperResponse
	scrape(
		TRTrackerAnnouncer	tracker_client )
	{
		TOTorrent	torrent = tracker_client.getTorrent();

		if ( TorrentUtils.isDecentralised( torrent )){

			return( dht_scraper.scrape( tracker_client ));
			
		}else{
			
			return( bt_scraper.scrape( tracker_client ));
		}
	}

	public void
	remove(
		TOTorrent		torrent )
	{
		if ( TorrentUtils.isDecentralised( torrent )){

			dht_scraper.remove( torrent );

		}else{
		
			bt_scraper.remove( torrent );
		}
	}
	
	public void
	scrapeReceived(
		TRTrackerScraperResponse		response )
	{
		listeners.dispatch( LDT_SCRAPE_RECEIVED, response );
	}

	public void
	setClientResolver(
		TRTrackerScraperClientResolver	resolver )
	{
		client_resolver	= resolver;
	}
	
	public TRTrackerScraperClientResolver
	getClientResolver()
	{
		return( client_resolver );
	}
	
	public boolean
	isTorrentDownloading(
		HashWrapper		hash )
	{
		if ( client_resolver == null ){
			
			return( false );
		}
		
		int	state = client_resolver.getStatus( hash );
		
		return( state == TRTrackerScraperClientResolver.ST_RUNNING );
	}
	
	public boolean
	isTorrentRunning(
		HashWrapper		hash )
	{
		if ( client_resolver == null ){
			
			return( false );
		}
		
		int	state = client_resolver.getStatus( hash );
		
		return( state == TRTrackerScraperClientResolver.ST_RUNNING || state == TRTrackerScraperClientResolver.ST_QUEUED );
	}
	
	public boolean
	isNetworkEnabled(
		HashWrapper	hash,
		URL			url )
	{
		if ( client_resolver == null ){
			
			return( false );
		}
		
		return( client_resolver.isNetworkEnabled( hash, url ));
	}
	
	public Object[]
	getExtensions(
		HashWrapper	hash )
	{
		if ( client_resolver == null ){
			
			return( null );
		}
		
		return( client_resolver.getExtensions( hash ));
	}
	
	public boolean
	redirectTrackerUrl(
		HashWrapper		hash,
		URL				old_url,
		URL				new_url )
	{
		return( client_resolver.redirectTrackerUrl( hash, old_url, new_url ));
	}
	
	public void
	addListener(
		TRTrackerScraperListener	l )
	{
		listeners.addListener(l);
	}
	
	public void
	removeListener(
		TRTrackerScraperListener	l )
	{
		listeners.removeListener(l);
	}
}
