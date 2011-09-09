package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FSearchCancel extends OSF2FSearch implements OSF2FMessage {

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 4;

    public OSF2FSearchCancel(byte version, int searchID) {
        super(version, searchID);
    }

    public OSF2FSearchCancel clone() {
        return new OSF2FSearchCancel(this.getVersion(), this.getSearchID());
    }

    public String getID() {
        return OSF2FMessage.ID_OS_SEARCH_CANCEL;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_SEARCH_CANCEL_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_SEARCH_CANCEL;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_SEARCH_CANCEL + "\tsearch="
                    + Integer.toHexString(getSearchID());
        }

        return description;
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, getSearchID());
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
        int search = data.getInt(DirectByteBuffer.SS_MSG);
        data.returnToPool();
        return new OSF2FSearchCancel(version, search);
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

    public int getValueID() {
        return 0;
    }
}
