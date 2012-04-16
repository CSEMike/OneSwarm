/**
 * 
 */
package edu.washington.cs.publickey.xmpp;

import edu.washington.cs.publickey.FriendNetwork;

public class XMPPNetwork {

	public static XMPPNetwork GTALK = new XMPPNetwork(FriendNetwork.XMPP_GOOGLE, "talk.google.com", 5222, "gmail.com");
	public static XMPPNetwork[] networks = new XMPPNetwork[] { GTALK };

	private final String serverAddr;
	private final int serverPort;
	private final String serviceName;
	private final FriendNetwork friendNetwork;

	private XMPPNetwork(FriendNetwork friendNetwork, String _server_addr, int _server_port, String _service_name) {
		this.friendNetwork = friendNetwork;
		this.serverAddr = _server_addr;
		this.serverPort = _server_port;
		this.serviceName = _service_name;
	}

	public FriendNetwork getFriendNetwork() {
		return friendNetwork;
	}

	public String getDisplayName() {
		return friendNetwork.getNetworkName();
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public int getServerPort() {
		return serverPort;
	}

	public String getServiceName() {
		return serviceName;
	}

	public static XMPPNetwork getFromName(String name) {
		for (XMPPNetwork network : networks) {
			if (network.getDisplayName().equals(name)) {
				return network;
			}
		}
		throw new RuntimeException("unable to find xmpp network: '" + name + "'");
	}

}