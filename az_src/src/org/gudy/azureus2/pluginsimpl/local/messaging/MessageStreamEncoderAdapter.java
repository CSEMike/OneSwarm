/*
 * Created on Feb 21, 2005
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.messaging.MessageStreamEncoder;
import org.gudy.azureus2.plugins.network.RawMessage;
import org.gudy.azureus2.pluginsimpl.local.network.RawMessageAdapter;


/**
 *
 */
public class MessageStreamEncoderAdapter implements com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder {
  
  private final MessageStreamEncoder plug_encoder;
  
  
  public MessageStreamEncoderAdapter( MessageStreamEncoder plug_encoder ) {
    this.plug_encoder = plug_encoder;
  }
  
  public com.aelitis.azureus.core.networkmanager.RawMessage[] encodeMessage( com.aelitis.azureus.core.peermanager.messaging.Message message ) {
    Message plug_msg;
    
    if( message instanceof MessageAdapter ) {  //original message created by plugin, unwrap
      plug_msg = ((MessageAdapter)message).getPluginMessage();
    }
    else {
      plug_msg = new MessageAdapter( message );  //core created
    }
    
    RawMessage raw_plug = plug_encoder.encodeMessage( plug_msg );
    return new com.aelitis.azureus.core.networkmanager.RawMessage[]{ new RawMessageAdapter( raw_plug )};
  }
  

}
