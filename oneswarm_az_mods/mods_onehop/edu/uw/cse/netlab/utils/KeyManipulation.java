package edu.uw.cse.netlab.utils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.gudy.azureus2.core3.util.ByteFormatter;

import edu.uw.cse.netlab.reputation.LocalIdentity;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

public class KeyManipulation
{
	/**
	 * Given an encoded public key (by Java, per the X509 spec), returns a PublicKey object
	 * 
	 * @param inBytes the encoded bytes
	 * @return the public key
	 * @throws InvalidKeySpecException 
	 */
	public static PublicKey keyForEncodedBytes( byte [] inBytes ) throws InvalidKeySpecException 
	{
		X509EncodedKeySpec key_spec = new X509EncodedKeySpec(inBytes);
		KeyFactory keyFactory = null;
		try
		{
			keyFactory = KeyFactory.getInstance("RSA");
		} 
		catch( NoSuchAlgorithmException e )
		{
			e.printStackTrace();
			return null;
		}
		
		return keyFactory.generatePublic(key_spec);
	}
	
	/**
	 * Generates a throw-away KeyPair for debugging 
	 * 
	 * @return a key pair
	 */
	public static KeyPair randomKey() 
	{
		try
		{
			X500Name certName = new X500Name("OneSwarm User", "Organiational Unit", "Organization", "City/Locality", "State/Province", "CountryCode");
			CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1withRSA", null);
			keyPair.generate(LocalIdentity.KEY_SIZE_BITS);
			X509Certificate[] chain = new X509Certificate[1];
			int validDays = 100*365;
			chain[0] = keyPair.getSelfCertificate(certName, 60*60*24*validDays);
			KeyPair keys = new KeyPair(keyPair.getPublicKey(), keyPair.getPrivateKey());
			return keys;
		}
		catch( Exception e )
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static final void main( String [] args ) throws Exception
	{
		//KeyPair r = KeyManipulation.randomKey();
		
		byte [][] r = new byte[50][];
		for( int i=0; i<r.length; i++ )
			r[i] = KeyManipulation.randomKey().getPublic().getEncoded();
		
		System.out.println(ByteManip.objectToBytes(r).length + " bytes long");
	}

	public static String concise( byte[] encoded ) 
	{
		try 
		{
			MessageDigest digest = MessageDigest.getInstance("MD5");
			return ByteFormatter.encodeString(digest.digest(encoded));
		}
		catch( Exception e )
		{
			return "null";
		}
	}
}
