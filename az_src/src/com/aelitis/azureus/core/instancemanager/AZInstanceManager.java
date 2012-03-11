/*
 * Created on 20-Dec-2005
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

package com.aelitis.azureus.core.instancemanager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.PatternSyntaxException;


public interface 
AZInstanceManager 
{
	public static final int	AT_TCP				= 1;
	public static final int	AT_UDP				= 2;
	public static final int	AT_UDP_NON_DATA		= 3;
	
	public void
	initialize();
	
	public boolean
	isInitialized();
	
	public AZInstance
	getMyInstance();
	
	public int
	getOtherInstanceCount();
	
	public AZInstance[]
	getOtherInstances();
	
	public void
	updateNow();
	
	public AZInstanceTracked[]
	track(
		byte[]								hash,
		AZInstanceTracked.TrackTarget		target );
	
	public InetSocketAddress
	getLANAddress(
		InetSocketAddress	external_address,
		int					address_type );
	
	public InetSocketAddress
	getExternalAddress(
		InetSocketAddress	lan_address,
		int					address_type );
	
	public boolean
	isLANAddress(
		InetAddress			address );
	
	public boolean
	isExternalAddress(
		InetAddress			address );
	
	public boolean
	addLANSubnet(
		String				subnet )
	
		throws PatternSyntaxException;
	
	public boolean
	getIncludeWellKnownLANs();
	
	public void
	setIncludeWellKnownLANs(
		boolean		include );
	
	public long
	getClockSkew();
	
	public boolean
	addInstance(
		InetAddress			explicit_address );
	
	public void
	addListener(
		AZInstanceManagerListener	l );
	
	public void
	removeListener(
		AZInstanceManagerListener	l );
}
