package edu.washington.cs.oneswarm.f2f.puzzle.test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.SHA1Hasher;
import org.junit.Assert;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.puzzle.PuzzleManager;
import edu.washington.cs.oneswarm.f2f.puzzle.PuzzleSolutionCallback;
import edu.washington.cs.oneswarm.util.Box;

/** Test for the {@code PuzzleManager} class. */
public class PuzzleManagerTest {

	private static Logger logger = Logger.getLogger(PuzzleManagerTest.class.getName());

	@Test
	public void testComputeMatchingBitsLeftToRight() throws Exception {
		logger.info("Start testComputeMatchingBitsLeftToRight()");

		// Test plan: For several synthetic bitstrings, compute the number of sequentially matching 
		// bits and verify that it matches our manually computed expectation.

		byte[] lhs = new byte[] { (byte) 242 }; // 1111 0010
		byte[] rhs = new byte[] { (byte) 72 }; // 0100 1000
		Assert.assertEquals(PuzzleManager.computeMatchingBitsLeftToRight(lhs, rhs), 0);
		
		lhs = new byte[] { (byte) 186 }; // 1011 1010
		rhs = new byte[] { (byte) 170 }; // 1010 1010
		Assert.assertEquals(PuzzleManager.computeMatchingBitsLeftToRight(lhs, rhs), 3);

		lhs = new byte[] { (byte) 213 }; // 1101 0101
		rhs = new byte[] { (byte) 213 }; // 1101 0101
		Assert.assertEquals(PuzzleManager.computeMatchingBitsLeftToRight(lhs, rhs), 8);

		lhs = new byte[] { (byte) 213, (byte) 186, (byte) 242 }; // 1101 0101 1011 1010 1111 0010
		rhs = new byte[] { (byte) 213, (byte) 170, (byte) 72 }; // 1101 0101 1010 1010 0100 1000
		Assert.assertEquals(PuzzleManager.computeMatchingBitsLeftToRight(lhs, rhs), 11);

		logger.info("End testComputeMatchingBitsLeftToRight()");
	}

	@Test
	public void testSolvePuzzle() throws Exception {
		logger.info("Start testSolvePuzzle()");

		// Test plan: Try to solve puzzles in an increasing number of bits with an infinite timeout.
		// Verify that solutions are found.

		PuzzleManager puzzler = PuzzleManager.get();
		Random r = new Random(5);
		byte[] bytesIn = new byte[40];
		r.nextBytes(bytesIn);
		final Box<Boolean> good = new Box<Boolean>(true);
		for (int matchingBits = 1; matchingBits < 20; matchingBits++) {
			final int matchingBitsShadow = matchingBits;
			final CountDownLatch latch = new CountDownLatch(1);
			final long started = System.currentTimeMillis();
			puzzler.solvePuzzleWithTimeout(bytesIn, matchingBits, 1, TimeUnit.DAYS,
					new PuzzleSolutionCallback() {
						@Override
						public void solved(byte[] offeredBytes, byte[] bestSolution,
								int purportedMatchingBits) {
							SHA1Hasher hasher = new SHA1Hasher();
							byte[] origHash = hasher.calculateHash(offeredBytes);
							byte[] solutionHash = hasher.calculateHash(bestSolution);
							int matching = PuzzleManager.computeMatchingBitsLeftToRight(origHash,
									solutionHash);
							if (matching < matchingBitsShadow) {
								logger.severe("solvePuzzleWithTimeout() matched in " + matching
										+ " / expected: " + matchingBitsShadow);
								good.set(false);
							} else {
								logger.info("Matched " + matching + " in "
										+ (System.currentTimeMillis() - started) + " ms");
							}
							latch.countDown();
						}
					});
			latch.await(20, TimeUnit.SECONDS);

            Assert.assertTrue("solvePuzzle failed for " + matchingBits + " bits.", good.get());
		}

		logger.info("End testSolvePuzzle()");
	}

	public static final void main(String... args) throws Exception {
		org.junit.runner.JUnitCore.main(PuzzleManagerTest.class.getCanonicalName());
	}
}
