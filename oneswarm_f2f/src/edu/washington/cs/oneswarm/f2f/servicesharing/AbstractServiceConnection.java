package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;

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
    public static final Logger logger = Logger.getLogger(AbstractServiceConnection.class.getName());

    protected final LinkedList<OSF2FChannelDataMsg> bufferedServiceMessages;
    protected final LinkedList<DirectByteBuffer> bufferedNetworkMessages;

    public abstract boolean isOutgoing();

    public abstract void addChannel(FriendConnection channel, OSF2FHashSearch search, OSF2FHashSearchResp response);

    protected final PriorityQueue<ServiceChannelEndpoint> connections;

    public AbstractServiceConnection() {
		this.connections = new PriorityQueue<ServiceChannelEndpoint>(1, new ChannelComparator());
		this.bufferedServiceMessages = new LinkedList<OSF2FChannelDataMsg>();
 		this.bufferedNetworkMessages = new LinkedList<DirectByteBuffer>();
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
    
    void writeMessageToServiceConnection(OSF2FChannelDataMsg message) {
        synchronized(bufferedServiceMessages) {
            bufferedServiceMessages.add(message);
        }
    }

    void removeChannel(ServiceChannelEndpoint channel) {
        this.connections.remove(channel);
    }
    
    void routeMessageToChannel(DirectByteBuffer msg) {
    	logger.info("ASC routing service message to a channel.");
        ServiceChannelEndpoint channel = this.connections.peek();
        if (!channel.isStarted()) {
            System.out.println("Unstarted channel prioritized, msg buffered");
            synchronized(bufferedNetworkMessages) {
                bufferedNetworkMessages.add(msg);
            }
        } else {
            System.out.println("Msg written to available channel.");
            channel.writeMessage(msg);
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
    
    private class ChannelComparator implements Comparator<ServiceChannelEndpoint> {
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
                this.connections.peek().writeMessage(bufferedNetworkMessages.pop());
            }
        }
    }
}