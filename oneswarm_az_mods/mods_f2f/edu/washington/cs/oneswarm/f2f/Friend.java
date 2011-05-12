package edu.washington.cs.oneswarm.f2f;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Debug;

public class Friend
{

	public final static int			KEY_BYTES_LENGTH						= 162;

	public final static String	 KEY_POST_STRING						 = "QAB";

	public final static String	 KEY_PRE_STRING							= "MIG";

	public final static int			KEY_STRING_LENGTH					 = 216;

	public final static int			MAX_CONNECTING_TIME				 = 15 * 1000;

	private static MessageDigest md;

	public static final int			NOT_CONNECTED_CONNECTION_ID = -1;

	public final static int			STATUS_CONNECTING					 = 1;

	public final static int			STATUS_HANDSHAKING					= 2;

	public final static int			STATUS_OFFLINE							= 0;

	public final static int			STATUS_ONLINE							 = 3;

	static {
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean							allowChat									 = true;

	private String							 bannedReason								= "";

	private boolean							blocked										 = false;

	private boolean							canSeeFileList							= true;

	private volatile int				 connectionId								= NOT_CONNECTED_CONNECTION_ID;

	private StringBuffer				 connectionLog							 = new StringBuffer();

	private Date								 dateAdded;

	private long								 friendBannedUntil					 = 0;

	private String							 group											 = null;

	private int									hash												= 0;

	private long								 lastAttempt;

	private Date								 lastConnectDate;

	private InetAddress					lastConnectIP;

	private int									lastConnectPort;

	private boolean							newFriend									 = false;

	private String							 nick;

	private final byte[]				 publicKey;

	private PublicKey						publicKeyObj;

	private boolean							requestFileList						 = true;

	private byte[]							 dhtWriteLocation;

	private byte[]							 dhtReadLocation;

	private boolean							dhtLocationConfirmed				= false;

	public boolean isDhtLocationConfirmed() {
		return dhtLocationConfirmed;
	}

	public void setDhtLocationConfirmed(boolean dhtLocationConfirmed) {
		this.dhtLocationConfirmed = dhtLocationConfirmed;
	}

	public byte[] getDhtWriteLocation() {
		return dhtWriteLocation;
	}

	public void setDhtWriteLocation(byte[] dhtWriteLocation) {
		this.dhtWriteLocation = dhtWriteLocation;
	}

	public byte[] getDhtReadLocation() {
		return dhtReadLocation;
	}

	public void setDhtReadLocation(byte[] dhtReadLocation) {
		this.dhtReadLocation = dhtReadLocation;
	}

	private final String			 sourceNetwork;

	private volatile int status										= 0;

	private boolean			supportsChat							= false;

	private boolean			supportsExtendedFileLists = false;

    private boolean supportsPuzzleMessages = false;

	private long				 totalDownloaded;

	private long				 totalDownloadSinceAppStart;

	private long				 totalUploaded;

	private long				 totalUploadSinceAppStart;

	public Friend(boolean canSeeFileList, boolean allowChat, Date dateAdded,
			Date lastConnectDate, InetAddress lastConnectIP, int lastConnectPort,
			String nick, byte[] publicKey, String sourceNetwork,
			long totalDownloaded, long totalUploaded, boolean blocked,
			boolean newFriend) {
		super();
		this.canSeeFileList = canSeeFileList;
		this.allowChat = allowChat;
		this.dateAdded = dateAdded;
		this.lastConnectDate = lastConnectDate;
		this.lastConnectIP = lastConnectIP;
		this.lastConnectPort = lastConnectPort;
		this.nick = nick;
		this.publicKey = publicKey;
		this.sourceNetwork = sourceNetwork;
		this.totalDownloaded = totalDownloaded;
		this.totalUploaded = totalUploaded;
		this.blocked = blocked;
		this.newFriend = newFriend;

		/**
		 * Fix this problem for old friends that were serialized before these bugs were fixed. 
		 */
		if (dateAdded == null) {
			dateAdded = new Date();
		}
	}

	//	public Friend(boolean canSeeFileList, Date dateAdded, Date lastConnectDate,
	//			InetAddress lastConnectIP, int lastConnectPort, String nick,
	//			byte[] publicKey, String sourceNetwork, long totalDownloaded,
	//			long totalUploaded, boolean blocked, boolean newFriend) {
	//		super();
	//		this.canSeeFileList = canSeeFileList;
	//		this.dateAdded = dateAdded;
	//		this.lastConnectDate = lastConnectDate;
	//		this.lastConnectIP = lastConnectIP;
	//		this.lastConnectPort = lastConnectPort;
	//		this.nick = nick;
	//		this.publicKey = publicKey;
	//		this.sourceNetwork = sourceNetwork;
	//		this.totalDownloaded = totalDownloaded;
	//		this.totalUploaded = totalUploaded;
	//		this.blocked = blocked;
	//		this.newFriend = newFriend;
	//		this.allowChat = true;
	//	}

	/**
	 * used for comparison purposes
	 * 
	 * @param publicKey
	 */

	public Friend(String sourceNetwork, String nick, byte[] publicKey,
			boolean canSeeFilelist) {
		this.sourceNetwork = sourceNetwork;
		this.nick = nick;
		this.publicKey = publicKey;
		this.dateAdded = new Date();
		this.canSeeFileList = canSeeFilelist;
		this.newFriend = true;
	}

	public Friend(String sourceNetwork, String nickName, String publicKey)
			throws InvalidKeyException {
		publicKey = publicKey.replaceAll("\\s+", "");
		if (nickName == null || nickName.length() == 0 || publicKey == null
				|| publicKey.length() == 0) {
			throw new InvalidKeyException(
					"Both Nick and public key must be non empty");
		}

		if (publicKey.length() != KEY_STRING_LENGTH) {
			throw new InvalidKeyException(
					"Public key has wrong length, did you paste it all? ("
							+ publicKey.length() + "!=" + KEY_STRING_LENGTH + ")");
		}

		if (!publicKey.startsWith(KEY_PRE_STRING)) {
			throw new InvalidKeyException("Public key usually starts with: '"
					+ KEY_PRE_STRING + "'");
		}
		if (!publicKey.endsWith(KEY_POST_STRING)) {
			throw new InvalidKeyException("Public key usually ends with: '"
					+ KEY_POST_STRING + "'");
		}

		this.publicKey = Base64.decode(publicKey);
		this.sourceNetwork = sourceNetwork;
		this.nick = nickName;

		if (this.publicKey.length != KEY_BYTES_LENGTH) {
			throw new InvalidKeyException(
					"Public key has wrong length, did you paste it all? ("
							+ this.publicKey.length + "!=" + KEY_BYTES_LENGTH + ")");
		}
		try {
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(this.publicKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			publicKeyObj = kf.generatePublic(publicKeySpec);
		} catch (Exception e) {
			throw new InvalidKeyException("Got error when creating public key: "
					+ e.getMessage());
		}

		this.blocked = true;
		this.canSeeFileList = false;
		this.newFriend = true;
	}

	public void connected() {
		synchronized (this) {
			if (this.status == Friend.STATUS_HANDSHAKING) {
				Debug.out("got connected event, even though we already are handshaking!");

			} else if (this.status == Friend.STATUS_ONLINE) {
				Debug.out("got connected event even though we are already connected");
			}
			status = STATUS_HANDSHAKING;
		}
	}

	public void connectionAttempt(String logMessage) {
		synchronized (this) {
			this.lastAttempt = System.currentTimeMillis();
			updateConnectionLog(false, logMessage);
			if (status != STATUS_ONLINE) {
				status = STATUS_CONNECTING;
			}
		}
	}

	public void disconnected(int connectionIdHash) {
		synchronized (this) {
			updateConnectionLog(true, connectionIdHash, "disconnected");
			connectionId = NOT_CONNECTED_CONNECTION_ID;
			status = STATUS_OFFLINE;
		}
	}

	@Override
    public boolean equals(Object obj) {
		if (obj instanceof Friend) {
			Friend comp = (Friend) obj;
			if (Arrays.equals(comp.getPublicKey(), this.getPublicKey())) {
				return true;
			}
		}
		return false;
	}

	public String getBannedReason() {
		return bannedReason;
	}

	public int getConnectionId() {
		synchronized (this) {
			return connectionId;
		}
	}

	public String getConnectionLog() {
		return connectionLog.toString();
	}

	public Date getDateAdded() {
		return dateAdded;
	}

	public long getFriendBannedUntil() {
		return friendBannedUntil;
	}

	public double getFriendScore() {
		return (double) totalDownloaded / (double) totalUploaded;
	}

	public String getGroup() {
		return group;
	}

	public Date getLastConnectDate() {
		return lastConnectDate;
	}

	public InetAddress getLastConnectIP() {
		return lastConnectIP;
	}

	public int getLastConnectPort() {
		return lastConnectPort;
	}

	public String getNick() {
		return nick;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public PublicKey getPublicKeyObj() throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		if (publicKeyObj == null) {
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(this.publicKey);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			publicKeyObj = kf.generatePublic(publicKeySpec);
		}
		return publicKeyObj;
	}

	public String getSourceNetwork() {
		return sourceNetwork;
	}

	public int getStatus() {
		synchronized (this) {
			if (status == STATUS_CONNECTING) {
				if (msSinceLastAttempt() > MAX_CONNECTING_TIME) {
					status = STATUS_OFFLINE;
				}
			}
			return status;
		}
	}

	public long getTotalDownloaded() {
		return totalDownloaded;
	}

	public long getTotalDownloadSinceAppStart() {
		return totalDownloadSinceAppStart;
	}

	public long getTotalUploaded() {
		return totalUploaded;
	}

	public long getTotalUploadSinceAppStart() {
		return totalUploadSinceAppStart;
	}

    public void handShakeCompleted(int connectionHash, boolean extendedFileLists, boolean chat,
            boolean puzzles) {
		synchronized (this) {
			if (this.connectionId != NOT_CONNECTED_CONNECTION_ID) {
				Debug.out("got Friend.handShakeCompleted even though the connection id isn't 'not connected'");
				System.err.println("old=" + this.connectionId + " new=" + connectionId);
				System.err.println("friend: " + this.toString());
			}
			status = STATUS_ONLINE;
			this.connectionId = connectionHash;
			this.supportsChat = chat;
			this.supportsExtendedFileLists = extendedFileLists;
            this.supportsPuzzleMessages = puzzles;

			updateConnectionLog(true, connectionHash, "handshake completed");
		}
	}

	@Override
    public int hashCode() {
		if (hash == 0) {
			hash = Arrays.hashCode(publicKey);
		}
		return hash;
	}

	public boolean isAllowChat() {
		return allowChat;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public boolean isCanSeeFileList() {
		return canSeeFileList;
	}

	public boolean isConnected() {
		return this.getStatus() == Friend.STATUS_ONLINE;
	}

	public boolean isNewFriend() {
		return newFriend;
	}

	public boolean isRequestFileList() {
		return requestFileList;
	}

	public boolean isSupportsChat() {
		return supportsChat;
	}

	public boolean isSupportsExtendedFileLists() {
		return supportsExtendedFileLists;
	}

	public long msSinceLastAttempt() {
		return System.currentTimeMillis() - lastAttempt;
	}

	public void setAllowChat(boolean allowChat) {
		this.allowChat = allowChat;
	}

	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
	}

	public void setCanSeeFileList(boolean canSeeFileList) {
		this.canSeeFileList = canSeeFileList;
	}

	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	public void setDateAdded(Date inDate) {
		dateAdded = inDate;
	}

	public void setFriendBannedUntil(String reason, long until) {
		this.bannedReason = reason;
		this.friendBannedUntil = until;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public void setLastConnectIP(InetAddress lastConnectIP) {
		this.lastConnectIP = lastConnectIP;
	}

	public void setLastConnectPort(int lastConnectPort) {
		this.lastConnectPort = lastConnectPort;
	}

	public void setNewFriend(boolean newFriend) {
		this.newFriend = newFriend;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public void setRequestFileList(boolean requestFileList) {
		this.requestFileList = requestFileList;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
    public String toString() {
		return nick + " (" + sourceNetwork + ")";
	}

	public void updateConnectedDate() {
		this.lastConnectDate = new Date();
	}

	public void updateConnectionLog(boolean append, int connectionHashId,
			String message) {
		if (!append || connectionLog == null) {
			connectionLog = new StringBuffer();
		}
		Date d = new Date();
		DateFormat f = new SimpleDateFormat("hh:mm:ss");//SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
		if (connectionHashId != 0) {
			connectionLog.append(f.format(d) + " "
					+ Integer.toHexString(connectionHashId) + ":  " + message + "\n");
		} else {
			connectionLog.append(f.format(d) + ":  " + message + "\n");

		}
	}

	public void updateConnectionLog(boolean append, String message) {
		updateConnectionLog(append, 0, message);
	}

	public void updateDownloaded(long downloaded) {
		if (downloaded > 0) {
			updateConnectedDate();
		}
		this.totalDownloaded += downloaded;
		this.totalDownloadSinceAppStart += downloaded;
	}

	public void updateUploaded(long uploaded) {
		if (uploaded > 0) {
			updateConnectedDate();
		}
		this.totalUploaded += uploaded;
		this.totalUploadSinceAppStart += uploaded;
	}

	public static byte[] getSha1(byte[] bytes) throws NoSuchAlgorithmException,
			UnsupportedEncodingException {
		md.update(bytes, 0, bytes.length);
		byte[] sha1hash = md.digest();
		md.reset();
		return sha1hash;
	}
}
