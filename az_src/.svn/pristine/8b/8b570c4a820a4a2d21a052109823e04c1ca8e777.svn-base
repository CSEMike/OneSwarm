/*
 * Created on 18-Jan-2006
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

package com.aelitis.azureus.core.networkmanager.impl;

import java.io.IOException;
import java.nio.ByteBuffer;


import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.AddressUtils;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.networkmanager.NetworkManager;

public class 
ProtocolDecoderInitial 
	extends ProtocolDecoder
{	
	private static final LogIDs LOGID = LogIDs.NWMAN;

	private ProtocolDecoderAdapter	adapter;
	
	private TransportHelperFilter	filter;
	
	private TransportHelper	transport;

	private byte[][]	shared_secrets;
	private ByteBuffer	initial_data;
	private ByteBuffer	decode_buffer; 
	private int			decode_read;
	
	private long	start_time	= SystemTime.getCurrentTime();
	
	private ProtocolDecoderPHE	phe_decoder;
	
	private long	last_read_time		= 0;
	
	private boolean processing_complete;
	
	public
	ProtocolDecoderInitial(
		TransportHelper				_transport,
		byte[][]					_shared_secrets,
		boolean						_outgoing,
		ByteBuffer					_initial_data,
		ProtocolDecoderAdapter		_adapter )
	
		throws IOException
	{
		super( true );
		
		transport		= _transport;
		shared_secrets	= _shared_secrets;
		initial_data	= _initial_data;
		adapter			= _adapter;
		
		final TransportHelperFilterTransparent transparent_filter = new TransportHelperFilterTransparent( transport, false );
				
		filter	= transparent_filter;
		
		if ( _outgoing ){  //we assume that for outgoing connections, if we are here, we want to use crypto

			if ( ProtocolDecoderPHE.isCryptoOK()){
					
				decodePHE( null );
					
			}else{
				
				throw( new IOException( "Crypto required but unavailable" ));
			}
		
		}else{
			
			decode_buffer = ByteBuffer.allocate( adapter.getMaximumPlainHeaderLength());
			
			transport.registerForReadSelects(
				new TransportHelper.selectListener()
				{
				   	public boolean 
			    	selectSuccess(
			    		TransportHelper	helper, 
			    		Object 			attachment )
				   	{
						try{
							int	len = helper.read( decode_buffer );
							
							if ( len < 0 ){
								
								failed( new IOException( "end of stream on socket read: in=" + decode_buffer.position()));
								
							}else if ( len == 0 ){
								
								return( false );
							}
							
							last_read_time = SystemTime.getCurrentTime();
							
							decode_read += len;
							
							int	match =  adapter.matchPlainHeader( decode_buffer );
							
							if ( match != ProtocolDecoderAdapter.MATCH_NONE ){
								
								helper.cancelReadSelects();
																		
								if ( NetworkManager.REQUIRE_CRYPTO_HANDSHAKE && match == ProtocolDecoderAdapter.MATCH_CRYPTO_NO_AUTO_FALLBACK ){
								
									if ( NetworkManager.INCOMING_HANDSHAKE_FALLBACK_ALLOWED ){
										if (Logger.isEnabled())
											Logger.log(new LogEvent(LOGID, "Incoming connection ["+ transport.getAddress() + "] is not encrypted but has been accepted as fallback is enabled" ));
									}
									else if( AddressUtils.isLANLocalAddress( transport.getAddress().getAddress().getHostAddress() ) == AddressUtils.LAN_LOCAL_YES ) {
										if (Logger.isEnabled())
											Logger.log(new LogEvent(LOGID, "Incoming connection ["+ transport.getAddress() + "] is not encrypted but has been accepted as lan-local" ));
									}
									else{										
										throw( new IOException( "Crypto required but incoming connection has none" ));
									}
								}
								
								decode_buffer.flip();
								
								transparent_filter.insertRead( decode_buffer );
								
								complete( initial_data );
								
							}else{
								
								if ( !decode_buffer.hasRemaining()){
									
									helper.cancelReadSelects();

									if ( NetworkManager.INCOMING_CRYPTO_ALLOWED ){
									
										decode_buffer.flip();
									
										decodePHE( decode_buffer );
										
									}else{
										
										if (Logger.isEnabled())
											Logger.log(new LogEvent(LOGID, "Incoming connection ["+ transport.getAddress() + "] encrypted but rejected as not permitted" ));

										throw( new IOException( "Incoming crypto connection not permitted" ));
									}
								}
							}
							
							return( true );
							
						}catch( Throwable e ){
							
							selectFailure( helper, attachment, e );
							
							return( false );
						}
				   	}

			        public void 
			        selectFailure(
			        	TransportHelper	helper,
			        	Object 			attachment, 
			        	Throwable 		msg)
			        {
						helper.cancelReadSelects();
						
						failed( msg );
			        }
				},
				this );
		}
	}
	
	protected void
	decodePHE(
		ByteBuffer	buffer )	
	
		throws IOException
	{
		ProtocolDecoderAdapter	phe_adapter = 
			new ProtocolDecoderAdapter()
			{
				public void
				decodeComplete(
					ProtocolDecoder	decoder,
					ByteBuffer		remaining_initial_data )	
				{
					filter = decoder.getFilter();
					
					complete( remaining_initial_data );
				}
				
				public void
				decodeFailed(
					ProtocolDecoder	decoder,
					Throwable			cause )
				{
					failed( cause );
				}
				
				public void
				gotSecret(
					byte[]				session_secret )
				{
					adapter.gotSecret( session_secret );
				}
				
				public int
				getMaximumPlainHeaderLength()
				{
					throw( new RuntimeException());
				}
				
				public int
				matchPlainHeader(
					ByteBuffer			buffer )
				{
					throw( new RuntimeException());
				}
			};
		
		phe_decoder = new ProtocolDecoderPHE( transport, shared_secrets, buffer, initial_data, phe_adapter );
	}
	
	public boolean
	isComplete(
		long	now )
	{
		if ( !processing_complete ){
		
			if ( start_time > now ){
				
				start_time	= now;
			}
			
			if ( last_read_time > now ){
				
				last_read_time	= now;
			}
			
			if ( phe_decoder != null ){
				
				last_read_time	= phe_decoder.getLastReadTime();
			}
				
			long	timeout;
			long	time;
			
			if ( last_read_time == 0 ){
				
				timeout = transport.getConnectTimeout();
				time	= start_time;
				
			}else{
				
				timeout = transport.getReadTimeout();
				time	= last_read_time;
			}
			
			if ( now - time > timeout ){
				
				try{
					transport.cancelReadSelects();
					
					transport.cancelWriteSelects();
											
				}catch( Throwable e ){
					
				}
				
				String	phe_str = "";
				
				if ( phe_decoder != null ){
					
					phe_str = ", crypto: " + phe_decoder.getString();
				}
				
		       	if ( Logger.isEnabled()){
		       		
					Logger.log(new LogEvent(LOGID, "Connection ["
							+ transport.getAddress() + "] forcibly timed out after "
							+ timeout/1000 + "sec due to socket inactivity"));
		       	}
		       	
				failed( new Throwable( "Protocol decode aborted: timed out after " + timeout/1000+ "sec: " + decode_read + " bytes read" + phe_str ));
			}
		}
		
		return( processing_complete );
	}
	
	public TransportHelperFilter
	getFilter()
	{
		return( filter );
	}
	
	protected void
	complete(
		ByteBuffer	remaining_initial_data )
	{
		if ( !processing_complete ){
			
			processing_complete	= true;
			
			adapter.decodeComplete( this, remaining_initial_data );
		}
	}
	
	protected void
	failed(
		Throwable	reason )
	{
		if ( !processing_complete ){
			
			processing_complete	= true;
			
			adapter.decodeFailed( this, reason );
		}
	}
}
