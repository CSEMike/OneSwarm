/*
 * Created on Feb 20, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.tracker.server.impl;

import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.dht.netcoords.DHTNetworkPosition;

public interface 
TRTrackerServerSimplePeer 
{
	public byte[]
	getIPAsRead();
	
	public byte[]
	getIPAddressBytes();

	public HashWrapper
   	getPeerId();

	public int
	getTCPPort();
	
	public int
	getUDPPort();
	
	public int
	getHTTPPort();
	
	public boolean
	isSeed();
	
	public boolean
	isBiased();
	
	public byte
	getCryptoLevel();
	
	public byte
	getAZVer();
	
	public int
	getUpSpeed();
	
	public DHTNetworkPosition
	getNetworkPosition();
}
