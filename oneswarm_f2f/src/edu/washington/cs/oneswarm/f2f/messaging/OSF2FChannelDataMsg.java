package edu.washington.cs.oneswarm.f2f.messaging;

import java.util.logging.Level;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FChannelDataMsg extends OSF2FChannelMsg {

    public static final int BASE_LENGTH = 4;
    private String description = null;
    private final byte version;
    private final int channelID;
    private final DirectByteBuffer[] buffer = new DirectByteBuffer[2];
    private int messageLength;

    public OSF2FChannelDataMsg(byte _version, int channelID, DirectByteBuffer data) {
        super(channelID);
        this.version = _version;
        this.channelID = channelID;
        this.buffer[1] = data;
        updateMessageLength();
    }

    private void updateMessageLength() {
        if (buffer[1] != null) {
            messageLength = BASE_LENGTH + buffer[1].remaining(DirectByteBuffer.SS_MSG);
        } else {
            messageLength = BASE_LENGTH;
        }
    }

    public DirectByteBuffer getPayload() {
        return buffer[1];
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] < " + BASE_LENGTH);
        }

        int channelID = data.getInt(DirectByteBuffer.SS_MSG);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Deserialized: " + getDescription());
        }
        return new OSF2FChannelDataMsg(version, channelID, data);
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_CHANNEL_DATA_MSG;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_CHANNEL_DATA_MSG_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_CHANNEL_DATA_MSG;
    }

    @Override
    public int getType() {
        return Message.TYPE_DATA_PAYLOAD;
    }

    @Override
    public byte getVersion() {
        return version;
    };

    @Override
    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_CHANNEL_DATA_MSG + "\tchannel=" + channelID
                    + "\tbytes=";
            if (buffer[1] != null)
                description += buffer[1].remaining(DirectByteBuffer.SS_MSG);
        }

        return description;
    }

    @Override
    public void destroy() {
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != null) {
                buffer[i].returnToPool();
            }
        }
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer[0] == null) {
            buffer[0] = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, BASE_LENGTH);
            buffer[0].putInt(DirectByteBuffer.SS_MSG, channelID);
            buffer[0].flip(DirectByteBuffer.SS_MSG);
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Serialized: " + getDescription());
        }

        return buffer;

    }

    // used to remove all messages for a given channel from the queue
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj)
            return true;
        if (obj instanceof OSF2FChannelDataMsg) {
            OSF2FChannelDataMsg other = (OSF2FChannelDataMsg) obj;
            if (other.getChannelId() == this.getChannelId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getMessageSize() {
        return messageLength;
    }

    public DirectByteBuffer transferPayload() {
        DirectByteBuffer payload = buffer[1];
        buffer[1] = null;
        return payload;
    }

    public void updatePayload(DirectByteBuffer newPayload) {
        if (buffer[1] != null) {
            buffer[1].returnToPool();
        }
        buffer[1] = newPayload;
        updateMessageLength();
    }
}
