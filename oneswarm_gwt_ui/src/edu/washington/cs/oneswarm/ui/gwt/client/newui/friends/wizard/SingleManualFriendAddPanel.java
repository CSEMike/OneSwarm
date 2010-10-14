package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.Date;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendPropertiesPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class SingleManualFriendAddPanel extends VerticalPanel implements KeyUpHandler, Updateable {
	private static OSMessages msg = OneSwarmGWT.msg;

	private final TextBox nickBox = new TextBox();
	private final TextArea friendsPublicKeyArea = new TextArea();
	private final TextArea ownPublicKeyArea = new TextArea();

	private final Button addButton = new Button(msg.add_friends_manual_button_add_friend());

	private final HTML problemLabel = new HTML();

	private final static int WIDTH = FriendsImportWizard.WIDTH - 2;

	private final static int PUB_KEY_WIDTH = FriendsImportWizard.WIDTH - 10;
	private final CheckBox limitedFriendBox = new CheckBox(msg.add_friends_manual_limited_friend());
	private FriendsImportCallback fwcallback;

	public SingleManualFriendAddPanel(final FriendsImportCallback fwcallback) {
		this.fwcallback = fwcallback;

		/*********************************************************
		 * step 1: friends name
		 */
		Label selectLabel = new HTML(msg.add_friends_manual_step_1_type_nickname_HTML());
		selectLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		super.add(selectLabel);

		// nickname
		HorizontalPanel nickPanel = new HorizontalPanel();
		nickPanel.setSpacing(3);
		nickPanel.setWidth(WIDTH + "px");

		Label nickLabel = new Label(msg.friend_properties_nickname_label());
		nickLabel.setWidth("100%");
		nickPanel.add(nickLabel);
		nickPanel.setCellVerticalAlignment(nickLabel, VerticalPanel.ALIGN_MIDDLE);
		nickPanel.setCellWidth(nickLabel, "95px");

		nickPanel.add(nickBox);
		nickBox.setWidth("100%");
		nickBox.addKeyUpHandler(this);
		// nickPanel.setCellHorizontalAlignment(nickBox,
		// HorizontalPanel.ALIGN_RIGHT);

		HelpButton nameHelp = new HelpButton(msg.friend_properties_nickname_help());
		nickPanel.add(nameHelp);
		nickPanel.setCellVerticalAlignment(nameHelp, VerticalPanel.ALIGN_MIDDLE);
		nickPanel.setCellHorizontalAlignment(nameHelp, HorizontalPanel.ALIGN_RIGHT);
		super.add(nickPanel);

		/*************************************************************
		 * step 2: send own public key
		 */
		Label ownLabel = new HTML(msg.add_friends_manual_step_2_send_key_HTML());
		ownLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
		ownLabel.setWidth(WIDTH + "px");
		super.add(ownLabel);

		ownPublicKeyArea.setWidth(PUB_KEY_WIDTH + "px");
		ownPublicKeyArea.setVisibleLines(7);
		ownPublicKeyArea.setReadOnly(true);
		ownPublicKeyArea.addStyleName("os-add_friend_publickey_text");
		super.add(ownPublicKeyArea);

		/*************************************************************
		 * step 3: send own public key
		 */
		Label publicKeyLabel = new HTML(msg.add_friends_manual_step_3_paste_key_HTML());
		publicKeyLabel.setWidth(WIDTH + "px");
		super.add(publicKeyLabel);
		publicKeyLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);

		friendsPublicKeyArea.setVisibleLines(7);
		friendsPublicKeyArea.setWidth(PUB_KEY_WIDTH + "px");
		friendsPublicKeyArea.addKeyUpHandler(this);
		friendsPublicKeyArea.addStyleName("os-add_friend_publickey_text");
		super.add(friendsPublicKeyArea);
		/*
		 * add the problem label
		 */
		problemLabel.setVisible(false);
		super.add(problemLabel);

		/*********************************************************
		 * step 4, extra settings
		 */
		Label limitedLabel = new HTML(msg.add_friends_manual_step_4_options_HTML());
		limitedLabel.setWidth(WIDTH + "px");
		super.add(limitedLabel);
		limitedLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);

		limitedFriendBox.setValue(true);
		HorizontalPanel limitedPanel = new HorizontalPanel();
		limitedPanel.setSpacing(10);
		limitedPanel.add(limitedFriendBox);
		limitedPanel.setCellVerticalAlignment(limitedFriendBox, HorizontalPanel.ALIGN_MIDDLE);

		HelpButton helpButton = new HelpButton(FriendPropertiesPanel.LIMITED_FRIEND);
		limitedPanel.add(helpButton);
		limitedPanel.setCellVerticalAlignment(helpButton, HorizontalPanel.ALIGN_MIDDLE);
		limitedPanel.setCellHorizontalAlignment(helpButton, HorizontalPanel.ALIGN_RIGHT);

		super.add(limitedPanel);

		/*
		 * buttons
		 */
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setSpacing(5);

		Button cancelButton = new Button(msg.button_back());
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				fwcallback.back();
			}
		});
		buttonPanel.add(cancelButton);

		addButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addFriend();
			}
		});
		addButton.setEnabled(false);
		buttonPanel.add(addButton);
		super.add(buttonPanel);
		super.setCellHorizontalAlignment(buttonPanel, ALIGN_RIGHT);

		update();

		OneSwarmGWT.addToUpdateTask(this);
	}

	public void onChange(Object sender) {
		if (sender.equals(friendsPublicKeyArea)) {
			if (friendsPublicKeyArea.getSelectionLength() == 0) {
				wrap(friendsPublicKeyArea);
			}
		}
		checkKey();
	}

	private void checkKey() {
		if (friendsPublicKeyArea.getText().length() > 0) {
			if (nickBox.getText().length() == 0) {
				problemLabel.setHTML(msg.add_friends_manual_problem_nick_HTML());
				problemLabel.setVisible(true);
				return;
			}
			String session = OneSwarmRPCClient.getSessionID();
			OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

			AsyncCallback<Void> callback = new AsyncCallback<Void>() {
				public void onSuccess(Void result) {
					addButton.setEnabled(true);
					problemLabel.setVisible(false);
				}

				public void onFailure(Throwable caught) {
					problemLabel.setHTML(msg.add_friends_manual_problem_key_HTML() + " " + caught.getMessage());
					problemLabel.setVisible(true);
					addButton.setEnabled(false);
				}
			};
			boolean testOnly = true;
			FriendInfoLite f = new FriendInfoLite();
			f.setPublicKey(friendsPublicKeyArea.getText());
			f.setName(nickBox.getText());
			f.setSource("Manual");
			service.addFriend(session, f, testOnly, callback);

		}
	}

	public void update(int count) {
		if (friendsPublicKeyArea.getText().length() > 0) {
			onChange(friendsPublicKeyArea);
		}
	}

	protected void addFriend() {

		final FriendInfoLite friend = new FriendInfoLite();
		friend.setSource("Manual");
		friend.setBlocked(false);
		friend.setCanSeeFileList(!limitedFriendBox.getValue());
		friend.setPublicKey(friendsPublicKeyArea.getText());
		friend.setName(nickBox.getText());
		friend.setConnectionId(-1);
		friend.setDateAdded(new Date());

		String session = OneSwarmRPCClient.getSessionID();
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		AsyncCallback<Void> callback = new AsyncCallback<Void>() {
			public void onSuccess(Void result) {
				OneSwarmGWT.log("added friend " + result);
				// fwcallback.connectSuccesful(new FriendInfoLite[] { friend });
				/*
				 * skip the callback, we only add one friend at the time, and we
				 * already specified all settings
				 */
				// just call cancel to hide the popup
				fwcallback.cancel();
			}

			public void onFailure(Throwable caught) {
				// well, do nothing...
				Window.alert("Problem adding Friend: " + "\n" + caught.getMessage());
			}
		};
		service.addFriend(session, friend, false, callback);

	}

	private static void wrap(TextArea area) {
		String oldText = area.getText();
		int prevPos = area.getCursorPos();

		String text = oldText.replaceAll("\\s+", "");
		int pos = 0;
		int len = text.length();

		StringBuilder b = new StringBuilder();
		while (pos < len) {
			int maxlen = Math.min(45, text.length() - pos);
			b.append(text.substring(pos, pos + maxlen));
			b.append("\n");
			pos += maxlen;
		}
		boolean changed = false;

		String newText = b.toString();
		if (oldText.length() == newText.length()) {
			for (int i = 0; i < oldText.length(); i++) {
				if (oldText.charAt(i) != newText.charAt(i)) {
					changed = true;
				}
			}
		} else {
			changed = true;
		}
		if (changed) {
			area.setText(newText);
			area.setCursorPos(Math.min(newText.length(), prevPos));
		}
	}

	private void update() {
		String session = OneSwarmRPCClient.getSessionID();
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		AsyncCallback<String> callback = new AsyncCallback<String>() {
			public void onSuccess(String result) {
				ownPublicKeyArea.setText(result);
				wrap(ownPublicKeyArea);
				// System.out.println("got own public key, length: " +
				// result.length() + " / " + result);
			}

			public void onFailure(Throwable caught) {
				// well, do nothing...
				OneSwarmGWT.log("error " + caught.getMessage());
			}
		};
		service.getMyPublicKey(session, callback);
	}

	public void onDetach() {
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	public void onKeyUp(KeyUpEvent event) {
		onChange(event.getSource());
	}
}
