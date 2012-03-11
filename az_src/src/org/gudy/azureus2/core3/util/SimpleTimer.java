/*
 * Created on 12-Jul-2004
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

public class 
SimpleTimer 
{
		/**
		 * A simple timer class for use by application components that want to schedule 
		 * low-overhead events (i.e. when fired the event shouldn't take significant processing
		 * time as there is a limited thread pool to service it 
		 */
	
	protected static final Timer	timer;
	
	static{
		timer = new Timer("Simple Timer",32);
		
		timer.setIndestructable();
		
		timer.setWarnWhenFull();
		
		// timer.setLogCPU();
		
		// timer.setLogging(true);
	}
	
	public static TimerEvent
	addEvent(
		String				name,
		long				when,
		TimerEventPerformer	performer )
	{
		TimerEvent	res = timer.addEvent( name, when, performer );
				
		return( res );
	}
	
	public static TimerEvent
	addEvent(
		String				name,
		long				when,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		TimerEvent	res = timer.addEvent( name, when, absolute, performer );
				
		return( res );
	}
	
	public static TimerEventPeriodic
	addPeriodicEvent(
		String				name,
		long				frequency,
		TimerEventPerformer	performer )
	{
		TimerEventPeriodic	res = timer.addPeriodicEvent( name, frequency, performer );
				
		return( res );
	}
	
	public static TimerEventPeriodic
	addPeriodicEvent(
		String				name,
		long				frequency,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		TimerEventPeriodic	res = timer.addPeriodicEvent( name, frequency, absolute, performer );
				
		return( res );
	}
}
