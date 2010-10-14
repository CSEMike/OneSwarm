/*
 * File    : GlobalManagerFactory.java
 * Created : 21-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.global;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.global.impl.*;

import com.aelitis.azureus.core.AzureusCore;


public class 
GlobalManagerFactory 
{
	/*
	public static GlobalManager
	create(
		AzureusCore						core,
		GlobalMangerProgressListener 	operation )
	{
		return( new GlobalManagerImpl( operation, 0 ));
	}
  
	public static GlobalManager
	create(
		AzureusCore						core )
	{
		return( new GlobalManagerImpl( null, 0 ));
	}
	*/
	public static GlobalManager
	create(
		AzureusCore						core,
		GlobalMangerProgressListener 	operation,
		long 							existingTorrentLoadDelay)
	{
		return( new GlobalManagerImpl( core, operation, existingTorrentLoadDelay ));
	}
  
	/*
	public static GlobalManager
	create(
		AzureusCore						core,
		long 							existingTorrentLoadDelay)
	{
		return( new GlobalManagerImpl( null, existingTorrentLoadDelay ));
	}
	*/
}
