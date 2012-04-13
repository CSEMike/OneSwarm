package edu.washington.cs.publickey.storage.sql.mysql;



import java.sql.SQLException;
import java.util.Properties;

import org.junit.Before;

import edu.washington.cs.publickey.storage.sql.PersistentStorageSQLBigFatTest;


public class PersistentStorageMySQLBigFatTest extends PersistentStorageSQLBigFatTest {

	@Before
	public void setUp() throws Exception {
		p = new PersistentStorageMySQL(new Properties(),true);
	}

	@Override
	protected void clearTables() throws SQLException {
		TablesMySQL.createTables(p.getConnection(), true);
	}
}
