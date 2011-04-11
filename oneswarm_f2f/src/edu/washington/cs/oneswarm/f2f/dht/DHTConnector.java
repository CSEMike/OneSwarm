package edu.washington.cs.oneswarm.f2f.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;
import org.gudy.azureus2.plugins.ddb.DistributedDatabase;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseContact;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseEvent;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseException;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseKey;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseListener;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferHandler;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseTransferType;
import org.gudy.azureus2.plugins.ddb.DistributedDatabaseValue;
import org.gudy.azureus2.pluginsimpl.local.ddb.DDBaseImpl;

import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerListener;
import com.aelitis.azureus.core.instancemanager.AZInstanceTracked;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslKeyManager;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.FriendInvitation.Status;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.friends.FriendManager;
import edu.washington.cs.oneswarm.f2f.friends.LanFriendFinder;
import edu.washington.cs.oneswarm.f2f.invitations.InvitationManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;

public class DHTConnector {

	// run the connect task every 30s, most of the time it will not do
	// anything

	private static final int CONNECTOR_FREQUENCY = 30 * 1000;

	private static final int ENCRYPTED_LENGTH = 128;
	private static Logger logger = Logger.getLogger(DHTConnector.class.getName());

	// don't try to connect more than once every 15 minutes,
	// our friends will hopefully try to connect to us if they get online
	private final static long MIN_CONNECT_TIME_DIFF = 15 * 60 * 1000;

	private final static long REPUBLISH_TIME = 3600 * 1000;
	private static final int SIGN_LENGTH = 128;
	private final static String USE_CHT_PROXY_SETTINGS_KEY = "OSF2F.Use DHT Proxy";

	private CHTClientUDP chtClientUDP;

	private long chtLastPublishTime;

	private long dhtLastPublishTime;

	private InetAddress externalIp = null;
	
	/*
	 * keep track of this, when the system recovers from standby (time jumpes)
	 * we want to force connection attempts
	 */
	private long lastConnectorRunTime = System.currentTimeMillis();

	private InetAddress lastPublishedIP;
	private int lastPublishedPort;
	// private final FriendManager friendManager;
	private final OverlayManager overlayManager;

	private final InvitationManager invitationManager;

	private byte[] ownPublicKey = null;

	private int tcpListeningPort;

	private final HashMap<HashWrapper, DistributedDatabaseKey> dhtKeyCache = new HashMap<HashWrapper, DistributedDatabaseKey>();

	private long queuedDHTReadRequests = 0;
	private long completedDHTReadRequests = 0;
	private long timedoutDHTReadRequests = 0;
	private long queuedDHTWriteRequests = 0;
	private long completedDHTWriteRequests = 0;
	private long timedoutDHTWriteRequests = 0;
	private static final long DHT_TIMEOUT = 60 * 1000;
	private final static int MAX_DHT_READ_QUEUE_LENGTH = 200;
	private final static int MAX_DHT_WRITE_QUEUE_LENGTH = 200;

	public DHTConnector(FriendManager friendManager, InvitationManager invitationManager,
			OverlayManager _overlayManager) {
		logger.fine("cht enabled=" + isChtEnabled());
		this.overlayManager = _overlayManager;
		this.invitationManager = invitationManager;

		PublicKey k = overlayManager.getOwnPublicKey();
		if (k != null) {
			ownPublicKey = k.getEncoded();
		}
		// this.friendManager = friendManager;

		// this.pendingConnections = new ConcurrentHashMap<Friend, Long>();
		final AZInstance myInstance = AzureusCoreImpl.getSingleton().getInstanceManager().getMyInstance();
		logger.finer("before external IP");
		try {
			externalIp = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			externalIp = null;
		}
		Thread t = (new Thread("Get external IP") {
			@Override
			public void run() {
				try {
					DHTConnector.this.externalIp = myInstance.getExternalAddress();
					logger.finer("got external IP: " + externalIp.getHostAddress());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		this.tcpListeningPort = myInstance.getTCPListenPort();
		logger.finer("after external IP");
		AzureusCoreImpl.getSingleton().getInstanceManager().addListener(new AZInstanceManagerListener() {
			@Override
			public void instanceChanged(AZInstance instance) {

				AZInstance newInstance = AzureusCoreImpl.getSingleton().getInstanceManager().getMyInstance();
				boolean changed = false;
				if (!newInstance.getExternalAddress().equals(externalIp) || newInstance.getTCPListenPort() != tcpListeningPort) {
					changed = true;
					externalIp = newInstance.getExternalAddress();
					tcpListeningPort = newInstance.getTCPListenPort();
					publishLocationInfo();
				}
				if (changed) {
					logger.fine("ip address/port changed, new addr: " + newInstance.getExternalAddress() + ":" + newInstance.getTCPListenPort());
					/*
					 * try to connect to friends again and republish in 1 minute
					 * (should give them time to get disconnected and for things
					 * to settle
					 */
					Timer t = new Timer("IpChangedFriendReconnector", true);
					t.schedule(new TimerTask() {
						@Override
						public void run() {
							logger.fine("ip changed, trying to connect to friends");
							List<Friend> disconnectedFriends = overlayManager.getDisconnectedFriends();
							for (Friend friend : disconnectedFriends) {
								if (!friend.isBlocked()) {
									connectToFriend(friend);
								}
							}

							LanFriendFinder lanFriendFinder = OSF2FMain.getSingelton().getLanFriendFinder();
							if (lanFriendFinder != null) {
								lanFriendFinder.stop();
								lanFriendFinder.start();
							}
						}

					}, 30 * 1000);
				}
			}

			@Override
			public void instanceFound(AZInstance instance) {
			}

			@Override
			public void instanceLost(AZInstance instance) {
			}

			@Override
			public void instanceTracked(AZInstanceTracked instance) {
			}
		});
		/*
		 * Create the cht client to assist the dht
		 */

		try {
			this.chtClientUDP = new CHTClientUDP("cht.oneswarm.org", 11744);
		} catch (UnknownHostException e) {
			Debug.out("unable to create CHT Client", e);
		}

		Timer connectorTimer = new Timer("OS Friend Connector", true);
		FriendConnectorRunnable r = new FriendConnectorRunnable();
		connectorTimer.schedule(r, 0, CONNECTOR_FREQUENCY);

		Timer inviteConnectorTimer = new Timer("OS Invite Connector", true);
		InvitationConnectorRunnable ir = new InvitationConnectorRunnable();
		inviteConnectorTimer.schedule(ir, 0, CONNECTOR_FREQUENCY);

		// for dht testing purposes
		boolean test = false;
		if (test) {
			test();
		}

	}

	private boolean chtPublishAllowed(InetAddress localAddress, int localPort) {
		if (!isChtEnabled()) {
			return false;
		}
		if (ownPublicKey == null) {
			PublicKey k = overlayManager.getOwnPublicKey();
			if (k != null) {
				ownPublicKey = k.getEncoded();
				if (ownPublicKey == null) {
					return false;
				}
			} else {
				logger.fine("unknown local public key, " + "can't publish ip:port in cht");
				return false;
			}
		} else if (lastPublishedIP != null && (!lastPublishedIP.equals(localAddress) || localPort != lastPublishedPort)) {
			// ip changed, new publish allowed
			return true;
		} else if (System.currentTimeMillis() - chtLastPublishTime < REPUBLISH_TIME) {
			logger.finest("can't publish ip:port in cht: " + "already published within time limit");
			return false;
		}
		return true;
	}

	private boolean connectAttemptAllowed(Friend friend) {

		/*
		 * if we just added the friend, be a bit more aggressive (allow 1
		 * attempt per minute)
		 */
		if (System.currentTimeMillis() - friend.getDateAdded().getTime() < 15 * 60 * 1000) {
			// allow connect every minute
			if (friend.msSinceLastAttempt() > 60 * 1000) {
				return true;
			}
		}

		if (friend.msSinceLastAttempt() < MIN_CONNECT_TIME_DIFF) {
			logger.finest("connect attempt not allowed, " + "tried: " + (friend.msSinceLastAttempt() / (1000 * 60)) + " minutes ago");
			return false;
		}
		if (friend.getStatus() != Friend.STATUS_OFFLINE) {
			logger.finest("connect attempt not allowed, (connecting/connected...)");
			return false;
		}

		return true;
	}

	public void connectToFriend(final Friend friend) {
		connectToFriend(friend, true);
	}

	public void connectToFriend(final Friend friend, boolean allowDht) {
		final ConcurrentHashMap<String, Boolean> triedIps = new ConcurrentHashMap<String, Boolean>();
		if (friend.getStatus() == Friend.STATUS_ONLINE) {
			return;
		}
		if (friend.isBlocked()) {
			logger.finer("tried to connect to blocked friend: " + friend.getNick());
			return;
		}

		friend.connectionAttempt("Connection attempt initiated");

		/*
		 * first, try to connect to the cached ip if possible
		 */
		try {
			if (friend.getLastConnectIP() != null && !friend.getLastConnectIP().equals(InetAddress.getByName("0.0.0.0")) && friend.getLastConnectPort() != 0) {
				logger.finer("cache connecting to: " + friend.getNick());
				String ipPort = friend.getLastConnectIP().getHostAddress() + ":" + friend.getLastConnectPort();
				if (!triedIps.contains(ipPort)) {
					triedIps.put(ipPort, true);
					friend.updateConnectionLog(true, "Trying cached location: " + ipPort);
					overlayManager.createOutgoingConnection(new ConnectionEndpoint(new InetSocketAddress(friend.getLastConnectIP(), friend.getLastConnectPort())), friend);
				} else {
					friend.updateConnectionLog(true, "Skipping location: " + ipPort + " (already tried)");
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * connect to the agreed on location
		 */
		if (friend.getDhtReadLocation() != null) {
			byte[] key = friend.getDhtReadLocation();
			if (chtClientUDP != null && isChtEnabled()) {
				chtLookupAndConnect(friend, triedIps, key, "secret loc");
			}
			if (allowDht && getDht().isAvailable()) {
				dhtLookupAndConnect(friend, triedIps, key, "secret loc");
			} else {
				friend.updateConnectionLog(true, "DHT not available");
			}
		}

		if (!friend.isDhtLocationConfirmed()) {

			/*
			 * get the location to look at,
			 * 
			 * our friend has published his ip:port at "his key" append
			 * "our key"
			 */
			byte[] key = new byte[friend.getPublicKey().length + ownPublicKey.length];
			System.arraycopy(friend.getPublicKey(), 0, key, 0, friend.getPublicKey().length);
			System.arraycopy(ownPublicKey, 0, key, friend.getPublicKey().length, ownPublicKey.length);

			/*
			 * and try the cht (centralized hash table) :-)
			 */
			if (chtClientUDP != null && isChtEnabled()) {
				byte[] keySha1 = new SHA1Simple().calculateHash(key);
				chtLookupAndConnect(friend, triedIps, keySha1, "pubkey loc");
			}

			/*
			 * and try the dht
			 */
			if (allowDht && getDht().isAvailable()) {
				dhtLookupAndConnect(friend, triedIps, key, "pubkey loc");
			} else {
				friend.updateConnectionLog(true, "DHT not available");
			}
		}
	}

	/*
	 * A mock DHT that we use during startup until the real DHT exists. This simply results in all
	 * DHT actions being deferred until the real DHT is available.
	 */
	DistributedDatabase currentlyAvailableDht = new DistributedDatabase() {
		@Override
		public boolean isAvailable() {
			return false;
		}

		@Override
		public boolean isExtendedUseAllowed() {
			return false;
		}

		@Override
		public DistributedDatabaseContact getLocalContact() {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public DistributedDatabaseKey createKey(Object key) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public DistributedDatabaseKey createKey(Object key, String description)
				throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public DistributedDatabaseValue createValue(Object value)
				throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public DistributedDatabaseContact importContact(InetSocketAddress address)
				throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void write(DistributedDatabaseListener listener, DistributedDatabaseKey key,
				DistributedDatabaseValue value) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void write(DistributedDatabaseListener listener, DistributedDatabaseKey key,
				DistributedDatabaseValue[] values) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void read(DistributedDatabaseListener listener, DistributedDatabaseKey key,
				long timeout) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void read(DistributedDatabaseListener listener, DistributedDatabaseKey key,
				long timeout, int options) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void readKeyStats(DistributedDatabaseListener listener, DistributedDatabaseKey key,
				long timeout) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void delete(DistributedDatabaseListener listener, DistributedDatabaseKey key)
				throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public void addTransferHandler(DistributedDatabaseTransferType type,
				DistributedDatabaseTransferHandler handler) throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

		@Override
		public DistributedDatabaseTransferType getStandardTransferType(int standard_type)
				throws DistributedDatabaseException {
			throw new RuntimeException("Unsupported");
		}

	};
	AtomicBoolean dhtGetCalled = new AtomicBoolean(false);

	/**
	 * Returns the real DHT if it has completed initialization, otherwise a mock object that always
	 * indicates that the DHT is unavailable. This routine serves to prevent initialization from
	 * blocking when the DHT is enabled, since we will be able to locate many IP-port pairs from our
	 * cache, the CHT, or community servers.
	 */
	private DistributedDatabase getDht() {
		// Only do this once.
		if (dhtGetCalled.compareAndSet(false, true)) {
			Thread t = new Thread("DHTConnector DHT acquirer.") {
				@Override
				public void run() {
					currentlyAvailableDht = DDBaseImpl.getSingleton(AzureusCoreImpl.getSingleton());
				}
			};
			t.setDaemon(true);
			t.start();
		}
		return currentlyAvailableDht;
	}

	private final ConcurrentHashMap<Friend, Long> lastDhtLookupForFriend = new ConcurrentHashMap<Friend, Long>();

	private void dhtLookupAndConnect(final Friend friend, final ConcurrentHashMap<String, Boolean> triedIps, byte[] key, final String locSource) {
		logger.finer("DHT: connecting to: " + friend.getNick());
		if (getOutstandingDhtReadRequests() > MAX_DHT_READ_QUEUE_LENGTH) {
			logger.finest("Skipping DHT location lookup, dht read queue too long: " + getOutstandingDhtReadRequests());
			friend.updateConnectionLog(true, "Skipping DHT location lookup, dht read queue too long: " + getOutstandingDhtReadRequests());
			return;
		}
		lastDhtLookupForFriend.put(friend, System.currentTimeMillis());

		friend.updateConnectionLog(true, "Looking up friend location in DHT(" + locSource + ")");
		try {
			final DistributedDatabaseKey dhtKey = createKey(key);
			queuedDHTReadRequests++;
			getDht().read(new DistributedDatabaseListener() {
				@Override
				public void event(DistributedDatabaseEvent event) {
					logger.finest("DHT read event:" + event.getType());
					if (event.getType() == DistributedDatabaseEvent.ET_VALUE_READ) {
						DistributedDatabaseValue value = event.getValue();
						try {
							byte[] bytes = (byte[]) value.getValue(byte[].class);
							decryptAndConnect(triedIps, friend, bytes, "DHT(" + locSource + ")");
						} catch (Exception e) {
							friend.updateConnectionLog(true, "dht value error: " + e.getMessage());
						}
					} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
						completedDHTReadRequests++;
						logger.fine("DHT read event completed, queued=" + queuedDHTReadRequests + " completed=" + completedDHTReadRequests + " outstanding=" + getOutstandingDhtReadRequests() + " timeout=" + timedoutDHTReadRequests);
					} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
						logger.fine("DHT read event timed out, queued=" + queuedDHTReadRequests + " completed=" + completedDHTReadRequests + " outstanding=" + getOutstandingDhtReadRequests() + " timeout=" + timedoutDHTReadRequests);
						timedoutDHTReadRequests++;
					}

				}

			}, dhtKey, DHT_TIMEOUT);
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long getOutstandingDhtReadRequests() {
		return queuedDHTReadRequests - completedDHTReadRequests - timedoutDHTReadRequests;
	}

	public long getOutstandingDhtWriteRequests() {
		return queuedDHTWriteRequests - completedDHTWriteRequests - timedoutDHTWriteRequests;
	}

	private void chtLookupAndConnect(final Friend friend, final ConcurrentHashMap<String, Boolean> triedIps, byte[] key, final String locSource) {
		logger.finer("CHT: connecting to: " + friend.getNick());
		friend.updateConnectionLog(true, "Looking up friend location in CHT (" + locSource + ")");
		chtClientUDP.get(key, new CHTClientUDP.CHTCallback() {
			@Override
			public void errorReceived(Throwable cause) {
				friend.updateConnectionLog(true, "CHT lookup failed: " + cause.getMessage());
			}

			@Override
			public void valueReceived(byte[] key, byte[] value) {
				try {
					decryptAndConnect(triedIps, friend, value, "CHT (" + locSource + ")");
				} catch (Exception e) {
					friend.updateConnectionLog(true, "cht value error: " + e.getMessage());
					e.printStackTrace();
				}

			}
		});
	}

	private DistributedDatabaseKey createKey(byte[] key) throws DistributedDatabaseException {
		final DistributedDatabaseKey dhtKey;
		HashWrapper keyHash = new HashWrapper(key);
		if (dhtKeyCache.containsKey(keyHash)) {
			dhtKey = dhtKeyCache.get(keyHash);
		} else {
			dhtKey = getDht().createKey(key);
			dhtKeyCache.put(keyHash, dhtKey);
			logger.finer("Creating dht key, size=" + dhtKeyCache.size());
			if (dhtKeyCache.size() > 10000 && dhtKeyCache.size() % 100 == 0) {
				Debug.out("Creating a lot of dht keys, size=" + dhtKeyCache.size());
			}
		}
		return dhtKey;
	}

	public void connectToFriend(final Friend friend, InetAddress addrHint, int portHint) {
		if (friend.getStatus() == Friend.STATUS_ONLINE) {
			return;
		}
		if (friend.isBlocked()) {
			return;
		}
		if (friend.msSinceLastAttempt() < 60 * 1000) {
			return;
		}
		friend.connectionAttempt("Got location from LanFriendFinder: " + addrHint.getHostAddress() + ":" + portHint);
		overlayManager.createOutgoingConnection(new ConnectionEndpoint(new InetSocketAddress(addrHint, portHint)), friend);
	}

	public void connectToInvitation(final FriendInvitation invitation) {
		invitation.connectAttempted();

		/*
		 * the ip:port is xored with the first n bytes in the SHA1(bytes 10-20)
		 * in the key
		 * 
		 * the last 10 bytes are left untouched to make any offline attacks hard
		 */

		// first, check if we have ip:port in the invite
		String lastConnectIp = invitation.getLastConnectIp();
		if (lastConnectIp != null && invitation.getLastConnectPort() > 0) {
			try {
				InetAddress byName = InetAddress.getByName(lastConnectIp);
				ConnectionEndpoint c = new ConnectionEndpoint(InetSocketAddress.createUnresolved(byName.getHostAddress(), invitation.getLastConnectPort()));
				logger.finer("Connecting to: " + byName + ":" + invitation.getLastConnectPort());
				invitationManager.newOutgoingConnection(c, invitation);
			} catch (Exception e) {
				// ignoreany problems...
			}

		}

		// calc key base
		// if we created the invitation, look up the non-creator address
		// if we redeem the invitation, look up the creator address
		boolean creatorAddress = true;
		if (invitation.isCreatedLocally()) {
			creatorAddress = false;
		}
		byte[] keyBase = invitation.getDHTKeyBase(creatorAddress);

		// then, do the cht lookup
		if (chtClientUDP != null && isChtEnabled()) {
			byte[] loc = new SHA1Simple().calculateHash(keyBase);
			chtClientUDP.get(loc, new CHTClientUDP.CHTCallback() {
				@Override
				public void errorReceived(Throwable cause) {
					logger.finest(cause.getMessage());
				}

				@Override
				public void valueReceived(byte[] key, byte[] value) {
					logger.finest("got value from cht: " + Base32.encode(value));
					try {
						createAuthConnFromHtValue(invitation, value);
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			logger.finest("locating invitation: addr=" + Base32.encode(loc));
		}
		// and last the dht lookup
		if (getDht().isAvailable()) {
			try {
				DistributedDatabaseKey dhtKey = createKey(keyBase);
				queuedDHTReadRequests++;
				getDht().read(new DistributedDatabaseListener() {
					@Override
					public void event(DistributedDatabaseEvent event) {
						logger.finest("DHT read event:" + event.getType());
						if (event.getType() == DistributedDatabaseEvent.ET_VALUE_READ) {
							DistributedDatabaseValue value = event.getValue();
							try {
								byte[] bytes = (byte[]) value.getValue(byte[].class);
								createAuthConnFromHtValue(invitation, bytes);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
							completedDHTReadRequests++;
							logger.finest("DHT read event completed or timed out, queued=" + queuedDHTReadRequests + " completed=" + completedDHTReadRequests + " outstanding=" + getOutstandingDhtReadRequests());
						} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
							logger.finer("DHT read event completed or timed out, queued=" + queuedDHTReadRequests + " completed=" + completedDHTReadRequests + " outstanding=" + getOutstandingDhtReadRequests());
							timedoutDHTReadRequests++;
						}
					}
				}, dhtKey, DHT_TIMEOUT);
			} catch (DistributedDatabaseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}

	private void createAuthConnFromHtValue(final FriendInvitation invitation, byte[] value) throws UnknownHostException {

		byte[] ipPort = invitation.xorDhtValue(value);

		byte[] ip = new byte[ipPort.length - 2];
		System.arraycopy(ipPort, 0, ip, 0, ip.length);
		InetAddress address = InetAddress.getByAddress(ip);

		byte[] portBytes = new byte[] { 0, 0, ipPort[ip.length], ipPort[ip.length + 1] };
		int port = byteArrayToInt(portBytes);

		logger.finest("after decrypt: " + address.getHostAddress() + ":" + port);
		ConnectionEndpoint c = new ConnectionEndpoint(new InetSocketAddress(address, port));
		if (invitation.getStatus().getCode() > Status.STATUS_NEW.getCode() && invitation.getStatus().getCode() < Status.STATUS_IP_PORT_LOCATED.getCode()) {
			invitation.setStatus(Status.STATUS_IP_PORT_LOCATED);
		}
		invitationManager.newOutgoingConnection(c, invitation);
	}

	private void decryptAndConnect(ConcurrentHashMap<String, Boolean> triedIps, final Friend friend, byte[] value, String source) throws Exception, UnknownHostException {
		byte[] decrypted = verifyAndDecrypt(value, friend.getPublicKeyObj());
		InetAddress addr = getInetAddress(decrypted);
		int port = getPort(decrypted);
		long timeStamp = getTimeStamp(decrypted);
		long age = System.currentTimeMillis() - timeStamp;
		String ipPort = addr.getHostAddress() + ":" + port;
		
		if (!triedIps.containsKey(ipPort)) {
			triedIps.put(ipPort, true);
			friend.updateConnectionLog(true, "got friend location from " + source + ": " + ipPort + " age=" + age / (60 * 1000) + " minutes");
			overlayManager.createOutgoingConnection(new ConnectionEndpoint(new InetSocketAddress(addr, port)), friend);
		} else {
			friend.updateConnectionLog(true, "Skipping ip:port from " + source + ": " + ipPort + " (already tried)");
		}
		
		if( source.startsWith("DHT") ) {
			logger.fine("DHT lookup verified successfully (" + friend.getNick() + ")");
		}
	}

	private boolean dhtPublishAllowed(InetAddress localAddress, int localPort) {
		if (!getDht().isAvailable()) {
			return false;
		}
		if (ownPublicKey == null) {
			PublicKey k = overlayManager.getOwnPublicKey();
			if (k != null) {
				ownPublicKey = k.getEncoded();
				if (ownPublicKey == null) {
					return false;
				}
			} else {
				logger.finest("unknown local public key, " + "can't publish ip:port in dht");
				return false;
			}
		} else if (lastPublishedIP != null && (!lastPublishedIP.equals(localAddress) || localPort != lastPublishedPort)) {
			// ip changed, new publish allowed
			return true;
		} else if (System.currentTimeMillis() - dhtLastPublishTime < REPUBLISH_TIME) {
			logger.finest("can't publish ip:port in dht: " + "already published within time limit");
			return false;
		}
		return true;
	}

	private boolean isChtEnabled() {
		return COConfigurationManager.getBooleanParameter(USE_CHT_PROXY_SETTINGS_KEY);
	}

	private void publishAttempted(InetAddress localAddress, int localPort, boolean dht, boolean cht) {
		this.lastPublishedIP = localAddress;
		this.lastPublishedPort = localPort;
		if (dht) {
			this.dhtLastPublishTime = System.currentTimeMillis();
		}
		if (cht) {
			this.chtLastPublishTime = System.currentTimeMillis();
		}
	}

	public void publishInvitation(final FriendInvitation invitation) {
		if (!invitation.isStillValid()) {
			logger.finer("not publishing invitation location");
			return;
		}
		logger.finest("publishing invitation loc info");
		boolean published = false;
		byte[] keyBase = invitation.getDHTKeyBase(invitation.isCreatedLocally());

		byte[] value = shaAndXor(invitation, externalIp, tcpListeningPort);
		if (chtClientUDP != null && isChtEnabled()) {
			try {
				byte[] loc = new SHA1Simple().calculateHash(keyBase);
				chtClientUDP.put(loc, value);
				logger.finest("publishing invitation: key=" + Base32.encode(loc));
				published = true;
				logger.finest("before encrypt: " + externalIp.getHostAddress() + ":" + tcpListeningPort + " after: " + Base32.encode(value));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.finest("cht done");
		if (getDht().isAvailable()) {
			try {
				DistributedDatabaseKey dhtKey = createKey(keyBase);
				final DistributedDatabaseValue[] dhtValue = new DistributedDatabaseValue[] { getDht()
						.createValue(value) };
				published = true;
				queuedDHTWriteRequests++;
				getDht().write(new DistributedDatabaseListener() {
					@Override
					public void event(DistributedDatabaseEvent event) {
						// Log.log("DHT write event:" + event.getType(),
						// logToStdOut);
						// if (event.getType() ==
						// DistributedDatabaseEvent.ET_VALUE_WRITTEN) {
						// Log.log("dht publish succeded", logToStdOut);
						// }
						if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
							completedDHTWriteRequests++;
							logger.fine("DHT write event completed, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests() + " timeout=" + timedoutDHTWriteRequests);
						} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
							logger.fine("DHT write event timed out, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests() + " timeout=" + timedoutDHTWriteRequests);
							timedoutDHTWriteRequests++;
						}
					}
				}, dhtKey, dhtValue);
			} catch (DistributedDatabaseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if (invitation.getStatus().equals(Status.STATUS_NEW) && published) {
			invitation.setStatus(Status.STATUS_IP_PORT_PUBLISHED);
		}
		logger.finest("dht done");
	}

	private final ConcurrentHashMap<Friend, Long> lastDhtPublishForFriend = new ConcurrentHashMap<Friend, Long>();

	public boolean publishLocationInfo() {
		InetAddress localAddress = externalIp;
		int localPort = tcpListeningPort;

		boolean useDht = dhtPublishAllowed(localAddress, localPort);
		boolean useCht = chtPublishAllowed(localAddress, localPort);

		publishAttempted(localAddress, localPort, useDht, useCht);

		if (useDht) {
			logger.fine("putting location info in DHT: " + localAddress.getHostAddress() + ":" + localPort);
		}
		if (useCht) {
			logger.fine("putting location info in CHT: " + localAddress.getHostAddress() + ":" + localPort);
		}

		byte[] ipPortTimeStamp = convertToIPPortTimeStamp(localAddress, localPort, System.currentTimeMillis());

		try {
			final AtomicBoolean dhtAvailable = new AtomicBoolean(false);
			if (getDht().isAvailable()) {
				dhtAvailable.set(true);
			}
			ArrayList<Friend> friendsSorted = new ArrayList<Friend>(Arrays.asList(OSF2FMain.getSingelton().getFriendManager().getFriends()));
			Collections.sort(friendsSorted, new Comparator<Friend>() {
				@Override
				public int compare(Friend o1, Friend o2) {
					if (dhtAvailable.get() == false) {
						return lastConnectCompare(o1, o2);
					} else {
						/*
						 * if o1 never been published in the dht, do so first
						 */
						Long o1Publish = lastDhtPublishForFriend.get(o1);
						Long o2Publish = lastDhtPublishForFriend.get(o2);
						if (o1Publish == null && o2Publish == null) {
							// neither is checked, sort by last connect
							return lastConnectCompare(o1, o2);
						}
						if (o1Publish != null && o2Publish == null) {
							// o1 has been checked before
							return -1;
						} else if (o1Publish == null && o2Publish != null) {
							return 1;
						} else {
							/*
							 * sort by last dht lookup time
							 */
							return o1Publish.compareTo(o2Publish);
						}

					}
				}

				private int lastConnectCompare(Friend o1, Friend o2) {
					if (o1.getLastConnectDate() != null) {
						if (o2.getLastConnectDate() == null) {
							return -1;
						} else {
							return -1 * o1.getLastConnectDate().compareTo(o2.getLastConnectDate());
						}
					} else {
						return 1;
					}
				}
			});
			for (Friend f : friendsSorted) {
				publishLocationInfoForFriend(f, ipPortTimeStamp, useDht, useCht);
			}
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return useCht || useDht;
	}

	public void publishLocationInfoForFriend(Friend f) throws NoSuchAlgorithmException, InvalidKeySpecException, DistributedDatabaseException, Exception {
		InetAddress localAddress = externalIp;
		int localPort = tcpListeningPort;
		byte[] ipPortTimeStamp = convertToIPPortTimeStamp(localAddress, localPort, System.currentTimeMillis());

		boolean useDht = false;
		if (getDht().isAvailable()) {
			useDht = true;
		}

		boolean useCht = isChtEnabled();
		publishLocationInfoForFriend(f, ipPortTimeStamp, useDht, useCht);
	}

	// private byte[] dhtValueMerge(HashMap<Byte, byte[]> dataRead) {
	// int totalSize = 0;
	// for (byte[] b : dataRead.values()) {
	// totalSize += b.length - 2;
	// }
	//
	// byte[] merged = new byte[totalSize];
	// int pos = 0;
	// for (byte i = 0; i < dataRead.size(); i++) {
	// byte[] chunk = dataRead.get(i);
	// System.arraycopy(chunk, 2, merged, pos, chunk.length - 2);
	// pos += chunk.length - 2;
	// }
	// Log.log("merged " + dataRead.size() + " chunks (" + merged.length +
	// " bytes)", logToStdOut);
	// return merged;
	// }

	// private DistributedDatabaseValue[] dhtValueSplit(byte[] bigChunkOfData)
	// throws DistributedDatabaseException {
	// // use chunk sizes of 130 bytes
	// int MAX_CHUNK_SIZE = 256;
	// int SPLIT_CHUNK_SIZE = 128;
	// if (bigChunkOfData.length <= MAX_CHUNK_SIZE) {
	// return new DistributedDatabaseValue[] {
	// dhtManager.createValue(bigChunkOfData) };
	// } else {
	// List<DistributedDatabaseValue> values = new
	// LinkedList<DistributedDatabaseValue>();
	//
	// int pos = 0;
	// byte num = 0;
	// int totalChunks = ((bigChunkOfData.length / SPLIT_CHUNK_SIZE) + 1);
	// if (totalChunks > 128) {
	// throw new RuntimeException("max chunk num=128");
	// }
	// while (pos < bigChunkOfData.length) {
	// int sizeOfCurrentChunk = Math.min(bigChunkOfData.length - pos,
	// SPLIT_CHUNK_SIZE);
	// byte[] currentChunk = new byte[sizeOfCurrentChunk + 2];
	// currentChunk[0] = num;
	// currentChunk[1] = (byte) totalChunks;
	// System.arraycopy(bigChunkOfData, pos, currentChunk, 2,
	// sizeOfCurrentChunk);
	//
	// DistributedDatabaseValue v = dhtManager.createValue(currentChunk);
	// values.add(v);
	// // System.err.println("created dht value: pos=" + pos + " len="
	// // + currentChunk.length + " total_len=" + bigChunkOfData.length
	// // + " chunk=" + num + " total_chunks=" + totalChunks);
	// pos += sizeOfCurrentChunk;
	// num++;
	//
	// }
	// return values.toArray(new DistributedDatabaseValue[values.size()]);
	// }
	//
	// }

	private void publishLocationInfoForFriend(Friend f, byte[] ipPortTimeStamp, boolean dht, boolean cht) throws NoSuchAlgorithmException, InvalidKeySpecException, DistributedDatabaseException, Exception {
		if (!dht && !cht) {
			return;
		}

		if (!f.isBlocked()) {

			if (dht) {
				logger.finer("DHT: publishing to: " + f.getNick());
				if (getOutstandingDhtWriteRequests() > MAX_DHT_WRITE_QUEUE_LENGTH) {
					logger.finest("Skipping DHT location publish, dht write queue too long: " + getOutstandingDhtWriteRequests());
					dht = false;
				} else {
					lastDhtPublishForFriend.put(f, System.currentTimeMillis());
				}
			}

			/*
			 * create a custom value for the friend
			 */
			byte[] value = encryptAndSign(ipPortTimeStamp, f.getPublicKeyObj());

			/*
			 * public key based key, format is: our key in bytes, append friends
			 * key in bytes, Azureus will calc the sha1 of this
			 * 
			 * if the friend has an unconfirmed custom dht location, publish to
			 * both, else publish only to the custom location
			 */

			/*
			 * if we have custom dht values
			 */
			if (f.getDhtWriteLocation() != null) {
				byte[] key = f.getDhtWriteLocation();
				/*
				 * and write to dht
				 */
				if (dht) {
					logger.finer("putting location info into dht for friend: " + f.getNick());
					final DistributedDatabaseKey dhtKey = createKey(key);
					final DistributedDatabaseValue[] dhtValue = new DistributedDatabaseValue[] { getDht()
							.createValue(value) };
					queuedDHTWriteRequests++;
					getDht().write(new DistributedDatabaseListener() {
						@Override
						public void event(DistributedDatabaseEvent event) {
							if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
								completedDHTWriteRequests++;
								logger.finest("DHT write event completed, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests());
							} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
								logger.finest("DHT write event timed out, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests());
								timedoutDHTWriteRequests++;
							}
						}
					}, dhtKey, dhtValue);
				}
				if (cht) {
					chtClientUDP.put(key, value);
				}
			}

			/*
			 * if the dht location is not confirmed we need to publish to the
			 * public key based location as well
			 */
			if (!f.isDhtLocationConfirmed()) {
				byte[] friendKey = f.getPublicKey();
				byte[] key = new byte[friendKey.length + ownPublicKey.length];
				System.arraycopy(ownPublicKey, 0, key, 0, ownPublicKey.length);
				System.arraycopy(friendKey, 0, key, ownPublicKey.length, friendKey.length);
				/*
				 * and write to dht
				 */
				if (dht) {
					final DistributedDatabaseKey dhtKey = createKey(key);
					final DistributedDatabaseValue[] dhtValue = new DistributedDatabaseValue[] { getDht()
							.createValue(value) };
					queuedDHTWriteRequests++;
					getDht().write(new DistributedDatabaseListener() {
						@Override
						public void event(DistributedDatabaseEvent event) {
							if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE || event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
								if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_COMPLETE) {
									completedDHTWriteRequests++;
									logger.finest("DHT write event completed, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests());
								} else if (event.getType() == DistributedDatabaseEvent.ET_OPERATION_TIMEOUT) {
									logger.finest("DHT write event timed out, queued=" + queuedDHTWriteRequests + " completed=" + completedDHTWriteRequests + " outstanding=" + getOutstandingDhtWriteRequests());
									timedoutDHTWriteRequests++;
								}
							}
						}
					}, dhtKey, dhtValue);
				}
				if (cht) {
					byte[] keySha = new SHA1Simple().calculateHash(key);
					chtClientUDP.put(keySha, value);
				}
			}
		}
	}

	private void test() {
		DHTLog.logging_on = true;
		DHTLog.setLogger(null);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						Thread.sleep(30 * 1000);
						System.out.println("testing write");
						testWrite();
						Thread.sleep(15 * 1000);
						System.out.println("testing read");
						testRead();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.setName("dht tester thread");
		t.start();
	}

	private void testRead() {
		byte[] keySha1 = new SHA1Simple().calculateHash(ownPublicKey);
		chtClientUDP.get(keySha1, new CHTClientUDP.CHTCallback() {
			@Override
			public void errorReceived(Throwable cause) {
				System.out.println("CHT lookup failed");
			}

			@Override
			public void valueReceived(byte[] key, byte[] value) {
				try {
					byte[] data = verifyAndDecrypt(value, overlayManager.getOwnPublicKey());
					System.out.println("got cht: " + getInetAddress(data) + ":" + getPort(data) + " age=" + ((System.currentTimeMillis() - getTimeStamp(data)) / (1000)) + " s");
					// byte[] verif
				} catch (Exception e) {
					System.out.println("got error: " + e.getMessage());
					e.printStackTrace();
				}

			}
		});

		try {
			final DistributedDatabaseKey dhtKey = createKey(ownPublicKey);
			getDht().read(new DistributedDatabaseListener() {
				@Override
				public void event(DistributedDatabaseEvent event) {
					logger.fine("DHT read event:" + event.getType());
					if (event.getType() == DistributedDatabaseEvent.ET_VALUE_READ) {
						DistributedDatabaseValue value = event.getValue();
						try {
							byte[] bytes = (byte[]) value.getValue(byte[].class);
							byte[] data = verifyAndDecrypt(bytes, overlayManager.getOwnPublicKey());
							logger.fine("got dht: " + getInetAddress(data) + ":" + getPort(data) + " age=" + ((System.currentTimeMillis() - getTimeStamp(data)) / (1000)) + " s");
							// byte[] verif
						} catch (Exception e) {
							System.out.println("got error: " + e.getMessage());
							e.printStackTrace();
						}

					}

				}

			}, dhtKey, DHT_TIMEOUT);
		} catch (DistributedDatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void testWrite() {
		try {
			InetAddress localAddress = externalIp;
			int localPort = tcpListeningPort;

			byte[] ipPortTimeStamp = convertToIPPortTimeStamp(localAddress, localPort, System.currentTimeMillis());

			byte[] value;

			value = encryptAndSign(ipPortTimeStamp, overlayManager.getOwnPublicKey());

			final DistributedDatabaseKey dhtKey = createKey(ownPublicKey);
			final DistributedDatabaseValue[] dhtValue = new DistributedDatabaseValue[] { getDht()
					.createValue(value) };
			getDht().write(new DistributedDatabaseListener() {
				@Override
				public void event(DistributedDatabaseEvent event) {
					System.out.println("DHT write event:" + event.getType());
					if (event.getType() == DistributedDatabaseEvent.ET_VALUE_WRITTEN) {
						System.out.println("dht publish succeded");
					}
				}
			}, dhtKey, dhtValue);

			byte[] keySha = new SHA1Simple().calculateHash(ownPublicKey);
			chtClientUDP.put(keySha, value);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int byteArrayToInt(byte[] b) {
		return (b[0] << 24) + ((b[1] & 255) << 16) + ((b[2] & 255) << 8) + (b[3] & 255);
	}

	private static long byteArrayToLong(byte[] b) {
		return (((long) b[0] << 56) + ((long) (b[1] & 255) << 48) + ((long) (b[2] & 255) << 40) + ((long) (b[3] & 255) << 32) + ((long) (b[4] & 255) << 24) + ((b[5] & 255) << 16) + ((b[6] & 255) << 8) + ((b[7] & 255) << 0));
	}

	/**
	 * returns: 4/16 bytes inet addr, 2 byte port, 8 byte timestamp
	 * 
	 * @param addr
	 * @param port
	 * @return
	 */
	private static byte[] convertToIPPortTimeStamp(InetAddress addr, int port, long time) {

		int addrLength = addr.getAddress().length;
		byte[] dhtBytes = new byte[addrLength + 2 + 8];

		/*
		 * copy in the inet address
		 */
		System.arraycopy(addr.getAddress(), 0, dhtBytes, 0, addrLength);

		/*
		 * add the port
		 */
		byte[] portBytes = intToByteArray(port);
		dhtBytes[addrLength] = portBytes[2];
		dhtBytes[addrLength + 1] = portBytes[3];

		/*
		 * and the time stamp
		 */
		byte[] timeStamp = longToByteArray(time);
		System.arraycopy(timeStamp, 0, dhtBytes, addrLength + 2, timeStamp.length);

		return dhtBytes;
	}

	private static byte[] encryptAndSign(byte[] data, PublicKey friendsPublicKey) throws Exception {
		/*
		 * encrypt the payload so that only the friend can read it + verify that
		 * the data is correct
		 */
		byte[] encrypted = OneSwarmSslKeyManager.getInstance().encrypt(data, friendsPublicKey);
		if (encrypted.length > ENCRYPTED_LENGTH) {
			throw new Exception("encrypted data length > " + ENCRYPTED_LENGTH);
		}
		// System.out.println("length before encryption: " + data.length +
		// " after: " + encrypted.length);
		/*
		 * sign the entire thing (both length field and encrypted data)
		 */
		byte[] sign = OneSwarmSslKeyManager.getInstance().sign(encrypted);
		// System.out.println("signature length: " + sign.length);
		if (sign.length > SIGN_LENGTH) {
			throw new Exception("signature length > " + SIGN_LENGTH);
		}

		/*
		 * copy it into the final byte[]
		 */
		byte[] dhtValue = new byte[encrypted.length + sign.length];
		System.arraycopy(encrypted, 0, dhtValue, 0, encrypted.length);
		System.arraycopy(sign, 0, dhtValue, encrypted.length, sign.length);
		return dhtValue;
	}

	private static InetAddress getInetAddress(byte[] dhtValue) throws UnknownHostException {
		byte[] inetAddr = new byte[dhtValue.length - 10];
		System.arraycopy(dhtValue, 0, inetAddr, 0, inetAddr.length);
		return InetAddress.getByAddress(inetAddr);
	}

	// private static byte[] decryptWithoutVerify(byte[] data) throws Exception
	// {
	// int dataLen = byteArrayToInt(new byte[] { 0, 0, data[0], data[1] });
	// if (dataLen < 0 || dataLen > 1024) {
	// throw new
	// Exception("Got bad data (length of encrypted data specified as: " +
	// dataLen + ") full len=" + data.length);
	// }
	// byte[] decrypted = OneSwarmSslKeyManager.getInstance().decrypt(data, 2,
	// dataLen);
	// return decrypted;
	// }

	private static int getPort(byte[] dhtValue) {
		byte[] port = { 0, 0, dhtValue[dhtValue.length - 10], dhtValue[dhtValue.length - 9] };
		return byteArrayToInt(port);
	}

	private static long getTimeStamp(byte[] dhtValue) {
		byte[] b = new byte[8];
		System.arraycopy(dhtValue, dhtValue.length - 8, b, 0, 8);
		return byteArrayToLong(b);
	}

	private static byte[] intToByteArray(int v) {
		return new byte[] { (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v };
	}

	private static byte[] longToByteArray(long v) {
		byte[] b = new byte[8];
		b[0] = (byte) (v >>> 56);
		b[1] = (byte) (v >>> 48);
		b[2] = (byte) (v >>> 40);
		b[3] = (byte) (v >>> 32);
		b[4] = (byte) (v >>> 24);
		b[5] = (byte) (v >>> 16);
		b[6] = (byte) (v >>> 8);
		b[7] = (byte) (v >>> 0);
		return b;
	}

	public static void main(String[] args) {
		try {
			InetAddress a = InetAddress.getByName("www.cs.washington.edu");
			int port = 12303;
			long time = System.currentTimeMillis();

			System.out.println("input4=" + a.getHostAddress() + "\t" + port + "\t" + time);
			byte[] serialized = convertToIPPortTimeStamp(a, port, time);
			System.out.println("outpu4=" + getInetAddress(serialized).getHostAddress() + "\t" + getPort(serialized) + "\t" + getTimeStamp(serialized));
			byte[] encryptedAndSigned = encryptAndSign(serialized, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			byte[] decrypted = verifyAndDecrypt(encryptedAndSigned, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			System.out.println("data verified succesfully");
			System.out.println("decry4=" + getInetAddress(decrypted).getHostAddress() + "\t" + getPort(decrypted) + "\t" + getTimeStamp(decrypted));

			a = InetAddress.getByName("fe80::223:32ff:fed5:1f20");
			port = 17;
			time = System.currentTimeMillis();
			System.out.println("input6=" + a.getHostAddress() + "\t" + port + "\t" + time);
			serialized = convertToIPPortTimeStamp(a, port, time);
			System.out.println("outpu6=" + getInetAddress(serialized).getHostAddress() + "\t" + getPort(serialized) + "\t" + getTimeStamp(serialized));

			encryptedAndSigned = encryptAndSign(serialized, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			decrypted = verifyAndDecrypt(encryptedAndSigned, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			System.out.println("decry6=" + getInetAddress(decrypted).getHostAddress() + "\t" + getPort(decrypted) + "\t" + getTimeStamp(decrypted));

			System.out.print("testing data modification detection (expect exception)... ");
			byte oldval = encryptedAndSigned[0];
			encryptedAndSigned[0] = 100;
			try {
				decrypted = verifyAndDecrypt(encryptedAndSigned, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			} catch (Exception e) {
				System.out.println("test successful (error caught: " + e.getMessage() + ")");
			}
			encryptedAndSigned[0] = oldval;

			System.out.print("testing data modification detection (expect exception)... ");
			oldval = encryptedAndSigned[56];
			encryptedAndSigned[56] = 100;
			try {
				decrypted = verifyAndDecrypt(encryptedAndSigned, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			} catch (Exception e) {
				System.out.println("test successful (error caught: " + e.getMessage() + ")");
			}
			encryptedAndSigned[56] = oldval;

			System.out.print("testing data modification detection (expect exception)... ");
			encryptedAndSigned[149] = 100;
			try {
				decrypted = verifyAndDecrypt(encryptedAndSigned, OneSwarmSslKeyManager.getInstance().getOwnPublicKey());
			} catch (Exception e) {
				System.out.println("test successful (error caught: " + e.getMessage() + ")");
			}

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static byte[] shaAndXor(FriendInvitation invitation, InetAddress ip, int port) {
		int addrLength = ip.getAddress().length;
		byte[] dhtBytes = new byte[addrLength + 2];

		/*
		 * copy in the inet address
		 */
		System.arraycopy(ip.getAddress(), 0, dhtBytes, 0, addrLength);

		/*
		 * add the port
		 */
		byte[] portBytes = intToByteArray(port);
		dhtBytes[addrLength] = portBytes[2];
		dhtBytes[addrLength + 1] = portBytes[3];
		return invitation.xorDhtValue(dhtBytes);
	}

	private static byte[] verifyAndDecrypt(byte[] data, PublicKey friendsPublicKey) throws Exception {

		boolean verify = OneSwarmSslKeyManager.getInstance().signVerify(data, 0, ENCRYPTED_LENGTH, friendsPublicKey, data, ENCRYPTED_LENGTH, SIGN_LENGTH);
		if (!verify) {
			throw new Exception("Got bad data (signature did not verify)");
		}

		byte[] decrypted = OneSwarmSslKeyManager.getInstance().decrypt(data, 0, ENCRYPTED_LENGTH);
		return decrypted;
	}

	private class FriendConnectorRunnable extends TimerTask {
		private boolean firstDhtRunCompleted = false;
		private final Logger logger = Logger.getLogger(FriendConnectorRunnable.class.getName());

		@Override
		public void run() {

			logger.fine("Running friend connector");
			if (!overlayManager.getFilelistManager().isInitialFileListGenerated()) {
				logger.fine("skipping friend connector run, file list not done yet");
				return;
			}
			boolean returnedFromStandby = false;

			if ((System.currentTimeMillis() - lastConnectorRunTime) > 5 * 1000 + CONNECTOR_FREQUENCY) {
				returnedFromStandby = true;
				logger.finer("time travel detected (returned from standby?), friend connection attempt triggered");
			}

			synchronized (this) {
				logger.finest("Checking if dht is available");

				boolean firstDhtRun = false;
				final AtomicBoolean dhtAvailable = new AtomicBoolean(false);
				if (getDht().isAvailable()) {
					dhtAvailable.set(true);
					if (!firstDhtRunCompleted) {
						firstDhtRun = true;
						firstDhtRunCompleted = true;
					}
				}
				int attempts = 0;
				logger.finer("dht available=" + dhtAvailable);
				publishLocationInfo();
				logger.finer("publish completed, connecting to friends");

				// then try to connect to friends that are disconnected
				List<Friend> disconnectedFriends = overlayManager.getDisconnectedFriends();
				/*
				 * if the dht is unavailable, sort by last connect date
				 * 
				 * else sort by last dht lookup date
				 */
				Collections.sort(disconnectedFriends, new Comparator<Friend>() {
					@Override
					public int compare(Friend o1, Friend o2) {
						if (dhtAvailable.get() == false) {
							return lastConnectCompare(o1, o2);
						} else {
							/*
							 * if o1 never been looked up in the dht, do so
							 * first
							 */
							Long o1Checked = lastDhtLookupForFriend.get(o1);
							Long o2Checked = lastDhtLookupForFriend.get(o2);
							if (o1Checked == null && o2Checked == null) {
								// neither is checked, sort by last connect
								return lastConnectCompare(o1, o2);
							}
							if (o1Checked != null && o2Checked == null) {
								// o1 has been checked before
								return -1;
							} else if (o1Checked == null && o2Checked != null) {
								return 1;
							} else {
								/*
								 * sort by last dht lookup time
								 */
								return o1Checked.compareTo(o2Checked);
							}

						}
					}

					private int lastConnectCompare(Friend o1, Friend o2) {
						if (o1.getLastConnectDate() != null) {
							if (o2.getLastConnectDate() == null) {
								return -1;
							} else {
								return -1 * o1.getLastConnectDate().compareTo(o2.getLastConnectDate());
							}
						} else {
							return 1;
						}
					}
				});

				for (Friend friend : disconnectedFriends) {
					boolean connectAllowed;

					if (!friend.isBlocked()) {
						if (firstDhtRun) {
							/*
							 * we can connect if the dht just came up
							 */
							connectAllowed = true;
						} else if (returnedFromStandby) {
							/*
							 * or if we just returned from standby
							 */
							connectAllowed = true;
						} else if (connectAttemptAllowed(friend)) {
							/*
							 * or if we are allowed :-)
							 */
							connectAllowed = true;
						} else {
							connectAllowed = false;
						}

						if (connectAllowed) {
							connectToFriend(friend, dhtAvailable.get());
							attempts++;
						}
					}
				}
				logger.fine("friend connector task completed, attempts: " + attempts);
				lastConnectorRunTime = System.currentTimeMillis();
			}

		}
	}

	private class InvitationConnectorRunnable extends TimerTask {
		private boolean firstDhtRunCompleted = false;
		private final Logger logger = Logger.getLogger(InvitationConnectorRunnable.class.getName());

		@Override
		public void run() {

			logger.fine("Running invitation connector");
			if (!overlayManager.getFilelistManager().isInitialFileListGenerated()) {
				logger.fine("skipping invitation connector run, file list not done yet");
				return;
			}
			boolean returnedFromStandby = false;

			if ((System.currentTimeMillis() - lastConnectorRunTime) > 5 * 1000 + CONNECTOR_FREQUENCY) {
				returnedFromStandby = true;
				logger.fine("time travel detected (returned from standby?), friend connection attempt triggered");
			}

			synchronized (this) {
				logger.finest("Checking if dht is available");
				boolean firstDhtRun = false;
				boolean dhtAvailable = false;
				if (getDht().isAvailable()) {
					dhtAvailable = true;
					if (!firstDhtRunCompleted) {
						firstDhtRun = true;
						firstDhtRunCompleted = true;
					}
				}
				int attempts = 0;
				logger.fine("dht available=" + dhtAvailable);
				publishLocationInfo();
				logger.fine("publish completed, connecting to friends");

				// then try to connect to invitations
				List<FriendInvitation> invitations = invitationManager.getInvitations();

				for (FriendInvitation invitation : invitations) {
					boolean connectAllowed;

					if (firstDhtRun) {
						/*
						 * we can connect if the dht just came up
						 */
						connectAllowed = true;
					} else if (returnedFromStandby) {
						/*
						 * or if we just returned from standby
						 */
						connectAllowed = true;
					} else if (invitation.connectAttemptsAllowed()) {
						/*
						 * or if we are allowed :-)
						 */
						connectAllowed = true;
					} else {
						connectAllowed = false;
					}

					if (connectAllowed) {
						connectToInvitation(invitation);
					}

					logger.fine("friend connector task completed, attempts: " + attempts);
					lastConnectorRunTime = System.currentTimeMillis();
				}
			}

		}
	}
}
