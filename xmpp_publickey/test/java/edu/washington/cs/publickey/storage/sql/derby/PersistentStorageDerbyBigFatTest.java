package edu.washington.cs.publickey.storage.sql.derby;



import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQLBigFatTest;


public class PersistentStorageDerbyBigFatTest extends PersistentStorageSQLBigFatTest {
	@Before
	public void setUp() throws Exception {
		System.out.println("*******************setup");
		try {
			Properties props = new Properties();
			props.put(PersistentStorageDerby.key_db_username, PersistentStorageDerbyTest.username);
			props.put(PersistentStorageDerby.key_db_password, PersistentStorageDerbyTest.password);
			props.put(PersistentStorageDerby.key_db_path, PersistentStorageDerbyTest.DATA_BASE_DIR.getCanonicalPath());
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
