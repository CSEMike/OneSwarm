/*
 * Created on 02-Jan-2005
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.tracker.server.impl.tcp.nonblocking;

import java.util.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerException;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelectorFactory;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;

/**
 * @author parg
 *
 */

public class  
TRNonBlockingServer 
	extends 	TRTrackerServerTCP
	implements 	VirtualServerChannelSelector.SelectListener
{
	private static final LogIDs LOGID = LogIDs.TRACKER;
	private static final int TIMEOUT_CHECK_INTERVAL = 10*1000;  //10sec
	
	private static final int CLOSE_DELAY			= 5*1000;
	
	private TRNonBlockingServerProcessorFactory	processor_factory;
	
	private final VirtualChannelSelector read_selector;
	private final VirtualChannelSelector write_selector;
	
	private 	  List			connections_to_close 		= new ArrayList();
	
	private		  List		processors					= new ArrayList();
	
	/*
	private long	last_stats_time;
	private long	last_timeouts;
	private long	last_connections;
	*/
	
	private InetAddress	current_bind_ip;
	
	private long	total_timeouts;
	private long	total_connections;
	
	public static final int MAX_CONCURRENT_CONNECTIONS	= COConfigurationManager.getIntParameter( "Tracker TCP NonBlocking Conc Max" );
	
	private final AEMonitor this_mon = new AEMonitor( "TRNonBlockingServer" );

	private VirtualServerChannelSelector accept_server;
	
	private boolean immediate_close = COConfigurationManager.getBooleanParameter( "Tracker TCP NonBlocking Immediate Close" );
	
	private volatile boolean	closed;
	
	public
	TRNonBlockingServer(
		String								_name,
		int									_port,
		InetAddress							_bind_ip,
		boolean								_apply_ip_filter,
		TRNonBlockingServerProcessorFactory	_processor_factory )
		
		throws TRTrackerServerException
	{
		this( _name, _port, _bind_ip, _apply_ip_filter, true, _processor_factory );
	}
	
	public
	TRNonBlockingServer(
		String								_name,
		int									_port,
		InetAddress							_bind_ip,
		boolean								_apply_ip_filter,
		boolean								_start_up_ready,
		TRNonBlockingServerProcessorFactory	_processor_factory )
		
		throws TRTrackerServerException
	{
		super( _name, _port, false, _apply_ip_filter, _start_up_ready );
		
		processor_factory	= _processor_factory;
		
		read_selector	 	= new VirtualChannelSelector( _name + ":" + _port, VirtualChannelSelector.OP_READ, false );
		write_selector 		= new VirtualChannelSelector( _name + ":" + _port, VirtualChannelSelector.OP_WRITE, true );

		boolean	ok = false;
		
		if ( _port == 0 ){
			
			throw( new TRTrackerServerException( "port of 0 not currently supported"));
		}
		
		try{
			InetSocketAddress	address;
			
			if ( _bind_ip == null ){
				
				_bind_ip = NetworkAdmin.getSingleton().getSingleHomedServiceBindAddress();
		
				if ( _bind_ip == null ){
					
					address = new InetSocketAddress( _port );
					
				}else{
	
					current_bind_ip = _bind_ip;
					
					address = new InetSocketAddress( _bind_ip, _port );			
				}
			}else{
				
				current_bind_ip = _bind_ip;

				address = new InetSocketAddress(  _bind_ip, _port );	
			}
			
			accept_server = VirtualServerChannelSelectorFactory.createBlocking( address, 0, this );
			
			accept_server.start();
		      
			AEThread	read_thread = 
				new AEThread( "TRTrackerServer:readSelector")
				{
					public void
					runSupport()
					{
						selectLoop( read_selector );
					}
				};
				
			read_thread.setDaemon(true);
				
			read_thread.start();
				
			AEThread	write_thread = 
				new AEThread( "TRTrackerServer:writeSelector")
				{
					public void
					runSupport()
					{
						selectLoop( write_selector );
					}
				};
				
			write_thread.setDaemon(true);
				
			write_thread.start();
			
			AEThread	close_thread = 
				new AEThread( "TRTrackerServer:closeScheduler")
				{
					public void
					runSupport()
					{
						closeLoop();
					}
				};
				
			close_thread.setDaemon(true);
				
			close_thread.start();
			
			Logger.log(new LogEvent(LOGID,
					"TRTrackerServer: Non-blocking listener established on port "
							+ getPort())); 

			ok	= true;
			
		}catch( Throwable e){
			
			Logger.logTextResource(new LogAlert(LogAlert.UNREPEATABLE,
					LogAlert.AT_ERROR, "Tracker.alert.listenfail"), new String[] { ""
					+ getPort() });
						
			throw( new TRTrackerServerException( "TRTrackerServer: accept fails", e ));
			
		}finally{
			
			if ( !ok ){
				
				destroySupport();
			}
		}
	}
	
	public InetAddress 
	getBindIP()
	{
		return( current_bind_ip );
	}
	
	public void
	setImmediateClose(
		boolean	immediate )
	{
		immediate_close = immediate;
	}
	
	protected void
	selectLoop(
      VirtualChannelSelector	selector )
	{
		long	last_time	= 0;
		
		while( !closed ){
			
			try{
				selector.select( 100 );
				
					// only use one selector to trigger the timeouts!
				
				if ( selector == read_selector ){
					
					long	now = SystemTime.getCurrentTime();
					
					if ( now < last_time ){
						
						last_time	= now;
						
					}else if ( now - last_time >= TIMEOUT_CHECK_INTERVAL ){
						
						last_time	= now;
						
						checkTimeouts(now);
					}
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
    public void 
	newConnectionAccepted( 
		ServerSocketChannel	server,
		SocketChannel 		channel ) 
    {
        final TRNonBlockingServerProcessor processor = processor_factory.create( this, channel );
        
        int	num_processors;
        
        try{
        	this_mon.enter();
        	
        	total_connections++;
        	
        	processors.add( processor );
        
        	num_processors	= processors.size();
        	
        }finally{
        	
        	this_mon.exit();
        }
        
        if ( 	MAX_CONCURRENT_CONNECTIONS != 0 &&
        		num_processors > MAX_CONCURRENT_CONNECTIONS ){
        	
        	removeAndCloseConnection( processor );
        
        }else if ( 	isIPFilterEnabled() && 
					ip_filter.isInRange( channel.socket().getInetAddress().getHostAddress(), "Tracker", null )){
        	
        	removeAndCloseConnection( processor );
        	
        }else{

          VirtualChannelSelector.VirtualSelectorListener read_listener = 
	        	new VirtualChannelSelector.VirtualSelectorListener() 
				{
        	  		private boolean	selector_registered;
        	  		
	        		public boolean 
					selectSuccess( 
						VirtualChannelSelector 	selector, 
						SocketChannel 			sc, 
						Object 					attachment ) 
	        		{
	        			try{
		        			int read_result = processor.processRead();
		            
		        			if ( read_result == 0 ) {  //read processing is complete
		        				
		        				if ( selector_registered ){
		        					
		        					read_selector.pauseSelects( sc );
		        				}
		        					        						
		        			}else if ( read_result < 0 ) {  //a read error occured

		        				removeAndCloseConnection( processor );
		        				
		        			}else{
		        			
			        			if ( !selector_registered ){
			        				
			        				selector_registered	= true;
			        				
			        		        read_selector.register( sc, this, null );
			        			}
		        			}
			        			
		        			return( read_result != 2 );
		        			
	              		}catch( Throwable e ){
	            			
	            			Debug.printStackTrace(e);
	            			
	            			removeAndCloseConnection( processor );
	            			
	            			return( false );
	            		}
	        		}
	
	        		public void 
					selectFailure( 
						VirtualChannelSelector 	selector, 
						SocketChannel 			sc, 
						Object 					attachment, 
						Throwable 				msg ) 
	        		{
	        			removeAndCloseConnection( processor );
	        		}	
				};
				
			read_listener.selectSuccess( read_selector, channel, null );
        }
    }

    protected void
	readyToWrite(
		final TRNonBlockingServerProcessor	processor )
    {
        final VirtualChannelSelector.VirtualSelectorListener write_listener = 
        	new VirtualChannelSelector.VirtualSelectorListener() 
			{
        		private boolean	selector_registered;

            	public boolean 
				selectSuccess( 
					VirtualChannelSelector 	selector, 
					SocketChannel 			sc, 
					Object 					attachment ) 
            	{
            		try{
	            		int write_result = processor.processWrite();
	              
	            		if( write_result > 0 ) { //more writing is needed
	            			
	            			if ( selector_registered ){
	            				
		            			write_selector.resumeSelects( sc );  //resume for more writing

	            			}else{
	            				
	            				selector_registered	= true;
	            				
	            				write_selector.register( sc, this, null ); 
	            			}
	            			
	            		}else if( write_result == 0 ) {  //write processing is complete

	            			removeAndCloseConnection( processor );
	
	            		}else if( write_result < 0 ) {  //a write error occured

	            			processor.failed();
	            			
	            			removeAndCloseConnection( processor );
	            		}
	            		
	            		return( write_result != 2 );
	            		
            		}catch( Throwable e ){
            			
            			Debug.printStackTrace(e);
            			
            			removeAndCloseConnection( processor );
            			
            			return( false );
            		}
            	}

            	public void 
				selectFailure( 
					VirtualChannelSelector 	selector, 
					SocketChannel 			sc, 
					Object 					attachment, 
					Throwable 				msg ) 
            	{
            		removeAndCloseConnection( processor );
            	}
			};
  

		write_listener.selectSuccess( write_selector, processor.getSocketChannel(), null );
    }
    
    protected void
    removeAndCloseConnection(
    	TRNonBlockingServerProcessor	processor )
    {
    	processor.completed();
    	
        try{
        	this_mon.enter();
        	
        	if ( processors.remove( processor )){
            
        		read_selector.cancel( processor.getSocketChannel() );
        		write_selector.cancel( processor.getSocketChannel() );
        	
        		if ( immediate_close ){

        			try{
        				processor.closed();

        				processor.getSocketChannel().close();
        				
        			}catch( Throwable e ){
        				
        			}
        		}else{
        			
        			connections_to_close.add( processor );
        		}
        	}
        	
        }finally{
        	
        	this_mon.exit();
        }
    }
    
	public void
	checkTimeouts(
		long	now )
	{
   		// we don't particularly care about timeouts if nothing's going on, hence we only
		// trigger the check on the arrival of a new connection
	
		/*
		String	con_rate 	= "";
		String	tim_rate	= "";
		
		if ( last_stats_time > 0 ){
						
			long	time_diff = (now - last_stats_time)/1000;
			
			long	conn_diff 	= total_connections - last_connections;
			long	tim_diff	= total_timeouts - last_timeouts;
			
			con_rate = "" + (conn_diff/time_diff);
			tim_rate = "" + (tim_diff/time_diff);
		}

		System.out.println( "Tracker: con/sec = " + con_rate + ", timeout/sec = " + tim_rate + ", tot_con = " + total_connections+ ", total timeouts = " + total_timeouts + 
							", current connections = " + processors.size() + ", closing = " + connections_to_close.size());

		last_stats_time		= now;
		last_connections	= total_connections;
		last_timeouts		= total_timeouts;

		*/
		
		try{
        	this_mon.enter();
        
        	List	new_processors = new ArrayList(processors.size());
        	
        	for (int i=0;i<processors.size();i++){
        		
        		TRNonBlockingServerProcessor	processor = (TRNonBlockingServerProcessor)processors.get(i);
        		
        		if ( now - processor.getStartTime() > PROCESSING_GET_LIMIT ){
              
        			read_selector.cancel( processor.getSocketChannel() );
        			write_selector.cancel( processor.getSocketChannel() );
              
        			connections_to_close.add( processor );
                	            		
        			total_timeouts++;
               		
        		}else{
        			
        			new_processors.add( processor );
        		}
        	}
        	
        	processors	= new_processors;
        	
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	closeLoop()
	{
			// socket channel close ops can block, hence we move it off the main processing loops
			// to ensure that a rogue connection doesn't stall us
		
		List	pending_list	= new ArrayList();
		
		long	default_delay = CLOSE_DELAY*2/3;
		
		long	delay = default_delay;
		
		while( !closed ){

				// wait a small amount of time to allow the client to close the connection rather
				// than us. This prevents a buildup of TIME_WAIT state sockets
			
			if ( delay > 0 ){
				
				try{
					Thread.sleep( delay );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
			
			// System.out.println( "close delay = " + delay + ", pending =" + pending_list.size());
			
			long	start = SystemTime.getCurrentTime();
			
	        for (int i=0;i<pending_list.size();i++){
	        	
	        	try{
	        		TRNonBlockingServerProcessor processor = (TRNonBlockingServerProcessor)pending_list.get(i);
	        		
	        		processor.closed();
	        		
	        		processor.getSocketChannel().close();
	        		
	        	}catch( Throwable e ){
	        		
	        	}
	        }
				        	
		    try{
		    	this_mon.enter();
	        
		    	pending_list	= connections_to_close;
		    	
		    	connections_to_close	= new ArrayList();
		    	
	        }finally{
	        	
	        	this_mon.exit();
	        }
	        
	        	// reduce the sleep time if we're not keeping up
	        
	        long	duration = SystemTime.getCurrentTime() - start;
	        
	        if ( duration < 0 ){
	        	
	        	duration	= 0;
	        }
	        
	        delay = default_delay - duration;
		}	
	}
	
	protected void 
	closeSupport() 
	{
		closed	= true;
		
		accept_server.stop();
		
		destroySupport();
	}
}
