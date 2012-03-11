/*
 * Created on May 29, 2009
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


package com.aelitis.azureus.core.instancemanager;

import java.net.InetAddress;

import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

public interface 
AZInstanceManagerAdapter 
{
	public String
	getID();
	
	public int[]
	getPorts();
	
	public DHTPlugin
	getDHTPlugin();
	
	public UPnPPlugin
	getUPnPPlugin();
	
	public InetAddress
	getPublicAddress();
	
	public VCPublicAddress
	getVCPublicAddress();
		
	public AZInstanceTracked.TrackTarget
	track(
		byte[]		hash );
	
	public void
	addListener(
		StateListener		listener );
	
	public interface
	StateListener
	{
		public void
		started();
		
		public void
		stopped();
	}
	
	public interface
	VCPublicAddress
	{
		public String
		getAddress();
		
		public long
		getCacheTime();
	}
}
