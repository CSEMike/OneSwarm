package edu.washington.cs.oneswarm.f2f.messaging;

import java.util.HashMap;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;

import com.aelitis.azureus.core.networkmanager.RawMessage;
import com.aelitis.azureus.core.networkmanager.impl.RawMessageImpl;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;
import com.aelitis.azureus.core.peermanager.messaging.MessageManager;

public class OSF2FMessageFactory {
    private final static boolean NO_DELAY = true;
    private final static boolean DELAY_OK = false;
    private static final String[] id_to_name = new String[OSF2FMessage.LAST_ID + 1];
    private static final HashMap<String, LegacyData> legacy_data = new HashMap<String, LegacyData>();
    static {
        legacy_data.put(OSF2FMessage.ID_OS_TEXT_SEARCH, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_TEXT_SEARCH));
        id_to_name[OSF2FMessage.SUBID_OS_TEXT_SEARCH] = OSF2FMessage.ID_OS_TEXT_SEARCH;

        legacy_data.put(OSF2FMessage.ID_OS_TEXT_SEARCH_RESP, new LegacyData(
                RawMessage.PRIORITY_HIGH, NO_DELAY, null, OSF2FMessage.SUBID_OS_TEXT_SEARCH_RESP));
        id_to_name[OSF2FMessage.SUBID_OS_TEXT_SEARCH_RESP] = OSF2FMessage.ID_OS_TEXT_SEARCH_RESP;

        legacy_data.put(OSF2FMessage.ID_OS_METAINFO_REQ, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_METAINFO_REQ));
        id_to_name[OSF2FMessage.SUBID_OS_METAINFO_REQ] = OSF2FMessage.ID_OS_METAINFO_REQ;

        legacy_data.put(OSF2FMessage.ID_OS_METAINFO_RESP, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_METAINFO_RESP));
        id_to_name[OSF2FMessage.SUBID_OS_METAINFO_RESP] = OSF2FMessage.ID_OS_METAINFO_RESP;

        legacy_data.put(OSF2FMessage.ID_OS_CHANNEL_SETUP, new LegacyData(RawMessage.PRIORITY_HIGH,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_CHANNEL_SETUP));
        id_to_name[OSF2FMessage.SUBID_OS_CHANNEL_SETUP] = OSF2FMessage.ID_OS_CHANNEL_SETUP;

        legacy_data
                .put(OSF2FMessage.ID_OS_CHANNEL_DATA_MSG, new LegacyData(
                        RawMessage.PRIORITY_NORMAL, NO_DELAY, null,
                        OSF2FMessage.SUBID_OS_CHANNEL_DATA_MSG));
        id_to_name[OSF2FMessage.SUBID_OS_CHANNEL_DATA_MSG] = OSF2FMessage.ID_OS_CHANNEL_DATA_MSG;

        legacy_data.put(OSF2FMessage.ID_OS_CHANNEL_RST, new LegacyData(RawMessage.PRIORITY_LOW,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_CHANNEL_RST));
        id_to_name[OSF2FMessage.SUBID_OS_CHANNEL_RST] = OSF2FMessage.ID_OS_CHANNEL_RST;

        legacy_data.put(OSF2FMessage.ID_OS_HASH_SEARCH, new LegacyData(RawMessage.PRIORITY_NORMAL,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_HASH_SEARCH));
        id_to_name[OSF2FMessage.SUBID_OS_HASH_SEARCH] = OSF2FMessage.ID_OS_HASH_SEARCH;

        legacy_data.put(OSF2FMessage.ID_OS_SEARCH_CANCEL, new LegacyData(
                RawMessage.PRIORITY_NORMAL, NO_DELAY, null, OSF2FMessage.SUBID_OS_SEARCH_CANCEL));
        id_to_name[OSF2FMessage.SUBID_OS_SEARCH_CANCEL] = OSF2FMessage.ID_OS_SEARCH_CANCEL;

        legacy_data.put(OSF2FMessage.ID_OS_CHAT, new LegacyData(RawMessage.PRIORITY_NORMAL,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_CHAT));
        id_to_name[OSF2FMessage.SUBID_OS_CHAT] = OSF2FMessage.ID_OS_CHAT;

        legacy_data.put(OSF2FMessage.ID_OS_DHT_LOCATION, new LegacyData(RawMessage.PRIORITY_NORMAL,
                NO_DELAY, null, OSF2FMessage.SUBID_OS_DHT_LOCATION));
        id_to_name[OSF2FMessage.SUBID_OS_DHT_LOCATION] = OSF2FMessage.ID_OS_DHT_LOCATION;

        legacy_data.put(OSF2FMessage.ID_OS_PUZZLE_WRAPPER, new LegacyData(
                RawMessage.PRIORITY_NORMAL, NO_DELAY, null, OSF2FMessage.SUBID_OS_PUZZLE_WRAPPER));
        id_to_name[OSF2FMessage.SUBID_OS_PUZZLE_WRAPPER] = OSF2FMessage.ID_OS_PUZZLE_WRAPPER;
    }

    /**
     * Initialize the factory, i.e. register the messages with the message
     * manager.
     */
    public static void init() {
        try {

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FHandshake(OSF2FMessage.CURRENT_VERSION, new byte[0]));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FHashSearch(OSF2FMessage.CURRENT_VERSION, 0, 0));

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FHashSearchResp(OSF2FMessage.CURRENT_VERSION, 0, 0, 0));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FChannelDataMsg(OSF2FMessage.CURRENT_VERSION, 0, null));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FChannelReset(OSF2FMessage.CURRENT_VERSION, 0));

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FTextSearch(OSF2FMessage.CURRENT_VERSION, (byte) 0, 0, ""));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FTextSearchResp(OSF2FMessage.CURRENT_VERSION, (byte) 0, 0, 0,
                            new byte[0]));

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FMetaInfoReq(OSF2FMessage.CURRENT_VERSION, 0,
                            OSF2FMessage.METAINFO_TYPE_BITTORRENT, 0, new byte[0]));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FMetaInfoResp(OSF2FMessage.CURRENT_VERSION, 0,
                            OSF2FMessage.METAINFO_TYPE_BITTORRENT, 0, 0, 0, new byte[0]));

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FSearchCancel(OSF2FMessage.CURRENT_VERSION, 0));

            MessageManager.getSingleton().registerMessageType(
                    new OSF2FChat(OSF2FChat.CURRENT_VERSION, null));
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FDhtLocation(OSF2FDhtLocation.CURRENT_VERSION, null, null));
            
            MessageManager.getSingleton().registerMessageType(
                    new OSF2FPuzzleWrappedMessage(OSF2FPuzzleWrappedMessage.CURRENT_VERSION, null,
                            0, new byte[20]));

        } catch (MessageException me) {
            me.printStackTrace();
        }
    }

    public static int getMessageType(DirectByteBuffer stream_payload) {
        byte id = stream_payload.get(DirectByteBuffer.SS_MSG, 0);
        if (id == 101)
         {
            return Message.TYPE_PROTOCOL_PAYLOAD; // handshake
        }
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
        case OSF2FMessage.SUBID_OS_CHANNEL_SETUP:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_CHANNEL_SETUP_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_HASH_SEARCH:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_HASH_SEARCH_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_CHANNEL_DATA_MSG:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_CHANNEL_DATA_MSG_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_CHANNEL_RST:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_CHANNEL_RST_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_TEXT_SEARCH:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_TEXT_SEARCH_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_TEXT_SEARCH_RESP:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_TEXT_SEARCH_RESP_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_METAINFO_REQ:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_METAINFO_REQ_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_METAINFO_RESP:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_METAINFO_RESP_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_SEARCH_CANCEL:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_SEARCH_CANCEL_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_CHAT:
            return MessageManager.getSingleton().createMessage(OSF2FMessage.ID_OS_CHAT_BYTES,
                    stream_payload, OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_DHT_LOCATION:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_DHT_LOCATION_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
        case OSF2FMessage.SUBID_OS_PUZZLE_WRAPPER:
            return MessageManager.getSingleton().createMessage(
                    OSF2FMessage.ID_OS_PUZZLE_WRAPPER_BYTES, stream_payload,
                    OSF2FMessage.CURRENT_VERSION);
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

        LegacyData ld = legacy_data.get(base_message.getID());

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
