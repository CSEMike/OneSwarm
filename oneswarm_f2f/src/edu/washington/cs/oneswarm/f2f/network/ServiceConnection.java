package edu.washington.cs.oneswarm.f2f.network;

public class ServiceConnection extends OverlayTransport {

    public ServiceConnection(FriendConnection connection, int channelId, byte[] infohash,
            int pathID, boolean outgoing, long overlayDelayMs) {
        super(connection, channelId, infohash, pathID, outgoing, overlayDelayMs);
    }

}
