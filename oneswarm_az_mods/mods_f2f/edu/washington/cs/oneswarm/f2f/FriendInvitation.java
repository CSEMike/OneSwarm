/**
 * 
 */
package edu.washington.cs.oneswarm.f2f;

import java.util.Arrays;
import java.util.Date;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.gudy.azureus2.core3.util.SHA1Simple;

public class FriendInvitation
{

	public final static int		INV_KEY_LENGTH										 = 36;

	public final static int		INV_PUB_KEY_VERIFICATION_LENGTH		= 16;

	public final static int		INV_DHT_KEY_LENGTH								 = 26;

	public final static int		INV_DHT_VALUE_XOR_START						= INV_PUB_KEY_VERIFICATION_LENGTH;

	public final static int		INV_DHT_VALUE_XOR_LENGTH					 = 10;

	public final static int		INV_SECURITY_TYPE_POS							= INV_KEY_LENGTH - 1;

	public final static byte[] NOUNCE_PUBLISH_CREATED_INVITATION	= Base64.decode("GNvEsbbVLWOXtNLsmRQak0PXEpGnAwRamKsf5yQXSoYjVfEVT1aV2zDIyPcA7ttTbpdAQDj6Ye79NVLc30LAf0Z0TSPtHO4v17zd5vL0j0F7FVJagx2Ywx5c4O4zFKD9vRQmklMuiKfGRYlJP4FK9kyLjg3WKe8WeOY5HaPIA585Rk50rb4Ta8QtvG5s1pAl74CJkshg");

	public final static byte[] NOUNCE_PUBLISH_REDEEMED_INVITATION = Base64.decode("lmLdVREVe2HFdenxRFa9pCXWLBmTbpvAk3FVBdtAc4Hp2BSIaDLjfnSt6ByA04x0nrur0zCJWIVP21pnuMPEBOx3WzFYWHKBAzJ9vb6BXCjBh6xoteWRdXylhFowvZwJ33MwTsvD1K7X9d6ZBooxsyF3CK2gXUvJQWZoAB413rOsGAtIsdGRwnCa6n2GAT1CloDuQub1");

	public static final int		SECURITY_LEVEL_LOW								 = 0;

	public static final int		SECURITY_LEVEL_PIN								 = 1;

	private boolean						canSeeFileList;

	private long							 createdDate;

	private boolean						hasChanged												 = false;

	private final byte[]			 key;

	private long							 lastConnectDate;

	private String						 lastConnectIp;

	private int								lastConnectPort										= 0;

	private long							 maxAge;

	private String						 name;

	private byte[]						 remotePublicKey;

	private int								securityLevel;

	private boolean						createdLocally;

	private Status						 status														 = Status.STATUS_NEW;

	private transient long		 lastConnectAttempt								 = 0;

	private static long				MIN_MS_BETWEEN_CONNECT_ATTEMPTS		= 60 * 1000;

	public void connectAttempted() {
		lastConnectAttempt = System.currentTimeMillis();
	}

	public boolean connectAttemptsAllowed() {
		long age = System.currentTimeMillis() - lastConnectAttempt;
		boolean rateOk = age > MIN_MS_BETWEEN_CONNECT_ATTEMPTS;
		boolean stateOk = hasConnectableStatus();
		return rateOk && stateOk;
	}

	public boolean hasConnectableStatus() {
		boolean stateOk = status.getCode() >= FriendInvitation.Status.STATUS_NEW.getCode()
				&& status.getCode() < FriendInvitation.Status.STATUS_SUCCESS.getCode();
		return stateOk;
	}

	public boolean isStillValid() {
		long age = System.currentTimeMillis() - getCreatedDate();
		if (age > maxAge) {
			return false;
		}
		if (!hasConnectableStatus()) {
			return false;
		}
		return true;
	}

	public FriendInvitation(byte[] key) {
		this.key = key;

	}

	public long getCreatedDate() {
		return createdDate;
	}

	public byte[] getKey() {
		return key;
	}

	public byte[] getDHTKeyBase(boolean creatorAddress) {
		/*
		 * the base we use for keys is the first 30 bytes of the invite appended
		 * with a nounce. Different nounces are used depending on wether we
		 * created the invite or redeemed it
		 */
		int nounceLength = FriendInvitation.NOUNCE_PUBLISH_CREATED_INVITATION.length;

		byte[] keyBase = new byte[FriendInvitation.INV_DHT_KEY_LENGTH
				+ nounceLength];
		System.arraycopy(getKey(), 0, keyBase, 0,
				FriendInvitation.INV_DHT_KEY_LENGTH);
		if (creatorAddress) {
			System.arraycopy(FriendInvitation.NOUNCE_PUBLISH_CREATED_INVITATION, 0,
					keyBase, FriendInvitation.INV_DHT_KEY_LENGTH, nounceLength);
		} else {
			System.arraycopy(FriendInvitation.NOUNCE_PUBLISH_REDEEMED_INVITATION, 0,
					keyBase, FriendInvitation.INV_DHT_KEY_LENGTH, nounceLength);
		}
		return keyBase;
	}

	public byte[] xorDhtValue(byte[] value) {
		byte[] sha1 = new SHA1Simple().calculateHash(getKey(),
				FriendInvitation.INV_DHT_VALUE_XOR_START,
				FriendInvitation.INV_DHT_VALUE_XOR_LENGTH);
		byte[] xored = xor(value, sha1, 0);
		return xored;
	}

	private static byte[] xor(byte[] value, byte[] key, int keyOffset) {
		byte[] xored = new byte[value.length];
		for (int i = 0; i < value.length; i++) {
			xored[i] = (byte) (value[i] ^ key[keyOffset + i]);
		}
		return xored;
	}

	public long getLastConnectDate() {
		return lastConnectDate;
	}

	public String getLastConnectIp() {
		return lastConnectIp;
	}

	public int getLastConnectPort() {
		return lastConnectPort;
	}

	public long getMaxAge() {
		return maxAge;
	}

	public String getName() {
		return name;
	}

	public byte[] getRemotePublicKey() {
		return remotePublicKey;
	}

	public int getSecurityLevel() {
		return securityLevel;
	}

	public Status getStatus() {
		if (createdLocally) {
			if (status != Status.STATUS_SUCCESS
					&& System.currentTimeMillis() > createdDate + maxAge) {
				status = Status.STATUS_EXPIRED;
			}
		}
		return status;
	}

	public boolean isCanSeeFileList() {
		return canSeeFileList;
	}

	public boolean isHasChanged() {
		return hasChanged;
	}

	public boolean isCreatedLocally() {
		return createdLocally;
	}

	public boolean isRedeemed() {
		return !createdLocally;
	}

	public boolean keyEquals(byte[] key2) {
		if (key2 == null) {
			return false;
		}
		return Arrays.equals(key2, this.key);
	}

	public boolean pubKeyMatch(byte[] remotePublicKey) {
		byte[] remoteKeySha = new SHA1Hasher().calculateHash(remotePublicKey);
		// the lower INV_PUB_KEY_VERIFICATION_LENGTH bytes of the remote public 
		// key sha1 is the lower INV_PUB_KEY_VERIFICATION_LENGTH bytes of the invite
		if (remoteKeySha.length != 20) {
			System.err.println("wrong len: " + remoteKeySha.length + "!=20");
			return false;
		}
		for (int i = 0; i < INV_PUB_KEY_VERIFICATION_LENGTH; i++) {
			if (key[i] != remoteKeySha[i]) {
				//				System.err.println("wrong byte on pos " + i + " " + key[i] + "!="
				//						+ remoteKeySha[i]);
				//				System.err.println("key=" + Base32.encode(key));
				//				System.err.println("pubkeysha=" + Base32.encode(remoteKeySha));
				return false;
			}
		}
		return true;
	}

	public void setCanSeeFileList(boolean canSeeFileList) {
		this.canSeeFileList = canSeeFileList;
	}

	public void setCreatedDate(long date) {
		this.createdDate = date;
	}

	public void setHasChanged(boolean hasChanged) {
		this.hasChanged = hasChanged;
	}

	public void setLastConnectDate(long lastConnectDate) {
		this.lastConnectDate = lastConnectDate;
	}

	public void setLastConnectIp(String lastConnectIp) {
		this.lastConnectIp = lastConnectIp;
	}

	public void setLastConnectPort(int port) {
		this.lastConnectPort = port;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRemotePublicKey(byte[] remotePublicKey) {
		this.remotePublicKey = remotePublicKey;
	}

	public void setSecurityLevel(int securityLevel) {
		this.securityLevel = securityLevel;
	}

	public void setCreatedLocally(boolean sent) {
		this.createdLocally = sent;
	}

	public void setStatus(Status newStatus) {
		status = newStatus;
	}

	public String toString() {
		return name + " max_age=" + maxAge + " date=" + new Date(createdDate)
				+ " status=" + status.getDisplayString() + " sent=" + createdLocally
				+ " security_level=" + securityLevel;
	}

	public static enum Status {

		STATUS_ACCESS_DENIED(-2, "Access Denied"), STATUS_AUTHENTICATED(4,
				"Authenticated"), STATUS_CONNECTED(3, "Connected"), STATUS_EXPIRED(-1,
				"Expired"), STATUS_INVALID(-3, "Invalid"), STATUS_IP_PORT_LOCATED(2,
				"Ip located"), STATUS_IP_PORT_PUBLISHED(1, "Ip published"), STATUS_NEW(
				0, "New"), STATUS_SUCCESS(5, "Success");

		private final int		code;

		private final String displayString;

		Status(int code, String displayString) {
			this.code = code;
			this.displayString = displayString;
		}

		public int getCode() {
			return code;
		}

		public String getDisplayString() {
			return displayString;
		}

		public static Status getFromCode(int code) {

			if (code == STATUS_NEW.code) {
				return STATUS_NEW;
			}

			else if (code == STATUS_IP_PORT_PUBLISHED.code) {
				return STATUS_IP_PORT_PUBLISHED;
			}

			else if (code == STATUS_IP_PORT_LOCATED.code) {
				return STATUS_IP_PORT_LOCATED;
			}

			else if (code == STATUS_CONNECTED.code) {
				return STATUS_CONNECTED;
			}

			else if (code == STATUS_AUTHENTICATED.code) {
				return STATUS_AUTHENTICATED;
			}

			else if (code == STATUS_SUCCESS.code) {
				return STATUS_SUCCESS;
			}

			else if (code == STATUS_EXPIRED.code) {
				return STATUS_EXPIRED;
			}

			else if (code == STATUS_ACCESS_DENIED.code) {
				return STATUS_ACCESS_DENIED;
			}

			else if (code == STATUS_INVALID.code) {
				return STATUS_INVALID;
			}

			else {
				Debug.out("unknown status code: " + code);
				return STATUS_INVALID;
			}
		}
	}
}
