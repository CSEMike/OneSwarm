/*
 * Created on 13-Dec-2004
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

package com.aelitis.azureus.core.proxy.socks.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.proxy.AEProxyConnection;
import com.aelitis.azureus.core.proxy.socks.AESocksProxy;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyAddress;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyConnection;
import com.aelitis.azureus.core.proxy.socks.AESocksProxyPlugableConnection;


/**
 * @author parg
 *
 */

public class 
AESocksProxyPlugableConnectionDefault
	implements AESocksProxyPlugableConnection
{
	protected AESocksProxyConnection	socks_connection;
	protected AEProxyConnection			connection;
	
	protected SocketChannel		source_channel;
	protected SocketChannel		target_channel;

	protected proxyStateRelayData	relay_data_state;
	
	public
	AESocksProxyPlugableConnectionDefault(
		AESocksProxyConnection		_socks_connection )
	{
		socks_connection	= _socks_connection;
		connection			= socks_connection.getConnection();
		
		source_channel	= connection.getSourceChannel();
	}
	
	public String
	getName()
	{
		if ( target_channel != null ){
			
			return( target_channel.socket().getInetAddress() + ":" + target_channel.socket().getPort());
		}
		
		return( "" );
	}

	public InetAddress
	getLocalAddress()
	{
		return( target_channel.socket().getInetAddress());
	}
	
	public int
	getLocalPort()
	{
		return( target_channel.socket().getPort());
	}
	
	public void
	connect(
		AESocksProxyAddress		_address )
		
		throws IOException
	{
		if ( _address.getAddress() == null ){

			throw( new IOException( "DNS lookup of '" + _address.getUnresolvedAddress() + "' fails" ));
		}
		
		new proxyStateRelayConnect( new InetSocketAddress(_address.getAddress(), _address.getPort()));
	}
	
	public void
	relayData()
	
		throws IOException
	{
		new proxyStateRelayData();
	}
	
	public void
	close()
	{
		if ( target_channel != null ){
			
			try{
				connection.cancelReadSelect( target_channel );
				connection.cancelWriteSelect( target_channel );
        
				target_channel.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		if ( relay_data_state != null ){
			
			relay_data_state.destroy();
		}
	}
	
	
	protected class
	proxyStateRelayConnect
		extends AESocksProxyState
	{
		protected InetSocketAddress	address;
		
		protected
		proxyStateRelayConnect(
			InetSocketAddress	_address )
		
			throws IOException
		{
			super( socks_connection );
			
			address			= _address;
			
				// OK, we're almost ready to roll. Unregister the read select until we're 
				// connected
		
			connection.cancelReadSelect( source_channel );

			connection.setConnectState( this );
			
			target_channel = SocketChannel.open();
			
		    InetAddress bindIP = NetworkAdmin.getSingleton().getMultiHomedOutgoingRoundRobinBindAddress(address.getAddress());
		    
	        if ( bindIP != null ){
	        	
	        	target_channel.socket().bind( new InetSocketAddress( bindIP, 0 ) );
	        }
	        
	        target_channel.configureBlocking( false );
	        
	        target_channel.connect( address );
	   
	        connection.requestConnectSelect( target_channel );
		}
		
		protected boolean
		connectSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			if( !sc.finishConnect()){
				
				throw( new IOException( "finishConnect returned false" ));
			}
	           
				// if we've got a proxy chain, now's the time to negotiate the connection
			
			AESocksProxy	proxy = socks_connection.getProxy();
			
			if ( proxy.getNextSOCKSProxyHost() != null ){
				
			}
			
			socks_connection.connected();
			
			return( true );
		}
	}
	
	protected class
	proxyStateRelayData
		extends AESocksProxyState
	{
		protected DirectByteBuffer		source_buffer;
		protected DirectByteBuffer		target_buffer;
		
		protected long				outward_bytes	= 0;
		protected long				inward_bytes	= 0;
		
		protected
		proxyStateRelayData()
		
			throws IOException
		{		
			super( socks_connection );
			
			source_buffer	= DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PROXY_RELAY, 1024 );
			target_buffer	= DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_PROXY_RELAY, 1024 );
			
			relay_data_state	= this;
			
			if ( connection.isClosed()){
				
				destroy();
				
				throw( new IOException( "connection closed" ));
			}
			
			connection.setReadState( this );
			
			connection.setWriteState( this );
			
			connection.requestReadSelect( source_channel );
			
			connection.requestReadSelect( target_channel );
			
			connection.setConnected();
		}
		
		protected void
		destroy()
		{
			if ( source_buffer != null ){
				
				source_buffer.returnToPool();
				
				source_buffer	= null;
			}
			
			if ( target_buffer != null ){
				
				target_buffer.returnToPool();
				
				target_buffer	= null;
			}		
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			connection.setTimeStamp();
			
			SocketChannel	chan1 = sc;
			SocketChannel	chan2 = sc==source_channel?target_channel:source_channel;
			
			DirectByteBuffer	read_buffer = sc==source_channel?source_buffer:target_buffer;
									
			int	len = read_buffer.read( DirectByteBuffer.SS_PROXY, chan1 );
			
			if ( len == -1 ){
				
					//means that the channel has been shutdown
				
				connection.close();
				
			}else{
				
				if ( read_buffer.position( DirectByteBuffer.SS_PROXY ) > 0 ){
					
					read_buffer.flip(DirectByteBuffer.SS_PROXY);
					
					int	written = read_buffer.write( DirectByteBuffer.SS_PROXY, chan2 );
									
					if ( chan1 == source_channel ){
						
						outward_bytes += written;
						
					}else{
						
						inward_bytes += written;
					}
					
					if ( read_buffer.hasRemaining(DirectByteBuffer.SS_PROXY)){
						
						connection.cancelReadSelect( chan1 );
						
						connection.requestWriteSelect( chan2 );
						
					}else{
						
						read_buffer.position(DirectByteBuffer.SS_PROXY, 0);
						
						read_buffer.limit( DirectByteBuffer.SS_PROXY, read_buffer.capacity(DirectByteBuffer.SS_PROXY));
					}
				}
			}
			
			return( len > 0 );
		}
		
		protected boolean
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
				// socket SX -> SY via BX
				// so if SX = source_channel then BX is target buffer
			
			SocketChannel	chan1 = sc;
			SocketChannel	chan2 = sc==source_channel?target_channel:source_channel;
			
			DirectByteBuffer	read_buffer = sc==source_channel?target_buffer:source_buffer;
			
			int written = read_buffer.write( DirectByteBuffer.SS_PROXY, chan1 );
						
			if ( chan1 == target_channel ){
				
				outward_bytes += written;
				
			}else{
				
				inward_bytes += written;
			}
			
			if ( read_buffer.hasRemaining(DirectByteBuffer.SS_PROXY)){
								
				connection.requestWriteSelect( chan1 );
				
			}else{
				
				read_buffer.position(DirectByteBuffer.SS_PROXY,0);
				
				read_buffer.limit( DirectByteBuffer.SS_PROXY, read_buffer.capacity(DirectByteBuffer.SS_PROXY));
				
				connection.requestReadSelect( chan2 );
			}
			
			return( written > 0 );
		}
		
		public String
		getStateName()
		{
			String	state = this.getClass().getName();
			
			int	pos = state.indexOf( "$");
			
			state = state.substring(pos+1);
			
			return( state  +" [out=" + outward_bytes +",in=" + inward_bytes +"] " + source_buffer + " / " + target_buffer );
		}
	}
}
