/*
 * Created on Feb 11, 2005
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

package org.gudy.azureus2.pluginsimpl.local.network;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.network.RawMessage;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;

import com.aelitis.azureus.core.peermanager.messaging.Message;


/**
 *
 */
public class RawMessageAdapter extends MessageAdapter implements RawMessage, com.aelitis.azureus.core.networkmanager.RawMessage {
  private RawMessage plug_msg = null;
  private com.aelitis.azureus.core.networkmanager.RawMessage core_msg = null;
  
  
  public RawMessageAdapter( RawMessage plug_msg ) {
    super( plug_msg );
    this.plug_msg = plug_msg;
  }
  
  
  public RawMessageAdapter( com.aelitis.azureus.core.networkmanager.RawMessage core_msg ) {
    super( core_msg );
    this.core_msg = core_msg;
  }
  
  
  //plugin raw message implementation
  public ByteBuffer[] getRawPayload() {
    if( core_msg == null ) {
      return plug_msg.getRawPayload();
    }

    DirectByteBuffer[] dbbs = core_msg.getRawData();  
    ByteBuffer[] bbs = new ByteBuffer[ dbbs.length ];  //TODO cache it???
    for( int i=0; i < dbbs.length; i++ ) {
      bbs[i] = dbbs[i].getBuffer( DirectByteBuffer.SS_MSG );
    }
    return bbs;
  }
  
  
  //core raw message implementation
  public DirectByteBuffer[] getRawData() {
    if( plug_msg == null ) {
      return core_msg.getRawData();
    }
    
    ByteBuffer[] bbs = plug_msg.getRawPayload();
    DirectByteBuffer[] dbbs = new DirectByteBuffer[ bbs.length ];  //TODO cache it???
    for( int i=0; i < bbs.length; i++ ) {
      dbbs[i] = new DirectByteBuffer( bbs[i] );
    }
    return dbbs;
  }
  

  public int getPriority() {  return com.aelitis.azureus.core.networkmanager.RawMessage.PRIORITY_NORMAL;  }


  public boolean isNoDelay() {  return true;  }
 
  public void setNoDelay() {}

  public Message[] messagesToRemove() {  return null;  }
  
  
  public org.gudy.azureus2.plugins.messaging.Message getOriginalMessage() {
    if( plug_msg == null ) {
      return new MessageAdapter( core_msg.getBaseMessage() );
    }
    
    return plug_msg.getOriginalMessage();
  }
  
  
  public Message getBaseMessage() {
    if( core_msg == null ) {
      return new MessageAdapter( plug_msg.getOriginalMessage() );
    }
    
    return core_msg.getBaseMessage();
  }
  
}
