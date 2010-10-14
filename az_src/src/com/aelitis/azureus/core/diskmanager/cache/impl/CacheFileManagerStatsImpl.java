/*
 * Created on 06-Aug-2004
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

package com.aelitis.azureus.core.diskmanager.cache.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.Average;

import com.aelitis.azureus.core.diskmanager.cache.*;
import org.gudy.azureus2.core3.util.*;

public class 
CacheFileManagerStatsImpl
	implements CacheFileManagerStats
{
	protected CacheFileManagerImpl		manager;
	
		// average over 10 seconds
	
	protected Average	cache_read_average 	= Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 10);
	protected Average	cache_write_average = Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 10);
	protected Average	file_read_average 	= Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 10);
	
		// file writes are bursty so use a lower average time
	
	protected Average	file_write_average 	= Average.getInstance(CacheFileManagerImpl.STATS_UPDATE_FREQUENCY, 5);

	protected long		last_cache_read;
	protected long		last_cache_write;
	protected long		last_file_read;
	protected long		last_file_write;
	
	protected AEMonitor	this_mon	= new AEMonitor( "CacheFileManagerStats" );
	
	protected
	CacheFileManagerStatsImpl(
		CacheFileManagerImpl	_manager )
	{
		manager	= _manager;
	}
	
	protected void
	update()
	{
		try{
			this_mon.enter();
		
				// cache read
			
			long	cache_read		= manager.getBytesReadFromCache();
			long	cache_read_diff	= cache_read - last_cache_read;
			
			last_cache_read	= cache_read;
			
			cache_read_average.addValue( cache_read_diff );
			
				// cache write
			
			long	cache_write		= manager.getBytesWrittenToCache();
			long	cache_write_diff	= cache_write - last_cache_write;
			
			last_cache_write	= cache_write;
			
			cache_write_average.addValue( cache_write_diff );
	
				// file read
			
			long	file_read		= manager.getBytesReadFromFile();
			long	file_read_diff	= file_read - last_file_read;
			
			last_file_read	= file_read;
			
			file_read_average.addValue( file_read_diff );
			
				// file write
			
			long	file_write		= manager.getBytesWrittenToFile();
			long	file_write_diff	= file_write - last_file_write;
			
			last_file_write	= file_write;
			
			file_write_average.addValue( file_write_diff );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public long
	getSize()
	{
		return( manager.getCacheSize());
	}
	
	public long
	getUsedSize()
	{
		return( manager.getCacheUsed());
	}
	
	public long
	getBytesWrittenToCache()
	{
		return( manager.getBytesWrittenToCache());
	}
	
	public long
	getBytesWrittenToFile()
	{
		return( manager.getBytesWrittenToFile());
	}
	
	public long
	getBytesReadFromCache()
	{
		return( manager.getBytesReadFromCache());
	}
	
	public long
	getBytesReadFromFile()
	{
		return( manager.getBytesReadFromFile());
	}
	
	public long
	getAverageBytesWrittenToCache()
	{
		return( cache_write_average.getAverage());
	}
	
	public long
	getAverageBytesWrittenToFile()
	{
		return( file_write_average.getAverage() );
	}
	
	public long
	getAverageBytesReadFromCache()
	{
		return( cache_read_average.getAverage() );
	}
	
	public long
	getAverageBytesReadFromFile()
	{
		return( file_read_average.getAverage() );
	}
	
	public long
	getCacheReadCount()
	{
		return( manager.getCacheReadCount());
	}
	
	public long
	getCacheWriteCount()
	{
		return( manager.getCacheWriteCount());
	}
	
	public long
	getFileReadCount()
	{
		return( manager.getFileReadCount());
	}
	
	public long
	getFileWriteCount()
	{
		return( manager.getFileWriteCount());
	}
	
	public boolean[] getBytesInCache(TOTorrent torrent, long[] absoluteOffsets, long[] lengths)
	{
		return manager.getBytesInCache( torrent, absoluteOffsets, lengths);
	}
}
