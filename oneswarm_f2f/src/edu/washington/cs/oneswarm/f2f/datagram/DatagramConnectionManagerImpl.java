package edu.washington.cs.oneswarm.f2f.datagram;

import static com.aelitis.azureus.core.networkmanager.NetworkManager.UNLIMITED_RATE;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.RateHandler;
import com.aelitis.azureus.core.networkmanager.impl.WriteEventListener;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.impl.ExternalUdpPacketHandler;
import com.aelitis.net.udp.uc.impl.PRUDPPacketHandlerImpl;

import edu.uw.cse.netlab.utils.CoreWaiter;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public class DatagramConnectionManagerImpl extends CoreWaiter implements DatagramConnectionManager,
        ExternalUdpPacketHandler {

    private static final int MIN_MULTIHOME_KEY_CHECK_PERIOD = 60 * 1000;

    // The fraction of traffic that will be UDP during times of congestion.
    private static final double DATAGRAM_TRAFFIC_SHARE = 0.5;

    // How often the periodic token refill task is run.
    private static final int TOKEN_REFILL_PERIOD = 100;

    // Token bucket size in multipliers of ratelimit*(1000/refill_period)
    private static final double TOKEN_BUCKET_MAX_SIZE = 2;

    private static DatagramConnectionManagerImpl instance = new DatagramConnectionManagerImpl();
    public final static Logger logger = Logger.getLogger(DatagramConnectionManagerImpl.class
            .getName());

    public synchronized static DatagramConnectionManagerImpl get() {
        return instance;
    }

    private final HashMap<String, DatagramConnection> connections = new HashMap<String, DatagramConnection>();

    private final HashMap<String, Long> unknownIncomingConnections = new HashMap<String, Long>();

    private DatagramRateLimiter uploadRateLimiter;
    private DatagramRateLimiter lanUploadRateLimiter;

    private DatagramSocket socket;

    private DatagramConnectionManagerImpl() {
        super();
    }

    public DatagramConnection createConnection(FriendConnection connection)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        return new DatagramConnection(this, connection);
    }

    private boolean lan_rate_enabled;

    @Override
    protected void init() {
        // Get the socket for the udp.listen.port
        PRUDPPacketHandlerImpl handler = (PRUDPPacketHandlerImpl) (PRUDPPacketHandlerFactory
                .getHandler(COConfigurationManager.getIntParameter("UDP.Listen.Port")));
        handler.addExternalHandler(this);
        socket = handler.getSocket();

        initRateLimiting();
    }

    private void initRateLimiting() {
        uploadRateLimiter = new DatagramRateLimiter();
        lanUploadRateLimiter = new DatagramRateLimiter();

        COConfigurationManager.addAndFireParameterListeners(new String[] { "LAN Speed Enabled",
                "Max Upload Speed KBs", "Max LAN Upload Speed KBs" }, new ParameterListener() {
            @Override
            public void parameterChanged(String ignore) {
                lan_rate_enabled = COConfigurationManager.getBooleanParameter("LAN Speed Enabled");
                int max_upload_rate_bps_normal = COConfigurationManager
                        .getIntParameter("Max Upload Speed KBs") * 1024;
                if (max_upload_rate_bps_normal < 1024)
                    max_upload_rate_bps_normal = UNLIMITED_RATE;
                if (max_upload_rate_bps_normal > UNLIMITED_RATE)
                    max_upload_rate_bps_normal = UNLIMITED_RATE;

                int max_lan_upload_rate_bps = COConfigurationManager
                        .getIntParameter("Max LAN Upload Speed KBs") * 1024;
                if (max_lan_upload_rate_bps < 1024)
                    max_lan_upload_rate_bps = UNLIMITED_RATE;
                if (max_lan_upload_rate_bps > UNLIMITED_RATE)
                    max_lan_upload_rate_bps = UNLIMITED_RATE;

                int minBucketSize = 2 * DatagramConnection.MAX_DATAGRAM_SIZE;
                uploadRateLimiter.setTokenBucketSize(Math
                        .max(minBucketSize, (int) (1000 / TOKEN_REFILL_PERIOD
                                * TOKEN_BUCKET_MAX_SIZE * max_upload_rate_bps_normal)));
                lanUploadRateLimiter.setTokenBucketSize(Math.max(minBucketSize, (int) (1000
                        / TOKEN_REFILL_PERIOD * TOKEN_BUCKET_MAX_SIZE * max_lan_upload_rate_bps)));
            }
        });

        final RateHandler lanUploadRateHandler = NetworkManager.getSingleton()
                .getUploadRateHandler(DatagramConnection.MAX_DATAGRAM_PAYLOAD_SIZE, true);
        final RateHandler uploadRateHandler = NetworkManager.getSingleton().getUploadRateHandler(
                DatagramConnection.MAX_DATAGRAM_PAYLOAD_SIZE, false);

        NetworkManager.getSingleton().addWriteEventListener(new WriteEventListener() {
            @Override
            public void writeEvent() {
                refill(uploadRateLimiter, uploadRateHandler, DATAGRAM_TRAFFIC_SHARE);
                refill(lanUploadRateLimiter, lanUploadRateHandler, DATAGRAM_TRAFFIC_SHARE);
            }
        });

        TimerTask tokenRefillTask = new TimerTask() {
            @Override
            public void run() {
                refill(uploadRateLimiter, uploadRateHandler, 1);
                refill(lanUploadRateLimiter, lanUploadRateHandler, 1);
                uploadRateLimiter.allocateTokens();
                lanUploadRateLimiter.allocateTokens();
            }
        };
        Timer t = new Timer("DatagramTokenRefillTask", true);
        t.scheduleAtFixedRate(tokenRefillTask, 0, TOKEN_REFILL_PERIOD);
    }

    private void refill(DatagramRateLimiter datagramRateLimiter, RateHandler handler,
            double trafficShare) {
        if (datagramRateLimiter.isFull()) {
            return;
        }
        int available = handler.getCurrentNumBytesAllowed();
        int added = datagramRateLimiter.refillBucket((int) Math.round(available * trafficShare));
        handler.bytesProcessed(added);
    }

    @Override
    public boolean packetReceived(DatagramPacket packet) {
        int port = packet.getPort();
        InetAddress address = packet.getAddress();
        String key = getKey(address, port);
        logger.finest("checking udp packet match: " + key);
        DatagramConnection conn = connections.get(key);
        if (conn == null) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("no match found, keys=" + connections.keySet());
            }
            return doMultihomeCheck(packet, port, address, key);
        }
        if (!conn.messageReceived(packet)) {
            logger.finest("matched packet failed decryption");
            return false;
        }
        return true;
    }

    private boolean doMultihomeCheck(DatagramPacket packet, int port, InetAddress address,
            String key) {
        synchronized (connections) {
            Long lastChecked = unknownIncomingConnections.get(key);
            if (lastChecked == null
                    || System.currentTimeMillis() - lastChecked > MIN_MULTIHOME_KEY_CHECK_PERIOD) {
                logger.finest("checking if multihomed source");

                LinkedList<DatagramConnection> conns = new LinkedList<DatagramConnection>(
                        connections.values());
                for (DatagramConnection c : conns) {
                    // Only check connections on the same port.
                    if (c.getRemotePort() == port) {
                        unknownIncomingConnections.put(key, System.currentTimeMillis());
                        if (c.messageReceived(packet)) {
                            logger.fine("Found multihomed friend, adding remote address to datagram connection: "
                                    + address);
                            String newKey = c.addRemoteIpPort(address, port);
                            connections.put(newKey, c);
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    static String getKey(InetAddress address, int port) {
        return address.getHostAddress() + ":" + port;
    }

    @Override
    public void register(DatagramConnection connection) {
        synchronized (connections) {
            Set<String> keys = connection.getKeys();
            for (String key : keys) {
                logger.fine("registering connection to key: " + key);
                DatagramConnection existing = connections.get(key);
                if (existing != null) {
                    logger.warning("Registered udp connection but one is already there!");
                    existing.close();
                }
                connections.put(key, connection);
            }
            if (lan_rate_enabled && connection.isLanLocal()) {
                lanUploadRateLimiter.addQueue(connection);
            } else {
                uploadRateLimiter.addQueue(connection);
            }
        }
    }

    @Override
    public void send(DatagramPacket packet, boolean lanLocal) throws IOException {
        int length = packet.getLength();
        socket.send(packet);
        logger.finest("Packet sent, length=" + length);
    }

    @Override
    public void deregister(DatagramConnection conn) {
        synchronized (connections) {

            Set<String> keys = conn.getKeys();
            for (String key : keys) {
                DatagramConnection registered = connections.get(key);
                if (registered != null && registered.equals(conn)) {
                    connections.remove(key);
                    unknownIncomingConnections.remove(key);
                    logger.fine("Deregistering " + key);
                }
            }
            // Parameters might have changed since we added the conn
            // Remove from both to be on the safe side.
            lanUploadRateLimiter.removeQueue(conn);
            uploadRateLimiter.removeQueue(conn);
        }
    }

    @Override
    public int getPort() {
        return socket.getLocalPort();
    }

    @Override
    public void socketUpdated(DatagramSocket socket) {
        int oldPort = this.socket.getLocalPort();
        this.socket = socket;

        if (socket.getLocalPort() != oldPort) {
            // Reinitialize all datagram connections.
            LinkedList<DatagramConnection> conns = new LinkedList<DatagramConnection>(
                    connections.values());
            for (DatagramConnection connection : conns) {
                connection.reInitialize();
            }
        }
    }

    @Override
    public DatagramRateLimiter getMainRateLimiter() {
        return uploadRateLimiter;
    }
}