package edu.washington.cs.oneswarm.ui.gwt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Hex;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.ipfilter.IpRange;
import org.gudy.azureus2.core3.ipfilter.impl.IpRangeImpl;
import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.EventWaiter;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.osssl.OneSwarmSslTransportHelperFilterStream;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;

public class RemoteAccessForward {
    private static Logger logger = Logger.getLogger(RemoteAccessForward.class.getName());

    private final ConcurrentHashMap<ConnectionForwarder, Long> connectionForwarders;
    private final NetworkManager.ByteMatcher matcher;
    private volatile boolean running = false;

    // private final GlobalManagerStats stats;

    public RemoteAccessForward() {

        connectionForwarders = new ConcurrentHashMap<ConnectionForwarder, Long>();
        matcher = new HttpSSLMatcher();
        logger.fine("started remote access forward");
        Timer t = new Timer("Remote Access idle checker", true);
        t.schedule(new ForwardIdleTimeoutChecker(), 0, 10 * 1000);

    }

    public synchronized void start() {
        if (!running) {
            running = true;
            String type = COConfigurationManager.getStringParameter(
                    OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_KEY,
                    OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT);
            String filter = COConfigurationManager.getStringParameter(
                    OneSwarmConstants.REMOTE_ACCESS_LIMIT_IPS_KEY, "");
            try {
                NetworkManager.getSingleton()
                        .requestIncomingConnectionRouting(matcher,
                                new OsNetworkRouterListener(type, filter),
                                new DummyMessageDecoderFactory());
            } catch (OneSwarmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public synchronized void stop() {
        if (running) {
            for (Iterator<ConnectionForwarder> iterator = connectionForwarders.keySet().iterator(); iterator
                    .hasNext();) {
                ConnectionForwarder cf = iterator.next();
                cf.stop(false);
                iterator.remove();
            }
            NetworkManager.getSingleton().cancelIncomingConnectionRouting(matcher);
            running = false;
        }
    }

    private static class DummyMessageDecoderFactory implements MessageStreamFactory {
        public MessageStreamDecoder createDecoder() {
            return new MessageStreamDecoder() {

                public ByteBuffer destroy() {
                    return null;
                }

                public int getDataBytesDecoded() {
                    return 0;
                }

                public int getPercentDoneOfCurrentMessage() {
                    return 0;
                }

                public int getProtocolBytesDecoded() {
                    return 0;
                }

                public void pauseDecoding() {
                }

                public int performStreamDecode(Transport transport, int max_bytes)
                        throws IOException {
                    return 0;
                }

                public Message[] removeDecodedMessages() {
                    return null;
                }

                public void resumeDecoding() {
                }
            };
        }

        public MessageStreamEncoder createEncoder() {
            return new MessageStreamEncoder() {

                public RawMessage[] encodeMessage(Message message) {
                    return null;
                }
            };
        }
    }

    public List<Map<String, String>> getRemoteAccessStats() {
        List<Map<String, String>> stats = new LinkedList<Map<String, String>>();

        for (ConnectionForwarder f : connectionForwarders.keySet()) {
            Map<String, String> s = new HashMap<String, String>();

            s.put("remote_ip", f.getRemoteIp());

            String remoteDns = getRemoteDomain(f.getRemoteHostName());
            if (remoteDns != null) {
                s.put("remote_dns", remoteDns);
            }
            s.put("age", "" + f.getAge());
            s.put("total_uploaded", "" + f.getUploadTotal());
            s.put("total_downloaded", "" + f.getDownloadTotal());
            s.put("upload_rate", "" + f.getUploadRate());
            s.put("download_rate", f.getDownloadRate() + "");
            s.put("lan_local", "" + f.isLanLocal());
            s.put("idle_out", f.getOutIdleTime() + "");
            s.put("idle_in", f.getInIdleTime() + "");
            stats.add(s);
        }

        return stats;
    }

    private static String getRemoteDomain(String hostname) {
        if (hostname != null) {
            String[] s = hostname.split("\\.");
            if (s.length > 2) {
                String last3 = s[s.length - 3] + "." + s[s.length - 2] + "." + s[s.length - 1];
                if (s.length > 3) {
                    last3 = "..." + last3;
                }
                return last3;
            } else if (s.length > 1) {
                return s[s.length - 2] + "." + s[s.length - 1];
            }
        }
        return null;
    }

    private class ConnectionForwarder {
        private long started;
        private static final int BUFFER_SIZE = 5000;
        private static final int READ_POLL_RATE = 100;
        private static final int IDLE_TIME_OUT = 60 * 1000;

        private boolean quit = false;
        private Socket socket;
        private OutputStream sslToWebStream;
        private final Transport transport;
        private final NetworkConnection connection;
        private InputStream webToSslStream;
        private final Average upload = Average.getInstance(1000, 10);
        private final Average download = Average.getInstance(1000, 10);
        private final String remoteIp;
        private String hostname;

        public ConnectionForwarder(NetworkConnection connection) {
            this.transport = connection.getTransport();
            this.connection = connection;
            lanLocal = connection.isLANLocal();
            logger.fine("created remote access forwarder");
            this.remoteIp = connection.getEndpoint().getNotionalAddress().getAddress()
                    .getHostAddress();
            this.hostname = remoteIp;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        hostname = InetAddress.getByName(remoteIp).getCanonicalHostName();
                        logger.finer("resolved remote host: " + remoteIp + "(" + hostname + ")");
                        if (hostname != null) {
                            String[] s = hostname.split("\\.");
                            boolean onlyNumbers = true;
                            for (String e : s) {
                                try {
                                    int i = Integer.parseInt(e);
                                    logger.finer("resolved " + e + " to " + i);
                                } catch (NumberFormatException ex) {
                                    onlyNumbers = false;
                                }
                            }
                            if (onlyNumbers) {
                                logger.finer("hostname only numbers, setting to null");
                                hostname = null;
                            }
                        }
                    } catch (Throwable t) {
                        // this is ok
                        t.printStackTrace();
                    }
                }
            });
            t.setName("ip resolver thread");
            t.setDaemon(true);
            t.start();
        }

        public boolean isLanLocal() {
            return lanLocal;
        }

        public String getRemoteHostName() {
            return hostname;
        }

        private final GwtWebToSSL gwtWebToSSL = new GwtWebToSSL();
        private final SSLToGwtWeb sslToGwtWeb = new SSLToGwtWeb();
        private final boolean lanLocal;

        public void start() throws UnknownHostException, IOException {
            started = System.currentTimeMillis();
            socket = new Socket();

            socket.connect(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),
                    Constants.LOCAL_WEB_SERVER_PORT_AUTH));
            logger.fine("connected to:" + socket.getInetAddress().getHostAddress() + ":"
                    + socket.getPort());
            webToSslStream = socket.getInputStream();
            sslToWebStream = socket.getOutputStream();

            Thread t = new Thread(gwtWebToSSL);
            t.setName("GwtToSSLMover");
            t.setDaemon(true);
            t.start();

            Thread t2 = new Thread(sslToGwtWeb);
            t2.setName("SSLToGwtWebMover");
            t2.setDaemon(true);
            t2.start();
        }

        public long getDownloadRate() {
            return download.getAverage();
        }

        public long getUploadRate() {
            return upload.getAverage();
        }

        public long getUploadTotal() {
            return gwtWebToSSL.getTotalWritten();
        }

        public long getDownloadTotal() {
            return sslToGwtWeb.getTotalWritten();
        }

        public long getAge() {
            return System.currentTimeMillis() - started;
        }

        public long getOutIdleTime() {
            return gwtWebToSSL.getIdleTime();
        }

        public long getInIdleTime() {
            return sslToGwtWeb.getIdleTime();
        }

        public boolean isTimedOut() {
            if (getOutIdleTime() > IDLE_TIME_OUT && getInIdleTime() > IDLE_TIME_OUT) {
                return true;
            }

            return false;
        }

        public String getRemoteIp() {
            return remoteIp;
        }

        public void stop(boolean deregister) {
            if (!quit) {
                logger.fine("forwarder stopped, ->ssl: " + sslToGwtWeb.getTotalWritten()
                        + " ->gwt: " + gwtWebToSSL.getTotalWritten());
            }
            this.quit = true;

            connection.close();
            if (deregister) {
                connectionForwarders.remove(this);

            }
        }

        private class GwtWebToSSL implements Runnable {
            long lastWriteTime = System.currentTimeMillis();
            long totalRead = 0;
            long totalWritten = 0;

            public long getTotalWritten() {
                return totalWritten;
            }

            public long getIdleTime() {
                return System.currentTimeMillis() - lastWriteTime;
            }

            public void run() {
                byte[] readBuffer = new byte[BUFFER_SIZE];
                ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
                writeBuffer.clear();
                writeBuffer.flip();
                ByteBuffer[] writeBuffers = new ByteBuffer[] { writeBuffer };
                try {
                    while (!quit) {
                        // System.out.println("->SSL: remaining: " +
                        // writeBuffer.remaining());
                        if (writeBuffer.remaining() == 0) {
                            // System.out.println("->SSL: reading");
                            // int available = webToSslStream.available();
                            // int len = 0;
                            // if (available > 0) {
                            // /*
                            // * prefer to read whatever is available even if
                            // * it won't fill the entire buffer
                            // */
                            //
                            // len = webToSslStream.read(readBuffer, 0,
                            // Math.min(available, readBuffer.length));
                            // } else {
                            // /*
                            // * else, block until you can read one byte
                            // */
                            // len = webToSslStream.read(readBuffer, 0, 1);
                            // }

                            int len = webToSslStream.read(readBuffer);
                            if (len == -1) {
                                stop(true);
                                return;
                            }
                            totalRead += len;
                            logger.finest("->SSL: read: " + len + " total: " + totalRead);
                            writeBuffer.clear();
                            writeBuffer.put(readBuffer, 0, len);
                            writeBuffer.flip();
                            // System.out.println("->SSL: can write " +
                            // writeBuffer.remaining());
                            // System.out.println("'" + new String(readBuffer,
                            // 0, len, "UTF-8") + "'");
                        }
                        EventWaiter waiter = new EventWaiter();
                        if (!transport.isReadyForWrite(waiter)) {
                            // System.out.println("->SSL: waiting for transport")
                            // ;
                            waiter.waitForEvent(READ_POLL_RATE);
                        }
                        if (transport.isReadyForWrite(null)) {

                            long len = transport.write(writeBuffers, 0, 1);
                            // rateHandler.bytesProcessed((int) len);
                            totalWritten += len;
                            // stats.protocolBytesSent((int) len, lanLocal);
                            upload.addValue(len);
                            logger.finest("->SSL: wrote: " + len + " total: " + totalWritten);
                            lastWriteTime = System.currentTimeMillis();
                        }
                    }
                } catch (Throwable e) {
                    logger.fine("closing connection forwarder, got exception:" + e.getMessage());
                }
                ConnectionForwarder.this.stop(true);
            }
        }

        private class SSLToGwtWeb implements Runnable {
            private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            private final ByteBuffer[] readBuffers = new ByteBuffer[] { readBuffer };
            private final byte[] writeBuffer = new byte[BUFFER_SIZE];
            long totalRead = 0;
            long totalWritten = 0;
            long lastReadTime = System.currentTimeMillis();

            public long getTotalWritten() {
                return totalWritten;
            }

            public long getIdleTime() {
                return System.currentTimeMillis() - lastReadTime;
            }

            public void run() {
                try {
                    while (!quit) {
                        EventWaiter waiter = new EventWaiter();
                        if (!transport.isReadyForRead(waiter)) {
                            // System.out.println(
                            // "->GWT: transport not ready, waiting");
                            waiter.waitForEvent(READ_POLL_RATE);
                        }
                        if (transport.isReadyForRead(null)) {
                            // cast is safe, len can't be more than BUFFER_SIZE
                            readBuffer.clear();
                            // System.out.println("->GWT: trying to read: " +
                            // readBuffer.remaining());
                            // int len = (int) transport.read(readBuffers, 0,
                            // 1);
                            transport.read(readBuffers, 0, 1);
                            int len = readBuffer.position();
                            lastReadTime = System.currentTimeMillis();

                            // stats.protocolBytesReceived(len, lanLocal);
                            download.addValue(len);

                            totalRead += len;
                            logger.finest("->GWT: read: " + len + " total: " + totalRead);
                            if (len > 1) {
                                readBuffer.flip();
                                // System.out.println("len=" + len +
                                // " remaining=" + readBuffer.remaining());
                                readBuffer.get(writeBuffer, 0, readBuffer.remaining());
                                // System.out.println("->GWT: copied " + len +
                                // " bytes");
                                sslToWebStream.write(writeBuffer, 0, len);
                                totalWritten += len;
                                logger.finest("->GWT: wrote " + len + " total: " + totalWritten);

                                // System.out.println("'" + new
                                // String(writeBuffer, 0, len, "UTF-8") + "'");
                            } else {
                                // System.out.println(
                                // "->GWT: read 0 bytes, waiting for selects");
                            }
                        } else {
                            // nop
                        }

                    }
                } catch (Throwable e) {
                    logger.fine("->GWT: transport/socket closed: " + e.getMessage());
                    // e.printStackTrace();
                }
                ConnectionForwarder.this.stop(true);
            }
        }

    }

    private static class HttpSSLMatcher implements NetworkManager.ByteMatcher {
        private final static String GET_HANDSHAKE = new String("GET /");
        private final static String POST_HANDSHAKE = new String("POST /");

        public HttpSSLMatcher() {

        }

        public byte[][] getSharedSecrets() {
            return null;
        }

        public int getSpecificPort() {
            return (ConfigurationManager.getInstance().getIntParameter("TCP.Listen.Port"));
        }

        public Object matches(TransportHelper transport, ByteBuffer to_compare, int port) {

            int old_limit = to_compare.limit();
            int old_pos = to_compare.position();
            byte[] compareBytes = new byte[to_compare.remaining()];
            to_compare.get(compareBytes);

            String compareString = null;
            try {
                compareString = new String(compareBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            logger.finer("checking " + compareString.length() + " chars for http remote access:");
            // if (compareString.contains("\n")) {
            // String[] split = compareString.split("\\n");
            // System.err.println("checking http match for: " + split[0]);
            // }
            logger.finer("'" + compareString + "'");
            logger.finer("'" + new String(Hex.encode(compareBytes)) + "'");
            boolean matches = compareString.startsWith(GET_HANDSHAKE)
                    || compareString.startsWith(POST_HANDSHAKE);
            if (matches) {
                logger.finer("matches http");
                return "";
            } else {
                logger.finer("does not match http");
                to_compare.limit(old_limit); // restore buffer structure
                to_compare.position(old_pos);
                return null;
            }
        }

        public int matchThisSizeOrBigger() {
            return Math.max(GET_HANDSHAKE.length(), POST_HANDSHAKE.length());
        }

        public int maxSize() {
            return matchThisSizeOrBigger();
        }

        public Object minMatches(TransportHelper transport, ByteBuffer to_compare, int port) {
            return (matches(transport, to_compare, port));
        }

        public int minSize() {
            return matchThisSizeOrBigger();
        }
    }

    static final String redirect = "<html><HEAD>\r\n" + "<script language=\"javascript\">\r\n"
            + "if (document.location.protocol != \"https:\")\r\n" + "{\r\n"
            + "document.location.href = \"https://\" + document.location.host;\r\n" + "};\r\n"
            + "</script>\r\n" + "<title>OneSwarm Remote Acess</title>\r\n" + "</HEAD>\r\n"
            + "<body>\r\n" + "<p>Remote access requires the use of SSL.</p></body></html>\r\n";

    static final String accessDenied = "<html><HEAD>\r\n"
            + "<title>OneSwarm Remote Acess</title>\r\n" + "</HEAD>\r\n" + "<body>\r\n"
            + "<h1>Access Denied</h1><p>Connections from ==IP== not allowed</p></body></html>\r\n";

    private class OsNetworkRouterListener implements NetworkManager.RoutingListener {

        private final String ipFilterType;
        private final List<IpRange> allowedIpRanges;

        public OsNetworkRouterListener(String ipFilterType, String ipFilterString)
                throws OneSwarmException {
            this.ipFilterType = ipFilterType;
            if (OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE.equals(ipFilterType)) {
                this.allowedIpRanges = createIpFilter(ipFilterString);
            } else {
                this.allowedIpRanges = null;
            }
        }

        public boolean autoCryptoFallback() {
            return (false);
        }

        public void connectionRouted(NetworkConnection connection, Object routing_data) {
            boolean allowed = false;
            // first check if filters
            String remoteIp = connection.getEndpoint().getNotionalAddress().getAddress()
                    .getHostAddress();

            if (OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT.equals(this.ipFilterType)) {
                allowed = true;
            } else if (OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_LAN.equals(this.ipFilterType)) {
                allowed = connection.isLANLocal();
                logger.fine("remote access connection ip-local: " + allowed);
            } else if (OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE.equals(this.ipFilterType)) {
                allowed = isInRange(allowedIpRanges, remoteIp);
            } else {
                Debug.out("unknown ip filter type");
            }

            if (!allowed) {
                try {
                    byte[] message = accessDenied.replaceAll("==IP==", remoteIp).getBytes("UTF-8");
                    ByteBuffer b = ByteBuffer.allocate(message.length);
                    b.put(message);
                    b.flip();
                    connection.getTransport().write(new ByteBuffer[] { b }, 0, 1);
                    connection.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                logger.fine("connection closed due to IP range limits: " + connection);
                return;
            }

            if (!connection.getTransport().getEncryption()
                    .startsWith(OneSwarmSslTransportHelperFilterStream.SSL_NAME)) {
                logger.fine("remote OSGWT ui connection without SSL: " + connection + " ("
                        + connection.getTransport().getEncryption() + "), redirecting");
                try {
                    byte[] errorMessage = redirect.getBytes("UTF-8");
                    ByteBuffer b = ByteBuffer.allocate(errorMessage.length);
                    b.put(errorMessage);
                    b.flip();
                    connection.getTransport().write(new ByteBuffer[] { b }, 0, 1);
                    connection.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return;
            }

            logger.fine("Incoming connection from [" + connection
                    + "] successfully routed to OneSwarm GWT Remote: encr: "
                    + connection.getTransport().getEncryption());
            try {
                ConnectionForwarder connectionForwarder = new ConnectionForwarder(connection);
                connectionForwarders.put(connectionForwarder, System.currentTimeMillis());
                connectionForwarder.start();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    public static List<IpRange> createIpFilter(String filterString) throws OneSwarmException {
        List<IpRange> allowedRanges = new LinkedList<IpRange>();
        if (filterString == null) {
            return allowedRanges;
        }
        filterString = filterString.trim();
        if (filterString.length() == 0) {
            return allowedRanges;
        }
        /*
		 *
		 */
        String[] split = filterString.split(",");
        for (String s : split) {
            s = s.trim();
            String baseProblem = "Problem occured when processing '" + s + "': ";
            if (s.contains("-")) {
                String[] ips = s.split("-");
                if (ips.length != 2) {
                    throw new OneSwarmException(baseProblem + "- detected more than once");
                }
                String ip = null;
                InetAddress from = null;
                InetAddress to = null;
                try {
                    ip = ips[0].trim();
                    from = InetAddress.getByName(ip);
                    ip = ips[1].trim();
                    to = InetAddress.getByName(ip);
                } catch (UnknownHostException e) {
                    throw new OneSwarmException(baseProblem + " unable to parse ip: '" + ip + "':"
                            + e.getMessage());
                }
                IpRange r = new IpRangeImpl("", from.getHostAddress(), to.getHostAddress(), true);
                if (!r.isValid()) {
                    throw new OneSwarmException(baseProblem + " invalid ip range");
                }
                allowedRanges.add(r);
            } else if (s.contains("/")) {
                String[] parts = s.trim().split("/");
                if (parts.length != 2) {
                    throw new OneSwarmException(baseProblem + "/ detected more than once");
                }

                int mask = -1;
                try {
                    mask = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (mask < 0 || mask > 32) {
                    throw new OneSwarmException(baseProblem + "invalid mask, '" + parts[1] + "'");
                }
                InetAddress baseIp = null;
                try {
                    baseIp = InetAddress.getByName(parts[0]);
                } catch (UnknownHostException e) {
                    throw new OneSwarmException(baseProblem + " unable to parse ip: '" + parts[0]
                            + "':" + e.getMessage());
                }
                byte[] baseIPBytes = baseIp.getAddress();
                InetAddress from = null;
                InetAddress to = null;
                try {
                    from = InetAddress.getByAddress(setBits(baseIPBytes, mask, false));
                    to = InetAddress.getByAddress(setBits(baseIPBytes, mask, true));
                } catch (UnknownHostException e) {
                    throw new OneSwarmException(baseProblem + e.getMessage());
                }

                IpRange r = new IpRangeImpl("", from.getHostAddress(), to.getHostAddress(), true);
                if (!r.isValid()) {
                    throw new OneSwarmException(baseProblem + " invalid ip range");
                }
                allowedRanges.add(r);
            } else {
                InetAddress baseIp = null;
                try {
                    baseIp = InetAddress.getByName(s.trim());
                } catch (UnknownHostException e) {
                    throw new OneSwarmException(baseProblem + " unable to parse ip: '" + s + "':"
                            + e.getMessage());
                }
                IpRange r = new IpRangeImpl("", baseIp.getHostAddress(), baseIp.getHostAddress(),
                        false);
                allowedRanges.add(r);
            }
        }
        return allowedRanges;
    }

    public static byte[] setBits(byte[] array, int startingFromBit, boolean bitValue) {
        byte[] clone = new byte[array.length];
        System.arraycopy(array, 0, clone, 0, clone.length);
        for (int i = 0; i < clone.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (startingFromBit <= i * 8 + j) {
                    if (bitValue) {
                        // set it to 1
                        clone[i] |= 1 << (7 - j);
                    } else {
                        // set the bit to 0
                        clone[i] &= ~(1 << (7 - j));
                    }
                }
            }
        }
        return clone;
    }

    private static boolean isInRange(List<IpRange> ranges, String ip) {
        for (IpRange ipRange : ranges) {
            if (ipRange.isInRange(ip)) {
                logger.finer(ip + " is in range: " + ipRange.getStartIp() + "-"
                        + ipRange.getEndIp());
                return true;
            }
        }
        logger.finer(ip + " is NOT in any range");

        return false;
    }

    public static void main(String[] args) {
        try {
            List<IpRange> filter = createIpFilter("192.168.0.1-192.168.1.1, 192.168.2.128/22, 10.0.1.1");
            for (IpRange ipRange : filter) {
                System.out.println(ipRange.getStartIp() + "-" + ipRange.getEndIp());
            }
            String[] ipToTest = { "192.168.1.1", "192.168.3.1", "10.0.1.1", "10.1.0.1" };
            for (String ip : ipToTest) {
                isInRange(filter, ip);
            }
        } catch (OneSwarmException e) {
            System.err.println(e.getMessage());
        }
        /*
         * test errors
         */
        try {
            createIpFilter("afgwedsf");
        } catch (OneSwarmException e) {
            System.err.println(e.getMessage());
        }
        try {
            createIpFilter("192.168.0.1-192.168.1.1-192.167.13.2, 192.168.2.0/24,10.0.1.1");
        } catch (OneSwarmException e) {
            System.err.println(e.getMessage());
        }
        try {
            createIpFilter("192.168.1.1-192.168.0.1, 192.168.2.0/24,10.0.1.1");
        } catch (OneSwarmException e) {
            System.err.println(e.getMessage());
        }
        try {
            createIpFilter("192.168.1.1/42");
        } catch (OneSwarmException e) {
            System.err.println(e.getMessage());
        }
    }

    private class ForwardIdleTimeoutChecker extends TimerTask {

        @Override
        public void run() {
            for (Iterator<ConnectionForwarder> iterator = connectionForwarders.keySet().iterator(); iterator
                    .hasNext();) {
                ConnectionForwarder cf = iterator.next();
                if (cf.isTimedOut()) {
                    logger.fine("closing remote access connection (idle time out)");
                    cf.stop(false);
                    iterator.remove();
                }
            }
        }

    }
}
