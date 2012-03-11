/*
 * Created on Jun 8, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.net.udp.uc.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;

import com.aelitis.azureus.core.proxy.AEProxyFactory;
import com.aelitis.net.udp.uc.PRUDPPacket;
import com.aelitis.net.udp.uc.PRUDPPacketHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerException;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerStats;
import com.aelitis.net.udp.uc.PRUDPPacketReceiver;
import com.aelitis.net.udp.uc.PRUDPPrimordialHandler;
import com.aelitis.net.udp.uc.PRUDPRequestHandler;

public class 
PRUDPPacketHandlerSocks 
	implements PRUDPPacketHandler, PRUDPPacketHandlerImpl.PacketTransformer
{
	private static String	socks_host;
	private static int		socks_port;
	private static String	socks_user;
	private static String	socks_password;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{
				"Proxy.Host",
				"Proxy.Port",
				"Proxy.Username",
				"Proxy.Password",
			}, 
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameter_name ) 
				{
				    socks_host 		= COConfigurationManager.getStringParameter("Proxy.Host").trim();
					socks_port 		= Integer.parseInt(COConfigurationManager.getStringParameter("Proxy.Port").trim());
					socks_user 		= COConfigurationManager.getStringParameter("Proxy.Username").trim();
					socks_password 	= COConfigurationManager.getStringParameter("Proxy.Password").trim();
							     
					if ( socks_user.equalsIgnoreCase("<none>")){
						socks_user = "";
					}
						      
				}
			});
	}
	
	final private InetSocketAddress		target;
	
	private Socket					control_socket;
	
	private InetSocketAddress		relay;
	
	private PRUDPPacketHandler		delegate;
	
	private byte[]	packet_out_header;
	
	protected
	PRUDPPacketHandlerSocks(
		InetSocketAddress		_target )
	
		throws PRUDPPacketHandlerException
	{
		target	= _target;
		
		boolean	ok = false;
		
		try{
		    delegate = new PRUDPPacketHandlerImpl( 0, null, this );

			control_socket = new Socket( Proxy.NO_PROXY );
			
			control_socket.connect( new InetSocketAddress( socks_host, socks_port ));
		
			DataOutputStream 	dos = new DataOutputStream( new BufferedOutputStream( control_socket.getOutputStream(), 256 ));
			DataInputStream 	dis = new DataInputStream( control_socket.getInputStream());
						
			dos.writeByte( (byte)5 ); // socks 5
			dos.writeByte( (byte)2 ); // 2 methods
			dos.writeByte( (byte)0 ); // no auth
			dos.writeByte( (byte)2 ); // user/pw
			
			dos.flush();
			
		    dis.readByte();  // version byte
		    
		    byte method = dis.readByte();

		    if ( method != 0 && method != 2 ){
		    	
		        throw new IOException( "SOCKS 5: no valid method [" + method + "]" );
		    }

		      // auth
		    
		    if ( method == 2 ) {
		        
		    	dos.writeByte( (byte)1 ); // user/pw version
		    	dos.writeByte( (byte)socks_user.length() ); // user length
		    	dos.write( socks_user.getBytes() );
		    	dos.writeByte( (byte)socks_password.length() ); // password length
		    	dos.write( socks_password.getBytes() );

		    	dos.flush();
		    	
		    	dis.readByte();  // version byte
		    	
		    	byte status = dis.readByte();

		        if ( status != 0 ){
		        	
		        	throw( new IOException( "SOCKS 5: authentication fails [status=" +status+ "]" ));
		        }
		    }
		    
		    String	mapped_ip;
		    
		    if ( target.isUnresolved() || target.getAddress() == null ){
		    	
		    		// deal with long "hostnames" that we get for, e.g., I2P destinations
		    	
		      	mapped_ip = AEProxyFactory.getAddressMapper().internalise( target.getHostName() );
		      	
		    }else{
		    	  
		      	mapped_ip = target.getAddress().getHostName();
		    }
		    
		    dos.writeByte( (byte)5 ); // version
		    dos.writeByte( (byte)3 ); // udp associate
		    dos.writeByte( (byte)0 ); // reserved
		    
	    	dos.writeByte((byte)1);
	    	dos.write( new byte[4] );
	    	
		    dos.writeShort( (short)delegate.getPort()); // port
		    
		    dos.flush();
		    
		    dis.readByte();	// ver
		    
		    byte reply = dis.readByte();
		    
		    if ( reply != 0 ){
		    	
	        	throw( new IOException( "SOCKS 5: udp association fails [reply=" +reply+ "]" ));
		    }
		    
		    dis.readByte();	// reserved
		    
		    InetAddress	relay_address;
		    
		    byte atype = dis.readByte();
		    
		    if ( atype == 1 ){
		    	
		    	byte[]	bytes = new byte[4];
		    	
		    	dis.readFully( bytes );
		    	
		    	relay_address = InetAddress.getByAddress( bytes );
		    	
		    }else if ( atype == 3 ){
		    	
		    	byte	len = dis.readByte();
		    	
		    	byte[] bytes = new byte[(int)len&0xff ];
		    	
		    	dis.readFully( bytes );
		    	
		    	relay_address = InetAddress.getByName( new String( bytes ));
		    	
		    }else{
		    	
		    	byte[]	bytes = new byte[16];
		    	
		    	dis.readFully( bytes );
		    	
		    	relay_address = InetAddress.getByAddress( bytes );

		    }
		    
		    int	relay_port = ((dis.readByte()<<8)&0xff00) | (dis.readByte() & 0x00ff );
		    	
		    if ( relay_address.isAnyLocalAddress()){
		    	
		    	relay_address = control_socket.getInetAddress();
		    }
		    
		    relay = new InetSocketAddress( relay_address, relay_port );
		    			    
		    	// use the maped ip for dns resolution so we don't leak the
		    	// actual address if this is a secure one (e.g. I2P one)
		    
		    ByteArrayOutputStream	baos_temp 		= new ByteArrayOutputStream();
		    DataOutputStream		dos_temp	= new DataOutputStream( baos_temp );
	
		    dos_temp.writeByte(0);	// resv
		    dos_temp.writeByte(0);	// resv		    
		    dos_temp.writeByte(0);	// frag (none)
	
		    try {
		    	byte[] ip_bytes = HostNameToIPResolver.syncResolve( mapped_ip ).getAddress();
	
		    	dos_temp.writeByte( ip_bytes.length==4?(byte)1:(byte)4 );
		    	dos_temp.write( ip_bytes );
	
		    	
		    }catch( Throwable e ){
		    			    	
		    	dos_temp.writeByte( (byte)3 );  // address type = domain name
		    	dos_temp.writeByte( (byte)mapped_ip.length() );  // address type = domain name
		    	dos_temp.write( mapped_ip.getBytes() );
	
		    }
	
		    dos_temp.writeShort( (short)target.getPort() ); // port
	
		    dos_temp.flush();
		    packet_out_header = baos_temp.toByteArray();

		    	    
			ok = true;
			
			Thread.sleep(1000);
			
		}catch( Throwable e ){
			
			throw( new PRUDPPacketHandlerException( "socks setup failed: " + Debug.getNestedExceptionMessage(e), e));
			
		}finally{
			
			if ( !ok ){
				
				try{
					control_socket.close();
					
				}catch( Throwable e ){
					
					Debug.out( e );
					
				}finally{
					
					control_socket = null;
				}
				
				if ( delegate != null ){
					
					try{
					    delegate.destroy();
					    
					}finally{
						
						delegate = null;
					}
				}
			}
		}
	}
	
	public void
	transformSend(
		DatagramPacket	packet )
	{		
		byte[]	data 		= packet.getData();
		int		data_len	= packet.getLength();
		
		byte[]	new_data = new byte[data_len+packet_out_header.length];
		
		System.arraycopy( packet_out_header, 0, new_data, 0, packet_out_header.length );
		System.arraycopy( data, 0, new_data, packet_out_header.length, data_len);
		
		packet.setData( new_data );
	}
		
	public void
	transformReceive(
		DatagramPacket	packet )
	{
		byte[]	data 		= packet.getData();
		int		data_len	= packet.getLength();
		
		DataInputStream dis = new DataInputStream( new ByteArrayInputStream( data, 0, data_len ));
		
		try{
			dis.readByte();	// res
			dis.readByte();	// res
			dis.readByte();	// assume no frag
		
			byte	atype = dis.readByte();
			
			int	encap_len = 4;
			if ( atype == 1 ){
				
				encap_len += 4;
				
			}else if ( atype == 3 ){
				
				encap_len += 1 + (dis.readByte()&0xff);
				
			}else{
				
				encap_len += 16;
			}
			
			encap_len += 2;	// port
			
			byte[]	new_data = new byte[data_len-encap_len];
			
			System.arraycopy( data, encap_len, new_data, 0, data_len - encap_len );
			
			packet.setData( new_data );
			
		}catch( IOException e ){
			
			Debug.out( e );
		}
	}
			
	private void
	checkAddress(
		InetSocketAddress			destination )
	
		throws PRUDPPacketHandlerException
	{
		if ( !destination.equals( target )){
			
			throw( new PRUDPPacketHandlerException( "Destination mismatch" ));
		}
	}
	
	public void
	sendAndReceive(
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address,
		PRUDPPacketReceiver			receiver,
		long						timeout,
		int							priority )
	
		throws PRUDPPacketHandlerException
	{
		checkAddress( destination_address );
		
		delegate.sendAndReceive( request_packet, relay, receiver, timeout, priority );
	}
	
	public PRUDPPacket
	sendAndReceive(
		PasswordAuthentication		auth,
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address )
	
		throws PRUDPPacketHandlerException
	{
		checkAddress( destination_address );
		
		return( delegate.sendAndReceive( auth, request_packet, relay));
	}
	
	public PRUDPPacket
	sendAndReceive(
		PasswordAuthentication		auth,
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address,
		long						timeout_millis )
	
		throws PRUDPPacketHandlerException
	{
		checkAddress( destination_address );
		
		return( delegate.sendAndReceive(auth, request_packet, relay, timeout_millis ));
	}
	
	public PRUDPPacket
	sendAndReceive(
		PasswordAuthentication		auth,
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address,
		long						timeout_millis,
		int							priority )
	
		throws PRUDPPacketHandlerException
	{
		checkAddress( destination_address );
		
		return( delegate.sendAndReceive(auth, request_packet, relay, timeout_millis, priority ));
	}
	
	public void
	send(
		PRUDPPacket					request_packet,
		InetSocketAddress			destination_address )
	
		throws PRUDPPacketHandlerException
	{
		checkAddress( destination_address );
		
		delegate.send( request_packet, relay );
	}
	
	public PRUDPRequestHandler
	getRequestHandler()
	{
		return( delegate.getRequestHandler());
	}
	
	public void
	setRequestHandler(
		PRUDPRequestHandler	request_handler )
	{
		delegate.setRequestHandler( request_handler );
	}
	
	public void
	primordialSend(
		byte[]				data,
		InetSocketAddress	target )
	
		throws PRUDPPacketHandlerException
	{
		throw( new PRUDPPacketHandlerException( "not imp" ));
	}
	
	public void
	addPrimordialHandler(
		PRUDPPrimordialHandler	handler )
	{
	}
	
	public void
	removePrimordialHandler(
		PRUDPPrimordialHandler	handler )
	{
	}
	
	public int
	getPort()
	{
		return( delegate.getPort());
	}
	
	public void
	setDelays(
		int		send_delay,
		int		receive_delay,
		int		queued_request_timeout )
	{
		delegate.setDelays(send_delay, receive_delay, queued_request_timeout);
	}
	
	public void
	setExplicitBindAddress(
		InetAddress	address )
	{
		delegate.setExplicitBindAddress( address );
	}
	
	public PRUDPPacketHandlerStats
	getStats()
	{
		return( delegate.getStats());
	}
	
	public PRUDPPacketHandler
	openSession(
		InetSocketAddress	target )
	
		throws PRUDPPacketHandlerException
	{
		throw( new PRUDPPacketHandlerException( "not supported" ));
	}
	
	public void
	closeSession()
	
		throws PRUDPPacketHandlerException
	{	
		if ( control_socket != null ){
			
			try{
				control_socket.close();
				
				control_socket = null;
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		if ( delegate != null ){
			
			delegate.destroy();
		}
	}
	
	public void
	destroy()
	{
		try{
			closeSession();
			
		}catch( Throwable e ){
			
			Debug.out( e );
		}
	}
}
