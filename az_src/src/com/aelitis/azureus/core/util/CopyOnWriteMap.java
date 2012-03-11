/*
 * Created on 13 May 2008
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Allan Crooks
 *
 */
public class CopyOnWriteMap {
	private volatile Map map;
	
	public CopyOnWriteMap() {
		this.map = new HashMap(0);
	}
	
	public void put(Object key, Object val) {
		synchronized(map) {
			HashMap new_map = new HashMap(map);
			new_map.put(key, val);
			this.map = new_map;
		}
	}
	
	public Object remove(Object key) {
		synchronized(map) {
			HashMap new_map = new HashMap(map);
			Object res = new_map.remove(key);
			this.map = new_map;
			return res;
		}
	}
	
	public Object get(Object key) {
		return this.map.get(key);
	}
	
	public int size() {
		return this.map.size();
	}
	
	public boolean isEmpty() {
		return this.map.isEmpty();
	}
	
	/*
	 * shouldn't return underlying map directly as open to abuse. either wrap in unmodifyable
	 * map or implement desired features explicitly
	public Map getMap() {
		return this.map;
	}
	*/
}
