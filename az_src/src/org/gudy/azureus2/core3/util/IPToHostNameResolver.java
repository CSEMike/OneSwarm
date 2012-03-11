/*
 * Created on 27-May-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class 
IPToHostNameResolver 
{
	static protected AEThread2		resolver_thread;
	static protected List			request_queue		= new ArrayList();
	static protected AEMonitor		request_mon			= new AEMonitor( "IPToHostNameResolver" );

	static protected AESemaphore	request_semaphore	= new AESemaphore("IPToHostNameResolver");
	
	public static IPToHostNameResolverRequest
	addResolverRequest(
		String							ip,
		IPToHostNameResolverListener	l )
	{
		try{
			request_mon.enter();
			
			IPToHostNameResolverRequest	request = new IPToHostNameResolverRequest( ip, l );
			
			request_queue.add( request );
			
			request_semaphore.release();
			
			if ( resolver_thread == null ){
				
				resolver_thread = 
					new AEThread2("IPToHostNameResolver", true )
					{
						public void
						run()
						{
							while(true){
								
								try{
									request_semaphore.reserve(30000);
									
									IPToHostNameResolverRequest	req;
									
									try{
										request_mon.enter();
										
										if ( request_queue.isEmpty()){
											
											resolver_thread = null;
											
											break;
										}
										
										req	= (IPToHostNameResolverRequest)request_queue.remove(0);
										
									}finally{
										
										request_mon.exit();
									}
									
									IPToHostNameResolverListener	listener = req.getListener();
									
										// if listener is null the request has been cancelled
									
									if ( listener != null ){
										
										try{
											InetAddress addr = InetAddress.getByName( req.getIP());
												
											req.getListener().IPResolutionComplete( addr.getHostName(), true );
												
										}catch( Throwable e ){
											
											req.getListener().IPResolutionComplete( req.getIP(), false );
											
										}
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace( e );
								}
							}
						}
					};
										
				resolver_thread.start();
			}
			
			return( request );
			
		}finally{
			
			request_mon.exit();
		}
	}
	
	public static String
	syncResolve(
		String			ip,
		int				timeout )
	
		throws Exception
	{
		final AESemaphore	sem = new AESemaphore( "IPToHostNameREsolver:sync" );
		
		final Object[]	result = {null};
				
		addResolverRequest(
			ip, 
			new IPToHostNameResolverListener()
			{
				public void
				IPResolutionComplete(
					String		resolved_ip,
					boolean		succeeded )
				{
					try{
						synchronized( result ){

							if ( succeeded ){
									
								result[0] = resolved_ip;
							}
						}
					}finally{
						
						sem.release();
					}
				}
			});
			
		if ( !sem.reserve( timeout )){
			
			throw( new Exception( "Timeout" ));
		}
				
		synchronized( result ){

			if ( result[0] != null ){
				
				return((String)result[0]);
			}
			
			throw( new UnknownHostException( ip ));
		}
	}
}
