package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

class PrivacySettingsPanel extends SettingsPanel {

	private final List<SettingsCheckBox> checkBoxes = new ArrayList<SettingsCheckBox>();

	ClickHandler advancedCL = new ClickHandler() {
		public void onClick(ClickEvent event) {
			VersionCheckAdvancedDialog dlg = new VersionCheckAdvancedDialog();
			dlg.show();
			dlg.setVisible(false);
			dlg.center();
			dlg.setPopupPosition(dlg.getPopupLeft(), dlg.getPopupTop() + 20);
			dlg.setVisible(true);
		}
	};

	PrivacySettingsPanel() {
		loadNotify();
		
		final SettingsCheckBox autoUpdateCheckBox = new SettingsCheckBox(msg.settings_interface_privacy_updates_check(), new String[] { "update.periodic", "update.start" });
		checkBoxes.add(autoUpdateCheckBox);

		final SettingsCheckBox autoUpdateInstallCheckBox = new SettingsCheckBox(msg.settings_interface_privacy_updates_install(), "update.autodownload");
		checkBoxes.add(autoUpdateInstallCheckBox);

		final SettingsCheckBox autoUpdateStatsCheckBox = new SettingsCheckBox(msg.settings_interface_privacy_updates_stats(), "Send Version Info");
		checkBoxes.add(autoUpdateStatsCheckBox);

		final SettingsCheckBox newFriendNotification = new SettingsCheckBox(msg.settings_interface_privacy_friends_check(), "OSF2F.FriendNotifications");
		checkBoxes.add(newFriendNotification);

		final SettingsCheckBox lanFriendFinder = new SettingsCheckBox(msg.settings_interface_privacy_friends_lan(), "OSF2F.LanFriendFinder");
		checkBoxes.add(lanFriendFinder);

		final SettingsCheckBox dhtCheckbox = new SettingsCheckBox(msg.settings_interface_privacy_dht(), "dht.enabled");
		checkBoxes.add(dhtCheckbox);

		final SettingsCheckBox dhtProxyCheckbox = new SettingsCheckBox(msg.settings_interface_privacy_dht_proxy(), "OSF2F.Use DHT Proxy");
		checkBoxes.add(dhtProxyCheckbox);

		final SettingsCheckBox natCheckCheckbox = new SettingsCheckBox(msg.settings_interface_privacy_nat(), "Perform.NAT.Check");
		checkBoxes.add(natCheckCheckbox);

		final SettingsCheckBox silentURLsCheckbox = new SettingsCheckBox(msg.settings_interface_privacy_silent_url(), "Add URL Silently");
		checkBoxes.add(silentURLsCheckbox);

		// final SettingsCheckBox speedCheckCheckbox = new
		// SettingsCheckBox("Allow incoming speed checks",
		// "Allow.Incoming.Speed.Check");
		// checkBoxes.add(speedCheckCheckbox);

		Grid g = new Grid(checkBoxes.size(), 1);

		for (int row = 0; row < checkBoxes.size(); row++) {
			if (row == 0) {
				HorizontalPanel p = new HorizontalPanel();
				p.add(checkBoxes.get(row));
				Button advancedButton = new Button("Advanced");
				advancedButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
				advancedButton.addClickHandler(advancedCL);
				SimplePanel sp = new SimplePanel();
				sp.setWidth("5px");
				p.add(sp);
				p.add(advancedButton);
				g.setWidget(row, 0, p);
			} else {
				g.setWidget(row, 0, checkBoxes.get(row));
			}
		}

		super.add(g);
	}

	public void sync() {
		for (SettingsCheckBox checkBox : checkBoxes) {
			checkBox.save();
		}
	}

	@Override
	String validData() {
		return null;
	}
}
