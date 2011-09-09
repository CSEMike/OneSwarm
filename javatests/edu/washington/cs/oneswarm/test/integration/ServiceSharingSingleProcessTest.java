package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
import edu.washington.cs.oneswarm.f2f.servicesharing.EchoServer;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceConnection;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingLoopback;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.planetlab.ping.ExperimentalSearchManager;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

public class ServiceSharingSingleProcessTest extends OneSwarmTestBase {

    static final int SEARCH_KEY = 12345;
    static Logger logger = Logger.getLogger(ServiceSharingSingleProcessTest.class.getName());
    final static int ECHO_PORT = 26012;
    final static int CLIENT_PORT = 26013;
    final static String LOCALHOST = "127.0.0.1";
    private final static Random random = new Random(12345);

    @BeforeClass
    public static void setupClass() {
        TestUtils.awaitJVMOneSwarmStart();
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
        logFinest(EchoServer.logger);
        logFinest(ServiceSharingLoopback.logger);
        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceConnection.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testLocalServiceEcho() throws Exception {
        /*
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start echo server
         * * Register one server service
         * * Register one client service
         * * Connect to client service port
         * * Write bytes to client service port
         * * Verify that the correct bytes are echoed back (despite going
         * through all the azureus network layers).
         */
        try {
            // Register the server service
            ServiceSharingManager.getInstance().registerSharedService(SEARCH_KEY, "echo",
                    new InetSocketAddress(LOCALHOST, ECHO_PORT));

            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);
            doEchoTest();

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testLocalServiceEcho()");
        }
    }

    /**
     * Helper method for testing sharing of an echo service.
     * 
     * @throws UnknownHostException
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    static void doEchoTest() throws UnknownHostException, IOException,
            UnsupportedEncodingException, InterruptedException {
        // Echo server.
        EchoServer echoServer = new EchoServer(ECHO_PORT);
        echoServer.startDeamonThread(true);

        Socket s = new Socket(LOCALHOST, CLIENT_PORT);

        // add the traceroute magic number
        writeReadVerify(ByteManip.itob(ExperimentalSearchManager.MAGIC_NUMBER), s);
        // test 1 byte
        writeReadVerify("t".getBytes("UTF-8"), s);

        // test a couple of bytes
        writeReadVerify("hellš".getBytes("UTF-8"), s);

        // test a maximumSizePacket
        testRandom(s, DataMessage.MAX_PAYLOAD_SIZE);

        // test a maximumSizePacket +1
        // TODO testRandom(s, DataMessage.MAX_PAYLOAD_SIZE + 1);

        // test a 2*maximumSizePacket +1
        // TODO testRandom(s, 2 * DataMessage.MAX_PAYLOAD_SIZE + 1);

        // test a 2*maximumSizePacket +1
        // TODO testRandom(s, 1024 * 1024);
    }

    private static void testRandom(Socket s, int numBytes) throws IOException {
        byte[] randomData = new byte[numBytes];
        random.nextBytes(randomData);
        writeReadVerify(randomData, s);
    }

    private static void writeReadVerify(byte[] out, Socket s) throws IOException {
        long startTime = System.currentTimeMillis();
        InputStream inStream = s.getInputStream();
        OutputStream outStream = s.getOutputStream();

        outStream.write(out);
        logger.finest("\n#######################\nwrote: " + out.length);
        byte[] in = new byte[out.length];
        int total = 0;
        while (total < out.length) {
            total += inStream.read(in, total, out.length - total);
            logger.finest("\n#######################\nread: " + total);
        }
        logger.info("time=" + (System.currentTimeMillis() - startTime));
        assertEquals(in, out);
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
        TestUtils.swtCompatibleTestRunner(ServiceSharingSingleProcessTest.class);
    }
}
