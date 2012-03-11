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
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.client.impl.TRTrackerScraperResponseImpl;
import org.gudy.azureus2.core3.util.HashWrapper;

/**
 * @author parg
 *
 */

public class 
TRTrackerBTScraperResponseImpl
	extends TRTrackerScraperResponseImpl
{
	private TrackerStatus ts;

	private boolean	is_dht_backup;
	  
	protected 
	TRTrackerBTScraperResponseImpl(
		TrackerStatus _ts,
		HashWrapper _hash) 
	{
		this(_ts, _hash, -1, -1, -1,-1);
	}

	protected 
	TRTrackerBTScraperResponseImpl(
		TrackerStatus _ts,
		HashWrapper _hash,
		int  _seeds, 
		int  _peers,
		int completed,
		long _scrapeStartTime)  
	{
		super( _hash, _seeds, _peers, completed, _scrapeStartTime );

		ts	= _ts;
	}

	public TrackerStatus 
	getTrackerStatus() 
	{
		return ts;
	}

	public void 
	setSeedsPeers(
		int iSeeds, int iPeers ) 
	{
		setSeeds( iSeeds );
		setPeers( iPeers );

		if (isValid()){
			setStatus(TRTrackerScraperResponse.ST_ONLINE);
			setStatus( MessageText.getString("Scrape.status.ok"));
		} else {
			setStatus(TRTrackerScraperResponse.ST_INITIALIZING);
		}
		// XXX Is this a good idea?
		ts.scrapeReceived(this);
	}

	public URL
	getURL()
	{
		return( ts.getTrackerURL());
	}

	public void 
	setDHTBackup(
		boolean	is_backup )
	{
		is_dht_backup	= is_backup;
	}
	
	public boolean 
	isDHTBackup() 
	{	
		return is_dht_backup;
	}
}
