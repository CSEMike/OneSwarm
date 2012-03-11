/*
 * Created on 14-Feb-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.networkmanager;

import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.networkmanager.impl.tcp.VirtualBlockingServerChannelSelector;
import com.aelitis.azureus.core.networkmanager.impl.tcp.VirtualNonBlockingServerChannelSelector;

public class 
VirtualServerChannelSelectorFactory 
{
	public static VirtualServerChannelSelector
	createBlocking(
		InetSocketAddress 								bind_address, 
		int 											so_rcvbuf_size, 
		VirtualServerChannelSelector.SelectListener 	listener )
	{
		return( new VirtualBlockingServerChannelSelector( bind_address, so_rcvbuf_size, listener ));
	}
	
	public static VirtualServerChannelSelector
	createNonBlocking(
		InetSocketAddress 								bind_address, 
		int 											so_rcvbuf_size, 
		VirtualServerChannelSelector.SelectListener 	listener )
	{
		return( new VirtualNonBlockingServerChannelSelector( bind_address, so_rcvbuf_size, listener ));
	}
	
	public static VirtualServerChannelSelector
	createTest(
		InetSocketAddress 								bind_address, 
		int 											so_rcvbuf_size, 
		VirtualServerChannelSelector.SelectListener 	listener )
	{
			// test param to allow multiple ports to be created
		
		 int	range = COConfigurationManager.getIntParameter( "TCP.Listen.Port.Range", -1 );

		 if ( range == -1 ){
			
			 return( createBlocking( bind_address, so_rcvbuf_size, listener ));
			 
		 }else{
			
			 return( new VirtualNonBlockingServerChannelSelector( 
					 		bind_address.getAddress(),
					 		bind_address.getPort(), 
					 		range,
					 		so_rcvbuf_size, listener ));
		 }
	}
}
