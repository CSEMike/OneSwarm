package edu.washington.cs.oneswarm.test.integration;

import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.CLIENT_PORT;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.LOCALHOST;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.SEARCH_KEY;
import static edu.washington.cs.oneswarm.test.integration.ServiceSharingTeardownTest.SERVER_PORT;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceChannelEndpoint;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.FourProcessTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

public class MultipathResillianceTest extends FourProcessTestBase {

    private static Logger logger = Logger.getLogger(MultipathResillianceTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        FourProcessTestBase.startSelenium = false;
        FourProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingSingleProcessTest.logger);

        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testTeardown() throws InterruptedException {
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

    @Test
    public void testMultiClient() throws InterruptedException {
        try {
            tellRemoteToShareService("echo", SEARCH_KEY + 1, LOCALHOST, SERVER_PORT + 2);
            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient",
                    CLIENT_PORT + 6,
                    SEARCH_KEY + 1);

            Thread.sleep(5000);
            // Server.
            ServerSocket server = new ServerSocket(SERVER_PORT + 2);

            byte[] payload = "Test payload".getBytes();
            Socket client1 = new Socket(LOCALHOST, CLIENT_PORT + 6);
            OutputStream outStream1 = client1.getOutputStream();
            outStream1.write(payload);

            Socket serverclient1 = server.accept();
            InputStream inStream1 = serverclient1.getInputStream();

            Socket client2 = new Socket(LOCALHOST, CLIENT_PORT + 6);
            OutputStream outStream2 = client2.getOutputStream();
            outStream2.write(payload);

            Socket serverclient2 = server.accept();
            InputStream inStream2 = serverclient2.getInputStream();

            byte[] buffer = new byte[payload.length];
            Assert.assertEquals(payload.length, inStream1.read(buffer));
            Assert.assertEquals(payload.length, inStream2.read(buffer));

            serverclient2.close();
            Assert.assertEquals(-1, client2.getInputStream().read());

            server.close();
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
        TestUtils.swtCompatibleTestRunner(MultipathResillianceTest.class);
    }
}
