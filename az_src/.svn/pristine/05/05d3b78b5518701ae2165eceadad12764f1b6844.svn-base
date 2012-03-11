/*
 * Created on 26-Jan-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;

public class 
TransportHelperFilterSwitcher 
	implements TransportHelperFilter
{
	private TransportHelperFilter	current_reader;
	private TransportHelperFilter	current_writer;
	
	private TransportHelperFilter	first_filter;
	private TransportHelperFilter	second_filter;
	
	private int	read_rem;
	private int	write_rem;
	
	private ByteBuffer	read_insert;
	
	public
	TransportHelperFilterSwitcher(
		TransportHelperFilter		_filter1,
		TransportHelperFilter		_filter2,
		int							_switch_read,
		int							_switch_write )
	{
		first_filter	= _filter1;
		second_filter	= _filter2;

		read_rem	= _switch_read;
		write_rem	= _switch_write;
		
		current_reader	= read_rem<=0?second_filter:first_filter;
		current_writer	= write_rem<=0?second_filter:first_filter;
	}
	
	public
	TransportHelperFilterSwitcher(
		TransportHelperFilter		_filter1,
		TransportHelperFilter		_filter2,
		ByteBuffer					_read_insert )
	{
		first_filter	= _filter1;
		second_filter	= _filter2;

		read_insert		= _read_insert;
		
		current_reader	= read_rem<=0?second_filter:first_filter;
		current_writer	= write_rem<=0?second_filter:first_filter;
	}
	
	public long 
	write( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		long	total_written	= 0;
		
		if ( current_writer != second_filter ){
			
			int[]	limits = new int[buffers.length];
			
			int	to_write	= write_rem;
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				limits[i]	= buffer.limit();
				
				int	rem = buffer.remaining();
				
				if ( rem > to_write ){
					
					buffer.limit( buffer.position() + to_write );
					
					to_write = 0;
					
				}else{
					
					to_write	-= rem;
				}
			}
			
			try{
				
				total_written = current_writer.write( buffers, array_offset, length );
				
				if ( total_written <= 0 ){
					
					return( total_written );
				}
			}finally{
				
				for (int i=array_offset;i<array_offset+length;i++){
					
					ByteBuffer	buffer = buffers[i];
					
					buffer.limit( limits[i] );
				}
			}
			
			write_rem -= total_written;
			
			if ( write_rem == 0 ){
				
					// writer may have data buffered up pending next write call - if so then
					// we need to get out now
				
				if ( current_writer.hasBufferedWrite()){
					
					return( total_written );
				}
								
				current_writer	= second_filter;
				
			}else{
				
				return( total_written );
			}
		}
		
		total_written += current_writer.write( buffers, array_offset, length );
		
		return( total_written );
	}

	public long 
	read( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		long	total_read	= 0;
				
		if ( read_insert != null ){
		
			int	pos_before	= read_insert.position();
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				int	space = buffer.remaining();
				
				if ( space > 0 ){
					
					if ( space < read_insert.remaining()){
						
						int	old_limit = read_insert.limit();
						
						read_insert.limit( read_insert.position() + space );
						
						buffer.put( read_insert );

						read_insert.limit( old_limit );
						
					}else{
						
						buffer.put( read_insert );
					}
					
					if ( !read_insert.hasRemaining()){
											
						break;
					}
				}
			}
			
			total_read	= read_insert.position() - pos_before;
			
			if ( read_insert.hasRemaining()){
				
				return( total_read );
				
			}else{
				
				read_insert	= null;
			}
		}
		
		if ( current_reader != second_filter ){
			
			int[]	limits = new int[buffers.length];
			
			int	to_read	= read_rem;
			
			for (int i=array_offset;i<array_offset+length;i++){
				
				ByteBuffer	buffer = buffers[i];
				
				limits[i]	= buffer.limit();
				
				int	rem = buffer.remaining();
				
				if ( rem > to_read ){
					
					buffer.limit( buffer.position() + to_read );
					
					to_read = 0;
					
				}else{
					
					to_read	-= rem;
				}
			}
			
			long	read;
			
			try{			
				read = current_reader.read( buffers, array_offset, length );
				
				if ( read <= 0 ){
					
					return( total_read );
				}
				
				total_read += read;
				
			}finally{
				
				for (int i=array_offset;i<array_offset+length;i++){
					
					ByteBuffer	buffer = buffers[i];
					
					buffer.limit( limits[i] );
				}
			}
			
			read_rem -= read;
			
			if ( read_rem == 0 ){
				
				current_reader	= second_filter;
				
			}else{
				
				return( total_read );
			}
		}
		
		total_read += current_reader.read( buffers, array_offset, length );
		
		return( total_read );		
	}
	
	public boolean
	hasBufferedWrite()
	{
		return( current_writer.hasBufferedWrite());
	}

	public boolean 
	hasBufferedRead() 
	{
		return( read_insert != null || current_reader.hasBufferedRead());
	}
	
	public TransportHelper
	getHelper()
	{
		return( second_filter.getHelper());
	}
	
	public void
	setTrace(
			boolean	on )
	{
		first_filter.setTrace( on );
		second_filter.setTrace( on );
	}
	
	public boolean 
	isEncrypted()
	{
		return( current_reader.isEncrypted() || current_writer.isEncrypted());
	}
	
	public String
	getName(boolean verbose)
	{
		return( second_filter.getName(verbose));
	}
}
