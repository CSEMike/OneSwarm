/*
 * File    : TRTrackerScraperResponse.java
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
 
package org.gudy.azureus2.core3.tracker.client;

import java.net.URL;

import org.gudy.azureus2.core3.util.HashWrapper;


/**
 * @author parg
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface 
TRTrackerScraperResponse 
{
	public static final int	ST_INITIALIZING    = 0;
	public static final int ST_ERROR           = 1;
	public static final int	ST_ONLINE          = 2;
	public static final int	ST_SCRAPING        = 3;
	
	public HashWrapper
	getHash();
	
	public int getCompleted();
	
	public void setCompleted(int completed);
	
	public int
	getSeeds();
	
	public int
	getPeers();
	
	public void 
	setSeedsPeers(int iSeeds, int iPeers);
  
	public int
 	getStatus();

	public long
	getScrapeStartTime();

	public void
	setScrapeStartTime(
			long		time );
  
	public long
	getNextScrapeStartTime();

	public void
	setNextScrapeStartTime(
		long nextScrapeStartTime );

	public String
	getStatusString();

	public boolean isValid();
	
	public URL
	getURL();
	
	public boolean
	isDHTBackup();
	
	public String
	getString();
}
