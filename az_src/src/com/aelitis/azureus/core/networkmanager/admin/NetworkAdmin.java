/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin;

import java.net.InetAddress;

import org.gudy.azureus2.core3.util.IndentWriter;

import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminImpl;

public abstract class 
NetworkAdmin 
{
	private static NetworkAdmin	singleton;
	
	public static final String PR_NETWORK_INTERFACES	= "Network Interfaces";
	public static final String PR_DEFAULT_BIND_ADDRESS	= "Default Bind IP";
	public static final String PR_AS					= "AS";
	
	public static final String[]	PR_NAMES = {
		PR_NETWORK_INTERFACES,
		PR_DEFAULT_BIND_ADDRESS,
		PR_AS
	};
	
	public static synchronized NetworkAdmin
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new NetworkAdminImpl();
		}
		
		return( singleton );
	}
	
	public abstract InetAddress
	getSingleHomedServiceBindAddress();
	
	public abstract InetAddress[]
	getMultiHomedServiceBindAddresses();
	
	public abstract InetAddress
	getMultiHomedOutgoingRoundRobinBindAddress();
	
	public abstract String
	getNetworkInterfacesAsString();
	
	public abstract InetAddress[]
	getAllBindAddresses();

	public abstract NetworkAdminNetworkInterface[]
	getInterfaces();
	
	public abstract boolean
	hasIPV4Potential();
	
	public abstract boolean
	hasIPV6Potential();

	public abstract NetworkAdminProtocol[]
	getOutboundProtocols();
	
	public abstract NetworkAdminProtocol[]
	getInboundProtocols();
	
	public abstract InetAddress
	testProtocol(
		NetworkAdminProtocol	protocol )
	
		throws NetworkAdminException;
	
	public abstract NetworkAdminSocksProxy[]
	getSocksProxies();
	
	public abstract NetworkAdminHTTPProxy
	getHTTPProxy();
	
	public abstract NetworkAdminNATDevice[]
	getNATDevices();
	
		/**
		 * Only call if the supplied address is believed to be the current public address
		 * @param address
		 * @return
		 * @throws NetworkAdminException
		 */
		 
	public abstract NetworkAdminASN
	lookupCurrentASN(
		InetAddress		address )
	
		throws NetworkAdminException;
	
	public abstract NetworkAdminASN
	getCurrentASN();
	
		/**
		 * ad-hoc query
		 * @param address
		 * @return
		 * @throws NetworkAdminException
		 */
	
	public abstract NetworkAdminASN
	lookupASN(
		InetAddress		address )
	
		throws NetworkAdminException;
	public abstract boolean
	canTraceRoute();
	
	public abstract void
	getRoutes(
		final InetAddress					target,
		final int							max_millis,
		final NetworkAdminRoutesListener	listener )
	
		throws NetworkAdminException;
	
	public abstract boolean
	canPing();
	
	public abstract void
	pingTargets(
		final InetAddress					target,
		final int							max_millis,
		final NetworkAdminRoutesListener	listener )
	
		throws NetworkAdminException;
	
	public abstract void
	addPropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener );
	
	public abstract void
	addAndFirePropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener );

	public abstract void
	removePropertyChangeListener(
		NetworkAdminPropertyChangeListener	listener );
	
	public abstract void
	runInitialChecks();
	
	public abstract void
	logNATStatus(
		IndentWriter		iw );
	
	public abstract void
	generateDiagnostics(
		IndentWriter		iw );
}
