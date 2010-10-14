package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import com.aelitis.azureus.core.peermanager.messaging.Message;

public interface OSF2FAuthMessage extends Message {
	public static final String ONESWARM_AUTH_PROTOCOL = "OneSwarmAuth";

	public static final byte CURRENT_VERSION = 1;

	public static final String OSA_FEATURE_ID = "OSA1";

	public abstract int getMessageSize();
	
	public static final String ID_OSA_HANDSHAKE = "OSA_HANDSHAKE";
	public static final byte[] ID_OSA_HANDSHAKE_BYTES = ID_OSA_HANDSHAKE.getBytes();
	public static final byte SUBID_OSA_HANDSHAKE = 0 + 96;

	public static final String ID_OSA_AUTH_STATUS = "OSA_AUTH_STATUS";
	public static final byte[] ID_OSA_AUTH_STATUS_BYTES = ID_OSA_AUTH_STATUS.getBytes();
	public static final byte SUBID_OSA_AUTH_STATUS = 1 + 96;

	public static final String ID_OSA_AUTH_REQUEST = "OSA_AUTH_REQUEST";
	public static final byte[] ID_OSA_AUTH_REQUEST_BYTES = ID_OSA_AUTH_REQUEST.getBytes();
	public static final byte SUBID_OSA_AUTH_REQUEST = 2 + 96;

	public static final String ID_OSA_RESPONSE = "OSA_RESPONSE";
	public static final byte[] ID_OSA_RESPONSE_BYTES = ID_OSA_RESPONSE.getBytes();
	public static final byte SUBID_OSA_RESPONSE = 3 + 96;

	public static final byte LAST_ID = SUBID_OSA_RESPONSE;

}
