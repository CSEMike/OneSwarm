package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.Date;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInvitationLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;

public class InvitationRedeemPanel extends FriendsImportWithBack {
    private final FriendsImportCallback fwCallback;
    private Step1 step1;

    public InvitationRedeemPanel(FriendsImportCallback _fwcallback, String inviteCode, String name) {
        this.fwCallback = _fwcallback;
        this.step1 = new Step1(this, inviteCode, name);
        setFirstStep(step1);

    }

    public InvitationRedeemPanel(FriendsImportCallback _fwcallback) {
        this(_fwcallback, "", "");
    }

    private class Step1 extends FriendsImportWithBackStep implements KeyUpHandler, MouseUpHandler,
            ChangeHandler, MouseOutHandler {
        private boolean enteredNickname = false;
        private boolean enteredCode = false;
        private TextBox nickBox;
        private TextBox codeBox;
        private InvitationRedeemPanel parent;
        private final HTML errorLabel = new HTML();

        public Step1(InvitationRedeemPanel parent, String code, String name) {
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

            nickBox = new TextBox();
            nickBox.setText(name);
            nickBox.addKeyUpHandler(this);
            nickBox.addMouseUpHandler(this);
            nickBox.addChangeHandler(this);
            nickBox.setWidth("100%");
            nickPanel.add(nickBox);

            HelpButton nameHelp = new HelpButton(msg.friend_properties_nickname_help());
            nickPanel.add(nameHelp);
            nickPanel.setCellVerticalAlignment(nameHelp, VerticalPanel.ALIGN_MIDDLE);
            nickPanel.setCellHorizontalAlignment(nameHelp, HorizontalPanel.ALIGN_RIGHT);
            this.add(nickPanel);

            /*************************************************************
             * step 2: Invitation code
             */
            Label codeLabel = new HTML(msg.add_friends_invite_redeem_step2_enter_code_HTML());
            codeLabel.addStyleName(FriendsImportWizard.CSS_DIALOG_HEADER);
            codeLabel.setWidth(WIDTH + "px");
            this.add(codeLabel);

            // nickname
            HorizontalPanel codePanel = new HorizontalPanel();
            codePanel.setSpacing(3);
            codePanel.setWidth(WIDTH + "px");

            // Label cLabel = new Label("Invitation Code:");
            // cLabel.setWidth("100%");
            // codePanel.add(cLabel);
            // codePanel.setCellVerticalAlignment(cLabel,
            // VerticalPanel.ALIGN_MIDDLE);
            // codePanel.setCellWidth(cLabel, "95px");

            codeBox = new TextBox();
            codeBox.setText(code);
            codeBox.addKeyUpHandler(this);
            codeBox.addChangeHandler(this);
            codeBox.addMouseUpHandler(this);
            codeBox.addMouseOutHandler(this);

            codeBox.setWidth("100%");
            codeBox.addStyleName("friend_invitation-code");
            codePanel.add(codeBox);

            HelpButton codeHelp = new HelpButton(msg.add_friends_invite_redeem_invite_code_help());
            codePanel.add(codeHelp);
            codePanel.setCellVerticalAlignment(codeHelp, VerticalPanel.ALIGN_MIDDLE);
            codePanel.setCellHorizontalAlignment(codeHelp, HorizontalPanel.ALIGN_RIGHT);
            this.add(codePanel);

            errorLabel.setVisible(false);
            this.add(errorLabel);

            parent.enableNextButton(false);
            checkText();
        }

        @Override
        public FriendsImportWithBackStep createNextPanel() {
            FriendInvitationLite i = createInvitation();
            boolean testOnly = false;

            OneSwarmRPCClient.getService().redeemInvitation(OneSwarmRPCClient.getSessionID(), i,
                    testOnly, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(Void result) {
                            fwCallback.back();
                        }
                    });

            return null;
        }

        private FriendInvitationLite createInvitation() {
            FriendInvitationLite i = new FriendInvitationLite();
            i.setCanSeeFileList(true);
            i.setKey(codeBox.getText());
            i.setName(nickBox.getText());
            i.setSecurityLevel(SecurityLevel.NONE);
            i.setCreatedDate(new Date().getTime());
            i.setCreatedLocally(false);
            return i;
        }

        public void checkText() {
            enteredNickname = (nickBox.getText().length() > 0);
            enteredCode = (codeBox.getText().length() > 0);
            if (enteredCode && enteredNickname) {
                FriendInvitationLite i = createInvitation();
                boolean testOnly = true;

                OneSwarmRPCClient.getService().redeemInvitation(OneSwarmRPCClient.getSessionID(),
                        i, testOnly, new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                parent.enableNextButton(false);
                                setErrorText(caught.getMessage());
                            }

                            public void onSuccess(Void result) {
                                parent.enableNextButton(true);
                                errorLabel.setVisible(false);
                                errorLabel.setText("");
                            }
                        });
            } else if (enteredCode) {
                setErrorText(msg.add_friends_invite_redeem_error_need_nickname());
            }
        }

        private void setErrorText(String text) {
            boolean visible = text != null && text.length() > 0;
            errorLabel.setVisible(visible);
            errorLabel.setHTML(msg.add_friends_invite_redeem_error_HTML() + text);

        }

        @Override
        public String getNextButtonText() {
            return msg.add_friends_invite_redeem_button();
        }

        public void onKeyUp(KeyUpEvent event) {
            checkText();
        }

        public void onMouseUp(MouseUpEvent event) {
            checkText();
        }

        public void onChange(ChangeEvent event) {
            checkText();
        }

        public void onMouseOut(MouseOutEvent event) {
            checkText();
        }
    }

    public void enableNextButton(boolean enable) {
        this.nextButton.setEnabled(enable);
    }

    @Override
    protected void onLastBack() {
        this.fwCallback.back();
    }

    protected void onLastNext() {
        this.fwCallback.cancel();
    }
}
