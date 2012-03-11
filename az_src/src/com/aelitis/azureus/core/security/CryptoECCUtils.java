/*
 * Created on Jul 12, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.security;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

public class 
CryptoECCUtils 
{
	private static final ECNamedCurveParameterSpec ECCparam = ECNamedCurveTable.getParameterSpec("prime192v2");

	public static KeyPair 
	createKeys()
	
		throws CryptoManagerException
	{
		try
		{
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
			
			keyGen.initialize(ECCparam);

			return keyGen.genKeyPair();
			
		}catch(Throwable e){
			
			throw( new CryptoManagerException( "Failed to create keys", e ));
		}
	}
	
	public static Signature 
	getSignature(
		Key key )
	
		throws CryptoManagerException
	{
		try
		{
			Signature ECCsig = Signature.getInstance("SHA1withECDSA", "BC");
			
			if( key instanceof ECPrivateKey ){
				
				ECCsig.initSign((ECPrivateKey)key);
				
			}else if( key instanceof ECPublicKey ){
				
				ECCsig.initVerify((ECPublicKey)key);

			}else{
				
				throw new CryptoManagerException("Invalid Key Type, ECC keys required");
			}
			
			return ECCsig;
			
		}catch( CryptoManagerException e ){
		
			throw( e );
			
		}catch( Throwable e ){
			
			throw( new CryptoManagerException( "Failed to create Signature", e ));
		}
	}

	public static byte[] 
   	keyToRawdata( 
   		PrivateKey privkey )
   	
   		throws CryptoManagerException
   	{
   		if(!(privkey instanceof ECPrivateKey)){
   			
   			throw( new CryptoManagerException( "Invalid private key" ));
   		}
   		
   		return ((ECPrivateKey)privkey).getD().toByteArray();
   	}

	public static PrivateKey 
   	rawdataToPrivkey(
   		byte[] input )
   	
   		throws CryptoManagerException
   	{
   		BigInteger D = new BigInteger(input);
   		
   		KeySpec keyspec = new ECPrivateKeySpec(D,(ECParameterSpec)ECCparam);
   		
   		PrivateKey privkey = null;
   		
   		try{
   			privkey = KeyFactory.getInstance("ECDSA","BC").generatePrivate(keyspec);
   			
   			return privkey;
   			
   		}catch( Throwable e ){
   	
   			throw( new CryptoManagerException( "Failed to decode private key" ));
   		}
   	}
   	
	public static byte[] 
   	keyToRawdata(
   		PublicKey pubkey )
   	
   		throws CryptoManagerException
   	{
   		if(!(pubkey instanceof ECPublicKey)){
   			
   			throw( new CryptoManagerException( "Invalid public key" ));
   		}
   		
   		return ((ECPublicKey)pubkey).getQ().getEncoded();
   	}
   	
   	
	public static  PublicKey 
   	rawdataToPubkey(
   		byte[] input )
   	
   		throws CryptoManagerException
   	{
   		ECPoint W = ECCparam.getCurve().decodePoint(input);
   		
   		KeySpec keyspec = new ECPublicKeySpec(W,(ECParameterSpec)ECCparam);

   		try{
   			
   			return KeyFactory.getInstance("ECDSA", "BC").generatePublic(keyspec);
   			
   		}catch (Throwable e){
   		
   			throw( new CryptoManagerException( "Failed to decode public key", e ));
   		}
   	}	
}
