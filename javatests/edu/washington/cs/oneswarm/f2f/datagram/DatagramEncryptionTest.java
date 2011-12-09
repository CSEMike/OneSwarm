package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;
import org.gudy.azureus2.core3.util.Base32;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

public class DatagramEncryptionTest extends OneSwarmTestBase {

    @Before
    public void setupLogging() {
        logFinest(DatagramEncrypter.logger);
        logFinest(DatagramDecrypter.logger);
    }

    @Test
    public void testSimple() throws Exception {
        byte[] payloadBuffer = new byte[1500];

        String input = "oneswarm_udp_enc";
        System.out.println("input : " + input + "\t" + fromArray(input.getBytes(), true) + " size="
                + input.getBytes().length);
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(input.getBytes()), payloadBuffer);
        System.out.println("encrypted: " + p.sequenceNumber + " " + p.length + " bytes ");
        System.out.println("packet: \t" + fromArray(payloadBuffer, p.getLength(), true));

        ByteBuffer dest = ByteBuffer.allocate(1500);
        if (decr.decrypt(payloadBuffer, 0, p.length, dest)) {
            System.out.println("output : " + fromByteBuffer(dest, false));
        } else {
            Assert.fail("decrypt error");
        }
    }

    @Test
    public void testDropped() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        byte[] payloadBuffer = new byte[1500];

        String input = "oneswarm_udp_encr";
        System.out.println("input : " + input + "\t" + new String(Base32.encode(input.getBytes())));
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        for (int i = 0; i < 4; i++) {
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(input.getBytes()), payloadBuffer);
            System.out.println("encrypted: " + p.sequenceNumber + " " + p.payload.remaining()
                    + " bytes " + fromByteBuffer(p.payload, true) + "");

            // Missed packet
            if (i == 2) {
                continue;
            }
            dest.clear();
            if (decr.decrypt(payloadBuffer, 0, p.length, dest)) {
                System.out.println("output : " + fromByteBuffer(dest, false));
            } else {
                Assert.fail("decrypt error");
            }
        }
    }

    @Test
    public void testDamagedPayload() throws Exception {
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        byte[] packet1Buffer = new byte[1500];
        EncryptedPacket packet1 = encr
                .encrypt(ByteBuffer.wrap("packet1".getBytes()), packet1Buffer);

        packet1Buffer[10] = 123;

        Assert.assertFalse(decr.decrypt(packet1Buffer, 0, packet1.getLength(),
                ByteBuffer.allocate(1500)));
        System.out.println("Damaged payload test done.");
    }

    @Test
    public void testSizes() throws Exception {

        ByteBuffer decrypted = ByteBuffer.allocate(2000);
        byte[] encrypted = new byte[2000];
        byte[] payloadArray = new byte[2000];
        ByteBuffer payload = ByteBuffer.wrap(payloadArray);

        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        System.out.println("===doing size test===");
        Random random = new Random(12345);
        for (int i = 0; i < 1500; i++) {
            decrypted.clear();
            payload.clear();
            payload.limit(i);
            random.nextBytes(payloadArray);
            EncryptedPacket p = encr.encrypt(payload, encrypted);
            boolean decryptOk = decr.decrypt(encrypted, 0, p.getLength(), decrypted);
            if (!decryptOk) {
                System.err.println("error with size: " + i);
            }
            Assert.assertTrue(decryptOk);
            for (int j = 0; j < i; j++) {
                Assert.assertEquals(payloadArray[j], decrypted.get(j));
            }

        }
        System.out.println("done\n");
    }

    @Test
    public void testRandomSizes() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        byte[] payloadBuffer = new byte[1500];

        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        System.out.println("===doing rand test===");
        Random random = new Random(12345);
        for (int i = 0; i < 1000; i++) {
            byte[] payload = new byte[random.nextInt(1024) + 1];
            random.nextBytes(payload);
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(payload), payloadBuffer);
            dest.clear();
            Assert.assertTrue(decr.decrypt(payloadBuffer, 0, p.length, dest));
        }
        System.out.println("done\n");
    }

    @Test
    public void testOutOfOrderDrop() throws Exception {
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        // // Encrypt packet 1
        byte[] packet1Buffer = new byte[1500];
        EncryptedPacket packet1 = encr
                .encrypt(ByteBuffer.wrap("packet1".getBytes()), packet1Buffer);
        System.out.println("1:" + fromArray(packet1Buffer, packet1.length + 1, true));

        // Packet 2
        byte[] packet2Buffer = new byte[1500];
        EncryptedPacket packet2 = encr
                .encrypt(ByteBuffer.wrap("packet2".getBytes()), packet2Buffer);
        System.out.println("2:" + fromArray(packet2Buffer, packet2.length + 1, true));

        // Decrypt second packet.
        Assert.assertTrue(decr.decrypt(packet2Buffer, 0, packet2.length, ByteBuffer.allocate(1500)));

        // "delayed" packet should be dropped.
        Assert.assertFalse(decr.decrypt(packet1Buffer, 0, packet1.length, ByteBuffer.allocate(1500)));
        System.out.println("Out of order test done.");
    }

    /*
     * Results:
     * INIT ON EACH (call cipher.init on each packet)
     * ===doing perf test===
     * done: time=8.05s speed=12.42mb/s ,12.71kpps
     * 
     * NO INIT (use padding to ensure that each packet ends at a complete aes
     * round)
     * ===doing perf test===
     * done: time=6.41s speed=15.61mb/s ,15.98kpps
     * 
     * SHA1 instead of HMAC-SHA1
     * ===doing perf test===
     * done: time=5.22s speed=19.15mb/s ,19.61kpps
     */
    @Test
    public void testPerformance() throws Exception {
        logInfo(DatagramEncrypter.logger);
        logInfo(DatagramDecrypter.logger);

        ByteBuffer dest = ByteBuffer.allocate(1500);
        byte[] payloadBuffer = new byte[1500];

        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        System.out.println("===doing perf test===");
        long time = System.currentTimeMillis();
        int SIZE = 1024;
        int NUM = 102400;
        byte[] payload = new byte[SIZE];

        for (int i = 0; i < NUM; i++) {
            new Random().nextBytes(payload);
            payload[0] = (byte) i;
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(payload), payloadBuffer);
            if (i % 17000 == 0) {
                // continue;
            }
            dest.clear();
            Assert.assertTrue(decr.decrypt(payloadBuffer, 0, p.length, dest));
        }
        double mb = SIZE * NUM / (1024 * 1024.0);
        double elapsed = (System.currentTimeMillis() - time) / 1000.0;
        System.out.println(String.format("done: time=%.2fs speed=%.2fmb/s ,%.2fkpps", elapsed, mb
                / elapsed, NUM / elapsed / 1000));

        Assert.assertTrue(mb / elapsed > 10);
    }

    @Test
    public void testIv() throws Exception {
        IvTest ivTest = new IvTest();
        {
            // Test 16 byte block size only operations.
            byte[] data = new byte[16];
            String ciphertext;

            String[] autoCrt = new String[4];
            ivTest.setCounter(0);
            ciphertext = ivTest.encrypt(data);
            autoCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + autoCrt[ivTest.getCurrentCounter()]);
            ciphertext = ivTest.encrypt(data);
            autoCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + autoCrt[ivTest.getCurrentCounter()]);
            ciphertext = ivTest.encrypt(data);
            autoCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + autoCrt[ivTest.getCurrentCounter()]);

            String[] manCrt = new String[4];
            ivTest.setCounter(1);
            ciphertext = ivTest.encrypt(data);
            manCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + manCrt[ivTest.getCurrentCounter()]);
            ciphertext = ivTest.encrypt(data);
            manCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + manCrt[ivTest.getCurrentCounter()]);
            ivTest.setCounter(0);
            ciphertext = ivTest.encrypt(data);
            manCrt[ivTest.getCurrentCounter()] = ciphertext;
            System.out.println(ivTest.getCurrentCounter() + " " + "\t"
                    + manCrt[ivTest.getCurrentCounter()]);

            Assert.assertArrayEquals(manCrt, autoCrt);
        }
        {
            ivTest.setCounter(0);
            // Test non 16 byte blocks
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                b.append(ivTest.encrypt(new byte[1]));
            }
            System.out.println(ivTest.getCurrentCounter() + " " + "\t" + b.toString());
            String cipherText = ivTest.encrypt(new byte[16]);
            System.out.println(ivTest.getCurrentCounter() + "\t" + cipherText);
        }
    }

    class IvTest extends DatagramEncrytionBase {
        int bytecount = 0;

        public IvTest() throws Exception {
            SecureRandom random = new SecureRandom();
            ivSpec = createCtrIvForAES(0, random);

            // Create the encryption key and the cipher.
            key = createKeyForAES(AES_KEY_LENGTH, random);
            cipher = Cipher.getInstance(ENCR_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            // Create the mac key and mac
            byte[] hmac_key = new byte[HMAC_KEY_LENGTH];
            random.nextBytes(hmac_key);
            mac = Mac.getInstance(HMAC_ALGO);
            macKey = new SecretKeySpec(hmac_key, HMAC_ALGO);
        }

        public String encrypt(byte[] data) throws IllegalBlockSizeException, BadPaddingException {
            bytecount += data.length;
            return new String(Hex.encode(cipher.update(data)));
        }

        public int getCurrentCounter() {
            return (int) (initCounter + bytecount / 16);
        }

        long initCounter;

        private void setCounter(int num) throws InvalidKeyException,
                InvalidAlgorithmParameterException {
            bytecount = 0;
            initCounter = num;
            ivSpec = setSequenceNumber(num, ivSpec.getIV());
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

        }
    }

    static String fromByteBuffer(ByteBuffer buf, boolean base16) {
        int oldPos = buf.position();
        byte[] b = new byte[buf.remaining()];
        buf.get(b);
        buf.position(oldPos);
        return fromArray(b, base16);
    }

    static String fromArray(byte[] b, boolean base16) {
        return fromArray(b, b.length, base16);
    }

    static String fromArray(byte[] b, int length, boolean base16) {
        if (base16) {
            return new String(Hex.encode(b, 0, length));
        } else {
            return new String(b, 0, length);
        }
    }

    static String wrap(String data, int before, int after) {
        return wrap(data, "_", before, after);
    }

    static String wrap(String data, String pad, int before, int after) {
        return expand(pad, before) + data + expand(pad, after);
    }

    static String expand(String s, int num) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < num; i++) {
            b.append(s);
        }
        return b.toString();
    }

}
