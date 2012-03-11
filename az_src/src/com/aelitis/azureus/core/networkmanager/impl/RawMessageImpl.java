/*
 * Created on Jan 11, 2005
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

package com.aelitis.azureus.core.networkmanager.impl;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;


/**
 *
 * Basic raw message implementation used internally for
 * Message-->RawMessage conversions.
 */
public class RawMessageImpl implements RawMessage {
  private final Message message;
  private final DirectByteBuffer[] payload;
  private final int priority;
  private boolean is_no_delay;
  private final Message[] to_remove;

  
  
  /**
   * Create a new raw message using the given parameters.
   * @param source original message
   * @param raw_payload headers + original message data
   * @param priority in queue
   * @param is_no_delay is an urgent message
   * @param to_remove message types to auto-remove upon queue
   */  
  public RawMessageImpl( Message source,
                            DirectByteBuffer[] raw_payload,
                            int _priority,
                            boolean _is_no_delay,
                            Message[] _to_remove ) {
    this.message = source;
    this.payload = raw_payload;
    this.priority = _priority;
    this.is_no_delay = _is_no_delay;
    this.to_remove = _to_remove;
  }
  
  //message impl
  public String getID() {  return message.getID();  }
  public byte[] getIDBytes() {  return message.getIDBytes();  }
   
  public String getFeatureID() {  return message.getFeatureID();  }  
  
  public int getFeatureSubID() {  return message.getFeatureSubID();  }
  
  public int getType() {  return message.getType();  }
  
  public byte getVersion() { return message.getVersion(); }
  
  public String getDescription() {  return message.getDescription();  }
  
  public DirectByteBuffer[] getData() {  return message.getData();  }
  
  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {
    return message.deserialize( data, version );
  }
  
  
  //rawmessage impl
  public DirectByteBuffer[] getRawData() {  return payload;  }
  
  public int getPriority() {  return priority;  }
  
  public boolean isNoDelay() {  return is_no_delay;  }
  
  public void setNoDelay() { is_no_delay = true; }
  
  public Message[] messagesToRemove() {  return to_remove;  }
  
  public Message getBaseMessage() {  return message;  }
  
  
  public void destroy() {
    //NOTE: Assumes that the raw payload is made up of the original
    //      message data buffers plus some header data, so returning
    //      the raw buffers will therefore also take care of the data
    //      buffers return.
    for( int i=0; i < payload.length; i++ ) {
      payload[i].returnToPool();
    }
  }
  
  
  
  /*
  public boolean equals( Object obj ) {
    //ensure we are comparing the underlying Message (and its equals() override if exists)
    if( obj instanceof RawMessage ) {
      obj = ((RawMessage)obj).getBaseMessage();
    }
    
    return message.equals( obj );
  }
  
  
  public int hashCode() {
    return message.hashCode();
  }
  */
  
}
