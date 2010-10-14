package edu.uw.cse.netlab.reputation.storage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.SystemProperties;

import sun.security.x509.CertAndKeyGen;
import edu.uw.cse.netlab.reputation.Computation;
import edu.uw.cse.netlab.reputation.LocalIdentity;
import edu.uw.cse.netlab.reputation.messages.Attestation;
import edu.uw.cse.netlab.utils.ByteManip;
import edu.uw.cse.netlab.utils.KeyManipulation;

public class ReputationDAO
{
	private static Logger logger = Logger.getLogger(ReputationDAO.class.getName());
	
	private static ReputationDAO mInst = null;
	
	private String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	private String DB_CONNECT = "jdbc:derby:OneSwarm;create=true;databaseName=peers";
	
	private String [] CREATE_TABLES = 
			{"CREATE TABLE keys" +
			"(" +
			"	public_key	VARCHAR(4096), " +
			"	db_id		BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY " +
			")",
			// Since we go from public_key -> db_id often as well. 
			"CREATE INDEX pub_key_index ON keys (public_key)", 
			
			"CREATE TABLE state" +
			"(" +
			"	remote_id	BIGINT NOT NULL PRIMARY KEY, " +
			"" + // If remote_id and I are exchanging data directly
			"	sent_direct BIGINT DEFAULT 0, " +
			"	received_direct BIGINT DEFAULT 0, " +
			"" + // If remote_id is acting as intermediary between me and others
			"	local_sent_due_to_remote_reco BIGINT DEFAULT 0, " +
			"	local_recv_due_to_remote_reco BIGINT DEFAULT 0, " +
			"" + // If I'm acting as the intermediary between remote_id and others
			"	others_sent_due_to_my_reco BIGINT DEFAULT 0, " +
			"	others_recv_due_to_my_reco BIGINT DEFAULT 0, " +
			"" + // When is the last time any of the above data was touched
			"	last_aged TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
			"" + // How many times have 1) I seen this peer and 2) others seen this peer 
			"	my_observations INTEGER DEFAULT 0, " +
			"	indirect_observations DOUBLE PRECISION DEFAULT 0" +
			")", 
			
			"CREATE TABLE soft_state " +
			"(" +
			"	db_id	BIGINT NOT NULL PRIMARY KEY, " +
			"	last_ip	INTEGER DEFAULT 0, " +
			"	last_udp_port INTEGER DEFAULT 0, " +
			"	last_tcp_port INTEGER DEFAULT 0, " +
			"	last_update TIMESTAMP" +
			")",
			
			/**
			 * This avoids an effort to game topK selection by repeatedly connecting / disconnecting to people  
			 */
			"CREATE TABLE seen_today " +
			"(" +
			"	ip	INTEGER NOT NULL, " +
			"	infohash CHAR(40) NOT NULL, " + 
			"	time TIMESTAMP NOT NULL, " +
			"" +
			"	PRIMARY KEY(ip)" +
			")",
			
			"CREATE TABLE attestations " +
			"(" +
			"	signer BIGINT NOT NULL, " + 
			"	encoding_for BIGINT NOT NULL, " + 
			"" +
			"	intermediary_id BIGINT NOT NULL, " +
			"" +
			"	time TIMESTAMP NOT NULL, " +
			"" +
			"	needs_currency_verification SMALLINT DEFAULT 0, " + // this should be a BOOLEAN, but derby doesn't support it
			"	received_due_to_reco_offset_with_int INT DEFAULT 0, " +
			"	last_verification_attempt TIMESTAMP, " +
			"	verification_attempts SMALLINT DEFAULT 0, " +
			"" +
			"	bytes BLOB, " +
			"" +
			"	attest_id		BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY" +
			")", 
			// All the fields we will select on
			"CREATE INDEX attestation_index ON attestations (signer, encoding_for, time, last_verification_attempt, needs_currency_verification)", 
			
			// Record identifiers for updates that we've processed to prevent replays
			"CREATE TABLE processed_updates " +
			"(" +
			"	hash	CHAR(20) PRIMARY KEY " +
			")"
			};
	
	private Connection mDB = null; 
	
	private LocalTopK mTopKCache = null;
	private long mLastTopKRefresh = 0;
	
	private SoftStateSync mSoftStateSync = new SoftStateSync();
	public SoftStateSync getSoftStateSync() { return mSoftStateSync; }
	
	private ReputationDAO() 
	{
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
		
		logger.fine("starting table pruning timer");
		(new Timer("table prune", true)).schedule(new TimerTask(){
			public void run() {
				logger.fine("prune tables, named!");
				prune_tables();
			}}, 60*1000, 60*60*1000 );
	}
	
	private synchronized void prune_tables()
	{
		PreparedStatement stmt = null;
		try
		{
			stmt = mDB.prepareStatement("DELETE FROM seen_today WHERE time < ?");
			// TODO: magic number -- this should probably be less than 1 day -- maybe hrs? 
			stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis() - 86400000) ); // 1 day
			int pruned = stmt.executeUpdate();
			
			if( pruned == 0 ) {
				logger.fine("pruned " + pruned + " entries from seen_today");
			} else { 
				logger.fine("pruned " + pruned + " entries from seen_today");
			}
		}
		catch( SQLException e )
		{
			logger.severe(e.toString());
			e.printStackTrace();
		}
		finally
		{
			if( stmt != null )
			{
				try {
					stmt.close();
				} catch( Exception e ) {}
			}
		}
	}
	
	private synchronized void create_tables() 
	{
		try 
		{
			Statement s = mDB.createStatement();
			
//			Statement stmt = mDB.createStatement();
//			//for( String t : new String[]{"keys", "state", "soft_state", "attestations", "processed_updates"} )
//			for( String t : new String[]{"soft_state"} )	
//			{
//				try {
//					stmt.executeUpdate("DROP TABLE " + t);
//				} catch( Exception e ) {
//					Debug.out(e);
//				}
//			}
//			stmt.close();
		
			for( String t : CREATE_TABLES )
			{
				try {
					s.execute(t);
				} catch( Exception e ) {
					if( e.toString().endsWith("already exists in Schema 'APP'.") == false )
					{
						logger.warning(e.toString());
					}
				}
			}
			
			// first key is always ours:
			try
			{
				long int_id = get_internal_id(LocalIdentity.get().getCertificate().getPublicKey());
				String encoded = KeyManipulation.concise(LocalIdentity.get().getCertificate().getPublicKey().getEncoded());
				logger.fine("local internal_id: " + int_id + " " + encoded);
			} catch( IOException e )
			{
				logger.warning(e.toString());
				e.printStackTrace();
			}
				
			s.close();
		} catch( SQLException e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the topK set by observations (both direct and indirect weighted equally)
	 * TODO: make this cleaner, move function somewhere else?
	 * 
	 * @return topK peers by observations, descending
	 * @throws IOException
	 */
	public synchronized LocalTopK get_topK_by_obs() throws IOException
	{
		// only recompute this every 5 minutes
		if( mTopKCache != null && (mLastTopKRefresh + 5*60*1000) > System.currentTimeMillis() )
		{
			return mTopKCache;
		}
		
		logger.fine("recomputing top K");
		mLastTopKRefresh = System.currentTimeMillis();
		
		// TODO: keep per-execution change bits on these so we aren't constantly recomputing these during exec.
		// could be a pain to do so when the set of peers gets towards 4k... (then again, most peers should have occs <=1, so maybe 
		// we should just prune to that)
		final Map<Long, Double> peer_to_score = new HashMap<Long, Double>();
		
		String sql = "SELECT remote_id, my_observations + indirect_observations AS occs FROM state ORDER BY occs DESC";
		Statement stmt = null;
		PreparedStatement key_lookup = null, attest_lookup = null;
		List<PublicKey> topK = new ArrayList<PublicKey>();
		try 
		{
			stmt = mDB.createStatement();
			//stmt.setMaxRows(4000);
			key_lookup = mDB.prepareStatement("SELECT public_key FROM keys WHERE db_id = ?");
			attest_lookup = mDB.prepareStatement(
					"SELECT bytes FROM attestations WHERE " +
					"	encoding_for = 1 AND " +
					"	signer = ? " +
					"ORDER BY time DESC");
			//attest_lookup.setMaxRows(1);
			
			ResultSet rs = stmt.executeQuery(sql);
			
			while( rs.next() )
			{
				long candidate_id = rs.getLong(1);
				
				logger.fine("topk considering candidate: " + candidate_id);
				
				// 1. do we have an attestation for this popular intermediary? 
				attest_lookup.setLong(1, candidate_id);
				ResultSet attest_rs = attest_lookup.executeQuery();
				if( attest_rs.next() == false )
				{
					attest_rs.close();
					logger.fine("no attest");
					continue;
				}
				
				// 2. recover the receipt info from the attestation
				Blob blob = attest_rs.getBlob("bytes");
				Receipt latest_candidate_receipt = (Receipt) ByteManip.objectFromBytes(blob.getBytes(1, (int)blob.length()));
				
				// 3. Update the receipt with anything we've done to cash in on standing with this intermediary 
				latest_candidate_receipt.peer_received_due_to_reco = get_local_recv_due_to_remote_reco(candidate_id);
				
				// 4. The score of this candidate is their perception of our quality, computed here
				peer_to_score.put(candidate_id, Computation.peer_value_at_intermediary(latest_candidate_receipt));
				
				logger.fine("score: " + peer_to_score.get(candidate_id));
			}
			
			// We sort the scores and take the top 2000 positive ones as our top K set 
			Long [] scored_peers = peer_to_score.keySet().toArray(new Long[0]);
			Arrays.sort(scored_peers, new Comparator<Long>(){
				public int compare( Long o1, Long o2 ) {
					double diff = peer_to_score.get(o1) - peer_to_score.get(o2);
					if( diff > 0 )
						return -1;
					if( diff < 0 )
						return 1;
					return 0;
				}
			});
			logger.finest("sorted scored peers, first few...");
			for( int i=0; i<Math.min(scored_peers.length, 3); i++ )
				logger.finest(scored_peers[i] + " " + peer_to_score.get(scored_peers[i]));
			
			// TODO: (slightly less) magic constant
			for( int i=0; i<Math.min(2000, scored_peers.length); i++ )
			{
				long candidate_id = scored_peers[i];
				
				// TODO: should we include these? risk is that they will be chosen over peers with which we have better standing, hurting our performance, 
				// but finding ANY shared intermediary might be valuable in other cases. Check this later
				if( peer_to_score.get(candidate_id) < 1.0 )
					break;
				
				key_lookup.setLong(1, candidate_id);
				ResultSet keyRS = key_lookup.executeQuery();
				if( keyRS.next() == false )
					throw new IOException("Inconsistent DB: key not found given db id: " + candidate_id);
				PublicKey key = KeyManipulation.keyForEncodedBytes(ByteFormatter.decodeString(keyRS.getString(1)));
				topK.add(key);
			}
			
			logger.fine("top K has " + topK.size());
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		} 
		catch( InvalidKeySpecException e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				if( stmt != null ) stmt.close();
				if( key_lookup != null ) key_lookup.close();
				if( attest_lookup != null ) attest_lookup.close();
			} catch( Exception e ) {}
		}
		
		mTopKCache = new LocalTopK(topK.toArray(new PublicKey[0]));
		
		return mTopKCache;
	}
	
	public synchronized PublicKey get_public_key( long inInternalID ) throws IOException 
	{
		PublicKey result = null;
		PreparedStatement stmt = null;
		
		try 
		{
			stmt = mDB.prepareStatement("SELECT public_key FROM keys where db_id = ?");
			stmt.setLong(1, inInternalID);
			ResultSet rs = stmt.executeQuery();
			if( rs.next() != false )
				result = KeyManipulation.keyForEncodedBytes(ByteFormatter.decodeString(rs.getString(1)));
		}
		catch( Exception e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				stmt.close();
			} catch( Exception e ) {}
		}
		return result;
	}
	
	public synchronized long get_internal_id( PublicKey inPubKey ) throws IOException
	{
		try 
		{
			PreparedStatement stmt = mDB.prepareStatement("SELECT db_id FROM keys where public_key = ?");
			stmt.setString(1, ByteFormatter.encodeString(inPubKey.getEncoded()));
			ResultSet rs = stmt.executeQuery();
			boolean inserting = false;
			if( rs.next() == false )
			{
				// need to insert and get ID
				stmt.close();
				
				stmt = mDB.prepareStatement("INSERT INTO keys (public_key) VALUES (?)");
				stmt.setString(1, ByteFormatter.encodeString((inPubKey.getEncoded())));
				if( stmt.executeUpdate() != 1 )
					throw new IOException("Insert into keys DB didn't update anything");
				stmt.close();
				
				// reissue query
				stmt = mDB.prepareStatement("SELECT db_id FROM keys where public_key = ?");
				stmt.setString(1, ByteFormatter.encodeString(inPubKey.getEncoded()));
				rs = stmt.executeQuery();
				if( rs.next() == false )
					throw new IOException("Couldn't retrieve db_id immediately after insertion");
				
				inserting = true;
			}
			
			long id = rs.getLong(1);
			stmt.close();
			
			if( inserting )
			{
				// create a state entry for this. ensures that for all keys entries we have a state entry.
				stmt = mDB.prepareStatement("INSERT INTO state (remote_id) VALUES (?)");
				stmt.setLong(1, id);
				if( stmt.executeUpdate() != 1 )
					throw new IOException("Insert into keys DB didn't update anything");
				stmt.close();
			}
			mDB.commit();
			
			return id;
		}
		catch( Exception e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
	
	public void sent_direct( long inID, long bytes ) throws IOException { increment_field(inID, "sent_direct", bytes); }
	public void received_direct( long inID, long bytes ) throws IOException { increment_field(inID, "received_direct", bytes); }
	public long get_sent_direct( long inID ) throws IOException { return retrieve_long(inID, "sent_direct"); }
	public long get_received_direct( long inID ) throws IOException { return retrieve_long(inID, "received_direct"); }
	
	public void local_sent_due_to_remote_reco( long inID, long bytes ) throws IOException { increment_field(inID, "local_sent_due_to_remote_reco", bytes); }
	public void local_recv_due_to_remote_reco( long inID, long bytes ) throws IOException { increment_field(inID, "local_recv_due_to_remote_reco", bytes); }
	public long get_local_sent_due_to_remote_reco( long inID ) throws IOException { return retrieve_long(inID, "local_sent_due_to_remote_reco"); }
	public long get_local_recv_due_to_remote_reco( long inID ) throws IOException { return retrieve_long(inID, "local_recv_due_to_remote_reco"); }
	
	public void others_sent_due_to_my_reco( long inID, long bytes ) throws IOException { increment_field(inID, "others_sent_due_to_my_reco", bytes); }
	public void others_recv_due_to_my_reco( long inID, long bytes ) throws IOException { increment_field(inID, "others_recv_due_to_my_reco", bytes); }
	public long get_others_sent_due_to_my_reco( long inID ) throws IOException { return retrieve_long(inID, "others_sent_due_to_my_reco"); }
	public long get_others_recv_due_to_my_reco( long inID ) throws IOException { return retrieve_long(inID, "others_recv_due_to_my_reco"); }
	
	
	public synchronized String get_soft_state( PublicKey inKey ) throws IOException 
	{
		long internal_id = get_internal_id(inKey);
		String result = null;
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("SELECT last_ip, last_udp_port FROM soft_state WHERE db_id = ?");
			
			stmt.setLong(1, internal_id);
			ResultSet rs = stmt.executeQuery();
			
			// TODO: probably a better way to encode this than in a string that needs to be split
			if( rs.next() )
			{
				result = ByteManip.ntoa(rs.getInt(1));
				result += " " + rs.getInt(2);
			}
			
			stmt.close();
		}
		catch( SQLException e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		
		return result;
	}
	
	public synchronized java.util.Date get_last_soft_state_update( PublicKey inKey ) throws IOException
	{
		long internal_id = get_internal_id(inKey);
		java.util.Date out_time = null;
		PreparedStatement stmt = null;
		try
		{
			stmt = mDB.prepareStatement("SELECT last_update FROM soft_state WHERE db_id = ?");
			
			stmt.setLong(1, internal_id);
			ResultSet rs = stmt.executeQuery();
			
			if( rs.next() )
				out_time = new java.util.Date(rs.getTime(1).getTime());
		}
		catch( SQLException e )
		{
			System.err.println(e);
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				stmt.close();
			}catch( Exception e ) {}
		}
		return out_time;
	}
	
	public synchronized void update_soft_state( PublicKey inKey, byte [] inIP, int inTCPPort, int inUDPPort, java.util.Date inTimestamp ) throws IOException
	{
		long internal_id = get_internal_id(inKey);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(inIP));
		int ip_int =  dis.readInt();
		
		// make sure a record exists. if not, we'll immediately update, so we can skimp on everything except primary key
		try
		{
			PreparedStatement st = mDB.prepareStatement("INSERT INTO soft_state (db_id) VALUES (?)");
			st.setLong(1, internal_id);
			st.executeUpdate();
			st.close();
		} catch( SQLException e ) {} // we're expecting duplicate key errors 
		
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("UPDATE soft_state SET last_ip = ?, last_udp_port = ?, last_tcp_port = ?, last_update = ? WHERE db_id = ?");
			
			stmt.setInt(1, ip_int);
			stmt.setInt(2, inUDPPort);
			stmt.setInt(3, inTCPPort);
			stmt.setTimestamp(4, new Timestamp(inTimestamp.getTime()));
			stmt.setLong(5, internal_id);
			
			int updated = stmt.executeUpdate();
			logger.fine("sstate update updated: " + updated);
			stmt.close();
			
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		
		logger.fine("updated soft state: " + InetAddress.getByAddress(inIP).toString());
	}
	
	private boolean is_daily_duplicate( InetAddress inIP, byte [] inInfohash ) throws IOException
	{
		PreparedStatement stmt = null;
		boolean dupe = false;
		try
		{
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(inIP.getAddress()));
			int ip_int =  dis.readInt();
			
			stmt = mDB.prepareStatement("SELECT ip FROM seen_today WHERE ip = ? AND infohash = ? AND time > ?");
			stmt.setLong(1, ip_int);
			stmt.setString(2, ByteFormatter.encodeString(inInfohash));
			stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis() - 86400000) ); // 1 day
			
			ResultSet rs = stmt.executeQuery();
			if( rs.next() )
				dupe = true;
		}
		catch( SQLException e )
		{
			e.printStackTrace();
		}
		finally
		{
			if( stmt != null )
			{
				try {
					stmt.close();
				} catch( Exception e ) {}
			}
		}
		return dupe;
	}
	
	public synchronized void direct_observation( long inID, InetAddress inIP, byte [] inInfohash ) throws IOException
	{
		PreparedStatement stmt = null;
		try 
		{
			if( is_daily_duplicate( inIP, inInfohash ) )
			{
				logger.fine("ignoring duplicate observation for " + inID);
				return;
			}
			
			stmt = mDB.prepareStatement("UPDATE state SET my_observations = my_observations + 1 WHERE remote_id = ?");
			stmt.setLong(1, inID);
			stmt.executeUpdate();
			
			DataInputStream dis = new DataInputStream(new ByteArrayInputStream(inIP.getAddress()));
			int ip_int =  dis.readInt();
			
			stmt = mDB.prepareStatement("INSERT INTO seen_today (ip, infohash, time) VALUES (?, ?, ?)");
			stmt.setLong(1, ip_int);
			stmt.setString(2, ByteFormatter.encodeString(inInfohash));
			stmt.setTimestamp(3, new Timestamp((new java.util.Date()).getTime()));
			stmt.executeUpdate();
			
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			if( stmt != null )
			{
				try {
					stmt.close();
				} catch( Exception e ) {}
			}
		}
	}
	
	protected synchronized void multiplicative_decrease_observations( long inID ) throws IOException
	{
		PreparedStatement stmt = null;
		try 
		{
			stmt = mDB.prepareStatement(
					"UPDATE state SET " +
					// TODO: magic constant -- multiplicative decrease amount
					"my_observations = my_observations * 0.95, " +
					"indirect_observations = indirect_observations * 0.95 " +
					"WHERE remote_id = ?");
			stmt.setLong(1, inID);
			stmt.executeUpdate();
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				stmt.close();
			} catch( Exception e ) {}
		}
	}
	
	public synchronized void indirect_observation( long inID, double inFraction ) throws IOException
	{
		if( inID == 1 )
		{
			logger.fine("skipping indirect observation of ourself");
			return;
		}
		
		logger.fine("recording indirect observation: " + inID + " / " + inFraction);
		
		try 
		{
			PreparedStatement stmt = mDB.prepareStatement("UPDATE state SET indirect_observations = indirect_observations + ? WHERE remote_id = ?");
			stmt.setLong(2, inID);
			stmt.setDouble(1, inFraction);
			stmt.executeUpdate();
			stmt.close();
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
	
	public synchronized void record_update( Receipt inUpdateReceipt ) throws IOException
	{
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("INSERT INTO attestations (" +
					"signer, " +
					"encoding_for, " +
					"time, " +
					"bytes, " +
					"intermediary_id, " +
					"received_due_to_reco_offset_with_int, " +
					"needs_currency_verification) VALUES (?, ?, ?, ?, 1, 0, 0)");
			stmt.setLong(1, inUpdateReceipt.getSigningID());
			stmt.setLong(2, inUpdateReceipt.getEncodingStateForID());
			stmt.setTimestamp(3, new Timestamp(inUpdateReceipt.getTimestamp().getTime()));
			stmt.setBytes(4, ByteManip.objectToBytes(inUpdateReceipt));
			stmt.executeUpdate();
			
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
	
	public synchronized void record_attestation( Attestation inAttestation, HashMap<Long, Receipt> inReceiptBundle ) throws IOException
	{
		try
		{
			Receipt recpt = inAttestation.getReceipt();
			PreparedStatement stmt = mDB.prepareStatement("INSERT INTO attestations (signer, encoding_for, time, bytes, intermediary_id, received_due_to_reco_offset_with_int, needs_currency_verification) VALUES (?, ?, ?, ?, ?, ?, ?)");
			
			/**
			 * When it comes time to verify, we care only about verifying the most recent state. Thus, we mark previous unverified indirect receipts
			 * here as no longer requiring verification. This prevents a massive flood of updates if we've batched up a bunch while an intermediary 
			 * was offline. 
			 * 
			 * Update -- for accounting accuracy, we actually _do_ retain these old receipts and verify them, so the actual update is
			 * commented out below.  
			 * 
			 */
			PreparedStatement mark_previous = mDB.prepareStatement(
					"UPDATE attestations " +
					"SET " +
					"	needs_currency_verification = 0 " +
					"WHERE " +
					"	signer = ? AND " +
					"	encoding_for = ? AND " +
					"	intermediary_id = ? AND " +
					"	time < ?");
			
			stmt.setLong(1, get_internal_id(recpt.getSigningKey()));
			stmt.setLong(2, get_internal_id(recpt.getEncodingStateForKey()));
			stmt.setTimestamp(3, new Timestamp(recpt.getTimestamp().getTime()));
			
			mark_previous.setLong(1, get_internal_id(recpt.getSigningKey()));
			mark_previous.setLong(2, get_internal_id(recpt.getEncodingStateForKey()));
			// 3 is set per-intermediary below
			mark_previous.setTimestamp(4, new Timestamp(recpt.getTimestamp().getTime()));
			
			stmt.setBytes(4, ByteManip.objectToBytes(recpt));
			
			// This isn't very efficient storage but makes the rest of the code and data storage (which is done on a per-int basis) much more
			// comprehensible. 
			Set<Long> preferred_ints = inAttestation.getReceipt().getPreferredIntermediaries();
			if( preferred_ints.size() > 0 ) // indirect attrib
			{
				logger.fine("recording indirect attesting receipt");
				stmt.setShort(7, (short)1);
				for( Long intermediary_local_id : preferred_ints )
				{
					assert !(intermediary_local_id == 1 && recpt.getEncodingStateForID() == 1) : "recording attestation for us with ourself as intermediary"; 
					
					stmt.setLong(5, intermediary_local_id);
					stmt.setInt(6, inReceiptBundle.get(intermediary_local_id).get_received_due_to_reco_offset());
					stmt.executeUpdate();
					
					mark_previous.setLong(3, intermediary_local_id);
					
					// We actually do verify these for accounting accuracy. 
//					int marked = mark_previous.executeUpdate();
//					logger.fine("mark previous marked: " + marked); 
				}
			}
			else
			{
				stmt.setShort(7, (short)0);
				logger.fine("recording direct attesting receipt");
				stmt.setLong(5, -1);
				stmt.setInt(6, -1);
				stmt.executeUpdate();
			}
			mDB.commit();
			logger.fine("recorded attestation: " + inAttestation);
		}
		catch( SQLException e )
		{
			throw new IOException(e.toString());
		}
	}
	
	public synchronized boolean is_duplicate_update( Receipt inReceipt ) throws IOException
	{
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("SELECT * FROM processed_updates WHERE hash = ?");
			stmt.setString(1, new String(inReceipt.getSHA1()));
			ResultSet rs = stmt.executeQuery();
			if( rs.next() )
				return true;
			return false;
		}
		catch( SQLException e )
		{
			throw new IOException(e.toString());
		}
	}
	
	public synchronized void record_processed_update( Receipt inReceipt ) throws IOException
	{
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("INSERT INTO processed_updates (hash) VALUES (?)");
			stmt.setString(1, new String(inReceipt.getSHA1()));
			stmt.executeUpdate();
			mDB.commit();
		}
		catch( SQLException e )
		{
			throw new IOException(e.toString());
		}
		
	}
	
	private synchronized void increment_field( long inID, String inField, long inBytes ) throws IOException 
	{
		try 
		{
			String sql = "UPDATE state SET " + inField + " = " + inField + " + ? WHERE remote_id = ?";
			PreparedStatement stmt = mDB.prepareStatement(sql);
			stmt.setLong(1, inBytes);
			stmt.setLong(2, inID);
			stmt.executeUpdate();
			stmt.close();
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
	
	
	private synchronized long retrieve_long( long inID, String inField ) throws IOException 
	{
		try
		{
			PreparedStatement stmt = mDB.prepareStatement("SELECT " + inField + " FROM state WHERE remote_id = ?");
			stmt.setLong(1, inID);
			ResultSet rs = stmt.executeQuery();
			if( rs.next() == false )
				return -1;
			return rs.getLong(1);
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
	}
	
	public synchronized static ReputationDAO get() {
		if( mInst == null )
			mInst = new ReputationDAO();
		return mInst;
	}
	
	/**
	 * @param args
	 */
	public static void main( String[] args ) throws Exception
	{
		COConfigurationManager.preInitialise();
		//AzureusCoreFactory.create().start();
		
		ReputationDAO rep = ReputationDAO.get();
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
				else if( line.toLowerCase().startsWith("topk") )
				{
					System.out.println(rep.get_topK_by_obs());
				}
				else if( line.toLowerCase().startsWith("verify") )
				{
					Object [] out = rep.get_attestation_for_verification();
				}	
				else if( line.startsWith("attest" ) )
				{
					ResultSet rs = s.executeQuery( "SELECT bytes FROM attestations WHERE attest_id = " + toks[1] );
					if( rs.next() )
						System.out.println( ByteManip.objectFromBytes(rs.getBytes(1)) );					
				}
				else if( line.startsWith("show") )
				{
					ResultSet rs = s.executeQuery("select * from " + toks[1]);
					ResultSetMetaData md = rs.getMetaData();
					Map<String, Integer> cols = new HashMap<String, Integer>();
					System.out.println("col count: " + md.getColumnCount());
					
					for( int i=1; i<=md.getColumnCount(); i++ )
					{
						System.out.print( md.getColumnLabel(i) + " " );
					}
					System.out.println("");
					
					PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
					
					while( rs.next() )
					{
						for( int i=1; i<=md.getColumnCount(); i++ )
						{
							out.printf( "%" + md.getColumnLabel(i).length() + "s ", rs.getObject(i) == null ? "null" : rs.getObject(i).toString() );
							out.flush();
						}
						out.flush();
						System.out.println("");
					}
				}
				else if( line.equals("test") )
				{
					test(rep);
					return;
				}
				else if( line.equals("q") )
					return;
				else
				{
					if( line.toLowerCase().startsWith("select") )
					{
						int count =0;
						ResultSet rs = s.executeQuery(line);
						while( rs.next() )
							count++;
						System.out.println("count: " + count);
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

	public static final void test(ReputationDAO rep) 
	{
		try 
		{
			Statement stmt = rep.mDB.createStatement();
			
			rep.create_tables();
			
			System.out.println("dropped / recreated tables");
			
			CertAndKeyGen pair = new CertAndKeyGen("RSA", "SHA1withRSA", null);
			pair.generate(LocalIdentity.KEY_SIZE_BITS);
			System.out.println("id for new pair: " + rep.get_internal_id(pair.getPublicKey()) );
			
			long id = rep.get_internal_id(pair.getPublicKey());
			rep.others_sent_due_to_my_reco(id, 102);
			rep.others_recv_due_to_my_reco(id, 56);
			
			rep.local_sent_due_to_remote_reco(id, 204);
			rep.local_recv_due_to_remote_reco(id, 402);
			
			//rep.direct_observation(id);
			rep.indirect_observation(id, 0.21);
			
			rep.sent_direct(id, 34);
			rep.received_direct(id, 43);
		}
		catch( Exception e )
		{
			System.out.println(e);
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @return object tuple {Receipt, which intermediary}
	 * @throws IOException
	 */
	public synchronized Object [] get_attestation_for_verification() throws IOException 
	{
		//System.out.println("calling get_attestation_for_verification...");
		Receipt attest = null;
		long intermediary_id = -1;
		long attest_id = -1;
		
		Statement stmt = null;
		
		boolean done = false;
		
		try {
			SQLWarning warn = mDB.getWarnings();
			while( warn != null )
			{
				logger.warning("warning: " + warn);
				warn = warn.getNextWarning();
			}
			mDB.clearWarnings();
		} catch (SQLException e1) {
			System.err.println(e1);
			e1.printStackTrace();
		}
		
		while( !done )
		{
			try
			{
				stmt = mDB.createStatement();
				/**
				 * Retrieve long-term attestations that need verifying. This won't return the same attestation until 
				 * 15 minutes have elapsed. Callers of this handle short-term retries of verification.  
				 */
				stmt.setMaxRows(1);
				ResultSet rs = stmt.executeQuery(
						"SELECT bytes, attest_id, verification_attempts, time, intermediary_id, received_due_to_reco_offset_with_int " +
						"FROM attestations " +
						"WHERE " +
						"	needs_currency_verification = 1 AND " +
						// TODO: magic constant -- 15 minutes
						"	(last_verification_attempt < '" + new Timestamp(System.currentTimeMillis() - (15 * 60 * 1000)) + "' OR last_verification_attempt IS NULL) " + 
						"ORDER BY last_verification_attempt ASC ");
				
				if( rs.next() )
				{
					logger.finer("result had next");
					
					short verification_attempts = rs.getShort(3);
					Timestamp original_receipt_time = rs.getTimestamp(4);
					// TODO: magic constant -- how many long-term verifications before failing an update
					boolean perm_failure = verification_attempts > 10 ||
											verification_attempts > 1 && original_receipt_time.before( new Date(System.currentTimeMillis() - (24*60*60*1000)) ); 
					
					attest = (Receipt)ByteManip.objectFromBytes(rs.getBytes(1));
					
					// Since we didn't serialize this...
					attest.set_received_due_to_reco_offset(rs.getInt("received_due_to_reco_offset_with_int"));
					
					attest_id = rs.getLong(2);
					intermediary_id = rs.getLong(5);
					
					/**
					 * We're about to try this, so update the last_verification_attempt time
					 * TODO: this method probably shouldn't do this...
					 */
					String sql = 	"UPDATE attestations " +
									"SET " +
									(perm_failure ? "needs_currency_verification = 0, " : "") +  
									"verification_attempts = " + (verification_attempts+1) + ", " +
									"last_verification_attempt = '" + (new Timestamp(System.currentTimeMillis())).toString() + "' " +
									"WHERE attest_id = " + attest_id;
									
					logger.finer("trying: " + sql);
									
					if( stmt.executeUpdate(sql) != 1 )
					{
						throw new IOException("failed to update last_verification_attempt in attestations");
					}
					mDB.commit();
					
					/**
					 * If this update has permanently failed, decrease our opinion of this intermediary
					 */
					if( perm_failure )
					{
						logger.fine("perm failure, multiplicative decrease");
						multiplicative_decrease_observations(intermediary_id);
						continue;
					}
					
					logger.fine("found an attestation");
					done = true; // found something to do
				}
				else
				{
					logger.finest("found no attestations");
					done = true; // nothing to do
				}
				mDB.commit();
			}
			catch( Exception e )
			{
				System.err.println(e);
				e.printStackTrace();
				throw new IOException(e.toString());
			}
			finally
			{
				try {
					stmt.close();
				} catch( Exception e ) {}
			}
		}
		
		return new Object[]{attest, new Long(intermediary_id), new Long(attest_id)};
	}

	public synchronized List<Integer> compute_offsets_from_latest_receipts( List<Receipt> receipts ) throws IOException
	{
		/**
		 * Compare our local state with the state encoded in each of these receipts and
		 * return the offsets 
		 */
		
		List<Integer> offsets = new LinkedList<Integer>();
		for( Receipt r : receipts )
		{
			PublicKey remote = r.getSigningKey();
			/**
			 * R is an intermediary that we have standing with, but we may have been using this receipt multiple times (and 
			 * we may not have received a new one). In the case that updates were applied at the intermediary (and we're out of 
			 * sync)---we need to append an update so users can compute the more recent ROI and not be surprised if later 
			 * verification with the intermediary doesn't match up
			 * 
			 * We do this only for peer_received_due_to_reco since that might be verified with intermediaries (and lying would help us) but 
			 * not with sent_due_to_reco. Although we may be out of sync (i.e. the intermediary should have a higher opinion of us), there's 
			 * no real way for that to be verified independently with recourse (unless we make the protocol much more complicated). But, the 
			 * peer can prune an intermediary with which it does not have an up to date receipt from its intermediary list, giving it 
			 * control over whether or not it wants to risk never receiving the standing for that contribution
			 */
			int offset = (int)(get_local_recv_due_to_remote_reco(get_internal_id(remote)) - r.peer_received_due_to_reco);
			offsets.add(offset);
		}

		return offsets;
	}

	/**
	 * This is pretty similar to getting the top K except we don't condition on actually having a receipt for these. 
	 * Instead, we just promote popular peers whether or not we have directly interacted with them. 
	 * 
	 * TODO: may also have to cache results here to avoid computational bottlenecks during connections. 
	 */
	public synchronized PublicKey[] get_frequently_observed() throws IOException
	{
		Statement stmt = null;
		List<PublicKey> freq = new LinkedList<PublicKey>();
		try
		{
			long start = System.currentTimeMillis();
			String sql = "SELECT remote_id, my_observations + indirect_observations AS occs FROM state ORDER BY occs DESC";
			stmt = mDB.createStatement();
			stmt.setMaxRows(2001);
			ResultSet pop = stmt.executeQuery(sql);
			logger.finer("get_frequently_observed took: " + (System.currentTimeMillis()-start) + " ms");
			while( pop.next() )
			{
				long remote_id = pop.getLong("remote_id");
				// even if we're popular, we don't advertise ourselves
				if( remote_id == 1 )
					continue;
				freq.add(get_public_key(remote_id));
			}
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				stmt.close();
			} catch( Exception e ) {}
		}

		return freq.toArray(new PublicKey[0]);
	}

	public synchronized Receipt get_latest_attestation_for_id( long inID ) 
	{
		PreparedStatement attest_lookup = null;
		Receipt r = null;
		try
		{
			attest_lookup = mDB.prepareStatement(
					"SELECT bytes FROM attestations WHERE " +
					"	encoding_for = 1 AND " +
					"	signer = ? " +
					"ORDER BY time DESC");
			attest_lookup.setMaxRows(1);
			attest_lookup.setLong(1, inID);
			
			ResultSet rs = attest_lookup.executeQuery();
			if( rs.next() )
			{
				r = (Receipt)ByteManip.objectFromBytes(rs.getBytes("bytes"));
			}
		}
		catch( Exception e )
		{
			e.printStackTrace();
			logger.warning(e.toString());
		}
		finally
		{
			try {
				if( attest_lookup != null )
					attest_lookup.close();
			} catch( Exception e ) {}
		}
		return r;
	}
	
	/**
	 * The top K set is our standing with peers that we believe OTHERS will value. 
	 * This is the set of peers that WE value (when looking at the top K sets of others)
	 * 
	 * @return
	 * @throws IOException 
	 */
	public synchronized List<PublicKey> get_desired_peers() throws IOException 
	{
		// for now, just use top 2k frequently observed
		return Arrays.asList(get_frequently_observed());
	}

	public synchronized void attestation_verification_complete( long attest_id ) throws IOException 
	{
		String sql = 	"UPDATE attestations " +
						"SET " +
						"needs_currency_verification = 0 " +  
						"WHERE attest_id = ?";

		PreparedStatement stmt = null;
		try
		{
			stmt = mDB.prepareStatement(sql);
			stmt.setLong(1, attest_id);
			int updated = stmt.executeUpdate();
			if( updated != 1 )
				throw new IOException("Attempting to update supposedly verified attestation that doesn't exist (or is already verified), id: " + attest_id);
			mDB.commit();
		}
		catch( SQLException e )
		{
			e.printStackTrace();
			throw new IOException(e.toString());
		}
		finally
		{
			try {
				if( stmt != null )
					stmt.close();
			} catch( Exception e ) {}
		}
	}
	
}
