package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.uw.cse.netlab.utils.ByteManip;
import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceChannelEndpoint;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingLoopback;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.ConditionWaiter;
import edu.washington.cs.oneswarm.test.util.LocalProcessesTestBase;
import edu.washington.cs.oneswarm.test.util.TestReceivedServer;
import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * Tests that data can be sent from the client to the server. Similar to
 * ServiceSharingSingleProcessTest but only sends data in one direction.
 * 
 * @author Krysta Yousoufian
 * 
 */
public class ServiceSharingClientTest extends LocalProcessesTestBase {

    static final int SEARCH_KEY = 12345;
    static Logger logger = Logger.getLogger(ServiceSharingClientTest.class.getName());
    final static int SERVICE_PORT = 26012;
    final static int CLIENT_PORT = 26013;
    final static String LOCALHOST = "127.0.0.1";

    private final static Random random = new Random(12345);
    private static Socket clientSocket;
    private TestReceivedServer server;

    @BeforeClass
    public static void setupClass() {
        startLocalInstance();
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
        logFinest(TestReceivedServer.logger);
        logFinest(ServiceSharingLoopback.logger);
        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testLocalServiceClient() throws Exception {
        /*
         * Verify that data can be sent from client to server
         * 
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start local server
         * * Register one server service
         * * Register one client service
         * * Send bytes from client to server
         * * Verify that server receives bytes
         */

        try {
            // Register the server service
            ServiceSharingManager.getInstance().registerSharedService(SEARCH_KEY, "testReceived",
                    new InetSocketAddress(LOCALHOST, SERVICE_PORT));

            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("testReceivedClient",
                    CLIENT_PORT, SEARCH_KEY);

            // Create the server and open the client socket
            server = new TestReceivedServer(SERVICE_PORT);
            server.startDaemonThread(true);
            clientSocket = new Socket(LOCALHOST, CLIENT_PORT);

            // test 1 byte
            testDataSent("t".getBytes("UTF-8"));

            // test a couple of bytes
            testDataSent("hellš".getBytes("UTF-8"));

            // Send
            testRandomDataSent(DataMessage.MAX_PAYLOAD_SIZE);

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testLocalServiceClient()");
        }

    }

    private void testRandomDataSent(int numBytes) {
        byte[] out = new byte[numBytes];
        random.nextBytes(out);
    }

    private void testDataSent(byte[] out) throws IOException {
        // Send data
        long startTime = System.currentTimeMillis();
        OutputStream outStream = clientSocket.getOutputStream();
        outStream.write(out);

        // Wait for data to be received
        new ConditionWaiter(new ConditionWaiter.Predicate() {
            @Override
            public boolean satisfied() {
                return server.hasData();
            }
        }, 1000 * 1000).await();

        assertTrue(server.hasData());
        assertEquals(out, server.getLatestData());
        server.clearData();
        logger.info("time=" + (System.currentTimeMillis() - startTime));
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
        TestUtils.swtCompatibleTestRunner(ServiceSharingClientTest.class);
    }
}
