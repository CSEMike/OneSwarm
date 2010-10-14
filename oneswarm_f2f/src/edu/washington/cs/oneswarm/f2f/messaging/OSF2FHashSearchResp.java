package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FHashSearchResp implements OSF2FMessage, OSF2FSearchResp {

	private String description = null;
	private final byte version;
	private final int channelID;
	private final int searchID;
	private int pathID;
	private int originalPathID;
	private DirectByteBuffer buffer = null;
	private final static int MESSAGE_LENGTH = 12;

	public OSF2FHashSearchResp(byte _version, int searchID, int channelID, int pathID) {
		this.version = _version;
		this.channelID = channelID;
		this.searchID = searchID;
		this.pathID = pathID;
		this.originalPathID = pathID;
	}

	public OSF2FHashSearchResp clone() {
		return new OSF2FHashSearchResp(OSF2FMessage.CURRENT_VERSION, this.getSearchID(), this.getChannelID(), this.getPathID());
	}

	public int getChannelID() {
		return channelID;
	}

	public int getSearchID() {
		return searchID;
	}

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException("[" + getID() + "] decode error: data == null");
		}

		if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
			throw new MessageException("[" + getID() + "] decode error: payload.remaining[" + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
		}

		int search = data.getInt(DirectByteBuffer.SS_MSG);
		int channel = data.getInt(DirectByteBuffer.SS_MSG);
		int path = data.getInt(DirectByteBuffer.SS_MSG);
		data.returnToPool();

		return new OSF2FHashSearchResp(version, search, channel, path);
	}

	public String getID() {
		return OSF2FMessage.ID_OS_CHANNEL_SETUP;
	}

	public byte[] getIDBytes() {
		return OSF2FMessage.ID_OS_CHANNEL_SETUP_BYTES;
	}

	public String getFeatureID() {
		return OSF2FMessage.OS_FEATURE_ID;
	}

	public int getFeatureSubID() {
		return OSF2FMessage.SUBID_OS_CHANNEL_SETUP;
	}

	public int getType() {
		return Message.TYPE_PROTOCOL_PAYLOAD;
	}

	public byte getVersion() {
		return version;
	};

	public String getDescription() {
		if (description == null) {
			description = OSF2FMessage.ID_OS_CHANNEL_SETUP + "\tsearch=" + Integer.toHexString(searchID) + "\tchannel=" + Integer.toHexString(channelID) + "\tpathid=" + Integer.toHexString(originalPathID);
		}

		return description;
	}

	public void destroy() {
		if (buffer != null)
			buffer.returnToPool();
	}

	public DirectByteBuffer[] getData() {
		if (buffer == null) {
			buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
			buffer.putInt(DirectByteBuffer.SS_MSG, searchID);
			buffer.putInt(DirectByteBuffer.SS_MSG, channelID);
			buffer.putInt(DirectByteBuffer.SS_MSG, pathID);
			buffer.flip(DirectByteBuffer.SS_MSG);
		}

		return new DirectByteBuffer[] { buffer };
	}

	public int getPathID() {
		return pathID;
	}

	public void updatePathID(int randomness) {
		pathID = pathID ^ randomness;
	}

	public int getMessageSize() {
		return MESSAGE_LENGTH;
	}

}
