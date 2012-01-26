package edu.washington.cs.oneswarm.test.integration;

import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.CLIENT_PORT;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.LOCALHOST;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.SEARCH_KEY;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.SERVER_PORT;
import static org.testng.Assert.fail;

import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceChannelEndpoint;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.ThreeProcessTestBase;

public class ServiceSharingThreeProcessTeardownTest extends ThreeProcessTestBase {

    private static Logger logger = Logger.getLogger(ServiceSharingThreeProcessTeardownTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        ThreeProcessTestBase.startSelenium = false;
        ThreeProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingSingleProcessTest.logger);
        // logFinest(SetupPacketTraceRoute.logger);
        // logFinest(ReadController.logger);

        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testServiceTeardown() throws InterruptedException {

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
            logger.info("End testServiceSharing()");
        }
    }

    private void tellRemoteToShareService(String name, long searchKey, String address, int port) {
        processB.getCoordinator().addCommand(
                "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment");
        processB.getCoordinator().addCommand(
                "share_service " + name + " " + searchKey + " " + address + " " + port);
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ServiceSharingThreeProcessTeardownTest.class);
    }
}
