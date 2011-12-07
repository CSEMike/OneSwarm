package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;

class EncryptedPacket {
    public EncryptedPacket(long sequenceNumber, ByteBuffer payload) {
        super();
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    final long sequenceNumber;
    final ByteBuffer payload;

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public String toString() {
        return "EncryptedPacket: sequence=" + sequenceNumber + " payload: " + payload.remaining()
                + " bytes";
    }
}
