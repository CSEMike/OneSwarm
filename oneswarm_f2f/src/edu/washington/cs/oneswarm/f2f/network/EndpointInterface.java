package edu.washington.cs.oneswarm.f2f.network;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;

public interface EndpointInterface {

    public abstract boolean isOutgoing();

    /**
     * This method is called "from above", when the peer connection is
     * terminated, send a reset to other side
     */
    public abstract void close(String reason);

    /**
     * this method is called from below when a reset is received
     * 
     * @param reason
     */
    public abstract void closeChannelReset();

    /**
     * this method is called from below if the friend connection dies
     * 
     * @param reason
     */
    public abstract void closeConnectionClosed(FriendConnection friend, String reason);

    public abstract long getAge();

    public abstract long getArtificialDelay();

    public abstract long getBytesIn();

    public abstract long getBytesOut();

    public abstract int[] getChannelId();

    public abstract String getDescription();

    public abstract int getDownloadRate();

    public abstract long getLastMsgTime();

    public abstract int[] getPathID();

    public abstract Friend getRemoteFriend();

    public abstract String getRemoteIP();

    public abstract int getUploadRate();

    public abstract void incomingOverlayMsg(final OSF2FChannelDataMsg msg);

    public abstract boolean isLANLocal();

    public abstract boolean isStarted();

    public abstract boolean isTimedOut();

    public abstract void start();
}