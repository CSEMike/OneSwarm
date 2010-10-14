/*
 * Created on 19 Jun 2006
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class 
GenericMessage 
	implements Message
{
	private DirectByteBuffer buffer = null;

	private final String 	id;
	private final String	desc;
	private final boolean	already_encoded;
	
	protected
	GenericMessage(
		String				_id,
		String				_desc,
		DirectByteBuffer	_buffer,
		boolean				_already_encoded )
	{
		id				= _id;
		desc			= _desc;
		buffer			= _buffer;
		already_encoded	= _already_encoded;
	}
	
	protected boolean
	isAlreadyEncoded()
	{
		return( already_encoded );
	}
	
	public String getID()
	{
		return( id );
	}
	  
	public byte[] 
	getIDBytes()
	{
		return( id.getBytes());
	}
	
	public String 
	getFeatureID()
	{
		return( null );
	}
	  
	public int 
	getFeatureSubID()
	{
		return(0);
	}
	  
	public int 
	getType()
	{
		return( TYPE_DATA_PAYLOAD );
	}
	    
	public String 
	getDescription()
	{
		return( desc );
	}
	
	public byte
	getVersion()
	{
		return( 1 );
	}
	 
	public DirectByteBuffer
	getPayload()
	{
		return( buffer );
	}
	
	public DirectByteBuffer[]
	getData()
	{
		return new DirectByteBuffer[]{ buffer };
	}
	  
	public Message 
	deserialize( 
		DirectByteBuffer 	data,
		byte				version ) 
	  
	 	throws MessageException
	{
		throw( new MessageException( "not imp" ));
	}
	    
	public void 
	destroy()
	{
		buffer.returnToPool();
	}
}
