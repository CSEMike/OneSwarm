package edu.washington.cs.publickey.storage.sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.storage.PersistentStorage;

public abstract class PersistentStorageSQL extends PersistentStorage {
	private UserIdCache userIdCache = new UserIdCache(100000);
	private static final boolean ENABLE_LOGGING = false;

	private final boolean TRACE_QUERY_LATENCY = false;
	public String framework = "embedded";
	protected Connection conn;
	protected QueryManager queryManager;
	private HashMap<String, Long> startTime = new HashMap<String, Long>();
	private final PriorityBlockingQueue<Runnable> dbManagerQueue = new PriorityBlockingQueue<Runnable>();

	protected void startTrace(String function) {
		if (TRACE_QUERY_LATENCY) {
			// System.out.println("entered: " + function);
			this.startTime.put(function, System.currentTimeMillis());
		}
	}

	protected void endTrace(String function) {
		if (TRACE_QUERY_LATENCY) {
			Long time = startTime.get(function);
			System.out.println("(" + (System.currentTimeMillis() - time) + "ms) completed: " + function);
		}
	}

	private final ExecutorService dbManager = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, dbManagerQueue) {
		public <T> Future<T> submit(Callable<T> task) {
			if (task == null)
				throw new NullPointerException();
			FutureTask<T> ftask = new FutureAndComparable<T>(task);
			execute(ftask);
			return ftask;
		}
	};

	public PersistentStorageSQL() {
		super();
	}

	public synchronized void shutdown() {
		startTrace("shutdown");
		dbManager.shutdownNow();
		try {
			if (conn != null && !conn.isClosed()) {
				/*
				 * We end the transaction and the connection.
				 */
				conn.commit();
				conn.close();
				System.out.println("Committed transaction and closed connection");
			}
		} catch (SQLException e) {
			System.out.println("got exception when closing conn:" + e.getMessage());
		}

		/*
		 * In embedded mode, an application should shut down Derby. If the
		 * application fails to shut down Derby explicitly, the Derby does not
		 * perform a checkpoint when the JVM shuts down, which means that the
		 * next connection will be slower. Explicitly shutting down Derby with
		 * the URL is preferred. This style of shutdown will always throw an
		 * "exception".
		 */
		boolean gotSQLExc = false;

		if (framework.equals("embedded")) {
			try {
				DriverManager.getConnection("jdbc:derby:;shutdown=true");
			} catch (SQLException se) {
				gotSQLExc = true;
			}

			if (!gotSQLExc) {
				System.out.println("Database did not shut down normally");
			} else {
				System.out.println("Database shut down normally");
			}
		}
		endTrace("shutdown");
	}

	protected void verifyDbDir(File dataBaseDir) throws Exception, IOException {
		if (dataBaseDir.exists()) {
			if (!dataBaseDir.isDirectory()) {
				throw new Exception("specified db path is not a directory: '" + dataBaseDir.getCanonicalPath() + "'");
			}
		} else {
			System.out.println("creating dir: " + dataBaseDir.getCanonicalPath());
			dataBaseDir.mkdirs();
			if (!dataBaseDir.isDirectory()) {
				throw new Exception("unable to create db directory: '" + dataBaseDir.getCanonicalPath() + "'");
			}
		}
	}

	protected boolean addPublicKeyImpl(final PublicKeyFriend me) throws Exception {

		Future<Boolean> f = dbManager.submit(new DatabaseJob<Boolean>(DatabaseJob.PRIO_INTERACTIVE) {
			public Boolean call() throws Exception {
				startTrace("addPublicKey");
				boolean commitNeeded = false;
				// get our user id (or create one if not exists)
				Long userId = getUserIdAddIfNotExists(me);

				String keyNick = queryManager.keyExists(userId, me.getPublicKeySha1());
				log("existing keynick=" + keyNick);
				log("sent keynick=" + me.getKeyNick());
				if (keyNick == null) {
					commitNeeded = true;
					queryManager.insertOwnKey(userId, me.getKeyNick(), me.getPublicKey(), me.getPublicKeySha1());
				} else if (!keyNick.equals(me.getKeyNick())) {
					commitNeeded = true;
					queryManager.updateOwnKey(userId, me.getKeyNick(), me.getPublicKeySha1());
				} else {
					// log("key already exists");
				}

				if (commitNeeded) {
					// log("committing changes");
					conn.commit();
				}
				endTrace("addPublicKey");
				return commitNeeded;
			}
		});

		return f.get();
	}

	protected PublicKeyFriend[] getFriendPublicKeysImpl(final PublicKeyFriend friend) throws Exception {

		Future<PublicKeyFriend[]> f = dbManager.submit(new DatabaseJob<PublicKeyFriend[]>(DatabaseJob.PRIO_INTERACTIVE) {
			public PublicKeyFriend[] call() throws Exception {
				startTrace("getFriendsPublicKeys");
				long user_id = getUserIdAddIfNotExists(friend);
				List<PublicKeyFriend> mutualFriendsPublicKeys = queryManager.getMutualFriendsPublicKeys(user_id);

				endTrace("getFriendsPublicKeys");
				return mutualFriendsPublicKeys.toArray(new PublicKeyFriend[mutualFriendsPublicKeys.size()]);
			}
		});

		return f.get();
	}

	protected PublicKeyFriend[] getOwnPublicKeysImpl(final PublicKeyFriend me) throws Exception {
		startTrace("getOwnPublicKeys");
		Future<PublicKeyFriend[]> f = dbManager.submit(new DatabaseJob<PublicKeyFriend[]>(DatabaseJob.PRIO_INTERACTIVE) {
			public PublicKeyFriend[] call() throws Exception {
				Long userId = userIdCache.getUserIdAddIfNotExists(me);
				log("got userid=" + userId);
				if (userId != null) {
					PublicKeyFriend[] ownKeys = queryManager.ownKeys(me, userId);
					endTrace("getOwnPublicKeys");
					return ownKeys;
				}
				endTrace("getOwnPublicKeys");
				return new PublicKeyFriend[0];
			}
		});
		return f.get();
	}

	protected int addFriendsImpl(final PublicKeyFriend me, final PublicKeyFriend[] friends) throws Exception {

		Future<Integer> f = dbManager.submit(new DatabaseJob<Integer>(DatabaseJob.PRIO_INTERACTIVE) {
			public Integer call() throws Exception {
				startTrace("addFriends");
				boolean commitNeeded = false;
				long user_id = getUserIdAddIfNotExists(me);

				LinkedList<Long> allFriendIds = new LinkedList<Long>();

				// ok, get the users that are in the db already
				for (PublicKeyFriend friend : friends) {
					// first, check if in db
					allFriendIds.add(userIdCache.getUserIdAddIfNotExists(friend));
				}

				// get the users that we already have as friends
				List<Long> existingFriendsList = queryManager.getFriendsOf(user_id);
				HashSet<Long> existingFriends = new HashSet<Long>();
				for (Long f : existingFriendsList) {
					existingFriends.add(f);
				}
				log("existing friends: " + existingFriends.size());
				// get the friends we need to add
				List<Long> friendsToAdd = new ArrayList<Long>();
				for (Long f_id : allFriendIds) {
					if (!existingFriends.contains(f_id)) {
						friendsToAdd.add(f_id);
					}
				}

				log("adding friends: " + friendsToAdd.size());
				// add them all to the friends table
				if (friendsToAdd.size() > 0) {
					commitNeeded = true;
					queryManager.addFriends(user_id, friendsToAdd);
				}

				// and then, if we need to commit we should do that...
				if (commitNeeded) {
					conn.commit();
				}
				endTrace("addFriends");
				return friendsToAdd.size();
			}
		});

		return f.get();
	}

	private Long getUserIdAddIfNotExists(PublicKeyFriend publicKeyFriend) throws SQLException {
		return userIdCache.getUserIdAddIfNotExists(publicKeyFriend);
	}

	private class UserIdCache {
		private final int maxEntries;

		public UserIdCache(int maxEntries) {
			this.maxEntries = maxEntries;
		}

		long hits = 0;
		long total = 0;
		long added = 0;

		private final Map<NetAndUIDWrapper, Long> cache = new LinkedHashMap<NetAndUIDWrapper, Long>() {
			private static final long serialVersionUID = 1L;

			protected boolean removeEldestEntry(Map.Entry<NetAndUIDWrapper, Long> eldest) {
				boolean atCapacity = size() > maxEntries;
				if (atCapacity) {
					remove(eldest.getKey());
				}
				return atCapacity;
			}
		};

		public Long getUserIdAddIfNotExists(PublicKeyFriend user) throws SQLException {
			total++;
			NetAndUIDWrapper wrapper = new NetAndUIDWrapper(user.getSourceNetworkUid(), user.getSourceNetwork().getNetworkId());
			Long userId = cache.get(wrapper);
			if (userId == null) {
				userId = queryManager.getUserId(user.getSourceNetwork(), user.getSourceNetworkUid());
				if (userId == null) {
					added++;
					ArrayList<PublicKeyFriend> l = new ArrayList<PublicKeyFriend>();
					l.add(user);
					ArrayList<Long> res = queryManager.addUsers(l);
					if (res.size() != 1) {
						throw new SQLException("strange, tried to add 1 user but added " + res.size());
					}
					userId = res.get(0);
					// log("added new user, got userid=" + userId);
				}
				cache.put(wrapper, userId);
			} else {
				hits++;
			}
			if (total % 10000 == 0) {
				System.out.println("total: " + total + " hitrate: " + ((100 * hits) / total) + "% added: " + added);
			}
			return userId;
		}

		private class NetAndUIDWrapper {
			private final byte[] netUID;
			private final int net;
			private final int hashCode;

			public NetAndUIDWrapper(byte[] netUID, int net) {
				super();
				this.netUID = netUID;
				this.net = net;
				this.hashCode = Arrays.hashCode(netUID);
			}

			public boolean equals(Object o) {
				if (o instanceof NetAndUIDWrapper) {
					NetAndUIDWrapper n = (NetAndUIDWrapper) o;
					if (n.hashCode == hashCode) {
						if (n.net == net) {
							if (Arrays.equals(n.netUID, netUID)) {
								return true;
							}
						}
					}
				}
				return false;
			}

			public int hashCode() {
				return hashCode;
			}

		}
	}

	private void log(String msg) {
		if (ENABLE_LOGGING) {
			System.out.println(msg);
		}
	}

	/**
	 * For unit testing, do not use
	 * 
	 * @return The Querymanager in use
	 */
	public QueryManager getQueryManager() {
		return queryManager;
	}

	/**
	 * For unit testing, do not use
	 * 
	 * @return the connection in use
	 */
	public Connection getConnection() {
		return conn;
	}

	protected List<PublicKeyFriend> getFriendsUsingPublicKeyImpl(final byte[] publickeysha1) throws Exception {
		Future<List<PublicKeyFriend>> f = dbManager.submit(new DatabaseJob<List<PublicKeyFriend>>(DatabaseJob.PRIO_LOW) {
			public List<PublicKeyFriend> call() throws Exception {
				// step 1, get all user_id's associated with the public key
				List<Long> userIds = queryManager.getUserIdsGivenPublicKey(publickeysha1);
				// log("userIds found:" + userIds.size());

				List<PublicKeyFriend> allFriends = new LinkedList<PublicKeyFriend>();
				// step 2, get all public keys associated with the user_id's
				// for (Long userId : userIds.keySet()) {
				// PublicKeyFriend u = userIds.get(userId);
				// PublicKeyFriend[] ownKeys = queryManager.ownKeys(u, userId);
				// allFriends.addAll(Arrays.asList(ownKeys));
				// }

				// step 3, get all the friends
				for (Long userId : userIds) {
					List<PublicKeyFriend> mutualFriendsPublicKeys = queryManager.getMutualFriendsPublicKeys(userId);
					allFriends.addAll(mutualFriendsPublicKeys);
				}

				return allFriends;
			}
		});

		return f.get();
	}

	@Override
	public void updateUserLastSeen(final PublicKeyFriend user) throws Exception {
		Future<Void> f = dbManager.submit(new DatabaseJob<Void>(DatabaseJob.PRIO_INTERACTIVE) {
			public Void call() throws Exception {
				if (user.getSourceNetwork() != null && user.getSourceNetworkUid() != null && user.getPublicKeySha1() != null) {
					queryManager.updateUserLastSeen(user.getSourceNetwork(), user.getSourceNetworkUid(), user.getPublicKeySha1());
				} else if (user.getPublicKeySha1() != null) {
					queryManager.updateUserLastSeen(user.getPublicKeySha1());
				} else {
					throw new Exception("Unable to update user, not enough info in publickeyfriend object");
				}
				return null;
			}
		});

		f.get();
	}

	@Override
	public int getDbQueueLength() {
		return dbManagerQueue.size();
	}

	@Override
	public void deleteExpiredKeys() {
		try {
			queryManager.deleteExpiredKeys();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class FutureAndComparable<V> extends FutureTask<V> implements Comparable<FutureTask<V>> {

		public FutureAndComparable(Callable<V> callable) {
			super(callable);
			if (callable instanceof DatabaseJob) {
				this.job = (DatabaseJob<V>) callable;
			} else {
				job = null;
			}
		}

		private final DatabaseJob<V> job;

		public DatabaseJob<V> getJob() {
			return job;
		}

		@SuppressWarnings("unchecked")
		public int compareTo(FutureTask o) {
			if (job == null) {
				return 0;
			}
			if (o instanceof FutureAndComparable) {
				FutureAndComparable j = (FutureAndComparable) o;
				return job.compareTo(j.getJob());
			}

			return 0;

		}
	}

}