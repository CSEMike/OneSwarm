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

package org.gudy.azureus2.core3.tracker.client.impl.dht;

import java.util.*;
import java.net.URL;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperClientResolver;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperImpl;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

/**
 * @author parg
 *
 */

public class 
TRTrackerDHTScraperImpl 
{
	protected static TRTrackerDHTScraperImpl	singleton;
	protected static AEMonitor 					class_mon 	= new AEMonitor( "TRTrackerDHTScraper" );

	private TRTrackerScraperImpl		scraper;

	private Map<HashWrapper,TRTrackerDHTScraperResponseImpl>		responses = new HashMap<HashWrapper,TRTrackerDHTScraperResponseImpl>();
	
	public static TRTrackerDHTScraperImpl
	create(
		TRTrackerScraperImpl	_scraper )
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
			
				singleton =  new TRTrackerDHTScraperImpl( _scraper );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected
	TRTrackerDHTScraperImpl(
		TRTrackerScraperImpl	_scraper )
	{
		scraper	= _scraper;
	}
	
	public void
	setScrape(
		TOTorrent				torrent,
		URL						url,
		DownloadScrapeResult	result )
	{
		if ( torrent != null && result != null){
			
			try{
				TRTrackerDHTScraperResponseImpl resp = 
					new TRTrackerDHTScraperResponseImpl( torrent.getHashWrapper(), result.getURL());
							
				resp.setSeedsPeers( result.getSeedCount(), result.getNonSeedCount());
				
				resp.setScrapeStartTime( result.getScrapeStartTime());
				
				resp.setNextScrapeStartTime( result.getNextScrapeStartTime());
				
				resp.setStatus( 
						result.getResponseType()==DownloadScrapeResult.RT_SUCCESS?
								TRTrackerScraperResponse.ST_ONLINE:
								TRTrackerScraperResponse.ST_ERROR,
						result.getStatus()); 
			
				responses.put( torrent.getHashWrapper(), resp );
				
				scraper.scrapeReceived( resp );
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public TRTrackerScraperResponse
	scrape(
		TOTorrent		torrent,
		URL				unused_target_url,
		boolean			unused_force )
	{
		if ( torrent != null ){

			try{
				HashWrapper hw = torrent.getHashWrapper();
				
				TRTrackerDHTScraperResponseImpl response = responses.get( hw );
				
				if ( response == null ){
					
					TRTrackerScraperClientResolver resolver = scraper.getClientResolver();
					
					if ( resolver != null ){
						
						int[] cache = resolver.getCachedScrape( hw );
						
						if ( cache != null ){
							
							response = 
								new TRTrackerDHTScraperResponseImpl( 
										hw, torrent.getAnnounceURL());
										
							response.setSeedsPeers( cache[0], cache[1] );
							
							long now = SystemTime.getCurrentTime();
							
							response.setScrapeStartTime( now );
							
							response.setNextScrapeStartTime( now + 5*60*1000 );

							response.setStatus( 
									TRTrackerScraperResponse.ST_ONLINE,
									MessageText.getString( "Scrape.status.cached" )); 
						
							responses.put( torrent.getHashWrapper(), response );
							
							scraper.scrapeReceived( response );
						}
					}
				}
				
				return( response );
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
	
	public TRTrackerScraperResponse
	peekScrape(
		TOTorrent		torrent,
		URL				unused_target_url )
	{
		if ( torrent != null ){

			try{
				HashWrapper hw = torrent.getHashWrapper();
				
				return( responses.get( hw ));
				
			}catch( TOTorrentException e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		return( null );
	}
	
	public TRTrackerScraperResponse
	scrape(
		TRTrackerAnnouncer	tracker_client )
	{
		return( scrape( tracker_client.getTorrent(), null, false ));
	}
	
	public void
	remove(
		TOTorrent		torrent )
	{
		try{
			responses.remove( torrent.getHashWrapper());
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace(e);
		}
	}
}
