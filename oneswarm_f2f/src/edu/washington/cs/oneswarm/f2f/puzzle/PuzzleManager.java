package edu.washington.cs.oneswarm.f2f.puzzle;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/** A singleton that maintains a thread pool for performance search puzzle generation. */
public class PuzzleManager {

	/** Logger. */
	private static Logger logger = Logger.getLogger(PuzzleManager.class.getName());

	/** Singleton instance. */
	private static PuzzleManager instance = null;

	/**
	 * Powers of two constants used during bit matching in {@code computeMatchingBitsLeftToRight}.
	 */
	final static int[] POWERS_OF_2 = new int[] { 1, 2, 4, 8, 16, 32, 64, 128 };

	/** Thread pool for generator/verification tasks. */
	ExecutorService pool = null;

	/** Source of randomness used when generating candidate solutions. */
	Random random = new Random();

	public static PuzzleManager get() {
		if (instance == null) {
			instance = new PuzzleManager();
		}
		return instance;
	}

	private PuzzleManager() {
		pool = Executors.newFixedThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				t.setName("PuzzleManager worker thread.");
				return t;
			}
		});
	}

	/**
	 * Searches for a 20 byte entry whose SHA-1 hash matches {@code rawBytesIn} in
	 * {@code desiredMatchingBits} bits within {@code timeout} {@code unit}s. Once a match is found
	 * (or timeout occurs), {@code callback} is invoked.
	 */
	public void solvePuzzleWithTimeout(final byte[] rawBytesIn, final int desiredMatchingBits,
			final long timeout, final TimeUnit unit, final PuzzleSolutionCallback callback) {

		pool.execute(new Runnable() {
			@Override
			public void run() {
				long start = System.currentTimeMillis();

				// Compute the original hash of the data
				SHA1Hasher hasher = new SHA1Hasher();
				byte[] origHash = hasher.calculateHash(rawBytesIn);

				// While our timeout is not exhausted, generate random 20 byte hashes and retain the
				// one that matches the original hash in the most number of bits.
				byte[] best = new byte[20];
				int bestCount = 0;
				byte[] candidate = new byte[20];
				while (start + TimeUnit.MILLISECONDS.convert(timeout, unit) > System
						.currentTimeMillis() && bestCount < desiredMatchingBits) {
					random.nextBytes(candidate);
					byte[] candidateHash = new SHA1Hasher().calculateHash(candidate);
					int matchingBits = computeMatchingBitsLeftToRight(candidateHash, origHash);
					if (matchingBits > bestCount) {
						best = candidate;
						bestCount = matchingBits;
					}
				}

				callback.solved(rawBytesIn, best, bestCount);
			}
		});
	}

	/**
	 * Computes the number of overlapping bits in two byte arrays, sequentially, stopping at the
	 * first difference.
	 */
	@VisibleForTesting
    public static short computeMatchingBitsLeftToRight(byte[] lhs, byte[] rhs) {
		Preconditions.checkArgument(lhs.length == rhs.length,
				"Provided byte arrays are of different sizes.");

        short count = 0;
		for (int i = 0; i < lhs.length; i++) {
			if (lhs[i] == rhs[i]) {
				count += 8;
				continue;
			}

			for (int bit = 7; bit >= 0; bit--) {
				if ((lhs[i] & POWERS_OF_2[bit]) == (rhs[i] & POWERS_OF_2[bit])) {
					count++;
				} else {
					return count;
				}
			}
		}
		return count;
	}
}
