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
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.gudy.azureus2.core3.logging.*;

import com.aelitis.azureus.core.proxy.*;
import com.aelitis.azureus.core.proxy.socks.*;

/**
 * @author parg
 *
 */

public class
AESocksProxyState
	implements AEProxyState
{
	private static final LogIDs LOGID = LogIDs.NET;
	private AESocksProxyConnection	socks_connection;
	
	protected ByteBuffer					buffer;

	protected
	AESocksProxyState(
		AESocksProxyConnection	_socks_connection )
	{
		socks_connection	= _socks_connection;
		
		if ( AESocksProxyConnectionImpl.TRACE ){
			Logger.log(new LogEvent(LOGID, socks_connection.getConnection().getName()
					+ ":" + getStateName()));
		}
	}
	
	public String
	getStateName()
	{
		String	state = this.getClass().getName();
		
		int	pos = state.indexOf( "$");
		
		state = state.substring(pos+1);
		
		return( state  + ", buffer = " + buffer );
	}
	
	public final boolean
	read(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			return( readSupport(sc));
			
		}finally{
			
			trace();
		}
	}
	
	protected boolean
	readSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Read not supported: " + sc ));
	}
	
	public final boolean
	write(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			return( writeSupport(sc));
			
		}finally{
			
			trace();
		}		
	}	
	
	protected boolean
	writeSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Write not supported: " + sc ));
	}	
	
	public final boolean
	connect(
		SocketChannel 		sc )
	
		throws IOException
	{
		try{
			return( connectSupport(sc));
			
		}finally{
			
			trace();
		}
	}	
	
	protected boolean
	connectSupport(
		SocketChannel 		sc )
	
		throws IOException
	{
		throw( new IOException( "Connect not supported: " + sc ));
	}	
	
	protected void
	trace()
	{
		if ( AESocksProxyConnectionImpl.TRACE ){
			Logger.log(new LogEvent(LOGID, socks_connection.getConnection().getName()
					+ ":" + getStateName()));
		}
	}
}

