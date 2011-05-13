package edu.washington.cs.oneswarm.f2f.network;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager.SharedService;

public class ServerServiceConnection extends ServiceConnection {
    private final SharedService serverService;
    private final static Logger logger = Logger.getLogger(ServerServiceConnection.class.getName());

    public ServerServiceConnection(SharedService service, FriendConnection connection,
            int channelId, int pathID) {
        super(connection, channelId, pathID);
        this.serverService = service;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " server service: " + serverService.toString();
    }

    /**
     * Currently we connect to the server when we get the first message data.
     */
    // TODO (isdal): connect to the server on incoming search message and send
    // reply on successful connect?
    @Override
    public void start() {
        logger.fine(getDescription() + " starting");
        if (isStarted()) {
            logger.warning("Tried to start already started service");
            return;
        }
        if (!serverService.isEnabled()) {
            logger.fine("Tried to start disabled connection");
            return;
        }
        started = true;
        serverConnection = serverService.createConnection(new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                logger.fine(ServerServiceConnection.this.getDescription() + " connection failure");
                ServerServiceConnection.this.close("Exception during connect");
            }

            @Override
            public void connectStarted() {
                logger.fine(ServerServiceConnection.this.getDescription() + " connect started");
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
                logger.fine(ServerServiceConnection.this.getDescription() + " connected");
                serverConnection.getIncomingMessageQueue().registerQueueListener(
                        new ServerIncomingMessageListener());
                synchronized (bufferedMessages) {
                    for (OSF2FChannelDataMsg msg : bufferedMessages) {
                        logger.finest("sending queued message: " + msg.getDescription());
                        writeMessageToServerConnection(msg.getData());
                    }
                }
            }

            @Override
            public void exceptionThrown(Throwable error) {
                ServerServiceConnection.this.close("Exception during connect");
            }

            @Override
            public String getDescription() {
                return ServerServiceConnection.this.getDescription() + " connect listener";
            }
        });
    }
}
