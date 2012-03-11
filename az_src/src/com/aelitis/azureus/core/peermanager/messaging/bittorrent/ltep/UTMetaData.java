/*
 * Created on Mar 7, 2012
 * Created by Paul Gardner
 * 
 * Copyright 2012 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessagingUtil;

public class 
UTMetaData 
	implements LTMessage
{
	public static final int MSG_TYPE_REQUEST	= 0;
	public static final int MSG_TYPE_DATA		= 1;
	public static final int MSG_TYPE_REJECT		= 2;
	
	private final byte version;
	private DirectByteBuffer buffer = null;
	  
	private int					msg_type;
	private int					piece;
	private DirectByteBuffer	metadata;
	private int					total_size;
	
	public
	UTMetaData(
		int		_piece,
		byte	_version )
	{
		msg_type	= MSG_TYPE_REQUEST;
		piece		= _piece;
		version		= _version;
	}
	
	public
	UTMetaData(
		int				_piece,
		ByteBuffer		_data,
		int				_total_size,
		byte			_version )
	{
		msg_type	= _data==null?MSG_TYPE_REJECT:MSG_TYPE_DATA;
		piece		= _piece;
		total_size	= _total_size;
		version		= _version;
		
		if ( _data != null ){
			
			metadata = new DirectByteBuffer( _data );
		}
	}
	
	public 
	UTMetaData(
		Map					map,
		DirectByteBuffer	data,
		byte				_version )
	{
		if ( map != null ){
		
			msg_type = ((Long)map.get( "msg_type" )).intValue();
			piece	 = ((Long)map.get( "piece" )).intValue();
		}
		
		metadata	= data;
		version		= _version;
	}
	
	public String 
	getID()
	{
		return( ID_UT_METADATA );
	}

	public byte[] 
	getIDBytes()
	{
		return( ID_UT_METADATA_BYTES );
	}

	public String 
	getFeatureID() 
	{  
		return LTMessage.LT_FEATURE_ID;  
	}  
	
	public int 
	getFeatureSubID() 
	{ 
		return SUBID_UT_METADATA;  
	}
	
	public int 
	getType() 
	{  
		return Message.TYPE_PROTOCOL_PAYLOAD;  
	}
	
	public byte 
	getVersion() 
	{ 
		return version; 
	};

	public String 
	getDescription()
	{
		return( ID_UT_METADATA );
	}

	public int
	getMessageType()
	{
		return( msg_type );
	}
	
	public int
	getPiece()
	{
		return( piece );
	}
	
	public DirectByteBuffer
	getMetadata()
	{
		return( metadata );
	}
	
	public DirectByteBuffer[] 
	getData()
	{
		if ( buffer == null ){
			
			Map payload_map = new HashMap();

			payload_map.put( "msg_type", new Long( msg_type ));
			payload_map.put( "piece", new Long(piece));
			
			if ( total_size > 0 ){
				
				payload_map.put( "total_size", total_size );
			}
			
			buffer = MessagingUtil.convertPayloadToBencodedByteStream(payload_map, DirectByteBuffer.AL_MSG_UT_PEX);
		}

		if ( msg_type == MSG_TYPE_DATA ){
			
			return new DirectByteBuffer[]{ buffer, metadata };
		}else{
		
			return new DirectByteBuffer[]{ buffer };
		}
	}



	public Message 
	deserialize( 
		DirectByteBuffer 	data, 
		byte 				version ) 

		throws MessageException
	{
		int	pos = data.position( DirectByteBuffer.SS_MSG );
		
		byte[] dict_bytes = new byte[ Math.min( 128, data.remaining( DirectByteBuffer.SS_MSG )) ];
					
		data.get( DirectByteBuffer.SS_MSG, dict_bytes );
		
		try{
			Map root = BDecoder.decode( dict_bytes );

			data.position( DirectByteBuffer.SS_MSG, pos + BEncoder.encode( root ).length );			
								
			return( new UTMetaData( root, data, version ));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new MessageException( "decode failed", e ));
		}
	}


	public void 
	destroy()
	{
		if ( buffer != null ){
			
			buffer.returnToPool();
		}
	}
}