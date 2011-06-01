package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager.ByteMatcher;
import com.aelitis.azureus.core.networkmanager.NetworkManager.RoutingListener;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.IncomingSocketChannelManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection.OverlayRegistrationError;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager.HashSearchListener;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

public class ServiceSharingManager {
    private final static String CLIENT_SERVICE_CONFIG_KEY = "SERVICE_CLIENT";
    private final static ServiceSharingManager instance = new ServiceSharingManager();

    private static BigFatLock lock = OverlayManager.lock;

    public final static Logger logger = Logger.getLogger(ServiceSharingManager.class.getName());

    public static ServiceSharingManager getInstance() {
        return instance;
    }

    private ServiceSharingManager() {
    }

    public HashMap<Long, SharedService> serverServices = new HashMap<Long, SharedService>();

    public HashMap<Long, ClientService> clientServices = new HashMap<Long, ClientService>();

    public void registerServerService(long searchkey, SharedService service) {
        logger.fine("Registering service: key=" + searchkey + " service=" + service.toString());
        try {
            lock.lock();
            serverServices.put(searchkey, service);
        } finally {
            lock.unlock();
        }

    }

    public void deregisterServerService(long searchKey) {
        try {
            lock.lock();
            serverServices.remove(searchKey);
        } finally {
            lock.unlock();
        }
    }

    public void loadConfiguredClientServices() {
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            for (Long key : services) {
                ClientService cs = new ClientService(key);
                clientServices.put(key, cs);
                cs.activate();
            }
        } finally {
            lock.unlock();
        }

    }

    public int createClientService(String name, int suggestedPort, long searchKey) {
        int port = suggestedPort;
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            ClientService cs = clientServices.get(searchKey);
            // Create a new
            if (cs == null) {
                if (!services.contains(Long.valueOf(searchKey))) {
                    services.add(Long.valueOf(searchKey));
                    COConfigurationManager.setParameter(CLIENT_SERVICE_CONFIG_KEY, services);
                }
                cs = new ClientService(searchKey);
                cs.setName(name);
                cs.setPort(port);
                clientServices.put(searchKey, cs);
            }
            // Activate if needed, and update the port if not set
            if (!cs.active) {
                if (cs.getPort() == -1) {
                    cs.setPort(port);
                }
                cs.activate();
            }
            port = cs.getPort();
        } finally {
            lock.unlock();
        }
        return port;
    }

    public void deactivateClientService(long searchKey) {
        try {
            lock.lock();
            @SuppressWarnings("unchecked")
            List<Long> services = COConfigurationManager.getListParameter(
                    CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
            services.remove(Long.valueOf(searchKey));
            COConfigurationManager.setParameter(CLIENT_SERVICE_CONFIG_KEY, services);
            ClientService cs = clientServices.get(searchKey);
            if (cs != null) {
                cs.deactivate();
            }
        } finally {
            lock.unlock();
        }
    }

    public SharedService handleSearch(OSF2FHashSearch search) {
        long searchKey = search.getInfohashhash();
        return getSharedService(searchKey);
    }

    public SharedService getSharedService(long infohashhash) {
        SharedService service = null;
        try {
            lock.lock();
            service = serverServices.get(infohashhash);
        } finally {
            lock.unlock();
        }

        if (service == null || !service.isEnabled()) {
            return null;
        }
        return service;
    }

    public static class ClientService implements RoutingListener {
        private final class RawMessageFactory implements MessageStreamFactory {
            @Override
            public MessageStreamEncoder createEncoder() {

                return new RawMessageEncoder();
            }

            @Override
            public MessageStreamDecoder createDecoder() {
                return new RawMessageDecoder();
            }
        }

        private static class PortMatcher implements ByteMatcher {

            private final int port;

            public PortMatcher(int port) {
                this.port = port;
            }

            @Override
            public int minSize() {
                return 0;
            }

            @Override
            public Object minMatches(TransportHelper transport, ByteBuffer to_compare, int port) {
                return matches(transport, to_compare, port);
            }

            @Override
            public int maxSize() {
                return 0;
            }

            @Override
            public Object matches(TransportHelper transport, ByteBuffer to_compare, int port) {
                return port == getSpecificPort() ? "" : null;
            }

            @Override
            public int matchThisSizeOrBigger() {
                return 0;
            }

            @Override
            public int getSpecificPort() {
                return port;
            }

            @Override
            public byte[][] getSharedSecrets() {
                return null;
            }
        }

        public static final String CONFIGURATION_PREFIX = "SERVICE_CLIENT_";
        private final long serverSearchKey;
        private boolean active = false;
        private PortMatcher matcher;

        public ClientService(long key) {
            this.serverSearchKey = key;
            COConfigurationManager.setParameter(getEnabledKey(), false);
            IncomingSocketChannelManager incomingSocketChannelManager = new IncomingSocketChannelManager(
                    getPortKey(), getEnabledKey());
            try {
                incomingSocketChannelManager.setExplicitBindAddress(InetAddress
                        .getByName("127.0.0.1"));
            } catch (UnknownHostException e) {
            }

        }

        public void setName(String name) {
            COConfigurationManager.setParameter(getNameKey(), name);
        }

        public void setPort(int port) {
            COConfigurationManager.setParameter(getPortKey(), Long.valueOf(port));
        }

        private String getPortKey() {
            return CONFIGURATION_PREFIX + serverSearchKey + "_port";
        }

        private String getEnabledKey() {
            return CONFIGURATION_PREFIX + serverSearchKey + "_enabled";
        }

        private String getNameKey() {
            return CONFIGURATION_PREFIX + serverSearchKey + "_name";
        }

        public String getName() {
            return COConfigurationManager.getStringParameter(getNameKey());
        }

        public void activate() {
            logger.info("loading " + toString());
            if (active) {
                logger.warning("Tried to activate service " + getName()
                        + " but it is already active.");
                return;
            }
            active = true;
            try {
                int port = getPort();
                COConfigurationManager.setParameter(getEnabledKey(), true);
                matcher = new PortMatcher(port);
                NetworkManager.getSingleton().requestIncomingConnectionRouting(matcher, this,
                        new RawMessageFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void deactivate() {
            logger.info("deactivating " + toString());
            if (!active) {
                logger.warning("Tried to deactivate service " + getName()
                        + " but it is already deactivated.");
                return;
            }
            active = false;
            // remove the matcher
            if (matcher != null) {
                NetworkManager.getSingleton().cancelIncomingConnectionRouting(matcher);
            }
            COConfigurationManager.setParameter(getEnabledKey(), false);
            COConfigurationManager.removeParameter(getNameKey());
            COConfigurationManager.removeParameter(getPortKey());
            COConfigurationManager.removeParameter(getEnabledKey());
        }

        private int getPort() {
            int port = COConfigurationManager.getIntParameter(getPortKey(), -1);
            return port;
        }

        public String toString() {
            return "Service: " + getName() + " port=" + getPort() + " key=" + serverSearchKey;
        }

        @Override
        public void connectionRouted(final NetworkConnection incomingConnection, Object routing_data) {
            logger.fine("connection routed");
            // Check if local
            final AtomicInteger responseCount = new AtomicInteger(0);
            final SharedService sharedService = ServiceSharingManager.getInstance()
                    .getSharedService(serverSearchKey);
            if (sharedService != null) {
                logger.finer("got request for local service, doing loopback");
                ServiceSharingLoopback loopback = new ServiceSharingLoopback(sharedService,
                        incomingConnection);
                loopback.connect();
            } else {
                logger.finer("sending search to " + serverSearchKey);
                SearchManager searchManager = OSF2FMain.getSingelton().getOverlayManager()
                        .getSearchManager();
                searchManager.sendServiceSearch(serverSearchKey, new HashSearchListener() {
                    @Override
                    public void searchResponseReceived(OSF2FHashSearch search,
                            FriendConnection source, OSF2FHashSearchResp msg) {
                        // TODO Handle multiple responses
                        int count = responseCount.incrementAndGet();
                        if (count > 1) {
                            return;
                        }
                        ClientServiceConnection serviceConnection = new ClientServiceConnection(
                                ClientService.this, incomingConnection, source, msg.getChannelID(),
                                msg.getPathID());
                        // register it with the friendConnection
                        try {
                            source.registerOverlayTransport(serviceConnection);
                            // safe to start it since we know that the other
                            // party is interested
                            serviceConnection.start();
                        } catch (OverlayRegistrationError e) {
                            Debug.out("got an error when registering outgoing transport: "
                                    + e.getMessage());
                            return;
                        }
                    }
                });
            }
        }

        @Override
        public boolean autoCryptoFallback() {
            return false;
        }
    }

    public static class SharedService {
        // Time the service is disabled after a failed connect attempt;
        public static final long FAILURE_BACKOFF = 60 * 1000;

        public SharedService(InetSocketAddress address, String name) {
            super();
            this.address = address;
            this.name = name;
        }

        private final InetSocketAddress address;
        private final String name;
        private long lastFailedConnect;

        public boolean isEnabled() {
            long lastFailedAge = System.currentTimeMillis() - lastFailedConnect;
            boolean enabled = lastFailedAge > FAILURE_BACKOFF;
            if (!enabled) {
                logger.finer(String.format("Service %s is disabled, last failure: %d seconds ago",
                        name, lastFailedAge));
            }
            return enabled;
        }

        public NetworkConnection createConnection() {
            ConnectionEndpoint target = new ConnectionEndpoint(address);
            target.addProtocol(new ProtocolEndpointTCP(address));
            NetworkConnection conn = NetworkManager.getSingleton().createConnection(target,
                    new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);

            return conn;
        }

        public void connect(NetworkConnection conn, final ConnectionListener listener) {
            try {
                conn.connect(true, new ConnectionListener() {

                    @Override
                    public String getDescription() {
                        return name + "Listener";
                    }

                    @Override
                    public void exceptionThrown(Throwable error) {
                        listener.exceptionThrown(error);
                    }

                    @Override
                    public void connectSuccess(ByteBuffer remaining_initial_data) {
                        listener.connectSuccess(remaining_initial_data);
                    }

                    @Override
                    public void connectStarted() {
                        listener.connectStarted();
                    }

                    @Override
                    public void connectFailure(Throwable failure_msg) {
                        lastFailedConnect = System.currentTimeMillis();
                        listener.connectFailure(failure_msg);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String toString() {
            return name + " " + address + " enabled=" + isEnabled();
        }
    }

    public void clearLocalServices() {
        ArrayList<Long> currentServices = new ArrayList<Long>(clientServices.keySet());
        for (Long key : currentServices) {
            deactivateClientService(key);
        }
        currentServices.clear();
        currentServices.addAll(serverServices.keySet());
        for (Long key : currentServices) {
            deregisterServerService(key);
        }
    }
}
