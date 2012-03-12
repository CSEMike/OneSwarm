package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.servicesharing.EchoServer;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.TestUtils;
import edu.washington.cs.oneswarm.test.util.TwoProcessTestBase;

public class ServiceSharingChannelReuseTest extends TwoProcessTestBase {
    private static final int SEARCH_KEY = ServiceSharingSingleProcessTest.SEARCH_KEY;
    private final static int ECHO_PORT = ServiceSharingSingleProcessTest.ECHO_PORT;
    private final static int CLIENT_PORT = ServiceSharingSingleProcessTest.CLIENT_PORT;
    private final static String LOCALHOST = ServiceSharingSingleProcessTest.LOCALHOST;

    private static Logger logger = Logger.getLogger(ServiceSharingChannelReuseTest.class.getName());

    @BeforeClass
    public static void setUpClass() throws Exception {
        TwoProcessTestBase.startSelenium = false;
        TwoProcessTestBase.setUpClass();
    }

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingSingleProcessTest.logger);
        logFinest(EchoServer.logger);
        // logFinest(ReadController.logger);

        // logFinest(ServiceSharingManager.logger);
        // logFinest(ServiceConnection.logger);
        // logFinest(SearchManager.logger);
    }

    @Test
    public void testChannelResuse() throws InterruptedException {
        /*
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start a remote copy of oneswarm with this one as a friend
         * * Register one server service on the remote instance
         * * Register one client service in local instance
         * * Set up a couple concurrent connections between client & server.
         */

        try {
            tellRemoteToShareService("echo", SEARCH_KEY, LOCALHOST, ECHO_PORT);
            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);
            Thread.sleep(5000);
            doConcurrentEchoTest();
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testServiceSharing()");
        }
    }

    void doConcurrentEchoTest() throws UnknownHostException, IOException,
            UnsupportedEncodingException, InterruptedException {
        // Echo server.
        EchoServer echoServer = new EchoServer(ECHO_PORT);
        echoServer.startDeamonThread(true);

        Socket s1 = new Socket(LOCALHOST, CLIENT_PORT);
        // Talk.
        writeReadVerify("t".getBytes("UTF-8"), s1);

        Socket s2 = new Socket(LOCALHOST, CLIENT_PORT);
        writeReadVerify("t".getBytes("UTF-8"), s2);

        // test a couple of bytes
        writeReadVerify("hello".getBytes("UTF-8"), s1);

        s2.close();

        // Make sure the channel doesn't get killed by closing connections.
        writeReadVerify("goodbye".getBytes("UTF-8"), s1);
    }

    private void writeReadVerify(byte[] out, Socket s) throws IOException {
        long startTime = System.currentTimeMillis();
        InputStream inStream = s.getInputStream();
        OutputStream outStream = s.getOutputStream();

        outStream.write(out);
        logger.finest("\n#######################\nwrote: " + out.length);
        byte[] in = new byte[out.length];
        int total = 0;
        while (total < out.length) {
            int value = inStream.read(in, total, out.length - total);
            if (value == -1) {
                break;
            }
            total += value;
            logger.finest("\n#######################\nread: " + total);
        }
        logger.info("time=" + (System.currentTimeMillis() - startTime));
        assertEquals(in, out);
    }

    private void tellRemoteToShareService(String name, long searchKey, String address, int port) {
        localOneSwarm.getCoordinator().addCommand(
                "inject edu.washington.cs.oneswarm.test.integration.ServiceSharingExperiment");
        localOneSwarm.getCoordinator().addCommand(
                "share_service " + name + " " + searchKey + " " + address + " " + port);
    }

    /** Boilerplate code for running as executable. */
    public static void main(String[] args) throws Exception {
        TestUtils.swtCompatibleTestRunner(ServiceSharingChannelReuseTest.class);
    }
}
