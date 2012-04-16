/**
 * 
 */
package edu.washington.cs.publickey;

/**
 * @author isdal
 * 
 */
public enum FriendNetwork {
	XMPP_GOOGLE(0, "XMPP (Google)");
	private final static FriendNetwork[] val = { XMPP_GOOGLE };
	private final int networkId;

	private final String networkName;

	private FriendNetwork(int networkId, String networkName) {
		this.networkId = networkId;
		this.networkName = networkName;
	}

	public int getNetworkId() {
		return networkId;
	}

	public String getNetworkName() {
		return networkName;
	}

	public static FriendNetwork getFromId(int id) {
		if (id < val.length && id >= 0) {
			return val[id];
		}

		return null;
	}
}
