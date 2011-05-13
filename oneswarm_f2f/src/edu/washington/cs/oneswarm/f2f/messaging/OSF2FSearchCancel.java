package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FSearchCancel implements OSF2FSearch {

    private final byte version;
    private final int searchID;

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 4;

    public OSF2FSearchCancel(byte version, int searchID) {
        this.version = version;
        this.searchID = searchID;
    }

    @Override
    public OSF2FSearchCancel clone() {
        return new OSF2FSearchCancel(this.getVersion(), this.getSearchID());
    }

    @Override
    public int getSearchID() {
        return searchID;
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_SEARCH_CANCEL;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_SEARCH_CANCEL_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_SEARCH_CANCEL;
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
            description = OSF2FMessage.ID_OS_SEARCH_CANCEL + "\tsearch="
                    + Integer.toHexString(searchID);
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, searchID);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

    @Override
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

    @Override
    public void destroy() {
        if (buffer != null) {
            buffer.returnToPool();
        }
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

    @Override
    public int getValueID() {
        return 0;
    }
}
