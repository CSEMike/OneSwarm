package edu.washington.cs.oneswarm.f2f.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;

public class OverlayTransportEncrypted extends OverlayTransport {
	private final static Logger logger = Logger.getLogger(OverlayTransportEncrypted.class.getName());

	private static final String ENCRYPTION_ALGORITHM = "AES/CFB/NoPadding";
	public final static int KEY_LENGTH_BITS = 256;
	final Cipher readCipher;
	final Cipher writeCipher;

	public OverlayTransportEncrypted(FriendConnection connection, int channelId, byte[] infohash, int pathID, boolean outgoing, long overlayDelayMs, byte[] key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		super(connection, channelId, infohash, pathID, outgoing, overlayDelayMs);
		if (key.length / 8 != KEY_LENGTH_BITS) {
			throw new InvalidKeyException("invalid key length");
		}

		SecretKeySpec keySpec = new SecretKeySpec(key, 0, key.length, ENCRYPTION_ALGORITHM);
		readCipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
		readCipher.init(Cipher.DECRYPT_MODE, keySpec);
		writeCipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
		writeCipher.init(Cipher.ENCRYPT_MODE, keySpec);
	}

	public long write(ByteBuffer[] buffers, int array_offset, int length) throws IOException {

		if (closed) {
			// when closed just ignore the write requests
			// hopefully the peertransport will read everything in the
			// buffer
			// and get the exception there when done
			return 0;
		}
		int totalToWrite = 0;
		int totalWritten = 0;
		try {
			for (int i = array_offset; i < array_offset + length; i++) {
				totalToWrite += buffers[i].remaining();
			}
			logger.finest(getDescription() + "got write request for: " + totalToWrite);
			// only write one packet at the time
			if (isReadyForWrite(null)) {
				DirectByteBuffer msgBuffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, Math.min(totalToWrite, OSF2FMessage.MAX_PAYLOAD_SIZE));

				ByteBuffer dstBuffer = msgBuffer.getBuffer(DirectByteBuffer.SS_MSG);
				for (int i = 0; i < buffers.length; i++) {
					ByteBuffer currBuffer = buffers[i];

					if (currBuffer.remaining() > dstBuffer.remaining()) {
						// we have more to write than what we can fit
						// set the limit of the source to reflect this
						int oldLimit = currBuffer.limit();
						int newLimit = currBuffer.position() + dstBuffer.remaining();
						currBuffer.limit(newLimit);
						writeCipher.update(currBuffer, dstBuffer);
						// and restore the limit when done
						currBuffer.limit(oldLimit);
						break;
					} else {
						writeCipher.update(currBuffer, dstBuffer);
					}
				}
				msgBuffer.flip(DirectByteBuffer.SS_MSG);
				totalWritten += writeMessage(msgBuffer);
			}
		} catch (ShortBufferException e) {
			logger.warning("not enough room in the destination buffer, this should NEVER happen!");
			e.printStackTrace();
			super.close("short buffer exception");
		}

		return totalWritten;
	}

	public long read(ByteBuffer[] buffers, int array_offset, int length) throws IOException {
		DirectByteBuffer[] tempBufferPool = new DirectByteBuffer[buffers.length];
		ByteBuffer[] tempBuffers = new ByteBuffer[buffers.length];
		for (int i = 0; i < buffers.length; i++) {
			tempBufferPool[i] = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, buffers[i].remaining());
			tempBuffers[i] = tempBufferPool[i].getBuffer(DirectByteBuffer.SS_MSG);
		}
		long len = super.read(tempBuffers, array_offset, length);
		try {
			for (int i = 0; i < tempBuffers.length; i++) {
				readCipher.update(tempBuffers[i], buffers[i]);
				tempBufferPool[i].returnToPool();
			}
		} catch (ShortBufferException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return len;
	}

	public String getDescription() {
		return "ENCR: " + super.getDescription();
	}
}
