package com.aelitis.azureus.core.networkmanager.impl.osssl;

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
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.gudy.azureus2.core3.util.SystemProperties;

/**
 * Class for generating self signed SSL keys
 * 
 * @author isdal
 * 
 */
public class OneSwarmSslKeyManager
{

	private class OSSSLTrustManager
		implements X509TrustManager
	{

		public OSSSLTrustManager() {

		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
			// System.out.println("connetion from (" + authType + "):");
			// printCert(certs);
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
			// System.out.println("connected to (" + authType + "):");
			// printCert(certs);
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}

		// private void printCert(X509Certificate[] certs){
		// for (X509Certificate cert : certs) {
		// System.out.println(OsSSLTools.bytesToHex(cert.getPublicKey().getEncoded()));
		// }
		// }
	}

	private static String								KEYS_DIR_NAME	 = "keys";

	public static File									 OSF2F_DIR;

	private static String								OSF2F_FILE_NAME = "osf2f.keys";

	private static String								stringIdentity	= "os f2f test";

	private static String								stringName			= "OS F2F Anonymous Peer";

	private static String								stringPassword	= "oneswarm_default_password";

	private static OneSwarmSslKeyManager instance;

	static {
		OSF2F_DIR = new File(SystemProperties.getUserPath() + File.separator
				+ KEYS_DIR_NAME + File.separator);
		try {
			instance = new OneSwarmSslKeyManager();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static OneSwarmSslKeyManager getInstance() {
		return instance;
	}

	private final KeyStore	 keyStore;

	private final PrivateKey ownPrivateKey;

	private final PublicKey	ownPublicKey;

	private final SSLContext sslContext;

	private long						 startTime;

	private void logWithTime(String msg) {
		final String text = (System.currentTimeMillis() - startTime) + " ms:: "
				+ msg;
		System.err.println("   OneSwarmSslKeyManager: " + text);

	}

	protected OneSwarmSslKeyManager() throws KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException,
			UnrecoverableKeyException, IOException, InterruptedException {
		startTime = System.currentTimeMillis();
		logWithTime("OneSwarmSslKeyManager()");
		this.keyStore = getKeyStore();
		logWithTime("loaded key store");
		this.sslContext = createSSLContext();
		logWithTime("loaded ssl context");

		this.ownPublicKey = keyStore.getCertificate(stringIdentity).getPublicKey();
		this.ownPrivateKey = (PrivateKey) keyStore.getKey(stringIdentity,
				stringPassword.toCharArray());
		logWithTime("done with ssl loading");
	}

	private SSLContext createSSLContext() throws KeyManagementException,
			NoSuchAlgorithmException, KeyStoreException, CertificateException,
			UnrecoverableKeyException, IOException, InterruptedException {
		// init SSL stuff
		KeyStore ks = getKeyStore();

		char[] archPassword = OneSwarmSslKeyManager.stringPassword.toCharArray();
		// Create key manager.
		KeyManagerFactory keymanagerfactory = KeyManagerFactory.getInstance("SunX509");
		keymanagerfactory.init(ks, archPassword);
		KeyManager[] arkeymanager = keymanagerfactory.getKeyManagers();

		// Create trust manager.
		TrustManager[] osTrustManager = new TrustManager[] {
			new OSSSLTrustManager()
		};

		SSLContext sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(arkeymanager, osTrustManager, null);

		return sslcontext;
	}

	public byte[] decrypt(byte[] data, int off, int len) throws Exception {
		return OneSwarmSslTools.decrypt(data, off, len, ownPrivateKey);
	}

	public byte[] encrypt(byte[] data, PublicKey friendsPublicKey)
			throws Exception {
		return OneSwarmSslTools.encrypt(data, friendsPublicKey);
	}

	private KeyStore getKeyStore() throws NoSuchAlgorithmException, IOException,
			InterruptedException, KeyStoreException, CertificateException,
			UnrecoverableKeyException, KeyManagementException {
		if (keyStore != null) {
			return keyStore;
		}
		// Create home directory.
		File keyDirFile = OSF2F_DIR;
		if (!keyDirFile.isDirectory()) {
			keyDirFile.mkdirs();
		}
		// Create keystore.
		File fileKeyStore = new File(keyDirFile, OSF2F_FILE_NAME);

		if (fileKeyStore.exists() == false) {
			System.out.println("Creating keystore...");
			byte[] arb = new byte[16];
			SecureRandom securerandom = SecureRandom.getInstance("SHA1PRNG");
			securerandom.nextBytes(arb);
			stringName = OneSwarmSslTools.bytesToHex(arb);

			String[] arstringCommand = new String[] {
				System.getProperty("java.home") + File.separator + "bin"
						+ File.separator + "keytool",
				"-genkey",
				"-alias",
				stringIdentity,
				"-validity",
				"" + 100 * 365,
				"-keyalg",
				"RSA",
				"-keysize",
				"1024",
				"-dname",
				"CN=" + stringName,
				"-keystore",
				fileKeyStore.getPath(),
				"-keypass",
				stringPassword,
				"-storetype",
				"JKS",
				"-storepass",
				stringPassword
			};
			Process process = Runtime.getRuntime().exec(arstringCommand);
			process.waitFor();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				System.err.println(line);
			}
			BufferedReader err = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));

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
		logWithTime("creating key store instance");
		KeyStore keystore = KeyStore.getInstance("JKS");
		logWithTime("loding key");
		keystore.load(fileinputstream, archPassword);

		return keystore;
	}

	public PublicKey getOwnPublicKey() {
		return ownPublicKey;
	}

	public SSLContext getSSLContext() {
		return sslContext;
	}

	public byte[] sign(byte[] data) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
		//		System.out.println("signing");
		return OneSwarmSslTools.sign(ownPrivateKey, data, 0, data.length);
	}

	public boolean signVerify(byte[] data, int dataOff, int dataLen,
			PublicKey friendsPublicKey, byte[] signBytes, int signOff, int signLen)
			throws Exception {
		return OneSwarmSslTools.verifySignature(data, dataOff, dataLen,
				friendsPublicKey, signBytes, signOff, signLen);
	}

}
