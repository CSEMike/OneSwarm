package edu.washington.cs.oneswarm.f2f.servicesharing;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;

public class OSF2FServiceDataMsg extends OSF2FChannelDataMsg {
    /**
     * Service Message Header:
     * [version_][control_][window____________]
     * [sequence number_______________________]
     * [options____________________________...]
     * [data_______________________________...]
     * Control byte holds [length*4 reserved*4]
     */
    private final byte version;
    private final int[] options;
    private final short window;
    private final int sequenceNumber;
    // private final byte[] options;
    private DirectByteBuffer serviceHeader;
    private static final byte VERSION_NUM = 42;

    public OSF2FServiceDataMsg(byte _version, int channelID, int sequenceNumber, short window,
            int[] options,
            DirectByteBuffer data) {
        super(_version, channelID, data);
        this.version = VERSION_NUM;
        this.window = window;
        this.options = options;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " [Sequence number " + sequenceNumber + "]";
    }

    @Override
    public DirectByteBuffer[] getData() {
        DirectByteBuffer[] channelmsg = super.getData();
        DirectByteBuffer[] fullmsg = new DirectByteBuffer[3];

        if (serviceHeader == null) {
            int length = 2 + options.length;
            serviceHeader = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, 4 * length);
            serviceHeader.put(DirectByteBuffer.SS_MSG, version);
            byte control = 0;
            control += length << 4;
            serviceHeader.put(DirectByteBuffer.SS_MSG, control);
            serviceHeader.putShort(DirectByteBuffer.SS_MSG, window);
            serviceHeader.putInt(DirectByteBuffer.SS_MSG, sequenceNumber);
            for (int option : options) {
                serviceHeader.putInt(DirectByteBuffer.SS_MSG, option);
            }
            serviceHeader.flip(DirectByteBuffer.SS_MSG);
        }

        fullmsg[0] = channelmsg[0];
        fullmsg[1] = serviceHeader;
        fullmsg[2] = channelmsg[1];
        return fullmsg;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (serviceHeader != null) {
            serviceHeader.returnToPool();
        }
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public static OSF2FServiceDataMsg fromChannelMessage(OSF2FChannelDataMsg msg)
            throws MessageException {
        DirectByteBuffer payload = msg.transferPayload();

        if (payload.remaining(SS_MSG) < 8) {
            throw new MessageException("Not a Service Message - no Service Header");
        }
        byte version = payload.get(SS_MSG);
        if (version != VERSION_NUM) {
            throw new MessageException("Incorrect Service Version Number.");
        }
        byte control = payload.get(SS_MSG);
        int words = (control & 0xf0) >> 4;
        short window = payload.getShort(SS_MSG);
        words--;
        int num = payload.getInt(SS_MSG);
        words--;
        int[] options = new int[words];
        for (int i = 0; i < words; i++) {
            options[i] = payload.getInt(SS_MSG);
        }
        return new OSF2FServiceDataMsg(msg.getVersion(), msg.getChannelId(), num, window, options,
                payload);
    }
}
