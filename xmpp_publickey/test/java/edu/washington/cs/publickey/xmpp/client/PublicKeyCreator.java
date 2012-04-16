package edu.washington.cs.publickey.xmpp.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * Class for generating self signed SSL keys
 * 
 * @author isdal
 * 
 */
public class PublicKeyCreator {

    private static String stringIdentity = "publickey test";

    private static String stringPassword = "not a password";

    private static String stringName = "publickeytest";

    public static String keystoreFileName = "publickeytest.keys";

    public static File keystoreDir;
    static {
	keystoreDir = new File("/tmp/publickey_keystore");
    }

    private static PublicKeyCreator instance = new PublicKeyCreator();

    private SSLContext sslContext;

    private KeyStore keyStore;

    private PublicKey ownPublicKey = null;

    private PrivateKey ownPrivateKey = null;

    protected PublicKeyCreator() {

    }

    private byte[] sign(PrivateKey key, byte[] data) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
	if (keyStore == null) {
	    getKeyStore();
	}
	Signature dsa = Signature.getInstance("SHA1withRSA");
	dsa.initSign(key);
	dsa.update(data);
	return dsa.sign();
    }

    public byte[] sign(byte[] data) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException, KeyManagementException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
	return sign(ownPrivateKey, data);
    }

    private SSLContext createSSLContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
	// init SSL stuff
	KeyStore ks = getKeyStore();

	char[] archPassword = PublicKeyCreator.stringPassword.toCharArray();
	// Create key manager.
	KeyManagerFactory keymanagerfactory = KeyManagerFactory.getInstance("SunX509");
	keymanagerfactory.init(ks, archPassword);
	KeyManager[] arkeymanager = keymanagerfactory.getKeyManagers();

	// Create trust manager.
	TrustManager[] osTrustManager = new TrustManager[] { new OSSSLTrustManager() };

	SSLContext sslcontext = SSLContext.getInstance("SSL");
	sslcontext.init(arkeymanager, osTrustManager, null);

	return sslcontext;
    }

    private KeyStore getKeyStore() throws NoSuchAlgorithmException, IOException, InterruptedException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
	if (keyStore != null) {
	    return keyStore;
	}
	// Create home directory.
	File keyDirFile = keystoreDir;
	if (!keyDirFile.isDirectory()) {
	    keyDirFile.mkdirs();
	}
	// Create keystore.
	File fileKeyStore = new File(keyDirFile, keystoreFileName);

	if (fileKeyStore.exists() == false) {
	    System.out.println("Creating keystore...");
	    byte[] arb = new byte[16];
	    SecureRandom securerandom = SecureRandom.getInstance("SHA1PRNG");
	    securerandom.nextBytes(arb);
	    stringName = bytesToHex(arb);

	    String[] arstringCommand = new String[] { System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool", "-genkey", "-alias", stringIdentity, "-validity", "" + 100 * 365, "-keyalg", "RSA", "-keysize", "1024", "-dname", "CN=" + stringName, "-keystore", fileKeyStore.getPath(), "-keypass", stringPassword, "-storetype", "JKS", "-storepass", stringPassword };
	    Process process = Runtime.getRuntime().exec(arstringCommand);
	    process.waitFor();
	    BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line;
	    while ((line = in.readLine()) != null) {
		System.err.println(line);
	    }
	    BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

	    while ((line = err.readLine()) != null) {
		System.err.println(line);
	    }
	    if (process.exitValue() != 0) {
		throw new IOException("error creating ssl key");
	    }
	}
	// Open keystore.
	char[] archPassword = stringPassword.toCharArray();
	FileInputStream fileinputstream = new FileInputStream(fileKeyStore);
	KeyStore keystore = KeyStore.getInstance("JKS");

	keystore.load(fileinputstream, archPassword);
	System.out.println("loaded keystore, pubkey=" + Base64.encode(keystore.getCertificate(stringIdentity).getPublicKey().getEncoded()));
	this.keyStore = keystore;
	this.ownPublicKey = keyStore.getCertificate(stringIdentity).getPublicKey();
	this.ownPrivateKey = (PrivateKey) keyStore.getKey(stringIdentity, archPassword);
	return keystore;
    }

    public SSLContext getSSLContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
	synchronized (instance) {
	    if (sslContext == null) {
		sslContext = createSSLContext();
	    }
	}
	return sslContext;
    }

    public static PublicKeyCreator getInstance() {
	return instance;
    }

    public PublicKey getOwnPublicKey() {

	synchronized (instance) {
	    if (ownPublicKey == null) {
		try {
		    getKeyStore();
		} catch (KeyManagementException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (KeyStoreException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (CertificateException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (InterruptedException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}
	return ownPublicKey;
    }

    private static String bytesToHex(byte[] data) {
	StringBuffer buf = new StringBuffer();
	for (int i = 0; i < data.length; i++) {
	    buf.append(byteToHex(data[i]));
	}
	return (buf.toString());
    }

    private static String byteToHex(byte data) {
	StringBuffer buf = new StringBuffer();
	buf.append(Integer.toHexString(0x0100 + (data & 0x00FF)).substring(1));
	// buf.append(toHexChar((data >>> 4) & 0x0F));
	// buf.append(toHexChar(data & 0x0F));
	return buf.toString();
    }

    private class OSSSLTrustManager implements X509TrustManager {

	public OSSSLTrustManager() {

	}

	public X509Certificate[] getAcceptedIssuers() {
	    return new java.security.cert.X509Certificate[0];
	}

	public void checkClientTrusted(X509Certificate[] certs, String authType) {
	    // System.out.println("connetion from (" + authType + "):");
	    // printCert(certs);
	}

	public void checkServerTrusted(X509Certificate[] certs, String authType) {
	    // System.out.println("connected to (" + authType + "):");
	    // printCert(certs);
	}

	// private void printCert(X509Certificate[] certs){
	// for (X509Certificate cert : certs) {
	//System.out.println(OsSSLTools.bytesToHex(cert.getPublicKey().getEncoded
	// ()));
	// }
	// }
    }

}
