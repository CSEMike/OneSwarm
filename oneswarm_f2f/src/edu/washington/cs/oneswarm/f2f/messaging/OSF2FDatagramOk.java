package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FDatagramOk implements OSF2FMessage {

    private final byte version;

    private final int paddingBytes;
    private String description;
    private final static int MESSAGE_LENGTH = 0;
    private DirectByteBuffer padding;

    /**
     * Datagram used to ack a successful udp connection.
     * 
     * @param paddingBytes
     *            Additional padding bytes to ensure that a maximum size udp
     *            packet can make it through.
     */
    public OSF2FDatagramOk(int paddingBytes) {
        this.version = OSF2FMessage.CURRENT_VERSION;
        this.paddingBytes = paddingBytes;
        if (paddingBytes > 0) {
            padding = DirectByteBufferPool.getBuffer(SS_MSG, paddingBytes);
        }
    }

    public int getPaddingBytesNum() {
        return paddingBytes;
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
        if (padding == null) {
            return new DirectByteBuffer[] {};
        } else {
            padding.put(SS_MSG, new byte[paddingBytes]);
            padding.flip(SS_MSG);
            return new DirectByteBuffer[] { padding };
        }
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {

        int paddingBytes = 0;
        if (data != null) {
            paddingBytes = data.remaining(SS_MSG);
            data.returnToPool();
        }
        return new OSF2FDatagramOk(paddingBytes);
    }

    @Override
    public void destroy() {
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
