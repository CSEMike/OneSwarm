package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.LowLatencyMessageWriter;

public class ClientServiceConnection extends AbstractServiceConnection {
    public static final Logger logger = Logger.getLogger(ClientServiceConnection.class.getName());

    final ClientService clientService;
    private final NetworkConnection clientConnection;
    private boolean clientConnected = false;

    public ClientServiceConnection(ClientService service, NetworkConnection clientConnection) {
        super();
        logger.info("ASC Client service connection created.");
        this.clientService = service;
        this.clientConnection = clientConnection;
    }
    
    /**
     * Used in the ClientServiceConnection unit tests. Constructs a new
     * ClientServiceConnection object and attaches a listener to the
     * NetworkConnection so we can verify that messages are handled correctly
     * without starting the ClientServiceConnection
     * 
     * @return a new ClientServiceConnection that has not been started but has
     * a listener registered with clientConn's incoming message queue
     */
    public static ClientServiceConnection getConnectionForTest(NetworkConnection clientConn) {
        ClientServiceConnection conn = new ClientServiceConnection(null, clientConn);
        clientConn.getIncomingMessageQueue().registerQueueListener(
                conn.new ServerIncomingMessageListener());
        return conn;
    }

    @Override
    public boolean addChannel(FriendConnection channel,
            OSF2FHashSearch search, OSF2FHashSearchResp response) {
        if (this.connections.size() >= MAX_CHANNELS) {
            return false;
        }
        ServiceChannelEndpoint chan = new ServiceChannelEndpoint(this, channel, search, response,
                true);
        this.connections.add(chan);
        this.mmt.addChannel(chan);
        logger.info("ASC Client channel added. (now " + this.connections.size() + ")");
        return true;
    }

    @Override
    public void start() {
    	logger.info("Client service connection started.");
        clientConnection.connect(false, new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(ClientServiceConnection.this.getDescription()
                         + " connection failure (This should never happen, we are already connected!!)");
                ClientServiceConnection.this.close("Exception during connect");
            }

            @Override
            public void connectStarted() {
                logger.fine(ClientServiceConnection.this.getDescription() + " connect started");
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
                logger.fine(ClientServiceConnection.this.getDescription() + " connected");
                clientConnection.getIncomingMessageQueue().registerQueueListener(
                        new ServerIncomingMessageListener());
                clientConnection.startMessageProcessing();
                NetworkManager.getSingleton().upgradeTransferProcessing(clientConnection,
                        new ServiceRateHandler(ClientServiceConnection.this));
                clientConnection.getOutgoingMessageQueue().registerQueueListener(
                        new LowLatencyMessageWriter(clientConnection));
                clientConnected = true;
                writeMessageToServiceConnection();
            }

            @Override
            public void exceptionThrown(Throwable error) {
                ClientServiceConnection.this.close("Exception during connect");
            }

            @Override
            public String getDescription() {
                return ClientServiceConnection.this.getDescription() + " connect listener";
            }
        });
    }

    @Override
    public boolean isStarted() {
        return clientConnected;
    }

    @Override
    public boolean isOutgoing() {
        return true;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " client service: " +
                clientService.toString();
    }

    protected void writeMessageToClientConnection(DirectByteBuffer directByteBuffer) {
        /*
         * DirectByteBuffer deliveryBuffer =
         * DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
         * directByteBuffer.capacity((byte) 0));
         * deliveryBuffer.put((byte) 0, directByteBuffer);
         * deliveryBuffer.flip((byte) 0);
         */
        DataMessage msg = new DataMessage(directByteBuffer); // deliveryBuffer);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("writing message to server queue: " + msg.getDescription());
        }
        clientConnection.getOutgoingMessageQueue().addMessage(msg, false);
    }

    @Override
    void writeMessageToServiceConnection() {
        if (!clientConnected) {
            return;
        }

        synchronized (bufferedServiceMessages) {
            while (bufferedServiceMessages[serviceSequenceNumber & (SERVICE_MSG_BUFFER_SIZE - 1)] != null) {
                writeMessageToClientConnection(bufferedServiceMessages[serviceSequenceNumber
                        & (SERVICE_MSG_BUFFER_SIZE - 1)]);
                bufferedServiceMessages[serviceSequenceNumber & (SERVICE_MSG_BUFFER_SIZE - 1)] = null;
                serviceSequenceNumber++;
            }
        }
    }
}
