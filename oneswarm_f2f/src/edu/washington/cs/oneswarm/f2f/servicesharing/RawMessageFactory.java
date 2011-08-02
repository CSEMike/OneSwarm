package edu.washington.cs.oneswarm.f2f.servicesharing;

import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamFactory;

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;

class RawMessageFactory implements MessageStreamFactory {
    @Override
    public MessageStreamDecoder createDecoder() {
        return new RawMessageDecoder();
    }

    @Override
    public MessageStreamEncoder createEncoder() {

        return new RawMessageEncoder();
    }
}