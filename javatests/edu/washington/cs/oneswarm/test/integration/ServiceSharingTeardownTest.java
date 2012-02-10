package edu.washington.cs.oneswarm.test.integration;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.AbstractServiceConnection;
import edu.washington.cs.oneswarm.f2f.servicesharing.EchoServer;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceChannelEndpoint;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingLoopback;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.test.util.LocalProcessesTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

public class ServiceSharingTeardownTest extends LocalProcessesTestBase {

    static final int SEARCH_KEY = 12345;
    static Logger logger = Logger.getLogger(ServiceSharingTeardownTest.class.getName());
    final static int SERVER_PORT = 26012;
    final static int CLIENT_PORT = 26013;
    final static String LOCALHOST = "127.0.0.1";

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
        logFinest(EchoServer.logger);
        logFinest(ServiceSharingLoopback.logger);
        logFinest(ServiceSharingManager.logger);
        logFinest(AbstractServiceConnection.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testLocalServiceEcho() throws Exception {
        /*
         * Test plan:
         * * Start OneSwarm (done in setupClass())
         * * Start server port
         * * Register one server service
         * * Register one client service
         * * Connect to client service port
         * * Write bytes to client service port
         * * Verify that the correct bytes go to server.
         * * Close connection on the server.
         * * Verify the client's connection is closed.
         */
        try {
            // Register the server service
            ServiceSharingManager.getInstance().registerSharedService(SEARCH_KEY, "echo",
                    new InetSocketAddress(LOCALHOST, SERVER_PORT));

            // Register the client service
            ServiceSharingManager.getInstance().registerClientService("echoclient", CLIENT_PORT,
                    SEARCH_KEY);
            doTest();

        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testLocalServiceEcho()");
        }
    }

    /**
     * Helper method for testing sharing.
     * 
     * @throws UnknownHostException
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws InterruptedException
     */
    static void doTest() throws UnknownHostException, IOException,
            UnsupportedEncodingException, InterruptedException {
        // Server.
        ServerSocket server = new ServerSocket(SERVER_PORT);

        Socket client = new Socket(LOCALHOST, CLIENT_PORT);

        // test transfer
        Socket serversock = writeReadVerify("t".getBytes("UTF-8"), client, server);
        serversock.close();
        Assert.assertEquals(-1, client.getInputStream().read());
        server.close();
    }

    private static Socket writeReadVerify(byte[] out, Socket client, ServerSocket server)
            throws IOException {
        OutputStream outStream = client.getOutputStream();
        outStream.write(out);
        Socket serverclient = server.accept();
        InputStream inStream = serverclient.getInputStream();

        byte[] in = new byte[out.length];
        int total = 0;
        while (total < out.length) {
            int value = inStream.read(in, total, out.length - total);
            if (value == -1) {
                break;
            }
            total += value;
        }
        assertEquals(in, out);
        return serverclient;
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
        TestUtils.swtCompatibleTestRunner(ServiceSharingTeardownTest.class);
    }
}
