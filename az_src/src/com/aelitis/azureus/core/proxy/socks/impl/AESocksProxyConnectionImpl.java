/*
 * Created on 08-Dec-2004
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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.HostNameToIPResolverListener;

import com.aelitis.azureus.core.proxy.*;
import com.aelitis.azureus.core.proxy.socks.*;

/**
 * @author parg
 *
 */

public class 
AESocksProxyConnectionImpl
	implements AESocksProxyConnection, AEProxyConnectionListener
{
	private static final LogIDs LOGID = LogIDs.NET;
	public static final boolean	TRACE	= false;
	
	protected AESocksProxyImpl		proxy;
	protected AEProxyConnection		connection;
	protected boolean				disable_dns_lookups;
	
	protected SocketChannel			source_channel;
	
	protected int					socks_version;
	
	protected AESocksProxyPlugableConnection	plugable_connection;
	
	protected
	AESocksProxyConnectionImpl(
		AESocksProxyImpl						_proxy,
		AESocksProxyPlugableConnectionFactory	_connection_factory,
		AEProxyConnection						_connection )
	
		throws IOException
	{
		proxy		= _proxy;
		connection	= _connection;
		
		connection.addListener( this );
		
		source_channel	= connection.getSourceChannel();
		
		try{
			plugable_connection	= _connection_factory.create( this );
		
			if ( TRACE ){
				Logger.log(new LogEvent(LOGID, "AESocksProxyProcessor: " + getName()));
			}
		}catch( AEProxyException e ){
			
			throw( new IOException( e.getMessage()));
		}
	}
	
	public AESocksProxy
	getProxy()
	{
		return( proxy );
	}
	
	public void
	setDelegate(
		AESocksProxyPlugableConnection	target )
	{
		plugable_connection = target;
	}
	
	protected String
	getName()
	{
		String	name = connection.getName() + ", ver = " + socks_version;
					
		name += plugable_connection.getName();
		
		return( name );
	}
	
	protected AEProxyState
	getInitialState()
	{
		return( new proxyStateVersion());
	}
	
	public void
	connectionClosed(
		AEProxyConnection	con )
	{
		try{
			if ( plugable_connection != null ){
				
				plugable_connection.close();
			}
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public boolean
	isClosed()
	{
		return( connection.isClosed());
	}
	
	public AEProxyConnection
	getConnection()
	{
		return( connection );
	}
	
	public void
	disableDNSLookups()
	{
		disable_dns_lookups	= true;
	}
	
	public void
	enableDNSLookups()
	{
		disable_dns_lookups = false;
	}
	
	public void
	close()
	
		throws IOException
	{
		new proxyStateClose();
	}
	
	protected class
	proxyStateVersion
		extends AESocksProxyState
	{
		protected
		proxyStateVersion()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );
			
			buffer	= ByteBuffer.allocate(1);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			int	version	= buffer.get();
			
			if ( version == 5 ){
			
				new proxyStateV5MethodNumber();
				
			}else if ( version == 4 ){
				
				new proxyStateV4Request();
				
			}else{
				
				throw( new IOException( "Unsupported version " + version ));

			}
			
			return( true );
		}
	}
	
		// V4
	
	protected class
	proxyStateV4Request
		extends AESocksProxyState
	{
		boolean		got_header;
		
		protected int		port;
		protected byte[]	address;
		
		protected
		proxyStateV4Request()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(7);
		}
	
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			/*
			+----+----+----+----+----+----+----+----+----+----+....+----+
			| VN | CD | DSTPORT |      DSTIP        | USERID       |NULL|
			+----+----+----+----+----+----+----+----+----+----+....+----+
			# of bytes:	   1    1      2              4           variable       1
			*/

			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
						
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			if ( got_header ){
				
				if ( buffer.get() == (byte)0){
				
						// end of play
		
					if (	address[0] == 0 &&
							address[1] == 0 &&
							address[2] == 0 &&
							address[3] != 0 ){
						
							// socks 4a
						
						new proxyStateV4aRequest( port );
						
					}else{
							
						socks_version	= 4;
						
						plugable_connection.connect( 
								new AESocksProxyAddressImpl( "", InetAddress.getByAddress( address ), port ));
						
					}
				}else{
				
					// drop the user id byte
					
					buffer.flip();
					
				}
			}else{
				
				got_header	= true;
				
				byte	command	= buffer.get();
				
				if ( command != 1 ){
					
					throw( new IOException( "SocksV4: only CONNECT supported" ));
				}
				
				port = (((int)buffer.get() & 0xff) << 8 ) + ((int)buffer.get() & 0xff);

				address = new byte[4];
				
				for (int i=0;i<address.length;i++){
				
					address[i] = buffer.get();
				}
				
					// prepare for user id
				
				buffer = ByteBuffer.allocate(1);
			}
			
			return( true );
		}
	}
	
	protected class
	proxyStateV4aRequest
		extends AESocksProxyState
	{
		protected String	dns_address;
		protected int		port;
		
		protected
		proxyStateV4aRequest(
			int		_port )
		{
			super( AESocksProxyConnectionImpl.this );
			
			port		= _port;
			dns_address	= "";
			
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(1);
		}
	
		protected boolean
		readSupport(
			final SocketChannel 		sc )
		
			throws IOException
		{
				// dns name follows, null terminated

			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
							
			byte data = buffer.get();
			
			if ( data == 0 ){
				
				if ( disable_dns_lookups ){
					
					socks_version	= 4;
					
					plugable_connection.connect( new AESocksProxyAddressImpl( dns_address, null, port ));

				}else{
					final String	f_dns_address	= dns_address;
					
					connection.cancelReadSelect( sc );
					
					HostNameToIPResolver.addResolverRequest(
						dns_address,
						new HostNameToIPResolverListener()
						{
							public void
							hostNameResolutionComplete(
								InetAddress	address )
							{
								try{
									socks_version	= 4;
									
									plugable_connection.connect( new AESocksProxyAddressImpl( f_dns_address, address, port ));
		
										// re-activate the read select suspended while resolving
									
									connection.requestReadSelect( sc );
									
								}catch ( IOException e ){
									
									connection.failed(e);
								}
							}
						});		
				}
			}else{
				
				dns_address += (char)data;
				
				if ( dns_address.length() > 4096 ){
					
					throw( new IOException( "DNS name too long" ));
				}
				
					// ready for next byte
				
				buffer.flip();
			}
			
			return( true );
		}
	}
	
	protected class
	proxyStateV4Reply
		extends AESocksProxyState
	{
		protected
		proxyStateV4Reply()
		
			throws IOException
		{		
			super( AESocksProxyConnectionImpl.this );
			
			/*
			+----+----+----+----+----+----+----+----+
			| VN | CD | DSTPORT |      DSTIP        |
			+----+----+----+----+----+----+----+----+
			# of bytes:	   1    1      2              4
			*/

			connection.setWriteState( this );
			
			byte[]	addr = plugable_connection.getLocalAddress().getAddress();
			int		port = plugable_connection.getLocalPort();
			
			buffer	= ByteBuffer.wrap(
					new byte[]{	(byte)0,(byte)90,
								(byte)((port>>8)&0xff), (byte)(port&0xff),
								addr[0],addr[1],addr[2],addr[3]});
					
			
			write( source_channel );
		}
		
		protected boolean
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				connection.requestWriteSelect( sc );
				
			}else{
	
				plugable_connection.relayData();
			}
			
			return( len > 0 );
		}
	}
	
		// V5
	
	protected class
	proxyStateV5MethodNumber
		extends AESocksProxyState
	{
		
		protected
		proxyStateV5MethodNumber()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(1);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			int	num_methods	= buffer.get();
			
			new proxyStateV5Methods(num_methods);
			
			return( true );
		}
	}
	
	protected class
	proxyStateV5Methods
		extends AESocksProxyState
	{
		
		protected
		proxyStateV5Methods(
			int		methods )
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );

			buffer	= ByteBuffer.allocate(methods);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
				// we just ignore actual method values
			
			new proxyStateV5MethodsReply();
			
			return( true );
		}
	}
	
	protected class
	proxyStateV5MethodsReply
		extends AESocksProxyState
	{
		
		protected
		proxyStateV5MethodsReply()
		
			throws IOException
		{
			super( AESocksProxyConnectionImpl.this );
			
			new proxyStateV5Request();
			
			connection.setWriteState( this );
			
			buffer	= ByteBuffer.wrap(new byte[]{(byte)5,(byte)0});
			
			write( source_channel );
		}
		
		protected boolean
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int len = sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				connection.requestWriteSelect( sc );
			}
			
			return( len > 0 );
		}
	}
	
	/*
    +----+-----+-------+------+----------+----------+
    |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
    +----+-----+-------+------+----------+----------+
    | 1  |  1  | X'00' |  1   | Variable |    2     |
    +----+-----+-------+------+----------+----------+

		Where:

	          o  VER    protocol version: X'05'
	          o  CMD
	             o  CONNECT X'01'
	             o  BIND X'02'
	             o  UDP ASSOCIATE X'03'
	          o  RSV    RESERVED
	          o  ATYP   address type of following address
	             o  IP V4 address: X'01'
	             o  DOMAINNAME: X'03'
	             o  IP V6 address: X'04'
	          o  DST.ADDR       desired destination address
	          o  DST.PORT desired destination port in network octet
	             order
	             */
	
	protected class
	proxyStateV5Request
		extends AESocksProxyState
	{
		
		protected
		proxyStateV5Request()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );
			
			buffer	= ByteBuffer.allocate(4);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			buffer.get();		// version
			
			int	command			= buffer.get();
			
			buffer.get();		// reserved
			
			int address_type	= buffer.get();
			
			if ( command != 1 ){
				
				throw( new IOException( "V5: Only connect supported"));
			}
			
			if ( address_type == 1 ){
			
				new proxyStateV5RequestIP();
				
			}else if ( address_type == 3 ){
				
				new proxyStateV5RequestDNS();
				
			}else{
				
				throw( new IOException( "V5: Unsupported address type" ));
			}
			
			return( true );
		}
	}
	
	protected class
	proxyStateV5RequestIP
		extends AESocksProxyState
	{
		
		protected
		proxyStateV5RequestIP()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );
			
			buffer	= ByteBuffer.allocate(4);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			byte[]	bytes = new byte[4];
			
			buffer.get( bytes );
			
			InetAddress inet_address = InetAddress.getByAddress( bytes );
			
			new proxyStateV5RequestPort( "", inet_address );
			
			return( true );
		}
	}
	
	protected class
	proxyStateV5RequestDNS
		extends AESocksProxyState
	{
		boolean	got_length	= false;
		
		protected
		proxyStateV5RequestDNS()
		{
			super( AESocksProxyConnectionImpl.this );
			
			connection.setReadState( this );
			
			buffer	= ByteBuffer.allocate(1);
		}
		
		protected boolean
		readSupport(
			final SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
			
			if ( !got_length){
				
				int	length = ((int)buffer.get()) & 0xff;
				
				buffer = ByteBuffer.allocate( length );
				
				got_length	= true;
				
			}else{
				
				String dns_address = "";
				
				while( buffer.hasRemaining()){
				
					dns_address += (char)buffer.get();
				}
					
				if ( disable_dns_lookups ){
				
					new proxyStateV5RequestPort( dns_address, null );
				
				}else{
					
					final String	f_dns_address	= dns_address;
					
					connection.cancelReadSelect( sc );
					
					HostNameToIPResolver.addResolverRequest(
						dns_address,
						new HostNameToIPResolverListener()
						{
							public void
							hostNameResolutionComplete(
								InetAddress	address )
							{
								new proxyStateV5RequestPort( f_dns_address, address);
									
								connection.requestReadSelect( sc );
							}
						});
				}
			}
			
			return( true );
		}
	}
	
	protected class
	proxyStateV5RequestPort
		extends AESocksProxyState
	{
		protected String		unresolved_address;
		protected InetAddress	address;
		
		protected
		proxyStateV5RequestPort(
			String			_unresolved_address,
			InetAddress		_address )
		{
			super( AESocksProxyConnectionImpl.this );
			
			unresolved_address	= _unresolved_address;
			address				= _address;
			
			connection.setReadState( this );
			
			buffer	= ByteBuffer.allocate(2);
		}
		
		protected boolean
		readSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.read( buffer );
			
			if ( len == 0 ){
				
				return( false );
				
			}else if ( len == -1 ){
				
				throw( new IOException( "read channel shutdown" ));
			}
			
			
			if ( buffer.hasRemaining()){
				
				return( true );
			}
			
			buffer.flip();
						
			int	port = (((int)buffer.get() & 0xff) << 8 ) + ((int)buffer.get() & 0xff);
			
			socks_version	= 5;
			
			plugable_connection.connect( new AESocksProxyAddressImpl( unresolved_address, address, port ));
			
			return( true );
		}
	}
	
	/*
    +----+-----+-------+------+----------+----------+
    |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
    +----+-----+-------+------+----------+----------+
    | 1  |  1  | X'00' |  1   | Variable |    2     |
    +----+-----+-------+------+----------+----------+

 Where:

      o  VER    protocol version: X'05'
      o  REP    Reply field:
         o  X'00' succeeded
         o  X'01' general SOCKS server failure
         o  X'02' connection not allowed by ruleset
         o  X'03' Network unreachable
         o  X'04' Host unreachable
         o  X'05' Connection refused
         o  X'06' TTL expired
         o  X'07' Command not supported
         o  X'08' Address type not supported
         o  X'09' to X'FF' unassigned
      o  RSV    RESERVED
      o  ATYP   address type of following address

         o  IP V4 address: X'01'
         o  DOMAINNAME: X'03'
         o  IP V6 address: X'04'
      o  BND.ADDR       server bound address
      o  BND.PORT       server bound port in network octet order
      */
	
	
	protected class
	proxyStateV5Reply
		extends AESocksProxyState
	{
		protected
		proxyStateV5Reply()
		
			throws IOException
		{		
			super( AESocksProxyConnectionImpl.this );
			
			connection.setWriteState( this );
			
			byte[]	addr = plugable_connection.getLocalAddress().getAddress();
			
			int		port = plugable_connection.getLocalPort();
			
			buffer	= ByteBuffer.wrap(
					new byte[]{(byte)5,(byte)0,(byte)0,(byte)1,
								addr[0],addr[1],addr[2],addr[3],
								(byte)((port>>8)&0xff), (byte)(port&0xff)});
					
			
			write( source_channel );
		}
		
		protected boolean
		writeSupport(
			SocketChannel 		sc )
		
			throws IOException
		{
			int	len = sc.write( buffer );
			
			if ( buffer.hasRemaining()){
				
				connection.requestWriteSelect( sc );
				
			}else{
	
				plugable_connection.relayData();
			}
			
			return( len > 0 );
		}
	}
	
	public void
	connected()
	
		throws IOException
	{
		if ( socks_version == 4 ){
			
			new proxyStateV4Reply();
			
		}else{
			
			new proxyStateV5Reply();
		}
	}

	
	protected class
	proxyStateClose
		extends AESocksProxyState
	{
		protected
		proxyStateClose()
		
			throws IOException
		{	
			super( AESocksProxyConnectionImpl.this );
			
			connection.close();
			
			connection.setReadState( null);
			connection.setWriteState( null);
			connection.setConnectState( null);
		}
	}
}
