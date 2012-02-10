package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.fail;

import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;

public class ServiceSharingTwoProcessTeardownTest extends TwoProcessTestBase {
    private static final int SEARCH_KEY = ServiceSharingTeardownTest.SEARCH_KEY;
    private final static int SERVER_PORT = ServiceSharingTeardownTest.SERVER_PORT;
    private final static int CLIENT_PORT = ServiceSharingTeardownTest.CLIENT_PORT;
    private final static String LOCALHOST = ServiceSharingTeardownTest.LOCALHOST;

    private static Logger logger = Logger.getLogger(ServiceSharingTwoProcessTeardownTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        TwoProcessTestBase.startSelenium = false;
        TwoProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingTeardownTest.logger);
        // logFinest(ReadController.logger);

        // logFinest(ServiceSharingManager.logger);
        // logFinest(ServiceConnection.logger);
        // logFinest(SearchManager.logger);
    }

    @Test
    public void testServiceTeardown() throws InterruptedException {
        /*
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start a remote copy of oneswarm with this one as a friend
         * * Register one server service on the remote instance
         * * Register one client service in local instance
         * * Connect to client service port
         * * Write bytes to client service port
         * * Verify that the correct bytes are echoed back (despite going
         * through all the azureus network layers).
         */

        try {
            tellRemoteToShareService("echo", SEARCH_KEY, LOCALHOST, SERVER_PORT);
            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);
            Thread.sleep(5000);
            ServiceSharingTeardownTest.doTest();
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testServiceTeardown()");
        }
    }

    private void tellRemoteToShareService(String name, long searchKey, String address, int port) {
        localOneSwarm.getCoordinator().addCommand(
                "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment");
        localOneSwarm.getCoordinator().addCommand(
                "share_service " + name + " " + searchKey + " " + address + " " + port);
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ServiceSharingTwoProcessTeardownTest.class);
    }
}
