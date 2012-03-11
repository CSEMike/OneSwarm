/*
 * Created on 27-Aug-2004
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

package org.gudy.azureus2.core3.tracker.client;

import java.net.URL;

import org.gudy.azureus2.core3.util.HashWrapper;

/**
 * @author parg
 *
 */

public interface 
TRTrackerScraperClientResolver 
{
	public static final int	ST_NOT_FOUND		= 1;
	public static final int	ST_RUNNING			= 2;	// downloading, seeding 
	public static final int	ST_QUEUED			= 3;	
	public static final int	ST_OTHER			= 4;

	public static final Character FL_NONE					= new Character( 'n' );
	public static final Character FL_INCOMPLETE_STOPPED		= new Character( 's' );
	public static final Character FL_INCOMPLETE_QUEUED		= new Character( 'q' );
	public static final Character FL_INCOMPLETE_RUNNING		= new Character( 'r' );
	public static final Character FL_COMPLETE_STOPPED		= new Character( 'S' );
	public static final Character FL_COMPLETE_QUEUED		= new Character( 'Q' );
	public static final Character FL_COMPLETE_RUNNING		= new Character( 'R' );
	
		/**
		 * Gives access to a restricted set of states for this torrent from ST_ set
		 * @param torrent_hash
		 * @return
		 */
	
	public int
	getStatus(
		HashWrapper	torrent_hash );
	
		/**
		 * 
		 * @param hash
		 * @return
		 */
	
	public int[]
	getCachedScrape(
		HashWrapper	hash );

	public boolean
	isNetworkEnabled(
		HashWrapper	hash,
		URL			url );
	
		/**
		 * Two kinds of extensions: entry [0] = String (or null) that gets passed with the scrape verbotem after infohash
		 * entry [1] = Character - status of download, aggregated into a single String passed with scrape
		 * status flags are above FL_ values
		 * @param hash
		 * @return
		 */
	
	public Object[]
	getExtensions(
		HashWrapper	hash );
	
	public boolean
	redirectTrackerUrl(
		HashWrapper		hash,
		URL				old_url,
		URL				new_url );
}
