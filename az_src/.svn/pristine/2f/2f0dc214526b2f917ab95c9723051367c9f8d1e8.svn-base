/*
 * Created on 08-Nov-2005
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

import java.io.IOException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jce.provider.RSAUtil;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.plugins.utils.StaticUtilities;

import com.aelitis.azureus.core.clientmessageservice.ClientMessageService;
import com.aelitis.azureus.core.clientmessageservice.ClientMessageServiceClient;


public class 
SecureMessageServiceClientHelper
	implements ClientMessageService
{
	public static ClientMessageService 
	getServerService( 
		String 			server_address, 
		int 			server_port, 
		int				timeout_secs,
		String 			msg_type_id,
		RSAPublicKey	public_key ) 
	
		throws IOException
	{
		return new SecureMessageServiceClientHelper( server_address, server_port, timeout_secs, msg_type_id, public_key );
	}
	
	private ClientMessageService	delegate;
	private SecretKey 				session_key;
	private byte[]					encryped_session_key;
	
	protected
	SecureMessageServiceClientHelper(
		String 			server_address, 
		int 			server_port,
		int				timeout_secs,
		String 			msg_type_id,
		RSAPublicKey	public_key ) 
	
		throws IOException
	{
		try{
			KeyGenerator secret_key_gen = KeyGenerator.getInstance("DESede");
		
			session_key = secret_key_gen.generateKey();
				
			byte[] secret_bytes = session_key.getEncoded();
			
			try{
				Cipher	rsa_cipher = Cipher.getInstance( "RSA" );
		    
				rsa_cipher.init( Cipher.ENCRYPT_MODE, public_key );
		    
				encryped_session_key = rsa_cipher.doFinal( secret_bytes );
				
			}catch( Throwable e ){
				
					// fallback to the BC implementation for jdk1.4.2 as JCE RSA not available
				
				RSAEngine	eng = new RSAEngine();
				
				PKCS1Encoding	padded_eng = new PKCS1Encoding( eng );
				
	            CipherParameters param = RSAUtil.generatePublicKeyParameter(public_key);
	            
	            param = new ParametersWithRandom(param, RandomUtils.SECURE_RANDOM);
	            
	            padded_eng.init( true, param );
				
				encryped_session_key = padded_eng.processBlock(secret_bytes, 0, secret_bytes.length);
			}

		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new IOException( "Secure client message service initialisation fails - " + Debug.getNestedExceptionMessage(e)));
		}
		
		delegate = ClientMessageServiceClient.getServerService( server_address, server_port, msg_type_id );
	}
	
	public void 
	sendMessage( 
		Map plain_payload ) 
	
		throws IOException
	{
		Map	secure_payload = new HashMap();
		
		try{
		    byte[]	message_bytes = StaticUtilities.getFormatters().bEncode( plain_payload );
		    
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			
			cipher.init(Cipher.ENCRYPT_MODE, session_key ); 
	
			byte[]	encrypted_message = cipher.doFinal( message_bytes );
	
			secure_payload.put( "ver", "1" );
			secure_payload.put( "alg", "DESede" );
			secure_payload.put( "key", encryped_session_key );
			secure_payload.put( "content", encrypted_message );
			
		}catch( Throwable e ){
			
			throw( new IOException( "send message failed - " + Debug.getNestedExceptionMessage(e)));
		}
		
		delegate.sendMessage( secure_payload );
	}
	
	public Map 
	receiveMessage() 
	
		throws IOException
	{
		Map	secure_payload = delegate.receiveMessage();
		
		byte[]	encrypted_message	= (byte[])secure_payload.get( "content" );
		
		try{
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
			
			cipher.init(Cipher.DECRYPT_MODE, session_key ); 
	
			byte[]	message_bytes = cipher.doFinal( encrypted_message );
	
			Map plain_payload = StaticUtilities.getFormatters().bDecode( message_bytes );

			return( plain_payload );
			
		}catch( Throwable e ){
			
			throw( new IOException( "send message failed - " + Debug.getNestedExceptionMessage(e)));
		}	
	}
	
	public void 
	close()
	{
		delegate.close();
	}
	
	public void
	setMaximumMessageSize( int max_bytes )
	{
		delegate.setMaximumMessageSize( max_bytes );
	}
}
