package edu.washington.cs.publickey.ssl.server;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQL;
import edu.washington.cs.publickey.storage.sql.derby.PersistentStorageDerby;

public class PublicKeySSLServerTest {

	private static final File DATA_BASE_DIR = new File("/tmp/test1");
	private static final String username = "publickey";
	private final static String password = "";
	private PersistentStorageSQL storage;

	@Before
	public void setUp() throws Exception {
		System.out.println("*******************setup");
		try {
			Properties props = new Properties();
			props.put(PersistentStorageDerby.key_db_username, username);
			props.put(PersistentStorageDerby.key_db_password, password);
			props.put(PersistentStorageDerby.key_db_path, DATA_BASE_DIR.getCanonicalPath());
			storage = new PersistentStorageDerby(props);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPublicKeySSLServer() {
		Properties p = new Properties();
		String keyStorePath = "/tmp/keystore";
		// File keyStoreFile = new File(keyStorePath);
		// if (keyStoreFile.exists()) {
		// keyStoreFile.delete();
		// }

		p.put(PublicKeySSLServer.KEY_SSL_SERVER_KEYSTORE, keyStorePath);
		int serverPort = 12345;
		p.put(PublicKeySSLServer.KEY_SSL_PORT, serverPort + "");

		try {
			// this should create a new keystore
			char[] password = "abc123".toCharArray();
			PublicKeySSLServer publicKeySSLServer = new PublicKeySSLServer(p, storage, password);
			System.in.read();

			// SSLSocket s = new SSLKeyManager(new File("/tmp/client_keystore"),
			// password).createClientSocket("localhost", serverPort);
			// DataOutputStream out = new DataOutputStream(s.getOutputStream());
			// out.writeInt(52345);
			// Thread.sleep(1000);
			publicKeySSLServer.shutdown();


			// Thread.sleep(1000);
			// publicKeySSLServer = new PublicKeySSLServer(p, null, password);
			// Thread.sleep(1000);
			// publicKeySSLServer.shutdown();
			// Thread.sleep(1000);

			// this should load and existing key store
			// new PublicKeySSLServer(p, null, password);
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
