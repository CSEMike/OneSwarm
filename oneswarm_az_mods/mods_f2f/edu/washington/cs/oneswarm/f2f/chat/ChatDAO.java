/**
 * this class is (unfortunately) invoked via reflection from the core to add the chat message count to the system tray, so if
 * changing packages, need to update SystemTraySWT.
 */
package edu.washington.cs.oneswarm.f2f.chat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemProperties;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.ui.gwt.BackendErrorLog;

public class ChatDAO {

	private static Logger logger = Logger.getLogger(ChatDAO.class.getName());

	private static ChatDAO inst = null;

	private final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private final String DB_CONNECT = "jdbc:derby:OneSwarm;create=true;databaseName=chat";

	private Connection mDB = null;

	private final String [] CREATE_TABLES = {
			"CREATE TABLE messages " +
			"( " +
			"	uid BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
			"	public_key	VARCHAR(1024), " +
			"	nick_at_receive VARCHAR(256), " + // in case the friend is deleted before we read the message and we can't resolve the name later
			"	message	VARCHAR(2048), " +
			"	unread SMALLINT DEFAULT 1, " + // since derby doesn't have boolean
			"	outgoing SMALLINT DEFAULT 1, " + // did we send this or receive it?
			"	mtimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
			// added in v0.7
			" 	sent SMALLINT DEFAULT 1" + // keep in sync with upgrade_tables, only makes sense for outgoing messages.
			")"
		};

	class ChatScratch {
		public String plaintextMessage;
		public long timestamp;
		public Friend remoteFriend;
		public boolean outgoing;
	};

	/**
	 * After this limit is reached, received chat messages are dropped, a warning is logged (once)
	 */
	private static final int MAX_QUEUE_SIZE = 128;
	private boolean overflowWarning = false;
	private final BlockingQueue<ChatScratch> mToProcess = new LinkedBlockingQueue<ChatScratch>();

	private ChatDAO() {
		// allow this to be overridden
		if( System.getProperty("derby.system.home") == null )
			System.setProperty("derby.system.home", SystemProperties.getUserPath());

		System.setProperty("derby.storage.PageCacheSize", "50");

		// Create the Derby DB
		try
		{
			Class.forName(DRIVER);
		}
		catch( ClassNotFoundException e )
		{
			logger.severe(e.toString());
		}

		try
		{
			mDB = DriverManager.getConnection(DB_CONNECT);
		}
		catch( SQLException e )
		{
			logger.severe(e.toString());
			e.printStackTrace();
		}

		create_tables();
		upgrade_tables();
		start_dequeuer();
	}

	private synchronized void upgrade_tables() {
		ResultSet rs = null;
		PreparedStatement stmt = null;
		try {

			rs = mDB.getMetaData().getColumns(null, null, "MESSAGES", null);

			int count = 0;
			while( rs.next() ) {
				count++;
				if( rs.getString("COLUMN_NAME").equals("SENT") ) {
					logger.fine("messages table already has sent column, skipping upgrade.");
					return;
				}
			}

			logger.info("no sent column, attempting upgrade of messages table. (count: " + count + ")");

			stmt = mDB.prepareStatement("ALTER TABLE messages ADD sent SMALLINT DEFAULT 1");
			stmt.executeUpdate();

			logger.fine("sent columns added");

		} catch (SQLException e) {
			e.printStackTrace();
			logger.warning("Error when attempting table upgrade check: " + e.toString());
		} finally {
			if( rs !=  null ) {
				try { rs.close(); } catch( SQLException e ) {}
			}
			if( stmt != null ) {
				try { stmt.close(); } catch( SQLException e ) {}
			}
		}
	}

	private void start_dequeuer() {
		Thread dequeuer = new Thread("ChatDAO message dequeuer") {
			@Override
			public void run() {
				PreparedStatement stmt = null;
					try {
						stmt = mDB.prepareStatement("INSERT INTO messages (public_key, nick_at_receive, message, outgoing) VALUES (?, ?, ?, 0)");
					} catch (SQLException e1) {
						logger.severe("SQL error with chat DAO dequeuer: " + e1.toString());
						e1.printStackTrace();
						BackendErrorLog.get().logException(e1);
					}
					while( true ) {
						try {
							ChatScratch chat = mToProcess.take();

							synchronized(ChatDAO.this)
							{
								stmt.setString(1, new String(Base64.encode(chat.remoteFriend.getPublicKey())));
								stmt.setString(2, chat.remoteFriend.getNick());
								stmt.setString(3, chat.plaintextMessage);

								stmt.executeUpdate();
							}

							logger.finer("inserted received chat message into DB: " + chat.plaintextMessage + " from " + chat.remoteFriend.getNick());
						} catch( Exception e ) {
							logger.warning("**** Unhandled chat dequeuer thread error: " + e.toString());
							e.printStackTrace();
							BackendErrorLog.get().logException(e);
						} finally {
							if( stmt != null ) {
								try {
									stmt.close();
								} catch( Exception e ) {}
							}
						}
					}

			}
		};
		dequeuer.setDaemon(true);
		dequeuer.start();
	}

	private void create_tables() {
		try
		{
			Statement s = mDB.createStatement();

			for( String t : CREATE_TABLES )
			{
				try {
					s.execute(t);
				} catch( Exception e ) {
					if( e.toString().endsWith("already exists in Schema 'APP'.") )
					{
						; // this is fine.
					} else {
						logger.warning(e.toString() + " / " + t);
					}
				}
			}

			s.close();
		} catch( SQLException e ) {
			e.printStackTrace();
			logger.warning(e.toString());
		}
	}

	public synchronized void dropTables() {
		Statement s = null;
		try {
			s = mDB.createStatement();
			s.execute("DROP TABLE messages");
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				s.close();
			} catch( SQLException e ) {}
		}
	}

	public synchronized void recordOutgoing( Chat inOutgoing, String inBase64PublicKey ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("INSERT INTO messages (public_key, nick_at_receive, message, outgoing, unread, sent) VALUES (?, ?, ?, 1, 1, ?)");
			stmt.setString(1, inBase64PublicKey); // store the remote user's public key since this is a chat with them
			stmt.setString(2, inOutgoing.getNick());
			stmt.setString(3, inOutgoing.getMessage());
			stmt.setShort(4, inOutgoing.isSent() ? (short)1 : (short)0);

			stmt.executeUpdate();
		}
		catch( Exception e ) {
			e.printStackTrace();
		} finally {
			if( stmt != null ) {
				try {
					stmt.close();
				} catch( SQLException e ) {}
			}
		}
	}

	public synchronized List<Chat> getQueuedMessagesForUser( String inBase64Key ) {
		PreparedStatement stmt = null;
		try {

			stmt = mDB.prepareStatement("SELECT * FROM messages WHERE public_key = ? AND sent = 0 ORDER BY mtimestamp ASC");
			stmt.setString(1, inBase64Key);

			ResultSet rs = stmt.executeQuery();
			List<Chat> out = new ArrayList<Chat>();

			int unsent = 0;

			while( rs.next() ) {
				out.add(Chat.fromResultSet(rs));
				unsent++;
			}

			return out;

		} catch( SQLException e ) {
			e.printStackTrace();
		} finally {
			if( stmt != null ) {
				try { stmt.close(); } catch( SQLException e2 ) {}
			}
		}
		return new ArrayList<Chat>();
	}

	public synchronized void markSent( long uid ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("UPDATE messages SET sent = 1 WHERE uid = ?"  );
			stmt.setLong(1, uid);
			stmt.executeUpdate();
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( SQLException e ) {}
		}
	}

	public synchronized List<Chat> getMessagesForUser( String inBase64Key, boolean include_read, int limit ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("SELECT * FROM messages WHERE public_key = ? "
					+ (include_read == false ? " AND unread = 1" : "")
					+ " ORDER BY mtimestamp DESC"  );
			if( limit > 0 ) {
				stmt.setMaxRows(limit);
			}
			stmt.setString(1, inBase64Key);

			ResultSet rs = stmt.executeQuery();
			List<Chat> out = new ArrayList<Chat>();

			Set<Long> toMark = new HashSet<Long>();
			while( rs.next() ) {
				Chat neu = Chat.fromResultSet(rs);
				out.add(neu);

				toMark.add(rs.getLong("uid"));
			}

			Collections.reverse(out); // new stuff at the bottom

			for( Long l : toMark ) {
				markRead(l);
			}

			return out;
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( SQLException e ) {}
		}
		return null;
	}

	public synchronized List<String> getUsersWithMessages() {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("SELECT DISTINCT public_key FROM messages");

			ResultSet rs = stmt.executeQuery();
			List<String> out = new ArrayList<String>();

			while( rs.next() ) {
				out.add(rs.getString("public_key"));
			}

			return out;
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( SQLException e ) {}
		}
		return null;
	}

	public synchronized boolean markRead( long uid ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("UPDATE messages SET unread=0 WHERE uid=?");
			stmt.setLong(1, uid);
			return stmt.executeUpdate() == 1;
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( SQLException e ) {}
		}
		return false;
	}

	public synchronized boolean deleteMessage( long uid ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("DELETE FROM messages WHERE uid=?");
			stmt.setLong(1, uid);
			return stmt.executeUpdate() == 1;
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( Exception e ) {}
		}
		return false;
	}

	public synchronized int deleteUsersMessages( String inBase64PublicKey ) {
		PreparedStatement stmt = null;
		try {
			stmt = mDB.prepareStatement("DELETE FROM messages WHERE public_key=?");
			stmt.setString(1, inBase64PublicKey);
			return stmt.executeUpdate();
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( Exception e ) {}
		}
		return 0;
	}

	public synchronized static ChatDAO get() {
		if( inst == null ) {
			inst = new ChatDAO();
		}
		return inst;
	}

	/**
	 * Get this off the FriendConnection thread ASAP -- the SQL insert could
	 * take a bit of time (and involve disk)
	 */
	public void queuePlaintextMessageForProcessing( String plaintextMessage, Friend remoteFriend ) {
		if( plaintextMessage.trim().length() == 0 ) {
			return;
		}

		if( mToProcess.size() > MAX_QUEUE_SIZE ) {

			if( overflowWarning == false ) {
				overflowWarning = true;
				logger.warning("Overflowed text message queue, dropping: " + plaintextMessage);
			}

			return;
		}

		try {
			ChatScratch rc = new ChatScratch();
			rc.plaintextMessage = plaintextMessage;
			rc.timestamp = System.currentTimeMillis();
			rc.remoteFriend = remoteFriend;

			mToProcess.put(rc);
		} catch (InterruptedException e) {
			logger.warning(e.toString());
			e.printStackTrace();
		}
	}

	public static void main( String[] args ) throws Exception
	{
		COConfigurationManager.preInitialise();
		//AzureusCoreFactory.create().start();

		try {
			final LogManager logManager = LogManager.getLogManager();
			logManager.readConfiguration(new FileInputStream("./logging.properties"));
			System.out.println("read log configuration");
		} catch( Exception e ) {
			e.printStackTrace();
		}

		ChatDAO rep = ChatDAO.get();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		Statement s = null;
		s = rep.mDB.createStatement();

//		rep.getSoftStateSync().refreshRemoteID(LocalIdentity.get().getKeys().getPublic(), new SoftStateListener(){
//			public void refresh_complete( PublicKey inID )
//			{
//				logger.fine("refresh complete");
//				try {
//					logger.fine(ReputationDAO.get().get_soft_state(inID));
//				} catch( Exception e ) {
//					e.printStackTrace();
//				}
//			}});

		while( true )
		{
			String line;

			System.out.print( "\n> " );
			System.out.flush();
			line = in.readLine();
			String [] toks = line.split("\\s+");

			try
			{
				if( line.equals("create") )
				{
					rep.create_tables();
				}
				else if( line.equals("test") ) {
//					System.out.println(" inserted: " + s.executeUpdate("INSERT INTO messages (public_key, nick_at_receive, message) VALUES ('1', 'nick', 'foo')"));
//					System.out.println(" inserted: " + s.executeUpdate("INSERT INTO messages (public_key, nick_at_receive, message) VALUES ('2', 'nick2', 'foo2')"));
//					System.out.println(" inserted: " + s.executeUpdate("INSERT INTO messages (public_key, nick_at_receive, message) VALUES ('1', 'nick', 'foo2')"));

					ResultSet rs = s.executeQuery("select * from messages");
					rs.next();
					String key = rs.getString("public_key");
					System.out.println("key is: " + key);
					System.out.println("key is: " + key.replaceAll("\n", ""));

					break;

				} else if( line.startsWith("remove" )) {
					long which = Long.parseLong(line.split("\\s+")[1]);
					rep.deleteMessage(which);
				}else if( line.startsWith("read" )) {
					long which = Long.parseLong(line.split("\\s+")[1]);
					rep.markRead(which);
				}else if( line.startsWith("deluser" )) {
					String which = line.split("\\s+")[1];
					rep.deleteUsersMessages(which);
				} else if( line.startsWith("getusers")) {
					for( String k : rep.getUsersWithMessages() ) {
						System.out.println(k);
					}
				}
				else if( line.startsWith("show") )
				{
					System.out.println(rep.dump_table(toks[1]));
				} else if( line.equals("q") ) {
					return;
				} else {
					if( line.toLowerCase().startsWith("select") )
					{
						int count =0;
						ResultSet rs = s.executeQuery(line);
						printResultSet(System.out, rs);
					}
					else
						System.out.println( s.execute(line) + "" );
				}
			}
			catch( SQLException e )
			{
				System.err.println(e);
				e.printStackTrace();
			}
		}
	}

	/**
	 * this method is (unfortunately) invoked via reflection from the core to add the chat message count to the system tray, so if
	 * renaming, need to update SystemTraySWT.
	 */
	public synchronized HashMap<String, Integer> getUnreadMessageCounts() {
		HashMap<String, Integer> out = new HashMap<String, Integer>();
		Statement stmt = null;
		try {
			stmt = mDB.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT public_key, COUNT(*) FROM messages WHERE unread=1 GROUP BY public_key");
			while( rs.next() ) {
				out.put(rs.getString("public_key"), rs.getInt(2));
			}
		} catch( Exception e ) {
			e.printStackTrace();
		} finally {
			try {
				stmt.close();
			} catch( Exception e ) {}
		}
		return out;
	}

	public String dump_table(String tableName) {
		ByteArrayOutputStream backing = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(backing);
		try {
			Statement s = mDB.createStatement();
			ResultSet rs = s.executeQuery("select * from " + tableName);
			Map<String, Integer> cols = new HashMap<String, Integer>();

			printResultSet(out, rs);

			return backing.toString();
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return null;
	}

	public static void printResultSet( PrintStream out, ResultSet rs ) throws Exception{
		ResultSetMetaData md = rs.getMetaData();
		out.println("col count: " + md.getColumnCount());

		for( int i=1; i<=md.getColumnCount(); i++ )
		{
			out.print( md.getColumnLabel(i) + " " );
		}
		out.println("");

		while( rs.next() )
		{
			for( int i=1; i<=md.getColumnCount(); i++ )
			{
				out.printf( "%" + md.getColumnLabel(i).length() + "s ", rs.getObject(i) == null ? "null" : rs.getObject(i).toString() );
				out.flush();
			}
			out.flush();
			out.println("");
		}
	}
}
