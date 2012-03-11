/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.control;


import com.aelitis.azureus.core.dht.DHTLogger;
import com.aelitis.azureus.core.dht.control.impl.DHTControlImpl;
import com.aelitis.azureus.core.dht.transport.DHTTransport;

/**
 * @author parg
 *
 */

public class 
DHTControlFactory 
{
	public static DHTControl
	create(
		DHTControlAdapter	adapter,
		DHTTransport		transport,
		int					K,
		int					B,
		int					max_rep_per_node,
		int					search_concurrency,
		int					lookup_concurrency,
		int					original_republish_interval,
		int					cache_republish_interval,
		int					cache_at_closest_n,
		DHTLogger			logger )
	{
		return( new DHTControlImpl(
						adapter,
						transport, 
						K, B, max_rep_per_node,
						search_concurrency,
						lookup_concurrency,
						original_republish_interval,
						cache_republish_interval,
						cache_at_closest_n,
						logger));
	}
}
