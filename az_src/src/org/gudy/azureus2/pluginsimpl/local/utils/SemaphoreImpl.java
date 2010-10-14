/*
 * File    : SemaphoreImpl.java
 * Created : 24-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.core3.util.AESemaphore;

public class
SemaphoreImpl
	implements Semaphore
{
	private static long	next_sem_id;

	private AESemaphore		sem;
	
	protected
	SemaphoreImpl(
		PluginInterface		pi )
	{
		synchronized( SemaphoreImpl.class ){
			
			sem	= new AESemaphore("Plugin " + pi.getPluginID() + ":" + next_sem_id++ );
		}
	}
	
	public void
	reserve()
	{
		sem.reserve();
	}
	
	public boolean
	reserveIfAvailable()
	{
		return( sem.reserveIfAvailable());
	}
	
	public boolean
	reserve(
		long	timeout_millis )
	{
		return( sem.reserve( timeout_millis ));
	}
	
	public void
	release()
	{
		sem.release();
	}
	
	public void
	releaseAllWaiters() {
		sem.releaseAllWaiters();
	}
	
}
