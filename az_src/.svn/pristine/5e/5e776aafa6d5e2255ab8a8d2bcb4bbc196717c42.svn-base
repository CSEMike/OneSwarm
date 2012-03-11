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

import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.network.*;

import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;



/**
 *
 */
public class ConnectionImpl implements Connection {

  private final com.aelitis.azureus.core.networkmanager.NetworkConnection core_connection;
  private final OutgoingMessageQueueImpl out_queue;
  private final IncomingMessageQueueImpl in_queue;
  private final TransportImpl transport;
  private final boolean incoming;
  
  
  public ConnectionImpl( com.aelitis.azureus.core.networkmanager.NetworkConnection core_connection, boolean incoming ) {
    this.core_connection = core_connection;
    this.out_queue = new OutgoingMessageQueueImpl( core_connection.getOutgoingMessageQueue() );
    this.in_queue = new IncomingMessageQueueImpl( core_connection.getIncomingMessageQueue() );
    this.transport = new TransportImpl( core_connection );
    this.incoming = incoming;
  }
  
  
  public void connect( final ConnectionListener listener ) {
    core_connection.connect( ProtocolEndpoint.CONNECT_PRIORITY_MEDIUM, new com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener() {
      public int connectStarted( int ct ) { listener.connectStarted(); return( ct ); }
      
      public void connectSuccess( ByteBuffer remaining_initial_data) { listener.connectSuccess();  }
      
      public void connectFailure( Throwable failure_msg ) {  listener.connectFailure( failure_msg );  }
      public void exceptionThrown( Throwable error ) {  listener.exceptionThrown( error );  }
      
      public String
      getDescription()
      {
    	  return( "plugin connection: " + core_connection.getString());
      }
    });
  }
  
  
  public void close() {
    core_connection.close( null );
  }

  
  public OutgoingMessageQueue getOutgoingMessageQueue() {  return out_queue;  }

  public IncomingMessageQueue getIncomingMessageQueue() {  return in_queue;  }

  
  public void startMessageProcessing() {
	
    core_connection.startMessageProcessing();
    
    core_connection.enableEnhancedMessageProcessing( true, -1 );  //auto-upgrade connection
  }
  
  
  public Transport getTransport() {  return transport;  }
  
  
  public com.aelitis.azureus.core.networkmanager.NetworkConnection getCoreConnection() {
    return core_connection;
  }
  
  public boolean isIncoming() {
	  return this.incoming;
  }
  
  public String
  getString()
  {
	  com.aelitis.azureus.core.networkmanager.Transport t = core_connection.getTransport();
	  
	  if ( t == null ){
		  
		  return( "" );
		  
	  }else{
		  
		  return( t.getEncryption( false ));
	  }
  }
}
