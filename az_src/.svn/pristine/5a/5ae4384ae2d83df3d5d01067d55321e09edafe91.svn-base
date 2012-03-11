/*
 * Created on Oct 24, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.clientmessageservice.impl;



public interface ClientMessageHandler {

	/**
	 * Get the message type id that this handler handles.
	 * @return
	 */
	public String getMessageTypeID();
	
	
	/**
	 * Process the given message received from a client.
	 * @param message from client to process
	 */
	public void processMessage( ClientMessage message );
	
	
	/**
	 * Notification of reply message send attempt completion.
	 * NOTE: This method will always be called once for every preceeding ClientMessageServer.sendReplyMessage() call.
	 * @param message sent
	 * @param success true if reply send was successful, false if reply send failed
	 */
	public void sendAttemptCompleted( ClientMessage message );
	
	public void sendAttemptFailed( ClientMessage message, Throwable error );
	
}
