/*
 * Created on Oct 23, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.peermanager.control.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.peermanager.control.PeerControlInstance;
import com.aelitis.azureus.core.peermanager.control.SpeedTokenDispenser;
import com.aelitis.azureus.core.stats.AzureusCoreStatsProvider;

public class 
PeerControlSchedulerBasic
	extends PeerControlSchedulerImpl
	implements AzureusCoreStatsProvider
{
	private Random	random = new Random();
	
	private Map	instance_map = new HashMap();
	
	private List	pending_registrations = new ArrayList();
	
	private volatile boolean	registrations_changed;
	
	private volatile long		latest_time;
	
	protected AEMonitor	this_mon = new AEMonitor( "PeerControlSchedulerBasic" );
		
	private final SpeedTokenDispenserBasic tokenDispenser = new SpeedTokenDispenserBasic();

	private long	last_lag_log;
	
	protected void
	schedule()
	{
		latest_time	= SystemTime.getCurrentTime();
		
		SystemTime.registerConsumer(
			new SystemTime.TickConsumer()
			{
				public void
				consume(
					long	time )
				{
					synchronized( PeerControlSchedulerBasic.this ){
						
						latest_time	= time;
						
						PeerControlSchedulerBasic.this.notify();
					}
				}
			});
						
		
		List	instances = new LinkedList();

		long	latest_time_used	= 0;
		
		long	tick_count		= 0;
		long 	last_stats_time	= latest_time;
		
		while( true ){
			
			if ( registrations_changed ){
				
				try{
					this_mon.enter();
					
					Iterator	it = instances.iterator();
					
					while( it.hasNext()){
						
						if (((instanceWrapper)it.next()).isUnregistered()){
							
							it.remove();
						}
					}

					for (int i=0;i<pending_registrations.size();i++){
						
						instances.add( pending_registrations.get(i));
					}
					
					pending_registrations.clear();
					
					registrations_changed	= false;
					
				}finally{
					
					this_mon.exit();
				}	
			}
							
			for (Iterator it=instances.iterator();it.hasNext();){
				
				instanceWrapper	inst = (instanceWrapper)it.next();
									
				long	target = inst.getNextTick();
				
				long	diff = target - latest_time_used;			
				
				if ( diff <= 0 || diff > SCHEDULE_PERIOD_MILLIS ){
					
					tick_count++;
					
					inst.schedule( latest_time );
					
					schedule_count++;
					
					long new_target = target + SCHEDULE_PERIOD_MILLIS;
					
					diff = new_target - latest_time_used;
					
					if ( diff <= 0 || diff > SCHEDULE_PERIOD_MILLIS ){
						
						new_target = latest_time_used + SCHEDULE_PERIOD_MILLIS;
					}
					
					inst.setNextTick( new_target );
				}
			}
						
			synchronized( this ){
				
				if ( latest_time == latest_time_used ){
					
					wait_count++;
					
					try{
						long wait_start = SystemTime.getHighPrecisionCounter();
						
						wait();
						
						long wait_time 	= SystemTime.getHighPrecisionCounter() - wait_start;

						total_wait_time += wait_time;
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
					
				}else{
					
					yield_count++;
					
					Thread.yield();
				}
				
				latest_time_used	= latest_time;
			}
			
			long	stats_diff =  latest_time_used - last_stats_time;
			
			if ( stats_diff > 10000 ){
				
				// System.out.println( "stats: time = " + stats_diff + ", ticks = " + tick_count + ", inst = " + instances.size());
				
				last_stats_time	= latest_time_used;
				
				tick_count	= 0;
			}
		}
	}
	
	public void
	register(
		PeerControlInstance	instance )
	{
		instanceWrapper wrapper = new instanceWrapper( instance );
		
		wrapper.setNextTick( latest_time + random.nextInt( SCHEDULE_PERIOD_MILLIS ));
		
		try{
			this_mon.enter();
			
			Map	new_map = new HashMap( instance_map );
			
			new_map.put( instance, wrapper );
			
			instance_map = new_map;
			
			pending_registrations.add( wrapper );
			
			registrations_changed = true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	unregister(
		PeerControlInstance	instance )
	{
		try{
			this_mon.enter();
			
			Map	new_map = new HashMap( instance_map );
			
			instanceWrapper wrapper = (instanceWrapper)new_map.remove(instance);
			
			if ( wrapper == null ){
				
				Debug.out( "instance wrapper not found" );
				
				return;
			}
				
			wrapper.unregister();
			
			instance_map = new_map;
			
			registrations_changed = true;
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public SpeedTokenDispenser
	getSpeedTokenDispenser() 
	{
		return( tokenDispenser );
	}
	
	public void 
	updateScheduleOrdering() 
	{
	}
	
	protected class
	instanceWrapper
	{
		private PeerControlInstance		instance;
		private boolean					unregistered;
		
		private long					next_tick;
		
		private long					last_schedule;
		
		protected
		instanceWrapper(
			PeerControlInstance	_instance )
		{
			instance = _instance;
		}
		
		protected void
		unregister()
		{
			unregistered	= true;
		}
		
		protected boolean
		isUnregistered()
		{
			return( unregistered );
		}
		
		protected void
		setNextTick(
			long	t )
		{
			next_tick	= t;
		}
		
		protected long
		getNextTick()
		{
			return( next_tick );
		}
		
		protected void
		schedule(
			long	now )
		{
			if ( last_schedule > 0 ){
				
				if ( now - last_schedule > 1000 ){
					
					if ( now - last_lag_log > 1000 ){
						
						last_lag_log = now;
					
						System.out.println( "Scheduling lagging: " + (now - last_schedule ));
					}
				}
			}
			
			last_schedule = now;
			
			try{
				instance.schedule();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
}
