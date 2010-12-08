package edu.washington.cs.oneswarm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.ui.gwt.BackendErrorLog;


/**
 * Class which periodically profiles the overall state of the F2F plugin to check
 * invariants that we expect, thread liveness, etc.
 *
 * If an error is detected, it is logged as a reportable bug in the UI.
 */
public class HealthChecker extends Thread {

	private static Logger logger = Logger.getLogger(HealthChecker.class.getName());

	public static final List<String> EXPECTED_THREAD_NAMES = new ArrayList<String>();

	public HealthChecker() {
		setDaemon(true);
		setName("OneSwarmHealthChecker");

		for (String s : new String[] {
				"DelayedSearchQueue",
				"QueueLengthChecker",
				"CommunityServer polling",
				"torrent pruning and F2F startstop",
				"LanFriendFinderListener",
				"ChatDAO message dequeuer",
			}) {
			EXPECTED_THREAD_NAMES.add(s);
		}
	}

	@Override
	public void run() {

		try {
			// Wait until a minute after startup before first run to allow everything to boot
			Thread.sleep(60*1000);
		} catch (Exception e) {}

		while (true) {

			try {
				// Inspect the set of running threads and timers for the expected set.
				Thread [] threads = new Thread[Thread.activeCount()];
				Thread.enumerate(threads);

				Set<String> currentNames = new HashSet<String>();
				for (Thread t : threads) {
					currentNames.add(t.getName());
				}

				// If a thread has died, remove it from the expected set after logging
				// the error to avoid polluting the log.
				List<String> toRemove = new ArrayList<String>();
				for (String expected : EXPECTED_THREAD_NAMES) {
					if (currentNames.contains(expected) == false) {
						String errorStr = "*** Required thread seems to have died: " + expected;
						logger.severe(errorStr);
						BackendErrorLog.get().logString(errorStr);
						toRemove.add(expected);
					}
				}

				for (String r : toRemove) {
					EXPECTED_THREAD_NAMES.remove(r);
				}

				Thread.sleep(30*1000);

			} catch (Throwable t) {
				logger.warning("Unhandled error in the health checker: " + t.toString());
				t.printStackTrace();
			}
		}
	}
}
