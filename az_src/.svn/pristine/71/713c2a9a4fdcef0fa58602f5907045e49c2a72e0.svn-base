/*
 * Created on 9 Aug 2006
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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.plugins.messaging.MessageException;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnection;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageConnectionListener;
import org.gudy.azureus2.plugins.messaging.generic.GenericMessageEndpoint;
import org.gudy.azureus2.plugins.network.RateLimiter;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

import com.aelitis.azureus.core.nat.NATTraversalObserver;
import com.aelitis.azureus.core.nat.NATTraverser;

public class 
GenericMessageConnectionImpl
	implements GenericMessageConnection
{
	private static final boolean TRACE			= false;
	private static final boolean TEST_TUNNEL	= false;
	
	static{
		if ( TEST_TUNNEL ){
			System.out.println( "**** GenericMessageConnection: TEST_TUNNEL on ****" );
		}
		if ( TRACE ){
			System.out.println( "**** GenericMessageConnection: TRACE on **** " );
		}
	}
	
	private MessageManagerImpl					message_manager;
	
	private String								msg_id;
	private String								msg_desc;
	private GenericMessageEndpointImpl			endpoint;
	private int									stream_crypto;
	byte[][]									shared_secrets;
	
	private boolean								incoming;
	
	private volatile GenericMessageConnectionAdapter		delegate;
	private volatile boolean								closing;
	private volatile boolean								closed;
	private volatile boolean								connecting;
	
	private List	listeners	= new ArrayList();

	private int	connect_method_count;
	
	private List				inbound_rls;
	private List				outbound_rls;

	protected
	GenericMessageConnectionImpl(
		MessageManagerImpl				_message_manager,
		GenericMessageConnectionAdapter	_delegate )
	{
		message_manager	= _message_manager;
		delegate		= _delegate;
		
		incoming	= true;
		
		connect_method_count = 1;
		
		delegate.setOwner( this );
	}
	
	protected 
	GenericMessageConnectionImpl(
		MessageManagerImpl			_message_manager,
		String						_msg_id,
		String						_msg_desc,
		GenericMessageEndpointImpl	_endpoint,
		int							_stream_crypto,
		byte[][]					_shared_secrets )
	{
		message_manager	= _message_manager;
		msg_id			= _msg_id;
		msg_desc		= _msg_desc;
		endpoint		= _endpoint;
		stream_crypto	= _stream_crypto;
		shared_secrets	= _shared_secrets;
		
		connect_method_count = endpoint.getConnectionEndpoint().getProtocols().length;
		
		incoming	= false;
	}
	
	public GenericMessageEndpoint
	getEndpoint()
	{
		return( endpoint==null?delegate.getEndpoint():endpoint);
	}
	
	public int
	getMaximumMessageSize()
	{
		return( delegate==null?GenericMessageConnectionIndirect.MAX_MESSAGE_SIZE:delegate.getMaximumMessageSize());
	}
	
	public String
	getType()
	{
		if ( delegate == null ){
			
			return( "" );
			
		}else{
	
			return( delegate.getType());
		}
	}
	
	public int
	getTransportType()
	{
		if ( delegate == null ){
			
			return( TT_NONE );
		}else{
			
			return( delegate.getTransportType());
		}
	}
	

	public void
	addInboundRateLimiter(
		RateLimiter		limiter )
	{
		synchronized( this ){
			
			if ( delegate != null ){
				
				delegate.addInboundRateLimiter( limiter );

			}else{
				
				if ( inbound_rls == null ){
					
					inbound_rls = new ArrayList();
				}
				
				inbound_rls.add( limiter );
			}
		}
	}
	
	public void
	removeInboundRateLimiter(
		RateLimiter		limiter )
	{
		synchronized( this ){
			
			if ( delegate != null ){
				
				delegate.removeInboundRateLimiter( limiter );

			}else{
				
				if ( inbound_rls != null ){
										
					inbound_rls.remove( limiter );
				}
			}
		}
	}
	
	public void
	addOutboundRateLimiter(
		RateLimiter		limiter )
	{
		synchronized( this ){
			
			if ( delegate != null ){
				
				delegate.addOutboundRateLimiter( limiter );

			}else{
				
				if ( outbound_rls == null ){
					
					outbound_rls = new ArrayList();
				}
				
				outbound_rls.add( limiter );
			}
		}
	}
	
	public void
	removeOutboundRateLimiter(
		RateLimiter		limiter )
	{
		synchronized( this ){
			
			if ( delegate != null ){
				
				delegate.removeOutboundRateLimiter( limiter );

			}else{
				
				if ( outbound_rls != null ){
										
					outbound_rls.remove( limiter );
				}
			}
		}
	}
	
	public boolean
	isIncoming()
	{
		return( incoming );
	}
	
	public int
	getConnectMethodCount()
	{
		return( connect_method_count );
	}
	
	public void
	connect()
	
		throws MessageException
	{
		connect( null );
	}
	
	protected void
	setDelegate(
		GenericMessageConnectionAdapter		_delegate )
	{
	    synchronized( this ){
	    	
			delegate 	= _delegate;
			
	    	if ( inbound_rls != null ){
	    		
	    		for (int i=0;i<inbound_rls.size();i++){
	    			
	    			delegate.addInboundRateLimiter((RateLimiter)inbound_rls.get(i));
	    		}
	    		
	    		inbound_rls = null;
	    	}
	    	
	    	if ( outbound_rls != null ){
	    		
	    		for (int i=0;i<outbound_rls.size();i++){
	    			
	    			delegate.addOutboundRateLimiter((RateLimiter)outbound_rls.get(i));
	    		}
	    		
	    		inbound_rls = null;
	    	}
	    }
	}
	
		/**
		 * Outgoing connection
		 * @param initial_data
		 * @throws MessageException
		 */
	
	public void
	connect(
		ByteBuffer	initial_data )
	
		throws MessageException
	{
		if ( incoming ){
			
			throw( new MessageException( "Already connected" ));
		}
		
		if ( connecting ){
			
			throw( new MessageException( "Connect already performed" ));
		}
		
		connecting	= true;
		
		if ( closed ){
			
			throw( new MessageException( "Connection has been closed" ));
		}
		
		InetSocketAddress	tcp_ep = endpoint.getTCP();
				
		if ( tcp_ep != null ){
			
			connectTCP( initial_data, tcp_ep );
			
		}else{
			
			InetSocketAddress	udp_ep = endpoint.getUDP();

			if ( udp_ep != null ){
				
				connectUDP( initial_data, udp_ep, false );
				
			}else{
				
				throw( new MessageException( "No protocols availabld" ));
			}
		}
	}
	
	protected void
	connectTCP(
		final ByteBuffer		initial_data,
		InetSocketAddress		tcp_ep )
	{
		if ( TRACE ){
			System.out.println( "TCP connection attempt to " + tcp_ep  );
		}
		
		GenericMessageEndpointImpl	gen_tcp = new GenericMessageEndpointImpl( endpoint.getNotionalAddress());
		
		gen_tcp.addTCP( tcp_ep );
		
		final GenericMessageConnectionDirect tcp_delegate = new GenericMessageConnectionDirect( msg_id, msg_desc, gen_tcp, stream_crypto, shared_secrets );
			
		tcp_delegate.setOwner( this );

		tcp_delegate.connect( 
				initial_data,
				new GenericMessageConnectionAdapter.ConnectionListener()
				{
					private boolean	connected;
					
					public void
					connectSuccess()
					{
						connected	= true;
						
						setDelegate( tcp_delegate );
						
						if ( closed ){
							
							try{
								delegate.close();
								
							}catch( Throwable e ){
							}
							
							reportFailed( new MessageException( "Connection has been closed" ));

						}else{
							
							reportConnected();
						}
					}
					
					public void 
					connectFailure( 
						Throwable failure_msg )
					{
						InetSocketAddress	udp_ep = endpoint.getUDP();

						if ( udp_ep != null && !connected ){
							
							initial_data.rewind();
							
							connectUDP( initial_data, udp_ep, false );
							
						}else{
							
							reportFailed( failure_msg );
						}
					}
				});
	}
	
	protected void
	connectUDP(
		final ByteBuffer			initial_data,
		final InetSocketAddress		udp_ep,
		boolean						nat_traversal )
	{
		if ( TRACE ){
			System.out.println( "UDP connection attempt to " + udp_ep + " (nat=" + nat_traversal + ")" );
		}

		final GenericMessageEndpointImpl	gen_udp = new GenericMessageEndpointImpl( endpoint.getNotionalAddress());
		
		gen_udp.addUDP( udp_ep );
		
		final GenericMessageConnectionAdapter udp_delegate = new GenericMessageConnectionDirect( msg_id, msg_desc, gen_udp, stream_crypto, shared_secrets );
		
		udp_delegate.setOwner( this );
		
		if ( nat_traversal || TEST_TUNNEL ){
			
			final NATTraverser	nat_traverser = message_manager.getNATTraverser();
			
			Map	request = new HashMap();
								
			nat_traverser.attemptTraversal(
					message_manager,
					udp_ep,
					request,
					false,
					new NATTraversalObserver()
					{
						public void
						succeeded(
							final InetSocketAddress	rendezvous,
							final InetSocketAddress	target,
							Map						reply )
						{
							if ( closed ){
															
								reportFailed( new MessageException( "Connection has been closed" ));

							}else{
								
								connect_method_count++;

								if ( TEST_TUNNEL ){
									
									initial_data.rewind();
									
									connectTunnel( initial_data, gen_udp, rendezvous, target );
									
								}else{
								
									udp_delegate.connect( 
											initial_data,
											new GenericMessageConnectionAdapter.ConnectionListener()
											{
												private boolean	connected;
												
												public void
												connectSuccess()
												{
													connected	= true;
													
													setDelegate( udp_delegate );
													
													if ( closed ){
														
														try{
															delegate.close();
															
														}catch( Throwable e ){													
														}
														
														reportFailed( new MessageException( "Connection has been closed" ));
														
													}else{
														
														reportConnected();
													}												
												}
												
												public void 
												connectFailure( 
													Throwable failure_msg )
												{
													if ( connected ){
														
														reportFailed( failure_msg );
														
													}else{
														
														initial_data.rewind();
	
														connectTunnel( initial_data, gen_udp, rendezvous, target );
													}
												}
											});
								}
							}
						}
						
						public void
						failed(
							int			failure_type )
						{
							reportFailed( new MessageException( "UDP connection attempt failed - NAT traversal failed (" + NATTraversalObserver.FT_STRINGS[ failure_type ] + ")"));
						}
						
						public void
						failed(
							Throwable 	cause )
						{
							reportFailed( cause );
						}
						
						public void
						disabled()
						{
							reportFailed( new MessageException( "UDP connection attempt failed as DDB is disabled" ));
						}
					});
		}else{
	
			udp_delegate.connect( 
					initial_data,
					new GenericMessageConnectionAdapter.ConnectionListener()
					{
						private boolean	connected;
						
						public void
						connectSuccess()
						{
							connected	= true;
							
							setDelegate( udp_delegate );
							
							if ( closed ){
								
								try{
									delegate.close();
									
								}catch( Throwable e ){	
								}
								
								reportFailed( new MessageException( "Connection has been closed" ));

							}else{
								
								reportConnected();
							}
						}
						
						public void 
						connectFailure( 
							Throwable failure_msg )
						{
							if ( connected ){
								
								reportFailed( failure_msg );

							}else{
								
								initial_data.rewind();
									
								connectUDP( initial_data, udp_ep, true );
							}
						}
					});
		}
	}
	
	protected void
	connectTunnel(
		ByteBuffer				initial_data,
		GenericMessageEndpoint	ep,
		InetSocketAddress		rendezvous,
		InetSocketAddress		target )
	{
		if ( TRACE ){
			System.out.println( "Tunnel connection attempt to " + target + " (rendezvous=" + rendezvous + ")" );
		}
		
		final GenericMessageConnectionIndirect tunnel_delegate = 
			new GenericMessageConnectionIndirect( message_manager, msg_id, msg_desc, ep, rendezvous, target );
		
		tunnel_delegate.setOwner( this );
		
		tunnel_delegate.connect( 
				initial_data,
				new GenericMessageConnectionAdapter.ConnectionListener()
				{
					public void
					connectSuccess()
					{
						setDelegate( tunnel_delegate );
						
						if ( closed ){
							
							try{
								delegate.close();
								
							}catch( Throwable e ){
								
							}
							
							reportFailed( new MessageException( "Connection has been closed" ));

						}else{
							
							reportConnected();
						}
					}
					
					public void 
					connectFailure( 
						Throwable failure_msg )
					{
						reportFailed( failure_msg );
					}
				});
	}
	
		/**
		 * Incoming connection has been accepted
		 *
		 */
	
	protected void
	accepted()
	{
		delegate.accepted();
	}
	
	public void
	send(
		PooledByteBuffer			message )
	
		throws MessageException
	{
		int	size = ((PooledByteBufferImpl)message).getBuffer().remaining( DirectByteBuffer.SS_EXTERNAL );
		
		if ( size > getMaximumMessageSize()){
			
			throw( new MessageException( "Message is too large: supplied is " + size + ", maximum is " + getMaximumMessageSize()));
		}
		
		delegate.send( message );
	}
	
	protected void
	receive(
		GenericMessage	message )
	{
		boolean	handled = false;
		
		for (int i=0;i<listeners.size();i++){
			
			PooledByteBuffer	buffer = new PooledByteBufferImpl(message.getPayload());
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).receive( this, buffer );
				
				handled = true;
				
			}catch( Throwable f ){
				
				buffer.returnToPool();
				
				if ( !( f instanceof MessageException )){
				
					Debug.printStackTrace(f);
				}
			}
		}
		
		if ( !handled && !( closed || closing )){
			
			Debug.out( "GenericMessage: incoming message not handled" );
		}
	}
	
	public void
	closing()
	{
		closing = true;
	}
	
	public void
	close()
	
		throws MessageException
	{
		closed	= true;
		
		if ( delegate != null ){
			
			delegate.close();
		}
	}
	
	protected void
	reportConnected()
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).connected( this );
				
			}catch( Throwable f ){
				
				Debug.printStackTrace(f);
			}
		}
	}
	
	protected void
	reportFailed(
		Throwable	e )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((GenericMessageConnectionListener)listeners.get(i)).failed( this, e );
				
			}catch( Throwable f ){
				
				Debug.printStackTrace(f);
			}
		}
	}
	
	public void
	addListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.add( listener );
	}
	
	public void
	removeListener(
		GenericMessageConnectionListener		listener )
	{
		listeners.remove( listener );
	}
}
