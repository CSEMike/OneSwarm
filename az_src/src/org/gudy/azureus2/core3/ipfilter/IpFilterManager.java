/*
 * Created on 19-Jul-2004
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

package org.gudy.azureus2.core3.ipfilter;

/**
 * @author parg
 *
 */

public interface 
IpFilterManager 
{
	public IpFilter
	getIPFilter();
	
	public BadIps
	getBadIps();

	/**
	 * @param range
	 * @return
	 */
	byte[] getDescription(Object info);

	/**
	 * @param range
	 * @param description
	 */
	Object addDescription(IpRange range, byte[] description);

	/**
	 * 
	 */
	void cacheAllDescriptions();

	/**
	 * 
	 */
	void clearDescriptionCache();

	/**
	 * 
	 *
	 * @since 3.0.1.5
	 */
	void deleteAllDescriptions();
}
