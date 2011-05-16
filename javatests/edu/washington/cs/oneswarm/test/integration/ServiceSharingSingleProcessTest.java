package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
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

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager.SharedService;
import edu.washington.cs.oneswarm.test.util.EchoServer;
import edu.washington.cs.oneswarm.test.util.TestUtils;

public class ServiceSharingSingleProcessTest {

    private static final int SEARCH_KEY = 12345;
    private static Logger logger = Logger.getLogger(CommunityServerTest.class.getName());
    private final static int ECHO_PORT = 26012;
    private final static int CLIENT_PORT = 26013;
    private final static String LOCALHOST = "127.0.0.1";
    private final Random random = new Random(12345);

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
            // Echo server.
            EchoServer echoServer = new EchoServer(ECHO_PORT);
            echoServer.startDeamonThread(true);
            // Register the server service
            ServiceSharingManager.getInstance().registerServerService(SEARCH_KEY,
                    new SharedService(new InetSocketAddress(LOCALHOST, ECHO_PORT), "echo"));

            // Register the client service
            ServiceSharingManager.getInstance().createClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);
            Socket s = new Socket(LOCALHOST, CLIENT_PORT);

            // test 1 byte
            writeReadVerify("t".getBytes("UTF-8"), s);

            // test a couple of bytes
            writeReadVerify("hellš".getBytes("UTF-8"), s);

            // test a maximumSizePacket
            testRandom(s, DataMessage.MAX_PAYLOAD_SIZE);

            // test a maximumSizePacket +1
            testRandom(s, DataMessage.MAX_PAYLOAD_SIZE + 1);

            // test a 2*maximumSizePacket +1
            testRandom(s, 2 * DataMessage.MAX_PAYLOAD_SIZE + 1);

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testLocalServiceEcho()");
        }
    }

    private void testRandom(Socket s, int numBytes) throws IOException {
        byte[] randomData = new byte[numBytes];
        random.nextBytes(randomData);
        writeReadVerify(randomData, s);
    }

    private void writeReadVerify(byte[] out, Socket s) throws IOException {
        InputStream inStream = s.getInputStream();
        OutputStream outStream = s.getOutputStream();

        outStream.write(out);
        System.out.println("wrote: " + out.length);
        byte[] in = new byte[out.length];
        int total = 0;
        while (total < out.length) {
            total += inStream.read(in, total, out.length - total);
            System.out.println("read: " + total);
        }
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
