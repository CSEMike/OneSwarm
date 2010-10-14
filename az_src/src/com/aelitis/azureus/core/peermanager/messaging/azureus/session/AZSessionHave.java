/*
 * Created on Apr 30, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.messaging.azureus.session;


import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessage;

/**
 * BitTorrent have message.
 */
public class AZSessionHave implements AZMessage {
  private final byte version;
  private DirectByteBuffer buffer = null;
  private String description = null;
  
  private final int session_id;
  private final int[] piece_numbers;

  
  public AZSessionHave( int session_id, int[] piece_numbers, byte version ) {
    this.session_id = session_id;
    this.piece_numbers = piece_numbers;
    this.version = version;
  }
  
  
  public int getSessionID() {  return session_id;  }
  
  public int[] getPieceNumbers() {  return piece_numbers;  }
  

  public String getID() {  return AZMessage.ID_AZ_SESSION_HAVE;  }
  public byte[] getIDBytes() {  return AZMessage.ID_AZ_SESSION_HAVE_BYTES;  }
  
  public String getFeatureID() {  throw new RuntimeException( "not implemented" );  }   //TODO  
  public int getFeatureSubID() {  throw new RuntimeException( "not implemented" );  }   //TODO
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }

  public byte getVersion() { return version; };

  
  public String getDescription() {
    if( description == null ) {
      String numbers = "";
      for( int i=0; i < piece_numbers.length; i++ ) {
        numbers += " #" +piece_numbers[i];
      }
      
      description = getID()+ " session #" +session_id+ " pieces" +numbers;
    }
    
    return description;
  }
  
  
  public DirectByteBuffer[] getData() {
    if( buffer == null ) {
      buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, 4 + (piece_numbers.length*4) );
      buffer.putInt( DirectByteBuffer.SS_MSG, session_id );
      
      for( int i=0; i < piece_numbers.length; i++ ) {
        buffer.putInt( DirectByteBuffer.SS_MSG, piece_numbers[i] );
      }
      
      buffer.flip( DirectByteBuffer.SS_MSG );
    }
    
    return new DirectByteBuffer[]{ buffer };
  }
  
  
  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() +"] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 8 ) {
      throw new MessageException( "[" +getID() +"] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 8" );
    }
    
    int id = data.getInt( DirectByteBuffer.SS_MSG );

    if( data.remaining( DirectByteBuffer.SS_MSG ) % 4 != 0 ) {
      throw new MessageException( "[" +getID() + "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] % 4 != 0" );
    }
    
    int[] numbers = new int[ data.remaining( DirectByteBuffer.SS_MSG ) / 4 ];
    
    for( int i=0; i < numbers.length; i++ ) {
      numbers[i] = data.getInt( DirectByteBuffer.SS_MSG );
      
      if( numbers[i] < 0 ) {
        throw new MessageException( "[" +getID() + "] decode error: number < 0" );
      }
    }

    data.returnToPool();
    
    return new AZSessionHave( id, numbers, version );
  }

  
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();
  }
  
}
