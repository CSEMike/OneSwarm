package edu.washington.cs.oneswarm.test.integration;

import static edu.washington.cs.oneswarm.test.integration.ServiceSharingSingleProcessTest.CLIENT_PORT;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingSingleProcessTest.ECHO_PORT;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingSingleProcessTest.LOCALHOST;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingSingleProcessTest.SEARCH_KEY;
import static org.testng.Assert.fail;

import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.EchoServer;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceChannelEndpoint;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.FourProcessTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

public class ServiceSharingFourProcessTest extends FourProcessTestBase {

    private static Logger logger = Logger.getLogger(ServiceSharingFourProcessTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        FourProcessTestBase.startSelenium = false;
        FourProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingSingleProcessTest.logger);
        logFinest(EchoServer.logger);

        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testServiceSharing() throws InterruptedException {
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
            tellRemoteToShareService("echo", SEARCH_KEY, LOCALHOST, ECHO_PORT);
            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);

            Thread.sleep(5000);
            ServiceSharingSingleProcessTest.doEchoTest();
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testServiceSharing()");
        }
    }

    private void tellRemoteToShareService(String name, long searchKey, String address, int port) {
        processC.getCoordinator().addCommand(
                "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment");
        processC.getCoordinator().addCommand(
                "share_service " + name + " " + searchKey + " " + address + " " + port);
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ServiceSharingFourProcessTest.class);
    }
}
