package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class DatagramEncrypter extends DatagramEncrytionBase {
    public final static Logger logger = Logger.getLogger(DatagramEncrypter.class.getName());

    private long packetCount = 0;

    public DatagramEncrypter() throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecureRandom random = new SecureRandom();
        ivSpec = createCtrIvForAES(packetCount, random);

        // Create the encryption key and the cipher.
        key = createKeyForAES(AES_KEY_LENGTH, random);
        cipher = Cipher.getInstance(ENCR_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        // Create the mac key and mac
        byte[] hmac_key = new byte[HMAC_KEY_LENGTH];
        random.nextBytes(hmac_key);
        mac = Mac.getInstance(HMAC_ALGO);
        macKey = new SecretKeySpec(hmac_key, HMAC_ALGO);
        logger.fine("DatagramEncrypter created");
    }

    public EncryptedPacket encrypt(ByteBuffer input, ByteBuffer payloadBuffer)
            throws ShortBufferException, IllegalBlockSizeException, BadPaddingException,
            IllegalStateException, InvalidKeyException, InvalidAlgorithmParameterException {
        packetCount++;
        int inputBytes = input.remaining();
        // Update the iv
        ivSpec = setSequenceNumber(packetCount, ivSpec.getIV());
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        // Save the read position so it can be reused for the mac computation.
        int oldPos = input.position();
        cipher.update(input, payloadBuffer);

        // Restore the read position.
        input.position(oldPos);
        mac.init(macKey);
        mac.update(input);
        cipher.update(ByteBuffer.wrap(mac.doFinal(), 0, mac.getMacLength()), payloadBuffer);
        payloadBuffer.flip();
        EncryptedPacket packet = new EncryptedPacket(packetCount, payloadBuffer);

        logger.finest(String.format("Packet encrypted, in_bytes=%d, out_bytes=%d, packetnum=%d",
                inputBytes, payloadBuffer.remaining(), packetCount));
        return packet;
    }

    public byte[] getKey() {
        return key.getEncoded();
    }

    public byte[] getIv() {
        return ivSpec.getIV();
    }

    public byte[] getHmac() {
        return macKey.getEncoded();
    }
}