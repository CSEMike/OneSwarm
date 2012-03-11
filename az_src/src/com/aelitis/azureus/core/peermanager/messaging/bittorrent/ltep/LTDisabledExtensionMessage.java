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

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

/**
 * @author Allan Crooks
 *
 */
public class LTDisabledExtensionMessage implements LTMessage {
	
	public static LTDisabledExtensionMessage INSTANCE = new LTDisabledExtensionMessage();

	private LTDisabledExtensionMessage() {}

	public Message deserialize(DirectByteBuffer data, byte version) {
		return INSTANCE;
	}

	public void destroy() {}

	// Not meant to be used for outgoing messages, so raise an error if anyone tries to do it.
	public DirectByteBuffer[] getData() {
		throw new RuntimeException("Disabled extension message not meant to be used for serialisation!");
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.peermanager.messaging.Message#getDescription()
	 */
	public String getDescription() {
		return "Disabled extension message over LTEP (ignored)";
	}

	public String getFeatureID() {return LTMessage.LT_FEATURE_ID;}
	public int getFeatureSubID() {return LTMessage.SUBID_DISABLED_EXT;}
	public String getID() {return LTMessage.ID_DISABLED_EXT;}
	public byte[] getIDBytes() {return LTMessage.ID_DISABLED_EXT_BYTES;}
	public int getType() {return Message.TYPE_PROTOCOL_PAYLOAD;}
	public byte getVersion() {return 0;}

}
