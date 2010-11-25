package edu.washington.cs.oneswarm.test.util;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import edu.washington.cs.oneswarm.test.integration.LocalOneSwarm;
import edu.washington.cs.oneswarm.test.integration.LocalOneSwarmListener;

/**
 * Miscellaneous utility functions for running OneSwarm integration tests.
 *
 * All methods in this class should be static.
 */
public class TestUtils {

	/** Blocks until the LocalOneSwarm {@code instance} has started. */
	public static void awaitInstanceStart(LocalOneSwarm instance) {
		final CountDownLatch latch = new CountDownLatch(1);

		/*
		 * We need to add the listener before checking if we're running to avoid an
		 * initialization race.
		 */
		LocalOneSwarmListener listener = new LocalOneSwarmListener() {
			public void instanceStarted(LocalOneSwarm instance) {
				latch.countDown();
			}
		};
		instance.addListener(listener);

		try {
			if (instance.getState() == LocalOneSwarm.State.RUNNING) {
				latch.countDown();
			}
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			instance.removeListener(listener);
		}
	}

	/** Starts the selenium RC server and returns the associated {@code Process}. */
	public static Process startSeleniumServer(String rootPath) throws IOException {
		ProcessBuilder pb = new ProcessBuilder("/usr/bin/java",
				"-jar",
				rootPath + "/build/test-libs/selenium-server.jar");

		Process p = pb.start();
		new ProcessLogConsumer("SeleniumServer", p).start();
		return p;
	}
}
