package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

public class DatagramEncrypter extends DatagramEncrytionBase {
    public final static Logger logger = Logger.getLogger(DatagramEncrypter.class.getName());

    private long ctrRoundCount = 0;
    private final byte[] paddingBuffer = new byte[BLOCK_SIZE];
    private final ByteBuffer paddingBB;

    public DatagramEncrypter() throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecureRandom random = new SecureRandom();
        ivSpec = createCtrIvForAES(ctrRoundCount, random);

        // Create the encryption key and the cipher.
        key = createKeyForAES(AES_KEY_LENGTH, random);
        cipher = Cipher.getInstance(ENCR_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        // Create the mac key
        hmac_key = new byte[HMAC_KEY_LENGTH];
        random.nextBytes(hmac_key);

        paddingBB = ByteBuffer.wrap(paddingBuffer);
        logger.fine("DatagramEncrypter created");

    }

    public EncryptedPacket encrypt(ByteBuffer unencryptedPayload, byte[] destination)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException,
            IllegalStateException, InvalidKeyException, InvalidAlgorithmParameterException {
        return encrypt(new ByteBuffer[] { unencryptedPayload }, destination);
    }

    public EncryptedPacket encrypt(ByteBuffer[] unencryptedPayload, byte[] destination)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException,
            IllegalStateException, InvalidKeyException, InvalidAlgorithmParameterException {

        EncryptedPacket packet = new EncryptedPacket(ctrRoundCount);

        ByteBuffer payloadBuffer = ByteBuffer.wrap(destination);

        // Write the counter value for decoding the packet.
        payloadBuffer.putLong(ctrRoundCount);
        int packetLength = SEQUENCE_NUMBER_BYTES;

        int inputBytes = 0;
        // Encrypt the payload.
        for (int i = 0; i < unencryptedPayload.length; i++) {
            inputBytes += cipher.update(unencryptedPayload[i], payloadBuffer);
        }

        // Add the padding for this packet.
        preparePaddingBB(inputBytes);

        // Encrypt the padding into the packet.
        inputBytes += cipher.update(paddingBB, payloadBuffer);

        // Keep the counter field in sync with the aes internal counter.
        assert (inputBytes % BLOCK_SIZE == 0);
        ctrRoundCount += inputBytes / BLOCK_SIZE;
        packetLength += inputBytes;

        // Prepare the buffer for reading the current content.
        payloadBuffer.flip();

        // Calculate the sha1 digest.
        sha1.reset();
        sha1.update(hmac_key);
        sha1.update(payloadBuffer);

        // Add the sha1 digest
        payloadBuffer.limit(packetLength + HMAC_SIZE);
        payloadBuffer.position(packetLength);
        byte[] digestBytes = sha1.getDigest();
        payloadBuffer.put(digestBytes);
        packetLength += digestBytes.length;

        // Prepare for reading.
        payloadBuffer.rewind();
        packet.payload = payloadBuffer;
        packet.length = packetLength;

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "Packet encrypted, in_bytes=%d, out_bytes=%d, packetnum=%d", inputBytes,
                    payloadBuffer.remaining(), packet.getSequenceNumber()));
        }

        return packet;
    }

    private void preparePaddingBB(int inputBytes) {
        byte paddingLen = (byte) (BLOCK_SIZE - inputBytes % BLOCK_SIZE);
        if (paddingLen == 0) {
            paddingLen = BLOCK_SIZE;
        }
        Arrays.fill(paddingBuffer, 0, paddingLen, paddingLen);
        paddingBB.clear();
        paddingBB.limit(paddingLen);
    }

    public byte[] getKey() {
        return key.getEncoded();
    }

    public byte[] getIv() {
        return ivSpec.getIV();
    }

    public byte[] getHmac() {
        return hmac_key;
    }
}