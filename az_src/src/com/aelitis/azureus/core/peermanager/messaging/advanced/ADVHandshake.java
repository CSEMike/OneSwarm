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

package com.aelitis.azureus.core.peermanager.messaging.advanced;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 * BitTorrent handshake message.
 */
public class ADVHandshake implements ADVMessage {

  private byte version;
  private DirectByteBuffer buffer = null;
  private String description = null;
  
  
  
  public ADVHandshake( byte _version ) {  //TODO
	  version = _version;
    /*
    for( int i=0; i < reserved.length; i++ ) {  //locate any reserved bits
      for( int x=7; x >= 0; x-- ) {
        byte b = (byte) (reserved[i] >> x);
        int val = b & 0x01;
        if( val == 1 ) {
          String id = new String(peer_id);
          
          if( id.startsWith( "-AZ23" ) )  break;
          if( id.startsWith( "exbc" ) )  break;
          if( id.startsWith( "FUTB" ) )  break;
          
          System.out.println( "BT_HANDSHAKE:: reserved bit @ [" +i+ "/" +(7 - x)+ "] for [" +id+ "]" );
        }
      }
    }
    */
    
  }
  

  private void constructBuffer() {
    buffer = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_BT_HAND, 68 );
    //buffer.put( DirectByteBuffer.SS_MSG, (byte)PROTOCOL.length() );
    //buffer.put( DirectByteBuffer.SS_MSG, PROTOCOL.getBytes() );
    //buffer.put( DirectByteBuffer.SS_MSG, reserved_bytes );
    //buffer.put( DirectByteBuffer.SS_MSG, datahash_bytes );
    //buffer.put( DirectByteBuffer.SS_MSG, peer_id_bytes );
    //buffer.flip( DirectByteBuffer.SS_MSG );
  }
  
  
  

  

  // message
  public String getID() {  return ADVMessage.ID_ADV_HANDSHAKE;  }
  public byte[] getIDBytes() {  return ADVMessage.ID_ADV_HANDSHAKE_BYTES;  }
   
public String getFeatureID() {  return ADVMessage.ADV_FEATURE_ID;  } 
  
  public int getFeatureSubID() {  return ADVMessage.SUBID_ADV_HANDSHAKE;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public byte getVersion() { return version; };

  public String getDescription() {
    if( description == null ) {
      description = getID();
    }
    
    return description; 
  }
  
  
  public DirectByteBuffer[] getData() { 
    if( buffer == null ) {
      constructBuffer();
    }
    
    return new DirectByteBuffer[]{ buffer };
  }

  
  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {    
    if( data == null ) {
      throw new MessageException( "[" +getID() + "] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) != 68 ) {
      throw new MessageException( "[" +getID() + "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] != 68" );
    }
    
    
    byte[] reserved = new byte[ 8 ];
    data.get( DirectByteBuffer.SS_MSG, reserved );          
    
    byte[] infohash = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, infohash );
    
    byte[] peerid = new byte[ 20 ];
    data.get( DirectByteBuffer.SS_MSG, peerid );
    
    data.returnToPool();
    
    return new ADVHandshake(version);
  }
  
 
  public void destroy() {
    if( buffer != null )  buffer.returnToPool();    
  }
  
}
