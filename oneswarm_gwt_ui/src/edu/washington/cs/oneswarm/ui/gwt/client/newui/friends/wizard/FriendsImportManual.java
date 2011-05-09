/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

class FriendsImportManual extends VerticalPanel {
    private static OSMessages msg = OneSwarmGWT.msg;

    // private final FriendsImportCallback fwcallback;

    private DecoratedTabPanel mTabs;
    private SingleManualFriendAddPanel mSingleFriendAdd = null;
    private MultipleManualFriendAddPanel mMultipleFriendAdd = null;

    private final static int WIDTH = FriendsImportWizard.WIDTH - 2;

    public FriendsImportManual(FriendsImportCallback _fwcallback) {
        // this.fwcallback = _fwcallback;
        super.setWidth(FriendsImportWizard.WIDTH + "px");

        mSingleFriendAdd = new SingleManualFriendAddPanel(_fwcallback);
        mMultipleFriendAdd = new MultipleManualFriendAddPanel(_fwcallback);

        mTabs = new DecoratedTabPanel();
        mTabs.addStyleName(TorrentDownloaderDialog.CSS_F2F_TABS);
        mTabs.add(mSingleFriendAdd, msg.add_friends_manual_tab_single_friends());
        mTabs.add(mMultipleFriendAdd, msg.add_friends_manual_tab_multiple_friends());
        mTabs.setWidth(WIDTH + "px");
        mTabs.setHeight("100%");
        mTabs.addSelectionHandler(new SelectionHandler<Integer>() {

            public void onSelection(SelectionEvent<Integer> event) {
                switch (event.getSelectedItem()) {
                case 0:
                    // mTabs.setWidth(WIDTH+"px");
                    // mainTabPanel.setWidth(WIDTH+"px");
                    break;

                case 1:
                    // mTabs.setWidth("350px");
                    // mainTabPanel.setWidth("350px");
                    // if( selectLabel.isAttached() ) {
                    // selectLabel.setWidth(mainTabPanel.getOffsetWidth()+"px");
                    // }
                    break;
                }
            }
        });

        mTabs.selectTab(0);

        this.add(mTabs);
        this.setCellVerticalAlignment(mTabs, VerticalPanel.ALIGN_TOP);

    }

}