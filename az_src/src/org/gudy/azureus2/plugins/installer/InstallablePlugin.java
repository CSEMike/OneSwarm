/*
 * Created on 30-Nov-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.plugins.installer;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author parg
 *
 */

public interface 
InstallablePlugin 
{
	public String
	getId();
	
	public String
	getVersion();
	
	public String
	getName();
	
	public String
	getDescription();
	
	public String
	getRelativeURLBase();
	
		/**
		 * Returns the plugin's interface if already installed, null if it isn't
		 * @return
		 */
	
	public PluginInterface
	getAlreadyInstalledPlugin();
	
	public boolean
	isAlreadyInstalled();
	
	public void
	install(
		boolean		shared )
	
		throws PluginException;
	
		/**
		 * Install with a few options to control process
		 * @param shared
		 * @param low_noise			don't prompt user
		 * @param wait_until_done	if true blocks until process complete, otherwise it is async
		 * @throws PluginException
		 * @since 3.1.1.1
		 */
	
	public void
	install(
		boolean		shared,
		boolean		low_noise,
		boolean		wait_until_done )
	
		throws PluginException;
	
		/**
		 * uninstall this plugin
		 * @throws PluginException
		 */

	public void
	uninstall()
	
		throws PluginException;
	
	public PluginInstaller
	getInstaller();
}
