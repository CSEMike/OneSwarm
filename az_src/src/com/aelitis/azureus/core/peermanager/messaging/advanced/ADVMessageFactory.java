/*
 * Created on Feb 19, 2005
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

import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;




/**
 * Factory for handling ADV message creation.
 */
//based on http://82.182.115.6/extension.txt  //TODO

public class ADVMessageFactory {
  public static final byte MESSAGE_VERSION_INITIAL	= BTMessageFactory.MESSAGE_VERSION_INITIAL;
  
  private static final byte bss = DirectByteBuffer.SS_MSG;

  private static final Map legacy_data = new HashMap();
  static {
    legacy_data.put( BTMessage.ID_BT_CHOKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUnchoke((byte)0), new BTPiece(-1, -1, null, (byte)0 )} ) );
    legacy_data.put( BTMessage.ID_BT_UNCHOKE, new LegacyData( RawMessage.PRIORITY_NORMAL, true, new Message[]{new BTChoke((byte)0)} ) );
    legacy_data.put( BTMessage.ID_BT_INTERESTED, new LegacyData( RawMessage.PRIORITY_HIGH, true, new Message[]{new BTUninterested((byte)0)} ) );
    legacy_data.put( BTMessage.ID_BT_UNINTERESTED, new LegacyData( RawMessage.PRIORITY_NORMAL, false, new Message[]{new BTInterested((byte)0)} ) );
    legacy_data.put( BTMessage.ID_BT_HAVE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
    legacy_data.put( BTMessage.ID_BT_BITFIELD, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_REQUEST, new LegacyData( RawMessage.PRIORITY_NORMAL, true, null ) );
    legacy_data.put( BTMessage.ID_BT_PIECE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
    legacy_data.put( BTMessage.ID_BT_CANCEL, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_HANDSHAKE, new LegacyData( RawMessage.PRIORITY_HIGH, true, null ) );
    legacy_data.put( BTMessage.ID_BT_KEEP_ALIVE, new LegacyData( RawMessage.PRIORITY_LOW, false, null ) );
  }
  
  
  
  private static Message[] registered_messages;
  
  private static final HashMap mapping_table = new HashMap();
  
  
  
  /**
   * Initialize the factory, i.e. register the messages with the message manager.
   */
  public static void init() {
/*    try {

    }
    catch( MessageException me ) {  me.printStackTrace();  }
*/
  }
  
  
  /**
   * Register a generic map payload type with the factory.
   * @param type_id to register
   * @throws MessageException on registration error
   */
  //TODO plugin mapping
  private static void registerGenericMapPayloadMessageType( String type_id ) throws MessageException {
  	MessageManager.getSingleton().registerMessageType( new AZGenericMapPayload( type_id, null, MESSAGE_VERSION_INITIAL ) );
  }
  
  
  
  
  private static void refreshMappingTables() {
  	Message[] new_msgs = MessageManager.getSingleton().getRegisteredMessages();
  	
  	if( !Arrays.equals( registered_messages, new_msgs ) ) {  //cached table is out of date
  		registered_messages = new_msgs;
  		mapping_table.clear();
  		
  		for( int i=0; i < registered_messages.length; i++ ) {
  			
  			String feature_id = registered_messages[i].getFeatureID();
  			int sub_id = registered_messages[i].getFeatureSubID();
  			
  			
  			byte[] raw_message_id = (byte[])mapping_table.get( feature_id );
  			
  			if( raw_message_id == null ) {
  				raw_message_id = new byte[3];
  				mapping_table.put( feature_id, raw_message_id );
  			}
  			
  			if( raw_message_id.length != 3 )  Debug.out( "raw_message_id.length[" +raw_message_id.length+ "] != 3" );
  			
  			  			
  			
  			
  			
  			
  		}
  		
  	}
  }
  
  
  
  
  /**
   * Construct a new ADV message instance from the given message raw byte stream.
   * @param stream_payload data
   * @return decoded/deserialized ADV message
   * @throws MessageException if message creation failed.
   * NOTE: Does not auto-return given direct buffer on thrown exception.
   */
  public static Message createADVMessage( int id, int sub_id, DirectByteBuffer stream_payload ) throws MessageException {
    int id_length = stream_payload.getInt( bss );

    if( id_length < 1 || id_length > 1024 || id_length > stream_payload.remaining( bss ) - 1 ) {
      byte bt_id = stream_payload.get( (byte)0, 0 );
      throw new MessageException( "invalid ADV id length given: " +id_length+ ", stream_payload.remaining(): " +stream_payload.remaining( bss )+ ", BT id?=" +bt_id );
    }
    
    byte[] id_bytes = new byte[ id_length ];
    
    stream_payload.get( bss, id_bytes );
    
    byte version = stream_payload.get( bss );
    
    return MessageManager.getSingleton().createMessage( id_bytes, stream_payload, version );
  }
  
  
  
  
  /**
   * Create the proper ADV raw message from the given base message.
   * @param base_message to create from
   * @return ADV raw message
   */
  public static RawMessage createADVRawMessage( Message base_message ) {
    byte[] id_bytes = base_message.getID().getBytes();
    DirectByteBuffer[] payload = base_message.getData();
    
    int payload_size = 0;
    for( int i=0; i < payload.length; i++ ) {
      payload_size += payload[i].remaining( bss );
    }
    
    //create and fill header buffer
    DirectByteBuffer header = DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_MSG_AZ_HEADER, 9 + id_bytes.length );
    header.putInt( bss, 5 + id_bytes.length + payload_size );
    header.putInt( bss, id_bytes.length );
    header.put( bss, id_bytes );
    //header.put( bss, base_message.getVersion() );
    header.flip( bss );
    
    DirectByteBuffer[] raw_buffs = new DirectByteBuffer[ payload.length + 1 ];
    raw_buffs[0] = header;
    for( int i=0; i < payload.length; i++ ) {
      raw_buffs[i+1] = payload[i];
    }
     
    LegacyData ld = (LegacyData)legacy_data.get( base_message.getID() );  //determine if a legacy BT message
    
    if( ld != null ) {  //legacy message, use pre-configured values
      return new RawMessageImpl( base_message, raw_buffs, ld.priority, ld.is_no_delay, ld.to_remove );
    }
    
    //standard message, ensure that protocol messages have wire priority over data payload messages
    int priority = base_message.getType() == Message.TYPE_DATA_PAYLOAD ? RawMessage.PRIORITY_LOW : RawMessage.PRIORITY_NORMAL;
    
    return new RawMessageImpl( base_message, raw_buffs, priority, true, null );
  }
  
  
  
  
  
  
  protected static class LegacyData {
  	protected final int priority;
    protected final boolean is_no_delay;
    protected final Message[] to_remove;
    
    protected LegacyData( int prio, boolean no_delay, Message[] remove ) {
      this.priority = prio;
      this.is_no_delay = no_delay;
      this.to_remove = remove;
    }
  }
  
}
