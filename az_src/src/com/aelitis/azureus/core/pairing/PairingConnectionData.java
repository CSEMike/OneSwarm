/*
 * Created on Oct 5, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.pairing;

public interface 
PairingConnectionData 
{
	public static final String ATTR_IP_V4			= "ip4";
	public static final String ATTR_IP_V6			= "ip6";
	public static final String ATTR_PORT			= "port";
	public static final String ATTR_PORT_OVERRIDE	= "port_or";
	public static final String ATTR_PROTOCOL		= "protocol";
	public static final String ATTR_HOST			= "host";
	
	public void
	setAttribute(
		String		name,
		String		value );
	
	public String
	getAttribute(
		String		name );
	
	public void
	sync();
}
