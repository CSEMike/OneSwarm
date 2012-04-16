package edu.washington.cs.publickey;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedList;
import java.util.List;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.Base64;

public class Tools {

	public final static byte[] SHA1_SEED = Base64.decode("jRofQW+cWzw0ESusBZ4eO0Zm4ZuTWt1j" + "nqpIS5UDMAt3QPluOWenQUy2RZzvJJv1" + "ab5QyEqfFrEQVRcWEDun+mb9051wtHZD" + "gt0vMbVOWLdX/Ptd7C/aZUiIcgpWqaR6" + "ruv6s6ZO7xWXHP/ZU+HbWIUXg7eFCj4B" + "+WBtbm319fcH0A/kAeUsRFBUSOh3K/Qf" + "DvQpAPVG730acxWQTKbkNg4CmPi5iVmd" + "9ow0QwPntAMw0wMocP3DBWbuqM1hrQ4d" + "wWSef52A52PV4qTQjus9vc1T7Br8fDOG" + "3NK8CwzB1bgH72tuj13Go8CCNwHksLWm" + "6wJwEPbRLt4/NUXBIYdv9CbEmBMDc601" + "i7Y/9IKhIW01fAatjZwTM5aTsQ1qj+BZ" + "S9z384SQ1d6QAQN5Gz0GxXcaQJdGN7M3" + "rTmP4SMebOwGPR99MBG8zStNzcoVC/mk" + "JTjJFXYtqNWTtWMefMZCY8vBjk43Oslt");

	public static class AcceptAllFilter implements PacketFilter {

		public boolean accept(Packet packet) {
			return true;
		}
	}

	public final static String DEFAULT_PUBLIC_KEY_SERVER = "publickey.cs.washington.edu@gmail.com";

	// sent in the client hello
	public final static String PUBLICKEY_PAYLOAD_KEY__PublicKeyFriend = "PUBLICKEY_PAYLOAD_PUBLIC_KEY";

	// sent the in server challenge
	public final static String PUBLICKEY_PAYLOAD_NOUNCE__base64_byte_array = "PUBLICKEY_PAYLOAD_NOUNCE";

	// sent in the client request
	public final static String PUBLICKEY_PAYLOAD_SIGNATURE__String_Base64 = "PUBLICKEY_PAYLOAD_SIGNATURE";
	public final static String PUBLICKEY_PAYLOAD_FRIENDS__MergeSha1_Base64 = "PUBLICKEY_PAYLOAD_FRIENDS";
	public final static String PUBLICKEY_PAYLOAD_KNOWN_KEYS__MergeSha1_Base64 = "PUBLICKEY_PAYLOAD_KNOWN_KEYS";

	// send in the server response
	public final static String PUBLICKEY_PAYLOAD_FRIENDS_KEYS__PublicKeyFriend_array = "PUBLICKEY_PAYLOAD_FRIENDS_KEYS";

	private static MessageDigest md;

	static {
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static byte[] getSha1(String networkUid) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		if (networkUid.contains("/")) {
			String newId = networkUid.split("/")[0];
			// System.out.println("converting: " + networkUid + "->" + newId);
			networkUid = newId;
		}
		return getSha1(networkUid.getBytes());
	}

	public static byte[] getSha1(byte[] bytes) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		md.update(SHA1_SEED);
		md.update(bytes, 0, bytes.length);
		byte[] sha1hash = md.digest();
		md.reset();
		return sha1hash;
	}

	public static List<byte[]> getListSha1(String mergedSha1s) {
		byte[] merged = Base64.decode(mergedSha1s);
		if (merged.length % 20 != 0) {
			throw new RuntimeException("error, only 20 byte sha1 allowed: " + merged.length + " mod 20!=0");
		}
		List<byte[]> listOfSha1s = new LinkedList<byte[]>();
		for (int i = 0; i < merged.length / 20; i++) {
			byte[] sha1 = new byte[20];
			System.arraycopy(merged, i * 20, sha1, 0, 20);
			listOfSha1s.add(sha1);
		}
		return listOfSha1s;
	}

	public static String mergeSha1sAndBase64(List<byte[]> listOfSha1s) {
		byte[] merged = new byte[listOfSha1s.size() * 20];
		for (int i = 0; i < listOfSha1s.size(); i++) {
			if (listOfSha1s.get(i).length != 20) {
				throw new RuntimeException("only 20 byte sha1 hashes is allowed");
			}
			System.arraycopy(listOfSha1s.get(i), 0, merged, i * 20, 20);
		}
		return Base64.encodeBytes(merged, Base64.DONT_BREAK_LINES);
	}

	public static PublicKey keyForEncodedBytes(byte[] inBytes) throws InvalidKeySpecException {

		X509EncodedKeySpec key_spec = new X509EncodedKeySpec(inBytes);
		KeyFactory keyFactory = null;
		try {
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException e) {
			System.err.println(e);
			e.printStackTrace();
			return null;
		}

		return keyFactory.generatePublic(key_spec);
	}

	public static boolean verifySignature(PublicKey key, byte[] nounce, byte[] signature) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1withRSA");
		sig.initVerify(key);
		sig.update(nounce);
		return sig.verify(signature);
	}
}
