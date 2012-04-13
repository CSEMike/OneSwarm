package edu.washington.cs.publickey.storage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.washington.cs.publickey.PublicKeyFriend;

public abstract class PersistentStorage {

	private final static boolean CACHE_ENABLED = true;
	private final static int CACHE_SIZE = 100000;
	private final static boolean CACHE_DEBUG = true;

	private final Cache cache;

	private static boolean ENABLE_LOGGING = false;

	public PersistentStorage() {
		if (CACHE_ENABLED) {
			cache = new Cache(CACHE_SIZE);
		} else {
			cache = null;
		}
	}

	public int addFriends(PublicKeyFriend user, PublicKeyFriend[] friends) throws Exception {
		int friendsAdded = addFriendsImpl(user, friends);
		if (friendsAdded > 0 && CACHE_ENABLED) {
			long time = System.currentTimeMillis();
			// expire the user from the cache
			cache.remove(new PublicKeyHashKey(user.getPublicKeySha1()));

			// expire all own keys
			PublicKeyFriend[] ownKeys = getOwnPublicKeys(user);
			cache.remove(ownKeys);

			// get all friends and expire them as well
			PublicKeyFriend[] dbFriends = getFriendPublicKeys(user);
			cache.remove(dbFriends);
			log("keeping cache up to date, overhead: " + (System.currentTimeMillis() - time) + "ms");
		}
		return friendsAdded;
	}

	private void log(String msg) {
		if (ENABLE_LOGGING) {
			System.out.println(msg);
		}
	}

	protected abstract int addFriendsImpl(PublicKeyFriend user, PublicKeyFriend[] friends) throws Exception;

	public void addPublicKey(PublicKeyFriend key) throws Exception {
		boolean dbModified = addPublicKeyImpl(key);
		if (dbModified && CACHE_ENABLED) {
			long time = System.currentTimeMillis();
			// expire the user from the cache
			cache.remove(new PublicKeyHashKey(key.getPublicKeySha1()));

			// get all friends and expire them as well
			PublicKeyFriend[] dbFriends = getFriendPublicKeys(key);
			cache.remove(dbFriends);

			log("keeping cache up to date, overhead: " + (System.currentTimeMillis() - time) + "ms");
		}
	}

	protected abstract boolean addPublicKeyImpl(PublicKeyFriend key) throws Exception;

	public PublicKeyFriend[] getFriendPublicKeys(PublicKeyFriend friend) throws Exception {
		return getFriendPublicKeysImpl(friend);
	}

	protected abstract PublicKeyFriend[] getFriendPublicKeysImpl(PublicKeyFriend friend) throws Exception;

	public List<PublicKeyFriend> getFriendsUsingPublicKey(PublicKeyFriend f) throws Exception {
		if (!CACHE_ENABLED) {
			return getFriendsUsingPublicKeyImpl(f.getPublicKeySha1());
		} else {
			List<PublicKeyFriend> cachedEntries = cache.getFriendsUsingPublicKey(f);
			if (CACHE_DEBUG) {
				List<PublicKeyFriend> dbEntries = getFriendsUsingPublicKeyImpl(f.getPublicKeySha1());
				boolean same = sameResult(dbEntries, cachedEntries);
				if (!same) {
					System.err.println("cache inconsistent, clearing");
					cache.clear();
				}
				return dbEntries;
			}
			return cachedEntries;
		}
	}

	private static boolean sameResult(List<PublicKeyFriend> dbEntries, List<PublicKeyFriend> cachedEntries) {
		if (cachedEntries.size() != dbEntries.size()) {
			System.err.println("Cache error!!!, cached=" + cachedEntries.size() + " db=" + dbEntries.size());
			return false;
		}
		HashMap<PublicKeyHashKey, PublicKeyFriend> cacheMap = new HashMap<PublicKeyHashKey, PublicKeyFriend>();
		HashMap<PublicKeyHashKey, PublicKeyFriend> dbMap = new HashMap<PublicKeyHashKey, PublicKeyFriend>();
		for (PublicKeyFriend publicKeyFriend : cachedEntries) {
			cacheMap.put(new PublicKeyHashKey(publicKeyFriend.getPublicKeySha1()), publicKeyFriend);
		}
		for (PublicKeyFriend dbFriend : dbEntries) {
			PublicKeyHashKey dbKey = new PublicKeyHashKey(dbFriend.getPublicKeySha1());
			dbMap.put(dbKey, dbFriend);

			if (!cacheMap.containsKey(dbKey)) {
				System.err.println("Cache error!!!, cache does not contain: " + dbFriend.getKeyNick());
				return false;
			}
		}
		for (PublicKeyFriend cacheFriend : cachedEntries) {
			PublicKeyHashKey cacheKey = new PublicKeyHashKey(cacheFriend.getPublicKeySha1());
			cacheMap.put(cacheKey, cacheFriend);
			if (!dbMap.containsKey(cacheKey)) {
				System.err.println("Cache error!!!, db does not contain: " + cacheFriend.getKeyNick());
				return false;
			}
		}
		return true;
	}

	protected abstract List<PublicKeyFriend> getFriendsUsingPublicKeyImpl(final byte[] publickeysha1) throws Exception;

	public PublicKeyFriend[] getOwnPublicKeys(PublicKeyFriend user) throws Exception {
		return getOwnPublicKeysImpl(user);
	}

	protected abstract PublicKeyFriend[] getOwnPublicKeysImpl(PublicKeyFriend user) throws Exception;

	public abstract void shutdown();

	public abstract void updateUserLastSeen(PublicKeyFriend user) throws Exception;

	public abstract int getDbQueueLength();

	public abstract void deleteExpiredKeys();
	
	class Cache {

		private final CacheMap cache;

		private long cacheHits;

		private long totalLookups;

		public Cache(final int maxEntries) {
			cache = new CacheMap(maxEntries);

		}

		public void remove(PublicKeyFriend[] dbFriends) {
			for (PublicKeyFriend f : dbFriends) {
				remove(new PublicKeyHashKey(f.getPublicKeySha1()));
			}
		}

		public void clear() {
			System.err.println("clearing cache");
			cache.clear();
		}

		public List<PublicKeyFriend> getFriendsUsingPublicKey(PublicKeyFriend f) throws Exception {
			totalLookups++;
			PublicKeyHashKey k = new PublicKeyHashKey(f.getPublicKeySha1());
			List<PublicKeyFriend> v = cache.get(k);
			if (v != null) {
				cacheHits++;
				System.out.println("Serving from $$, cache hit rate=" + Math.round(((100.0) * cacheHits) / totalLookups) + "%, size=" + cache.size());
				return v;
			} else {
				List<PublicKeyFriend> dbValue = getFriendsUsingPublicKeyImpl(f.getPublicKeySha1());
				cache.put(k, new PublicKeyCacheValue(dbValue));
				System.out.println("Serving from db, cache hit rate=" + Math.round(((100.0) * cacheHits) / totalLookups) + "%, size=" + cache.size());
				return dbValue;
			}
		}

		public void remove(PublicKeyHashKey k) {
			cache.remove(k);
		}

	}

	private static class CacheMap {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final Map<PublicKeyHashKey, PublicKeyCacheValue> cache = new LinkedHashMap<PublicKeyHashKey, PublicKeyCacheValue>() {
			private static final long serialVersionUID = 1L;

			protected boolean removeEldestEntry(Map.Entry<PublicKeyHashKey, PublicKeyCacheValue> eldest) {
				boolean atCapacity = size() > maxEntries;
				if (atCapacity) {
					remove(eldest.getKey());
				}
				return atCapacity;
			}
		};
		private final long maxEntries;

		public CacheMap(int maxEntries) {
			this.maxEntries = maxEntries;

		}

		public int size() {
			return cache.size();
		}

		public void clear() {
			cache.clear();
		}

		public synchronized List<PublicKeyFriend> get(PublicKeyHashKey key) {
			PublicKeyCacheValue v = cache.get(key);
			if (v != null) {
				return v.cachedValue;
			} else {
				return null;
			}

		}

		public synchronized void put(PublicKeyHashKey k, PublicKeyCacheValue v) {
			cache.put(k, v);
		}

		public synchronized PublicKeyCacheValue remove(PublicKeyHashKey key) {
			return cache.remove(key);
		}

	}

	// private static class NetIDHashKey {
	// final int hashcode;
	// final byte[] netuidsha;
	//
	// public NetIDHashKey(byte[] netuidsha) {
	// this.netuidsha = netuidsha;
	// this.hashcode = Arrays.hashCode(netuidsha);
	// }
	//
	// public boolean equals(Object o) {
	// if (o instanceof NetIDHashKey) {
	// NetIDHashKey c = (NetIDHashKey) o;
	// if (Arrays.equals(c.netuidsha, netuidsha)) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// public int hashCode() {
	// return hashcode;
	// }
	// }

	private static class PublicKeyCacheValue {
		List<PublicKeyFriend> cachedValue;
		long lastSeen;

		public PublicKeyCacheValue(List<PublicKeyFriend> v) {
			this.cachedValue = v;
			this.lastSeen = System.currentTimeMillis();
		}
	}

	private static class PublicKeyHashKey {
		final int hashcode;
		final byte[] publickey;

		public PublicKeyHashKey(byte[] publickeysha) {
			this.publickey = publickeysha;
			this.hashcode = Arrays.hashCode(publickey);
		}

		public boolean equals(Object o) {
			if (o instanceof PublicKeyHashKey) {
				PublicKeyHashKey c = (PublicKeyHashKey) o;
				if (Arrays.equals(c.publickey, publickey)) {
					return true;
				}
			}
			return false;
		}

		public int hashCode() {
			return hashcode;
		}
	}
}
