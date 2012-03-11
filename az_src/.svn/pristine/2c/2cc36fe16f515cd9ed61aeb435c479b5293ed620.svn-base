/*
 * Created on Apr 24, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.diskmanager;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.security.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * This class implements a virtual disk file backed by a pool of direct
 * memory MappedByteBuffers, designed for high-speed random reads/writes.
 * 
 * NOTE: Abandoning this code for now, as JRE 1.4 series does not provide an
 * unmap() method for MappedByteBuffers, so we have to wait for the VM
 * to lazily free the underlying direct memory regions, which means we
 * eventually run out of direct memory when accessing files larger than the
 * allowed direct memory space, since the unused buffers are not freed
 * quickly enough.  Forcing the memory unmap via sun.misc.Cleaner does not work
 * properly under Windows, throwing the error described in the clean(x) method
 * below.
 *  
 */
public class MemoryMappedFile {

  public static final int MODE_READ_ONLY  = 0;
  public static final int MODE_READ_WRITE = 1;

  public static long cache_hits = 0;
  public static long cache_misses = 0;
  
  private static final int BLOCK_SIZE = 10*1024*1024; //10MB
  private final File file;
  private int access_mode = MODE_READ_ONLY;
  private FileChannel channel;
  private Object[] mapKeys;
  private int[] counts = new int[ 10000 ];
  
  public MemoryMappedFile( File file ) {
    this.file = file;
    mapKeys = new Object[ new Long(file.length()/BLOCK_SIZE).intValue() + 1 ];
    Arrays.fill( counts, 0 );
  }
  
  
  public int getAccessMode() {  return access_mode;  }
  public void setAccessMode( int mode ) {
  	if ( mode == MODE_READ_ONLY && access_mode == MODE_READ_WRITE && channel != null ) {
  		try { channel.close(); } catch( Exception e ) { Debug.printStackTrace( e ); }
      channel = null;
  	}
    access_mode = mode;
  }
 
  public void write( DirectByteBuffer buffer, int buffer_offset, long file_offset, int length ) throws IOException {
  	if ( access_mode == MODE_READ_ONLY ) {
      throw new IOException( "cannot write to a read-only file" );
    }
        
    if ( buffer.limit(DirectByteBuffer.SS_OTHER) - buffer_offset < length ) {
      throw new IOException( "not enough buffer remaining to write given length" );
    }
    
    file.createNewFile();
    
    int key_pos = new Long( file_offset / BLOCK_SIZE ).intValue();
    int map_offset = new Long( file_offset % BLOCK_SIZE ).intValue();
    int written = 0;
    
    while( written < length ) {
      MappedByteBuffer mbb = null;
      long f_offset = file_offset + written;
      
      int length_to_write = BLOCK_SIZE - map_offset;
      if ( length - written < length_to_write ) length_to_write = length - written;
      
      //try grab the buffer from the pool
      if ( mapKeys.length > key_pos ) {
        Object key = mapKeys[ key_pos ];  
        if ( key != null ) {
          mbb = MemoryMapPool.getBuffer( key );
          //check if the current buff is too small
          if ( mbb != null && mbb.capacity() < (map_offset + length_to_write ) ) {
          	MemoryMapPool.clean( mbb );
          	mbb = null;
          }
        }
      }
      else {  //need to extend the key array
        mapKeys = new Object[ key_pos * 2 ];
      }
            
      //create new buffer, fully-sized, unless it's extending the file
      if ( mbb == null ) {
        int size = BLOCK_SIZE;
        if ( f_offset + length_to_write > file.length() ) {
          size = map_offset + length_to_write;
        }
        mbb = createMappedBuffer( f_offset - map_offset, size );
        cache_misses++;
      }
      else cache_hits++;
      
      //do the write
      buffer.position( DirectByteBuffer.SS_OTHER,buffer_offset + written );
      buffer.limit( DirectByteBuffer.SS_OTHER,buffer.position(DirectByteBuffer.SS_OTHER) + length_to_write );
      mbb.position( map_offset );
      mbb.put( buffer.getBuffer(DirectByteBuffer.SS_OTHER) );
      written += length_to_write;
      
      //add the buffer (back) to the pool
      mapKeys[ key_pos ] = MemoryMapPool.addBuffer( mbb );
      
      key_pos++;
      map_offset = 0;
    }
    
  }
  

  private MappedByteBuffer createMappedBuffer( long file_offset, int length ) throws IOException {
    if ( channel == null ) {
      FileChannel fc = new RandomAccessFile( file, "rw" ).getChannel();
      MappedByteBuffer mbb = fc.map( FileChannel.MapMode.READ_WRITE, file_offset, length );
      if ( access_mode == MODE_READ_ONLY ) fc.close();
      else channel = fc;
      return mbb;
    }
    
    return channel.map( FileChannel.MapMode.READ_WRITE, file_offset, length );
  }
  
  
  
  private static class MemoryMapPool {
    private static long MAX_SIZE = 100*1024*1024; //100MB
    private static final MemoryMapPool instance = new MemoryMapPool();
    private long total_size = 0;
  	private final AEMonitor		buffers_mon		= new AEMonitor( "MemoryMappedFile:buffers" );

    private final Map buffers = new LinkedHashMap( (int)(MAX_SIZE/BLOCK_SIZE), .75F, true ){
    	
    	//This method is called just after a new entry has been added
      public boolean removeEldestEntry(Map.Entry eldest) {
        boolean remove = total_size > MAX_SIZE;
        if ( remove ) {
          MappedByteBuffer mbb = (MappedByteBuffer)eldest.getValue();
          total_size -= mbb.capacity();
          clean( mbb );
        }
    		return remove;
    	}
    };  
    
    //may return null despite a previously-valid key, because the buffer was GC'd
    //note: the buffer is removed from the pool
    private static MappedByteBuffer getBuffer( Object key ) {
      try{
      	instance.buffers_mon.enter();
      
        MappedByteBuffer mbb = (MappedByteBuffer)instance.buffers.remove( key );
        if ( mbb != null ) instance.total_size -= mbb.capacity();
        return mbb;
      }finally{
      	
      	instance.buffers_mon.exit();
      }
    }
       
    private static Object addBuffer( MappedByteBuffer buffer ) {
      Object key = new Object();
      try{
      	instance.buffers_mon.enter();
      
        instance.total_size += buffer.capacity();
        instance.buffers.put( key, buffer );
      }finally{
      	
      	instance.buffers_mon.exit();
      }
      return key;
    }
    
    
    
    //Manually Cleaning the buffer doesn't work properly under windows:
    //java.lang.Error: Cleaner terminated abnormally:
    //Caused by: java.io.IOException: The process cannot access the file
    //because another process has locked a portion of the file
    //See Sun bug id #4938372.
  	private static void clean( final MappedByteBuffer buffer ) {
  		AccessController.doPrivileged( new PrivilegedAction() {
  			public Object run() {
  				try {
  					//Method getCleanerMethod = buffer.getClass().getMethod( "cleaner", new Class[0] );
  					//getCleanerMethod.setAccessible( true );
  					//sun.misc.Cleaner cleaner = (sun.misc.Cleaner)getCleanerMethod.invoke( buffer, new Object[0] );
  					//cleaner.clean();
  				}
          catch (Exception e) { Debug.printStackTrace( e ); }
  				return null;
  			}
  		});
  	}
    
    
  }


}

