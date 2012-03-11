/*
 * Created on 11-Jul-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

public abstract class 
ThreadPoolTask
	extends AERunnable
{
	static final int RELEASE_AUTO = 0x00;
	static final int RELEASE_MANUAL = 0x01;
	static final int RELEASE_MANUAL_ALLOWED = 0x02;
	
	int manualRelease;
	
	protected ThreadPool.threadPoolWorker		worker;
	
	public void
	setTaskState(
		String		state )
	{
		worker.setState( state );
	}
	
	public String
	getTaskState()
	{
		return( worker == null ? "" : worker.getState());
	}
	
	public String
	getName()
	{
		return( null );
	}
	
	public abstract void
	interruptTask();
	
	public void
	taskStarted()
	{
	}
	
	public void
	taskCompleted()
	{
	}
	
	/**
	 * only invoke this method after the first run of the threadpooltask as it is only meant to join
	 * on a task when it has child tasks and thus is running in manual release mode
	 */
	synchronized void join()
	{
		while(manualRelease != RELEASE_AUTO)
		{
			try
			{
				wait();
			} catch (Exception e)
			{
				Debug.printStackTrace(e);
			}
		}
	}

	/**
	 * only invoke this method after the first run of the threadpooltask as it is only meant to
	 * update the state of a task when it has child tasks and thus is running in manual release mode
	 */
	synchronized boolean isAutoReleaseAndAllowManual()
	{
		if(manualRelease == RELEASE_MANUAL)
			manualRelease = RELEASE_MANUAL_ALLOWED;
		return manualRelease == RELEASE_AUTO;
	}
	
	public synchronized void releaseToPool()
	{
		// releasing before the initial run finished, so just let the runner do the cleanup
		if(manualRelease == RELEASE_MANUAL)
			manualRelease = RELEASE_AUTO;
		else if(manualRelease == RELEASE_MANUAL_ALLOWED)
		{
			taskCompleted();
			worker.getOwner().releaseManual(this);
			manualRelease = RELEASE_AUTO;
		} else if(manualRelease == RELEASE_AUTO)
			Debug.out("this should not happen");
		
		notifyAll();
	}
}
