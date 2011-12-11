package edu.washington.cs.oneswarm.f2f.datagram;

import java.io.IOException;
import java.net.DatagramPacket;

/**
 * Interface to the DatagramConnectionManager class used to stup out the class
 * in unit tests. Feel free to use the Impl in code as needed (as long as it
 * doesn't break any tests...).
 * 
 * @author isdal
 * 
 */
interface DatagramConnectionManager {

    void deregister(DatagramConnection conn);

    void send(DatagramPacket packet) throws IOException;

    void register(DatagramConnection connection);

    int getPort();
}