/*
 * Created on Sep 9, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;

public class 
AEMemoryMonitor 
{
	private static final long MB = 1024*1024;

	private static long	max_heap_mb;
	
	protected static void
	initialise()
	{
		try{
			List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
		
			MemoryPoolMXBean 	pool_to_monitor = null;
			long				ptm_size		= 0;
			
			long	overall_max = 0;
			
			for ( MemoryPoolMXBean pool: pools ){
			
				long pool_max = pool.getUsage().getMax();
				
				if ( pool_max > 0 ){
					
					if ( pool.getType() == MemoryType.HEAP ){
					
						overall_max += pool_max;
					}
				}
				
				if ( pool.getType() == MemoryType.HEAP && pool.isCollectionUsageThresholdSupported()){
				
					long max = pool.getUsage().getMax();
					
					if ( max > ptm_size ){
						
						pool_to_monitor = pool;
						ptm_size		= max;
					}
				}
			}
			
			max_heap_mb = (overall_max+MB-1)/MB;

			if ( pool_to_monitor != null ){

				long max = pool_to_monitor.getUsage().getMax();
				
				long threshold = max*3/4;
				
				threshold = Math.min( threshold, 5*1024*1024 );
				
				MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();

				NotificationEmitter emitter = (NotificationEmitter) mbean;

				
				emitter.addNotificationListener(
					new NotificationListener()
					{
						private long	last_mb_log = Long.MAX_VALUE;
						
						private boolean increase_tried;
						
						public void 
						handleNotification(
							Notification 	notification, 
							Object 			handback ) 
						{
							MemoryPoolMXBean pool = (MemoryPoolMXBean)handback;

							long used 	= pool.getCollectionUsage().getUsed();
							long max 	= pool.getUsage().getMax();
							
							long avail = max-used;
							
							if ( avail < 0 ){
								avail = 0;
							}
							
							long	mb = (avail+MB-1)/(MB);
							
							if ( mb <= 4 ){
								
								synchronized( this ){
									
									if ( mb >= last_mb_log ){
										
										return;
									}
									
									last_mb_log = mb;
								}
								
								Runtime runtime = Runtime.getRuntime();
								
								Debug.out( "MemMon: notify triggered: pool=" + pool.getName() + 
											", used=" + used + 
											", max=" + max + 
											": runtime free=" + runtime.freeMemory() + ", tot=" + runtime.totalMemory() + ", max=" + runtime.maxMemory());

	 							Logger.logTextResource(
	 								new LogAlert(
	 									LogAlert.REPEATABLE, 
	 									LogAlert.AT_WARNING,
										"memmon.low.warning"), 
										new String[] {
	 										(mb==0?"< ":"") + DisplayFormatters.formatByteCountToKiBEtc( Math.max(1,mb)*MB, true ),
	 										DisplayFormatters.formatByteCountToKiBEtc( max_heap_mb*MB, true )});
	 							
	 							if ( mb == 1 && !increase_tried ){
	 								
	 								increase_tried = true;
	 							
	 								if ( COConfigurationManager.getBooleanParameter( "jvm.heap.auto.increase.enable", true )){

		 								PlatformManager platform = PlatformManagerFactory.getPlatformManager();
	
		 								if ( platform.hasCapability( PlatformManagerCapabilities.AccessExplicitVMOptions )){
		 									
		 									try{
		 										String[] options = platform.getExplicitVMOptions();
	
		 										long	max_mem = getJVMLongOption( options, "-Xmx" );
	
		 										if ( max_mem <= 0 ){
		 											
		 											max_mem = getMaxHeapMB()*MB;
		 										}
		 										
		 										final long HEAP_AUTO_INCREASE_MAX 	= 256*MB;
		 										final long HEAP_AUTO_INCREASE_BY	= 16*MB;
		 										
		 										if ( max_mem > 0 && max_mem < HEAP_AUTO_INCREASE_MAX ){
		 												 							 												
	 												max_mem += HEAP_AUTO_INCREASE_BY;
	 												
	 												if ( max_mem > HEAP_AUTO_INCREASE_MAX ){
	 													
	 													max_mem = HEAP_AUTO_INCREASE_MAX;
	 												}
	 												
	 												long	last_increase = COConfigurationManager.getLongParameter( "jvm.heap.auto.increase.last", 0 );
	 												
	 												if ( max_mem > last_increase ){
	 													
	 													COConfigurationManager.setParameter( "jvm.heap.auto.increase.last", max_mem );
	 															
		 												options = setJVMLongOption( options, "-Xmx", max_mem );
		 												
		 												platform.setExplicitVMOptions( options );
		 												
		 					 							Logger.logTextResource(
		 						 								new LogAlert(
		 						 									LogAlert.REPEATABLE, 
		 						 									LogAlert.AT_WARNING,
		 															"memmon.heap.auto.increase.warning"),
		 															new String[] {
		 					 											DisplayFormatters.formatByteCountToKiBEtc( max_mem, true )});
	 												}
	 											}
		 									
		 									}catch( Throwable e ){
		 										
		 										Debug.out( e );
		 									}
		 								}
	 								}
	 							}
							}
						}
					},
					null, pool_to_monitor );

				pool_to_monitor.setCollectionUsageThreshold( threshold );

			}
		}catch( Throwable e ){
		
			Debug.out( e );
		}
		
		if ( max_heap_mb == 0 ){
			
			max_heap_mb = ( Runtime.getRuntime().maxMemory()+MB-1)/MB;
		}
	}
	
	public static long
	getMaxHeapMB()
	{
		return( max_heap_mb );
	}
	
	public static  long
	getJVMLongOption(
		String[]	options,
		String		prefix )
	{		
		long	value = -1;
		
		for ( String option: options ){
			
			try{
				if ( option.startsWith( prefix )){
					
					String	val = option.substring( prefix.length());
					
					value = decodeJVMLong( val );
				}
			}catch( Throwable e ){
					
				Debug.out( "Failed to process option '" + option + "'", e );
			}
		}
		
		return( value );
	}
	
	public static  String[]
	setJVMLongOption(
		String[]	options,
		String		prefix,
		long		val )
	{
		String new_option = prefix + encodeJVMLong( val );
				
		for (int i=0;i<options.length;i++){
			
			String option = options[i];
			
			if ( option.startsWith( prefix )){
			
				options[i] = new_option;
				
				new_option = null;
			}
		}
		
		if ( new_option != null ){
		
			String[] new_options = new String[options.length+1];
		
			System.arraycopy( options, 0, new_options, 0, options.length );
			
			new_options[options.length] = new_option;
			
			options = new_options;
		}
		
		return( options );
	}
		
	public static  long
	decodeJVMLong(
		String		val )
	
		throws Exception
	{
		long	 mult = 1;
		
		char last_char = Character.toLowerCase( val.charAt( val.length()-1 ));
		
		if ( !Character.isDigit( last_char )){
			
			val = val.substring( 0, val.length()-1 );
			
			if ( last_char == 'k' ){
					
				mult	= 1024;
				
			}else if ( last_char == 'm' ){
				
				mult	= 1024*1024;
				
			}else if ( last_char == 'g' ){
				
				mult	= 1024*1024*1024;
				
			}else{
				
				throw( new Exception( "Invalid size unit '" + last_char + "'" ));
			}
		}
		
		return( Long.parseLong( val ) * mult );
	}
	
	public static String
	encodeJVMLong(
		long	val )
	{
		if ( val < 1024 ){
			
			return( String.valueOf( val ));
		}
		
		val = val/1024;
		
		if ( val < 1024 ){
			
			return( String.valueOf( val ) + "k" );
		}
		
		val = val/1024;
		
		if ( val < 1024 ){
			
			return( String.valueOf( val ) + "m" );
		}
		
		val = val/1024;
		
		return( String.valueOf( val ) + "g" );
	}
}
