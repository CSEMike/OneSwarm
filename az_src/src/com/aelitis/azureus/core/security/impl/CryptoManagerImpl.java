/*
 * Created on 15 Jun 2006
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

package com.aelitis.azureus.core.security.impl;

import java.util.*;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordException;
import com.aelitis.azureus.core.security.CryptoManagerPasswordHandler;

public class 
CryptoManagerImpl 
	implements CryptoManager
{
	private static final int 	PBE_ITERATIONS	= 100;
	private static final String	PBE_ALG			= "PBEWithMD5AndDES";
	
	private static CryptoManagerImpl		singleton;
	
	
	public static synchronized CryptoManager
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton = new CryptoManagerImpl();
		}
		
		return( singleton );
	}
	
	private byte[]				secure_id;
	private CryptoHandler		ecc_handler;
	private List				listeners	= Collections.synchronizedList( new ArrayList());
	
	protected
	CryptoManagerImpl()
	{
		SESecurityManager.initialise();
		
		ecc_handler = new CryptoHandlerECC( this, 1 );
	}
	
	public byte[]
	getSecureID()
	{
		String key = CryptoManager.CRYPTO_CONFIG_PREFIX + "id";
		
		if ( secure_id == null ){
			
			secure_id = COConfigurationManager.getByteParameter( key, null );
		}
		
		if ( secure_id == null ){
			
			secure_id = new byte[20];
		
			new SecureRandom().nextBytes( secure_id );
			
			COConfigurationManager.setParameter( key, secure_id );
			
			COConfigurationManager.save();
		}
		
		return( secure_id );
	}
	
	public CryptoHandler
	getECCHandler()
	{
		return( ecc_handler );
	}
	
	protected byte[]
	encryptWithPBE(
		byte[]		data,
		char[]		password )
	
		throws CryptoManagerException
	{
		try{
			byte[]	salt = new byte[8];
			
			new SecureRandom().nextBytes( salt );
			
			PBEKeySpec keySpec = new PBEKeySpec(password);
		
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( PBE_ALG );
		
			SecretKey key = keyFactory.generateSecret(keySpec);
		
			PBEParameterSpec paramSpec = new PBEParameterSpec( salt, PBE_ITERATIONS );
		
			Cipher cipher = Cipher.getInstance( PBE_ALG );
			
			cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
	
			byte[]	enc = cipher.doFinal( data );
			
			byte[]	res = new byte[salt.length + enc.length];
			
			System.arraycopy( salt, 0, res, 0, salt.length );
			
			System.arraycopy( enc, 0, res, salt.length, enc.length );
			
			return( res );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "PBE encryption failed", e ));
		}
	}
	
	protected byte[]
   	decryptWithPBE(
   		byte[]		data,
   		char[]		password )
	
		throws CryptoManagerException
   	{
		boolean fail_is_pw_error = false;
		
		try{
			byte[]	salt = new byte[8];
			
			System.arraycopy( data, 0, salt, 0, 8 );
			
			PBEKeySpec keySpec = new PBEKeySpec(password);
	
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( PBE_ALG );
	
			SecretKey key = keyFactory.generateSecret(keySpec);
	
			PBEParameterSpec paramSpec = new PBEParameterSpec(salt, PBE_ITERATIONS);
	
			Cipher cipher = Cipher.getInstance( PBE_ALG );
			
			cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
	
			fail_is_pw_error = true;
			
			return( cipher.doFinal( data, 8, data.length-8 ));
			
		}catch( Throwable e ){
			
			if ( fail_is_pw_error ){
				
				throw( new CryptoManagerPasswordException( e ));
				
			}else{
				throw( new CryptoManagerException( "PBE decryption failed", e ));
			}
		}
   	}
	
	protected char[]
	getPassword(
		int		handler,
		int		action,
		String	reason )
	
		throws CryptoManagerException
	{
		System.out.println( "getPassword:" + handler + "/" + action + "/" + reason );
		
		if ( listeners.size() == 0 ){
			
			throw( new CryptoManagerException( "No password handlers registered" ));
		}
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				char[]	pw = ((CryptoManagerPasswordHandler)listeners.get(i)).getPassword( handler, action, reason );
				
				if ( pw != null ){
					
					return( pw );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		throw( new CryptoManagerException( "No password handlers returned a password" ));
	}
	

	public void
	addPasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		listeners.add( handler );
	}
	
	public void
	removePasswordHandler(
		CryptoManagerPasswordHandler		handler )
	{
		listeners.remove( handler );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{

			String	stuff = "12345";
			
			CryptoManagerImpl man = (CryptoManagerImpl)getSingleton();
			
			man.addPasswordHandler(
				new CryptoManagerPasswordHandler()
				{
					public char[] 
					getPassword(
							int handler_type, 
							int action_type, 
							String reason )
					{
						return( "trout".toCharArray());
					}
				});
			
			CryptoHandler	handler1 = man.getECCHandler();
			
			CryptoHandler	handler2 = new CryptoHandlerECC( man, 2 );
			

			//handler.resetKeys( "monkey".toCharArray() );
			
			byte[]	sig = handler1.sign( stuff.getBytes(), "Test signing" );
			
			System.out.println( handler1.verify( handler1.getPublicKey(  "Test verify" ), stuff.getBytes(), sig ));
			
			byte[]	enc = handler1.encrypt( handler2.getPublicKey( "" ), stuff.getBytes(), "" );
			
			System.out.println( "pk1 = " + ByteFormatter.encodeString( handler1.getPublicKey("")));
			System.out.println( "pk2 = " + ByteFormatter.encodeString( handler2.getPublicKey("")));
			
			System.out.println( "dec: " + new String( handler2.decrypt(handler1.getPublicKey( "" ), enc, "" )));
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
