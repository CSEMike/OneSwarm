package edu.washington.cs.oneswarm.f2f.servicesharing;

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
    private final int channelid;

    protected SequenceNumber(int n, int channelid) {
        this.number = n;
        this.channelid = channelid;
    }

    public int getNum() {
        return number;
    }

    public int getChannel() {
        return channelid;
    }
}