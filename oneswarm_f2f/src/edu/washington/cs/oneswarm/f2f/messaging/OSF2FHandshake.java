package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FHandshake implements OSF2FMessage, RawMessage {

    private String description = null;
    private final byte version;
    private byte[] reserved = null;
    private DirectByteBuffer buffer;
    private boolean noDelay = true;

    /**
     * Possible supported extensions
     */
    // this includes directory tag lists
    public static final byte SUPPORTS_EXTENDED_FILE_LISTS = 1;
    public static final byte SUPPORTS_CHAT = 2;
    public static final byte SUPPORTS_DHT_LOCATION_HS = 4;
    public static final byte SUPPORTS_UDP = 8;
    /**
     * Protocol extensions we support in the current version. This is a very
     * hacky way to do protocol versioning, but it's what we're using for now to
     * maintain backwards compatibility. TODO: introduce proper versioning in a
     * future handshake message as an option and then exchange an extended
     * handshake message
     */
    public final static byte[] OS_FLAGS = new byte[] {
            SUPPORTS_EXTENDED_FILE_LISTS | SUPPORTS_CHAT | SUPPORTS_DHT_LOCATION_HS | SUPPORTS_UDP,
            0, 0, 0, 0, 0,
            0, 0 };

    public final static byte MESSAGE_LENGTH = (byte) (1 + ONESWARM_PROTOCOL.length() + OS_FLAGS.length);

    public OSF2FHandshake(byte _version, byte[] reserved) {
        this.version = _version;
        this.reserved = reserved;
    }

    public byte[] getFlags() {
        return reserved;
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
        byte len = data.get(DirectByteBuffer.SS_MSG);
        if (len != (byte) ONESWARM_PROTOCOL.length()) {
            throw new MessageException("[" + getID() + "] decode error: payload.get() != "
                    + "(byte)PROTOCOL.length() " + "got " + len + " expected " + MESSAGE_LENGTH);
        }

        byte[] header = new byte[ONESWARM_PROTOCOL.getBytes().length];
        data.get(DirectByteBuffer.SS_MSG, header);

        if (!ONESWARM_PROTOCOL.equals(new String(header))
                && !SPD_HANDSHAKE.equals(new String(header))) {
            throw new MessageException("[" + getID() + "] decode error: invalid protocol given: "
                    + new String(header));
        }

        byte[] reserved = new byte[8];
        data.get(DirectByteBuffer.SS_MSG, reserved);
        // Log.log("successfully decoded OSF2F handshake");
        data.returnToPool();

        return new OSF2FHandshake(version, reserved);
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_HANDSHAKE;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_HANDSHAKE_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_HANDSHAKE;
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
            description = OSF2FMessage.ID_OS_HANDSHAKE + " flags: " + Base32.encode(reserved);
        }

        return description;
    }

    @Override
    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    private void constructBuffer() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.put(DirectByteBuffer.SS_MSG, (byte) ONESWARM_PROTOCOL.length());
            buffer.put(DirectByteBuffer.SS_MSG, ONESWARM_PROTOCOL.getBytes());
            buffer.put(DirectByteBuffer.SS_MSG, reserved);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
    }

    @Override
    public DirectByteBuffer[] getData() {
        this.constructBuffer();

        return new DirectByteBuffer[] { buffer };
    }

    @Override
    public DirectByteBuffer[] getRawData() {
        this.constructBuffer();

        return new DirectByteBuffer[] { buffer };
    }

    @Override
    public int getPriority() {
        return RawMessage.PRIORITY_HIGH;
    }

    @Override
    public boolean isNoDelay() {
        return noDelay;
    }

    @Override
    public Message[] messagesToRemove() {
        return null;
    }

    @Override
    public Message getBaseMessage() {
        return this;
    }

    @Override
    public void setNoDelay() {
        noDelay = true;
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
