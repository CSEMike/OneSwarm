package edu.washington.cs.oneswarm.planetlab;

import org.apache.commons.lang.time.StopWatch;

/** A class which accepts cumulative values and differences. */
public class Rate<T extends Number> {

	T eldestValue = null;
	StopWatch watch = new StopWatch();

	public Rate() {
		eldestValue = null;
		watch.start();
	}

	/**
	 * Returns the rate of change per second relative to the previous call. If no call has been
	 * made, returns 0 and sets an initial base value.
	 */
	public double updateAndGetRate(T latestValue) {

		if (eldestValue == null) {
			eldestValue = latestValue;
			return 0;
		}

		if (watch.getTime() == 0) {
			return 0;
		}

		double out = (latestValue.doubleValue() - eldestValue.doubleValue())
				/ (watch.getTime() / 1000.0);
		watch.start();
		eldestValue = latestValue;
		return out;
	}
}
