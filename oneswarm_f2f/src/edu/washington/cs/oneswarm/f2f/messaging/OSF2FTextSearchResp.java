package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FTextSearchResp extends OSF2FMessageBase implements OSF2FMessage, OSF2FSearchResp {

	private static final int BASE_LENGTH = 1 + 4 + 4;
	private String description = null;
	private byte version;
	private final byte[] filelist;
	private final byte type;
	private final int searchID;
	private final int channelID;
	private final DirectByteBuffer[] buffer;
	private final int messageLength;

	public OSF2FTextSearchResp(byte _version, byte type, int searchID, int channelID, byte[] filelist) {
		this.version = _version;
		this.filelist = filelist;
		this.type = type;
		this.searchID = searchID;
		this.channelID = channelID;
		if (filelist.length == 0) {
			messageLength = BASE_LENGTH + filelist.length;
			buffer = new DirectByteBuffer[1];
		} else {
			messageLength = BASE_LENGTH;
			buffer = new DirectByteBuffer[2];
		}
	}

	public OSF2FTextSearchResp clone() {
		return new OSF2FTextSearchResp(version, type, searchID, channelID, filelist);
	}

	public byte getFileListType() {
		return type;
	}

	public byte[] getFileList() {
		return filelist;
	}

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException("[" + getID() + "] decode error: data == null");
		}

		if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGTH) {
			throw new MessageException("[" + getID() + "] decode error: payload.remaining[" + data.remaining(DirectByteBuffer.SS_MSG) + "] to small ");
		}
		byte t = data.get(DirectByteBuffer.SS_MSG);
		if (t != FILE_LIST_TYPE_BLOOM && t != FILE_LIST_TYPE_COMPLETE && t != FILE_LIST_TYPE_PARTIAL) {
			throw new MessageException("[" + getID() + "] decode error: unknown type[" + type + "] != " + FILE_LIST_TYPE_BLOOM + "||" + FILE_LIST_TYPE_COMPLETE + "||" + FILE_LIST_TYPE_PARTIAL);
		}
		int sID = data.getInt(DirectByteBuffer.SS_MSG);
		int cID = data.getInt(DirectByteBuffer.SS_MSG);
		byte[] f = new byte[data.remaining(DirectByteBuffer.SS_MSG)];
		data.get(DirectByteBuffer.SS_MSG, f);

		data.returnToPool();
		return new OSF2FTextSearchResp(version, t, sID, cID, f);
	}

	public String getID() {
		return OSF2FMessage.ID_OS_TEXT_SEARCH_RESP;
	}

	public byte[] getIDBytes() {
		return OSF2FMessage.ID_OS_TEXT_SEARCH_RESP_BYTES;
	}

	public String getFeatureID() {
		return OSF2FMessage.OS_FEATURE_ID;
	}

	public int getFeatureSubID() {
		return OSF2FMessage.SUBID_OS_TEXT_SEARCH_RESP;
	}

	public int getType() {
		return Message.TYPE_PROTOCOL_PAYLOAD;
	}

	public byte getVersion() {
		return version;
	};

	public String getDescription() {
		if (description == null) {
			description = OSF2FMessage.ID_OS_TEXT_SEARCH_RESP + "\t type=" + type;
			if (filelist != null) {
				description += " len=" + filelist.length + " searchid=" + Integer.toHexString(searchID) + " channelid=" + Integer.toHexString(channelID);
			}
		}

		return description;
	}

	public void destroy() {
		if (buffer[0] != null)
			buffer[0].returnToPool();
		if (buffer.length == 2 && buffer[1] != null)
			buffer[1].returnToPool();
	}

	public DirectByteBuffer[] getData() {
		if (buffer[0] == null) {
			buffer[0] = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, BASE_LENGTH);
			buffer[0].put(DirectByteBuffer.SS_MSG, type);
			buffer[0].putInt(DirectByteBuffer.SS_MSG, searchID);
			buffer[0].putInt(DirectByteBuffer.SS_MSG, channelID);
			buffer[0].flip(DirectByteBuffer.SS_MSG);
		}
		if (buffer.length == 2 && buffer[1] == null) {
			buffer[1] = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, filelist.length);
			buffer[1].put(DirectByteBuffer.SS_MSG, filelist);
			buffer[1].flip(DirectByteBuffer.SS_MSG);
		}
		return buffer;
	}

	public int getChannelID() {
		return channelID;
	}

	public int getSearchID() {
		return searchID;
	}

	public int getMessageSize() {
		return messageLength;
	}

}
