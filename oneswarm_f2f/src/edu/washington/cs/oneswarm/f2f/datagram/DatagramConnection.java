package edu.washington.cs.oneswarm.f2f.datagram;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FDatagramInit;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FDatagramOk;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageFactory;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

/**
 * Connection used in parallel to the standard SSL connection between 2 friends.
 * 
 * * Wire level packet format:
 * [Unencrypted]
 * 8 bytes sequence number.
 * [Encrypted]
 * 1 byte message type.
 * x bytes payload
 * 20 bytes hmac
 * 
 * @author isdal
 * 
 */
public class DatagramConnection {
    /**
     * Receive states:
     * 
     * NEW: no packets sent
     * 
     * OK_SENT: init received, ok sent back
     * 
     * ACTIVE: we have successfully decoded one udp packet and are expecting
     * more.
     * 
     * CLOSED: the connection is closed.
     * 
     * 
     * State changes:
     * 
     * NEW->OK_SENT: got an incoming init packet, send back an ok
     * 
     * OK_SENT->ACTIVE: got an incoming udp packet, send back second ok.
     * 
     */
    enum ReceiveState {
        ACTIVE, CLOSED, NEW, OK_SENT;
    }

    /**
     * Send states:
     * 
     * NEW: no packets sent
     * 
     * INIT_SENT: remote side supports UDP and an init packet was sent over
     * 
     * UDP_OK_SENT: the remote side acked our init packet with an ok
     * packet and we have sent over a udp ok.
     * 
     * ACTIVE: the remote side acked our udp ok with an tcp ok packet.
     * 
     * CLOSED: the connection is closed.
     * 
     * State changes:
     * 
     * NEW->INIT_SENT: friendconnection handshake completed, send INIT
     * packet
     * 
     * INIT_SEND->UDP_OK_SENT: remote side acked our INIT with an tcp ok and we
     * have sent over a UDP OK packet.
     * 
     * UDP_OK_SENT->ACTIVE: remote side acked our udp ok with an tcp ok,
     * udp channel is active and packets can be sent.
     */
    enum SendState {
        ACTIVE, CLOSED, INIT_SENT, INIT_PENDING, UDP_OK_SENT, NEW
    }

    private final static byte AL = DirectByteBuffer.AL_NET_CRYPT;

    public final static Logger logger = Logger.getLogger(DatagramConnection.class.getName());

    // According to netalyzr more than 98% of hosts have a path MTU of 1450
    // bytes. Set max size to 1410 to be on the safer side. (room for 8 byte UDP
    // header and 20 byte ip header).
    // (MAX_DATAGRAM_SIZE - HMAC_SIZE - SEQUENCE_NUMBER_BYTES % BLOCK_SIZE) == 0
    // must be 0;
    public static final int MAX_DATAGRAM_SIZE = 1410;
    // The actual payload we can use is a bit less.
    // -4 for length field
    // -1 for type field
    // -8 for sequence number
    // -1 for minimum padding
    // -20 for sha1 digest
    public static final int MAX_DATAGRAM_PAYLOAD_SIZE = MAX_DATAGRAM_SIZE
            - OSF2FMessage.MESSAGE_HEADER_LEN - DatagramEncrypter.SEQUENCE_NUMBER_BYTES - 1
            - DatagramEncrypter.HMAC_SIZE;

    private final static int MAX_UNACKED_UDP_OKs = 10;

    private final static byte SS = DirectByteBuffer.SS_MSG;

    private final long createdAt = System.currentTimeMillis();

    private DatagramDecrypter decrypter;

    // Visible for testing.
    final DatagramEncrypter encrypter;

    private final DatagramListener friendConnection;
    private int udpOkCount = 0;
    private long lastPacketReceived = System.currentTimeMillis();

    private final DatagramConnectionManager manager;

    // Visible for testing
    DatagramConnection.ReceiveState receiveState = ReceiveState.NEW;

    private int remotePort;

    // Visible for testing
    DatagramConnection.SendState sendState = SendState.NEW;

    // Visible for testing
    final DatagramSendThread sendThread;
    private final InetAddress remoteIp;
    private final ByteBuffer decryptBuffer;

    private boolean registered;
    private final HashSet<String> remoteIpPorts = new HashSet<String>();

    public DatagramConnection(DatagramConnectionManager manager, DatagramListener friendConnection)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        this.friendConnection = friendConnection;
        this.encrypter = new DatagramEncrypter();
        this.manager = manager;
        this.remoteIp = friendConnection.getRemoteIp();
        this.decryptBuffer = ByteBuffer.allocateDirect(MAX_DATAGRAM_SIZE);

        sendThread = new DatagramSendThread();
        sendThread.start();
    }

    public void close() {
        sendState = SendState.CLOSED;
        receiveState = ReceiveState.CLOSED;
        deregister();
        sendThread.quit();
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    public OSF2FDatagramInit createInitMessage() {
        sendState = SendState.INIT_SENT;
        OSF2FDatagramInit initMessage = new OSF2FDatagramInit(OSF2FMessage.CURRENT_VERSION,
                encrypter.getCryptoAlgo(), encrypter.getKey(), encrypter.getIv(),
                encrypter.getHmac(), manager.getPort());
        logger.fine(toString() + "Init message created: " + initMessage.getDescription());
        return initMessage;
    }

    Set<String> getKeys() {
        return remoteIpPorts;
    }

    public long getLastMessageSentTime() {
        return System.currentTimeMillis() - sendThread.lastPacketSent;
    }

    public void initMessageReceived(OSF2FDatagramInit message) {
        logger.fine(toString() + "Got init message: " + message.getDescription());
        this.remotePort = message.getLocalPort();
        this.remoteIpPorts.add(DatagramConnectionManagerImpl.getKey(remoteIp, remotePort));
        try {
            decrypter = new DatagramDecrypter(message.getEncryptionKey(), message.getIv(),
                    message.getHmacKey());
            receiveState = ReceiveState.OK_SENT;
            register();
            friendConnection.sendDatagramOk(new OSF2FDatagramOk(0));
        } catch (Exception e) {
            e.printStackTrace();
            sendState = SendState.CLOSED;
            return;
        }
    }

    private void register() {
        if (registered) {
            manager.deregister(this);
        }
        manager.register(this);
        this.registered = true;
    }

    private void deregister() {
        manager.deregister(this);
        this.registered = false;
    }

    public boolean isSendingActive() {
        return sendState == SendState.ACTIVE;
    }

    boolean messageReceived(DatagramPacket packet) {
        if (decrypter == null) {
            logger.fine(toString() + "Got unknown packet");
            return false;
        }
        synchronized (decrypter) {
            if (receiveState == ReceiveState.CLOSED) {
                logger.finest(toString() + "Got packet on closed connection");
                return false;
            }
            try {
                byte[] data = packet.getData();
                decryptBuffer.clear();
                if (!decrypter.decrypt(data, packet.getOffset(), packet.getLength(), decryptBuffer)) {
                    logger.finer(toString() + "DatagramDecryption error: " + toString()
                            + " packet=" + packet);
                    return false;
                }
                lastPacketReceived = System.currentTimeMillis();

                int oldLimit = decryptBuffer.limit();
                while (decryptBuffer.hasRemaining()) {
                    // The message length is 1 (for the type field) + the actual
                    // message length.
                    int messageLength = decryptBuffer.getInt();
                    if (messageLength > MAX_DATAGRAM_SIZE) {
                        logger.warning("got oversized length field!");
                        return false;
                    }
                    DirectByteBuffer messageBuffer = DirectByteBufferPool.getBuffer(AL,
                            messageLength);
                    // Set the limit so that only the current message is read.
                    decryptBuffer.limit(decryptBuffer.position() + messageLength);
                    messageBuffer.put(SS, decryptBuffer);
                    messageBuffer.flip(SS);
                    // Restore the limit to the old limit to prepare to read the
                    // next message.
                    decryptBuffer.limit(oldLimit);

                    Message message = OSF2FMessageFactory.createOSF2FMessage(messageBuffer);
                    if (message instanceof OSF2FChannelMsg) {
                        ((OSF2FChannelMsg) message).setDatagram(true);
                    }
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(toString() + "packet decrypted: " + message.getDescription());
                    }
                    if (receiveState == ReceiveState.OK_SENT) {
                        // First packet received, set state to active and tell
                        // friend connection to send ok.
                        if (!(message instanceof OSF2FDatagramOk)) {
                            logger.warning(toString() + "first datagram message not an OK message!");
                            receiveState = ReceiveState.CLOSED;
                            return false;
                        }
                        OSF2FDatagramOk ok = (OSF2FDatagramOk) message;
                        if (ok.getPaddingBytesNum() != MAX_DATAGRAM_PAYLOAD_SIZE) {
                            logger.warning(toString()
                                    + "Got OK message, but the payload is cropped (buggy router on path?), len="
                                    + ok.getPaddingBytesNum() + "<" + MAX_DATAGRAM_PAYLOAD_SIZE);
                            return false;
                        }
                        friendConnection.sendDatagramOk(new OSF2FDatagramOk(0));
                        receiveState = ReceiveState.ACTIVE;
                    }
                    friendConnection.datagramDecoded(message, messageLength);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public String addRemoteIpPort(InetAddress ip, int port) {
        String key = DatagramConnectionManagerImpl.getKey(ip, port);
        this.remoteIpPorts.add(key);
        return key;
    }

    public void okMessageReceived() {
        logger.fine(toString() + "OK message received, state=" + sendState);
        if (sendState == SendState.UDP_OK_SENT) {
            sendState = SendState.ACTIVE;
            logger.fine(toString() + "State set to " + sendState);
        } else if (sendState == SendState.INIT_SENT) {
            sendUpdOK();
            sendState = SendState.UDP_OK_SENT;
        }
    }

    public void sendMessage(OSF2FMessage message) {
        if (sendState == SendState.CLOSED) {
            logger.finest("Tried to send packet on a closed connection");
            return;
        }
        if (isTimedOut()) {
            logger.fine("Connection timed out (no packets received in a long time), closing");
            close();
            return;
        }
        try {
            sendThread.queueMessage(message);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendUpdOK() {
        // We need to have received the remote init packet to be able to send.
        if (!(receiveState == ReceiveState.OK_SENT || receiveState == ReceiveState.ACTIVE)) {
            return;
        }
        if (sendState == SendState.ACTIVE) {
            sendMessage(new OSF2FDatagramOk(0));
        } else if ((sendState == SendState.INIT_SENT || sendState == SendState.UDP_OK_SENT)
                && udpOkCount < MAX_UNACKED_UDP_OKs) {
            udpOkCount++;
            // Before the connection is set to active we need to make sure that
            // the remote side can receive udp packets of max mtu size (we use
            // 1400 bytes mtu without any path mtu discovery, 98% of paths can
            // support this packet size, and of the rest of the paths only 40%
            // handle
            // discovery anyway.).
            sendMessage(new OSF2FDatagramOk(MAX_DATAGRAM_PAYLOAD_SIZE));
        }
        return;
    }

    @Override
    public String toString() {
        return "DatagramConnection-" + friendConnection.toString() + " ";
    }

    public boolean isTimedOut() {
        return System.currentTimeMillis() - lastPacketReceived > FriendConnection.KEEP_ALIVE_TIMEOUT;
    }

    public void reInitialize() {
        if (sendState == SendState.CLOSED) {
            logger.warning("tried to reinitialize closed connection");
            return;
        }
        friendConnection.initDatagramConnection();
    }

    public int getQueueLength() {
        return sendThread.queueLength;
    }

    /**
     * Sending enrypted udp packets is cpu intensive and potentially blocking.
     * Each connection is sending
     * 
     * @author isdal
     * 
     */
    // Visible for testing.
    class DatagramSendThread implements Runnable {
        private long lastPacketSent = System.currentTimeMillis();

        private final ByteBuffer[] unencryptedPayload;

        // Visible for testing.
        final LinkedBlockingQueue<OSF2FMessage> messageQueue;
        private final Thread thread;
        private final byte[] outgoingPacketBuf = new byte[2048];
        private volatile boolean quit = false;

        private volatile int queueLength = 0;

        public DatagramSendThread() {
            messageQueue = new LinkedBlockingQueue<OSF2FMessage>(1024);
            thread = new Thread(this);
            thread.setName("DatagramSendThread-" + DatagramConnection.this.toString());
            thread.setDaemon(true);
            unencryptedPayload = new ByteBuffer[MAX_DATAGRAM_PAYLOAD_SIZE];
        }

        public void quit() {
            quit = true;
            thread.interrupt();
        }

        public void start() {
            thread.start();
        }

        public void queueMessage(OSF2FMessage message) throws InterruptedException {
            int messageSize = message.getMessageSize();
            if (messageSize > MAX_DATAGRAM_PAYLOAD_SIZE) {
                logger.warning("tried to send too large datagram: " + messageSize);
                return;
            }
            queueLength += messageSize + OSF2FMessage.MESSAGE_HEADER_LEN;
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("message queued, queue_length=" + queueLength);
            }
            messageQueue.put(message);
        }

        @Override
        public void run() {
            try {
                // Create a vehicle for sending more than one message in the
                // same datagram.
                RawMessage[] messageBuffer = new RawMessage[1 + MAX_DATAGRAM_PAYLOAD_SIZE / 5];
                while (!quit) {
                    int datagramSize = 0;
                    int packetNum = 0;
                    OSF2FMessage message = messageQueue.take();
                    synchronized (encrypter) {
                        do {
                            final int messageSize = message.getMessageSize();
                            datagramSize += messageSize + OSF2FMessage.MESSAGE_HEADER_LEN;
                            messageBuffer[packetNum++] = OSF2FMessageFactory
                                    .createOSF2FRawMessage(message);
                            if (logger.isLoggable(Level.FINEST)) {
                                logger.finest(String.format("Adding message, packets=%d, size=%d",
                                        packetNum, datagramSize));
                            }
                            // This is going to get sent, update the queue size
                            queueLength -= datagramSize;
                            // Check if we can fit more packets in there.
                        } while ((message = messageQueue.peek()) != null
                                && datagramSize + message.getMessageSize() <= MAX_DATAGRAM_PAYLOAD_SIZE
                                && (message = messageQueue.remove()) != null);
                        sendMessage(messageBuffer, packetNum);
                    }

                }
            } catch (InterruptedException e) {
                logger.fine("Datagram send thread closed: " + DatagramConnection.this.toString());
                OSF2FMessage message;
                while ((message = messageQueue.poll()) != null) {
                    message.destroy();
                }
            }
        }

        /**
         * Send a message over this UDP connection.
         * 
         * @param message
         */
        private void sendMessage(RawMessage[] messages, int num) {
            try {
                lastPacketSent = System.currentTimeMillis();
                int size = 0;
                int buffers = 0;
                for (int messageNum = 0; messageNum < num; messageNum++) {
                    // Get the message data.
                    DirectByteBuffer[] data = messages[messageNum].getRawData();

                    for (int i = 0; i < data.length; i++) {
                        ByteBuffer bb = data[i].getBuffer(SS);
                        unencryptedPayload[buffers++] = bb;
                        size += bb.remaining();
                    }
                }
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("encrypting " + size + " bytes");
                }
                // Encrypt the serialized payload into the payload buffer.
                EncryptedPacket encrypted = encrypter.encrypt(unencryptedPayload, buffers,
                        outgoingPacketBuf);

                // Return the incoming messages buffers to the pool.
                for (int i = 0; i < num; i++) {
                    messages[i].destroy();
                }

                // Create and send the packet.
                DatagramPacket packet = new DatagramPacket(outgoingPacketBuf, 0,
                        encrypted.getLength(), remoteIp, remotePort);
                manager.send(packet, friendConnection.isLanLocal());
            } catch (Exception e) {
                e.printStackTrace();
                sendState = SendState.CLOSED;
            }
        }
    }

    public int getRemotePort() {
        return remotePort;
    }
}