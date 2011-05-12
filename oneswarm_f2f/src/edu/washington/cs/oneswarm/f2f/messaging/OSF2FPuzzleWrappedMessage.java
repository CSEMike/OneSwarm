package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.google.inject.internal.Preconditions;

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

    private final int wrappedMessageSize;

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
             * Format: [0...19] -- Solution to the puzzle [20...27] -- Recent
             * timestamp [28...] -- Wrapped message
             */

            // Prepend the solution to the puzzle
            DirectByteBuffer solutionBuffer = DirectByteBufferPool.getBuffer(
                    DirectByteBuffer.AL_MSG, puzzleSolution.length + 8);
            solutionBuffer.put(DirectByteBuffer.SS_MSG, puzzleSolution);
            // For some reason, the Azureus wrapper doesn't export putLong, so
            // we do this directly.
            solutionBuffer.getBuffer(DirectByteBuffer.SS_MSG).putLong(timestamp);
            solutionBuffer.flip(DirectByteBuffer.SS_MSG);

            // Attach the underlying message data
            DirectByteBuffer[] wrapped = wrappedMessage;

            // Compute the message length
            mMessageLength = puzzleSolution.length + 8 + wrappedMessageSize;

            buffer = new DirectByteBuffer[wrapped.length + 1];
            buffer[0] = solutionBuffer;
            for (int i = 0; i < wrapped.length; i++) {
                buffer[i + 1] = wrapped[i];
            }
        }
        // Shallow copy of the buffer array.
        return buffer.clone();
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

        try {
            data.get(DirectByteBuffer.SS_MSG, incomingSolution);
            incomingTimestamp = data.getBuffer(DirectByteBuffer.SS_MSG).getLong();
            incomingWrappedMessageSize = length - (incomingSolution.length + 8);
            incomingWrappedMessage = new byte[incomingWrappedMessageSize];
            data.get(DirectByteBuffer.SS_MSG, incomingWrappedMessage);
            
            // Wrap up byte arrays in DirectByteBuffers.
            DirectByteBuffer buff = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG,
                    incomingWrappedMessageSize);
            buff.put(DirectByteBuffer.SS_MSG, incomingWrappedMessage);
            buff.flip(DirectByteBuffer.SS_MSG);

            return new OSF2FPuzzleWrappedMessage(version, new DirectByteBuffer[] { buff },
                    incomingWrappedMessageSize, incomingTimestamp, incomingSolution);
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
     * Constructs a message wrapping {@code toWrap} with {@code puzzleSolution}
     * attached as the solution to the puzzle. {@code timestamp}, included in
     * the puzzle, must be in the recent past.
     */
    public OSF2FPuzzleWrappedMessage(byte version, OSF2FPuzzleSupportingMessage toWrap,
            long timestamp, byte[] puzzleSolution) {
        this(version, toWrap.getData(), toWrap.getMessageSize(), timestamp, puzzleSolution);
    }

    public OSF2FPuzzleWrappedMessage(byte version, DirectByteBuffer[] wrappedMessage,
            int wrappedMessageSize, long timestamp, byte[] puzzleSolution) {

        // All puzzle solutions are 20 bytes.
        Preconditions.checkArgument(puzzleSolution.length == 20);

        this.timestamp = timestamp;
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

    public DirectByteBuffer[] getWrappedMessage() {
        return wrappedMessage;
    }

    public int getWrappedMessageSize() {
        return wrappedMessageSize;
    }
}
