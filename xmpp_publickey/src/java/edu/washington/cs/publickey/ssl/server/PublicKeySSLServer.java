package edu.washington.cs.publickey.ssl.server;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.Tools;
import edu.washington.cs.publickey.storage.PersistentStorage;

public class PublicKeySSLServer {

	static final String KEY_SSL_SERVER_KEYSTORE = "ssl_server_keystore";
	final static String KEY_SSL_PORT = "ssl_server_port";

	private static final int NUM_THREADS = 500;
	private static final int MAX_DB_QUEUE = 20;

	/*
	 * to protect against dos, only allow 2 connection attempts/second from the
	 * same ip and max 50 concurrent connections
	 */
	private static final long MIN_MS_BETWEEN_CONNECT_ATTEMPTS_PER_IP = 500;
	private static final Integer MAX_CONNECTION_PER_IP = 10;

	private final PersistentStorage storage;
	private final int serverPort;
	private volatile boolean quit = false;
	private final ExecutorService threadPool;
	private SSLServerSocket serverSocket;

	private HashMap<String, Integer> activeConnections = new HashMap<String, Integer>();
	private HashMap<String, Long> lastConnectAttempt = new HashMap<String, Long>();

	private volatile int queueLength = 0;

	public PublicKeySSLServer(Properties props, PersistentStorage storage, char[] keystorePassword) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, InterruptedException {
		this.serverPort = Integer.parseInt((String) props.get(KEY_SSL_PORT));
		this.storage = storage;
		this.threadPool = Executors.newFixedThreadPool(NUM_THREADS);

		File keyStoreFile = new File(props.getProperty(KEY_SSL_SERVER_KEYSTORE));

		SSLKeyManager sslManager = new SSLKeyManager(keyStoreFile, keystorePassword);
		serverSocket = sslManager.createServerSocket(serverPort);
		serverSocket.setNeedClientAuth(true);

		Thread t = new Thread(new Runnable() {
			public void run() {
				System.out.println("SSL server: listening on port " + serverPort);
				while (!quit) {
					try {
						SSLSocket csocket = (SSLSocket) serverSocket.accept();

						String remoteIp = csocket.getInetAddress().getHostAddress();
						if (isConnectionAllowed(remoteIp)) {
							queueLength++;
							// System.out.println("connection from: " + remoteIp
							// + " queue=" + queueLength);
							initiatingConnection(remoteIp);
							PublicKeySSLServerProtocol publicKeySSLServerProtocol = new PublicKeySSLServerProtocol(csocket);
							threadPool.execute(publicKeySSLServerProtocol);
						}
					} catch (IOException e) {
						if (e instanceof java.net.SocketException && e.getMessage().equals("Socket closed")) {
							System.out.println("SSL Server: closing socket");
						} else {
							e.printStackTrace();
						}
					}
				}
			}
		});
		t.setName("SSL Server accept thread");
		t.start();
	}

	private void initiatingConnection(String remoteIp) {
		synchronized (lastConnectAttempt) {
			lastConnectAttempt.put(remoteIp, System.currentTimeMillis());
			Integer active = activeConnections.get(remoteIp);
			if (active == null) {
				activeConnections.put(remoteIp, 1);
			} else {
				activeConnections.put(remoteIp, active + 1);
			}
		}
	}

	private void closingConnection(String remoteIp) {
		synchronized (lastConnectAttempt) {
			Integer active = activeConnections.get(remoteIp);
			if (active <= 1) {
				activeConnections.remove(remoteIp);
			} else {
				activeConnections.put(remoteIp, active - 1);
			}
		}
	}

	private boolean isConnectionAllowed(String remoteIp) {
		synchronized (lastConnectAttempt) {
			Long lastAttempt = lastConnectAttempt.get(remoteIp);

			if (lastAttempt != null) {
				long timeSince = System.currentTimeMillis() - lastAttempt;
				if (timeSince < MIN_MS_BETWEEN_CONNECT_ATTEMPTS_PER_IP) {
					System.err.println(new Date() + ": connection from '" + remoteIp + "' denied, " + " to high connect frequency (" + timeSince + "ms<" + MIN_MS_BETWEEN_CONNECT_ATTEMPTS_PER_IP + "ms)");
					return false;
				}
			}
			Integer numActiveConnections = activeConnections.get(remoteIp);
			if (numActiveConnections != null && numActiveConnections > MAX_CONNECTION_PER_IP) {
				System.err.println(new Date() + ": connection from '" + remoteIp + "' denied, " + " to many connections (" + numActiveConnections + ">" + MAX_CONNECTION_PER_IP + ")");
				return false;
			}
			return true;
		}
	}

	class PublicKeySSLServerProtocol implements Runnable {

		private final SSLSocket socket;
		private final String remoteIp;
		long timeInclNet;

		public PublicKeySSLServerProtocol(SSLSocket socket) {

			this.socket = socket;
			this.remoteIp = socket.getInetAddress().getHostAddress();
			timeInclNet = System.currentTimeMillis();
		}

		public void run() {
			try {
				int dbQueueLength = storage.getDbQueueLength();
				if (dbQueueLength > MAX_DB_QUEUE) {

					long t = System.currentTimeMillis();

					DataInputStream in = new DataInputStream(socket.getInputStream());
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(socket.getOutputStream())));

					// read to not confuse the client
					readIgnoreList(in);

					out.write(PublicKeyFriend.serialize(new PublicKeyFriend[0]));
					out.close();
					in.close();
					// log("dropping incoming connection, db_queue: " +
					// dbQueueLength + " conn_queue: " + queueLength + " took: "
					// + (System.currentTimeMillis() - t) + "ms");
				} else {
					Certificate[] remoteCerts;

					remoteCerts = socket.getSession().getPeerCertificates();
					if (remoteCerts.length > 0) {
						byte[] remoteKey = remoteCerts[0].getPublicKey().getEncoded();
						// log("accepting incoming connection, db_queue: " +
						// dbQueueLength + " conn_queue: " + queueLength);
						DataInputStream in = new DataInputStream(socket.getInputStream());
						BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(socket.getOutputStream())));

						HashSet<Integer> keysToIgnore = readIgnoreList(in);

						long timeExclNet = System.currentTimeMillis();
						// ok, now we know what to ignore
						// lets try to find new friends
						PublicKeyFriend f = new PublicKeyFriend();
						f.setPublicKey(remoteKey);
						f.setPublicKeySha1(Tools.getSha1(remoteKey));
						long dbStartTime = System.currentTimeMillis();
						List<PublicKeyFriend> allFriends = storage.getFriendsUsingPublicKey(f);
						long preLastSeenUpdate = System.currentTimeMillis();
						storage.updateUserLastSeen(f);
						long dbTime = System.currentTimeMillis() - dbStartTime;

						long lastSeenOverhead = System.currentTimeMillis() - preLastSeenUpdate;
						Map<Integer, PublicKeyFriend> newFriends = new HashMap<Integer, PublicKeyFriend>();

						// add them all
						for (PublicKeyFriend friend : allFriends) {
							// ignore friends the user already knows of
							int friendHash = Arrays.hashCode(friend.getPublicKeySha1());
							if (!keysToIgnore.contains(friendHash)) {
								// check if we already added this one
								if (!newFriends.containsKey(friendHash)) {
									newFriends.put(friendHash, friend);
								}
							}
						}

						List<PublicKeyFriend> friendsArray = new LinkedList<PublicKeyFriend>();
						friendsArray.addAll(newFriends.values());

						/*
						 * for debugging, just return an empty list
						 */
						String serialized = PublicKeyFriend.serialize(new PublicKeyFriend[0]);
						// String serialized =
						// PublicKeyFriend.serialize(friendsArray.toArray(new
						// PublicKeyFriend[newFriends.size()]));
						timeExclNet = System.currentTimeMillis() - timeExclNet;
						out.write(serialized);
						out.close();
						in.close();
						timeInclNet = System.currentTimeMillis() - timeInclNet;
						log("done, returned: " + friendsArray.size() + " ignored: " + keysToIgnore.size() + " time: (queries: " + timeExclNet + " lastSeen: " + lastSeenOverhead + " total: " + timeInclNet + " db=" + dbTime + ")");
					}

				}
			} catch (java.io.EOFException e) {
				System.err.println(remoteIp + ": " + "EOF to early");
			} catch (SSLPeerUnverifiedException e) {
				System.err.println(remoteIp + ": no cert, closing conn");
			} catch (java.net.SocketException e) {
				if ("Connection timed out".equals(e.getMessage())) {
					System.err.println(remoteIp + ": other side timed out");
				} else if ("Connection reset".equals(e.getMessage())) {
					System.err.println(remoteIp + ": other side closed socket");
				} else if ("Broken pipe".equals(e.getMessage())) {
					System.err.println(remoteIp + ": broken pipe");
				} else {
					e.printStackTrace();
				}
			} catch (IOException e) {

				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e1) {
					}
				}
				closingConnection(remoteIp);
				queueLength--;
			}
		}

		private HashSet<Integer> readIgnoreList(DataInputStream in) throws IOException {
			int numToIgnore = in.readInt();
			if (numToIgnore > 100000) {
				System.err.println("warning: user specified more that 100000 friends (!!!???), closing conn");
				socket.close();
				throw new IOException("user specified invalid data");
			}

			HashSet<Integer> keysToIgnore = new HashSet<Integer>();
			byte[] pubKeySha = new byte[20];
			for (int i = 0; i < numToIgnore; i++) {
				// read 20 bytes
				in.readFully(pubKeySha);
				int hash = Arrays.hashCode(pubKeySha);
				keysToIgnore.add(hash);
			}
			return keysToIgnore;
		}

		private void log(String msg) {
			System.out.println(remoteIp + ": " + msg);
		}
	}

	public void shutdown() {
		quit = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		threadPool.shutdown();
		storage.shutdown();
	}
}
