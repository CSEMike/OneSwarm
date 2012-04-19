package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.ReferenceCountedDirectByteBuffer;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService.DelayedExecutor;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.OverlayEndpoint;
import edu.washington.cs.oneswarm.f2f.network.OverlayTransport;

/**
 * This class represents one Friend connection channel used for multiplexed
 * service channels.
 * Functionality extends from {@code OverlayEndpoint}, the additional
 * functionality from
 * this class is that received data is forwarded to the aggregate service
 * connection, and
 * outstanding sent data is tracked for congestion control across channels.
 * 
 * @author willscott
 * 
 */
public class ServiceChannelEndpoint extends OverlayEndpoint {
    public final static Logger logger = Logger.getLogger(ServiceChannelEndpoint.class.getName());
    private static final byte ss = 0;
    // Moving average sampling weight for latency estimation.
    private static final double EWMA = 0.25;
    // How long (in # RTT) before packet retransmission.
    private static final double RETRANSMISSION_MIN = 2;
    private static final double RETRANSMISSION_MAX = 3;

    public static final int MAX_SERVICE_MESSAGE_SIZE = 1024;

    private final DelayedExecutor delayedExecutor;
    protected final Hashtable<SequenceNumber, sentMessage> sentMessages;
    protected final Hashtable<Short, ServiceChannelEndpointDelegate> delegates = new Hashtable<Short, ServiceChannelEndpointDelegate>();
    protected final ArrayList<Short> delegateOrder = new ArrayList<Short>();
    private int outstandingBytes;
    private long latency = 1000;
    private long minLatency = Long.MAX_VALUE;
    private final long serviceKey;

    public ServiceChannelEndpoint(FriendConnection connection, OSF2FHashSearch search,
            OSF2FHashSearchResp response,
            boolean outgoing) {
        super(connection, response.getPathID(), 0, search, response, outgoing);
        logger.info("Service Channel Endpoint Created.");

        this.sentMessages = new Hashtable<SequenceNumber, sentMessage>();
        this.outstandingBytes = 0;
        this.delayedExecutor = DelayedExecutorService.getInstance().getVariableDelayExecutor();
        this.serviceKey = search.getInfohashhash();

        this.started = true;
        friendConnection.isReadyForWrite(new OverlayTransport.WriteQueueWaiter() {
            @Override
            public void readyForWrite() {
                logger.info("friend connection marked ready for write.");
                for (ServiceChannelEndpointDelegate d : ServiceChannelEndpoint.this.delegates
                        .values()) {
                    d.channelIsReady(ServiceChannelEndpoint.this);
                }
            }
        });
    }

    public void addDelegate(ServiceChannelEndpointDelegate d, short flow) {
        if (d.writesMessages()) {
            this.delegateOrder.add(flow);
        }
        this.delegates.put(flow, d);
        if (friendConnection.isReadyForWrite(null)) {
            d.channelIsReady(this);
        }
    }

    public void removeDelegate(ServiceChannelEndpointDelegate d) {
        for (Short flow : this.delegates.keySet()) {
            if (this.delegates.get(flow).equals(d)) {
                this.delegates.remove(flow);
                this.delegateOrder.remove(flow);
                break;
            }
        }
    }

    public int getPotentialWriteCapacity() {
        int channelCapacity = friendConnection.getSendQueuePotentialCapacity(this.channelId);
        return channelCapacity / this.delegateOrder.size();
    }

    public int getWriteCapacity(ServiceChannelEndpointDelegate d) {
        int networkCapacity = friendConnection.getSendQueueCurrentCapacity(this.channelId);
        int fullPackets = networkCapacity / (this.delegates.size() * MAX_SERVICE_MESSAGE_SIZE);

        int delegatePriority = this.delegates.size();
        for (Short flow : this.delegates.keySet()) {
            if (this.delegates.get(flow).equals(d)) {
                delegatePriority = this.delegateOrder.indexOf(flow);
            }
        }

        networkCapacity -= fullPackets * this.delegates.size() * MAX_SERVICE_MESSAGE_SIZE
                + delegatePriority * MAX_SERVICE_MESSAGE_SIZE;

        if (networkCapacity >= MAX_SERVICE_MESSAGE_SIZE) {
            fullPackets += 1;
        }

        return fullPackets * MAX_SERVICE_MESSAGE_SIZE;
    }

    @Override
    public void start() {
        // TODO(willscott): allow server to open channels.
    }

    @Override
    public boolean isStarted() {
        if (!this.outgoing) {
            return this.getBytesIn() > 0;
        }
        return friendConnection.isHandshakeReceived();
    }

    @Override
    protected void destroyBufferedMessages() {
        // No buffered messages to destroy.
        for (sentMessage b : this.sentMessages.values()) {
            b.msg.returnToPool();
        }
        this.sentMessages.clear();
        this.outstandingBytes = 0;
    }

    @Override
    public void cleanup() {
        for (ServiceChannelEndpointDelegate d : this.delegates.values()) {
            d.channelDidClose(this);
        }
    };

    @Override
    protected void handleDelayedOverlayMessage(OSF2FChannelDataMsg msg) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("incoming message: " + msg.getDescription());
        }

        if (closed) {
            return;
        }
        if (!this.isStarted()) {
            start();
        }
        if (!(msg instanceof OSF2FServiceDataMsg)) {
            logger.warning("Msg wasn't SDM: " + msg.getDescription());
            return;
        }
        OSF2FServiceDataMsg newMessage = (OSF2FServiceDataMsg) msg;
        // logger.fine("Received msg with sequence number " +
        if (!newMessage.isAck()) {
            logger.finest("ack enqueued for " + newMessage.getDescription());
            super.writeMessage(OSF2FServiceDataMsg.acknowledge(OSF2FMessage.CURRENT_VERSION,
                    channelId, newMessage.getSubchannel(),
                    new int[] { newMessage.getSequenceNumber() }));
        }

        for (ServiceChannelEndpointDelegate d : this.delegates.values()) {
            if (d.channelGotMessage(this, newMessage)) {
                break;
            }
        }
    }
    
    public long getServiceKey() {
        return this.serviceKey;
    }

    public void writeMessage(final SequenceNumber num, DirectByteBuffer buffer, boolean datagram) {
        // Move the requester to the bottom of the priority list.
        try {
            this.delegateOrder.remove(num.getFlow());
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        this.delegateOrder.add(num.getFlow());

        boolean rst = buffer == null;
        if (buffer == null) {
            buffer = new DirectByteBuffer(ByteBuffer.allocate(0));
        }

        int length = buffer.remaining(ss);
        ReferenceCountedDirectByteBuffer cpy = buffer.getReferenceCountedBuffer();
        sentMessage msg = new sentMessage(num, cpy, length, 0, datagram, rst);

        writeMessage(msg);
    }

    private void writeMessage(sentMessage msg) {
        SequenceNumber num = msg.num;
        synchronized (sentMessages) {
            this.sentMessages.put(num, msg);
        }
        this.outstandingBytes += msg.length;

        double retransmit = RETRANSMISSION_MIN + (RETRANSMISSION_MAX - RETRANSMISSION_MIN)
                * Math.random();
        // Remember the message may need to be retransmitted.
        delayedExecutor.queue((long) (retransmit * this.latency * (1 << msg.attempt)), msg);

        if (msg.creation + latency < System.currentTimeMillis()) {
            logger.warning("Skipping over-aggresive retransmission.");
            return;
        }
        msg.creation = System.currentTimeMillis();

        // Outgoing msg will be freed by super.writeMessage.
        msg.msg.incrementReferenceCount();
        OSF2FServiceDataMsg outgoing = new OSF2FServiceDataMsg(OSF2FMessage.CURRENT_VERSION,
                channelId, num.getNum(), num.getFlow(), new int[0], msg.msg);

        if (num.getNum() == 0 && !msg.rst) {
            // Mark SYN messages.
            outgoing.setControlFlag(4);
        }
        if (msg.rst) {
            outgoing.setControlFlag(2);
        }
        if (msg.datagram) {
            // Set datagram flag to allow the packet to be sent over UDP.
            outgoing.setDatagram(true);
        }

        long totalWritten = msg.length;
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format("Wrote %s to network. bytes: %d", num, msg.length));
        }
        super.writeMessage(outgoing);
        bytesOut += totalWritten;
    }

    public int getOutstanding() {
        return this.outstandingBytes;
    }

    /**
     * Get the recent latency experienced on the channel. Latency is recorded as
     * an exponentially weighted moving average. Each acknowledgment is weighted
     * as some fraction of the total latency, and previous samples are decayed
     * accordingly.
     * 
     * @return Channel latency estimate.
     */
    public long getLatency() {
        return this.latency;
    }

    public DirectByteBuffer getMessage(SequenceNumber num) {
        synchronized (sentMessages) {
            sentMessage m = this.sentMessages.get(num);
            if (m != null) {
                return m.msg;
            }
            return null;
        }
    }

    /**
     * Attempt to forget a sent message.
     * 
     * @param num
     *            The message to forget
     * @return True if the message was successfully stopped from retransmitting.
     */
    public boolean forgetMessage(SequenceNumber num) {
        synchronized (sentMessages) {
            sentMessage msg = this.sentMessages.remove(num);
            if (msg == null) {
                return false;
            }
            msg.cancel();
            this.outstandingBytes -= msg.length;
            long now = System.currentTimeMillis();
            long sample = now - msg.creation;
            // If not the first attempt, we don't know which attempt was acked.
            if (msg.attempt == 0) {
                this.latency = (long) (this.latency * (1 - EWMA) + sample * EWMA);
                if (sample < minLatency) {
                    minLatency = sample;
                }

                // Pending messages sent before this one were potentially lost
                sentMessage[] messages = this.sentMessages.values().toArray(new sentMessage[0]);
                for (sentMessage m : messages) {
                    if (m.creation < msg.creation) {
                        m.run();
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected boolean isService() {
        return true;
    }

    private class sentMessage extends TimerTask {
        public ReferenceCountedDirectByteBuffer msg;
        public int length;
        private final int position;
        public long creation;
        private final SequenceNumber num;
        private final int attempt;
        private final boolean datagram;
        public final boolean rst;

        public sentMessage(SequenceNumber num, ReferenceCountedDirectByteBuffer msg, int length,
                int attempt, boolean datagram, boolean rst) {
            this.creation = System.currentTimeMillis();
            this.msg = msg;
            this.position = msg.position(ss);
            msg.incrementReferenceCount();
            this.length = length;
            this.num = num;
            this.attempt = attempt;
            this.datagram = datagram;
            this.rst = rst;
        }

        @Override
        public void run() {
            synchronized (sentMessages) {
                sentMessage self = sentMessages.remove(num);
                if (self == null || closed) {
                    return;
                }
                if (self.attempt != attempt) {
                    logger.warning("Message queue concurency issues");
                    sentMessages.put(num, self);
                    return;
                }

                logger.fine("retransmitting " + num + ", try " + attempt);
                outstandingBytes -= length;
                msg.position(ss, position);
                writeMessage(this);
            }
        }

        @Override
        public boolean cancel() {
            msg.returnToPool();
            return super.cancel();
        }
    }
}
