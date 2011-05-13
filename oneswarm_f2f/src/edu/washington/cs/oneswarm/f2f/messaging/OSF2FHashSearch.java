package edu.washington.cs.oneswarm.f2f.messaging;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FHashSearch implements OSF2FSearch, OSF2FPuzzleSupportingMessage {

    private final byte version;
    private final int searchID;
    private final long infohashhash;

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 12;

    /** State associated with the puzzle solution for this message, if any. */
    OSF2FPuzzleWrappedMessage puzzleWrapper = null;

    /** A buffer containing the puzzle material. */
    byte[] puzzleMaterialBuffer = null;

    public OSF2FHashSearch(byte version, int searchID, long infohashhash) {
        this.infohashhash = infohashhash;
        this.version = version;
        this.searchID = searchID;
    }

    @Override
    public OSF2FHashSearch clone() {
        return new OSF2FHashSearch(this.getVersion(), this.getSearchID(), this.getInfohashhash());
    }

    @Override
    public int getSearchID() {
        return searchID;
    }

    public long getInfohashhash() {
        return infohashhash;
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_HASH_SEARCH;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_HASH_SEARCH_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_HASH_SEARCH;
    }

    @Override
    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    @Override
    public byte getVersion() {
        return version;
    };

    @Override
    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_HASH_SEARCH + "\tsearch="
                    + Integer.toHexString(searchID) + "\thash=" + Long.toHexString(infohashhash);
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, searchID);
            buffer.getBuffer(DirectByteBuffer.SS_MSG).putLong(infohashhash);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
        }
        int search = data.getInt(DirectByteBuffer.SS_MSG);
        long hash = data.getBuffer(DirectByteBuffer.SS_MSG).getLong();
        data.returnToPool();
        return new OSF2FHashSearch(version, search, hash);
    }

    @Override
    public void destroy() {
        if (buffer != null) {
            buffer.returnToPool();
        }
    }

    @Override
    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

    @Override
    public int getValueID() {
        return (int) infohashhash;
    }

    @Override
    public byte[] getPuzzleMaterial() {
        if (puzzleMaterialBuffer == null) {
            try {
                // We include the search ID and infohash hash.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                baos.write(searchID);
                dos.writeLong(infohashhash);
                puzzleMaterialBuffer = baos.toByteArray();
            } catch (IOException e) {
                Debug.out("error during puzzle material retrieval", e);
            }
        }
        return puzzleMaterialBuffer;
    }

    /** Set during unwrapping of a puzzle supporting message. */
    public void setPuzzleWrapper(OSF2FPuzzleWrappedMessage wrapper) {
        this.puzzleWrapper = wrapper;
    }

    @Override
    public OSF2FPuzzleWrappedMessage getPuzzleWrapper() {
        return puzzleWrapper;
    }
}
