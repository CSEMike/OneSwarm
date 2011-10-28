package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.EndpointInterface;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.OverlayEndpoint;

public class ServiceChannelEndpoint extends OverlayEndpoint {
    public final static Logger logger = Logger.getLogger(ServiceChannelEndpoint.class.getName());
    protected AbstractServiceConnection serviceAggregator;

    public ServiceChannelEndpoint(AbstractServiceConnection aggregator,
            FriendConnection connection, OSF2FHashSearch search,
            OSF2FHashSearchResp response, boolean outgoing) {
        super(connection, response.getPathID(), 0, search, response, outgoing);
        this.serviceAggregator = aggregator;
        this.started = true;
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub
    }

    @Override
    protected void destroyBufferedMessages() {
        // No buffered messages to destroy.
    }

    public void cleanup() {
        serviceAggregator.removeChannel(this);
    };

    protected void handleDelayedOverlayMessage(OSF2FChannelDataMsg msg) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("incoming message: " + msg.getDescription());
        }

        if (closed) {
            System.out.println("MSG died: closed");
            return;
        }
        if (!started) {
            start();
        }
        // We need to create a new message here and transfer the payload over so
        // the buffer won't be returned while the packet is in the queue.
        OSF2FChannelDataMsg newMessage = new OSF2FChannelDataMsg(msg.getVersion(),
                msg.getChannelId(), msg.transferPayload());
        writeMessageToAggregateConnection(newMessage.getPayload());
    }

    protected void writeMessageToAggregateConnection(DirectByteBuffer directByteBuffer) {
        DataMessage msg = new DataMessage(directByteBuffer);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("writing message to server queue: " + msg.getDescription());
        }
        serviceAggregator.writeMessageToServiceConnection(new OSF2FChannelDataMsg(
                msg.getVersion(),
                0, /* channelID */
                directByteBuffer));
    }

    public void writeMessage(DirectByteBuffer msg) {
        this.writeMessageToFriendConnection(msg);
    }
}
