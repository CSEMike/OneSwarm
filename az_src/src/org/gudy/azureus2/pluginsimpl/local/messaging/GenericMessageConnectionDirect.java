/*
 * Created on 19 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.messaging;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.MessageManager;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messaging.Message;

public class 
GenericMessageConnectionDirect 
	implements GenericMessageConnectionAdapter
{	
	public static final int MAX_MESSAGE_SIZE	= GenericMessageDecoder.MAX_MESSAGE_LENGTH;

	protected static GenericMessageConnectionDirect
	receive(
		GenericMessageEndpointImpl	endpoint,
		String				msg_id,
		String				msg_desc,
		int					stream_crypto,
		byte[][]			shared_secrets )
	{		
		GenericMessageConnectionDirect direct_connection = new GenericMessageConnectionDirect( msg_id, msg_desc, endpoint, stream_crypto, shared_secrets );
		
		return( direct_connection );
	}
	
	private GenericMessageConnectionImpl	owner;
	
	private String						msg_id;
	private String						msg_desc;
	private int							stream_crypto;
	private byte[][]					shared_secrets;
	private GenericMessageEndpointImpl	endpoint;
	private NetworkConnection			connection;
	
	private volatile boolean	connected;
	private volatile boolean	closed;
	

		
	protected 
	GenericMessageConnectionDirect(
		String						_msg_id,
		String						_msg_desc,
		GenericMessageEndpointImpl	_endpoint,
		int							_stream_crypto,
		byte[][]					_shared_secrets )
	{
		msg_id			= _msg_id;
		msg_desc		= _msg_desc;
		endpoint		= _endpoint;
		stream_crypto	= _stream_crypto;
		shared_secrets	= _shared_secrets;
	}
	
	public void
	setOwner(
		GenericMessageConnectionImpl	_owner )
	{
		owner	= _owner;
	}
	
	public int
	getMaximumMessageSize()
	{
		return( MAX_MESSAGE_SIZE );
	}
	
		/**
		 * Incoming connect call
		 * @param _connection
		 */
	
	protected void
	connect(
		NetworkConnection		_connection )
	{
		connection		= _connection;

		connection.connect(
				false,
				new NetworkConnection.ConnectionListener()
				{
					public void 
					connectStarted()
					{
					}
	
					public void 
					connectSuccess(
						ByteBuffer remaining_initial_data )
					{
						connected	= true;
					}
					    
					public void 
					connectFailure( 
						Throwable failure_msg )
					{
						owner.reportFailed( failure_msg );
						
						connection.close();
					}
					    
					public void 
					exceptionThrown( 
						Throwable error )
					{
						owner.reportFailed( error );
						
						connection.close();
					}
					
					public String 
					getDescription() 
					{
						return( "generic connection: " + connection.getString());
					}
				});
	}
	
	public void
	accepted()
	{
		startProcessing();
	}
	
	public GenericMessageEndpoint
	getEndpoint()
	{
		return( endpoint );
	}
	
	public void
	connect(
		ByteBuffer													upper_initial_data,
		final GenericMessageConnectionAdapter.ConnectionListener	listener )
	{
		if ( connected ){
			
			return;
		}
				
		connection = 
			NetworkManager.getSingleton().createConnection( 
				endpoint.getConnectionEndpoint(),
				new GenericMessageEncoder(),
				new GenericMessageDecoder( msg_id, msg_desc ),
				stream_crypto != MessageManager.STREAM_ENCRYPTION_NONE, 			// use crypto
				stream_crypto != MessageManager.STREAM_ENCRYPTION_RC4_REQUIRED, 	// allow fallback
				shared_secrets );
		
		ByteBuffer	initial_data = ByteBuffer.wrap( msg_id.getBytes());
		
		if ( upper_initial_data != null ){
		
			GenericMessage	gm = new GenericMessage( msg_id, msg_desc, new DirectByteBuffer( upper_initial_data ), false );
			
			DirectByteBuffer[]	payload = new GenericMessageEncoder().encodeMessage( gm )[0].getRawData();
			
			int	size = initial_data.remaining();
			
			for (int i=0;i<payload.length;i++){
				
				size += payload[i].remaining( DirectByteBuffer.SS_MSG );
			}
			
			ByteBuffer	temp = ByteBuffer.allocate( size );
			
			temp.put( initial_data );
			
			for (int i=0;i<payload.length;i++){
			
				temp.put( payload[i].getBuffer( DirectByteBuffer.SS_MSG ));
			}
			
			temp.rewind();
			
			initial_data = temp;
		}
						
		connection.connect(
				initial_data,
				false,
				new NetworkConnection.ConnectionListener()
				{
					public void 
					connectStarted()
					{
					}
	
					public void 
					connectSuccess(
						ByteBuffer remaining_initial_data )
					{
						connected	= true;
														
						try{
						    
						    if ( remaining_initial_data != null && remaining_initial_data.remaining() > 0){
						    	
						    		// queue as a *raw* message as already encoded
						    	
								connection.getOutgoingMessageQueue().addMessage( 
										new GenericMessage(  msg_id, msg_desc, new DirectByteBuffer( remaining_initial_data ), true), false );
						    }
						    
						    listener.connectSuccess();
							
							startProcessing();

						}catch( Throwable e ){
							
							connectFailure( e );
						}
					}
					    
					public void 
					connectFailure( 
						Throwable failure_msg )
					{
						listener.connectFailure( failure_msg );
						
						connection.close();
					}
					    
					public void 
					exceptionThrown( 
						Throwable error )
					{
						listener.connectFailure( error );
						
						connection.close();
					}
					
					public String 
					getDescription() 
					{
						return( "generic connection: " + connection.getString());
					}
				});
	}
	
	protected void
	startProcessing()
	{
	    connection.getIncomingMessageQueue().registerQueueListener( 
	    		new IncomingMessageQueue.MessageQueueListener()
	    		{
	    			public boolean 
	    			messageReceived( 
	    				Message 	_message )
	    			{
	    				GenericMessage	message = (GenericMessage)_message;
	    						   
	    				owner.receive( message );
	    				
	    				return( true );
	    			}
	
	    			public void 
	    			protocolBytesReceived( 
	    				int byte_count )
	    			{
	    			}
	    			    
	    			public void 
	    			dataBytesReceived( 
	    				int byte_count )
	    			{	
	    			}
	    		});
	    
	    connection.getOutgoingMessageQueue().registerQueueListener( 
	    		new OutgoingMessageQueue.MessageQueueListener() 
	    		{
	    			public boolean 
	    			messageAdded( 
	    				Message message )
	    			{
	    				//System.out.println( "    added: " + message );
	    				
	    				return( true );
	    			}
	    			    
	    			public void 
	    			messageQueued( 
	    				Message message )
	    			{
	    				//System.out.println( "    queued: " + message );
	    			}
	    			    
	   			    public void 
	   			    messageRemoved( 
	   			    	Message message )
	   			    {
	   			    	//System.out.println( "    removed: " + message );
	   			    }
	    			    
		    		public void 
		    		messageSent( 
		    			Message message )
		    		{
		    			//System.out.println( "    sent: " + message );
		    		}
	    			    
	    			public void 
	    			protocolBytesSent( 
	    				int byte_count )
	    			{
	    			}
	
	  			    public void 
	  			    dataBytesSent( 
	  			    	int byte_count )
	  			    {  			    	
	  			    }
	  			    
	  			    public void flush(){}
	    		});
	}
	
	public void
	send(
		PooledByteBuffer			data )
	
		throws MessageException
	{
		if ( !connected ){
			
			throw( new MessageException( "not connected" ));
		}
		
		PooledByteBufferImpl	impl = (PooledByteBufferImpl)data;
		
		try{
			connection.getOutgoingMessageQueue().addMessage( 
					new GenericMessage( msg_id, msg_desc, impl.getBuffer(), false ), false );
			
		}catch( Throwable e ){
			
			throw( new MessageException( "send failed", e ));
		}
	}
	
	public void
	close()		
	
		throws MessageException
	{
		if ( !connected ){
			
			throw( new MessageException( "not connected" ));
		}	
		
		if ( !closed ){
	
			closed	= true;
			
			connection.close();
		}
	}
}
