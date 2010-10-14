/*
 * Created on 13 Jun 2006
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

package org.gudy.azureus2.core3.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class 
AEVerifier 
{
	
		//*********************************************************
	/*
	 * Edit, by isdal
	 * added support for the oneswarm signature as well
	 */
	private final static String modulus		= "a4ef7d3e8e6ed74c0442050e18b881de6dfa8d079643461d39bcf092cf716ae28d2ef938420931a3833261e9976199c4829d88d380e51e2896c18099814c4ed662b64457a7a1f5776fd8c5262b367bec0029c1c3939fdad83fe564dee6a0ed2c4b4d3f5b212e3200b9fa190d95d1826e5cdd8f27e0799e726bbef34da32f60221f9a2b0817a308637a258915d9725d469963cfd2e06c8929c0dc1b758812d6bcb3cb51f6b0cf33c09d4c549706804f01f44c66b3cad62f739012069b15fcf8087acb5b3a88f0b8582fec08adc290f1bb2fd8bcfb5e821a353b7c232f1fb185811378ed9986af143e34cf398741d6d246360abc30bd9e437dcbfc546e155caa43";

	private static final String pub_exp		= "10001";

	private static final String az_modulus = "9a68296f49bf47b2a83ae4ba3cdb5a840a2689e5b36a6f2bfc27b916fc4dc9437f9087c4f0b5ae2fc5127a901b3c048753aa63d29cd7f9da7c81d475380de68236bd919230b0074aa6f40f29a78ac4a14e84fb8946cbcb5a840d1c2f77d83c795c289e37135843b8da008e082654a83b8bd3341b9f2ff6064e20b6c7ba89a707a1f3e1d8b2e0035dae539b04e49775eba23e5cbe89e22290da6c84ec3f450d07";

	public static void verifyData(File file)

	throws AEVerifierException, Exception {
		KeyFactory key_factory = KeyFactory.getInstance("RSA");

		RSAPublicKeySpec public_key_spec = new RSAPublicKeySpec(new BigInteger(
				modulus, 16), new BigInteger(pub_exp, 16));

		RSAPublicKey public_key = (RSAPublicKey) key_factory.generatePublic(public_key_spec);

		try {
			verifyData(file, public_key);
		} catch (AEVerifierException e) {
			System.out.println("signature check failed for oneswarm key, trying az key");

			public_key_spec = new RSAPublicKeySpec(new BigInteger(az_modulus, 16),
					new BigInteger(pub_exp, 16));

			public_key = (RSAPublicKey) key_factory.generatePublic(public_key_spec);
			verifyData(file, public_key);
		}
	}

	//**********************************************************
	
	protected static void
	verifyData(
		File			file,
		RSAPublicKey	key )
	
		throws AEVerifierException, Exception
	{
		ZipInputStream	zis = null;
		
		try{
			zis = new ZipInputStream( 
					new BufferedInputStream( new FileInputStream( file ) ));
				
			byte[]		signature	= null;
			
			Signature	sig = Signature.getInstance("MD5withRSA" );

			sig.initVerify( key );
			
			while( true ){
				
				ZipEntry	entry = zis.getNextEntry();
					
				if ( entry == null ){
					
					break;
				}
			
				if ( entry.isDirectory()){
					
					continue;
				}
				
				String	name = entry.getName();
			
				ByteArrayOutputStream	output = null;
				
				if ( name.equalsIgnoreCase("azureus.sig")){
					
					output	= new ByteArrayOutputStream();
				}
												
				byte[]	buffer = new byte[65536];
				
				while( true ){
				
					int	len = zis.read( buffer );
					
					if ( len <= 0 ){
						
						break;
					}
					
					if ( output == null ){
						
						sig.update( buffer, 0, len );
						
					}else{
						
						output.write( buffer, 0, len );
					}
				}
				
				if ( output != null ){
					
					signature = output.toByteArray();
				}
			}
						
			if ( signature == null ){
								
				throw( new AEVerifierException( AEVerifierException.FT_SIGNATURE_MISSING, "Signature missing from file" ));
			}
			
			if ( !sig.verify( signature )){
				
				throw( new AEVerifierException( AEVerifierException.FT_SIGNATURE_BAD, "Signature doesn't match data" ));
			}
		}finally{
			
			if ( zis != null ){
				
				zis.close();
			}
		}
	}
	
	public static void
	verifyData(
		String			data,
		byte[]			signature )
	
		throws AEVerifierException, Exception
	{
		KeyFactory key_factory = KeyFactory.getInstance("RSA");
		
		RSAPublicKeySpec 	public_key_spec = 
			new RSAPublicKeySpec( new BigInteger(modulus,16), new BigInteger(pub_exp,16));

		RSAPublicKey public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );
		
		Signature	sig = Signature.getInstance("MD5withRSA" );

		sig.initVerify( public_key );
		
		sig.update( data.getBytes( "UTF-8" ));
			
		if ( !sig.verify( signature )){
			
			throw( new AEVerifierException( AEVerifierException.FT_SIGNATURE_BAD, "Data verification failed, signature doesn't match data" ));
		}
	}
}
