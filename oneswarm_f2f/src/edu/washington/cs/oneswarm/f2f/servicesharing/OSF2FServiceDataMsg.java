package edu.washington.cs.oneswarm.f2f.servicesharing;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;

public class OSF2FServiceDataMsg extends OSF2FChannelDataMsg {
    /**
     * Service Message Header:
     * [version_][control_][subchannel________]
     * [sequence number_______________________]
     * [options____________________________...]
     * [data_______________________________...]
     * Control byte holds [length*4 ack syn rst reserved]
     * When the ack bit is set, 'sequence number' + all data words
     * are interpreted as acknowledgments.
     */
    private final byte version;
    private byte control = 0;
    private final int[] options;
    private final short subchannel;
    private final int sequenceNumber;
    // private final byte[] options;
    private DirectByteBuffer serviceHeader;
    private static final byte VERSION_NUM = 42;
    private static final byte ss = 1;
    // with no options: 1 word channel, 2 word header.
    public static final int BASE_LENGTH = 12;

    public OSF2FServiceDataMsg(byte _version, int channelID, int sequenceNumber, short subchannel,
            int[] options, DirectByteBuffer data) {
        super(_version, channelID, data);
        this.version = VERSION_NUM;
        this.subchannel = subchannel;
        this.options = options;
        this.sequenceNumber = sequenceNumber;
    }

    private OSF2FServiceDataMsg(byte _version, int channelID, int sequenceNumber, short subchannel,
            int[] options, DirectByteBuffer data, byte control) {
        this(_version, channelID, sequenceNumber, subchannel, options, data);
        this.control = control;
    }

    static OSF2FServiceDataMsg acknowledge(byte _version, int channelID, short subchannel,
            int[] acknowledgements) {
        int payloadSize = acknowledgements.length - 1;
        DirectByteBuffer data = null;
        if (payloadSize > 0) {
            data = DirectByteBufferPool.getBuffer(ss, 4 * payloadSize);
            for (int i = 0; i < payloadSize; i++) {
                data.putInt(ss, acknowledgements[i + 1]);
            }
            data.flip(ss);
        }
        OSF2FServiceDataMsg msg = new OSF2FServiceDataMsg(_version, channelID, acknowledgements[0],
                subchannel, new int[0], data, (byte) 8);
        return msg;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + " [Sequence number " + sequenceNumber + "]";
    }

    @Override
    public DirectByteBuffer[] getData() {
        DirectByteBuffer[] channelmsg = super.getData();
        DirectByteBuffer[] fullmsg;
        if (channelmsg[1] == null) {
            fullmsg = new DirectByteBuffer[2];
        } else {
            fullmsg = new DirectByteBuffer[3];
            fullmsg[2] = channelmsg[1];
        }

        if (serviceHeader == null) {
            int length = 2 + options.length;
            serviceHeader = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, 4 * length);
            serviceHeader.put(DirectByteBuffer.SS_MSG, version);
            byte control = this.control;
            control += length << 4;
            serviceHeader.put(DirectByteBuffer.SS_MSG, control);
            serviceHeader.putShort(DirectByteBuffer.SS_MSG, subchannel);
            serviceHeader.putInt(DirectByteBuffer.SS_MSG, sequenceNumber);
            for (int option : options) {
                serviceHeader.putInt(DirectByteBuffer.SS_MSG, option);
            }
            serviceHeader.flip(DirectByteBuffer.SS_MSG);
            // System.err.println(String.format(
            // "OUT: %d, CONTROL: %d, WORDS: %d, WINDOW: %d, NUM:%d, REMAINING: %d",
            // version,
            // control, length, window, sequenceNumber,
            // getPayload().remaining(SS_MSG)));
        }

        fullmsg[0] = channelmsg[0];
        fullmsg[1] = serviceHeader;
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

    public short getSubchannel() {
        return subchannel;
    }

    public static OSF2FServiceDataMsg fromChannelMessage(OSF2FChannelDataMsg msg)
            throws MessageException {
        if (msg instanceof OSF2FServiceDataMsg) {
            throw new MessageException("message already OSF2FServiceDataMsg!");
        }
        checkIfServiceMessage(msg);
        DirectByteBuffer payload = msg.transferPayload();
        byte version = payload.get(SS_MSG);
        byte control = payload.get(SS_MSG);
        int words = (control & 0xf0) >> 4;
        short subchannel = payload.getShort(SS_MSG);
        words--;
        int num = payload.getInt(SS_MSG);
        words--;
        // System.err.println(String.format(
        // "VERSION: %d, CONTROL: %d, WORDS: %d, WINDOW: %d, NUM:%d REMAINING: %d",
        // version,
        // control, words, window, num, payload.remaining(SS_MSG)));
        int[] options = new int[words];
        for (int i = 0; i < words; i++) {
            options[i] = payload.getInt(SS_MSG);
        }
        return new OSF2FServiceDataMsg(version, msg.getChannelId(), num, subchannel, options,
                payload,
                (byte) (control & 0x0f));
    }

    private static void checkIfServiceMessage(OSF2FChannelDataMsg msg) throws MessageException {
        DirectByteBuffer payload = msg.getPayload();
        if (payload.remaining(SS_MSG) < 8) {
            throw new MessageException("Not a Service Message - no Service Header");
        }
        int oldPos = payload.position(SS_MSG);
        byte version = payload.get(SS_MSG);
        payload.position(SS_MSG, oldPos);
        if (version != VERSION_NUM) {
            throw new MessageException("Incorrect Service Version Number.");
        }
    }

    @Override
    public int getMessageSize() {
        return super.getMessageSize() + 4 * (2 + options.length);
    }

    public boolean isAck() {
        return (this.control & 8) == 8;
    }

    public boolean isSyn() {
        return (this.control & 4) == 4;
    }

    public void setControlFlag(int flag) {
        if (flag < 0 || flag > 15) {
            return;
        }
        this.control |= 1 << flag;
    }

    public boolean isRst() {
        return (this.control & 2) == 2;
    }
}
