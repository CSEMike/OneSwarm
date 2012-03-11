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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.tracker.server.impl.TRTrackerServerImpl;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerProcessorTCP;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AsyncController;
import org.gudy.azureus2.core3.util.SystemTime;

/**
 * @author parg
 *
 */

public abstract class 
TRNonBlockingServerProcessor 
	extends TRTrackerServerProcessorTCP
{
	private static final int			READ_BUFFER_INITIAL		= 1024;
	private static final int			READ_BUFFER_INCREMENT	= 1024;
	private static final int			READ_BUFFER_LIMIT		= 32*1024;	// needs to be reasonable size to handle scrapes with plugin generated per-hash content
		
	private SocketChannel				socket_channel;
	
	private long						start_time;
	
	private ByteBuffer					read_buffer;
	private String						request_header;
	
	private ByteBuffer					write_buffer;
	
	protected
	TRNonBlockingServerProcessor(
		TRTrackerServerTCP		_server,
		SocketChannel			_socket )
	{
		super( _server );
		
		socket_channel		= _socket;
		
		start_time	= SystemTime.getCurrentTime(); 
		
		read_buffer = ByteBuffer.allocate( READ_BUFFER_INITIAL );
		
		// System.out.println( "created: " + System.currentTimeMillis());
	}

		// 0 -> complete
		// 1 -> more to do
		// 2 -> no progress
		// -1 -> error
	
	protected int
	processRead()
	{
		if ( read_buffer.remaining() == 0 ){
			
			int	capacity = read_buffer.capacity();
			
			if ( capacity == READ_BUFFER_LIMIT ){
				
				return( -1 );
				
			}else{
				
				read_buffer.position(0);
				
				byte[]	data = new byte[capacity];
				
				read_buffer.get( data );
				
				read_buffer = ByteBuffer.allocate( capacity + READ_BUFFER_INCREMENT );
				
				read_buffer.put( data );
			}
		}
		
		try{
			int	len = socket_channel.read( read_buffer );
			
			// System.out.println( "read op[" + len + "]: " + System.currentTimeMillis());


			if ( len < 0 ){
				
				return( -1 );
				
			}else if ( len == 0 ){
				
				return( 2 );	// no progress
			}
			
			byte[]	data = read_buffer.array();
						
			for (int i=read_buffer.position()-4;i>=0;i--){
				
				if ( 	data[i]   == CR &&
						data[i+1] == FF &&
						data[i+2] == CR &&
						data[i+3] == FF ){
					
					request_header = new String(data,0,read_buffer.position());
					
					// System.out.println( "read done: " + System.currentTimeMillis());
					
					getServer().runProcessor( this );
					
					return( 0 );				
				}
			}
			
			return( 1 );
			
		}catch( IOException e ){
			
			return( -1 );
		}
	}
	
		// 0 -> complete
		// 1 -> more to do
		// 2 -> no progress made
		// -1 -> error
	
	protected int
	processWrite()
	{
		if ( write_buffer == null ){
			
			return( -1 );
		}
		
		if ( !write_buffer.hasRemaining()){
			
			return( 0 );
		}
		
		try{
			int	written = socket_channel.write( write_buffer );
			
			if ( written == 0 ){
				
				return( 2 );
			}
			
			if ( write_buffer.hasRemaining()){
				
				return( 1 );
			}
			
			return( 0 );
			
		}catch( IOException e ){
			
			return( -1 );
		}
	}
	
	public void
	runSupport()
	{
		boolean	async = false;
		
		try{
			String	url = request_header.substring(4).trim();
			
			int	pos = url.indexOf( " " );
									
			url = url.substring(0,pos);
				
			final AESemaphore[]				went_async 		= { null };
			final ByteArrayOutputStream[]	async_stream	= { null };
			
			AsyncController	async_control = 
				new AsyncController()
				{
					public void
					setAsyncStart()
					{
						went_async[0] = new AESemaphore( "async" );
					}
					
					public void
					setAsyncComplete()
					{
						went_async[0].reserve();
						
						asyncProcessComplete( async_stream[0] );
					}
				};
			
			try{
				ByteArrayOutputStream	response = 
					process( 	request_header,
								request_header.toLowerCase(),
								url, 
								(InetSocketAddress)socket_channel.socket().getRemoteSocketAddress(),
								TRTrackerServerImpl.restrict_non_blocking_requests,
								new ByteArrayInputStream(new byte[0]),
								async_control );
				
					// two ways of going async
					//	1) return is null and something else will call asyncProcessComplete later
					//	2) return is 'not-yet-filled' os and async controller is managing things
				
				if ( response == null ){
					
					async = true;
					
				}else if ( went_async[0] != null ){
					
					async_stream[0] = response;
					
					async = true;
					
				}else{
					
					write_buffer = ByteBuffer.wrap( response.toByteArray());
				}
			}finally{
				
				if ( went_async[0] != null ){
				
					went_async[0].release();
				}
			}
		}catch( Throwable e ){
			
			
		}finally{
			
			if ( !async ){
				
				((TRNonBlockingServer)getServer()).readyToWrite( this );
			}
		}
	}
	
	protected abstract ByteArrayOutputStream
	process(
		String				input_header,
		String				lowercase_input_header,
		String				url_path,
		InetSocketAddress	client_address,
		boolean				announce_and_scrape_only,
		InputStream			is,
		AsyncController		async )
	
		throws IOException;
	
	protected void
	asyncProcessComplete(
		ByteArrayOutputStream	response )
	{
		write_buffer = ByteBuffer.wrap( response.toByteArray());
	
		((TRNonBlockingServer)getServer()).readyToWrite( this );
	}
	
	protected SocketChannel
	getSocketChannel()
	{
		return( socket_channel );
	}
	
	protected long
	getStartTime()
	{
		return( start_time );
	}
	
	public void
	interruptTask()
	{
	}
	
		// overridden if subclass is interested in failures, so don't remove!
	
	protected void
	failed()
	{	
	}
	
	protected void
	completed()
	{
		// System.out.println( "complete: " + System.currentTimeMillis());
	}
	
	protected void
	closed()
	{
		// System.out.println( "close: " + System.currentTimeMillis());
	}
}
