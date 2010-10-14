/*
 * Created on 22 Jun 2006
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.networkmanager.impl.udp;

import java.util.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
NetworkGlueLoopBack
	implements NetworkGlue
 
{
	private int latency			= 0;
	
	private NetworkGlueListener		listener;
	
	private List	message_queue	= new ArrayList();
	
	private Random	random = new Random();
	
	protected
	NetworkGlueLoopBack(
		NetworkGlueListener		_listener )
	{
		listener	= _listener;
				
		new AEThread( "NetworkGlueLoopBack", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					try{
						Thread.sleep(1);
						
					}catch( Throwable e ){
						
					}
				
					InetSocketAddress	target_address 	= null;
					InetSocketAddress	source_address 	= null;
					byte[]				data			= null;
					
					long	now = SystemTime.getCurrentTime();
					
					synchronized( message_queue ){
						
						if ( message_queue.size() > 0 ){
							
							Object[]	entry = (Object[])message_queue.get(0);
							
							if (((Long)entry[0]).longValue() < now ){
								
								message_queue.remove(0);
								
								source_address	= (InetSocketAddress)entry[1];
								target_address 	= (InetSocketAddress)entry[2];
								data			= (byte[])entry[3];
							}
						}
					}
					
					if ( source_address != null ){
	
						listener.receive( target_address.getPort(), source_address, data, data.length );
					}
				}
			}
		}.start();
	}
	
	public int
	send(
		int					local_port,
		InetSocketAddress	target,
		byte[]				data )
	
		throws IOException
	{	
		Long	expires = new Long( SystemTime.getCurrentTime() + latency );
			
		InetSocketAddress local_address = new InetSocketAddress( target.getAddress(), local_port );
			
		synchronized( message_queue ){
				
			if ( random.nextInt(4) != 9 ){
					
				message_queue.add( new Object[]{ expires, local_address, target, data });
			}
		}
		
		return( data.length );
	}
	
	public long[]
	getStats()
	{
		return( new long[]{ 0,0,0,0 });
	}
}
