/*
 * Created on 14-Feb-2005
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

package org.gudy.azureus2.core3.tracker.client.impl.bt;

import java.net.URL;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperImpl;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperResponseImpl;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

/**
 * @author parg
 *
 */

public class 
TRTrackerBTScraperImpl 
{
	protected static TRTrackerBTScraperImpl	singleton;
	protected static AEMonitor 				class_mon 	= new AEMonitor( "TRTrackerBTScraper" );

	private TRTrackerScraperImpl		scraper;
	private TrackerChecker				tracker_checker;

	public static TRTrackerBTScraperImpl
	create(
		TRTrackerScraperImpl	_scraper )
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
			
				singleton =  new TRTrackerBTScraperImpl( _scraper );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected
	TRTrackerBTScraperImpl(
		TRTrackerScraperImpl	_scraper )
	{
		scraper	= _scraper;
		
		tracker_checker = new TrackerChecker( this );
	}
	
	protected TRTrackerScraperImpl
	getScraper()
	{
		return( scraper );
	}
	
	public void
	setScrape(
		TOTorrent				torrent,
		URL						url,
		DownloadScrapeResult	result )
	{
		if ( torrent != null && result != null ){
			
			TRTrackerScraperResponseImpl resp =	tracker_checker.getHashData( torrent, url );
			
			URL result_url = result.getURL();
			
			boolean	update_is_dht	= TorrentUtils.isDecentralised( result_url );
			
				// only override details if underlying scrape is failing or this is an update
				// to an existing dht-backup result
			
			if ( 	resp != null && 
					( 	resp.getStatus() == TRTrackerScraperResponse.ST_ERROR ) ||
						resp.isDHTBackup() && update_is_dht ){
				
				resp.setDHTBackup( update_is_dht );
				
				resp.setScrapeStartTime( result.getScrapeStartTime());
				
					// leave nextScrapeStartTime alone as we still want the existing
					// scraping mechanism to kick in and check the torrent's tracker
				
				resp.setStatus( 
						result.getResponseType()==DownloadScrapeResult.RT_SUCCESS?
								TRTrackerScraperResponse.ST_ONLINE:
								TRTrackerScraperResponse.ST_ERROR,
						result.getStatus() + " (" + (update_is_dht?MessageText.getString( "dht.backup.only" ):(result_url==null?"<null>":result_url.getHost())) + ")");

				// call this last before dispatching listeners as it does another dispatch by itself ~~
				resp.setSeedsPeers( result.getSeedCount(), result.getNonSeedCount());
			
				scraper.scrapeReceived( resp );
			}
		}
	}
	
	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent,
		URL				target_url,
		boolean			force )
	{
		if (torrent == null){
			
			return null;
		}

		if ( force ){
			
			tracker_checker.syncUpdate( torrent, target_url );
		}
		
		TRTrackerScraperResponse	res = tracker_checker.getHashData( torrent, target_url );
		
		// System.out.println( "scrape: " + torrent + " -> " + (res==null?"null":""+res.getSeeds()));
		
		return( res );
	}
	
	public TRTrackerScraperResponse
	peekScrape(
		TOTorrent		torrent,
		URL				target_url )
	{
		if ( torrent == null ){
			
			return null;
		}

		TRTrackerScraperResponse	res = tracker_checker.peekHashData( torrent, target_url );
				
		return( res );
	}
	
	public TRTrackerScraperResponse
	scrape(
		TRTrackerAnnouncer	tracker_client )
	{
		TRTrackerScraperResponse	res = tracker_checker.getHashData( tracker_client );
		
		// System.out.println( "scrape: " + tracker_client + " -> " + (res==null?"null":""+res.getSeeds()));
		
		return( res );
	}
	
	public void
	remove(
		TOTorrent		torrent )
	{
		tracker_checker.removeHash( torrent );
	}
}
