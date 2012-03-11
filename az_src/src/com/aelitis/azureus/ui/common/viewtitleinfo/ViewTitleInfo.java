/**
 * Created on Jul 8, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */
 
package com.aelitis.azureus.ui.common.viewtitleinfo;


/**
 * @author TuxPaper
 * @created Jul 8, 2008
 *
 */
public interface ViewTitleInfo
{
	public static int TITLE_TEXT = 5;

	public static int TITLE_INDICATOR_TEXT = 0;
	
	public static int TITLE_INDICATOR_COLOR = 8;

	
	public static int TITLE_ACTIVE_STATE = 9;	// -> Long: 0 - not supported; 1 - active; 2 - inactive

	public static int TITLE_INDICATOR_TEXT_TOOLTIP = 1;
	
	public static int TITLE_IMAGEID = 2;
	
	public static int TITLE_IMAGE_TOOLTIP = 3;

	public static int TITLE_LOGID	= 7;
	

	/**
	 * 
	 * @param propertyID TITLE_*
	 * @return value, or null if you don't want to set it
	 *
	 * @since 3.1.1.1
	 */
	public Object getTitleInfoProperty(int propertyID);
}
