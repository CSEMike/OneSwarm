/*
 * Created on 14-Jun-2004
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

package com.aelitis.net.upnp;

/**
 * @author parg
 *
 */

import com.aelitis.net.upnp.impl.*;
import com.aelitis.net.upnp.impl.ssdp.SSDPCore;


public class 
UPnPFactory 
{
	public static UPnP
	getSingleton(
		UPnPAdapter		adapter,
		String[]		selected_interfaces )
	
		throws UPnPException
	{
		return( UPnPImpl.getSingleton( adapter, selected_interfaces ));
	}
	
	public static UPnPSSDP
	getSSDP(
		UPnPSSDPAdapter		adapter,
		String				group_address,
		int					group_port,
		int					control_port,
		String[]			selected_interfaces )
	
		throws UPnPException
	{
		return( SSDPCore.getSingleton( adapter, group_address, group_port, control_port, selected_interfaces ));
	}
}
