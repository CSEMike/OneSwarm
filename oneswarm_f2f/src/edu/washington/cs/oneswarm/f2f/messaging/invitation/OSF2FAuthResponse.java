package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthRequest.AuthType;

public class OSF2FAuthResponse implements OSF2FAuthMessage {

	private final byte version;

	private final AuthType authType;
	private String description;

	private final static int BASE_LENGTH = 4;
	private DirectByteBuffer buffer;
	private byte[] response;

	public OSF2FAuthResponse(byte version, AuthType authType, byte[] response) {
		this.version = version;
		this.response = response;
		this.authType = authType;
	}

	public OSF2FAuthResponse clone() {
		return new OSF2FAuthResponse(this.getVersion(), this.getAuthType(), this.getResponse());
	}

	public AuthType getAuthType() {
		return authType;
	}

	public byte[] getResponse() {
		return response;
	}

	public String getID() {
		return OSF2FAuthMessage.ID_OSA_RESPONSE;
	}

	public byte[] getIDBytes() {
		return OSF2FAuthMessage.ID_OSA_RESPONSE_BYTES;
	}

	public String getFeatureID() {
		return OSF2FAuthMessage.OSA_FEATURE_ID;
	}

	public int getFeatureSubID() {
		return OSF2FAuthMessage.SUBID_OSA_RESPONSE;
	}

	public int getType() {
		return Message.TYPE_PROTOCOL_PAYLOAD;
	}

	public byte getVersion() {
		return version;
	};

	public String getDescription() {
		if (description == null) {
			description = OSF2FAuthMessage.ID_OSA_RESPONSE + "\ttype" + authType.name();
		}

		return description;
	}

	public DirectByteBuffer[] getData() {
		if (buffer == null) {
			buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, getMessageSize());
			buffer.putInt(DirectByteBuffer.SS_MSG, authType.getID());
			buffer.put(DirectByteBuffer.SS_MSG, response);
			buffer.flip(DirectByteBuffer.SS_MSG);
		}
		return new DirectByteBuffer[] { buffer };
	}

	public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
		if (data == null) {
			throw new MessageException("[" + getID() + "] decode error: data == null");
		}

		if (data.remaining(DirectByteBuffer.SS_MSG) < BASE_LENGTH) {
			throw new MessageException("[" + getID() + "] decode error: payload.remaining[" + data.remaining(DirectByteBuffer.SS_MSG) + "] != " + BASE_LENGTH);
		}
		int a = data.getInt(DirectByteBuffer.SS_MSG);
		byte[] r = new byte[data.remaining(DirectByteBuffer.SS_MSG)];
		data.get(DirectByteBuffer.SS_MSG, r);
		data.returnToPool();
		return new OSF2FAuthResponse(version, AuthType.getFromID(a), r);
	}

	public void destroy() {
		if (buffer != null)
			buffer.returnToPool();
	}

	public long bytesToLong(byte[] b) {
		{
			long val = 0;
			for (int i = 0; i < 8; i++) {
				int shift = 7 - i;
				val |= (b[i] << shift);
			}
			return val;
		}
	}

	public int getMessageSize() {
		return BASE_LENGTH + response.length;
	}
}
