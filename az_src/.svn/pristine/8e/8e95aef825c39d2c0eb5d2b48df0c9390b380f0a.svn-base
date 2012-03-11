/*
 * Created on 02-Dec-2005
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
 
package org.gudy.azureus2.core3.disk.impl.access.impl;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;


public class 
DiskManagerWriteRequestImpl
	extends DiskManagerRequestImpl
	implements DiskManagerWriteRequest
{  
	private int 				pieceNumber;
	private int 				offset;
	private DirectByteBuffer	buffer;
	private Object				user_data;
 
	public 
	DiskManagerWriteRequestImpl(
		int 					_pieceNumber,
		int 					_offset,
		DirectByteBuffer 		_buffer,
		Object 					_user_data )
	{
		pieceNumber = _pieceNumber;
	    offset 		= _offset;
	    buffer		= _buffer;
	    user_data	=_user_data;
	}
	
	protected String
	getName()
	{
		return( "Write: " + pieceNumber + ",off=" + offset +",len=" + buffer.remaining( DirectByteBuffer.SS_DW ));
	}
	
	public int 
	getPieceNumber()
	{
		return( pieceNumber );
	}
	 
	public int 
	getOffset()
	{
		return( offset );
	}
	
	public DirectByteBuffer
	getBuffer()
	{
		return( buffer );
	}
	
	public Object
	getUserData()
	{
		return( user_data );
	}
}