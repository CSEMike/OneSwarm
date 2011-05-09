package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FAuthHandshake implements OSF2FAuthMessage, RawMessage {

    private String description = null;
    private final byte version;
    private final byte[] reserved;
    private DirectByteBuffer buffer;
    private boolean noDelay = true;
    public static final byte[] OSA_RESERVED = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };

    public final static byte MESSAGE_LENGTH = (byte) (1 + ONESWARM_AUTH_PROTOCOL.length() + OSA_RESERVED.length);

    public OSF2FAuthHandshake(byte _version, byte[] reserved) {
        this.version = _version;
        this.reserved = reserved;
    }

    public byte[] getFlags() {
        return reserved;
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
        }
        byte len = data.get(DirectByteBuffer.SS_MSG);
        if (len != (byte) ONESWARM_AUTH_PROTOCOL.length()) {
            throw new MessageException("[" + getID() + "] decode error: payload.get() != "
                    + "(byte)PROTOCOL.length() " + "got " + len + " expected " + MESSAGE_LENGTH);
        }

        byte[] header = new byte[ONESWARM_AUTH_PROTOCOL.getBytes().length];
        data.get(DirectByteBuffer.SS_MSG, header);

        if (!ONESWARM_AUTH_PROTOCOL.equals(new String(header))) {
            throw new MessageException("[" + getID() + "] decode error: invalid protocol given: "
                    + new String(header));
        }

        byte[] reserved = new byte[8];
        data.get(DirectByteBuffer.SS_MSG, reserved);
        // Log.log("successfully decoded OSF2F handshake");
        data.returnToPool();

        return new OSF2FAuthHandshake(version, reserved);
    }

    public String getID() {
        return OSF2FAuthMessage.ID_OSA_HANDSHAKE;
    }

    public byte[] getIDBytes() {
        return OSF2FAuthMessage.ID_OSA_HANDSHAKE_BYTES;
    }

    public String getFeatureID() {
        return OSF2FAuthMessage.OSA_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FAuthMessage.SUBID_OSA_HANDSHAKE;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FAuthMessage.ID_OSA_HANDSHAKE;
        }

        return description;
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    private void constructBuffer() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.put(DirectByteBuffer.SS_MSG, (byte) ONESWARM_AUTH_PROTOCOL.length());
            buffer.put(DirectByteBuffer.SS_MSG, ONESWARM_AUTH_PROTOCOL.getBytes());
            buffer.put(DirectByteBuffer.SS_MSG, reserved);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
    }

    public DirectByteBuffer[] getData() {
        this.constructBuffer();

        return new DirectByteBuffer[] { buffer };
    }

    public DirectByteBuffer[] getRawData() {
        this.constructBuffer();

        return new DirectByteBuffer[] { buffer };
    }

    public int getPriority() {
        return RawMessage.PRIORITY_HIGH;
    }

    public boolean isNoDelay() {
        return noDelay;
    }

    public Message[] messagesToRemove() {
        return null;
    }

    public Message getBaseMessage() {
        return this;
    }

    public void setNoDelay() {
        noDelay = true;
    }

    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

}
