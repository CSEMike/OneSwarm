/*
 * Created on Feb 9, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.pluginsimpl.local.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.plugins.messaging.MessageStreamDecoder;
import org.gudy.azureus2.plugins.messaging.MessageStreamEncoder;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.network.ConnectionManager;
import org.gudy.azureus2.plugins.network.Transport;
import org.gudy.azureus2.plugins.network.TransportCipher;
import org.gudy.azureus2.plugins.network.TransportException;
import org.gudy.azureus2.plugins.network.TransportFilter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamDecoderAdapter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamEncoderAdapter;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpointFactory;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilterStreamCipher;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPTransport;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPTransportHelper;


/**
 *
 */
public class ConnectionManagerImpl implements ConnectionManager {
  
  private static ConnectionManagerImpl instance;
  
  
  public static synchronized ConnectionManagerImpl 
  getSingleton(
	AzureusCore		core )
  {
	  if ( instance == null ){
		  
		  instance = new ConnectionManagerImpl( core );
	  }
	  
	  return( instance );
  }
  
  private AzureusCore		azureus_core;
  
  private ConnectionManagerImpl(AzureusCore _core) {
  
	  azureus_core	= _core;
  }
  

  public Connection 
  createConnection( 
	InetSocketAddress remote_address, 
	MessageStreamEncoder encoder, 
	MessageStreamDecoder decoder ) 
  {
	  ConnectionEndpoint connection_endpoint	= new ConnectionEndpoint( remote_address );
	  
	  connection_endpoint.addProtocol( ProtocolEndpointFactory.createEndpoint( ProtocolEndpoint.PROTOCOL_TCP, remote_address ));
	 
	  com.aelitis.azureus.core.networkmanager.NetworkConnection core_conn =
		  NetworkManager.getSingleton().createConnection( connection_endpoint, new MessageStreamEncoderAdapter( encoder ), new MessageStreamDecoderAdapter( decoder ), false, false, null );
    
	  return new ConnectionImpl( core_conn, false );
  }
  
  public int
  getNATStatus()
  {
	  return( azureus_core.getGlobalManager().getNATStatus());
  }
  
  public TransportCipher createTransportCipher(String algorithm, int mode, SecretKeySpec key_spec, AlgorithmParameterSpec params) throws TransportException {
	  try {
		  com.aelitis.azureus.core.networkmanager.impl.TransportCipher cipher = new com.aelitis.azureus.core.networkmanager.impl.TransportCipher(algorithm, mode, key_spec, params);
		  return new TransportCipherImpl(cipher);
	  }
	  catch (Exception e) {
		  throw new TransportException(e);
	  }
  }
  
  public TransportFilter createTransportFilter(Connection connection, TransportCipher read_cipher, TransportCipher write_cipher) throws TransportException {
	  Transport transport = connection.getTransport();
	  com.aelitis.azureus.core.networkmanager.Transport core_transport;
	  try {core_transport = ((TransportImpl)transport).coreTransport();}
	  catch (IOException e) {throw new TransportException(e);}
	  
	  TransportHelper helper;
	  
	  if (core_transport instanceof TCPTransportImpl) {
		  TransportHelperFilter hfilter = ((TCPTransportImpl)core_transport).getFilter();
		  if (hfilter != null) {helper = hfilter.getHelper();}
		  else {
			  helper = new TCPTransportHelper(((TCPTransportImpl)(core_transport)).getSocketChannel());
		  }
	  } else if (core_transport instanceof UDPTransport) {
		  TransportHelperFilter hfilter = ((UDPTransport)core_transport).getFilter();
		  if (hfilter != null) {helper = hfilter.getHelper();}
		  else {
			  helper = ((UDPTransport)core_transport).getFilter().getHelper();
			  InetSocketAddress addr = core_transport.getTransportEndpoint().getProtocolEndpoint().getConnectionEndpoint().getNotionalAddress();
			  if (!connection.isIncoming()) {
				try {helper = new UDPTransportHelper(UDPNetworkManager.getSingleton().getConnectionManager(), addr, (UDPTransport)core_transport);}
				catch (IOException ioe) {throw new TransportException(ioe);}
			  }
			  else {
				/**
				 * Not sure how I can grab the UDPConnection object to pass to the incoming
				 * connection constructor. The only time I can figure out where we can link
				 * up the UDPConnection object is in UDPConnectionManager.accept - we have a
				 * transport object and we construct a UDPConnection object, so we could link
				 * them there - but I don't know if we really should associate the UDP connection
				 * with the transport (might breaks encapsulation).
				 */
				  //helper = new UDPTransportHelper(UDPNetworkManager.getSingleton().getConnectionManager(), addr, (UDPTransport)core_transport);
				  throw new TransportException("udp incoming transport type not supported - " + core_transport);
			  }
		  }
		} else {
			throw new TransportException("transport type not supported - " + core_transport);
	  }

	  TransportHelperFilterStreamCipher core_filter = new TransportHelperFilterStreamCipher(helper, ((TransportCipherImpl)read_cipher).cipher, ((TransportCipherImpl)write_cipher).cipher);
	  return new TransportFilterImpl(core_filter);
  }
  
}
