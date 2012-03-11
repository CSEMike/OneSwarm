/*
 * Created on 17 Sep 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package com.aelitis.azureus.core.peermanager.messaging.bittorrent.ltep;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageDecoder;

/**
 * @author Allan Crooks
 *
 */
public class LTMessageDecoder extends BTMessageDecoder {
	
	private Map<Byte,byte[]>	entension_handlers = new HashMap<Byte, byte[]>();
	
	public LTMessageDecoder() {}

	protected Message createMessage(DirectByteBuffer ref_buff) throws MessageException {
		// Check to see if it is a LT-extension message. If not, delegate to BTMessageDecoder.
		int old_position = ref_buff.position(DirectByteBuffer.SS_MSG);
		byte id = ref_buff.get(DirectByteBuffer.SS_MSG);
		if (id != 20) {
			ref_buff.position(DirectByteBuffer.SS_MSG, old_position);
			return super.createMessage(ref_buff);
		}
		
		// Here is where we decode the message.
		id = ref_buff.get(DirectByteBuffer.SS_MSG);
		switch(id) {
			case 0:
				return MessageManager.getSingleton().createMessage(LTMessage.ID_LT_HANDSHAKE_BYTES, ref_buff, (byte)1);
			case 1:
				return MessageManager.getSingleton().createMessage(LTMessage.ID_UT_PEX_BYTES, ref_buff, (byte)1);
			case 3:
				return MessageManager.getSingleton().createMessage(LTMessage.ID_UT_METADATA_BYTES, ref_buff, (byte)1);
			default: {
			  byte[]	message_id;
			  synchronized( entension_handlers ){
					
				  message_id = entension_handlers.get( id );
			  }
			  
			  if ( message_id != null ){
				return MessageManager.getSingleton().createMessage( message_id, ref_buff, (byte)1);
			  }
		      System.out.println( "Unknown LTEP message id [" +id+ "]" );
		      throw new MessageException( "Unknown LTEP message id [" +id+ "]" );
			}
		}
	}
	
	public void
	addExtensionHandler(
		byte		id,
		byte[]		message_id )
	{
		synchronized( entension_handlers ){
		
			entension_handlers.put( id, message_id );
		}
	}
}
