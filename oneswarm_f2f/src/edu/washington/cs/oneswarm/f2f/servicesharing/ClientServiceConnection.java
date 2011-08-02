package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.LowLatencyMessageWriter;

public class ClientServiceConnection extends ServiceConnection {

    private final ClientService clientService;

    public ClientServiceConnection(ClientService service, NetworkConnection incomingConnetion,
            FriendConnection connection, int channelId, int pathID) {
        super(connection, channelId, pathID);
        this.clientService = service;
        this.serverConnection = incomingConnetion;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " client service: " + clientService.toString();
    }

    /**
     * Currently we connect to the server when we get the first message data.
     */
    // TODO (isdal): connect to the server on incoming search message and send
    // reply on successful connect?
    @Override
    public void start() {
        logger.fine(getDescription() + " starting");
        if (isStarted()) {
            logger.warning("Tried to start already started service");
            return;
        }
        started = true;
        serverConnection.connect(false, new ConnectionListener() {
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
                serverConnection.getIncomingMessageQueue().registerQueueListener(
                        new ServerIncomingMessageListener());
                serverConnection.startMessageProcessing();
                serverConnection.enableEnhancedMessageProcessing(true);
                serverConnection.getOutgoingMessageQueue().registerQueueListener(
                        new LowLatencyMessageWriter(serverConnection));
                synchronized (bufferedMessages) {
                    for (OSF2FChannelDataMsg msg : bufferedMessages) {
                        logger.finest("sending queued message: " + msg.getDescription());
                        writeMessageToServerConnection(msg.getPayload());
                    }
                    bufferedMessages.clear();
                }
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

}
