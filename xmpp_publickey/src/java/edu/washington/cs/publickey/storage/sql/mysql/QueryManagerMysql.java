package edu.washington.cs.publickey.storage.sql.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import edu.washington.cs.publickey.storage.sql.QueryManager;

public class QueryManagerMysql extends QueryManager {

	public QueryManagerMysql(Connection connection) throws SQLException {
		super(connection);
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
}
