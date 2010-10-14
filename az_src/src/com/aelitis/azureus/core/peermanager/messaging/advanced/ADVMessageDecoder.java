/*
 * Created on Feb 8, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.peermanager.messaging.*;


//based on http://82.182.115.6/extension.txt  //TODO

public class ADVMessageDecoder implements MessageStreamDecoder {
	private static final byte SS = DirectByteBuffer.SS_MSG;
	private static final int MAX_MESSAGE_LENGTH = 262144;  //256K arbitrary limit

	private DirectByteBuffer payload_buffer = null;
	private final ByteBuffer[] decode_array = new ByteBuffer[] { null, null };
	
	
	private final ADVHeaderReader header_reader = new ADVHeaderReader();
	
  
  
  private boolean reading_length_mode = true;
  
  
 
  private int message_length;
  private int pre_read_start_buffer;
  private int pre_read_start_position;
  
  private volatile boolean destroyed = false;
  private volatile boolean is_paused = false;
  
  private ArrayList messages_last_read = new ArrayList();
  private int protocol_bytes_last_read = 0;
  private int data_bytes_last_read = 0;

  private int data_bytes_owed = 0;
  private int percent_complete = -1;
  
  
  
  
  public ADVMessageDecoder() {
    /*nothing*/
  }
  
  
  
  public int performStreamDecode( Transport transport, int max_bytes ) throws IOException {
    protocol_bytes_last_read = 0;
    data_bytes_last_read = 0;
    
    int bytes_remaining = max_bytes;
    
    while( bytes_remaining > 0 ) {
      if( destroyed ) {
        Debug.out( "ADV decoder already destroyed: " +transport.getDescription() );
        break;
      }

      if( is_paused ) {
        Debug.out( "ADV decoder paused" );
        break;
      }
      
      int bytes_possible = preReadProcess( bytes_remaining );

      if( bytes_possible < 1 ) {
        Debug.out( "ERROR ADV: bytes_possible < 1" );
        break;
      }
      
      if( reading_length_mode ) {
        transport.read( decode_array, 1, 1 );  //only read into length buffer
      }
      else {
        transport.read( decode_array, 0, 2 );  //read payload buffer, and possibly next message length buffer
      }
      
      int bytes_read = postReadProcess();
      
      bytes_remaining -= bytes_read;
      
      if( bytes_read < bytes_possible ) {
        break;
      }
    }
            
    return max_bytes - bytes_remaining;
  }
  

  
  public int getPercentDoneOfCurrentMessage() {
    return percent_complete;
  }
  
  
  
  public Message[] removeDecodedMessages() {
    if( messages_last_read.isEmpty() )  return null;
    
    Message[] msgs = (Message[])messages_last_read.toArray( new Message[messages_last_read.size()] );
    messages_last_read.clear();
    
    return msgs;
  }
    
  

  public int getProtocolBytesDecoded() {  return protocol_bytes_last_read;  }
    
  
  
  public int getDataBytesDecoded() {  return data_bytes_last_read;  }


  public ByteBuffer destroy() {
    is_paused = true;
    destroyed = true;
    
    header_reader.destroy();
    
    if( payload_buffer != null ) {
      payload_buffer.returnToPool();
      payload_buffer = null;
    }
 
    for( int i=0; i < messages_last_read.size(); i++ ) {
      Message msg = (Message)messages_last_read.get( i );
      msg.destroy();
    }
    messages_last_read.clear();
    
    return null;  //NOTE: we don't bother returning any already-read data
  }
  
  
  
  
  
  private int preReadProcess( int allowed ) {
    if( allowed < 1 ) {
      Debug.out( "allowed < 1" );
    }
    
    decode_array[ 0 ] = payload_buffer == null ? null : payload_buffer.getBuffer( SS );  //ensure the decode array has the latest payload pointer
    decode_array[ 1 ] = header_reader.getReadBuffer();  //ensure it has the latest header buffer ref
    
    int bytes_available = 0;
    boolean shrink_remaining_buffers = false;
    int start_buff = reading_length_mode ? 1 : 0;
    boolean marked = false;    
    
    for( int i = start_buff; i < 2; i++ ) {  //set buffer limits according to bytes allowed
      ByteBuffer bb = decode_array[ i ];
      
      if( bb == null ) {
        Debug.out( "preReadProcess:: bb["+i+"] == null, decoder destroyed=" +destroyed );
      }
      
      if( shrink_remaining_buffers ) {
        bb.limit( 0 );  //ensure no read into this next buffer is possible
      }
      else {
        int remaining = bb.remaining();
        
        if( remaining < 1 )  continue;  //skip full buffer

        if( !marked ) {
          pre_read_start_buffer = i;
          pre_read_start_position = bb.position();
          marked = true;
        }

        if( remaining > allowed ) {  //read only part of this buffer
          bb.limit( bb.position() + allowed );  //limit current buffer
          bytes_available += bb.remaining();
          shrink_remaining_buffers = true;  //shrink any tail buffers
        }
        else {  //full buffer is allowed to be read
          bytes_available += remaining;
          allowed -= remaining;  //count this buffer toward allowed and move on to the next
        }
      }
    }
    
    return bytes_available;
  }
  
  

  
  private int postReadProcess() throws IOException {
    int bytes_read = 0;
    
    if( !reading_length_mode && !destroyed ) {  //reading payload data mode
      //ensure-restore proper buffer limits
      payload_buffer.limit( SS, message_length );
      
      bytes_read += payload_buffer.position( SS ) - pre_read_start_position;

      if( !payload_buffer.hasRemaining( SS ) && !is_paused ) {  //full message received!
        payload_buffer.position( SS, 0 );  //prepare for use

        DirectByteBuffer ref_buff = payload_buffer;
        payload_buffer = null;
        
        try {
        	
        	header_reader.getMessageID();
        	header_reader.getMessageSubID();
        	
          Message msg = null;//AZMessageFactory.createAZMessage( ref_buff );  //TODO
          if( false )  throw new MessageException( "" );  //TODO remove
          
          messages_last_read.add( msg );

          //we only learn what type of message it is AFTER we are done decoding it, so we probably need to work off the count post-hoc
          if( msg.getType() == Message.TYPE_DATA_PAYLOAD ) {
            data_bytes_owed += message_length;
          }
        }
        catch( MessageException me ) {
          ref_buff.returnToPool();
          throw new IOException( "ADV message decode failed: " + me.getMessage() );
        }
        
        reading_length_mode = true;  //see if we've already read the next message's length
        percent_complete = -1;  //reset receive percentage
      }
      else {  //only partial received so far       
        percent_complete = (payload_buffer.position( SS ) * 100) / message_length;  //compute receive percentage
      }
    }
    
    
    if( reading_length_mode && !destroyed ) {
      bytes_read += (pre_read_start_buffer == 1) ? decode_array[1].position() - pre_read_start_position : decode_array[1].position();
      
      if( header_reader.isHeaderReadComplete() ) {  //done reading the length and message id header
      	reading_length_mode = false;
      	message_length = header_reader.getMessageLength();      	
      	
      	if( message_length > MAX_MESSAGE_LENGTH ) {
          throw new IOException( "Invalid message length given for ADV message decode: " + message_length );
        }
        
        payload_buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_AZ_PAYLOAD, message_length );
      }      
    }
    
    if( bytes_read < data_bytes_owed ) {
      data_bytes_last_read += bytes_read;
      data_bytes_owed -= bytes_read;
    }
    else {  //bytes_read >= data_bytes_owed
      data_bytes_last_read += data_bytes_owed;
      data_bytes_owed = 0;
      
      protocol_bytes_last_read += bytes_read - data_bytes_owed;
    }

    return bytes_read;
  }
  
  

  public void pauseDecoding() {
    is_paused = true;
  }
  

  public void resumeDecoding() {
    is_paused = false;
  }
  
  
}
