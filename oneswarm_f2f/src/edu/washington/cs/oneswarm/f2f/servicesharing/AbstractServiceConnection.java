package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.EndpointInterface;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.OverlayEndpoint;

public abstract class AbstractServiceConnection implements EndpointInterface {

    protected static final Logger logger = Logger.getLogger(OverlayEndpoint.class.getName());

    protected final LinkedList<OSF2FChannelDataMsg> bufferedServiceMessages;
    protected final LinkedList<DirectByteBuffer> bufferedNetworkMessages;

    public abstract boolean isOutgoing();

    public abstract void addChannel(FriendConnection channel, OSF2FHashSearch search, OSF2FHashSearchResp response);

    protected final ArrayList<ServiceChannelEndpoint> connections;

    public AbstractServiceConnection() {
        this.connections = new ArrayList<ServiceChannelEndpoint>();
        this.bufferedServiceMessages = new LinkedList<OSF2FChannelDataMsg>();
        this.bufferedNetworkMessages = new LinkedList<DirectByteBuffer>();
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

    /**
     * Currently we connect to the server when we get the first message data.
     */
    @Override
    public void start() {
        logger.fine(getDescription() + " starting");
        for (ServiceChannelEndpoint conn: this.connections) {
            if (!conn.isStarted()) {
                conn.start();
            }
        }
    }

    @Override
    public void close(String reason) {
        for (ServiceChannelEndpoint conn : this.connections) {
            conn.close(reason);
        }
 
        synchronized (bufferedServiceMessages) {
            while (bufferedServiceMessages.size() > 0) {
                bufferedServiceMessages.removeFirst().destroy();
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
            return this.connections.get(0).getArtificialDelay();
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
    public boolean isStarted() {
        for (ServiceChannelEndpoint conn : this.connections) {
            if (conn.isStarted())
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
    
    void writeMessageToServiceConnection(OSF2FChannelDataMsg message) {
        synchronized(bufferedServiceMessages) {
            bufferedServiceMessages.add(message);
        }
    }

    void removeChannel(ServiceChannelEndpoint channel) {
        this.connections.remove(channel);
    }
    
    // TODO(willscott): Choose correct channel for messages.
    void routeMessageToChannel(DirectByteBuffer msg) {
        for (ServiceChannelEndpoint channel : this.connections) {
            if (channel.isStarted()) {
                System.out.println("MSG directly queued with started channel.");
                channel.writeMessage(msg);
                return;
            }
        }
        synchronized(bufferedNetworkMessages) {
            System.out.println("MSG for mesh added to shared buffer.");
            bufferedNetworkMessages.add(msg);
        }
    }

    void channelReady(ServiceChannelEndpoint i) {
        synchronized(bufferedNetworkMessages) {
            while (bufferedNetworkMessages.size() > 0) {
                System.out.println("MSG for mesh taken off of shared buffer.");
                i.writeMessage(bufferedNetworkMessages.removeFirst());
            }
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
                AbstractServiceConnection.this.close(msg);
                return false;
            }
            DataMessage dataMessage = (DataMessage) message;
            DirectByteBuffer data = dataMessage.getPayload();
            int pos = data.position((byte) 0);
            System.out.println("MSG entered with val " + data.get((byte) 0));
            data.position((byte) 0, pos);
            AbstractServiceConnection.this.routeMessageToChannel(dataMessage.transferPayload());
            return true;
        }

        @Override
        public void protocolBytesReceived(int byte_count) {
        }
    }
}