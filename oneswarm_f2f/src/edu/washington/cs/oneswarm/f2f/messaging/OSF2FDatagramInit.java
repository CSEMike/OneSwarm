package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.datagram.DatagramEncrytionBase;

/**
 * This class encapsulates the information required at the remote end to set up
 * a udp connection with the current friend.
 * 
 * Information required:
 * Encryption Key: symmectric AES 128 bit key
 * 
 * Initialization vector, 128 bit iv
 * 
 * Hmac key, 20 bytes
 * 
 * @author isdal
 * 
 */
public class OSF2FDatagramInit implements OSF2FMessage {
    private final byte version;

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 4 + DatagramEncrytionBase.HMAC_KEY_LENGTH + 2
            * DatagramEncrytionBase.BLOCK_SIZE + 4;

    private final byte[] encryptionKey;
    private final byte[] iv;
    private final byte[] hmacKey;
    private final int localPort;
    private final int cryptoAlgo;

    public OSF2FDatagramInit(byte version, int cryptoAlgo, byte[] encryptionKey, byte[] iv,
            byte[] hmacKey, int localPort) {
        this.version = version;
        this.cryptoAlgo = cryptoAlgo;
        this.encryptionKey = encryptionKey;
        this.iv = iv;
        this.hmacKey = hmacKey;
        this.localPort = localPort;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getHmacKey() {
        return hmacKey;
    }

    public int getLocalPort() {
        return localPort;
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_DATAGRAM_INIT;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_DATAGRAM_INIT_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_DATAGRAM_INIT;
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
            description = OSF2FMessage.ID_OS_DATAGRAM_INIT + "\t port=" + localPort;
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(SS_MSG, cryptoAlgo);
            buffer.put(DirectByteBuffer.SS_MSG, encryptionKey);
            buffer.put(DirectByteBuffer.SS_MSG, iv);
            buffer.put(DirectByteBuffer.SS_MSG, hmacKey);
            buffer.putInt(DirectByteBuffer.SS_MSG, localPort);
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

        int cryptoAlgo = data.getInt(SS_MSG);

        byte[] eKey = new byte[DatagramEncrytionBase.BLOCK_SIZE];
        data.get(SS_MSG, eKey);

        byte[] initVector = new byte[DatagramEncrytionBase.BLOCK_SIZE];
        data.get(SS_MSG, initVector);

        byte[] hKey = new byte[DatagramEncrytionBase.HMAC_KEY_LENGTH];
        data.get(SS_MSG, hKey);

        int localport = data.getInt(SS_MSG);
        data.returnToPool();
        return new OSF2FDatagramInit(version, cryptoAlgo, eKey, initVector, hKey, localport);
    }

    @Override
    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }
}
