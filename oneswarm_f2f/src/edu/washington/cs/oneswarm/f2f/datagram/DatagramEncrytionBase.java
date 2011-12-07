package edu.washington.cs.oneswarm.f2f.datagram;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public abstract class DatagramEncrytionBase {
	public final static String ENCR_STD = "AES";
	public final static String ENCR_ALGO = "AES/CTR/PKCS5Padding";
	public final static String HMAC_ALGO = "HmacSHA1";

	public final static int HMAC_KEY_LENGTH = 20;
	public final static int AES_KEY_LENGTH = 128;

	protected Mac mac;
	protected Key macKey;

	protected IvParameterSpec ivSpec;
	protected Key key;
	protected Cipher cipher;

	protected static SecretKey createKeyForAES(int bitLength,
			SecureRandom random) throws NoSuchAlgorithmException,
			NoSuchProviderException {
		KeyGenerator generator = KeyGenerator.getInstance(ENCR_STD);
		generator.init(AES_KEY_LENGTH, random);
		return generator.generateKey();
	}

	protected static IvParameterSpec createCtrIvForAES(long sequenceNumber,
			SecureRandom random) {
		byte[] ivBytes = new byte[16];
		random.nextBytes(ivBytes);
		return setSequenceNumber(sequenceNumber, ivBytes);
	}

	protected static IvParameterSpec setSequenceNumber(long sequenceNumber,
			byte[] ivBytes) {
		ByteBuffer buf = ByteBuffer.wrap(ivBytes);
		buf.order(ByteOrder.BIG_ENDIAN); // Sun implements counter as
											// big-endian
		buf.position(8); // Counter is the last 8 bytes
		buf.putLong(sequenceNumber);
		return new IvParameterSpec(ivBytes);
	}

	protected static long getSequenceNumber(IvParameterSpec ivSpec) {
		ByteBuffer buf = ByteBuffer.wrap(ivSpec.getIV());
		buf.order(ByteOrder.BIG_ENDIAN); // Sun implements counter as
											// big-endian
		buf.position(8); // Counter is the last 8 bytes
		return buf.getLong();
	}

}