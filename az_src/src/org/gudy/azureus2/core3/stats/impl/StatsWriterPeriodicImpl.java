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

import java.io.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.stats.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.util.AEThread;

import com.aelitis.azureus.core.AzureusCore;
/**
 * @author parg
 */

public class 
StatsWriterPeriodicImpl 
	implements StatsWriterPeriodic, COConfigurationListener
{
	private static final LogIDs LOGID = LogIDs.CORE; 
	
	private static StatsWriterPeriodicImpl	singleton;
	private static AEMonitor				class_mon	= new AEMonitor( "StatsWriterPeriodic" );
		
	private static int				start_count;
	private static Thread			current_thread;
	
	private long			last_write_time	= 0;
	private AzureusCore		core;
	
	private boolean			config_enabled;
	private int				config_period;	
	private String			config_dir;
	private String			config_file;
	
	public static StatsWriterPeriodic
	create(
		AzureusCore		_core )
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new StatsWriterPeriodicImpl(_core);
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected
	StatsWriterPeriodicImpl(
		AzureusCore		_core )
	{
		core	= _core;
		
		COConfigurationManager.addListener( this );	
	}
	
	protected void
	update()
	{
		readConfigValues();
		
		while( true ){
									
			try{
				class_mon.enter();
				
				if ( Thread.currentThread() != current_thread ){
					
					break;
				}
				
				writeStats();	
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e );
				
			}finally{
				
				class_mon.exit();
			}
			
			try{
				int	period;
				
				if ( !config_enabled ){
					
					period = DEFAULT_SLEEP_PERIOD;
								
				}else{
				
				 	period = config_period*1000;
				}
				
				if ( period > DEFAULT_SLEEP_PERIOD ){
					
					period = DEFAULT_SLEEP_PERIOD;
				}
				
				Thread.sleep( period );
				
			}catch( InterruptedException e ){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	protected void
	readConfigValues()
	{
		config_enabled 	= COConfigurationManager.getBooleanParameter( "Stats Enable" );
		
		config_period	= COConfigurationManager.getIntParameter( "Stats Period" );
		
		config_dir		= COConfigurationManager.getStringParameter( "Stats Dir" );
		
		config_file		= COConfigurationManager.getStringParameter( "Stats File" );
	}
	
	protected void
	writeStats()
	{							
		if ( !config_enabled ){
			
			return;
		}

		int	period = config_period;
		
		long	now = SystemTime.getCurrentTime() /1000;
        
        if( now < last_write_time ) {  //time went backwards
          last_write_time   = now;
        }
		
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
		try{
			class_mon.enter();
			
			start_count++;
			
			if ( start_count == 1 ){
							
				current_thread = 
					new AEThread("StatsWriter"){
						public void
						runSupport()
						{
							update();
						}
					};
					
				current_thread.setDaemon( true );
				
				current_thread.start();
			}
		}finally{
			
			class_mon.exit();
		}
	}
	
	public void
	stop()
	{
		try{
			class_mon.enter();
			
			start_count--;
			
			if ( start_count == 0 ){
				
				current_thread = null;
			}
		}finally{
			
			class_mon.exit();
		}
	}
	

}
