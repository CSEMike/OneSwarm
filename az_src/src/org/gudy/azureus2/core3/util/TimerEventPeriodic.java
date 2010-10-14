/*
 * File    : TimerEventPeriodic.java
 * Created : 07-Dec-2003
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
public class 
TimerEventPeriodic
	implements TimerEventPerformer
{
	private Timer					timer;
	private long					frequency;
	private TimerEventPerformer	performer;
	
	private String				name;
	private TimerEvent			current_event;
	private boolean				cancelled;
	
	protected
	TimerEventPeriodic(
		Timer				_timer,
		long				_frequency,
		TimerEventPerformer	_performer )
	{
		timer		= _timer;
		frequency	= _frequency;
		performer	= _performer;
		
		current_event = timer.addEvent( 	SystemTime.getCurrentTime()+ frequency,
											this );
	}
	
	public void
	setName(
		String	_name )
	{
		name	= _name;
		
		synchronized( this ){
			
			if ( current_event != null ){
				
				current_event.setName( name );
			}
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	protected TimerEventPerformer
	getPerformer()
	{
		return( performer );
	}
	
	public long
	getFrequency()
	{
		return( frequency );
	}
	
	public boolean
	isCancelled()
	{
		return( cancelled );
	}
	
	public void
	perform(
		TimerEvent	event )
	{
		if ( !cancelled ){
			
			try{
				performer.perform( event );
				
			}catch( Throwable e ){
				
				DebugLight.printStackTrace( e );
			}
		
			synchronized( this ){
				
				if ( !cancelled ){
				
					current_event = timer.addEvent(name, SystemTime.getCurrentTime()+ frequency, this );	
				}
			}
		}
	}
	
	public synchronized void
	cancel()
	{
		if ( current_event != null ){
			
			current_event.cancel();
			
			cancelled	= true;
		}
	}
	
	protected String
	getString()
	{
		TimerEvent ce = current_event;
		
		String	ev_data;
		
		if ( ce == null ){
			
			ev_data = "?";
		}else{
			
			ev_data = "when=" + ce.getWhen() + ",run=" + ce.hasRun() + ", can=" + ce.isCancelled();
		}
		
		return( ev_data  + ",freq=" + getFrequency() + ",target=" + getPerformer() + (name==null?"":(",name=" + name )));
	}
}
