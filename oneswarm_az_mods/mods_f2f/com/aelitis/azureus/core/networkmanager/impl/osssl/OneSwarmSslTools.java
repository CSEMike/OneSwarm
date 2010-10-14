package com.aelitis.azureus.core.networkmanager.impl.osssl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Cipher;

public class OneSwarmSslTools
{
	public final static int SSL_HEADER_MIN_LENGTH = 2 + 1 + 2 + 2 + 2 + 2;

	/**
	 * Convenience method to convert an int to a hex char.
	 * 
	 * @param i
	 *            the int to convert
	 * @return char the converted char
	 */
	public static char toHexChar(int i) {
		if ((0 <= i) && (i <= 9))
			return (char) ('0' + i);
		else
			return (char) ('a' + (i - 10));
	}

	/**
	 * Convenience method to convert a byte array to a hex string.
	 * 
	 * @param data
	 *            the byte[] to convert
	 * @return String the converted byte[]
	 */
	public static String bytesToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			buf.append(byteToHex(data[i]));
		}
		return (buf.toString());
	}

	public static String byteToHex(byte data) {
		StringBuffer buf = new StringBuffer();
		buf.append(Integer.toHexString(0x0100 + (data & 0x00FF)).substring(1));
		// buf.append(toHexChar((data >>> 4) & 0x0F));
		// buf.append(toHexChar(data & 0x0F));
		return buf.toString();
	}

	public static final short unsignedByteToShort(byte b) {
		short s = 0;
		s |= b & 0xFF;
		return s;
	}

	public static final int unsignedShortToInt(byte[] b) {
		int i = 0;
		i |= b[0] & 0xFF;
		i <<= 8;
		i |= b[1] & 0xFF;
		return i;
	}

	public static final long unsignedIntToLong(byte[] b) {
		long l = 0;
		l |= b[0] & 0xFF;
		l <<= 8;
		l |= b[1] & 0xFF;
		l <<= 8;
		l |= b[2] & 0xFF;
		l <<= 8;
		l |= b[3] & 0xFF;
		return l;
	}

	public static byte[] sign(PrivateKey key, byte[] data, int off, int len)
			throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchProviderException, SignatureException {
		Signature dsa = Signature.getInstance("SHA1withRSA");
		dsa.initSign(key);
		dsa.update(data, off, len);

		byte[] sign = dsa.sign();
		//		System.out.println("signature: " + Base64.encode(sign));
		return sign;
	}

	public static boolean verifySignature(byte[] data, int dataOff, int dataLen,
			PublicKey friendsPublicKey, byte[] signBytes, int signOff, int signLen)
			throws Exception {
		Signature signature = Signature.getInstance("SHA1withRSA");
		signature.initVerify(friendsPublicKey);
		signature.update(data, dataOff, dataLen);
		return signature.verify(signBytes, signOff, signLen);

	}

	public static byte[] decrypt(byte[] data, int off, int len, PrivateKey key)
			throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		return cipher.doFinal(data, off, len);
	}

	public static byte[] encrypt(byte[] data, PublicKey friendsPublicKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, friendsPublicKey);
		return cipher.doFinal(data);
	}
}
