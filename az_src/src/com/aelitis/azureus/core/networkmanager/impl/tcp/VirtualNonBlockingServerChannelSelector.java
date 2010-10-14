/*
 * Created on Dec 4, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.net.*;
import java.nio.channels.*;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelector;



/**
 * Virtual server socket channel for listening and accepting incoming connections.
 */
public class 
VirtualNonBlockingServerChannelSelector
	implements VirtualServerChannelSelector
{
	private static final LogIDs LOGID = LogIDs.NWMAN;
	
	private List	server_channels	= new ArrayList();
  
	private final InetAddress bind_address;
	private int		start_port;
	private int		num_ports;
	private final int receive_buffer_size;
	private final VirtualBlockingServerChannelSelector.SelectListener listener;
  
	protected AEMonitor	this_mon	= new AEMonitor( "VirtualNonBlockingServerChannelSelector" );

	private long last_accept_time;
  
  
	  /**
	   * Create a new server listening on the given address and reporting to the given listener.
	   * @param bind_address ip+port to listen on
	   * @param so_rcvbuf_size new socket receive buffer size
	   * @param listener to notify of incoming connections
	   */
	
	public 
	VirtualNonBlockingServerChannelSelector( 
		InetSocketAddress 										bind_address, 
		int 													so_rcvbuf_size, 
		VirtualBlockingServerChannelSelector.SelectListener 	listener ) 
	{
		this( bind_address.getAddress(), bind_address.getPort(), 1, so_rcvbuf_size, listener );
	}
  
	public 
	VirtualNonBlockingServerChannelSelector( 
		InetAddress 										_bind_address, 
		int													_start_port,
		int													_num_ports,
		int 												_so_rcvbuf_size, 
		VirtualBlockingServerChannelSelector.SelectListener _listener ) 
	{
		bind_address		= _bind_address;
		start_port			= _start_port;
		num_ports			= _num_ports;
		receive_buffer_size	= _so_rcvbuf_size;
		listener			= _listener;
	}
  
  /**
   * Start the server and begin accepting incoming connections.
   * 
   */
  public void start() {
  	try{
  		this_mon.enter();
  	
	    if( !isRunning() ) {
	    	for (int i=start_port;i<start_port+num_ports;i++){
	      
	    		try {
	    			final ServerSocketChannel	server_channel = ServerSocketChannel.open();
	        
	    			server_channels.add( server_channel );
	    			
	    			server_channel.socket().setReuseAddress( true );
	    			
	    			if( receive_buffer_size > 0 )  server_channel.socket().setReceiveBufferSize( receive_buffer_size );
	        	    			
	    			server_channel.socket().bind( new InetSocketAddress( bind_address, i ), 1024 );
	        
	    			if (Logger.isEnabled()) 	Logger.log(new LogEvent(LOGID, "TCP incoming server socket "	+ bind_address));
	        
	    			server_channel.configureBlocking( false );
	        
			        VirtualAcceptSelector.getSingleton().register( 
			        		server_channel,
			        		new VirtualAcceptSelector.AcceptListener()
			        		{
			        			public void 
			        			newConnectionAccepted( 
			        				SocketChannel channel )
			        			{
			        			    last_accept_time = SystemTime.getCurrentTime();
			        			    
			        				listener.newConnectionAccepted( server_channel, channel );
			        			}
			        		});
	    		}catch( Throwable t ) {
	    			Debug.out( t );
	    			Logger.log(new LogAlert(LogAlert.UNREPEATABLE,	"ERROR, unable to bind TCP incoming server socket to " +i, t));
	    		}
	    	}
	      
	    	last_accept_time = SystemTime.getCurrentTime();  //init to now
	    }
  	}finally{
  		
  		this_mon.exit();
  	} 	
  }
  
  
  /**
   * Stop the server.
   */
  public void stop() {
  	try{
  		this_mon.enter();

	    for (int i=0;i<server_channels.size();i++){
	      try {
	    	  ServerSocketChannel	server_channel = (ServerSocketChannel)server_channels.get(i);
	    	
	    	  VirtualAcceptSelector.getSingleton().cancel( server_channel );
	        
	    	  server_channel.close();
	       
	      }
	      catch( Throwable t ) {  Debug.out( t );  }
	    }
	    
	    server_channels.clear();
  	}finally{
  		
  		this_mon.exit();
  	}
  }
  
  
  /**
   * Is this selector actively running
   * @return true if enabled, false if not running
   */
  public boolean isRunning() {
	  if ( server_channels.size() == 0 ){
		  return( false);
	  }
	  
	  ServerSocketChannel	server_channel = (ServerSocketChannel)server_channels.get(0);
	  
  	  if( server_channel != null && server_channel.isOpen() )  return true;
  	  return false;
  }
  
  
  public InetAddress getBoundToAddress() {
	  if ( server_channels.size() == 0 ){
		  return( null);
	  }
	  ServerSocketChannel	server_channel = (ServerSocketChannel)server_channels.get(0);
 	
	  return server_channel.socket().getInetAddress();
  }
  
  
  public long getTimeOfLastAccept() {
  	return last_accept_time;
  }
}
