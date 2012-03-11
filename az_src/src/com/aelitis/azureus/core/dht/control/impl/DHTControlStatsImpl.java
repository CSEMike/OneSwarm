/*
 * Created on 31-Jan-2005
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

package com.aelitis.azureus.core.dht.control.impl;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.router.*;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTControlStatsImpl 
	implements DHTTransportFullStats, DHTControlStats
{
	private static final int	UPDATE_INTERVAL	= 10*1000;
	private static final int	UPDATE_PERIOD	= 120;
	
	private DHTControlImpl		control;
	
	
	private Average	packets_in_average 		= Average.getInstance(UPDATE_INTERVAL, UPDATE_PERIOD );
	private Average	packets_out_average 	= Average.getInstance(UPDATE_INTERVAL, UPDATE_PERIOD );
	private Average	bytes_in_average 		= Average.getInstance(UPDATE_INTERVAL, UPDATE_PERIOD );
	private Average	bytes_out_average 		= Average.getInstance(UPDATE_INTERVAL, UPDATE_PERIOD );

	private DHTTransportStats	transport_snapshot;
	private long[]				router_snapshot;
	private int[]				value_details_snapshot;
	
	protected
	DHTControlStatsImpl(
		DHTControlImpl		_control )
	{
		control	= _control;
		
		transport_snapshot	= control.getTransport().getStats().snapshot();
		
		router_snapshot		= control.getRouter().getStats().getStats();
		
		SimpleTimer.addPeriodicEvent(
			"DHTCS:update",
			UPDATE_INTERVAL,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					update();
				
					control.poke();
				}
			});
	}
	
	protected void
	update()
	{
		DHTTransport	transport 	= control.getTransport();
		
		DHTTransportStats	t_stats = transport.getStats().snapshot();
					
		packets_in_average.addValue( 
				t_stats.getPacketsReceived() - transport_snapshot.getPacketsReceived());
			
		packets_out_average.addValue( 
				t_stats.getPacketsSent() - transport_snapshot.getPacketsSent());
			
		bytes_in_average.addValue( 
				t_stats.getBytesReceived() - transport_snapshot.getBytesReceived());
			
		bytes_out_average.addValue( 
				t_stats.getBytesSent() - transport_snapshot.getBytesSent());
		
		transport_snapshot	= t_stats;
		
		router_snapshot	= control.getRouter().getStats().getStats();
		
		value_details_snapshot = null;
	}
	
	public long
	getTotalBytesReceived()
	{
		return( transport_snapshot.getBytesReceived());
	}
	
	public long
	getTotalBytesSent()
	{
		return( transport_snapshot.getBytesSent());
	}
	
	public long
	getTotalPacketsReceived()
	{
		return( transport_snapshot.getPacketsReceived());
	}
	
	public long
	getTotalPacketsSent()
	{
		return( transport_snapshot.getPacketsSent());
		
	}
	
	public long
	getTotalPingsReceived()
	{
		return( transport_snapshot.getPings()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalFindNodesReceived()
	{
		return( transport_snapshot.getFindNodes()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalFindValuesReceived()
	{
		return( transport_snapshot.getFindValues()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalStoresReceived()
	{
		return( transport_snapshot.getStores()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalKeyBlocksReceived()
	{
		return( transport_snapshot.getKeyBlocks()[DHTTransportStats.STAT_RECEIVED]);
	}

		// averages
	
	public long
	getAverageBytesReceived()
	{
		return( bytes_in_average.getAverage());
	}
	
	public long
	getAverageBytesSent()
	{
		return( bytes_out_average.getAverage());	
	}
	
	public long
	getAveragePacketsReceived()
	{
		return( packets_in_average.getAverage());
	}
	
	public long
	getAveragePacketsSent()
	{
		return( packets_out_average.getAverage());
	}
	
	public long
	getIncomingRequests()
	{
		return( transport_snapshot.getIncomingRequests());
	}
		// DB
	
	protected int[]
	getValueDetails()
	{
		int[] vd = value_details_snapshot;
		
		if ( vd == null ){
			
			vd = control.getDataBase().getStats().getValueDetails();
			
			value_details_snapshot = vd;
		}
		
		return( vd );
	}
	
	public long
	getDBValuesStored()
	{
		int[]	vd = getValueDetails();
		
		return( vd[ DHTDBStats.VD_VALUE_COUNT ]);
	}
	
	public long
	getDBKeyCount()
	{
		return( control.getDataBase().getStats().getKeyCount());
	}
	
	public long
	getDBValueCount()
	{
		return( control.getDataBase().getStats().getValueCount());
	}
	
	public long
	getDBKeysBlocked()
	{
		return( control.getDataBase().getStats().getKeyBlockCount());
	}
	
	public long
	getDBKeyDivSizeCount()
	{
		int[]	vd = getValueDetails();
		
		return( vd[ DHTDBStats.VD_DIV_SIZE ]);
	}
	
	public long
	getDBKeyDivFreqCount()
	{
		int[]	vd = getValueDetails();
		
		return( vd[ DHTDBStats.VD_DIV_FREQ ]);
	}

	public long
	getDBStoreSize()
	{
		return( control.getDataBase().getStats().getSize());
	}
	
		// Router
	
	public long
	getRouterNodes()
	{
		return( router_snapshot[DHTRouterStats.ST_NODES]);
	}
	
	public long
	getRouterLeaves()
	{
		return( router_snapshot[DHTRouterStats.ST_LEAVES]);
	}
	
	public long
	getRouterContacts()
	{
		return( router_snapshot[DHTRouterStats.ST_CONTACTS]);
	}
	
	public long
	getRouterUptime()
	{
		return( control.getRouterUptime());
	}
	
	public int
	getRouterCount()
	{
		return( control.getRouterCount());
	}
	
	public String
	getVersion()
	{
		return( Constants.AZUREUS_VERSION );
	}
	
	public long
	getEstimatedDHTSize()
	{
		return( control.getEstimatedDHTSize());
	}
	
	public String
	getString()
	{
		return(	"transport:" + 
				getTotalBytesReceived() + "," +
				getTotalBytesSent() + "," +
				getTotalPacketsReceived() + "," +
				getTotalPacketsSent() + "," +
				getTotalPingsReceived() + "," +
				getTotalFindNodesReceived() + "," +
				getTotalFindValuesReceived() + "," +
				getTotalStoresReceived() + "," +
				getTotalKeyBlocksReceived() + "," +
				getAverageBytesReceived() + "," +
				getAverageBytesSent() + "," +
				getAveragePacketsReceived() + "," +
				getAveragePacketsSent() + "," +
				getIncomingRequests() + 
				",router:" +
				getRouterNodes() + "," +
				getRouterLeaves() + "," +
				getRouterContacts() + 
				",database:" +
				getDBKeyCount() + ","+
				getDBValueCount() + ","+
				getDBValuesStored() + ","+
				getDBStoreSize() + ","+
				getDBKeyDivFreqCount() + ","+
				getDBKeyDivSizeCount() + ","+
				getDBKeysBlocked()+ 
				",version:" + getVersion()+","+
				getRouterUptime() + ","+
				getRouterCount());
	}
}
