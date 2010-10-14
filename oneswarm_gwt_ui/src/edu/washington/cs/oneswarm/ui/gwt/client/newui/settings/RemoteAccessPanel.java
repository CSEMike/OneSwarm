package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

class RemoteAccessPanel extends SettingsPanel {
	private static final String NO_PASSWORD_CHANGE_PW = "7JdGVgPjtQ0WLB5yw81s";
	private final SettingsCheckBox enableRemoteAccess;
	private final TextBox usernameArea = new TextBox();
	private final PasswordTextBox passwordTextArea = new PasswordTextBox();
	private final Grid bindAddressesGrid = new Grid(2, 2);

	RemoteAccessPanel() {

		passwordTextArea.setText(NO_PASSWORD_CHANGE_PW);
		passwordTextArea.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (passwordTextArea.getText().equals(NO_PASSWORD_CHANGE_PW)) {
					passwordTextArea.setText("");
				}
			}
		});

		loadNotify();
		HorizontalPanel panel = new HorizontalPanel();
		panel.setWidth("340px");
		enableRemoteAccess = new SettingsCheckBox(msg.settings_net_remote_access_enable(), new String[] { OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY });
		panel.add(enableRemoteAccess);
		enableRemoteAccess.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				enableFields(enableRemoteAccess.getValue());
			}
		});
		OneSwarmRPCClient.getService().getBooleanParameterValue(OneSwarmRPCClient.getSessionID(), OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Boolean result) {
				enableFields(result);
			}
		});

		Button advancedButton = new Button(msg.button_advanced());
		advancedButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		advancedButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				RemoteAccessAdvancedDialog dlg = new RemoteAccessAdvancedDialog();
				dlg.show();
				dlg.setVisible(false);
				dlg.center();
				dlg.setPopupPosition(dlg.getPopupLeft(), dlg.getPopupTop() + 20);
				dlg.setVisible(true);
			}
		});
		panel.add(advancedButton);

		Grid g = new Grid(2, 2);

		g.setWidget(0, 0, new Label(msg.settings_net_remote_access_user()));
		/*
		 * fix the username area
		 */
		g.setWidget(0, 1, usernameArea);

		OneSwarmRPCClient.getService().getRemoteAccessUserName(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(String result) {
				usernameArea.setText(result);
				if (result.equals("username")) {
					passwordTextArea.setText("");
				}
			}
		});

		g.setWidget(1, 0, new Label(msg.settings_net_remote_access_password()));
		g.setWidget(1, 1, passwordTextArea);

		bindAddressesGrid.setVisible(false);
		OneSwarmRPCClient.getService().getListenAddresses(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String[]>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(String[] result) {
				Label externalLabel = new Label(msg.settings_net_remote_access_address_remote());
				Label internalLabel = new Label(msg.settings_net_remote_access_address_lan());
				String external = "https://" + result[0] + "/";
				String internal = "https://" + result[1] + "/";
				String externalLink = "<a href='" + external + "' target='_blank'>" + external + "</a>";
				String internalLink = "<a href='" + internal + "' target='_blank'>" + internal + "</a>";
				bindAddressesGrid.setWidget(0, 0, externalLabel);
				bindAddressesGrid.setWidget(0, 1, new HTML(externalLink, true));
				if (!result[0].equals(result[1])) {
					bindAddressesGrid.setWidget(1, 0, internalLabel);
					bindAddressesGrid.setWidget(1, 1, new HTML(internalLink, true));
				}
			}
		});

		bindAddressesGrid.setWidth("98%");

		super.add(panel);
		super.add(g);
		super.add(bindAddressesGrid);

	}

	private void enableFields(boolean enabled) {
		usernameArea.setEnabled(enabled);

		if (OneSwarmGWT.isRemoteAccess()) {
			usernameArea.setEnabled(false);
			passwordTextArea.setEnabled(false);
		} else {
			passwordTextArea.setEnabled(enabled);
			bindAddressesGrid.setVisible(enabled);
		}
	}

	public void sync() {
		enableRemoteAccess.save();
		if (enableRemoteAccess.getValue()) {
			String pw = passwordTextArea.getText();
			if (pw.equals(NO_PASSWORD_CHANGE_PW)) {
				pw = null;
			}
			OneSwarmRPCClient.getService().saveRemoteAccessCredentials(OneSwarmRPCClient.getSessionID(), usernameArea.getText(), pw, new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(String result) {
					if (result != null) {
						enableRemoteAccess.setValue(false);
						enableRemoteAccess.save();

						Window.alert(msg.settings_net_remote_access_prompt_unable_to_save() + "\n" + result);
					}
				}
			});
		}
	}

	@Override
	String validData() {
		String message = msg.settings_net_remote_access_prompt_invalid_setting()+"\n  ";
		if (!enableRemoteAccess.getValue()) {
			return null;
		} else {
			String u = usernameArea.getText();
			if (u.length() < 1) {
				return message + msg.settings_net_remote_access_prompt_username_to_short();
			}
			if (u.equals("username")) {
				return message + msg.settings_net_remote_access_prompt_username_not_allowed();
			}
			String p = passwordTextArea.getText();
			if (p.length() < 8) {
				return message + msg.settings_net_remote_access_prompt_password_to_short();
			}

			return null;
		}

	}

	public class RemoteAccessAdvancedDialog extends OneSwarmDialogBox {
		private static final int WIDTH = 400;

		Button okButton = new Button(msg.button_save());
		Button cancelButton = new Button(msg.button_cancel());
		TextBox ipRangeBox = new TextBox();
		RadioButton noLimitButton = new RadioButton("limitGroup", msg.settings_net_remote_access_ip_limit_no_limit());
		RadioButton lanButton = new RadioButton("limitGroup", msg.settings_net_remote_access_ip_limit_lan_only());
		final RadioButton ipRangeButton = new RadioButton("limitGroup", msg.settings_net_remote_access_ip_limit_range_only());

		ClickHandler clickListener = new ClickHandler() {
			public void onClick(ClickEvent event) {
				updateSelection(getSelectedType());
			}
		};

		public RemoteAccessAdvancedDialog() {
			super(false, true, false);

			setText(msg.settings_net_remote_access_advanced());

			Label selectLabel = new Label(msg.settings_net_remote_access_ip_limit());
			selectLabel.addStyleName(CSS_DIALOG_HEADER);
			selectLabel.setWidth(WIDTH + "px");

			VerticalPanel panel = new VerticalPanel();
			panel.add(selectLabel);
			panel.setWidth(WIDTH + "px");

			noLimitButton.setEnabled(false);
			noLimitButton.addClickHandler(clickListener);
			panel.add(noLimitButton);

			lanButton.setEnabled(false);
			lanButton.addClickHandler(clickListener);
			panel.add(lanButton);

			panel.add(ipRangeButton);
			ipRangeButton.setEnabled(false);
			ipRangeButton.addClickHandler(clickListener);

			HorizontalPanel boxPanel = new HorizontalPanel();
			boxPanel.setWidth(WIDTH + "px");
			boxPanel.add(ipRangeBox);
			ipRangeBox.setWidth((WIDTH - 30) + "px");
			ipRangeBox.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					if (ipRangeBox.isReadOnly()) {
						updateSelection(OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE);
					}
				}
			});
			ipRangeBox.setReadOnly(true);
			ipRangeBox.setEnabled(false);

			HelpButton helpButton = new HelpButton(msg.settings_net_remote_access_ip_limit_help());
			boxPanel.add(helpButton);
			boxPanel.setCellVerticalAlignment(helpButton, HorizontalPanel.ALIGN_MIDDLE);
			boxPanel.setCellHorizontalAlignment(helpButton, HorizontalPanel.ALIGN_LEFT);

			panel.add(boxPanel);

			HorizontalPanel buttons = new HorizontalPanel();
			buttons.add(cancelButton);
			buttons.add(okButton);
			buttons.setSpacing(3);

			okButton.addClickHandler(this);
			cancelButton.addClickHandler(this);
			panel.add(buttons);

			panel.setCellHorizontalAlignment(buttons, HorizontalPanel.ALIGN_RIGHT);

			this.setWidget(panel);

			/*
			 * and set the initial data
			 */
			OneSwarmRPCClient.getService().getStringParameterValue(OneSwarmRPCClient.getSessionID(), OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_KEY, new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
					hide();
				}

				public void onSuccess(String result) {
					updateSelection(result);
				}
			});

			OneSwarmRPCClient.getService().getStringParameterValue(OneSwarmRPCClient.getSessionID(), OneSwarmConstants.REMOTE_ACCESS_LIMIT_IPS_KEY, new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
					hide();
				}

				public void onSuccess(String result) {
					if (result != null) {
						ipRangeBox.setText(result);
					}
					ipRangeBox.setEnabled(true);
				}
			});
		}

		public String getSelectedType() {
			if (noLimitButton.getValue()) {
				return OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT;
			} else if (lanButton.getValue()) {
				return OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_LAN;
			} else if (ipRangeButton.getValue()) {
				return OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE;
			} else {
				return OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT;
			}
		}

		public void updateSelection(String type) {
			boolean boxEnabled = false;
			noLimitButton.setEnabled(true);
			lanButton.setEnabled(true);
			ipRangeButton.setEnabled(true);
			if (type == null) {
				noLimitButton.setValue(true);
			} else if (type.equals(OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_NOLIMIT)) {
				noLimitButton.setValue(true);
			} else if (type.equals(OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_LAN)) {
				lanButton.setValue(true);
			} else if (type.equals(OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE)) {
				ipRangeButton.setValue(true);
				boxEnabled = true;
			} else {
				noLimitButton.setValue(true);
			}

			ipRangeBox.setReadOnly(!boxEnabled);
		}

		public void onClick(ClickEvent event) {
			if (event.getSource().equals(okButton)) {
				okButton.setEnabled(false);
				OneSwarmRPCClient.getService().updateRemoteAccessIpFilter(OneSwarmRPCClient.getSessionID(), getSelectedType(), ipRangeBox.getText(), new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						Window.alert(caught.getMessage());
						okButton.setEnabled(true);
					}

					public void onSuccess(Void result) {
						hide();
					}
				});

			} else if (event.getSource().equals(cancelButton)) {
				hide();
			} else {
				super.onClick(event);
			}
		}
	}

}
