package edu.washington.cs.oneswarm.f2f.datagram;

import java.nio.ByteBuffer;
import java.util.Random;

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
    public void testPerformance() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(1500);

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
            payloadBuffer.clear();
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(payload), payloadBuffer);
            if (i % 17000 == 0) {
                continue;
            }
            Assert.assertTrue(decr.decrypt(p, dest));
        }
        double mb = SIZE * NUM / (1024 * 1024.0);
        double elapsed = (System.currentTimeMillis() - time) / 1000.0;
        System.out.println(String.format("done: time=%.2fs speed=%.2fmb/s ,%.2fkpps", elapsed, mb
                / elapsed, NUM / elapsed / 1000));

        Assert.assertTrue(mb / elapsed > 10);
    }

    @Test
    public void testRandomSizes() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(1500);

        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        System.out.println("===doing rand test===");
        Random random = new Random(12345);
        for (int i = 0; i < 1000; i++) {
            byte[] payload = new byte[random.nextInt(1024) + 1];
            random.nextBytes(payload);
            payloadBuffer.clear();
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(payload), payloadBuffer);
            Assert.assertTrue(decr.decrypt(p, dest));
        }
        System.out.println("done\n");
    }

    @Test
    public void testSimple() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(1500);

        String input = "oneswarm_udp_encr";
        System.out.println("input : " + input + "\t" + new String(Base32.encode(input.getBytes())));
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(input.getBytes()), payloadBuffer);
        System.out.println("encrypted: " + p.sequenceNumber + " " + p.payload.remaining()
                + " bytes " + fromByteBuffer(p.payload, true) + "");

        if (decr.decrypt(p, dest)) {
            System.out.println("output : " + fromByteBuffer(dest, false));
        } else {
            Assert.fail("decrypt error");
        }
    }

    @Test
    public void testDropped() throws Exception {
        ByteBuffer dest = ByteBuffer.allocate(1500);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(1500);

        String input = "oneswarm_udp_encr";
        System.out.println("input : " + input + "\t" + new String(Base32.encode(input.getBytes())));
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        for (int i = 0; i < 4; i++) {
            payloadBuffer.clear();
            EncryptedPacket p = encr.encrypt(ByteBuffer.wrap(input.getBytes()), payloadBuffer);
            System.out.println("encrypted: " + p.sequenceNumber + " " + p.payload.remaining()
                    + " bytes " + fromByteBuffer(p.payload, true) + "");

            // Missed packet
            if (i == 3) {
                continue;
            }
            if (decr.decrypt(p, dest)) {
                System.out.println("output : " + fromByteBuffer(dest, false));
            } else {
                Assert.fail("decrypt error");
            }
        }
    }

    @Test
    public void testOutOfOrderDrop() throws Exception {
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        // Encrypt packet 1
        ByteBuffer packet1Buffer = ByteBuffer.allocate(1500);
        EncryptedPacket packet1 = encr
                .encrypt(ByteBuffer.wrap("packet1".getBytes()), packet1Buffer);

        // Packet 2
        ByteBuffer packet2Buffer = ByteBuffer.allocate(1500);
        EncryptedPacket packet2 = encr
                .encrypt(ByteBuffer.wrap("packet2".getBytes()), packet2Buffer);

        // Decrypt second packet.
        Assert.assertTrue(decr.decrypt(packet2, ByteBuffer.allocate(1500)));

        // "delayed" packet should be dropped.
        Assert.assertFalse(decr.decrypt(packet1, ByteBuffer.allocate(1500)));
        System.out.println("Out of order test done.");
    }

    @Test
    public void testDamagedPayload() throws Exception {
        DatagramEncrypter encr = new DatagramEncrypter();
        DatagramDecrypter decr = new DatagramDecrypter(encr.key.getEncoded(), encr.ivSpec.getIV(),
                encr.macKey.getEncoded());

        ByteBuffer packet1Buffer = ByteBuffer.allocate(1500);
        EncryptedPacket packet1 = encr
                .encrypt(ByteBuffer.wrap("packet1".getBytes()), packet1Buffer);

        packet1Buffer.position(10);
        packet1Buffer.put((byte) 123);
        packet1Buffer.position(0);

        Assert.assertFalse(decr.decrypt(packet1, ByteBuffer.allocate(1500)));
        System.out.println("Damaged payload test done.");
    }

    static String fromByteBuffer(ByteBuffer buf, boolean base32) {
        int oldPos = buf.position();
        byte[] b = new byte[buf.remaining()];
        buf.get(b);
        buf.position(oldPos);
        if (base32) {
            return new String(Base32.encode(b));
        } else {
            return new String(b);
        }
    }

}
