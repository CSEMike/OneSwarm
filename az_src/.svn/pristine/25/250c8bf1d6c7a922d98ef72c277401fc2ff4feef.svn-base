/*
 * Created on 08-Jun-2004
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

package org.bouncycastle.jce.provider;

/**
 * @author parg
 *
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactorySpi;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.interfaces.ElGamalPrivateKey;
import org.bouncycastle.jce.interfaces.ElGamalPublicKey;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

public abstract class 
JDKKeyFactory 
	extends KeyFactorySpi
{
    protected KeySpec engineGetKeySpec(
            Key    key,
            Class    spec)
        throws InvalidKeySpecException
        {
           if (spec.isAssignableFrom(PKCS8EncodedKeySpec.class) && key.getFormat().equals("PKCS#8"))
           {
                   return new PKCS8EncodedKeySpec(key.getEncoded());
           }
           else if (spec.isAssignableFrom(X509EncodedKeySpec.class) && key.getFormat().equals("X.509"))
           {
                   return new X509EncodedKeySpec(key.getEncoded());
           }
           else if (spec.isAssignableFrom(RSAPublicKeySpec.class) && key instanceof RSAPublicKey)
           {
                RSAPublicKey    k = (RSAPublicKey)key;

                return new RSAPublicKeySpec(k.getModulus(), k.getPublicExponent());
           }
           else if (spec.isAssignableFrom(RSAPrivateKeySpec.class) && key instanceof RSAPrivateKey)
           {
                RSAPrivateKey    k = (RSAPrivateKey)key;

                return new RSAPrivateKeySpec(k.getModulus(), k.getPrivateExponent());
           }
           else if (spec.isAssignableFrom(RSAPrivateCrtKeySpec.class) && key instanceof RSAPrivateCrtKey)
           {
                RSAPrivateCrtKey    k = (RSAPrivateCrtKey)key;

                return new RSAPrivateCrtKeySpec(
                                k.getModulus(), k.getPublicExponent(),
                                k.getPrivateExponent(),
                                k.getPrimeP(), k.getPrimeQ(),
                                k.getPrimeExponentP(), k.getPrimeExponentQ(),
                                k.getCrtCoefficient());
           }


            throw new RuntimeException("not implemented yet " + key + " " + spec);
        }

        protected Key engineTranslateKey(
            Key    key)
            throws InvalidKeyException
        {
            if (key instanceof RSAPublicKey)
            {
                return new JCERSAPublicKey((RSAPublicKey)key);
            }
            else if (key instanceof RSAPrivateCrtKey)
            {
                //return new JCERSAPrivateCrtKey((RSAPrivateCrtKey)key);
            }
            else if (key instanceof RSAPrivateKey)
            {
                //return new JCERSAPrivateKey((RSAPrivateKey)key);
            }
            else if (key instanceof DHPublicKey)
            {
                //return new JCEDHPublicKey((DHPublicKey)key);
            }
            else if (key instanceof DHPrivateKey)
            {
                //return new JCEDHPrivateKey((DHPrivateKey)key);
            }
            else if (key instanceof DSAPublicKey)
            {
                //return new JDKDSAPublicKey((DSAPublicKey)key);
            }
            else if (key instanceof DSAPrivateKey)
            {
                //return new JDKDSAPrivateKey((DSAPrivateKey)key);
            }
            else if (key instanceof ElGamalPublicKey)
            {
                //return new JCEElGamalPublicKey((ElGamalPublicKey)key);
            }
            else if (key instanceof ElGamalPrivateKey)
            {
                //return new JCEElGamalPrivateKey((ElGamalPrivateKey)key);
            }

            throw new InvalidKeyException("key type unknown");
        }
	
	
	   static PublicKey createPublicKeyFromDERStream(
		        InputStream         in)
		        throws IOException
		    {
		        return createPublicKeyFromPublicKeyInfo(
		                new SubjectPublicKeyInfo((ASN1Sequence)(new DERInputStream(in).readObject())));
		    }
	   
    static PublicKey createPublicKeyFromPublicKeyInfo(
        SubjectPublicKeyInfo         info)
    {
        AlgorithmIdentifier     algId = info.getAlgorithmId();
        
        if (algId.getObjectId().equals(PKCSObjectIdentifiers.rsaEncryption)
        	|| algId.getObjectId().equals(X509ObjectIdentifiers.id_ea_rsa))
        {
              return new JCERSAPublicKey(info);
        }
        else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_ecPublicKey))
        {
              return new JCEECPublicKey(info);
        }
        else
        {
            throw new RuntimeException("algorithm identifier in key not recognised");
        }
    }
    
    static PrivateKey createPrivateKeyFromDERStream(
            InputStream         in)
            throws IOException
        {
            return createPrivateKeyFromPrivateKeyInfo(
                    new PrivateKeyInfo((ASN1Sequence)(new DERInputStream(in).readObject())));
        }

        /**
         * create a private key from the given public key info object.
         */ 
        static PrivateKey createPrivateKeyFromPrivateKeyInfo(
            PrivateKeyInfo      info)
        {
            AlgorithmIdentifier     algId = info.getAlgorithmId();
            
            /*
            if (algId.getObjectId().equals(PKCSObjectIdentifiers.rsaEncryption))
            {
                  return new JCERSAPrivateCrtKey(info);
            }
            else if (algId.getObjectId().equals(PKCSObjectIdentifiers.dhKeyAgreement))
            {
                  return new JCEDHPrivateKey(info);
            }
            else if (algId.getObjectId().equals(OIWObjectIdentifiers.elGamalAlgorithm))
            {
                  return new JCEElGamalPrivateKey(info);
            }
            else if (algId.getObjectId().equals(X9ObjectIdentifiers.id_dsa))
            {
                  return new JDKDSAPrivateKey(info);
            }
            else */
            if (algId.getObjectId().equals(X9ObjectIdentifiers.id_ecPublicKey))
            {
                  return new JCEECPrivateKey(info);
            }
            else
            {
                throw new RuntimeException("algorithm identifier in key not recognised");
            }
        }
    
    public static class EC
    extends JDKKeyFactory
	{
	    String  algorithm;
	
	    public EC()
	    {
	        this("EC");
	    }
	
	    public EC(
	        String  algorithm)
	    {
	        this.algorithm = algorithm;
	    }
	
	    protected PrivateKey engineGeneratePrivate(
	        KeySpec    keySpec)
	        throws InvalidKeySpecException
	    {
	        if (keySpec instanceof PKCS8EncodedKeySpec)
	        {
	            try
	            {
	                return JDKKeyFactory.createPrivateKeyFromDERStream(
	                            new ByteArrayInputStream(((PKCS8EncodedKeySpec)keySpec).getEncoded()));
	            }
	            catch (Exception e)
	            {
	                throw new InvalidKeySpecException(e.toString());
	            }
	        }
	        else if (keySpec instanceof ECPrivateKeySpec)
	        {
	            return new JCEECPrivateKey(algorithm, (ECPrivateKeySpec)keySpec);
	        }
	
	        throw new InvalidKeySpecException("Unknown KeySpec type.");
	    }
	
	    protected PublicKey engineGeneratePublic(
	        KeySpec    keySpec)
	        throws InvalidKeySpecException
	    {
	        if (keySpec instanceof X509EncodedKeySpec)
	        {
	            try
	            {
	                return JDKKeyFactory.createPublicKeyFromDERStream(
	                            new ByteArrayInputStream(((X509EncodedKeySpec)keySpec).getEncoded()));
	            }
	            catch (Exception e)
	            {
	                throw new InvalidKeySpecException(e.toString());
	            }
	        }
	        else if (keySpec instanceof ECPublicKeySpec)
	        {
	            return new JCEECPublicKey(algorithm, (ECPublicKeySpec)keySpec);
	        }
	
	        throw new InvalidKeySpecException("Unknown KeySpec type.");
	    }
	}
    
    public static class ECDSA
    extends EC
    {
	    public ECDSA()
	    {
	        super("ECDSA");
	    }
    }
}
