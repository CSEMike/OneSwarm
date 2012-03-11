/*
 * File    : PEPeerTransportFactory.java
 * Created : 21-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */


import java.util.*;

import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;


public class 
PEPeerTransportFactory 
{
	protected static Map	extension_handlers = new HashMap();
	
  
  /**
   * Create a new default outgoing connection transport.
   * @param control
   * @param ip
   * @param port
   * @return transport
   */
	public static PEPeerTransport
	createTransport(
		PEPeerControl	 	control,
		String				peer_source,
		String 				ip, 
		int 				tcp_port,
		int					udp_port,
		boolean				use_tcp,
		boolean 			require_crypto_handshake,
		byte				crypto_level,
		Map					initial_user_data )			
	{
		return new PEPeerTransportProtocol( control, peer_source, ip, tcp_port, udp_port, use_tcp, require_crypto_handshake, crypto_level, initial_user_data );    
	}
  
  
  //incoming
	
  public static PEPeerTransport 
  createTransport( 
  		PEPeerControl 		control,
		String				peer_source,
		NetworkConnection 	connection,
		Map					initial_user_data )
  {
    return new PEPeerTransportProtocol( control, peer_source, connection, initial_user_data );
  }
  
  
  

	public static void
	registerExtensionHandler(
		String								protocol_name,
		PEPeerTransportExtensionHandler		handler )
	{
		extension_handlers.put( protocol_name, handler );
	}
	
	public static List
	createExtendedTransports(
		PEPeerControl	manager,
		String			protocol_name,
		Map				details )
	{
		System.out.println( "createExtendedTransports:" + protocol_name );
		
		PEPeerTransportExtensionHandler	handler = (PEPeerTransportExtensionHandler)extension_handlers.get( protocol_name );
		
		if ( handler == null ){
			
			System.out.println( "\tNo handler");
			
			return( new ArrayList());
		}
		
		return( handler.handleExtension( manager, details ));
	}
	
}
