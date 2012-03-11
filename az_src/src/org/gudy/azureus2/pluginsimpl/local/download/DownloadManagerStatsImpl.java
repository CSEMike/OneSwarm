/*
 * Created on 15-Jul-2005
 * Created by Paul Gardner
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
 *
 */

package org.gudy.azureus2.pluginsimpl.local.download;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.plugins.download.DownloadManagerStats;

public class 
DownloadManagerStatsImpl
	implements DownloadManagerStats
{
	private GlobalManagerStats		global_manager_stats;
	
	private OverallStats			overall_stats;
	
	protected
	DownloadManagerStatsImpl(
		GlobalManager	_gm )
	{
		global_manager_stats	= _gm.getStats();
		
		overall_stats = StatsFactory.getStats();
	}
	
	public long
	getOverallDataBytesReceived()
	{
		return( overall_stats.getDownloadedBytes());
	}
	
	public long
	getOverallDataBytesSent()
	{
		return( overall_stats.getUploadedBytes());
	}
	
	public long
	getSessionUptimeSeconds()
	{
		return( overall_stats.getSessionUpTime());
	}
	
	public int 
	getDataReceiveRate()
	{
		return( global_manager_stats.getDataReceiveRate());
	}
	  
	public int 
	getProtocolReceiveRate()
	{
		return( global_manager_stats.getProtocolReceiveRate());
	}
		
	public int 
	getDataAndProtocolReceiveRate()
	{
		return( global_manager_stats.getDataAndProtocolReceiveRate());
	}
	public int 
	getDataSendRate()
	{
		return( global_manager_stats.getDataSendRate());
	}
	  
	public int 
	getProtocolSendRate()
	{
		return( global_manager_stats.getProtocolSendRate());
	}
	   
	public int 
	getDataAndProtocolSendRate()
	{
		return( global_manager_stats.getDataAndProtocolSendRate());
	}
	
	public long 
	getDataBytesReceived()
	{
		return( global_manager_stats.getTotalDataBytesReceived());
	}
	  
	public long 
	getProtocolBytesReceived()
	{
		return( global_manager_stats.getTotalProtocolBytesReceived());
	}
	  	
	public long 
	getDataBytesSent()
	{
		return( global_manager_stats.getTotalDataBytesSent());
	}
	  
	public long 
	getProtocolBytesSent()
	{
		return( global_manager_stats.getTotalProtocolBytesSent());
	}
}
