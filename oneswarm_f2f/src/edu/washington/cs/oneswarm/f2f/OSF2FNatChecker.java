package edu.washington.cs.oneswarm.f2f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.NetworkManager.RoutingListener;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageDecoder;
import com.aelitis.azureus.core.peermanager.messaging.azureus.AZMessageEncoder;

public class OSF2FNatChecker {
    private static final String NAT_HANDSHAKE = "OneSwarm NAT";

    private final static byte[] legacy_handshake_header = new byte[NAT_HANDSHAKE.getBytes().length + 1];
    static {
        ByteBuffer b = ByteBuffer.wrap(legacy_handshake_header);
        b.put((byte) NAT_HANDSHAKE.length());
        b.put(NAT_HANDSHAKE.getBytes());
    }

    private final static Logger logger = Logger.getLogger(OSF2FNatChecker.class.getName());
    private NetworkManager.ByteMatcher natMatcher;
    private final static byte S = DirectByteBuffer.SS_OTHER;

    private NatCheckResult lastTcpResult;
    private NatCheckResult lastUpdResult;

    private boolean running = false;

    public OSF2FNatChecker() {

        COConfigurationManager.addAndFireParameterListener("Perform.NAT.Check",
                new ParameterListener() {
                    public void parameterChanged(String parameterName) {
                        boolean enabled = COConfigurationManager
                                .getBooleanParameter("Perform.NAT.Check");

                        if (enabled) {
                            installNatCheckMatcher();
                        } else {
                            uninstallNatCheckMatcher();
                        }
                    }
                });
    }

    public void triggerNatCheck() {
        if (running) {
            logger.finer("NAT check triggered, doing update check");
            // set last to waiting
            lastTcpResult = new NatCheckResult();
            UpdateMonitor.getSingleton(AzureusCoreFactory.getSingleton()).performCheck(true, false,
                    false, new UpdateCheckInstanceListener() {
                        public void cancelled(UpdateCheckInstance instance) {
                        }

                        public void complete(UpdateCheckInstance instance) {
                            logger.finer("NAT check completed");
                        }
                    });
        } else {
            logger.finer("NAT check not running, skipping nat check trigger");
        }
    }

    public NatCheckResult getResult() {
        return lastTcpResult;
    }

    private void uninstallNatCheckMatcher() {
        synchronized (this) {
            if (running && natMatcher != null) {
                logger.fine("removing nat check routing");
                NetworkManager.getSingleton().cancelIncomingConnectionRouting(natMatcher);
                running = false;
            }
        }
    }

    private void installNatCheckMatcher() {
        synchronized (this) {
            if (!running) {
                Thread t = new Thread(new NatCheckRunnable());
                t.setDaemon(true);
                t.setName("OSF2F protocol matcher loader");
                t.start();
            }
        }
    }

    private final class NatCheckRunnable implements Runnable {

        public void run() {
            synchronized (OSF2FNatChecker.this) {
                running = true;
                logger.fine("installing nat check routing");
                natMatcher = new OsNatMatcher();
                NetworkManager.getSingleton().requestIncomingConnectionRouting(natMatcher,
                        new RoutingListener() {
                            public boolean autoCryptoFallback() {
                                // TODO Auto-generated method stub
                                return false;
                            }

                            @SuppressWarnings("unchecked")
                            public void connectionRouted(NetworkConnection connection,
                                    Object routing_data) {
                                logger.fine("connection routed to NAT checker, isTcp="
                                        + connection.getTransport().isTCP());
                                DirectByteBuffer lenBuffer = DirectByteBufferPool.getBuffer(S, 4);

                                try {

                                    final Transport tr = connection.getTransport();
                                    // start by checking the handShake
                                    byte[] hsBuffer = new byte[NAT_HANDSHAKE.getBytes().length + 1];
                                    tr.read(new ByteBuffer[] { ByteBuffer.wrap(hsBuffer) }, 0, 1);
                                    if (!Arrays.equals(hsBuffer, legacy_handshake_header)) {
                                        final String msg = "got wrong handshake to NatChecker, incoming: "
                                                + new String(hsBuffer);
                                        Debug.out(msg);
                                        throw new IOException(msg);
                                    }

                                    // then read the actual stuff
                                    tr.read(new ByteBuffer[] { lenBuffer.getBuffer(S) }, 0, 1);
                                    lenBuffer.flip(S);
                                    int len = lenBuffer.getInt(S);

                                    int MAX_LEN = 1024;
                                    if (len > MAX_LEN || len <= 0) {
                                        throw new IOException("invalid len specified: " + len);
                                    }
                                    ByteBuffer b = ByteBuffer.allocate(len);

                                    tr.read(new ByteBuffer[] { b }, 0, 1);
                                    b.flip();
                                    int read = b.remaining();

                                    logger.finer("read " + read + " bytes for NAT check");
                                    byte[] backingBuffer = b.array();

                                    final Map<String, Object> message = BDecoder.decode(
                                            backingBuffer, 0, read);
                                    logger.finest("decoded " + message.size() + " keys");

                                    if (!message.containsKey("ip")) {
                                        final String msg = "Invalid nat check response, no 'ip' key";
                                        throw new IOException(msg);
                                    }
                                    if (!message.containsKey("port")) {
                                        final String msg = "Invalid nat check response, no 'port' key";
                                        throw new IOException(msg);
                                    }
                                    String ip = new String((byte[]) message.get("ip"));
                                    logger.finest("ip=" + ip);
                                    int port = (int) ((Long) message.get("port")).longValue();
                                    logger.finest("port=" + port);
                                    boolean isTcp = tr.isTCP();
                                    if (isTcp) {
                                        lastTcpResult = new NatCheckResult(ip, port);
                                        logger.fine("got tcp nat check: "
                                                + lastTcpResult.toString());
                                    } else {
                                        lastUpdResult = new NatCheckResult(ip, port);
                                        logger.fine("got udp nat check: "
                                                + lastUpdResult.toString());
                                    }

                                    // and write back the handshake
                                    final ByteBuffer outBuffer = ByteBuffer
                                            .wrap(legacy_handshake_header);
                                    logger.finest("writing " + outBuffer.remaining() + " bytes");
                                    long written = 0;
                                    while (tr.write(new ByteBuffer[] { outBuffer }, 0, 1) > 0
                                            && outBuffer.remaining() > 0) {
                                        logger.finest("total written: " + written);
                                    }
                                    connection.close();
                                } catch (Exception e) {
                                    Debug.out("Error during incoming nat check" + e.getMessage());
                                }

                            }
                        }, new MessageStreamFactory() {
                            public MessageStreamEncoder createEncoder() {
                                return new AZMessageEncoder(false); /* unused */
                            }

                            public MessageStreamDecoder createDecoder() {
                                return new AZMessageDecoder(); /* unused */
                            }

                        });

            }
        }
    }

    static class NatCheckResult {

        public final static long NAT_CHECK_TIMEOUT = 20 * 1000;

        static enum Status {
            WAITING(0), FAILED(-1), SUCCESS(1);
            private final int statusCode;

            Status(int code) {
                this.statusCode = code;
            }

            public int getCode() {
                return statusCode;
            }

            static Status getFromCode(int code) {
                switch (code) {
                case -1:
                    return FAILED;
                case 1:
                    return SUCCESS;
                default:
                    return WAITING;
                }
            }
        }

        Status status;
        String ip;
        int port;
        final long time;

        public NatCheckResult() {
            this.status = Status.WAITING;
            this.time = System.currentTimeMillis();

        }

        public NatCheckResult(String ip, int port) {
            this.status = Status.SUCCESS;
            this.ip = ip;
            this.port = port;
            this.time = System.currentTimeMillis();
        }

        public int hashCode() {
            return ip.hashCode() ^ port;
        }

        public boolean equals(Object o) {
            if (o instanceof NatCheckResult == false) {
                return false;
            }
            NatCheckResult p = (NatCheckResult) o;
            if (p.ip == null || !p.ip.equals(ip)) {
                return false;
            }

            if (p.port != port) {
                return false;
            }

            return true;
        }

        public String toString() {
            return ip + ":" + port;
        }

        public long getAge() {
            return System.currentTimeMillis() - time;
        }

        public Status getStatus() {
            if (status == Status.WAITING && getAge() > NAT_CHECK_TIMEOUT) {
                status = Status.FAILED;
            }
            return status;
        }

    }

    static class OsNatMatcher implements NetworkManager.ByteMatcher {

        private final int size;

        public OsNatMatcher() {
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
}
