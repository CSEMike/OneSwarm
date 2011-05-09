package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FChannelReset extends OSF2FChannelMsg {

    private String description = null;
    private final byte version;
    private final int channelID;
    private DirectByteBuffer buffer = null;
    private final static int MESSAGE_LENGTH = 4;

    public OSF2FChannelReset(byte _version, int channelID) {
        super(channelID);

        this.version = _version;
        this.channelID = channelID;
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
        }

        int number = data.getInt(DirectByteBuffer.SS_MSG);
        data.returnToPool();
        return new OSF2FChannelReset(version, number);
    }

    public String getID() {
        return OSF2FMessage.ID_OS_CHANNEL_RST;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_CHANNEL_RST_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_CHANNEL_RST;
    }

    public int getType() {
        return Message.TYPE_DATA_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_CHANNEL_RST + "\tchannel=" + channelID;
        }

        return description;
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, channelID);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }

        return new DirectByteBuffer[] { buffer };
    }

    public int getMessageSize() {

        return MESSAGE_LENGTH;
    }

}
