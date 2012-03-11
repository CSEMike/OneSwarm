/*
 * Created on Mar 20, 2008
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


package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class 
BTRawMessage
	implements BTMessage, RawMessage
{
	private DirectByteBuffer		buffer;
	
	public
	BTRawMessage(
		DirectByteBuffer		_buffer )
	{
		buffer	= _buffer;
	}
	
	public String getID()
	{
		return( "" );
	}
	  
	public byte[] 
	getIDBytes()
	{
		return( null );
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
		return( "<raw bt data>" );
	}
	
	public byte
	getVersion()
	{
		return( 1 );
	}
	
	public Message 
	deserialize(
		DirectByteBuffer	data, 
		byte 				version )
	
		throws MessageException
	{
		throw( new MessageException( "not imp" ));
	}
	
	public DirectByteBuffer[] 
	getData()
	{ 
		return new DirectByteBuffer[]{ buffer };
	}
	
	public DirectByteBuffer[] 
	getRawData()
	{
		return( new DirectByteBuffer[]{ buffer });
	}
     
	public int 
	getPriority()
	{
		return( PRIORITY_HIGH );
	}
	 
	public boolean 
	isNoDelay()
	{
		return( true );
	}
	  
	public void
	setNoDelay()
	{
	}
	  
	public Message[] 
	messagesToRemove()
	{
		return( null );
	}
	 
	public Message 
	getBaseMessage()
	{
		return( this );
	}
	
	public void 
	destroy() 
	{
		if( buffer != null )  buffer.returnToPool();    
	}
}
