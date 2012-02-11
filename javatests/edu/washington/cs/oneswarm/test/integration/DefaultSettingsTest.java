package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter.Predicate;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TorrentExperimentFunctions;

public class DefaultSettingsTest extends OneSwarmTestBase {
    private static final String RESULTS_FILE = "/tmp/new_user_default_settings_test";
    public static String TEST_FILE_BASE64 = "OktR+K6G/Y3E6xlSJE3Jv2GKG/A=";
    public static String TEST_FILE_URI = "oneswarm:?xt=urn:osih:HJFVD6FOQ36Y3RHLDFJCITOJX5QYUG7Q";

    static Logger logger = Logger.getLogger(DefaultSettingsTest.class.getName());

    /** The locally running selenium test server. */
    protected static Process seleniumServer;

    /** The selenium control interface. */
    protected static Selenium selenium;
    protected static boolean startSelenium = true;

    public static final int MAX_METAINFO_RETRIES = 5;
    public static final int MIN_FRIENDS = 3;
    public static final int MAX_WAIT = 20 * 60 * 1000;
    public static final int MAX_METAINFO_WAIT = 60 * 1000;

    @BeforeClass
    public static void setUpClass() throws Exception {
        TestUtils.DEFAULT_NAME = "OneSwarmHourlyPerfTest";
        if (startSelenium) {
            seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));
        }

        // Start a local client in this JVM
        if (!TestUtils.swtTestRunnerUsed()) {
            new Thread("Off-main Oneswarm") {
                @Override
                public void run() {
                    try {
                        TestUtils.startOneSwarmForTest();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }
            }.start();
        }
        TestUtils.awaitJVMOneSwarmStart();

        logger.info("OOP LocalOneSwarm started.");
        if (startSelenium) {
            selenium = new DefaultSelenium("127.0.0.1", 4444, "*firefox",
                    TestUtils.JVM_INSTANCE_WEB_UI) {
                // Fix for bug:
                // http://code.google.com/p/selenium/issues/detail?id=408
                @Override
                public void open(String url) {
                    commandProcessor.doCommand("open", new String[] { url, "true" });
                }
            };
            selenium.start();
        }
    }

    @Before
    public void setupTest() throws IOException {
        if (TestUtils.isLocalCommunityServerRunning()) {
            TestUtils.flushCommunityServerState();
        }
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
    }

    long friendConnect = -1;
    long total = -1;
    long metainfo = -1;
    long transfer = -1;
    int numRetries = -1;

    @Test
    public void testDownloadFile() throws Exception {
        long startTime = System.currentTimeMillis();
        /*
         * Test plan:
         * * Click 'use default settings'
         * * Wait for at least 5 friends to connect
         * * Download test file
         */
        try {
            // Register the server service

            selenium.openWindow("http://127.0.0.1:4000/", "localinstance");
            selenium.selectWindow("localinstance");

            Thread.sleep(5000);
            TestUtils.awaitAndClick(selenium, "useDefaultsLink");
            Thread.sleep(5000);
            final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            new ConditionWaiter(new Predicate() {
                @Override
                public boolean satisfied() {
                    return f2fMain.getOverlayManager().getConnectCount() > MIN_FRIENDS;
                }
            }, MAX_WAIT).awaitFail();

            long friendsConnectedAt = System.currentTimeMillis();
            friendConnect = friendsConnectedAt - startTime;

            TorrentExperimentFunctions functions = new TorrentExperimentFunctions(
                    ExperimentalHarnessManager.get().getCoreInterface(), null);
            final GlobalManager globalManager = AzureusCoreImpl.getSingleton().getGlobalManager();

            Predicate downloadStart = new Predicate() {
                @Override
                public boolean satisfied() {
                    return globalManager.getDownloadManagers().size() > 0;
                }
            };
            numRetries = 0;
            while (numRetries < MAX_METAINFO_RETRIES) {
                functions.downloadAndStart(TEST_FILE_BASE64, 0);
                if (new ConditionWaiter(downloadStart, MAX_METAINFO_WAIT).awaitWarn()) {
                    break;
                }
                numRetries++;
            }
            if (!downloadStart.satisfied()) {
                fail("Download did not start after " + numRetries + " attempts");
            }
            long metainfoDownloadedAt = System.currentTimeMillis();
            metainfo = metainfoDownloadedAt - friendsConnectedAt;
            final DownloadManager dm = (DownloadManager) globalManager.getDownloadManagers().get(0);
            new ConditionWaiter(new Predicate() {
                @Override
                public boolean satisfied() {
                    if (dm.getDiskManager() == null) {
                        return false;
                    }
                    return dm.getDiskManager().getPercentDone() == 100;
                }
            }, MAX_WAIT).awaitFail();
            long downloadCompletedAt = System.currentTimeMillis();
            transfer = downloadCompletedAt - metainfoDownloadedAt;
            total = System.currentTimeMillis() - startTime;

            logger.fine("Download completed, total=" + total + " ms");
            logger.finer("Friend connect: time=" + friendConnect + " ms");
            logger.finer("Metainfo: time=" + metainfo + " ms, retries=" + numRetries);
            logger.finer("Transfer: time=" + transfer + " ms");

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            writeResult();
            logger.info("End testLocalServiceEcho()");
        }
    }

    private void writeResult() throws IOException {
        String[] header = new String[] { "total", "friend_connect", "metainfo", "metainfo_retries",
                "transfer" };
        Object[] result = new Object[] { total, friendConnect, metainfo, numRetries, transfer };
        saveResult(RESULTS_FILE, header, result);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        // Quit OneSwarm
        if (UIFunctionsManager.getUIFunctions() != null) {
            UIFunctionsManager.getUIFunctions().requestShutdown();
        }
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(DefaultSettingsTest.class);
    }
}
