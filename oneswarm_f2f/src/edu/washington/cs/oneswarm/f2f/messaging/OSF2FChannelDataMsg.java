package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FChannelDataMsg extends OSF2FChannelMsg {

	private static final int BASE_LENGHT = 4;
	private String description = null;
	private byte version;
	private int channelID;
	private DirectByteBuffer[] buffer = new DirectByteBuffer[2];
	private final int messageLength;

	public OSF2FChannelDataMsg(byte _version, int channelID, DirectByteBuffer data) {
		super(channelID);
		this.version = _version;
		this.channelID = channelID;
		this.buffer[1] = data;
		if (data != null) {
			messageLength = BASE_LENGHT + data.remaining(DirectByteBuffer.SS_MSG);
		} else {
			messageLength = BASE_LENGHT;
		}
	}

	public DirectByteBuffer getPayload() {
		return buffer[1];
	}

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException("[" + getID() + "] decode error: data == null");
		}

		if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGHT) {
			throw new MessageException("[" + getID() + "] decode error: payload.remaining[" + data.remaining(DirectByteBuffer.SS_MSG) + "] < " + BASE_LENGHT);
		}

		int channelID = data.getInt(DirectByteBuffer.SS_MSG);

		return new OSF2FChannelDataMsg(version, channelID, data);
	}

	public String getID() {
		return OSF2FMessage.ID_OS_CHANNEL_DATA_MSG;
	}

	public byte[] getIDBytes() {
		return OSF2FMessage.ID_OS_CHANNEL_DATA_MSG_BYTES;
	}

	public String getFeatureID() {
		return OSF2FMessage.OS_FEATURE_ID;
	}

	public int getFeatureSubID() {
		return OSF2FMessage.SUBID_OS_CHANNEL_DATA_MSG;
	}

	public int getType() {
		return Message.TYPE_DATA_PAYLOAD;
	}

	public byte getVersion() {
		return version;
	};

	public String getDescription() {
		if (description == null) {
			description = OSF2FMessage.ID_OS_CHANNEL_DATA_MSG + "\tchannel=" + channelID + "\tbytes=";
			if (buffer[1] != null)
				description += buffer[1].remaining(DirectByteBuffer.SS_MSG);
		}

		return description;
	}

	public void destroy() {
		if (buffer[0] != null)
			buffer[0].returnToPool();
		if (buffer[1] != null)
			buffer[1].returnToPool();
	}

	public DirectByteBuffer[] getData() {
		if (buffer[0] == null) {
			buffer[0] = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, BASE_LENGHT);
			buffer[0].putInt(DirectByteBuffer.SS_MSG, channelID);
			buffer[0].flip(DirectByteBuffer.SS_MSG);
		}

		return buffer;

	}

	// used to remove all messages for a given channel from the queue
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj)
			return true;
		if (obj instanceof OSF2FChannelDataMsg) {
			OSF2FChannelDataMsg other = (OSF2FChannelDataMsg) obj;
			if (other.getChannelId() == this.getChannelId()) {
				return true;
			}
		}
		return false;
	}

	public int getMessageSize() {
		return messageLength;
	}

}
