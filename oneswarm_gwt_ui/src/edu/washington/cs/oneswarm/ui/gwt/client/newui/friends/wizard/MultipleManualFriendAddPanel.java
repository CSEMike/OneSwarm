package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.creation.CreateSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendPropertiesPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class MultipleManualFriendAddPanel extends VerticalPanel implements ClickHandler {
	private static OSMessages msg = OneSwarmGWT.msg;

	public static final int WIDTH = 530;
	private String mPasted;
	private HTML contextLabel;
	private TextBox nickBox;
	private Button nextButton;
	private Button skipButton;
	protected Map<Integer, String> mScannedKeys;
	protected Integer[] mScannedKeysByID;
	private boolean generateName = true;
	private TextArea bigArea;
	private int currKeyIndex = 0;
	private FriendsImportCallback callback;
	private Hyperlink moreUpLink;
	private Hyperlink moreDownLink;
	private int currAheadContext;
	private int currBehindContext;
	private CheckBox limitedFriendBox;
	private HTML headerLabel;

	public MultipleManualFriendAddPanel(FriendsImportCallback _fwcallback) {

		this.callback = _fwcallback;

		Label selectLabel = new Label(msg.add_friends_manual_multi_instructions());
		selectLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		add(selectLabel);

		bigArea = new TextArea();
		bigArea.setVisibleLines(20);
		bigArea.setWidth(WIDTH - 8 + "px");
		// bigArea.setHeight("400px");
		add(bigArea);

		final Button scanButton = new Button(msg.add_friends_manual_multi_button_scan());
		HorizontalPanel hp = new HorizontalPanel();
		hp.add(scanButton);
		hp.setSpacing(3);
		add(hp);
		this.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_RIGHT);

		scanButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				scanButton.setEnabled(false);
				OneSwarmRPCClient.getService().scanXMLForFriends(OneSwarmRPCClient.getSessionID(), bigArea.getText(), new AsyncCallback<FriendInfoLite[]>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(FriendInfoLite[] result) {
						if (result == null) {
							parseAsText();
						} else {
							callback.connectSuccesful(result, true);
						}
					}
				});
			}
		});
	}

	protected void parseAsText() {
		mScannedKeys = look_for_keys(bigArea.getText());

		if (mScannedKeys.size() == 0) {
			Window.alert(msg.add_friends_manual_multi_alert_none_found());
			return;
		}

		mScannedKeysByID = mScannedKeys.keySet().toArray(new Integer[0]);
		Arrays.sort(mScannedKeysByID);

		redisplayWithKeys();
	}

	protected void redisplayWithKeys() {
		this.clear();

		skipButton = new Button(msg.add_friends_manual_multi_button_skip());
		nextButton = new Button(mScannedKeys.size() > 1 ? msg.button_next() : msg.button_done());
		nickBox = new TextBox();
		limitedFriendBox = new CheckBox(msg.friend_properties_limited());

		skipButton.addClickHandler(this);
		nextButton.addClickHandler(this);

		skipButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		nextButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

		HorizontalPanel header = new HorizontalPanel();
		header.setWidth(WIDTH + "px");
		// header.setSpacing(3);
		Label label = new HTML(msg.add_friends_manual_multi_key_counter_HTML(1, mScannedKeys.size()));
		headerLabel = (HTML) label;
		header.add(label);
		header.setCellVerticalAlignment(label, ALIGN_MIDDLE);
		HorizontalPanel bb = new HorizontalPanel();
		bb.add(skipButton);
		bb.add(nextButton);
		header.addStyleName(CreateSwarmDialogBox.CSS_DIALOG_HEADER);

		HorizontalPanel nickPanel = new HorizontalPanel();
		nickPanel.setSpacing(3);
		label = new Label(msg.friend_properties_nickname_label());
		nickPanel.add(label);
		nickPanel.setCellVerticalAlignment(label, ALIGN_MIDDLE);
		nickPanel.add(nickBox);

		HorizontalPanel limitedPanel = new HorizontalPanel();
		limitedPanel.setSpacing(3);
		limitedFriendBox.setValue(false);
		limitedPanel.add(limitedFriendBox);
		limitedPanel.setCellVerticalAlignment(limitedFriendBox, HorizontalPanel.ALIGN_MIDDLE);

		HelpButton helpButton = new HelpButton(FriendPropertiesPanel.LIMITED_FRIEND);
		limitedPanel.add(helpButton);
		limitedPanel.setCellVerticalAlignment(helpButton, HorizontalPanel.ALIGN_MIDDLE);
		limitedPanel.setCellHorizontalAlignment(helpButton, HorizontalPanel.ALIGN_RIGHT);
		// limitedPanel.setWidth("100%");

		nickPanel.add(limitedPanel);
		// nickPanel.setWidth("100%");

		nickPanel.add(bb);
		nickPanel.setCellHorizontalAlignment(bb, ALIGN_RIGHT);
		nickPanel.setCellVerticalAlignment(bb, ALIGN_MIDDLE);

		moreUpLink = new Hyperlink(msg.add_friends_manual_multi_show_more(), "more");
		moreDownLink = new Hyperlink(msg.add_friends_manual_multi_show_more(), "more");
		moreUpLink.addClickHandler(this);
		moreDownLink.addClickHandler(this);
		contextLabel = new HTML("");
		contextLabel.setWidth(WIDTH + "px");

		contextLabel.setWordWrap(true);
		DOM.setStyleAttribute(contextLabel.getElement(), "word-wrap", "break-word");

		this.add(header);
		this.add(nickPanel);

		this.add(moreUpLink);
		this.add(contextLabel);
		this.add(moreDownLink);

		currBehindContext = currAheadContext = 128;
		selectKey(currKeyIndex);
	}

	protected void selectKey(int index) {

		headerLabel.setHTML(msg.add_friends_manual_multi_key_counter_HTML(index + 1, mScannedKeys.size()));

		int previousContext = this.currBehindContext;
		int nextContext = this.currAheadContext;

		Integer offset = mScannedKeysByID[index];
		String pasted = mPasted;

		int len = 0;
		for (int i = offset, valid = 0; valid < 216 && i < pasted.length(); i++, len++) {
			if (pasted.charAt(i) == '\n') {
				continue;
			}
			valid++;
		}

		pasted = pasted.substring(0, offset) + "<b>" + pasted.substring(offset, offset + len) + "</b>" + pasted.substring(offset + len, pasted.length());

		int begin = Math.max(offset - previousContext, 0);
		int end = Math.min(offset + len + nextContext, pasted.length());

		String context = pasted.substring(begin, end);

		context = context.replaceAll("\n", "<br>");

		contextLabel.setHTML(context);

		String keyStr = mScannedKeys.get(offset);

		if (generateName) {
			nickBox.setText("Friend_" + keyStr.hashCode());
			generateName = false;
		}
	}

	public Map<Integer, String> look_for_keys(String str) {
		Map<Integer, String> indices = new HashMap<Integer, String>();

		str = str.replaceAll("<", "");
		str = str.replaceAll(">", "");

		mPasted = str;

		int curr = 0;
		while (curr < str.length()) {
			int candidate = str.indexOf("MIG", curr);
			if (candidate == -1) {
				// System.out.println("no more prefixes, done scan");
				break;
			}
			String keyStr = "";
			for (int i = candidate; i < str.length(); i++, curr++) {
				if (str.charAt(i) == '\n') {
					// System.out.println("skipping newline");
					continue;
				}
				if (isValidBase64Char(str.charAt(i)) == false) {
					// System.out.println("invalid base64 char after: " +
					// keyStr);
					break;
				}
				keyStr += str.charAt(i);

				if (keyStr.length() == 216) {
					break;
				}
			}
			if (keyStr.length() == 216) {
				indices.put(candidate, keyStr);
				// System.out.println("good key: " + keyStr);
			} else {
				// System.out.println("bad length: " + keyStr.length() + " / " +
				// keyStr);
			}
		}

		return indices;
	}

	public static boolean isValidBase64Char(char inChar) {
		// Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='
		return Character.isLetterOrDigit(inChar) || inChar == '+' || inChar == '/' || inChar == '=';
	}

	public void onClick(ClickEvent event) {
		Object sender = event.getSource();
		if (sender.equals(nextButton)) {
			if (nickBox.getText().length() == 0) {
				Window.alert(msg.add_friends_manual_multi_alert_nickname_required());
				return;
			} else {

				final FriendInfoLite friend = new FriendInfoLite();
				friend.setSource("Manual");
				friend.setBlocked(false);
				friend.setCanSeeFileList(!limitedFriendBox.getValue());
				friend.setPublicKey(mScannedKeys.get(mScannedKeysByID[currKeyIndex]));
				friend.setName(nickBox.getText());
				friend.setConnectionId(-1);
				friend.setDateAdded(new Date());

				String session = OneSwarmRPCClient.getSessionID();
				OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

				nextButton.setEnabled(false);

				service.addFriend(session, friend, false, new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						Window.alert(msg.add_friends_manual_multi_alert_friend_add_failed(caught.toString()));
						nextButton.setEnabled(true);
					}

					public void onSuccess(Void result) {
						if (currKeyIndex + 1 == mScannedKeys.size()) {
							callback.cancel();
						} else {
							generateName = true;
							currBehindContext = currAheadContext = 128;
							selectKey(++currKeyIndex);
							nextButton.setEnabled(true);
						}
					}
				});

			}
		} else if (sender.equals(skipButton)) {
			generateName = true;
			if (currKeyIndex + 1 < mScannedKeys.size()) {
				currBehindContext = currAheadContext = 128;
				selectKey(++currKeyIndex);
			} else {
				callback.cancel();
			}
		} else if (sender.equals(moreUpLink)) {
			currBehindContext += 64;
			selectKey(currKeyIndex);
		} else if (sender.equals(moreDownLink)) {
			currAheadContext += 64;
			selectKey(currKeyIndex);
		}

		if (mScannedKeys != null) {
			if (currKeyIndex + 1 == mScannedKeys.size()) {
				nextButton.setText(msg.button_done());
			}
		} // if scanned keys != null
	}
}
