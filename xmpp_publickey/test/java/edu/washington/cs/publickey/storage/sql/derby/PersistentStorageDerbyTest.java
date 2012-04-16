/**
 * 
 */
package edu.washington.cs.publickey.storage.sql.derby;

import static org.junit.Assert.fail;

import java.io.File;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQLTest;

/**
 * @author isdal
 * 
 */
public class PersistentStorageDerbyTest extends PersistentStorageSQLTest {

	public static final File DATA_BASE_DIR = new File("/tmp/test1");
	public static final String username = "publickey";
	public final static String password = "";

	public PersistentStorageDerbyTest() {

	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("*******************setup");
		try {
			Properties props = new Properties();
			props.put(PersistentStorageDerby.key_db_username, username);
			props.put(PersistentStorageDerby.key_db_password, password);
			props.put(PersistentStorageDerby.key_db_path, DATA_BASE_DIR.getCanonicalPath());
			p = new PersistentStorageDerby(props);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}



	@Override
	protected void clearTables() throws SQLException {
		TablesDerby.createTables(p.getConnection(), true);
	}

}
