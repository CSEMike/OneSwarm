package edu.washington.cs.oneswarm.test.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage;

public class MessageStreamDecoderTestImpl implements MessageStreamDecoder {
    
    private byte[] data;
    
    public void setData(byte[] dataBytes) {
        data = Arrays.copyOf(dataBytes, dataBytes.length);
    }

    @Override
    public int performStreamDecode(Transport transport, int max_bytes)
            throws IOException {
        return data.length;
    }

    @Override
    public Message[] removeDecodedMessages() {
        if (data == null) {
            return new Message[0];
        } else {
            Message[] messages = new Message[1];
            messages[0] = new DataMessage(new DirectByteBuffer( ByteBuffer.wrap( data)));
            return messages;
        }
    }

    @Override
    public int getProtocolBytesDecoded() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDataBytesDecoded() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getPercentDoneOfCurrentMessage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void pauseDecoding() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resumeDecoding() {
        // TODO Auto-generated method stub

    }

    @Override
    public ByteBuffer destroy() {
        // TODO Auto-generated method stub
        return null;
    }

}
