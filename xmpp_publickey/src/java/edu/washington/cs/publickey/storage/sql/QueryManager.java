/**
 * 
 */
package edu.washington.cs.publickey.storage.sql;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sql.rowset.serial.SerialBlob;

import edu.washington.cs.publickey.FriendNetwork;
import edu.washington.cs.publickey.PublicKeyFriend;

/**
 * @author isdal
 * 
 */
public class QueryManager {
	private static final int EXPIRE_AFTER_DAYS = 30;
	/*
	 * For getting own userid
	 */
	protected PreparedStatement getOwnUserIdStmt;
	protected PreparedStatement addUserStmt;

	/*
	 * for updating last seen
	 */
	protected PreparedStatement updateUserLastSeen;
	protected PreparedStatement updateKeyLastSeen;

	/*
	 * For adding own key
	 */
	protected PreparedStatement keyExistsStmt;
	protected PreparedStatement insertOwnKeyStmt;
	protected PreparedStatement updateOwnKeyStmt;

	/*
	 * For getting own keys
	 */
	protected PreparedStatement getOwnKeysStmt;

	/*
	 * For adding friends
	 */
	protected PreparedStatement checkIfFriendsStatement;
	protected PreparedStatement addFriendsStatement;
	protected PreparedStatement getFriends;

	/*
	 * For getting friends public keys
	 */
	protected PreparedStatement getMutualFriends;

	/*
	 * For getting the public key of all mutual friends
	 */
	protected PreparedStatement getMutualFriendsPubKeys;
	/*
	 * For getting the public key of all a users friends
	 */
	protected PreparedStatement getFriendsPubKeys;

	/*
	 * For getting user id's given a public key
	 */
	protected PreparedStatement getUserIdsGivenPublicKey;

	/*
	 * For getting user id's given a public key
	 */
	protected PreparedStatement deleteExpiredKeys;

	
	/**
	 * Retrieves the user id given a FriendNetwork and a networkUID
	 * 
	 * @param network
	 * @param networkUid
	 * @return
	 * @throws SQLException
	 * @throws SQLException
	 */
	public QueryManager(Connection connection) throws SQLException {
		// for getting own key
		addUserStmt = connection.prepareStatement(Queries.ADD_USER, Statement.RETURN_GENERATED_KEYS);
		getOwnUserIdStmt = connection.prepareStatement(Queries.GET_OWN_USER_ID);

		// for updating last seen
		updateUserLastSeen = connection.prepareStatement(Queries.USER_UPDATE_LAST_SEEN);
		updateKeyLastSeen = connection.prepareStatement(Queries.KEY_UPDATE_LAST_SEEN);

		// for adding own key
		keyExistsStmt = connection.prepareStatement(Queries.CHECK_KEY_EXISTS);
		insertOwnKeyStmt = connection.prepareStatement(Queries.INSERT_OWN_KEY);
		updateOwnKeyStmt = connection.prepareStatement(Queries.UPDATE_OWN_KEY);

		// for getting own keys
		getOwnKeysStmt = connection.prepareStatement(Queries.GET_OWN_KEYS);

		// for checking if friends
		checkIfFriendsStatement = connection.prepareStatement(Queries.CHECK_FRIENDS);

		addFriendsStatement = connection.prepareStatement(Queries.ADD_FRIENDS);
		getFriends = connection.prepareStatement(Queries.GET_FRIENDS);

		getMutualFriends = connection.prepareStatement(Queries.GET_MUTUAL_FRIENDS);

		getMutualFriendsPubKeys = connection.prepareStatement(Queries.GET_MUTUAL_FRIENDS_PUBLIC_KEYS);
		getFriendsPubKeys = connection.prepareStatement(Queries.GET_FRIENDS_PUBLIC_KEYS);

		getUserIdsGivenPublicKey = connection.prepareStatement(Queries.GET_USERS_ID_GIVEN_PUBLIC_KEY_SHA);
		
		deleteExpiredKeys = connection.prepareStatement(Queries.DELETE_EXPIRED_KEYS);
	}

	public Long getUserId(FriendNetwork network, byte[] netUid) throws SQLException {
		getOwnUserIdStmt.setInt(Queries.GET_OWN_USER_ID_INDEX_NET, network.getNetworkId());
		// System.out.println("len=" + networkUid.length);
		getOwnUserIdStmt.setBytes(Queries.GET_OWN_USER_ID_INDEX_NET_UID, netUid);

		ResultSet ownIdRs = getOwnUserIdStmt.executeQuery();
		if (ownIdRs.next()) {
			long userId = ownIdRs.getLong("user_id");
			ownIdRs.close();
			return userId;
		}
		return null;
	}

	public ArrayList<Long> addUsers(ArrayList<PublicKeyFriend> users) throws SQLException {
		ArrayList<Long> addedUsers = new ArrayList<Long>();
		if (users.size() == 0) {
			System.out.println("no users to add");
			return addedUsers;
		}
		/*
		 * I tried to use addBatch() but it seems like you can't get the auto
		 * increments keys that way
		 */
		for (int i = 0; i < users.size(); i++) {
			PublicKeyFriend user = users.get(i);
			addUserStmt.setInt(Queries.ADD_USER_INDEX_NET, user.getSourceNetwork().getNetworkId());
			addUserStmt.setBytes(Queries.ADD_USER_INDEX_NET_UID, user.getSourceNetworkUid());
			addUserStmt.execute();

			ResultSet addedId = addUserStmt.getGeneratedKeys();
			if (addedId.next()) {
				long userId = addedId.getLong(1);
				// System.out.println("added user:" + userId);
				addedUsers.add(userId);
			} else {
				throw new SQLException("didn't get any auto generated key from query");
			}
			addedId.close();
		}

		if (addedUsers.size() != users.size()) {
			throw new SQLException("something strange happened, tried to add " + users.size() + " users but only got " + addedUsers.size() + " user_ids");
		}
		return addedUsers;
	}

	public void updateUserLastSeen(FriendNetwork network, byte[] netUid, byte[] publickeysha1) throws SQLException {
		Long userId = getUserId(network, netUid);
		if (userId != null && publickeysha1 != null) {
			updateUserLastSeen(userId, publickeysha1);
		}
	}

	public void updateUserLastSeen(long userId, byte[] publicKeySha1) throws SQLException {
		updateUserLastSeen.setLong(Queries.USER_UPDATE_LAST_SEEN_UID, userId);
		updateUserLastSeen.execute();

		updateKeyLastSeen.setLong(Queries.KEY_UPDATE_LAST_SEEN_UID, userId);
		updateKeyLastSeen.setBytes(Queries.KEY_UPDATE_LAST_SEEN_KEY_SHA1, publicKeySha1);
		updateKeyLastSeen.execute();
	}

	public void updateUserLastSeen(byte[] publicKeySha1) throws SQLException {
		List<Long> userIds = getUserIdsGivenPublicKey(publicKeySha1);
		for (Long uid : userIds) {
			updateUserLastSeen(uid,publicKeySha1);
		}
	}

	public String keyExists(long userId, byte[] key_sha1) throws SQLException {
		keyExistsStmt.setLong(Queries.CHECK_KEY_EXISTS_INDEX_USER_ID, userId);
		keyExistsStmt.setBytes(Queries.CHECK_KEY_EXISTS_INDEX_KEY_SHA1, key_sha1);
		ResultSet rs = keyExistsStmt.executeQuery();
		if (rs.next()) {
			String nick = rs.getString("pubkey_nick");
			rs.close();
			return nick;
		}
		return null;
	}

	public void updateOwnKey(long userId, String keyNick, byte[] publicKeySha1) throws SQLException {
		updateOwnKeyStmt.setString(Queries.UPDATE_OWN_KEY_INDEX_KEY_NICK, keyNick);
		updateOwnKeyStmt.setLong(Queries.UPDATE_OWN_KEY_INDEX_USER_ID, userId);
		updateOwnKeyStmt.setBytes(Queries.UPDATE_OWN_KEY_INDEX_KEY_SHA1, publicKeySha1);
		updateOwnKeyStmt.execute();
	}

	public void insertOwnKey(long userId, String keyNick, byte[] publicKey, byte[] publicKeySha1) throws SQLException {
		insertOwnKeyStmt.setLong(Queries.INSERT_OWN_KEY_INDEX_USER_ID, userId);
		insertOwnKeyStmt.setString(Queries.INSERT_OWN_KEY_INDEX_KEY_NICK, keyNick);
		insertOwnKeyStmt.setBlob(Queries.INSERT_OWN_KEY_INDEX_KEY, new SerialBlob(publicKey));

		insertOwnKeyStmt.setBytes(Queries.INSERT_OWN_KEY_INDEX_KEY_SHA1, publicKeySha1);
		insertOwnKeyStmt.execute();
	}

	public PublicKeyFriend[] ownKeys(PublicKeyFriend me, long userId) throws SQLException {

		final List<PublicKeyFriend> keys = new ArrayList<PublicKeyFriend>();
		getOwnKeysStmt.setLong(1, userId);
		final ResultSet rs = getOwnKeysStmt.executeQuery();
		while (rs.next()) {
			PublicKeyFriend f = new PublicKeyFriend();
			f.setSourceNetwork(me.getSourceNetwork());
			f.setSourceNetworkUid(me.getSourceNetworkUid());
			f.setKeyNick(rs.getString("pubkey_nick"));
			Blob pubkeyBlob = rs.getBlob("pubkey");
			f.setPublicKey(pubkeyBlob.getBytes(1, (int) Math.min(Integer.MAX_VALUE, pubkeyBlob.length())));
			f.setPublicKeySha1(rs.getBytes("pubkey_sha1"));
			keys.add(f);
		}
		rs.close();
		return keys.toArray(new PublicKeyFriend[keys.size()]);
	}

	public void addFriends(long user_id, List<Long> friendsToAdd) throws SQLException {

		for (Long friend_id : friendsToAdd) {
			addFriendsStatement.setLong(Queries.INSERT_FRIENDS_INDEX_USER_ID, user_id);
			addFriendsStatement.setLong(Queries.INSERT_FRIENDS_INDEX_FRIEND_ID, friend_id);
			addFriendsStatement.addBatch();
		}
		int[] rs = addFriendsStatement.executeBatch();
		int added = 0;
		for (int r : rs) {
			added += r;
		}
		if (added != friendsToAdd.size()) {
			throw new SQLException("something strange happened, tried to add " + friendsToAdd + " friend links but only got " + added);
		}

	}

	public List<Long> getFriendsOf(long user_id) throws SQLException {
		List<Long> friends = new LinkedList<Long>();
		getFriends.setLong(Queries.GET_FRIENDS_INDEX_USER_ID, user_id);

		ResultSet rs = getFriends.executeQuery();
		while (rs.next()) {
			long friend_id = rs.getLong("friend_id");
			friends.add(friend_id);
		}
		rs.close();

		return friends;
	}

	public List<Long> getMutualFriendsOf(long user_id) throws SQLException {

		getMutualFriends.setLong(1, user_id);
		getMutualFriends.setLong(2, user_id);
		List<Long> friends = new ArrayList<Long>();
		ResultSet rs = getMutualFriends.executeQuery();
		while (rs.next()) {
			friends.add(rs.getLong("friend_id"));
		}
		rs.close();
		return friends;
	}

	// public List<PublicKeyFriend> getMutualFriendsPublicKeys(long user_id)
	// throws SQLException {
	// getMutualFriendsPubKeys.setLong(1, user_id);
	// getMutualFriendsPubKeys.setLong(2, user_id);
	// List<PublicKeyFriend> friends = new ArrayList<PublicKeyFriend>();
	// ResultSet rs = getMutualFriendsPubKeys.executeQuery();
	// while (rs.next()) {
	// PublicKeyFriend f = new PublicKeyFriend();
	// f.setKeyNick(rs.getString("pubkey_nick"));
	// Blob pubkeyBlob = rs.getBlob("pubkey");
	// f.setPublicKey(pubkeyBlob.getBytes(1, (int) Math.min(Integer.MAX_VALUE,
	// pubkeyBlob.length())));
	// f.setPublicKeySha1(rs.getBytes("pubkey_sha1"));
	// f.setSourceNetwork(FriendNetwork.getFromId(rs.getInt("net")));
	// f.setSourceNetworkUid(rs.getBytes("net_uid"));
	// friends.add(f);
	// }
	// rs.close();
	// return friends;
	// }

	public List<PublicKeyFriend> getMutualFriendsPublicKeys(long user_id) throws SQLException {
		getFriendsPubKeys.setLong(1, user_id);

		List<PublicKeyFriend> friends = new ArrayList<PublicKeyFriend>();
		ResultSet rs = getFriendsPubKeys.executeQuery();
		while (rs.next()) {
			PublicKeyFriend f = new PublicKeyFriend();
			f.setKeyNick(rs.getString("pubkey_nick"));
			Blob pubkeyBlob = rs.getBlob("pubkey");
			f.setPublicKey(pubkeyBlob.getBytes(1, (int) Math.min(Integer.MAX_VALUE, pubkeyBlob.length())));
			f.setPublicKeySha1(rs.getBytes("pubkey_sha1"));
			f.setSourceNetwork(FriendNetwork.getFromId(rs.getInt("net")));
			f.setSourceNetworkUid(rs.getBytes("net_uid"));
			long friend_id = rs.getLong("user_id");
			if (areFriends(friend_id, user_id)) {
				friends.add(f);
			}
		}
		rs.close();
		return friends;
	}

	public boolean areFriends(long user_id, long friend_id) throws SQLException {
		checkIfFriendsStatement.setLong(Queries.CHECK_FRIENDS_INDEX_USER_ID, user_id);
		checkIfFriendsStatement.setLong(Queries.CHECK_FRIENDS_INDEX_FRIEND_ID, friend_id);
		ResultSet rs = checkIfFriendsStatement.executeQuery();
		if (rs.next()) {
			return true;
		}
		return false;
	}

	public List<Long> getUserIdsGivenPublicKey(byte[] publicKeySha1) throws SQLException {
		List<Long> userIds = new ArrayList<Long>();
		if (publicKeySha1 == null) {
			throw new RuntimeException("no publickey sha1 in publickeyfriend");
		}
		getUserIdsGivenPublicKey.setBytes(1, publicKeySha1);

		ResultSet rs = getUserIdsGivenPublicKey.executeQuery();
		while (rs.next()) {
			long userId = rs.getLong("user_id");
			userIds.add(userId);
		}

		rs.close();
		return userIds;
	}
	
	public void deleteExpiredKeys() throws SQLException{
		deleteExpiredKeys.setInt(Queries.DELETE_EXPIRED_KEYS_NUM_DAYS_ID, EXPIRE_AFTER_DAYS);
		deleteExpiredKeys.execute();
	}
}
