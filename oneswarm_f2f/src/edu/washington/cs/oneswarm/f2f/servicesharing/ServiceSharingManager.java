package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;

import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

public class ServiceSharingManager {

    private final static ServiceSharingManager instance = new ServiceSharingManager();

    private static BigFatLock lock = OverlayManager.lock;

    private final static Logger logger = Logger.getLogger(ServiceSharingManager.class.getName());

    public static ServiceSharingManager getInstance() {
        return instance;
    }

    private ServiceSharingManager() {
    }

    public HashMap<Long, SharedService> serverServices = new HashMap<Long, SharedService>();

    public void registerServerService(long searchkey, SharedService service) {
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

    public SharedService handleSearch(OSF2FHashSearch search) {
        SharedService service = null;
        try {
            lock.lock();
            service = serverServices.get(search.getInfohashhash());
        } finally {
            lock.unlock();
        }

        if (service == null || !service.isEnabled()) {
            return null;
        }
        return service;
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
            logger.finer(String.format("Service %s is disabled, last failure: %d seconds ago",
                    name, lastFailedAge));
            return enabled;
        }

        public NetworkConnection createConnection(final ConnectionListener listener) {
            ConnectionEndpoint target = new ConnectionEndpoint(address);
            NetworkConnection conn = NetworkManager.getSingleton().createConnection(target,
                    new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);
            conn.connect(false, new ConnectionListener() {

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
            return conn;
        }

        public String toString() {
            return name + " " + address;
        }
    }
}
