package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class Sha1Ed2kSettingsPanel extends SettingsPanel {
	TextBox speed_box = new TextBox();
	Label tempLocationLabel = new Label("...");
	SettingsCheckBox multiSwarmDownloadsEnabled = new SettingsCheckBox(msg.settings_multiswarm_enabled(), "oneswarm.multi.torrent.enabled");
	protected String mTempSaveLocation;

	public Sha1Ed2kSettingsPanel() {
		speed_box.setText("...");
		HorizontalPanel p = new HorizontalPanel();
		p.setSpacing(5);
		Label l = new Label(msg.settings_files_max_rehash_speed());
		p.add(l);
		p.setCellVerticalAlignment(l, ALIGN_MIDDLE);
		p.add(speed_box);
		speed_box.setWidth("55px");
		HelpButton h = new HelpButton(msg.settings_files_max_rehash_speed_help());
		p.add(h);
		p.setCellVerticalAlignment(h, ALIGN_MIDDLE);
		this.add(p);
		this.setWidth("100%");

		OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.max.sha1.hash.rate.kbps", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				speed_box.setText((result / 1024.0) + "");
				if (speed_box.getText().equals("...") == false) {
					loadNotify();
				}
			}
		});

		HorizontalPanel multiSwarmPanel = new HorizontalPanel();
		multiSwarmPanel.add(multiSwarmDownloadsEnabled);
		multiSwarmPanel.add(new HelpButton(msg.settings_multiswarm_help()));
		multiSwarmPanel.setSpacing(3);
		this.add(multiSwarmPanel);

		this.add(new Label(msg.settings_multiswarm_save_location()));
		HorizontalPanel browsePanel = new HorizontalPanel();
		this.add(browsePanel);

		final Button multiSwarmBrowse = new Button(msg.button_browse());
		multiSwarmBrowse.addStyleName(OneSwarmCss.SMALL_BUTTON);
		browsePanel.add(multiSwarmBrowse);
		browsePanel.add(tempLocationLabel);
		browsePanel.setCellVerticalAlignment(tempLocationLabel, ALIGN_MIDDLE);
		
		if( OneSwarmGWT.isRemoteAccess() ) { 
			multiSwarmBrowse.setEnabled(false);
		} else {
			multiSwarmBrowse.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					multiSwarmBrowse.setEnabled(false);
					OneSwarmRPCClient.getService().selectFileOrDirectory(OneSwarmRPCClient.getSessionID(), true, new AsyncCallback<String>() {
						public void onFailure(Throwable caught) {
							caught.printStackTrace();
						}

						public void onSuccess(String result) {

							multiSwarmBrowse.setEnabled(true);
							
							if( result == null ) { 
								return;
							}
							
							if (result.length() > 0) {
								mTempSaveLocation = result;
								tempLocationLabel.setText(StringTools.truncate(result, 50, false));
							}
						}
					});
				}
			});
		}

		browsePanel.setSpacing(3);
		
		OneSwarmRPCClient.getService().getMultiTorrentSourceTemp(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(String result) {
				mTempSaveLocation = result;
				tempLocationLabel.setText(StringTools.truncate(result, 50, false));
			}
		});
		
	}

	@Override
	public void sync() {
		int limit = (int) Math.round(1024 * Double.parseDouble(speed_box.getText()));
		OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.max.sha1.hash.rate.kbps", limit, new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Void result) {
				System.out.println("success for UL sync");
			}
		});

		OneSwarmRPCClient.getService().setStringParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.multi.torrent.download.temp.dir", mTempSaveLocation, new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Void result) {
				System.out.println("success for UL sync");
			}
		});
		multiSwarmDownloadsEnabled.save();
	}

	@Override
	String validData() {
		try {
			Double.parseDouble(speed_box.getText());
		} catch (NumberFormatException e) {
			return msg.settings_files_max_rehash_number_error();
		} catch (Exception e) {
			return e.toString();
		}
		return null;
	}

}
