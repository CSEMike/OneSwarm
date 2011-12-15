package edu.washington.cs.oneswarm.f2f.datagram;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.NoSuchPaddingException;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.RateHandler;
import com.aelitis.net.udp.uc.PRUDPPacketHandlerFactory;
import com.aelitis.net.udp.uc.impl.ExternalUdpPacketHandler;
import com.aelitis.net.udp.uc.impl.PRUDPPacketHandlerImpl;

import edu.uw.cse.netlab.utils.CoreWaiter;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public class DatagramConnectionManagerImpl extends CoreWaiter implements DatagramConnectionManager,
        ExternalUdpPacketHandler {
    private static final int MIN_MULTIHOME_KEY_CHECK_PERIOD = 60 * 1000;
    private static DatagramConnectionManagerImpl instance = new DatagramConnectionManagerImpl();
    public final static Logger logger = Logger.getLogger(DatagramConnectionManagerImpl.class
            .getName());

    public synchronized static DatagramConnectionManagerImpl get() {
        return instance;
    }

    private final HashMap<String, DatagramConnection> connections = new HashMap<String, DatagramConnection>();

    private final HashMap<String, Long> unknownIncomingConnections = new HashMap<String, Long>();

    private DatagramSocket socket;

    private DatagramConnectionManagerImpl() {
        super();
    }

    public DatagramConnection createConnection(FriendConnection connection)
            throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        return new DatagramConnection(this, connection);
    }

    private RateHandler lanUploadRateHandler;
    private RateHandler uploadRateHandler;

    @Override
    protected void init() {
        // Get the socket for the udp.listen.port
        PRUDPPacketHandlerImpl handler = (PRUDPPacketHandlerImpl) (PRUDPPacketHandlerFactory
                .getHandler(COConfigurationManager.getIntParameter("UDP.Listen.Port")));
        handler.addExternalHandler(this);
        socket = handler.getSocket();
        this.lanUploadRateHandler = NetworkManager.getSingleton().getUploadRateHandler(
                DatagramConnection.MAX_DATAGRAM_PAYLOAD_SIZE, true);
        this.uploadRateHandler = NetworkManager.getSingleton().getUploadRateHandler(
                DatagramConnection.MAX_DATAGRAM_PAYLOAD_SIZE, false);
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
        }
    }

    @Override
    public void send(DatagramPacket packet, boolean lanLocal) throws IOException {
        int length = packet.getLength();
        socket.send(packet);
        logger.finest("Packet sent, length=" + length);
        if (lanLocal) {
            lanUploadRateHandler.bytesProcessed(length);
        } else {
            uploadRateHandler.bytesProcessed(length);
        }
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
}