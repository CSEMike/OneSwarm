package edu.uw.cse.netlab.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.gudy.azureus2.core3.util.ByteFormatter;

public class BloomFilter
	implements Serializable
{
	private static final long serialVersionUID = 1L;

	// TODO: rewrite this wihtout the use of this bitset object (which forces us to retain and serialize mBitsCapacity separately!)
	//BitSet mBits = null;
	BitArray									mBits						= null;

	int											 mHashesCount		 = -1;

	transient MessageDigest	 mDigest					= null;

	transient ByteBuffer			buff						 = null;

	int											 mBitsCapacity;

	int											 mInToStore			 = -1;	// this gives the fractional amount intermediaries should attribute

	byte[][]									salts						= null;

	private final static int	SALT_LENGTH			= 20;

	public boolean equals(BloomFilter rhs) {
		return mBits.equals(mBits) && mHashesCount == rhs.mHashesCount
				&& mBitsCapacity == rhs.mBitsCapacity && mInToStore == rhs.mInToStore;
	}

	@Override
    public String toString() {
		String out = "[BloomFilter: store: " + mInToStore + " hashes: "
				+ mHashesCount + " bits: " + mBits.length() + "] ";
		if (mBits.length() < 100) {
			for (int i = 0; i < mBits.length(); i++)
				out += mBits.get(i) == true ? "1" : "0";
		}
		return out;
	}

	public int getStoredCount() {
		return mInToStore;
	}

	public BloomFilter(int inNumBits, int inMaxToStore)
			throws NoSuchAlgorithmException {
		mBitsCapacity = inNumBits;
		mBits = new BitArray(inNumBits);
		mHashesCount = BloomFilter.computeHashes(inNumBits, inMaxToStore);
		salts = new byte[mHashesCount][0];

		/*
		 * use random salts to avoid any bloomfilter filling attacks
		 */
		for (int i = 0; i < mHashesCount; i++) {
			byte[] salt = new byte[SALT_LENGTH];
			rand.nextBytes(salt);
			salts[i] = salt;
		}
		mInToStore = inMaxToStore;
		buff = ByteBuffer.allocate(4);
		mDigest = MessageDigest.getInstance("MD5");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		try {
			mDigest = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			System.err.println("** Couldn't get MD5 hasher!");
		}
		buff = ByteBuffer.allocate(4);

		in.defaultReadObject();
	}

	private static int computeHashes(int inNumBits, int inMaxToStore) {
		return (int) Math.ceil((Math.log(2) * ((double) inNumBits / (double) inMaxToStore)));
	}

	//	 Returns a bitset containing the values in bytes.
	// The byte-ordering of bytes must be big-endian which means the most significant bit is in element 0.
	public static BitSet fromByteArray(byte[] bytes) {
		BitSet bits = new BitSet();
		for (int i = 0; i < bytes.length * 8; i++) {
			if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
				bits.set(i);
			}
		}
		return bits;
	}

	private int[] getBits(byte[] inBytes) {
		int[] bits = new int[mHashesCount];
		for (int funcItr = 0; funcItr < mHashesCount; funcItr++) {
			try {
				// create many bloom filter hashes through consistent salting
				mDigest.update(salts[funcItr]);

				byte[] hash = mDigest.digest(inBytes);
				buff.position(0);
				buff.put(hash, 0, 4);
				buff.position(0);
                bits[funcItr] = Math.abs(buff.getInt() % mBitsCapacity);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("error hashing cert into bloom filter: " + e);
				return null;
			}
		}
		return bits;
	}

	public void insert(byte[] inBytes) {
		int[] bits = getBits(inBytes);
		boolean alreadyThere = true;

		for (int i : bits) {
			if (mBits.get(i) == false) {
				alreadyThere = false;
			}
			mBits.set(i);
		}

		if (!alreadyThere) {
			objectsStored++;
		}

	}

	private int objectsStored = 0;

	public int getUniqueObjectsStored() {
		return objectsStored;
	}

	public boolean test(byte[] inBytes) {
		int[] bits = getBits(inBytes);

		//		StringBuilder sb = new StringBuilder();
		//		for( int i : bits )
		//			sb.append(i + " ");
		//		
		//		System.out.println("testing: " + ByteFormatter.encodeString(inBytes) + " gives: " + sb.toString());

		for (int i : bits) {
			if (mBits.get(i) == false)
				return false;
		}
		return true;
	}

	public void clear() {
		mBits.clear();
	}

	static Random rand = new Random();

	public static byte[] random_bytes(int inSize) {
		byte[] b = new byte[inSize];
		rand.nextBytes(b);
		return b;
	}

	public double getPredictedFalsePositiveRate() {
		return getPredictedFalsePositiveRate(mBitsCapacity,
				getUniqueObjectsStored());
	}

	public static double getPredictedFalsePositiveRate(int size, int to_store) {
		return Math.pow(0.6185, (double) size / (double) to_store);
	}

	public static final void main(String[] args) throws Exception {
		int size = 512 * 1024, to_store = 20000;
		BloomFilter bf = new BloomFilter(size, to_store);
		Set<String> set = new HashSet<String>();
		System.out.println("mbits size (ints)=" + bf.mBits.back.length);
		byte[] to_test = null;

		long start = System.currentTimeMillis();
		for (int i = 0; i < to_store; i++) {
			byte[] b = random_bytes(8);
			set.add(new String(b));
			bf.insert(b);
			if (!bf.test(b)) {
				System.err.println("inserted but test failed!");
			}

			if (to_test == null)
				to_test = b;
		}
		System.out.println("inserting took: "
				+ (System.currentTimeMillis() - start) + " ms");

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				"/tmp/foo"));
		oos.writeObject(bf);
		oos.close();

		int fps = 0, to_check = 100000;
		for (int i = 0; i < to_check; i++) {
			byte[] b = null;
			do {
				b = random_bytes(8);
			} while (set.contains(new String(b)) == true);
			if (bf.test(b) == true)
				fps++;
		}
		System.out.println("false positives: " + fps + " of " + to_check + " / "
				+ ((double) fps / (double) to_check) * 100.0 + "% / Predicted: "
				+ (getPredictedFalsePositiveRate(size, to_store) * 100.0)
				+ "% / based on current state="
				+ (bf.getPredictedFalsePositiveRate() * 100) + "%");

		// serialization
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		oos = new ObjectOutputStream(baos);
		oos.writeObject(bf);
		oos.close();
		BloomFilter two = (BloomFilter) (new ObjectInputStream(
				new ByteArrayInputStream(baos.toByteArray()))).readObject();
		System.out.println(two);
		two.test(to_test);
	}
}
