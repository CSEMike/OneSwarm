package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.EndpointInterface;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.OverlayEndpoint;

public abstract class AbstractServiceConnection implements EndpointInterface {

    protected static final Logger logger = Logger.getLogger(OverlayEndpoint.class.getName());

    public abstract boolean isOutgoing();

    public abstract void addChannel(NetworkConnection incomingConnection, FriendConnection channel, OSF2FHashSearch search, OSF2FHashSearchResp response);

    protected final ArrayList<ServiceConnection> connections;

    public AbstractServiceConnection() {
        this.connections = new ArrayList<ServiceConnection>();
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
        for (ServiceConnection conn: this.connections) {
            if (!conn.isStarted()) {
                conn.start();
            }
        }
    }

    @Override
    public void close(String reason) {
        for (ServiceConnection conn : this.connections) {
            conn.close(reason);
        }
        this.connections.clear();
    }

    @Override
    public void closeChannelReset() {
        for (ServiceConnection conn : this.connections) {
            conn.closeChannelReset();
        }
        this.connections.clear();
    }

    @Override
    public void closeConnectionClosed(String reason) {
        for (ServiceConnection conn : this.connections) {
            conn.closeConnectionClosed(reason);
        }
        this.connections.clear();
    }

    @Override
    public long getAge() {
        long time = 0;
        for (ServiceConnection conn : this.connections) {
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
        for (ServiceConnection conn : this.connections) {
            in += conn.getBytesIn();
        }
        return in;
    }

    @Override
    public long getBytesOut() {
        long out = 0;
        for (ServiceConnection conn : this.connections) {
            out += conn.getBytesOut();
        }
        return out;
    }

    @Override
    public int[] getChannelId() {
        int[] channels = new int[this.connections.size()];
        int i = 0;
        for (ServiceConnection conn: this.connections) {
            channels[i++] = conn.getChannelId()[0];
        }
        return channels;
    }

    @Override
    public int[] getPathID() {
        int[] paths = new int[this.connections.size()];
        int i = 0;
        for (ServiceConnection conn: this.connections) {
            paths[i++] = conn.getPathID()[0];
        }
        return paths;
    }

    @Override
    public int getDownloadRate() {
        int rate = 0;
        for (ServiceConnection conn : this.connections) {
            rate += conn.getDownloadRate();
        }
        return rate;
    }

    @Override
    public int getUploadRate() {
        int rate = 0;
        for (ServiceConnection conn : this.connections) {
            rate += conn.getUploadRate();
        }
        return rate;
    }

    @Override
    public long getLastMsgTime() {
        long time = 0;
        for (ServiceConnection conn : this.connections) {
            time = Math.max(time, conn.getLastMsgTime());
        }
        return time;
    }

    @Override
    public Friend getRemoteFriend() {
        for (ServiceConnection conn : this.connections) {
            if (conn.isStarted())
                return conn.getRemoteFriend();
        }
        return null;
    }

    @Override
    public String getRemoteIP() {
        for (ServiceConnection conn : this.connections) {
            if (conn.isStarted())
                return conn.getRemoteIP();
        }
        return null;
    }

    @Override
    public void incomingOverlayMsg(OSF2FChannelDataMsg msg) {
        int channelId = msg.getChannelId();
        for (ServiceConnection conn : this.connections) {
            if (conn.getChannelId()[0] == channelId) {
                conn.incomingOverlayMsg(msg);
            }
        }
    }

    @Override
    public boolean isLANLocal() {
        for (ServiceConnection conn : this.connections) {
            if (conn.isLANLocal())
                return true;
        }
        return false;
    }

    @Override
    public boolean isStarted() {
        for (ServiceConnection conn : this.connections) {
            if (conn.isStarted())
                return true;
        }
        return false;
    }

    @Override
    public boolean isTimedOut() {
        for (ServiceConnection conn : this.connections) {
            if (!conn.isTimedOut())
                return false;
        }
        return true;
    }
}