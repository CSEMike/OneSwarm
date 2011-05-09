package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.OsgwtuiMain;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCallback;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.InvitationCreatePanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendsSidePanel extends VerticalPanel implements Updateable {
    private final FriendListPanel friendList;
    private static OSMessages msg = OneSwarmGWT.msg;

    public FriendsSidePanel() {
        super();

        friendList = new FriendListPanel();
        // Hyperlink friends = new Hyperlink("Friends", "Add/view friends");
        // friends.setStyleName("os-friendsLink");
        // friends.addClickListener(new ClickListener() {
        // public void onClick(Widget sender) {
        //
        // }
        // });
        // add(friends);
        setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);

        add(friendList);

        if (!OneSwarmGWT.isRemoteAccess()) {
            final FriendsSideInvitePanel invitePanel = new FriendsSideInvitePanel();
            invitePanel.addStyleName("os-friendInviteSide");
            add(invitePanel);
        }
    }

    public FriendListPanel getFriendListPanel() {
        return friendList;
    }

    public void update(int count) {
        friendList.update(count);
    }

    private class FriendsSideInvitePanel extends VerticalPanel {

        private final DisclosurePanel disclosurePanel = new DisclosurePanel(
                msg.friends_sidebar_invitation_header(), false);

        private final TextBox emailBox = new TextBox();

        public FriendsSideInvitePanel() {
            disclosurePanel.setOpen(true);
            disclosurePanel.addStyleName("os-friendInviteSidePanel");
            disclosurePanel.setWidth("100%");
            this.add(disclosurePanel);

            VerticalPanel contentPanel = new VerticalPanel();
            contentPanel.setWidth("100%");
            Label inviteLabel = new Label(msg.friends_sidebar_invitation_msg());
            inviteLabel.addStyleName("os-friendInviteSideContent");
            contentPanel.add(inviteLabel);

            final Button sendButton = new Button(msg.friends_sidebar_invitation_button());
            sendButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    sendIt();
                }
            });
            sendButton.addStyleName("os-friendInviteSideContent");
            sendButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
            sendButton.setEnabled(false);
            emailBox.setWidth("150px");
            emailBox.addStyleName("os-friendInviteSideContent");

            emailBox.addKeyUpHandler(new KeyUpHandler() {
                public void onKeyUp(KeyUpEvent event) {
                    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                        sendIt();
                    }

                    sendButton.setEnabled(emailBox.getText().length() > 0);
                }
            });

            contentPanel.add(emailBox);

            contentPanel.add(sendButton);

            disclosurePanel.add(contentPanel);
        }

        private void sendIt() {
            if (emailBox.getText().length() > 0) {
                final OneSwarmDialogBox dlg = new OneSwarmDialogBox();
                dlg.setWidth(FriendsImportWizard.WIDTH + "px");
                dlg.setWidget(new InvitationCreatePanel(emailBox.getText(),
                        new FriendsImportCallback() {
                            public void back() {
                                dlg.hide();
                            }

                            public void cancel() {
                                dlg.hide();
                            }

                            public void connectSuccesful(FriendInfoLite[] changes, boolean showSkip) {
                                dlg.hide();
                            }
                        }));
                dlg.setText("Send Invitation");
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setVisible(true);
                emailBox.setText("");
            }
        }
    }
}
