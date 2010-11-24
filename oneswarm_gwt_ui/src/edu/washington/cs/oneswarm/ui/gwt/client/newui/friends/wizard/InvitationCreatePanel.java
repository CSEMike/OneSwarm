package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInvitationLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public class InvitationCreatePanel extends FriendsImportWithBack {

	public static final String FRIEND_INVITE_NAME_HELP = msg.add_friends_invite_create_nickname_help();
	private static final long MAX_AGE = 14 * 24 * 60 * 60 * 1000;

	private final FriendsImportCallback friendWizardCallback;

	public InvitationCreatePanel(String email, FriendsImportCallback cb) {
		String nick = email;
		this.friendWizardCallback = cb;
		if (email.contains("@")) {
			nick = email.substring(0, email.indexOf('@'));
		}
		setFirstStep(new Step2(this, nick, SecurityLevel.NONE, email));
		setWidth(FriendsImportWizard.WIDTH + "px");
	}

	public InvitationCreatePanel(final FriendsImportCallback friendWizardCallback) {
		this.friendWizardCallback = friendWizardCallback;
		setFirstStep(new Step1(this));
	}

	private class Step1 extends FriendsImportWithBackStep implements KeyUpHandler {

		private final TextBox nickBox = new TextBox();
		private final InvitationCreatePanel parent;

		public Step1(InvitationCreatePanel parent) {
			this.parent = parent;

			/*********************************************************
			 * step 1: friends name
			 */
			Label selectLabel = new HTML(msg.add_friends_manual_step_1_type_nickname_HTML());
			selectLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
			selectLabel.setWidth(WIDTH + "px");
			this.add(selectLabel);

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
			parent.enableNextButton(false);
			// nickPanel.setCellHorizontalAlignment(nickBox,
			// HorizontalPanel.ALIGN_RIGHT);

			HelpButton nameHelp = new HelpButton(FRIEND_INVITE_NAME_HELP);
			nickPanel.add(nameHelp);
			nickPanel.setCellVerticalAlignment(nameHelp, VerticalPanel.ALIGN_MIDDLE);
			nickPanel.setCellHorizontalAlignment(nameHelp, HorizontalPanel.ALIGN_RIGHT);
			this.add(nickPanel);

			/*************************************************************
			 * step 2: security
			 */
			Label sercurityLabel = new HTML(msg.add_friends_invite_create_step2_security_HTML());
			sercurityLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
			sercurityLabel.setWidth(WIDTH + "px");
			// this.add(sercurityLabel);

			noSecurityButton = new RadioButtonWithHelp("security_buttons", msg.add_friends_invite_create_no_security(), false, msg.add_friends_invite_create_no_security_help_HTML());
			noSecurityButton.setValue(true);
			// this.add(noSecurityButton);

			pinCodeButton = new RadioButtonWithHelp("security_buttons", msg.add_friends_invite_create_pin_security(), false, msg.add_friends_invite_create_pin_security_help_HTML());
			// this.add(pinCodeButton);

			/************************************************************
			 * step 3: delivery
			 */
			HTML step3Label = new HTML(msg.add_friends_invite_create_step3_delivery_HTML());
			step3Label.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
			step3Label.setWidth(WIDTH + "px");
			this.add(step3Label);

			final VerticalPanel deliveryPanel = new VerticalPanel();
			deliveryPanel.setWidth(WIDTH + "px");
			/*
			 * the panel used to ask the user for their friends email
			 */
			final HorizontalPanel emailAdressPanel = new HorizontalPanel();
			emailAdressPanel.setVisible(false);
			emailAdressPanel.setSpacing(3);
			emailAdressPanel.setWidth(WIDTH + "px");
			Label sendToLabel = new Label(msg.add_friends_invite_create_delivery_send_to());
			emailAdressPanel.add(sendToLabel);
			emailAdressPanel.setCellVerticalAlignment(sendToLabel, VerticalPanel.ALIGN_MIDDLE);

			final String exampleEmail = msg.add_friends_invite_create_delivery_example_email();
			emailTextBox.setWidth("80%");
			emailTextBox.setText(exampleEmail);
			emailTextBox.addMouseDownHandler(new MouseDownHandler() {
				public void onMouseDown(MouseDownEvent event) {
					if (emailTextBox.getText().equals(exampleEmail)) {
						emailTextBox.setText("");
					}
				}
			});
			emailTextBox.addKeyUpHandler(this);
			emailAdressPanel.add(emailTextBox);

			RadioButtonWithHelp manual = new RadioButtonWithHelp("delivery_group", msg.add_friends_invite_create_delivery_manual(), false, msg.add_friends_invite_create_delivery_manual_help());
			manual.setValue(true);
			deliveryPanel.add(manual);
			manual.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					emailAdressPanel.setVisible(false);
				}
			});

			emailRadioButton = new RadioButtonWithHelp("delivery_group", msg.add_friends_invite_create_delivery_email(), false, msg.add_friends_invite_create_delivery_email_help());
			deliveryPanel.add(emailRadioButton);
			emailRadioButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					emailAdressPanel.setVisible(true);
				}
			});
			deliveryPanel.add(emailAdressPanel);
			deliveryPanel.setCellHorizontalAlignment(emailAdressPanel, HorizontalPanel.ALIGN_CENTER);
			this.add(deliveryPanel);

		}

		private final TextBox emailTextBox = new TextBox();
		private final RadioButtonWithHelp emailRadioButton;
		private final RadioButtonWithHelp noSecurityButton;
		private final RadioButtonWithHelp pinCodeButton;

		@Override
		public FriendsImportWithBackStep createNextPanel() {
			SecurityLevel securityLevel = null;
			if (noSecurityButton.getValue()) {
				securityLevel = SecurityLevel.NONE;
			} else if (pinCodeButton.getValue()) {
				securityLevel = SecurityLevel.PIN;
			} else {
				securityLevel = SecurityLevel.NONE;
			}
			return new Step2(parent, nickBox.getText(), securityLevel, emailRadioButton.getValue() ? emailTextBox.getText() : null);
		}

		@Override
		public String getNextButtonText() {
			return "Create Invite";
		}

		public void onKeyUp(KeyUpEvent event) {
			boolean nickOk = nickBox.getText().length() > 0;
			boolean emailOk = true;
			if (emailRadioButton.getValue() && emailTextBox.getText().length() <= 3) {
				emailOk = false;
			}
			parent.enableNextButton(nickOk && emailOk);
		}
	}

	private class Step2 extends FriendsImportWithBackStep {

		public Step2(final InvitationCreatePanel parent, final String nickname, SecurityLevel securityLevel, final String email) {

			final Label wait = new Label(msg.add_friends_invite_create_generating());
			parent.enableNextButton(false);
			this.add(wait);
			backButton.setVisible(false);
			OneSwarmRPCClient.getService().createInvitation(OneSwarmRPCClient.getSessionID(), nickname, true, MAX_AGE, securityLevel, new AsyncCallback<FriendInvitationLite>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
					Window.alert("error: " + caught.getMessage());
				}

				public void onSuccess(final FriendInvitationLite invite) {
					Step2.this.remove(wait);
					final String key = invite.getKey();
					if (email != null) {
						OneSwarmRPCClient.getService().getComputerName(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {
							public void onFailure(Throwable caught) {
							}

							public void onSuccess(String nick) {
								parent.enableNextButton(true);
								mailtoImpl(buildMailto(email, key, nick));
								Step2.this.add(createEmailSendPanel(key));
							}
						});

					} else {
						Step2.this.add(createDisplayCodePanel(key));
					}
				}
			});

		}

		private native void mailtoImpl(String mailto) /*-{
			$wnd.location = mailto;
		}-*/;

		private VerticalPanel createDisplayCodePanel(String code) {
			VerticalPanel p = new VerticalPanel();

			HTML label = new HTML(msg.add_friends_invite_create_done_step4_send_HTML());
			label.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
			label.setWidth(WIDTH + "px");
			p.add(label);

			Label codeLabel = new Label("" + code);
			codeLabel.addStyleName("friend_invitation-code");
			codeLabel.addStyleName("friend_invitation-code-padding");
			codeLabel.setWidth(WIDTH + "px");
			p.add(codeLabel);

			return p;
		}

		private VerticalPanel createEmailSendPanel(String code) {
			VerticalPanel p = new VerticalPanel();

			HTML label = new HTML(msg.add_friends_invite_create_done_step4_email_HTML());
			label.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
			label.setWidth(WIDTH + "px");
			p.add(label);
			Label explanaition = new Label(msg.add_friends_invite_create_done_step4_email_notice());
			p.add(explanaition);

			Label codeLabel = new Label("" + code);
			codeLabel.addStyleName("friend_invitation-code");
			codeLabel.addStyleName("friend_invitation-code-padding");
			codeLabel.setWidth(WIDTH + "px");
			p.add(codeLabel);

			return p;
		}

		private String buildMailto(String email, String code, String nick) {

			String mailto = null;
			mailto = "mailto:" + email + "?subject=" + msg.add_friends_invite_email_subject() + "&body=" + URL.encode(getEmailBody(code, nick));
			return mailto;
		}

		private String getEmailBody(String code, String nick) {
			String text = msg.add_friends_invite_email_description();
			text += "\n" + "http://oneswarm.cs.washington.edu";
			text += "\n\n" + "================================================";
			text += "\n\n" + msg.add_friends_invite_email_invite_how();
			text += "\n\n" + msg.add_friends_invite_email_invite1_download();
			text += "\n" + msg.add_friends_invite_email_invite2_run();
			text += "\n" + msg.add_friends_invite_email_invite3_click_link();
			text += "\n" + URL.encode(OneSwarmConstants.ONESWARM_ENTRY_URL + "#" + OneSwarmConstants.FRIEND_INVITE_PREFIX + OneSwarmConstants.FRIEND_INVITE_CODE_PREFIX + code + ":" + OneSwarmConstants.FRIEND_INVITE_NICK_PREFIX + nick);
			text += "\n" + msg.add_friends_invite_email_invite4_redeem();
			return text;
		}

		@Override
		public FriendsImportWithBackStep createNextPanel() {

			return null;
		}

		@Override
		public String getNextButtonText() {
			return msg.button_done();
		}
	}

	private static String wrapInEquals(String s, int len) {
		boolean front = true;
		String r = new String(s);
		while (r.length() < len) {
			if (front) {
				r = "=" + r;
			} else {
				r = r + "=";
			}
			front = !front;
		}

		return r;
	}

	private static String getWrappedInviteCode(String code) {
		String invitationCode = "Invitation Code";
		return wrapInEquals(invitationCode, code.length()) + "\n" + code + "\n" + wrapInEquals("", code.length());
	}

	static class RadioButtonWithHelp extends HorizontalPanel {
		private final RadioButton rb;

		public RadioButtonWithHelp(String buttonGroup, String label, boolean asHTML, String helpHTML) {
			rb = new RadioButton(buttonGroup, label, asHTML);
			this.add(rb);

			HelpButton helpButton = new HelpButton(helpHTML);
			this.add(helpButton);
			this.setCellHorizontalAlignment(helpButton, HorizontalPanel.ALIGN_RIGHT);

			this.setSpacing(3);
			this.setWidth("100%");
		}

		public void setValue(boolean checked) {
			rb.setValue(checked);
		}

		public boolean getValue() {
			return rb.getValue();
		}

		public void setEnabled(boolean enabled, String message) {
			rb.setEnabled(enabled);
			rb.setTitle(message);
		}

		public void setEnabled(boolean enabled) {
			setEnabled(enabled, "");
		}

		public void addClickHandler(ClickHandler ch) {
			rb.addClickHandler(ch);
		}
	}

	public void enableNextButton(boolean enable) {
		this.nextButton.setEnabled(enable);
	}

	protected FriendsImportWithBackStep createNextPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void onLastBack() {
		friendWizardCallback.back();
	}

	@Override
	protected void onLastNext() {
		friendWizardCallback.cancel();
	}
}
