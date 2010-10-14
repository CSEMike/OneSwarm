package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.HashMap;
import java.util.Map;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class Strings {
	/*
	 * TODO: proper i18n. replace this (and all other strings) with
	 * ResourceBundle.
	 */

	private static Strings inst = null;
	private static OSMessages msg = OneSwarmGWT.msg;

	public static final String CREATE_SWARM = "create_swarm";

	public static final String SETTINGS = "settings";

	public static final String ADD_SWARM_URL = "add_swarm_url";
	public static final String ADD_SWARM_FILE = "add_swarm_file";

	public static final String SWARM_BROWSER_PLAY = "swarm_browserplay";
	public static final String SWARM_DEFAULT_PLAY = "swarm_defaultplay";
	public static final String SWARM_REVEAL = "swarm_reveal";
	public static final String SWARM_DELETE = "swarm_delete";
	public static final String SWARM_TAGS = "swarm_tags";
	public static final String SWARM_DETAILS = "swarm_details";
	public static final String SWARM_COPY_MAGNET = "swarm_copy_magnet";
	public static final String SWARM_PUBLISH = "swarm_publish";

	public static final String MANAGE_PERMS = "manage_perms";

	public static final String DEBUG = "debug";

	public static final String SWARM_STARTSTOP = "swarm_startstop";

	public static final String[] SWARM_BUTTON_ACTIONS = new String[] { ADD_SWARM_URL, SWARM_BROWSER_PLAY, SWARM_DELETE };
	public static final String[] SWARM_MORE_ACTIONS_SELECTED = new String[] { SWARM_STARTSTOP, SWARM_DETAILS, SWARM_DEFAULT_PLAY, SWARM_COPY_MAGNET, SWARM_PUBLISH, SWARM_REVEAL };
	public static final String[] SWARM_MORE_ACTIONS_NOSELECTION = new String[] { ADD_SWARM_FILE, MANAGE_PERMS };

	public static final String SIDEBAR_DL_RATE = "sidebar_dl";
	public static final String SIDEBAR_UL_RATE = "sidebar_ul";
	public static final String SIDEBAR_COUNT = "sidebar_count";
	public static final String SIDEBAR_DAYCOUNT = "sidebar_daycount";
	public static final String SIDEBAR_WEEKCOUNT = "sidebar_weekcount";
	public static final String SIDEBAR_MONTHCOUNT = "sidebar_monthcount";
	public static final String SIDEBAR_YEARCOUNT = "sidebar_yearcount";
	public static final String SIDEBAR_DAILYLIMIT = "sidebar_dailylimit";
	public static final String SIDEBAR_WEEKLYLIMIT = "sidebar_weeklylimit";
	public static final String SIDEBAR_MONTHLYLIMIT = "sidebar_monthlylimit";
	public static final String SIDEBAR_YEARLYLIMIT = "sidebar_yearlylimit";
	public static final String SIDEBAR_TOTAL = "sidebar_total";
	public static final String LIST_VIEW = "list_view";
	public static final String ICON_VIEW = "icon_view";

	// dialogs

	// Delete swarm
	public static final String DELETE_DIALOG_SELECT_PROMPT = "deletedialog_prompt";
	public static final String DELETE_DIALOG_SWARM_ONLY = "deletedialog_swarmonly";
	public static final String DELETE_DIALOG_ALL = "deletedialog_all";

	// create progress
	public static final String CREATE_PROGRESS_OVERALL = "createstatusdialog_overall";
	public static final String CREATE_PROGRESS_INDIVIDUAL = "createstatusdialog_individual";

	// f2f download results
	public static final String F2F_RESULTS_NAME = msg.search_result_filename();
	public static final String F2F_RESULTS_SIZE = msg.search_result_size();
	public static final String F2F_RESULTS_COLLECTION = msg.search_result_collection();
	public static final String F2F_RESULTS_SOURCES = msg.search_result_paths();
	public static final String F2F_RESULTS_DELAY = msg.search_result_delay();

	public static final String[] F2F_RESULTS_COLS = new String[] { F2F_RESULTS_NAME, F2F_RESULTS_SIZE, F2F_RESULTS_COLLECTION, F2F_RESULTS_SOURCES /*, F2F_RESULTS_DELAY, /*"Hash"*/ };

	// public transfer details
	public static final String XFER_NAME = msg.transfers_public_name();
	public static final String XFER_PEERS = msg.transfers_public_peers();
	public static final String XFER_SEEDS = msg.transfers_public_seeds();
	public static final String XFER_PROGRESS = msg.transfers_public_progress();
	public static final String XFER_SIZE = msg.transfers_public_size();
	public static final String XFER_UPLOAD_RATE = msg.transfers_public_upload_rate();
	public static final String XFER_DOWNLOAD_RATE = msg.transfers_public_download_rate();
	public static final String XFER_REMAINING = msg.transfers_public_remaining();

	public static final String[] TRANSFER_DETAILS_COLUMNS = new String[] { XFER_NAME, XFER_SEEDS, XFER_PEERS, XFER_PROGRESS, XFER_SIZE, XFER_UPLOAD_RATE, XFER_DOWNLOAD_RATE, XFER_REMAINING };

	// when public transfer details are in F2F mode
	public static final String XFER_F2F_PEERS = msg.transfers_f2f_peer_channels();
	public static final String XFER_F2F_SEEDS = msg.transfers_f2f_seed_channels();

	public static final String[] TRANSFER_F2F_DETAILS_COLUMNS = new String[] { XFER_NAME, XFER_F2F_SEEDS, XFER_F2F_PEERS, XFER_PROGRESS, XFER_SIZE, XFER_UPLOAD_RATE, XFER_DOWNLOAD_RATE, XFER_REMAINING };

	// Friend forwarding details
	public static final String F2F_XFER_CONTENT = msg.transfers_forwarding_content();
	public static final String F2F_XFER_FROM = msg.transfers_forwarding_from();
	public static final String F2F_XFER_TO = msg.transfers_forwarding_to();
	public static final String F2F_XFER_RATE = msg.transfers_forwarding_rate();
	public static final String F2F_XFER_TOTAL = msg.transfers_forwarding_total();
	public static final String F2F_XFER_CHANNEL_ID = msg.transfers_forwarding_channel_id();

	public static final String[] F2F_DETAILS_COLUMNS = new String[] { F2F_XFER_CHANNEL_ID, F2F_XFER_CONTENT, F2F_XFER_FROM, F2F_XFER_TO, F2F_XFER_RATE, F2F_XFER_TOTAL };

	public static final String SEEDING_STATUS_LABEL = "transfertable_seeding_label";

	public static final String NO_FILES_MESSAGE = "text_nofiles";
	public static final String WELCOME_MESSAGE = "text_welcome";

	public static final String SIDEBAR_REMOTE = "remote_rate";
	public static final String SIDEBAR_REMOTE_IPS = "remote_ips";

	public static final String NO_FRIENDS_MSG = "no_friends";

	private Strings() {
		map.put(NO_FILES_MESSAGE, msg.swarm_browser_no_files_of_type_HTML());
		map.put(WELCOME_MESSAGE, msg.swarm_browser_welcome_message_HTML());
		map.put(NO_FRIENDS_MSG, msg.swarm_browser_no_friends_HTML());

		map.put(LIST_VIEW, msg.swarm_browser_view_as_list());
		map.put(ICON_VIEW, msg.swarm_browser_view_previews());

		// swarm actions
		map.put(SWARM_BROWSER_PLAY, msg.swarm_browser_button_play());
		map.put(SWARM_DEFAULT_PLAY, msg.swarm_browser_more_actions_default_app());
		map.put(SWARM_REVEAL, msg.swarm_browser_more_actions_reveal());
		map.put(SWARM_DELETE, msg.swarm_browser_button_delete());
		map.put(SWARM_TAGS, msg.swarm_browser_button_tags());
		map.put(SWARM_DETAILS, msg.swarm_browser_more_actions_swarm_details());
		map.put(SWARM_COPY_MAGNET, msg.swarm_browser_more_actions_copy_magnet());
		map.put(SWARM_PUBLISH, msg.swarm_browser_more_actions_publish());

		map.put(ADD_SWARM_URL, msg.swarm_browser_button_addUrl());
		map.put(ADD_SWARM_FILE, msg.swarm_browser_more_actions_load_torrent_file());

		map.put(DEBUG, "Debugging...");

		map.put(CREATE_SWARM, msg.swarm_browser_button_share());

		map.put(MANAGE_PERMS, msg.swarm_browser_more_actions_manage_visibility());

		map.put(SETTINGS, msg.swarm_browser_button_setting());

		// dialogs

		// delete swarm
		map.put(DELETE_DIALOG_SELECT_PROMPT, msg.swarm_browser_delete_prompt());
		map.put(DELETE_DIALOG_SWARM_ONLY, msg.swarm_browser_delete_swarm_only());
		map.put(DELETE_DIALOG_ALL, msg.swarm_browser_delete_all());

		// create progress
		map.put(CREATE_PROGRESS_OVERALL, msg.create_swarm_progress_overall());
		map.put(CREATE_PROGRESS_INDIVIDUAL, msg.create_swarm_progress_individual());

		map.put(SEEDING_STATUS_LABEL, "Complete (seeding)");
	}

	public static String get(String inString) {
		if (inst == null)
			inst = new Strings();
		return map.get(inString);
	}

	private static Map<String, String> map = new HashMap<String, String>();

}
