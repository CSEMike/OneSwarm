package edu.washington.cs.oneswarm.f2f.datagram;

import static edu.washington.cs.oneswarm.f2f.datagram.DatagramConnection.MAX_DATAGRAM_PAYLOAD_SIZE;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.net.udp.uc.impl.ExternalUdpPacketHandler;

import edu.washington.cs.oneswarm.f2f.datagram.DatagramConnection.ReceiveState;
import edu.washington.cs.oneswarm.f2f.datagram.DatagramConnection.SendState;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FDatagramInit;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FDatagramOk;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageFactory;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

public class DatagramConnectionTest extends OneSwarmTestBase {
    public static final byte AL = DirectByteBuffer.AL_MSG;
    public static final byte SS = DirectByteBuffer.SS_MSG;

    private static class MockDatagramConnectionManager implements DatagramConnectionManager {
        DatagramSocket socket;
        final String desc;
        private DatagramConnection conn;

        public MockDatagramConnectionManager(String desc) throws SocketException {
            this.socket = new DatagramSocket();
            socket.setSoTimeout(500);
            this.desc = desc;
        }

        @Override
        public void send(DatagramPacket packet, boolean lan) throws IOException {
            // System.out.println(desc + ": sending packet: " +
            // packet.getSocketAddress());
            socket.send(packet);
        }

        @Override
        public void register(DatagramConnection connection) {
            conn = connection;
        }

        @Override
        public int getPort() {
            return socket.getLocalPort();
        }

        @Override
        public void deregister(DatagramConnection conn) {
            conn = null;
        }

        void receive() throws IOException {
            DatagramPacket p = new DatagramPacket(new byte[1450], 0, 1450);
            socket.receive(p);
            // System.out.println(desc + ": received packet: " +
            // p.getSocketAddress());

            conn.messageReceived(p);
        }

        public void socketUpdated() throws SocketException {
            int oldPort = this.socket.getLocalPort();
            this.socket.close();
            this.socket = new DatagramSocket();
            System.out.println("port change: " + oldPort + "->" + socket.getLocalPort());
            conn.reInitialize();
        }
    }

    MockDatagramConnectionManager manager1;
    MockDatagramConnectionManager manager2;
    DatagramConnection conn1;
    DatagramConnection conn2;
    LinkedList<Message> conn1Incoming;
    LinkedList<Message> conn2Incoming;
    boolean skipOkPackets;

    public void setupLogging() {
        logFinest(DatagramEncrypter.logger);
        logFinest(DatagramDecrypter.logger);
        logFinest(DatagramConnection.logger);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        OSF2FMessageFactory.init();
    }

    @After
    public void tearDown() throws Exception {
        manager1.socket.close();
        manager2.socket.close();
    }

    @Before
    public void setUp() throws Exception {
        setupLogging();
        skipOkPackets = true;
        final InetAddress localhost = InetAddress.getByName("127.0.0.1");
        manager1 = new MockDatagramConnectionManager("1");
        conn1Incoming = new LinkedList<Message>();
        conn1 = new DatagramConnection(manager1, new DatagramListener() {

            @Override
            public void sendDatagramOk(OSF2FDatagramOk osf2fDatagramOk) {
                // Fake instant reception at conn2.
                conn2.okMessageReceived();
            }

            @Override
            public InetAddress getRemoteIp() {
                return localhost;
            }

            @Override
            public void datagramDecoded(Message message, int size) {
                if (skipOkPackets && message instanceof OSF2FDatagramOk) {
                    return;
                }
                conn1Incoming.add(message);
            }

            @Override
            public String toString() {
                return "1";
            }

            @Override
            public void initDatagramConnection() {
                OSF2FDatagramInit init1 = conn1.createInitMessage();
                conn2.initMessageReceived(init1);
            }

            @Override
            public boolean isLanLocal() {
                return false;
            }
        });
        manager2 = new MockDatagramConnectionManager("2");
        conn2Incoming = new LinkedList<Message>();
        conn2 = new DatagramConnection(manager2, new DatagramListener() {

            @Override
            public void sendDatagramOk(OSF2FDatagramOk osf2fDatagramOk) {
                conn1.okMessageReceived();
            }

            @Override
            public InetAddress getRemoteIp() {
                return localhost;
            }

            @Override
            public void datagramDecoded(Message message, int size) {
                if (skipOkPackets && message instanceof OSF2FDatagramOk) {
                    return;
                }
                conn2Incoming.add(message);
            }

            @Override
            public String toString() {
                return "2";
            }

            @Override
            public void initDatagramConnection() {
                OSF2FDatagramInit init2 = conn2.createInitMessage();
                conn1.initMessageReceived(init2);
            }

            @Override
            public boolean isLanLocal() {
                return false;
            }
        });

        // Send init packet from 1 to 2.
        conn1.reInitialize();
        // And from 2 to 1.
        conn2.reInitialize();

        // This should eventually result in an UDP packet getting sent from conn
        // 2 to conn 1.
        manager1.receive();

        // When using a real friend connection the init message must arrive
        // before the OK message, but in the test the ok message will arrive
        // before so we need to trigger a manual ok message since the first one
        // was dropped.
        conn1.sendUpdOK();
        manager2.receive();

        Assert.assertEquals(conn1.sendState, SendState.ACTIVE);
        Assert.assertEquals(conn2.sendState, SendState.ACTIVE);
        Assert.assertEquals(conn1.receiveState, ReceiveState.ACTIVE);
        Assert.assertEquals(conn2.receiveState, ReceiveState.ACTIVE);
    }

    @Test
    public void testSendReceiveSimple() throws Exception {
        byte[] testData = "hello".getBytes();
        OSF2FChannelDataMsg msg = createPacket(testData);
        conn1.sendMessage(msg);
        manager2.receive();
        checkPacket(testData);
    }

    private void checkPacket(byte[] testData) {
        Assert.assertTrue(conn2Incoming.size() > 0, "No packets left.");
        OSF2FChannelDataMsg incoming = (OSF2FChannelDataMsg) conn2Incoming.removeFirst();
        byte[] inData = new byte[incoming.getPayload().remaining(SS)];
        incoming.getPayload().get(SS, inData);
        Assert.assertEquals(inData, testData);
        incoming.destroy();
    }

    private OSF2FChannelDataMsg createPacket(byte[] testData) {
        DirectByteBuffer data = DirectByteBufferPool.getBuffer(AL, MAX_DATAGRAM_PAYLOAD_SIZE);
        data.put(SS, testData);
        data.flip(SS);
        OSF2FChannelDataMsg msg = new OSF2FChannelDataMsg((byte) 0, 123, data);
        return msg;
    }

    @Test
    public void testMultipleSimple() throws Exception {
        byte[] testData1 = "hello1".getBytes();
        OSF2FChannelDataMsg msg1 = createPacket(testData1);
        byte[] testData2 = "hello2".getBytes();
        OSF2FChannelDataMsg msg2 = createPacket(testData2);
        // Make sure that both are sent together.
        synchronized (conn1.encrypter) {
            conn1.sendMessage(msg1);
            conn1.sendMessage(msg2);
        }
        // Only one call to receive (both messages must be in one packet).
        manager2.receive();
        checkPacket(testData1);
        checkPacket(testData2);
    }

    @Test
    public void testMultipleOverfull() throws Exception {
        // Queue up 3 packets, the last should not fit in the first datagram.
        int saveRoomFor = 2 * (OSF2FMessage.MESSAGE_HEADER_LEN + OSF2FChannelDataMsg.BASE_LENGTH) - 1;
        byte[] testData1 = new byte[MAX_DATAGRAM_PAYLOAD_SIZE - OSF2FChannelDataMsg.BASE_LENGTH
                - saveRoomFor];
        System.out.println(testData1.length);
        OSF2FChannelDataMsg msg1 = createPacket(testData1);
        OSF2FChannelDataMsg msg2 = createPacket(new byte[0]);
        OSF2FChannelDataMsg msg3 = createPacket(new byte[0]);
        // Make sure that all are sent together.
        synchronized (conn1.encrypter) {
            conn1.sendMessage(msg1);
            conn1.sendMessage(msg2);
            conn1.sendMessage(msg3);
        }
        // Only one call to receive (both messages must be in one packet).
        manager2.receive();
        checkPacket(testData1);
        checkPacket(new byte[0]);
        Assert.assertEquals(conn2Incoming.size(), 0);
        manager2.receive();
        checkPacket(new byte[0]);
    }

    @Test
    public void testAllMinSize() throws Exception {
        // Make sure that all are sent together.
        skipOkPackets = false;
        int packets = MAX_DATAGRAM_PAYLOAD_SIZE / (OSF2FMessage.MESSAGE_HEADER_LEN) + 1 + 1;
        synchronized (conn1.encrypter) {
            for (int i = 0; i < packets; i++) {
                conn1.sendMessage(new OSF2FDatagramOk(0));
            }
        }
        // Only one call to receive (both messages must be in one packet).
        manager2.receive();

        Assert.assertEquals(conn2Incoming.size(), packets - 1);
        manager2.receive();
        Assert.assertEquals(conn2Incoming.size(), packets);
    }

    @Test
    public void testSocketChange() throws Exception {
        byte[] testData1 = "hello1".getBytes();
        OSF2FChannelDataMsg msg1 = createPacket(testData1);
        conn1.sendMessage(msg1);
        manager2.receive();
        checkPacket(testData1);
        manager1.socketUpdated();

        byte[] testData2 = "hello2".getBytes();
        OSF2FChannelDataMsg msg2 = createPacket(testData2);
        conn1.sendMessage(msg2);
        manager2.receive();
        checkPacket(testData2);
    }

    /**
     * done: time=5.67s speed=23.53MB/s ,17.65kpps
     * 
     * @throws Exception
     */
    @Test
    public void testSendPerformance() throws Exception {
        logInfo(DatagramEncrypter.logger);
        logInfo(DatagramDecrypter.logger);
        logInfo(DatagramConnection.logger);

        long startTime = System.currentTimeMillis();
        final int packets = 200000;
        int length = MAX_DATAGRAM_PAYLOAD_SIZE - 4;

        for (int i = 0; i < packets; i++) {
            DirectByteBuffer data = DirectByteBufferPool.getBuffer(AL, length);
            OSF2FMessage message = new OSF2FChannelDataMsg((byte) 0, 1, data);
            conn1.sendMessage(message);
        }
        double mb = length * packets / (1024 * 1024.0);
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        while (conn1.sendThread.messageQueue.peek() != null) {
            Thread.sleep(10);
        }
        System.out.println(String.format("done: time=%.2fs speed=%.2fMB/s ,%.2fkpps", elapsed, mb
                / elapsed, packets / elapsed / 1000));
    }

    /**
     * received 96.8 percent
     * done: time=12.77s speed=20.22MB/s ,15.66kpps
     * 
     * @throws Exception
     */
    @Test
    public void testReceivePerformance() throws Exception {
        logInfo(DatagramEncrypter.logger);
        logInfo(DatagramDecrypter.logger);
        logInfo(DatagramConnection.logger);

        long startTime = System.currentTimeMillis();
        final int packets = 200000;
        final int length = MAX_DATAGRAM_PAYLOAD_SIZE - 4;
        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < packets; i++) {
                    DirectByteBuffer data = DirectByteBufferPool.getBuffer(AL, length);
                    OSF2FMessage message = new OSF2FChannelDataMsg((byte) 0, 1, data);
                    conn1.sendMessage(message);
                }
            }
        });
        sendThread.start();

        int packet = 0;
        try {
            for (; packet < packets; packet++) {
                manager2.receive();
                conn2Incoming.removeFirst().destroy();
            }
        } catch (java.net.SocketTimeoutException e) {
            // Expected
        }
        double mb = length * packet / (1024 * 1024.0);
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        double percentReceived = packet * 100.0 / packets;
        System.out.println(String.format("received %.1f percent", percentReceived));
        System.out.println(String.format("done: time=%.2fs speed=%.2fMB/s ,%.2fkpps", elapsed, mb
                / elapsed, packets / elapsed / 1000));

        Assert.assertTrue(percentReceived > 0.8,
                String.format("high packet loss (%.1f) percent", percentReceived));

    }
}
