/*
 * File    : Timer.java
 * Created : 21-Nov-2003
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.lang.ref.WeakReference;
import java.util.*;

public class Timer
	extends 	AERunnable
	implements	SystemTime.ChangeListener
{
	private static boolean DEBUG_TIMERS = true;
	private static ArrayList timers = null;
	private static AEMonitor timers_mon = new AEMonitor("timers list");
	
	private ThreadPool	thread_pool;
		
	private Set	events = new TreeSet();
		
	private long	unique_id_next	= 0;
	
	private volatile boolean	destroyed;
	private boolean				indestructable;
	
	private boolean		log;
	private int			max_events_logged;
	
	public
	Timer(
		String	name )
	{
		this( name, 1 );
	}

	public
	Timer(
		String	name,
		int		thread_pool_size )
	{
		this(name, thread_pool_size, Thread.NORM_PRIORITY);
	}

	public
	Timer(
		String	name,
		int		thread_pool_size,
		int		thread_priority )
	{
		if (DEBUG_TIMERS) {
			try {
				timers_mon.enter();
				if (timers == null) {
					timers = new ArrayList();
					AEDiagnostics.addEvidenceGenerator(new evidenceGenerator()); 
				}
				timers.add(new WeakReference(this));
			} finally {
				timers_mon.exit();
			}
		}

		thread_pool = new ThreadPool(name,thread_pool_size);
	
		SystemTime.registerClockChangeListener( this );

		Thread t = new Thread(this, "Timer:" + name );
		
		t.setDaemon( true );
		
		t.setPriority(thread_priority);
			
		t.start();
	}
	
	public void
	setIndestructable()
	{
		indestructable	= true;
	}
	
	public synchronized List
	getEvents()
	{
		return( new ArrayList( events ));
	}
	public void
	setLogging(
		boolean	_log )
	{
		log	= _log;
	}
	
	public boolean getLogging() {
		return log;
	}
	
	public void
	setWarnWhenFull()
	{
		thread_pool.setWarnWhenFull();
	}
	
	public void
	setLogCPU()
	{
		thread_pool.setLogCPU();
	}
	
	public void
	runSupport()
	{
		while( true ){
			
			try{
				TimerEvent	event_to_run = null;
				
				synchronized(this){
					
					if ( destroyed ){
						
						break;
					}
					
					if ( events.isEmpty()){
						
						// System.out.println( "waiting forever" );
						
						this.wait();
						
					}else{
						long	now = SystemTime.getCurrentTime();
						
						TimerEvent	next_event = (TimerEvent)events.iterator().next();
						
						long	delay = next_event.getWhen() - now;
						
						if ( delay > 0 ){
							
							// System.out.println( "waiting for " + delay );
							
							this.wait(delay);
						}
					}
				
					if ( destroyed ){
						
						break;
					}
					
					long	now = SystemTime.getCurrentTime();
					
					Iterator	it = events.iterator();
					
					while( it.hasNext()){
						
						TimerEvent	event = (TimerEvent)it.next();
						
						if ( event.getWhen() <= now ){
							
							event_to_run = event;
							
							it.remove();
							
							break;
						}
					}
				}
				
				if ( event_to_run != null ){
					
					event_to_run.setHasRun();
					
					if (log) {
						System.out.println( "running: " + event_to_run.getString() );
					}
					
					thread_pool.run(event_to_run.getRunnable());
				}
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public void
	clockChanged(
		long	current_time,
		long	offset )
	{
		// System.out.println( "Timer '" + thread_pool.getName() +"': clock change by " + offset );
		  
		if ( Math.abs( offset ) >= 60*1000 ){
			
				// fix up the timers
			
			synchronized( this ){
				
				boolean resort = false;
				
				Iterator	it = events.iterator();
				
				while (it.hasNext()){
					
					TimerEvent	event = (TimerEvent)it.next();
					
						// absolute events don't have their timings fiddled with
					
					if ( event.isAbsolute()){
						
							// event ordering may change
						
						resort = true;
						
					}else{
						
						long	old_when = event.getWhen();
						long	new_when = old_when + offset;
						
						TimerEventPerformer performer = event.getPerformer();
						
							// sanity check for periodic events
						
						if ( performer instanceof TimerEventPeriodic ){
							
							TimerEventPeriodic	periodic_event = (TimerEventPeriodic)performer;
							
							long	freq = periodic_event.getFrequency();
													
							if ( new_when > current_time + freq + 5000 ){
								
								long	adjusted_when = current_time + freq;
								
								Debug.outNoStack( periodic_event.getName() + ": clock change sanity check. Reduced schedule time from " + new_when + " to " +  adjusted_when );
									
								new_when = adjusted_when;
							}
						}
						
						// don't wrap around by accident
						
						if ( old_when > 0 && new_when < 0 && offset > 0 ){

							// Debug.out( "Ignoring wrap around for " + event.getName());
							
						}else{
							
							// System.out.println( "    adjusted: " + old_when + " -> " + new_when );
						
							event.setWhen( new_when );
						}
					}
				}
				
				if ( resort ){
					
					events = new TreeSet( events );
				}

				notify();
			}
		}
	}
	
	public void
	adjustAllBy(
		long	offset )
	{
		// fix up the timers

		synchronized (this) {

			// as we're adjusting all events by the same amount the ordering remains valid

			Iterator it = events.iterator();

			while (it.hasNext()) {

				TimerEvent event = (TimerEvent) it.next();

				long old_when = event.getWhen();
				long new_when = old_when + offset;
				
					// don't wrap around by accident
				
				if ( old_when > 0 && new_when < 0 && offset > 0 ){

					// Debug.out( "Ignoring wrap around for " + event.getName());
					
				}else{
					
					// System.out.println( "    adjusted: " + old_when + " -> " + new_when );

					event.setWhen( new_when );
				}
			}

			notify();
		}
	}

	public synchronized TimerEvent
	addEvent(
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( SystemTime.getCurrentTime(), when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( name, SystemTime.getCurrentTime(), when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				when,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		return( addEvent( name, SystemTime.getCurrentTime(), when, absolute, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		long				creation_time,
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( null, creation_time, when, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		long				creation_time,
		long				when,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		return( addEvent( null, creation_time, when, absolute, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				creation_time,
		long				when,
		TimerEventPerformer	performer )
	{
		return( addEvent( name, creation_time, when, false, performer ));
	}
	
	public synchronized TimerEvent
	addEvent(
		String				name,
		long				creation_time,
		long				when,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		TimerEvent	event = new TimerEvent( this, unique_id_next++, creation_time, when, absolute, performer );
		
		if ( name != null ){
			
			event.setName( name );
		}
		
		events.add( event );
		
		if ( log ){
			
			if ( events.size() > max_events_logged ){
		
				max_events_logged = events.size();
				
				System.out.println( "Timer '" + thread_pool.getName() + "' - events = " + max_events_logged );
			}
		}
		
		// System.out.println( "event added (" + when + ") - queue = " + events.size());
				
		notify();
		
		return( event );
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		long				frequency,
		TimerEventPerformer	performer )
	{
		return( addPeriodicEvent( null, frequency, performer ));
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		String				name,
		long				frequency,
		TimerEventPerformer	performer )
	{
		return( addPeriodicEvent( name, frequency, false, performer ));
	}
	
	public synchronized TimerEventPeriodic
	addPeriodicEvent(
		String				name,
		long				frequency,
		boolean				absolute,
		TimerEventPerformer	performer )
	{
		TimerEventPeriodic periodic_performer = new TimerEventPeriodic( this, frequency, absolute, performer );
		
		if ( name != null ){
			
			periodic_performer.setName( name );
		}
		
		if ( log ){
						
			System.out.println( "Timer '" + thread_pool.getName() + "' - added " + periodic_performer.getString());
		}
		
		return( periodic_performer );
	}
	
	protected synchronized void
	cancelEvent(
		TimerEvent	event )
	{
		if ( events.contains( event )){
			
			events.remove( event );
		
			// System.out.println( "event cancelled (" + event.getWhen() + ") - queue = " + events.size());
	
			notify();
		}
	}
	
	public synchronized void
	destroy()
	{
		if ( indestructable ){
			
			Debug.out( "Attempt to destroy indestructable timer '" + getName() + "'" );
			
		}else{
			
			destroyed	= true;
			
			notify();
			
			SystemTime.unregisterClockChangeListener( this );
		}

		if (DEBUG_TIMERS) {
			try {
				timers_mon.enter();
				// crappy
				for (Iterator iter = timers.iterator(); iter.hasNext();) {
					WeakReference timerRef = (WeakReference) iter.next();
					Object timer = timerRef.get();
					if (timer == null || timer == this) {
						iter.remove();
					}
				}
			} finally {
				timers_mon.exit();
			}
		}
	}
	
	public String
	getName()
	{
		return( thread_pool.getName());
	}
	
	public synchronized void
	dump()
	{
		System.out.println( "Timer '" + thread_pool.getName() + "': dump" );

		Iterator	it = events.iterator();
		
		while(it.hasNext()){
			
			TimerEvent	ev = (TimerEvent)it.next();
			
			System.out.println( "\t" + ev.getString());
		}
	}

	private class 
	evidenceGenerator implements AEDiagnosticsEvidenceGenerator
	{
		public void generate(IndentWriter writer) {
			if (!DEBUG_TIMERS) {
				return;
			}

			ArrayList lines = new ArrayList();
			int count = 0;
			try {
				try {
					timers_mon.enter();
					// crappy
					for (Iterator iter = timers.iterator(); iter.hasNext();) {
						WeakReference timerRef = (WeakReference) iter.next();
						Timer timer = (Timer) timerRef.get();
						if (timer == null) {
							iter.remove();
						} else {
							count++;
							
							List	events = timer.getEvents();
							
							lines.add(timer.thread_pool.getName() + ", "
									+ events.size() + " events:");

							Iterator it = events.iterator();
							while (it.hasNext()) {
								TimerEvent ev = (TimerEvent) it.next();

								lines.add("  " + ev.getString());
							}
						}
					}
				} finally {
					timers_mon.exit();
				}

				writer.println("Timers: " + count);
				writer.indent();
				for (Iterator iter = lines.iterator(); iter.hasNext();) {
					String line = (String) iter.next();
					writer.println(line);
				}
				writer.exdent();
			} catch (Throwable e) {
				writer.println(e.toString());
			}
		}
	}
}
