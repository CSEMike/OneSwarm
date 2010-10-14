package edu.washington.cs.oneswarm.f2f.messaging;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

public class OSF2FMessageEncoder implements MessageStreamEncoder {

	public OSF2FMessageEncoder() {
		/* nothing */
	}

	public RawMessage[] encodeMessage(Message message) {
		return new RawMessage[] { OSF2FMessageFactory
				.createOSF2FRawMessage(message) };
	}

}
