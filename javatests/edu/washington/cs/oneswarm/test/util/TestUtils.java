package edu.washington.cs.oneswarm.test.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestResult;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Assert;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarm;
import edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarmListener;

/**
 * Miscellaneous utility functions for running OneSwarm integration tests.
 * 
 * All methods in this class should be static.
 */
public class TestUtils {

    private static Logger logger = Logger.getLogger(TestUtils.class.getName());

    /** The web interface port used by the JVM OneSwarm instance. */
    public static final int JVM_INSTANCE_WEB_UI_PORT = 4000;

    /** The port used by the JVM instance StartServer. */
    public static final int JVM_INSTANCE_START_SERVER_PORT = JVM_INSTANCE_WEB_UI_PORT + 2;

    /** The URL of the web UI for the JVM test instance. */
    public static final String JVM_INSTANCE_WEB_UI = "http://127.0.0.1:" + JVM_INSTANCE_WEB_UI_PORT
            + "/";

    // Changed to 8889 to allow running OneSwarm in hosted mode along with the
    // community server
    public static final String TEST_COMMUNITY_SERVER = "localhost:8889";

    /** Checks if a test instance of the community server is running locally. */
    public static boolean isLocalCommunityServerRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://" + TEST_COMMUNITY_SERVER
                    + "/community").openConnection();

            // Expecting BAD_REQUEST since we didn't include a key -- indicates
            // that the server is
            // up.
            if (conn.getResponseCode() == HttpServletResponse.SC_BAD_REQUEST) {
                return true;
            }

            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Community server check failed: " + e.toString());
            return false;
        }
        return false;
    }

    /** Blocks until the LocalOneSwarm {@code instance} has started. */
    public static void awaitInstanceStart(LocalOneSwarm instance) {
        final CountDownLatch latch = new CountDownLatch(1);

        /*
         * We need to add the listener before checking if we're running to avoid
         * an initialization race.
         */
        LocalOneSwarmListener listener = new LocalOneSwarmListener() {
            @Override
            public void instanceStarted(LocalOneSwarm instance) {
                latch.countDown();
            }
        };
        instance.addListener(listener);

        try {
            if (instance.getState() == LocalOneSwarm.State.RUNNING) {
                latch.countDown();
            }
            if (!latch.await(30, TimeUnit.SECONDS)) {
                Assert.fail();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            instance.removeListener(listener);
        }
    }

    /**
     * Starts the a OneSwarm client in the current JVM in a testing
     * configuration. This method will never return.
     */
    public static void startOneSwarmForTest() throws IOException {

        final String label = "OneSwarmJVM";

        // Configure the environment for tests
        System.setProperty("MULTI_INSTANCE", "true");
        String warPath = new File("./gwt-bin", "war").getAbsolutePath();
        System.setProperty("debug.war", warPath);

        Map<String, String> scratchPaths = createScratchLocationsForTest(label);

        System.setProperty("oneswarm.integration.test", "1");
        System.setProperty("oneswarm.integration.user.data", scratchPaths.get("userData"));
        System.setProperty("azureus.config.path", scratchPaths.get("userData"));
        System.setProperty("oneswarm.integration.web.ui.port", JVM_INSTANCE_WEB_UI_PORT + "");
        System.setProperty("oneswarm.integration.start.server.port",
                Integer.toString(JVM_INSTANCE_START_SERVER_PORT));
        System.setProperty("azureus.security.manager.install", "0");
        System.setProperty("oneswarm.experimental.config.file",
                scratchPaths.get("experimentalConfig"));
        System.setProperty("nolaunch_startup", "1");

        // We use an experimental config to set the instance name.
        PrintStream experimentalConfig = new PrintStream(new FileOutputStream(
                scratchPaths.get("experimentalConfig")));
        experimentalConfig.println("name " + label);

        com.aelitis.azureus.ui.Main.main(new String[] {});
    }

    /** Awaits the start of this JVM's OneSwarm instance. */
    public static void awaitJVMOneSwarmStart() {
        // Await start of this JVM's instance of OneSwarm.
        final CountDownLatch latch = new CountDownLatch(1);

        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return AzureusCoreImpl.isCoreAvailable();
            }
        }, 60 * 1000).await();

        AzureusCore core = AzureusCoreImpl.getSingleton();
        AzureusCoreLifecycleListener l = new AzureusCoreLifecycleListener() {
            @Override
            public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
            }

            @Override
            public void started(AzureusCore core) {
                latch.countDown();
            }

            @Override
            public void stopping(AzureusCore core) {
            }

            @Override
            public void stopped(AzureusCore core) {
            }

            @Override
            public boolean stopRequested(AzureusCore core) throws AzureusCoreException {
                return true;
            }

            @Override
            public boolean restartRequested(AzureusCore core) throws AzureusCoreException {
                return true;
            }

            @Override
            public boolean syncInvokeRequired() {
                return false;
            }
        };

        core.addLifecycleListener(l);
        if (core.isStarted()) {
            latch.countDown();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        core.removeLifecycleListener(l);
    }

    /**
     * Creates the set of directories needed for OneSwarm test instances and
     * returns a {@code Map} with the paths.
     */
    public static Map<String, String> createScratchLocationsForTest(String label)
            throws IOException {
        Map<String, String> scratchPaths = new HashMap<String, String>();
        for (String dir : new String[] { "userData", "workingDir" }) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"), label + "-" + dir);
            FileUtils.deleteDirectory(tmpDir);
            tmpDir.mkdirs();
            scratchPaths.put(dir, tmpDir.getAbsolutePath());
        }

        scratchPaths.put("experimentalConfig", new File(scratchPaths.get("workingDir"),
                "exp.config").getAbsolutePath());

        return scratchPaths;
    }

    /**
     * Starts the selenium RC server and returns the associated {@code Process}.
     */
    public static Process startSeleniumServer(String rootPath) throws IOException {

        // TODO(piatek): Replace /usr/bin/java with something configurable
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", rootPath
                + "/build/test-libs/selenium-server.jar");

        pb.redirectErrorStream(true);
        Process p = pb.start();
        new ProcessLogConsumer("SeleniumServer", p).start();
        return p;
    }

    /**
     * Asynchronously executes JUnit tests for a particular class in a manner
     * suitable for OSX, which requires SWT execution on the main thread.
     */
    public static void swtCompatibleTestRunner(final Class<?> testClass) throws IOException {
        final junit.framework.Test suite = new JUnit4TestAdapter(testClass);
        final TestResult[] box = new TestResult[1];
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread("Off-main TestRunner") {
            @Override
            public void run() {
                box[0] = junit.textui.TestRunner.run(suite);
                latch.countDown();

                if (box[0].errorCount() + box[0].failureCount() > 0) {
                    System.exit(-1);
                }

                // TODO(piatek): [bug] For some reason, the MainWindow$16 inner
                // class throws a ClassNotFoundException
                // on Windows and Linux that prevents a proper shutdown from the
                // test. As a work-around, we quit here
                // manually.
                System.exit(0);
            }
        }.start();
        TestUtils.startOneSwarmForTest();
        try {
            latch.await();
        } catch (final InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Blocks while creating a new {@code LocalOneSwarm} instance which has the
     * local JVM client added and connected as a friend.
     */
    public static LocalOneSwarm spawnOneSwarmInstance(boolean connectToLocalInstance)
            throws Exception {
        final LocalOneSwarm localOneSwarm = new LocalOneSwarm();
        localOneSwarm.start();
        TestUtils.awaitInstanceStart(localOneSwarm);

        // Connect the two clients. First, get our key and add it to the remote
        // instance.
        if (connectToLocalInstance) {
            final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            String base64Key = new String(Base64.encode(f2fMain.getOverlayManager()
                    .getOwnPublicKey().getEncoded()));
            localOneSwarm.getCoordinator().addCommand("addkey TEST " + base64Key + " true true");

            // Wait for the friend connectors to become available (prerequisite
            // for friend
            // connection)
            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    return f2fMain.getDHTConnector() != null;
                }
            }, 90 * 1000).await();

            new ConditionWaiter(new ConditionWaiter.Predicate() {
                @Override
                public boolean satisfied() {
                    return localOneSwarm.getCoordinator().isFriendConnectorAvailable();
                }
            }, 90 * 1000).await();

            // Next add remote friend's key to our instance
            String remoteKey = localOneSwarm.getPublicKey();
            Friend f = new Friend(true, true, new Date(), new Date(), InetAddress.getLocalHost(),
                    localOneSwarm.getCoordinator().getPort(), localOneSwarm.getLabel(),
                    Base64.decode(remoteKey), "test", 0, 0, false, true);
            f2fMain.getFriendManager().addFriend(f);

            f2fMain.getDHTConnector().connectToFriend(f);

            // Wait for the connection to be established
            localOneSwarm.waitForOnlineFriends(1);
        }

        return localOneSwarm;
    }

    /** Blocks until a given selenium {@code elementId} can be found. */
    public static void awaitElement(final Selenium selenium, final String elementId) {
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                boolean isPresent = selenium.isElementPresent(elementId);
                if (isPresent) {
                    logger.info("Found: " + elementId);
                } else {
                    logger.info("Missing: " + elementId);
                }
                return isPresent;
            }
        }, 15000).await();
    }

    /** Uses special servlet handler for test to flush all server side storage. */
    public static void flushCommunityServerState() throws IOException {
        sendAction("flush");
        logger.info("Server state flushed.");
    }

    /** Sends a given {@code action} string to the local dev app server. */
    private static void sendAction(String action) throws IOException {
        String url = "http://" + TEST_COMMUNITY_SERVER + "/test?action=" + action;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        if (conn.getResponseCode() != HttpServletResponse.SC_OK) {
            throw new IOException("Bad status: " + conn.getResponseCode() + " for action: "
                    + action);
        }
    }

    /** Awaits the presence of an element and then sends a click event. */
    public static void awaitAndClick(Selenium selenium, String elementXpath) {
        awaitElement(selenium, elementXpath);
        selenium.click(elementXpath);
    }

    /**
     * Start and leave a test instance of OneSwarm running -- use with the
     * selenium IDE to develop tests.
     */
    public static void main(String[] args) throws Exception {
        TestUtils.startOneSwarmForTest();
    }

}
