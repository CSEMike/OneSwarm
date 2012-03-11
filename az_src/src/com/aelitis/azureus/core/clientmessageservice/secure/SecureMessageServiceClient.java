/*
 * Created on 03-Nov-2005
 * Created by Paul Gardner
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.clientmessageservice.secure;

import java.util.Map;

public interface 
SecureMessageServiceClient 
{
	public SecureMessageServiceClientMessage
	sendMessage(
		Map			request,
		Object		client_data,
		String		description );
	
		/**
		 * This shouldn't be required under normal circumstances as message addition causes
		 * dispatch and the server handles retries itself. However, sometimes it is necessary to
		 * force a dispatch to occur (e.g. to validate new authentication information immediately
		 * rather than wait for it to happen naturally)
		 */
	
	public void
	sendMessages();
	
	public SecureMessageServiceClientMessage[]
	getMessages();
	
	public void
	addListener(
		SecureMessageServiceClientListener	l );
	
	public void
	removeListener(
		SecureMessageServiceClientListener	l );
}
