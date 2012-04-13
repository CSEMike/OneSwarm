/**
 * 
 */
package edu.washington.cs.publickey.storage.sql;

/**
 * @author isdal
 * 
 */
interface Queries {

	/**
	 * Query for getting the internal user_id of a user
	 */
	public final static String GET_OWN_USER_ID = "SELECT user_id FROM users WHERE net=? AND net_uid=?";
	public final static int GET_OWN_USER_ID_INDEX_NET = 1;
	public final static int GET_OWN_USER_ID_INDEX_NET_UID = 2;

	/**
	 * Query for adding a user to the db
	 */
	public final static String ADD_USER = "INSERT INTO users (net,net_uid,last_seen) VALUES(?,?,CURRENT_DATE)";
	public final static int ADD_USER_INDEX_NET = 1;
	public final static int ADD_USER_INDEX_NET_UID = 2;

	/**
	 * Query for updating the last seen value of a user
	 */
	public final static String USER_UPDATE_LAST_SEEN = "UPDATE users SET last_seen=CURRENT_DATE WHERE user_id=?";
	public final static int USER_UPDATE_LAST_SEEN_UID = 1;

	/**
	 * Query for updating the last seen value of a public key
	 */
	public final static String KEY_UPDATE_LAST_SEEN = "UPDATE pubkeys SET last_seen=CURRENT_DATE WHERE user_id=? AND pubkey_sha1=?";
	public final static int KEY_UPDATE_LAST_SEEN_UID = 1;
	public final static int KEY_UPDATE_LAST_SEEN_KEY_SHA1 = 2;

	/**
	 * Query for checking if a key is in the db
	 */
	public final static String CHECK_KEY_EXISTS = "SELECT pubkey_nick FROM pubkeys WHERE user_id=? AND pubkey_sha1=?";
	public final static int CHECK_KEY_EXISTS_INDEX_USER_ID = 1;
	public final static int CHECK_KEY_EXISTS_INDEX_KEY_SHA1 = 2;

	/**
	 * Query for updating the nick of a key
	 */
	public final static String UPDATE_OWN_KEY = "UPDATE pubkeys SET pubkey_nick=? WHERE user_id=? AND pubkey_sha1=? AND last_seen=CURRENT_DATE";
	public final static int UPDATE_OWN_KEY_INDEX_KEY_NICK = 1;
	public final static int UPDATE_OWN_KEY_INDEX_USER_ID = 2;
	public static final int UPDATE_OWN_KEY_INDEX_KEY_SHA1 = 3;
	/**
	 * Query for inserting a new public key into the db
	 */
	public final static String INSERT_OWN_KEY = "INSERT INTO pubkeys (pubkey_nick,last_seen,user_id,pubkey,pubkey_sha1) VALUES (?,CURRENT_DATE,?,?,?)";
	public final static int INSERT_OWN_KEY_INDEX_KEY_NICK = 1;
	public final static int INSERT_OWN_KEY_INDEX_USER_ID = 2;
	public final static int INSERT_OWN_KEY_INDEX_KEY = 3;
	public final static int INSERT_OWN_KEY_INDEX_KEY_SHA1 = 4;

	/**
	 * Query for getting all own public keys
	 */
	public final static String GET_OWN_KEYS = "SELECT pubkey_nick,pubkey,pubkey_sha1 FROM pubkeys WHERE user_id=?";
	public final static int GET_OWN_KEYS_INDEX_USER_ID = 1;

	/**
	 * Query for adding friends
	 */
	public final static String ADD_FRIENDS = "INSERT INTO friends (user_id,friend_id) VALUES (?,?)";
	public final static int INSERT_FRIENDS_INDEX_USER_ID = 1;
	public final static int INSERT_FRIENDS_INDEX_FRIEND_ID = 2;

	/**
	 * Query for checking if two users are friends
	 */
	public final static String CHECK_FRIENDS = "SELECT user_id,friend_id FROM friends WHERE user_id=? AND friend_id=?";
	public final static int CHECK_FRIENDS_INDEX_USER_ID = 1;
	public final static int CHECK_FRIENDS_INDEX_FRIEND_ID = 2;

	/**
	 * Query for getting the friends of a user
	 */
	public static final String GET_FRIENDS = "SELECT friend_id FROM friends WHERE user_id=?";
	public final static int GET_FRIENDS_INDEX_USER_ID = 1;

	/**
	 * Query for getting mutual friends
	 * 
	 * More formally: users u1 and u2 are mutual friends iff (u1 has specified
	 * u2 as their friend, AND u2 has specified u1 as their friend)
	 */
	public static final String GET_MUTUAL_FRIENDS = "SELECT friend_id FROM friends " + "WHERE user_id=? " + "AND friend_id IN " + "(SELECT user_id FROM friends WHERE friend_id=?)";

	/**
	 * Query for getting the public keys of the friends of a user u
	 * 
	 * NOTE: The query will only return the public keys of users f where (f has
	 * specified u as their friend, AND u has specified f as their friend)
	 */

	public static final String GET_MUTUAL_FRIENDS_PUBLIC_KEYS = "SELECT u.user_id AS user_id," + "u.net_uid AS net_uid, " + "u.net AS net, " + "k.pubkey AS pubkey, " + "k.pubkey_nick AS pubkey_nick, " + "k.pubkey_sha1 AS pubkey_sha1 " + "FROM users u, pubkeys k " + "WHERE k.user_id = u.user_id " + "AND u.user_id IN (" + GET_MUTUAL_FRIENDS + ")";

	/**
	 * Query for getting the public keys of the friends of a user u
	 * 
	 * NOTE: This query is unfiltered, the returned friends will have to be
	 * manually checked for symmetry using the CHECK_FRIENDS query
	 */
	public static final String GET_FRIENDS_PUBLIC_KEYS = "SELECT u.user_id AS user_id," + "u.net_uid AS net_uid, " + "u.net AS net, " + "k.pubkey AS pubkey, " + "k.pubkey_nick AS pubkey_nick, " + "k.pubkey_sha1 AS pubkey_sha1 " + "FROM friends f " + "INNER JOIN users u ON f.friend_id = u.user_id " + "INNER JOIN pubkeys k ON u.user_id = k.user_id " + "WHERE f.user_id=?";

	/**
	 * Query for getting the users associated with a given public key sha1
	 */

	public static final String GET_USERS_ID_GIVEN_PUBLIC_KEY_SHA = "SELECT u.user_id, u.net, u.net_uid, p.pubkey_nick " + "FROM users u INNER JOIN pubkeys p " + "ON u.user_id = p.user_id WHERE p.pubkey_sha1=?";

	/**
	 * Query for getting expired keys
	 */

	public static final String DELETE_EXPIRED_KEYS = "DELETE FROM pubkeys WHERE TIMESTAMPDIFF(DAY,last_seen,CURRENT_DATE) > ?";
	public final static int DELETE_EXPIRED_KEYS_NUM_DAYS_ID = 1;
}
