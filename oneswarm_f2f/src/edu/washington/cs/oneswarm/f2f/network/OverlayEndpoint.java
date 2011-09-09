package edu.washington.cs.oneswarm.f2f.network;

import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkManager;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService.DelayedExecutor;

/**
 * This class handles shared functionality of all overlay end-points and must be
 * extended.
 * 
 * @author isdal
 * 
 */
public abstract class OverlayEndpoint {
    private final static Logger logger = Logger.getLogger(OverlayEndpoint.class.getName());
    /*
     * max number of ms that a message can be delivered earlier than
     * overlayDelayMs if that avoids a call to Thread.sleep()
     */
    private final static int INCOMING_MESSAGE_DELAY_SLACK = 10;

    private long bytesIn = 0;

    private long bytesOut = 0;
    protected boolean started = false;

    protected final int channelId;

    protected boolean closed = false;
    protected String closeReason = "";

    private String desc = null;
    private final DelayedExecutor delayedOverlayMessageTimer;

    protected Average downloadRateAverage = Average.getInstance(1000, 10);

    protected final FriendConnection friendConnection;
    protected long lastMsgTime;
    private final long overlayDelayMs;
    protected final int pathID;
    private boolean sentReset = false;

    private final long startTime;
    private final int TIMEOUT = 2 * 60 * 1000;

    protected Average uploadRateAverage = Average.getInstance(1000, 10);
    private final OSF2FHashSearch search;
    private final OSF2FHashSearchResp response;

    public OverlayEndpoint(FriendConnection friendConnection, int pathID, long overlayDelayMs,
            OSF2FHashSearch search, OSF2FHashSearchResp response) {
        this.friendConnection = friendConnection;
        this.channelId = response.getChannelID();
        this.pathID = pathID;
        this.overlayDelayMs = overlayDelayMs;
        this.lastMsgTime = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
        delayedOverlayMessageTimer = DelayedExecutorService.getInstance().getFixedDelayExecutor(
                overlayDelayMs);
        this.search = search;
        this.response = response;
    }

    protected abstract void cleanup();

    private void deregister() {
        // remove it from the friend connection
        friendConnection.deregisterOverlayTransport(this);
        cleanup();
    }

    /**
     * This method is called "from above", when the peer connection is
     * terminated, send a reset to other side
     */
    public void close(String reason) {
        if (!closed) {
            closeReason = "peer - " + reason;
            logger.fine(getDescription() + ": OverlayTransport closed, reason:" + closeReason);

            closed = true;
            this.sendReset();
        }
        // we don't expect anyone to read whatever we have left in the buffer
        this.destroyBufferedMessages();

        deregister();
    }

    /**
     * this method is called from below when a reset is received
     * 
     * @param reason
     */
    public void closeChannelReset() {
        if (sentReset) {
            // ok, this is the response to our previous close
            deregister();
        } else {
            if (!closed) {
                closeReason = "remote host closed overlay channel";
                logger.fine(getDescription() + ": OverlayTransport closed, reason:" + closeReason);
                // this is the remote side saying that the connection is closed
                // send a reset back to confirm
                closed = true;
                sendReset();
                deregister();
            }
        }
    }

    /**
     * this method is called from below if the friend connection dies
     * 
     * @param reason
     */
    public void closeConnectionClosed(String reason) {
        closeReason = reason;
        logger.fine(getDescription() + ": OverlayTransport closed, reason:" + closeReason);

        closed = true;
        deregister();
    }

    protected abstract void destroyBufferedMessages();

    public long getAge() {
        return System.currentTimeMillis() - startTime;
    }

    public long getArtificialDelay() {
        return overlayDelayMs;
    }

    public long getBytesIn() {
        return bytesIn;
    }

    public long getBytesOut() {
        return bytesOut;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getDescription() {
        if (desc == null) {
            desc = NetworkManager.OSF2F_TRANSPORT_PREFIX + ": "
                    + friendConnection.getRemoteFriend().getNick() + ":"
                    + Integer.toHexString(channelId);
        }
        return desc;
    }

    public int getDownloadRate() {
        return (int) downloadRateAverage.getAverage();
    }

    public long getLastMsgTime() {
        return System.currentTimeMillis() - lastMsgTime;
    }

    public int getPathID() {
        return pathID;
    }

    public Friend getRemoteFriend() {
        return friendConnection.getRemoteFriend();
    }

    public String getRemoteIP() {
        return friendConnection.getRemoteIp().getHostAddress();
    }

    public int getUploadRate() {
        return (int) uploadRateAverage.getAverage();
    }

    protected abstract void handleDelayedOverlayMessage(final OSF2FChannelDataMsg msg);

    public void incomingOverlayMsg(final OSF2FChannelDataMsg msg) {
        lastMsgTime = System.currentTimeMillis();
        msg.setByteInChannel(bytesIn);
        bytesIn += msg.getMessageSize();
        if (closed) {
            return;
        }

        delayedOverlayMessageTimer.queue(overlayDelayMs, INCOMING_MESSAGE_DELAY_SLACK,
                new TimerTask() {
                    @Override
                    public void run() {
                        SetupPacketListener setupPacketListener = friendConnection
                                .getSetupPacketListener();
                        if (setupPacketListener != null && msg.getByteInChannel() == 0) {
                            setupPacketListener.packetArrivedAtFinalDestination(friendConnection,
                                    search, response, msg);
                        }
                        handleDelayedOverlayMessage(msg);
                    }
                });
    }

    public boolean isLANLocal() {
        return friendConnection.getNetworkConnection().isLANLocal();
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastMsgTime > TIMEOUT;
    }

    private void sendReset() {
        sentReset = true;
        friendConnection.sendChannelRst(new OSF2FChannelReset(OSF2FChannelReset.CURRENT_VERSION,
                channelId));
    }

    public abstract void start();

    protected long writeMessageToFriendConnection(DirectByteBuffer msgBuffer) {
        OSF2FChannelDataMsg msg = new OSF2FChannelDataMsg(OSF2FMessage.CURRENT_VERSION, channelId,
                msgBuffer);
        long totalWritten = msgBuffer.remaining(DirectByteBuffer.SS_MSG);
        msg.setForward(false);
        msg.setByteInChannel(bytesOut);
        SetupPacketListener setupPacketListener = friendConnection.getSetupPacketListener();
        if (setupPacketListener != null && msg.getByteInChannel() == 0) {
            setupPacketListener
                    .packetAddedToTransportQueue(friendConnection, search, response, msg);
        }
        friendConnection.sendChannelMsg(msg, true);
        bytesOut += totalWritten;
        return totalWritten;
    }
}
