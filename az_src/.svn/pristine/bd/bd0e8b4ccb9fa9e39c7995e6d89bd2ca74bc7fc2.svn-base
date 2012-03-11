/*
 * Created on 13-Jul-2004
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

package com.aelitis.azureus.core;

/**
 * @author parg
 *
 */

import com.aelitis.azureus.core.impl.*;

public class 
AzureusCoreFactory 
{
		/**
		 * Azureus core is a singleton that must be initially created by someone, and initialised
		 * @return
		 * @throws AzureusCoreException
		 */
	
	public static AzureusCore
	create()
	
		throws AzureusCoreException
	{
		return( AzureusCoreImpl.create());
	}
	
	/**
	 * Returns whether the core is available.  All features
	 * of the core (such as GlobalManager) may not be available yet.
	 * 
	 * @return
	 */
	public static boolean
	isCoreAvailable()
	{
		return( AzureusCoreImpl.isCoreAvailable());
	}
	
	/**
	 * Returns whether the core is running.  All features of the
	 * core (GlobalManager) should be available when the result
	 * is true.
	 * 
	 * @return
	 */
	public static boolean
	isCoreRunning() {
		return AzureusCoreImpl.isCoreRunning();
	}
		/**
		 * Once created the singleton can be accessed via this method
		 * @return
		 * @throws AzureusCoreException
		 */
	
	public static AzureusCore
	getSingleton()
	
		throws AzureusCoreException
	{
		return( AzureusCoreImpl.getSingleton());
	}	

	/**
	 * Adds a listener that is triggered once the core is running.
	 * <p>
	 * This is in AzureusCoreFactory instead of {@link AzureusCoreLifecycleListener}
	 * so that listeners can be added before the core instance is
	 * even created.
	 * 
	 * @param l Listener to trigger when the core is running.  If
	 *          the core is already running, listener is fired
	 *          immediately
	 */
	public static void
	addCoreRunningListener(AzureusCoreRunningListener l) {
		AzureusCoreImpl.addCoreRunningListener(l);
	}
}
