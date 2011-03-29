package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServerAddPanel extends VerticalPanel {

	private static OSMessages msg = OneSwarmGWT.msg;

	public static final String DEFAULT_COMMUNITY_SERVER = "https://community.oneswarm.org/";
	public static final int DEFAULT_PRUNING_THRESHOLD = 50;

	private final TextBox urlTextBox = new TextBox();

	private final TextBox usernameTB = new TextBox();
	private final TextBox passwordTB = new PasswordTextBox();
	// private CheckBox savePWCheckbox = new CheckBox("Save password", true);
	// private CheckBox applyDeletes = new CheckBox("Synchronize removals",
	// true);

	private final RadioButton synchronizeDeletes = new RadioButton("removalGroup", msg.add_community_sync_removals());
	private final RadioButton localThreshold = new RadioButton("removalGroup", msg.add_community_remove_old_after());

	private final Label statusLabel = new Label();
	private final CheckBox authRequired;
	private final CheckBox confirmUpdates = new CheckBox(msg.add_community_confirm_updates_manually(), true);

	private final CheckBox defaultChat = new CheckBox(msg.add_community_chat_default());
	// private CheckBox defaultLimited = new
	// CheckBox(msg.add_community_limited_default());
	private final CheckBox skipSSL = new CheckBox(msg.add_community_skip_ssl());

	private final CheckBox acceptFilterList = new CheckBox(msg.add_community_accept_filter(), false);

	/** Checkbox indicating whether clients should publish port / address information. */
	private final CheckBox allowAddressResolution = new CheckBox(
			msg.add_community_allow_address_resolution(), true);

	private final TextBox targetGroupTB = new TextBox();

	private final TextBox thresholdCountTextBox;

	private final TextBox minimumRefreshTextBox;

	static final KeyboardListenerAdapter digitsOnly = new KeyboardListenerAdapter() {
		@Override
		public void onKeyPress(Widget sender, char keyCode, int modifiers) {
			if ((!Character.isDigit(keyCode)) && (keyCode != (char) KEY_TAB) && (keyCode != (char) KEY_BACKSPACE) && (keyCode != (char) KEY_DELETE) && (keyCode != (char) KEY_ENTER) && (keyCode != (char) KEY_HOME) && (keyCode != (char) KEY_END) && (keyCode != (char) KEY_LEFT) && (keyCode != (char) KEY_UP) && (keyCode != (char) KEY_RIGHT) && (keyCode != (char) KEY_DOWN)) {
				// TextBox.cancelKey() suppresses the current keyboard event.
				((TextBox) sender).cancelKey();
			}
		}
	};

	/**
	 * these are for consistency when we write back to the server for updates if
	 * the backend has determined that the server does support the
	 * publishing/name revision
	 */
	private String supports_publish = null;
	private String server_name = null;

	public static final int WIDTH = FriendsImportCommunityServer.WIDTH;

	public CommunityServerAddPanel() {
		this(DEFAULT_COMMUNITY_SERVER, "", "", "Community contacts", false, false, false, false,
				DEFAULT_PRUNING_THRESHOLD, null, null, false, true, true, 0, false, true);
	}

	public CommunityServerAddPanel(CommunityRecord rec) {
		this(rec.getRealUrl(), rec.getUsername(), rec.getPw(), rec.getGroup(), rec
				.isAuth_required(), rec.isConfirm_updates(), rec.isSavePW(), rec.isSync_deletes(),
				rec.getPruning_threshold(), rec.getSupports_publish(), rec.getServer_name(), rec
						.isChat_default(), rec.isLimited_default(), rec.getNonssl_port() >= 0, rec
						.getMinimum_refresh_interval(), rec.isAcceptFilterList(), rec
						.isAllowAddressResolution());
	}

	// TODO(piatek): Fix the disaster that is this constructor.
	public CommunityServerAddPanel(String inURL, String username, String password, String group,
			boolean needs_auth, boolean manual_update, boolean savePW, boolean server_sync_deletes,
			int localRemoveThreshold, String supports_publish, String server_name,
			boolean allowChatDefault, boolean limitedDefault, boolean skipSSLDefault,
			int minRefreshDefault, boolean acceptFilterDefault, boolean allowAddressResolveDefault) {

		this.supports_publish = supports_publish;
		this.server_name = server_name;

		Grid urlGrid = new Grid(2, 2);
		Label l = new Label(msg.add_community_site_url());
		urlGrid.setWidget(0, 0, l);
		l.setWidth("70px");
		urlGrid.setWidget(0, 1, urlTextBox);
		urlGrid.setWidget(1, 0, new Label(msg.friend_properties_group()));
		urlGrid.setWidget(1, 1, targetGroupTB);
		targetGroupTB.setText(group);
		urlTextBox.setText(inURL);
		urlTextBox.setWidth("310px");

		VerticalPanel disclosed = new VerticalPanel();

		HorizontalPanel checkButtonHP = new HorizontalPanel();
		checkButtonHP.setWidth("100%");

		confirmUpdates.setValue(manual_update);
		defaultChat.setValue(allowChatDefault);
		// defaultLimited.setValue(limitedDefault);
		skipSSL.setValue(skipSSLDefault);
		acceptFilterList.setValue(acceptFilterDefault);
		allowAddressResolution.setValue(allowAddressResolveDefault);


		l = new Label(msg.add_community_prompt());
		l.addStyleName(OneSwarmDialogBox.CSS_DIALOG_HEADER);
		l.setWidth(WIDTH + "px");
		this.add(l);
		this.add(urlGrid);

		final Grid grid = new Grid(2, 2);
		grid.setWidget(0, 0, new Label(msg.settings_net_remote_access_user()));
		grid.setWidget(1, 0, new Label(msg.settings_net_remote_access_password()));
		grid.setWidget(0, 1, usernameTB);
		grid.setWidget(1, 1, passwordTB);
		// grid.setWidget(2, 0, savePWCheckbox);

		usernameTB.setText(username);
		passwordTB.setText(password);

		// applyDeletes.setValue(sync_deletes);

		authRequired = new CheckBox(msg.add_community_auth_required());
		authRequired.setValue(needs_auth);
		checkButtonHP.add(authRequired);
		checkButtonHP.setCellVerticalAlignment(authRequired, VerticalPanel.ALIGN_MIDDLE);
		checkButtonHP.setCellHorizontalAlignment(authRequired, HorizontalPanel.ALIGN_LEFT);
		checkButtonHP.setSpacing(3);
		disclosed.add(checkButtonHP);
		authRequired.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (((CheckBox) event.getSource()).getValue()) {
					grid.setVisible(true);
				} else {
					grid.setVisible(false);
				}
			}
		});

		grid.setVisible(needs_auth);
		disclosed.add(grid);

		disclosed.add(packageOption(confirmUpdates, msg.add_community_confirm_updates_help(), 3));

		disclosed.add(packageOption(defaultChat, null, 3));

		disclosed.add(packageOption(acceptFilterList, msg.add_community_filter_help(), 3));

		disclosed.add(packageOption(allowAddressResolution, msg.add_community_resolver_help(), 3));

		// disclosed.add(packageOption(defaultLimited, null, 3));

		HorizontalPanel thresholdGroup = new HorizontalPanel();
		thresholdCountTextBox = new TextBox();
		thresholdCountTextBox.addKeyboardListener(digitsOnly);

		l = new Label("friends");
		thresholdCountTextBox.setText(localRemoveThreshold + "");
		thresholdCountTextBox.setWidth("25px");
		thresholdCountTextBox.setTextAlignment(TextBox.ALIGN_CENTER);
		thresholdGroup.add(localThreshold);
		localThreshold.setValue(!server_sync_deletes);
		Image img = new Image("images/spacer.png");
		img.setWidth("3px");
		img.setHeight("3px");
		thresholdGroup.add(img);
		thresholdGroup.add(thresholdCountTextBox);
		// img = new Image("images/spacer.png");
		// img.setWidth("3px");
		// img.setHeight("3px");
		// thresholdGroup.add(img);
		// thresholdGroup.add(l);
		// thresholdGroup.setCellVerticalAlignment(l,
		// VerticalPanel.ALIGN_MIDDLE);
		thresholdGroup.setCellVerticalAlignment(thresholdCountTextBox, VerticalPanel.ALIGN_MIDDLE);
		thresholdGroup.setCellVerticalAlignment(localThreshold, VerticalPanel.ALIGN_MIDDLE);
		disclosed.add(packageOption(thresholdGroup, msg.add_community_remove_old_help(), 3));

		disclosed.add(packageOption(synchronizeDeletes, msg.add_community_sync_removals_help(), 3));
		synchronizeDeletes.setValue(server_sync_deletes);

		HorizontalPanel intervalGroup = new HorizontalPanel();
		l = new Label(msg.add_community_minimum_refresh() + ":");
		intervalGroup.add(l);
		intervalGroup.setCellVerticalAlignment(l, ALIGN_MIDDLE);
		minimumRefreshTextBox = new TextBox();

		minimumRefreshTextBox.setText(minRefreshDefault + "");

		minimumRefreshTextBox.addKeyboardListener(digitsOnly);
		intervalGroup.add(minimumRefreshTextBox);
		intervalGroup.setSpacing(3);
		minimumRefreshTextBox.setWidth("50px");
		disclosed.add(intervalGroup);

		disclosed.add(skipSSL);

		statusLabel.setVisible(false);
		disclosed.add(statusLabel);

		DisclosurePanel dp = new DisclosurePanel(msg.button_advanced());
		dp.add(disclosed);

		if (needs_auth) {
			dp.setOpen(true);
		}

		this.add(dp);
	}

	private HorizontalPanel packageOption(Widget widget, String helpString, int spacing) {
		HorizontalPanel dummy = new HorizontalPanel();
		dummy.add(widget);
		if (helpString != null) {
			HelpButton hb = new HelpButton(helpString);
			dummy.add(hb);
			dummy.setCellHorizontalAlignment(hb, HorizontalPanel.ALIGN_RIGHT);
			dummy.setCellVerticalAlignment(hb, HorizontalPanel.ALIGN_MIDDLE);
		}
		dummy.setSpacing(spacing);
		dummy.setWidth("100%");
		return dummy;
	}

	// public static void appendToSettings(final String url, final String
	// username, final String pw, final boolean need_auth, final String group,
	// final boolean savePW, final boolean auth_required, final boolean
	// confirm_updates, final boolean syncDeletes) {
	// OneSwarmRPCClient.getService().getStringListParameterValue(OneSwarmRPCClient.getSessionID(),
	// "oneswarm.community.servers", new AsyncCallback<List<String>>(){
	// public void onFailure(Throwable caught) {
	// caught.printStackTrace();
	// // statusLabel.setText("Error: " + caught.toString());
	// }
	//
	// public void onSuccess(final List<String> result1) {
	//
	// List<String> converted = new ArrayList<String>();
	// for (String p : result1) {
	// converted.add(p);
	// }
	// /**
	// * Format is: url, username, pw, group, storepw;authrequired;manualupdate
	// */
	// converted.add(url);
	// converted.add(username);
	// converted.add(pw);
	// converted.add(group);
	// converted.add(savePW+";"+auth_required+";"+confirm_updates+";"+syncDeletes);
	//
	// OneSwarmRPCClient.getService().setStringListParameterValue(OneSwarmRPCClient.getSessionID(),
	// "oneswarm.community.servers", converted, new AsyncCallback<Void>(){
	// public void onFailure(Throwable caught) {
	// caught.printStackTrace();
	// // statusLabel.setText("Error: " + caught.toString());
	// }
	//
	// public void onSuccess(Void f) {
	// // statusLabel.setText("Done.");
	// }});
	// }});
	// }

	public String getURL() {
		return urlTextBox.getText();
	}

	public String getUsername() {
		return usernameTB.getText();
	}

	public String getPW() {
		return passwordTB.getText();
	}

	public String getGroup() {
		return targetGroupTB.getText();
	}

	public boolean getAuthRequired() {
		return authRequired.getValue();
	}

	public boolean getSavePW() {
		// return savePWCheckbox.getValue();
		return true;
	}

	public boolean getConfirmUpdates() {
		return confirmUpdates.getValue();
	}

	public void disableStuff() {
		// savePWCheckbox.setEnabled(false);
		confirmUpdates.setEnabled(false);
		authRequired.setEnabled(false);
		targetGroupTB.setEnabled(false);
		passwordTB.setEnabled(false);
		usernameTB.setEnabled(false);
		urlTextBox.setEnabled(false);
		synchronizeDeletes.setEnabled(false);
		localThreshold.setEnabled(false);
		acceptFilterList.setEnabled(false);
		defaultChat.setEnabled(false);
		allowAddressResolution.setEnabled(true);
	}

	public boolean getSyncDeletes() {
		return synchronizeDeletes.getValue();
	}

	public int getPruningThreshold() {
		int out = Integer.parseInt(thresholdCountTextBox.getText());
		if (out < 0) {
			out = 0;
		}
		return out;
	}

	public String getSupportsPublish() {
		return supports_publish;
	}

	public String getServerName() {
		return server_name;
	}

	public boolean isChatDefault() {
		return defaultChat.getValue();
	}

	public boolean isLimitedDefault() {
		// return defaultLimited.getValue();
		return true;
	}

	public boolean getUseNonSSL() {
		return skipSSL.getValue();
	}

	public int getMinimumRefreshInterval() {
		if (minimumRefreshTextBox.getText().length() > 0) {
			try {
				return Integer.parseInt(minimumRefreshTextBox.getText());
			} catch (Exception e) {
			}
		}
		return 0;
	}

	public boolean getAcceptFilterList() {
		return acceptFilterList.getValue();
	}

	public boolean isAllowAddressResolution() {
		return allowAddressResolution.getValue();
	}
}
