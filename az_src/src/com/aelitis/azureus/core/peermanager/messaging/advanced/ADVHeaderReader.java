/*
 * Created on Jan 16, 2006
 * Created by Alon Rohter
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
package com.aelitis.azureus.core.peermanager.messaging.advanced;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;


//based on http://82.182.115.6/extension.txt  //TODO

/**
 * 
 */
public class ADVHeaderReader {	
	private static final byte SS = DirectByteBuffer.SS_MSG;
	
	private static final int SHORT_HEADER_SIZE = 2;
	private static final int LONG_HEADER_SIZE  = 4;
	
	private final DirectByteBuffer short_header_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, SHORT_HEADER_SIZE );
	private final DirectByteBuffer long_header_buff = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, LONG_HEADER_SIZE );


  private boolean reading_handshake = true;  //stream format during handshake is just a 32bit int length prefix  
	private boolean reading_short_header = false;
	
	
	private int message_length;
	private int message_id;
	private int message_sub_id;
	
	
	protected ADVHeaderReader() {
		/*nothing*/
	}
	
	
	
	
	public ByteBuffer getReadBuffer() {
		if( reading_short_header ) {
			return short_header_buff.getBuffer( SS );
		}
		
		return long_header_buff.getBuffer( SS );
	}
	
	
	
	public boolean isHeaderReadComplete() {
		boolean completed = false;

		if( reading_short_header ) {
			short_header_buff.limit( SS, SHORT_HEADER_SIZE );  //ensure proper limit
			if( short_header_buff.position( SS ) == SHORT_HEADER_SIZE ) {
				completed = true;
			}
		}
		else {
			long_header_buff.limit( SS, LONG_HEADER_SIZE );  //ensure proper limit
			if( long_header_buff.position( SS ) == LONG_HEADER_SIZE ) {
				completed = true;
			}
		}
	
		
		
		if( completed ) {
			if( reading_short_header ) {				
				message_length = short_header_buff.get( SS, 0 );
				
				System.out.println( "NOTE: short message length = " +message_length );
				
				if( message_length == 0xFF ) {  //long part of header still needs to be read
					reading_short_header = false;
					completed = false;
				}
				else {					
					message_id = short_header_buff.get( SS, 1 ) >>> 4;  //use leftmost 4 bits	
					message_sub_id = short_header_buff.get( SS, 1 ) & 0x0F;  //use rightmost 4 bits	
				}
			}
			else if( reading_handshake ) {
				long_header_buff.position( SS, 0 ); //rewind
				message_length = long_header_buff.getInt( SS );				
				
				System.out.println( "NOTE: handshake message length = " +message_length );				
				
				message_id = -1;  //negative to signify stream handshake
				
				reading_handshake = false;
				reading_short_header = true;
			}
			else {  //reading long part of header			
				message_length = (short_header_buff.get( SS, 1 ) << 20) |
												 (long_header_buff.get( SS, 0 )  << 12) |
												 (long_header_buff.get( SS, 1 )  << 4)  |
												 (long_header_buff.get( SS, 2 )  >>> 4);

				System.out.println( "NOTE: long message length = " +message_length );
				
				message_id = (long_header_buff.get( SS, 2 ) << 4) |
				             (long_header_buff.get( SS, 3 ) >> 4 );
				
				message_sub_id = long_header_buff.get( SS, 3 ) & 0x0F;		
				
				reading_short_header = true;
			}
		}

		
		if( completed ) {			
			short_header_buff.position( SS, 0 );  //reset buffer positions for next read round
			long_header_buff.position( SS, 0 );
		}
		
		return completed;
	}
	
	
	
	public int getMessageLength() {
		return message_length;
	}
	
	public int getMessageID() {
		return message_id;
	}
	
	public int getMessageSubID() {
		return message_sub_id;
	}

	
	public void destroy() {
		short_header_buff.returnToPool();
		long_header_buff.returnToPool();
	}
	
	
	
}
