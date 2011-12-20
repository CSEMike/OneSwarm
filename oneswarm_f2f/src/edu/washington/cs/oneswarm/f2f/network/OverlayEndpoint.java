package edu.washington.cs.oneswarm.f2f.network;

import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService.DelayedExecutor;
import edu.washington.cs.oneswarm.f2f.servicesharing.OSF2FServiceDataMsg;

/**
 * This class handles shared functionality of all overlay end-points and must be
 * extended.
 * 
 * @author isdal
 * 
 */
public abstract class OverlayEndpoint implements EndpointInterface {
    private final static Logger logger = Logger.getLogger(OverlayEndpoint.class.getName());
    /*
     * max number of ms that a message can be delivered earlier than
     * overlayDelayMs if that avoids a call to Thread.sleep()
     */
    private final static int INCOMING_MESSAGE_DELAY_SLACK = 10;

    private long bytesIn = 0;

    protected long bytesOut = 0;
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
    protected final boolean outgoing;

    public OverlayEndpoint(FriendConnection friendConnection, int pathID, long overlayDelayMs,
            OSF2FHashSearch search, OSF2FHashSearchResp response, boolean outgoing) {
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
        this.outgoing = outgoing;
    }

    protected abstract void cleanup();

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#isOutgoing()
     */
    @Override
    public boolean isOutgoing() {
        return outgoing;
    }

    private void deregister() {
        // remove it from the friend connection
        friendConnection.deregisterOverlayTransport(this);
        cleanup();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#close(java.lang
     * .String)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#closeChannelReset
     * ()
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see edu.washington.cs.oneswarm.f2f.network.EndpointInterface#
     * closeConnectionClosed(java.lang.String)
     */
    @Override
    public void closeConnectionClosed(FriendConnection f, String reason) {
        closeReason = reason;
        logger.fine(getDescription() + ": OverlayTransport closed, reason:" + closeReason);

        closed = true;
        deregister();
    }

    protected abstract void destroyBufferedMessages();

    /*
     * (non-Javadoc)
     * 
     * @see edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getAge()
     */
    @Override
    public long getAge() {
        return System.currentTimeMillis() - startTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getArtificialDelay
     * ()
     */
    @Override
    public long getArtificialDelay() {
        return overlayDelayMs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getBytesIn()
     */
    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getBytesOut()
     */
    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getChannelId()
     */
    @Override
    public int[] getChannelId() {
        int[] channels = new int[1];
        channels[0] = channelId;
        return channels;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getDescription()
     */
    @Override
    public String getDescription() {
        if (desc == null) {
            desc = NetworkManager.OSF2F_TRANSPORT_PREFIX + ": "
                    + friendConnection.getRemoteFriend().getNick() + ":"
                    + Integer.toHexString(channelId);
        }
        return desc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getDownloadRate
     * ()
     */
    @Override
    public int getDownloadRate() {
        return (int) downloadRateAverage.getAverage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getLastMsgTime()
     */
    @Override
    public long getLastMsgTime() {
        return System.currentTimeMillis() - lastMsgTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getPathID()
     */
    @Override
    public int[] getPathID() {
        int[] paths = new int[1];
        paths[0] = pathID;
        return paths;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getRemoteFriend
     * ()
     */
    @Override
    public Friend getRemoteFriend() {
        return friendConnection.getRemoteFriend();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getRemoteIP()
     */
    @Override
    public String getRemoteIP() {
        return friendConnection.getRemoteIp().getHostAddress();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#getUploadRate()
     */
    @Override
    public int getUploadRate() {
        return (int) uploadRateAverage.getAverage();
    }

    protected abstract void handleDelayedOverlayMessage(final OSF2FChannelDataMsg msg);

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#incomingOverlayMsg
     * (edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg)
     */
    @Override
    public void incomingOverlayMsg(OSF2FChannelDataMsg msg) {
        lastMsgTime = System.currentTimeMillis();
        msg.setByteInChannel(bytesIn);
        bytesIn += msg.getMessageSize();
        if (closed) {
            return;
        }
        if (isService()) {
            try {
                if (!(msg instanceof OSF2FServiceDataMsg)) {
                    msg = OSF2FServiceDataMsg.fromChannelMessage(msg);
                }
                PacketListener setupPacketListener = friendConnection.getSetupPacketListener();
                if (setupPacketListener != null && msg.getByteInChannel() == 0) {
                    setupPacketListener.packetArrivedAtFinalDestination(friendConnection, search,
                            response, msg, outgoing);
                }
            } catch (MessageException m) {
                logger.warning("Got non service message to a service endpoint!: " + m.getMessage());
                return;
            }

        }
        final OSF2FChannelDataMsg message = msg;
        delayedOverlayMessageTimer.queue(overlayDelayMs, INCOMING_MESSAGE_DELAY_SLACK,
                new TimerTask() {
                    @Override
                    public void run() {
                        handleDelayedOverlayMessage(message);
                    }
                });
    }

    protected abstract boolean isService();

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#isLANLocal()
     */
    @Override
    public boolean isLANLocal() {
        return friendConnection.getNetworkConnection().isLANLocal();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.washington.cs.oneswarm.f2f.network.EndpointInterface#isStarted()
     */
    @Override
    public boolean isStarted() {
        return started;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.washington.cs.oneswarm.f2f.network.EndpointInterface#isTimedOut()
     */
    @Override
    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastMsgTime > TIMEOUT;
    }

    private void sendReset() {
        sentReset = true;
        friendConnection.sendChannelRst(new OSF2FChannelReset(OSF2FChannelReset.CURRENT_VERSION,
                channelId));
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.washington.cs.oneswarm.f2f.network.EndpointInterface#start()
     */
    @Override
    public abstract void start();

    protected long writeMessageToFriendConnection(DirectByteBuffer msgBuffer) {
        OSF2FChannelDataMsg msg = new OSF2FChannelDataMsg(OSF2FMessage.CURRENT_VERSION, channelId,
                msgBuffer);
        long totalWritten = msgBuffer.remaining(DirectByteBuffer.SS_MSG);
        this.writeMessage(msg);
        bytesOut += totalWritten;
        return totalWritten;
    }

    protected void writeMessage(OSF2FChannelDataMsg msg) {
        msg.setForward(false);
        msg.setByteInChannel(bytesOut);
        PacketListener setupPacketListener = friendConnection.getSetupPacketListener();
        if (setupPacketListener != null && msg.getByteInChannel() == 0) {
            setupPacketListener.packetAddedToTransportQueue(friendConnection, search, response,
                    outgoing, msg);
        }
        friendConnection.sendChannelMsg(msg, true);
    }
}
