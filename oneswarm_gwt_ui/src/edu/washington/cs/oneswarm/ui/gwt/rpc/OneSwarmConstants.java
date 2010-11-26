package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;

public interface OneSwarmConstants {

	public final static String BITTORRENT_MAGNET_PREFIX = "urn_btih_";
	public final static String BITTORRENT_MAGNET_PREFIX_REAL = "magnet:?xt=urn:btih:";
	public final static String BROWSE_SHARE_PATH = "/files";
	public final static String checkPath = "/check/";

	public static final int DEFAULT_WEB_PLAYER_HEIGTH = 480;
	public static final int DEFAULT_WEB_PLAYER_WIDTH = 640;

	public final static String DOWNLOAD_SHARE_PATH = "/download";
	public static final int ERROR_REPORTING_PORT = 6666;

	public static final String ERROR_REPORTING_SERVER = "127.0.0.1";

	public final static String FRIEND_INVITE_CODE_PREFIX = "code=";
	public final static String FRIEND_INVITE_NICK_PREFIX = "nick=";
	public final static String FRIEND_INVITE_PREFIX = "invite:";

	public static final String ONESWARM_DEFAULT_ENTRY_URL = "http://127.0.0.1:29615/";

	public final static String ONESWARM_DIRECT_LINK = ONESWARM_DEFAULT_ENTRY_URL + "redirect.html";

	public final static String REMOTE_ACCESS_LIMIT_IPS_KEY = "OSGWTUI.RemoteAccess.Limit.IPs";

	public final static String REMOTE_ACCESS_LIMIT_TYPE_KEY = "OSGWTUI.RemoteAccess.Limit.Type";

	public final static String REMOTE_ACCESS_LIMIT_TYPE_LAN = "lan";

	public final static String REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT = "nolimit";

	public final static String REMOTE_ACCESS_LIMIT_TYPE_RANGE = "range";

	public final static String REMOTE_ACCESS_PROPERTIES_KEY = "OSGWTUI.RemoteAccess";

	public final static String servletPath = "/oneswarmgwt/OneSwarmGWT";

	public final static int ST_ERROR = 8;

	public final static int ST_STOPPED = 7;

	public final static int ST_STOPPING = 6;

	public final static String STATS_PATH = "/expstats";

	public static Date TEN_YEARS_FROM_NOW = new Date(new Date().getTime() + 10 * 365 * 24 * 60 * 60 * 1000);

	// TODO: reduce this, go back to paged style?
	public final static int TORRENTS_PER_PAGE = Integer.MAX_VALUE;

	public final static String videoImagePath = "image/";
	// the /'s are not necessary (and apparently mess things up in hosted mode)
	public final static String videoPath = "oneswarmgwt/flv_movie";
	public final static String WEB_PARAM_FLV_PLAYER_AUTOSTART = "autostart";
	public final static String WEB_PARAM_FLV_PLAYER_BUFFER_LENGTH = "bufferlength";
	public final static String WEB_PARAM_FLV_PLAYER_HEIGHT = "height";
	// public static final String ONESWARM_LOCAL_DNS = "local.oneswarm.net";
	// public final static String ONESWARM_URL = "http://" + ONESWARM_LOCAL_DNS
	// + ":" + LOCAL_WEB_SERVER_PORT;
	public static final String WEB_PARAM_FLV_PLAYER_PLAYLIST = "playlist";
	public static final String WEB_PARAM_FLV_PLAYER_PLAYLIST_SIZE = "playlistsize";
	public static final String WEB_PARAM_FLV_PLAYER_REPEAT = "repeat";
	public final static String WEB_PARAM_FLV_PLAYER_WIDTH = "width";

	public final static String WEB_PARAM_METAINFO_PATH = "metaInfoUrl";
	public final static String WEB_PARAM_TORRENT_ID = "torrentID";
	public final static String WEB_PARAM_VIDEO_PATH = "videoPath";

	// private final static String[] SUPPORTED_VIDEO_TYPES =
	// org.gudy.azureus2.core3.disk.DiskManagerFileInfo.IN_ORDER_TYPES;
	// private final static String[] NO_CONVERT_TYPES = new String[] {
	// "mp3,flv", "flash_ready.mp4", "aac" };
	// private final static String[] NO_CONVERT_TYPES_MIME = new String[] { "",
	// "video/x-FLV", "", "audio/x-aac" };

	/*
	 * Remember to update
	 * org.gudy.azureus2.core3.disk.DiskManagerFileInfo.IN_ORDER_TYPES when
	 * adding a type and in order download is wanted
	 */
	public static enum InOrderType {
		AAC("aac", true, "audio/x-aac", "audio", FileTypeFilter.Audio),

		ASF("asf", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		AVI("avi", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		DIVX("divx", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		FLAC("flac", true, "video/x-FLV", "video", FileTypeFilter.Audio),

		FLV("flv", false, "video/x-FLV", "video", FileTypeFilter.Videos),

		GIF("gif", false, "image/gif", "image", FileTypeFilter.Other),

		JPEG("jpeg", false, "image/jpeg", "image", FileTypeFilter.Other),

		JPG("jpg", false, "image/jpeg", "image", FileTypeFilter.Other),

		MKV("mkv", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		MOV("mov", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		MP3("mp3", false, "audio/mpeg", "audio", FileTypeFilter.Audio),

		MP4("mp4", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		MP4_VIDEO("m4v", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		MP4_AUDIO("m4a", true, "audio/mpeg", "audio", FileTypeFilter.Audio),

		MP4_BOOK("m4b", true, "video/x-FLV", "video", FileTypeFilter.Audio),

		MPEG("mpeg", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		MPG("mpg", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		OGA("oga", true, "video/x-FLV", "video", FileTypeFilter.Audio),

		OGG("ogg", true, "video/x-FLV", "video", FileTypeFilter.Audio),

		OGV("ogv", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		PNG("png", false, "image/png", "image", FileTypeFilter.Other),

		WMA("wma", true, "video/x-FLV", "video", FileTypeFilter.Audio),

		WMV("wmv", true, "video/x-FLV", "video", FileTypeFilter.Videos),

		XVID("xvid", true, "video/x-FLV", "video", FileTypeFilter.Videos)

		// TIFF("tiff", true, "video/x-FLV", "image", FileTypeFilter.Other),

		// TIF("tif", true, "video/x-FLV", "image", FileTypeFilter.Other);

		;

		public final String convertedMime;

		public final boolean convertNeeded;

		public final String jwPlayerType;

		public final String suffix;
		public final FileTypeFilter type;

		InOrderType(String suffix, boolean convertNeeded, String convertedMime, String jwPlayerType, FileTypeFilter type) {
			this.suffix = suffix;
			this.convertNeeded = convertNeeded;
			this.convertedMime = convertedMime;
			this.jwPlayerType = jwPlayerType;
			this.type = type;
		}

		public FileTypeFilter getFileTypeFilter() {
			return type;
		}

		public static InOrderType getType(String filename) {
			if (filename != null) {
				for (InOrderType type : values()) {
					if (filename.toLowerCase().endsWith(type.suffix)) {
						return type;
					}
				}
			}
			return null;
		}
	}

	public static enum SecurityLevel implements IsSerializable {

		NONE((byte) 0), PIN((byte) 1);

		public static final byte NONE_CODE = 0;

		public static final byte PIN_CODE = 1;

		private final byte level;

		SecurityLevel(byte code) {
			this.level = code;
		}

		public byte getLevel() {
			return level;
		}

		public static SecurityLevel fromCode(int level) {
			switch (level) {
			case NONE_CODE:
				return NONE;
			case PIN_CODE:
				return PIN;
			default:
				return PIN;
			}
		}
	}
}
