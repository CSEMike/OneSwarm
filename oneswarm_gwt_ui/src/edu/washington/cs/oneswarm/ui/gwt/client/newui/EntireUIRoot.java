package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.Date;
import java.util.HashMap;

import com.allen_sauer.gwt.dnd.client.DragEndEvent;
import com.allen_sauer.gwt.dnd.client.DragHandler;
import com.allen_sauer.gwt.dnd.client.DragStartEvent;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.VetoDragException;
import com.allen_sauer.gwt.dnd.client.drop.DropController;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.TagEditorDialog.TreeItemDropController;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCallback;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.InvitationRedeemPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class EntireUIRoot extends DockPanel {
    private final NavigationSidePanel navSidePanel;
    SwarmsBrowser swarmFileBrowser = null;
    private Header header;

    long mLastAction = 0;
    public final static String SEARCH_HISTORY_TOKEN = "search:";
    public final static String ADD_COMMUNITY_SERVER_TOKEN = "addcserver:";
    private static final String CSS_UI_ROOT = "os-ui_root";
    private static final String CSS_SEARCH_TABS = "os-search_tabs";
    private static final String CSS_SEARCH_TAB_ITEM = "os-search_tab_item";

    public static final String CSS_BROWSER_WARNING = "os-browser_warning";

    boolean hasShift = false;
    boolean hasAlt = false;
    private SimplePanel keyboardRecorder;

    HTML warningHTML = new HTML("");
    boolean mWarningVisible = false;

    boolean sticky_warning = false;

    private String uiVersion = null;

    ValueChangeHandler<String> historyChangeListener = new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {

            // String historyToken = event.getValue();
            /**
             * We can't use the value provided by the event because of a firefox
             * decoding bug (we rely on encoded responses to make parsing
             */
            String historyToken = getHistoryToken();

            OneSwarmGWT.log("History value changed: " + historyToken);

            if (historyToken.startsWith("cserver") == false
                    && swarmFileBrowser.isAttached() == false) {
                keyboardRecorder.clear();
                keyboardRecorder.setWidget(swarmFileBrowser);
                header.getFilterBar().setVisible(true);
            }

            actionOccurred();
            System.out.println("got history changed event: " + historyToken);
            boolean recognized = false;
            for (FileTypeFilter filter : FileTypeFilter.values()) {
                if (filter.history_state_name.equals(historyToken)) {
                    swarmFileBrowser.changeFilter(filter, false); // this will
                                                                  // clear
                    // the search
                    // bar
                    // logically. we
                    // do this in
                    // the UI below
                    recognized = true;
                }
            }

            if (historyToken.equals(NavigationSidePanel.HYPERLINK_LABEL_TRANSFERS)) {
                System.out.println("show transfers");
                swarmFileBrowser.showTransfers();
                recognized = true;
            } else if (historyToken.equals(NavigationSidePanel.HYPERLINK_LABEL_FRIENDS)) {
                System.out.println("show friends");
                swarmFileBrowser.showFriends();
                recognized = true;
            } else if (historyToken.startsWith("friend")) {
                // internal file browser code will recognize that we have a
                // friend
                // selected, this code will remove transfers (if selected)
                // and clear other state appropriate to the switch
                swarmFileBrowser.pageZero();
                swarmFileBrowser.changeFilter(FileTypeFilter.All, false);

                /**
                 * Select browse files to see these since the intent is clearly
                 * to view them
                 */
                if (searchTabs != null) {
                    searchTabs.selectTab(0);
                }

                recognized = true;
            } else if (historyToken.startsWith("cserver")) {

                keyboardRecorder.clear();
                header.getFilterBar().setVisible(false);

                CommunityRecord selectedServer = navSidePanel.getCommunityServersPanel()
                        .getSelectedServer();
                if (selectedServer == null) {
                    keyboardRecorder.add(new Label("No selected community server."));
                } else {

                    String base = selectedServer.getBaseURLSkipSSL();

                    final Frame frame = new Frame(base + "/?osclient="
                            + URL.encode(mFooter.getVersion()));
                    frame.setWidth("98%");

                    final int off = 75;

                    frame.setHeight((Window.getClientHeight() - off) + "px");

                    Window.addResizeHandler(new ResizeHandler() {
                        @Override
                        public void onResize(ResizeEvent event) {
                            frame.setHeight((Window.getClientHeight() - off) + "px");
                        }
                    });

                    keyboardRecorder.add(frame);

                    if (searchTabs != null) {
                        searchTabs.selectTab(0);
                    }

                    // HTML frame = new HTML("<iframe src=\"" +
                    // selectedServer.getBaseURL() +
                    // "\" width=\"100%\" height=\"100%\" frameborder=\"0\" vspace=\"0\" "
                    // +
                    // "hspace=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"yes\" noresize");
                    // keyboardRecorder.add(frame);
                }

                recognized = true;
            }

            if (historyToken.equals("addfriends")) {
                OneSwarmDialogBox dlg = new FriendsImportWizard(new HashMap<String, Integer>());
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
                dlg.setVisible(true);
            }

            if (historyToken.startsWith(OneSwarmConstants.FRIEND_INVITE_PREFIX)) {
                handleInviteParameters(URL.decode(historyToken));
            }

            if (historyToken.startsWith(ADD_COMMUNITY_SERVER_TOKEN)) {
                handleCommunityServerAdd(getHistoryToken());
            }

            if (historyToken.startsWith(SEARCH_HISTORY_TOKEN)) {
                String searchString = historyToken.substring(SEARCH_HISTORY_TOKEN.length());
                displaySearch(URL.decode(searchString));
            }

            if (recognized) {
                header.getFilterBar().clearSearchFieldText();
            }
        }
    };

    public void setWarning(String html) {
        warningHTML.setHTML(html);
        mWarningVisible = true;
        DOM.setStyleAttribute(warningHTML.getElement(), "visibility", "visible");
        sticky_warning = false;
    }

    public void clearWarning() {
        if (mWarningVisible && !sticky_warning) {
            DOM.setStyleAttribute(warningHTML.getElement(), "visibility", "hidden");
            mWarningVisible = false;
        }
    }

    private static boolean supportsKeyboardShortcuts() {
        // if (OneSwarmGWT.isIE()) {
        // return false;
        // }
        if (OneSwarmGWT.isWebKit()) {
            return false;
        }
        return true;
    }

    private final PickupDragController dragController;

    public PickupDragController getDragController() {
        return dragController;
    }

    public EntireUIRoot(boolean useDebug) {

        if (OneSwarmGWT.isIE()) {
            if (OneSwarmGWT.getIEVersion() < 8) {
                setWarning("<b>Note:</b> Old versions of Internet Explorer does not render OneSwarm properly. Please consider <a href=\"http://oneswarm.cs.washington.edu/faq.html#q4\">upgrading your browser</a> for a better experience.");
                sticky_warning = true;
            } else {
                setWarning("<b>Note:</b> OneSwarm support for Internet Explorer 8 is experimental, if you experience issues please consider <a href=\"http://oneswarm.cs.washington.edu/faq.html#q4\">switching browser.</a>");
                sticky_warning = false;
            }
        }
        // create a DragController to manage drag-n-drop actions
        // note: This creates an implicit DropController for the boundary panel
        dragController = new PickupDragController(RootPanel.get(), true);
        dragController.setBehaviorDragProxy(true);
        dragController.addDragHandler(new DragHandler() {

            @Override
            public void onPreviewDragStart(DragStartEvent event) throws VetoDragException {
            }

            @Override
            public void onPreviewDragEnd(DragEndEvent event) throws VetoDragException {
                DropController dropController = event.getContext().dropController;
                if (dropController == null || !(dropController instanceof TreeItemDropController)) {
                    throw new VetoDragException();
                }
            }

            @Override
            public void onDragStart(DragStartEvent event) {
            }

            @Override
            public void onDragEnd(DragEndEvent event) {
            }
        });
        addStyleName(CSS_UI_ROOT);

        swarmFileBrowser = new SwarmsBrowser(useDebug);

        if (OneSwarmGWT.isRemoteAccess()) {
            OneSwarmRPCClient.getService().getComputerName(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<String>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(String result) {
                            Window.setTitle("OneSwarm - " + result + " (remote access)");
                        }
                    });
        } else {
            OneSwarmRPCClient.getService().getComputerName(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<String>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(String result) {
                            Window.setTitle("OneSwarm - " + result);
                        }
                    });

        }
        navSidePanel = new NavigationSidePanel();
        /**
         * Compensate for ScrollTable resizing bug in FireFox. See:
         * http://code.google
         * .com/p/google-web-toolkit-incubator/issues/detail?id=43
         */
        // ResizableWidgetCollection.get().setResizeCheckingEnabled(false);
        Widget north = null;
        String urlParams = History.getToken();
        boolean focusSearchField = urlParams != null && urlParams.length() > 1;
        header = new Header(useDebug, focusSearchField);
        VerticalPanel notice = new VerticalPanel();
        warningHTML.setWidth("600px");
        warningHTML.addStyleName(CSS_BROWSER_WARNING);
        notice.add(warningHTML);
        notice.setCellHorizontalAlignment(warningHTML, HorizontalPanel.ALIGN_CENTER);
        notice.setWidth("100%");
        notice.add(header);
        north = notice;

        this.add(north, DockPanel.NORTH);
        this.add(navSidePanel, DockPanel.WEST);
        this.setCellWidth(navSidePanel, "200px");

        /**
         * This is a hack to get around the fact that keyboard state is not
         * exposed to proper click listeners. I'm not sure if this worse than
         * just never using click listeners when we care about getting shift
         * events...
         * 
         * Also, FocusPanel's in the current version of GWT cause problems with
         * WebKit-based browsers, so no keyboard shortcuts for them.
         */
        if (supportsKeyboardShortcuts()) {

            System.out.println("adding keyboard recorder");

            keyboardRecorder = new FocusPanel();
            ((FocusPanel) keyboardRecorder).addKeyPressHandler(new KeyPressHandler() {
                @Override
                public void onKeyPress(KeyPressEvent event) {

                    // for instance, when showing a community server
                    if (swarmFileBrowser.isAttached() == false) {
                        return;
                    }

                    hasShift = event.isShiftKeyDown();
                    hasAlt = event.isAltKeyDown();

                    System.out.println("key press: " + event.getCharCode() + " shift?: " + hasShift);

                    if (Character.toLowerCase(event.getCharCode()) == 'j') {
                        hasShift = false;
                        swarmFileBrowser.selectPreviousSwarm();
                    } else if (Character.toLowerCase(event.getCharCode()) == 'k') {
                        hasShift = false;
                        swarmFileBrowser.selectNextSwarm();
                    } else if (Character.toLowerCase(event.getCharCode()) == 'n') {
                        swarmFileBrowser.nextPage();
                    } else if (Character.toLowerCase(event.getCharCode()) == 'p') {
                        swarmFileBrowser.previousPage();
                    } else if (Character.toLowerCase(event.getCharCode()) == 'd') {
                        swarmFileBrowser.dispatchSwarmAction(Strings.SWARM_DELETE,
                                swarmFileBrowser.getSelectedSwarms());
                    } else if (event.getCharCode() == 13) { // enter
                        swarmFileBrowser.doubleClick();
                    } else if (event.getCharCode() == '/') {
                        header.focusSearch();
                    }
                }
            });
        } else {
            keyboardRecorder = new SimplePanel();
        }

        keyboardRecorder.setWidget(swarmFileBrowser);

        this.add(keyboardRecorder, DockPanel.EAST);
        // this.add(swarmFileBrowser, DockPanel.EAST);

        if (supportsKeyboardShortcuts()) {
            ((FocusPanel) keyboardRecorder).setFocus(true);
        }

        /**
         * compensate for the memory leaks typical of browsers by periodically
         * refreshing (if nothing is playing)
         */
        (new Timer() {
            @Override
            public void run() {
                long now = (new Date()).getTime();
                if (getPlayingVideo() == false && (mLastAction + 60 * 1000) < now) {
                    System.out.println("f5 refreshing... " + (new Date()));
                    // TODO: make the currently selected section persist across
                    // reloads (e.g., leaving open the transfers window seems
                    // like a common use case)
                    reload();
                } else {
                    System.out.println("skipped f5 refresh due to playing or recent action "
                            + (new Date()));
                }
            }
        }).scheduleRepeating(120 * 60 * 1000);

        (new Timer() {
            @Override
            public void run() {
                OneSwarmRPCClient.getService().ping(OneSwarmRPCClient.getSessionID(), uiVersion,
                        new AsyncCallback<String>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                if ("OneSwarm updated".equals(caught.getMessage())) {
                                    reload();
                                } else {
                                    setWarning("<b>Warning:</b> The OneSwarm client appears to have stopped. Please restart it.");
                                }
                            }

                            @Override
                            public void onSuccess(String result) {
                                uiVersion = result;
                                clearWarning();
                            }
                        });
            }
        }).scheduleRepeating(5 * 1000);

        History.addValueChangeHandler(historyChangeListener);

        // History.addHistoryListener(new HistoryListener() {
        // public void onHistoryChanged(String historyToken) {
        //
        // System.out.println("history tok: " + historyToken);
        //
        // if (historyToken.equals("addfriends")) {
        // OneSwarmDialogBox dlg = new FriendsImportWizard(new HashMap<String,
        // Integer>());
        // dlg.show();
        // dlg.setVisible(false);
        // dlg.center();
        // dlg.setVisible(true);
        // History.newItem("#");
        // } else if( historyToken.startsWith("id:") ) {
        // displaySearch(URL.decode(historyToken));
        // }
        // }
        // });
    }

    protected void handleCommunityServerAdd(String historyToken) {
        String str = historyToken.substring(ADD_COMMUNITY_SERVER_TOKEN.length());
        CommunityRecord rec = new CommunityRecord(str);
        OneSwarmDialogBox dlg = new FriendsImportWizard(rec);
        dlg.show();
        dlg.setVisible(false);
        dlg.center();
        dlg.setVisible(true);
    }

    private void handleInviteParameters(String historyToken) {
        String values = historyToken.replaceAll(OneSwarmConstants.FRIEND_INVITE_PREFIX, "");
        String[] split = values.split(":");
        String code = "";
        String nick = "";
        for (String s : split) {
            if (s.startsWith(OneSwarmConstants.FRIEND_INVITE_CODE_PREFIX)) {
                code = s.substring((OneSwarmConstants.FRIEND_INVITE_CODE_PREFIX.length()));
            } else if (s.startsWith(OneSwarmConstants.FRIEND_INVITE_NICK_PREFIX)) {
                nick = s.substring(OneSwarmConstants.FRIEND_INVITE_NICK_PREFIX.length());
            }
        }
        final OneSwarmDialogBox dlg = new OneSwarmDialogBox();
        dlg.setWidget(new InvitationRedeemPanel(new FriendsImportCallback() {
            @Override
            public void back() {
                dlg.hide();
            }

            @Override
            public void cancel() {
                dlg.hide();
            }

            @Override
            public void connectSuccesful(FriendInfoLite[] changes, boolean showSkip) {
                dlg.hide();
            }
        }, code, nick));
        dlg.setText("Redeem Invitation");
        dlg.show();
        dlg.setVisible(false);
        dlg.center();
        dlg.setVisible(true);
    }

    private native void reload() /*-{
                                 $wnd.location.reload();
                                 }-*/;

    /**
     * We include this function from Google's RSH (reallysimplehistory) to work
     * around a hash decoding bug in firefox. See:
     * https://bugzilla.mozilla.org/show_bug.cgi?id=378962
     * https://bugzilla.mozilla.org/show_bug.cgi?id=483304
     */
    private native String getHistoryToken() /*-{
                                            var r = $wnd.location.href;
                                            var i = r.indexOf("#");
                                            return (i >= 0
                                            ? r.substr(i+1)
                                            : ""
                                            );
                                            }-*/;

    // public void onHistoryChanged(String historyToken) {
    //
    // if (historyToken.equals("cserver") == false &&
    // swarmFileBrowser.isAttached() == false) {
    // keyboardRecorder.clear();
    // keyboardRecorder.setWidget(swarmFileBrowser);
    // header.getFilterBar().setVisible(true);
    // }
    //
    // actionOccurred();
    // System.out.println("got history changed event: " + historyToken);
    // boolean recognized = false;
    // for (FileTypeFilter filter : FileTypeFilter.values()) {
    // if (filter.history_state_name.equals(historyToken)) {
    // swarmFileBrowser.changeFilter(filter, false); // this will clear
    // // the search
    // // bar
    // // logically. we
    // // do this in
    // // the UI below
    // recognized = true;
    // }
    // }
    //
    // if (historyToken.equals(NavigationSidePanel.HYPERLINK_LABEL_TRANSFERS)) {
    // System.out.println("show transfers");
    // swarmFileBrowser.showTransfers();
    // recognized = true;
    // } else if
    // (historyToken.equals(NavigationSidePanel.HYPERLINK_LABEL_FRIENDS)) {
    // System.out.println("show friends");
    // swarmFileBrowser.showFriends();
    // recognized = true;
    // } else if (historyToken.equals("friend")) {
    // // internal file browser code will recognize that we have a friend
    // // selected, this code will remove transfers (if selected)
    // // and clear other state appropriate to the switch
    // swarmFileBrowser.pageZero();
    // swarmFileBrowser.changeFilter(FileTypeFilter.All, false);
    //
    // /**
    // * Select browse files to see these since the intent is clearly to
    // * view them
    // */
    // if (searchTabs != null) {
    // searchTabs.selectTab(0);
    // }
    //
    // recognized = true;
    // } else if (historyToken.equals("cserver")) {
    //
    // keyboardRecorder.clear();
    // header.getFilterBar().setVisible(false);
    //
    // CommunityRecord selectedServer =
    // navSidePanel.getCommunityServersPanel().getSelectedServer();
    // if (selectedServer == null) {
    // keyboardRecorder.add(new Label("No selected community server."));
    // } else {
    //
    // final Frame frame = new Frame(selectedServer.getBaseURL() + "?osclient="
    // + URL.encode(mFooter.getVersion()));
    // frame.setWidth("98%");
    //
    // final int off = 120;
    //
    // frame.setHeight((Window.getClientHeight() - off) + "px");
    //
    // Window.addResizeHandler(new ResizeHandler() {
    // public void onResize(ResizeEvent event) {
    // frame.setHeight((Window.getClientHeight() - off) + "px");
    // }
    // });
    //
    // keyboardRecorder.add(frame);
    //
    // // HTML frame = new HTML("<iframe src=\"" +
    // // selectedServer.getBaseURL() +
    // // "\" width=\"100%\" height=\"100%\" frameborder=\"0\" vspace=\"0\" "
    // // +
    // //
    // "hspace=\"0\" marginwidth=\"0\" marginheight=\"0\" scrolling=\"yes\" noresize");
    // // keyboardRecorder.add(frame);
    // }
    //
    // recognized = true;
    // }
    //
    // if (historyToken.startsWith(OneSwarmConstants.FRIEND_INVITE_PREFIX)) {
    // handleInviteParameters(URL.decode(historyToken));
    // }
    //
    // if( historyToken.startsWith(ADD_COMMUNITY_SERVER_TOKEN) ) {
    // handleCommunityServerAdd(historyToken);
    // }
    //
    // if (historyToken.startsWith(SEARCH_HISTORY_TOKEN)) {
    // String searchString =
    // historyToken.substring(SEARCH_HISTORY_TOKEN.length());
    // displaySearch(URL.decode(searchString));
    // }
    //
    // if (recognized) {
    // header.getFilterBar().clearSearchFieldText();
    // }
    // }

    /**
     * Indicates that something has changed on the back end that warrants
     * refreshing the current swarm list
     * 
     * TODO: might just signal the removals/additions explicitly instead of
     * needing to refresh the back end in case these refreshes (of all swarms)
     * start to take a long time. alternatively, we could do some paged-style
     * presentation
     */
    public void refreshSwarms() {
        System.out.println("refreshSwarms()");
        swarmFileBrowser.refreshActive(false);
    }

    public static EntireUIRoot getRoot(Widget inWidget) {
        while (inWidget != null) {
            if (inWidget instanceof EntireUIRoot) {
                return (EntireUIRoot) inWidget;
            }
            inWidget = inWidget.getParent();
        }
        return null;
    }

    public void filterTextChanged(String text) {
        swarmFileBrowser.refreshFromFilterText(text);
    }

    public void friendFilterChanged() {
        FriendInfoLite selectedFriend = navSidePanel.getFriendPanel().getFriendListPanel()
                .getSelectedFriend();
        if (selectedFriend != null) {
            navSidePanel.clearSelection();
            navSidePanel.getCommunityServersPanel().clearSelectedServer();
            History.newItem("friend-" + selectedFriend.getId());

        } else {
            // TODO: we should set the active filter to all files
        }
    }

    public void serverFilterChanged() {
        CommunityRecord selectedServer = navSidePanel.getCommunityServersPanel()
                .getSelectedServer();
        if (selectedServer != null) {
            navSidePanel.clearSelection();
            navSidePanel.getFriendPanel().getFriendListPanel().clearSelectedFriend();
            History.newItem("cserver-" + selectedServer.getBaseURL().hashCode());
        }
    }

    /**
     * hack attack
     */
    public FriendInfoLite getSelectedFriend() {
        return navSidePanel.getFriendPanel().getFriendListPanel().getSelectedFriend();
    }

    public void clearNonLocalSelections() {
        navSidePanel.getFriendPanel().getFriendListPanel().clearSelectedFriend();
        navSidePanel.getCommunityServersPanel().clearSelectedServer();
    }

    public void pageZero() {
        swarmFileBrowser.pageZero();

        if (searchTabs != null) {
            searchTabs.selectTab(0);
        }
    }

    boolean mPlayingVideo = false;

    public void setPlayingVideo(boolean inPlaying) {
        mPlayingVideo = inPlaying;
    }

    public boolean getPlayingVideo() {
        return mPlayingVideo;
    }

    public void actionOccurred() {
        mLastAction = (new Date()).getTime();
        // System.out.println("action: " + mLastAction);
    }

    DecoratedTabPanel searchTabs = null;
    private final String CSS_SEARCH_RESULTS_PANEL = "os-search_results_panel";

    public void displaySearch(final String keywords) {
        // swarmFileBrowser.newSearch(text);
        header.getFilterBar().clearSearchFieldText();
        /**
         * Two cases: either the tab bar is currently being shown or not. If
         * not, create it.
         */

        if (searchTabs == null) {
            keyboardRecorder.removeFromParent();

            searchTabs = new DecoratedTabPanel();
            searchTabs.add(keyboardRecorder, "Swarms");
            searchTabs.addStyleName(CSS_SEARCH_TABS);
            searchTabs.setWidth("100%");
            // searchTabs.setHeight("100%");

            // this.setCellWidth(searchTabs, "100%");
            // this.setCellHeight(searchTabs, "100%");

            this.add(searchTabs, DockPanel.EAST);
        }

        final HorizontalPanel closable = new HorizontalPanel();
        closable.addStyleName(CSS_SEARCH_TAB_ITEM);
        final Label searchLabel = new Label(StringTools.truncate(keywords, 15, true) + " (0)");

        Image closeImg = new Image(OneSwarmDialogBox.CLOSE_IMAGE_URL);
        closable.add(searchLabel);
        closable.setCellVerticalAlignment(searchLabel, VerticalPanel.ALIGN_MIDDLE);
        Image spacerImg = new Image("images/spacer.png");
        spacerImg.setWidth("5px");
        spacerImg.setHeight("5px");
        closable.add(spacerImg);
        closable.add(closeImg);
        closable.setCellVerticalAlignment(closeImg, VerticalPanel.ALIGN_MIDDLE);
        final VerticalPanel resultsWidget = new VerticalPanel();
        resultsWidget.addStyleName(CSS_SEARCH_RESULTS_PANEL);
        final HTML resultsCount = new HTML("0 results");
        resultsWidget.add(resultsCount);
        resultsWidget.setCellHorizontalAlignment(resultsCount, HorizontalPanel.ALIGN_CENTER);
        searchTabs.add(resultsWidget, closable);
        searchTabs.selectTab(searchTabs.getWidgetIndex(resultsWidget));

        closeImg.addClickListener(new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                if (searchTabs.getTabBar().getSelectedTab() == searchTabs
                        .getWidgetIndex(resultsWidget)) {
                    searchTabs.selectTab(searchTabs.getTabBar().getSelectedTab() - 1);
                }
                searchTabs.remove(resultsWidget);

                /*
                 * Remove the tab bar if the only tab remaining is the files tab
                 */
                if (searchTabs.getTabBar().getTabCount() == 1) {

                    searchTabs.removeFromParent();
                    keyboardRecorder.removeFromParent();

                    EntireUIRoot.this.add(keyboardRecorder, DockPanel.EAST);

                    searchTabs = null;
                }
            }
        });

        /**
         * Now that the UI is set up, do the actual search.
         */
        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<Integer> callback = new AsyncCallback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                OneSwarmGWT.log("sending search, id=" + result);
                // resultsWidget.clear();
                resultsWidget.insert(new F2FSearchQueryWithResults(keywords, EntireUIRoot.this,
                        result, resultsCount, new F2FSearchQueryWithResults.ResultsCallback() {
                            int oldCount = 0;

                            @Override
                            public void updateCount(int count) {
                                if (count != oldCount) {
                                    searchLabel.setText(StringTools.truncate(keywords, 15, true)
                                            + " (" + count + ")");
                                    oldCount = count;
                                }
                            }
                        }), 0);
            }

            @Override
            public void onFailure(Throwable caught) {
                // well, do nothing...
                OneSwarmGWT.log("error " + caught.getMessage());
                caught.printStackTrace();
                resultsWidget.add(new Label("Error: " + caught.toString()));
                // searchField.setText(caught.getMessage());
            }
        };
        service.sendSearch(session, keywords, callback);
        header.focusSearch();
    }

    public void startChat(final FriendInfoLite selectedFriend) {
        if (selectedFriend != null) {
            if (selectedFriend.isSupportsChat() == false
                    && selectedFriend.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                Window.alert("This user's OneSwarm client does not support chat (or has disabled it).");
                return;
            }

            if (selectedFriend.isAllowChat() == false
                    && selectedFriend.getStatus() == FriendInfoLite.STATUS_ONLINE) {
                Window.alert("You have disabled chat for this friend.");
                return;
            }
        }

        final FriendInfoLite[] allFriends = navSidePanel.getFriendPanel().getFriendListPanel()
                .getAllFriends();

        boolean openNewWindow = true;
        if (ChatDialog.showing()) {
            if (!ChatDialog.tryHide()) {
                openNewWindow = false;
            }
        }

        if (openNewWindow) {
            ChatDialog dlg = new ChatDialog(allFriends,
                    selectedFriend != null ? selectedFriend.getPublicKey() : null, this);
        }
    }

    public void setUnreadChatCount(int total) {
        navSidePanel.setUnreadChatCount(total);
    }

    public TorrentInfo[] getSelectedSwarms() {
        return swarmFileBrowser.getSelectedSwarms();
    }

    Footer mFooter = new Footer();

    public Footer getFooter() {
        return mFooter;
    }
}
