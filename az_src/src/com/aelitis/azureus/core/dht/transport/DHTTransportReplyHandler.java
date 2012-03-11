/*
 * Created on 12-Jan-2005
 * Created by Paul Gardner
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

package com.aelitis.azureus.core.dht.transport;

/**
 * @author parg
 *
 */

import java.util.*;

public interface 
DHTTransportReplyHandler 
{
	public void
	pingReply(
		DHTTransportContact contact,
		int					elapsed_time );
		
	public void
	statsReply(
		DHTTransportContact 	contact,
		DHTTransportFullStats	stats );

	public void
	storeReply(
		DHTTransportContact contact,
		byte[]				diversifications );
	
	public void
	queryStoreReply(
		DHTTransportContact contact,
		List<byte[]>		response );
	
	public void
	findNodeReply(
		DHTTransportContact 	contact,
		DHTTransportContact[]	contacts );
	
	public void
	findValueReply(
		DHTTransportContact 	contact,
		DHTTransportValue[]		values,
		byte					diversification_type,
		boolean					more_to_come );
	
	public void
	findValueReply(
		DHTTransportContact 	contact,
		DHTTransportContact[]	contacts );
	
	public void
	keyBlockReply(
		DHTTransportContact 	contact );
	
	public void
	keyBlockRequest(
		DHTTransportContact 	contact,
		byte[]					key,
		byte[]					key_signature );
	
	public void
	failed(
		DHTTransportContact 	contact,
		Throwable				error );

}
