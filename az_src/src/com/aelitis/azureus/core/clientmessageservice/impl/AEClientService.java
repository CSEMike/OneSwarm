/*
 * Created on Oct 31, 2005
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
import java.nio.ByteBuffer;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.clientmessageservice.*;
import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;


/**
 * 
 */
public class AEClientService implements ClientMessageService {
	
	private final String address;
	private final int port;
	private final String msg_type_id;
	private final int timeout_secs;
	private int max_message_bytes	= -1;
	private ClientConnection conn;
	
	private final AESemaphore read_block = new AESemaphore( "AEClientService:R" );
	private final AESemaphore write_block = new AESemaphore( "AEClientService:W" );
  
	private final ArrayList received_messages = new ArrayList();  
  
	private final NonBlockingReadWriteService rw_service;
	
	private volatile Throwable error;
	
  
	public AEClientService( String server_address, int server_port, String _msg_type_id ) {

		this( server_address, server_port, 30, _msg_type_id );
	}
  
	public AEClientService( String server_address, int server_port, int timeout, String _msg_type_id ) {
		this.address = server_address;
		this.port = server_port;
		this.timeout_secs = timeout;
		this.msg_type_id = _msg_type_id;
		
		try {
			AZMessageFactory.registerGenericMapPayloadMessageType( msg_type_id );  //register for incoming type decoding
		}
		catch( MessageException me ) {  /*ignore, since message type probably already registered*/ }
		
		rw_service = new NonBlockingReadWriteService( msg_type_id, timeout, 0, new NonBlockingReadWriteService.ServiceListener() {			
			public void messageReceived( ClientMessage message ) {
				received_messages.add( message.getPayload() );
				read_block.release();
			}
			
			public void connectionError( ClientConnection connection, Throwable msg ) {
				error = msg;
				read_block.releaseForever();
				write_block.releaseForever();
			}
		});
	}
	
	
	

	//NOTE: blocking op
	private void connect() throws IOException {
		
	InetSocketAddress	tcp_target = new InetSocketAddress( address, port );
	
	ConnectionEndpoint	ce = new ConnectionEndpoint( tcp_target );
	
	ProtocolEndpointFactory.createEndpoint( ProtocolEndpoint.PROTOCOL_TCP, ce, tcp_target );
	   
    final AESemaphore connect_block = new AESemaphore( "AEClientService:C" );
    
    ce.connectOutbound( false, false, null, null, ProtocolEndpoint.CONNECT_PRIORITY_MEDIUM, new Transport.ConnectListener() {  //NOTE: async operation!
    	public int connectAttemptStarted( int default_connect_timeout ) { return( default_connect_timeout ); }
      
    	public void connectSuccess(Transport transport, ByteBuffer remaining_initial_data ){
    		conn = new ClientConnection((TCPTransportImpl)transport );
    		if ( max_message_bytes != -1 ){
    			conn.setMaximumMessageSize( max_message_bytes );
    		}
    		connect_block.release();       
    	}
     
    	public void connectFailure( Throwable failure_msg ) {
    		error = failure_msg;
    		connect_block.release();  
    	}
    });
    
    if ( !connect_block.reserve( timeout_secs*1000 )){
        throw new IOException( "connect op failed: timeout" );
    }
    
    //connect op finished   
    
    if( error != null ) {  //connect failure
      close();
      throw new IOException( "connect op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
    }
        
    rw_service.addClientConnection( conn );  //register for read/write handling
	}
	
	

	
	public void sendMessage( Map message ) throws IOException {
		if( conn == null ) {  //not yet connected
			connect();
		}
		
		if( error != null ) {
		    close();
		    throw new IOException( "send op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
		}
		
		ClientMessage client_msg = new ClientMessage( msg_type_id, conn, message, new ClientMessageHandler() {
			public String getMessageTypeID(){  return msg_type_id;  }

			public void processMessage( ClientMessage message ) {
				Debug.out( "ERROR: should never be called" );
			}

			public void sendAttemptCompleted( ClientMessage message ){
				write_block.release();
			}
			public void sendAttemptFailed( ClientMessage message, Throwable cause) {
				error = cause;
				write_block.release();
			}
		});
		
		rw_service.sendMessage( client_msg );  //queue message for sending	

		write_block.reserve();  //block until send completes
		
		//send op finished
    
    if( error != null ) {  //connect failure
      close();
      throw new IOException( "send op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
    }
	}
	
	
	

	public Map receiveMessage() throws IOException {
		if( conn == null ) {  //not yet connected
			connect();
		}	
		
		read_block.reserve();  //block until receive completes

		if( !received_messages.isEmpty() ) {  //there were still read messages left from the previous read call
			Map recv_msg = (Map)received_messages.remove( 0 );
			return recv_msg;
		}
		
		//receive op finished	
    
		if (error == null ){
			error = new IOException( "receive op inconsistent" );
		}
		
		close();
		throw new IOException( "receive op failed: " + error.getMessage() == null ? "[]" : error.getMessage() );
	}
	
	
	
	//no handler notification
	public void close() {
		if( conn != null ) {
			rw_service.removeClientConnection( conn );
			conn.close( new Exception( "Connection closed" ));
		}
		rw_service.destroy();
	}
	
	public void
	setMaximumMessageSize( int max_bytes )
	{
		max_message_bytes	= max_bytes;
	}
}
