package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;

public class SharedService implements Comparable<SharedService> {
    // Time the service is disabled after a failed connect attempt;
    public static final long FAILURE_BACKOFF = 60 * 1000;
    public static final String CONFIGURATION_PREFIX = "SHARED_SERVICE_";

    private long lastFailedConnect;
    private int activeConnections = 0;

    final long searchKey;

    SharedService(long searchKey) {
        this.searchKey = searchKey;
    }

    public String getName() {
        return COConfigurationManager.getStringParameter(getNameKey());
    }

    private String getNameKey() {
        return CONFIGURATION_PREFIX + searchKey + "_name";
    }

    InetSocketAddress getAddress() {
        try {
            int port = COConfigurationManager.getIntParameter(getPortKey(), -1);
            String ip = COConfigurationManager.getStringParameter(getIpKey());
            return new InetSocketAddress(InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String getPortKey() {
        return CONFIGURATION_PREFIX + searchKey + "_port";
    }

    private String getIpKey() {
        return CONFIGURATION_PREFIX + searchKey + "_ip";
    }

    public void setName(String name) {
        COConfigurationManager.setParameter(getNameKey(), name);
    }

    public void setAddress(InetSocketAddress address) {
        COConfigurationManager.setParameter(getPortKey(), address.getPort());
        COConfigurationManager.setParameter(getIpKey(), address.getAddress().getHostAddress());
    }

    @Override
    public int compareTo(SharedService that) {
        return this.getName().compareTo(that.getName());
    }

    private ConnectionListener getMonitoringListener()
    {
        final SharedService self = this;
        return new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                self.activeConnections--;
                self.lastFailedConnect = System.currentTimeMillis();
            }

            @Override
            public void connectStarted() {
                self.activeConnections++;
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
            }

            @Override
                public void exceptionThrown(Throwable error) {
                self.activeConnections--;
            }

            @Override
            public String getDescription() {
                    return "Shared Service Listener";
            }
        };
    }

    public NetworkConnection createConnection() {
        InetSocketAddress address = getAddress();
        ConnectionEndpoint target = new ConnectionEndpoint(address);
        target.addProtocol(new ProtocolEndpointTCP(address));
        NetworkConnection conn = NetworkManager.getSingleton().createConnection(target,
                new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);
        return new ListenedNetworkConnection(conn, this.getMonitoringListener());
    }

    public boolean isEnabled() {
        long lastFailedAge = System.currentTimeMillis() - lastFailedConnect;
        if (activeConnections > 0) {
            return true;
        }
        boolean enabled = lastFailedAge > FAILURE_BACKOFF;
        if (!enabled) {
            ServiceSharingManager.logger.finer(String.format(
                    "Service %s is disabled, last failure: %d seconds ago", getName(),
                    lastFailedAge));
        }
        return enabled;
    }

    public SharedServiceDTO toDTO() {
        InetSocketAddress address = getAddress();
        return new SharedServiceDTO(getName(), Long.toHexString(searchKey), address.getAddress()
                .getHostAddress(), address.getPort());
    }

    @Override
    public String toString() {
        InetSocketAddress address = getAddress();
        return "key=" + searchKey + getName() + " " + address + " enabled=" + isEnabled();
    }

    public void clean() {
        COConfigurationManager.removeParameter(getPortKey());
        COConfigurationManager.removeParameter(getIpKey());
        COConfigurationManager.removeParameter(getNameKey());
    }
}