package edu.washington.cs.publickey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public abstract class PublicKeyClient {
	private final static String DEFAULT_EXISTING_FRIENDS = "friends.publickeyfriends";
	public static boolean logToStdOut = false;

	private static void log(String msg) {
		if (logToStdOut) {
			System.out.println(msg);
		}
	}

	private final File existingFriendsFile;

	protected List<PublicKeyFriend> knownFriends;
	protected List<byte[]> knownKeys;

	public PublicKeyClient(File existingFriendsFile, List<byte[]> knownKeys) {
		this.existingFriendsFile = existingFriendsFile;
		this.knownKeys = knownKeys;
		this.loadKnownFriends();
	}

	protected void addKnownFriends(List<PublicKeyFriend> newFriends) throws IOException {
		log("adding friends:" + newFriends.size());

		HashMap<String, PublicKeyFriend> netUidHashToUid = new HashMap<String, PublicKeyFriend>();
		HashMap<String, Boolean> knownPubKeys = new HashMap<String, Boolean>();
		for (PublicKeyFriend f : knownFriends) {
			String netUid = Base64.encode(f.getSourceNetworkUid());
			netUidHashToUid.put(netUid, f);
			if (f.getPublicKey() != null) {
				String publicKey = Base64.encode(f.getPublicKey());
				knownPubKeys.put(publicKey, true);
			}
			/*
			 * check if any of the known friends does not have any realname, in
			 * that case search for it
			 */
			if (f.getRealName() == null) {
				for (PublicKeyFriend newFriend : newFriends) {
					if (Arrays.equals(newFriend.getSourceNetworkUid(), f.getSourceNetworkUid())) {
						if (f.getRealName() == null) {
							log("Updating friend, just got the realname");
							f.setRealName(newFriend.getRealName());
						}
					}
				}
			}
		}
		// for each entry, check if we already know about that friend
		// if not, add, if we do, update or ignore
		for (PublicKeyFriend newFriend : newFriends) {
			// users can either be added if they are the first user with a
			// certain uid
			String netUid = Base64.encode(newFriend.getSourceNetworkUid());
			if (!netUidHashToUid.containsKey(netUid)) {
				knownFriends.add(newFriend);
				log("adding: " + newFriend);
			} else if (newFriend.getPublicKey() != null) {
				// or if we don't have that public key
				String publicKey = Base64.encode(newFriend.getPublicKey());
				if (!knownPubKeys.containsKey(publicKey)) {
					// check if we need to add the real name
					// we might have a dummy user in there
					PublicKeyFriend existing = netUidHashToUid.get(netUid);
					if (existing.getPublicKey() == null) {
						log("updating: " + existing);
						existing.setPublicKey(newFriend.getPublicKey());
						existing.setPublicKeySha1(newFriend.getPublicKeySha1());
						existing.setKeyNick(newFriend.getKeyNick());
						log(existing.toString());
					} else {
						// the existing one already has a public key
						// this must mean that this is the second+ for that user
						newFriend.setRealName(existing.getRealName());
						knownFriends.add(newFriend);
						log("adding: " + newFriend);
					}
				} else {
					if (newFriend.getRealName() == null) {
						log("strange, new public key but no uid->realname mapping: " + newFriend);
					}
				}
			}
		}

		File friendPath = existingFriendsFile;
		if (friendPath == null) {
			friendPath = new File(DEFAULT_EXISTING_FRIENDS);
		}
		/*
		 * create the parent directory if not exists
		 */
		if (!friendPath.exists()) {
			File parent = friendPath.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
		}

		String serialized = PublicKeyFriend.serialize(knownFriends.toArray(new PublicKeyFriend[knownFriends.size()]));
		BufferedWriter out = new BufferedWriter(new FileWriter(friendPath));
		out.write(serialized);
		out.close();
	}

	public abstract void connect() throws Exception;

	public abstract void disconnect() throws Exception;

	public List<PublicKeyFriend> getFriends() {
		return knownFriends;
	}

	protected List<byte[]> getKnownKeySha1s() {
		List<byte[]> knownKeyList = new ArrayList<byte[]>();
		for (byte[] key : knownKeys) {
			try {
				knownKeyList.add(Tools.getSha1(key));
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return knownKeyList;
	}

	private void loadKnownFriends() {
		knownFriends = new ArrayList<PublicKeyFriend>();
		File friendPath = existingFriendsFile;
		if (friendPath == null) {
			friendPath = new File(DEFAULT_EXISTING_FRIENDS);
		}

		StringBuilder b = new StringBuilder();
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(friendPath));
			String line;
			while ((line = in.readLine()) != null) {
				b.append(line);
			}
			in.close();
			knownFriends.addAll(Arrays.asList(PublicKeyFriend.deserialize(b.toString())));
		} catch (FileNotFoundException e) {
			log("existing friends file not found: " + friendPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public abstract void updateFriends() throws Exception;

}
