/*
 * Created on Jul 29, 2004
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

package com.aelitis.azureus.core.networkmanager.impl;


import java.io.IOException;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.Debug;


import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.Transport.ConnectListener;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;



/**
 *
 */

public class 
NetworkConnectionImpl
	extends NetworkConnectionHelper
	implements NetworkConnection 
{
  private final ConnectionEndpoint	connection_endpoint;
  
  private boolean connect_with_crypto;
  private boolean allow_fallback;
  private byte[][] shared_secrets;
  
  private ConnectionListener connection_listener;
  private boolean 	is_connected;
  private byte		is_lan_local	= AddressUtils.LAN_LOCAL_MAYBE;

  private final OutgoingMessageQueueImpl outgoing_message_queue;
  private final IncomingMessageQueueImpl incoming_message_queue;
  
  private Transport	transport;
  
  private volatile ConnectionAttempt	connection_attempt;
  private volatile boolean				closed;
  
  private double weight = -1;
  public double getWeight() { return weight; }
  public void setWeight( double inWeight ) { weight = inWeight; }
  
  /**
   * Constructor for new OUTbound connection.
   * The connection is not yet established upon instantiation; use connect() to do so.
   * @param _remote_address to connect to
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   */
  public NetworkConnectionImpl( 
		  		ConnectionEndpoint _target, MessageStreamEncoder encoder, 
		  		MessageStreamDecoder decoder, boolean _connect_with_crypto, boolean _allow_fallback,
		  		byte[][] _shared_secrets ) 
  {
	connection_endpoint	= _target;
    connect_with_crypto	= _connect_with_crypto;
    allow_fallback = _allow_fallback;
    shared_secrets = _shared_secrets;
    
    
    is_connected = false;
    outgoing_message_queue = new OutgoingMessageQueueImpl( encoder );
    incoming_message_queue = new IncomingMessageQueueImpl( decoder, this );
  }
  
  
  /**
   * Constructor for new INbound connection.
   * The connection is assumed to be already established, by the given already-connected channel.
   * @param _remote_channel connected by
   * @param data_already_read bytestream already read during routing
   * @param encoder default message stream encoder to use for the outgoing queue
   * @param decoder default message stream decoder to use for the incoming queue
   */
  public NetworkConnectionImpl( Transport _transport, MessageStreamEncoder encoder, MessageStreamDecoder decoder ) {
    transport = _transport;
    connection_endpoint = transport.getTransportEndpoint().getProtocolEndpoint().getConnectionEndpoint();
    is_connected = true;
    outgoing_message_queue = new OutgoingMessageQueueImpl( encoder );
    outgoing_message_queue.setTransport( transport );
    incoming_message_queue = new IncomingMessageQueueImpl( decoder, this );
  }
  

  public ConnectionEndpoint
  getEndpoint()
  {
	  return( connection_endpoint );
  }
  
  public void connect( boolean high_priority, ConnectionListener listener ) {
	  connect( null, high_priority, listener );
  }
  
  public void connect( ByteBuffer initial_outbound_data, boolean high_priority, ConnectionListener listener ) {
    this.connection_listener = listener;
    
    if( is_connected ){
    	
      connection_listener.connectStarted();
      
      connection_listener.connectSuccess( initial_outbound_data );
      
      return;
    }
    
    if ( connection_attempt != null ){
    	
    	Debug.out( "Connection attempt already active" );
    	
    	listener.connectFailure( new Throwable( "Connection attempt already active" ));
    	
    	return;
    }
    
    connection_attempt = 
    	connection_endpoint.connectOutbound( 
    			connect_with_crypto, 
    			allow_fallback, 
    			shared_secrets, 
    			initial_outbound_data,
    			high_priority,
    			new Transport.ConnectListener() {
			      public void connectAttemptStarted() {
			        connection_listener.connectStarted();
			      }
			      
			      public void connectSuccess( Transport	_transport, ByteBuffer remaining_initial_data ) {
			        is_connected = true;
			        transport	= _transport;
			        outgoing_message_queue.setTransport( transport );
			        connection_listener.connectSuccess( remaining_initial_data );
			        connection_attempt	= null;
			      }
			      
			      public void connectFailure( Throwable failure_msg ) {
			        is_connected = false;
			        connection_listener.connectFailure( failure_msg );
			      }
			    });
    
    if ( closed ){
    	
    	ConnectionAttempt	ca = connection_attempt;
    	
    	if ( ca != null ){
    		
    		ca.abandon();
    	}
    }
  }
  
  public Transport 
  detachTransport()
  {
	  Transport	t = transport;
	  
	  transport = new bogusTransport( transport );
	  
	  close();
	  
	  return( t );
  }
  
  public boolean isClosed(){
	  return closed;
  }
  
  public void close() {
	closed	= true;
  	NetworkManager.getSingleton().stopTransferProcessing( this );   
  
    if ( connection_attempt != null ){
    	connection_attempt.abandon();
    }
    if ( transport != null ){
    	transport.close( "Tidy close" );
    }
    incoming_message_queue.destroy();
   	outgoing_message_queue.destroy();  
    is_connected = false;
  }
  

  public void notifyOfException( Throwable error ) {
    if( connection_listener != null ) {
      connection_listener.exceptionThrown( error );
    }
    else {
      Debug.out( "notifyOfException():: connection_listener == null for exception: " +error.getMessage() );
    }
  }
  

  public OutgoingMessageQueue getOutgoingMessageQueue() {  return outgoing_message_queue;  }

  public IncomingMessageQueue getIncomingMessageQueue() {  return incoming_message_queue;  }
  

  public void 
  startMessageProcessing()
  {
  	NetworkManager.getSingleton().startTransferProcessing( this );
  }
  
  
  public void enableEnhancedMessageProcessing( boolean enable ) {
    if( enable ) {
    	NetworkManager.getSingleton().upgradeTransferProcessing( this );
    }
    else {
      NetworkManager.getSingleton().downgradeTransferProcessing( this );
    }
  }
  

  public Transport getTransport() {  return transport;  }
  
  public TransportBase getTransportBase() {  return transport;  }
  
  public int
  getMssSize()
  {
	  if ( transport == null ){
		  
		  return( NetworkManager.getMinMssSize());
		  
	  }else{
		  
		  return( transport.getMssSize());
	  }
  }
  

	
  public String toString() {
    return( transport==null?connection_endpoint.getDescription():transport.getDescription() );
  }


	public boolean isConnected() {
		return is_connected;
	}
  
	
	public boolean isLANLocal() {
		if ( is_lan_local == AddressUtils.LAN_LOCAL_MAYBE ){
			
			is_lan_local = AddressUtils.isLANLocalAddress( connection_endpoint.getNotionalAddress());
		}
		return( is_lan_local == AddressUtils.LAN_LOCAL_YES );
	}
	
	public String
	getString()
	{
		return( "tran=" + (transport==null?"null":transport.getDescription()+",w_ready=" + transport.isReadyForWrite(null)+",r_ready=" + transport.isReadyForRead( null ))+ ",in=" + incoming_message_queue.getPercentDoneOfCurrentMessage() + 
				",out=" + (outgoing_message_queue==null?0:outgoing_message_queue.getTotalSize()) + ",owner=" + (connection_listener==null?"null":connection_listener.getDescription()));
	}
	
	protected static class
	bogusTransport
		implements Transport
	{
		private Transport transport;
		
		protected
		bogusTransport(
			Transport	_transport )
		{
			transport = _transport;
		}
		
		public boolean 
		isReadyForWrite( 
			EventWaiter waiter )
		{
			return( false );
		}
		  		
		public boolean 
		isReadyForRead( 
			EventWaiter waiter )
		{
			return( false );
		}
		
		public boolean 
		isTCP()
		{
			return( transport.isTCP());
		}
		
		public String 
		getDescription()
		{
			return( transport.getDescription());
		}
		
		public int
		getMssSize()
		{
			return( transport.getMssSize());
		}

		public void 
		setAlreadyRead( 
			ByteBuffer bytes_already_read )
		{
			Debug.out( "Bogus Transport Operation" );
		}

		public TransportEndpoint 
		getTransportEndpoint()
		{
			return( transport.getTransportEndpoint());
		}

		public boolean 
		isEncrypted()
		{
			return( transport.isEncrypted());
		}

		public String 
		getEncryption()
		{
			return( transport.getEncryption());
		}

		public void 
		setReadyForRead()
		{
			Debug.out( "Bogus Transport Operation" );
		}

		public long 
		write( 
			ByteBuffer[] buffers, 
			int array_offset, 
			int length ) 
		
			throws IOException
		{
			Debug.out( "Bogus Transport Operation" );

			throw( new IOException( "Bogus transport!" ));
		}

		public long 
		read( 
			ByteBuffer[] buffers, int array_offset, int length )
		
			throws IOException
		{
			Debug.out( "Bogus Transport Operation" );

			throw( new IOException( "Bogus transport!" ));
		}

		public void 
		setTransportMode( 
			int mode )
		{
			Debug.out( "Bogus Transport Operation" );
		}

		public int 
		getTransportMode()
		{
			return( transport.getTransportMode());
		}

		public void
		connectOutbound(
			ByteBuffer			initial_data,
			ConnectListener 	listener,
			boolean				high_priority )
		{
			Debug.out( "Bogus Transport Operation" );
			
			listener.connectFailure( new Throwable( "Bogus Transport" ));
		}

		public void
		connectedInbound()
		{
			Debug.out( "Bogus Transport Operation" );
		}
		
		public void 
		close( 
			String reason )
		{
			// we get here after detaching a transport and then closing the peer connection
		}

		public void
		setTrace(
			boolean	on )
		{
		}
	}
}
