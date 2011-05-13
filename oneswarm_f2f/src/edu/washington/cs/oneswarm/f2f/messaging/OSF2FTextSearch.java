package edu.washington.cs.oneswarm.f2f.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FTextSearch implements OSF2FSearch, OSF2FPuzzleSupportingMessage {

    private final byte version;
    private final int searchID;
    private final byte type;

    private final String searchString;
    private byte[] searchStringBytes;

    private String description;
    private DirectByteBuffer buffer;
    private final static int BASE_MESSAGE_LENGTH = 5;
    private final static int MAX_MESSAGE_LENGTH = 109;

    private final int messageLength;

    /** A buffer containing the puzzle material. */
    byte[] puzzleMaterialBuffer = null;

    /** The puzzle message which wrapped this search, if any. */
    private OSF2FPuzzleWrappedMessage puzzleWrapper;

    public OSF2FTextSearch(byte version, byte type, int searchID, String searchString) {
        this.searchString = searchString;
        try {
            this.searchStringBytes = searchString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            this.searchStringBytes = searchString.getBytes();
        }
        this.version = version;
        this.type = type;
        this.searchID = searchID;
        this.messageLength = Math.min(MAX_MESSAGE_LENGTH, BASE_MESSAGE_LENGTH
                + searchStringBytes.length);
    }

    @Override
    public OSF2FTextSearch clone() {
        return new OSF2FTextSearch(this.getVersion(), type, this.getSearchID(),
                this.getSearchString());
    }

    @Override
    public int getSearchID() {
        return searchID;
    }

    public String getSearchString() {
        return searchString;
    }

    @Override
    public String getID() {
        return OSF2FMessage.ID_OS_TEXT_SEARCH;
    }

    @Override
    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_TEXT_SEARCH_BYTES;
    }

    @Override
    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    @Override
    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_TEXT_SEARCH;
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
            description = OSF2FMessage.ID_OS_TEXT_SEARCH + "\tsearchID="
                    + Integer.toHexString(searchID) + "\tstring=" + searchString;
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, messageLength);
            buffer.put(DirectByteBuffer.SS_MSG, type);
            buffer.putInt(DirectByteBuffer.SS_MSG, searchID);
            buffer.put(DirectByteBuffer.SS_MSG, searchStringBytes);

            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        if (data == null) {
            throw new MessageException("[" + getID() + "] decode error: data == null");
        }

        if (data.remaining(DirectByteBuffer.SS_MSG) <= BASE_MESSAGE_LENGTH) {
            throw new MessageException("[" + getID() + "] decode error: payload.remaining["
                    + data.remaining(DirectByteBuffer.SS_MSG) + "] <= " + BASE_MESSAGE_LENGTH);
        }
        byte type = data.get(DirectByteBuffer.SS_MSG);
        int search = data.getInt(DirectByteBuffer.SS_MSG);
        int stringLength = data.remaining(DirectByteBuffer.SS_MSG);
        byte[] stringBytes = new byte[stringLength];
        data.get(DirectByteBuffer.SS_MSG, stringBytes);

        data.returnToPool();
        try {
            return new OSF2FTextSearch(version, type, search, new String(stringBytes, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Debug.out("unable to decode packet using utf-8, fallback to std encoding", e);
            return new OSF2FTextSearch(version, type, search, new String(stringBytes));
        }
    }

    @Override
    public void destroy() {
        if (buffer != null) {
            buffer.returnToPool();
        }
    }

    public byte getRequestType() {
        return type;
    }

    @Override
    public int getMessageSize() {
        return messageLength;
    }

    @Override
    public int getValueID() {
        return searchString.hashCode();
    }

    @Override
    public byte[] getPuzzleMaterial() {
        if (puzzleMaterialBuffer == null) {
            try {
                // We include the search ID and text string.
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(searchID);
                baos.write(searchStringBytes);
                puzzleMaterialBuffer = baos.toByteArray();
            } catch (IOException e) {
                Debug.out("error during puzzle material retrieval", e);
            }
        }
        return puzzleMaterialBuffer;
    }

    @Override
    public void setPuzzleWrapper(OSF2FPuzzleWrappedMessage wrapper) {
        this.puzzleWrapper = wrapper;
    }

    @Override
    public OSF2FPuzzleWrappedMessage getPuzzleWrapper() {
        return puzzleWrapper;
    }
}
