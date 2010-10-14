/*
 * File    : ThreadPool.java
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

import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

public class 
ThreadPool 
{
	private static final int	IDLE_LINGER_TIME	= 10000;
	
	private static final boolean	LOG_WARNINGS	= false;
	private static final int		WARN_TIME		= 10000;
	
	private static List		busy_pools			= new ArrayList();
	private static boolean	busy_pool_timer_set	= false;
	
	private static boolean	debug_thread_pool;
	private static boolean	debug_thread_pool_log_on;
	
	static{
		if ( System.getProperty("transitory.startup", "0").equals("0")){

			AEDiagnostics.addEvidenceGenerator(
				new AEDiagnosticsEvidenceGenerator()
				{
					public void
					generate(
						IndentWriter		writer )
					{
						writer.println( "Thread Pools" );
						
						try{
							writer.indent();

							List	pools;	
							
							synchronized( busy_pools ){
								
								pools	= new ArrayList( busy_pools );
							}
							
							for (int i=0;i<pools.size();i++){
								
								((ThreadPool)pools.get(i)).generateEvidence( writer );
							}
						}finally{
						
							writer.exdent();
						}
					}
				});
		}
	}
	
	private static ThreadLocal		tls	= 
		new ThreadLocal()
		{
			public Object
			initialValue()
			{
				return( null );
			}
		};
		
	protected static void
	checkAllTimeouts()
	{
		List	pools;	
	
			// copy the busy pools to avoid potential deadlock due to synchronization
			// nestings
		
		synchronized( busy_pools ){
			
			pools	= new ArrayList( busy_pools );
		}
		
		for (int i=0;i<pools.size();i++){
			
			((ThreadPool)pools.get(i)).checkTimeouts();
		}
	}
	
	
	private String	name;
	private int		max_size;
	private int		thread_name_index	= 1;
	
	private long	execution_limit;
	
	private Stack	thread_pool;
	private List	busy;
	private boolean	queue_when_full;
	private List	task_queue	= new ArrayList();
	
	private AESemaphore	thread_sem;
	
	private int			thread_priority	= Thread.NORM_PRIORITY;
	private boolean		warn_when_full;

	private long		task_total;
	private long		task_total_last;
	private Average		task_average	= Average.getInstance( WARN_TIME, 120 );
	
	public
	ThreadPool(
		String	_name,
		int		_max_size )
	{
		this( _name, _max_size, false );
	}
	
	public
	ThreadPool(
		String	_name,
		int		_max_size,
		boolean	_queue_when_full )
	{
		name			= _name;
		max_size		= _max_size;
		queue_when_full	= _queue_when_full;
		
		thread_sem = new AESemaphore( "ThreadPool::" + name, _max_size );
		
		thread_pool	= new Stack();
					
		busy		= new ArrayList( _max_size );
	}

	private void
	generateEvidence(
		IndentWriter		writer )
	{
		writer.println( name + ": max=" + max_size +",qwf=" + queue_when_full + ",queue=" + task_queue.size() + ",busy=" + busy.size() + ",pool=" + thread_pool.size() + ",total=" + task_total + ":" + DisplayFormatters.formatDecimal(task_average.getDoubleAverage(),2) + "/sec");
	}
	
	public void
	setWarnWhenFull()
	{
		warn_when_full	= true;
	}
	
	public int
	getMaxThreads()
	{
		return( max_size );
	}
	
	public void
	setThreadPriority(
		int	_priority )
	{
		thread_priority	= _priority;
	}
	
	public void
	setExecutionLimit(
		long		millis )
	{
		execution_limit	= millis;
	}
	
	public threadPoolWorker
	run(
		AERunnable	runnable )
	{
		return( run( runnable, false ));
	}
	
		/**
		 * 
		 * @param runnable
		 * @param high_priority		inserts at front if tasks queueing
		 * @return
		 */
	
	public threadPoolWorker
	run(
		AERunnable	runnable,
		boolean		high_priority )
	{
		// System.out.println( "Thread pool:" + name + " - sem = " + thread_sem.getValue() + ", queue = " + task_queue.size());
		
			// not queueing, grab synchronous sem here
		
		if ( !queue_when_full ){
		
			if ( !thread_sem.reserveIfAvailable()){
			
					// defend against recursive entry when in queuing mode (yes, it happens)
				
				threadPoolWorker	recursive_worker = (threadPoolWorker)tls.get();
				
				if ( recursive_worker == null || recursive_worker.getOwner() != this ){
	
						// do a blocking reserve here, not recursive 
					
					checkWarning();
					
					thread_sem.reserve();
	
				}else{
						// run immediately
							
					if ( runnable instanceof ThreadPoolTask ){
						
						ThreadPoolTask task = (ThreadPoolTask)runnable;
						                        
						task.worker = recursive_worker;
						
						try{
							task.taskStarted();
							
							task.run();
							
						}finally{
							
							task.taskCompleted();  
						}
					}else{
					
						runnable.runSupport();
					}
					
					return( recursive_worker );
				}
			}
		}
						
		threadPoolWorker allocated_worker;
						
		synchronized( this ){
		
				// reserve if available is non-blocking
			
			if ( queue_when_full && !thread_sem.reserveIfAvailable()){
			
				allocated_worker	= null;
			
				checkWarning();
				
				if ( high_priority ){
					
					task_queue.add( 0, runnable );
					
				}else{
				
					task_queue.add( runnable );
				}
			}else{
				
				if ( thread_pool.isEmpty()){
							
					allocated_worker = new threadPoolWorker();	
		
				}else{
									
					allocated_worker = (threadPoolWorker)thread_pool.pop();
				}
				
				if ( runnable instanceof ThreadPoolTask ){
					
					((ThreadPoolTask)runnable).worker = allocated_worker;
				}
				
				allocated_worker.run( runnable );
			}
		}
		
		return( queue_when_full?null:allocated_worker );
	}
	
	protected void
	checkWarning()
	{
		if ( warn_when_full ){
			
			String	task_names = "";
			
			try{
				synchronized( ThreadPool.this ){
						
					for (int i=0;i<busy.size();i++){
							
						threadPoolWorker	x = (threadPoolWorker)busy.get(i);
												
						AERunnable r = x.runnable;
	
						if ( x != null ){
							
							String	name;
							
							if ( r instanceof ThreadPoolTask ){
								
								name = ((ThreadPoolTask)r).getName();
								
							}else{
								
								name = x.getClass().getName();
							}
							
							task_names += (task_names.length()==0?"":",") + name;
						}
					}
				}
			}catch( Throwable e ){
			}
			
			Debug.out( "Thread pool '" + getName() + "' is full (busy=" + task_names + ")" );
			
			warn_when_full	= false;
		}
	}
	
	public AERunnable[]
	getQueuedTasks()
	{
		synchronized( this ){

			AERunnable[]	res = new AERunnable[task_queue.size()];
			
			task_queue.toArray(res);
			
			return( res );
		}
	}
	
	public int
	getQueueSize()
	{
		synchronized( this ){
			
			return( task_queue.size());
		}
	}
	
	public boolean
	isQueued(
		AERunnable	task )
	{
		synchronized( this ){

			return( task_queue.contains( task ));
		}
	}
	
	public AERunnable[]
	getRunningTasks()
	{
		List	runnables	= new ArrayList();
		
		synchronized( this ){

			Iterator	it = busy.iterator();
			
			while( it.hasNext()){
				
				threadPoolWorker	worker = (threadPoolWorker)it.next();
				
				AERunnable	runnable = worker.getRunnable();
				
				if ( runnable != null ){
					
					runnables.add( runnable );
				}
			}
		}
		
		AERunnable[]	res = new AERunnable[runnables.size()];
			
		runnables.toArray(res);
			
		return( res );
	}
	
	protected void
	checkTimeouts()
	{
		synchronized( ThreadPool.this ){
		
			long	diff = task_total - task_total_last;
			
			task_average.addValue( diff );
			
			task_total_last = task_total;
			
			if ( debug_thread_pool_log_on ){
				
				System.out.println( "ThreadPool '" + getName() + "'/" + thread_name_index + ": max=" + max_size + ",sem=[" + thread_sem.getString() + "],pool=" + thread_pool.size() + ",busy=" + busy.size() + ",queue=" + task_queue.size());
			}
			
			long	now = SystemTime.getCurrentTime();
			
			for (int i=0;i<busy.size();i++){
					
				threadPoolWorker	x = (threadPoolWorker)busy.get(i);
			
				long	elapsed = now - x.run_start_time ;
					
				if ( elapsed > ( WARN_TIME * (x.warn_count+1))){
		
					x.warn_count++;
					
					if ( LOG_WARNINGS ){
						
						DebugLight.out( x.getWorkerName() + ": running, elapsed = " + elapsed + ", state = " + x.state );
					}
					
					if ( execution_limit > 0 && elapsed > execution_limit ){
						
						if ( LOG_WARNINGS ){
							
							DebugLight.out( x.getWorkerName() + ": interrupting" );
						}
						
						AERunnable r = x.runnable;

						if ( r != null ){
							
							try{
								if ( r instanceof ThreadPoolTask ){
									
									((ThreadPoolTask)r).interruptTask();
									
								}else{
									
									x.worker_thread.interrupt();
								}
							}catch( Throwable e ){
								
								DebugLight.printStackTrace( e );
							}
						}
					}
				}
			}
		}
	}
	
	public class
	threadPoolWorker
	{
		private final String	worker_name;
		
		private final AEThread2	worker_thread;
		
		private AESemaphore my_sem = new AESemaphore("TPWorker");
		
		private volatile AERunnable	runnable;
		
		private long		run_start_time;
		private int			warn_count;
		
		private String	state	= "<none>";
		
		protected
		threadPoolWorker()
		{			
			worker_name = name + "[" + (thread_name_index++) +  "]";
			
			worker_thread = new AEThread2( worker_name, true )
				{
					public void 
					run()
					{
						if ( thread_priority != Thread.NORM_PRIORITY ){
							
							setPriority( thread_priority );
						}
						
						tls.set( threadPoolWorker.this );
						
						boolean	time_to_die = false;
			
outer:
						while(true){
							
							try{
								while( !my_sem.reserve(IDLE_LINGER_TIME)){
																		
									synchronized( ThreadPool.this ){
										
										if ( runnable == null ){
											
											time_to_die	= true;
											
											thread_pool.remove( threadPoolWorker.this );
																						
											break outer;
										}
									}
								}
								
								while( runnable != null ){
									
									try{
										
										synchronized( ThreadPool.this ){
												
											run_start_time	= SystemTime.getCurrentTime();
											warn_count		= 0;
											
											busy.add( threadPoolWorker.this );
											
											task_total++;
											
											if ( busy.size() == 1 ){
												
												synchronized( busy_pools ){
													
													if ( !busy_pools.contains( ThreadPool.this  )){
														
														busy_pools.add( ThreadPool.this );
														
														if  ( !busy_pool_timer_set ){
															
																// we have to defer this action rather
																// than running as a static initialiser
																// due to the dependency between
																// ThreadPool, Timer and ThreadPool again
															
															COConfigurationManager.addAndFireParameterListeners(
																	new String[]{ "debug.threadpool.log.enable", "debug.threadpool.debug.trace" },
																	new ParameterListener()
																	{
																		public void 
																		parameterChanged(
																			String name )
																		{
																			debug_thread_pool 			= COConfigurationManager.getBooleanParameter( "debug.threadpool.log.enable", false );
																			debug_thread_pool_log_on 	= COConfigurationManager.getBooleanParameter( "debug.threadpool.debug.trace", false );
																		}
																	});
																
															busy_pool_timer_set	= true;
															
															SimpleTimer.addPeriodicEvent(
																	"ThreadPool:timeout",
																	WARN_TIME,
																	new TimerEventPerformer()
																	{
																		public void
																		perform(
																			TimerEvent	event )
																		{
																			checkAllTimeouts();
																		}
																	});
														}
													}
												}
											}
										}
										
										if ( runnable instanceof ThreadPoolTask ){
										
											ThreadPoolTask	tpt = (ThreadPoolTask)runnable;
											
											String	task_name = tpt.getName();
																
											try{
												if ( task_name != null ){
													
													setName( worker_name + "{" + task_name + "}" );
												}
							
												tpt.taskStarted();
												
												runnable.run();
												
											}finally{
												
												if ( task_name != null ){
													
													setName( worker_name );
												}
												
												tpt.taskCompleted();
											}
										}else{
											
											runnable.run();
										}
										
									}catch( Throwable e ){
										
										DebugLight.printStackTrace( e );		
	
									}finally{
																					
										synchronized( ThreadPool.this ){
												
											long	elapsed = SystemTime.getCurrentTime() - run_start_time;
											
											if ( elapsed > WARN_TIME && LOG_WARNINGS ){
												
												DebugLight.out( getWorkerName() + ": terminated, elapsed = " + elapsed + ", state = " + state );
											}
											
											busy.remove( threadPoolWorker.this );
											
												// if debug is on we leave the pool registered so that we
												// can trace on the timeout events
											
											if ( busy.size() == 0 && !debug_thread_pool ){
												
												synchronized( busy_pools ){
												
													busy_pools.remove( ThreadPool.this );
												}
											}
										
											if ( task_queue.size() > 0 ){
												
												runnable = (AERunnable)task_queue.remove(0);
												
											}else{
											
												runnable	= null;
											}
										}
									}
								}
							}catch( Throwable e ){
									
								DebugLight.printStackTrace( e );
											
							}finally{
										
								if ( !time_to_die ){
									
									synchronized( ThreadPool.this ){
											
										if ( thread_pool.contains( threadPoolWorker.this )){
											
											Debug.out( "Thread pool already contains worker!" );
										}
										
										thread_pool.push( threadPoolWorker.this );
									}
								
									thread_sem.release();
								}
							}
						}
					}
				};
							
			worker_thread.start();
		}
		
		public void
		setState(
			String	_state )
		{
			//System.out.println( "state = " + _state );
			
			state	= _state;
		}
		
		public String
		getState()
		{
			return( state );
		}
		
		protected String
		getWorkerName()
		{
			return( worker_name );
		}
		
		protected ThreadPool
		getOwner()
		{
			return( ThreadPool.this );
		}
		
		protected void
		run(
			AERunnable	_runnable )
		{
			if ( runnable != null ){
				
				Debug.out( "Runnable already set" );
			}
			
			runnable	= _runnable;
			
			my_sem.release();
		}
		
		protected AERunnable
		getRunnable()
		{
			return( runnable );
		}
	}
	
	public String
	getName()
	{
		return( name );
	}
}
