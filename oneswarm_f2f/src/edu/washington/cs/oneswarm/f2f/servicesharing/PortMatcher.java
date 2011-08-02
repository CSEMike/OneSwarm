package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.nio.ByteBuffer;

import com.aelitis.azureus.core.networkmanager.NetworkManager.ByteMatcher;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;

class PortMatcher implements ByteMatcher {

    private final int port;

    public PortMatcher(int port) {
        this.port = port;
    }

    @Override
    public byte[][] getSharedSecrets() {
        return null;
    }

    @Override
    public int getSpecificPort() {
        return port;
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
    public int maxSize() {
        return 0;
    }

    @Override
    public Object minMatches(TransportHelper transport, ByteBuffer to_compare, int port) {
        return matches(transport, to_compare, port);
    }

    @Override
    public int minSize() {
        return 0;
    }
}