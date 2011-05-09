package edu.washington.cs.oneswarm.f2f.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransportFactory;
import org.gudy.azureus2.core3.util.AENetworkClassifier;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.ProtocolEndpoint;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.TransportEndpoint;
import com.aelitis.azureus.core.networkmanager.impl.NetworkConnectionImpl;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.BTMessageEncoder;

import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.OSF2FAzSwtUi;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.DelayedExecutorService.DelayedExecutor;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter.DownloadManagerStartListener;

public class OverlayTransport implements Transport {

    private final static Logger logger = Logger.getLogger(OverlayTransport.class.getName());

    class OverlayProtocolEndpoint implements ProtocolEndpoint {
        private ConnectionEndpoint connectionEndpoint;

        public OverlayProtocolEndpoint() {
            connectionEndpoint = new ConnectionEndpoint(getRandomAddr());
        }

        public Transport connectOutbound(boolean connect_with_crypto, boolean allow_fallback,
                byte[][] shared_secrets, ByteBuffer initial_data, boolean high_priority,
                ConnectListener listener) {
            Debug.out("tried to create outgoing OverlayTransport, this should never happen!!!");
            throw new RuntimeException("not implemented");
        }

        public Transport connectOutbound(boolean connect_with_crypto, boolean allow_fallback,
                byte[][] shared_secrets, ByteBuffer initial_data, ConnectListener listener) {
            Debug.out("tried to create outgoing OverlayTransport, this should never happen!!!");
            throw new RuntimeException("not implemented");
        }

        public ConnectionEndpoint getConnectionEndpoint() {
            return connectionEndpoint;
        }

        public String getDescription() {
            return "PROTOCOL_TCP";
        }

        public int getType() {
            return PROTOCOL_TCP;
        }

        public void setConnectionEndpoint(ConnectionEndpoint ce) {
            this.connectionEndpoint = ce;
        }
    }

    interface WriteQueueWaiter {
        public void readyForWrite();
    }

    /*
     * max number of ms that a message can be delivered earlier than
     * overlayDelayMs if that avoids a call to Thread.sleep()
     */
    private final static int INCOMING_MESSAGE_DELAY_SLACK = 10;

    public final static byte[] ID_BYTES = new String("-OS-F2F-").getBytes();
    private final static int HANDSHAKE_RESERVED_BITS_START_POS = 20;
    private final static int HANDSHAKE_RESERVED_BITS_END_POS = 28;
    private final static int HANDSHAKE_INFO_HASH_START_POS = 28;
    private final static int HANDSHAKE_INFO_HASH_END_POS = 48;
    private final static int HANDSHAKE_PEER_ID_POS = 48;
    private static final int HANDSHAKE_END_POS = HANDSHAKE_PEER_ID_POS + 20;
    private final static int HANDSHAKE_PEER_ID_KEEP = ID_BYTES.length;
    private static final int HANDSHAKE_PEER_ID_START_MOD_POS = HANDSHAKE_PEER_ID_POS
            + HANDSHAKE_PEER_ID_KEEP;

    public static InetSocketAddress getRandomAddr() {
        byte[] randomAddr = new byte[16];
        randomAddr[0] = (byte) 0xfc;
        randomAddr[1] = 0;
        Random r = new Random();
        byte[] rand = new byte[randomAddr.length - 2];
        r.nextBytes(rand);
        System.arraycopy(rand, 0, randomAddr, 2, rand.length);
        InetAddress addr;
        try {
            addr = InetAddress.getByAddress(randomAddr);
            InetSocketAddress remoteFakeAddr = new InetSocketAddress(addr, 1);
            return remoteFakeAddr;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private ByteBuffer data_already_read = null;

    protected boolean closed = false;
    private String closeReason = "";
    private boolean sentReset = false;
    private final int TIMEOUT = 2 * 60 * 1000;
    private long lastMsgTime;

    private final byte[] infoHash;
    private final long startTime;
    private boolean started = false;
    private final int channelId;

    private final int pathID;
    // all operations on this object must be in a synchronized block
    private final LinkedList<OSF2FChannelDataMsg> bufferedMessages;

    private final FriendConnection connection;

    private List<EventWaiter> readWaiter = new LinkedList<EventWaiter>();
    private List<EventWaiter> writeWaiter = new LinkedList<EventWaiter>();

    private int transport_mode;

    private int posInHandshake = 0;

    private byte[] remoteHandshakeInfoHashBytes = new byte[20];
    private volatile boolean remoteHandshakeRecieved;
    private volatile boolean outgoing;

    private final byte[] channelPeerId;

    private long bytesIn = 0;
    private long bytesOut = 0;
    private Average uploadRateAverage = Average.getInstance(1000, 10);
    private Average downloadRateAverage = Average.getInstance(1000, 10);

    private final long overlayDelayMs;

    private final DelayedExecutor delayedOverlayMessageTimer;

    public OverlayTransport(FriendConnection connection, int channelId, byte[] infohash,
            int pathID, boolean outgoing, long overlayDelayMs) {
        this.lastMsgTime = System.currentTimeMillis();
        this.overlayDelayMs = overlayDelayMs;
        this.infoHash = infohash;
        this.bufferedMessages = new LinkedList<OSF2FChannelDataMsg>();
        this.connection = connection;
        this.channelId = channelId;
        logger.fine(getDescription() + ": Creating overlay transport");
        this.channelPeerId = generatePeerId();
        this.pathID = pathID;
        this.outgoing = outgoing;
        this.startTime = System.currentTimeMillis();
        delayedOverlayMessageTimer = DelayedExecutorService.getInstance().getFixedDelayExecutor(
                overlayDelayMs);
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
        synchronized (bufferedMessages) {
            while (bufferedMessages.size() > 0) {
                bufferedMessages.removeFirst().destroy();
            }
        }
        // and remove it from the friend connection
        connection.deregisterOverlayTransport(this);

    }

    /**
     * this method is called from below when a reset is received
     * 
     * @param reason
     */
    public void closeChannelReset() {

        if (sentReset) {
            // ok, this is the response to our previous close
            connection.deregisterOverlayTransport(this);
        } else {
            if (!closed) {
                closeReason = "remote host closed overlay channel";
                logger.fine(getDescription() + ": OverlayTransport closed, reason:" + closeReason);
                // this is the remote side saying that the connection is closed
                // send a reset back to confirm
                sendReset();
                closed = true;
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
        connection.deregisterOverlayTransport(this);
    }

    public void connectedInbound() {
        throw new RuntimeException("not implemented");
    }

    public void connectOutbound(ByteBuffer initial_data, ConnectListener listener) {
        throw new RuntimeException("not implemented");
    }

    public void connectOutbound(ByteBuffer initial_data, ConnectListener listener,
            boolean high_priority) {
        throw new RuntimeException("not implemented");
    }

    static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public byte[] generatePeerId() {
        byte[] peerId = new byte[20];
        System.arraycopy(ID_BYTES, 0, peerId, 0, ID_BYTES.length);

        for (int i = HANDSHAKE_PEER_ID_KEEP; i < 20; i++) {
            int pos = (int) (Math.random() * chars.length());
            peerId[i] = (byte) chars.charAt(pos);
        }

        return peerId;
    }

    public int getChannelId() {
        return channelId;
    }

    private String desc = null;

    public String getDescription() {
        if (desc == null) {
            desc = NetworkManager.OSF2F_TRANSPORT_PREFIX + ": "
                    + connection.getRemoteFriend().getNick() + ":" + Integer.toHexString(channelId);
        }
        return desc;
    }

    public String getEncryption() {
        return ("FriendToFriend over SSL");
    }

    public int getMssSize() {
        return OSF2FMessage.MAX_MESSAGE_SIZE;
    }

    public int getPathID() {
        return pathID;
    }

    public TransportEndpoint getTransportEndpoint() {

        return new TransportEndpoint() {

            public ProtocolEndpoint getProtocolEndpoint() {

                final ProtocolEndpoint p = new OverlayProtocolEndpoint();
                p.getConnectionEndpoint().addProtocol(p);
                return p;
            }
        };
    }

    public int getTransportMode() {
        return transport_mode;
    }

    public void incomingOverlayMsg(final OSF2FChannelDataMsg msg) {
        lastMsgTime = System.currentTimeMillis();
        if (closed) {
            return;
        }
        delayedOverlayMessageTimer.queue(overlayDelayMs, INCOMING_MESSAGE_DELAY_SLACK,
                new TimerTask() {
                    @Override
                    public void run() {
                        handleDelayedOverlayMessage(msg);
                    }
                });
    }

    private void handleDelayedOverlayMessage(final OSF2FChannelDataMsg msg) {
        synchronized (bufferedMessages) {
            bufferedMessages.add(msg);

            if (readWaiter.size() > 0) {
                // Log.log("Overlay transport: notifying readwaiter");
                for (EventWaiter w : readWaiter) {
                    w.eventOccurred();
                }
                readWaiter.clear();
            }
        }
    }

    public boolean isEncrypted() {
        return (true);
    }

    public boolean isReadyForRead(EventWaiter waiter) {
        // we need the layers above to get the exception
        if (closed) {
            return true;
        }
        if (data_already_read != null) {
            return true;
        }
        synchronized (bufferedMessages) {
            if (bufferedMessages.size() > 0) {
                return true;
            }

            if (waiter != null) {
                readWaiter.add(waiter);
            }
            return false;
        }
    }

    public boolean isReadyForWrite(final EventWaiter waiter) {

        // if this is an incoming connection, we to wait
        // for the incoming handshake before we
        // send our own see PEPeerTransportProtocol.java:~390
        if (!remoteHandshakeRecieved && !outgoing) {
            if (waiter != null) {
                writeWaiter.add(waiter);
            }
            return false;
        }
        if (closed) {
            return false;
        }
        if (!connection.isReadyForWrite(new WriteQueueWaiter() {
            public void readyForWrite() {
                if (waiter != null) {
                    logger.finest(getDescription() + ": connection ready, notifying waiter");
                    waiter.eventOccurred();
                }
            }
        })) {
            logger.finest(getDescription() + ": connection not ready, adding waiter");
            return false;
        }
        logger.finest(getDescription() + ": connection ready");
        return true;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isTCP() {
        return true;
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastMsgTime > TIMEOUT;
    }

    private byte modifyIncomingHandShake(byte b) {
        if (posInHandshake == -1) {
            return b;
        } else {
            if (posInHandshake >= HANDSHAKE_PEER_ID_START_MOD_POS
                    && posInHandshake < HANDSHAKE_END_POS) {
                b = channelPeerId[posInHandshake - HANDSHAKE_PEER_ID_START_MOD_POS];

            } else if (posInHandshake >= HANDSHAKE_RESERVED_BITS_START_POS
                    && posInHandshake < HANDSHAKE_RESERVED_BITS_END_POS) {
                b = (byte) 0;
            } else if (posInHandshake >= HANDSHAKE_INFO_HASH_START_POS
                    && posInHandshake < HANDSHAKE_INFO_HASH_END_POS) {
                remoteHandshakeInfoHashBytes[posInHandshake - HANDSHAKE_INFO_HASH_START_POS] = b;
            } else if (posInHandshake == HANDSHAKE_INFO_HASH_END_POS) {
                // check if the info hash sent is what we
                // expected, see PEPeerTransportProtocol:~390
                if (!Arrays.equals(infoHash, remoteHandshakeInfoHashBytes)) {
                    logger.warning(getDescription()
                            + ": WARNING in "
                            + connection
                            + " :: remote host different infohash "
                            + "than what we expected ,expected:\n "
                            + new String(Base64.encode(infoHash) + " got\n"
                                    + new String(Base64.encode(remoteHandshakeInfoHashBytes))));
                } else {
                    logger.finer(getDescription() + ": remote handshake matches, notifying waiter");
                    remoteHandshakeRecieved = true;
                    for (EventWaiter waiter : writeWaiter) {
                        waiter.eventOccurred();
                    }
                    writeWaiter.clear();
                }
            }
            posInHandshake++;
            if (posInHandshake > HANDSHAKE_END_POS) {

                posInHandshake = -1;
            }
        }
        return b;
    }

    private int putInBuffer(ByteBuffer sources[], int array_offset, int length,
            DirectByteBuffer target) {

        int copied = 0;
        ByteBuffer t = target.getBuffer(DirectByteBuffer.SS_MSG);

        for (int i = array_offset; i < array_offset + length; i++) {
            ByteBuffer source = sources[i];
            if (t.remaining() == 0) {
                break;
            }
            if (source.remaining() == 0) {
                continue;
            }

            int numBytesToCopy = Math.min(t.remaining(), source.remaining());
            if (t.remaining() < source.remaining()) {
                // we need to set the limit to avoid buffer overflow
                int oldLimit = source.limit();
                source.limit(source.position() + t.remaining());
                t.put(source);
                source.limit(oldLimit);
            } else {
                t.put(source);
            }
            copied += numBytesToCopy;
        }
        return copied;
    }

    /**
     * This function is used when reading from the network
     * 
     * @param source
     * @param targets
     * @param array_offset
     * @param length
     * @return
     */

    private int putInBuffers(ByteBuffer source, ByteBuffer targets[], int array_offset, int length) {
        /*
         * check if we are past the handshake, in that case do the efficient
         * copy
         */
        int copied = 0;

        if (posInHandshake == -1) {
            for (int i = array_offset; i < array_offset + length; i++) {
                ByteBuffer t = targets[i];
                if (source.remaining() == 0) {
                    break;
                }
                if (t.remaining() == 0) {
                    continue;
                }

                int numBytesToCopy = Math.min(t.remaining(), source.remaining());
                if (t.remaining() < source.remaining()) {
                    // we need to set the limit to avoid buffer overflow
                    int oldLimit = source.limit();
                    source.limit(source.position() + t.remaining());
                    t.put(source);
                    source.limit(oldLimit);
                } else {
                    t.put(source);
                }
                copied += numBytesToCopy;
            }
        } else {
            for (int i = array_offset; i < array_offset + length; i++) {
                // int start = copied;
                while (targets[i].hasRemaining() && source.hasRemaining()) {
                    byte b = source.get();
                    // in the beginning we need to mod the handshake some
                    b = modifyIncomingHandShake(b);
                    targets[i].put(b);
                    // System.out.print(b + ",");
                    copied++;

                }
                // System.out.println("copied " + (copied - start)
                // + " bytes into buffer " + i + " offset=" + array_offset
                // + " len=" + length);
            }
        }
        return copied;
    }

    public long read(ByteBuffer[] buffers, int array_offset, int length) throws IOException {

        int totalRead = 0;
        int totalSpace = 0;
        for (int i = array_offset; i < array_offset + length; i++) {
            totalSpace += buffers[i].remaining();
        }

        // check if we have any pushback data
        if (data_already_read != null) {
            totalRead += putInBuffers(data_already_read, buffers, array_offset, length);
            if (!data_already_read.hasRemaining()) {
                data_already_read = null;
            }
        }

        synchronized (bufferedMessages) {
            while (bufferedMessages.size() > 0 && totalRead < totalSpace) {
                // get first message in buffer
                OSF2FChannelDataMsg msg = bufferedMessages.getFirst();
                DirectByteBuffer data = msg.getPayload();

                // Log.log("ready to read: "
                // + data.remaining(DirectByteBuffer.SS_MSG) + " space: "
                // + totalSpace);
                totalRead += putInBuffers(data.getBuffer(DirectByteBuffer.SS_MSG), buffers,
                        array_offset, length);

                // check if we read the entire message
                if (totalRead < totalSpace) {
                    // if we did, we can remove it and destroy it
                    bufferedMessages.removeFirst().destroy();
                }
            }
        }

        if (closed && totalRead == 0) {
            throw new IOException("Channel closed: " + getDescription() + " reason: " + closeReason);
        }
        bytesIn += totalRead;
        downloadRateAverage.addValue(totalRead);
        return totalRead;
    }

    public void sendReset() {
        sentReset = true;
        connection.sendChannelRst(new OSF2FChannelReset(OSF2FChannelReset.CURRENT_VERSION,
                channelId));
    }

    public void setAlreadyRead(ByteBuffer bytes_already_read) {
        if (data_already_read != null) {
            Debug.out("push back already performed");
        }
        if (bytes_already_read != null && bytes_already_read.hasRemaining()) {
            data_already_read = bytes_already_read;
        }
    }

    public void setReadyForRead() {
        throw new RuntimeException("not implemented");
    }

    public void setTrace(boolean on) {
        throw new RuntimeException("not implemented");
    }

    public void setTransportMode(int mode) {
        this.transport_mode = mode;
    }

    public void start() {

        logger.fine("Starting overlay transport");
        started = true;
        // get the PEPeerMananger AKA PEPeerControl
        final DownloadManager downloadManager = AzureusCoreImpl.getSingleton().getGlobalManager()
                .getDownloadManager(new HashWrapper(infoHash));
        // check if the download is stopped, in that case start it
        startDownloadManager(downloadManager);

    }

    private void startDownloadManager(final DownloadManager downloadManager) {
        DownloadManagerStarter.startDownload(downloadManager, new DownloadManagerStartListener() {
            public void downloadStarted() {
                createPeerTransport(downloadManager);
            }
        });
    }

    public long getArtificialDelay() {
        return overlayDelayMs;
    }

    private void createPeerTransport(DownloadManager downloadManager) {
        // final check, we only allow this if the osf2f network is enabled, and
        // osf2f friend search is a valid peer source
        boolean allowed = checkOSF2FAllowed(downloadManager.getDownloadState().getPeerSources(),
                downloadManager.getDownloadState().getNetworks());

        if (!allowed) {
            Debug.out("denied request to create a peer");
            this.closeConnectionClosed("access denied when creating overlay");
            return;
        }

        PEPeerManager manager = downloadManager.getPeerManager();

        PEPeerControl control = (PEPeerControl) manager;
        // set it up the same way as an incoming connection
        final NetworkConnection overlayConn = new NetworkConnectionImpl(this,
                new BTMessageEncoder(), new BTMessageDecoder());
        PEPeerTransport pt = PEPeerTransportFactory.createTransport(control, PEPeerSource.PS_OSF2F,
                overlayConn, null);

        // start it
        pt.start();
        // and add it to the control
        control.addPeerTransport(pt);

        // add the friend
        pt.setData(OSF2FAzSwtUi.KEY_OVERLAY_TRANSPORT, this);
    }

    /**
     * Checks if a download allows both OSF2F network, and osf2f search peers
     * 
     * @param downloadManager
     * @return
     */
    public static boolean checkOSF2FAllowed(String[] peerSources, String[] netSources) {
        boolean allowed = true;

        // check if the download allows osf2f peers
        boolean peerSourceOk = false;

        for (int i = 0; i < peerSources.length; i++) {
            String source = peerSources[i];
            if (source.equals(PEPeerSource.PS_OSF2F)) {
                peerSourceOk = true;
            }
        }

        boolean networkOk = false;
        for (int i = 0; i < netSources.length; i++) {
            String network = netSources[i];
            if (network.equals(AENetworkClassifier.AT_OSF2F)) {
                networkOk = true;
            }
        }
        if (peerSourceOk == false || networkOk == false) {
            allowed = false;
        }
        return allowed;
    }

    public long write(ByteBuffer[] buffers, int array_offset, int length) throws IOException {
        if (closed) {
            // when closed just ignore the write requests
            // hopefully the peertransport will read everything in the buffer
            // and get the exception there when done
            return 0;
        }
        int totalToWrite = 0;
        int totalWritten = 0;
        for (int i = array_offset; i < array_offset + length; i++) {
            totalToWrite += buffers[i].remaining();
        }
        logger.finest(getDescription() + "got write request for: " + totalToWrite);
        // only write one packet at the time
        if (isReadyForWrite(null)) {
            DirectByteBuffer msgBuffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                    Math.min(totalToWrite, OSF2FMessage.MAX_PAYLOAD_SIZE));
            this.putInBuffer(buffers, array_offset, length, msgBuffer);
            msgBuffer.flip(DirectByteBuffer.SS_MSG);
            totalWritten += writeMessage(msgBuffer);
        }
        logger.finest("wrote " + totalWritten + " to overlay channel " + channelId);
        bytesOut += totalWritten;
        uploadRateAverage.addValue(totalWritten);
        return totalWritten;
    }

    protected long writeMessage(DirectByteBuffer msgBuffer) {
        OSF2FChannelDataMsg msg = new OSF2FChannelDataMsg(OSF2FMessage.CURRENT_VERSION, channelId,
                msgBuffer);
        long totalWritten = msgBuffer.remaining(DirectByteBuffer.SS_MSG);
        msg.setForward(false);
        connection.sendChannelMsg(msg, true);
        return totalWritten;
    }

    public String getRemoteIP() {
        return connection.getRemoteIp().getHostAddress();
    }

    public Friend getRemoteFriend() {
        return connection.getRemoteFriend();
    }

    public long getAge() {
        return System.currentTimeMillis() - startTime;
    }

    public long getLastMsgTime() {
        return System.currentTimeMillis() - lastMsgTime;
    }

    public long getBytesIn() {
        return bytesIn;
    }

    public long getBytesOut() {
        return bytesOut;
    }

    public int getUploadRate() {
        return (int) uploadRateAverage.getAverage();
    }

    public int getDownloadRate() {
        return (int) downloadRateAverage.getAverage();
    }

    public boolean isLANLocal() {
        return connection.getNetworkConnection().isLANLocal();
    }

}
