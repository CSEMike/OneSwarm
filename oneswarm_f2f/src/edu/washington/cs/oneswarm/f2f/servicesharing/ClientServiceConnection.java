package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.LowLatencyMessageWriter;

public class ClientServiceConnection extends AbstractServiceConnection {
    final ClientService clientService;
    public ClientServiceConnection(ClientService service) {
        super();
        this.clientService = service;
    }

    @Override
    public void addChannel(NetworkConnection incomingConnection, FriendConnection channel,
            OSF2FHashSearch search, OSF2FHashSearchResp response) {
        final NetworkConnection serviceNetworkConnection = incomingConnection;
        this.connections.add(new ServiceConnection(channel, search, response, true) {
            @Override
            public void start() {
                serverConnection = serviceNetworkConnection;
                final ServiceConnection self = this;
                serverConnection.connect(false, new ConnectionListener() {
                    @Override
                    public void connectFailure(Throwable failure_msg) {
                        logger.fine(ClientServiceConnection.this.getDescription()
                                + " connection failure (This should never happen, we are already connected!!)");
                        self.close("Exception during connect");
                        ClientServiceConnection.this.connections.remove(self);
                    }

                    @Override
                    public void connectStarted() {
                        logger.fine(self.getDescription() + " connect started");
                    }

                    @Override
                    public void connectSuccess(ByteBuffer remaining_initial_data) {
                        logger.fine(self.getDescription() + " connected");
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
                        self.close("Exception during connect");
                        ClientServiceConnection.this.connections.remove(self);
                    }

                    @Override
                    public String getDescription() {
                        return self.getDescription() + " connect listener";
                    }
                });
            }
        });
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
}
