package edu.washington.cs.oneswarm.f2f.invitations;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SHA1Simple;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.FriendInvitation.Status;
import edu.washington.cs.oneswarm.f2f.friends.FriendManager;
import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthMessageFatory;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanReader;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanWriter;
import edu.washington.cs.oneswarm.f2f.xml.OSF2FXMLBeanReader.OSF2FXMLBeanReaderCallback;

public class InvitationManager {

	private static final long BANNED_PERIOD = 60 * 60 * 1000;

	private static final String OSF2F_INVITE_FILE = "osf2f.invites";
	private static Logger logger = Logger.getLogger(InvitationManager.class.getName());

	private Map<String, InvitationConnection> authConnections = new HashMap<String, InvitationConnection>();
	private Map<String, Long> bannedIps = new HashMap<String, Long>();
	private AuthCallback callback = new AuthCallback() {

		public void authenticated(FriendInvitation invitation) throws InvalidKeyException {
			synchronized (InvitationManager.this) {
				Friend f = new Friend("Invited", invitation.getName(), new String(Base64.encode(invitation.getRemotePublicKey())));

				f.setCanSeeFileList(invitation.isCanSeeFileList());
				f.setRequestFileList(invitation.isCanSeeFileList());
				f.setBlocked(false);
				f.setAllowChat(true);
				
				f.setDateAdded(new Date());
				try {
					f.setLastConnectIP(InetAddress.getByName(invitation.getLastConnectIp()));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				f.setLastConnectPort(invitation.getLastConnectPort());
				f.setAllowChat(true);

				friendManager.addFriend(f);
				invitation.setStatus(Status.STATUS_SUCCESS);

				logger.fine("adding friend: " + f);
			}
		}

		public void banIp(String remoteIP) {
			synchronized (InvitationManager.this) {
				bannedIps.put(remoteIP, System.currentTimeMillis());
			}
		}

		public void closed(InvitationConnection conn) {
			synchronized (InvitationManager.this) {
				authConnections.remove(conn.getRemoteIp().getHostAddress());
			}
		}

		public FriendInvitation getInvitationFromInviteKey(HashWrapper key) {
			synchronized (InvitationManager.this) {
				FriendInvitation friendInvitation = invitations.get(key);
				if (friendInvitation != null) {
					if (friendInvitation.isCreatedLocally() && friendInvitation.isStillValid()) {
						return friendInvitation;
					}
				}
				return null;
			}
		}

		/**
		 * this is for getting the invitation if we get an incoming connection
		 * for an invite that we redeem
		 */
		public FriendInvitation getInvitationFromPublicKey(byte[] remotePublicKey) {
			FriendInvitation invitation = InvitationManager.this.getInvitationFromPublicKey(remotePublicKey);
			if (invitation != null && invitation.isStillValid()) {
				return invitation;
			}
			return null;
		}

	};

	public FriendInvitation getInvitationFromPublicKey(byte[] remotePublicKey) {
		synchronized (InvitationManager.this) {
			for (FriendInvitation i : invitations.values()) {
				if (i.isRedeemed()) {
					if (i.pubKeyMatch(remotePublicKey)) {
						return i;
					}
				}
			}
			return null;
		}
	}

	private final ClassLoader cl;

	private FriendManager friendManager;
	private HashMap<HashWrapper, FriendInvitation> invitations = new HashMap<HashWrapper, FriendInvitation>();
	private final byte[] localPublicKey;
	private final SecureRandom random;

	private final Semaphore diskSemaphore = new Semaphore(1);

	public InvitationManager(ClassLoader classLoader, FriendManager friendManager, byte[] localPublicKey) {
		this.friendManager = friendManager;
		this.localPublicKey = localPublicKey;
		this.random = new SecureRandom();
		this.cl = classLoader;
		OSF2FAuthMessageFatory.init();
		readFromDisk();

		Timer timer = new Timer("invitation write timer", true);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				flushToDisk(false, true, false);
			}
		}, 5 * 60 * 1000, 2 * 60 * 1000);

		registerShutdownHook();
	}

	public void connectToInvitations() {
		this.waitForRead();
		for (FriendInvitation invitation : invitations.values()) {
			if (invitation.getStatus().getCode() >= 0 && invitation.getStatus() != Status.STATUS_SUCCESS) {
				OSF2FMain.getSingelton().getDHTConnector().connectToInvitation(invitation);
			}
		}
	}

	private void registerShutdownHook() {
		AzureusCoreImpl.getSingleton().addLifecycleListener(new AzureusCoreLifecycleListener() {

			public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
				// TODO Auto-generated method stub

			}

			public boolean restartRequested(AzureusCore core) throws AzureusCoreException {
				// TODO Auto-generated method stub
				return false;
			}

			public void started(AzureusCore core) {
				// TODO Auto-generated method stub

			}

			public void stopped(AzureusCore core) {
				// TODO Auto-generated method stub

			}

			public void stopping(AzureusCore core) {
				logger.fine("stopping, ");
				flushToDisk(false, true, false);

			}

			public boolean stopRequested(AzureusCore core) throws AzureusCoreException {
				// System.out
				// .println("stop requested, flushing friends to disk");
				// flushToDisk(false);
				return true;
			}

			public boolean syncInvokeRequired() {
				// TODO Auto-generated method stub
				return false;
			}
		});
	}

	public FriendInvitation createInvitation(String name, boolean canSeeFileList, long maxAge, byte securityLevel) {

		FriendInvitation invitation = new FriendInvitation(generateNewInvitationKey(securityLevel));
		invitation.setName(name);
		invitation.setCanSeeFileList(canSeeFileList);
		invitation.setMaxAge(maxAge);
		invitation.setSecurityLevel(securityLevel);
		invitation.setCreatedLocally(true);
		invitation.setStatus(Status.STATUS_NEW);
		invitation.setCreatedDate(System.currentTimeMillis());

		this.waitForRead();
		synchronized (InvitationManager.this) {
			this.invitations.put(new HashWrapper(invitation.getKey()), invitation);
			OSF2FMain.getSingelton().getDHTConnector().publishInvitation(invitation);
			logger.fine("creating invitation: num=" + invitations.size());
			logger.finer(invitation.toString());
		}

		flushToDisk(true, false, false);
		return invitation;
	}

	public void deleteInvitation(FriendInvitation invitation) {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			this.invitations.remove(new HashWrapper(invitation.getKey()));
		}
		flushToDisk(true, false, true);
	}

	public List<FriendInvitation> getInvitations() {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			ArrayList<FriendInvitation> i = new ArrayList<FriendInvitation>();
			i.addAll(invitations.values());
			return i;
		}
	}

	private byte[] generateNewInvitationKey(byte securityLevel) {
		// the invitation code is INV_DHT_KEY_LENGTH bytes long
		byte[] key = new byte[FriendInvitation.INV_KEY_LENGTH];

		// fill it with random bytes
		random.nextBytes(key);

		// the first INV_PUB_KEY_VERIFICATION_LENGTH bytes in the key is the
		// lower INV_PUB_KEY_VERIFICATION_LENGTH bytes of the sha1 of
		// the local public key
		byte[] localPubKeySha1 = new SHA1Simple().calculateHash(localPublicKey);
		logger.finest("local public key: " + new String(Base64.encode(localPublicKey)));
		logger.finest("local pubkeysha1: " + Base32.encode(localPubKeySha1));
		System.arraycopy(localPubKeySha1, 0, key, 0, FriendInvitation.INV_PUB_KEY_VERIFICATION_LENGTH);

		/*
		 * set the security level in the last field so the receiver knows
		 */
		key[FriendInvitation.INV_SECURITY_TYPE_POS] = securityLevel;
		logger.finer("invitekey=" + Base32.encode(key));
		return key;
	}

	public FriendInvitation getInvitation(HashWrapper key) {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			return invitations.get(key);
		}
	}

	public List<FriendInvitation> getLocallyCreatedInvitations() {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			List<FriendInvitation> inv = new LinkedList<FriendInvitation>();
			for (FriendInvitation friendInvitation : invitations.values()) {
				if (friendInvitation.isCreatedLocally()) {
					inv.add(friendInvitation);
				}
			}
			return inv;
		}

	}

	public List<FriendInvitation> getRedeemedInvitations() {
		this.waitForRead();
		List<FriendInvitation> inv = new ArrayList<FriendInvitation>();
		for (FriendInvitation friendInvitation : invitations.values()) {
			if (friendInvitation.isRedeemed()) {
				inv.add(friendInvitation);
			}
		}
		return inv;
	}

	public boolean newIncomingConnection(byte[] publicKey, NetworkConnection netConn) {

		synchronized (InvitationManager.this) {
			String remoteIp = netConn.getEndpoint().getNotionalAddress().getAddress().getHostAddress();
			logger.fine("new incoming auth connetion: " + remoteIp);
			/*
			 * first, check if banned
			 */
			Long lastBanned = bannedIps.get(remoteIp);
			if (lastBanned != null && System.currentTimeMillis() < lastBanned + BANNED_PERIOD) {
				logger.fine("banned ip (" + remoteIp + "), closing");
				return false;
			}
			if (authConnections.containsKey(remoteIp)) {
				logger.fine("already connected to ip (" + remoteIp + "), closing");
				return false;
			}
			authConnections.put(remoteIp, new InvitationConnection(publicKey, netConn, callback));
			return true;
		}
	}

	public boolean newOutgoingConnection(ConnectionEndpoint remoteFriendAddr, FriendInvitation invitation) {

		synchronized (InvitationManager.this) {
			InetSocketAddress notionalAddress = remoteFriendAddr.getNotionalAddress();
			InetAddress address = notionalAddress.getAddress();
			String remoteIp = address.getHostAddress();
			logger.fine("making outgoing auth connection to: " + remoteIp);
			if (authConnections.containsKey(remoteIp)) {
				return false;
			}
			authConnections.put(remoteIp, new InvitationConnection(remoteFriendAddr, invitation, callback));
			return true;
		}
	}

	public void redeemInvitation(FriendInvitation invitation, boolean testOnly) throws Exception {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			HashWrapper key = new HashWrapper(invitation.getKey());
			if (invitation.pubKeyMatch(localPublicKey)) {
				throw new Exception("Invitation created by this computer");
			}

			if (invitations.get(key) != null) {
				throw new Exception("Invitation already redeemed");
			}

			if (!testOnly) {
				invitations.put(key, invitation);
				OSF2FMain.getSingelton().getDHTConnector().publishInvitation(invitation);
				OSF2FMain.getSingelton().getDHTConnector().connectToInvitation(invitation);
				flushToDisk(true, false, false);
			}
		}
	}

	public void updateInvitation(FriendInvitation invitation) {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			HashWrapper key = new HashWrapper(invitation.getKey());
			if (this.invitations.containsKey(key)) {
				this.invitations.put(key, invitation);
			} else {
				throw new RuntimeException("Unable to update invitation, not found");
			}
		}
	}

	private void flushToDisk(boolean makeBackup, boolean block, boolean allowDecreasedSize) {
		this.waitForRead();
		synchronized (InvitationManager.this) {
			InvitationBean[] b = new InvitationBean[invitations.size()];
			FriendInvitation[] f = invitations.values().toArray(new FriendInvitation[invitations.size()]);
			for (int i = 0; i < b.length; i++) {
				b[i] = InvitationBean.createBean(f[i]);
			}

			Thread t = new Thread(new OSF2FXMLBeanWriter<InvitationBean>(b, diskSemaphore, OSF2F_INVITE_FILE, makeBackup, allowDecreasedSize));
			t.setName("OSF2FXMLBeanWriter - flushToDisk (inviationbean)");
			t.setContextClassLoader(cl);

			t.start();
			if (block) {
				try {
					t.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private boolean readCompleted = false;

	private void waitForRead() {
		if (!readCompleted) {
			try {
				diskSemaphore.acquire();
				diskSemaphore.release();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			readCompleted = true;
		}
	}

	private void readFromDisk() {
		try {
			diskSemaphore.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OSF2FXMLBeanReader<InvitationBean> reader = new OSF2FXMLBeanReader<InvitationBean>(cl, InvitationBean.class, OSF2F_INVITE_FILE, diskSemaphore, new OSF2FXMLBeanReaderCallback<InvitationBean>() {
			public void readObject(InvitationBean object) {
				FriendInvitation invitation = InvitationBean.getInvitation(object);
				invitations.put(new HashWrapper(invitation.getKey()), invitation);
			}

			public void completed() {
				// TODO Auto-generated method stub
				
			}
		});
		Thread t = new Thread(reader);
		t.setDaemon(true);
		t.start();
		// reader.run();
	}

	public interface AuthCallback {
		public void authenticated(FriendInvitation invitation) throws InvalidKeyException;

		public void banIp(String remoteIP);

		public void closed(InvitationConnection conn);

		public FriendInvitation getInvitationFromInviteKey(HashWrapper key);

		public FriendInvitation getInvitationFromPublicKey(byte[] remotePublicKey);
	}
}
