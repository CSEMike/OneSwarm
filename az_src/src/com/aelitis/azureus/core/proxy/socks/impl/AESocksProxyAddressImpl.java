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

import java.net.InetAddress;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;

import com.aelitis.azureus.core.proxy.socks.AESocksProxyAddress;


/**
 * @author parg
 *
 */

public class 
AESocksProxyAddressImpl
	implements AESocksProxyAddress
{
	protected String			unresolved_address;
	protected InetAddress		address;
	protected int				port;
	
	protected
	AESocksProxyAddressImpl(
		String			_unresolved_address,
		InetAddress		_address,
		int				_port )
	{
		unresolved_address	= _unresolved_address;
		address				= _address;
		port				= _port;
		
		if ( address == null ){
			
				// see if we've been passed an IP address as unresolved
				// TODO: IPV6 one day?
			
			int			dots 	= 0;
			boolean		ok		= true;
			
			for (int i=0;i<unresolved_address.length();i++){
				
				char	c = unresolved_address.charAt(i);
				
				if ( c == '.' ){
					
					dots++;
					
					if ( dots>3 ){
						
						ok	= false;
						
						break;
					}
				}else if ( !Character.isDigit( c )){
					
					ok = false;
					
					break;
					
				}else{
					
						// nnn.nnn.nnn.nnn
					
					if ( i > 15 ){
						
						ok = false;
						
						break;
					}
				}
			}
			
			if ( ok && dots == 3 ){
			
				try{
					address = HostNameToIPResolver.syncResolve( unresolved_address );
					
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}
	}
	
	public String
	getUnresolvedAddress()
	{
		return( unresolved_address );
	}
	
	public InetAddress
	getAddress()
	{
		return( address );
	}
	
	public int
	getPort()
	{
		return( port );
	}
}
