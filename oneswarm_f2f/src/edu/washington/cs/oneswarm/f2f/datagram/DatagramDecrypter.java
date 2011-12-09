package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DatagramDecrypter extends DatagramEncrytionBase {
    public final static Logger logger = Logger.getLogger(DatagramDecrypter.class.getName());

    private long currentAesCounter = 0;

    public DatagramDecrypter(byte[] encryptionKey, byte[] iv, byte[] hmacKey)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        ivSpec = new IvParameterSpec(iv);
        key = new SecretKeySpec(encryptionKey, "AES");
        cipher = Cipher.getInstance(ENCR_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        this.hmac_key = hmacKey;

        logger.finest("DatagramDecrypter created");
    }

    public boolean decrypt(byte[] data, int offset, int length, ByteBuffer decryptBuffer)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException, ShortBufferException {
        if (data.length < BLOCK_SIZE + SEQUENCE_NUMBER_BYTES) {
            return false;
        }
        ByteBuffer payload = ByteBuffer.wrap(data, offset + SEQUENCE_NUMBER_BYTES, length
                - HMAC_SIZE - SEQUENCE_NUMBER_BYTES);
        ByteBuffer incomingDigest = ByteBuffer.wrap(data, offset + length - HMAC_SIZE, HMAC_SIZE);
        ByteBuffer sequnceNumber = ByteBuffer.wrap(data, 0, SEQUENCE_NUMBER_BYTES);

        // Check the sha1 digest.
        sha1.reset();
        sha1.update(hmac_key);
        sha1.update(sequnceNumber);
        sha1.update(payload);
        byte[] caclulatedDigest = sha1.getDigest();

        for (int i = 0; i < caclulatedDigest.length; i++) {
            byte b = incomingDigest.get();
            if (caclulatedDigest[i] != b) {
                logger.warning("Encrypted UDP packet hash error");
                return false;
            }
        }

        long sequenceNumber = sequnceNumber.getLong();

        // Check for packet reordering and retransmissions.
        if (sequenceNumber < currentAesCounter) {
            logger.finer(String.format("old packet, min sequence number accepted=%s, received=%s",
                    currentAesCounter, sequenceNumber));
            return false;
        } else if (sequenceNumber != currentAesCounter) {
            logger.finer(String.format("lost packet(s), expected=%s, received=%s",
                    currentAesCounter, sequenceNumber));
            // Update the iv
            ivSpec = setSequenceNumber(sequenceNumber, ivSpec.getIV());
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }

        if ((payload.remaining() % DatagramDecrypter.BLOCK_SIZE) != 0) {
            logger.warning("payload length is not an even number of blocks: " + payload.remaining());
            return false;
        }

        // Decrypt the data
        int decryptedBytes = cipher.update(payload, decryptBuffer);
        currentAesCounter += decryptedBytes / BLOCK_SIZE;
        decryptBuffer.flip();

        // Strip the padding
        decryptBuffer.position(decryptBuffer.limit() - 1);
        byte padLength = decryptBuffer.get();

        decryptBuffer.position(decryptBuffer.limit() - padLength);
        for (int i = 0; i < padLength; i++) {
            byte pad = decryptBuffer.get();
            if (pad != padLength) {
                logger.warning("PADDING ERROR at pos: " + pad + "!=" + padLength);
                return false;
            }
        }
        decryptBuffer.limit(decryptBuffer.limit() - padLength);
        decryptBuffer.flip();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(String.format(
                    "Packet decrypted, in_bytes=%d, out_bytes=%d, packet_sequence_num=%d", length,
                    decryptBuffer.remaining(), currentAesCounter));
        }
        return true;
    }
}