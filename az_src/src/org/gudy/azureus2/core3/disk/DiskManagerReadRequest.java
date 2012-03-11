/*
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 */
package org.gudy.azureus2.core3.disk;

import org.gudy.azureus2.plugins.peers.PeerReadRequest;

/**
 * 
 * This class represents a Bittorrent Request.
 * and a time stamp to know when it was created.
 * 
 * Request may expire after some time, which is used to determine who is snubbed.
 * 
 * @author Olivier
 *
 * 
 */
public interface 
DiskManagerReadRequest
	extends PeerReadRequest, DiskManagerRequest
{  
	public int getPieceNumber();
 
	public int getOffset();
 
	public int getLength();
	
	public long getTimeCreated(final long now);
	
		/**
		 * If flush is set then data held in memory will be flushed to disk during the read operation
		 * @param flush
		 */
	
	public void
	setFlush(
		boolean	flush );
	
	public boolean
	getFlush();
	
	public void
	setUseCache(
		boolean	cache );
	
	public boolean
	getUseCache();
	
	 /**
	   * We override the equals method
	   * 2 requests are equals if
	   * all their bt fields (piece number, offset, length) are equal
	   */
	public boolean equals(Object o);
	public int	hashCode();
}