/*
 * Created on Nov 4, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.util;

import java.util.*;
import java.nio.ByteBuffer;

public class 
DirectByteBufferPoolHeap
	extends DirectByteBufferPool
{
	private final boolean USE_POOLS	= false;
	
	private final boolean TRACE	= false;
	
	private final int MIN_POOL;
	private final int MAX_POOL;
	

	private final LinkedList[]		pools;
	private final int[]				pool_sizes;

	private Map	sizes;
	
	protected
	DirectByteBufferPoolHeap()
	{
			/**
			 * On linux (at least) JDK1.7.0 using heap buffers doesn't play well as the JVM still
			 * goes and creates DirectByteBuffers under the covers and things run out of control
			 * (after a few mins I have a 9GB VM...)
			 */
		
		if ( USE_POOLS ){
			
			pool_sizes = new int[]{ 
					2*1024, 
					4*1024+128, 
					8*1024+128,
					16*1024+128,
					32*1024+128,
					64*1024+128,
					128*1024+128,
					256*1024 };
			
			MIN_POOL	= pool_sizes[0];
			MAX_POOL	= pool_sizes[pool_sizes.length-1];
			
			pools = new LinkedList[ pool_sizes.length ];
			
			for (int i=0;i<pools.length;i++){
				
				pools[i] = new LinkedList();
			}
			
			if ( TRACE ){
				
				sizes		= new TreeMap();
	
				SimpleTimer.addPeriodicEvent(
					"HeapPoolDumper",
					5000,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{
							synchronized( sizes ){
								
								String	str = "";
								
								Iterator it = sizes.entrySet().iterator();
								
								while( it.hasNext()){
									
									Map.Entry	entry = (Map.Entry)it.next();
									
									str += (str.length()==0?"":",") + entry.getKey() + "->" + entry.getValue();
								}
								
								System.out.println( "HB allocs: " + str );
							}
						}
					});
			}
		}else{
			
			MIN_POOL = MAX_POOL = 0;
			
			pools 		= null;
			pool_sizes	= null;
		}
	}
	
	protected DirectByteBuffer
	getBufferSupport(
		byte		allocator,
		int			length )
	{
		if ( USE_POOLS ){
			
			if ( TRACE ){
				
				synchronized( sizes ){
					
					Integer key = new Integer( length/32*32 );
					
					Integer count = (Integer)sizes.get( key );
					
					if ( count == null ){
						
						sizes.put( key, new Integer(1));
						
					}else{
						
						sizes.put( key, new Integer( count.intValue() + 1 ));
					}
				}
			}
			
			int	pool_index = getPoolIndex( length );
			
			if ( pool_index != -1 ){
				
				LinkedList pool = pools[pool_index];
							
				synchronized( pool ){
					
					if ( !pool.isEmpty()){
						
						Object[] entry = (Object[])pool.removeLast();
						
						ByteBuffer buff = (ByteBuffer)entry[0];
						
				        buff.clear();
				        
						buff.limit( length );
						
						return( new DirectByteBuffer( allocator, buff, this ));
					}
				}	
	
				DirectByteBuffer	buffer = new DirectByteBuffer( allocator, ByteBuffer.allocate( pool_sizes[pool_index] ), this );
				
				ByteBuffer buff = buffer.getBufferInternal();
					        
				buff.limit( length );
				
				return( buffer );
	
			}else{
			
				return( new DirectByteBuffer( allocator, ByteBuffer.allocate( length ), this ));
			}
		}else{
			
			return( new DirectByteBuffer( allocator, ByteBuffer.allocate( length ), this ));
		}
	}
	
	protected void
	returnBufferSupport(
		DirectByteBuffer	buffer )
	{
		if ( USE_POOLS ){
			
			ByteBuffer	buff = buffer.getBufferInternal();
			
			int	length = buff.capacity();
			
			int	pool_index = getPoolIndex( length );
			
			if ( pool_index != -1 ){
				
				LinkedList pool = pools[pool_index];
				
				synchronized( pool ){
					
					pool.addLast( new Object[]{ buff });
				}
			}
		}
	}
	
	protected int
	getPoolIndex(
		int		length )
	{
		if ( length < MIN_POOL|| length > MAX_POOL ){
			
			return( -1 );
		}
		
		for (int i=0;i<pool_sizes.length;i++){
			
			if ( length <= pool_sizes[i] ){
				
				return( i );
			}
		}
		
		return( -1 );
	}
}
