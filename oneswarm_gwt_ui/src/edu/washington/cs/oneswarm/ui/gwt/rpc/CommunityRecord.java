package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.List;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.IsSerializable;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.CommunityServerAddPanel;

public class CommunityRecord implements IsSerializable {
	private String group;
	private String pw;
	private String username;
	private String url;
	private boolean auth_required;
	private boolean savePW;
	private boolean confirm_updates;
	private boolean sync_deletes;
	private boolean accept_filter_list;
	private int pruning_threshold;
	private String supports_publish;
	private String server_name;
	private String community_path;
	private String splash_path;

	/**
	 * the semantics of nonssl_port are clunky. -1 -> don't use nonSSL, ever. 0
	 * -> use it, but we don't know it yet. >0 -> the value is the actual port
	 */
	private int nonssl_port = 0;

	private int minimum_refresh_interval;

	private boolean chat_default;

	public CommunityRecord() {
	}

	public int getNonssl_port() {
		return nonssl_port;
	}

	public void setNonssl_port(int nonsslPort) {
		nonssl_port = nonsslPort;
	}

	public int getMinimum_refresh_interval() {
		return minimum_refresh_interval;
	}

	public void setMinimum_refresh_interval(int minimumRefreshInterval) {
		minimum_refresh_interval = minimumRefreshInterval;
	}

	public boolean isChat_default() {
		return chat_default;
	}

	public void setChat_default(boolean chatDefault) {
		chat_default = chatDefault;
	}

	public boolean isLimited_default() {
		return true;
	}

	// public void setLimited_default(boolean limitedDefault) {
	// //limited_default = limitedDefault;
	// }

	public String getCommunity_path() {
		return community_path;
	}

	public String getSplash_path() {
		return splash_path;
	}

	public void setCommunity_path(String communityPath) {
		community_path = communityPath;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getPw() {
		return pw;
	}

	public void setPw(String pw) {
		this.pw = pw;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getRealUrl() {
		return url;
	}

	public String getUrl() {

		if (this.getCommunity_path() != null) {
			if (url.contains("/" + getCommunity_path())) {
				return url;
			} else {
				return url + (url.endsWith("/") == false ? "/" + getCommunity_path() : getCommunity_path());
			}
		}

		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isAuth_required() {
		return auth_required;
	}

	public void setAuth_required(boolean authRequired) {
		auth_required = authRequired;
	}

	public boolean isSavePW() {
		return savePW;
	}

	public void setSavePW(boolean savePW) {
		this.savePW = savePW;
	}

	public boolean isConfirm_updates() {
		return confirm_updates;
	}

	public void setConfirm_updates(boolean confirmUpdates) {
		confirm_updates = confirmUpdates;
	}

	public boolean isSync_deletes() {
		return sync_deletes;
	}

	public void setSync_deletes(boolean syncDeletes) {
		sync_deletes = syncDeletes;
	}

	public int getPruning_threshold() {
		return pruning_threshold;
	}

	public void setPruning_threshold(int pruningThreshold) {
		pruning_threshold = pruningThreshold;
	}

	public String getSupports_publish() {
		return supports_publish;
	}

	public void setSupports_publish(String supportsPublish) {
		supports_publish = supportsPublish;
	}

	public String getServer_name() {
		return server_name;
	}

	public void setServer_name(String serverName) {
		server_name = serverName;
	}

	public CommunityRecord(String historyString) {

		OneSwarmGWT.log("historyString: " + historyString);
		String[] toks = historyString.split(":");

		OneSwarmGWT.log("got " + toks.length);
		for (String s : toks) {
			OneSwarmGWT.log("tok: " + s);
		}

		try {
			this.url = URL.decodeComponent(toks[0]);

			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			this.group = URL.decodeComponent(toks[1]);
			this.auth_required = Boolean.parseBoolean(toks[2]);
			this.confirm_updates = Boolean.parseBoolean(toks[3]);
			this.sync_deletes = Boolean.parseBoolean(toks[4]);
			this.pruning_threshold = Integer.parseInt(toks[5]);
			this.supports_publish = URL.decodeComponent(toks[6]);
			this.server_name = URL.decodeComponent(toks[7]);
			this.community_path = URL.decodeComponent(toks[8]);
			this.chat_default = Boolean.parseBoolean(toks[9]);

		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	public CommunityRecord(String group, String pw, String username, String url, boolean auth_required, boolean savePW, boolean confirm_updates, boolean sync_deletes, int pruning_threshold, String supports_publish, String server_name, String community_path, String splash_path, boolean defaultChat, int minRefreshInterval, boolean acceptFilterList) {
		this.group = group;
		this.pw = pw;
		this.username = username;
		this.url = url;
		this.auth_required = auth_required;
		this.savePW = savePW;
		this.confirm_updates = confirm_updates;
		this.sync_deletes = sync_deletes;
		this.pruning_threshold = pruning_threshold;
		this.supports_publish = supports_publish;
		this.server_name = server_name;
		this.community_path = community_path;
		this.splash_path = splash_path;
		this.chat_default = defaultChat;
		this.minimum_refresh_interval = minRefreshInterval;
		this.accept_filter_list = acceptFilterList;
	}

	public CommunityRecord(List<String> toks, int offset) {
		url = toks.get(offset + 0);
		username = toks.get(offset + 1);
		pw = toks.get(offset + 2);
		group = toks.get(offset + 3);
		String[] extras = toks.get(offset + 4).split(";");
		savePW = Boolean.parseBoolean(extras[0]);
		auth_required = Boolean.parseBoolean(extras[1]);
		confirm_updates = Boolean.parseBoolean(extras[2]);
		if (extras.length > 3) {
			sync_deletes = Boolean.parseBoolean(extras[3]);
		} else {
			sync_deletes = false;
		}

		if (extras.length > 4) {
			pruning_threshold = Integer.parseInt(extras[4]);
		} else {
			pruning_threshold = CommunityServerAddPanel.DEFAULT_PRUNING_THRESHOLD;
		}

		if (extras.length > 5 && extras[5].equals("null") == false) {
			supports_publish = extras[5];
		} else {
			supports_publish = null;
		}

		if (extras.length > 6 && extras[6].equals("null") == false) {
			server_name = extras[6];
		} else {
			server_name = null;
		}

		if (extras.length > 7 && extras[7].equals("null") == false) {
			community_path = extras[7];
		} else {
			community_path = null;
		}

		if (extras.length > 8 && extras[8].equals("null") == false) {
			splash_path = extras[8];
		} else {
			splash_path = null;
		}

		if (extras.length > 9) {
			chat_default = Boolean.parseBoolean(extras[9]);
		} else {
			chat_default = false;
		}

		if (extras.length > 10) {
			try {
				nonssl_port = Integer.parseInt(extras[10]);
			} catch (NumberFormatException e) {
				nonssl_port = 0;
			}
		} else {
			nonssl_port = 0;
		}

		if (extras.length > 11) {
			minimum_refresh_interval = Integer.parseInt(extras[11]);
		} else {
			minimum_refresh_interval = 0;
		}

		if (extras.length > 12) {
			accept_filter_list = Boolean.parseBoolean(extras[12]);
		} else {
			accept_filter_list = false;
		}
	}

	public CommunityRecord(CommunityServerAddPanel p) {
		url = p.getURL();
		username = p.getUsername();
		pw = p.getPW();
		group = p.getGroup();
		auth_required = p.getAuthRequired();
		savePW = p.getSavePW();
		confirm_updates = p.getConfirmUpdates();
		sync_deletes = p.getSyncDeletes();
		pruning_threshold = p.getPruningThreshold();
		supports_publish = p.getSupportsPublish();
		server_name = p.getServerName();
		chat_default = p.isChatDefault();
		if (nonssl_port < 0 && p.getUseNonSSL()) {
			nonssl_port = 0;
		} else if (p.getUseNonSSL() == false) {
			nonssl_port = -1;
		}
		minimum_refresh_interval = p.getMinimumRefreshInterval();
		accept_filter_list = p.getAcceptFilterList();
	}

	public String[] toTokens() {
		return new String[] { url, username, pw, group, savePW + ";" + auth_required + ";" + confirm_updates + ";" + sync_deletes + ";" + pruning_threshold + ";" + supports_publish + ";" + server_name + ";" + community_path + ";" + splash_path + ";" + chat_default + ";" + nonssl_port + ";" + minimum_refresh_interval + ";" + accept_filter_list };
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		for (String s : toTokens()) {
			b.append(s + " :: ");
		}
		return b.toString();
	}

	public void setSplash_path(String splash_path) {
		this.splash_path = splash_path;
	}

	public String getBaseURL() {
		return getBaseURL(getUrl());
	}

	public static String getBaseURL(String url) {
		String nossl = "http://", ssl = "https://";
		String proto = "";
		if (url.startsWith(nossl)) {
			url = url.substring(nossl.length());
			proto = nossl;
		} else if (url.startsWith(ssl)) {
			url = url.substring(ssl.length());
			proto = ssl;
		}

		if (url.indexOf('/') != -1) {
			return proto + url.substring(0, url.indexOf('/'));
		} else {
			return proto + url;
		}
	}

	public String getBaseURLSkipSSL() {
		String base = getBaseURL();

		if (base.startsWith("http:")) {
			return base;
		}

		if (!base.startsWith("https:")) {
			System.err.println("Strange URL: " + base);
			return base;
		}

		// if == 0 we don't know it yet, so might as well stick to base.
		if (getNonssl_port() <= 0) {
			return base;
		}

		base = base.substring("https://".length());

		/**
		 * Now just need to fix up the port. Two cases: the SSL was on the
		 * default port or not
		 */
		if (base.indexOf(":") == -1) {
			return "http://" + base + ":" + getNonssl_port();
		} else {
			return "http://" + base.substring(0, base.indexOf(":")) + ":" + getNonssl_port();
		}
	}

	public boolean isAcceptFilterList() {
		return accept_filter_list;
	}

	public void setAcceptFilterList(boolean acceptFilterList) {
		accept_filter_list = acceptFilterList;
	}

}
