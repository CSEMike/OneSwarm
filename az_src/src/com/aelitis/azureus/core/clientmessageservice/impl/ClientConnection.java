/*
 * Created on Oct 28, 2005
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.impl.OutgoingMessageQueueImpl;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TransportEndpointTCP;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


/**
 * 
 */
public class ClientConnection {

	private Transport parent_transport;
	private final Transport light_transport;
	private final OutgoingMessageQueue out_queue;
	private final AZMessageDecoder decoder;
	private static final AZMessageEncoder encoder = new AZMessageEncoder( AZMessageEncoder.PADDING_MODE_NONE );
	private long last_activity_time;
	
	private final AEMonitor msg_mon = new AEMonitor( "ClientConnection" );
	private final ArrayList sending_msgs = new ArrayList();
	
	private Map		user_data;
	private boolean	close_pending;
	private boolean	closed;
	
	private boolean	last_write_made_progress;
	private String debug_string = "<>";
	
	private Throwable	closing_reason;
	
	/**
	 * Create a new connection based on an incoming socket.
	 * @param channel
	 */
	public ClientConnection( SocketChannel channel ) {
		decoder = new AZMessageDecoder();
		
		InetSocketAddress remote = null;	// unfortunately we don't have an address at this point (see NATTestService)
		
		ProtocolEndpointTCP	pe = 
			(ProtocolEndpointTCP)ProtocolEndpointFactory.createEndpoint( ProtocolEndpoint.PROTOCOL_TCP, remote );
							
		light_transport = pe.connectLightWeight( channel );
		
		out_queue = new OutgoingMessageQueueImpl( encoder );
		out_queue.setTransport( light_transport );
		last_activity_time = System.currentTimeMillis();
	}
	
	
	/**
	 * Create a new connection based on an already-established outgoing socket.
	 * @param transport parent
	 */
	public ClientConnection( TCPTransportImpl transport ) {
		this( transport.getSocketChannel() );  //run as a lightweight
		parent_transport = transport;  //save parent for close
	}
	
	
	
	/**
	 * Get any messages read from the client.
	 * @return read messages, or null of no new messages were read
	 * @throws IOException on error
	 */
	public Message[] readMessages() throws IOException {
		int bytes_read = decoder.performStreamDecode( light_transport, 1024*1024 );
		if( bytes_read > 0 )  last_activity_time = System.currentTimeMillis();
		
		return decoder.removeDecodedMessages();
	}
	
	public boolean
	getLastReadMadeProgress()
	{
		return( decoder.getLastReadMadeProgress());
	}
	
	public boolean
	getLastWriteMadeProgress()
	{
		return( last_write_made_progress );
	}
	
	public void sendMessage( final ClientMessage client_msg, final Message msg ) {
		try{  msg_mon.enter();
			sending_msgs.add( client_msg );
		}
		finally{ msg_mon.exit(); }
		
		out_queue.registerQueueListener( new OutgoingMessageQueue.MessageQueueListener() {
				public boolean messageAdded( Message message ){  return true;  }
		    public void messageQueued( Message message ){}
		    public void messageRemoved( Message message ){}
		    public void protocolBytesSent( int byte_count ){}
		    public void dataBytesSent( int byte_count ){}
		    public void flush() {}
		    public void messageSent( Message message ){
		    	if( message.equals( msg ) ) {
		    		try{  msg_mon.enter();
		  				sending_msgs.remove( client_msg );
		    		}
		    		finally{ msg_mon.exit(); }
		    		
		    		client_msg.reportComplete();
		    	}
		    }	    
			});
			
		out_queue.addMessage( msg, false );

	}
	
	
	/**
	 * Write any queued messages back to the client.
	 * @return true if more writing is required, false if all message data has been sent
	 * @throws IOException on error
	 */
	public boolean writeMessages() throws IOException {
		int bytes_written = out_queue.deliverToTransport( 1024*1024, false );
		if( bytes_written > 0 )  last_activity_time = System.currentTimeMillis();
		
		last_write_made_progress = bytes_written > 0;
		
		return out_queue.getTotalSize() > 0;
	}
	
	
	//public boolean hasDataToSend() {
	//	return out_queue.getTotalSize() > 0;
	//}
	
	
	public void close( Throwable reason ) {
		ClientMessage[] messages = null;
		
		try{  msg_mon.enter();
			if ( closed ){
				return;
			}
			closed	= true;
			if( !sending_msgs.isEmpty() ) {
				messages = (ClientMessage[])sending_msgs.toArray( new ClientMessage[sending_msgs.size()] );
			}
		}
		finally{ msg_mon.exit(); }
	
		if( messages != null ) {
			if ( reason == null ){
				reason = new Exception( "Connection closed" );
			}
			for( int i=0; i < messages.length; i++ ) {
				ClientMessage msg = messages[i];
				msg.reportFailed( reason );
			}
		}
	
		decoder.destroy();
		out_queue.destroy();
		
		String x = "Tidy close" + ( reason==null?"":(": " + Debug.getNestedExceptionMessage( reason )));
		if( parent_transport != null ) {
			parent_transport.close( x );  //have the parent do the close if possible
		}
		else {
			light_transport.close( x );
		}
	}
	
		/**
		 * Marks the socket as complete and ready to undergo any close-delay prior to it being closed
		 * by the server
		 */
	
	public void
	closePending()
	{
		last_activity_time 	= System.currentTimeMillis();
		close_pending		= true;
	}
	
	public boolean
	isClosePending()
	{
		return( close_pending );
	}
	
	public SocketChannel getSocketChannel(){  return ((TransportEndpointTCP)light_transport.getTransportEndpoint()).getSocketChannel();  }
	
	
  /**
   * Get the last time this connection had read or write activity.
   * @return system time of last activity
   */
  public long getLastActivityTime() {  return last_activity_time;  }

  
  /**
   * Reset the last activity time to the current time.
   */
  public void resetLastActivityTime() {  last_activity_time = System.currentTimeMillis();  }
  
  public void
  setClosingReason(
		  Throwable	r )
  {
	  closing_reason = r;
  }
  
  public Throwable
  getClosingReason()
  {
	  return( closing_reason );
  }
  
  public Object
  getUserData(
	Object	key )
  {
	  Map	m = user_data;
	  
	  if ( m == null ){
		  
		  return( null );
	  }
	  
	  return( m.get(key));
  }
  
  public void
  setUserData(
	Object	key,
	Object	data )
  {
	try{  
		msg_mon.enter();
		
			// assumption is write infrequently, read often -> copy-on-write
		
		Map	m = (user_data==null)?new HashMap():new HashMap( user_data );
		
		m.put( key, data );
		
		user_data	= m;
	}finally{
		
		msg_mon.exit();
	}
  }
  
  
  public void setDebugString( String debug ) {  debug_string = debug;  }
  
  public String getDebugString() {  return debug_string;  }
  
  public void
  setMaximumMessageSize(
		int	max_bytes )
  {
	  if ( decoder != null ){
			decoder.setMaximumMessageSize( max_bytes );
	  }
  } 
}
