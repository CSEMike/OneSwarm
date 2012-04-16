package edu.washington.cs.publickey.storage.sql;

import static org.junit.Assert.fail;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.junit.Test;

import edu.washington.cs.publickey.PublicKeyFriend;

public abstract class PersistentStorageSQLBigFatTest extends PersistentStorageSQLTest {

	private static HashMap<PublicKeyFriend, HashSet<PublicKeyFriend>> friends = new HashMap<PublicKeyFriend, HashSet<PublicKeyFriend>>();
	int NUM_READ_THREADS = 10;
	private final Semaphore readSemaphore = new Semaphore(NUM_READ_THREADS);
	private final Semaphore writeSemaphore = new Semaphore(1);

	public PersistentStorageSQLBigFatTest() {
		super();
	}

	public abstract void setUp() throws Exception;

	@Test
	public void testBigFatTablePerformance() {
		try {
			clearTables();

			final Random r = new Random(12345678);
			System.out.println("creating friend relationships");
			for (int i = 0; i < testFriendsBig.length; i++) {
				int numFriends = r.nextInt(200) + 1;
				long addStart = System.currentTimeMillis();
				PublicKeyFriend u = testFriendsBig[i];
				for (int j = 0; j < numFriends; j++) {
					PublicKeyFriend f = testFriendsBig[r.nextInt(testFriendsBig.length)];
					boolean symmetric = r.nextFloat() < 0.7;
					addFriend(u, f, symmetric);
				}
				long checkStart = System.currentTimeMillis();
				checkgetFriendsPublicKeysQuery(u);
				long addElapsed = checkStart - addStart;
				System.out.println("adding user " + i + " numFriends=" + numFriends + " addTime=" + addElapsed + " (" + addElapsed / numFriends + ")" + " checkTime=" + (System.currentTimeMillis() - checkStart) + "");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testConcurrentClientPerformance() throws SQLException {
		clearTables();
		Thread writerThread = new Thread(new Runnable() {
			public void run() {
				try {
					final Random r = new Random(12345678);
					System.out.println("creating friend relationships");
					for (int i = 0; i < testFriendsBig.length; i++) {
						int numFriends = r.nextInt(200) + 1;
						long addStart = System.currentTimeMillis();
						PublicKeyFriend u = testFriendsBig[i];
						for (int j = 0; j < numFriends; j++) {
							PublicKeyFriend f = testFriendsBig[r.nextInt(testFriendsBig.length)];
							boolean symmetric = r.nextFloat() < 0.7;
							symmetric = true;
							addFriend(u, f, symmetric);
						}
						long checkStart = System.currentTimeMillis();
						long addElapsed = checkStart - addStart;
						System.out.println("adding user " + i + " numFriends=" + numFriends + " addTime=" + addElapsed + " (" + addElapsed / numFriends + ")" + " checkTime=" + (System.currentTimeMillis() - checkStart) + "");
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		writerThread.setName("Writer thread");
		writerThread.start();
		final Random r = new Random(543212);
		Thread readerThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);
						checkgetFriendsPublicKeysQuery(testFriendsBig[r.nextInt(testFriendsBig.length)]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		readerThread.setName("reader");
		readerThread.start();
	}

	private synchronized void addFriend(PublicKeyFriend u1, PublicKeyFriend u2, boolean symmetric) throws Exception {
		addFriend(u1, u2);
		if (symmetric) {
			addFriend(u2, u1);
		}
	}

	private synchronized void addFriend(PublicKeyFriend u1, PublicKeyFriend u2) throws Exception {
		// add the actual friends in the db and in the local state
		p.addPublicKey(u1);
		p.addFriends(u1, new PublicKeyFriend[] { u2 });

		HashSet<PublicKeyFriend> u1FriendSet = friends.get(u1);
		if (u1FriendSet == null) {
			u1FriendSet = new HashSet<PublicKeyFriend>();
			friends.put(u1, u1FriendSet);
		}
		u1FriendSet.add(u2);
	}

	private boolean areSymFriends(PublicKeyFriend u1, PublicKeyFriend u2) {
		if (areFriends(u1, u2) && areFriends(u2, u1)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean areFriends(PublicKeyFriend u1, PublicKeyFriend u2) {
		HashSet<PublicKeyFriend> u1Friends = friends.get(u1);
		if (u1Friends != null) {
			return u1Friends.contains(u2);
		}
		return false;
	}

	private synchronized HashSet<PublicKeyFriend> getSymmetricFriends(PublicKeyFriend u) {
		HashSet<PublicKeyFriend> sym = new HashSet<PublicKeyFriend>();

		HashSet<PublicKeyFriend> friendMap = friends.get(u);
		if (friendMap != null) {
			for (PublicKeyFriend f : friendMap) {
				if (areSymFriends(u, f)) {
					sym.add(f);
				}
			}
		}
		return sym;

	}

	private synchronized void checkgetFriendsPublicKeysQuery(PublicKeyFriend u) throws Exception {
		// PublicKeyFriend[] dbFriends = p.getFriendPublicKeys(u);
		List<PublicKeyFriend> dbFriends = p.getFriendsUsingPublicKey(u);
		HashSet<PublicKeyFriend> friendMap = getSymmetricFriends(u);

		// step 1, check that all friends returned are in our map as well
		for (PublicKeyFriend dbFriend : dbFriends) {
			if (!friendMap.contains(dbFriend)) {
				fail("friends in db but not in map");
			}
		}

		// step 2, check the opposite
		for (PublicKeyFriend mapFriend : friendMap) {
			if (!contains(dbFriends, mapFriend)) {
				fail("friends in map but not in db");
			}
		}
	}

	@SuppressWarnings("unused")
	private static boolean contains(PublicKeyFriend[] friends, PublicKeyFriend u) {
		for (PublicKeyFriend f : friends) {
			if (f.equals(u)) {
				return true;
			}
		}

		return false;
	}

	private static boolean contains(List<PublicKeyFriend> friends, PublicKeyFriend u) {
		for (PublicKeyFriend f : friends) {
			if (f.equals(u)) {
				return true;
			}
		}

		return false;
	}

	protected void deleteFileOrDir(File file) {

		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				deleteFileOrDir(f);
			}
			System.out.println("del_dir: " + file.getAbsolutePath());
			file.delete();
		} else {
			System.out.println("del_file: " + file.getAbsolutePath());
			file.delete();
		}
	}

}