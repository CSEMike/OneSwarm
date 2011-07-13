package edu.washington.cs.oneswarm.f2f.network;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.impl.TransportImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;

/**
 * The WriteController will wait( 50) if it attempts
 * to write and there
 * is no data available. This call will notify the
 * waiter to trigger
 * and instant write attempt.
 * 
 * @author isdal
 * 
 */
public class LowLatencyMessageWriter implements OutgoingMessageQueue.MessageQueueListener {
    private final NetworkConnection connection;

    public LowLatencyMessageWriter(NetworkConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean messageAdded(Message message) {
        return true;
    }

    @Override
    public void messageQueued(Message message) {
        if (connection.getTransport() instanceof TransportImpl) {
            ((TransportImpl) (connection.getTransport())).setReadyForWrite();
        }
    }

    @Override
    public void messageRemoved(Message message) {
    }

    @Override
    public void messageSent(Message message) {
    }

    @Override
    public void protocolBytesSent(int byte_count) {
    }

    @Override
    public void dataBytesSent(int byte_count) {
    }

    @Override
    public void flush() {
    }
}