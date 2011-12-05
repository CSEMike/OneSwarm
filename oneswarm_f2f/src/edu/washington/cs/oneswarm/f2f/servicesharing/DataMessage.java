package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;

public class DataMessage implements OSF2FMessage {
    private static final byte SS = DirectByteBuffer.SS_MSG;
    public final static int MAX_SERVICE_PAYLOAD_SIZE = 1024;

    private DirectByteBuffer buffer = null;
    private static String ID = "RAW_MESSAGE";
    private final String desc;
    private final int size;

    public DataMessage(DirectByteBuffer _buffer) {
        this.buffer = _buffer;
        size = _buffer.remaining(SS);
        desc = "Raw message: " + size + " bytes";
    }

    @Override
    public int getMessageSize() {
        return size;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public byte[] getIDBytes() {
        return ID.getBytes();
    }

    @Override
    public String getFeatureID() {
        return (null);
    }

    @Override
    public int getFeatureSubID() {
        return (0);
    }

    @Override
    public int getType() {
        return (TYPE_DATA_PAYLOAD);
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public byte getVersion() {
        return (1);
    }

    public DirectByteBuffer getPayload() {
        return (buffer);
    }

    @Override
    public DirectByteBuffer[] getData() {
        return new DirectByteBuffer[] { buffer };
    }

    @Override
    public Message deserialize(DirectByteBuffer data, byte version) throws MessageException {
        throw (new MessageException("not implemented"));
    }

    @Override
    public void destroy() {
        if (buffer != null) {
            buffer.returnToPool();
            buffer = null;
        }
    }

    /**
     * Retrieve the payload from this message for transfer into a new message.
     * 
     * The new message is responsible for returning the buffer on destroy.
     * 
     * @return
     */
    public DirectByteBuffer transferPayload() {
        DirectByteBuffer data = buffer;
        buffer = null;
        return data;
    }

    static class RawMessageEncoder implements MessageStreamEncoder {
        @Override
        public RawMessage[] encodeMessage(Message base_message) {
            return new RawMessage[] { new RawMessageImpl(base_message, base_message.getData(),
                    RawMessage.PRIORITY_NORMAL, true, null) };
        }

    }

    static class RawMessageDecoder implements MessageStreamDecoder {

        DirectByteBuffer payload_buffer;
        private boolean paused = false;
        private IOException pendingException;

        private final ArrayList<Message> messages_last_read = new ArrayList<Message>();

        @Override
        public void resumeDecoding() {
            paused = false;
        }

        @Override
        public Message[] removeDecodedMessages() {
            if (messages_last_read.isEmpty()) {
                return null;
            }
            Message[] msgs = messages_last_read.toArray(new Message[messages_last_read.size()]);
            messages_last_read.clear();
            return msgs;
        }

        @Override
        public int performStreamDecode(Transport transport, int max_bytes) throws IOException {
            if (pendingException != null) {
                throw pendingException;
            }
            int bytes_left = max_bytes;
            while (bytes_left > 0) {
                if (payload_buffer == null) {
                    payload_buffer = DirectByteBufferPool.getBuffer(SS, MAX_SERVICE_PAYLOAD_SIZE);
                }
                if (paused) {
                    break;
                }

                // If we reach the end of the stream (get and
                // "end of stream on socket read" error)
                // return whatever bytes we have read so far and package them up
                // in a data message
                // then save the exception and return it on the next read.
                long read = 0;
                try {
                    read = transport.read(new ByteBuffer[] { payload_buffer.getBuffer(SS) }, 0, 1);
                } catch (IOException e) {
                    pendingException = e;
                }
                bytes_left -= read;
                // Message is done if:
                // * payload is full
                // * transport has no more data
                if (payload_buffer.remaining(SS) == 0 || read == 0) {
                    if (payload_buffer.position(SS) > 0) {
                        payload_buffer.flip(SS);
                        Message msg = new DataMessage(payload_buffer);
                        payload_buffer = null;
                        messages_last_read.add(msg);
                    }
                    // If we read all from transport, break
                    if (read == 0) {
                        break;
                    }
                }
            }
            return max_bytes - bytes_left;
        }

        @Override
        public void pauseDecoding() {
            paused = true;
        }

        @Override
        public int getProtocolBytesDecoded() {
            return 0;
        }

        @Override
        public int getPercentDoneOfCurrentMessage() {
            return (int) (getDataBytesDecoded() * 100.0 / MAX_SERVICE_PAYLOAD_SIZE);
        }

        @Override
        public int getDataBytesDecoded() {
            if (payload_buffer == null) {
                return 0;
            }
            return payload_buffer.position(SS);
        }

        @Override
        public ByteBuffer destroy() {
            if (payload_buffer != null) {
                payload_buffer.returnToPool();
                payload_buffer = null;
            }

            for (Message msg : messages_last_read) {
                msg.destroy();
            }
            messages_last_read.clear();
            return ByteBuffer.allocate(0);
        }
    }
}