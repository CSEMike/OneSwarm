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

package com.aelitis.azureus.core.peermanager.messaging.bittorrent;


import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;


/**
 * BitTorrent uninterested message.
 */
public class BTUninterested implements BTMessage {
  private final byte version;
  
  public BTUninterested(byte _version) {
    version = _version;
  }
    
  public String getID() {  return BTMessage.ID_BT_UNINTERESTED;  }
  public byte[] getIDBytes() {  return BTMessage.ID_BT_UNINTERESTED_BYTES;  }
  
  public String getFeatureID() {  return BTMessage.BT_FEATURE_ID;  } 
  
  public int getFeatureSubID() {  return BTMessage.SUBID_BT_UNINTERESTED;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public byte getVersion() { return version; };

  public String getDescription() {  return BTMessage.ID_BT_UNINTERESTED;  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[] {};  }
  
  public Message deserialize( DirectByteBuffer data, byte version ) throws MessageException {    
    if( data != null && data.hasRemaining( DirectByteBuffer.SS_MSG ) ) {
      throw new MessageException( "[" +getID() +"] decode error: payload not empty" );
    }
    
    if( data != null )  data.returnToPool();
    
    return new BTUninterested(version);
  }
  
  public void destroy() { /*nothing*/ } 
}
