/*
 * Created on Nov 3, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.clientmessageservice.impl;

import java.nio.channels.SocketChannel;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZGenericMapPayload;


/**
 * 
 */
public class NonBlockingReadWriteService {
	
  private final VirtualChannelSelector read_selector;
  private final VirtualChannelSelector write_selector;
  
  private final ArrayList connections = new ArrayList();
  private final AEMonitor connections_mon = new AEMonitor( "connections" );
  
  private final ServiceListener listener;
  private final String service_name;
  private volatile boolean destroyed;
  
  private long last_timeout_check_time = 0;
  private static final int TIMEOUT_CHECK_INTERVAL_MS = 10*1000;  //check for timeouts every 10sec
  private final int activity_timeout_period_ms;
  private final int close_delay_period_ms;
  
  
	public NonBlockingReadWriteService( String _service_name, int timeout, ServiceListener _listener ) {
		this( _service_name, timeout, 0, _listener );
	}
  
	public NonBlockingReadWriteService( String _service_name, int timeout, int close_delay, ServiceListener _listener ) {
		this.service_name = _service_name;
		this.listener = _listener;

		read_selector = new VirtualChannelSelector( service_name, VirtualChannelSelector.OP_READ, false );
		write_selector = new VirtualChannelSelector( service_name, VirtualChannelSelector.OP_WRITE, true );

		if( timeout < TIMEOUT_CHECK_INTERVAL_MS /1000 )  timeout = TIMEOUT_CHECK_INTERVAL_MS /1000;
		this.activity_timeout_period_ms = timeout *1000;
		close_delay_period_ms			= close_delay * 1000;
		
		new AEThread2( "[" +service_name+ "] Service Select", true ) 
		{
	      public void run() {
	        while( true ) {
	        	
	          boolean	stop_after_select = destroyed;
	          
	  	      if ( stop_after_select ){
	  	    	  read_selector.destroy();
	  	    	  write_selector.destroy();
	  	      }
	  	      
	          try{
	            read_selector.select( 50 );
	            write_selector.select( 50 );
	          }
	          catch( Throwable t ) {
	            Debug.out( "[" +service_name+ "] SelectorLoop() EXCEPTION: ", t );
	          }
	          
	          if (stop_after_select){
	        	  break;
	          }
	          
	          doConnectionTimeoutChecks();
	          
	          	// check this at the end so we have one last run through  the selectors to do cancels
	          	// before exiting
	        }
	      }
	    }.start();
	}
	
	
	
	public void 
	destroy() 
	{
	    try {  
	    	connections_mon.enter();
		    	    
	    	connections.clear();
	    	
	    	destroyed	= true;
	    	
	    }finally{
	    	connections_mon.exit();
	    }
	}
	
	
	
	public void addClientConnection( ClientConnection connection ) {
		//add to active list
		
    try {  connections_mon.enter();
    
    	if ( destroyed ){
    		
    		Debug.out( "connection added after destroy" );
    	}
    	
    	connections.add( connection );
    }finally {  
    	connections_mon.exit(); 
    }
    
    registerForSelection( connection );
	}
	
	
	
	public void removeClientConnection( ClientConnection connection ) {
    read_selector.cancel( connection.getSocketChannel() );
    write_selector.cancel( connection.getSocketChannel() );
    
    //remove from active list
    try {  connections_mon.enter();
      connections.remove( connection );
    }
    finally {  connections_mon.exit();  }
	}
	
	
	
	
	
	private void registerForSelection( final ClientConnection client ) {		
		//READS
		VirtualChannelSelector.VirtualSelectorListener read_listener = new VirtualChannelSelector.VirtualSelectorListener() {
			//SUCCESS
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {     	
      	try{
      		Message[] messages = client.readMessages();
      	
      		if( messages != null ) {    		
      			for( int i=0; i < messages.length; i++ ) {
      				AZGenericMapPayload msg = (AZGenericMapPayload)messages[i];
      				ClientMessage client_msg = new ClientMessage( msg.getID(), client, msg.getMapPayload(), null );  //note no handler. we let the listener attach it		
      				listener.messageReceived( client_msg );	
      			}
      		}	
      		
      		return( client.getLastReadMadeProgress());
      	}
      	catch( Throwable t ) {
      		if ( !client.isClosePending()){
      			
      			//System.out.println( "[" +new Date()+ "] Connection read error [" +sc.socket().getInetAddress()+ "] [" +client.getDebugString()+ "]: " +t.getMessage() );
      		}
      		
      		listener.connectionError( client, t );
      		return( false );
      	}
      }

      //FAILURE
      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
    	  if ( !destroyed ){
    		  msg.printStackTrace();
    	  }
        listener.connectionError( client, msg );
      }
    };
    
    
    //WRITES
    final VirtualChannelSelector.VirtualSelectorListener write_listener = new VirtualChannelSelector.VirtualSelectorListener() {
      public boolean selectSuccess( VirtualChannelSelector selector, SocketChannel sc, Object attachment ) {   	
      	try{
      		boolean more_writes_needed = client.writeMessages();
      	
      		if( more_writes_needed ) {
      			write_selector.resumeSelects( client.getSocketChannel() );  //we need to resume since write selects are auto-paused after select op
      		}
      		
      		return( client.getLastWriteMadeProgress());
      	}
      	catch( Throwable t ) {
          //System.out.println( "[" +new Date()+ "] Connection write error [" +sc.socket().getInetAddress()+ "] [" +client.getDebugString()+ "]: " +t.getMessage() );
          listener.connectionError( client, t );
          return( false );
      	}
      }

      public void selectFailure( VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg ) {
        if ( !destroyed ){
        	msg.printStackTrace();
        }
        listener.connectionError( client, msg );
      }
    };

    write_selector.register( client.getSocketChannel(), write_listener, null );  //start writing back to the connection
    write_selector.pauseSelects( client.getSocketChannel() );   //wait until we've got something to send before selecting
    
    read_selector.register( client.getSocketChannel(), read_listener, null );  //start reading from the connection
	}
	
	
  private void doConnectionTimeoutChecks() {
    //check timeouts
    long time = System.currentTimeMillis();
    if( time < last_timeout_check_time || time - last_timeout_check_time > TIMEOUT_CHECK_INTERVAL_MS ) {
      ArrayList timed_out = new ArrayList();
      
      try {  connections_mon.enter();
        long current_time = System.currentTimeMillis();
    
        for( int i=0; i < connections.size(); i++ ) {
          ClientConnection vconn = (ClientConnection)connections.get( i );
        
          if( current_time < vconn.getLastActivityTime() ) {  //time went backwards!
            vconn.resetLastActivityTime();
          }
          else{
        	  if( current_time - vconn.getLastActivityTime() > activity_timeout_period_ms ||
        			 ( close_delay_period_ms > 0 && 
        			   current_time - vconn.getLastActivityTime() > close_delay_period_ms )){
        		  
        		  timed_out.add( vconn );   //do actual removal outside the check loop
        	  }
          }
        }
      }
      finally {  connections_mon.exit();  }
      
      for( int i=0; i < timed_out.size(); i++ ) {  
        ClientConnection vconn = (ClientConnection)timed_out.get( i );
        // don't change the exception text - it is used elsewhere
        listener.connectionError( vconn, new Exception( "Timeout" ));
      }
      
      last_timeout_check_time = System.currentTimeMillis();
    }
  }
	
	

  
  
  public void sendMessage( ClientMessage message ) {
		ClientConnection vconn = message.getClient();
		
		boolean still_connected;
		
		try {  connections_mon.enter();
			still_connected = connections.contains( vconn );
		}
		finally {  connections_mon.exit();  }
		
		if( !still_connected ) {
			// System.out.println( "[" +new Date()+ "] Connection message send error [connection no longer connected]: " +vconn.getDebugString()+ "]" );
			message.reportFailed( new Exception("No longer connected" ));
			//listener.connectionError( vconn ); //no need to call this, as there is no connection to remove
      return;
		}
		
		Message reply = new AZGenericMapPayload( message.getMessageID(), message.getPayload(), (byte)1 );

		vconn.sendMessage( message, reply );
		
		write_selector.resumeSelects( vconn.getSocketChannel() );  //start write selecting now that there's something to send
  }
  
  
	

	public interface ServiceListener {

		public void messageReceived( ClientMessage message );
		
		public void connectionError( ClientConnection connection, Throwable error );
		
	}
	
}
