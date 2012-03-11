/*
 * Created on Oct 28, 2005
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
package com.aelitis.azureus.core.networkmanager.impl.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.TransportEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;


/**
 * This class is essentially a socket channel wrapper to support working with az message encoders/decoders.
 */
public class LightweightTCPTransport implements Transport {
	
	private final TransportEndpoint			transport_endpoint;
	private final TransportHelperFilter 	filter;	
	
	public LightweightTCPTransport( ProtocolEndpoint	pe, TransportHelperFilter filter ) {
		SocketChannel channel = ((TCPTransportHelper)filter.getHelper()).getSocketChannel();
		transport_endpoint	= new TransportEndpointTCP( pe, channel );
		this.filter = filter;
	}
	
	public TransportEndpoint
	getTransportEndpoint()
	{
		return( transport_endpoint );
	}

  public long write( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
  	return filter.write( buffers, array_offset, length );
  }

  
  public long read( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
  	return filter.read( buffers, array_offset, length );
  }
  

  public SocketChannel getSocketChannel(){  return ((TCPTransportHelper)filter.getHelper()).getSocketChannel();  }
  
  public InetSocketAddress 
  getRemoteAddress()
  {
	  return( new InetSocketAddress( getSocketChannel().socket().getInetAddress(), getSocketChannel().socket().getPort()));
  }
  
  public String getDescription(){  return getSocketChannel().socket().getInetAddress().getHostAddress() + ": " + getSocketChannel().socket().getPort();  }
  
  public void close( String reason ){
  	try {
  		getSocketChannel().close();  //close() can block
    }
    catch( Throwable t) { t.printStackTrace(); }
  }
  
  public int
  getMssSize()
  {
	  return( TCPNetworkManager.getTcpMssSize());
  }
  
  public void setAlreadyRead( ByteBuffer bytes_already_read ){ 	throw new RuntimeException( "not implemented" );  }
  public boolean isReadyForWrite(EventWaiter waiter){  throw new RuntimeException( "not implemented" );  }  
  public long isReadyForRead(EventWaiter waiter){  throw new RuntimeException( "not implemented" );  }  
  public void setReadyForRead(){ throw new RuntimeException( "not implemented" );  }  
  public void connectOutbound( ByteBuffer initial_data, ConnectListener listener, int priority ){ throw new RuntimeException( "not implemented" ); }  
  public void connectedInbound(){ throw new RuntimeException( "not implemented" ); }  
  public void setTransportMode( int mode ){ throw new RuntimeException( "not implemented" ); } 
  public int getTransportMode(){ throw new RuntimeException( "not implemented" );  }
  public void setTrace(boolean on) {
	  // TODO Auto-generated method stub

  }
  public String getEncryption(boolean verbose){ return( filter.getName(verbose)); }
  public boolean isEncrypted(){ return( filter.isEncrypted());}
  public boolean isTCP(){ return true; }
  public String getProtocol(){ return "TCP"; }
  
  public void
  bindConnection(
	NetworkConnection	connection )
  {
  }
  
  public void
  unbindConnection(
	NetworkConnection	connection )
  {  
  }
}
