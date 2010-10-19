package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class SaveLocationPanel extends SettingsPanel {
	final Label pathLabel = new Label(msg.loading());

	public boolean ready_to_save = false;

	String loc = "";

	public String getSaveLocation() {
		return loc;
	}

	public SaveLocationPanel() {
		super();

		final HorizontalPanel button_and_path = new HorizontalPanel();

		Label l = new Label(msg.settings_files_save_loc());
		VerticalPanel stacked = new VerticalPanel();
		stacked.add(l);
		final Button chooseButton = new Button(msg.button_browse());
		chooseButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		button_and_path.add(chooseButton);
		button_and_path.setSpacing(3);
		stacked.add(button_and_path);

		if (OneSwarmGWT.isRemoteAccess()) {
			chooseButton.setEnabled(false);
		} else {
			chooseButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					chooseButton.setEnabled(false);
					OneSwarmRPCClient.getService().selectFileOrDirectory(
							OneSwarmRPCClient.getSessionID(), true, new AsyncCallback<String>() {
								public void onFailure(Throwable caught) {
									caught.printStackTrace();
								}

								public void onSuccess(String path) {
									if (path != null) {
										pathLabel.setText(path);
										chooseButton.setEnabled(true);
									}
								}
							});
				}
			});
		}

		button_and_path.add(pathLabel);
		button_and_path.setCellVerticalAlignment(pathLabel, VerticalPanel.ALIGN_MIDDLE);

		button_and_path.setCellHorizontalAlignment(chooseButton, HorizontalPanel.ALIGN_LEFT);

		this.add(stacked);

		OneSwarmRPCClient.getService().getStringParameterValue(OneSwarmRPCClient.getSessionID(),
				"Default save path", new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(String result) {
						if (result != null) {
							loc = result;
							pathLabel.setText(StringTools.truncate(loc, 50, false));
							loadNotify();
						}
					}
				});
	}

	public void sync() {
		OneSwarmRPCClient.getService().setStringParameterValue(OneSwarmRPCClient.getSessionID(),
				"Default save path", pathLabel.getText(), new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(Void result) {
						System.out.println("success for save dir sync");
					}
				});
	}

	String validData() {
		return null; // chosen with file chooser
	}
}
