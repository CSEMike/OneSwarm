package edu.washington.cs.publickey.storage.sql;

import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import org.jivesoftware.smack.util.Base64;
import org.junit.After;
import org.junit.Test;

import edu.washington.cs.publickey.PublicKeyFriend;
import edu.washington.cs.publickey.PublicKeyFriendTest;

public abstract class PersistentStorageSQLTest {

	public static String letters = "abcdefghijklmnopABCDEFGHIJKLMNOP";
	public static PublicKeyFriend[] testFriends = new PublicKeyFriend[letters.length()];

	public static PublicKeyFriend[] testFriendsBig = new PublicKeyFriend[letters.length() * letters.length() * letters.length()];
	static {
		byte[] b = new byte[512];
		new Random().nextBytes(b);
		String s1 = Base64.encodeBytes(b, Base64.DONT_BREAK_LINES);
		for (int i = 0; i < b.length - 32; i += 32) {
			System.out.println("\"" + s1.substring(i, i + 32) + "\" +");
		}

		for (int i = 0; i < letters.length(); i++) {
			String s = Character.toString((letters.charAt(i)));
			testFriends[i] = PublicKeyFriendTest.getTestFriend(s);
		}

		System.out.println("small test set: " + testFriends.length + " users");

		for (int i = 0; i < letters.length(); i++) {
			for (int j = 0; j < letters.length(); j++) {
				for (int k = 0; k < letters.length(); k++) {
					String s = Character.toString((letters.charAt(i)));
					String t = Character.toString((letters.charAt(j)));
					String u = Character.toString((letters.charAt(k)));

					testFriendsBig[i * letters.length() * letters.length() + j * letters.length() + k] = PublicKeyFriendTest.getTestFriend(s + t + u);
				}
			}
			System.out.println("creating test user: " + i * letters.length() * letters.length());
		}
		System.out.println("big test set " + testFriendsBig.length + " users");

	}

	protected PersistentStorageSQL p;

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		p.shutdown();
	}

	public PersistentStorageSQLTest() {
		super();
	}

	protected abstract void clearTables() throws SQLException;

	public void profileAddFriends() {
		Random r = new Random();
		int NUM_FRIENDS = 100;
		try {
			clearTables();
			for (PublicKeyFriend f : testFriendsBig) {
				p.addPublicKey(f);
				int startNum = r.nextInt(testFriendsBig.length - NUM_FRIENDS);
				PublicKeyFriend[] friends = new PublicKeyFriend[NUM_FRIENDS];
				System.arraycopy(testFriendsBig, startNum, friends, 0, NUM_FRIENDS);
				p.addFriends(f, friends);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void profileAddUser() {
		try {
			clearTables();
			for (PublicKeyFriend f : testFriendsBig) {
				p.addPublicKey(f);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddFriends() {
		try {
			System.out.println("\n*******************testAddFriends");
			clearTables();
			int added = p.addFriends(PublicKeyFriendTest.testFriend1, testFriends);
			System.out.println("added " + added + " friends");
			if (added != testFriends.length) {
				fail("tried to add " + testFriends.length + " friends, but only got " + added);
			}
			long userId = p.getQueryManager().getUserId(PublicKeyFriendTest.testFriend1.getSourceNetwork(), PublicKeyFriendTest.testFriend1.getSourceNetworkUid());
			List<Long> friends = p.getQueryManager().getFriendsOf(userId);

			System.out.println("got " + friends.size() + " friends");
			if (friends.size() != testFriends.length) {
				fail("wrong number of friends: " + friends.size() + "!=" + testFriends.length);
			}

			added = p.addFriends(PublicKeyFriendTest.testFriend1, testFriends);
			if (added != 0) {
				fail("we already added the friends");
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("got exception:" + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link edu.washington.cs.publickey.storage.sql.derby.PersistentStorageDerby#addPublicKey(edu.washington.cs.publickey.FriendNetwork, java.lang.String, edu.washington.cs.publickey.FriendKeyBean)}
	 * .
	 */
	@Test
	public void testAddPublicKey() {
		System.out.println("\n*******************testAddPublicKey");
		try {
			clearTables();
			// p = new PersistentStorageDerby(DATA_BASE_DIR);
			p.addPublicKey(PublicKeyFriendTest.testFriend1);
			p.addPublicKey(PublicKeyFriendTest.testFriend1);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link edu.washington.cs.publickey.storage.sql.derby.PersistentStorageDerby#getFriendsPublicKeys(edu.washington.cs.publickey.FriendNetwork, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetFriendsPublicKeys() {
		System.out.println("\n*******************testGetFriendsPublicKeys");
		try {
			clearTables();
			for (int i = 0; i < testFriends.length; i++) {
				// make them all friends with each other
				p.addPublicKey(testFriends[i]);
				p.addFriends(testFriends[i], testFriends);
				long user_id = p.getQueryManager().getUserId(testFriends[i].getSourceNetwork(), testFriends[i].getSourceNetworkUid());
				List<PublicKeyFriend> friendKeys = p.getQueryManager().getMutualFriendsPublicKeys(user_id);
				if (i + 1 != friendKeys.size()) {
					fail("wrong count of friend public keys " + friendKeys.size() + " is not " + (i + 1));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 */
	@Test
	public void testGetMutualFriends() {
		System.out.println("\n*******************testGetMutualFriends");
		try {
			clearTables();
			for (int i = 0; i < testFriends.length; i++) {
				// make them all friends with each other
				p.addFriends(testFriends[i], testFriends);
				long user_id = p.getQueryManager().getUserId(testFriends[i].getSourceNetwork(), testFriends[i].getSourceNetworkUid());
				List<Long> mutualFriends = p.getQueryManager().getMutualFriendsOf(user_id);
				if (i + 1 != mutualFriends.size()) {
					fail("mutual friends returned " + mutualFriends.size() + " exptected " + (i + 1));
				}
				// System.out.println("mutual friends returned "
				// + mutualFriends.size() + " exptected " + (i + 1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Test method for
	 * {@link edu.washington.cs.publickey.storage.sql.derby.PersistentStorageDerby#getOwnPublicKeys(edu.washington.cs.publickey.FriendNetwork, java.lang.String)}
	 * .
	 */
	@Test
	public void testGetOwnPublicKeys() {
		try {
			System.out.println("\n*******************testGetOwnPublicKeys");
			// clear the tables
			clearTables();
			// first, add it
			for (int i = 0; i < 2; i++) {
				p.addPublicKey(PublicKeyFriendTest.testFriend1);

				PublicKeyFriend[] keys = p.getOwnPublicKeys(PublicKeyFriendTest.testFriend1);
				System.out.println("got: " + keys.length + " keys");
				if (keys.length == 1) {
					if (!keys[0].equals(PublicKeyFriendTest.testFriend1)) {
						fail("got strange keys");
					}

				} else {
					fail("got wrong number of keys");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}