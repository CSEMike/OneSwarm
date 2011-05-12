package edu.washington.cs.oneswarm.f2f.puzzle.messaging;

import java.util.Random;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.junit.Assert;
import org.junit.Test;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FPuzzleWrappedMessage;

public class OSF2FPuzzleWrappedMessageTest {

    private static Logger logger = Logger.getLogger(OSF2FPuzzleWrappedMessageTest.class.getName());

    @Test
    public void testSerializeDeserializeMessage() throws Exception {
        logger.info("Start testSerializeDeserializeMessage()");
        
        // Test plan: Serialize a puzzle wrapped message and verify that the
        // deserialized message matches.
        
        try {
            // Construct and serialize a random message.
            byte[] originalBytes = "abc123".getBytes();
            byte[] originalSolution = new byte[20];
            Random r = new Random(5);
            r.nextBytes(originalSolution);
            long originalTimestamp = System.currentTimeMillis();

            DirectByteBuffer wrapped = DirectByteBufferPool.getBuffer((byte) 0,
                    originalBytes.length);
            wrapped.put((byte) 0, originalBytes);
            wrapped.flip((byte) 0);

            OSF2FPuzzleWrappedMessage original = new OSF2FPuzzleWrappedMessage((byte) 1,
                    new DirectByteBuffer[] { wrapped }, originalBytes.length, originalTimestamp,
                    originalSolution);

            int flatSize = 0;
            for (DirectByteBuffer b : original.getData()) {
                flatSize += b.remaining((byte) 0);
            }

            DirectByteBuffer flattened = DirectByteBufferPool.getBuffer((byte) 0, flatSize);
            for (DirectByteBuffer b : original.getData()) {
                flattened.put((byte) 0, b);
            }
            flattened.flip((byte) 0);

            // Deserialize the flattened, serialized message.
            OSF2FPuzzleWrappedMessage deserialized = (OSF2FPuzzleWrappedMessage) new OSF2FPuzzleWrappedMessage(
                    (byte) 0, null, 0, 0, new byte[20]).deserialize(flattened, (byte) 1);

            // Unpack and verify equality
            Assert.assertEquals(originalTimestamp, deserialized.getTimestamp());
            DirectByteBuffer scratch = deserialized.getWrappedMessage()[0];
            byte[] deserializedBytes = new byte[scratch.remaining((byte) 0)];
            scratch.get((byte) 0, deserializedBytes);
            Assert.assertArrayEquals(originalBytes, deserializedBytes);
            Assert.assertArrayEquals(originalSolution, deserialized.getPuzzleSolution());
        } finally {
            logger.info("End testSerializeDeserializeMessage()");
        }
    }

}
