/**
 * 
 */
package edu.washington.cs.publickey;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author isdal
 * 
 */
public class PublicKeyFriendTest {

	private final static String testkey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC" + "maE6Qdiraw+dEEGAhefobtojIa6uusurSkVa0deZ6qwR7" + "CR4Ccs9hKd1v5nr7vh9NK3g7PIsi2AjbnwvcLZqGfpRNS" + "KkHsKiMoRpBZ5X4PsXkvP1LqSfYAK7Badg7maHfGMg2nL" + "jslRg5A73tleOjvse04O/DQmSZUP6yzOBOOwIDAQAB";

	public static PublicKeyFriend getTestFriend(String pre) {
		try {

			PublicKeyFriend testFriend = new PublicKeyFriend();
			testFriend.setKeyNick("testkeynick_" + pre);
			testFriend.setSourceNetwork(FriendNetwork.XMPP_GOOGLE);
			testFriend.setSourceNetworkUid(Tools.getSha1("publickey.cs.washington.edu." + pre + "@gmail.com"));
			testFriend.setPublicKey(pre + testkey.substring(pre.length()));
			testFriend.setPublicKeySha1(Tools.getSha1(testFriend.getPublicKey()));

			return testFriend;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static PublicKeyFriend testFriend1 = getTestFriend("");

	private static String testFriendSerialized = "<?xml version='1.0' encoding='UTF-8'?> <java version='1.5.0_13' class='java.beans.XMLDecoder'> <array class='edu.washington.cs.publickey.PublicKeyFriendBean' length='1'> <void index='0'> <object class='edu.washington.cs.publickey.PublicKeyFriendBean'> <void property='keyNick'> <string>testkeynick</string> </void> <void property='publicKey'> <string>MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCmaE6Qdiraw+dEEGAhefobtojIa6uusurSkVa0deZ6qwR7CR4Ccs9hKd1v5nr7vh9NK3g7PIsi2AjbnwvcLZqGfpRNSKkHsKiMoRpBZ5X4PsXkvP1LqSfYAK7Badg7maHfGMg2nLjslRg5A73tleOjvse04O/DQmSZUP6yzOBOOwIDAQAB</string> </void> <void property='sourceNetworkUid'> <string>0mVA0O+78MBpxMqCP6ndPlDMiHU=</string> </void> </object> </void> </array> </java> ";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link edu.washington.cs.publickey.PublicKeyFriendBean#deserialize(java.lang.String)}
	 * .
	 */
	@Test
	public void testDeserialize() {
		try {
			PublicKeyFriend f = PublicKeyFriend.deserialize(testFriendSerialized)[0];
			PublicKeyFriend[] fArray = new PublicKeyFriend[] { f, f };
			if (fArray[0].equals(testFriend1) && fArray[1].equals(testFriend1)) {
				System.out.println("PublicKeyFriend.deserialize() " + "tested sucessfully (read " + fArray.length + ")");
			} else {
				fail("deserialized friend not equals testFriend");
			}
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link edu.washington.cs.publickey.PublicKeyFriendBean#serialize()} .
	 */
	@Test
	public void testSerialize() {
		try {
			String res = testFriend1.serialize();
			System.out.println(res);
			PublicKeyFriend[] f = PublicKeyFriend.deserialize(res);
			if (f[0].equals(testFriend1)) {
				System.out.println("PublicKeyFriend.serialize() " + "tested sucessfully (read " + f.length + ")");
			} else {
				fail("deserialized friend not equals testFriend");
			}

		} catch (Throwable t) {
			fail(t.getMessage());
		}
	}
}
