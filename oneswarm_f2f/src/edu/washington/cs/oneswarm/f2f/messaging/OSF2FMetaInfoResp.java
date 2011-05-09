package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FMetaInfoResp extends OSF2FChannelMsg {

    private static final int BASE_LENGTH = 21;
    private String description = null;
    private byte version;

    private final int channelId;
    private final byte type;
    private final long infoHashHash;
    private final int startByte;
    private final int totalMetaInfoLength;
    private final byte[] metainfo;

    private DirectByteBuffer buffer;

    public OSF2FMetaInfoResp(byte _version, int channelId, byte type, long infohashhash,
            int startByte, int totalBytes, byte[] payload) {
        super(channelId);
        this.version = _version;
        this.channelId = channelId;
        this.type = type;
        this.infoHashHash = infohashhash;
        this.startByte = startByte;
        this.totalMetaInfoLength = totalBytes;
        this.metainfo = payload;
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] to small ");
        }
        int _channel = data.getInt(DirectByteBuffer.SS_MSG);
        byte _type = data.get(DirectByteBuffer.SS_MSG);
        long _hashhash = data.getBuffer(DirectByteBuffer.SS_MSG).getLong();
        int _start = data.getInt(DirectByteBuffer.SS_MSG);
        int _total = data.getInt(DirectByteBuffer.SS_MSG);
        byte[] i = new byte[data.remaining(DirectByteBuffer.SS_MSG)];
        data.get(DirectByteBuffer.SS_MSG, i);
        data.returnToPool();

        return new OSF2FMetaInfoResp(version, _channel, _type, _hashhash, _start, _total, i);
    }

    public byte[] getMetaInfo() {
        return metainfo;
    }

    public byte getMetaInfoType() {
        return type;
    }

    public String getID() {
        return OSF2FMessage.ID_OS_METAINFO_RESP;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_METAINFO_RESP_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_METAINFO_RESP;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public int getStartByte() {
        return startByte;
    }

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_METAINFO_RESP + " startByte=" + startByte
                    + " channel=" + Integer.toHexString(channelId) + " infohashhash="
                    + Long.toHexString(infoHashHash);
            if (metainfo != null) {
                description += " len=" + metainfo.length;
            }
        }

        return description;
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, metainfo.length
                    + BASE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, channelId);
            buffer.put(DirectByteBuffer.SS_MSG, type);
            buffer.getBuffer(DirectByteBuffer.SS_MSG).putLong(infoHashHash);
            buffer.putInt(DirectByteBuffer.SS_MSG, startByte);
            buffer.putInt(DirectByteBuffer.SS_MSG, totalMetaInfoLength);
            buffer.put(DirectByteBuffer.SS_MSG, metainfo);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }

        return new DirectByteBuffer[] { buffer };
    }

    public int getTotalMetaInfoLength() {
        return totalMetaInfoLength;
    }

    public int getMessageSize() {
        return BASE_LENGTH + metainfo.length;
    }

    public long getInfoHashHash() {
        return infoHashHash;
    }

}
