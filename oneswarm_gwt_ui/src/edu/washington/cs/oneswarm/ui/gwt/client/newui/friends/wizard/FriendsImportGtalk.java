/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

class FriendsImportGtalk extends VerticalPanel implements Updateable {
	private static OSMessages msg = OneSwarmGWT.msg;

	public final static String FRIEND_NETWORK_GTALK_NAME = "XMPP (Google)";
	private final TextBox computerNameBox = new TextBox();
	Button connectButton;
	private final KeyUpHandler enterListener = new KeyUpHandler() {
		boolean enabled = true;

		public void onKeyUp(KeyUpEvent event) {
			if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
				if (enabled) {
					connect();
					enabled = false;
				}
			} else {
				enabled = true;
			}
		}
	};

	private final FriendsImportCallback fwcallback;

	private final PasswordTextBox passwordBox = new PasswordTextBox();

	private final TextBox usernameBox = new TextBox();

	private final Label statusLabel = new Label();
	boolean connecting = false;

	public FriendsImportGtalk(FriendsImportCallback _callback) {
		super.add(getUserNamePasswordPage());
		this.fwcallback = _callback;

		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setSpacing(5);

		Button cancelButton = new Button(msg.button_cancel());
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				fwcallback.cancel();
			}
		});
		buttonPanel.add(cancelButton);

		connectButton = new Button(msg.button_connect());
		connectButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				connect();
			}
		});
		buttonPanel.add(connectButton);

		super.add(statusLabel);
		statusLabel.setVisible(false);

		super.add(buttonPanel);
		super.setCellHorizontalAlignment(buttonPanel, ALIGN_RIGHT);
	}

	public void onDetach() {
		OneSwarmGWT.removeFromUpdateTask(this);
		super.onDetach();
	}

	private void connect() {
		OneSwarmGWT.addToUpdateTask(this);
		connectButton.setEnabled(false);
		connectButton.setText(msg.add_friends_gtalk_connecting());
		connecting = true;
		String session = OneSwarmRPCClient.getSessionID();
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		AsyncCallback<FriendInfoLite[]> callback = new AsyncCallback<FriendInfoLite[]>() {
			public void onFailure(Throwable caught) {
				Window.alert(msg.add_friends_gtalk_error_unable_to_connect(caught.getMessage()));
				connectButton.setEnabled(true);
				connectButton.setText(msg.button_next());
				connecting = false;
			}

			public void onSuccess(FriendInfoLite[] result) {

				// set the permissions to default
				FriendInfoLite[] newFriends = result;
				for (FriendInfoLite f : newFriends) {
					f.setBlocked(false);
					f.setCanSeeFileList(true);
					f.setRequestFileList(true);
				}
				fwcallback.connectSuccesful(newFriends, true);

			}
		};
		service.getNewUsersFromXMPP(session, FRIEND_NETWORK_GTALK_NAME, usernameBox.getText(), passwordBox.getText().toCharArray(), computerNameBox.getText(), callback);
	}

	private VerticalPanel getUserNamePasswordPage() {
		VerticalPanel userPassPanel = new VerticalPanel();
		userPassPanel.setWidth("395px");

		Grid g = new Grid(3, 2);

		g.setWidget(0, 0, new Label(msg.add_friends_gtalk_computer_nick_label()));
		g.setWidget(0, 1, computerNameBox);

		g.setWidget(1, 0, new Label(msg.add_friends_gtalk_username_label()));
		g.setWidget(1, 1, usernameBox);

		g.setWidget(2, 0, new Label(msg.add_friends_gtalk_password_label()));
		g.setWidget(2, 1, passwordBox);
		usernameBox.setText("@gmail.com");
		passwordBox.addKeyUpHandler(enterListener);
		userPassPanel.add(g);

		String session = OneSwarmRPCClient.getSessionID();
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		AsyncCallback<String> callback = new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				System.out.println("error getting computer name " + caught.getMessage());
			}

			public void onSuccess(String result) {
				if (result != null) {
					if (computerNameBox.getText().equals("")) {
						computerNameBox.setText(result);
					}
					System.out.println("computer name=" + result);
				}
			}
		};
		service.getComputerName(session, callback);

		return userPassPanel;
	}

	public void update(int count) {
		if (connecting) {
			OneSwarmRPCClient.getService().getGtalkStatus(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {

				public void onFailure(Throwable caught) {
				}

				public void onSuccess(String result) {
					// OneSwarmGWT.log("got result: " + result);
					if (result != null) {
						String newStatus = msg.add_friends_gtalk_status(result);
						if (!statusLabel.getText().equals(newStatus)) {
							statusLabel.setText(newStatus);
						}
						statusLabel.setVisible(true);
					} else {
						statusLabel.setText("");
						statusLabel.setVisible(false);
					}
				}
			});
		} else {
			statusLabel.setVisible(false);
		}
	}
}