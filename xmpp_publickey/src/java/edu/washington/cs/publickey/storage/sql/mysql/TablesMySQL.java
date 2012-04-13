/**
 * 
 */
package edu.washington.cs.publickey.storage.sql.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author isdal
 * 
 */
class TablesMySQL {

	public final static String USER_TABLE = "CREATE TABLE users " + "(" + "user_id BIGINT NOT NULL AUTO_INCREMENT," + "net SMALLINT NOT NULL," + "net_uid VARBINARY(20) NOT NULL, " + "last_seen DATE NOT NULL, " + "PRIMARY KEY (user_id)" + ") ENGINE=InnoDB";
	public final static String USER_TABLE_DROP = "DROP TABLE users";

	public final static String USER_TABLE_NET_NET_UID_INDEX = "CREATE UNIQUE INDEX net_net_uid_idx ON users" + "(net,net_uid)";

	public final static String KEYS_TABLE = "CREATE TABLE pubkeys " + "(" + "user_id BIGINT NOT NULL, last_seen DATE NOT NULL, " + "pubkey_nick VARCHAR(255) NOT NULL," + "pubkey_sha1 VARBINARY(20) NOT NULL," + "pubkey BLOB NOT NULL," + "FOREIGN KEY (user_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT" + ") ENGINE=InnoDB";
	public final static String KEYS_TABLE_USER_ID_INDEX = "CREATE INDEX key_user_id_idx ON pubkeys" + "(user_id)";
	public final static String KEYS_TABLE_LAST_SEEN_INDEX = "CREATE INDEX key_last_seen_idx ON pubkeys" + "(last_seen)";
	public final static String KEYS_TABLE_KEY_INDEX = "CREATE INDEX key_pubkey_id_idx ON pubkeys" + "(pubkey_sha1)";
	public final static String KEYS_TABLE_USER_ID_KEY_INDEX = "CREATE INDEX key_user_pubkey_id_idx ON pubkeys" + "(user_id,pubkey_sha1)";
	public final static String KEYS_TABLE_DROP = "DROP TABLE pubkeys";

	public final static String FRIENDS_TABLE = "CREATE TABLE friends " + "(" + "user_id BIGINT NOT NULL, " + "friend_id BIGINT NOT NULL, " + "FOREIGN KEY (user_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT, " + "FOREIGN KEY (friend_id) " + "REFERENCES users (user_id) ON DELETE CASCADE ON UPDATE RESTRICT " + ") ENGINE=InnoDB";
	public final static String FRIENDS_TABLE_UID_INDEX = "CREATE INDEX friends_uid_idx ON friends" + "(user_id)";
	public final static String FRIENDS_TABLE_FID_INDEX = "CREATE INDEX friends_fid_idx ON friends" + "(friend_id)";
	public final static String FRIENDS_TABLE_UID_FID_INDEX = "CREATE UNIQUE INDEX friends_uid_fid_idx ON friends" + "(user_id,friend_id)";

	public final static String FRIENDS_TABLE_DROP = "DROP TABLE friends";

	public static void createTables(Connection conn, boolean dropIfExists) throws SQLException {
		Statement s = conn.createStatement();

		// if drop, from the tables
		if (dropIfExists) {
			try {
				s.execute(FRIENDS_TABLE_DROP);
				System.out.println("dropped table: friends");
			} catch (Exception e) {
				if (e.getMessage().equals("Unknown table 'friends'")) {
					System.out.println("friends table not found");
				} else {
					e.printStackTrace();
				}
			}
			try {
				s.execute(KEYS_TABLE_DROP);
				System.out.println("dropped table: pubkeys");
			} catch (Exception e) {
				if (e.getMessage().equals("Unknown table 'pubkeys'")) {
					System.out.println("pubkeys table not found");
				} else {
					e.printStackTrace();
				}
			}
			try {
				s.execute(USER_TABLE_DROP);
				System.out.println("dropped table: users");
			} catch (Exception e) {
				if (e.getMessage().equals("Unknown table 'users'")) {
					System.out.println("friends table not found");

				} else {
					e.printStackTrace();
				}
			}
		}

		/*
		 * create the tables
		 */
		TablesMySQL.createUsersTable(s);
		TablesMySQL.createKeysTable(s);
		TablesMySQL.createFriendsTable(s);
		s.close();
		conn.commit();

	}

	private static void createUsersTable(Statement s) throws SQLException {
		try {
			s.execute(TablesMySQL.USER_TABLE);
			s.execute(USER_TABLE_NET_NET_UID_INDEX);
			System.out.println("Created table users");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table 'users' already exists")) {

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
			s.execute(KEYS_TABLE_LAST_SEEN_INDEX);
			System.out.println("Created table pubkeys");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table 'pubkeys' already exists")) {
			} else {
				throw e;
			}
		}
	}

	private static void createFriendsTable(Statement s) throws SQLException {

		try {
			s.execute(TablesMySQL.FRIENDS_TABLE);
			s.execute(FRIENDS_TABLE_UID_INDEX);
			s.execute(FRIENDS_TABLE_FID_INDEX);
			s.execute(FRIENDS_TABLE_UID_FID_INDEX);
			System.out.println("Created table friends");
		} catch (SQLException e) {
			if (e.getMessage().equals("Table 'friends' already exists")) {

			} else {
				throw e;
			}
		}
	}
}
