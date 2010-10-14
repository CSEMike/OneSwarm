/*
 * Created on 25-Jul-2005
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

package org.gudy.azureus2.plugins;

public interface 
LaunchablePlugin
	extends Plugin
{
		/**
		 * Invoked before azureus core is started to set defaults such as application details
		 */
	
	public void
	setDefaults(
		String[]	args );
	
		/**
		 * Some environments have requirements regarding the main application thread and processing
		 * on it. In particular SWT on OSX insists that the SWT dispatch thread. When this thread
		 * is returned from Azureus is either stopper or restarted, depending on the return value
		 * @return whether to close down or restart (true->restart) 
		 */
	
	public boolean
	process();
}
