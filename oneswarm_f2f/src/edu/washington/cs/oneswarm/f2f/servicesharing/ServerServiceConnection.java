package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.LowLatencyMessageWriter;

public class ServerServiceConnection extends AbstractServiceConnection {
    public static final Logger logger = Logger.getLogger(ServerServiceConnection.class.getName());

    private final SharedService serverService;
    protected NetworkConnection serverConnection;
    private boolean serviceConnected = false;

    public ServerServiceConnection(SharedService service) {
        super();
        logger.info("Server Service Connection created.");
        this.serverService = service;
    }
    
    @Override
    public void addChannel(FriendConnection channel,
            OSF2FHashSearch search, OSF2FHashSearchResp response) {
    	logger.info("Server Service Connection channel added.");
        this.connections.add(new ServiceChannelEndpoint(
                this, channel, search, response, false));
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " server service: " + serverService.toString();
    }

    @Override
    public boolean isOutgoing() {
        return false;
    }

    @Override
    public void start() {
        if (!serverService.isEnabled()) {
            logger.fine("Tried to start disabled connection");
            return;
        }

        serverConnection = serverService.createConnection();
        serverService.connect(serverConnection, new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(ServerServiceConnection.this.getDescription() + " connection failure");
                ServerServiceConnection.this.close("Exception during connect");
            }

            @Override
            public void connectStarted() {
                logger.fine(ServerServiceConnection.this.getDescription() + " connect started");
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
                logger.fine(ServerServiceConnection.this.getDescription() + " connected");
                serverConnection.startMessageProcessing();
                serverConnection.enableEnhancedMessageProcessing(true);
                serverConnection.getOutgoingMessageQueue().registerQueueListener(
                        new LowLatencyMessageWriter(serverConnection));

                serverConnection.getIncomingMessageQueue().registerQueueListener(
                        new ServerIncomingMessageListener());
                serviceConnected = true;
                writeMessageToServiceConnection();
            }

            @Override
            public void exceptionThrown(Throwable error) {
                logger.fine("got exception in server service connection: "
                        + error.getClass().getName() + "::" + error.getMessage());
                error.printStackTrace();
                ServerServiceConnection.this.close("Exception in connection to server");
            }

            @Override
            public String getDescription() {
                return ServerServiceConnection.this.getDescription() + " connect listener";
            }
        });
    }

    @Override
    public boolean isStarted() {
        return serviceConnected;
    }
    
    public void cleanup() {
        if (serverConnection != null) {
            final NetworkConnection conn = serverConnection;
            serverConnection = null;
            // if (outgoingMessageQueue.getTotalSize() == 0) {
            logger.fine("closing connection: " + getDescription());
            conn.close();
        }
    }

    @Override
    void writeMessageToServiceConnection() {
        if (!serviceConnected) {
            return;
        }
        synchronized (bufferedServiceMessages) {
            while (bufferedServiceMessages[serviceSequenceNumber & (SERVICE_MSG_BUFFER_SIZE - 1)] != null) {
                writeMessageToServerConnection(bufferedServiceMessages[serviceSequenceNumber
                        & (SERVICE_MSG_BUFFER_SIZE - 1)]);
                bufferedServiceMessages[serviceSequenceNumber & (SERVICE_MSG_BUFFER_SIZE - 1)] = null;
                serviceSequenceNumber++;
            }
        }
    }

    protected void writeMessageToServerConnection(DirectByteBuffer directByteBuffer) {
        DataMessage msg = new DataMessage(directByteBuffer);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("writing message to server queue: " + msg.getDescription());
        }
        serverConnection.getOutgoingMessageQueue().addMessage(msg, false);
    }
}
