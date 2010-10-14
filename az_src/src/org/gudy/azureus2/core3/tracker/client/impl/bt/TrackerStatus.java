/*
 * Created on 22 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperClientResolver;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperImpl;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperResponseImpl;
import org.gudy.azureus2.core3.tracker.protocol.udp.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.clientid.ClientIDException;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.pluginsimpl.local.clientid.ClientIDManagerImpl;

import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.net.udp.uc.PRUDPPacket;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerException;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;

/**
 * @author Olivier
 * 
 */
 
/** One TrackerStatus object handles scrape functionality for all torrents
 * on one tracker.
 */
public class TrackerStatus {
  // Used to be componentID 2
	private final static LogIDs LOGID = LogIDs.TRACKER;
  // header for our MessageText messages.  Used to shorten code. 
  private final static String SS = "Scrape.status.";
  private final static String SSErr = "Scrape.status.error.";

  private final static int FAULTY_SCRAPE_RETRY_INTERVAL = 60 * 10 * 1000;
  private final static int NOHASH_RETRY_INTERVAL = 1000 * 60 * 60 * 3; // 3 hrs
  
  /**
   * When scraping a single hash, also scrape other hashes that are going to
   * be scraped within this range.
   */
  private final static int GROUP_SCRAPES_MS 	= 60 * 15 * 1000;
  private final static int GROUP_SCRAPES_LIMIT	= 20;

  
  static{
  	PRUDPTrackerCodecs.registerCodecs();
  }
  
	private byte autoUDPscrapeEvery = 1;
	
	private int scrapeCount;
  
  private static List	logged_invalid_urls	= new ArrayList();
  
  private static ThreadPool	thread_pool = new ThreadPool( "TrackerStatus", 10, true );	// queue when full rather than block
  
  private final URL		tracker_url;
  private boolean		az_tracker;
  
  private String 	scrapeURL = null;
 
  /** key = Torrent hash.  values = TRTrackerScraperResponseImpl */
  private HashMap 					hashes;
  /** only needed to notify listeners */ 
  private TRTrackerScraperImpl		scraper;
  
  private boolean bSingleHashScrapes = false;
    
  protected AEMonitor hashes_mon 	= new AEMonitor( "TrackerStatus:hashes" );
  private final TrackerChecker checker;
  
  private volatile int numActiveScrapes = 0;

  public 
  TrackerStatus(
  	TrackerChecker 			_checker, 
  	TRTrackerScraperImpl	_scraper, 
	URL 					_tracker_url ) 
  {    	
  	checker 	= _checker;
	scraper		= _scraper;
    tracker_url	= _tracker_url;
    
    az_tracker = TRTrackerUtils.isAZTracker( tracker_url );
    
    bSingleHashScrapes	= COConfigurationManager.getBooleanParameter( "Tracker Client Scrape Single Only" );
    
    String trackerUrl	= tracker_url.toString();
    
    hashes = new HashMap();
    
    try {
      trackerUrl = trackerUrl.replaceAll(" ", "");
      int position = trackerUrl.lastIndexOf('/');
      if(	position >= 0 &&
      		trackerUrl.length() >= position+9 && 
      		trackerUrl.substring(position+1,position+9).equals("announce")) {

        this.scrapeURL = trackerUrl.substring(0,position+1) + "scrape" + trackerUrl.substring(position+9);
        // System.out.println( "url = " + trackerUrl + ", scrape =" + scrapeURL );
        
      }else if ( trackerUrl.toLowerCase().startsWith("udp:")){
      		// UDP scrapes aren't based on URL rewriting, just carry on
      	
      	scrapeURL = trackerUrl;
      	
       }else if ( position >= 0 && trackerUrl.lastIndexOf('.') < position ){
       	
       		// some trackers support /scrape appended but don't have an /announce
       		// don't do this though it the URL ends with .php (or infact .<anything>)
       	
       	scrapeURL = trackerUrl + (trackerUrl.endsWith("/")?"":"/") + "scrape";
       	
      } else {
        if (!logged_invalid_urls.contains(trackerUrl)) {

          logged_invalid_urls.add(trackerUrl);
          // Error logging is done by the caller, since it has the hash/torrent info
        }
      }
    } catch (Throwable e) {
    	Debug.printStackTrace( e );
    } 
  }

  
  protected boolean 
  isTrackerScrapeUrlValid() 
  {
    return scrapeURL != null;
  }
  
  
  
  protected TRTrackerScraperResponseImpl getHashData(HashWrapper hash) {
  	try{
  		hashes_mon.enter();
  		
  		return (TRTrackerScraperResponseImpl) hashes.get(hash);
  	}finally{
  		
  		hashes_mon.exit();
  	}
  }

 


  protected void 
  updateSingleHash(
	HashWrapper hash, 
	boolean force) 
  {
    updateSingleHash(hash, force, true);
  }

  	protected void 
  	updateSingleHash(
  		HashWrapper 	hash, 
  		boolean 		force, 
  		boolean 		async ) 
  	{      
  		//LGLogger.log( "updateSingleHash():: force=" + force + ", async=" +async+ ", url=" +scrapeURL+ ", hash=" +ByteFormatter.nicePrint(hash, true) );
    
  		if ( scrapeURL == null ){
  			if (Logger.isEnabled()) {
					Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
						"TrackerStatus: scrape cancelled.. url null"));
  			}
      
  			return;
  		}
    
  		try {
  			ArrayList responsesToUpdate = new ArrayList();

  			TRTrackerScraperResponseImpl response;
    
  			try{
  				hashes_mon.enter();
   		
	    		response = (TRTrackerScraperResponseImpl)hashes.get( hash );
		    
	    	}finally{
	    	
	    		hashes_mon.exit();
	    	}
	
    		if ( response == null ){
    			
    			response = addHash(hash);
    		}
    		
	    	long lMainNextScrapeStartTime = response.getNextScrapeStartTime();
	
	    	if( !force && lMainNextScrapeStartTime > SystemTime.getCurrentTime() ) {
	  			if (Logger.isEnabled()) {
						Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
							"TrackerStatus: scrape cancelled.. not forced and still "
									+ (lMainNextScrapeStartTime - SystemTime.getCurrentTime())
									+ "ms"));
	  			}
	    		return;
	    	}
    
	    		// Set status id to SCRAPING, but leave status string until we actually
	    		// do the scrape
	    	
	    	response.setStatus(TRTrackerScraperResponse.ST_SCRAPING,
					MessageText.getString(SS + "scraping.queued"));
  			if (Logger.isEnabled()) {
					Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
						"TrackerStatus: setting to scraping"));
  			}

	    	responsesToUpdate.add(response);
	    
	    		// Go through hashes and pick out other scrapes that are "close to" wanting a new scrape.
	    
		    if (!bSingleHashScrapes){
		    	
		    	try{
		    	  hashes_mon.enter();
		    		
			      Iterator iterHashes = hashes.values().iterator();
			      
			      	// if we hit trackers with excessive scrapes they respond in varying fashions - from no reply
			      	// to returning 414 to whatever. Rather than hit trackers with large payloads that they then
			      	// reject we limit to MULTI_SCRAPE_LIMIT in one go
			      
			      while( iterHashes.hasNext() && responsesToUpdate.size() < GROUP_SCRAPES_LIMIT ){
			      	
			        TRTrackerScraperResponseImpl r = (TRTrackerScraperResponseImpl)iterHashes.next();
			        
			        if ( !r.getHash().equals( hash )) {
			        	
			          long lTimeDiff = Math.abs(lMainNextScrapeStartTime - r.getNextScrapeStartTime());
			          
			          if (lTimeDiff <= GROUP_SCRAPES_MS && r.getStatus() != TRTrackerScraperResponse.ST_SCRAPING) {
			          	
			    	    	r.setStatus(TRTrackerScraperResponse.ST_SCRAPING,
			    						MessageText.getString(SS + "scraping.queued"));
			      			if (Logger.isEnabled()) {
			    					Logger.log(new LogEvent(TorrentUtils.getDownloadManager(r.getHash()), LOGID,
			    						"TrackerStatus: setting to scraping via group scrape"));
			      			}
			            
			            responsesToUpdate.add(r);
			          }
			        }
			      }
		      }finally{
		      	
		      	hashes_mon.exit();
		      }
			}
		    
		    runScrapes(responsesToUpdate,  force, async);
		    
  		}catch( Throwable t ) {
      
  			Debug.out( "updateSingleHash() exception", t );
  		}
  	}
	
  	protected void
  	runScrapes(
  		final ArrayList 	responses, 
  	    final boolean 		force, 
  	    boolean 			async) 
  	{
  		numActiveScrapes++;
  		if ( async ){
  			
  			thread_pool.run( 
  				new AERunnable()
  				{
  					public void
  					runSupport()
  					{
  						runScrapesSupport( responses, force );
  					}
  				});
  			
  			if (Logger.isEnabled()) {
  				Logger.log(new LogEvent(LOGID, "TrackerStatus: queuing '" + scrapeURL
  						+ "', for " + responses.size() + " of " + hashes.size() + " hashes"
  						+ ", single_hash_scrapes: " + (bSingleHashScrapes ? "Y" : "N")
  						+ ", queue size=" + thread_pool.getQueueSize()));
  			}
  		}else{
  		
  			runScrapesSupport( responses, force );
  		}
  	}
  	
 
  	protected void 
    runScrapesSupport(
    	ArrayList 	responses, 
    	boolean 	force ) 
    {
		try {
			if (Logger.isEnabled()) {
				Logger.log(new LogEvent(LOGID, "TrackerStatus: scraping '" + scrapeURL
						+ "', for " + responses.size() + " of " + hashes.size() + " hashes"
						+ ", single_hash_scrapes: " + (bSingleHashScrapes ? "Y" : "N")));
			}

			boolean original_bSingleHashScrapes = bSingleHashScrapes;

			boolean disable_all_scrapes = !COConfigurationManager
					.getBooleanParameter("Tracker Client Scrape Enable");
			boolean disable_stopped_scrapes = !COConfigurationManager
					.getBooleanParameter("Tracker Client Scrape Stopped Enable");

			byte[]	scrape_reply = null;
			
			try {
				// if URL already includes a query component then just append our
				// params

				HashWrapper one_of_the_hashes = null;
				TRTrackerScraperResponseImpl one_of_the_responses = null;

				char first_separator = scrapeURL.indexOf('?') == -1 ? '?' : '&';

				String info_hash = "";

				String flags = "";
				
				List hashesForUDP = new ArrayList();
				
				for (int i = 0; i < responses.size(); i++) {
					TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses.get(i);

					HashWrapper hash = response.getHash();

					if (Logger.isEnabled())
						Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
								"TrackerStatus: scraping, single_hash_scrapes = "
										+ bSingleHashScrapes));

					if (!scraper.isNetworkEnabled(hash, tracker_url)) {

						response.setNextScrapeStartTime(SystemTime.getCurrentTime()
								+ FAULTY_SCRAPE_RETRY_INTERVAL);

						response.setStatus(TRTrackerScraperResponse.ST_ERROR, MessageText
								.getString(SS + "networkdisabled"));

						scraper.scrapeReceived(response);

					} else if ( !force && ( 
								disable_all_scrapes ||
								(disable_stopped_scrapes && !scraper.isTorrentRunning(hash)))){

						response.setNextScrapeStartTime(SystemTime.getCurrentTime()
								+ FAULTY_SCRAPE_RETRY_INTERVAL);

						response.setStatus(TRTrackerScraperResponse.ST_ERROR, MessageText
								.getString(SS + "disabled"));

						scraper.scrapeReceived(response);

					} else {

						response.setStatus(TRTrackerScraperResponse.ST_SCRAPING,
								MessageText.getString(SS + "scraping"));

						// technically haven't recieved a scrape yet, but we need
						// to notify listeners (the ones that display status)
						scraper.scrapeReceived(response);

						// the client-id stuff RELIES on info_hash being the FIRST
						// parameter added by
						// us to the URL, so don't change it!

						info_hash += ((one_of_the_hashes != null) ? '&' : first_separator)
								+ "info_hash=";

						info_hash += URLEncoder.encode(
								new String(hash.getBytes(), Constants.BYTE_ENCODING),
								Constants.BYTE_ENCODING).replaceAll("\\+", "%20");

						Object[]	extensions = scraper.getExtensions(hash);
						
						if ( extensions != null ){
							
							if ( extensions[0] != null ){
								
								info_hash += (String)extensions[0]; 
							}
							
							flags += (Character)extensions[1];
							
						}else{
							
							flags += TRTrackerScraperClientResolver.FL_NONE;
						}
						
						one_of_the_responses = response;
						one_of_the_hashes = hash;
						
						// 28 + 16 + 70*20 -> IPv4/udp packet size of 1444 , that should go through most lines unfragmented
						if(hashesForUDP.size() < 70)
							hashesForUDP.add(hash);
					}
				} // for responses

				if (one_of_the_hashes == null)
					return;

				// set context in case authentication dialog is required
				TorrentUtils.setTLSTorrentHash(one_of_the_hashes);

				String	request = scrapeURL + info_hash;
				
				if ( az_tracker ){
					
					String	port_details = TRTrackerUtils.getPortsForURL();
					
					request += port_details;
					
					request += "&azsf=" + flags + "&azver=" + TRTrackerAnnouncer.AZ_TRACKER_VERSION_CURRENT;
				}
				
				URL reqUrl = new URL( request );

				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID,
							"Accessing scrape interface using url : " + reqUrl));

				ByteArrayOutputStream message = new ByteArrayOutputStream();

				long scrapeStartTime = SystemTime.getCurrentTime();

				URL	redirect_url = null;
				
				String protocol = reqUrl.getProtocol();
				
		  		URL udpScrapeURL = null;
		  		
		  		if(protocol.equalsIgnoreCase("udp"))
		  			udpScrapeURL = reqUrl;
		  		else if(protocol.equalsIgnoreCase("http") && !az_tracker && scrapeCount % autoUDPscrapeEvery == 0)
		  			udpScrapeURL = new URL(reqUrl.toString().replaceFirst("^http", "udp"));
		  			
		  		
		  		if ( udpScrapeURL != null){
		  			
		  			boolean success = scrapeUDP( reqUrl, message, hashesForUDP );
		  			
		  			if((!success || message.size() == 0) && !protocol.equalsIgnoreCase("udp"))
		  			{ // automatic UDP probe failed, use HTTP again
		  				udpScrapeURL = null;
		  				message.reset();
		  				if(autoUDPscrapeEvery < 16)
		  					autoUDPscrapeEvery <<= 1;
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_INFORMATION, "redirection of http scrape ["+scrapeURL+"] to udp failed, will retry in "+autoUDPscrapeEvery+" scrapes"));
		  			} else if(success && !protocol.equalsIgnoreCase("udp"))
		  			{
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_INFORMATION, "redirection of http scrape ["+scrapeURL+"] to udp successful"));
		  				autoUDPscrapeEvery = 1;
		  			}
		  				
		  		}
		  		
		  		scrapeCount++;
		  		
		  		if(udpScrapeURL == null)
		  			redirect_url = scrapeHTTP(reqUrl, message);
				
				scrape_reply = message.toByteArray();
				
				Map map = BDecoder.decode( scrape_reply );
								
				boolean	this_is_az_tracker = map.get( "aztracker" ) != null;
				
				if ( az_tracker != this_is_az_tracker ){
						
					az_tracker	= this_is_az_tracker;
					
					TRTrackerUtils.setAZTracker( tracker_url, az_tracker );
				}
				
				Map mapFiles = map == null ? null : (Map) map.get("files");

				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Response from scrape interface "
							+ scrapeURL + ": "
							+ ((mapFiles == null) ? "null" : "" + mapFiles.size())
							+ " returned"));

				int iMinRequestInterval = 0;
				if (map != null) {
					/* "The spec":
					 * files
					 *   infohash
					 *   complete
					 *   incomplete
					 *   downloaded
					 *   name
					 *  flags
					 *    min_request_interval
					 *  failure reason
					 */
					/*
					 * files infohash complete incomplete downloaded name flags
					 * min_request_interval
					 */
					Map mapFlags = (Map) map.get("flags");
					if (mapFlags != null) {
						Long longScrapeValue = (Long) mapFlags
								.get("min_request_interval");
						if (longScrapeValue != null)
							iMinRequestInterval = longScrapeValue.intValue();
						// Tracker owners want this log entry
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID,
								"Received min_request_interval of " + iMinRequestInterval));
					}
				}

				if (mapFiles == null || mapFiles.size() == 0) {

					// azureus extension here to handle "failure reason" returned for
					// scrapes

					byte[] failure_reason_bytes = map == null ? null : (byte[]) map
							.get("failure reason");

					if (failure_reason_bytes != null) {
						long nextScrapeTime = SystemTime.getCurrentTime()
								+ ((iMinRequestInterval == 0) ? FAULTY_SCRAPE_RETRY_INTERVAL
										: iMinRequestInterval * 1000);

						for (int i = 0; i < responses.size(); i++) {

							TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
									.get(i);

							response.setNextScrapeStartTime(nextScrapeTime);

							response.setStatus(TRTrackerScraperResponse.ST_ERROR,
									MessageText.getString(SS + "error")
											+ new String(failure_reason_bytes,
													Constants.DEFAULT_ENCODING));

							// notifiy listeners

							scraper.scrapeReceived(response);
						}

					} else {
						if (responses.size() > 1) {
							// multi were requested, 0 returned. Therefore, multi not
							// supported
							bSingleHashScrapes = true;
							if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, scrapeURL
										+ " doesn't properly support " + "multi-hash scrapes"));

							for (int i = 0; i < responses.size(); i++) {
								TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
										.get(i);

								response.setStatus(TRTrackerScraperResponse.ST_ERROR,
										MessageText.getString(SS + "error")
												+ MessageText.getString(SSErr + "invalid"));
								// notifiy listeners
								scraper.scrapeReceived(response);
							}
						} else {
							long nextScrapeTime = SystemTime.getCurrentTime()
									+ ((iMinRequestInterval == 0) ? NOHASH_RETRY_INTERVAL
											: iMinRequestInterval * 1000);
							// 1 was requested, 0 returned. Therefore, hash not found.
							TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
									.get(0);
							response.setNextScrapeStartTime(nextScrapeTime);
							response.setStatus(TRTrackerScraperResponse.ST_ERROR,
									MessageText.getString(SS + "error")
											+ MessageText.getString(SSErr + "nohash"));
							// notifiy listeners
							scraper.scrapeReceived(response);
						}
					}

					return;
				}

				/*
				 * If we requested mutliple hashes, but only one was returned, revert
				 * to Single Hash Scrapes, but continue on to process the one has that
				 * was returned (it may be a random one from the list)
				 */
				if (!bSingleHashScrapes && responses.size() > 1
						&& mapFiles.size() == 1) {
					bSingleHashScrapes = true;
					if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING, scrapeURL
								+ " only returned " + mapFiles.size()
								+ " hash scrape(s), but we asked for " + responses.size()));
				}

				for (int i = 0; i < responses.size(); i++) {
					TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
							.get(i);

					// LGLogger.log( "decoding response #" +i+ ": " +
					// ByteFormatter.nicePrint( response.getHash(), true ) );

					// retrieve the scrape data for the relevent infohash
					Map scrapeMap = (Map) mapFiles.get(new String(response.getHash().getBytes(),
							Constants.BYTE_ENCODING));

					if (scrapeMap == null) {
						// some trackers that return only 1 hash return a random one!
						if (responses.size() == 1 || mapFiles.size() != 1) {

							response.setNextScrapeStartTime(SystemTime.getCurrentTime()
									+ NOHASH_RETRY_INTERVAL);

							response.setStatus(TRTrackerScraperResponse.ST_ERROR,
									MessageText.getString(SS + "error")
											+ MessageText.getString(SSErr + "nohash"));
							// notifiy listeners
							scraper.scrapeReceived(response);
						} else {
							// This tracker doesn't support multiple hash requests.
							// revert status to what it was

							response.revertStatus();

							if (response.getStatus() == TRTrackerScraperResponse.ST_SCRAPING) {

								// System.out.println("Hash " +
								// ByteFormatter.nicePrint(response.getHash(), true) + "
								// mysteriously reverted to ST_SCRAPING!");

								// response.setStatus(TRTrackerScraperResponse.ST_ONLINE, "");

								response.setNextScrapeStartTime(SystemTime.getCurrentTime()
										+ FAULTY_SCRAPE_RETRY_INTERVAL);

								response.setStatus(TRTrackerScraperResponse.ST_ERROR,
										MessageText.getString(SS + "error")
												+ MessageText.getString(SSErr + "invalid"));

							} else {

								// force single-hash scrapes here

								bSingleHashScrapes = true;

								// only leave the next retry time if this is the first single
								// hash fail

								if (original_bSingleHashScrapes) {

									response.setNextScrapeStartTime(SystemTime.getCurrentTime()
											+ FAULTY_SCRAPE_RETRY_INTERVAL);
								}

							}
							// notifiy listeners
							scraper.scrapeReceived(response);

							// if this was the first scrape request in the list,
							// TrackerChecker
							// will attempt to scrape again because we didn't reset the
							// nextscrapestarttime. But the next time, bSingleHashScrapes
							// will be true, and only 1 has will be requested, so there
							// will not be infinite looping
						}
						// System.out.println("scrape: hash missing from reply");
					} else {
						// retrieve values
						int seeds = ((Long) scrapeMap.get("complete")).intValue();
						int peers = ((Long) scrapeMap.get("incomplete")).intValue();
						Long comp = (Long) scrapeMap.get("downloaded");
						int completed = comp == null ? -1 : comp.intValue();

						// make sure we dont use invalid replies
						if (seeds < 0 || peers < 0 || completed < -1) {
							if (Logger.isEnabled()) {
								HashWrapper hash = response.getHash();
								Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash),
										LOGID, "Invalid scrape response from '" + reqUrl
												+ "': map = " + scrapeMap));
							}

							// We requested multiple hashes, but tracker didn't support
							// multiple hashes and returned 1 hash. However, that hash is
							// invalid because seeds or peers was < 0. So, exit. Scrape
							// manager will run scrapes for each individual hash.
							if (responses.size() > 1 && bSingleHashScrapes) {

								response.setStatus(TRTrackerScraperResponse.ST_ERROR,
										MessageText.getString(SS + "error")
												+ MessageText.getString(SSErr + "invalid"));

								scraper.scrapeReceived(response);

								continue;
							}

							response.setNextScrapeStartTime(SystemTime.getCurrentTime()
									+ FAULTY_SCRAPE_RETRY_INTERVAL);
							response.setStatus(TRTrackerScraperResponse.ST_ERROR,
									MessageText.getString(SS + "error")
											+ MessageText.getString(SSErr + "invalid")
											+ " "
											+ (seeds < 0 ? MessageText
													.getString("MyTorrentsView.seeds")
													+ " == " + seeds + ". " : "")
											+ (peers < 0 ? MessageText
													.getString("MyTorrentsView.peers")
													+ " == " + peers + ". " : "")
											+ (completed < 0 ? MessageText
													.getString("MyTorrentsView.completed")
													+ " == " + completed + ". " : ""));

							scraper.scrapeReceived(response);

							continue;
						}

						int scrapeInterval = TRTrackerScraperResponseImpl
								.calcScrapeIntervalSecs(iMinRequestInterval, seeds);

						long nextScrapeTime = SystemTime.getCurrentTime()
								+ (scrapeInterval * 1000);
						response.setNextScrapeStartTime(nextScrapeTime);

						// create the response
						response.setScrapeStartTime(scrapeStartTime);
						response.setSeeds(seeds);
						response.setPeers(peers);
						response.setCompleted(completed);
						response.setStatus(TRTrackerScraperResponse.ST_ONLINE,
								MessageText.getString(SS + "ok"));

						// notifiy listeners
						scraper.scrapeReceived(response);
						
						try{
							if ( responses.size() == 1 && redirect_url != null ){
								
									// we only deal with redirects for single urls - if the tracker wants to
									// redirect one of a group is has to force single-hash scrapes anyway
								
								String	redirect_str = redirect_url.toString();
								
								int s_pos =  redirect_str.indexOf( "/scrape" );
								
								if ( s_pos != -1 ){
									
									URL	new_url = new URL( redirect_str.substring(0,s_pos) +
													"/announce" + redirect_str.substring(s_pos+7));
									
									if ( scraper.redirectTrackerUrl( response.getHash(), tracker_url, new_url )){
										
										removeHash( response.getHash());
									}
								}
							}
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				} // for responses

			} catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
				for (int i = 0; i < responses.size(); i++) {
					TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
							.get(i);
					response.setNextScrapeStartTime(SystemTime.getCurrentTime()
							+ FAULTY_SCRAPE_RETRY_INTERVAL);
					response.setStatus(TRTrackerScraperResponse.ST_ERROR, MessageText
							.getString(SS + "error")
							+ ignoreSSL.getMessage());
					// notifiy listeners
					scraper.scrapeReceived(response);
				}
			} catch (FileNotFoundException e) {
				for (int i = 0; i < responses.size(); i++) {
					TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
							.get(i);
					response.setNextScrapeStartTime(SystemTime.getCurrentTime()
							+ FAULTY_SCRAPE_RETRY_INTERVAL);
					response.setStatus(TRTrackerScraperResponse.ST_ERROR, MessageText
							.getString(SS + "error")
							+ MessageText.getString("DownloadManager.error.filenotfound"));
					// notifiy listeners
					scraper.scrapeReceived(response);
				}
			} catch (SocketException e) {
				setAllError(e);
			} catch (SocketTimeoutException e) {
				setAllError(e);
			} catch (UnknownHostException e) {
				setAllError(e);
			} catch (BEncodingException e) {
				setAllError(e);
			} catch (Exception e) {
				
				// for apache we can get error 414 - URL too long. simplest solution
				// for this
				// is to fall back to single scraping

				String error_message = e.getMessage();
				
				if (error_message != null) {
					if (error_message.indexOf(" 500 ") >= 0 
							|| error_message.indexOf(" 400 ") >= 0 
							|| error_message.indexOf(" 403 ") >= 0
							|| error_message.indexOf(" 404 ") >= 0
							|| error_message.indexOf(" 501 ") >= 0) {
						// various errors that have a 99% chance of happening on
						// any other scrape request
						setAllError(e);
						return;
					}

  				if (error_message.indexOf("414") != -1
  						&& !bSingleHashScrapes) {
  					bSingleHashScrapes = true;
  					// Skip the setuing up the response.  We want to scrape again
  					return;
  				}
				}

				String msg = Debug.getNestedExceptionMessage(e);

				if ( scrape_reply != null ){

	 				String	trace_data;
	 				
	 				if ( scrape_reply.length <= 150 ){
	 					
	 					trace_data = new String(scrape_reply);
	 					
	 				}else{
	 					
	 					trace_data = new String(scrape_reply,0,150) + "...";
	 				}
	 				
	 				msg += " [" + trace_data + "]";
				}
				
				for (int i = 0; i < responses.size(); i++) {
					TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) responses
							.get(i);

					if (Logger.isEnabled()) {
						HashWrapper hash = response.getHash();
						Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
								LogEvent.LT_ERROR, "Error from scrape interface " + scrapeURL
										+ " : " + msg + " (" + e.getClass() + ")"));
					}

					response.setNextScrapeStartTime(SystemTime.getCurrentTime()
							+ FAULTY_SCRAPE_RETRY_INTERVAL);
					response.setStatus(TRTrackerScraperResponse.ST_ERROR, MessageText
							.getString(SS + "error")
							+ msg);
					// notifiy listeners
					scraper.scrapeReceived(response);
				}
			}

		} catch (Throwable t) {
			Debug.out("runScrapesSupport failed", t);
		} finally {
			numActiveScrapes--;
		}
	}

  /**
	 * @param e
	 */
	private void setAllError(Exception e) {
		// Error will apply to ALL hashes, so set all
		Object[] values;
		try {
			values = hashes.values().toArray();
			hashes_mon.enter();
		} finally {
			hashes_mon.exit();
		}

		String msg = e.getLocalizedMessage();
		
		if(e instanceof BEncodingException)
			if(msg.indexOf("html") != -1)
				msg = "could not decode response, appears to be a website instead of tracker scrape: "+msg.replace('\n', ' ');
			else msg = "bencoded response malformed:"+msg;

		for (int i = 0; i < values.length; i++) {
			TRTrackerScraperResponseImpl response = (TRTrackerScraperResponseImpl) values[i];

			if (Logger.isEnabled()) {
				HashWrapper hash = response.getHash();
				Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID,
						LogEvent.LT_WARNING, "Error from scrape interface " + scrapeURL
								+ " : " + msg));
				//e.printStackTrace();
			}

			response.setNextScrapeStartTime(SystemTime.getCurrentTime()
					+ FAULTY_SCRAPE_RETRY_INTERVAL);
			response.setStatus(TRTrackerScraperResponse.ST_ERROR,StringInterner.intern(
					MessageText.getString(SS + "error") + msg + " (IO)"));
			// notifiy listeners
			scraper.scrapeReceived(response);
		}
	}


	protected URL 
  scrapeHTTP(
  	URL 					reqUrl, 
	ByteArrayOutputStream 	message )
  
  	throws IOException
  {
	URL	redirect_url = null;
	
  	TRTrackerUtils.checkForBlacklistedURLs( reqUrl );
  	
    reqUrl = TRTrackerUtils.adjustURLForHosting( reqUrl );

    reqUrl = AddressUtils.adjustURL( reqUrl );
    
  	// System.out.println( "scraping " + reqUrl.toString());
  	
	Properties	http_properties = new Properties();
		
	http_properties.put( ClientIDGenerator.PR_URL, reqUrl );
		
	try{
		ClientIDManagerImpl.getSingleton().generateHTTPProperties( http_properties );
		
	}catch( ClientIDException e ){
		
		throw( new IOException( e.getMessage()));
	}
	
	reqUrl = (URL)http_properties.get( ClientIDGenerator.PR_URL );

  	InputStream is = null;
  	
  	try{
	  	HttpURLConnection con = null;

	  	if ( reqUrl.getProtocol().equalsIgnoreCase("https")){
	  		
	  			// see ConfigurationChecker for SSL client defaults
	  		
	  		HttpsURLConnection ssl_con = (HttpsURLConnection)reqUrl.openConnection();
	  		
	  			// allow for certs that contain IP addresses rather than dns names
	  		
	  		ssl_con.setHostnameVerifier(
	  				new HostnameVerifier() {
	  					public boolean verify(String host, SSLSession session) {
	  						return( true );
	  					}
	  				});
	  		
	  		con = ssl_con;
	  		
	  	} else {
	  		con = (HttpURLConnection) reqUrl.openConnection();
	  	}

		String	user_agent = (String)http_properties.get( ClientIDGenerator.PR_USER_AGENT );
 		
 		if ( user_agent != null ){
 			
 			con.setRequestProperty("User-Agent", user_agent );
 		}
 		
 			// some trackers support gzip encoding of replies
 		
	    con.addRequestProperty("Accept-Encoding","gzip");
	    
	    con.setRequestProperty("Connection", "close" );
	    
	  	con.connect();

	  	is = con.getInputStream();
	
		String	resulting_url_str = con.getURL().toString();
			
		if ( !reqUrl.toString().equals( resulting_url_str )){
			
				// some kind of redirect has occurred. Unfortunately we can't get at the underlying
				// redirection reason (temp, perm etc) so we support the use of an explicit indicator
				// in the resulting url
			
			String	marker = "permredirect=1";
			
			int	pos = resulting_url_str.indexOf( marker );
		
			if ( pos != -1 ){
				
				pos = pos-1;	// include the '&' or '?'
				
				try{
					redirect_url = 
						new URL( resulting_url_str.substring(0,pos));
								
				}catch( Throwable e ){
					Debug.printStackTrace(e);
				}
			}
		}
		
	  	String encoding = con.getHeaderField( "content-encoding");
	  	
	  	boolean	gzip = encoding != null && encoding.equalsIgnoreCase("gzip");
	  	
	  	// System.out.println( "encoding = " + encoding );
	  	
	  	if ( gzip ){
	  		
	  		is = new GZIPInputStream( is );
	  	}
	  	
	  	byte[]	data = new byte[1024];
	  	
	  	int num_read = 0;
	  	
	  	while( true ){
	  		
	  		try {
				int	len = is.read(data);
					
				if ( len > 0 ){
					
					message.write(data, 0, len);
					
					num_read += len;
					
					if ( num_read > 128*1024 ){
						
							// someone's sending us junk, bail out
					   
						message.reset();
						
						throw( new Exception( "Tracker response invalid (too large)" ));
						
					}
				}else if ( len == 0 ){
					
					Thread.sleep(20);
					
				}else{
					
					break;
				}
	  		} catch (Exception e) {
	  			
	  			if (Logger.isEnabled())
						Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
								"Error from scrape interface " + scrapeURL + " : "
										+ Debug.getNestedExceptionMessage(e)));

	  			return( null );
	  		}
	  	}
	  } finally {
	  	if (is != null) {
        try {
	  		  is.close();
  	  	} catch (IOException e1) { }
  	  }
	  }
	  
	  return( redirect_url );
  }
  
  protected boolean scrapeUDP(URL reqUrl, ByteArrayOutputStream message, List hashes) throws Exception {
		Map rootMap = new HashMap();
		Map files = new ByteEncodedKeyHashMap();
		rootMap.put("files", files);
		
		
		/*
		 * reduce network traffic by only scraping UDP when the torrent isn't
		 * running as UDP version 2 contains scrape data in the announce
		 * response
		 */
		
		/* removed implementation for the time being
		 
		for (Iterator it = hashes.iterator(); it.hasNext();)
		{
			HashWrapper hash = (HashWrapper) it.next();
			if (PRUDPPacketTracker.VERSION == 2 && scraper.isTorrentDownloading(hash))
			{
				if (Logger.isEnabled())
					Logger.log(new LogEvent(TorrentUtils.getDownloadManager(hash), LOGID, LogEvent.LT_WARNING, "Scrape of " + reqUrl + " skipped as torrent running and " + "therefore scrape data available in " + "announce replies"));
				// easiest approach here is to brew up a response that looks like the current one
				Map file = new HashMap();
				byte[] resp_hash = hash.getBytes();
				// System.out.println("got hash:" + ByteFormatter.nicePrint( resp_hash, true ));
				files.put(new String(resp_hash, Constants.BYTE_ENCODING), file);
				file.put("complete", new Long(current_response.getSeeds()));
				file.put("downloaded", new Long(-1)); // unknown
				file.put("incomplete", new Long(current_response.getPeers()));
				byte[] data = BEncoder.encode(rootMap);
				message.write(data);
				return true;
			}
		}
		
		*/
  	
  	
	reqUrl = TRTrackerUtils.adjustURLForHosting( reqUrl );

	PasswordAuthentication	auth 	= null;
	boolean					auth_ok	= false;
	
	try{
		if ( reqUrl.getQuery().toLowerCase().indexOf("auth") != -1 ){
					
			auth = SESecurityManager.getPasswordAuthentication( "UDP Tracker", reqUrl );
		}		
	
		int port = UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();
		
		PRUDPPacketHandler handler = PRUDPPacketHandlerFactory.getHandler( port );
		
		InetSocketAddress destination = new InetSocketAddress(reqUrl.getHost(),reqUrl.getPort()==-1?80:reqUrl.getPort());
		
		String	failure_reason = null;
		
		for (int retry_loop=0;retry_loop<PRUDPPacketTracker.DEFAULT_RETRY_COUNT;retry_loop++){
		
			try{
				PRUDPPacket connect_request = new PRUDPPacketRequestConnect();
				
				PRUDPPacket reply = handler.sendAndReceive( auth, connect_request, destination );
				
				if ( reply.getAction() == PRUDPPacketTracker.ACT_REPLY_CONNECT ){
					
					PRUDPPacketReplyConnect connect_reply = (PRUDPPacketReplyConnect)reply;
					
					long	my_connection = connect_reply.getConnectionId();
					
					PRUDPPacketRequestScrape scrape_request = new PRUDPPacketRequestScrape( my_connection, hashes );
									
					reply = handler.sendAndReceive( auth, scrape_request, destination );
					
					if ( reply.getAction() == PRUDPPacketTracker.ACT_REPLY_SCRAPE ){
	
						auth_ok	= true;
	
						if ( PRUDPPacketTracker.VERSION == 1 ){
							PRUDPPacketReplyScrape	scrape_reply = (PRUDPPacketReplyScrape)reply;
							
							/*
							int	interval = scrape_reply.getInterval();
							
							if ( interval != 0 ){
								
								map.put( "interval", new Long(interval ));
							}
							*/
							
							byte[][]	reply_hashes 	= scrape_reply.getHashes();
							int[]		complete 		= scrape_reply.getComplete();
							int[]		downloaded 		= scrape_reply.getDownloaded();
							int[]		incomplete 		= scrape_reply.getIncomplete();
							
					
							for (int i=0;i<reply_hashes.length;i++){
								
								Map	file = new HashMap();
								
								byte[]	resp_hash = reply_hashes[i];
								
								// System.out.println("got hash:" + ByteFormatter.nicePrint( resp_hash, true ));
							
								files.put( new String(resp_hash, Constants.BYTE_ENCODING), file );
								
								file.put( "complete", new Long(complete[i]));
								file.put( "downloaded", new Long(downloaded[i]));
								file.put( "incomplete", new Long(incomplete[i]));
							}
							
							byte[] data = BEncoder.encode( rootMap );
							
							message.write( data );
							
							return true;
						}else{
							PRUDPPacketReplyScrape2	scrape_reply = (PRUDPPacketReplyScrape2)reply;
							
							
							/*
							int	interval = scrape_reply.getInterval();
							
							if ( interval != 0 ){
								
								map.put( "interval", new Long(interval ));
							}
							*/
							
							int[]		complete 	= scrape_reply.getComplete();
							int[]		downloaded 	= scrape_reply.getDownloaded();
							int[]		incomplete 	= scrape_reply.getIncomplete();
							
							int i=0;
							for(Iterator it = hashes.iterator();it.hasNext() && i < complete.length;i++)
							{
								HashWrapper hash = (HashWrapper)it.next();
								Map file = new HashMap();
								file.put( "complete", new Long(complete[i]));
								file.put( "downloaded", new Long(downloaded[i]));
								file.put( "incomplete", new Long(incomplete[i]));
								files.put( new String(hash.getBytes(), Constants.BYTE_ENCODING), file );
							}
							
							// System.out.println("got hash:" + ByteFormatter.nicePrint( resp_hash, true ));
							
							byte[] data = BEncoder.encode( rootMap );
							
							message.write( data );
							
							return true;
						}
					}else{
						
						failure_reason = ((PRUDPPacketReplyError)reply).getMessage();
						
						if (Logger.isEnabled())
								Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
										"Response from scrape interface "+ reqUrl +" : " + failure_reason));
						
						break;
					}
				}else{
	
					failure_reason = ((PRUDPPacketReplyError)reply).getMessage();
					
					if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR, "Response from scrape interface "+ reqUrl +" : "
											+ ((PRUDPPacketReplyError) reply).getMessage()));
				
					break;
				}
	
			}catch( PRUDPPacketHandlerException e ){
				
				if ( e.getMessage() == null || e.getMessage().indexOf("timed out") == -1 ){
					
					throw( e );
				}
				
				failure_reason	= "Timeout";
			}
		}
		
		if ( failure_reason != null ){
			
			rootMap.put( "failure reason", failure_reason.getBytes());
			rootMap.remove("files");
			
			byte[] data = BEncoder.encode( rootMap );
			message.write( data );
		}
		
		return false;
	}finally{
		if ( auth != null ){
			
			SESecurityManager.setPasswordAuthenticationOutcome( TRTrackerBTAnnouncerImpl.UDP_REALM, reqUrl, auth_ok );
		}
	}
  }
  
  protected String
  getURLParam(
  		String		url,
		String		param )
  {
  	int	p1 = url.indexOf( param + "=" );
  	
  	if ( p1 == -1 ){
  		
  		return( null );
  	}
  	
  	int	p2 = url.indexOf( "&", p1 );
  	
  	if ( p2 == -1 ){
  		
  		return( url.substring(p1+param.length()+1));
  	}
  	
  	return( url.substring(p1+param.length()+1,p2));
  }
  

  protected TRTrackerScraperResponseImpl addHash(HashWrapper hash) {
 
	TRTrackerScraperResponseImpl response;
	  
  	try{
  		hashes_mon.enter();
  	
  		response = (TRTrackerScraperResponseImpl)hashes.get( hash );
  		
  		if ( response == null ){
  			
	  	    response = new TRTrackerBTScraperResponseImpl(this, hash);
	  	    
	  	    if ( scrapeURL == null ){
	  	    	
	  	      response.setStatus(TRTrackerScraperResponse.ST_ERROR,
	  	                         MessageText.getString(SS + "error") + 
	  	                         MessageText.getString(SSErr + "badURL"));
	  	    }else{
	  	    	
	  	      response.setStatus(TRTrackerScraperResponse.ST_INITIALIZING,
	  	                         MessageText.getString(SS + "initializing"));
	  	    }
	  	    
	  	    response.setNextScrapeStartTime(checker.getNextScrapeCheckOn());
	  	    
	  		hashes.put( hash, response);
  		}
  	}finally{
  		
  		hashes_mon.exit();
  	}

  		//notifiy listeners
  	
    scraper.scrapeReceived( response );

  	return response;
  }
  
  protected void removeHash(HashWrapper hash) {
  	try{
  		hashes_mon.enter();
  	
  		hashes.remove( hash );
  		
  	}finally{
  		
  		hashes_mon.exit();
  	}
  }
  
  protected URL
  getTrackerURL()
  {
  	return( tracker_url );
  }
  
  protected Map getHashes() {
    return hashes;
  }
  
  protected AEMonitor
  getHashesMonitor()
  {
  	return( hashes_mon );
  }

	protected void scrapeReceived(TRTrackerScraperResponse response) {
	  scraper.scrapeReceived(response);
	}

	public boolean getSupportsMultipeHashScrapes() {
		return !bSingleHashScrapes;
	}
	
	protected String
	getString()
	{	  
	  return( tracker_url + ", " + scrapeURL + ", multi-scrape=" + !bSingleHashScrapes );
	}
	
	public int getNumActiveScrapes() {
		return numActiveScrapes;
	}
}
