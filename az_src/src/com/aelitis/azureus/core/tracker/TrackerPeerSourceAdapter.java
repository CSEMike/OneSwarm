/*
 * Created on Dec 9, 2009
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


package com.aelitis.azureus.core.tracker;

public abstract class 
TrackerPeerSourceAdapter 
	implements TrackerPeerSource
{
	public int
	getType()
	{
		return( TP_UNKNOWN );
	}
	
	public String
	getName()
	{
		return( "" );
	}
	
	public int
	getStatus()
	{
		return( ST_UNKNOWN );
	}
	
	public String
	getStatusString()
	{
		return( null );
	}
	
	public int
	getSeedCount()
	{
		return( -1 );
	}
	
	public int
	getLeecherCount()
	{
		return( -1 );
	}
	
	public int
	getPeers()
	{
		return( -1 );
	}
	
	public int
	getSecondsToUpdate()
	{
		return( -1 );
	}
	
	public int
	getInterval()
	{
		return( -1 );
	}
	
	public int
	getMinInterval()
	{
		return( -1 );
	}
	
	public boolean
	isUpdating()
	{
		return( false );
	}
	
	public boolean
	canManuallyUpdate()
	{
		return( false );
	}
	
	public void
	manualUpdate()
	{
	}
}
