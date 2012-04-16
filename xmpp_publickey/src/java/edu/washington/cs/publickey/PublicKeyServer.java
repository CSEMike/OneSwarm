/**
 * 
 */
package edu.washington.cs.publickey;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.washington.cs.publickey.ssl.server.PublicKeySSLServer;
import edu.washington.cs.publickey.storage.PersistentStorage;
import edu.washington.cs.publickey.storage.sql.derby.PersistentStorageDerby;
import edu.washington.cs.publickey.storage.sql.mysql.PersistentStorageMySQL;
import edu.washington.cs.publickey.xmpp.server.PublicKeyXmppServer;

/**
 * @author isdal
 * 
 */
public class PublicKeyServer {

	private final static String key_db_type = "db_type";
	private final static String key_xmpp = "xmpp";
	private final static String key_ssl = "ssl";
	private final static String key_ssl_passwd = "ssl_keystore_passwd";
	private PersistentStorage storage;
	private final List<PublicKeyXmppServer> publicKeyXmppServers = new LinkedList<PublicKeyXmppServer>();
	private PublicKeySSLServer publicKeySSLServer;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("USAGE: PublicKeyXmppServer configFile");
			System.exit(1);
		}

		try {
			FileInputStream fis = new FileInputStream(args[0]);

			Properties props = new Properties();

			props.load(fis);
			new PublicKeyServer(props);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public PublicKeyServer(Properties properties) {
		String dbType = properties.getProperty(key_db_type);
		try {
			if ("DERBY".toLowerCase().equals(dbType.toLowerCase())) {
				storage = new PersistentStorageDerby(properties);
			} else if ("MYSQL".toLowerCase().equals(dbType.toLowerCase())) {
				storage = new PersistentStorageMySQL(properties, false);
			} else {
				System.err.println("unknown storage type: " + dbType);
				System.exit(1);
			}
		} catch (Exception e) {
			System.err.println("error when initializing storate type:" + dbType);
			e.printStackTrace();
			System.exit(1);
		}

		String xmppEnabled = properties.getProperty(key_xmpp);
		try {
			if (xmppEnabled != null && xmppEnabled.equals("1")) {
				String xmppUserNames = properties.getProperty(PublicKeyXmppServer.key_username);
				String[] split = xmppUserNames.split(",");
				for (String u : split) {
					u = u.trim();
					Properties localProperties = new Properties(properties);
					localProperties.setProperty(PublicKeyXmppServer.key_username, u);
					publicKeyXmppServers.add(new PublicKeyXmppServer(localProperties, storage));
					Thread.sleep(10000);
				}
			}
		} catch (InterruptedException e) {
		}

		String sslEnabled = properties.getProperty(key_ssl);
		try {
			if (sslEnabled != null && sslEnabled.equals("1")) {
				char[] keyStorePasswd;
				if (properties.containsKey(key_ssl_passwd)) {
					keyStorePasswd = properties.getProperty(key_ssl_passwd).toCharArray();
				} else {
					System.out.println("Please supply the key store password");
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

					keyStorePasswd = in.readLine().toCharArray();

				}
				publicKeySSLServer = new PublicKeySSLServer(properties, storage, keyStorePasswd);

			}
		} catch (Exception e) {
			System.err.println("unable to create publickeyserver");
			e.printStackTrace();
		}

		// add a shutdown hook to make sure that we close nicely
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (storage != null) {
					storage.shutdown();
				}
				for (PublicKeyXmppServer publicKeyXmppServer : publicKeyXmppServers) {
					publicKeyXmppServer.shutdown();
				}
				if (publicKeySSLServer != null) {
					publicKeySSLServer.shutdown();
				}
			}
		});

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (!in.readLine().equals("exit")) {
				System.out.println("type exit to shut down the server");
			}
		} catch (Exception ex) {
			// ex.printStackTrace();
		}

	}
}
