/*
 * Created on Jun 20, 2008
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


package org.gudy.azureus2.plugins.utils.search;

import java.util.Map;

public interface 
SearchProvider 
{
		// properties
	
	public static final int PR_ID							= 0;	// getProperty only; Long
	public static final int PR_NAME							= 1;	// mandatory; String
	public static final int PR_ICON_URL						= 2;	// optional; String
	public static final int PR_DOWNLOAD_LINK_LOCATOR		= 3;	// optional; String
	public static final int PR_REFERER						= 4;	// optional; String
	public static final int PR_SUPPORTS_RESULT_FIELDS		= 5;	// optional; int[]
	public static final int PR_USE_ACCURACY_FOR_RANK		= 6;	// optional; Boolean
	
		// search parameters
	
	public static final String	SP_SEARCH_TERM			 	= "s";	// String
	public static final String	SP_MATURE				 	= "m";	// Boolean
	
	public SearchInstance
	search(
		Map<String,Object>	search_parameters,
		SearchObserver		observer )
	
		throws SearchException;
	
	public Object
	getProperty(
		int			property );
	
	public void
	setProperty(
		int			property,
		Object		value );
}
