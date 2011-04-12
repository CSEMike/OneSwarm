package edu.washington.cs.oneswarm.test.util;

import junit.framework.Assert;

/**
 * A utility class which waits a maximum time for some condition to be satisfied, failing
 * if the condition is not satisfied.
 */
public class ConditionWaiter {

	/** The condition predicate interface. */
	public interface Predicate {
		public boolean satisfied();
	}

	/** The predicate on which to wait. */
	private final Predicate predicate;

	/** The maximum amount of time to wait for {@code predicate} to be satisified. */
	private final long maxDelay;

	/** Constructs a ConditionWaiter which checks a given {@code predicate} for {@code maxDelay}. */
	public ConditionWaiter(Predicate predicate, long maxDelay) {
		this.predicate = predicate;
		this.maxDelay = maxDelay;
	}

	/** Waits for the specified predicate, failing if not satisfied in the timeout interval. */
	public void await() {
		try {
			long started = System.currentTimeMillis();
			while (!predicate.satisfied()) {
				Thread.sleep(100);

				if (started + maxDelay < System.currentTimeMillis()) {
					Assert.fail("ConditionWaiter timed out.");
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
