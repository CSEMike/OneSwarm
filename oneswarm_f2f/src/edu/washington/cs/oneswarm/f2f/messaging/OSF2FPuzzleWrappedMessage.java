package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.SHA1Hasher;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.google.inject.internal.Preconditions;

import edu.washington.cs.oneswarm.f2f.puzzle.PuzzleManager;

/**
 * A message that wraps another message with a solution to a computational
 * puzzle.
 * 
 * (The puzzle is to generate a SHA-1 hash collision in N bits with a given
 * immutable part of the contained message, obtained via
 * {@code getPuzzleMaterial()}.)
 */
public class OSF2FPuzzleWrappedMessage implements OSF2FMessage {

    private final byte version;

    /**
     * The solution to the puzzle of cat(timestamp,
     * wrappedMessage.getPuzzleMaterial().
     */
    private final byte[] puzzleSolution;

    /**
     * A recent timestamp associated with the puzzle solution. Should be in the
     * recent past.
     */
    private final long timestamp;

    /** The underlying message to be wrapped in a puzzle. */
    private final DirectByteBuffer[] wrappedMessage;

    /** The serialized message. */
    private DirectByteBuffer[] buffer = null;

    /** Length of the message, computed in {@code getData()}. */
    private int mMessageLength;

    /** The size of the wrapped message, ignoring the header byte. */
    private final int wrappedMessageSize;

    /** The type of the wrapped message, taken from {@code OSF2FMessage}. */
    private final byte messageSubId;

    /**
     * The number of bits in which SHA-1(puzzleSolution) matches
     * SHA-1(getPuzzleMaterial()).
     */
    private short solutionMatchingBits = -1;

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_PUZZLE_WRAPPER;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_PUZZLE_WRAPPER_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_PUZZLE_WRAPPER;
    }

    @Override
    public byte getVersion() {
        return version;
    }

    @Override
    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    @Override
    public String getDescription() {
        return OSF2FMessage.ID_OS_PUZZLE_WRAPPER;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {

            /*
             * Format: [0...19] -- Solution to the puzzle
             * [20...27] -- Recent timestamp
             * [28] -- Wrapped message sub id from OSF2FMessage
             * [29...] -- Wrapped message
             */

            // Prepend the solution to the puzzle
            DirectByteBuffer headerBuffer = DirectByteBufferPool.getBuffer(
                    DirectByteBuffer.AL_MSG, puzzleSolution.length + 8 + 1);
            headerBuffer.put(DirectByteBuffer.SS_MSG, puzzleSolution);
            // For some reason, the Azureus wrapper doesn't export putLong, so
            // we do this directly.
            headerBuffer.getBuffer(DirectByteBuffer.SS_MSG).putLong(timestamp);

            // This must preceed the wrapped message bytes since they will be
            // deserialized together using
            // OSF2FMessageFactory.createOSF2FMessage, which expects an initial
            // message type identifier.
            headerBuffer.put(DirectByteBuffer.SS_MSG, messageSubId);
            headerBuffer.flip(DirectByteBuffer.SS_MSG);

            // Attach the underlying message data
            DirectByteBuffer[] wrapped = wrappedMessage;

            // Compute the message length
            mMessageLength = puzzleSolution.length + 8 + 1 + wrappedMessageSize;

            buffer = new DirectByteBuffer[wrapped.length + 1];
            buffer[0] = headerBuffer;
            for (int i = 0; i < wrapped.length; i++) {
                buffer[i + 1] = wrapped[i];
            }
        }
        // Shallow copy of the buffer array.
        return buffer.clone();
    }

    /** Decodes the raw bytes wrapped in this message as an OSF2FMessage type. */
    public OSF2FMessage getWrappedMessage() throws MessageException {

        DirectByteBuffer flattened = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                wrappedMessageSize + 1);
        flattened.put(DirectByteBuffer.SS_MSG, messageSubId);
        for (DirectByteBuffer b : buffer) {
            flattened.put(DirectByteBuffer.SS_MSG, b);
            b.flip(DirectByteBuffer.SS_MSG);
        }
        flattened.flip(DirectByteBuffer.SS_MSG);

        return (OSF2FMessage) OSF2FMessageFactory.createOSF2FMessage(flattened);
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        int length = data.remaining(DirectByteBuffer.SS_MSG);

        byte[] incomingSolution = new byte[20];
        long incomingTimestamp;
        byte[] incomingWrappedMessage;
        int incomingWrappedMessageSize;
        byte incomingWrappedMessageType;

        try {
            data.get(DirectByteBuffer.SS_MSG, incomingSolution);
            incomingTimestamp = data.getBuffer(DirectByteBuffer.SS_MSG).getLong();
            incomingWrappedMessageSize = length - (incomingSolution.length + 8 + 1);
            incomingWrappedMessage = new byte[incomingWrappedMessageSize];
            incomingWrappedMessageType = data.get(DirectByteBuffer.SS_MSG);

            data.get(DirectByteBuffer.SS_MSG, incomingWrappedMessage);
            
            // Wrap up byte arrays in DirectByteBuffers.
            DirectByteBuffer buff = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                    incomingWrappedMessageSize);
            buff.put(DirectByteBuffer.SS_MSG, incomingWrappedMessage);
            buff.flip(DirectByteBuffer.SS_MSG);

            return new OSF2FPuzzleWrappedMessage(version, incomingWrappedMessageType,
                    new DirectByteBuffer[] { buff }, incomingWrappedMessageSize, incomingTimestamp,
                    incomingSolution);
        } finally {
            data.returnToPool();
        }
    }

    @Override
    public void destroy() {
        if (buffer != null) {
            for (DirectByteBuffer b : buffer) {
                b.returnToPool();
            }
            buffer = null;
        }
    }

    @Override
    public int getMessageSize() {
        if (buffer == null) {
            getData();
        }
        return mMessageLength;
    }

    /**
     * Computes a hash of the puzzle bytes and compares it to the stored
     * solution. The number of matching bytes is memoized.
     * 
     * As an optimization, this function takes a typed version of the message it
     * encapsulated, {@code convertedType}. Because this method is called only
     * when the encapsulated message has been unpacked, we avoid unpacking it
     * for a second time here.
     */
    public short computePuzzleMatchingBits(OSF2FPuzzleSupportingMessage convertedType) {
        if (solutionMatchingBits == -1) {
            SHA1Hasher hasher = new SHA1Hasher();
            byte[] targetHash = hasher.calculateHash(convertedType.getPuzzleMaterial());
            byte[] solutionHash = hasher.calculateHash(puzzleSolution);
            solutionMatchingBits = PuzzleManager.computeMatchingBitsLeftToRight(solutionHash,
                    targetHash);
        }
        return solutionMatchingBits;
    }

    /** Returns the computed number of matching bits. */
    public short getPuzzleMatchingBitCount() {
        // Check that we did in fact compute this before making this call
        Preconditions.checkState(solutionMatchingBits >= 0,
                "Attempted to retrieve matching bits before computing the count.");
        return solutionMatchingBits;
    }

    /**
     * Constructs a message wrapping {@code toWrap} with {@code puzzleSolution}
     * attached as the solution to the puzzle. {@code timestamp}, included in
     * the puzzle, must be in the recent past.
     */
    public OSF2FPuzzleWrappedMessage(byte version, OSF2FPuzzleSupportingMessage toWrap,
            long timestamp, byte[] puzzleSolution) {
        this(version, (byte) toWrap.getFeatureSubID(), toWrap.getData(), toWrap.getMessageSize(),
                timestamp, puzzleSolution);
    }

    /**
     * Creates a puzzle wrapped message from a raw byte buffer.
     */
    public OSF2FPuzzleWrappedMessage(byte version, byte messageSubId,
            DirectByteBuffer[] wrappedMessage,
            int wrappedMessageSize, long timestamp, byte[] puzzleSolution) {

        // All puzzle solutions are 20 bytes.
        Preconditions.checkArgument(puzzleSolution.length == 20);

        this.timestamp = timestamp;
        this.messageSubId = messageSubId;
        this.puzzleSolution = puzzleSolution;
        this.wrappedMessage = wrappedMessage;
        this.wrappedMessageSize = wrappedMessageSize;
        this.version = version;
    }

    public byte[] getPuzzleSolution() {
        return puzzleSolution;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public DirectByteBuffer[] getWrappedMessageBuffer() {
        return wrappedMessage;
    }

    public int getWrappedMessageSize() {
        return wrappedMessageSize;
    }

    public byte getWrappedMessageFeatureId() {
        return messageSubId;
    }
}
