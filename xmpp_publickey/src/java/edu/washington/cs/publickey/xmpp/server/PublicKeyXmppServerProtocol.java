package edu.washington.cs.publickey.xmpp.server;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.Base64;

import edu.washington.cs.publickey.FriendNetwork;
import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.Tools;
import edu.washington.cs.publickey.storage.PersistentStorage;

public class PublicKeyXmppServerProtocol {

	private static final long MAX_TIME = 60 * 1000;
	private static final int MAX_RETURNED = 7;

	private ProtocolStatus protocolStatus;

	private final String remoteUser;
	private final PublicKey remoteUserPublickey;
	private final String remoteUserPublicKeyNick;
	private final XMPPConnection conn;

	private final String messageId;

	private final byte[] nounce = new byte[1024];

	private final PublicKeyIdPacketFilter packetFilter;
	private final PublicKeyProtocolListener packetListener;

	private final PersistentStorage storage;
	private final PublicKeyXmppServer parent;
	private final long startTime;
	private boolean closed = false;

	public PublicKeyXmppServerProtocol(Packet clientHandshake, XMPPConnection conn, PersistentStorage storage, PublicKeyXmppServer parent) throws Exception {
		this.parent = parent;

		this.protocolStatus = ProtocolStatus.HANDSHAKE_RECV;
		// if (clientHandshake.getFrom().contains("/")) {
		// remoteUser = clientHandshake.getFrom().split("/")[0];
		// } else {
		this.remoteUser = clientHandshake.getFrom();
		// }
		log("connection from: " + remoteUser);
		this.conn = conn;
		this.storage = storage;
		this.startTime = System.currentTimeMillis();
		if (remoteUser == null) {
			throw new Exception("Remote user=null");
		}
		Object pubkeyFriendObj = clientHandshake.getProperty(Tools.PUBLICKEY_PAYLOAD_KEY__PublicKeyFriend);
		if (pubkeyFriendObj != null && pubkeyFriendObj instanceof String) {

			PublicKeyFriend[] f = PublicKeyFriend.deserialize((String) pubkeyFriendObj);
			if (f.length == 1) {
				this.remoteUserPublickey = Tools.keyForEncodedBytes(f[0].getPublicKey());
				this.remoteUserPublicKeyNick = f[0].getKeyNick();
			} else {
				throw new Exception("got strange data=" + pubkeyFriendObj);
			}
		} else {
			throw new Exception("Remote public key=" + pubkeyFriendObj);
		}

		Random random = new Random();
		this.messageId = clientHandshake.getPacketID();
		random.nextBytes(nounce);

		this.packetFilter = new PublicKeyIdPacketFilter();
		this.packetListener = new PublicKeyProtocolListener();
		conn.addPacketListener(packetListener, packetFilter);
		sendServerChallengeMessage();

	}

	private void sendServerChallengeMessage() {
		Message msg = new Message(remoteUser, Message.Type.chat);
		msg.setBody(null);
		msg.setProperty(Tools.PUBLICKEY_PAYLOAD_NOUNCE__base64_byte_array, Base64.encodeBytes(nounce, Base64.DONT_BREAK_LINES));
		msg.setFrom(conn.getUser());
		msg.setPacketID(messageId);
		log("sending server challenge to " + remoteUser);
		// log(msg.toXML());
		this.protocolStatus = ProtocolStatus.CHALLENGE_SENT;
		conn.sendPacket(msg);

	}

	private void sendServerResponseMessage(String serialized) {
		Message msg = new Message(remoteUser, Message.Type.chat);
		msg.setBody(null);
		msg.setProperty(Tools.PUBLICKEY_PAYLOAD_FRIENDS_KEYS__PublicKeyFriend_array, serialized);
		msg.setFrom(conn.getUser());
		msg.setPacketID(messageId);
		conn.sendPacket(msg);
	}

	private void log(String msg) {
		parent.log(remoteUser + " " + protocolStatus.name() + ": " + msg, true);
	}

	public void close() {
		if (conn.isConnected()) {
			String u = remoteUser;
			if (u.contains("/")) {
				u = u.split("/")[0];
			}
			RosterEntry entry = conn.getRoster().getEntry(u);
			if (entry != null) {
				try {
					conn.getRoster().removeEntry(entry);
					System.out.println("removed " + u + " from roster");
				} catch (XMPPException e) {
					System.err.println("problem occured when removing '" + u + "': " + e.getMessage());
				}
			}

		}
		conn.removePacketListener(packetListener);
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	public boolean isTimedOut() {
		return System.currentTimeMillis() - startTime > MAX_TIME;
	}

	private class PublicKeyProtocolListener implements PacketListener {

		protected PublicKeyProtocolListener() {

		}

		public void processPacket(Packet packet) {
			if (!isTimedOut()) {
				if (protocolStatus == ProtocolStatus.CHALLENGE_SENT) {
					Object base64Sign = packet.getProperty(Tools.PUBLICKEY_PAYLOAD_SIGNATURE__String_Base64);
					if (base64Sign != null && base64Sign instanceof String) {
						byte[] sign = Base64.decode((String) base64Sign);
						try {
							boolean verified = Tools.verifySignature(remoteUserPublickey, nounce, sign);
							if (verified) {

								protocolStatus = ProtocolStatus.SIGNATURE_RECV;

								// add the user to the db
								PublicKeyFriend user = getUser();
								storage.addPublicKey(user);
								storage.updateUserLastSeen(user);

								// add the friends of the user to the db
								addFriends(packet, user);

								// get the public keys of any new friends (not
								// in
								// the known public key list)
								ArrayList<PublicKeyFriend> allKeys = new ArrayList<PublicKeyFriend>();
								PublicKeyFriend[] friendsPublicKeys = storage.getFriendPublicKeys(user);
								log("got");
								allKeys.addAll(Arrays.asList(friendsPublicKeys));
								allKeys.addAll(Arrays.asList(storage.getOwnPublicKeys(user)));

								List<PublicKeyFriend> newFriends = filterKnownKeys(packet, allKeys, user,MAX_RETURNED);
								String serialized = PublicKeyFriend.serialize(newFriends.toArray(new PublicKeyFriend[newFriends.size()]));
								sendServerResponseMessage(serialized.toString());

								protocolStatus = ProtocolStatus.COMPLETED;
							} else {
								log("signature did not verify");
							}
						} catch (InvalidKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchProviderException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SignatureException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (packet.getProperty(Tools.PUBLICKEY_PAYLOAD_KEY__PublicKeyFriend) != null) {
						log("got handshake again !!!???");
						return;
					} else {
						log("signature problem: got '" + base64Sign + "'");
					}
				} else {
					log("got message, but in wrong protocol state: (" + protocolStatus.name() + "): " + packet.toXML());
				}
			} else {
				log("got message, but max time is reached");
			}

			close();

		}

		private List<PublicKeyFriend> filterKnownKeys(Packet packet, ArrayList<PublicKeyFriend> allKeys, PublicKeyFriend user, int max) {
			HashMap<String, Boolean> knownKeysMap = new HashMap<String, Boolean>();
			knownKeysMap.put(Base64.encodeBytes(user.getPublicKeySha1()), true);
			Object knownKeysObj = packet.getProperty(Tools.PUBLICKEY_PAYLOAD_KNOWN_KEYS__MergeSha1_Base64);
			if (knownKeysObj != null && knownKeysObj instanceof String) {
				List<byte[]> knownKeys = Tools.getListSha1((String) knownKeysObj);
				for (byte[] key : knownKeys) {
					knownKeysMap.put(Base64.encodeBytes(key, Base64.DONT_BREAK_LINES), true);
				}
			}

			List<PublicKeyFriend> newFriends = new ArrayList<PublicKeyFriend>();
			for (PublicKeyFriend publicKeyFriend : allKeys) {

				if (!knownKeysMap.containsKey(Base64.encodeBytes(publicKeyFriend.getPublicKeySha1()))) {
					newFriends.add(publicKeyFriend);
				}
				if(newFriends.size() >= max){
					break;
				}
			}

			log("filtering known keys: new=" + newFriends.size() + " total=" + allKeys.size());
			return newFriends;
		}

		private void addFriends(Packet packet, PublicKeyFriend user) throws Exception {
			Object friendListObj = packet.getProperty(Tools.PUBLICKEY_PAYLOAD_FRIENDS__MergeSha1_Base64);
			if (friendListObj != null && friendListObj instanceof String) {
				List<byte[]> uids = Tools.getListSha1((String) friendListObj);
				List<PublicKeyFriend> friends = new ArrayList<PublicKeyFriend>();
				for (byte[] uid : uids) {
					PublicKeyFriend f = new PublicKeyFriend();
					f.setSourceNetworkUid(uid);
					f.setSourceNetwork(user.getSourceNetwork());
					friends.add(f);
					// log("adding friend: " + Base64.encodeBytes(uid,
					// Base64.DONT_BREAK_LINES));
				}
				storage.addFriends(user, friends.toArray(new PublicKeyFriend[friends.size()]));
			}
		}

		private PublicKeyFriend getUser() throws NoSuchAlgorithmException, UnsupportedEncodingException {
			PublicKeyFriend user = new PublicKeyFriend();
			user.setSourceNetwork(parent.getNetwork().getFriendNetwork());
			user.setSourceNetworkUid(Tools.getSha1(remoteUser));
			user.setPublicKey(remoteUserPublickey.getEncoded());
			user.setPublicKeySha1(Tools.getSha1(remoteUserPublickey.getEncoded()));
			user.setKeyNick(remoteUserPublicKeyNick);
			return user;
		}
	}

	private class PublicKeyIdPacketFilter implements PacketFilter {
		protected PublicKeyIdPacketFilter() {
		}

		public boolean accept(Packet packet) {

			if (messageId.equals(packet.getPacketID()) && packet.getFrom() != null && packet.getFrom().equals(remoteUser)) {
				return true;
			}

			return false;
		}
	}

	static enum ProtocolStatus {
		HANDSHAKE_RECV, CHALLENGE_SENT, SIGNATURE_RECV, COMPLETED;
	}
}
