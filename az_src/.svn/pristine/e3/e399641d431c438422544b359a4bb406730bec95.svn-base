/*
 * Created on 11-Sep-2006
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

package com.aelitis.azureus.core.impl;

import java.net.*;
import java.io.*;


/**
 * Single instance management is a bit of a mess. For some reason the UIs have their own implementations of clients and servers.
 * We also have a more generic plugin-accessible single instance service that can be used by launchable plugins but don't give
 * a generic mechanism for dealing with the basic mechanism used by the UIs (that run on 6880).
 * I have introduced this class to give a programmatic way of passing arguments using the existing 6880 port. Perhaps one day
 * the various UI implementations will be rewritten to use this...
 * @author Parg
 */

public class 
AzureusCoreSingleInstanceClient 
{
	public static final String ACCESS_STRING = "Azureus Start Server Access";
	
	private static final int CONNECT_TIMEOUT	= 500;
	private static final int READ_TIMEOUT		= 5000;
	
	public boolean
	sendArgs(
		String[]	args,
		int			max_millis_to_wait )
	{
			// limit the subset here as we're looping waiting for something to be alive and we can't afford to take ages getting back to the start
		
		long	start = System.currentTimeMillis();
		
		while( true ){
			
			long	connect_start = System.currentTimeMillis();
			
			if ( connect_start < start ){
				
				start  = connect_start;
			}
			
			if ( connect_start - start > max_millis_to_wait ){
				
				return( false );
			}
			
			Socket	sock = null;
			
			try{
				sock = new Socket();
				
				sock.connect( new InetSocketAddress( "127.0.0.1", 6880 ), CONNECT_TIMEOUT );
				
				sock.setSoTimeout( READ_TIMEOUT );
				
		   		PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(),"UTF-8"));
		   	 
		  		StringBuffer buffer = new StringBuffer( ACCESS_STRING + ";args;");
		         
	    		for ( int i = 0 ; i < args.length ; i++ ){
	    			
	    			String arg = args[i].replaceAll("&","&&").replaceAll(";","&;");
	    			
	    			buffer.append(arg);
	    			
	    			buffer.append(';');
	    		}
	        
	    		pw.println(buffer.toString());
	    		
	    		pw.flush();
	    		
	    		return( true );
	    		
			}catch( Throwable e ){
				
				long connect_end = System.currentTimeMillis();
				
				long time_taken = connect_end - connect_start;
				
				if ( time_taken < CONNECT_TIMEOUT ){
				
					try{
						Thread.sleep( CONNECT_TIMEOUT - time_taken );
						
					}catch( Throwable f ){
					}
				}
			}finally{
				
				try{
					if ( sock != null ){
						
						sock.close();
					}
				}catch( Throwable e ){
				}
			}
			
	
		}
	}
	
	public static void
	main(
		String[]	args )
	{
		new AzureusCoreSingleInstanceClient().sendArgs( new String[]{ "6C0B39D9897AF42F624AC2DE010CF33F55CB45EC" }, 30000 );
	}
}
