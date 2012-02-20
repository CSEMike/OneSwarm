package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearch;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FHashSearchResp;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection.OverlayRegistrationError;

/**
 * This class manages active service connections (ServiceChannelEndpoints.)
 * Each channel can be used for multiple connections, such that a client can
 * initiate follow-on connections to a service without re-executing a search.
 * 
 * @author willscott
 * 
 */
public class ServiceConnectionManager implements ServiceChannelEndpointDelegate {
    // Singleton Pattern.
    private final static ServiceConnectionManager instance = new ServiceConnectionManager();
    public final static Logger logger = Logger.getLogger(ServiceConnectionManager.class.getName());

    private ServiceConnectionManager() {
    }

    public static ServiceConnectionManager getInstance() {
        return instance;
    }

    private final HashMap<Long, List<ServiceChannelEndpoint>> connections = new HashMap<Long, List<ServiceChannelEndpoint>>();
    private final HashMap<Long, List<ServiceConnection>> services = new HashMap<Long, List<ServiceConnection>>();

    public ServiceChannelEndpoint createChannel(FriendConnection nextHop, OSF2FHashSearch search,
            OSF2FHashSearchResp response, boolean outgoing) {
        ServiceChannelEndpoint channel = new ServiceChannelEndpoint(nextHop, search,
                response, outgoing);
        try {
            nextHop.registerOverlayTransport(channel);
        } catch (OverlayRegistrationError e) {
            logger.warning("got an error when registering outgoing transport: " + e.getMessage());
            return channel;
        }

        this.addChannel(channel);
        return channel;
    }

    public void addChannel(ServiceChannelEndpoint channel) {
        logger.fine("Network Channel registered with Connection Manager");
        Long key = channel.getServiceKey();
        if (!this.connections.containsKey(key)) {
            registerKey(key);
        }
        if (this.connections.get(key).contains(channel)) {
            logger.info("Attempting to register existing channel:" + channel);
            return;
        }
        this.connections.get(key).add(channel);
        channel.addDelegate(this);
        if (this.services.get(key).size() > 0) {
            for (ServiceConnection service : this.services.get(key)) {
                logger.finest("Channel added to existing service: " + service.getDescription());
                service.addChannel(channel);
            }
        }
    }

    private void registerKey(Long key) {
        List<ServiceChannelEndpoint> channel = new ArrayList<ServiceChannelEndpoint>();
        this.connections.put(key, channel);
        List<ServiceConnection> service = new ArrayList<ServiceConnection>();
        this.services.put(key, service);
    }

    public Collection<ServiceChannelEndpoint> getChannelsForService(long key) {
        return this.connections.get(Long.valueOf(key));
    }

    /* ServiceChannelEndpointDelegate implementation. */
    @Override
    public void channelDidClose(ServiceChannelEndpoint sender) {
        Long key = sender.getServiceKey();
        if (!this.connections.containsKey(key)) {
            logger.info("Attempting to deregister channel for unknown service.");
            return;
        }
        synchronized (this.connections) {
            List<ServiceChannelEndpoint> list = this.connections.get(key);
            if (list == null) {
                return;
            }
            list.remove(sender);
            if (list.size() == 0) {
                logger.fine("All service connections closed for key " + key);
                this.connections.remove(key);
            }
        }
    }

    @Override
    public void channelDidConnect(ServiceChannelEndpoint sender) {
    }

    @Override
    public void channelIsReady(ServiceChannelEndpoint sender) {
    }

    @Override
    public boolean channelGotMessage(ServiceChannelEndpoint sender, OSF2FServiceDataMsg msg) {
        // Alert the service manager when a new flow is established.
        if (msg.isSyn()) {
            logger.info("New Flow Established over " + sender.getChannelId());
            long serviceKey = sender.getServiceKey();
            SharedService ss = ServiceSharingManager.getInstance().getSharedService(serviceKey);
            List<ServiceConnection> existing = services.get(serviceKey);
            short subchannel = 0;
            if (existing == null) {
                services.put(serviceKey, new ArrayList<ServiceConnection>());
            } else {
                for (ServiceConnection c : existing) {
                    if (c.subchannelId == msg.getSubchannel()) {
                        // Ignore duplicate syn messages.
                        return false;
                    }
                }
                subchannel = msg.getSubchannel();
            }
            NetworkConnection outgoingConnection = ss.createConnection();
            ServiceConnection c = new ServiceConnection(false, subchannel, outgoingConnection);
            this.services.get(serviceKey).add(c);
            for (ServiceChannelEndpoint channel : this.getChannelsForService(serviceKey)) {
                c.addChannel(channel);
            }
            c.channelGotMessage(sender, msg);
            return true;
        } else if (msg.isRst()) {
            // TODO(willscott): Implement explicit channel closing.
            return true;
        }
        return false;
    }

    public boolean requestService(NetworkConnection incomingConnection, long serverSearchKey) {
        // Create a new sub flow if channels exist, or note the request for when
        // one does.
        Collection<ServiceChannelEndpoint> channels = this.getChannelsForService(serverSearchKey);
        if (channels != null && channels.size() > 0) {
            short subchannel = (short) services.get(serverSearchKey).size();
            ServiceConnection c = new ServiceConnection(true, subchannel, incomingConnection);
            for (ServiceChannelEndpoint channel : channels) {
                c.addChannel(channel);
            }
            services.get(serverSearchKey).add(c);
            logger.fine("Service requested - existing channel found. Search Skipped.");
            return true;
        } else {
            registerKey(serverSearchKey);
            ServiceConnection c = new ServiceConnection(true, (short)0, incomingConnection);
            services.get(serverSearchKey).add(c);
            logger.fine("Service requested - existing channel not present. Search Needed.");
            return false;
        }
    }

}
