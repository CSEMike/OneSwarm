/*
 * Created on Jun 4, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package org.gudy.azureus2.core3.util;

import org.gudy.azureus2.core3.security.SESecurityManager;

public class 
TimeLimitedTask 
{
	private String		name;
	private int			max_millis;
	private int			priority;
	private task		t;
	
	public
	TimeLimitedTask(
		String			_name,
		int				_max_millis,
		int				_priority,
		task			_t )
	{
		name		= _name;
		max_millis	= _max_millis;
		priority	= _priority;
		t			= _t;
	}
	
	public Object
	run()
	
		throws Throwable
	{
		final Object[]	result	= { null };
		
		final AESemaphore sem = new AESemaphore( name );
		
		final Thread thread = 
			new Thread( name )
			{
				public void
				run()
				{
					try{
						result[0] = t.run();
						
					}catch( Throwable e ){
						
						result[0] = e;
						
					}finally{
						
						sem.releaseForever();
					}
				}
			};
		
		DelayedEvent ev = 
			new DelayedEvent(
				name, 
				max_millis,
				new AERunnable()
				{
					public void
					runSupport()
					{
						if ( !sem.isReleasedForever()){
							
							SESecurityManager.stopThread( thread );
						}
					}
				});

		thread.setPriority( priority );
		
		thread.start();
		
		sem.reserve();
		
		ev.cancel();
		
		if ( result[0] instanceof Throwable ){
			
			throw((Throwable)result[0] );
		}
		
		return( result[0] );
	}
	
	public interface
	task
	{
		public Object
		run()
		
			throws Exception;
	}
}
