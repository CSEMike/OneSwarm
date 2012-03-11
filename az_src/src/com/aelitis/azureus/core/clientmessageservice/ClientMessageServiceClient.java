/*
 * Created on Oct 31, 2005
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
package com.aelitis.azureus.core.clientmessageservice;


import com.aelitis.azureus.core.clientmessageservice.impl.AEClientService;

/**
 * 
 */
public class ClientMessageServiceClient {

	/**
	 * Create a new message server service connection.
	 * @param server_address of service
	 * @param server_port of service
	 * @param message type id to use for messages
	 * @return server service connection
	 */
	public static ClientMessageService getServerService( String server_address, int server_port, int timeout_secs, String msg_type_id ) {
		return new AEClientService( server_address, server_port, timeout_secs, msg_type_id );
	}
	
	public static ClientMessageService getServerService( String server_address, int server_port, String msg_type_id ) {
		return new AEClientService( server_address, server_port, msg_type_id );
	}
}
