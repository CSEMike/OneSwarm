/**
 * Created on May 27, 2008
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

package com.aelitis.azureus.ui.swt.utils;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author TuxPaper
 * @created May 27, 2008
 *
 */
public class UIMagnetHandler
{

	/**
	 * @param azureus_core
	 */
	public UIMagnetHandler(AzureusCore core) {
		int val = Integer.parseInt(Constants.getBaseVersion().replaceAll("\\.", ""));

		String ui = COConfigurationManager.getStringParameter("ui");
		if (!"az2".equals(ui)) {
			val += 10000;
		}

		MagnetURIHandler magnetURIHandler = MagnetURIHandler.getSingleton();
		magnetURIHandler.addInfo("get-version-info", val);
	}
}
