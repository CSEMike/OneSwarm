package edu.uw.cse.netlab.reputation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.SystemProperties;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

public class LocalIdentity
{
	private static Logger logger = Logger.getLogger(LocalIdentity.class.getName());
	
	// DEFAULTS
	public static final int KEY_SIZE_BITS = 1024;
	public static final String PRIVATE_KEY_ALIAS = "OneSwarm_local_private_key";
	public static final String KEY_STORE_NAME = "oneswarm.keystore";
	
	KeyPair mKeys = null;
	private X509Certificate mLocalCertificate = null;
	
	private static LocalIdentity mInstance = null;
	static {
		try { 
			mInstance = new LocalIdentity();
		} catch( Exception e ) {
			logger.severe("Couldn't create local identity: " + e);
		}
	}
	
	private LocalIdentity() throws Exception 
	{
		mKeys = loadOrGenerateKeys();
	}
	
	public static LocalIdentity get() { return mInstance; }
	
	public KeyPair getKeys() { return mKeys; }
	public X509Certificate getCertificate() { return mLocalCertificate; }
	
	private String getKeyStorePath()
	{
		if( System.getProperty("keystore") == null )
			return SystemProperties.getUserPath() + "/" + KEY_STORE_NAME;
		else
			return System.getProperty("keystore") + "/" + KEY_STORE_NAME;
	}
	
	public KeyPair loadOneSwarmKeyPair( String inPassword ) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException
	{
		File keystoreFile = new File(getKeyStorePath());
		FileInputStream fis = new FileInputStream(keystoreFile);
		
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(fis, inPassword.toCharArray());
		
		Key privateKey = ks.getKey(PRIVATE_KEY_ALIAS, inPassword.toCharArray());
		Certificate cert = ks.getCertificate(PRIVATE_KEY_ALIAS);
		mLocalCertificate = (X509Certificate)cert;
		PublicKey publicKey = cert.getPublicKey();
		
		if( !(privateKey instanceof PrivateKey) )
			throw new IOException(PRIVATE_KEY_ALIAS + " is not of type PrivateKey / " + privateKey.getClass().getName() );
		
		logger.fine("loaded local key pair");
		
		return new KeyPair(publicKey, (PrivateKey)privateKey);
	}
	
	public void saveOneSwarmKeyPair( CertAndKeyGen inKeyPair, Certificate [] inCertChain, char [] inPassword ) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException
	{
		// Create a keystore for the private key and public key certificate
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		String password = "foobear";
		ks.load(null, password.toCharArray());
		ks.setKeyEntry(PRIVATE_KEY_ALIAS, inKeyPair.getPrivateKey(), password.toCharArray(), inCertChain );
		
		//	Save the key store file as temp...
        File file = new File(getKeyStorePath());
        FileOutputStream fos = new FileOutputStream(file);
        ks.store(fos, password.toCharArray());
        fos.flush();
        fos.close();
        
        logger.fine("saved local key pair");
	}
	
	public KeyPair loadOrGenerateKeys() throws Exception
	{
		KeyPair keys = null;
		
		X500Name certName = new X500Name("OneSwarm User", "Organiational Unit", "Organization", "City/Locality", "State/Province", "CountryCode");
		
		try 
		{
			keys = loadOneSwarmKeyPair("foobear");
		} 
		catch( Exception e )
		{
			logger.fine("couldn't load existing key store: " + e);
			
			// Generate public/private key pair and self-signed certificate
			CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1withRSA", null);
			keyPair.generate(KEY_SIZE_BITS);
			
			// This certificate is good for ~100 years
			X509Certificate[] chain = new X509Certificate[1];
			int validDays = 100*365;
			chain[0] = keyPair.getSelfCertificate(certName, 60*60*24*validDays);
			mLocalCertificate = chain[0];
			
			saveOneSwarmKeyPair( keyPair, chain, "foobear".toCharArray() );
			
			keys = new KeyPair(keyPair.getPublicKey(), keyPair.getPrivateKey());
		}
		
		return keys;
	}
	
	public void exportLocalCertificate( File outFile ) throws IOException
	{
		if( mLocalCertificate == null )
			throw new IOException("no local certificate exists!");
		try
		{
			(new FileOutputStream(outFile)).write(mLocalCertificate.getEncoded());
		} catch (CertificateEncodingException e)
		{
			throw new IOException("Certificate encoding exception: " + e);
		}
	}
}
