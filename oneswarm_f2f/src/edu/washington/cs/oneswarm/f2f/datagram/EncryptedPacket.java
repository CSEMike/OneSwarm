package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;

class EncryptedPacket {
    public EncryptedPacket(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    final long sequenceNumber;
    ByteBuffer payload;
    int length;

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String toString() {
        return "EncryptedPacket: sequence=" + sequenceNumber + " payload: " + payload.remaining()
                + " bytes";
    }

    public int getLength() {
        return length;
    }
}
