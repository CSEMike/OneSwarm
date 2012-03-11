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
package org.gudy.azureus2.core3.tracker.client.impl;

/**
 * 
 * @author Olivier
 * @author TuxPaper
 */


import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.StringInterner;

public abstract class 
TRTrackerScraperResponseImpl 
  implements TRTrackerScraperResponse
{
	private HashWrapper hash;
	private int     	seeds;
	private int    		peers;
	private int			completed;
  
  private long scrapeStartTime;
  private long nextScrapeStartTime;
  private String sStatus = "";
  private String sLastStatus = "";
  private int status;
  private int last_status;

  protected 
  TRTrackerScraperResponseImpl(                                     
	  HashWrapper _hash ) 
  {
    this( _hash, -1, -1, -1, -1);
  }

  protected 
  TRTrackerScraperResponseImpl(
	 HashWrapper _hash,
     int  _seeds, 
     int  _peers,
     int  completed,
     long _scrapeStartTime)  
  {
    hash = _hash;
    seeds = _seeds;
    this.completed = completed;
    peers = _peers;

    scrapeStartTime = _scrapeStartTime;
    
    status = (!isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    nextScrapeStartTime = -1;
  }
  

  	public int getCompleted() {
		return completed;
	}
  	
  	public void setCompleted(int completed) {
  		this.completed = completed;
  	}

  public HashWrapper getHash() {
    return hash;
  }
    

  public int getSeeds() {
    return seeds ;
  }
  
  public void
  setSeeds(
  	int		s )
  {
  	seeds	= s;
  }
  
  public int getPeers() {
    return peers;
  }
  
  public void
  setPeers(
  	int	p )
  {
  	peers	= p;
  }


  public int getStatus() {
    return status;
  }
  
  public void
  setStatus(
  	int	s )
  {
  	status	= s;
  }
  
  protected void
  setStatus(
	String	str )
  {
	  sStatus	= str;
  }
  
  public void setStatus(int iNewStatus, String sNewStatus) {
    if (last_status != status && iNewStatus != status)
      last_status = status;
    if (iNewStatus == TRTrackerScraperResponse.ST_ONLINE) {
      status = (!isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    } else {
      status = iNewStatus;
    }
    
    if (sNewStatus == null)
      return;

    if (!sLastStatus.equals(sStatus)) {
      sLastStatus = sStatus;
    }
    sStatus = StringInterner.intern(sNewStatus);
  }
  
  public void revertStatus() {
    status = last_status;
    sStatus = sLastStatus;
  }
  
  public void 
  setScrapeStartTime(long time) 
  {
    scrapeStartTime = time;
  }
    
  
  public long 
  getScrapeStartTime() 
  {
    return scrapeStartTime;
  }

  public long getNextScrapeStartTime() {
    return nextScrapeStartTime;
  }
 
  public void setNextScrapeStartTime(long _nextScrapeStartTime) {
    nextScrapeStartTime = _nextScrapeStartTime;
  }
   
  public String getStatusString() {
    return sStatus;
  }
  
  public boolean isValid() {
    return !(seeds == -1 && peers == -1);
  }
  
  public abstract void
  setDHTBackup(
		boolean	is_backup );
  
  /**
	 * add the same, random value per session so that azureus peers diverge over
	 * time, that should reduce negative swarming behavior for trackers
	 */
  private static final int scrapeFuzzAdd = (int)(Math.random() * 3 * 60); 

  /**
   * Calculate Scrape interval, applying internal min/max limits and default
   * calculations.
   * 
   * @param iRecIntervalSecs Recommended Interval in Seconds, or 0 for no
   *                          recommendation
   * @param iNumSeeds        # of seeds torrent has, used to calculate scrape
   *                          interval
   * @return Calculated interval in Seconds
   */
	public static int calcScrapeIntervalSecs(int iRecIntervalSecs, int iNumSeeds) {
		final int MIN = 15 * 60;
		final int MAX = 3 * 60 * 60;

		// Min 15 min, plus 10 seconds for every seed
		// ex. 10 Seeds = 15m + 100s = ~16.66m
		// 60 seeds = 15m + 600s = ~25m
		// 1000 seeds = 15m + 10000s = ~2h 52m
		int scrapeInterval = MIN + (iNumSeeds * 10);
		if (iRecIntervalSecs > scrapeInterval)
			scrapeInterval = iRecIntervalSecs;
		
		// randomize scrape interval by 3 minutes to 
		scrapeInterval += scrapeFuzzAdd;

		if (scrapeInterval > MAX)
			scrapeInterval = MAX;

		return scrapeInterval;
	}
	
	public String
	getString()
	{
	  return( getURL() + ": " + ByteFormatter.encodeString(hash.getBytes()) +",seeds=" + seeds + ",peers=" + peers +",state="+status+
			  "/"+sStatus+",last="+last_status+"/"+sLastStatus+",start="+scrapeStartTime+",next="+nextScrapeStartTime);
	}
}
