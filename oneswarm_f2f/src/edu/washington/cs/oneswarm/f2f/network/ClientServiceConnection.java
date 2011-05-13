package edu.washington.cs.oneswarm.f2f.network;

import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager.ClientService;

public class ClientServiceConnection extends ServiceConnection {
    private final static Logger logger = Logger.getLogger(ClientServiceConnection.class.getName());

    private final ClientService clientService;

    public ClientServiceConnection(ClientService service, NetworkConnection serverConnection,
            FriendConnection connection, int channelId, int pathID) {
        super(connection, channelId, pathID);
        this.clientService = service;
        this.serverConnection = serverConnection;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " client service: " + clientService.toString();
    }

    @Override
    void start() {
        // TODO Auto-generated method stub

    }
}
