package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.IncomingMessageQueue;
import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.TransportBase;

class ListenedNetworkConnection implements NetworkConnection {
    final NetworkConnection underlyingConnection;
    final ConnectionListener listener;

    private class MultiConnectionListener implements ConnectionListener {
        final ConnectionListener a;
        final ConnectionListener b;

        public MultiConnectionListener(ConnectionListener a, ConnectionListener b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void connectStarted() {
            a.connectStarted();
            b.connectStarted();
        }

        @Override
        public void connectSuccess(ByteBuffer remaining_initial_data) {
            a.connectSuccess(remaining_initial_data);
            b.connectSuccess(remaining_initial_data);
        }

        @Override
        public void connectFailure(Throwable failure_msg) {
            a.connectFailure(failure_msg);
            b.connectFailure(failure_msg);
        }

        @Override
        public void exceptionThrown(Throwable error) {
            a.exceptionThrown(error);
            b.exceptionThrown(error);
        }

        @Override
        public String getDescription() {
            return "[" + a.getDescription() + ", " + b.getDescription() + "]";
        }
    }

    public ListenedNetworkConnection(NetworkConnection n, ConnectionListener l) {
        this.underlyingConnection = n;
        this.listener = l;
    }

    @Override
    public ConnectionEndpoint getEndpoint() {
        return underlyingConnection.getEndpoint();
    }

    @Override
    public void notifyOfException(Throwable error) {
        underlyingConnection.notifyOfException(error);
    }

    @Override
    public OutgoingMessageQueue getOutgoingMessageQueue() {
        return underlyingConnection.getOutgoingMessageQueue();
    }

    @Override
    public IncomingMessageQueue getIncomingMessageQueue() {
        return underlyingConnection.getIncomingMessageQueue();
    }

    @Override
    public TransportBase getTransportBase() {
        return underlyingConnection.getTransportBase();
    }

    @Override
    public int getMssSize() {
        return underlyingConnection.getMssSize();
    }

    @Override
    public boolean isLANLocal() {
        return underlyingConnection.isLANLocal();
    }

    @Override
    public void setUploadLimit(int limit) {
        underlyingConnection.setUploadLimit(limit);
    }

    @Override
    public int getUploadLimit() {
        return underlyingConnection.getUploadLimit();
    }

    @Override
    public void setDownloadLimit(int limit) {
        underlyingConnection.setDownloadLimit(limit);
    }

    @Override
    public int getDownloadLimit() {
        return underlyingConnection.getDownloadLimit();
    }

    @Override
    public LimitedRateGroup[] getRateLimiters(boolean upload) {
        return underlyingConnection.getRateLimiters(upload);
    }

    @Override
    public void addRateLimiter(LimitedRateGroup limiter, boolean upload) {
        underlyingConnection.addRateLimiter(limiter, upload);
    }

    @Override
    public void removeRateLimiter(LimitedRateGroup limiter, boolean upload) {
        underlyingConnection.removeRateLimiter(limiter, upload);
    }

    @Override
    public String getString() {
        return underlyingConnection.getString();
    }

    @Override
    public void connect(boolean high_priority, ConnectionListener listener) {
        underlyingConnection.connect(high_priority, new MultiConnectionListener(this.listener,
                listener));
    }

    @Override
    public void connect(ByteBuffer initial_outbound_data, boolean high_priority,
            ConnectionListener listener) {
        underlyingConnection.connect(initial_outbound_data, high_priority,
                new MultiConnectionListener(this.listener, listener));
    }

    @Override
    public void close() {
        underlyingConnection.close();
    }

    @Override
    public void startMessageProcessing() {
        underlyingConnection.startMessageProcessing();
    }

    @Override
    public void enableEnhancedMessageProcessing(boolean enable) {
        underlyingConnection.enableEnhancedMessageProcessing(enable);
    }

    @Override
    public Transport detachTransport() {
        return underlyingConnection.detachTransport();
    }

    @Override
    public Transport getTransport() {
        return underlyingConnection.getTransport();
    }

    @Override
    public boolean isConnected() {
        return underlyingConnection.isConnected();
    }
}