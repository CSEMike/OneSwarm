package edu.washington.cs.oneswarm.planetlab;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.DispatchMode;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import edu.washington.cs.oneswarm.f2f.FileListManager;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.network.SearchManager.RotatingBloomFilter;

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

        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (Exception e) {
            logger.warning("Error during hostname retrieval: " + e.toString());
        }

        logger.info("Using hostname: " + hostname);

        AnalyticsConfigData analyticsConfig = new AnalyticsConfigData(trackingCode);
        JGoogleAnalyticsTracker analyticsTracker = new JGoogleAnalyticsTracker(analyticsConfig,
                GoogleAnalyticsVersion.V_4_7_2, DispatchMode.SYNCHRONOUS);

        AzureusCore core = AzureusCoreImpl.getSingleton();

        String versionString = "Unknown";
        try {
            versionString = Constants.getOneSwarmAzureusModsVersion();
            InputStream buildInfo = getClass().getClassLoader().getResourceAsStream("build.txt");
            Properties info = new Properties();
            info.load(buildInfo);
            versionString = info.getProperty("build.number");
        } catch (Exception e) {
            logger.warning("Error loading build info: " + e.toString()
                    + " (This is expected in integration tests.)");
        }

        Rate<Long> searchRate = new Rate<Long>();
        Rate<Long> cacheHitRate = new Rate<Long>();

        Rate<Long> uploadRate = new Rate<Long>();
        Rate<Long> downloadRate = new Rate<Long>();

        // Collect the stats, report, wait an hour, repeat.
        while (true) {

            try {

                FileListManager filelistManager = f2f.getOverlayManager().getFilelistManager();

                double searchRateInst = searchRate.updateAndGetRate(filelistManager
                        .getSearchesTotal());
                double cacheHitRateInst = cacheHitRate.updateAndGetRate(filelistManager
                        .getSearchCacheHits());

                long sessionUL = core.getGlobalManager().getStats().getTotalDataBytesSent()
                        + core.getGlobalManager().getStats().getTotalProtocolBytesSent();
                long sessionDL = core.getGlobalManager().getStats().getTotalDataBytesReceived()
                        + core.getGlobalManager().getStats().getTotalProtocolBytesReceived();

                RotatingBloomFilter recentSearches = f2f.getOverlayManager().getSearchManager()
                        .getRecentSearchesBloomFilter();
                analyticsTracker.trackEvent("Stats", "Scalars", "bfStoredSearches",
                        recentSearches.getPrevFilterNumElements());
                analyticsTracker.trackEvent("Stats", "Scalars", "bfFalsePostive100000",
                        (int) (100000 * recentSearches.getPrevFilterFalsePositiveEst()));

                // Heartbeat
                analyticsTracker.trackPageView("/running.plab", versionString, hostname);

                // Update rates
                analyticsTracker.trackEvent("Stats", "Rates", "searchRate", (int) searchRateInst);
                analyticsTracker.trackEvent("Stats", "Rates", "searchCacheHitRate",
                        (int) cacheHitRateInst);
                analyticsTracker.trackEvent("Stats", "Rates", "upload",
                        (int) uploadRate.updateAndGetRate(sessionUL));
                analyticsTracker.trackEvent("Stats", "Rates", "download",
                        (int) downloadRate.updateAndGetRate(sessionDL));

                // Update scalars
                analyticsTracker.trackEvent("Stats", "Scalars", "friendConnections", f2f
                        .getOverlayManager().getConnectCount());

                System.gc();
                analyticsTracker.trackEvent("Stats", "Scalars", "mem", (int) Runtime.getRuntime()
                        .totalMemory());

                logger.info("Reported analytics stats events...");

            } catch (Exception e) {
                logger.warning("Error during analytics reporting: " + e.toString());
                e.printStackTrace();
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
