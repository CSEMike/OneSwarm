/*
 * Created on 21-Jul-2005
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

package org.gudy.azureus2.pluginsimpl.local.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;

public class 
PooledByteBufferImpl 
	implements PooledByteBuffer
{
	private DirectByteBuffer	buffer;
	
	public
	PooledByteBufferImpl(
		DirectByteBuffer	_buffer )
	{
		buffer	= _buffer;
	}
	
	public
	PooledByteBufferImpl(
		int		size )
	{
		buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL, size );
	}
	
	public
	PooledByteBufferImpl(
		byte[]		data )
	{
		buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL, data.length );
		
		buffer.put( DirectByteBuffer.AL_EXTERNAL, data );
		
		buffer.position( DirectByteBuffer.AL_EXTERNAL, 0 );
	}
	
	public
	PooledByteBufferImpl(
		byte[]		data,
		int			offset,
		int			length )
	{
		buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL, length );
		
		buffer.put( DirectByteBuffer.AL_EXTERNAL, data, offset, length );
		
		buffer.position( DirectByteBuffer.AL_EXTERNAL, 0 );
	}
	
	public byte[]
	toByteArray()
	{
		buffer.position( DirectByteBuffer.SS_EXTERNAL, 0 );
		
		int	len = buffer.limit( DirectByteBuffer.SS_EXTERNAL );
		
		byte[]	res = new byte[len];
		
		buffer.get( DirectByteBuffer.SS_EXTERNAL, res );
		
		buffer.position( DirectByteBuffer.SS_EXTERNAL, 0 );

		return( res );
	}
	
	public ByteBuffer
	toByteBuffer()
	{
		return( buffer.getBuffer( DirectByteBuffer.SS_EXTERNAL ));
	}
	
	public Map
	toMap()
	
		throws IOException
	{
		return( BDecoder.decode( toByteArray()));
	}
	
	public DirectByteBuffer
	getBuffer()
	{
		return( buffer );
	}
	
	public void
	returnToPool()
	{
		buffer.returnToPool();
	}
}
