package edu.washington.cs.oneswarm.f2f.servicesharing;

import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.peermanager.messaging.MessageException;

import edu.washington.cs.oneswarm.f2f.messaging.OSF2FChannelDataMsg;

public class OSF2FServiceDataMsg extends OSF2FChannelDataMsg {
    private final int sequenceNumber;
    private static final int SEQ_NUM_LENGTH = 4;
    private DirectByteBuffer serviceHeader;

    public OSF2FServiceDataMsg(byte _version, int channelID, int sequenceNumber,
            DirectByteBuffer data) {
        super(_version, channelID, data);
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
            serviceHeader = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG, SEQ_NUM_LENGTH);
            serviceHeader.putInt(DirectByteBuffer.SS_MSG, sequenceNumber);
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

        if (payload.remaining(SS_MSG) < SEQ_NUM_LENGTH) {
            throw new MessageException("Not a Service Message - no Service Header");
        }
        int num = payload.getInt(SS_MSG);
        return new OSF2FServiceDataMsg(msg.getVersion(), msg.getChannelId(), num, payload);
    }
}
