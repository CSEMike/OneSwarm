/*
 * Created on 04-Aug-2004
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

import java.io.*;


import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author parg
 *
 */


public class 
Test 
{
	public static void
	main(
		String	[]args )
	{
		System.setProperty("azureus.log.stdout","1");
		
		Logger.addListener(new ILogEventListener() {
			public void log(LogEvent event) {
				System.out.println(event.text);
			}
		});
		
		try{
			CacheFileManagerImpl	manager = (CacheFileManagerImpl)CacheFileManagerFactory.getSingleton();
			
			//manager.initialise( false, 8*1024*1024 );
	
			//new Test().writeTest(manager);
			
			manager.initialise( true, true, true, 10*1024*1024, 1024 );

			new Test().writeTest(manager);
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	writeTest(
		CacheFileManagerImpl	manager )
	{
		try{
			final File	f = new File("C:\\temp\\cachetest.dat" );
			
			f.delete();
			
			CacheFile	cf = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file " + f.toString() );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFile(String name) 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					f, CacheFile.CT_LINEAR );
			
			cf.setAccessMode( CacheFile.CF_WRITE );
			
			long	start = System.currentTimeMillis();
			
			int		loop	= 10000;
			int		block	= 1*1024;
			
			for (int i=0;i<loop;i++){
				
				DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER, block);
				
				cf.writeAndHandoverBuffer( buffer, i*block);
			}
			
			cf.close();
			
			long 	now = System.currentTimeMillis();
			
			long	total = loop*block;
			
			long	elapsed = now - start;
			
			System.out.println( "time = " + elapsed + ", speed = " + (total/elapsed));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	manualTest(
		CacheFileManager	manager )
	{
		try{
			final File	f = new File("C:\\temp\\cachetest.dat" );
			
			f.delete();
			
			CacheFile	cf = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file " + f.toString() );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFile(String name) 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					f, CacheFile.CT_LINEAR );
			DirectByteBuffer	write_buffer1 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			DirectByteBuffer	write_buffer2 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			DirectByteBuffer	write_buffer3 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			
			cf.writeAndHandoverBuffer( write_buffer2, 512 );
				
			cf.flushCache();
			
			cf.writeAndHandoverBuffer( write_buffer3, 1024 );
			cf.writeAndHandoverBuffer( write_buffer1, 0 );
			
			cf.flushCache();
			
			write_buffer1 = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,512);
			cf.writeAndHandoverBuffer( write_buffer1, 0 );

			cf.flushCache();
				
			cf.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public void
	randomTest(
		CacheFileManager	manager )
	{
		try{			
			CacheFile[]	files = new CacheFile[3];
			
			byte[][]	file_data	= new byte[3][];
			
			for (int i=0;i<files.length;i++){
				
				final	int f_i = i;
			
				file_data[i] = new byte[randomInt(200000)];
				
				files[i] = manager.createFile(
					new CacheFileOwner()
					{
						public String
						getCacheFileOwnerName()
						{
							return( "file" + f_i );
						}
						
						public TOTorrentFile
						getCacheFileTorrentFile()
						{
							return( null );
						}
						public File 
						getCacheFileControlFile(String name) 
						{
							return null;
						}
   						public int
						getCacheMode()
						{
							return( CacheFileOwner.CACHE_MODE_NORMAL );
						}
					},
					new File( "C:\\temp\\cachetest" + i + ".dat" ), CacheFile.CT_LINEAR);
				
				files[i].setAccessMode( CacheFile.CF_WRITE );
				
				DirectByteBuffer bb = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_OTHER,file_data[i].length);
				
				bb.put( DirectByteBuffer.SS_CACHE, file_data[i]);
				
				bb.position(DirectByteBuffer.SS_CACHE, 0);
				
				files[i].write(bb,0);
			}
			
			int	quanitize_to					= 100;
			int quanitize_to_max_consec_write	= 1;
			int quanitize_to_max_consec_read	= 3;
			
			for (int x=0;x<10000000;x++){
				
				int	file_index = randomInt(files.length);
				
				CacheFile	cf = files[file_index];
				
				byte[]	bytes = file_data[ file_index ];
				
				
				int	p1 = randomInt( bytes.length );
				int p2 = randomInt( bytes.length );
				
				p1 = (p1/quanitize_to)*quanitize_to;
				p2 = (p2/quanitize_to)*quanitize_to;
				
				if ( p1 == p2 ){
					
					continue;
				}
				
				int start 	= Math.min(p1,p2);
				int len	 	= Math.max(p1,p2) - start;
				
				int	function = randomInt(100);
				
				if ( function < 30){
					
					if ( len > quanitize_to*quanitize_to_max_consec_read ){
						
						len = quanitize_to*quanitize_to_max_consec_read;
					}
					
					DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER,len );
					
					System.out.println( "read:" + start + "/" + len );
					
					cf.read( buffer, start, CacheFile.CP_READ_CACHE );
					
					buffer.position(DirectByteBuffer.SS_CACHE, 0);
					
					byte[]	data_read = new byte[len];
					
					buffer.get( DirectByteBuffer.SS_CACHE, data_read );
					
					for (int i=0;i<data_read.length;i++){
						
						if ( data_read[i] != bytes[ i+start ]){
							
							throw( new Exception( "data read mismatch" ));
						}
					}
					
					buffer.returnToPool();
					
				}else if ( function < 80 ){
					if ( len > quanitize_to*quanitize_to_max_consec_write ){
						
						len = quanitize_to*quanitize_to_max_consec_write;
					}
					
					System.out.println( "write:" + start + "/" + len );
					
					DirectByteBuffer	buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_OTHER,len );
					
					for (int i=0;i<len;i++){
						
						bytes[start+i] = (byte)randomInt(256);
						
						buffer.put( DirectByteBuffer.SS_CACHE, bytes[start+i]);
					}
					
					buffer.position(DirectByteBuffer.SS_CACHE, 0);
					
					cf.writeAndHandoverBuffer( buffer, start );
					
				}else if ( function < 90 ){
					
					cf.flushCache();
					
				}else if ( function < 91 ){
					
					cf.clearCache();
					
					//System.out.println( "closing file" );
					
					//cf.close();
				}
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	static int
	randomInt(
		int	num )
	{
		return( (int)(Math.random()*num ));
	}
}
