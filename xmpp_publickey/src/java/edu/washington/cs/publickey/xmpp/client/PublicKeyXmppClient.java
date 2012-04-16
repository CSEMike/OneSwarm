package edu.washington.cs.publickey.xmpp.client;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Base64;

import edu.washington.cs.publickey.CryptoHandler;
import edu.washington.cs.publickey.PublicKeyClient;
import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.Tools;
import edu.washington.cs.publickey.xmpp.XMPPNetwork;

public class PublicKeyXmppClient extends PublicKeyClient {

	private static final long TIMEOUT = 20 * 1000;
	private boolean disconnectRequested = false;
	private final XMPPConnection connection;
	private final PublicKeyFriend me;
	private final XMPPNetwork network;
	private List<PublicKeyFriend> networkFriends;

	private final char[] password;

	private String serverUserId;

	private final CryptoHandler signer;

	private String status = "";

	private final String username;

	public PublicKeyXmppClient(File knownFriendsFile, List<byte[]> knownKeys, XMPPNetwork network, String username, char[] password, String localKeyNick, CryptoHandler signer) throws Exception {
		super(knownFriendsFile, knownKeys);
		this.signer = signer;
		this.password = password;
		this.network = network;
		this.username = username;
		log("Starting IM client");
		ConnectionConfiguration connConfig = new ConnectionConfiguration(network.getServerAddr(), network.getServerPort(), network.getServiceName());
		connection = new XMPPConnection(connConfig);
		// create the "me" user
		me = new PublicKeyFriend();
		me.setKeyNick(localKeyNick);
		me.setPublicKey(signer.getPublicKey().getEncoded());
		me.setPublicKeySha1(Tools.getSha1(signer.getPublicKey().getEncoded()));
		me.setSourceNetwork(network.getFriendNetwork());
		me.setSourceNetworkUid(Tools.getSha1(username));
		me.setRealName("Me");
	}

	/**
	 * Connect to the xmpp server
	 */
	public void connect() throws Exception {

		connection.connect();
		status = "connected";
		log(status);
		// TODO: talk to the smack guys about using a char[] for passwords so
		// it can get overwritten
		connection.login(username, new String(password));
		status = "logged in to: " + network.getDisplayName();
		log(status);
		// set presence
		Presence p = new Presence(Presence.Type.unavailable);
		connection.sendPacket(p);

		// don't allow any new users on my watch
		connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.reject_all);

		// add the publickey serverbot to the contact list
		// so we can send and receive messages from it
		// if (!connection.getRoster().contains(getServerBotUserId())) {
		// connection.getRoster().createEntry(getServerBotUserId(),
		// serverBotName, null);
		// }
	}

	private String serverBotName = "PublicKey Server Bot";

	public void setServerBotName(String name) {
		this.serverBotName = name;
	}

	/**
	 * Create a client hello message
	 * 
	 *The message contains the public key of the user
	 * 
	 * @param myKey
	 * @return
	 * @throws IOException
	 */
	private Message createClientHello(PublicKeyFriend me) throws IOException {
		Message msg = new Message(getServerBotUserId(), Message.Type.chat);
		msg.setBody(null);
		// we only have to set the public key and key nick fienlds here, the
		// rest is calculated
		// on the server anyway (it won't trust any of that data from here)
		PublicKeyFriend myKey = new PublicKeyFriend();
		myKey.setPublicKey(me.getPublicKey());
		myKey.setKeyNick(me.getKeyNick());
		msg.setProperty(Tools.PUBLICKEY_PAYLOAD_KEY__PublicKeyFriend, myKey.serialize());
		msg.setFrom(connection.getUser());
		return msg;
	}

	/**
	 * Creates a client request packet based on the server challenge packet
	 * 
	 * @param serverChallenge
	 * @return
	 * @throws Exception
	 */

	private Message createClientRequest(Packet serverChallenge) throws Exception {
		Object nounceObj = serverChallenge.getProperty(Tools.PUBLICKEY_PAYLOAD_NOUNCE__base64_byte_array);
		if (nounceObj != null && nounceObj instanceof String) {
			byte[] nounce = Base64.decode((String) nounceObj);
			byte[] signature = signer.sign(nounce);

			Message m = new Message();
			m.setPacketID(serverChallenge.getPacketID());
			m.setTo(serverChallenge.getFrom());
			m.setBody(null);
			m.setFrom(connection.getUser());
			m.setProperty(Tools.PUBLICKEY_PAYLOAD_SIGNATURE__String_Base64, Base64.encodeBytes(signature, Base64.DONT_BREAK_LINES));
			m.setProperty(Tools.PUBLICKEY_PAYLOAD_FRIENDS__MergeSha1_Base64, getFriendsCompact());

			List<byte[]> knownKeyList = getKnownKeySha1s();
			m.setProperty(Tools.PUBLICKEY_PAYLOAD_KNOWN_KEYS__MergeSha1_Base64, Tools.mergeSha1sAndBase64(knownKeyList));

			return m;
		} else {
			throw new Exception("server handshake invalid, expected random nounce, got: '" + nounceObj + "'");
		}
	}

	public void disconnect() throws Exception {
		if (connection.isConnected() && !disconnectRequested) {
			disconnectRequested = true;

			/*
			 * do this in a separate thread
			 */
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						// clean up the connection, unsubscribe the bot and set
						// to
						// unavailable
						log("sending presence unsubscribe to: " + serverUserId);
						status = "cleaning up connection";
						Presence presenceUnsubscribe = new Presence(Presence.Type.unsubscribe);
						presenceUnsubscribe.setTo(serverUserId);
						connection.sendPacket(presenceUnsubscribe);

						RosterEntry entry = connection.getRoster().getEntry(getServerBotUserId());
						if (entry != null) {
							connection.getRoster().removeEntry(entry);
							System.out.println("removing: " + entry.getUser() + " from roster");
						}

						Presence presenceUnavailable = new Presence(Presence.Type.unavailable);
						connection.sendPacket(presenceUnavailable);
						log(status);
					} catch (XMPPException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					try {
						connection.disconnect();
					} catch (java.security.AccessControlException e) {
						System.err.println("unable to close connection (check if thread has modifyThread permission)");
					}
				}
			});
			t.setDaemon(true);
			t.setName("XMPP disconnect thread");
			t.start();
		} else {
			status = "disconnected";
		}
		for (int i = 0; i < password.length; i++) {
			password[i] = 'a';
		}
	}

	private String getFriendsCompact() throws NoSuchAlgorithmException, IOException {
		networkFriends = new LinkedList<PublicKeyFriend>();

		// add self both for mapping and to make the server not return us
		networkFriends.add(me);
		byte[] serverSha1 = Tools.getSha1(getServerBotUserId());
		List<byte[]> friends = new LinkedList<byte[]>();
		for (RosterEntry friend : connection.getRoster().getEntries()) {
			byte[] sha1 = Tools.getSha1(friend.getUser());
			// dont add the server bot
			if (!Arrays.equals(serverSha1, sha1)) {

				friends.add(sha1);

				PublicKeyFriend f = new PublicKeyFriend();
				f.setSourceNetwork(network.getFriendNetwork());
				f.setSourceNetworkUid(sha1);
				String name = friend.getName();
				if (name != null) {
					f.setRealName(name);
				} else {
					f.setRealName(friend.getUser());
				}
				networkFriends.add(f);
			}
		}
		super.addKnownFriends(networkFriends);
		return Tools.mergeSha1sAndBase64(friends);
	}

	/**
	 * This is the meat of the xmpp client: it follows the
	 * "publickey xmpp protocol"
	 * 
	 * 1: Client->Server: clients public key
	 * 
	 * 2: S->C: nounce to sign
	 * 
	 * 3: C->S: signature of nounce, list of friends, list of sha1 of known keys
	 * 
	 * 4: S->C: list of new friends
	 */
	public void updateFriends() throws Exception {
		try {

			// 0.1: send the presence packet to the bot
			try {
				log("sending presence subscribe to: " + serverUserId + ", waiting");
				status = "locating friend finder";
				Presence presenceSubscribe = new Presence(Presence.Type.subscribe);
				presenceSubscribe.setTo(serverUserId);
				sendPacketAndWaitForResponse(presenceSubscribe);
				log("got presence subscribed: " + serverUserId);
			} catch (XMPPException e) {
				log("got no presence subscribed response, trying to continue anyway");
			}
			// 0.2 set presence to available
			try {
				status = "sending status=online";
				Presence presenceAvailable = new Presence(Presence.Type.available);
				presenceAvailable.setMode(Presence.Mode.xa);
				presenceAvailable.setTo(serverUserId);
				sendPacketAndWaitForResponse(presenceAvailable);
				log(status);
			} catch (XMPPException e) {
				log("got no presence available response, trying to continue anyway");
			}

			// 1: Client->Server: clients public key
			Message msg = createClientHello(me);
			status = "sending client hello, waiting for server";
			log(status);

			// 2: S->C: nounce to sign
			Packet serverChallenge = sendPacketAndWaitForResponse(msg);
			status = "got server challenge, signing + sending client request";
			log(status);
			Message clientRequest = createClientRequest(serverChallenge);

			// 3: C->S: signature of nounce, list of friends, list of sha1 of
			// known keys
			Packet serverResponse = sendPacketAndWaitForResponse(clientRequest);
			status = "sent client request, waiting for friend list";
			log(status);

			// 4: S->C: list of new friends
			Object resp = serverResponse.getProperty(Tools.PUBLICKEY_PAYLOAD_FRIENDS_KEYS__PublicKeyFriend_array);
			if (resp instanceof String) {
				PublicKeyFriend[] friends = PublicKeyFriend.deserialize((String) resp);
				status = "got server response, new friend count: " + friends.length;
				addKnownFriends(Arrays.asList(friends));
				disconnect();
			} else {
				disconnect();
				throw new Exception("strange, got non friend array back");
			}
		} catch (Exception e) {
			disconnect();
			throw e;
		}

	}

	public String getStatus() {
		return status;
	}

	public String getServerBotUserId() {
		String toUser;
		if (serverUserId == null) {
			toUser = Tools.DEFAULT_PUBLIC_KEY_SERVER;
		} else {
			toUser = serverUserId;
		}
		return toUser;
	}

	private void log(String mesg) {
		System.out.println(username + ": " + mesg);
	}

	private Packet sendPacketAndWaitForResponse(Packet packet) throws XMPPException {
		log("sending packet: " + packet.toXML());
		PacketCollector collector = connection.createPacketCollector(new PacketIDFilter(packet.getPacketID()));
		connection.sendPacket(packet);
		log("waiting for response");
		Packet response = collector.nextResult(TIMEOUT);
		collector.cancel();
		if (response == null) {
			throw new XMPPException("No response from the server.");
		} else if (response.getError() != null) {
			XMPPError error = response.getError();
			throw new XMPPException("Got error from xmpp server '" + network.getServerAddr() + ":" + network.getServerPort() + "/" + serverUserId + "' error:" + error);
		}
		log("got response: " + response.toXML());
		return response;
	}

	public void setServerBotUserId(String serverUserId) throws Exception {
		if (!connection.isConnected()) {
			this.serverUserId = serverUserId;
		} else {
			throw new Exception("setting server user id is " + "not allowed after connect()");
		}
	}

}
