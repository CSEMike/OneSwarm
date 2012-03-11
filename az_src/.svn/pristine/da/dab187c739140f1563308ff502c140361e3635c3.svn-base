/**
 * Created on Dec 30, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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
 
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.util.AERunnable;

/**
 * Delayed {@link Listener} that denies more triggers while one is pending
 * 
 * @author TuxPaper
 * @created Dec 30, 2010
 *
 */
public abstract class DelayedListenerMultiCombiner
	implements Listener
{
	Object lock = new Object();
	private boolean pending = false;

	public final void handleEvent(final Event event) {
		synchronized (lock) {
			if (pending) {
				return;
			}
			
			pending = true;
		}

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				try {
					handleDelayedEvent(event);
				} finally {
  				synchronized (lock) {
  					pending = false;
  				}
				}
			}
		});
	}

	public abstract void handleDelayedEvent(Event firstEvent);
}
