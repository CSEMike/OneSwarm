package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager.ByteMatcher;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager;
import com.aelitis.azureus.core.networkmanager.impl.IncomingConnectionManager.MatchListener;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.IncomingSocketChannelManager;

import edu.washington.cs.oneswarm.f2f.BigFatLock;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

public class ServiceSharingManager {
	private final static String CLIENT_SERVICE_CONFIG_KEY = "SERVICE_CLIENT";
	private final static ServiceSharingManager instance = new ServiceSharingManager();

	private static BigFatLock lock = OverlayManager.lock;

	private final static Logger logger = Logger.getLogger(ServiceSharingManager.class.getName());

	public static ServiceSharingManager getInstance() {
		return instance;
	}

	private ServiceSharingManager() {
	}

	public HashMap<Long, SharedService> serverServices = new HashMap<Long, SharedService>();

	public HashMap<Long, ClientService> clientServices = new HashMap<Long, ClientService>();

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

	public void createClientService(String name, int port, long searchKey) {
		try {
			lock.lock();
			@SuppressWarnings("unchecked")
			List<Long> services = COConfigurationManager.getListParameter(
					CLIENT_SERVICE_CONFIG_KEY, new LinkedList<Long>());
			if (!services.contains(Long.valueOf(searchKey))) {
				services.add(Long.valueOf(searchKey));
				COConfigurationManager.setParameter(CLIENT_SERVICE_CONFIG_KEY, services);
			}
			ClientService cs = new ClientService(searchKey);
			COConfigurationManager.setParameter(cs.getNameKey(), name);
			COConfigurationManager.setParameter(cs.getPortKey(), Long.valueOf(port));
			COConfigurationManager.setParameter(cs.getEnabledKey(), true);
			cs.activate();
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

	public static class ClientService {
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
		private boolean active;

		public ClientService(long key) {
			this.serverSearchKey = key;
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
			try {
				IncomingSocketChannelManager incomingSocketChannelManager = new IncomingSocketChannelManager(
						getPortKey(), getEnabledKey());
				incomingSocketChannelManager.setExplicitBindAddress(InetAddress
						.getByName("127.0.0.1"));
				int port = getPort();
				IncomingConnectionManager.getSingleton().registerMatchBytes(new PortMatcher(port),
						new MatchListener() {
							@Override
							public void connectionMatched(Transport transport, Object routing_data) {
								logger.info("connection routed");
							}

							@Override
							public boolean autoCryptoFallback() {
								return false;
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private int getPort() {
			int port = COConfigurationManager.getIntParameter(getPortKey());
			return port;
		}

		public String toString() {
			return "Service: " + getName() + " port=" + getPort() + " key=" + serverSearchKey;
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
