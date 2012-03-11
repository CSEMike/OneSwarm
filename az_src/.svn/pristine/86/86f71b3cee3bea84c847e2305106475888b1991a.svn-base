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
package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

/**
 * @author Allan Crooks
 *
 */
public class BTLTMessage implements BTMessage {
	
	public byte extension_id;
	public Message base_message;
	public DirectByteBuffer buffer_header;
	
	public BTLTMessage(Message base_message, byte extension_id) {
		this.base_message = base_message;
		this.extension_id = extension_id;
	}

	// This class should not be used for deserialisation!
	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		throw new MessageException("BTLTMessage cannot be used for message deserialization!");
	}

	public void destroy() {
		if (base_message != null) {base_message.destroy();}
		if (buffer_header != null) {
			buffer_header.returnToPool();
			buffer_header = null;
		}
	}

	public DirectByteBuffer[] getData() {
		DirectByteBuffer[] orig_data = this.base_message.getData();
		DirectByteBuffer[] new_data = new DirectByteBuffer[orig_data.length + 1];

		if (buffer_header == null ) {
			buffer_header = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG_LT_EXT_MESSAGE, 1);
			buffer_header.put(DirectByteBuffer.SS_MSG, this.extension_id);
			buffer_header.flip(DirectByteBuffer.SS_MSG);
		}
		
		new_data[0] = buffer_header;
		System.arraycopy(orig_data, 0, new_data, 1, orig_data.length);
		return new_data;
	}

	public String getDescription() {
		return base_message.getDescription();
	}

	public String getFeatureID() {
		return base_message.getFeatureID();
	}

	public int getFeatureSubID() {
		return base_message.getFeatureSubID();
	}

	public String getID() {
		return BTMessage.ID_BT_LT_EXT_MESSAGE;
	}

	public byte[] getIDBytes() {
		return BTMessage.ID_BT_LT_EXT_MESSAGE_BYTES;
	}

	public int getType() {
		return Message.TYPE_PROTOCOL_PAYLOAD;
	}

	public byte getVersion() {
		return 0;
	}

}
