package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.EndpointInterface;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public abstract class AbstractServiceConnection implements EndpointInterface {
    public static final Logger logger = Logger.getLogger(AbstractServiceConnection.class.getName());

    static final String SERVICE_PRIORITY_KEY = "SERVICE_CLIENT_MULTIPLEX_QUEUE";
    static final int SERVICE_MSG_BUFFER_SIZE = 2^6;
    protected int serviceSequenceNumber;
    protected final DirectByteBuffer[] bufferedServiceMessages = new DirectByteBuffer[SERVICE_MSG_BUFFER_SIZE];
    protected final LinkedList<DirectByteBuffer> bufferedNetworkMessages;
    private final MessageStreamMultiplexer mmt;

    @Override
    public abstract boolean isOutgoing();

    public abstract void addChannel(FriendConnection channel, OSF2FHashSearch search, OSF2FHashSearchResp response);

    protected final PriorityQueue<ServiceChannelEndpoint> connections;

    public AbstractServiceConnection() {
        String channelScheme = COConfigurationManager.getStringParameter(SERVICE_PRIORITY_KEY);
        if (channelScheme == "roundrobin") {
            this.connections = new PriorityQueue<ServiceChannelEndpoint>(1,
                    new FairChannelComparator());
        } else if (channelScheme == "random") {
            this.connections = new PriorityQueue<ServiceChannelEndpoint>(1,
                    new RandomChannelComparator());
        } else {
            this.connections = new PriorityQueue<ServiceChannelEndpoint>(1,
                    new WeightedChannelComparator());
        }
        this.serviceSequenceNumber = 0;
 		this.bufferedNetworkMessages = new LinkedList<DirectByteBuffer>();
        this.mmt = new MessageStreamMultiplexer();
    }

    @Override
    public String getDescription() {
        String connectionInfo = "";
        if (this.connections.size() > 0) {
            connectionInfo = " via: " + this.connections.peek().getRemoteFriend().getNick();
        }
        if (this.connections.size() > 1) {
            connectionInfo += " and " + (this.connections.size() - 1) + " others.";
        }
        return NetworkManager.OSF2F_TRANSPORT_PREFIX + connectionInfo;
    }

    @Override
    public void close(String reason) {
        for (ServiceChannelEndpoint conn : this.connections) {
            conn.close(reason);
        }
 
        synchronized (bufferedServiceMessages) {
            for (int i = 0; i < SERVICE_MSG_BUFFER_SIZE; i++) {
                if (bufferedServiceMessages[i] != null) {
                    bufferedServiceMessages[i].returnToPool();
                }
            }
        }
        
        synchronized (bufferedNetworkMessages) {
            bufferedNetworkMessages.clear();
        }
 
        this.connections.clear();
    }

    @Override
    public void closeChannelReset() {
        for (ServiceChannelEndpoint conn : this.connections) {
            conn.closeChannelReset();
        }
        this.connections.clear();
    }

    @Override
    public void closeConnectionClosed(String reason) {
        for (ServiceChannelEndpoint conn : this.connections) {
            conn.closeConnectionClosed(reason);
        }
        this.connections.clear();
    }

    @Override
    public long getAge() {
        long time = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            time = Math.max(time, conn.getAge());
        }
        return time;
    }

    @Override
    public long getArtificialDelay() {
        if (this.connections.size() > 0) {
            return this.connections.peek().getArtificialDelay();
        }
        return 0;
    }

    @Override
    public long getBytesIn() {
        long in = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            in += conn.getBytesIn();
        }
        return in;
    }

    @Override
    public long getBytesOut() {
        long out = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            out += conn.getBytesOut();
        }
        return out;
    }

    @Override
    public int[] getChannelId() {
        int[] channels = new int[this.connections.size()];
        int i = 0;
        for (ServiceChannelEndpoint conn: this.connections) {
            channels[i++] = conn.getChannelId()[0];
        }
        return channels;
    }

    @Override
    public int[] getPathID() {
        int[] paths = new int[this.connections.size()];
        int i = 0;
        for (ServiceChannelEndpoint conn: this.connections) {
            paths[i++] = conn.getPathID()[0];
        }
        return paths;
    }

    @Override
    public int getDownloadRate() {
        int rate = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            rate += conn.getDownloadRate();
        }
        return rate;
    }

    @Override
    public int getUploadRate() {
        int rate = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            rate += conn.getUploadRate();
        }
        return rate;
    }

    @Override
    public long getLastMsgTime() {
        long time = 0;
        for (ServiceChannelEndpoint conn : this.connections) {
            time = Math.max(time, conn.getLastMsgTime());
        }
        return time;
    }

    @Override
    public Friend getRemoteFriend() {
        for (ServiceChannelEndpoint conn : this.connections) {
            if (conn.isStarted())
                return conn.getRemoteFriend();
        }
        return null;
    }

    @Override
    public String getRemoteIP() {
        for (ServiceChannelEndpoint conn : this.connections) {
            if (conn.isStarted())
                return conn.getRemoteIP();
        }
        return null;
    }

    @Override
    public void incomingOverlayMsg(OSF2FChannelDataMsg msg) {
        int channelId = msg.getChannelId();
        for (ServiceChannelEndpoint conn : this.connections) {
            if (conn.getChannelId()[0] == channelId) {
                conn.incomingOverlayMsg(msg);
            }
        }
    }

    @Override
    public boolean isLANLocal() {
        for (ServiceChannelEndpoint conn : this.connections) {
            if (conn.isLANLocal())
                return true;
        }
        return false;
    }

    @Override
    public boolean isTimedOut() {
        for (ServiceChannelEndpoint conn : this.connections) {
            if (!conn.isTimedOut())
                return false;
        }
        return true;
    }
    
    void writeMessageToServiceBuffer(OSF2FServiceDataMsg message) {
        synchronized(bufferedServiceMessages) {
            if (message.getSequenceNumber() >= serviceSequenceNumber + SERVICE_MSG_BUFFER_SIZE) {
                // Throw out to prevent buffer overflow.
                logger.warning("Incoming service message dropped, exceeded message buffer.");
                return;
            } else {
                bufferedServiceMessages[message.getSequenceNumber() & (SERVICE_MSG_BUFFER_SIZE - 1)] = message
                        .transferPayload();
            }
        }
        writeMessageToServiceConnection();
    }

    abstract void writeMessageToServiceConnection();

    void removeChannel(ServiceChannelEndpoint channel) {
        // TODO(willscott): Get retransmission working.
        if (mmt.hasOutstanding(channel)) {
            synchronized (bufferedNetworkMessages) {
                bufferedNetworkMessages.addAll(mmt.getOutstanding(channel));
            }
        }
        mmt.removeChannel(channel);
        this.connections.remove(channel);
    }
    
    void routeMessageToChannel(DirectByteBuffer msg) {
    	logger.info("ASC routing service message to a channel.");
        ServiceChannelEndpoint channel = this.connections.peek();
        if (!channel.isStarted()) {
            logger.fine("Unstarted channel prioritized, msg buffered");
            synchronized(bufferedNetworkMessages) {
                bufferedNetworkMessages.add(msg);
            }
        } else {
            channel.writeMessage(mmt.nextMsg(channel), msg);
        }
    }
    
    protected class ServerIncomingMessageListener implements MessageQueueListener {

        @Override
        public void dataBytesReceived(int byte_count) {
        }

        @Override
        public boolean messageReceived(Message message) {
        	logger.info("ASC Service message recieved.");

        	if (!(message instanceof DataMessage)) {
                String msg = "got wrong message type from server: ";
                logger.warning(msg + message.getDescription());
                AbstractServiceConnection.this.close(msg);
                return false;
            }
            DataMessage dataMessage = (DataMessage) message;
            AbstractServiceConnection.this.routeMessageToChannel(dataMessage.transferPayload());
            return true;
        }

        @Override
        public void protocolBytesReceived(int byte_count) {
        }
    }
    
    private class WeightedChannelComparator implements Comparator<ServiceChannelEndpoint> {
        static final int CHANNEL_BUFFER = 1024;

        @Override
        public int compare(ServiceChannelEndpoint first, ServiceChannelEndpoint second) {
            boolean firstReady = first.isStarted() && first.getOutstanding() < CHANNEL_BUFFER;
            boolean secondReady = second.isStarted() && second.getOutstanding() < CHANNEL_BUFFER;
            if (firstReady && secondReady) {
                // If both available, go with the faster one.
                return second.getUploadRate() - first.getUploadRate();
            } else if (firstReady) {
                return -1;
            } else if (secondReady) {
                return 1;
            } else {
                // If neither available, prioritize closed channels so they get started.
                return first.isStarted() ? (second.isStarted() ? 0 : 1): -1;
            }
        }
    }

    private class RandomChannelComparator implements Comparator<ServiceChannelEndpoint> {
        @Override
        public int compare(ServiceChannelEndpoint first, ServiceChannelEndpoint second) {
            return Math.random() < 0.5 ? -1 : 1;
        }
    }

    private class FairChannelComparator implements Comparator<ServiceChannelEndpoint> {
        @Override
        public int compare(ServiceChannelEndpoint first, ServiceChannelEndpoint second) {
            return (int) (first.getBytesOut() - second.getBytesOut());
        }
    }

    public void channelReady(ServiceChannelEndpoint channel) {
        if (!this.connections.contains(channel)) {
            System.out.println("bad channel.");
            logger.warning("Unregistered channel attempted to provide service transit.");
            return;
        }
        this.connections.remove(channel);
        this.connections.add(channel);

        // At least one message can be written, since a channel just indicated readyness.
        synchronized(bufferedNetworkMessages) {
            if (bufferedNetworkMessages.size() > 0) {
                System.out.println("Buffered message written to ready channel.");
                ServiceChannelEndpoint ready = this.connections.peek();
                ready.writeMessage(mmt.nextMsg(ready), bufferedNetworkMessages.pop());
            }
        }
    }
}