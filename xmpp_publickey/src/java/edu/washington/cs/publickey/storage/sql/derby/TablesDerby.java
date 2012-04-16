/**
 * 
 */
package edu.washington.cs.publickey.storage.sql.derby;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author isdal
 * 
 */
class TablesDerby {

	public final static String USER_TABLE = "CREATE TABLE publickey.users " + "(" + "user_id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," + "net SMALLINT NOT NULL," + "net_uid VARCHAR(20) FOR BIT DATA NOT NULL, " + "last_seen DATE NOT NULL, " + "PRIMARY KEY (user_id)" + ")";
	public final static String USER_TABLE_DROP = "DROP TABLE publickey.users";

	public final static String USER_TABLE_NET_NET_UID_INDEX = "CREATE UNIQUE INDEX net_net_uid_idx ON publickey.users" + "(net,net_uid)";

	public final static String KEYS_TABLE = "CREATE TABLE publickey.pubkeys " + "(" + "user_id BIGINT NOT NULL, last_seen DATE NOT NULL, " + " pubkey_nick VARCHAR(255) NOT NULL," + "pubkey_sha1 VARCHAR(20) FOR BIT DATA NOT NULL," + "pubkey BLOB NOT NULL," + "FOREIGN KEY (user_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT" + ")";
	public final static String KEYS_TABLE_USER_ID_INDEX = "CREATE INDEX key_user_id_idx ON publickey.pubkeys" + "(user_id)";
	public final static String KEYS_TABLE_LAST_SEEN_INDEX = "CREATE INDEX key_last_seen_idx ON pubkeys" + "(last_seen)";
	public final static String KEYS_TABLE_KEY_INDEX = "CREATE INDEX key_pubkey_id_idx ON publickey.pubkeys" + "(pubkey_sha1)";
	public final static String KEYS_TABLE_USER_ID_KEY_INDEX = "CREATE INDEX key_user_pubkey_id_idx ON publickey.pubkeys" + "(user_id,pubkey_sha1)";
	public final static String KEYS_TABLE_DROP = "DROP TABLE publickey.pubkeys";

	public final static String FRIENDS_TABLE = "CREATE TABLE publickey.friends " + "(" + "user_id BIGINT NOT NULL, " + "friend_id BIGINT NOT NULL, " + "FOREIGN KEY (user_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT, " + "FOREIGN KEY (friend_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT " + ")";
	public final static String FRIENDS_TABLE_UID_INDEX = "CREATE INDEX friends_uid_idx ON publickey.friends" + "(user_id)";
	public final static String FRIENDS_TABLE_FID_INDEX = "CREATE INDEX friends_fid_idx ON publickey.friends" + "(friend_id)";
	public final static String FRIENDS_TABLE_UID_FID_INDEX = "CREATE UNIQUE INDEX friends_uid_fid_idx ON publickey.friends" + "(user_id,friend_id)";

	public final static String FRIENDS_TABLE_DROP = "DROP TABLE publickey.friends";

	public static void createTables(Connection conn, boolean dropIfExists) throws SQLException {
		Statement s = conn.createStatement();

		// if drop, from the tables
		if (dropIfExists) {
			try {
				s.execute(FRIENDS_TABLE_DROP);
				System.out.println("dropped table: friends");
			} catch (SQLException e) {
				if (e.getMessage().equals("'DROP TABLE' cannot be performed on " + "'PUBLICKEY.FRIENDS' " + "because it does not exist.") || e.getMessage().equals("Schema 'PUBLICKEY' does not exist")) {
					System.out.println("friends table not found");

				} else {
					e.printStackTrace();
				}
			}
			try {
				s.execute(KEYS_TABLE_DROP);
				System.out.println("dropped table: pubkeys");
			} catch (SQLException e) {
				if (e.getMessage().equals("'DROP TABLE' cannot be performed on " + "'PUBLICKEY.FRIENDS' " + "because it does not exist.") || e.getMessage().equals("Schema 'PUBLICKEY' does not exist")) {
					System.out.println("friends table not found");

				} else {
					e.printStackTrace();
				}
			}
			try {
				s.execute(USER_TABLE_DROP);
				System.out.println("dropped table: users");
			} catch (SQLException e) {
				if (e.getMessage().equals("'DROP TABLE' cannot be performed on " + "'PUBLICKEY.FRIENDS' " + "because it does not exist.") || e.getMessage().equals("Schema 'PUBLICKEY' does not exist")) {
					System.out.println("friends table not found");

				} else {
					e.printStackTrace();
				}
			}
		}

		/*
		 * create the tables
		 */
		TablesDerby.createUsersTable(s);
		TablesDerby.createKeysTable(s);
		TablesDerby.createFriendsTable(s);
		s.close();
		conn.commit();

	}

	private static void createUsersTable(Statement s) throws SQLException {
		try {
			s.execute(TablesDerby.USER_TABLE);
			s.execute(USER_TABLE_NET_NET_UID_INDEX);
			System.out.println("Created table users");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table/View 'USERS' already exists in Schema 'PUBLICKEY'.")) {

			} else {
				throw e;
			}
		}
	}

	private static void createKeysTable(Statement s) throws SQLException {

		try {
			s.execute(KEYS_TABLE);
			s.execute(KEYS_TABLE_USER_ID_INDEX);
			s.execute(KEYS_TABLE_KEY_INDEX);
			s.execute(KEYS_TABLE_USER_ID_KEY_INDEX);
			System.out.println("Created table pubkeys");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table/View 'PUBKEYS' already exists in Schema 'PUBLICKEY'.")) {
			} else {
				throw e;
			}
		}
	}

	private static void createFriendsTable(Statement s) throws SQLException {

		try {
			s.execute(TablesDerby.FRIENDS_TABLE);
			s.execute(FRIENDS_TABLE_UID_INDEX);
			s.execute(FRIENDS_TABLE_FID_INDEX);
			s.execute(FRIENDS_TABLE_UID_FID_INDEX);
			System.out.println("Created table friends");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table/View 'FRIENDS' already exists in Schema 'PUBLICKEY'.")) {

			} else {
				throw e;
			}
		}
	}
}
