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
 * BitTorrent piece message.
 */
public class AZSessionPiece implements AZMessage {
  private final byte version;
  private final DirectByteBuffer[] buffer = new DirectByteBuffer[ 2 ];
  private String description;
  
  private final int session_id;
  private final int piece_number;
  private final int piece_offset;
  private final int piece_length;
  
  
  public AZSessionPiece( int session_id, int piece_number, int piece_offset, DirectByteBuffer data, byte version ) {
    this.session_id = session_id;
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    this.piece_length = data == null ? 0 : data.remaining( DirectByteBuffer.SS_BT );
    buffer[1] = data;
    this.version = version;
  }
  
  
  public int getSessionID() {  return session_id;  }
  
  public int getPieceNumber() {  return piece_number;  }
  
  public int getPieceOffset() {  return piece_offset;  }
  
  public DirectByteBuffer getPieceData() {  return buffer[1];  }
  
  

  public String getID() {  return AZMessage.ID_AZ_SESSION_PIECE;  }
  public byte[] getIDBytes() {  return AZMessage.ID_AZ_SESSION_PIECE_BYTES;  }
  
  public String getFeatureID() {  throw new RuntimeException( "not implemented" );  }   //TODO  
  public int getFeatureSubID() {  throw new RuntimeException( "not implemented" );  }   //TODO
  
  public int getType() {  return Message.TYPE_DATA_PAYLOAD;  }
    
  public byte getVersion() { return version; };

  public String getDescription() {
    if( description == null ) {
      description = getID()+ " session #" +session_id+ " data for piece #" + piece_number + ":" + piece_offset + "->" + (piece_offset + piece_length -1);
    }
    
    return description;
  }
  
  
  public DirectByteBuffer[] getData() {
    if( buffer[0] == null ) {
      buffer[0] = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG, 12 );
      buffer[0].putInt( DirectByteBuffer.SS_MSG, session_id );
      buffer[0].putInt( DirectByteBuffer.SS_MSG, piece_number );
      buffer[0].putInt( DirectByteBuffer.SS_MSG, piece_offset );
      buffer[0].flip( DirectByteBuffer.SS_MSG );
    }
    
    return buffer;
  }
  
  
  
  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + "] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 12 ) {
      throw new MessageException( "[" +getID() + "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 12" );
    }
    
    
    int id = data.getInt( DirectByteBuffer.SS_MSG );
    
    int number = data.getInt( DirectByteBuffer.SS_MSG );
    if( number < 0 ) {
      throw new MessageException( "[" +getID() + "] decode error: number < 0" );
    }
    
    int offset = data.getInt( DirectByteBuffer.SS_MSG );
    if( offset < 0 ) {
      throw new MessageException( "[" +getID() + "] decode error: offset < 0" );
    }
    
    return new AZSessionPiece( id, number, offset, data, version );
  }
  
  
  public void destroy() {
    if( buffer[0] != null ) buffer[0].returnToPool();
    if( buffer[1] != null ) buffer[1].returnToPool();
  }
}
