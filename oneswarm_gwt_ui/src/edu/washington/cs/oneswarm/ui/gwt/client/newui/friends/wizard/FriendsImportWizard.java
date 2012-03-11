package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendPropertiesTable;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendsImportWizard extends OneSwarmDialogBox {

    private static OSMessages msg = OneSwarmGWT.msg;

    public final static String FRIEND_SRC_COMMUNITY = msg.add_friends_wizard_src_community();
    // private final static String FRIEND_SRC_GTALK =
    // msg.add_friends_wizard_src_gtalk();
    private final static String FRIEND_SRC_LAN = msg.add_friends_wizard_src_lan();
    private final static String FRIEND_SRC_MANUAL = msg.add_friends_wizard_src_manual();
    private final static String FRIEND_SRC_INVITATIONS = msg.add_friends_wizard_src_invites();

    private final static String FRIENDS_SRC_DEFAULT = FRIEND_SRC_COMMUNITY;

    private static final int HEIGHT = 400;
    private final static String[] IMPORT_SOURCES = { FRIEND_SRC_COMMUNITY, FRIEND_SRC_LAN,
            FRIEND_SRC_MANUAL, FRIEND_SRC_INVITATIONS };
    private final static String[] IMPORT_SOURCES_KEYS = {
            FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME,
            FriendsImportLAN.FRIEND_NETWORK_LAN_NAME, "", "Invitation" };
    public static final int WIDTH = 400;
    public static final int BWIDTH = 405;
    private final ClickHandler cancelListener = new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
            hide();
        }
    };

    public FriendsImportCallback getCallBack() {
        return friendWizardCallback;
    }

    private List<RadioButton> friendSrcRb;

    private final FriendsImportCallback friendWizardCallback = new FriendsImportCallback() {

        CheckBox removal = null;

        @Override
        public void cancel() {
            hide();
        }

        @Override
        public void back() {
            panel.clear();
            panel.add(createInitalPanel());
        }

        @Override
        public void connectSuccesful(FriendInfoLite[] changes, boolean showSkip) {
            final FriendPropertiesTable friendPropertiesTable = new FriendPropertiesTable(changes,
                    true, showSkip);
            panel.clear();
            panel.add(friendPropertiesTable);

            final List<FriendInfoLite> friendsToDelete = new ArrayList<FriendInfoLite>();
            for (FriendInfoLite f : changes) {
                if (f.getStatus() == FriendInfoLite.STATUS_TO_BE_DELETED) {
                    friendsToDelete.add(f);
                }
            }

            if (friendsToDelete.size() > 0) {
                removal = new CheckBox(msg.add_friends_community_remove(friendsToDelete.size()));
                panel.add(removal);
            }

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setSpacing(3);
            panel.insert(buttonPanel, 0);
            panel.setCellHorizontalAlignment(buttonPanel, HorizontalPanel.ALIGN_LEFT);

            Button saveButton = new Button(msg.button_save());
            if (changes.length == 0 && friendsToDelete.size() == 0) {
                saveButton.setText(msg.button_dismiss());
            } else {
                Button cancelButton = new Button(msg.button_cancel());
                cancelButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        hide();
                    }
                });
                buttonPanel.add(cancelButton);
            }
            buttonPanel.add(saveButton);
            saveButton.getElement().setId("communitySaveAfterReceiveButton");

            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    if (friendPropertiesTable != null) {
                        boolean reallySkip = mSelectImportMethod.startsWith(FRIEND_SRC_COMMUNITY);
                        friendPropertiesTable.saveChanges(reallySkip);

                        if (removal != null) {
                            if (removal.getValue()) {
                                if (friendsToDelete.size() > 0) {
                                    OneSwarmRPCClient.getService().deleteFriends(
                                            OneSwarmRPCClient.getSessionID(),
                                            friendsToDelete.toArray(new FriendInfoLite[0]),
                                            new AsyncCallback<Void>() {
                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    caught.printStackTrace();
                                                }

                                                @Override
                                                public void onSuccess(Void result) {
                                                    System.out.println("deletes successful: "
                                                            + friendsToDelete.size());
                                                    friendsToDelete.clear();
                                                }
                                            });
                                }
                            }
                        }

                        hide();
                    }
                }
            });

            // Update the width to deal with the table size increase
            panel.setWidth("601px");
        }
    };;

    private final VerticalPanel panel = new VerticalPanel();
    private Map<String, Integer> newFriendRequestCounts = null;

    public FriendsImportWizard() {
        this((Map) null);
    }

    public FriendsImportWizard(String which) {
        // this((Map)null);
        super(false, true, true);

        sharedInit(null);

        if (which.equals(FRIEND_SRC_COMMUNITY)) {
            mSelectImportMethod = FRIEND_SRC_COMMUNITY;
            show_community_import(null);
        } else if (which.equals("preload")) {
            // two splits here.
            // no more code splitting, adds bugs...
            // wrappedInitialize(true);
            // show_community_import(null, true);
        } else {
            // panel.add(createInitalPanel());
            wrappedInitialize();
        }
    }

    /**
     * And the hacks keep on coming!
     */
    public FriendsImportWizard(CommunityRecord toAdd) {
        // this((Map)null);
        super(false, true, true);

        sharedInit(null);

        mSelectImportMethod = FRIEND_SRC_COMMUNITY;
        show_community_import(toAdd);
    }

    protected void sharedInit(Map<String, Integer> newFriendRequestCounts) {
        if (newFriendRequestCounts == null) {
            newFriendRequestCounts = new HashMap<String, Integer>();
        }
        this.newFriendRequestCounts = newFriendRequestCounts;

        OneSwarmGWT.registerImportWizard(this);
        super.setText(msg.add_friends_wizard_title());

        panel.setWidth(BWIDTH + "px");
        panel.setWidth(HEIGHT + "px");

        setWidget(panel);
    }

    public FriendsImportWizard(Map<String, Integer> newFriendRequestCounts) {
        super(false, true, true);

        sharedInit(newFriendRequestCounts);

        wrappedInitialize();
    }

    private void wrappedInitialize() {
        /*
         * need to create the callback or we get nullpointers when adding
         * community servers
         */
        // GWT.runAsync(new RunAsyncCallback(){
        // public void onFailure(Throwable reason) {
        // Window.alert("Failed to load friend import javascript: " +
        // reason.toString());
        // }
        //
        // public void onSuccess() {
        // if( preload ) {
        // return;
        // }

        panel.add(createInitalPanel());
        // }});
    }

    private String mSelectImportMethod = null;

    protected VerticalPanel createInitalPanel() {

        VerticalPanel p = new VerticalPanel();

        friendSrcRb = new LinkedList<RadioButton>();
        Label selectLabel = new Label(msg.add_friends_wizard_src_choose());
        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");
        p.add(selectLabel);
        p.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);

        for (int i = 0; i < IMPORT_SOURCES.length; i++) {
            String title = IMPORT_SOURCES[i];
            Integer newFriendCount = newFriendRequestCounts.get(IMPORT_SOURCES_KEYS[i]);
            if (newFriendCount != null && newFriendCount > 0) {
                title += " (<b>" + newFriendCount + "</b>)";
            }
            RadioButton rb = new RadioButton("friendSrcGroup", title, true);
            p.add(rb);
            friendSrcRb.add(rb);
            if (IMPORT_SOURCES[i].equals(mSelectImportMethod)) {
                rb.setValue(true);
            } else if (IMPORT_SOURCES[i].equals(FRIENDS_SRC_DEFAULT)) {
                rb.setValue(true);
            }
        }

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);

        Button cancelButton = new Button(msg.button_cancel());
        buttonPanel.add(cancelButton);
        cancelButton.addClickHandler(cancelListener);

        Button nextButton = new Button(msg.button_next());
        nextButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                mSelectImportMethod = null;
                for (RadioButton b : friendSrcRb) {
                    if (b.getValue()) {
                        mSelectImportMethod = b.getText();
                        break;
                    }
                }
                if (mSelectImportMethod.startsWith(FRIEND_SRC_COMMUNITY)) {
                    show_community_import(null);
                } else if (mSelectImportMethod.startsWith(FRIEND_SRC_LAN)) {
                    setText(msg.add_friends_wizard_title_local_network(getText()));
                    VerticalPanel lanP = new FriendsImportLAN(friendWizardCallback);
                    panel.clear();
                    panel.add(lanP);
                } else if (mSelectImportMethod.startsWith(FRIEND_SRC_MANUAL)) {
                    setText(msg.add_friends_wizard_title_manual(getText()));
                    VerticalPanel manP = new FriendsImportManual(friendWizardCallback);
                    panel.clear();
                    panel.add(manP);
                } else if (mSelectImportMethod.startsWith(FRIEND_SRC_INVITATIONS)) {
                    setText(msg.add_friends_wizard_title_invites(getText()));
                    VerticalPanel invP = new InvitationPanel(friendWizardCallback);
                    panel.clear();
                    panel.add(invP);
                }
            }
        });
        buttonPanel.add(nextButton);
        p.add(buttonPanel);
        p.setCellHorizontalAlignment(buttonPanel, HorizontalPanel.ALIGN_RIGHT);

        return p;
    }

    protected void show_community_import(final CommunityRecord toAdd) {

        setText(msg.add_friends_wizard_title_community(getText()));

        boolean isNotification = false;

        if (newFriendRequestCounts
                .containsKey(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME)) {
            isNotification = newFriendRequestCounts
                    .get(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME) > 0;
        }

        VerticalPanel communityP = new FriendsImportCommunityServer(friendWizardCallback,
                isNotification, toAdd);
        panel.clear();
        panel.add(communityP);
    }
}
