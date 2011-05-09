package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FMetaInfoReq extends OSF2FChannelMsg {

    private byte version;
    private final int channelId;
    private final byte type;
    private final int startByte;
    private final byte[] infohash;

    private String description;
    private DirectByteBuffer buffer;

    private final static int METAINFO_LENGTH = 20;
    private final static int MESSAGE_LENGTH = METAINFO_LENGTH + 9;

    public OSF2FMetaInfoReq(byte version, int channelId, byte type, int startByte, byte[] infohash) {
        super(channelId);
        this.version = version;
        this.channelId = channelId;
        this.type = type;
        this.startByte = startByte;
        this.infohash = infohash;
    }

    public OSF2FMetaInfoReq clone() {
        return new OSF2FMetaInfoReq(OSF2FMessage.CURRENT_VERSION, channelId, type, startByte,
                infohash);
    }

    public byte[] getInfoHash() {
        return infohash;
    }

    public byte getMetaInfoType() {
        return type;
    }

    public int getStartByte() {
        return startByte;
    }

    public String getID() {
        return OSF2FMessage.ID_OS_METAINFO_REQ;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_METAINFO_REQ_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_METAINFO_REQ;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_METAINFO_REQ + " startByte=" + startByte + " channel="
                    + Integer.toHexString(channelId);
        }

        return description;
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, channelId);
            buffer.put(DirectByteBuffer.SS_MSG, type);
            buffer.putInt(DirectByteBuffer.SS_MSG, startByte);
            buffer.put(DirectByteBuffer.SS_MSG, infohash);
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

        int _channel = data.getInt(DirectByteBuffer.SS_MSG);
        byte _type = data.get(DirectByteBuffer.SS_MSG);
        int _start = data.getInt(DirectByteBuffer.SS_MSG);
        byte[] _hash = new byte[METAINFO_LENGTH];
        data.get(DirectByteBuffer.SS_MSG, _hash);

        data.returnToPool();
        return new OSF2FMetaInfoReq(version, _channel, _type, _start, _hash);
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
