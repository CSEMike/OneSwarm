package edu.washington.cs.oneswarm.planetlab;

import java.util.logging.Logger;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.DispatchMode;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import edu.washington.cs.oneswarm.f2f.FileListManager;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;

public class AnalyticsReporter extends Thread {

	private static Logger logger = Logger.getLogger(AnalyticsReporter.class.getName());

	public static final String EXPERIMENTAL_TRACKING_PROPERTY = "oneswarm.experimental.tracking.code";

	private final OSF2FMain f2f;

	public AnalyticsReporter(OSF2FMain f2f) {
		setDaemon(true);
		setName("PLab analytics reporter");
		this.f2f = f2f;
	}

	@Override
	public void run() {

		String trackingCode = System.getProperty(EXPERIMENTAL_TRACKING_PROPERTY);
		if (trackingCode == null) {
			logger.warning("Could not load analytics tracking code, skipping reporting...");
			return;
		}

		logger.info("Got tracking code: " + trackingCode);

		AnalyticsConfigData analyticsConfig = new AnalyticsConfigData(trackingCode);
		JGoogleAnalyticsTracker analyticsTracker = new JGoogleAnalyticsTracker(analyticsConfig,
				GoogleAnalyticsVersion.V_4_7_2, DispatchMode.SYNCHRONOUS);

		// Collect the stats, report, wait an hour, repeat.
		while (true) {
			
			Rate<Long> searchRate = new Rate<Long>();
			Rate<Long> cacheHitRate = new Rate<Long>();
			
			try {
				
				FileListManager filelistManager = f2f.getOverlayManager().getFilelistManager();
				
				double searchRateInst = searchRate.updateAndGetRate(filelistManager
						.getSearchesTotal());
				double cacheHitRateInst = cacheHitRate.updateAndGetRate(filelistManager
						.getSearchCacheHits());

				analyticsTracker
						.trackEvent("stats", "rates", "searchHitRate", (int) searchRateInst);
				
				analyticsTracker.trackEvent("stats", "rates", "searchCacheHitRate",
						(int) cacheHitRateInst);
				
				logger.info("Reported analytics stats events...");

			} catch (Exception e) {
				logger.warning("Error during analytics reporting: " + e.toString());
			}

			// 1 hour between stat reporting
			try {
				Thread.sleep(60 * 60 * 1000);
			} catch (Exception e) {
			}
		}

	}

	/** Test driver for the analytics code. */
	public static final void main(String[] args) throws Exception {
		String trackingCode = System.getProperty(EXPERIMENTAL_TRACKING_PROPERTY);
		System.out.println("Got tracking code: " + trackingCode);
		AnalyticsConfigData analyticsConfig = new AnalyticsConfigData(trackingCode);
		JGoogleAnalyticsTracker analyticsTracker = new JGoogleAnalyticsTracker(analyticsConfig,
				GoogleAnalyticsVersion.V_4_7_2, DispatchMode.SYNCHRONOUS);

		System.out.println("tracking events...");

		analyticsTracker.trackPageView("/pagewitheverything.java", "page with everything",
				"www.dmurph.com");
		analyticsTracker.trackEvent("Greetings", "Hello");
		analyticsTracker.trackEvent("test", "report", "someAction", 5);
		analyticsTracker.trackPageView("foobear.html", "test page", "somehostname");

		System.out.println("Done with tracking events.");

		Thread.sleep(1000);

	}

}
