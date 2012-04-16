/**
 * 
 */
package edu.washington.cs.publickey.storage.sql.derby;

import java.io.File;
import java.sql.DriverManager;
import java.util.Properties;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQL;
import edu.washington.cs.publickey.storage.sql.QueryManager;


/**
 * @author isdal
 * 
 */
public class PersistentStorageDerby extends PersistentStorageSQL {
	private static final String DERBY_SYSTEM_HOME = "derby.system.home";
	private static final String DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String DERBY_PROTOCOL = "jdbc:derby:";
	public static final String key_db_username = "db_derby_username";
	public static final String key_db_password = "db_derby_password";
	public static final String key_db_path = "db_derby_path";
	

	public PersistentStorageDerby(Properties systemProperties) throws Exception {
		
		File dataBaseDir = new File(systemProperties.getProperty(key_db_path));
		System.out.println("Using derby storage at: " + dataBaseDir.getCanonicalPath());
		startTrace("PersistentStorageDerby");
		// check if all is right with the dir
		verifyDbDir(dataBaseDir);

		System.setProperty(DERBY_SYSTEM_HOME, dataBaseDir.getCanonicalPath());

		/*
		 * The driver is installed by loading its class. In an embedded
		 * environment, this will start up Derby, since it is not already
		 * running.
		 */
		try {
			Class.forName(DERBY_DRIVER).newInstance();
		} catch (ClassNotFoundException e) {
			throw new Exception("Unable to find derby library " + "(class path issues?): " + e.getMessage(), e);
		}
		// System.out.println("Loaded the appropriate driver.");

		Properties props = new Properties();
		props.put("user", systemProperties.getProperty(key_db_username));
		props.put("password", systemProperties.getProperty(key_db_password));

		/*
		 * The connection specifies create=true to cause the database to be
		 * created. To remove the database, remove the directory derbyDB and its
		 * contents. The directory derbyDB will be created under the directory
		 * that the system property derby.system.home points to, or the current
		 * directory if derby.system.home is not set.
		 */
		conn = DriverManager.getConnection(DERBY_PROTOCOL + "publickey;create=true", props);
		conn.setAutoCommit(false);
		// System.out.println("Connected to and created database publickey");
		TablesDerby.createTables(conn, false);

		queryManager = new QueryManager(conn);

		queryManager.deleteExpiredKeys();
		
		// System.out.println("Closed result set and statement");

		// System.out.println("Committed transaction and closed connection");

		// System.out.println("SimpleApp finished");
		endTrace("PersistentStorageDerby");
	}
}
