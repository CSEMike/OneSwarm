/**
 * 
 */
package edu.washington.cs.publickey;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author isdal
 * 
 */
public class PublicKeyFriendBean {
	public static boolean logToStdOut = false;

	public static PublicKeyFriendBean[] deserialize(String raw) throws IOException {
		log("'" + raw + "'");
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader beanClassLoader = PublicKeyFriendBean.class.getClassLoader();
		log("using cl " + oldClassLoader);
		if (!oldClassLoader.equals(beanClassLoader)) {
			log("setting class loader to: " + beanClassLoader);
			Thread.currentThread().setContextClassLoader(beanClassLoader);
		}
		if (raw.length() == 0) {
			return new PublicKeyFriendBean[0];
		}
		PublicKeyFriendBean[] res = null;
		ByteArrayInputStream in = new ByteArrayInputStream(raw.getBytes("UTF-8"));
		XMLDecoder d = new XMLDecoder(in);

		Object o = d.readObject();
		if (o instanceof PublicKeyFriendBean[]) {
			res = ((PublicKeyFriendBean[]) o);
		}
		in.close();
		if (!oldClassLoader.equals(beanClassLoader)) {
			log("setting old classloader: " + oldClassLoader);
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
		return res;
	}

	private static void log(String msg) {
		if (logToStdOut) {
			System.out.println(msg);
		}
	}

	public static String serialize(PublicKeyFriendBean[] f) throws IOException {
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader beanClassLoader = PublicKeyFriendBean.class.getClassLoader();
		log("using cl " + oldClassLoader);
		if (!oldClassLoader.equals(beanClassLoader)) {
			log("setting class loader to: " + beanClassLoader);
			Thread.currentThread().setContextClassLoader(beanClassLoader);
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLEncoder e = new XMLEncoder(out);
		e.writeObject(f);
		e.close();
		out.close();
		String ret = new String(out.toByteArray(), "UTF-8");
		log("'" + ret + "'");
		if (!oldClassLoader.equals(beanClassLoader)) {
			log("setting old classloader: " + oldClassLoader);
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
		return ret;
	}

	private String keyNick;
	private String publicKey;
	private String publicKeySha1;

	private String realName;

	private int sourceNetwork;

	private String sourceNetworkUid;

	public boolean equals(Object o) {
		if (o != null) {
			if (o instanceof PublicKeyFriendBean) {
				PublicKeyFriendBean c = (PublicKeyFriendBean) o;
				if (!publicKey.equals(c.getPublicKey())) {
					log("public key not same");
					log("'" + publicKey + "'");
					log("'" + c.getPublicKey() + "'");
					return false;
				}
				if (sourceNetwork != c.getSourceNetwork()) {
					log("source net not same");
					return false;
				}
				if (!sourceNetworkUid.equals(c.getSourceNetworkUid())) {
					log("net uid not same");
					log("'" + sourceNetworkUid + "'");
					log("'" + c.getSourceNetworkUid() + "'");
					return false;
				}
				if (!keyNick.equals(c.getKeyNick())) {
					log("key nick not same");
					return false;
				}

				return true;
			}
		}

		return false;
	}

	public String getKeyNick() {
		return keyNick;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getPublicKeySha1() {
		return publicKeySha1;
	}

	public String getRealName() {
		return realName;
	}

	public int getSourceNetwork() {
		return sourceNetwork;
	}

	public String getSourceNetworkUid() {
		return sourceNetworkUid;
	}

	// public static PublicKeyFriendBean[] deserialize(String raw) throws
	// IOException {
	// return deserialize(raw, Thread.currentThread().getContextClassLoader());
	// }

	public String serialize() throws IOException {
		return serialize(new PublicKeyFriendBean[] { this });
	}

	// public static String serialize(PublicKeyFriendBean[] f) throws
	// IOException {
	// return serialize(f, Thread.currentThread().getContextClassLoader());
	// }

	public void setKeyNick(String keyNick) {
		this.keyNick = keyNick;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}

	public void setPublicKeySha1(String publicKeySha1) {
		this.publicKeySha1 = publicKeySha1;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public void setSourceNetwork(int sourceNetwork) {
		this.sourceNetwork = sourceNetwork;
	}

	public void setSourceNetworkUid(String sourceNetworkUid) {
		this.sourceNetworkUid = sourceNetworkUid;
	}

	public String toString() {
		return realName + " (" + keyNick + ") " + sourceNetwork + " " + sourceNetworkUid + " " + publicKey;
	}
}
