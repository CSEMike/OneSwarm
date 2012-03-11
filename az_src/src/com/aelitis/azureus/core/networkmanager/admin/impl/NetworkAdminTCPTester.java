/*
 * Created on 1 Nov 2006
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.networkmanager.admin.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.gudy.azureus2.core3.ipchecker.natchecker.NatChecker;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminException;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminProgressListener;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
NetworkAdminTCPTester 
	implements NetworkAdminProtocolTester
{
	private AzureusCore						core;
	private NetworkAdminProgressListener	listener;
	
	protected
	NetworkAdminTCPTester(
		AzureusCore						_core,
		NetworkAdminProgressListener	_listener )
	{
		core		= _core;
		listener	= _listener;
	}
	
	public InetAddress
	testOutbound(
		InetAddress		bind_ip,
		int				bind_port )
	
		throws NetworkAdminException
	{
		try{
				// try to use our service first 
			
			return( VersionCheckClient.getSingleton().getExternalIpAddressTCP( bind_ip, bind_port, false ));
			
		}catch( Throwable e ){
			
				// fallback to something else
			
			try{
				  Socket socket = new Socket();
					 
				  if ( bind_ip != null ){
					  
					  socket.bind( new InetSocketAddress( bind_ip, bind_port ));
					  
				  }else if ( bind_port != 0 ){
					  
					  socket.bind( new InetSocketAddress( bind_port ));
				  }
				  
				  socket.setSoTimeout( 10000 );
				
				  socket.connect( new InetSocketAddress( "www.google.com", 80 ), 10000 );
				  
				  socket.close();
				  
				  return( null );
				  
			}catch( Throwable f ){

				throw( new NetworkAdminException( "Outbound test failed", e ));
			}
		}
	}
	
	public InetAddress
	testInbound(			
		InetAddress		bind_ip,
		int				local_port )
	
		throws NetworkAdminException
	{
		NatChecker	checker = new NatChecker( core, bind_ip, local_port, false );
		
		if ( checker.getResult() == NatChecker.NAT_OK ){
			
			return( checker.getExternalAddress());
			
		}else{
			
			throw( new NetworkAdminException( "NAT test failed: " + checker.getAdditionalInfo()));
		}
	}
}
