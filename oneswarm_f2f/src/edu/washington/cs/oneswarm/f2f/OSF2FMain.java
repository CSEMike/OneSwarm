package edu.washington.cs.oneswarm.f2f;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.f2f.dht.DHTConnector;
import edu.washington.cs.oneswarm.f2f.friends.FriendManager;
import edu.washington.cs.oneswarm.f2f.friends.LanFriendFinder;
import edu.washington.cs.oneswarm.f2f.invitations.InvitationManager;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageDecoder;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageEncoder;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessage;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessageDecoder;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessageEncoder;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1DownloadManager;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1CalcListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1HashJobListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1Result;
import edu.washington.cs.oneswarm.f2f.network.F2FDownloadManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;

public class OSF2FMain {

	private static Logger logger = Logger.getLogger(OSF2FMain.class.getName());

	private boolean initialized = false;
	// private NetworkManager.ByteMatcher osMatcher;
	// private NetworkManager.ByteMatcher osAuthMatcher;

	private OverlayManager overlayManager;
	private FriendManager friendManager;
	private DHTConnector friendConnector;
	private FileListManager fileListManager;
	private F2FDownloadManager f2DownloadManager;
	private final static OSF2FMain instance = new OSF2FMain();
	private ClassLoader classLoader;
	private LanFriendFinder lanFriendFinder;
	private PermissionsDAO permissionManager;
	private GlobalManagerStats stats;
	private OSF2FNatChecker natChecker;
	private OSF2FSpeedChecker speedChecker;
	private Sha1DownloadManager sha1DownloadManager;
	public static OSF2FMain getSingelton() {
		return instance;
	}

	protected OSF2FMain() {

		try {
			/*
			 * check if the logging is initialized, if not load it
			 */
			System.err.println("OSF2FMain: loading log settings. cwd: " + (new File(".")).getAbsolutePath());
			File logConfig = new File("./logging.properties");
			if (logConfig.exists()) {
				System.err.println("OSF2FMain: log file found");

				synchronized (LogManager.class) {
					final LogManager logManager = LogManager.getLogManager();
					Enumeration<String> loggers = logManager.getLoggerNames();
					// check if we have any non null loggers
					boolean logConfLoaded = false;
					while (loggers.hasMoreElements()) {
						final String l = loggers.nextElement();
						if (l.length() > 0 && Logger.getLogger(l).getLevel() != null) {
							logConfLoaded = true;
							break;
						}
					}
					if (!logConfLoaded) {
						logManager.readConfiguration(new FileInputStream(logConfig));
						System.err.println("OSF2FMain: read log configuration");
						Enumeration<String> loggerNames = logManager.getLoggerNames();
						while (loggerNames.hasMoreElements()) {
							final String l = loggerNames.nextElement();
							System.out.println("OSF2FMain: log-" + l + " " + Logger.getLogger(l).getLevel());
						}
					}
				}
				System.err.println("OSF2FMain: logger created");
				logger.fine("logger loaded");
				System.err.println("OSF2FMain: logger loaded");
			}
		} catch (IOException e) {
			Debug.out("error loading logger", e);
		}

	}

	private long startTime;

	private void logWithTime(String msg, Level level) {
		final String text = (System.currentTimeMillis() - startTime) + " ms:: " + msg;
		logger.log(level, text);
		System.err.println("OSF2FMain: " + text);

	}

	private InvitationManager invitationManager;

	public void init(final PluginInterface pIf) {
		logWithTime("init started", Level.FINE);
		synchronized (instance) {
			if (!initialized) {
				initialized = true;
				startTime = System.currentTimeMillis();
				logWithTime("in synchronized block", Level.FINE);
				this.classLoader = pIf.getPluginClassLoader();
				logWithTime("getting global manager stats", Level.FINE);
				this.stats = AzureusCoreImpl.getSingleton().getGlobalManager().getStats();
				logWithTime("getting own public key", Level.FINE);
				PublicKey localPublicKey = OneSwarmSslKeyManager.getInstance().getOwnPublicKey();

				logWithTime("creating friend manager", Level.FINE);
				this.friendManager = new FriendManager(classLoader, localPublicKey.getEncoded());
				logWithTime("loading permissions manager", Level.FINE);
				this.permissionManager = PermissionsDAO.get();
				this.permissionManager.init(pIf.getIPC());

				logWithTime("creating invitation manager", Level.FINE);
				this.invitationManager = new InvitationManager(classLoader, friendManager, localPublicKey.getEncoded());

				logWithTime("creating file list manager", Level.FINE);
				this.fileListManager = new FileListManager(permissionManager);
				logWithTime("creating overlaymanager", Level.FINE);
				this.overlayManager = new OverlayManager(friendManager, localPublicKey, fileListManager, stats);
				logWithTime("starting protocol matcher thread)", Level.FINE);
				Thread t = new Thread(new Runnable() {

					public void run() {
						logWithTime("Protocol matcher thread started", Level.FINE);
						OsProtocolMatcher osMatcher = new OsProtocolMatcher(true);
						NetworkManager.getSingleton().requestIncomingConnectionRouting(osMatcher, new OsNetworkRouterListener(), new MessageStreamFactory() {
							public MessageStreamDecoder createDecoder() {
								return new OSF2FMessageDecoder();
							}

							public MessageStreamEncoder createEncoder() {
								return new OSF2FMessageEncoder();
							}
						});
						logWithTime("OS Protocol matcher installed", Level.FINE);
						OsProtocolMatcher osAuthMatcher = new OsProtocolMatcher(false);
						NetworkManager.getSingleton().requestIncomingConnectionRouting(osAuthMatcher, new OsAuthRouterListener(), new MessageStreamFactory() {
							public MessageStreamDecoder createDecoder() {
								return new OSF2FAuthMessageDecoder();
							}

							public MessageStreamEncoder createEncoder() {
								return new OSF2FAuthMessageEncoder();
							}
						});
						logWithTime("OS-Auth Protocol matcher installed", Level.FINE);
					}
				});
				t.setDaemon(true);
				t.setName("OSF2F protocol matcher loader");
				t.start();

				pIf.addListener(new PluginListener() {
					public void initializationComplete() {
						AEThread2 t = new AEThread2("OSF2F::dhtConnect", true) {
							public void run() {
								logWithTime("loading DHT manager", Level.FINE);
								DistributedDatabase distributedDatabase = pIf.getDistributedDatabase();
								logWithTime("creating friend connector", Level.FINE);
								friendConnector = new DHTConnector(distributedDatabase, friendManager, invitationManager, overlayManager);
								logWithTime("crating lan friend finder", Level.FINE);
								try {
									lanFriendFinder = new LanFriendFinder(friendConnector, friendManager, invitationManager, overlayManager.getOwnPublicKey().getEncoded());
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								logWithTime("dht dependent init completed", Level.FINE);
							}
						};
						t.start();
					}

					public void closedownInitiated() {
					}

					public void closedownComplete() {
					}
				});

				logWithTime("creating f2f download manager", Level.FINE);
				this.f2DownloadManager = new F2FDownloadManager(overlayManager);

				this.natChecker = new OSF2FNatChecker();
				this.speedChecker = new OSF2FSpeedChecker(stats);

				logWithTime("loading sha1 matcher", Level.INFO);
				sha1DownloadManager = new Sha1DownloadManager();
				sha1DownloadManager.addHashJobListener(new Sha1HashJobListener() {
					public Sha1CalcListener jobAdded(String name) {
						return new Sha1CalcListener() {
							public void completed(Sha1Result result) {
								fileListManager.scheduleFileListRefresh();
							}

							public void errorOccured(Throwable cause) {
							}

							public void progress(double fraction) {
							}
						};
					}
				});

				sanity_check_perms();

				PermissionsDAO.get().f2fInitialized();

				logWithTime("f2f init done", Level.INFO);
			}
		}
	}

	public OSF2FNatChecker getNatChecker() {
		return natChecker;
	}

	public OSF2FSpeedChecker getSpeedChecker() {
		return speedChecker;
	}

	@SuppressWarnings("unchecked")
	private void sanity_check_perms() {

		long start = System.currentTimeMillis();
		for (DownloadManager dm : (List<DownloadManager>) AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers()) {
			try {
				byte[] hashBytes = dm.getTorrent().getHash();
				List<GroupBean> groups = permissionManager.getGroupsForHash(ByteFormatter.encodeString(hashBytes));

				boolean any_friend = false, pub_net = false;
				for (GroupBean g : groups) {
					if (g.equals(GroupBean.PUBLIC) == false) {
						any_friend = true;
					} else {
						pub_net = true;
					}
				}

				boolean update = false;
				if (F2FDownloadManager.isSharedWithFriends(hashBytes) != any_friend) {
					logger.warning("f2f mismatch in network / perms. perm: " + any_friend + " / net: " + F2FDownloadManager.isSharedWithFriends(hashBytes));
					update = true;
				}

				if (F2FDownloadManager.isSharedWithPublic(hashBytes) != pub_net) {
					logger.warning("public mismatch in network / perms. perm: " + pub_net + " / net: " + F2FDownloadManager.isSharedWithPublic(hashBytes));
					update = true;
				}

				if (update) {
					f2DownloadManager.setTorrentPrivacy(hashBytes, pub_net, any_friend);
				} else {
					// Log.log("good perms/privacy for: " + dm.getDisplayName()
					// + " f2f: " + any_friend + " pub: " + pub_net);
				}

			} catch (Exception e) {
				StackTraceElement[] st = e.getStackTrace();
				logWithTime("error sanity checking perms for: " + dm.getDisplayName() + " / " + e.toString() + " " + st[0].getFileName() + ":" + st[0].getLineNumber(), Level.WARNING);
			}
		}
		logWithTime("sanity check perms took: " + (System.currentTimeMillis() - start), Level.FINE);

	}

	public LanFriendFinder getLanFriendFinder() {
		return lanFriendFinder;
	}

	public F2FDownloadManager getF2DownloadManager() {
		return f2DownloadManager;
	}

	public FriendManager getFriendManager() {
		return friendManager;
	}

	public DHTConnector getDHTConnector() {
		return friendConnector;
	}

	public OverlayManager getOverlayManager() {
		return overlayManager;
	}

	private class OsNetworkRouterListener implements NetworkManager.RoutingListener {
		public boolean autoCryptoFallback() {
			return (false);
		}

		public void connectionRouted(NetworkConnection connection, Object routing_data) {

			if (!connection.getTransport().getEncryption().startsWith(OneSwarmSslTransportHelperFilterStream.SSL_NAME)) {
				logger.warning("OSF2F connection without SSL!: " + connection + " (" + connection.getTransport().getEncryption() + "), disconnecting");
				connection.close();
				return;
			}
			if (routing_data == null || !(routing_data instanceof byte[])) {
				logger.warning("got strange routing data: " + routing_data);
				connection.close();
				return;
			}

			byte[] remotePub = (byte[]) routing_data;
			logger.finest("Incoming connection from [" + connection + "] successfully routed to OneSwarm F2F: encr: " + connection.getTransport().getEncryption() + "\n" + "remote public key: " + new String(Base64.encode(remotePub)));

			Friend f = friendManager.getFriend(remotePub);
			if (f == null) {
				logger.warning("unknown remote host (not friend), disconnecting. Remote public key: \n" + new String(Base64.encode(remotePub)));
				friendManager.gotConnectionFromNonFriend(connection.getEndpoint().getNotionalAddress().getAddress().getHostAddress(), remotePub);
				connection.close();
			} else if (f.isBlocked()) {
				logger.warning("connection from blocked user: " + f.getNick() + ", diconnecting");
				connection.close();
			} else {
				logger.finer("connection from: " + f.getNick());
				overlayManager.createIncomingConnection(remotePub, connection);
			}
		}
	}

	private class OsAuthRouterListener implements NetworkManager.RoutingListener {
		public boolean autoCryptoFallback() {
			return (false);
		}

		public void connectionRouted(NetworkConnection connection, Object routing_data) {

			if (!connection.getTransport().getEncryption().startsWith(OneSwarmSslTransportHelperFilterStream.SSL_NAME)) {
				logger.warning("OSAuth connection without SSL!: " + connection + " (" + connection.getTransport().getEncryption() + ")");
				connection.close();
				return;
			}

			if (routing_data == null || !(routing_data instanceof byte[])) {
				logger.warning("got strange routing data: " + routing_data);
				connection.close();
				return;
			}

			byte[] remotePub = (byte[]) routing_data;
			if (remotePub == null) {
				logger.warning("got incoming auth connection with null public key!");
				connection.close();
				return;
			}
			logger.fine("Incoming connection from [" + connection + "] successfully routed to OneSwarm Auth: encr: " + connection.getTransport().getEncryption() + "\n" + "remote public key: " + new String(Base64.encode(remotePub)));

			if (!invitationManager.newIncomingConnection(remotePub, connection)) {
				connection.close();
			}
		}
	}

	static class OsProtocolMatcher implements NetworkManager.ByteMatcher {
		private final ByteBuffer legacy_handshake_header;

		private int size;

		public OsProtocolMatcher(boolean defaultProtocol) {
			if (defaultProtocol) {
				legacy_handshake_header = ByteBuffer.allocate(20);
				legacy_handshake_header.put((byte) OSF2FMessage.ONESWARM_PROTOCOL.length());
				legacy_handshake_header.put(OSF2FMessage.ONESWARM_PROTOCOL.getBytes());
				legacy_handshake_header.flip();
				this.size = legacy_handshake_header.remaining();
			} else {
				legacy_handshake_header = ByteBuffer.allocate(20);
				legacy_handshake_header.put((byte) OSF2FAuthMessage.ONESWARM_AUTH_PROTOCOL.length());
				legacy_handshake_header.put(OSF2FAuthMessage.ONESWARM_AUTH_PROTOCOL.getBytes());
				legacy_handshake_header.flip();
				this.size = legacy_handshake_header.remaining();
			}
		}

		public byte[][] getSharedSecrets() {
			return null;
		}

		public int getSpecificPort() {
			return (-1);
		}

		public Object matches(TransportHelper transport, ByteBuffer to_compare, int port) {

			// System.out.println("incoming connection to match: "
			// + transport.getAddress());
			Object obj = transport.getUserData(OneSwarmSslTransportHelperFilterStream.REMOTE_SSL_PUBLIC_KEY);

			if (!(obj instanceof byte[])) {
				return null;
			}
			// System.out.println(new String(to_compare.array()));

			int old_limit = to_compare.limit();
			to_compare.limit(to_compare.position() + maxSize());
			boolean matches = to_compare.equals(legacy_handshake_header);
			to_compare.limit(old_limit); // restore buffer structure
			if (!matches) {
				return null;
			}
			return obj;
		}

		public int matchThisSizeOrBigger() {
			return (maxSize());
		}

		public int maxSize() {
			return size;
		}

		public Object minMatches(TransportHelper transport, ByteBuffer to_compare, int port) {
			return (matches(transport, to_compare, port));
		}

		public int minSize() {
			return maxSize();
		}
	}

	public boolean isInitialized() {
		return initialized;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void redeemInvitation(FriendInvitation invitation, boolean testOnly) throws Exception {
		invitationManager.redeemInvitation(invitation, testOnly);

	}

	public InvitationManager getAuthManager() {
		return invitationManager;
	}

	public List<FriendInvitation> getLocallyCreatedInvitations() {
		return invitationManager.getLocallyCreatedInvitations();
	}	
}