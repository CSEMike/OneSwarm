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

    protected SequenceNumber(int n) {
        this.number = n;
    }

    public int getNum() {
        return number;
    }
}