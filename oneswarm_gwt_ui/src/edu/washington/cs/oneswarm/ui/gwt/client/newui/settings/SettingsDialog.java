package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser;

public class SettingsDialog extends OneSwarmDialogBox {
	public static final String CSS_HRULE = "os-hrule";

	private static final int WIDTH = 450;
	private static final int HEIGHT = 200;

	List<SettingsPanel> settingsPanels = new ArrayList<SettingsPanel>();

	boolean cancel = false;

	Button refreshAssociationsButton = new Button(msg.button_refresh_associations());

	private SwarmsBrowser mSwarmsBrowser;

	DecoratedTabPanel mTabs;

	private int mInitialSelectedTab;

	public SettingsDialog(EntireUIRoot inRoot, SwarmsBrowser swarmsBrowser, int selectedTab) {
		super(false, true, true);

		mInitialSelectedTab = selectedTab;
		mSwarmsBrowser = swarmsBrowser;

		GWT.runAsync(new RunAsyncCallback() {
			public void onFailure(Throwable caught) {
				Window.alert("Error loading settings: " + caught.toString());
			}

			public void onSuccess() {
				// indicates prefetch
				if( mInitialSelectedTab < 0 ) {
					return;
				}
				
				SettingsDialog dlg = onInitialize();
				dlg.show();
				dlg.setVisible(false);
				dlg.center();
				dlg.setPopupPosition(dlg.getAbsoluteLeft(), 300);
				dlg.setVisible(true);
			}
		});
	}

	// for code splitting support
	public SettingsDialog onInitialize() {

		PortsSettingsPanel ports_settings = new PortsSettingsPanel();
		SaveLocationPanel save_loc = new SaveLocationPanel();
		SpeedLimitsPanel speed_limits = new SpeedLimitsPanel();
		MagicDirectorySettingsPanel magic_dirs = new MagicDirectorySettingsPanel();
		UISettingsPanel ui_settings = new UISettingsPanel();
		TagsSettingsPanel tags_settings = new TagsSettingsPanel();

		PrivacySettingsPanel privacySettingsPanel = new PrivacySettingsPanel();
		RemoteAccessPanel remoteAccessPanel = new RemoteAccessPanel();
		Sha1Ed2kSettingsPanel sha1HashSpeedPanel = new Sha1Ed2kSettingsPanel();

		// DataUsage dataUsagePanel = new DataUsage();
		CommunityServersSettingsPanel communityPanel = new CommunityServersSettingsPanel();
		KeywordFilterSettingsPanel filterPanel = new KeywordFilterSettingsPanel();

		Label selectLabel = new Label(msg.settings_dialog_msg());
		if (OneSwarmGWT.isRemoteAccess()) {
			selectLabel.setText(msg.settings_dialog_msg_remote_access());
		}

		mTabs = new DecoratedTabPanel();
		mTabs.addStyleName(TorrentDownloaderDialog.CSS_F2F_TABS);
		final VerticalPanel filesTab = new VerticalPanel();
		// windows
		OneSwarmRPCClient.getService().getPlatform(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(String result) {
				if (result.equals("windows")) {
					refreshAssociationsButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
					refreshAssociationsButton.addClickHandler(new ClickHandler() {
						public void onClick(ClickEvent event) {
							OneSwarmRPCClient.getService().refreshFileAssociations(OneSwarmRPCClient.getSessionID(), new AsyncCallback<Void>() {
								public void onFailure(Throwable caught) {
									caught.printStackTrace();
								}

								public void onSuccess(Void result) {
									System.out.println("refresh file associations: success");
								}
							});
						}
					});
					filesTab.add(refreshAssociationsButton);
				}
			}
		});
		filesTab.add(save_loc);
		filesTab.add(magic_dirs);
		filesTab.add(sha1HashSpeedPanel);
		mTabs.add(filesTab, msg.settings_tab_files());

		VerticalPanel networkTab = new VerticalPanel();
		networkTab.add(speed_limits);
		networkTab.add(ports_settings);

		DisclosurePanel communityDisclosurePanel = new DisclosurePanel(msg.settings_net_community_servers(), true);
		communityDisclosurePanel.add(communityPanel);
		networkTab.add(communityDisclosurePanel);

		DisclosurePanel filterDisclosurePanel = new DisclosurePanel(msg.settings_net_search_filter(), false);
		filterDisclosurePanel.add(filterPanel);
		networkTab.add(filterDisclosurePanel);

		if (OneSwarmGWT.isRemoteAccess() == false) {
			DisclosurePanel remoteAccessDisclosurePanel = new DisclosurePanel(msg.settings_net_remote_access(), true);
			remoteAccessDisclosurePanel.add(remoteAccessPanel);
			networkTab.add(remoteAccessDisclosurePanel);
		}

		mTabs.add(networkTab, msg.settings_tab_network());

		VerticalPanel uiTab = new VerticalPanel();
		uiTab.add(ui_settings);
		DisclosurePanel tagsDisclosurePanel = new DisclosurePanel(msg.settings_interface_tags(), true);
		tagsDisclosurePanel.add(tags_settings);
		uiTab.add(tagsDisclosurePanel);
		mTabs.add(uiTab, msg.settings_tab_interface());

		VerticalPanel privacyTab = new VerticalPanel();
		privacyTab.add(privacySettingsPanel);
		mTabs.add(privacyTab, msg.settings_tab_privacy());

		mTabs.selectTab(mInitialSelectedTab);
		mTabs.setWidth("100%");
		mTabs.setHeight("100%");

		selectLabel.addStyleName(CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");

		settingsPanels.add(save_loc);
		settingsPanels.add(magic_dirs);
		settingsPanels.add(sha1HashSpeedPanel);
		settingsPanels.add(speed_limits);
		settingsPanels.add(ports_settings);
		settingsPanels.add(ui_settings);
		settingsPanels.add(tags_settings);
		settingsPanels.add(privacySettingsPanel);
		if (OneSwarmGWT.isRemoteAccess() == false) {
			settingsPanels.add(remoteAccessPanel);
		}
		// settingsPanels.add(dataUsagePanel);
		settingsPanels.add(communityPanel);
		settingsPanels.add(filterPanel);

		setText(msg.settings_dialog_header());

		// mUIRoot = inRoot;

		VerticalPanel main_vp = new VerticalPanel();

		main_vp.add(selectLabel);
		main_vp.add(mTabs);
		main_vp.setCellVerticalAlignment(mTabs, VerticalPanel.ALIGN_TOP);

		HorizontalPanel revert_defaults_hp = new HorizontalPanel();
		HorizontalPanel lhs = new HorizontalPanel();
		lhs.setSpacing(3);
		HorizontalPanel rhs = new HorizontalPanel();
		Button saveButton = new Button(msg.button_save());
		Button cancelButton = new Button(msg.button_cancel());

		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				cancel = true;
				hide();
			}
		});

		saveButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				hide();
			}
		});

		rhs.add(cancelButton);
		rhs.add(saveButton);
		rhs.setSpacing(3);
		revert_defaults_hp.add(lhs);
		revert_defaults_hp.add(rhs);
		revert_defaults_hp.setCellHorizontalAlignment(lhs, HorizontalPanel.ALIGN_LEFT);
		revert_defaults_hp.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);
		revert_defaults_hp.setWidth("100%");

		main_vp.add(revert_defaults_hp);

		main_vp.setWidth(WIDTH + "px");
		main_vp.setHeight(HEIGHT + "px");

		setWidget(main_vp);

		return this;
	}

	public void hide() {
		if (!cancel) {
			for (SettingsPanel p : settingsPanels) {
				String error = p.validData();
				if (error != null) {
					Window.alert(error);
					return;
				}
			}

			for (SettingsPanel p : settingsPanels) {
				if (p.isReadyToSave()) {

					/**
					 * Not allowed, so just skip.
					 */
					if (OneSwarmGWT.isRemoteAccess() && p instanceof RemoteAccessPanel) {
						continue;
					}

					p.sync();
				}
			}

			/**
			 * This has to come _after_ all the settings have actually
			 * synchronized, otherwise we'll miss any changes. Although this is
			 * a hack, synchronizing after a second works.
			 */
			(new Timer() {
				public void run() {
					mSwarmsBrowser.sync_settings();
				}
			}).schedule(1000);
		}
		super.hide();
	}

}
