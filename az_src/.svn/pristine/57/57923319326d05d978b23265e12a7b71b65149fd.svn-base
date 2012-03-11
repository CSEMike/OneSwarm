/*
 * Created on 12 Apr 2008
 * Created by Allan Crooks
 * Copyright (C) 2008 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.pluginsimpl.local.ui;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.plugins.ui.UIMessage;

/**
 * @author Allan Crooks
 *
 */
public abstract class AbstractUIMessage implements UIMessage {

	protected int message_type = MSG_NONE;
	protected int input_type = INPUT_OK;
	protected String title = "";
	protected String[] messages = new String[0];

	public void setInputType(int input_type) {this.input_type = input_type;}
	public void setMessageType(int msg_type) {this.message_type = msg_type;}
	public void setLocalisedTitle(String title) {this.title = title;}
	public void setLocalisedMessage(String message) {setLocalisedMessages(new String[] {message});}
	public void setLocalisedMessages(String[] messages) {this.messages = messages;}
	public void setMessage(String message) {setLocalisedMessage(localise(message));}
	public void setTitle(String title) {setLocalisedTitle(localise(title));}
	
	protected final String messagesAsString() {
		if (messages.length == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer(messages[0]);
		for (int i=1; i<messages.length; i++) {
			sb.append("\n");
			sb.append(messages[i]);
		}
		return sb.toString();
	}

	public void setMessages(String[] messages) {
		String[] new_messages = new String[messages.length];
		for (int i=0; i<new_messages.length; i++) {
			new_messages[i] = this.localise(messages[i]);
		}
		this.setLocalisedMessages(new_messages);
	}
	
	private final String localise(String key) {
		return MessageText.getString(key); 
	}

	public int ask() {
		return 0;
	}

}
