/*
 * Created on 13-Jul-2004
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

package org.gudy.azureus2.core3.security.impl;

/**
 * @author parg
 *
 */

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;

import org.bouncycastle.jce.*;
import org.bouncycastle.asn1.x509.X509Name;
import org.gudy.azureus2.core3.util.RandomUtils;
import org.gudy.azureus2.core3.util.SystemTime;

public class 
SESecurityManagerBC 
{
	protected static void
	initialise()
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	}
	
	public static Certificate
	createSelfSignedCertificate(
		SESecurityManagerImpl	manager,
		String					alias,
		String					cert_dn,
		int						strength )
	
		throws Exception
	{
		KeyPairGenerator	kg = KeyPairGenerator.getInstance( "RSA" );
		
		kg.initialize(strength, RandomUtils.SECURE_RANDOM );

		KeyPair pair = kg.generateKeyPair();
					
		X509V3CertificateGenerator certificateGenerator = 
			new X509V3CertificateGenerator();
		
		certificateGenerator.setSignatureAlgorithm( "MD5WithRSAEncryption" );
		
		certificateGenerator.setSerialNumber( new BigInteger( ""+SystemTime.getCurrentTime()));
					
		X509Name	issuer_dn = new X509Name(true,cert_dn);
		
		certificateGenerator.setIssuerDN(issuer_dn);
		
		X509Name	subject_dn = new X509Name(true,cert_dn);
		
		certificateGenerator.setSubjectDN(subject_dn);
		
		Calendar	not_after = Calendar.getInstance();
		
		not_after.add(Calendar.YEAR, 1);
		
		certificateGenerator.setNotAfter( not_after.getTime());
		
		certificateGenerator.setNotBefore(Calendar.getInstance().getTime());
		
		certificateGenerator.setPublicKey( pair.getPublic());
		
		X509Certificate certificate = certificateGenerator.generateX509Certificate(pair.getPrivate());
		
		java.security.cert.Certificate[] certChain = {(java.security.cert.Certificate) certificate };

		manager.addCertToKeyStore( alias, pair.getPrivate(), certChain );
		
		return( certificate );
	}
}
