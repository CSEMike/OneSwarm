package edu.washington.cs.oneswarm.f2f.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;

public interface OSF2FMessage extends Message {
    public static final String ONESWARM_PROTOCOL = "OneSwarm F2F";
    public static final String SPD_HANDSHAKE = "OneSwarm SPD";

    public static final byte CURRENT_VERSION = 1;

    public static final String OS_FEATURE_ID = "OS1";

    public static final String ID_OS_HANDSHAKE = "OS_HANDSHAKE";
    public static final byte[] ID_OS_HANDSHAKE_BYTES = ID_OS_HANDSHAKE.getBytes();
    public static final byte SUBID_OS_HANDSHAKE = 0 + 64;

    public static final String ID_OS_HASH_SEARCH = "OS_HASH_SEARCH";
    public static final byte[] ID_OS_HASH_SEARCH_BYTES = ID_OS_HASH_SEARCH.getBytes();
    public static final byte SUBID_OS_HASH_SEARCH = 1 + 64;

    public static final String ID_OS_CHANNEL_SETUP = "OS_CHANNEL_SETUP";
    public static final byte[] ID_OS_CHANNEL_SETUP_BYTES = ID_OS_CHANNEL_SETUP.getBytes();
    public static final byte SUBID_OS_CHANNEL_SETUP = 2 + 64;

    public static final String ID_OS_CHANNEL_DATA_MSG = "OS_CHANNEL_MSG";
    public static final byte[] ID_OS_CHANNEL_DATA_MSG_BYTES = ID_OS_CHANNEL_DATA_MSG.getBytes();
    public static final byte SUBID_OS_CHANNEL_DATA_MSG = 3 + 64;

    public static final String ID_OS_CHANNEL_RST = "OS_CHANNEL_RST";
    public static final byte[] ID_OS_CHANNEL_RST_BYTES = ID_OS_CHANNEL_RST.getBytes();
    public static final byte SUBID_OS_CHANNEL_RST = 4 + 64;

    public static final String ID_OS_TEXT_SEARCH = "OS_TEXT_SEARCH";
    public static final byte[] ID_OS_TEXT_SEARCH_BYTES = ID_OS_TEXT_SEARCH.getBytes();
    public static final byte SUBID_OS_TEXT_SEARCH = 5 + 64;

    public static final String ID_OS_TEXT_SEARCH_RESP = "OS_TEXT_SEARCH_RESP";
    public static final byte[] ID_OS_TEXT_SEARCH_RESP_BYTES = ID_OS_TEXT_SEARCH_RESP.getBytes();
    public static final byte SUBID_OS_TEXT_SEARCH_RESP = 6 + 64;

    public static final String ID_OS_METAINFO_REQ = "OS_METAINFO_REQ";
    public static final byte[] ID_OS_METAINFO_REQ_BYTES = ID_OS_METAINFO_REQ.getBytes();
    public static final byte SUBID_OS_METAINFO_REQ = 7 + 64;

    public static final String ID_OS_METAINFO_RESP = "OS_METAINFO_RESP";
    public static final byte[] ID_OS_METAINFO_RESP_BYTES = ID_OS_METAINFO_RESP.getBytes();
    public static final byte SUBID_OS_METAINFO_RESP = 8 + 64;

    public static final String ID_OS_SEARCH_CANCEL = "OS_SEARCH_CANCEL";
    public static final byte[] ID_OS_SEARCH_CANCEL_BYTES = ID_OS_SEARCH_CANCEL.getBytes();
    public static final byte SUBID_OS_SEARCH_CANCEL = 9 + 64;

    public static final String ID_OS_CHAT = "OS_CHAT";
    public static final byte[] ID_OS_CHAT_BYTES = ID_OS_CHAT.getBytes();
    public static final byte SUBID_OS_CHAT = 10 + 64;

    public static final String ID_OS_DHT_LOCATION = "OS_DHT_LOCATION";
    public static final byte[] ID_OS_DHT_LOCATION_BYTES = ID_OS_DHT_LOCATION.getBytes();
    public static final byte SUBID_OS_DHT_LOCATION = 11 + 64;

    public static final String ID_OS_DATAGRAM_INIT = "OS_DATAGRAM_INIT";
    public static final byte[] ID_OS_DATAGRAM_INIT_BYTES = ID_OS_DATAGRAM_INIT.getBytes();
    public static final byte SUBID_OS_DATAGRAM_INIT = 12 + 64;

    public static final String ID_OS_DATAGRAM_OK = "OS_DATAGRAM_OK";
    public static final byte[] ID_OS_DATAGRAM_OK_BYTES = ID_OS_DATAGRAM_OK.getBytes();
    public static final byte SUBID_OS_DATAGRAM_OK = 13 + 64;

    public static final byte LAST_ID = SUBID_OS_DATAGRAM_OK;

    public final byte METAINFO_TYPE_BITTORRENT = 0;
    public final byte METAINFO_TYPE_THUMBNAIL = 1;
    public final byte[] METAINFO_TYPES = { METAINFO_TYPE_BITTORRENT, METAINFO_TYPE_THUMBNAIL };

    public final byte FILE_LIST_TYPE_COMPLETE = 0;
    public final byte FILE_LIST_TYPE_BLOOM = 1;
    public final byte FILE_LIST_TYPE_PARTIAL = 2;

    public final byte[] FILE_LIST_TYPES = { FILE_LIST_TYPE_COMPLETE, FILE_LIST_TYPE_BLOOM,
            FILE_LIST_TYPE_PARTIAL };

    // 4 for the length field
    // 1 for the type field
    public final static int MESSAGE_HEADER_LEN = 4 + 1;

    public final static int MAX_MESSAGE_SIZE = 16384;

    // save 16 bytes for headers and stuff, this ensures that the
    // receiver can use 4096 byte buffers
    public final static int MAX_PAYLOAD_SIZE = MAX_MESSAGE_SIZE - 16;

    public static final int METAINFO_CHUNK_SIZE = 4096;

    static final byte SS_MSG = DirectByteBuffer.SS_MSG;

    public abstract int getMessageSize();
}
