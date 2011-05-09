package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ComputerNameDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class FriendsImportLAN extends VerticalPanel implements Updateable {
    private static OSMessages msg = OneSwarmGWT.msg;

    public final static String CSS_BOLD_TEXT = OneSwarmCss.TEXT_BOLD;
    public final static String CSS_DISABLED_TEXT = "os-text_color_disabled";
    private static final int FIELD_LEN_DESC = 150;

    private static final int FIELD_LEN_IP = 120;
    private static final int FIELD_LEN_NAME = 150;

    public final static String FRIEND_NETWORK_LAN_NAME = "Lan";
    private Button addButton = new Button(msg.add_friends_lan_button_add_selected());
    private HashMap<String, LanFriendPanel> existingPanels = new HashMap<String, LanFriendPanel>();

    private int friendsAdded = 0;

    private FriendInfoLite[] friendsToAddArray = new FriendInfoLite[0];

    private final FriendsImportCallback fwcallback;

    private VerticalPanel hostsPanel = new VerticalPanel();

    private FriendInfoLite[] localUsers = new FriendInfoLite[0];

    private FriendInfoLite me = null;

    public FriendsImportLAN(FriendsImportCallback callback) {
        this.fwcallback = callback;

        Label myInfoLabel = new Label(msg.add_friends_lan_my_information());
        HorizontalPanel infoLabelPanel = new HorizontalPanel();
        myInfoLabel.addStyleName(OneSwarmDialogBox.CSS_DIALOG_HEADER);
        infoLabelPanel.add(myInfoLabel);
        myInfoLabel.setWidth(FriendsImportWizard.WIDTH + "px");
        super.add(myInfoLabel);
        // super.setCellWidth(infoLabelPanel, "100%");

        super.add(getMyInfoPanel());

        Label detectedLabel = new Label(msg.add_friends_lan_local_oneswarm_users());
        detectedLabel.addStyleName(OneSwarmDialogBox.CSS_DIALOG_HEADER);
        detectedLabel.setWidth(FriendsImportWizard.WIDTH + "px");
        super.add(detectedLabel);

        // HorizontalPanel hostsHeader = new HorizontalPanel();
        // Label nameLabel = new Label("OneSwarm name");
        // nameLabel.addStyleName(OneSwarmDialogBox.CSS_DIALOG_HEADER);
        // hostsHeader.add(nameLabel);
        // hostsHeader.setCellWidth(nameLabel, FIELD_LEN_NAME + "px");
        //
        // Label ipLabel = new Label("Location");
        // ipLabel.addStyleName(OneSwarmDialogBox.CSS_DIALOG_HEADER);
        // hostsHeader.add(ipLabel);
        // hostsHeader.setCellWidth(ipLabel, FIELD_LEN_IP + "px");
        // super.add(hostsHeader);

        super.add(hostsPanel);

        /*
         * buttons
         */
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);

        Button cancelButton = new Button(msg.button_back());
        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                fwcallback.back();
                for (LanFriendPanel panel : existingPanels.values()) {
                    final FriendInfoLite friendInfo = panel.getFriendInfo();
                    if (!panel.isChecked() && panel.isEnabled() && panel.isFriendRequest()) {
                        addToIgnoreList(friendInfo);
                    }
                }
                OneSwarmGWT.removeFromUpdateTask(FriendsImportLAN.this);
            }
        });
        buttonPanel.add(cancelButton);

        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                List<FriendInfoLite> friendsToAdd = new LinkedList<FriendInfoLite>();
                for (LanFriendPanel panel : existingPanels.values()) {
                    final FriendInfoLite friendInfo = panel.getFriendInfo();
                    if (panel.isChecked()) {
                        friendInfo.setBlocked(false);
                        friendInfo.setCanSeeFileList(true);
                        friendsToAdd.add(friendInfo);
                    } else if (!panel.isChecked() && panel.isEnabled()) {
                        addToIgnoreList(friendInfo);
                    }
                }
                friendsToAddArray = friendsToAdd.toArray(new FriendInfoLite[friendsToAdd.size()]);
                for (FriendInfoLite f : friendsToAddArray) {
                    addFriend(f);
                }
            }

        });
        buttonPanel.add(addButton);

        super.add(buttonPanel);
        super.setCellHorizontalAlignment(buttonPanel, ALIGN_RIGHT);
        update(0);
        OneSwarmGWT.addToUpdateTask(this);
    }

    public void onDetach() {
        super.onDetach();

        OneSwarmGWT.removeFromUpdateTask(this);
    }

    private void addFriend(final FriendInfoLite friend) {
        friend.setSource("LAN");
        friend.setBlocked(false);
        friend.setCanSeeFileList(true);
        friend.setRequestFileList(true);
        friend.setConnectionId(-1);

        friendsAdded++;

        if (friendsAdded == friendsToAddArray.length) {
            fwcallback.connectSuccesful(friendsToAddArray, false);
        }
        // String session = OneSwarmRPCClient.getSessionID();
        // OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
        // AsyncCallback<Void> callback = new AsyncCallback<Void>() {
        // public void onSuccess(Void result) {
        // friendsAdded++;
        //
        // if (friendsAdded == friendsToAddArray.length) {
        // fwcallback.connectSuccesful(friendsToAddArray);
        // }
        // }
        //
        // public void onFailure(Throwable caught) {
        // // well, do nothing...
        // OneSwarmGWT.log("error " + caught.getMessage());
        // }
        // };
        // try {
        // service.addFriend(session, friend, false, callback);
        // } catch (OneSwarmException e) {
        // e.printStackTrace();
        // }
    }

    private void addToIgnoreList(final FriendInfoLite friendInfo) {
        OneSwarmRPCClient.getService().addToIgnoreRequestList(OneSwarmRPCClient.getSessionID(),
                friendInfo, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        Window.alert("got error when denying friend request friend: "
                                + caught.getMessage());
                    }

                    public void onSuccess(Void result) {
                        OneSwarmGWT.log("ignoring future requests from: " + friendInfo.getName());
                    }
                });
    }

    private VerticalPanel getMyInfoPanel() {
        final VerticalPanel p = new VerticalPanel();
        HorizontalPanel namePanel = new HorizontalPanel();
        namePanel.setSpacing(2);
        Grid g = new Grid(2, 2);
        g.setWidth((2 * FIELD_LEN_NAME) + "px");
        p.add(g);
        g.setWidget(0, 0, new Label(msg.add_friends_lan_computer_nick_name()));

        final Label nameLabel = new Label(msg.loading());
        namePanel.add(nameLabel);
        g.setWidget(0, 1, namePanel);

        Anchor nameEditAnchor = new Anchor(" (" + msg.swarm_browser_change_nick_name() + ")");
        nameEditAnchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ComputerNameDialog dlg = new ComputerNameDialog(nameLabel.getText(), true,
                        new ComputerNameDialog.NameSetCallback() {
                            public void setName(String newName) {
                                nameLabel.setText(newName);
                            }
                        });

                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(0, dlg.getPopupTop() - 50));
                dlg.setVisible(true);
            }
        });
        namePanel.add(nameEditAnchor);

        g.setWidget(1, 0, new Label(msg.loading()));
        final Label ipLabel = new Label(msg.loading());
        g.setWidget(1, 1, ipLabel);

        OneSwarmRPCClient.getService().getSelf(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<FriendInfoLite>() {
                    public void onFailure(Throwable caught) {
                        Window.alert("got error when denying friend request friend: "
                                + caught.getMessage());
                    }

                    public void onSuccess(FriendInfoLite result) {
                        me = result;
                        ipLabel.setText(result.getLastConnectIp());
                        nameLabel.setText(result.getName());
                    }
                });
        return p;
    }

    private void update() {

        for (LanFriendPanel p : existingPanels.values()) {
            p.clearUpdated();
        }
        for (FriendInfoLite f : localUsers) {
            // don't add self
            if (me == null || !me.getPublicKey().equals(f.getPublicKey())) {
                LanFriendPanel lanPanel = existingPanels.get(f.getPublicKey());
                if (lanPanel == null) {
                    lanPanel = new LanFriendPanel(f);
                    hostsPanel.add(lanPanel);
                    existingPanels.put(f.getPublicKey(), lanPanel);
                }
                lanPanel.update(f);
            }
        }

        boolean anyChecked = false;
        List<LanFriendPanel> toRemove = new LinkedList<LanFriendPanel>();
        for (LanFriendPanel p : existingPanels.values()) {
            if (!p.isUpdated()) {
                toRemove.add(p);
            } else {
                if (p.isChecked()) {
                    anyChecked = true;
                }
            }
        }
        for (LanFriendPanel p : toRemove) {
            existingPanels.remove(p.getPublicKey());
            p.removeFromParent();
        }

        addButton.setEnabled(anyChecked);

    }

    public void update(int count) {

        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<FriendInfoLite[]> callback = new AsyncCallback<FriendInfoLite[]>() {
            public void onFailure(Throwable caught) {
                // well, do nothing...
                OneSwarmGWT.log("error " + caught.getMessage());
            }

            public void onSuccess(FriendInfoLite[] result) {
                if (result != null) {
                    localUsers = result;
                    update();
                } else {
                    System.out.println("got null");
                }
            }
        };
        service.getLanOneSwarmUsers(session, callback);

    }

    private class LanFriendPanel extends HorizontalPanel {

        public final String FRIEND_REQUEST = msg.add_friends_lan_friend_request();
        private final CheckBox checkBox = new CheckBox();
        private final Label descLabel;
        private final FriendInfoLite friendInfoLite;
        private final Label ipLabel;
        private boolean isFriend = false;
        private boolean updated = true;

        public LanFriendPanel(FriendInfoLite f) {
            super.setVisible(false);
            this.friendInfoLite = f;
            super.add(checkBox);
            super.setCellWidth(checkBox, FIELD_LEN_NAME + "px");
            checkBox.setText(f.getName());
            checkBox.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (checkBox.isEnabled()) {
                        addButton.setEnabled(true);
                    }
                }
            });

            // PublicKeyColorPanel publicKeyColorPanel = new
            // PublicKeyColorPanel(8, null);
            // publicKeyColorPanel.update(f.getPublicKey());
            // super.add(publicKeyColorPanel);
            // super.setCellHorizontalAlignment(publicKeyColorPanel,
            // HorizontalPanel.ALIGN_RIGHT);
            ipLabel = new Label(f.getLastConnectIp());
            super.add(ipLabel);
            super.setCellWidth(ipLabel, FIELD_LEN_IP + "px");

            descLabel = new Label("");
            super.add(descLabel);
            super.setCellWidth(descLabel, FIELD_LEN_DESC + "px");
            // super.setCellHorizontalAlignment(ipLabel,
            // HorizontalPanel.ALIGN_RIGHT);
            // super.setWidth("100%");
            checkBox.setEnabled(false);
            checkBox.setValue(false);

            ipLabel.addStyleName(CSS_DISABLED_TEXT);
            descLabel.addStyleName(CSS_DISABLED_TEXT);

        }

        public boolean isFriendRequest() {
            return descLabel.getText().equals(FRIEND_REQUEST);
        }

        public void clearUpdated() {
            updated = false;
        }

        public FriendInfoLite getFriendInfo() {
            return friendInfoLite;
        }

        public String getPublicKey() {
            return friendInfoLite.getPublicKey();
        }

        public boolean isChecked() {
            return checkBox.getValue();
        }

        public boolean isEnabled() {
            return checkBox.isEnabled();
        }

        public boolean isUpdated() {
            return updated;
        }

        public void update(final FriendInfoLite f) {
            if (!f.getPublicKey().equals(friendInfoLite.getPublicKey())) {
                throw new RuntimeException(
                        "tried to update LanFriendPanel, but publickey does no match");
            }
            if (!isFriend) {
                checkBox.setText(f.getName());
            }
            updated = true;

            final String session = OneSwarmRPCClient.getSessionID();
            final OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

            /*
             * check if we already are friends with the user, in that case mark
             * as grey
             */
            boolean testOnly = true;
            service.addFriend(session, f, testOnly, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    LanFriendPanel.super.setVisible(true);
                    // well, do nothing...

                    checkBox.setTitle(caught.getMessage());

                    service.getUpdatedFriendInfo(session, f, new AsyncCallback<FriendInfoLite>() {
                        public void onFailure(Throwable caught) {
                            // well, do nothing...
                            OneSwarmGWT.log("error " + caught.getMessage());
                        }

                        public void onSuccess(FriendInfoLite result) {
                            if (result != null) {
                                checkBox.setText(result.getName());
                                checkBox.setTitle(msg.add_friends_lan_already_friend());
                                descLabel.setText(msg.add_friends_lan_already_friend());
                                isFriend = true;
                            }
                        }
                    });
                }

                public void onSuccess(Void result) {
                    LanFriendPanel.super.setVisible(true);
                    checkBox.setEnabled(true);
                    ipLabel.removeStyleName(CSS_DISABLED_TEXT);
                    descLabel.removeStyleName(CSS_DISABLED_TEXT);

                    /*
                     * check if the user has tried to contact us, in that case
                     * mark as bold and add "friend request"
                     */

                    service.getDeniedIncomingConnections(session,
                            new AsyncCallback<HashMap<String, String>>() {
                                public void onFailure(Throwable caught) {
                                }

                                public void onSuccess(HashMap<String, String> result) {
                                    if (result.containsKey(friendInfoLite.getPublicKey())) {
                                        descLabel.setText(FRIEND_REQUEST);
                                        descLabel.addStyleName(CSS_BOLD_TEXT);
                                        checkBox.addStyleName(CSS_BOLD_TEXT);
                                        ipLabel.addStyleName(CSS_BOLD_TEXT);
                                    } else {
                                        descLabel.removeStyleName(CSS_BOLD_TEXT);
                                        checkBox.removeStyleName(CSS_BOLD_TEXT);
                                        ipLabel.removeStyleName(CSS_BOLD_TEXT);
                                    }
                                }
                            });
                }
            });

        }
    }
}
