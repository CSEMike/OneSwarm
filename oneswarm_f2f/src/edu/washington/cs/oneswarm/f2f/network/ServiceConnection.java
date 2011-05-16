package edu.washington.cs.oneswarm.f2f.network;

import java.util.LinkedList;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage;

public abstract class ServiceConnection extends OverlayEndpoint {
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
    protected final LinkedList<OSF2FChannelDataMsg> bufferedMessages;
    protected NetworkConnection serverConnection;

    public ServiceConnection(FriendConnection connection, int channelId, int pathID) {
        super(connection, channelId, pathID, 0);
        this.bufferedMessages = new LinkedList<OSF2FChannelDataMsg>();
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

    protected void writeMessageToServerConnection(DirectByteBuffer[] data) {
        for (DirectByteBuffer directByteBuffer : data) {
            DataMessage msg = new DataMessage(directByteBuffer);
            logger.finest("writing message to server queue: " + msg.getDescription());
            serverConnection.getOutgoingMessageQueue().addMessage(msg, false);
        }
    }

    protected class ServerIncomingMessageListener implements MessageQueueListener {

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
