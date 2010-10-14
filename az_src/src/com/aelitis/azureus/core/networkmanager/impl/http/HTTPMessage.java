/*
 * Created on 5 Oct 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.http;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class 
HTTPMessage
	implements Message
{
	public static final String	MSG_ID 			= "HTTP_DATA";
	private static final byte[]	MSG_ID_BYTES	= MSG_ID.getBytes();
	private static final String	MSG_DESC		= "HTTP data";
	
	
	private DirectByteBuffer[]	data;
	
	protected
	HTTPMessage(
		String	stuff )
	{
		data = new DirectByteBuffer[]{ new DirectByteBuffer( ByteBuffer.wrap( stuff.getBytes())) };
	}
	
	protected
	HTTPMessage(
		byte[]	stuff )
	{
		data = new DirectByteBuffer[]{ new DirectByteBuffer( ByteBuffer.wrap( stuff )) };
	}
	
	public String 
	getID()
	{
		return( MSG_ID );
	}
	  
	public byte[] 
	getIDBytes()
	{
		return( MSG_ID_BYTES );
	}
	
	public String 
	getFeatureID()
	{
		return( null );
	}

	public int 
	getFeatureSubID()
	{
		return( 0 );
	}
	
	public int 
	getType()
	{
		return( TYPE_DATA_PAYLOAD );
	}

	public byte
	getVersion()
	{
		return( 1 );
	}
	
	public String 
	getDescription()
	{
		return( MSG_DESC );
	}

	public DirectByteBuffer[] 
	getData()
	{
		return( data );
	}

	public Message 
	deserialize( 
		DirectByteBuffer 	data,
		byte				version ) 
	
		throws MessageException
	{
		throw( new MessageException( "not supported" ));
	}

	protected RawMessage
	encode(
		Message message )
	{
		return( 
				new RawMessageImpl( 
						message, 
						data,
						RawMessage.PRIORITY_HIGH, 
						true, 
						new Message[0] ));
	}
	
	public void 
	destroy()
	{
		data[0].returnToPool();
	}
}
