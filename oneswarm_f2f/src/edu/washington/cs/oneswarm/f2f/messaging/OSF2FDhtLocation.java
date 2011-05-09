package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FDhtLocation implements OSF2FMessage {

    /**
     * the location at which the remote host should read dht values from
     * 
     * @return
     */
    public byte[] getReadLocation() {
        return readLocation;
    }

    /**
     * the location from which the remote hosts should write dht values to
     * 
     * @return
     */
    public byte[] getWriteLocation() {
        return writeLocation;
    }

    private final byte version;

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 40;
    private final byte[] readLocation;
    private final byte[] writeLocation;

    public OSF2FDhtLocation(byte version, byte[] readLocation, byte[] writeLocation) {
        this.version = version;
        this.readLocation = readLocation;
        this.writeLocation = writeLocation;
    }

    public String getID() {
        return OSF2FMessage.ID_OS_DHT_LOCATION;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_DHT_LOCATION_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_DHT_LOCATION;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_DHT_LOCATION;
        }

        return description;
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.put(DirectByteBuffer.SS_MSG, readLocation);
            buffer.put(DirectByteBuffer.SS_MSG, writeLocation);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
        }
        byte[] readLoc = new byte[20];
        data.get(SS_MSG, readLoc);

        byte[] writeLoc = new byte[20];
        data.get(SS_MSG, writeLoc);
        data.returnToPool();
        return new OSF2FDhtLocation(version, readLoc, writeLoc);
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
