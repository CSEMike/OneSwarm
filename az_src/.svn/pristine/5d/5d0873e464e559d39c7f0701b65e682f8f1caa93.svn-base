/*
 * Created on 06-Mar-2005
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

package org.gudy.azureus2.core3.util.protocol.magnet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;


import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author parg
 *
 */

public class 
MagnetConnection
	extends HttpURLConnection
{
	private Socket	socket;
	
	private static final String	NL			= "\r\n";

	private String	status = "";
	
	protected
	MagnetConnection(
		URL		_url )
	{
		super( _url );
	}
	
	public void
	connect()
		throws IOException
		
	{
		socket = new Socket( "127.0.0.1", MagnetURIHandler.getSingleton().getPort());
						
		String	get = "GET " + "/download/" + getURL().toString().substring( 7 ) + " HTTP/1.0" + NL + NL;
		
		socket.getOutputStream().write( get.getBytes());
		
		socket.getOutputStream().flush();
	}
	
	public InputStream
	getInputStream()
	
		throws IOException
	{
		InputStream	is = socket.getInputStream();
		
		String	line = "";
		
		byte[]	buffer = new byte[1];

		byte[]	line_bytes		= new byte[2048];
		int		line_bytes_pos 	= 0;
		
		while(true){
		
			int	len = is.read( buffer );
			
			if ( len == -1 ){
				
				break;
			}
			
			line += (char)buffer[0];
			
			line_bytes[line_bytes_pos++] = buffer[0];
			
			if ( line.endsWith( NL )){
				
				line = line.trim();
				
				if ( line.length() == 0 ){
					
					break;
				}
				
				if ( line.startsWith( "X-Report:")){
					
					line = new String( line_bytes, 0, line_bytes_pos, "UTF-8" );
					
					line = line.substring( 9 );
										
					line = line.trim();
					
					status = Character.toUpperCase( line.charAt(0)) + line.substring(1);
				}
				
				line			= "";
				line_bytes_pos	= 0;
			}
		}
		
		return( is );
	}
	
	public int
	getResponseCode()
	{
		return( HTTP_OK );
	}
	
	public String
	getResponseMessage()
	{
		return( status );
	}
	
	public boolean
	usingProxy()
	{
		return( false );
	}
	
	public void
	disconnect()
	{
		try{
			socket.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
}
