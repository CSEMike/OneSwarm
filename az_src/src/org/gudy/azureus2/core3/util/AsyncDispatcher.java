/*
 * Created on 17 Jul 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

import java.util.LinkedList;

public class 
AsyncDispatcher 
{
	private AEThread2	thread;
	private LinkedList	queue 		= new LinkedList();
	private AESemaphore	queue_sem 	= new AESemaphore( "AsyncDispatcher" );
	
	private int quiesce_after_millis;
	
	public
	AsyncDispatcher()
	{
		this( 10000 );
	}
	
	public
	AsyncDispatcher(
		int	_quiesce_after_millis )
	{
		quiesce_after_millis	= _quiesce_after_millis;
	}
	
	public void
	dispatch(
		AERunnable	target )
	{
		synchronized( this ){
			
			queue.add( target );
			
			if ( thread == null ){
				
				thread = 
					new AEThread2( "AsyncDispatcher", true )
					{
						public void
						run()
						{
							while( true ){
								
								queue_sem.reserve( quiesce_after_millis );
								
								AERunnable	to_run = null;
								
								synchronized( AsyncDispatcher.this ){
									
									if ( queue.isEmpty()){
										
										thread = null;
										
										break;
									}
									
									to_run = (AERunnable)queue.removeFirst();
								}
								
								try{
									to_run.runSupport();
									
								}catch( Throwable e ){
									
									Debug.printStackTrace(e);
								}
							}
						}
					};
					
				thread.start();
			}
		}
		
		queue_sem.release();
	}
}
