/*
 * Created on 17-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelector;
import com.aelitis.azureus.core.networkmanager.VirtualServerChannelSelectorFactory;
import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector.VirtualSelectorListener;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoder;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoderInitial;
import com.aelitis.azureus.core.networkmanager.impl.ProtocolDecoderAdapter;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelper;

public class 
PHETester 
{
	private final VirtualChannelSelector connect_selector = new VirtualChannelSelector( "PHETester", VirtualChannelSelector.OP_CONNECT, true );
	 
	private byte[]	TEST_HEADER	= "TestHeader".getBytes();
	
	private static boolean	OUTGOING_PLAIN = false;
	
	private static byte[]	shared_secret = "sdsjdksjdkj".getBytes();
	
	public
	PHETester()
	{
		ProtocolDecoder.addSecrets( new byte[][]{ shared_secret });
		
		VirtualServerChannelSelector
			accept_server = VirtualServerChannelSelectorFactory.createNonBlocking( 
					new InetSocketAddress( 8765 ), 
					0, 
					new VirtualServerChannelSelector.SelectListener() 
					{
						public void 
						newConnectionAccepted( 
							ServerSocketChannel	server,
							SocketChannel 		channel ) 
						{      
							incoming( channel );
						}
					});
		
		accept_server.start();
	
		new Thread()
		{
			public void
			run()
			{
				while( true ){
					try{
						connect_selector.select( 100 );
					}
					catch( Throwable t ) {
					  Debug.out( "connnectSelectLoop() EXCEPTION: ", t );
					}
				}
			}
		}.start();
		
		outgoings();
	}
	
	protected void
	incoming(
		SocketChannel	channel )
	{
		try{
			TransportHelper	helper = new TCPTransportHelper( channel );
			
			final ProtocolDecoderInitial	decoder = 
				new ProtocolDecoderInitial( 
						helper, 
						null,
						false,
						null,
						new ProtocolDecoderAdapter()
						{
							public void
							decodeComplete(
								ProtocolDecoder	decoder,
								ByteBuffer		remaining_initial_data )
							{
								System.out.println( "incoming decode complete: " +  decoder.getFilter().getName(false));
																
								readStream( "incoming", decoder.getFilter() );
								
								writeStream( "ten fat monkies", decoder.getFilter() );
							}
							
							public void
							decodeFailed(
								ProtocolDecoder	decoder,
								Throwable			cause )
							{
								System.out.println( "incoming decode failed: " + Debug.getNestedExceptionMessage(cause));
							}
							
							public void
							gotSecret(
								byte[]				session_secret )
							{
							}

							public int
							getMaximumPlainHeaderLength()
							{
								return( TEST_HEADER.length );
							}
							
							public int
							matchPlainHeader(
								ByteBuffer			buffer )
							{
								int	pos = buffer.position();
								int lim = buffer.limit();
								
								buffer.flip();
								
								boolean	match = buffer.compareTo( ByteBuffer.wrap( TEST_HEADER )) == 0;
								
								buffer.position( pos );
								buffer.limit( lim );
								
								System.out.println( "Match - " + match );
								
								return( match?ProtocolDecoderAdapter.MATCH_CRYPTO_NO_AUTO_FALLBACK:ProtocolDecoderAdapter.MATCH_NONE );
							}
						});
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	outgoings()
	{
		while( true ){
			
			outgoing();
			
			try{
				Thread.sleep(1000000);
				
			}catch( Throwable e ){
	
			}
		}
	}
	
	protected void
	outgoing()
	{
		try{			
			final SocketChannel	channel = SocketChannel.open();
			
			channel.configureBlocking( false );
		
			if ( channel.connect( new InetSocketAddress("localhost", 8765 ))){
							
				outgoing( channel );
				
			}else{
				
				connect_selector.register(
					channel,
					new VirtualSelectorListener()
					{
						public boolean 
						selectSuccess(
							VirtualChannelSelector selector, SocketChannel sc, Object attachment)
						{
							try{
								if ( channel.finishConnect()){
									
									outgoing( channel );
									
									return( true );
								}else{
									
									throw( new IOException( "finishConnect failed" ));
								}
							}catch( Throwable e ){
								
								e.printStackTrace();
								
								return( false );
							}
						}
	
						public void 
						selectFailure(
								VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg)
						{
							msg.printStackTrace();
						}
						 					
					},
					null );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	outgoing(
		SocketChannel	channel )
	{
		try{

			if ( OUTGOING_PLAIN ){
				
				writeStream( TEST_HEADER,  channel);
				
				writeStream( "two jolly porkers".getBytes(), channel );
				
			}else{			
				TransportHelper	helper = new TCPTransportHelper( channel );

				final ProtocolDecoderInitial decoder =
					new ProtocolDecoderInitial( 
						helper,
						new byte[][]{ shared_secret },
						true,
						null,
						new ProtocolDecoderAdapter()
						{
							public void
							decodeComplete(
								ProtocolDecoder	decoder,
								ByteBuffer		remaining_initial_data )
							{
								System.out.println( "outgoing decode complete: " +  decoder.getFilter().getName(false));
															
								readStream( "incoming", decoder.getFilter() );
								
								writeStream( TEST_HEADER,  decoder.getFilter());
								
								writeStream( "two jolly porkers", decoder.getFilter() );
							}
							
							public void
							decodeFailed(
								ProtocolDecoder	decoder,
								Throwable			cause )
							{
								System.out.println( "outgoing decode failed: " + Debug.getNestedExceptionMessage(cause));
	
							}
							
							public void
							gotSecret(
								byte[]				session_secret )
							{	
							}
							
							public int
							getMaximumPlainHeaderLength()
							{
								throw( new RuntimeException());
							}
							
							public int
							matchPlainHeader(
								ByteBuffer			buffer )
							{
								throw( new RuntimeException());
							}
						});
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	readStream(
		final String					str,
		final TransportHelperFilter	filter )
	{
		try{
			TCPNetworkManager.getSingleton().getReadSelector().register(
				((TCPTransportHelper)filter.getHelper()).getSocketChannel(),
				new VirtualSelectorListener()
				{
					public boolean 
					selectSuccess(
						VirtualChannelSelector selector, SocketChannel sc, Object attachment)
					{
						ByteBuffer	buffer = ByteBuffer.allocate(1024);
						
						try{
							long	len = filter.read( new ByteBuffer[]{ buffer }, 0, 1 );
						
							byte[]	data = new byte[buffer.position()];
							
							buffer.flip();
							
							buffer.get( data );
							
							System.out.println( str + ": " + new String(data));
							
							return( len > 0 );
							
						}catch( Throwable e ){
							
							e.printStackTrace();
							
							return( false );
						}
					}

					public void 
					selectFailure(
							VirtualChannelSelector selector, SocketChannel sc, Object attachment, Throwable msg)
					{
						msg.printStackTrace();
					}
				},
				null );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	writeStream(
		String						str,
		TransportHelperFilter	filter )
	{
		writeStream( str.getBytes(), filter );
	}
	
	protected void
	writeStream(
		byte[]						data,
		TransportHelperFilter	filter )
	{
		try{
			filter.write( new ByteBuffer[]{ ByteBuffer.wrap(data)}, 0, 1 );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	protected void
	writeStream(
		byte[]						data,
		SocketChannel				channel )
	{
		try{
			channel.write( new ByteBuffer[]{ ByteBuffer.wrap(data)}, 0, 1 );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		AEDiagnostics.startup( false );
		
		// OUTGOING_PLAIN	= true;
		
		COConfigurationManager.setParameter( "network.transport.encrypted.require", true );
		COConfigurationManager.setParameter( "network.transport.encrypted.min_level", "Plain" );
						
		new PHETester();
		
		try{
			Thread.sleep(10000000);
			
		}catch( Throwable e ){
			
		}
	}
}
