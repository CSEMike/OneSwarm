/*
 * Created on 03-Nov-2005
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
 * AELITIS, SAS au capital de 40,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.clientmessageservice.secure.impl;

import java.security.interfaces.RSAPublicKey;

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.clientmessageservice.ClientMessageService;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClient;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientAdapter;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientListener;
import com.aelitis.azureus.core.clientmessageservice.secure.SecureMessageServiceClientMessage;

public class 
SecureMessageServiceClientImpl
	implements SecureMessageServiceClient
{
		// these also occur in the server
	
	public static final int STATUS_OK					= 0;
	public static final int STATUS_LOGON_FAIL			= 1;
	public static final int STATUS_INVALID_SEQUENCE		= 2;	
	public static final int STATUS_FAILED				= 3;
	public static final int STATUS_ABORT				= 4;

	public static final String	SERVICE_NAME	= "SecureMsgServ";
	
	private static final long	MIN_RETRY_PERIOD		=    5*60*1000;
	private static final long	MAX_RETRY_PERIOD		= 2*60*60*1000;

	private String									host;
	private int										port;
	private int										timeout_secs;
	private RSAPublicKey							public_key;
	private SecureMessageServiceClientAdapter		adapter;

	private long				retry_millis			= MIN_RETRY_PERIOD;
	private int					connect_failure_count	= 0;
	
	
	private AEMonitor			message_mon;
	private AESemaphore			message_sem;
		
	private String				last_failed_user_pw			= "";
	private long				last_failed_user_pw_time;
	
	private List				messages 	= new ArrayList();
	private List				listeners	= new ArrayList();
	
	public
	SecureMessageServiceClientImpl(
		String								_host,
		int									_port,
		int									_timeout_secs,
		RSAPublicKey						_key,
		SecureMessageServiceClientAdapter	_adapter )	
	{
		host			= _host;
		port			= _port;
		timeout_secs	= _timeout_secs;
		public_key		= _key;
		adapter			= _adapter;
				
		message_mon	= new AEMonitor( "SecureService:messages" );
		
		message_sem = new AESemaphore( "SecureService:messages" );
		
		new AEThread( "SecureService::messageSender", true )
		{
			public void
			runSupport()
			{
				while( true ){
					
					long	time = retry_millis;
					
					if ( connect_failure_count > 0 ){
						
						for (int i=0;i<connect_failure_count;i++){
							
							time = time + time;
							
							if ( time > MAX_RETRY_PERIOD ){
								
								time = MAX_RETRY_PERIOD;
								
								break;
							}
						}
					}
					
					message_sem.reserve( time );
					
					try{
						sendMessagesSupport();
						
					}catch( Throwable e ){
						
						adapter.log( "Request processing failed", e);
					}
				}
			}
		}.start();
	}
	
	public void
	sendMessages()
	{
		message_sem.release();
	}
	
	protected void
	sendMessagesSupport()
	{	
		String	user 		= adapter.getUser();
		byte[]	password	= adapter.getPassword();
		
		String	user_password = user + "/" + new String( password );
		
			// user name must be defined, however we allow a blank password
		
		if ( user.length() == 0 ){
			
			adapter.authenticationFailed();
			
			return;
		}
		
			// if user-name + password hasn't changed recently and logon failed then
			// don't re-attempt
		
		if ( user_password.equals( last_failed_user_pw )){
			
			final long now =SystemTime.getCurrentTime();

            if (now >last_failed_user_pw_time &&now -last_failed_user_pw_time <60 *1000){

				adapter.authenticationFailed();

				return;
			}
		}
		
		List	outstanding_messages;
		
		try{
			message_mon.enter();

			outstanding_messages	= new ArrayList( messages );
			
		}finally{
			
			message_mon.exit();
		}
		
		if ( outstanding_messages.size() == 0 ){
			
			return;
		}
		
		List	complete_messages	= new ArrayList();
		
		boolean	failed = false;
		
		try{
			Iterator	it = outstanding_messages.iterator();
			
			while( it.hasNext() && !failed ){
				
				SecureMessageServiceClientMessageImpl	message = (SecureMessageServiceClientMessageImpl)it.next();
					
				boolean	retry 			= true;
				int		retry_count		= 0;
				
				while( retry && !failed ){
					
					retry	= false;
					
					ClientMessageService	message_service = null;

					boolean	got_reply = false;
					
					try{
						Map	content	= new HashMap();				
			
						long	sequence = adapter.getMessageSequence();
						
						content.put( "user", 		user );
						content.put( "password", 	password );
						content.put( "seq", 		new Long( sequence ));
						content.put( "request", 	message.getRequest());
							
						last_failed_user_pw = "";
						
						message_service = SecureMessageServiceClientHelper.getServerService( host, port, timeout_secs, SERVICE_NAME, public_key );					

						message_service.sendMessage( content );
						
						Map	reply = message_service.receiveMessage();
						
						got_reply	= true;
							
						long	status = ((Long)reply.get( "status" )).longValue();
						
						Long	new_retry = (Long)reply.get( "retry" );
						
						if ( new_retry != null ){
							
							retry_millis = new_retry.longValue();
							
							if ( retry_millis < MIN_RETRY_PERIOD ){
								
								retry_millis = MIN_RETRY_PERIOD;
							}
							
							adapter.log( "Server requested retry period of " + (retry_millis/1000) + " seconds" );
							
						}else{
							
							retry_millis = MIN_RETRY_PERIOD;
						}
						
						if ( status == STATUS_OK ){
		
							message.setReply( (Map)reply.get( "reply" ));

							adapter.log( "Request successfully sent: " + message.getRequest() + "->" + message.getReply());							
							
							adapter.setMessageSequence( sequence + 1 );
							
							adapter.serverOK();
	
							for (Iterator l_it=listeners.iterator();l_it.hasNext();){
								
								try{
									((SecureMessageServiceClientListener)l_it.next()).complete( message );
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
							
							complete_messages.add( message );
							
						}else if ( status == STATUS_LOGON_FAIL ){
							
							last_failed_user_pw 		= user_password;
							last_failed_user_pw_time	= SystemTime.getCurrentTime();
							
							adapter.serverOK();
							
							adapter.authenticationFailed();
							
							failed	= true;
						
						}else if ( status == STATUS_INVALID_SEQUENCE ){
							
							if ( retry_count == 1 ){
																
								adapter.serverFailed( new Exception( "Sequence resynchronisation failed" ));
								
								failed = true;
								
							}else{
							
								retry_count++;
								
								retry	= true;
								
								long	expected_sequence = ((Long)reply.get( "seq" )).longValue();
								
								adapter.log( "Sequence resynchronise: local = " + sequence + ", remote = " + expected_sequence );
								
								adapter.setMessageSequence( expected_sequence );
							}

						}else if ( status == STATUS_FAILED ){

							adapter.serverFailed( new Exception( new String( (byte[])reply.get( "error" ))));
							
							failed = true;
							
						}else{//  if ( status == STATUS_ABORT ){
						
								// this is when things have gone badly wrong server-side - we just
								// dump the message
							
							adapter.serverFailed( new Exception( "Server requested abort" ));

							for (Iterator l_it=listeners.iterator();l_it.hasNext();){
								
								try{
									((SecureMessageServiceClientListener)l_it.next()).aborted( 
											message,
											new String( (byte[])reply.get( "error" )));
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
							
							complete_messages.add( message );							
						}
						
					}catch( Throwable e ){
									
						adapter.serverFailed( e );
						
						failed	= true;
						
					}finally{
						
						if ( got_reply ){
							
							connect_failure_count = 0;
							
						}else{
							
							connect_failure_count++;
							
							if ( connect_failure_count > 1 ){
								
								try{
									adapter.log( "Failed to contact server " + connect_failure_count + " times in a row" );
									
								}catch( Throwable e ){
									
									e.printStackTrace();
								}
							}
						}
						
						if ( message_service != null ){
							
							message_service.close();
						}
					}
				}
			}
		}catch( Throwable e ){
			
			adapter.serverFailed( e );
			
		}finally{
			
			try{
				message_mon.enter();
					
				messages.removeAll( complete_messages );
				
			}finally{
				
				message_mon.exit();
			}
		}
	}
	
	public SecureMessageServiceClientMessage
	sendMessage(
		Map			request,
		Object		data,
		String		description )
	{
		try{
			message_mon.enter();
			
			SecureMessageServiceClientMessage	res =  new SecureMessageServiceClientMessageImpl( this, request, data, description );
			
			messages.add( res );
			
			message_sem.release();
			
			return( res );
			
		}finally{
			
			message_mon.exit();
		}
	}
	
	protected void
	cancel(
		SecureMessageServiceClientMessage	message )
	{
		boolean	inform	= false;
		
		try{
			message_mon.enter();
			
			if ( messages.remove( message )){
				
				inform	= true;
			}
		}finally{
			
			message_mon.exit();
		}
		
		if ( inform ){
			
			for (Iterator it=listeners.iterator();it.hasNext();){
				
				try{
					((SecureMessageServiceClientListener)it.next()).cancelled( message );
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	public SecureMessageServiceClientMessage[]
	getMessages()
	{
		try{
			message_mon.enter();
			
			return((SecureMessageServiceClientMessage[])messages.toArray( new SecureMessageServiceClientMessage[ messages.size()]));
			
		}finally{
			
			message_mon.exit();
		}	
	}
	
	public void
	addListener(
		SecureMessageServiceClientListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		SecureMessageServiceClientListener	l )
	{
		listeners.remove( l );
	}
}
