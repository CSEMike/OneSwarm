package edu.washington.cs.oneswarm.f2f.datagram;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

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
        ACTIVE, CLOSED, NEW, OK_SENT
    }

    /**
     * Send states:
     * 
     * NEW: no packets sent
     * 
     * INIT_SENT: remote side supports UDP and an init packet was sent over
     * 
     * KEEPALIVE_SENT: the remote side acked our init packet with an Ok
     * packet and we have sent over a udp keepalive.
     * 
     * ACTIVE: the remote side acked our udp keepalive with an Ok packet.
     * 
     * CLOSED: the connection is closed.
     * 
     * State changes:
     * 
     * NEW->INIT_SENT: friendconnection handshake completed, send INIT
     * packet
     * 
     * INIT_SEND->KEEPALIVE_SENT: remote side acked our INIT with an OK,
     * send a UDP keepalive packet.
     * 
     * KEEPALIVE_SENT->ACTIVE: remote side acked our keepalive with an OK,
     * udp channel is active and packets can be sent.
     */
    enum SendState {
        ACTIVE, CLOSED, INIT_SENT, KEEPALIVE_SENT, NEW
    }

    private final static byte AL = DirectByteBuffer.AL_NET_CRYPT;

    public final static Logger logger = Logger.getLogger(DatagramConnection.class.getName());

    private final static int MAX_UNACKED_KEEPALIVES = 10;

    private final static byte SS = DirectByteBuffer.SS_MSG;

    private final long createdAt = System.currentTimeMillis();

    private DatagramDecrypter decrypter;
    private final DatagramEncrypter encrypter;

    private final FriendConnection friendConnection;
    private int keepaliveCount = 0;
    private long lastPacketReceived;

    private long lastPacketSent;
    private final DatagramConnectionManager manager;
    private final byte[] outgoingPacketBuf = new byte[2048];
    private DatagramConnection.ReceiveState receiveState = ReceiveState.NEW;

    private int remotePort;

    private DatagramConnection.SendState sendState = SendState.NEW;

    private final ByteBuffer typeBuffer;
    private final DatagramSendThread sendThread;

    public DatagramConnection(DatagramConnectionManager manager, FriendConnection friendConnection)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        this.friendConnection = friendConnection;
        this.encrypter = new DatagramEncrypter();
        this.manager = manager;
        this.typeBuffer = ByteBuffer.allocate(1);
        sendThread = new DatagramSendThread();
        sendThread.start();
    }

    public void close() {
        sendState = SendState.CLOSED;
        receiveState = ReceiveState.CLOSED;
        manager.deregister(this);
        sendThread.interupt();
    }

    public long getAge() {
        return System.currentTimeMillis() - createdAt;
    }

    public OSF2FDatagramInit createInitMessage() {
        sendState = SendState.INIT_SENT;
        OSF2FDatagramInit initMessage = new OSF2FDatagramInit(OSF2FMessage.CURRENT_VERSION,
                encrypter.getCryptoAlgo(), encrypter.getKey(), encrypter.getIv(),
                encrypter.getHmac(), manager.getPort());
        logger.fine("Init message created: " + initMessage.getDescription());
        return initMessage;
    }

    String getKey() {
        return DatagramConnectionManager.getKey(friendConnection.getRemoteIp(), remotePort);
    }

    public long getLastMessageSentTime() {
        return System.currentTimeMillis() - lastPacketSent;
    }

    public void initMessageReceived(OSF2FDatagramInit message) {
        logger.fine("Got init message: " + message.getDescription());
        this.remotePort = message.getLocalPort();
        try {
            decrypter = new DatagramDecrypter(message.getEncryptionKey(), message.getIv(),
                    message.getHmacKey());
            receiveState = ReceiveState.OK_SENT;
            manager.register(this);
            friendConnection.sendDatagramOk(new OSF2FDatagramOk());
        } catch (Exception e) {
            e.printStackTrace();
            sendState = SendState.CLOSED;
            return;
        }
    }

    public boolean isSendingActive() {
        return sendState == SendState.ACTIVE;
    }

    boolean messageReceived(DatagramPacket packet) {
        if (decrypter == null) {
            logger.fine("Got unknown packet");
            return false;
        }
        synchronized (decrypter) {
            if (receiveState == ReceiveState.CLOSED) {
                logger.finest("Got packet on closed connection");
                return false;
            }
            try {
                byte[] data = packet.getData();
                DirectByteBuffer decryptBuffer = DirectByteBufferPool.getBuffer(AL, data.length);

                if (!decrypter.decrypt(data, packet.getOffset(), packet.getLength(),
                        decryptBuffer.getBuffer(AL))) {
                    logger.finer("DatagramDecryption error: " + friendConnection + " packet="
                            + packet);
                    return false;
                }
                Message message = OSF2FMessageFactory.createOSF2FMessage(decryptBuffer);
                if (message instanceof OSF2FChannelMsg) {
                    ((OSF2FChannelMsg) message).setDatagram(true);
                }
                logger.finest("packet decrypted: " + message.getDescription());
                if (receiveState == ReceiveState.OK_SENT) {
                    // First packet received, set state to active and tell
                    // friend connection to send ok.
                    friendConnection.sendDatagramOk(new OSF2FDatagramOk());
                    receiveState = ReceiveState.ACTIVE;
                }
                lastPacketReceived = System.currentTimeMillis();
                friendConnection.datagramDecoded(message, data.length);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public void okMessageReceived() {
        if (sendState == SendState.KEEPALIVE_SENT) {
            sendState = SendState.ACTIVE;
            logger.fine("OK message received, state=" + sendState);
        } else if (sendState == SendState.INIT_SENT) {
            sendKeepAlive();
            sendState = SendState.KEEPALIVE_SENT;
        }
    }

    public void sendMessage(OSF2FMessage message) {
        sendThread.queueMessage(message);
    }

    public void sendKeepAlive() {
        // We need to have received the remote init packet to be able to send.
        if (!(receiveState == ReceiveState.OK_SENT || receiveState == ReceiveState.ACTIVE)) {
            return;
        }
        logger.finest("Sending keepalive, state=" + sendState);
        if (sendState == SendState.ACTIVE) {
            sendMessage(new OSF2FDatagramOk());
        } else if ((sendState == SendState.INIT_SENT || sendState == SendState.KEEPALIVE_SENT)
                && keepaliveCount < MAX_UNACKED_KEEPALIVES) {
            keepaliveCount++;
            sendMessage(new OSF2FDatagramOk());
        }
        return;
    }

    /**
     * Sending enrypted udp packets is cpu intensive and potentially blocking.
     * Each connection is sending
     * 
     * @author isdal
     * 
     */
    class DatagramSendThread implements Runnable {
        private final LinkedBlockingQueue<OSF2FMessage> messageQueue;
        private final Thread thread;

        public DatagramSendThread() {
            messageQueue = new LinkedBlockingQueue<OSF2FMessage>();
            thread = new Thread(this);
            thread.setName("DatagramSendThread-" + friendConnection.toString());
            thread.setDaemon(true);
        }

        public void interupt() {
            thread.interrupt();
        }

        public void start() {
            thread.start();
        }

        public void queueMessage(OSF2FMessage message) {
            messageQueue.add(message);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    OSF2FMessage message = messageQueue.take();
                    sendMessage(message);
                }
            } catch (InterruptedException e) {
                logger.fine("Datagram send thread closed: " + friendConnection.toString());
            }
        }

        /**
         * Send a message over this UDP connection.
         * 
         * @param message
         */
        private void sendMessage(OSF2FMessage message) {
            try {
                synchronized (encrypter) {
                    lastPacketSent = System.currentTimeMillis();
                    int size = 0;

                    // Write the message type and prepare for reading.
                    typeBuffer.clear();
                    typeBuffer.put((byte) message.getFeatureSubID());
                    typeBuffer.flip();
                    size += 1;

                    // Get the message data.
                    DirectByteBuffer[] data = message.getData();

                    // Collect the buffers in one array.
                    ByteBuffer[] unencryptedPayload = new ByteBuffer[data.length + 1];
                    unencryptedPayload[0] = typeBuffer;
                    for (int i = 0; i < data.length; i++) {
                        ByteBuffer bb = data[i].getBuffer(SS);
                        unencryptedPayload[i + 1] = bb;
                        size += bb.remaining();
                    }

                    logger.finest("encrypting " + size + " bytes");

                    // Encrypt the serialized payload into the payload buffer.
                    EncryptedPacket encrypted = encrypter.encrypt(unencryptedPayload,
                            outgoingPacketBuf);

                    // Return the incoming message buffers to the pool.
                    message.destroy();

                    // Create and send the packet.
                    DatagramPacket packet = new DatagramPacket(outgoingPacketBuf, 0,
                            encrypted.getLength(), friendConnection.getRemoteIp(), remotePort);
                    manager.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendState = SendState.CLOSED;
            }
        }
    }
}