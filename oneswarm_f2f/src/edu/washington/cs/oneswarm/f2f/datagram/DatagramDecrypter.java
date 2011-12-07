package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DatagramDecrypter extends DatagramEncrytionBase {
    public final static Logger logger = Logger.getLogger(DatagramDecrypter.class.getName());

    private long prevSequenceNumber = 0;

    public DatagramDecrypter(byte[] encryptionKey, byte[] iv, byte[] hmacKey)
            throws InvalidKeyException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchPaddingException {
        System.out.println("key=" + encryptionKey.length + " iv=" + iv.length + " hmac="
                + hmacKey.length);
        ivSpec = new IvParameterSpec(iv);
        key = new SecretKeySpec(encryptionKey, "AES");
        cipher = Cipher.getInstance(ENCR_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        mac = Mac.getInstance(HMAC_ALGO);
        macKey = new SecretKeySpec(hmacKey, HMAC_ALGO);
        mac.init(macKey);
        logger.finest("DatagramEncrypter created");
    }

    public boolean decrypt(EncryptedPacket packet, ByteBuffer destination)
            throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, ShortBufferException {

        logger.finest("decrypting packet: " + packet.toString());
        // Check for packet reordering and retransmissions.
        long expectedSequenceNumber = prevSequenceNumber + 1;
        if (packet.sequenceNumber <= prevSequenceNumber) {
            logger.finer(String.format("old packet, min sequence number accepted=%s, received=%s",
                    expectedSequenceNumber, packet.sequenceNumber));
            return false;
        } else if (packet.sequenceNumber != expectedSequenceNumber) {
            logger.finer(String.format("lost packet(s), expected=%s, received=%s",
                    expectedSequenceNumber, packet.sequenceNumber));
        }
        prevSequenceNumber = packet.sequenceNumber;
        int payloadSize = packet.payload.remaining();

        // Update the iv
        ivSpec = setSequenceNumber(packet.sequenceNumber, ivSpec.getIV());
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        // Decryption step,
        destination.clear();
        destination.limit(payloadSize);
        cipher.update(packet.payload, destination);

        // Set limit and pos for reading payload only (without HMAC).
        destination.limit(payloadSize - mac.getMacLength());
        destination.position(0);
        mac.update(destination);
        destination.position(0);

        // Set limit and pos for reading only the HMAC.
        ByteBuffer incomingHash = destination.duplicate();
        incomingHash.position(payloadSize - mac.getMacLength());
        incomingHash.limit(payloadSize);

        ByteBuffer calculatedHash = ByteBuffer.wrap(mac.doFinal());

        // Verify correct hash
        if (!calculatedHash.equals(incomingHash)) {
            logger.warning("Encrypted UDP packet hash error");
            return false;
        }
        logger.finest(String.format(
                "Packet decrypted, in_bytes=%d, out_bytes=%d, packet_sequence_num=%d", payloadSize,
                destination.remaining(), packet.sequenceNumber));
        return true;
    }

    public boolean decrypt(byte[] data, int offset, int length, ByteBuffer decryptBuffer)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException, ShortBufferException {
        if (data.length < 8) {
            return false;
        }
        ByteBuffer payload = ByteBuffer.wrap(data, offset, length);
        long sequenceNumber = payload.getLong();

        return decrypt(new EncryptedPacket(sequenceNumber, payload), decryptBuffer);
    }
}