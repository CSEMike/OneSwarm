package edu.washington.cs.oneswarm.f2f.datagram;

import java.net.InetAddress;

import com.aelitis.azureus.core.peermanager.messaging.Message;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FDatagramOk;

public interface DatagramListener {

    public InetAddress getRemoteIp();

    public void datagramDecoded(Message message, int size);

    public void sendDatagramOk(OSF2FDatagramOk osf2fDatagramOk);

    public void initDatagramConnection();

    public boolean isLanLocal();
}
