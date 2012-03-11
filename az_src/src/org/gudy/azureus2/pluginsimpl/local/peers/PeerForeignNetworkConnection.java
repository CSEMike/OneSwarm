/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.pluginsimpl.local.peers;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.peers.Peer;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.NetworkConnectionHelper;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.TransportBase;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

public class 
PeerForeignNetworkConnection
	extends NetworkConnectionHelper
{
	private Peer				peer;
	
	private OutgoingMessageQueue	outgoing_message_queue = new omq();
	private IncomingMessageQueue	incoming_message_queue = new imq();
	
	private TransportBase			transport_base	= new tp();
			
	protected
	PeerForeignNetworkConnection(
		Peer		_peer )
	{
		peer	= _peer;
	}
	
	public ConnectionEndpoint
	getEndpoint()
	{
			// make up a vaguely usable endpoint

		return( new ConnectionEndpoint(new InetSocketAddress( peer.getIp(), peer.getPort())));
	}
	
	public void 
	notifyOfException( 
		Throwable error )
	{
		Debug.printStackTrace( error );
	}
	  
	public OutgoingMessageQueue 
	getOutgoingMessageQueue()
	{
		return( outgoing_message_queue );
	}
	  
	  
	public IncomingMessageQueue 
	getIncomingMessageQueue()
	{
		return( incoming_message_queue );
	}
	
	public TransportBase 
	getTransportBase()
	{
		return( transport_base );
	}
	 
	public int
	getMssSize()
	{
		return( NetworkManager.getMinMssSize() );
	}
		 
	public boolean 
	isLANLocal()
	{
		return( false );
	}
	
	public String
	getString()
	{
		String	peer_str = peer.getClass().getName();
		
		int	pos = peer_str.lastIndexOf('.');
		
		if ( pos != -1 ){
			
			peer_str = peer_str.substring( pos+1 );
		}
		
		peer_str += " " + peer.getIp() + ":" + peer.getPort();
		
		return( "peer=" + peer_str + ",in=" + incoming_message_queue.getPercentDoneOfCurrentMessage() + 
				",out=" + outgoing_message_queue.getTotalSize());
	}
	protected class
	tp
		implements TransportBase
	{	
		private long	last_ready_for_read	= SystemTime.getSteppedMonotonousTime();
		
		public boolean 
		isReadyForWrite( 
			EventWaiter waiter )
		{
			return( false );
		}
		 
		public long 
		isReadyForRead( 
			EventWaiter waiter )
		{
			long	now = SystemTime.getSteppedMonotonousTime();
			
			if ( peer.isTransferAvailable()){
				
				last_ready_for_read = now;
				
				return( 0 );
			}
						
			long	diff = now - last_ready_for_read + 1;	// make sure > 0
						
			return( diff );
		}
	
		public boolean
		isTCP()
		{
				// we don't know (or care?)
			
			return( false );
		}
		
		public String 
		getDescription()
		{
			return( "Peer transport delegate" );
		}
	}
	
	protected class
	imq
		implements IncomingMessageQueue
	{
		public void 
		setDecoder( 
			MessageStreamDecoder new_stream_decoder )
		{
		}
		  
		public MessageStreamDecoder
		getDecoder()
		{
			throw( new RuntimeException( "Not imp" ));
		}
		  
		public int 
		getPercentDoneOfCurrentMessage()
		{
			return( 0 );
		}
		  
		public int 
		receiveFromTransport( 
			int max_bytes ) throws IOException
		{
			return( peer.readBytes( max_bytes ));
		}
		 
		public void 
		notifyOfExternallyReceivedMessage( 
			Message message )
		{	
		}

		public void 
		resumeQueueProcessing()
		{	
		}
	
		public void 
		registerQueueListener( 
			MessageQueueListener listener )
		{	
		}
		 
		public void 
		cancelQueueListener( 
			MessageQueueListener listener )
		{	
		}

		public void 
		destroy()
		{
		}
		 
	}
	
	protected class
	omq
		implements OutgoingMessageQueue
	{
		public void
		setTransport(
			Transport		_transport )
		{
		}
			  
		public int
		getMssSize()
		{
			return( PeerForeignNetworkConnection.this.getMssSize());
		}

		public void
		setEncoder( 
			MessageStreamEncoder stream_encoder ) 
		{
		}	 
		  
		public MessageStreamEncoder
		getEncoder()
		{
			throw( new RuntimeException( "Not imp" ));
		}		  

		public int 
		getPercentDoneOfCurrentMessage()
		{
			return( 0 );
		}

		public void 
		destroy()
		{
		}
		 
		public void flush() 
		{
		}
		
		public boolean 
		isDestroyed() 
		{
			return false;
		}
		
		public int 
		getTotalSize()
		{
			return( 0 );
		}
		  
		public int
		getDataQueuedBytes()
		{
			return( 0 );
		}
		  
		public int
		getProtocolQueuedBytes()
		{
			return( 0 );
		}
		
		public boolean 
		getPriorityBoost()
		{
			return( false );
		}
			
		public void 
		setPriorityBoost( 
			boolean	boost )
		{
		}

		public boolean
		isBlocked()
		{
			return( false );
		}
		
		public boolean 
		hasUrgentMessage()
		{
			return( false );
		}
		  
		public void 
		addMessage( 
			Message message, 
			boolean manual_listener_notify )
		{
			throw( new RuntimeException( "Not imp" ));
		}
		 
		public void 
		removeMessagesOfType( 
			Message[] message_types, 
			boolean manual_listener_notify )
		{
			throw( new RuntimeException( "Not imp" ));
		}
		
		public boolean 
		removeMessage( 
			Message message, 
			boolean manual_listener_notify )
		{
			throw( new RuntimeException( "Not imp" ));
		}
		 
		public int 
		deliverToTransport( 
			int 		max_bytes, 
			boolean 	manual_listener_notify ) 
		
			throws IOException
		{	
			throw( new RuntimeException( "Not imp" ));
		}

		public void 
		doListenerNotifications()
		{
		}
		  
		public void
		setTrace(
				boolean	on )
		{
		}
		
		public String 
		getQueueTrace()
		{
			return( "" );
		}
		
		public void 
		registerQueueListener( 
			MessageQueueListener listener )
		{
		}

		public void 
		cancelQueueListener( 
			MessageQueueListener listener )
		{
		}
		 
		public void 
		notifyOfExternallySentMessage( 
			Message message )
		{
		}
	}
}
