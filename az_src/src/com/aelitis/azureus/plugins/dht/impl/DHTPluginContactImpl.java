/*
 * Created on 14-Jun-2005
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.dht.impl;

import java.net.InetSocketAddress;
import java.util.Map;

import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;

public class
DHTPluginContactImpl
	implements DHTPluginContact
{
	private DHTPluginImpl		plugin;
	private DHTTransportContact	contact;
	
	protected
	DHTPluginContactImpl(
		DHTPluginImpl		_plugin,
		DHTTransportContact	_contact )
	{
		plugin	= _plugin;
		contact	= _contact;
	}
	
	protected DHTTransportContact
	getContact()
	{
		return( contact );
	}
	
	public String
	getName()
	{
		return( contact.getName());
	}
	
	public byte
	getProtocolVersion()
	{
		return( contact.getProtocolVersion());
	}

	public InetSocketAddress
	getAddress()
	{
		return( contact.getAddress());
	}
	
	public boolean
	isAlive(
		long		timeout )
	{
		return( contact.isAlive( timeout ));
	}
	
	public boolean
	isOrHasBeenLocal()
	{
		return( plugin.isRecentAddress( contact.getAddress().getAddress().getHostAddress()));
	}
	
	public Map
	openTunnel()
	{
		DHTNATPuncher puncher = plugin.getDHT().getNATPuncher();
		
		if ( puncher == null ){
			
			return( null );
		}
		
		return( puncher.punch( "Tunnel", contact, null, null ));
	}
}