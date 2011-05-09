package edu.washington.cs.oneswarm.community;

// this mirrors an identical file in the community server. 

public final class CommunityConstants {
    /**
     * Form field names
     */
    public static final String BASE64_PUBLIC_KEY = "base64key";
    public static final String NICKNAME = "nick";
    public static final String CHALLENGE_RESPONSE = "resp";

    /**
     * Form response bodies
     */
    public static final String REGISTRATION_SUCCESS = "REGISTRATION_OK";
    public static final String REGISTRATION_DUPLICATE = "REGISTRATION_DUPLICATE";
    public static final String REGISTRATION_RATE_LIMITED = "REGISTRATION_RATE_LIMITED";

    public static final String CHALLENGE = "CHALLENGE";

    /**
     * XML elements and attributes
     */
    public static final String RESPONSE_ROOT = "CommunityServerResponse";
    public static final String REFRESH_INTERVAL = "RefreshInterval";
    public static final String FRIEND_LIST = "FriendList";
    public static final String FRIEND = "Friend";
    public static final String KEY_ATTRIB = "Base64Key";
    public static final String NICK_ATTRIB = "nick";

    public static final int MAX_NICK_LENGTH = 128;

    /**
     * Capabilities XML
     */
    public static final String CAPABILITIES_ROOT = "capabilities";

    public static final String PEERS = "peers";
    public static final String PUBLISH = "publish";
    public static final String PATH_ATTRIB = "path";

    public static final String ID = "id";
    public static final String NAME_ATTRIB = "name";

    public static final String SPLASH = "splash";

    public static final String SKIPSSL = "nossl";
    public static final String PORT_ATTRIB = "port";

    public static final String SEARCH_FILTER = "searchfilter";
    public static final String KEYWORD = "keyword";

    public static final String CHT = "cht";

    /**
     * Categories XML
     */
    public static final String CATEGORIES_ROOT = "categories";
    public static final String CATEGORY = "category";

}
