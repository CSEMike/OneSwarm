package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FAuthRequest implements OSF2FAuthMessage {

    public enum AuthType {
        KEY(0), PIN(1);

        private final int id;

        private AuthType(int id) {
            this.id = id;
        }

        public int getID() {
            return id;
        }

        public static AuthType getFromID(int id) {
            switch (id) {
            case 0:
                return KEY;
            case 1:
                return PIN;
            default:
                throw new RuntimeException("unknown auth type");
            }
        }
    }

    private final byte version;
    private final AuthType authType;
    private String description;
    private DirectByteBuffer buffer;
    private final static int BASE_LENGTH = 4;
    private final byte[] payload;

    public OSF2FAuthRequest(byte version, AuthType authType, byte[] payload) {
        this.authType = authType;
        this.version = version;
        this.payload = payload;
    }

    public OSF2FAuthRequest clone() {
        return new OSF2FAuthRequest(this.getVersion(), this.getAuthType(), this.getPayload());
    }

    public byte[] getPayload() {
        return payload;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public String getID() {
        return OSF2FAuthMessage.ID_OSA_AUTH_REQUEST;
    }

    public byte[] getIDBytes() {
        return OSF2FAuthMessage.ID_OSA_AUTH_REQUEST_BYTES;
    }

    public String getFeatureID() {
        return OSF2FAuthMessage.OSA_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FAuthMessage.SUBID_OSA_AUTH_REQUEST;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public byte getVersion() {
        return version;
    };

    public String getDescription() {
        if (description == null) {
            description = OSF2FAuthMessage.ID_OSA_AUTH_REQUEST + "auth_request: " + authType;
        }

        return description;
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, getMessageSize());
            buffer.putInt(DirectByteBuffer.SS_MSG, authType.getID());
            if (payload != null) {
                buffer.put(DirectByteBuffer.SS_MSG, payload);
            }

            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + BASE_LENGTH);
        }
        int response = data.getInt(DirectByteBuffer.SS_MSG);

        byte[] q = new byte[data.remaining(DirectByteBuffer.SS_MSG)];

        data.returnToPool();
        return new OSF2FAuthRequest(version, AuthType.getFromID(response), q);
    }

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public int getMessageSize() {
        int size = BASE_LENGTH;
        if (payload != null) {
            size += payload.length;
        }
        return size;
    }
}
