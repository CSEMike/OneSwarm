/*
 * Created on Sep 9, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.custom;

import java.io.File;
import java.io.InputStream;

public interface 
Customization 
{
	public static final String RT_META_SEARCH_TEMPLATES	= "metasearch";	// InputStream[]
	public static final String RT_SUBSCRIPTIONS			= "subs";		// InputStream[]
	public static final String RT_CNETWORKS				= "cnetworks";	// InputStream[]
	
	public String
	getName();
	
	public String
	getVersion();
	
	public Object
	getProperty(
		String		name );
	
	public boolean
	isActive();
	
	public void
	setActive(
		boolean		active );
	
	public InputStream
	getResource(
		String		resource_name );
	
	public InputStream[]
	getResources(
		String		resource_name );
	
	public void
	exportToVuzeFile(
		File		file )
	
		throws CustomizationException;
}
