/*
 * Created on 16-Dec-2005
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

package com.aelitis.azureus.plugins.extseed.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.URL;
import java.util.StringTokenizer;

import org.gudy.azureus2.core3.security.SEPasswordListener;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.util.Java15Utils;
import com.aelitis.azureus.plugins.extseed.ExternalSeedException;

public class 
ExternalSeedHTTPDownloaderRange 
	implements ExternalSeedHTTPDownloader, SEPasswordListener
{
	public static final String	NL = "\r\n";
	

	private URL			original_url;
	private String		user_agent;
	
	private URL			redirected_url;
	private int			consec_redirect_fails;
	
	private int			last_response;
	private int			last_response_retry_after_secs;
    	
	public
	ExternalSeedHTTPDownloaderRange(
		URL		_url,
		String	_user_agent )
	{
		original_url	= _url;
		user_agent		= _user_agent;
	}
	
	public URL
	getURL()
	{
		return( original_url );
	}
	
	public void
	download(
		int									length,
		ExternalSeedHTTPDownloaderListener	listener,
		boolean								con_fail_is_perm_fail )
	
		throws ExternalSeedException
	{
		download( new String[0], new String[0], length, listener, con_fail_is_perm_fail );
	}
	
	public void
	downloadRange(
		long								offset,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener,
		boolean								con_fail_is_perm_fail )
	
		throws ExternalSeedException
	{
		download( 	new String[]{ "Range" }, new String[]{ "bytes=" + offset + "-" + (offset+length-1)},
					length,
					listener,
					con_fail_is_perm_fail );
	}
	
	public void
	download(
		String[]							prop_names,
		String[]							prop_values,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener,
		boolean								con_fail_is_perm_fail )
	
		throws ExternalSeedException
	{
		boolean	connected = false;
		
		InputStream	is	= null;
				
		String	outcome = "";
		
		try{
			SESecurityManager.setThreadPasswordHandler( this );
			
			// System.out.println( "Connecting to " + url + ": " + Thread.currentThread().getId());

			HttpURLConnection	connection;
			int					response;
			
			while( true ){
				
				URL	target = redirected_url==null?original_url:redirected_url;
				
				connection = (HttpURLConnection)target.openConnection();
				
				connection.setRequestProperty( "Connection", "Keep-Alive" );
				connection.setRequestProperty( "User-Agent", user_agent );
				
				for (int i=0;i<prop_names.length;i++){
					
					connection.setRequestProperty( prop_names[i], prop_values[i] );
				}
				
				int	time_remaining	= listener.getPermittedTime();
				
				if ( time_remaining > 0 ){
					
					Java15Utils.setConnectTimeout( connection, time_remaining );
				}
							
				connection.connect();
			
				time_remaining	= listener.getPermittedTime();
									
				if ( time_remaining < 0 ){
					
					throw( new IOException( "Timeout during connect" ));
				}
				
				Java15Utils.setReadTimeout( connection, time_remaining );
						
				connected	= true;
				
				response = connection.getResponseCode();

				if (	response == HttpURLConnection.HTTP_ACCEPTED || 
						response == HttpURLConnection.HTTP_OK ||
						response == HttpURLConnection.HTTP_PARTIAL ){
					
					if ( redirected_url != null ){
						
						consec_redirect_fails = 0;
					}
					
					break;
				}
				
				if ( redirected_url == null ){
					
					break;
				}
				
					// try again with original URL
				
				consec_redirect_fails++;
				
				redirected_url = null;
			}
			
			URL final_url = connection.getURL();
			
			if ( consec_redirect_fails < 10 && !original_url.toExternalForm().equals( final_url.toExternalForm())){
				
				redirected_url = final_url;
			}
			
			last_response	= response;
			
			last_response_retry_after_secs	= -1;
			
            if ( response == 503 ){
                           
                	// webseed support for temp unavail - read the retry_after
            	
            	long retry_after_date = new Long(connection.getHeaderFieldDate("Retry-After", -1L)).longValue();
            	
                if ( retry_after_date <= -1 ){
                	
                	last_response_retry_after_secs = connection.getHeaderFieldInt("Retry-After", -1);
                    
                }else{
                	
                	last_response_retry_after_secs = (int)((retry_after_date - System.currentTimeMillis())/1000);
                	
                	if ( last_response_retry_after_secs < 0 ){
                		
                		last_response_retry_after_secs = -1;
                	}
                }
            }
            
			is = connection.getInputStream();
			
			if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
					response == HttpURLConnection.HTTP_OK ||
					response == HttpURLConnection.HTTP_PARTIAL ){
								
				int	pos = 0;
				
				byte[]	buffer 		= null;
				int		buffer_pos	= 0;
				int		buffer_len	= 0;

				while( pos < length ){
					
					if ( buffer == null ){
						
						buffer 		= listener.getBuffer();						
						buffer_pos	= listener.getBufferPosition();
						buffer_len	= listener.getBufferLength();
					}

					listener.setBufferPosition( buffer_pos );
					
					int	to_read = buffer_len - buffer_pos;
					
					int	permitted = listener.getPermittedBytes();
					
					if ( permitted < to_read ){
						
						to_read	= permitted;
					}
					
					int	len = is.read( buffer, buffer_pos, to_read );
					
					if ( len < 0 ){
						
						break;
					}
					
					listener.reportBytesRead( len );
					
					pos	+= len;
					
					buffer_pos	+= len;
					
					if ( buffer_pos == buffer_len ){
						
						listener.done();
						
						buffer		= null;
						buffer_pos	= 0;
					}
				}
				
				if ( pos != length ){
					
					String	log_str;
					
					if ( buffer == null ){
						
						log_str = "No buffer assigned";
						
					}else{
						
						log_str =  new String( buffer, 0, length );
						
						if ( log_str.length() > 64 ){
							
							log_str = log_str.substring( 0, 64 );
						}
					}
					
					outcome = "Connection failed: data too short - " + length + "/" + pos + " [" + log_str + "]";
					
					throw( new ExternalSeedException( outcome ));
				}
				
				outcome = "read " + pos + " bytes";
				
				// System.out.println( "download length: " + pos );
				
			}else{
				
				outcome = "Connection failed: " + connection.getResponseMessage();
				
				ExternalSeedException	error = new ExternalSeedException( outcome );
				
				error.setPermanentFailure( true );
				
				throw( error );
			}
		}catch( IOException e ){
			
			if ( con_fail_is_perm_fail && !connected ){
				
				outcome = "Connection failed: " + e.getMessage();
				
				ExternalSeedException	error = new ExternalSeedException( outcome );
				
				error.setPermanentFailure( true );
				
				throw( error );

			}else{
				
				outcome =  "Connection failed: " + Debug.getNestedExceptionMessage( e );
                
                if ( last_response_retry_after_secs >= 0){
                	
                    outcome += ", Retry-After: " + last_response_retry_after_secs + " seconds";
                }
				                
				ExternalSeedException excep = new ExternalSeedException( outcome, e );
				
				if ( e instanceof FileNotFoundException ){
					
					excep.setPermanentFailure( true );
				}
				
				throw( excep );
			}
		}catch( Throwable e ){
			
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			outcome = "Connection failed: " + Debug.getNestedExceptionMessage( e );
			
			throw( new ExternalSeedException("Connection failed", e ));
			
		}finally{
			
			SESecurityManager.unsetThreadPasswordHandler();

			// System.out.println( "Done to " + url + ": " + Thread.currentThread().getId() + ", outcome=" + outcome );

			if ( is != null ){
				
				try{
					is.close();
					
				}catch( Throwable e ){
					
				}
			}
		}
	}
	
	public void
	downloadSocket(
		int									length,
		ExternalSeedHTTPDownloaderListener	listener,
		boolean								con_fail_is_perm_fail )
	        	
	    throws ExternalSeedException
	{
		downloadSocket( new String[0], new String[0], length, listener, con_fail_is_perm_fail );
	}
	
	public void
	downloadSocket(
		String[]							prop_names,
		String[]							prop_values,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener,
		boolean								con_fail_is_perm_fail )
	
		throws ExternalSeedException
	{
		Socket	socket	= null;
		
		boolean	connected = false;
		
		try{				
			String	output_header = 
				"GET " + original_url.getPath() + "?" + original_url.getQuery() + " HTTP/1.1" + NL +
				"Host: " + original_url.getHost() + (original_url.getPort()==-1?"":( ":" + original_url.getPort())) + NL +
				"Accept: */*" + NL +
				"Connection: Close" + NL +	// if we want to support keep-alive we'll need to implement a socket cache etc.
				"User-Agent: " + user_agent + NL;
		
			for (int i=0;i<prop_names.length;i++){
				
				output_header += prop_names[i] + ":" + prop_values[i] + NL;
			}
			
			output_header += NL;
			
			int	time_remaining	= listener.getPermittedTime();
			
			if ( time_remaining > 0 ){
				
				socket = new Socket();
				
				socket.connect( new InetSocketAddress( original_url.getHost(), original_url.getPort()==-1?original_url.getDefaultPort():original_url.getPort()), time_remaining );
				
			}else{
		
				socket = new Socket(  original_url.getHost(), original_url.getPort()==-1?original_url.getDefaultPort():original_url.getPort());
			}
			
			connected	= true;
			
			time_remaining	= listener.getPermittedTime();

			if ( time_remaining < 0 ){
					
				throw( new IOException( "Timeout during connect" ));
				
			}else if ( time_remaining > 0 ){
				
				socket.setSoTimeout( time_remaining );
			}
			
			OutputStream	os = socket.getOutputStream();
			
			os.write( output_header.getBytes( "ISO-8859-1" ));
			
			os.flush();
			
			InputStream is = socket.getInputStream();
			
			try{
				String	input_header = "";
				
				while( true ){
					
					byte[]	buffer = new byte[1];
					
					int	len = is.read( buffer );
					
					if ( len < 0 ){
						
						throw( new IOException( "input too short reading header" ));
					}
					
					input_header	+= (char)buffer[0];
					
					if ( input_header.endsWith(NL+NL)){
					
						break;
					}
				}
								
				// HTTP/1.1 403 Forbidden
				
				int	line_end = input_header.indexOf(NL);
				
				if ( line_end == -1 ){
					
					throw( new IOException( "header too short" ));
				}
				
				String	first_line = input_header.substring(0,line_end);
				
				StringTokenizer	tok = new StringTokenizer(first_line, " " );
				
				tok.nextToken();
				
				int	response = Integer.parseInt( tok.nextToken());
				
				last_response	= response;
				
				last_response_retry_after_secs	= -1;
				
				String	response_str	= tok.nextToken();				
				
				if ( 	response == HttpURLConnection.HTTP_ACCEPTED || 
						response == HttpURLConnection.HTTP_OK ||
						response == HttpURLConnection.HTTP_PARTIAL ){
					
					byte[]	buffer 		= null;
					int		buffer_pos	= 0;
					int		buffer_len	= 0;
					
					int	pos = 0;
					
					while( pos < length ){
						
						if ( buffer == null ){
							
							buffer 		= listener.getBuffer();							
							buffer_pos	= listener.getBufferPosition();
							buffer_len	= listener.getBufferLength();
						}
						
						int	to_read = buffer_len - buffer_pos;
						
						int	permitted = listener.getPermittedBytes();
						
						if ( permitted < to_read ){
							
							to_read	= permitted;
						}
						
						int	len = is.read( buffer, buffer_pos, to_read );
						
						if ( len < 0 ){
							
							break;
						}
						
						listener.reportBytesRead( len );
						
						pos	+= len;
						
						buffer_pos	+= len;
						
						if ( buffer_pos == buffer_len ){
							
							listener.done();
							
							buffer		= null;
							buffer_pos	= 0;
						}
					}
					
					if ( pos != length ){
						
						String	log_str;
						
						if ( buffer == null ){
							
							log_str = "No buffer assigned";
							
						}else{
							
							log_str =  new String( buffer, 0, buffer_pos>64?64:buffer_pos );
						}
						
						throw( new ExternalSeedException("Connection failed: data too short - " + length + "/" + pos + " [last=" + log_str + "]" ));
					}
					
					// System.out.println( "download length: " + pos );
										
				}else if ( 	response == 503 ){
					
						// webseed support for temp unavail - read the data
					
					String	data_str = "";
					
					while( true ){
						
						byte[]	buffer = new byte[1];
						
						int	len = is.read( buffer );
						
						if ( len < 0 ){
							
							break;
						}
						
						data_str += (char)buffer[0];
					}
					
					last_response_retry_after_secs = Integer.parseInt( data_str );
				
						// this gets trapped below and turned into an appropriate ExternalSeedException
					
					throw( new IOException( "Server overloaded" ));
					
				}else{
					
					ExternalSeedException	error = new ExternalSeedException("Connection failed: " + response_str );
					
					error.setPermanentFailure( true );
					
					throw( error );
				}
			}finally{
				
				is.close();
			}
			
		}catch( IOException e ){
			
			if ( con_fail_is_perm_fail && !connected ){
				
				ExternalSeedException	error = new ExternalSeedException("Connection failed: " + e.getMessage());
				
				error.setPermanentFailure( true );
				
				throw( error );

			}else{
				
				String outcome =  "Connection failed: " + Debug.getNestedExceptionMessage( e );

				if ( last_response_retry_after_secs >= 0 ){
	                	
					outcome += ", Retry-After: " + last_response_retry_after_secs + " seconds";
	            }

				throw( new ExternalSeedException( outcome, e ));
			}
		}catch( Throwable e ){
						
			if ( e instanceof ExternalSeedException ){
				
				throw((ExternalSeedException)e);
			}
			
			throw( new ExternalSeedException("Connection failed", e ));
			
		}finally{
			
			if ( socket != null ){
				
				try{
					socket.close();
					
				}catch( Throwable e ){
				}
			}
		}
	}
	
	public void
	deactivate()
	{	
	}
	
	public PasswordAuthentication
	getAuthentication(
		String		realm,
		URL			tracker )
	{
		return( null );
	}
	
	public void
	setAuthenticationOutcome(
		String		realm,
		URL			tracker,
		boolean		success )
	{
	}
	
	public void
	clearPasswords()
	{
	}
	
	public int
	getLastResponse()
	{
		return( last_response );
	}
	
	public int
	getLast503RetrySecs()
	{
		return( last_response_retry_after_secs );
	}
	
	public static void
	main(
		String[]		args )
	{
		try{
			String	url_str = "";
			
			ExternalSeedHTTPDownloader downloader = 
		
				new ExternalSeedHTTPDownloaderRange(
						new URL( url_str ),
						"Azureus" );
				
			downloader.downloadRange( 
				0, 1,
				new ExternalSeedHTTPDownloaderListener()
				{
					private int	position;
					
					public byte[]
		        	getBuffer()
		        	
		        		throws ExternalSeedException
		        	{
						return( new byte[1024] );
		        	}
		        	
		        	public void
		        	setBufferPosition(
		        		int	_position )
		        	{
		        		position = _position;
		        	}
		        	
		        	public int
		        	getBufferPosition()
		        	{
		        		return( position );
		        	}
		        	
		        	public int
		        	getBufferLength()
		        	{
		        		return( 1024 );
		        	}
		        	
		        	public int
		        	getPermittedBytes()
		        	
		        		throws ExternalSeedException
		        	{
		        		return( 1024 );
		        	}
		        	
		        	public int
		           	getPermittedTime()
		        	{
		        		return( Integer.MAX_VALUE );
		        	}
		        	
		        	public void
		        	reportBytesRead(
		        		int		num )
		        	{
		        		System.out.println( "read " + num );
		        	}
		        	
		        	public boolean 
		        	isCancelled() 
		        	{
		        		return false;
		        	}
		        	
		        	public void
		        	done()
		        	{
		        		System.out.println( "done" );
		        	}
				},
				true );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
