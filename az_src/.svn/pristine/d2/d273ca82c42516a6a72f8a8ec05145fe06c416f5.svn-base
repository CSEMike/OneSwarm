/*
 * Created on 12-Sep-2005
 * Created by Paul Gardner
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

package org.gudy.azureus2.pluginsimpl.local.launch;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginManagerArgumentHandler;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;


public class 
PluginSingleInstanceHandler 
{
	private static boolean		active;
	
	private static int								port;
	private static PluginManagerArgumentHandler		handler;
	
	public static void
	initialise(
		int									_port,
		PluginManagerArgumentHandler		_handler )
	{
		port		= _port;
		handler		= _handler;
		
		String	multi_instance = System.getProperty( "MULTI_INSTANCE");
		
		if ( multi_instance != null && multi_instance.equalsIgnoreCase( "true" )){
			
			return;
		}
		
		active = true;
	}
	
	protected static boolean
	process(
		LoggerChannelListener	log,
		String[]				args )
	{
		if ( active ){
			
			if ( startListener( log )){
				
				return( false );
				
			}else{
				
				sendArguments( log, args );
				
				return( true );
			}
		}else{
			
			return( false );
		}
	}

	  
	protected static boolean
	startListener(
		final LoggerChannelListener	log )
	{
		try{
			final ServerSocket server_socket = new ServerSocket( port, 50, InetAddress.getByName("127.0.0.1"));
	              
			log.messageLogged( 
					  LoggerChannel.LT_INFORMATION,
					  "SingleInstanceHandler: listening on 127.0.0.1:" + port + " for passed arguments");
	    
			Thread t = 
				new Thread("Single Instance Handler")
				{
		    		public void 
		    		run()
					{
		    		    while ( true ){
		    		    	
		    		    	Socket socket			= null;
		    		    	ObjectInputStream	ois	= null;
		    		    	
		    		    	try{
		    		    		socket = server_socket.accept();
		    		    		
		    		    		String address = socket.getInetAddress().getHostAddress();
		    		    		
		    		    		if ( !( address.equals("localhost") || address.equals("127.0.0.1"))){
		    		    			
		    		    			socket.close();
		    		    			
		    		    			continue;
		    		    		}
		    		    		
		    		    		ois = new ObjectInputStream( socket.getInputStream());
		    		    		
		    		    		ois.readInt();	// version
		    		    		
		    		    		String	header = (String)ois.readObject();
		    		    		
		    		    		if ( !header.equals( getHeader())){
		    		    			
		    		    			log.messageLogged( 
		    		  					  LoggerChannel.LT_ERROR,
		    		  					  "SingleInstanceHandler: invalid header - " + header );
		    		    			
		    		    			continue;
		    		    		}

		    		    		String[]	args = (String[])ois.readObject();
		    		    		
		    		    		handler.processArguments( args );
		    		    		
		    		    	}catch( Throwable e ){
		    		    		
		    		    		log.messageLogged( "SingleInstanceHandler: receive error", e );

		    		    	}finally{
		    		    		
		    		    		if ( ois != null ){
		    		    			try{
		    		    				ois.close();
		    		    				
		    		    			}catch( Throwable e ){
		    		    			}
		    		    		}
		    		    		
		    		    		if ( socket != null ){
		    		    			try{
		    		    				socket.close();
		    		    				
		    		    			}catch( Throwable e ){
		    		    			}	
		    		    		}
		    		    	}
		    		    }
					}
				};
		    
			t.setDaemon( true );
				
			t.start(); 
			
			return( true );
			
		}catch( Throwable e ){
		
			return( false );
		}
	}
	   
	protected static void
	sendArguments(
		LoggerChannelListener	log,
		String[]				args )
	{
		Socket	socket = null;
	
		try{
			socket = new Socket( "127.0.0.1", port );
		       
			ObjectOutputStream	oos = new ObjectOutputStream( socket.getOutputStream());
			
			oos.writeInt( 0 );
			
			oos.writeObject( getHeader());
			
			oos.writeObject( args );
			
			log.messageLogged( LoggerChannel.LT_INFORMATION, "SingleInstanceHandler: arguments passed to existing process" );
			
    	}catch( Throwable e ){
    		
    		log.messageLogged( "SingleInstanceHandler: send error", e );

    	}finally{
    		
    		if ( socket != null ){
    			try{
    				socket.close();
    				
    			}catch( Throwable e ){
    			}	
    		}	
    	}
	}
	
	protected static String
	getHeader()
	{
		return( SystemProperties.getApplicationName() + " Single Instance Handler" );
	}
}
