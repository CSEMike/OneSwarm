/**
 * 
 */
package edu.washington.cs.publickey.storage.sql.mysql;

import java.sql.DriverManager;
import java.util.Properties;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQL;

/**
 * @author isdal
 * 
 */
public class PersistentStorageMySQL extends PersistentStorageSQL {

	public static final String key_db_database = "db_mysql_database";
	public static final String key_db_username = "db_mysql_username";
	public static final String key_db_password = "db_mysql_password";
	public static final String key_db_host = "db_mysql_host";
	public static final String key_db_port = "db_mysql_port";

	private final static String DEFAULT_USERNAME = "root";
	private final static String DEFAULT_PASSWORD = "";
	private final static String DEFAULT_DATABASE = "publickey";
	private final static String DEFAULT_HOSTNAME = "localhost";
	private final static String DEFAULT_PORT = "3306";

	public PersistentStorageMySQL(Properties systemProperties, boolean dropOldTables) throws Exception {

		startTrace("PersistentStorageMySQL");
		String database = systemProperties.getProperty(key_db_database, DEFAULT_DATABASE);
		String username = systemProperties.getProperty(key_db_username, DEFAULT_USERNAME);
		String hostname = systemProperties.getProperty(key_db_host, DEFAULT_HOSTNAME);
		String password = systemProperties.getProperty(key_db_password, DEFAULT_PASSWORD);
		int port = Integer.parseInt(systemProperties.getProperty(key_db_port, DEFAULT_PORT));

		System.out.println("Using mysql storage at: " + hostname + ":" + port + "/" + database);

		/*
		 * load the mysql driver
		 */
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		/*
		 * and create the connection
		 */
		conn = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database + "?" + "user=" + username + "&password=" + password);

		// System.out.println("Loaded the appropriate driver.");

		conn.setAutoCommit(false);
		// System.out.println("Connected to and created database publickey");
		TablesMySQL.createTables(conn, dropOldTables);

		queryManager = new QueryManagerMysql(conn);

		// System.out.println("Closed result set and statement");

		// System.out.println("Committed transaction and closed connection");

		// System.out.println("SimpleApp finished");
		queryManager.deleteExpiredKeys();
		endTrace("PersistentStorageMySQL");
	}
}
