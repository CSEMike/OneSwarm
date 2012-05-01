package edu.washington.cs.oneswarm.f2f.messaging;

import java.io.UnsupportedEncodingException;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FTextSearch extends OSF2FSearch implements OSF2FMessage {

    private final byte type;

    private final String searchString;
    private byte[] searchStringBytes;

    private String description;
    private DirectByteBuffer buffer;
    private final static int BASE_MESSAGE_LENGTH = 5; // 1 byte type, 4 byte id
    private final static int MAX_MESSAGE_LENGTH = 109;

    private static final int MAX_SEARCH_STRING_LENGTH = MAX_MESSAGE_LENGTH - BASE_MESSAGE_LENGTH;

    private final int messageLength;

    public OSF2FTextSearch(byte version, byte type, int searchID, String searchString) {
        super(version, searchID);
        this.searchString = searchString;
        byte[] bytes;
        try {
            bytes = searchString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            bytes = searchString.getBytes();
        }
        if (bytes.length <= MAX_SEARCH_STRING_LENGTH) {
            this.searchStringBytes = bytes;
        } else {
            // Search string too large, crop it.
            this.searchStringBytes = new byte[MAX_SEARCH_STRING_LENGTH];
            System.arraycopy(bytes, 0, this.searchStringBytes, 0, MAX_SEARCH_STRING_LENGTH);
            Debug.out("Search '" + searchString + "' too long, cropping.");
        }
        this.type = type;
        this.messageLength = BASE_MESSAGE_LENGTH + searchStringBytes.length;
    }

    @Override
    public OSF2FTextSearch clone() {
        return new OSF2FTextSearch(this.getVersion(), type, this.getSearchID(),
                this.getSearchString());
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
    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_TEXT_SEARCH + "\tsearchID="
                    + Integer.toHexString(getSearchID()) + "\tstring=" + searchString;
        }

        return description;
    }

    @Override
    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, messageLength);
            buffer.put(DirectByteBuffer.SS_MSG, type);
            buffer.putInt(DirectByteBuffer.SS_MSG, getSearchID());
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
        if (buffer != null)
            buffer.returnToPool();
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
}
