package com.aelitis.net.udp.uc.impl;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public interface ExternalUdpPacketHandler {

    public boolean packetReceived(DatagramPacket packet);

    public void socketUpdated(DatagramSocket socket);
}
