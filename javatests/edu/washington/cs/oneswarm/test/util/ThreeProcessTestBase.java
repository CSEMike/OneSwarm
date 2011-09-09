package edu.washington.cs.oneswarm.test.util;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.aelitis.azureus.ui.UIFunctionsManager;
import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.test.integration.oop.LocalOneSwarm;

/**
 * Set up a three process chained friend network:
 * 
 * (Local Process)<->(Process A)<->(Process B)
 * 
 * @author isdal
 * 
 */
public class ThreeProcessTestBase extends OneSwarmTestBase {

    private static Logger logger = Logger.getLogger(ThreeProcessTestBase.class.getName());

    /** The locally running selenium test server. */
    protected static Process seleniumServer;

    /** The selenium control interface. */
    protected static Selenium selenium;
    protected static boolean startSelenium = true;

    /** The external oneswarm processes */
    protected static LocalOneSwarm processA;
    protected static LocalOneSwarm processB;

    /** Should the two created peers be connected? */
    protected static boolean connectPeers = true;

    /** Should we include experimental support? */
    protected static boolean experimentalInstance = false;

    /** Commands to send before friends are connected */
    protected static List<String> preConnectCommands = new LinkedList<String>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        if (startSelenium) {
            seleniumServer = TestUtils.startSeleniumServer((new File(".").getAbsolutePath()));
        }
        // If running in experimental mode, set this but ignore the config.
        // We'll configure statically.
        if (experimentalInstance) {
            System.setProperty("oneswarm.experimental.config.file", "dummy");
        }

        // Start a local client in this JVM
        TestUtils.awaitJVMOneSwarmStart();

        // Fire up the other instances
        startInstances();

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

    private static void startInstances() throws Exception {
        final OSF2FMain f2fMain = OSF2FMain.getSingelton();

        /**
         * Fire up the other oneswarm processes and wait
         */
        processA = new LocalOneSwarm(experimentalInstance);
        processA.start();
        processB = new LocalOneSwarm(experimentalInstance);
        processB.start();
        TestUtils.awaitInstanceStart(processA);
        TestUtils.awaitInstanceStart(processB);

        runPreConnectCommands(processA);
        runPreConnectCommands(processB);

        /**
         * Wait for the friend connectors to start
         * 
         */
        // Wait for the friend connectors to become available (prerequisite
        // for friend connection)
        waitForFriendConnector(f2fMain);
        waitForFriendConnector(processA);
        waitForFriendConnector(processB);

        /**
         * Add the friend connections
         */
        // Get the public keys
        String localKey = new String(Base64.encode(f2fMain.getOverlayManager().getOwnPublicKey()
                .getEncoded()));
        String processAKey = processA.getPublicKey();
        String processBKey = processB.getPublicKey();

        // Connect us to node A
        Friend f = new Friend(true, true, new Date(), new Date(), InetAddress.getLocalHost(),
                processA.getCoordinator().getPort(), processA.getLabel(),
                Base64.decode(processAKey), "test", 0, 0, false, true);
        f2fMain.getFriendManager().addFriend(f);
        f2fMain.getDHTConnector().connectToFriend(f);

        // Add us and node B to node A
        processA.getCoordinator().addCommand("addkey TEST " + localKey + " true true");
        processA.getCoordinator().addCommand("addkey TEST " + processBKey + " true true");

        // Add node A to node B
        processB.getCoordinator().addCommand("addkey TEST " + processAKey + " true true");

        // Wait for the connections to be established
        processB.waitForOnlineFriends(1);
        processA.waitForOnlineFriends(2);
    }

    private static void runPreConnectCommands(LocalOneSwarm process) {
        for (String command : preConnectCommands) {
            System.err.println("ADDING: " + command);
            process.getCoordinator().addCommand(command);
        }
    }

    protected static void waitForFriendConnector(final LocalOneSwarm p) {
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return p.getCoordinator().isFriendConnectorAvailable();
            }
        }, 90 * 1000).await();
    }

    protected static void waitForFriendConnector(final OSF2FMain f2fMain) {
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return f2fMain.getDHTConnector() != null;
            }
        }, 90 * 1000).await();
    }

    private static void shutDown(final LocalOneSwarm process) {
        process.getCoordinator().addCommand("shutdown");
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return process.getCoordinator().getPendingCommands().size() == 0;
            }
        }, 10000).await();
        process.stop();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        logger.info("Tearing down test. Quitting JVM instance");
        // Quit OneSwarm
        if (UIFunctionsManager.getUIFunctions() != null) {
            UIFunctionsManager.getUIFunctions().requestShutdown();
        }
        logger.info("Sending shutdown to oop instance");
        shutDown(processA);
        shutDown(processB);

        logger.info("selenium.stop()");
        // Quit browser
        if (selenium != null) {
            selenium.stop();
        }
        logger.info("selenium server stop");
        // Quit RC Server
        if (seleniumServer != null) {
            seleniumServer.destroy();
        }
    }
}
