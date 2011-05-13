package edu.washington.cs.oneswarm.test.integration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.aelitis.azureus.ui.UIFunctionsManager;

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
        // Test plan:
        // * Start OneSwarm (done in setupClass)
        // * Start echo server
        // * Register one server service
        // * Register one client service
        // * Connect to client service port
        // * Write bytes to client service port
        // * verify that the correct bytes are echoed back

        // Echo server.
        Thread echoServerThread = new Thread(new EchoServer(ECHO_PORT));
        echoServerThread.setName("Echo server");
        echoServerThread.setDaemon(true);
        echoServerThread.start();

        // Register the server service
        ServiceSharingManager.getInstance().registerServerService(SEARCH_KEY,
                new SharedService(new InetSocketAddress(LOCALHOST, ECHO_PORT), "echo"));

        // Register the client service
        ServiceSharingManager.getInstance().createClientService("echoclient", CLIENT_PORT,
                SEARCH_KEY);

        Socket s = new Socket(LOCALHOST, CLIENT_PORT);
        s.getOutputStream().write("hello".getBytes("UTF-8"));
        Thread.sleep(10 * 1000);
        try {

        } catch (Exception e) {
            logger.severe(e.toString());
            e.printStackTrace();
            Assert.fail();
        } finally {
            logger.info("End testLocalServiceEcho()");
        }
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
