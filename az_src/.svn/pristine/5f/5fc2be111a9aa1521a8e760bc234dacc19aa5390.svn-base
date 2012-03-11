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
    private static final String pub_exp = "10001";
    private static final String modulus	= "9a68296f49bf47b2a83ae4ba3cdb5a840a2689e5b36a6f2bfc27b916fc4dc9437f9087c4f0b5ae2fc5127a901b3c048753aa63d29cd7f9da7c81d475380de68236bd919230b0074aa6f40f29a78ac4a14e84fb8946cbcb5a840d1c2f77d83c795c289e37135843b8da008e082654a83b8bd3341b9f2ff6064e20b6c7ba89a707a1f3e1d8b2e0035dae539b04e49775eba23e5cbe89e22290da6c84ec3f450d07";
    
	public static void
	verifyData(
		File		file )
	
		throws AEVerifierException, Exception
	{
		KeyFactory key_factory = KeyFactory.getInstance("RSA");
		
		RSAPublicKeySpec 	public_key_spec = 
			new RSAPublicKeySpec( new BigInteger(modulus,16), new BigInteger(pub_exp,16));

		RSAPublicKey public_key 	= (RSAPublicKey)key_factory.generatePublic( public_key_spec );

		verifyData( file, public_key );
	}
	
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
