package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

public class ServiceSharingManager {

    private final static ServiceSharingManager instace = new ServiceSharingManager();

    public static ServiceSharingManager getInstance() {
        return instace;
    }

    private ServiceSharingManager() {
    }

    public ConcurrentHashMap<Long, Service> services = new ConcurrentHashMap<Long, ServiceSharingManager.Service>();

    public void registerService(long searchkey, Service service) {
        services.put(searchkey, service);
    }

    public Service handleSearch(OSF2FHashSearch search) {
        Service service = services.get(search.getInfohashhash());
        return service;
    }

    static class Service {
        public Service(InetSocketAddress address, String name) {
            super();
            this.address = address;
            this.name = name;
        }

        private final InetSocketAddress address;
        private final String name;

        public NetworkConnection createConnection() {
            ConnectionEndpoint target = new ConnectionEndpoint(address);
            NetworkConnection conn = NetworkManager.getSingleton().createConnection(target,
                    new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);
            return conn;

        }
    }
}
