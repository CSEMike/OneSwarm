package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendListPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServersSidePanel extends VerticalPanel implements Updateable {

    private static OSMessages msg = OneSwarmGWT.msg;

    ServerPanel mSelectedServer = null;

    class ServerPanel extends FocusPanel {
        private static final int MAX_LABEL_NAME_LENGTH = 20;

        private boolean isSelected = false;
        private long lastClick = 0;

        private final ClickHandler clickListener = new ClickHandler() {
            public void onClick(ClickEvent event) {

                if (!isSelected) {
                    if (server.getSplash_path() == null) {
                        Window.alert(msg.community_servers_sidebar_no_files());
                        return;
                    }

                    ServerPanel.this.addStyleName(FriendListPanel.CSS_FRIEND_HIGHLIGHTED);
                    if (mSelectedServer != null) {
                        mSelectedServer.clearSelected();
                    }
                    mSelectedServer = ServerPanel.this;
                    setSelected();
                    EntireUIRoot.getRoot(CommunityServersSidePanel.this).serverFilterChanged();
                } else if ((System.currentTimeMillis() - lastClick) < SwarmsBrowser.DOUBLE_CLICK_THRESHOLD) {
                    Window.open(server.getBaseURL(), "_blank", null);
                }
                lastClick = System.currentTimeMillis();

            } // onClick()
        };

        private final HorizontalPanel mainPanel = new HorizontalPanel();
        private final Image statusImage = new Image(ImageConstants.ICON_FRIEND_LIMITED);
        private final HorizontalPanel imagePanel = new HorizontalPanel();
        private final AbsolutePanel labelPanel = new AbsolutePanel();
        private final Label nameLabel = new Label("");

        private CommunityRecord server;

        private final static int TOTAL_WIDTH = 170;
        private final static int STATUS_IMAGE = 12;
        private final static int NAME_LABEL_WIDTH = TOTAL_WIDTH - STATUS_IMAGE - 4;
        private final static int HEIGHT = 18;

        public ServerPanel(CommunityRecord server) {

            this.server = server;

            addStyleName(OneSwarmCss.CLICKABLE);

            mainPanel.setWidth(TOTAL_WIDTH + "px");

            statusImage.setHeight(STATUS_IMAGE + "px");
            statusImage.setWidth(STATUS_IMAGE + "px");
            imagePanel.add(statusImage);
            imagePanel.setHorizontalAlignment(ALIGN_CENTER);
            imagePanel.setWidth(STATUS_IMAGE + 2 + "px");

            mainPanel.add(imagePanel);
            mainPanel.setCellVerticalAlignment(imagePanel, HorizontalPanel.ALIGN_TOP);
            mainPanel.setCellHorizontalAlignment(imagePanel, HorizontalPanel.ALIGN_CENTER);
            statusImage.addClickHandler(clickListener);

            labelPanel.setHeight(HEIGHT + "px");

            labelPanel.setWidth(NAME_LABEL_WIDTH + "px");

            nameLabel.setWidth(NAME_LABEL_WIDTH + "px");
            nameLabel.setHeight(14 + "px");
            nameLabel.setText("test");
            labelPanel.add(nameLabel, 2, 0);

            nameLabel.addClickHandler(clickListener);

            mainPanel.add(labelPanel);

            super.add(mainPanel);

            refreshUI();
        }

        private void refreshUI() {
            String name = server.getServer_name() == null ? server.getUrl() : server
                    .getServer_name();
            String labelName;
            if (name.length() > MAX_LABEL_NAME_LENGTH) {
                labelName = name.substring(0, MAX_LABEL_NAME_LENGTH - 2) + "...";
            } else {
                labelName = name;
            }
            nameLabel.setText(labelName);
            nameLabel.setTitle(name);

            // System.out.println(statusImage.getUrl() + " / " +
            // server.getSplash_path());
            if (statusImage.getUrl().endsWith(ImageConstants.ICON_FRIEND_LIMITED)
                    && server.getSplash_path() != null) {
                statusImage.setUrl(ImageConstants.ICON_FRIEND_ONLINE);
            }
        }

        public void setSelected() {
            if (!isSelected) {
                ServerPanel.this.addStyleName(FriendListPanel.CSS_FRIEND_HIGHLIGHTED);
            }
            isSelected = true;
        }

        public void clearSelected() {
            if (isSelected) {
                ServerPanel.this.removeStyleName(FriendListPanel.CSS_FRIEND_HIGHLIGHTED);
                super.setFocus(false);
            }
            isSelected = false;
        }

        public CommunityRecord getRecord() {
            return server;
        }

        public void update(CommunityRecord po) {
            this.server = po;
            refreshUI();
        }
    }

    private final VerticalPanel serverListVP = new VerticalPanel();
    private final DisclosurePanel disclosurePanel = new DisclosurePanel(
            msg.community_servers_sidebar_header(), false);

    public CommunityServersSidePanel() {

        VerticalPanel contentPanel = new VerticalPanel();
        // add the panel that will contain the friends
        serverListVP.setWidth("100%");

        disclosurePanel.setOpen(true);
        disclosurePanel.addStyleName("os-friendList");

        MenuBar footerMenu = new MenuBar();
        footerMenu.addStyleName("os-friendListFooter");
        footerMenu.setWidth("100%");
        MenuItem addFriendItem = new MenuItem(msg.community_servers_sidebar_add(), new Command() {
            public void execute() {
                OneSwarmDialogBox dlg = new FriendsImportWizard(
                        FriendsImportWizard.FRIEND_SRC_COMMUNITY);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
                dlg.setVisible(true);
            }
        });

        addFriendItem.setStylePrimaryName("os-friendListFooterMenu");
        footerMenu.addItem(addFriendItem);
        addFriendItem.getElement().setId("addFriendItemLink");

        contentPanel.add(serverListVP);
        contentPanel.add(footerMenu);
        contentPanel.setCellHorizontalAlignment(footerMenu, HorizontalPanel.ALIGN_CENTER);

        disclosurePanel.add(contentPanel);

        this.add(disclosurePanel);

        OneSwarmGWT.addToUpdateTask(this);
    }

    long nextUpdateRPC = 0;

    public void update(int count) {
        if (System.currentTimeMillis() > nextUpdateRPC) {

            OneSwarmRPCClient.getService().getStringListParameterValue(
                    OneSwarmRPCClient.getSessionID(), "oneswarm.community.servers",
                    new AsyncCallback<ArrayList<String>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ArrayList<String> result) {
                            nextUpdateRPC = System.currentTimeMillis() + 5 * 1000;

                            if (result.size() / 5 == serverListVP.getWidgetCount()) {

                                // might need to update status
                                for (int i = 0; i < result.size() / 5; i++) {
                                    CommunityRecord rec = new CommunityRecord(result, i * 5);
                                    ServerPanel p = (ServerPanel) serverListVP.getWidget(i);
                                    p.update(rec);
                                }

                                return;
                            }

                            serverListVP.clear();

                            for (int i = 0; i < result.size() / 5; i++) {
                                CommunityRecord rec = new CommunityRecord(result, i * 5);
                                serverListVP.add(new ServerPanel(rec));
                            }
                        }
                    });

            nextUpdateRPC = System.currentTimeMillis() + 10 * 1000;
        }
    }

    public void clearSelectedServer() {
        if (mSelectedServer != null) {
            mSelectedServer.clearSelected();
            mSelectedServer = null;
        }
    }

    public CommunityRecord getSelectedServer() {
        if (mSelectedServer == null) {
            return null;
        }
        return mSelectedServer.getRecord();
    }

}
