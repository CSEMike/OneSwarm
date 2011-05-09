package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class InvitationPanel extends VerticalPanel {
    private static OSMessages msg = OneSwarmGWT.msg;

    private DecoratedTabPanel mTabs;
    private final static int WIDTH = FriendsImportWizard.WIDTH - 2;
    private InvitationCreatePanel friendsInvitationCreatePanel = null;
    private InvitationRedeemPanel friendsInvitationRedeemPanel = null;
    private InvitationViewPanel invitationViewCreatedPanel = null;
    private InvitationViewPanel invitationViewRedeemedPanel = null;

    public InvitationPanel(FriendsImportCallback _fwcallback) {
        mTabs = new DecoratedTabPanel();
        mTabs.addStyleName(TorrentDownloaderDialog.CSS_F2F_TABS);
        friendsInvitationRedeemPanel = new InvitationRedeemPanel(_fwcallback);
        mTabs.add(friendsInvitationRedeemPanel, msg.add_friends_invite_tab_redeem());

        friendsInvitationCreatePanel = new InvitationCreatePanel(_fwcallback);
        mTabs.add(friendsInvitationCreatePanel, msg.add_friends_invite_tab_create());

        invitationViewRedeemedPanel = new InvitationViewPanel(_fwcallback);
        mTabs.add(invitationViewRedeemedPanel, msg.add_friends_invite_tab_view());

        mTabs.setWidth(WIDTH + "px");
        mTabs.setHeight("100%");

        mTabs.selectTab(0);

        mTabs.addSelectionHandler(new SelectionHandler<Integer>() {
            public void onSelection(SelectionEvent<Integer> event) {
                if (event.getSelectedItem() == 3) {
                    invitationViewCreatedPanel.refresh();
                } else if (event.getSelectedItem() == 2) {
                    invitationViewRedeemedPanel.refresh();
                }
            }
        });

        this.add(mTabs);
        this.setCellVerticalAlignment(mTabs, VerticalPanel.ALIGN_TOP);
    }

}
