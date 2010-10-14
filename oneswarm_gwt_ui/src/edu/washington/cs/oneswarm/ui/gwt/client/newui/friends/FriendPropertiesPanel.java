package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.Date;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosureEvent;
import com.google.gwt.user.client.ui.DisclosureHandler;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.LabelWithHelp;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details.FormattedSize;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class FriendPropertiesPanel extends Composite implements Updateable {
	private static OSMessages msg = OneSwarmGWT.msg;

	protected static final int CLOSED_WIDTH = 400;
	protected static final int OPEN_WIDTH = 440;

	private final Button cancel2Button = new Button(msg.button_cancel());
	private final Button cancelButton = new Button(msg.button_cancel());

	private ConnectInfoPanel connectInfoPanel;

	private final Button deleteButton = new Button(msg.friend_properties_button_delete_friend());
	private HorizontalPanel deleteUndeletePanel;
	private final FriendInfoLite friend;
	private final CheckBox limitedFriendBox = new CheckBox(msg.friend_properties_limited());
	private final CheckBox requestFileListBox = new CheckBox(msg.friend_properties_request_file_list());
	private final CheckBox allowChatCheckBox = new CheckBox(msg.friend_properties_allow_chat());
	private DisclosurePanel moreInfoPanel = null;
	private TextBox nameBox;
	private final Button pdelete = new Button(msg.friend_properties_button_delete_friend_perm());
	private final Button saveButton = new Button(msg.button_save());
	private HorizontalPanel saveCancelPanel;
	private VerticalPanel allButtonsPanel = new VerticalPanel();
	private final Button undelete = new Button(msg.friend_properties_button_undelete());
	private boolean updateNeeded = false;
	private CheckBox enabledBox;

	private TextBox groupTB = new TextBox();

	/**
	 * Creates a horizontal panel without any buttons, it is the wrapping panel
	 * that has to call save
	 * 
	 * @param friend
	 * @param showSkip
	 */

	public FriendPropertiesPanel(final FriendInfoLite friend, boolean showSkip) {

		styleButtons();

		HorizontalPanel mainPanel = new HorizontalPanel();
		this.friend = friend;
		if (showSkip) {
			this.enabledBox = new CheckBox();
			enabledBox.setValue(true);
			enabledBox.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					setChecked(enabledBox.getValue());
				}
			});
			mainPanel.add(enabledBox);
			mainPanel.setCellVerticalAlignment(enabledBox, HorizontalPanel.ALIGN_MIDDLE);
			mainPanel.setSpacing(5);
		}
		// for the name field
		mainPanel.add(getNamePanel());
		HorizontalPanel settingsPanel = createSettingsPanel();
		mainPanel.add(settingsPanel);
		mainPanel.setCellVerticalAlignment(settingsPanel, VerticalPanel.ALIGN_MIDDLE);
		this.initWidget(mainPanel);
	}

	public void setChecked(boolean val) {
		if (enabledBox != null) {
			enabledBox.setValue(val);
		}
		nameBox.setEnabled(val);
		limitedFriendBox.setEnabled(val);
		allowChatCheckBox.setEnabled(val);
		requestFileListBox.setEnabled(val);
	}

	public static String LIMITED_FRIEND = msg.friend_properties_limited_help();
	public static String CHAT_HELP = "";

	private final LabelWithHelp statusLabel = new LabelWithHelp("", msg.friend_properties_status_connect_help_HTML(), true);

	private boolean useDebug = false;

	final VerticalPanel mainPanel = new VerticalPanel();

	public FriendPropertiesPanel(final FriendInfoLite friend, final OneSwarmDialogBox parent, boolean useDebug) {
		this.useDebug = useDebug;
		final HorizontalPanel lowerPanel = new HorizontalPanel();
		lowerPanel.setWidth("100%");

		styleButtons();

		limitedFriendBox.setTitle(LIMITED_FRIEND);
		requestFileListBox.setTitle(msg.friend_properties_request_file_list_help());
		mainPanel.setWidth(OPEN_WIDTH + "px");
		this.friend = friend;
		// for the name field
		HorizontalPanel namePanel = getNamePanel();
		mainPanel.add(namePanel);
		mainPanel.add(createSettingsPanel());

		moreInfoPanel = createMoreInfoPanel();
		if (friend.getStatus() == FriendInfoLite.STATUS_ONLINE || friend.getStatus() == FriendInfoLite.STATUS_HANDSHAKING) {
			statusLabel.setVisible(false);
		}
		mainPanel.add(statusLabel);
		mainPanel.add(lowerPanel);
		lowerPanel.add(moreInfoPanel);
		lowerPanel.add(allButtonsPanel);
		allButtonsPanel.setWidth("100%");
		moreInfoPanel.addEventHandler(new DisclosureHandler() {
			public void onClose(DisclosureEvent event) {
				mainPanel.setWidth(CLOSED_WIDTH + "px");
				deleteButton.setVisible(false);
				pdelete.setVisible(false);
				lowerPanel.add(allButtonsPanel);
				mainPanel.remove(allButtonsPanel);
			}

			public void onOpen(DisclosureEvent event) {
				mainPanel.setWidth(OPEN_WIDTH + "px");
				deleteButton.setVisible(true);
				pdelete.setVisible(true);
				lowerPanel.remove(allButtonsPanel);
				mainPanel.add(allButtonsPanel);
			}
		});

		saveCancelPanel = createOkCancelPanel(parent);
		deleteUndeletePanel = createPDeleteUndeletePanel(parent);
		allButtonsPanel.add(saveCancelPanel);
		allButtonsPanel.add(deleteUndeletePanel);

		mainPanel.setCellHorizontalAlignment(saveCancelPanel, VerticalPanel.ALIGN_RIGHT);
		mainPanel.setCellHorizontalAlignment(deleteUndeletePanel, VerticalPanel.ALIGN_RIGHT);

		setDeletedMode(friend.isBlocked());
		this.initWidget(mainPanel);

		update(0);
		OneSwarmGWT.addToUpdateTask(this);
	}

	private void styleButtons() {
		// cancelButton.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		// cancel2Button.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		// saveButton.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		// pdelete.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		// deleteButton.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
	}

	private DisclosurePanel createMoreInfoPanel() {
		DisclosurePanel p = new DisclosurePanel(msg.friend_properties_more());

		p.addEventHandler(new DisclosureHandler() {
			public void onClose(DisclosureEvent event) {
				cancelButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
				cancel2Button.addStyleName(OneSwarmCss.SMALL_BUTTON);
				saveButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
				deleteButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
			}

			public void onOpen(DisclosureEvent event) {
				cancelButton.removeStyleName(OneSwarmCss.SMALL_BUTTON);
				cancel2Button.removeStyleName(OneSwarmCss.SMALL_BUTTON);
				saveButton.removeStyleName(OneSwarmCss.SMALL_BUTTON);
				deleteButton.removeStyleName(OneSwarmCss.SMALL_BUTTON);
			}
		});

		// p.setWidth("100%");
		long downloadedTotal = friend.getDownloadedTotal();
		// long downloadedSession = friend.getDownloadedSession();
		long uploadedTotal = friend.getUploadedTotal();
		// long uploadedSession = friend.getUploadedSession();

		double upDownRatio = 1;
		if (uploadedTotal > 0 && downloadedTotal > 0) {
			upDownRatio = (int) (((100.0 * downloadedTotal) / uploadedTotal)) / 100.0;
		}

		VerticalPanel vert = new VerticalPanel();
		HorizontalPanel head = new HorizontalPanel();
		head.setWidth("100%");
		Label l = new Label(msg.friend_properties_friend_stats());
		head.add(l);
		if (useDebug) {
			Button b = new Button(msg.friend_properties_debug_message_log());
			b.addStyleName(OneSwarmCss.SMALL_BUTTON);
			b.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					OneSwarmDialogBox dlg = new MessageLog();
					dlg.show();
					dlg.setVisible(false);
					dlg.center();
					dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
					dlg.setVisible(true);
				}
			});
			head.add(b);
			head.setCellHorizontalAlignment(b, HorizontalPanel.ALIGN_RIGHT);
		}

		HorizontalPanel groupHP = new HorizontalPanel();
		l = new Label(msg.friend_properties_group());
		groupHP.add(l);
		groupHP.add(groupTB);
		// groupHP.setCellWidth(l, "50px");
		groupTB.setText(friend.getGroup());
		groupHP.setCellVerticalAlignment(l, VerticalPanel.ALIGN_MIDDLE);
		vert.add(groupHP);
		vert.add(head);

		Grid g = new Grid(3, 2);
		g.setWidth("250px");
		g.setWidget(0, 0, new Label(msg.friend_properties_friend_share_ratio()));
		g.setWidget(0, 1, new Label("" + upDownRatio));
		g.setWidget(1, 0, new Label(msg.friend_properties_downloaded_from_friend()));
		g.setWidget(1, 1, new FormattedSize(downloadedTotal));
		g.setWidget(2, 0, new Label(msg.friend_properties_uploaded_to_friend()));
		g.setWidget(2, 1, new FormattedSize(uploadedTotal));
		vert.add(g);
		connectInfoPanel = new ConnectInfoPanel(friend);
		// connectInfoPanel.setWidth("250px");
		vert.add(connectInfoPanel);
		p.add(vert);

		return p;
	}

	private HorizontalPanel createOkCancelPanel(final OneSwarmDialogBox parent) {
		HorizontalPanel okCancelPanel = new HorizontalPanel();
		okCancelPanel.setWidth("100%");
		okCancelPanel.setSpacing(3);
		deleteButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				friend.setBlocked(true);
				save(parent);
			}
		});
		okCancelPanel.add(deleteButton);
		okCancelPanel.setCellHorizontalAlignment(deleteButton, HorizontalPanel.ALIGN_LEFT);
		okCancelPanel.setCellWidth(deleteButton, "100%");
		deleteButton.setVisible(false);

		okCancelPanel.add(cancelButton);
		okCancelPanel.setCellHorizontalAlignment(cancelButton, HorizontalPanel.ALIGN_RIGHT);
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (parent != null) {
					parent.hide();
				}
			}
		});

		okCancelPanel.add(saveButton);
		okCancelPanel.setCellHorizontalAlignment(saveButton, HorizontalPanel.ALIGN_RIGHT);
		saveButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				friend.setCanSeeFileList(!limitedFriendBox.getValue());
				friend.setRequestFileList(requestFileListBox.getValue());
				friend.setAllowChat(allowChatCheckBox.getValue());
				friend.setName(nameBox.getText());
				friend.setGroup(groupTB.getText());
				save(parent);
			}

		});
		return okCancelPanel;
	}

	private HorizontalPanel createPDeleteUndeletePanel(final OneSwarmDialogBox parent) {
		final HorizontalPanel p = new HorizontalPanel();
		p.setWidth("100%");
		p.setSpacing(3);

		pdelete.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				boolean yes = Window.confirm(msg.friend_properties_confirm_perm_delete(friend.getName()));
				if (yes) {
					pdelete.setEnabled(false);
					undelete.setEnabled(false);
					cancelButton.setEnabled(false);
					OneSwarmRPCClient.getService().deleteFriends(OneSwarmRPCClient.getSessionID(), new FriendInfoLite[] { friend }, new AsyncCallback<Void>() {
						public void onFailure(Throwable caught) {
							Window.alert("got error when deleting friend: " + caught.getMessage());
						}

						public void onSuccess(Void result) {
							OneSwarmRPCClient.getService().setRecentChanges(OneSwarmRPCClient.getSessionID(), true, new AsyncCallback<Void>() {
								public void onFailure(Throwable caught) {
									OneSwarmGWT.log("delete friend: got error");
								}

								public void onSuccess(Void result) {
								}
							});
							parent.hide();
						}
					});
				}
			}
		});
		p.add(pdelete);
		p.setCellHorizontalAlignment(pdelete, HorizontalPanel.ALIGN_LEFT);
		p.setCellWidth(pdelete, "100%");
		pdelete.setVisible(false);

		p.add(cancel2Button);
		p.setCellHorizontalAlignment(cancel2Button, HorizontalPanel.ALIGN_RIGHT);
		cancel2Button.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (parent != null) {
					parent.hide();
				}
			}
		});

		undelete.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				setDeletedMode(false);
				friend.setBlocked(false);
			}
		});
		p.add(undelete);
		p.setCellHorizontalAlignment(undelete, HorizontalPanel.ALIGN_RIGHT);

		return p;
	}

	private HorizontalPanel getNamePanel() {
		HorizontalPanel namePanel = new HorizontalPanel();
		namePanel.setSpacing(3);
		Label nameLabel = new Label(msg.friend_properties_nickname_label());
		namePanel.add(nameLabel);
		namePanel.setCellHorizontalAlignment(nameLabel, HorizontalPanel.ALIGN_LEFT);
		namePanel.setCellVerticalAlignment(nameLabel, HorizontalPanel.ALIGN_MIDDLE);
		nameBox = new TextBox();
		nameBox.setWidth("130px");
		namePanel.add(nameBox);
		nameBox.setText(friend.getName());
		nameBox.setFocus(true);

		
		return namePanel;
	}
	
	private HorizontalPanel createSettingsPanel(){
		HorizontalPanel namePanel = new HorizontalPanel();
		limitedFriendBox.setValue(!friend.isCanSeeFileList());
		requestFileListBox.setValue(friend.isRequestFileList());
		allowChatCheckBox.setValue(friend.isAllowChat());
		
		namePanel.add(limitedFriendBox);
		namePanel.setCellVerticalAlignment(limitedFriendBox, HorizontalPanel.ALIGN_MIDDLE);
		HelpButton helpButton = new HelpButton(LIMITED_FRIEND);
		namePanel.add(helpButton);
		namePanel.setCellVerticalAlignment(helpButton, HorizontalPanel.ALIGN_MIDDLE);
		namePanel.setCellHorizontalAlignment(helpButton, HorizontalPanel.ALIGN_RIGHT);

		namePanel.add(requestFileListBox);
		namePanel.setCellVerticalAlignment(requestFileListBox, HorizontalPanel.ALIGN_MIDDLE);
		HelpButton requestHelpButton = new HelpButton(msg.friend_properties_request_file_list_help());
		namePanel.add(requestHelpButton);
		namePanel.setCellVerticalAlignment(requestHelpButton, HorizontalPanel.ALIGN_MIDDLE);
		namePanel.setCellHorizontalAlignment(requestHelpButton, HorizontalPanel.ALIGN_RIGHT);
		
		namePanel.add(allowChatCheckBox);
		namePanel.setCellVerticalAlignment(allowChatCheckBox, HorizontalPanel.ALIGN_MIDDLE);
		HelpButton chatHelpButton = new HelpButton(CHAT_HELP);
		namePanel.add(chatHelpButton);
		namePanel.setCellVerticalAlignment(chatHelpButton, HorizontalPanel.ALIGN_MIDDLE);
		namePanel.setCellHorizontalAlignment(chatHelpButton, HorizontalPanel.ALIGN_RIGHT);
		
		namePanel.setSpacing(3);
		
		return namePanel;
	}

	private void save(final OneSwarmDialogBox parent) {
		final String session = OneSwarmRPCClient.getSessionID();
		final OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		if (parent != null) {
			saveButton.setEnabled(false);
			cancelButton.setEnabled(false);
			deleteButton.setEnabled(false);
		}

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				// well, do nothing...
				OneSwarmGWT.log("error " + caught.getMessage());
				Window.alert(caught.getMessage());
			}

			public void onSuccess(Void result) {
				if (parent != null) {
					parent.hide();
				}
			}
		};
		service.setFriendsSettings(session, new FriendInfoLite[] { friend }, callback);
		OneSwarmRPCClient.getService().setRecentChanges(OneSwarmRPCClient.getSessionID(), true, new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				OneSwarmGWT.log("delete friend: got error");
			}

			public void onSuccess(Void result) {
			}
		});
	}

	public void saveChanges(final OneSwarmDialogBox parent, boolean forceUpdate, boolean reallySkipSkipped) {

		if (forceUpdate) {
			updateNeeded = true;
		}

		if (friend.isCanSeeFileList() == limitedFriendBox.getValue()) {
			updateNeeded = true;
		}
		if (!friend.getName().equals(nameBox.getText())) {
			updateNeeded = true;
		}
		if(friend.isRequestFileList() != requestFileListBox.getValue()){
			updateNeeded = true;
		}

		friend.setCanSeeFileList(!limitedFriendBox.getValue());
		friend.setRequestFileList(requestFileListBox.getValue());
		friend.setName(nameBox.getText());

		if (enabledBox != null) {
			if (enabledBox.getValue() == false && reallySkipSkipped) {
				return;
			}
			friend.setBlocked(!enabledBox.getValue());
		}
		/*
		 * add the friend if we are not friends already
		 */
		final String session = OneSwarmRPCClient.getSessionID();
		final OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onFailure(Throwable caught) {
				// this means that we already have the friend, save if needed
				if (updateNeeded) {
					save(parent);
				} else {
					parent.hide();
				}
			}

			public void onSuccess(Void result) {
				/*
				 * ok, we don't have the friend, add it and save
				 */
				service.addFriend(session, friend, false, new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						Window.alert("problem when adding friend: " + caught.getMessage());
						return;
					}

					public void onSuccess(Void result) {
						save(parent);
					}
				});

			}
		};
		boolean testOnly = true;
		service.addFriend(session, friend, testOnly, callback);
	}

	private void setDeletedMode(boolean deleted) {
		// if in deleted mode, disable most stuff, but show the delete undelete
		// panel
		deleteUndeletePanel.setVisible(deleted);

		saveCancelPanel.setVisible(!deleted);
		limitedFriendBox.setEnabled(!deleted);
		requestFileListBox.setEnabled(!deleted);
		nameBox.setEnabled(!deleted);
		connectInfoPanel.setEnabled(!deleted);

	}

	public void update(int count) {
		connectInfoPanel.update(count);

		/*
		 * check what to do with the status label
		 */
		String problemText = connectInfoPanel.getDiagnosis();
		if (!friend.isBlocked() && problemText != null && !"".equals(problemText)) {
			statusLabel.setVisible(true);
			statusLabel.setHTML(msg.friend_properties_connect_diag_HTML() + " " + problemText);
		} else {
			statusLabel.setVisible(false);
		}
	}

	public void stopUpdates() {
		if (connectInfoPanel != null) {
			OneSwarmGWT.removeFromUpdateTask(this);
			OneSwarmGWT.log("removing friend info from update task");
		}
	}

	public void totalStop() {
		OneSwarmGWT.removeFromUpdateTask(this);
		OneSwarmGWT.log("removing friend info from update task");
	}

	class MessageLog extends OneSwarmDialogBox {
		private TextArea area = new TextArea();

		public MessageLog() {
			super(true, false, true);
			super.setText(msg.friend_properties_debug_packet_log(friend.getName()));
			area.setWidth("900px");

			VerticalPanel p = new VerticalPanel();
			p.add(area);
			area.addStyleName("os-connectionLog");
			area.setVisibleLines(31);
			area.setReadOnly(true);
			area.getElement().setAttribute("WRAP", "OFF");
			Button b = new Button(msg.button_update());
			p.add(b);
			b.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					update();
				}
			});
			this.setWidget(p);
			update();
		}

		private void update() {
			OneSwarmRPCClient.getService().getDebugMessageLog(OneSwarmRPCClient.getSessionID(), friend.getPublicKey(), new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
				}

				public void onSuccess(String result) {
					area.setText(result);
				}
			});
		}
	}

	static class ConnectInfoPanel extends VerticalPanel {
		private String diagnosis = "";

		private static final String CONNECTED_SINCE_STRING = msg.friend_properties_connected_since();
		private static final String LAST_CONNECT_STRING = msg.friend_properties_last_connect();
		final Button connectButton = new Button(msg.button_force_connect());
		final TextArea connectionLog = new TextArea();
		boolean enabled = true;
		private FriendInfoLite friend;
		Grid infoGrid = new Grid(4, 3);
		final Label lastConnectedLabel = new Label(LAST_CONNECT_STRING);

		// final HTML problemLabel = new HTML();

		public ConnectInfoPanel(FriendInfoLite _friend) {
			super();
			this.friend = _friend;

			/*
			 * add a label
			 */
			Label connectInfo = new Label(msg.friend_properties_connect_info());
			super.add(connectInfo);

			/*
			 * add some info
			 */

			infoGrid.setWidget(0, 0, new Label(msg.friend_properties_status()));
			infoGrid.setWidget(0, 1, new Label(""));

			infoGrid.setWidget(1, 0, lastConnectedLabel);
			infoGrid.setWidget(1, 1, new Label(""));

			infoGrid.setWidget(2, 0, new Label(msg.friend_properties_last_connected_ip()));
			infoGrid.setWidget(2, 1, new Label(""));

			infoGrid.setWidth("100%");
			infoGrid.setWidget(2, 2, connectButton);
			connectButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

			infoGrid.setWidget(3, 0, new Label(msg.friend_properties_source()));
			infoGrid.setWidget(3, 1, new Label(StringTools.truncate(friend.getSource(), 30, false)));

			super.add(infoGrid);

			/*
			 * and the log
			 */
			connectionLog.setWidth("400px");
			connectionLog.setVisibleLines(6);
			connectionLog.addStyleName("os-connectionLog");
			connectionLog.setReadOnly(true);
			connectionLog.getElement().setAttribute("WRAP", "OFF");
			// connectionLog.setWidth("300px");
			// connectionLog.setHeight("100%");
			// ScrollPanel sp = new ScrollPanel(connectionLog);
			// sp.setWidth("300px");
			super.add(connectionLog);

			/*
			 * create the button
			 */
			connectButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					connectButton.setEnabled(false);
					String session = OneSwarmRPCClient.getSessionID();
					OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
					service.connectToFriends(session, new FriendInfoLite[] { friend }, new AsyncCallback<Void>() {
						public void onFailure(Throwable caught) {
							OneSwarmGWT.log("connect to friend: got error");
						}

						public void onSuccess(Void result) {
							OneSwarmGWT.log("connection attempt initiated");
						}
					});
				}
			});
			// super.add(problemLabel);
		}

		public String getDiagnosis() {
			return diagnosis;
		}

		public Grid getInfoGrid() {
			return infoGrid;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
			connectButton.setEnabled(enabled);
		}

		public void update(int count) {
			String session = OneSwarmRPCClient.getSessionID();
			OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

			AsyncCallback<FriendInfoLite> callback = new AsyncCallback<FriendInfoLite>() {

				private String getDiagnosis(FriendInfoLite f) {
					boolean hasBeenConnected = f.getLastConnectedDate() != null;
					boolean inDht = f.getConnectionLog().contains("got friend location from DHT");
					boolean inCht = f.getConnectionLog().contains("got friend location from CHT");
					boolean timeOut = f.getConnectionLog().contains("30s");
					if (!hasBeenConnected && !inDht) {
						return msg.friend_offline_diag_not_added();
					} else if ((inDht || inCht) && timeOut) {
						return msg.friend_offline_diag_nat();
					} else if (!(inDht || inCht)) {
						return msg.friend_offline_diag_offline();
					}
					return null;
				}

				public void onFailure(Throwable caught) {
					// well, do nothing...
					OneSwarmGWT.log("error " + caught.getMessage());
				}

				public void onSuccess(FriendInfoLite result) {
					friend = result;
					if (friend == null)
						return;

					OneSwarmGWT.log("updating friend details: " + friend.getName());
					/*
					 * button is only enabled if we are disconnected
					 */
					if (friend.getStatus() == FriendInfoLite.STATUS_OFFLINE) {
						if (enabled) {
							connectButton.setEnabled(true);
						}
						lastConnectedLabel.setText(LAST_CONNECT_STRING);
					} else {
						connectButton.setEnabled(false);
					}
					// problemLabel.setVisible(false);
					diagnosis = null;
					switch (friend.getStatus()) {
					case FriendInfoLite.STATUS_CONNECTING:
						updateGrid(0, 1, msg.friend_status_connecting());
						diagnosis = msg.friend_status_connecting() + "...";
						break;
					case FriendInfoLite.STATUS_HANDSHAKING:
						updateGrid(0, 1, msg.friend_status_handshaking());
						break;
					case FriendInfoLite.STATUS_ONLINE:
						updateGrid(0, 1, msg.friend_status_connected());
						lastConnectedLabel.setText(CONNECTED_SINCE_STRING);
						break;
					case FriendInfoLite.STATUS_OFFLINE:
						if (friend.getLastConnectedDate() == null) {
							updateGrid(0, 1, msg.friend_status_never_connected());
						} else {
							updateGrid(0, 1, msg.friend_status_diconnected());
						}
						diagnosis = getDiagnosis(friend);
						if (diagnosis != null) {
							// problemLabel.setHTML("<b>Likely problem: </b>" +
							// diagnosis);
							// problemLabel.setVisible(true);
						}
						break;
					default:
						break;
					}

					Date lastConnectedDate = friend.getLastConnectedDate();

					if (lastConnectedDate != null) {
						updateGrid(1, 1, StringTools.formatDateAppleLike(lastConnectedDate, true));
						updateGrid(2, 1, friend.getLastConnectIp() + ":" + friend.getLastConnectPort());
					} else {
						updateGrid(1, 1, msg.date_never());
						updateGrid(2, 1, "");
					}
					if (!connectionLog.getText().equals(friend.getConnectionLog())) {
						connectionLog.setText(friend.getConnectionLog());
						connectionLog.setCursorPos(connectionLog.getText().length());
					}
				}

				private void updateGrid(int row, int col, String text) {
					Label l = (Label) infoGrid.getWidget(row, col);
					if (!l.getText().equals(text)) {
						l.setText(text);
					}
				}
			};
			service.getUpdatedFriendInfo(session, friend, callback);
		}
	}

	public String getGroup() {
		return groupTB.getText();
	}
}
