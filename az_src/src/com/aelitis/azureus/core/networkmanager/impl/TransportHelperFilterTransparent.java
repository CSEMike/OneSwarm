/*
 * Created on 17-Jan-2006
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
TransportHelperFilterTransparent 
	implements TransportHelperFilter
{
	private TransportHelper		transport;
	private boolean				is_plain;
	
	private ByteBuffer			read_insert;
	
	public 
	TransportHelperFilterTransparent(
		TransportHelper		_transport,
		boolean				_is_plain )
	{
		transport	= _transport;
		is_plain	= _is_plain;
	}
	
	protected void
	insertRead(
		ByteBuffer	_read_insert )
	{
		read_insert	= _read_insert;
	}
	
	public boolean
	hasBufferedWrite()
	{
		return( transport.hasDelayedWrite());
	}
	
	public boolean 
	hasBufferedRead() 
	{
		return( read_insert != null && read_insert.remaining() > 0 );
	}
	
	public long 
	write( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		return( transport.write( buffers, array_offset, length ));
	}

	public int 
	write( 
		ByteBuffer 		buffer,
		boolean			partial_write )
	
		throws IOException
	{
		return( transport.write( buffer, partial_write ));
	}
	
	public long 
	read( 
		ByteBuffer[] 	buffers, 
		int 			array_offset, 
		int 			length ) 
	
		throws IOException
	{
		int	len = 0;
		
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
			
			len	= read_insert.position() - pos_before;
			
			if ( read_insert.hasRemaining()){
				
				return( len );
				
			}else{
				
				read_insert	= null;
			}
		}
		
		return( len + transport.read( buffers, array_offset, length ));
	}

	public int 
	read( 
		ByteBuffer 		buffer )
	
		throws IOException
	{

		if ( read_insert != null ){
			
			return((int)read( new ByteBuffer[]{ buffer }, 0, 1 ));
		}
		
		return( transport.read( buffer ));
	}
	
	public TransportHelper
	getHelper()
	{
		return( transport );
	}
	
	public void
	setTrace(
			boolean	on )
	{
		transport.setTrace( on );
	}
	  
	public boolean 
	isEncrypted()
	{
		return( false );
	}
	
	public String
	getName(boolean verbose)
	{
		String proto_str = getHelper().getName(verbose);
		
		if ( proto_str.length() > 0 ){
			
			proto_str = " (" + proto_str + ")";
		}
		
		return((is_plain?"Plain":"None") + proto_str );
	}
}
