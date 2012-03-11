/*
 * Created on 05-Nov-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

public class 
AEThreadMonitor 
{
	private static boolean disable_getThreadCpuTime = false; 
	public static void
	initialise()
	{	
		String	java_version = (String)System.getProperty("java.runtime.version");

		// getThreadCpuTime crashes on OSX with 1.5.0_06
		disable_getThreadCpuTime = Constants.isOSX
				&& java_version.startsWith("1.5.0_06");
				
		AEDiagnostics.addEvidenceGenerator(new EvidenceGenerateor());
		
		if ( !disable_getThreadCpuTime ){
			
			AEThread	thread = 
				new AEThread( "AEThreadMonitor" )
				{
					public void
					runSupport()
					{
						try{
							try{
								Class.forName( "java.lang.management.ManagementFactory" );
								
								monitor15();
								
							}catch( Throwable e ){
								
								//monitor14();
							}
							
						}catch( Throwable e ){
							
						}
					}
				};
			
			thread.setPriority( Thread.MAX_PRIORITY );
			
			thread.setDaemon( true );
			
			thread.start();
			
			/*
			new AEThread( "parp", true )
			{
				public void
				runSupport()
				{
					try{
						while( true ){
							//Thread.sleep(1);
						}
						
					}catch( Throwable e ){
						
					}
				}			
			}.start();
			*/
		}
	}
	
	private static void
	monitor14()
	{
		AEDiagnosticsLogger log = AEDiagnostics.getLogger( "thread" );

		int num_processors = Runtime.getRuntime().availableProcessors();

		if ( num_processors < 1 ){
			
			num_processors = 1;
		}
		
		log.log( "Monitoring starts [1.4] (processors =" + num_processors + ")" );

		while( true ){
						
			try{
				
				Thread.sleep(10000);
				
			}catch( Throwable e ){
				
				log.log(e);
			}
			
			try{
				ThreadGroup	group = Thread.currentThread().getThreadGroup();
				
				Thread[]	threads = new Thread[group.activeCount()];
				
				group.enumerate( threads );
				
				for (int i=0;i<threads.length;i++){
					
					Thread	t = threads[i];
					
					if ( t != null ){
						
						if ( t.getName().equals( "Thread-2")){
							
							t.dumpStack();
						}
					}
				}
			}catch( Throwable e ){
				
			}
		}
	}
	
	private static void
	monitor15()
	{
		AEDiagnosticsLogger log = AEDiagnostics.getLogger( "thread" );

		int num_processors = Runtime.getRuntime().availableProcessors();

		if ( num_processors < 1 ){
			
			num_processors = 1;
		}
		
		ThreadMXBean	bean = ManagementFactory.getThreadMXBean();
		
		log.log( "Monitoring starts (processors =" + num_processors + ")" );
		
		if ( !bean.isThreadCpuTimeSupported()){
			
			log.log( "ThreadCpuTime not supported" );
			
			return;
		}
		
		if ( !bean.isThreadCpuTimeEnabled()){
			
			log.log( "Enabling ThreadCpuTime" );
			
			bean.setThreadCpuTimeEnabled( true );
		}
		
		Map<Long,Long>	last_times = new HashMap<Long,Long>();
		
		long	time_available = 10000;
		
		while( true ){
			
			long	start = System.currentTimeMillis();
			
			try{
				
				Thread.sleep(time_available);
				
			}catch( Throwable e ){
				
				log.log(e);
			}
		
			long	end = System.currentTimeMillis();
			
			long	elapsed = end - start;
			
			long[]	ids 	= bean.getAllThreadIds();
			
			long[]	diffs 	= new long[ids.length];
			
			long	total_diffs 	= 0;
			long	biggest_diff 	= 0;
			int		biggest_index	= 0;
			
			Map<Long,Long>	new_times = new HashMap<Long,Long>();
			
			for (int i=0;i<ids.length;i++){
			
				long	id = ids[i];
				
				long	time = bean.getThreadCpuTime( id )/1000000;	// nanos -> millis
				
				Long	old_time = last_times.get( id );
				
				if ( old_time != null ){
					
					long	diff = time - old_time.longValue();
					
					if ( diff > biggest_diff ){
						
						biggest_diff	= diff;
						
						biggest_index	= i;
					}
					
					diffs[i] = diff;
					
					total_diffs += diff;
				}
				
				new_times.put( id, time );
			}
			
			ThreadInfo	info = bean.getThreadInfo( ids[biggest_index ]);

			String	thread_name = info==null?"<dead>":info.getThreadName();
			
			int	percent = (int)( 100*biggest_diff / time_available );
			
			Runtime rt = Runtime.getRuntime();
			
			log.log( "Thread state: elapsed=" + elapsed + ",cpu=" + total_diffs + ",max=" + thread_name + "(" + biggest_diff + "/" + percent + "%),mem:max=" + (rt.maxMemory()/1024)+",tot=" + (rt.totalMemory()/1024) +",free=" + (rt.freeMemory()/1024));
			
			if ( biggest_diff > time_available/4 ){
				
				info = bean.getThreadInfo( ids[biggest_index ], 255 );
				
				if ( info == null ){
					
					log.log( "    no info for max thread" );
					
				}else{
					
					StackTraceElement[] elts = info.getStackTrace();
					StringBuilder str = new StringBuilder(elts.length * 20);
					
					str.append("    ");
					for (int i=0;i<elts.length;i++){
						if(i != 0)
							str.append(", ");
						str.append(elts[i]);
					}
					
					log.log( str.toString() );
				}
			}
			
			last_times	= new_times;
		}
	}

	public static void
	dumpThreads()
	{
		StringWriter	sw = new StringWriter();
		
		IndentWriter iw = new IndentWriter( new PrintWriter( sw ));
		
		dumpThreads( iw );
		
		iw.close();
		
		Debug.out( sw.toString());
	}
	
	private static void
	dumpThreads(
		IndentWriter		writer )
	{
		final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

		long[] allThreadIds = threadBean.getAllThreadIds();
		writer.println("Threads " + allThreadIds.length);
		writer.indent();

		List<ThreadInfo> threadInfos = new ArrayList<ThreadInfo>(allThreadIds.length);
		for (int i = 0; i < allThreadIds.length; i++) {
			ThreadInfo info = threadBean.getThreadInfo(allThreadIds[i], 32);
			if(info != null)
				threadInfos.add(info);
		}

		if (!disable_getThreadCpuTime) {
			Collections.sort(threadInfos, new Comparator<ThreadInfo>() {
				public int compare(ThreadInfo o1, ThreadInfo o2) {
					
					long diff = threadBean.getThreadCpuTime(o2.getThreadId())
							- threadBean.getThreadCpuTime(o1.getThreadId());
					if (diff == 0) {
						return o1.getThreadName().compareToIgnoreCase(o2.getThreadName());
					}
					return diff > 0 ? 1 : -1;
				}
			});
		}

		for (int i = 0; i < threadInfos.size(); i++) {
			try {
				ThreadInfo threadInfo = threadInfos.get(i);

				long lCpuTime = disable_getThreadCpuTime ? -1
						: threadBean.getThreadCpuTime(threadInfo.getThreadId());
				if (lCpuTime == 0)
					break;

				String sState;
				switch (threadInfo.getThreadState()) {
					case BLOCKED:
						sState = "Blocked";
						break;
					case RUNNABLE:
						sState = "Runnable";
						break;
					case NEW:
						sState = "New";
						break;
					case TERMINATED:
						sState = "Terminated";
						break;
					case TIMED_WAITING:
						sState = "Timed Waiting";
						break;

					case WAITING:
						sState = "Waiting";
						break;

					default:
						sState = "" + threadInfo.getThreadState();
						break;

				}

				String sName = threadInfo.getThreadName();
				String sLockName = threadInfo.getLockName();

				writer.println(sName
						+ ": "
						+ sState
						+ ", "
						+ (lCpuTime / 1000000)
						+ "ms CPU, "
						+ "B/W: "
						+ threadInfo.getBlockedCount()
						+ "/"
						+ threadInfo.getWaitedCount()
						+ (sLockName == null ? "" : "; Locked by " + sLockName + "/"
								+ threadInfo.getLockOwnerName()));

				writer.indent();
				try {
					StackTraceElement[] stackTrace = threadInfo.getStackTrace();
					for (int j = 0; j < stackTrace.length; j++) {
						writer.println(stackTrace[j].toString());
					}
				} finally {
					writer.exdent();
				}

			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		writer.exdent();
	}

	private static class EvidenceGenerateor implements
			AEDiagnosticsEvidenceGenerator
	{
		public void generate(IndentWriter writer) {
			dumpThreads( writer );
		}
	}
}
