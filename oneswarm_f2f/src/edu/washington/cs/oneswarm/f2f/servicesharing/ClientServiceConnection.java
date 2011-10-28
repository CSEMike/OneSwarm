package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.logging.Level;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.LowLatencyMessageWriter;

public class ClientServiceConnection extends AbstractServiceConnection {
    final ClientService clientService;
    private final NetworkConnection clientConnection;

    public ClientServiceConnection(ClientService service, NetworkConnection clientConnection) {
        super();
        this.clientService = service;
        this.clientConnection = clientConnection;
    }

    @Override
    public void addChannel(FriendConnection channel,
            OSF2FHashSearch search, OSF2FHashSearchResp response) {
        this.connections.add(new ServiceChannelEndpoint(
                this, channel, search, response, false));
    }

    @Override
    public void start() {
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
                clientConnection.enableEnhancedMessageProcessing(true);
                clientConnection.getOutgoingMessageQueue().registerQueueListener(
                        new LowLatencyMessageWriter(clientConnection));
                synchronized (bufferedServiceMessages) {
                    for (OSF2FChannelDataMsg msg : bufferedServiceMessages) {
                        logger.finest("sending queued message: " + msg.getDescription());
                        writeMessageToClientConnection(msg.getPayload());
                    }
                    bufferedServiceMessages.clear();
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

    @Override
    public boolean isOutgoing() {
        return true;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " client service: " +
                clientService.toString();
    }

    @Override
    public void writeMessageToServiceConnection(OSF2FChannelDataMsg msg) {
        System.out.println("MSG received by client aggregator.");
        writeMessageToClientConnection(msg.getPayload());
    }

    protected void writeMessageToClientConnection(DirectByteBuffer directByteBuffer) {
        DirectByteBuffer deliveryBuffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                directByteBuffer.capacity((byte) 0));
        deliveryBuffer.put((byte) 0, directByteBuffer);
        deliveryBuffer.flip((byte) 0);
        DataMessage msg = new DataMessage(deliveryBuffer);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("writing message to server queue: " + msg.getDescription());
        }
        DirectByteBuffer data = msg.getPayload();
        int pos = data.position((byte) 0);
        System.out.println("MSG delivered with val " + data.get((byte) 0));
        data.position((byte) 0, pos);
        clientConnection.getOutgoingMessageQueue().addMessage(msg, false);
    }
}
