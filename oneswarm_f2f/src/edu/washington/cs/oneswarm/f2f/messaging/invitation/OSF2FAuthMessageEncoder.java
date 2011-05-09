package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

public class OSF2FAuthMessageEncoder implements MessageStreamEncoder {

    public OSF2FAuthMessageEncoder() {
        /* nothing */
    }

    public RawMessage[] encodeMessage(Message message) {
        return new RawMessage[] { OSF2FAuthMessageFatory.createOSF2FRawMessage(message) };
    }

}
