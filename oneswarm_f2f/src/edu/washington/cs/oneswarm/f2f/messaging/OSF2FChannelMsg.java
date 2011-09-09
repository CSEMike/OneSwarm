/**
 * 
 */
package edu.washington.cs.oneswarm.f2f.messaging;

import java.util.logging.Logger;

/**
 * @author isdal
 * 
 */
public abstract class OSF2FChannelMsg implements OSF2FMessage {
    private final int channelID;
    private final long createdTime;
    private boolean forward = false;
    public final static Logger logger = Logger.getLogger(OSF2FChannelMsg.class.getName());

    // Number of bytes previously forwarded in this channel. (for internal book
    // keeping)
    private long byteInChannel;

    protected OSF2FChannelMsg(int channelID) {
        this.channelID = channelID;
        this.createdTime = System.currentTimeMillis();
    }

    public long getByteInChannel() {
        return byteInChannel;
    }

    public void setByteInChannel(long bytesForwarded) {
        this.byteInChannel = bytesForwarded;
    }

    public final int getChannelId() {
        return channelID;
    }

    public abstract int getMessageSize();

    public final long getCreatedTime() {
        return createdTime;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public boolean isForward() {
        return forward;
    }

}
