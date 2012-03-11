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

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTLTMessage;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author Allan Crooks
 *
 */
public class LTMessageEncoder implements MessageStreamEncoder {
	
	protected final static LogIDs LOGID = LogIDs.PEER;
	private Object log_object;
	private HashMap extension_map;
	
	public LTMessageEncoder(Object log_object) {
		this.log_object = log_object;
		this.extension_map = null; // Only instantiate it when we need to.
	}

	public RawMessage[] encodeMessage(Message message) {
		if (!(message instanceof LTMessage)) {
			return new RawMessage[] {BTMessageFactory.createBTRawMessage(message)};
		}

		// What type of message is it? LT_handshake messages are always straight forward.
		if (message instanceof LTHandshake) {
			return new RawMessage[] {BTMessageFactory.createBTRawMessage(new BTLTMessage(message, (byte)0))};
		}
		
		// Other message types have to be matched up against the appropriate ID.
		if (extension_map != null) {
			Byte ext_id = (Byte)this.extension_map.get(message.getID());
			if (ext_id != null) {
				//Logger.log(new LogEvent(this.log_object, LOGID,	"Converting LT message to BT message, ext id is " + ext_id));
				return new RawMessage[] {BTMessageFactory.createBTRawMessage(new BTLTMessage(message, ext_id.byteValue()))};
			}
		}
		
		// Anything else means that the client doesn't support that extension.
		// We'll drop the message instead.
		if (Logger.isEnabled())
			Logger.log(new LogEvent(this.log_object, LOGID,	"Unable to send LT message of type " + message.getID() + ", not supported by peer - dropping message."));
		
		return new RawMessage[0];
	}
	
	public void updateSupportedExtensions(Map map) {
		try {
			Iterator itr = map.entrySet().iterator();
			while (itr.hasNext()) {
				Map.Entry extension = (Map.Entry)itr.next();
				String ext_name;
				Object ext_key = extension.getKey();
				if (ext_key instanceof byte[]) {
					ext_name = new String((byte[])ext_key, Constants.DEFAULT_ENCODING);
				}
				else if (ext_key instanceof String) {
					ext_name = (String)ext_key;
				}
				else {
					throw new RuntimeException("unexpected type for extension name: " + ext_key.getClass());
				}

				int ext_value;
				Object ext_value_obj = extension.getValue();
				
				if (ext_value_obj instanceof Long) {
					ext_value = ((Long)extension.getValue()).intValue();
				}
				else if (ext_value_obj instanceof byte[]) {
					byte[] ext_value_bytes = (byte[])ext_value_obj;
					if (ext_value_bytes.length == 1) {
						ext_value = (int)ext_value_bytes[0];
					}
					else {
						throw new RuntimeException("extension id byte array format length != 1: " + ext_value_bytes.length);
					}
				}
				else {
					throw new RuntimeException("unsupported extension id type: " + ext_value_obj.getClass().getName());
				}
				if (extension_map == null) {
					this.extension_map = new HashMap();
				}
				
				if (ext_value == 0) {this.extension_map.remove(ext_name);}
				else {this.extension_map.put(ext_name, new Byte((byte)ext_value));}
			}
		}
		catch (Exception e) {
			if (Logger.isEnabled())
				Logger.log(new LogEvent(this.log_object, LOGID, "Unable to update LT extension list for peer", e));
		}
	}
	
	public boolean supportsUTPEX() {
		if (this.extension_map == null) {return false;}
		Number num = (Number)this.extension_map.get("ut_pex");
		
		return( num != null && num.intValue() != 0 );
	}

	public boolean supportsUTMetaData() {
		if (this.extension_map == null) {return false;}
		Number num = (Number)this.extension_map.get("ut_metadata");
		
		return( num != null && num.intValue() != 0 );
	}
}
