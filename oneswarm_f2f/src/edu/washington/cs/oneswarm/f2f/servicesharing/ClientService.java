package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.gudy.azureus2.core3.config.COConfigurationManager;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.NetworkManager.RoutingListener;
import com.aelitis.azureus.core.networkmanager.impl.tcp.IncomingSocketChannelManager;

import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.SearchManager;
import edu.washington.cs.oneswarm.f2f.network.SearchManager.HashSearchListener;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceDTO;

public class ClientService implements RoutingListener, Comparable<ClientService> {
    public static final String CONFIGURATION_PREFIX = "SERVICE_CLIENT_";
    boolean active = false;
    private PortMatcher matcher;
    final long serverSearchKey;
    final int serviceChannels;
    private IncomingSocketChannelManager iscm;

    public ClientService(long key) {
        this.serverSearchKey = key;
        COConfigurationManager.setParameter(getEnabledKey(), false);
        this.serviceChannels = COConfigurationManager.getIntParameter(getChannelsKey());
    }

    public void activate() {
        ServiceSharingManager.logger.info("loading " + toString());
        if (active) {
            ServiceSharingManager.logger.warning("Tried to activate service " + getName()
                    + " but it is already active.");
            return;
        }
        active = true;
        iscm = new IncomingSocketChannelManager(getPortKey(), getEnabledKey());
        try {
            iscm.setExplicitBindAddress(InetAddress.getByName("127.0.0.1"));
        } catch (UnknownHostException e) {
        }

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

    @Override
    public boolean autoCryptoFallback() {
        return false;
    }

    @Override
    public int compareTo(ClientService that) {
        return this.getName().compareTo(that.getName());
    }

    @Override
    public void connectionRouted(final NetworkConnection incomingConnection, Object routing_data) {
        ServiceSharingManager.logger.fine("connection routed");
        final SharedService sharedService = ServiceSharingManager.getInstance().getSharedService(
                serverSearchKey);
        // Check if local
        if (sharedService != null) {
            ServiceSharingManager.logger.finer("got request for local service, doing loopback");
            ServiceSharingLoopback loopback = new ServiceSharingLoopback(sharedService,
                    incomingConnection);
            loopback.connect();
        } else {
            if (!ServiceConnectionManager.getInstance().requestService(incomingConnection,
                    serverSearchKey)) {
                ServiceSharingManager.logger.finer("sending search to " + serverSearchKey);
                SearchManager searchManager = OSF2FMain.getSingelton().getOverlayManager()
                    .getSearchManager();
                searchManager.sendServiceSearch(serverSearchKey, new HashSearchListener() {
                    @Override
                    public void searchResponseReceived(OSF2FHashSearch search,
                            FriendConnection source,
                        OSF2FHashSearchResp msg) {
                        ServiceConnectionManager.getInstance()
                            .createChannel(source, search, msg, true);
                    // Limit number of multiplexed channels.
                    }
                });
            }
        }
    }

    public void deactivate() {
        ServiceSharingManager.logger.info("deactivating " + toString());
        if (!active) {
            ServiceSharingManager.logger.warning("Tried to deactivate service " + getName()
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
        iscm.close();
    }

    private String getEnabledKey() {
        return CONFIGURATION_PREFIX + serverSearchKey + "_enabled";
    }

    public String getName() {
        return COConfigurationManager.getStringParameter(getNameKey());
    }

    private String getNameKey() {
        return CONFIGURATION_PREFIX + serverSearchKey + "_name";
    }

    int getPort() {
        int port = COConfigurationManager.getIntParameter(getPortKey(), -1);
        return port;
    }

    private String getPortKey() {
        return CONFIGURATION_PREFIX + serverSearchKey + "_port";
    }

    private String getChannelsKey() {
        return CONFIGURATION_PREFIX + "channels";
    }

    public void setName(String name) {
        COConfigurationManager.setParameter(getNameKey(), name);
    }

    public void setPort(int port) {
        COConfigurationManager.setParameter(getPortKey(), port);
    }

    public ClientServiceDTO toDTO() {
        return new ClientServiceDTO(getName(), Long.toHexString(serverSearchKey), getPort());
    }

    @Override
    public String toString() {
        return "Service: " + getName() + " port=" + getPort() + " key=" + serverSearchKey;
    }
}