package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A sequence number for a service packet.
 * If Erasure coding is used, logic will be needed at this level to support
 * reconstructing damaged streams, and to figure out what needs to be resent. At
 * present, sequence numbers are simply represented by integers.
 * 
 * @author willscott
 * 
 */

class SequenceNumber {
    private final int number;
    private final short flow;
    private final List<Integer> channels;
    private boolean acked;

    protected SequenceNumber(int n, short flow) {
        this.number = n;
        this.flow = flow;
        this.channels = Collections.synchronizedList(new ArrayList<Integer>());
        this.acked = false;
    }

    public void addChannel(int channelId) {
        channels.add(new Integer(channelId));
    }

    public int getNum() {
        return number;
    }

    public boolean isAcked() {
        return acked;
    }

    public void ack() {
        this.acked = true;
    }

    public List<Integer> getChannels() {
        return new ArrayList<Integer>(channels);
    }

    public void removeChannel(int channelId) {
        this.channels.remove(new Integer(channelId));
    }

    public short getFlow() {
        return flow;
    }

    @Override
    public String toString() {
        return "[message " + this.number + "." + this.flow + "]";
    }
}