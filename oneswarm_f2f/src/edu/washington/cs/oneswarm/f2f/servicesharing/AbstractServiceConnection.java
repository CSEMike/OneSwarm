package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.RateHandler;
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
    static final int SERVICE_MSG_BUFFER_SIZE = 1024;

    private static final int CHANNEL_BUFFER = 1024 * 4;
    protected final int MAX_CHANNELS = COConfigurationManager.getIntParameter(
            "SERVICE_CLIENT_channels", 4);
    protected final EnumSet<ServiceFeatures> FEATURES;
    protected int serviceSequenceNumber;
    protected final DirectByteBuffer[] bufferedServiceMessages = new DirectByteBuffer[SERVICE_MSG_BUFFER_SIZE];
    protected final LinkedList<BufferedMessage> bufferedNetworkMessages;
    protected final MessageStreamMultiplexer mmt;

    private class BufferedMessage {
        public BufferedMessage(DirectByteBuffer msg, SequenceNumber msgId) {
            this.messageId = msgId;
            this.message = msg;
        }

        public SequenceNumber messageId;
        public DirectByteBuffer message;
    };

    enum ServiceFeatures {
        UDP, PACKET_DUPLICATION, ADAPTIVE_DUPLICATION
    };

    @Override
    public abstract boolean isOutgoing();

    public abstract boolean addChannel(FriendConnection channel, OSF2FHashSearch search,
            OSF2FHashSearchResp response);

    protected final List<ServiceChannelEndpoint> connections = Collections
            .synchronizedList(new ArrayList<ServiceChannelEndpoint>());

    public AbstractServiceConnection() {
        this.serviceSequenceNumber = 0;
        this.bufferedNetworkMessages = new LinkedList<BufferedMessage>();
        this.mmt = new MessageStreamMultiplexer();

        // Load Configuration.
        ArrayList<ServiceFeatures> features = new ArrayList<ServiceFeatures>();
        if (COConfigurationManager.getBooleanParameter("SERVICE_CLIENT_udp")) {
            features.add(ServiceFeatures.UDP);
        }
        if (COConfigurationManager.getBooleanParameter("SERVICE_CLIENT_duplication")) {
            features.add(ServiceFeatures.PACKET_DUPLICATION);
        }
        if (COConfigurationManager.getBooleanParameter("SERVICE_CLIENT_adaptive")) {
            features.add(ServiceFeatures.ADAPTIVE_DUPLICATION);
        }
        this.FEATURES = EnumSet.copyOf(features);
    }

    @Override
    public String getDescription() {
        String connectionInfo = "";
        if (this.connections.size() > 0) {
            connectionInfo = " via: " + this.connections.get(0).getRemoteFriend().getNick();
        }
        if (this.connections.size() > 1) {
            connectionInfo += " and " + (this.connections.size() - 1) + " others.";
        }
        return NetworkManager.OSF2F_TRANSPORT_PREFIX + connectionInfo;
    }

    @Override
    public void close(String reason) {
        logger.info("Service Connection closed");

        ServiceChannelEndpoint[] channels = this.connections.toArray(new ServiceChannelEndpoint[0]);
        this.connections.clear();
        for (ServiceChannelEndpoint conn : channels) {
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
    }

    @Override
    public void closeChannelReset() {
        ServiceChannelEndpoint[] channels = this.connections.toArray(new ServiceChannelEndpoint[0]);
        for (ServiceChannelEndpoint conn : channels) {
            this.removeChannel(conn);
            conn.closeChannelReset();
        }
    }

    @Override
    public void closeConnectionClosed(FriendConnection f, String reason) {
        ServiceChannelEndpoint[] channels = this.connections.toArray(new ServiceChannelEndpoint[0]);
        for (ServiceChannelEndpoint conn : channels) {
            if (f == null || conn.getRemoteFriend() == f.getRemoteFriend()) {
                logger.info("Service channel " + conn.getChannelId()[0] + " closed with total "
                        + conn.getBytesOut() + "/" + conn.getBytesIn());
                this.removeChannel(conn);
                conn.closeConnectionClosed(f, reason);
            }
        }
    }

    @Override
    public long getAge() {
        long time = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                time = Math.max(time, conn.getAge());
            }
        }
        return time;
    }

    @Override
    public long getArtificialDelay() {
        if (this.connections.size() > 0) {
            return this.connections.get(0).getArtificialDelay();
        }
        return 0;
    }

    @Override
    public long getBytesIn() {
        long in = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                in += conn.getBytesIn();
            }
        }
        return in;
    }

    @Override
    public long getBytesOut() {
        long out = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                out += conn.getBytesOut();
            }
        }
        return out;
    }

    @Override
    public int[] getChannelId() {
        int[] channels;
        synchronized (this.connections) {
            channels = new int[this.connections.size()];
            int i = 0;
            for (ServiceChannelEndpoint conn : this.connections) {
                channels[i++] = conn.getChannelId()[0];
            }
        }
        return channels;
    }

    @Override
    public int[] getPathID() {
        int[] paths;
        synchronized (this.connections) {
            paths = new int[this.connections.size()];
            int i = 0;
            for (ServiceChannelEndpoint conn : this.connections) {
                paths[i++] = conn.getPathID()[0];
            }
        }
        return paths;
    }

    @Override
    public int getDownloadRate() {
        int rate = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                rate += conn.getDownloadRate();
            }
        }
        return rate;
    }

    @Override
    public int getUploadRate() {
        int rate = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                rate += conn.getUploadRate();
            }
        }
        return rate;
    }

    @Override
    public long getLastMsgTime() {
        long time = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                time = Math.max(time, conn.getLastMsgTime());
            }
        }
        return time;
    }

    @Override
    public Friend getRemoteFriend() {
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                if (conn.isStarted())
                    return conn.getRemoteFriend();
            }
        }
        return null;
    }

    @Override
    public String getRemoteIP() {
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                if (conn.isStarted())
                    return conn.getRemoteIP();
            }
        }
        return null;
    }

    @Override
    public void incomingOverlayMsg(OSF2FChannelDataMsg msg) {
        int channelId = msg.getChannelId();
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                if (conn.getChannelId()[0] == channelId) {
                    conn.incomingOverlayMsg(msg);
                }
            }
        }
    }

    @Override
    public boolean isLANLocal() {
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                if (conn.isLANLocal())
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTimedOut() {
        synchronized (this.connections) {
            for (ServiceChannelEndpoint conn : this.connections) {
                if (!conn.isTimedOut())
                    return false;
            }
        }
        return true;
    }

    void writeMessageToServiceBuffer(OSF2FServiceDataMsg message) {
        if (message.isAck()) {
            mmt.onAck(message);
            return;
        }

        synchronized (bufferedServiceMessages) {
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
        if (!this.connections.contains(channel)) {
            return;
        }

        if (mmt.hasOutstanding(channel)) {
            synchronized (bufferedNetworkMessages) {
                for (Map.Entry<SequenceNumber, DirectByteBuffer> e : mmt.getOutstanding(channel)
                        .entrySet()) {
                    bufferedNetworkMessages.add(new BufferedMessage(e.getValue(), e.getKey()));
                }
            }
        }

        mmt.removeChannel(channel);
        this.connections.remove(channel);
        channelReady(null);
    }

    private int getAvailableBytes() {
        int response = 0;
        synchronized (this.connections) {
            for (ServiceChannelEndpoint c : connections) {
                if (!c.isStarted() && !c.isOutgoing()) {
                    continue;
                }
                response += Math.max(CHANNEL_BUFFER - c.getOutstanding(), 0);
            }
        }
        return response;
    }

    /**
     * Route a message to appropriate channel(s).
     * 
     * @param msg
     *            The message to route.
     * @param msgId
     *            The sequence number of the msg if determined, or null.
     * @return Whether the msg was handled.
     */
    boolean routeMessageToChannel(DirectByteBuffer msg, SequenceNumber msgId) {
        List<ServiceChannelEndpoint> channels = new ArrayList<ServiceChannelEndpoint>();
        int totalOutstanding = 0;
        int totalBuffer = 0;

        synchronized (this.connections) {
            for (ServiceChannelEndpoint c : connections) {
                // Don't allow questionable paths to be opened by the
                // server.
                if (!c.isStarted() && !c.isOutgoing()) {
                    continue;
                }

                totalOutstanding += c.getOutstanding();
                totalBuffer += CHANNEL_BUFFER;
                // Don't allow full paths to get greedy.
                if (c.isStarted() && c.getOutstanding() > CHANNEL_BUFFER) {
                    continue;
                }

                // Don't resend on an active channel.
                if (msgId != null && msgId.getChannels().contains(new Integer(c.getChannelId()[0]))) {
                    continue;
                }

                // Decide on priority.
                if (c.isStarted()) {
                    boolean added = false;
                    for (int i = 0; i < channels.size(); i++) {
                        ServiceChannelEndpoint current = channels.get(i);
                        if (!current.isStarted()) {
                            channels.add(i, c);
                            added = true;
                            break;
                        } else if (c.getBytesOut() / c.getAge() > current.getBytesOut()
                                / current.getAge()) {
                            channels.add(i, c);
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        channels.add(c);
                    }
                } else {
                    channels.add(c);
                }
            }
        }
        if (channels.size() == 0) {
            logger.warning("not accepting more data from client, no available channel.");
            return false;
        }

        ArrayList<ServiceChannelEndpoint> channelsToUse = new ArrayList<ServiceChannelEndpoint>();
        if (!this.FEATURES.contains(ServiceFeatures.PACKET_DUPLICATION)) {
            channelsToUse.add(channels.get(0));
        } else if (this.FEATURES.contains(ServiceFeatures.ADAPTIVE_DUPLICATION)) {
            if (totalBuffer == 0 || totalOutstanding == 0) {
                channelsToUse.addAll(channels);
            } else {
                float replicationFactor = (float) ((totalBuffer - totalOutstanding) * 1.0 / totalBuffer);
                int replicas = (int) (replicationFactor * channels.size());
                if (msgId != null) {
                    replicas -= msgId.getChannels().size();
                }
                if (replicas > channels.size()) {
                    replicas = channels.size();
                }
                if (replicas < 1) {
                    replicas = 1;
                }
                for (int i = 0; i < replicas; i++) {
                    channelsToUse.add(channels.get(i));
                }
            }
        } else {
            channelsToUse.addAll(channels);
        }

        if (msgId == null) {
            msgId = mmt.nextMsg();
        }
        logger.warning("Message will attempt to send with replication " + channelsToUse.size());
        for (ServiceChannelEndpoint c : channelsToUse) {
            if (!c.isStarted()) {
                logger.finest("Unstarted channel chosen, msg buffered");
                synchronized (bufferedNetworkMessages) {
                    if (bufferedNetworkMessages.size() < SERVICE_MSG_BUFFER_SIZE) {
                        bufferedNetworkMessages.add(new BufferedMessage(msg, msgId));
                    }
                }
            } else {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Writing message to channel: " + c.getDescription());
                }
                mmt.sendMsg(msgId, c);
                c.writeMessage(msgId, msg, FEATURES.contains(ServiceFeatures.UDP));
            }
        }
        if (msgId.getChannels().size() == 0) {
            return false;
        } else {
            return true;
        }
    }

    protected class ServerIncomingMessageListener implements MessageQueueListener {

        @Override
        public void dataBytesReceived(int byte_count) {
        }

        @Override
        public boolean messageReceived(Message message) {
            logger.finest("ASC Service message recieved.");

            if (!(message instanceof DataMessage)) {
                String msg = "got wrong message type from server: ";
                logger.warning(msg + message.getDescription());
                AbstractServiceConnection.this.close(msg);
                return false;
            }
            DataMessage dataMessage = (DataMessage) message;
            boolean routed = AbstractServiceConnection.this.routeMessageToChannel(
                    dataMessage.transferPayload(), null);
            if (!routed) {
                logger.warning("No channel accepted incoming packet.");
            }
            return true;
        }

        @Override
        public void protocolBytesReceived(int byte_count) {
        }
    }

    public void channelReady(ServiceChannelEndpoint channel) {
        if (channel != null && !this.connections.contains(channel)) {
            logger.warning("Unregistered channel attempted to provide service transit.");
            return;
        }

        synchronized (bufferedNetworkMessages) {
            int size = bufferedNetworkMessages.size();
            while (size > 0) {
                BufferedMessage b = bufferedNetworkMessages.pop();
                if (!b.messageId.isAcked()) {
                    routeMessageToChannel(b.message, b.messageId);
                }
                if (bufferedNetworkMessages.size() == size) {
                    break;
                }
                size = bufferedNetworkMessages.size();
            }
        }
    }

    protected class ServiceRateHandler implements RateHandler {
        private final AbstractServiceConnection connection;

        ServiceRateHandler(AbstractServiceConnection c) {
            this.connection = c;
        }

        @Override
        public int getCurrentNumBytesAllowed() {
            return this.connection.getAvailableBytes();
        }

        @Override
        public void bytesProcessed(int num_bytes_processed) {
            return;
        }

    }
}