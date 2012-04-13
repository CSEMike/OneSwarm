/**
 * 
 */
package edu.washington.cs.publickey;

import java.io.IOException;
import java.util.Arrays;

import org.jivesoftware.smack.util.Base64;

/**
 * @author isdal
 * 
 */
public class PublicKeyFriend {
	public static PublicKeyFriend[] deserialize(String testFriendSerialized) throws IOException {
		PublicKeyFriendBean[] beans = PublicKeyFriendBean.deserialize(testFriendSerialized);
		PublicKeyFriend[] fks = new PublicKeyFriend[beans.length];
		for (int i = 0; i < fks.length; i++) {
			fks[i] = new PublicKeyFriend(beans[i]);
		}

		return fks;
	}

	public static String serialize(PublicKeyFriend[] f) throws IOException {
		PublicKeyFriendBean[] beans = new PublicKeyFriendBean[f.length];
		for (int i = 0; i < beans.length; i++) {
			beans[i] = f[i].getBean();
		}
		return PublicKeyFriendBean.serialize(beans);
	}

	private final PublicKeyFriendBean bean;

	private transient int hashCode = 0;

	public PublicKeyFriend() {
		bean = new PublicKeyFriendBean();
	}

	public PublicKeyFriend(PublicKeyFriendBean bean) {
		this.bean = bean;
	}

	PublicKeyFriendBean getBean() {
		return bean;
	}

	public String getKeyNick() {
		return bean.getKeyNick();
	}

	public byte[] getPublicKey() {
		if (bean.getPublicKey() == null) {
			return null;
		}
		return Base64.decode(bean.getPublicKey());
	}

	public byte[] getPublicKeySha1() {
		return Base64.decode(bean.getPublicKeySha1());
	}

	public String getRealName() {
		return bean.getRealName();
	}

	public FriendNetwork getSourceNetwork() {
		return FriendNetwork.getFromId(bean.getSourceNetwork());
	}

	public byte[] getSourceNetworkUid() {
		String sourceNetworkUid = bean.getSourceNetworkUid();
		if (sourceNetworkUid == null) {
			return null;
		}
		return Base64.decode(sourceNetworkUid);
	}

	public String serialize() throws IOException {
		return bean.serialize();
	}

	public void setKeyNick(String keyNick) {
		if (keyNick.length() > 255) {
			throw new RuntimeException("max key nick length is 255");
		}
		bean.setKeyNick(keyNick);
	}

	public void setPublicKey(byte[] publicKey) {
		bean.setPublicKey(Base64.encodeBytes(publicKey, Base64.DONT_BREAK_LINES));
	}

	public void setPublicKey(String publicKey) {
		setPublicKey(Base64.decode(publicKey));
	}

	public void setPublicKeySha1(byte[] publicKeySha1) {
		bean.setPublicKeySha1(Base64.encodeBytes(publicKeySha1, Base64.DONT_BREAK_LINES));
	}

	public void setRealName(String realName) {
		bean.setRealName(realName);
	}

	public void setSourceNetwork(FriendNetwork f) {
		bean.setSourceNetwork(f.getNetworkId());
	}

	public void setSourceNetworkUid(byte[] uid) {
		if (uid == null || uid.length != 20) {
			throw new RuntimeException("uid == null or uid.length != 20");
		}
		bean.setSourceNetworkUid(new String(Base64.encodeBytes(uid, Base64.DONT_BREAK_LINES)));
	}

	public String toString() {
		return bean.toString();
	}

	public boolean equals(Object o) {
		if (o.hashCode() == this.hashCode()) {
			if (o instanceof PublicKeyFriend) {
				PublicKeyFriend f = (PublicKeyFriend) o;
				if (f.getPublicKey() != null && this.getPublicKey() != null) {
					return Arrays.equals(f.getPublicKey(), this.getPublicKey());
				} else if (f.getSourceNetworkUid() != null && this.getSourceNetworkUid() != null) {
					return Arrays.equals(f.getSourceNetworkUid(), this.getSourceNetworkUid());
				}

			}
		}
		return false;
	}

	public int hashCode() {
		if (hashCode == 0) {
			if (getPublicKey() != null) {
				hashCode = Arrays.hashCode(getPublicKey());
			} else if (getSourceNetworkUid() != null) {
				hashCode = Arrays.hashCode(getSourceNetworkUid());
			}
		}

		return hashCode;
	}

}
