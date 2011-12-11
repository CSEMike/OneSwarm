package edu.washington.cs.oneswarm.f2f;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager.RoutingListener;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelMsg;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelReset;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHandshake;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageDecoder;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageEncoder;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessageFactory;

public class OSF2FSpeedChecker {
    private final static byte[] legacy_handshake_header = new byte[OSF2FMessage.SPD_HANDSHAKE
            .getBytes().length + 1];
    private final static Logger logger = Logger.getLogger(OSF2FSpeedChecker.class.getName());

    public final static long SPEED_CHECK_TIME = 15 * 1000;

    static {
        ByteBuffer b = ByteBuffer.wrap(legacy_handshake_header);
        b.put((byte) OSF2FMessage.SPD_HANDSHAKE.length());
        b.put(OSF2FMessage.SPD_HANDSHAKE.getBytes());
    }
    private Timer checkTimer;

    private int currentId = 0;
    private LinkedList<IncomingSpeedCheckConnection> incomingConnections = new LinkedList<IncomingSpeedCheckConnection>();
    private boolean running = false;

    private BufferedWriter speedCheckLog;
    private Map<Integer, OutgoingSpeedCheck> speedChecks = new HashMap<Integer, OutgoingSpeedCheck>();

    private NetworkManager.ByteMatcher speedMatcher;

    private final GlobalManagerStats stats;

    public OSF2FSpeedChecker(GlobalManagerStats stats) {
        this.stats = stats;
        COConfigurationManager.addAndFireParameterListener("Allow.Incoming.Speed.Check",
                new ParameterListener() {
                    public void parameterChanged(String parameterName) {
                        boolean enabled = COConfigurationManager
                                .getBooleanParameter("Allow.Incoming.Speed.Check");
                        if (enabled) {
                            installSpeedCheckMatcher();
                        } else {
                            uninstallSpeedCheckMatcher();
                        }
                    }
                });
    }

    public OutgoingSpeedCheck getSpeedCheck(int testId) {
        return speedChecks.get(testId);
    }

    private void installSpeedCheckMatcher() {
        synchronized (this) {
            if (!running) {
                Thread t = new Thread(new Runnable() {

                    public void run() {
                        synchronized (OSF2FSpeedChecker.this) {
                            running = true;
                            logger.fine("installing speed check routing");
                            speedMatcher = new OsSpeedMatcher();
                            NetworkManager.getSingleton().requestIncomingConnectionRouting(
                                    speedMatcher, new RoutingListener() {
                                        public boolean autoCryptoFallback() {
                                            // TODO Auto-generated method stub
                                            return false;
                                        }

                                        public void connectionRouted(NetworkConnection connection,
                                                Object routing_data) {
                                            logger.fine("connection routed to speed checker, isTcp="
                                                    + connection.getTransport().isTCP());
                                            synchronized (incomingConnections) {
                                                incomingConnections
                                                        .add(new IncomingSpeedCheckConnection(
                                                                stats, connection));
                                            }
                                        }
                                    }, new MessageStreamFactory() {
                                        public MessageStreamDecoder createDecoder() {
                                            return new OSF2FMessageDecoder();
                                        }

                                        public MessageStreamEncoder createEncoder() {
                                            return new OSF2FMessageEncoder();
                                        }

                                    });

                        }
                    }
                });
                t.setDaemon(true);
                t.setName("OSF2F speed matcher loader");
                t.start();
                try {
                    speedCheckLog = new BufferedWriter(new FileWriter(new File("speed_check.log"),
                            true));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                checkTimer = new Timer("speed check connection checker", true);
                checkTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        StringBuilder toLog = new StringBuilder();
                        synchronized (incomingConnections) {
                            for (Iterator<IncomingSpeedCheckConnection> iterator = incomingConnections
                                    .iterator(); iterator.hasNext();) {
                                IncomingSpeedCheckConnection c = iterator.next();
                                if (c.isTimedOut()) {
                                    c.close();
                                }
                                if (c.isClosed()) {
                                    toLog.append(System.currentTimeMillis()
                                            + " "
                                            + c.getRemoteIp()
                                            + " "
                                            + new String(Base64.encode(c.incomingHandshake
                                                    .getFlags())) + " " + c.remoteLocalEstimate
                                            + " " + c.remoteRemoteEstimate + " "
                                            + c.getAverageSpeed() + " " + c.getSecondHalfSpeed()
                                            + " " + c.getTimeStamps() + "\n");
                                    iterator.remove();
                                }
                            }
                            logger.finest("speed check connection cleanup done, currently active="
                                    + incomingConnections.size());
                        }
                        try {
                            speedCheckLog.append(toLog.toString());
                            speedCheckLog.flush();
                            logger.finest(toLog.toString());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    }
                }, 30 * 1000, 10 * 1000);
            }
        }
    }

    public int performSpeedCheck() {
        currentId++;
        speedChecks.put(currentId, new OutgoingSpeedCheck(stats));
        return currentId;
    }

    private void uninstallSpeedCheckMatcher() {
        synchronized (this) {
            if (running && speedMatcher != null) {
                logger.fine("removing speed check routing");
                NetworkManager.getSingleton().cancelIncomingConnectionRouting(speedMatcher);
                running = false;
                if (checkTimer != null) {
                    checkTimer.cancel();
                }
                if (speedCheckLog != null) {
                    try {
                        speedCheckLog.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class IncomingSpeedCheckConnection implements MessageQueueListener {
        private final static long TIMEOUT_MS = 60 * 1000;

        private boolean closed = false;
        final NetworkConnection connection;
        private OSF2FHandshake incomingHandshake;

        private long lastMessageTime;

        private LinkedList<Integer> messageTimes = new LinkedList<Integer>();
        private int packetSize = 0;
        private int remoteLocalEstimate = -1;
        private int remoteRemoteEstimate = -1;
        private long startTime;
        private final GlobalManagerStats stats;

        public IncomingSpeedCheckConnection(GlobalManagerStats stats, NetworkConnection conn) {
            this.startTime = System.currentTimeMillis();
            this.connection = conn;
            this.stats = stats;
            /*
             * register for notifications about incoming messages
             */
            connection.getIncomingMessageQueue().registerQueueListener(this);

            connection.connect(true, new ConnectionListener() {

                public void connectFailure(Throwable failureMsg) {
                    connection.close();
                }

                public void connectStarted() {
                }

                public void connectSuccess(ByteBuffer remainingInitialData) {
                    NetworkManager.getSingleton().upgradeTransferProcessing(connection, null);
                    logger.finest("incoming speed check connected: " + getRemoteIp());
                }

                public void exceptionThrown(Throwable error) {
                    logger.warning("got exception in incoming speed test: " + error.getMessage());
                    error.printStackTrace();
                    connection.close();
                }

                public String getDescription() {
                    return "speed connection listener";
                }
            });
            NetworkManager.getSingleton().startTransferProcessing(connection);

        }

        public void close() {
            if (!closed) {
                closed = true;
                logger.fine("closing incoming connection");
                connection.close();
            }
        }

        public void dataBytesReceived(int byteCount) {
            if (stats != null) {
                stats.protocolBytesReceived(byteCount, false);
            }
        }

        public long getAverageSpeed() {
            long bytesTransfered = messageTimes.size() * packetSize;
            if (bytesTransfered == 0) {
                return 0;
            }
            long timeTaken = messageTimes.getLast();
            return (1000 * bytesTransfered) / timeTaken;
        }

        public String getRemoteIp() {
            return connection.getEndpoint().getNotionalAddress().getAddress().getHostAddress();
        }

        public long getSecondHalfSpeed() {
            long bytesTransfered = (messageTimes.size() * packetSize) / 2;
            if (bytesTransfered == 0) {
                return 0;
            }
            long timeTaken = messageTimes.getLast() - messageTimes.get(messageTimes.size() / 2);
            return (1000 * bytesTransfered) / timeTaken;
        }

        public String getTimeStamps() {
            StringBuilder b = new StringBuilder();
            for (Integer l : messageTimes) {
                b.append(l + " ");
            }
            return b.toString();
        }

        public boolean isClosed() {
            return closed;
        }

        public boolean isTimedOut() {
            return System.currentTimeMillis() - lastMessageTime > TIMEOUT_MS;
        }

        public boolean messageReceived(Message message) {
            logger.finest("incoming speed packet: " + message.getDescription());
            lastMessageTime = System.currentTimeMillis();
            if (incomingHandshake == null) {
                if (message instanceof OSF2FHandshake) {
                    incomingHandshake = (OSF2FHandshake) message;
                } else {
                    logger.warning("incoming connection not started with handshake");
                    close();
                }
            } else if (message instanceof OSF2FChannelReset) {
                OSF2FChannelReset m = (OSF2FChannelReset) message;
                if (remoteLocalEstimate == -1) {
                    remoteLocalEstimate = m.getChannelId();
                } else {
                    remoteRemoteEstimate = m.getChannelId();
                    logger.finer("estimate of " + getRemoteIp() + " remote_local_estimate="
                            + remoteLocalEstimate + " remote_remote=" + remoteRemoteEstimate);
                    close();
                }
            } else if ((message instanceof OSF2FChannelMsg)) {
                OSF2FChannelMsg m = (OSF2FChannelMsg) message;
                if (packetSize == 0) {
                    packetSize = m.getMessageSize();
                }
                if (m.getMessageSize() != packetSize) {
                    logger.warning("got different size payload packet");
                    close();
                    return true;
                }
                /*
                 * ok all is good, send back a timestamp to the other side
                 */
                int relTime = (int) (System.currentTimeMillis() - startTime);
                messageTimes.add(relTime);
                /*
                 * we only need to send an int over, channel reset is the
                 * smallest message (5+4 bytes), a bit of a hack but wth...
                 */
                connection.getOutgoingMessageQueue().addMessage(
                        new OSF2FChannelReset(OSF2FMessage.CURRENT_VERSION, relTime), false);
            } else {
                logger.warning("incoming speed check connection unknown packet");
                close();
                return true;
            }
            return true;
        }

        public void protocolBytesReceived(int byteCount) {
            if (stats != null) {
                stats.protocolBytesReceived(byteCount, false);
            }
        };
    }

    static class OsSpeedMatcher implements NetworkManager.ByteMatcher {

        private final int size;

        public OsSpeedMatcher() {
            this.size = legacy_handshake_header.length;
        }

        public byte[][] getSharedSecrets() {

            return null;
        }

        public int getSpecificPort() {
            return (-1);
        }

        public Object matches(TransportHelper transport, ByteBuffer to_compare, int port) {
            // logger.finest("looking at: " + new String(to_compare.array()));
            int old_limit = to_compare.limit();
            to_compare.limit(to_compare.position() + maxSize());
            boolean matches = to_compare.equals(ByteBuffer.wrap(legacy_handshake_header));
            to_compare.limit(old_limit); // restore buffer structure
            return matches ? "" : null;
        }

        public int matchThisSizeOrBigger() {
            return (maxSize());
        }

        public int maxSize() {
            return size;
        }

        public Object minMatches(TransportHelper transport, ByteBuffer to_compare, int port) {
            return (matches(transport, to_compare, port));
        }

        public int minSize() {
            return maxSize();
        }
    }

    static class OutgoingSpeedCheck {

        private static final String SPEEDTEST_URL = "http://" + Constants.VERSION_SERVER_V4
                + "/speedcheck";

        private List<OutgoingSpeedCheckConnection> connections = new LinkedList<OutgoingSpeedCheckConnection>();

        private int localEstimate = -1;

        private int remoteEstimate = -1;
        private final long startTime;

        private StringBuffer log = new StringBuffer();
        private String lastLine = "";

        private void log(String str) {
            log.append(str + "\n");
            lastLine = str;
            logger.finer(str);
        }

        public void close() {
            for (OutgoingSpeedCheckConnection c : connections) {
                try {
                    c.closeSocket();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public OutgoingSpeedCheck(GlobalManagerStats stats) {
            this.startTime = System.currentTimeMillis();
            log("starting speed test");
            InetAddress addr;
            try {

                log("connecting to " + SPEEDTEST_URL);
                HttpURLConnection conn = (HttpURLConnection) new URL(SPEEDTEST_URL)
                        .openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(
conn.getInputStream()));

                String line;
                List<String> hosts = new LinkedList<String>();
                while ((line = in.readLine()) != null && !line.equals("")) {
                    hosts.add(line);
                }

                for (String s : hosts) {
                    String[] split = s.split(" ");
                    String host = split[0];
                    int port = Integer.parseInt(split[1]);
                    OutgoingSpeedCheckConnection c = new OutgoingSpeedCheckConnection(stats, host,
                            port);
                    connections.add(c);
                    Thread t = new Thread(c);
                    t.setName("speed check connection: " + s);
                    t.setDaemon(true);
                    t.start();
                }
                log("Speed test running to " + hosts.size() + " servers");

                final Timer killTimer = new Timer("speed check killer", true);
                // kill it 5 sec after we are done
                killTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            localEstimate = getLocalEstimate();
                            remoteEstimate = getRemoteEstimate();

                            for (int i = 0; i < 10; i++) {
                                boolean allDone = true;
                                for (OutgoingSpeedCheckConnection c : connections) {
                                    if (!c.isCompleted()) {
                                        allDone = false;
                                    }
                                }

                                if (allDone) {
                                    break;
                                } else {
                                    Thread.sleep(1000);
                                }
                            }

                            for (OutgoingSpeedCheckConnection c : connections) {
                                try {
                                    c.sendReport((int) localEstimate);
                                    c.sendReport((int) remoteEstimate);
                                    // remote side should close conn after
                                    // getting
                                    // second report
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            connections.clear();

                            log("speed test completed");
                            logger.fine("done clearing connections, local=" + localEstimate
                                    + " remote=" + remoteEstimate);
                            killTimer.cancel();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }, 1000 + SPEED_CHECK_TIME);
            } catch (UnknownHostException e1) {
                log("unable to resolv host: " + SPEEDTEST_URL);
            } catch (IOException e) {
                log("unable to get server list");
            }
        }

        public double getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0, (1.0 * elapsed) / (SPEED_CHECK_TIME));
        }

        public int getLocalEstimate() {
            if (localEstimate != -1) {
                return localEstimate;
            }
            int totalSpeed = 0;
            synchronized (connections) {
                for (OutgoingSpeedCheckConnection c : connections) {
                    totalSpeed += c.getLocalEstimate();
                }
            }
            return totalSpeed;
        }

        public int getRemoteEstimate() {
            if (remoteEstimate != -1) {
                return remoteEstimate;
            }
            int totalSpeed = 0;
            synchronized (connections) {
                for (OutgoingSpeedCheckConnection c : connections) {
                    totalSpeed += c.getRemoteEstimate();
                }
            }
            return totalSpeed;
        }

        public int getGoodServers() {
            int num = 0;
            for (OutgoingSpeedCheckConnection c : connections) {
                if (c.getRemoteEstimate() > 0) {
                    num++;
                }
            }
            return num;
        }

        public int getServerCount() {
            return connections.size();
        }

        public boolean isClosed() {
            for (OutgoingSpeedCheckConnection c : connections) {
                if (!c.isClosed()) {
                    return false;
                }
            }
            return true;

        }

        public boolean isCompleted() {
            for (OutgoingSpeedCheckConnection c : connections) {
                if (!c.isCompleted()) {
                    return false;
                }
            }
            return true;
        }

        public static void main(String[] args) throws SecurityException, FileNotFoundException,
                IOException, InterruptedException {
            LogManager.getLogManager().readConfiguration(
                    new FileInputStream("./logging.properties"));

            // List<String> speedTestHosts = new LinkedList<String>();
            // // speedTestHosts.add("127.0.0.1:11338");
            // speedTestHosts.add("swede:46549");
            // speedTestHosts.add("jermaine:48401");
            OutgoingSpeedCheck check = new OutgoingSpeedCheck(null);// ,
            // speedTestHosts);

            while (!check.isClosed()) {
                System.out.println(("" + check.getProgress()).substring(0, 3) + ": local="
                        + check.getLocalEstimate() + " remote=" + check.getRemoteEstimate());
                Thread.sleep(1000);
            }
            System.out.println("finished: completed=" + check.isCompleted() + " closed="
                    + check.isClosed());
            System.out.println("local=" + check.getLocalEstimate() + " remote="
                    + check.getRemoteEstimate());

        }

    }

    static class OutgoingSpeedCheckConnection implements Runnable {
        private boolean closed = false;
        private boolean completed = false;
        private final String host;
        private List<Integer> localTimeStamps = new Vector<Integer>();
        private OutputStream out;
        private final int port;
        private List<Integer> remoteTimeStamps = new Vector<Integer>();
        private Socket s;
        private boolean sendCompleted = false;
        private final GlobalManagerStats stats;

        private OutgoingSpeedCheckConnection(GlobalManagerStats stats, String host, int port) {
            super();
            this.stats = stats;
            this.port = port;
            this.host = host;
        }

        public void closeSocket() throws IOException {
            if (s != null) {
                s.close();
            }
        }

        public long getLocalEstimate() {
            return getSpeed(localTimeStamps);
        }

        public long getRemoteEstimate() {
            return getSpeed(remoteTimeStamps);
        }

        public boolean isClosed() {
            return closed;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void run() {
            long startTime = System.currentTimeMillis();

            try {
                s = new Socket(InetAddress.getByName(host), port);

                out = s.getOutputStream();
                final DataInputStream in = new DataInputStream(s.getInputStream());
                Thread readThread = new Thread(new Runnable() {
                    public void run() {
                        logger.fine("running speed check to " + host + ":" + port);
                        try {
                            /*
                             * read the time stamps
                             */
                            while (!sendCompleted
                                    || localTimeStamps.size() > remoteTimeStamps.size()) {
                                logger.finest("reading again: complete=" + sendCompleted
                                        + " sizediff="
                                        + (localTimeStamps.size() - remoteTimeStamps.size()));
                                // read message len
                                int len = in.readInt();
                                logger.finest("message len: " + len);
                                byte id = in.readByte();
                                logger.finest("message id: " + id);
                                int timestamp = in.readInt();
                                logger.finest("message timestamp: " + timestamp);
                                remoteTimeStamps.add(timestamp);
                                if (stats != null) {
                                    stats.protocolBytesReceived(9, false);
                                }
                            }
                            logger.fine("incoming reader complete");
                            completed = true;
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            closed = true;
                        }
                    }
                });
                readThread.setDaemon(true);
                readThread.setName("speed check outgoing: inputreader " + host + ":" + port);
                readThread.start();

                /*
                 * first, send the handshake
                 */
                byte[] rand = new byte[8];
                new Random().nextBytes(rand);
                out.write(getHsBytes(rand));

                /*
                 * second, send the dummy data
                 */
                byte[] dummyPacket = getDummyPacket();
                long elapsed;
                while ((elapsed = System.currentTimeMillis() - startTime) < SPEED_CHECK_TIME) {
                    logger.finest("sending " + dummyPacket.length + " bytes");
                    localTimeStamps.add((int) elapsed);
                    out.write(dummyPacket);
                    if (stats != null) {
                        stats.protocolBytesSent(dummyPacket.length, false);
                    }
                }
                sendCompleted = true;
                logger.fine("send completed: " + host + ":" + port);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                closed = true;
            }
        }

        public void sendReport(int speed) throws IOException {
            if (!closed) {
                out.write(getReportBytes(speed));
            }
        }

        private static byte[] getDummyPacket() throws IOException {
            DirectByteBuffer dbuffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                    OSF2FMessage.MAX_PAYLOAD_SIZE);
            while (dbuffer.hasRemaining(DirectByteBuffer.SS_NET)) {
                dbuffer.put(DirectByteBuffer.SS_NET, (byte) 0);
            }
            dbuffer.flip(DirectByteBuffer.SS_NET);

            DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(
                    new OSF2FChannelDataMsg(OSF2FMessage.CURRENT_VERSION, 0, dbuffer)).getRawData();
            ByteBuffer buf = ByteBuffer.allocate(OSF2FMessage.MAX_MESSAGE_SIZE + 20);
            for (int i = 0; i < data.length; i++) {
                buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
            }
            buf.flip();
            byte[] b = new byte[buf.remaining()];
            buf.get(b);
            return b;
        }

        private static byte[] getHsBytes(byte[] rand) {
            ByteBuffer buf = ByteBuffer.allocate(2000);
            buf.put((byte) OSF2FMessage.SPD_HANDSHAKE.getBytes().length);
            buf.put(OSF2FMessage.SPD_HANDSHAKE.getBytes());
            buf.put(rand);
            buf.flip();
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            return data;
        }

        private static byte[] getReportBytes(int speed) throws IOException {
            DirectByteBuffer[] data = OSF2FMessageFactory.createOSF2FRawMessage(
                    new OSF2FChannelReset(OSF2FMessage.CURRENT_VERSION, speed)).getRawData();
            ByteBuffer buf = ByteBuffer.allocate(20);
            for (int i = 0; i < data.length; i++) {
                buf.put(data[i].getBuffer(DirectByteBuffer.SS_MSG));
            }
            buf.flip();
            byte[] b = new byte[buf.remaining()];
            buf.get(b);
            return b;
        }

        private static long getSpeed(List<Integer> timeStamps) {
            long bytesTransfered = timeStamps.size() * OSF2FMessage.MAX_MESSAGE_SIZE;
            if (bytesTransfered == 0) {
                return 0;
            }
            if (timeStamps.size() < 2) {
                return 0;
            }
            long timeTaken = timeStamps.get(timeStamps.size() - 1);
            if (timeTaken <= 0) {
                return 0;
            }
            return (1000 * bytesTransfered) / timeTaken;
        }
    }
}
