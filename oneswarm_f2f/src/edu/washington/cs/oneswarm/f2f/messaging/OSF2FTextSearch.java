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
    private final static int BASE_MESSAGE_LENGTH = 5;
    private final static int MAX_MESSAGE_LENGTH = 109;

    private int messageLength;

    public OSF2FTextSearch(byte version, byte type, int searchID, String searchString) {
        super(version, searchID);
        this.searchString = searchString;
        try {
            this.searchStringBytes = searchString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            this.searchStringBytes = searchString.getBytes();
        }
        this.type = type;
        this.messageLength = Math.min(MAX_MESSAGE_LENGTH, BASE_MESSAGE_LENGTH
                + searchStringBytes.length);
    }

    public OSF2FTextSearch clone() {
        return new OSF2FTextSearch(this.getVersion(), type, this.getSearchID(),
                this.getSearchString());
    }

    public String getSearchString() {
        return searchString;
    }

    public String getID() {
        return OSF2FMessage.ID_OS_TEXT_SEARCH;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_TEXT_SEARCH_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_TEXT_SEARCH;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_TEXT_SEARCH + "\tsearchID="
                    + Integer.toHexString(getSearchID()) + "\tstring=" + searchString;
        }

        return description;
    }

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

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public byte getRequestType() {
        return type;
    }

    public int getMessageSize() {
        return messageLength;
    }

    public int getValueID() {
        return searchString.hashCode();
    }
}
