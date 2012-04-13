package edu.washington.cs.publickey.ssl.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.util.Base64;

public class SSLKeyManager {

	private static String KEY_ID = "PublicKey SSL Key";

	public static String bytesToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			buf.append(byteToHex(data[i]));
		}
		return (buf.toString());
	}

	public static String byteToHex(byte data) {
		StringBuffer buf = new StringBuffer();
		buf.append(Integer.toHexString(0x0100 + (data & 0x00FF)).substring(1));
		// buf.append(toHexChar((data >>> 4) & 0x0F));
		// buf.append(toHexChar(data & 0x0F));
		return buf.toString();
	}

	private final KeyStore keyStore;
	private final File keyStoreFile;

	private final char[] password;

	private final SSLContext sslContext;

	public SSLKeyManager(File keyStore, char[] password) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
		this.keyStoreFile = keyStore;
		this.password = password;

		this.keyStore = createKeyStore();
		this.sslContext = createSSLContext();
	}

	private KeyStore createKeyStore() throws NoSuchAlgorithmException, IOException, InterruptedException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {

		if (keyStoreFile == null) {
			throw new IOException("keystore file is null!");
		}
		// Check if we need to create the file/dirs
		if (keyStoreFile.isDirectory()) {
			throw new IOException("keystore file is a directory, " + keyStoreFile.getAbsolutePath());
		} else if (!keyStoreFile.exists()) {
			File parent = keyStoreFile.getParentFile();
			if (!parent.exists()) {
				boolean success = keyStoreFile.mkdirs();
				if (!success) {
					throw new IOException("unable to create directory: '" + parent.getAbsolutePath() + "'");
				}
			}
		}

		// Create keystore.
		if (keyStoreFile.exists() == false) {
			System.out.println("Generating new keystore...");
			byte[] randomStuff = new byte[16];
			SecureRandom securerandom = SecureRandom.getInstance("SHA1PRNG");
			securerandom.nextBytes(randomStuff);
			String stringName = bytesToHex(randomStuff);

			String stringPassword = new String(password);
			String[] cmd = new String[] { System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool", "-genkey", "-alias", KEY_ID, "-validity", "" + 100 * 365, "-keyalg", "RSA", "-keysize", "1024", "-dname", "CN=" + stringName, "-keystore", keyStoreFile.getPath(), "-keypass", stringPassword, "-storetype", "JKS", "-storepass", stringPassword };
			Process process = Runtime.getRuntime().exec(cmd);
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
		FileInputStream fileinputstream = new FileInputStream(keyStoreFile);
		KeyStore keystore = KeyStore.getInstance("JKS");

		keystore.load(fileinputstream, password);
		String encodedPublicKey = Base64.encodeBytes(keystore.getCertificate(KEY_ID).getPublicKey().getEncoded(), Base64.DONT_BREAK_LINES);
		System.out.println("public key=" + encodedPublicKey);
		return keystore;
	}

	public SSLServerSocket createServerSocket(int serverPort) throws IOException {
		SSLServerSocket serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(serverPort);
		return serverSocket;
	}

	public SSLSocket createClientSocket(String serverHost, int serverPort) throws IOException {
		SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(serverHost, serverPort);
		socket.setNeedClientAuth(true);
		return socket;
	}

	private SSLContext createSSLContext() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, IOException, InterruptedException {
		// init SSL stuff

		KeyManagerFactory keymanagerfactory = KeyManagerFactory.getInstance("SunX509");
		keymanagerfactory.init(keyStore, password);
		KeyManager[] arkeymanager = keymanagerfactory.getKeyManagers();

		// Create trust manager, we accept any certificates (it will be
		// checked later)
		TrustManager[] osTrustManager = new TrustManager[] { new AllTrustingManager() };

		SSLContext sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(arkeymanager, osTrustManager, null);

		return sslcontext;
	}

	private static class AllTrustingManager implements X509TrustManager {

		public void checkClientTrusted(X509Certificate[] certs, String authType) {

		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}

		public X509Certificate[] getAcceptedIssuers() {
			return new java.security.cert.X509Certificate[0];
		}
	}
}
