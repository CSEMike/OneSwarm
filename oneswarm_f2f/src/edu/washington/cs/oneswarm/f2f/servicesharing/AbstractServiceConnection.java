package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
    static final int SERVICE_MSG_BUFFER_SIZE = 1024;

    private static final int CHANNEL_BUFFER = 1024 * 4;
    protected int serviceSequenceNumber;
    protected final DirectByteBuffer[] bufferedServiceMessages = new DirectByteBuffer[SERVICE_MSG_BUFFER_SIZE];
    protected final LinkedList<DirectByteBuffer> bufferedNetworkMessages;
    private final MessageStreamMultiplexer mmt;

    @Override
    public abstract boolean isOutgoing();

    public abstract void addChannel(FriendConnection channel, OSF2FHashSearch search, OSF2FHashSearchResp response);

    protected final List<ServiceChannelEndpoint> connections = Collections
            .synchronizedList(new ArrayList<ServiceChannelEndpoint>());

    private enum SCPolicy {
        ROUNDROBIN, RANDOM, WEIGHTED
    };

    final SCPolicy policy;

    public AbstractServiceConnection() {
        	String channelScheme = COConfigurationManager.getStringParameter(SERVICE_PRIORITY_KEY);
        if (channelScheme.equals("roundrobin")) {
            policy = SCPolicy.ROUNDROBIN;
        } else if (channelScheme.equals("random")) {
            policy = SCPolicy.RANDOM;
        } else {
            policy = SCPolicy.WEIGHTED;
        }
        this.serviceSequenceNumber = 0;
 		this.bufferedNetworkMessages = new LinkedList<DirectByteBuffer>();
        this.mmt = new MessageStreamMultiplexer();
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
        // Handle acknowledgments.
        synchronized (this.connections) {
            for (ServiceChannelEndpoint s : this.connections) {
                if (s.getChannelId()[0] == message.getChannelId()) {
                    mmt.onAck(message, s);
                }
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
        // logger.info("ASC routing service message to a channel.");
        ServiceChannelEndpoint channel = null;
        if (policy == SCPolicy.RANDOM) {
            while (true) {
                synchronized (this.connections) {
                    int id = (int) (Math.random() * connections.size());
                    channel = this.connections.get(id);
                }

                // Assume some channel is 'started'
                if (!channel.isStarted() && !channel.isOutgoing()) {
                    continue;
                }
                break;
            }
        } else if (policy == SCPolicy.ROUNDROBIN) {
            while (true) {
                channel = this.connections.remove(0);
                this.connections.add(channel);

                // Assume some channel is 'started'
                if (!channel.isStarted() && !channel.isOutgoing()) {
                    continue;
                }
                break;
            }
        } else {
            synchronized (this.connections) {
            for(ServiceChannelEndpoint c : connections) {
                // Don't allow questionable paths to be opened by the server.
                if (!c.isStarted() && !c.isOutgoing()) {
                    continue;
                }
                if (channel == null) {
                    channel = c;
                    continue;
                }
                if (!c.isStarted() && channel.isStarted()) {
                    if (channel.getOutstanding() > CHANNEL_BUFFER) {
                        channel = c;
                    }
                    continue;
                } else if (!c.isStarted() && !channel.isStarted()) {
                    continue;
                } else if (c.isStarted() && !channel.isStarted()) {
                    if (c.getOutstanding() < CHANNEL_BUFFER) {
                        channel = c;
                    }
                    continue;
                } else if (c.isStarted() && channel.isStarted()) {
                    if (c.getBytesOut() / c.getAge() > channel.getBytesOut() / channel.getAge()
                            && c.getOutstanding() < CHANNEL_BUFFER) {
                        channel = c;
                    } else if (channel.getOutstanding() > CHANNEL_BUFFER
                            && c.getOutstanding() < channel.getOutstanding()) {
                        channel = c;
                    }
                }
            }
            }
        }
        if (channel == null) {
            logger.warning("No channel selected by policy.  data lost.");
            return;
        }
        logger.warning("channel status:" + channel.getBytesIn() + "/" + channel.getBytesOut()
                + "; " + channel.getDownloadRate() + " / " + channel.getUploadRate() + "; "
                + channel.getOutstanding());
        if (!channel.isStarted()) {
            logger.fine("Unstarted channel prioritized, msg buffered");
            synchronized (bufferedNetworkMessages) {
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
            // logger.info("ASC Service message recieved.");

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

    public void channelReady(ServiceChannelEndpoint channel) {
        if (!this.connections.contains(channel)) {
            logger.warning("Unregistered channel attempted to provide service transit.");
            return;
        }

        // At least one message can be written, since a channel just indicated readyness.
        synchronized (bufferedNetworkMessages) {
            while (bufferedNetworkMessages.size() > 0) {
                routeMessageToChannel(bufferedNetworkMessages.pop());
            }
        }
    }
}