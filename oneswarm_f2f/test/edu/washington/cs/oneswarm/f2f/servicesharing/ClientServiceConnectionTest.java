package edu.washington.cs.oneswarm.f2f.servicesharing;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.ui.UIFunctionsManager;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageEncoder;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.QueueManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.test.integration.ServiceSharingClientTest;
import edu.washington.cs.oneswarm.test.util.MessageStreamDecoderTestImpl;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;
import edu.washington.cs.oneswarm.test.util.TestUtils;

/**
 * Tests ServiceConnections, verifying that data put into its NetworkConnection
 * makes its way to at least one of the connection endpoints.
 * 
 * @author Krysta
 * 
 */
public class ClientServiceConnectionTest extends OneSwarmTestBase {

    private static Logger logger = Logger.getLogger(ServiceSharingClientTest.class.getName());
    private final static Random random = new Random(12345);

    /* The number of friend connections to create. */
    private final static int NUM_FRIENDS = 2;

    /* The various layers of data-handling */
    private static ServiceConnection clientConn;
    private static NetworkConnection netConn;
    private static List<FriendConnection> friends;
    private static MessageStreamDecoderTestImpl decoder;
    

    @Before
    public void setupLogging() {
        logFinest(logger);
        logFinest(ServiceSharingLoopback.logger);
        logFinest(ServiceSharingManager.logger);
        logFinest(ServiceChannelEndpoint.logger);
        logFinest(SearchManager.logger);
    }

    @Test
    public void testClientService() throws Exception {
        /*
         * Verify that data put into the network connection from ClientServiceConnection
         * makes its way to exactly one of its friend channels
         * 
         * Test plan:
         * * Create a ClientServiceConnection with NUM_FRIEND FriendConnections
         * * Have the service connection's network connection retrieve a message
         * * Verify that exactly one friend added the message to its outgoing queue
         */

        try {
            setupConnections();

            // Send a single byte
            sendData(new byte[] {'a'});
            
            // Send random bytes
            byte[] randomData = new byte[100];
            random.nextBytes(randomData);
            sendData(randomData);
            
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.toString());
            fail();
        } finally {
            logger.info("End testClientService()");
        }
    }
    
    // Construct all the objects involved in handling data - NetworkConnection,
    // ClientServiceConnection, FriendConnections, etc.
    private static void setupConnections() {
        decoder = new MessageStreamDecoderTestImpl();
        ProtocolEndpointTCP tcpEndpoint = new ProtocolEndpointTCP(null);
        netConn = NetworkManager.getSingleton().createConnection(
                tcpEndpoint.getConnectionEndpoint(), new OSF2FMessageEncoder(), decoder,
                false, false, null);

        clientConn = new ServiceConnection(true, (short) 0, netConn);
        friends = new ArrayList<FriendConnection>();
        QueueManager qMgr = new QueueManager();
        
        for (int i = 0; i < NUM_FRIENDS; i++) {
            Friend remoteFriend = new Friend("", "Remote " + i, null, false);
            FriendConnection friend = FriendConnection.createStubForTests(qMgr, netConn, remoteFriend);
            friends.add(friend);
            ServiceChannelEndpoint ep = new ServiceChannelEndpoint(friend, new OSF2FHashSearch(
                    (byte) 0, 0, 0), new OSF2FHashSearchResp((byte) 0, 0, 0, 0), true);
            clientConn.addChannel(ep);
        }
    }
    
    
    private static void sendData(byte[] origData) throws IOException {
        // Put some data into the network
        // This is simulated by passing data to our mock decoder and telling
        // the network connection to receive incoming data
        decoder.setData(origData);
        int amt = netConn.getIncomingMessageQueue().receiveFromTransport(10);
        
        // Verify that the network connection received the correct number of
        // bytes (mainly this is a sanity check of our mock decoder)\
        assertEquals(origData.length, amt, "Decoder returned incorrect number of bytes");
        
        // Verify that exactly the original data went to exactly one friend
        int channelsEntered = 0;
        for (FriendConnection friend : friends) {
            OSF2FMessage msg = friend.getLastMessageQueued();
            if (msg == null || msg.getData().length < 2)
                continue;

            DirectByteBuffer finalData = msg.getData()[1];
            if (origData.length == finalData.remaining((byte) 0)) {
                boolean match = true;
                
                for (int i = 0; i < origData.length; i++)
                    match = match && (origData[i] == finalData.get((byte) 0));

                if (match)
                    channelsEntered++;
            }
        }
        
        assertEquals(1, channelsEntered,
            "Data not put into any channel or put into too many channels");
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
        TestUtils.swtCompatibleTestRunner(ClientServiceConnectionTest.class);
    }

}
