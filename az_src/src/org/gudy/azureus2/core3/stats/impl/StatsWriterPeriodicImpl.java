/*
 * File    : StatsWriterPeriodicImpl.java
 * Created : 23-Oct-2003
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
 
package org.gudy.azureus2.core3.stats.impl;

import java.io.File;

import org.gudy.azureus2.core3.config.COConfigurationListener;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.stats.StatsWriterPeriodic;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.AzureusCore;
/**
 * @author parg
 */

public class 
StatsWriterPeriodicImpl 
	implements StatsWriterPeriodic, COConfigurationListener, TimerEventPerformer
{
	private static final LogIDs LOGID = LogIDs.CORE; 
	
	private static StatsWriterPeriodicImpl	singleton;
		
	private boolean started;
	
	private long			last_write_time	= 0;
	private AzureusCore		core;
	
	private TimerEventPeriodic event;
	private boolean			config_enabled;
	private int				config_period;	
	private String			config_dir;
	private String			config_file;
	
	public static synchronized StatsWriterPeriodic create(AzureusCore _core) {
		synchronized (StatsWriterPeriodicImpl.class)
		{
			if (singleton == null)
			{
				singleton = new StatsWriterPeriodicImpl(_core);
			}
			return (singleton);
		}
	}
	
	protected
	StatsWriterPeriodicImpl(
		AzureusCore		_core )
	{
		core	= _core;
	}
	

	public void perform(TimerEvent event) {
		update();
	}
	
	protected void
	update()
	{
		try {
			writeStats();
		} catch (Throwable e)
		{
			Debug.printStackTrace(e);
		}
	}
	
	protected synchronized void
	readConfigValues()
	{
		config_enabled 	= COConfigurationManager.getBooleanParameter( "Stats Enable" );
		
		config_period	= COConfigurationManager.getIntParameter( "Stats Period" );
		
		config_dir		= COConfigurationManager.getStringParameter( "Stats Dir" );
		
		config_file		= COConfigurationManager.getStringParameter( "Stats File" );
		
		if(config_enabled)
		{
			long targetFrequency = 1000 * (config_period < DEFAULT_SLEEP_PERIOD ? config_period : DEFAULT_SLEEP_PERIOD); 
			if(event != null && event.getFrequency() != targetFrequency)
			{
				event.cancel();
				event = null;
			}
			
			if(event == null)
				event = SimpleTimer.addPeriodicEvent("StatsWriter", targetFrequency, this);
			
		} else if(event != null)
		{
			event.cancel();
			event = null;
		}


		
	}
	
	protected void
	writeStats()
	{							
		if ( !config_enabled ){
			
			return;
		}

		int	period = config_period;
		
		long	now = SystemTime.getMonotonousTime() /1000;
        
		
			// if we have a 1 second period then now-last-write_time will often be 0 (due to the
			// rounding of SystemTime) and the stats won't be written - hence the check against
			// (period-1). Its only
		
		if ( now - last_write_time < ( period - 1 ) ){
			
			return;
		}
		
		last_write_time	= now;
		
		try{
			String	dir = config_dir;

			dir = dir.trim();
			
			if ( dir.length() == 0 ){
				
				dir = File.separator;			
			}
			
			String	file_name = dir;
			
			if ( !file_name.endsWith( File.separator )){
				
				file_name = file_name + File.separator;
			}
			
			String	file = config_file;

			if ( file.trim().length() == 0 ){
				
				file = DEFAULT_STATS_FILE_NAME;
			}
			
			file_name += file;
		
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Stats Logged to '" + file_name + "'"));				
			
			new StatsWriterImpl( core ).write( file_name );
			
		}catch( Throwable e ){
			Logger.log(new LogEvent(LOGID, "Stats Logging fails", e));
		}			
	}
	
	public void
	configurationSaved()
	{
			// only pick up configuration changes when saved
			
		readConfigValues();
		
		writeStats();
	}
	
	public void
	start()
	{
		if(started)
			return;
		started = true;
		COConfigurationManager.addListener( this );	
		configurationSaved();
	}
	
	public void
	stop()
	{
		COConfigurationManager.removeListener( this );	
		if(event != null)
			event.cancel();
	}

	

}
