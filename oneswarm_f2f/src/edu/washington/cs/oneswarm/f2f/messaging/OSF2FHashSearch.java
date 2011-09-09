package edu.washington.cs.oneswarm.f2f.messaging;

import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.network.SearchManager.HashSearchListener;

public class OSF2FHashSearch extends OSF2FSearch implements OSF2FMessage {

    private final long infohashhash;

    private String description;
    private DirectByteBuffer buffer;
    private final static int MESSAGE_LENGTH = 12;

    // Listener to call if we receive a response
    private final List<HashSearchListener> listeners = new LinkedList<HashSearchListener>();

    public OSF2FHashSearch(byte version, int searchID, long infohashhash) {
        super(version, searchID);
        this.infohashhash = infohashhash;
    }

    public void addListener(HashSearchListener listener) {
        listeners.add(listener);
    }

    public List<HashSearchListener> getListeners() {
        return listeners;
    }

    public OSF2FHashSearch clone() {
        return new OSF2FHashSearch(this.getVersion(), this.getSearchID(), this.getInfohashhash());
    }

    public long getInfohashhash() {
        return infohashhash;
    }

    public String getID() {
        return OSF2FMessage.ID_OS_HASH_SEARCH;
    }

    public byte[] getIDBytes() {
        return OSF2FMessage.ID_OS_HASH_SEARCH_BYTES;
    }

    public String getFeatureID() {
        return OSF2FMessage.OS_FEATURE_ID;
    }

    public int getFeatureSubID() {
        return OSF2FMessage.SUBID_OS_HASH_SEARCH;
    }

    public int getType() {
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    public String getDescription() {
        if (description == null) {
            description = OSF2FMessage.ID_OS_HASH_SEARCH + "\tsearch="
                    + Integer.toHexString(getSearchID()) + "\thash="
                    + Long.toHexString(infohashhash);
        }

        return description;
    }

    public DirectByteBuffer[] getData() {
        if (buffer == null) {
            buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
            buffer.putInt(DirectByteBuffer.SS_MSG, getSearchID());
            buffer.getBuffer(DirectByteBuffer.SS_MSG).putLong(infohashhash);
            buffer.flip(DirectByteBuffer.SS_MSG);
        }
        return new DirectByteBuffer[] { buffer };
    }

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

    public void destroy() {
        if (buffer != null)
            buffer.returnToPool();
    }

    public int getMessageSize() {
        return MESSAGE_LENGTH;
    }

    public int getValueID() {
        return (int) infohashhash;
    }
}
