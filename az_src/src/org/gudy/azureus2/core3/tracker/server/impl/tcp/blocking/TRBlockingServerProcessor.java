/*
 * Created on 02-Jan-2005
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

package org.gudy.azureus2.core3.tracker.server.impl.tcp.blocking;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerException;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerProcessorTCP;
import org.gudy.azureus2.core3.tracker.server.impl.tcp.TRTrackerServerTCP;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Constants;

/**
 * @author parg
 *
 */

public class 
TRBlockingServerProcessor
	extends TRTrackerServerProcessorTCP
{
	private static final LogIDs LOGID = LogIDs.TRACKER;
	protected Socket				socket;
	

	protected int					timeout_ticks		= 1;
	protected String				current_request;
	

	protected
	TRBlockingServerProcessor(
		TRTrackerServerTCP		_server,
		Socket					_socket )
	{
		super( _server );
		
		socket	= _socket;
	}

	public void
	runSupport()
	{
		try{
	
			setTaskState( "entry" );
			
			try{
												
				socket.setSoTimeout( SOCKET_TIMEOUT );
										
			}catch ( SocketException e ){
													
				// e.printStackTrace();
			}
										
			String	header_plus = "";
			
			setTaskState( "reading header" );

			try{
										
				InputStream	is = socket.getInputStream();
				
				byte[]	buffer = new byte[1024];
				
				while( header_plus.length()< 4096 ){
						
					int	len = is.read(buffer);
						
					if ( len == -1 ){
					
						break;
					}
									
					header_plus += new String( buffer, 0, len, Constants.BYTE_ENCODING );
									
					if ( 	header_plus.endsWith(NL+NL) ||
							header_plus.indexOf( NL+NL ) != -1 ){
						
						break;
					}
				}
		
				if ( Logger.isEnabled()){
					
					String	log_str = header_plus;
					
					int	pos = log_str.indexOf( NL );
					
					if ( pos != -1 ){
						
						log_str = log_str.substring(0,pos);
					}
					
					Logger.log(new LogEvent(LOGID, "Tracker Server: received header '"
							+ log_str + "'"));
				}				
					
				// System.out.println( "got header:" + header_plus );
				
				InputStream	post_is 	= null;
				File		post_file	= null;
				
				String	actual_header;
				String	lowercase_header;
				
				boolean	head	= false;
				
				String	url;
				

				if ( header_plus.startsWith( "GET " )){
				
					timeout_ticks		= 1;
					
					actual_header		= header_plus;
					lowercase_header	= actual_header.toLowerCase();
					url					 = actual_header.substring(4).trim();
					
				}else if ( header_plus.startsWith( "HEAD " )){
					
					timeout_ticks		= 1;
					
					actual_header		= header_plus;
					lowercase_header	= actual_header.toLowerCase();			
					url 				= actual_header.substring(4).trim();

					head	= true;
					
				}else if ( header_plus.startsWith( "POST ")){
					
					timeout_ticks	= TRTrackerServerTCP.PROCESSING_POST_MULTIPLIER;
					
					if ( timeout_ticks == 0 ){
						
						setTimeoutsDisabled( true );
					}
					
					setTaskState( "reading content" );

					int	header_end = header_plus.indexOf(NL+NL);
					
					if ( header_end == -1 ){
					
						throw( new TRTrackerServerException( "header truncated" ));
					}
					
					actual_header 		= header_plus.substring(0,header_end+4);
					lowercase_header	= actual_header.toLowerCase();
					url 				= actual_header.substring(4).trim();

					int	cl_start = lowercase_header.indexOf("content-length:");
					
					if ( cl_start == -1 ){
						
						throw( new TRTrackerServerException( "header Content-Length start missing" ));
					}
					
					int	cl_end = actual_header.indexOf( NL, cl_start );
					
					if ( cl_end == -1 ){
						
						throw( new TRTrackerServerException( "header Content-Length end missing" ));
					}
					
					int	content_length = Integer.parseInt( actual_header.substring(cl_start+15,cl_end ).trim());
				
					ByteArrayOutputStream	baos		= null;
					FileOutputStream		fos			= null;
				
					OutputStream	data_os;
					
					if ( content_length <= 256*1024 ){
						
						baos = new ByteArrayOutputStream();
					
						data_os	= baos;
						
					}else{
						
						post_file	= AETemporaryFileHandler.createTempFile();
						
						post_file.deleteOnExit();
						
						fos	= new FileOutputStream( post_file );
						
						data_os	= fos;
					}
					
						// if we have X<NL><NL>Y get Y
					
					int	rem = header_plus.length() - (header_end+4);
					
					if ( rem > 0 ){
						
						content_length	-= rem;
						
						data_os.write( header_plus.substring(header_plus.length()-rem).getBytes( Constants.BYTE_ENCODING ));
					}
					
					while( content_length > 0 ){
						
						int	len = is.read( buffer );
						
						if ( len < 0 ){
							
							throw( new TRTrackerServerException( "premature end of input stream" ));
						}
						
						data_os.write( buffer, 0, len );
						
						content_length -= len;
					}
					
					if ( baos != null ){
						
						post_is = new ByteArrayInputStream(baos.toByteArray());
						
					}else{
						
						fos.close();
						
						post_is = new BufferedInputStream( new FileInputStream( post_file ), 256*1024 );
					}
					
					// System.out.println( "TRTrackerServerProcessorTCP: request data = " + baos.size());
					
				}else{
					
					int	pos = header_plus.indexOf(' ');
					
					if ( pos == -1 ){
						
						throw( new TRTrackerServerException( "header doesn't have space in right place" ));
					}
					
					timeout_ticks		= 1;
						
					actual_header		= header_plus;
					lowercase_header	= actual_header.toLowerCase();
					url					= actual_header.substring(pos+1).trim();
				}
				
				setTaskState( "processing request" );

				current_request	= actual_header;
				
				try{
					if ( post_is == null ){
						
							// set up a default input stream 
						
						post_is = new ByteArrayInputStream(new byte[0]);
					}
					
					int	pos = url.indexOf( " " );
					
					if ( pos == -1 ){
						
						throw( new TRTrackerServerException( "header doesn't have space in right place" ));
					}
									
					url = url.substring(0,pos);
					
					if ( head ){
						
						ByteArrayOutputStream	head_response = new ByteArrayOutputStream(4096);
						
						processRequest( actual_header,
										lowercase_header,
										url, 
										(InetSocketAddress)socket.getRemoteSocketAddress(),
										false,
										post_is,
										head_response );
						
						byte[]	head_data = head_response.toByteArray();
						
						int	header_length = head_data.length;
						
						for (int i=3;i<head_data.length;i++){
							
							if ( 	head_data[i-3] 	== CR &&
									head_data[i-2] 	== FF &&
									head_data[i-1] 	== CR &&
									head_data[i]	== FF ){
								
								header_length = i+1;
						
								break;
							}
						}
												
						setTaskState( "writing head response" );

						socket.getOutputStream().write( head_data, 0, header_length );
						
						socket.getOutputStream().flush();
						
					}else{
					
						processRequest( actual_header,
										lowercase_header,
										url, 
										(InetSocketAddress)socket.getRemoteSocketAddress(),
										false,
										post_is,
										socket.getOutputStream() );
					}
				}finally{
					
					if ( post_is != null ){
						
						post_is.close();
					}
					
					if ( post_file != null ){
						
						post_file.delete();
					}
				}
			}catch( SocketTimeoutException e ){
				
				// System.out.println( "TRTrackerServerProcessor: timeout reading header, got '" + header + "'");
				// ignore it
							
			}catch( Throwable e ){
				
				 // e.printStackTrace();
			}
	
		}finally{
					
			setTaskState( "final socket close" );

			try{
				socket.close();
																							
			}catch( Throwable e ){
													
				// e.printStackTrace();
			}
		}
	}

	public void
	interruptTask()
	{
		try{
			if ( areTimeoutsDisabled() ){
				
				// Debug.out( "External tracker request timeout ignored: state = " + getTaskState() + ", req = " + current_request  );
				
			}else{
				
				timeout_ticks--;
					
				if ( timeout_ticks <= 0 ){
					
					System.out.println( "Tracker task interrupted in state '" + getTaskState() + "' : processing time limit exceeded for " + socket.getInetAddress() );
					
					socket.close();
				}
			}
																						
		}catch( Throwable e ){
												
			// e.printStackTrace();
		}
	}

}
