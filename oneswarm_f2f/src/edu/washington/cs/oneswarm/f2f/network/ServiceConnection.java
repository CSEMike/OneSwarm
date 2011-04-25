package edu.washington.cs.oneswarm.f2f.network;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager.SharedService;

public class ServiceConnection extends OverlayEndpoint {
    /**
     ** High level **
     * 
     * Searcher: Register service, enter local port and searchkey, Open local
     * port, On incoming connection: Search
     * 
     * 
     * Service host: Search hit->Search reply->contact server On server timeout
     * or any other error: send channel reset
     * 
     * Searcher: On search reply: send any incoming data to the channel
     * 
     */

    /**
     ** Details **
     * 
     * For service host:
     * 
     * Create: Create new network connection, register the network connection to
     * handle rate limited reads/writes.
     * 
     * Server->Overlay: On incoming message: move the payload into a new overlay
     * message with the proper channel id and queue on the friend connection.
     * The new message now owns the payload and is responsible for destroying
     * it.
     * 
     * Overlay->Server: put the message in the outgoing queue on the server
     * connection.
     */

    private final static Logger logger = Logger.getLogger(ServiceConnection.class.getName());
    // all operations on this object must be in a synchronized block
    private final LinkedList<OSF2FChannelDataMsg> bufferedMessages;
    private NetworkConnection serverConnection;

    private final SharedService service;
    private final boolean serverSide;

    public ServiceConnection(SharedService service, FriendConnection connection, int channelId,
            int pathID, boolean serverSide) {
        super(connection, channelId, pathID, 0);
        this.service = service;
        this.bufferedMessages = new LinkedList<OSF2FChannelDataMsg>();
        this.serverSide = true;
    }

    @Override
    public void cleanup() {
        serverConnection.close();
    }

    @Override
    protected void destroyBufferedMessages() {
        synchronized (bufferedMessages) {
            while (bufferedMessages.size() > 0) {
                bufferedMessages.removeFirst().destroy();
            }
        }
    };

    @Override
    public String getDescription() {
        return super.getDescription() + " " + service.toString() + " serverside=" + serverSide;
    }

    @Override
    protected void handleDelayedOverlayMessage(OSF2FChannelDataMsg msg) {
        if (closed) {
            return;
        }
        if (!started) {
            start();
        }
        if (!serverConnection.isConnected()) {
            synchronized (bufferedMessages) {
                bufferedMessages.add(msg);
            }
            return;
        }
        writeMessageToServerConnection(msg.getData());
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
        if (!service.isEnabled()) {
            logger.fine("Tried to start disabled connection");
            return;
        }
        serverConnection = service.createConnection(new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(ServiceConnection.this.getDescription() + " connection failure");
                ServiceConnection.this.close("Exception during connect");
            }

            @Override
            public void connectStarted() {
                logger.fine(ServiceConnection.this.getDescription() + " connect started");
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
                logger.fine(ServiceConnection.this.getDescription() + " connected");
                serverConnection.getIncomingMessageQueue().registerQueueListener(
                        new ServerIncomingMessageListener());
                synchronized (bufferedMessages) {
                    for (OSF2FChannelDataMsg msg : bufferedMessages) {
                        logger.finest("sending queued message: " + msg.getDescription());
                        writeMessageToServerConnection(msg.getData());
                    }
                }
            }

            @Override
            public void exceptionThrown(Throwable error) {
                ServiceConnection.this.close("Exception during connect");
            }

            @Override
            public String getDescription() {
                return ServiceConnection.this.getDescription() + " connect listener";
            }
        });
        started = true;
    }

    private void writeMessageToServerConnection(DirectByteBuffer[] data) {
        for (DirectByteBuffer directByteBuffer : data) {
            DataMessage msg = new DataMessage(directByteBuffer);
            logger.finest("writing message to server queue: " + msg.getDescription());
            serverConnection.getOutgoingMessageQueue().addMessage(msg, false);
        }
    }

    private class ServerIncomingMessageListener implements MessageQueueListener {

        @Override
        public void dataBytesReceived(int byte_count) {
        }

        @Override
        public boolean messageReceived(Message message) {
            logger.finest("Message from server: " + message.getDescription());
            if (!(message instanceof DataMessage)) {
                String msg = "got wrong message type from server: ";
                logger.warning(msg + message.getDescription());
                ServiceConnection.this.close(msg);
                return false;
            }
            DataMessage dataMessage = (DataMessage) message;
            ServiceConnection.this.writeMessageToFriendConnection(dataMessage.transferPayload());
            return true;
        }

        @Override
        public void protocolBytesReceived(int byte_count) {
        }
    }
}
