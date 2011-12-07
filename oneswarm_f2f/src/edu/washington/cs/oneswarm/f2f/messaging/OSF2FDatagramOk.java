package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FDatagramOk implements OSF2FMessage {

    private final byte version;

    private String description;
    private final static int MESSAGE_LENGTH = 0;

    public OSF2FDatagramOk() {
        this.version = OSF2FMessage.CURRENT_VERSION;
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_DATAGRAM_OK;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_DATAGRAM_OK_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_DATAGRAM_OK;
    }

    @Override
    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    @Override
    public byte getVersion() {
        return version;
    };

    @Override
    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_DATAGRAM_OK;
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        return new DirectByteBuffer[] {};
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data != null && data.hasRemaining(DirectByteBuffer.SS_MSG)) {
            throw new MessageException("[" + getID() + "] decode error: payload not empty ["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "]");
        }
        if (data != null)
            data.returnToPool();

        return new OSF2FDatagramOk();
    }

    @Override
    public void destroy() {
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
