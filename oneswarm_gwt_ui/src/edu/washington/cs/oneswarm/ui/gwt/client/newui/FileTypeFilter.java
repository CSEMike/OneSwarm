package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.HashSet;
import java.util.Set;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public enum FileTypeFilter {

	All("filter_all", ImageConstants.ICON_DOCUMENT_SIDEBAR, ImageConstants.ICON_DOCUMENT_CENTER),

	Videos("filter_videos", ImageConstants.ICON_VIDEO_SIDEBAR, ImageConstants.ICON_VIDEO_BROWSER, "asf", "avi", "mov", "mp4", "flv", "mpg", "m4v", "wmv", "divx", "xvid", "mpeg", "mkv", "ogv", "ac3", "aac"),

	Audio("filter_audio", ImageConstants.ICON_AUDIO_SIDEBAR, ImageConstants.ICON_AUDIO_BROWSER, "mp3", "aac", "wav", "aiff", "aif", "wma", "m4a", "oga", "ogg"),

	Other("filter_other", ImageConstants.ICON_OTHER_SIDEBAR, ImageConstants.ICON_OTHER_BROWSER); // show
	// only
	// things
	// which
	// CANNOT
	// be
	// identified

	public final String sidebar_icon_path;
	private final String browser_icon_path;

	public Set<String> mTypes;

	public String history_state_name;

	FileTypeFilter(String inHistoryStateName, String inIconPath, String inBrowserIconPath, String... inTypes) {
		// mTypes = inTypes;
		mTypes = new HashSet<String>();
		for (String type : inTypes) {
			mTypes.add(type);
		}

		history_state_name = inHistoryStateName;
		sidebar_icon_path = inIconPath;
		browser_icon_path = inIconPath;

	}

	public String getUiLabel() {
		final OSMessages msg = OneSwarmGWT.msg;
		switch (this) {
		case All:
			return msg.navigation_allFiles();
		case Videos:
			return msg.navigation_videos();
		case Audio:
			return msg.navigation_audio();
		case Other:
			return msg.navigation_other();
		}

		return "unspecified: " + this.name();
	}

	public String getBrowserIconPath() {
		return browser_icon_path;
	}

	public static boolean match(String inFilename, FileTypeFilter which) {

		String ext = getExtension(inFilename);
		return which.mTypes.contains(ext);

	}

	private static String getExtension(String inFilename) {
		/**
		 * Get the extension
		 */
		StringBuffer extension = new StringBuffer(5);
		for (int i = inFilename.length() - 1; i >= 0; i--) {
			if (inFilename.charAt(i) == '.') {
				break;
			}
			extension.insert(0, inFilename.charAt(i));
		}
		return extension.toString();
	}

	public static FileTypeFilter match(String inFilename) {
		String ext = getExtension(inFilename);
		for (FileTypeFilter candidate : values()) {
			if (candidate.mTypes.contains(ext)) {
				return candidate;
			}
		}
		return Other;
	}

	public static FileTypeFilter getFromName(String name) {
		for (FileTypeFilter f : FileTypeFilter.values()) {
			if (f.name().equals(name)) {
				return f;
			}
		}
		return FileTypeFilter.All;
	}
}
