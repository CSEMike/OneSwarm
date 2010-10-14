package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

public class OSF2FAuthStatus implements OSF2FAuthMessage {

	private final byte version;
	private final int status;

	private String description;
	private DirectByteBuffer buffer;
	private final static int MESSAGE_LENGTH = 4;
	public static final int STATUS_INVITE_KEY_OK = 0;
	public static final int STATUS_INVITE_ERR_PUB_KEY = -1;
	public static final int STATUS_INVITE_ERR_INV_KEY = -2;
	public static final int STATUS_INVITE_ERR_PROTOCOL = -3;
	

	public OSF2FAuthStatus(byte version, int response) {
		this.status = response;
		this.version = version;
	}

	public OSF2FAuthStatus clone() {
		return new OSF2FAuthStatus(this.getVersion(), this.getStatus());
	}

	public String getID() {
		return OSF2FAuthMessage.ID_OSA_AUTH_STATUS;
	}

	public byte[] getIDBytes() {
		return OSF2FAuthMessage.ID_OSA_AUTH_STATUS_BYTES;
	}

	public String getFeatureID() {
		return OSF2FAuthMessage.OSA_FEATURE_ID;
	}

	public int getFeatureSubID() {
		return OSF2FAuthMessage.SUBID_OSA_AUTH_STATUS;
	}

	public int getType() {
		return Message.TYPE_PROTOCOL_PAYLOAD;
	}

	public byte getVersion() {
		return version;
	};

	public String getDescription() {
		if (description == null) {
			description = OSF2FAuthMessage.ID_OSA_AUTH_STATUS + "\tauth_status=" + status;
		}

		return description;
	}

	public DirectByteBuffer[] getData() {
		if (buffer == null) {
			buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, MESSAGE_LENGTH);
			buffer.putInt(DirectByteBuffer.SS_MSG, status);
			buffer.flip(DirectByteBuffer.SS_MSG);
		}
		return new DirectByteBuffer[] { buffer };
	}

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException("[" + getID() + "] decode error: data == null");
		}

		if (data.remaining(DirectByteBuffer.SS_MSG) != MESSAGE_LENGTH) {
			throw new MessageException("[" + getID() + "] decode error: payload.remaining[" + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + MESSAGE_LENGTH);
		}
		byte response = data.get(DirectByteBuffer.SS_MSG);
		data.returnToPool();
		return new OSF2FAuthStatus(version, response);
	}

	public void destroy() {
		if (buffer != null)
			buffer.returnToPool();
	}

	public int getMessageSize() {
		return MESSAGE_LENGTH;
	}

	public int getStatus() {
		return status;
	}
}
