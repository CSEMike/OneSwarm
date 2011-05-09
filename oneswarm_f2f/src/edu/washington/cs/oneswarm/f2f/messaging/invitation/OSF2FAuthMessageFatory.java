package edu.washington.cs.oneswarm.f2f.messaging.invitation;

import java.util.HashMap;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;

import edu.washington.cs.oneswarm.f2f.messaging.invitation.OSF2FAuthRequest.AuthType;

public class OSF2FAuthMessageFatory {
    private final static boolean NO_DELAY = true;
    // private final static boolean DELAY_OK = false;
    private static final String[] id_to_name = new String[OSF2FAuthMessage.LAST_ID + 1];
    private static final HashMap<String, LegacyData> legacy_data = new HashMap<String, LegacyData>();
    static {
        legacy_data.put(OSF2FAuthMessage.ID_OSA_HANDSHAKE, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FAuthMessage.SUBID_OSA_HANDSHAKE));
        id_to_name[OSF2FAuthMessage.SUBID_OSA_HANDSHAKE] = OSF2FAuthMessage.ID_OSA_HANDSHAKE;

        legacy_data.put(OSF2FAuthMessage.ID_OSA_AUTH_STATUS, new LegacyData(
                RawMessage.PRIORITY_HIGH, NO_DELAY, null, OSF2FAuthMessage.SUBID_OSA_AUTH_STATUS));
        id_to_name[OSF2FAuthMessage.SUBID_OSA_AUTH_STATUS] = OSF2FAuthMessage.ID_OSA_AUTH_STATUS;

        legacy_data.put(OSF2FAuthMessage.ID_OSA_AUTH_REQUEST, new LegacyData(
                RawMessage.PRIORITY_HIGH, NO_DELAY, null, OSF2FAuthMessage.SUBID_OSA_AUTH_REQUEST));
        id_to_name[OSF2FAuthMessage.SUBID_OSA_AUTH_REQUEST] = OSF2FAuthMessage.ID_OSA_AUTH_REQUEST;

        legacy_data.put(OSF2FAuthMessage.ID_OSA_RESPONSE, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FAuthMessage.SUBID_OSA_RESPONSE));
        id_to_name[OSF2FAuthMessage.SUBID_OSA_RESPONSE] = OSF2FAuthMessage.ID_OSA_RESPONSE;

    }

    /**
     * Initialize the factory, i.e. register the messages with the message
     * manager.
     */
    public static void init() {
        try {

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FAuthHandshake(OSF2FAuthMessage.CURRENT_VERSION, new byte[0]));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FAuthStatus(OSF2FAuthMessage.CURRENT_VERSION, 0));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FAuthRequest(OSF2FAuthMessage.CURRENT_VERSION,
                            OSF2FAuthRequest.AuthType.KEY, null));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FAuthResponse(OSF2FAuthMessage.CURRENT_VERSION, AuthType.getFromID(0),
                            new byte[0]));
        } catch (MessageException me) {
            me.printStackTrace();
        }
    }

    public static int getMessageType(DirectByteBuffer stream_payload) {
        byte id = stream_payload.get(DirectByteBuffer.SS_MSG, 0);
        System.err.println("decoding: id=" + id);
        if (id == 83)
            return Message.TYPE_PROTOCOL_PAYLOAD; // handshake
        // message byte
        // in position 4
        // ("e" in
        // OneSwarm)
        if (id >= 0 && id < id_to_name.length) {
            return MessageManager.getSingleton().lookupMessage(id_to_name[id]).getType();
        }
        // invalid, return whatever
        return Message.TYPE_PROTOCOL_PAYLOAD;
    }

    /**
     * Construct a new OSF2F message instance from the given message raw byte
     * stream.
     * 
     * @param stream_payload
     *            data
     * @return decoded/deserialized BT message
     * @throws MessageException
     *             if message creation failed NOTE: Does not auto-return given
     *             direct buffer on thrown exception.
     */
    public static Message createOSF2FMessage(DirectByteBuffer stream_payload)
            throws MessageException {
        byte id = stream_payload.get(DirectByteBuffer.SS_MSG);

        switch (id) {
        case OSF2FAuthMessage.SUBID_OSA_HANDSHAKE:
            return MessageManager.getSingleton().createMessage(
                    OSF2FAuthMessage.ID_OSA_HANDSHAKE_BYTES, stream_payload,
                    OSF2FAuthMessage.CURRENT_VERSION);
        case OSF2FAuthMessage.SUBID_OSA_AUTH_REQUEST:
            return MessageManager.getSingleton().createMessage(
                    OSF2FAuthMessage.ID_OSA_AUTH_REQUEST_BYTES, stream_payload,
                    OSF2FAuthMessage.CURRENT_VERSION);
        case OSF2FAuthMessage.SUBID_OSA_AUTH_STATUS:
            return MessageManager.getSingleton().createMessage(
                    OSF2FAuthMessage.ID_OSA_AUTH_STATUS_BYTES, stream_payload,
                    OSF2FAuthMessage.CURRENT_VERSION);
        case OSF2FAuthMessage.SUBID_OSA_RESPONSE:
            return MessageManager.getSingleton().createMessage(
                    OSF2FAuthMessage.ID_OSA_RESPONSE_BYTES, stream_payload,
                    OSF2FAuthMessage.CURRENT_VERSION);

        default: {
            System.out.println("Unknown OSF2F message id [" + id + "]");
            throw new MessageException("Unknown OSF2F message id [" + id + "]");
        }
        }
    }

    /**
     * Create the proper OSF2F raw message from the given base message.
     * 
     * @param base_message
     *            to create from
     * @return BT raw message
     */
    public static RawMessage createOSF2FRawMessage(Message base_message) {
        if (base_message instanceof RawMessage) { // used for handshake and
            // keep-alive messages
            return (RawMessage) base_message;
        }

        LegacyData ld = (LegacyData) legacy_data.get(base_message.getID());

        if (ld == null) {
            Debug.out("legacy message type id not found for [" + base_message.getID() + "]");
            return null; // message id type not found
        }

        DirectByteBuffer[] payload = base_message.getData();

        int payload_size = 0;
        for (int i = 0; i < payload.length; i++) {
            payload_size += payload[i].remaining(DirectByteBuffer.SS_MSG);
        }

        DirectByteBuffer header = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_MSG_BT_HEADER,
                5);
        header.putInt(DirectByteBuffer.SS_MSG, 1 + payload_size);
        header.put(DirectByteBuffer.SS_MSG, ld.bt_id);
        header.flip(DirectByteBuffer.SS_MSG);

        DirectByteBuffer[] raw_buffs = new DirectByteBuffer[payload.length + 1];
        raw_buffs[0] = header;
        for (int i = 0; i < payload.length; i++) {
            raw_buffs[i + 1] = payload[i];
        }

        return new RawMessageImpl(base_message, raw_buffs, ld.priority, ld.is_no_delay,
                ld.to_remove);
    }

    protected static class LegacyData {
        protected final int priority;
        protected final boolean is_no_delay;
        protected final Message[] to_remove;
        protected final byte bt_id;

        protected LegacyData(int prio, boolean no_delay, Message[] remove, byte btid) {
            this.priority = prio;
            this.is_no_delay = no_delay;
            this.to_remove = remove;
            this.bt_id = btid;
        }
    }
}
