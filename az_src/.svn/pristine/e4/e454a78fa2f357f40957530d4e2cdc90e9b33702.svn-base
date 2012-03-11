/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.updater2;

import org.gudy.azureus2.update.UpdaterUtils;

import com.aelitis.azureus.core.AzureusCore;

/**
 * @created Sep 6, 2007
 *
 */
public class PreUpdateChecker
{

	/**
	 * @param ui
	 *
	 * @since 3.0.2.3
	 */
	
	public static void 
	initialize(
		AzureusCore		core,
		String 			ui )
	{
		if ( ui.equals( "az3") && !"0".equals(System.getProperty("azureus.loadplugins"))) {
			
			/* EMP is no longer auto-install
			if ( UpdaterUtils.ensurePluginPresent(
					"azemp",
					"com.azureus.plugins.azemp.EmbeddedMediaPlayerPlugin",
					"Embedded Media Player" )){
			
					// rescan if we've done anything  
				
				core.getPluginManager().refreshPluginList();
			}
			*/
		}
	}
}
