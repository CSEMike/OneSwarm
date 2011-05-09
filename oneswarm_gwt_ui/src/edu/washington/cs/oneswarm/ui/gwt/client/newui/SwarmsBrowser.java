package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.widgetideas.client.ProgressBar;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.UpdateSkippedFilesDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.creation.CreateSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.debug.DebugDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendsDetailsListPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.SwarmPermissionsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.publish.PublishSwarmsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.UISettingsPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details.TransferDetailsPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details.TransferDetailsTable;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendErrorReport;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PagedTorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class SwarmsBrowser extends VerticalPanel implements Updateable {

    class PreviewPanel extends SimplePanel {
    };

    private static OSMessages msg = OneSwarmGWT.msg;

    public static final int ICON_WIDTH = 128;
    public static final int ICON_HEIGHT = 128;

    public static final int CELL_WIDTH = 150; // also need to change this in
    // the CSS...
    public static final int DIRECTORY_SCROLL_WIDTH = 150;

    public static final String CSS_VIDEO_BROWSER = "os-video_browser";

    private static final String CSS_FILE_HIGHLIGHT = "os-file_highlighted";

    private static final String CSS_F2F_LABEL = "os-f2f_label";

    private static final String CSS_BROWSER_HEADER = "os-browser_header_buttons";

    private static final String CSS_FILE_ICON = "os-file_icon";

    private static final String CSS_VIDEO_PREVIEW_BG = "os-video_preview_bg";
    private static final String CSS_PREVIEW_TRANSFERRING = "os-transferring";

    private static final String CSS_FILTERED_COUNT = "os-filtered_count_label";

    private static final String COOKIE_SWARMS_PER_PAGE_INDEX = "swarmsPerPage";
    private static final String COOKIE_SWARMS_VIEW = "oneswarmView";

    private static final String COOKIE_SHOW_FRIENDS_SWARMS = "showFriendsSwarms";

    private static final int FNAME_BUFFER = 20;
    protected static final String ROOT_TAG_LABEL = msg.swarm_browser_tags_root();

    private int mSwarmsPerPage = 5;

    private long mNextWarningCheck = 0;
    private boolean mShowingFriends = false;
    /**
     * UI widgets and widgets reflecting data
     */
    List<GWTCanvas[]> mHighlighted = new ArrayList<GWTCanvas[]>();
    // private GWTCanvas[] mCurrentHighlights = new GWTCanvas[2];

    TransferDetailsTable transferDetailsTable = null;

    HTML allFilesFiltered = new HTML("<div id=\"" + OneSwarmCss.CSS_NOTHING_SHOWING + "\">"
            + msg.swarm_browser_no_files_search_friends_HMTL("") + "</a></div>");
    HTML noFilesHTML = new HTML("<div id=\"" + OneSwarmCss.CSS_NOTHING_SHOWING + "\">"
            + Strings.get(Strings.NO_FILES_MESSAGE) + "</div>");
    HTML noFriendsFilesHTML = new HTML("<div id=\"" + OneSwarmCss.CSS_NOTHING_SHOWING + "\">"
            + msg.swarm_browser_no_files_shared() + "</div>");
    Widget welcomePanel = new NewUserSetupPanel(this);
    /**
     * This is a total hack to compensate for the fact that GWT-FX RC2 doesn't
     * have any way to remove effects on a panel (or clear them) without a
     * reference to the Effect object itself.
     */
    Map<VerticalPanel, PreviewPanel> filePanel_to_fade = new HashMap<VerticalPanel, PreviewPanel>();

    final List<Button> headerButtons = new ArrayList<Button>(4);

    final FlowPanel filesFlowPanel = new FlowPanel();

    final Label filteredCount = new Label();

    final ListBox moreActions = new ListBox();

    final CheckBox mShowFriendsCheckbox = new CheckBox(msg.swarm_browser_show_friends_swarms());

    // We may need to change the text on this to reflect whether or not we can
    // actually play it
    Button playButton = null;
    Button debugButton = null;

    boolean mShowingTransfers = false;

    private int mPreviousFullRefreshTotalSwarmsCount;

    private TorrentInfo[] mSwarmsOrderedByDisplay = null;

    private Tree mDirectoryTree = new Tree();
    private ScrollPanel mDirectoryScroll = null; // so we can hide/show this
    private HorizontalPanel mFoldersAndFiles = null;
    private TreeItem mRootTreeItem = null;
    /**
     * Actual data
     */
    // TorrentList mTorrents = new TorrentList();
    List<TorrentInfo> mTransferringSwarms = new ArrayList<TorrentInfo>();
    List<ProgressBar> mProgressBars = new ArrayList<ProgressBar>();

    enum DoubleClickAction {
        BrowserPlay(Strings.SWARM_BROWSER_PLAY), SystemPlay(Strings.SWARM_DEFAULT_PLAY), OpenFolder(
                Strings.SWARM_REVEAL);

        String action;

        DoubleClickAction(String inAction) {
            action = inAction;
        }

        public String getAction() {
            return action;
        }
    };

    DoubleClickAction mDoubleClickAction = DoubleClickAction.BrowserPlay;

    /**
     * Glue
     */
    // Maintained by addFile() / removeFile()
    HashMap<TorrentInfo, PreviewPanel> torrent_to_panel = new HashMap<TorrentInfo, PreviewPanel>();

    // List<EffectPanel> mCurrentlyFiltered = new ArrayList<EffectPanel>();
    String mFilterPattern = "";

    final SwarmsBrowser this_shadow = this;

    long mLastFileRefresh = 0;

    /**
     * The TorrentInfo associated with the swarm most recently moused over. This
     * is distinct from the selected swarm since we need to support the popup
     * menu.
     */
    // TorrentInfo mMouseOverSwarm = null;
    // The TorrentInfo associated with the currently selected swarm
    List<TorrentInfo> mSelectedSwarms = new ArrayList<TorrentInfo>();
    private HorizontalPanel mPagingPanel;
    private ListBox swarmsPerPagePopup;
    private HorizontalPanel mSwarmsPerPagePanel;

    private boolean mUsingIconView = true;
    private boolean useDebug = false;
    private SimplePanel mTagsFilesSeparator;

    public SwarmsBrowser(boolean useDebug) {

        this.useDebug = useDebug;

        addStyleName(CSS_VIDEO_BROWSER);

        this.add(createHeaderButtons());
        mPagingPanel = createPagingNavigationPanel();
        this.add(mPagingPanel);

        // setWidth("100%");
        mFoldersAndFiles = new HorizontalPanel();
        mFoldersAndFiles.setWidth("100%");
        mDirectoryScroll = new ScrollPanel();
        // mDirectoryScroll.addStyleName(CSS_TAGS_TREE_SCROLL);

        mDirectoryScroll.setWidth(DIRECTORY_SCROLL_WIDTH + "px");
        mDirectoryScroll.add(mDirectoryTree);
        mFoldersAndFiles.add(mDirectoryScroll);
        mFoldersAndFiles.setCellWidth(mDirectoryScroll, DIRECTORY_SCROLL_WIDTH + "px");

        /**
         * Hack attack.
         */
        mTagsFilesSeparator = new SimplePanel();
        mTagsFilesSeparator.setWidth("1px");
        mFoldersAndFiles.add(mTagsFilesSeparator);
        com.google.gwt.user.client.Element td = DOM.getParent(mTagsFilesSeparator.getElement());
        DOM.setStyleAttribute(td, "borderRight", "1px solid #d2d2d2");
        /********/

        mFoldersAndFiles.add(filesFlowPanel);

        mDirectoryTree.addTreeListener(new TreeListener() {
            public void onTreeItemSelected(TreeItem item) {
                refreshActive(true);
            }

            public void onTreeItemStateChanged(TreeItem item) {
            }
        });

        // this.add(filesFlowPanel);
        this.add(mFoldersAndFiles);

        filteredCount.addStyleName(CSS_FILTERED_COUNT);
        updateFilteredCount(0);

        this.add(filteredCount);

        mSwarmsPerPagePanel = new HorizontalPanel();
        mSwarmsPerPagePanel.setSpacing(3);

        Label sppLabel = new Label(msg.swarm_browser_swarms_per_page());
        mSwarmsPerPagePanel.add(sppLabel);
        swarmsPerPagePopup = new ListBox();
        for (int i : new int[] { 10, 20, 35, 50, 100, 200, 500 }) {
            swarmsPerPagePopup.addItem(Integer.toString(i));
        }
        mSwarmsPerPagePanel.setCellVerticalAlignment(sppLabel, VerticalPanel.ALIGN_MIDDLE);
        mSwarmsPerPagePanel
                .setCellVerticalAlignment(swarmsPerPagePopup, VerticalPanel.ALIGN_MIDDLE);

        try {
            int spp = Integer.parseInt(Cookies.getCookie(COOKIE_SWARMS_PER_PAGE_INDEX));
            if (spp > 0 && spp < swarmsPerPagePopup.getItemCount()) {
                swarmsPerPagePopup.setSelectedIndex(spp);
            }
        } catch (Exception e) {
            swarmsPerPagePopup.setSelectedIndex(1);
        }

        try {
            if (Cookies.getCookie(COOKIE_SWARMS_VIEW).equals("list")) {
                mUsingIconView = false;
            }
        } catch (Exception e) {
            mUsingIconView = true;
        }

        mSwarmsPerPage = Integer.parseInt(swarmsPerPagePopup.getItemText(swarmsPerPagePopup
                .getSelectedIndex()));
        swarmsPerPagePopup.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                mSwarmsPerPage = Integer.parseInt(swarmsPerPagePopup.getItemText(swarmsPerPagePopup
                        .getSelectedIndex()));
                Cookies.setCookie(COOKIE_SWARMS_PER_PAGE_INDEX,
                        swarmsPerPagePopup.getSelectedIndex() + "",
                        OneSwarmConstants.TEN_YEARS_FROM_NOW);
                pageZero();
                changeFilter(mCurrentFileTypeFilter, true);
            }
        });

        mSwarmsPerPagePanel.add(swarmsPerPagePopup);
        this.add(mSwarmsPerPagePanel);
        this.setCellHorizontalAlignment(mSwarmsPerPagePanel, HorizontalPanel.ALIGN_CENTER);

        /*
         * save the "show friends swarms" state
         */
        try {
            boolean show = Boolean.parseBoolean(Cookies.getCookie(COOKIE_SHOW_FRIENDS_SWARMS));
            mShowFriendsCheckbox.setValue(show);
        } catch (Exception e) {
            mShowFriendsCheckbox.setValue(false);
        }
        this.add(mShowFriendsCheckbox);
        mShowFriendsCheckbox.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                Cookies.setCookie(COOKIE_SHOW_FRIENDS_SWARMS, mShowFriendsCheckbox.getValue() + "",
                        OneSwarmConstants.TEN_YEARS_FROM_NOW);
                refreshActive(true);
            }
        });

        sync_settings();

        /*
         * add click listener to noFriendsFilesLabel
         */
        allFilesFiltered.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (mFilterPattern.trim().length() < NavigationFilterBar.MIN_SEARCH_LENGTH) {
                    Window.alert(msg
                            .swarm_browser_warning_short_search(NavigationFilterBar.MIN_SEARCH_LENGTH));
                    return;
                }
                mEntireUIRoot.displaySearch(mFilterPattern);
            }
        });

        this.setCellHorizontalAlignment(mShowFriendsCheckbox, HorizontalPanel.ALIGN_CENTER);

        refreshActive(false);

        OneSwarmGWT.addToUpdateTask(this);

    }

    protected void onAttach() {
        super.onAttach();
        mEntireUIRoot = EntireUIRoot.getRoot(this);
    }

    ListBox mSortByList = new ListBox();
    ListBox mJumpList = new ListBox();

    Label mShowingLabel = new Label();

    public final static String SORT_BY_NAME = msg.swarm_browser_sort_name();
    public final static String SORT_BY_DATE = msg.swarm_browser_sort_date();
    public final static String SORT_BY_SIZE = msg.swarm_browser_sort_size();

    public final static int SORT_BY_NAME_ID = 0;
    public final static int SORT_BY_DATE_ID = 1;
    public final static int SORT_BY_SIZE_ID = 2;

    public static final long DOUBLE_CLICK_THRESHOLD = 750;

    private int mSortingMetric = SORT_BY_DATE_ID;
    private ChangeListener mJumpChangeListener = new ChangeListener() {
        public void onChange(Widget sender) {
            System.out.println("change listener, mJumpList.getSelectedIndex: "
                    + mJumpList.getSelectedIndex());
            changeFilter(SwarmsBrowser.this.mCurrentFileTypeFilter, true);
            updateShowingLabel();
        }
    };
    private ClickHandler nextPreviousPageLinkListener;
    private Hyperlink prevLink;
    private Hyperlink nextLink;
    private HTML mChatHTML;

    private HorizontalPanel createPagingNavigationPanel() {
        HorizontalPanel hp = new HorizontalPanel();

        HorizontalPanel lhs = new HorizontalPanel(), rhs = new HorizontalPanel(), center = new HorizontalPanel();

        hp.add(lhs);
        hp.add(center);
        hp.add(rhs);

        hp.setCellHorizontalAlignment(lhs, HorizontalPanel.ALIGN_LEFT);
        hp.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);
        hp.setCellHorizontalAlignment(center, HorizontalPanel.ALIGN_CENTER);

        hp.setCellVerticalAlignment(rhs, HorizontalPanel.ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(lhs, HorizontalPanel.ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(center, HorizontalPanel.ALIGN_MIDDLE);

        mSortByList.addItem(SORT_BY_DATE, "" + SORT_BY_DATE_ID);
        mSortByList.addItem(SORT_BY_NAME, "" + SORT_BY_NAME_ID);
        mSortByList.addItem(SORT_BY_SIZE, "" + SORT_BY_SIZE_ID);

        mSortByList.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                int oldMetric = mSortingMetric;
                mSortingMetric = Integer.parseInt(mSortByList.getValue(mSortByList
                        .getSelectedIndex()));

                System.out.println("sorting metric: " + mSortingMetric);

                if (mSortingMetric == oldMetric == false) {
                    System.out.println("changeFilter()");
                    pageZero(); // this calls changeFilter()
                    mJumpChangeListener.onChange(mJumpList); // force
                    // refiltering,
                    // etc.
                    // changeFilter(mCurrentFileTypeFilter, true); // this will
                    // resort
                }
            }
        });

        String which = Cookies.getCookie(COOKIE_SWARMS_VIEW);
        if (which == null) {
            which = "icons";
        } else if (!(which.equals("icons") || which.equals("list"))) {
            which = "icons";
        }
        final Hyperlink viewLink = new Hyperlink(
                which.equals("icons") ? Strings.get(Strings.LIST_VIEW)
                        : Strings.get(Strings.ICON_VIEW), "");
        viewLink.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (viewLink.getText().equals(Strings.get(Strings.LIST_VIEW))) {
                    viewLink.setText(Strings.get(Strings.ICON_VIEW));
                    mUsingIconView = false;
                    Cookies.setCookie(COOKIE_SWARMS_VIEW, "list",
                            OneSwarmConstants.TEN_YEARS_FROM_NOW);
                    refreshActive(false);
                } else {
                    viewLink.setText(Strings.get(Strings.LIST_VIEW));
                    mUsingIconView = true;
                    Cookies.setCookie(COOKIE_SWARMS_VIEW, "icons",
                            OneSwarmConstants.TEN_YEARS_FROM_NOW);
                    refreshActive(false);
                }
            }
        });

        mChatHTML = new HTML("| <a href=\"#\">" + msg.swarm_browser_chat() + "</a>");
        mChatHTML.setVisible(false);
        mChatHTML.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                mEntireUIRoot.startChat(mEntireUIRoot.getSelectedFriend());
            }
        });

        Label sortLabel = new Label(msg.swarm_browser_sort());
        lhs.add(sortLabel);
        lhs.add(mSortByList);
        Image spacer = new Image(GWT.getModuleBaseURL() + "images/spacer.png");
        spacer.setWidth("3px");
        spacer.setHeight("3px");
        lhs.add(spacer);
        lhs.add(viewLink);
        lhs.add(spacer);
        lhs.add(mChatHTML);
        // lhs.add(iconView);
        // lhs.add(listView);

        lhs.setCellVerticalAlignment(sortLabel, VerticalPanel.ALIGN_MIDDLE);
        lhs.setCellVerticalAlignment(mSortByList, VerticalPanel.ALIGN_MIDDLE);
        lhs.setCellVerticalAlignment(viewLink, VerticalPanel.ALIGN_MIDDLE);
        lhs.setCellVerticalAlignment(mChatHTML, VerticalPanel.ALIGN_MIDDLE);

        center.add(mShowingLabel);

        prevLink = new Hyperlink(msg.swarm_browser_show_prev_swarms(), "prevfiles");
        nextLink = new Hyperlink(msg.swarm_browser_show_next_swarms(), "morefiles");

        nextPreviousPageLinkListener = new ClickHandler() {
            public void onClick(ClickEvent event) {
                int incDec = 0;
                Object sender = event.getSource();
                if (sender.equals(prevLink)) {
                    if (mJumpList.getSelectedIndex() - 1 < 0)
                        return;

                    incDec = -1;
                } else if (sender.equals(nextLink)) {
                    if (mJumpList.getSelectedIndex() + 1 >= mJumpList.getItemCount())
                        return;

                    incDec = 1;
                } else {
                    System.err.println("unknown next/prev widget, shouldn't happen");
                    incDec = 0;
                }

                mJumpList.setSelectedIndex(mJumpList.getSelectedIndex() + incDec);

                System.out.println("selectedIndex: " + mJumpList.getSelectedIndex());

                changeFilter(SwarmsBrowser.this.mCurrentFileTypeFilter, true);
            }
        };

        prevLink.addClickHandler(nextPreviousPageLinkListener);
        nextLink.addClickHandler(nextPreviousPageLinkListener);
        rhs.add(prevLink);
        Label divideLabel = new Label("|");
        rhs.add(divideLabel);
        rhs.add(nextLink);
        rhs.setSpacing(3);

        rhs.add(mJumpList);

        rhs.setCellVerticalAlignment(nextLink, VerticalPanel.ALIGN_MIDDLE);
        rhs.setCellVerticalAlignment(prevLink, VerticalPanel.ALIGN_MIDDLE);
        rhs.setCellVerticalAlignment(divideLabel, VerticalPanel.ALIGN_MIDDLE);
        rhs.setCellVerticalAlignment(mJumpList, VerticalPanel.ALIGN_MIDDLE);

        hp.setWidth("100%");

        updateNavigationPanel();

        mJumpList.addChangeListener(mJumpChangeListener);

        return hp;
    }

    /**
     * changes the pages count in response to events (e.g. filtering changes)
     */
    private void updateNavigationPanel() {
        int current = Math.max(mJumpList.getSelectedIndex(), 0); // /
        // math.max
        // for
        // corner
        // case of
        // nothing
        // selected
        // at start
        int count = (int) Math.max(1,
                Math.ceil((double) mTotalSwarmsForFilterType / (double) mSwarmsPerPage));

        if (mJumpList.getItemCount() != count) {
            mJumpList.clear();

            for (int i = 0; i < count; i++) {
                mJumpList.addItem(Integer.toString(i));
            }

            mJumpList.setSelectedIndex(Math.min(current, mJumpList.getItemCount() - 1));
        }
        updateShowingLabel();
    }

    private void updateShowingLabel() {
        int curr = mJumpList.getSelectedIndex() * mSwarmsPerPage;
        String extra = "";
        if (mTotalSwarmSizeForFilterType != null) {
            extra += " (" + mTotalSwarmSizeForFilterType + ")";
        }
        mShowingLabel.setText(msg.swarm_browser_showing_swarms(curr,
                Math.min(curr + mSwarmsPerPage, mTotalSwarmsForFilterType),
                (mTotalSwarmsForFilterType)) + extra);
    }

    private void updateFilteredCount(int count) {
        filteredCount.setText(count + " filtered");
        if (count == 0) {
            DOM.setStyleAttribute(filteredCount.getElement(), "visibility", "hidden");
            DOM.setStyleAttribute(filteredCount.getElement(), "height", "0pt");
        } else {
            DOM.setStyleAttribute(filteredCount.getElement(), "visibility", "visible");
            DOM.setStyleAttribute(filteredCount.getElement(), "height", "10pt");
        }
    }

    EntireUIRoot mEntireUIRoot = null;

    /**
     * a hack to prevent using MouseListener on a FocusWidget on the
     * EntireUIRoot (which can use a lot of CPU)
     */
    public void propAction() {
        if (mEntireUIRoot == null) {
            mEntireUIRoot = mEntireUIRoot;
        }
        if (mEntireUIRoot != null) {
            mEntireUIRoot.actionOccurred();
        }
    }

    public void dispatchSwarmAction(String inActionKey, TorrentInfo[] inTorrents) {

        propAction();

        System.out.println("got swarm action: " + inActionKey);

        if (inActionKey.equals(Strings.SWARM_BROWSER_PLAY)) {
            System.out.println("got play action");
            if (inTorrents[0].isF2FOnly() == false) {

                boolean goForIt = true;
                if (inTorrents[0].getProgress() < 1000) {
                    if (inTorrents[0].getDownloadRate() == 0
                            || (inTorrents[0].getSeeders() + inTorrents[0].getLeechers() == 0)) {
                        goForIt = Window.confirm(msg.swarm_browser_warning_stalled_swarm());
                    } else if (inTorrents[0].getDownloadRate() < 50
                            && inTorrents[0].getProgress() < 900) {
                        goForIt = Window.confirm(msg.swarm_browser_warning_slow_swarm());
                    }
                }

                if (goForIt) {
                    startBrowserPlayer(inTorrents[0]);
                }
            } else {
                for (TorrentInfo inTorrent : inTorrents) {
                    TorrentDownloaderDialog dlg = new TorrentDownloaderDialog(mEntireUIRoot, null,
                            inTorrent.getF2F_ID(), inTorrent.getFileListLiteRepresentation(), true);
                    dlg.everythingChecked();

                    dlg.show();
                    dlg.setVisible(false);
                    dlg.center();
                    dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                    dlg.setVisible(true);
                }
            }
        } else if (inActionKey.equals(Strings.SWARM_DETAILS)) {

            for (TorrentInfo inTorrent : inTorrents) {
                UpdateSkippedFilesDialog dlg = new UpdateSkippedFilesDialog(mEntireUIRoot,
                        inTorrent);

                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                dlg.setVisible(true);
            }

        } else if (inActionKey.equals(Strings.SWARM_PUBLISH)) {

            TorrentInfo[] infos = getSelectedSwarms();
            if (infos != null && infos.length >= 1) {
                PublishSwarmsDialog dlg = new PublishSwarmsDialog(mEntireUIRoot, infos);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                dlg.setVisible(true);
            }

        } else if (inActionKey.equals(Strings.SWARM_COPY_MAGNET)) {

            TorrentInfo[] infos = getSelectedSwarms();
            if (infos != null && infos.length >= 1) {
                OneSwarmRPCClient.getService().copyTorrentInfoToMagnetLink(
                        OneSwarmRPCClient.getSessionID(), new String[] { infos[0].getTorrentID() },
                        new AsyncCallback<String>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                                OneSwarmGWT.log("Error: " + caught.getMessage());
                            }

                            public void onSuccess(String success) {
                                System.out.println("copy success: " + success);
                                if (OneSwarmGWT.isRemoteAccess()) {
                                    Window.alert("Link: " + success);
                                }
                            }
                        });
            }

        } else if (inActionKey.equals(Strings.ADD_SWARM_FILE)) {

            OneSwarmRPCClient.getService().selectFileOrDirectory(OneSwarmRPCClient.getSessionID(),
                    false, new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(String result) {

                            if (result == null) {
                                return; // cancelled
                            }

                            /**
                             * We enable everything here since it's pretty
                             * likely that what is being opened is a public
                             * torrent. (Even if it is an F2F torrent, it will
                             * have an invalid tracker URL)
                             */
                            ArrayList<PermissionsGroup> permitted = new ArrayList<PermissionsGroup>();
                            permitted.add(new PermissionsGroup(PermissionsGroup.ALL_FRIENDS));
                            permitted.add(new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET));

                            OneSwarmRPCClient.getService()
                                    .addDownloadFromLocalTorrentDefaultSaveLocation(
                                            OneSwarmRPCClient.getSessionID(), result, permitted,
                                            new AsyncCallback<Void>() {
                                                public void onFailure(Throwable caught) {
                                                    caught.printStackTrace();
                                                    // new
                                                    // ReportableErrorDialogBox(caught.getMessage(),
                                                    // false);
                                                    Window.alert(msg
                                                            .swarm_browser_warning_invalid_torrent());
                                                }

                                                public void onSuccess(Void result) {
                                                    ; // success
                                                }
                                            });
                        }
                    });

        } else if (inActionKey.equals(Strings.SWARM_DELETE)) {

            // keyboard shortcut doesn't check for this
            if (deleteButton.isEnabled() == false) {
                return;
            }

            /**
             * We ask for confirmation
             */
            DeleteTorrentDialog dlg = new DeleteTorrentDialog(getSelectedSwarms(), mEntireUIRoot);
            dlg.show();
            dlg.setVisible(false);
            dlg.center();
            dlg.setVisible(true);
        } else if (inActionKey.equals(Strings.SWARM_REVEAL)) {
            /*
             * check if remote access,
             */
            if (OneSwarmGWT.isRemoteAccess()) {
                /*
                 * open a new window and redirect to /files/hash/
                 */
                TorrentInfo[] swarms = getSelectedSwarms();
                for (TorrentInfo swarm : swarms) {
                    Window.open("/files/" + swarm.getTorrentID().replace("urn_btih_", "") + "/",
                            "_blank", "status=0,toolbar=0,menubar=0,location=0,resizable=0");
                }
            } else {
                /*
                 * otherwise open the normal folder view
                 */
                OneSwarmRPCClient.getService().revealSwarmInFinder(
                        OneSwarmRPCClient.getSessionID(), getSelectedSwarms(),
                        new AsyncCallback<ReportableException>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(ReportableException result) {
                                if (result != null) {
                                    new ReportableErrorDialogBox(result, false);
                                }
                            }
                        });
            }
        } else if (inActionKey.equals(Strings.SWARM_DEFAULT_PLAY)) {
            System.out.println("got default play event");
            OneSwarmRPCClient.getService().openFileDefaultApp(OneSwarmRPCClient.getSessionID(),
                    getSelectedSwarms(), new AsyncCallback<ReportableException>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ReportableException result) {
                            if (result != null) {
                                new ReportableErrorDialogBox(result, false);
                            }
                        }
                    });
        } else if (inActionKey.equals(Strings.MANAGE_PERMS)) {
            // SwarmDetailsDialog dlg = new
            // SwarmDetailsDialog(EntireUIRoot.getRoot(this),
            // torrent_to_panel.keySet().toArray(new TorrentInfo[0]));
            if (mPreviousFullRefreshTotalSwarmsCount == 0) {
                Window.alert(msg.swarm_browser_warning_no_visibility_with_no_swarms());
            } else {
                OneSwarmRPCClient.getService().getPagedAndFilteredSwarms(0, Integer.MAX_VALUE, "",
                        SORT_BY_NAME_ID, "all", false, Integer.MIN_VALUE, null,
                        new AsyncCallback<PagedTorrentInfo>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(PagedTorrentInfo result) {
                                TorrentInfo[] selectedarr = getSelectedSwarms();
                                TorrentInfo selected = null;
                                if (selectedarr != null) {
                                    if (selectedarr.length > 0) {
                                        selected = selectedarr[0];
                                    }
                                }

                                SwarmPermissionsDialog dlg = new SwarmPermissionsDialog(
                                        result.swarms, selected);
                                dlg.show();
                                dlg.setVisible(false);
                                dlg.center();
                                dlg.setPopupPosition(dlg.getPopupLeft(),
                                        Window.getScrollTop() + 100);
                                dlg.setVisible(true);
                            }
                        });
            }
        }
        // else if (inActionKey.equals(Strings.SEARCH_FRIENDS)) {
        // SearchDialog dlg = new SearchDialog(EntireUIRoot.getRoot(this),
        // null);
        // dlg.show();
        // dlg.setVisible(false);
        // dlg.center();
        // dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() +
        // 100);
        // dlg.setVisible(true);
        // }
        else if (inActionKey.equals(Strings.DEBUG)) {

            DebugDialog dlg = new DebugDialog();
            dlg.show();
            dlg.setVisible(false);
            dlg.center();
            dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 20);
            dlg.setVisible(true);

        } else if (inActionKey.equals(Strings.SWARM_STARTSTOP)) {
            System.out.println("stopstart action");
            TorrentInfo[] selected = getSelectedSwarms();
            final String[] selected_ids = new String[selected.length];
            if (selected[0].isStarted()) {
                for (int i = 0; i < selected.length; i++) {
                    selected[i].setStatus(TorrentInfo.ST_STOPPED);
                    selected_ids[i] = selected[i].getTorrentID();
                }
                OneSwarmRPCClient.getService().stopTorrent(OneSwarmRPCClient.getSessionID(),
                        selected_ids, new AsyncCallback<Boolean>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(Boolean result) {
                                System.out.println("stopped torrents: " + selected_ids.length);
                            }
                        });
            } else {
                for (int i = 0; i < selected.length; i++) {
                    selected[i].setStatus(TorrentInfo.ST_STOPPED);
                    selected_ids[i] = selected[i].getTorrentID();
                }
                OneSwarmRPCClient.getService().startTorrent(OneSwarmRPCClient.getSessionID(),
                        selected_ids, new AsyncCallback<Boolean>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(Boolean result) {
                                System.out.println("started torrents: " + selected_ids.length);
                            }
                        });
            }
        } // swarm_startstop
    }

    long mLastClickTime = 0;

    private void selectSwarm(TorrentInfo inTorrent) {
        selectSwarm(inTorrent, false);
    }

    private void selectSwarm(TorrentInfo inTorrent, boolean fromClick) {
        selectSwarm(inTorrent, fromClick, false);
    }

    private void selectSwarm(TorrentInfo inTorrent, boolean fromClick, boolean shiftDown) {
        if (inTorrent != null) {
            if (mSelectedSwarms.size() > 0) {
                if (mSelectedSwarms.contains(inTorrent) && fromClick == true
                        && (System.currentTimeMillis() - mLastClickTime) < DOUBLE_CLICK_THRESHOLD) {
                    if (inTorrent.isF2FOnly()) {
                        // really: download
                        dispatchSwarmAction(Strings.SWARM_BROWSER_PLAY, getSelectedSwarms());
                    } else {
                        doubleClick();
                    }
                    return;
                }
            }

            mLastClickTime = System.currentTimeMillis();
        }

        /**
         * If we aren't adding to the selection, remove all selections...
         */
        if ((shiftDown == false) && mHighlighted.size() > 0) {
            for (GWTCanvas[] highlight : mHighlighted) {
                if (highlight[0].getParent() != null) {
                    if (highlight[0].getParent().getParent() != null) {
                        highlight[0].getParent().getParent().removeStyleName(CSS_FILE_HIGHLIGHT);
                    }
                }

                for (GWTCanvas c : highlight) {
                    c.removeFromParent();
                }
            }
            mHighlighted.clear();
            mSelectedSwarms.clear();
        }

        if (inTorrent != null) {
            propAction();

            /**
             * This is brittle.
             */
            VerticalPanel fnamePanel = (VerticalPanel) torrent_to_panel.get(inTorrent).getWidget(); // this
            // is
            // icon_with_name
            // verticalpanel
            AbsolutePanel fnameAbsolute = (AbsolutePanel) fnamePanel.getWidget(1);
            Label fnameLabel = (Label) fnameAbsolute.getWidget(0);

            int height = ((MaxHeightWithTorrentInfoLabel) fnameLabel).getActualHeight();
            int width = fnameLabel.getOffsetWidth();

            GWTCanvas canvas = new GWTCanvas(width, height);
            Color fillColor = new Color(56, 117, 215);

            canvas.setFillStyle(fillColor);

            if (OneSwarmGWT.isIE()) {
                canvas.setBackgroundColor(fillColor);
            }
            canvas.setStrokeStyle(new Color(0, 255, 0));

            canvas.arc(5, height - 5, 5, (float) (Math.PI / 2), (float) (Math.PI), false);
            canvas.lineTo(0, 5);
            canvas.arc(5, 5, 5, (float) (Math.PI), (float) (3 * Math.PI / 2), false);
            canvas.lineTo(width - 5, 0);
            canvas.arc(width - 5, 5, 5, (float) (3 * Math.PI / 2), (float) (2 * Math.PI), false);
            canvas.lineTo(width, height - 5);
            canvas.arc(width - 5, height - 5, 5, (float) (2 * Math.PI), (float) (5 * Math.PI / 2),
                    false);
            canvas.lineTo(0, height);
            canvas.fill();

            fnameAbsolute.add(canvas, 0, 0);
            DOM.setStyleAttribute(canvas.getElement(), "zIndex", "-1");

            GWTCanvas[] highlight = new GWTCanvas[2];
            highlight[0] = canvas;
            highlight[0].getParent().getParent().addStyleName(CSS_FILE_HIGHLIGHT);

            /**
             * Next, for the background for the icon
             */
            height = ICON_HEIGHT;
            width = ICON_WIDTH + FNAME_BUFFER;
            canvas = new GWTCanvas(width, height);
            fillColor = new Color(172, 172, 172);
            canvas.setFillStyle(fillColor);

            if (OneSwarmGWT.isIE()) {
                canvas.setBackgroundColor(fillColor);
            }

            canvas.arc(5, height - 5, 5, (float) (Math.PI / 2), (float) (Math.PI), false);
            canvas.lineTo(0, 5);
            canvas.arc(5, 5, 5, (float) (Math.PI), (float) (3 * Math.PI / 2), false);
            canvas.lineTo(width - 5, 0);
            canvas.arc(width - 5, 5, 5, (float) (3 * Math.PI / 2), (float) (2 * Math.PI), false);
            canvas.lineTo(width, height - 5);
            canvas.arc(width - 5, height - 5, 5, (float) (2 * Math.PI), (float) (5 * Math.PI / 2),
                    false);
            canvas.lineTo(0, height);
            canvas.fill();
            AbsolutePanel overlay = (AbsolutePanel) fnamePanel.getWidget(0);
            overlay.add(canvas, 0, 0);
            DOM.setStyleAttribute(canvas.getElement(), "zIndex", "-1");

            highlight[1] = canvas;
            mHighlighted.add(highlight);

            if (currentMoveIcon != null) {
                currentMoveIcon.setVisible(false);
            }
            if (mHighlighted.size() == 1 && !inTorrent.isF2FOnly()) {
                for (int i = 0; i < overlay.getWidgetCount(); i++) {
                    Widget w = overlay.getWidget(i);
                    if (w instanceof Image && !w.isVisible()) {
                        w.setVisible(true);
                        currentMoveIcon = (Image) w;
                        break;
                    }
                }
            }
        }

        if (inTorrent != null) {
            mSelectedSwarms.add(inTorrent);
        } else {
            mSelectedSwarms.clear();
        }

        refreshHeaderButtons();
    }

    public void startBrowserPlayer(TorrentInfo inTorrent) {
        OneSwarmDialogBox dlg = new VideoDialog(inTorrent.getTorrentID(),
                inTorrent.getProgress() != 1000, "", inTorrent.getNumFiles(), mEntireUIRoot);
        dlg.show();
        dlg.setVisible(false);
        dlg.center();
        dlg.setPopupPosition(dlg.getAbsoluteLeft(), Window.getScrollTop() + 100);
        dlg.setVisible(true);
    }

    private FileTypeFilter mCurrentFileTypeFilter = FileTypeFilter.All;
    private long mNextPagedUpdate;
    private long mNextErrorPoll = 0;
    private int mTotalSwarmsForFilterType;
    private String mTotalSwarmSizeForFilterType = null;
    private Button deleteButton;
    private Button tagButton;

    protected void refreshActive(boolean pageZero) {

        if (pageZero) {
            mJumpList.setSelectedIndex(0);
        }

        mLastFileRefresh = System.currentTimeMillis();
        boolean showAllFriendsFiles = mShowFriendsCheckbox.getValue();
        // filesFlowPanel.clear();
        // torrent_to_panel.clear();

        FileTypeFilter fileType = mCurrentFileTypeFilter;

        int selectedFriendID = Integer.MIN_VALUE;
        EntireUIRoot root = mEntireUIRoot;
        if (root != null) {
            FriendInfoLite friend = mEntireUIRoot.getSelectedFriend();
            if (friend != null) {
                selectedFriendID = friend.getConnectionId();
                showAllFriendsFiles = false;
                fileType = FileTypeFilter.All;
                if (friend.isSupportsChat() && friend.isAllowChat()) {
                    mChatHTML.setVisible(true);
                } else {
                    mChatHTML.setVisible(false);
                }
            } else {
                mChatHTML.setVisible(false);
            }
        } else {
            // during the first refreshActive() this will be null since we are
            // not embedded. at this point, however, there's nothing selected
            // anyway
        }

        // System.out.println("paged and filtered call. selected friend id: " +
        // selectedFriendID);
        final int selected_shadow = selectedFriendID;

        OneSwarmRPCClient.getService().getPagedAndFilteredSwarms(
                mJumpList.getSelectedIndex(),
                Integer.parseInt(swarmsPerPagePopup.getItemText(swarmsPerPagePopup
                        .getSelectedIndex())), mFilterPattern, mSortingMetric, fileType.name(),
                showAllFriendsFiles, selectedFriendID, getSelectedTagPath(),
                new AsyncCallback<PagedTorrentInfo>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(PagedTorrentInfo result) {

                        /**
                         * Out of order RPC.
                         */
                        if (SwarmsBrowser.this.mShowingFriends
                                || SwarmsBrowser.this.mShowingTransfers) {
                            return;
                        }

                        filesFlowPanel.clear();
                        torrent_to_panel.clear();

                        mProgressBars.clear();
                        mTransferringSwarms.clear();

                        updateTags(result.tags, result.truncated_tags);

                        mTotalSwarmsForFilterType = result.total_swarms;
                        mPreviousFullRefreshTotalSwarmsCount = result.total_swarms;
                        mTotalSwarmSizeForFilterType = result.total_size;
                        mSwarmsOrderedByDisplay = result.swarms;

                        updateFilteredCount(result.filtered_count);
                        updateNavigationPanel();
                        boolean reselected = false;

                        if (mUsingIconView == false) {
                            final SwarmsListTable st = new SwarmsListTable(result.swarms,
                                    SwarmsBrowser.this);
                            filesFlowPanel.add(st);

                            final Button selectAllButton = new Button(msg.button_select_all());
                            selectAllButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
                            final Button selectNoneButton = new Button(msg.button_select_none());
                            selectNoneButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

                            filesFlowPanel.add(selectAllButton);
                            filesFlowPanel.add(selectNoneButton);

                            ClickHandler cl = new ClickHandler() {
                                public void onClick(ClickEvent event) {
                                    Object w = event.getSource();
                                    if (w.equals(selectAllButton)) {
                                        st.checkAll(true);
                                        refreshHeaderButtons();
                                    } else {
                                        st.checkAll(false);
                                        refreshHeaderButtons();
                                    }
                                }
                            };

                            selectAllButton.addClickHandler(cl);
                            selectNoneButton.addClickHandler(cl);

                            refreshHeaderButtons();
                        } else {
                            for (TorrentInfo t : result.swarms) {
                                if (mSelectedSwarms.contains(t)) {
                                    reselected = true;
                                }
                                addFileIcon(t, true);
                            }

                            if (!reselected) {
                                selectSwarm(null);
                            }
                        }

                        boolean filterEnabled = false;
                        if (mFilterPattern != null && mFilterPattern.length() > 0) {
                            filterEnabled = true;
                            allFilesFiltered.setHTML("<div id=\""
                                    + OneSwarmCss.CSS_NOTHING_SHOWING
                                    + "\">"
                                    + msg.swarm_browser_no_files_search_friends_HMTL(mFilterPattern)
                                    + "</a></div>");

                        }
                        if (result.total_swarms == 0 && result.by_type_count == 0
                                && mCurrentFileTypeFilter.equals(FileTypeFilter.All)
                                && selected_shadow == Integer.MIN_VALUE) {
                            if (filterEnabled) {
                                filesFlowPanel.add(allFilesFiltered);
                            } else {
                                filesFlowPanel.add(welcomePanel);
                            }
                        } else if (result.total_swarms == 0 && selected_shadow == Integer.MIN_VALUE) {
                            if (filterEnabled) {
                                filesFlowPanel.add(allFilesFiltered);
                            } else {
                                filesFlowPanel.add(noFilesHTML);
                            }

                        } else if (result.total_swarms == 0 && selected_shadow != Integer.MIN_VALUE) {
                            if (filterEnabled) {
                                filesFlowPanel.add(allFilesFiltered);
                            } else {
                                filesFlowPanel.add(noFriendsFilesHTML);
                            }
                        }

                        // System.out.println("got paged torrent result: total: "
                        // +
                        // mTotalSwarmsForFilterType + " / " +
                        // result.total_swarms + " "
                        // + result.filtered_count + " filtered bytype: " +
                        // result.by_type_count);
                    }
                });
    }

    private String getSelectedTagPath() {
        TreeItem selected = mDirectoryTree.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String path = reverse(selected, 0);
        return path;
    }

    protected void updateTags(FileTree tags, boolean truncated_tags) {
        TreeItem root = mRootTreeItem;

        /**
         * We need to do this carefully to avoid changing the expansion /
         * selection status of entries
         */
        List<String> expandedPaths = null;
        String selectedPath = null;
        if (root != null) {
            /**
             * to eliminate firefox flicker, check to make sure if the current
             * hierarchy is the same and only do the .clear() and repopulate if
             * changes occurred. (no changes is by far the common case)
             */
            if (changed(root, tags, 0) == false) {
                // System.out.println("no change, skipping tree update");
                return;
            }

            expandedPaths = getExpandedPaths(root);
            selectedPath = null;

            TreeItem selected = mDirectoryTree.getSelectedItem();
            if (selected != null) {
                selectedPath = ROOT_TAG_LABEL + "/" + reverse(selected, 0);
            }

            for (String expanded : expandedPaths) {
                System.out.println("expanded: " + expanded);
            }
            System.out.println("selected: " + selectedPath);
        }

        mDirectoryTree.clear();

        HTML rootTagLabel = new HTML(ROOT_TAG_LABEL);
        root = mDirectoryTree.addItem(ROOT_TAG_LABEL);
        if (truncated_tags) {
            rootTagLabel.setHTML(ROOT_TAG_LABEL + "<a href=\"#\" >(!)</a>");
            rootTagLabel.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    Window.alert(msg.swarm_browser_warning_tags_are_hidden());
                }
            });
        }
        mRootTreeItem = root;

        if (tags != null) {
            TagEditorDialog
                    .addChildren(root, tags.getChildren(), mEntireUIRoot.getDragController());
        } else {
            System.err.println("null tags!");
        }

        mRootTreeItem.setState(true);

        TreeItem curr = mRootTreeItem;
        if (expandedPaths != null) {

            for (String path : expandedPaths) {
                String[] toks = path.split("/");

                if (toks.length == 0) {
                    System.err.println("no toks on path! " + path);
                    continue;
                }

                if (toks[0].equals(curr.getText())) {
                    curr.setState(true);
                }

                for (int tokItr = 1; tokItr < toks.length; tokItr++) {
                    for (int cItr = 0; cItr < curr.getChildCount(); cItr++) {
                        if (toks[tokItr].equals(curr.getChild(cItr).getText())) {
                            curr = curr.getChild(cItr);
                            curr.setState(true);
                            break;
                        }
                    }
                }
            } // for over expandedPaths
        } else { // if( expandedPaths != null )
            // expand first level
            mRootTreeItem.setState(true);
        }

        if (selectedPath != null) {

            System.out.println("selected is: " + selectedPath);

            String[] toks = selectedPath.split("/");
            curr = mRootTreeItem;

            // corner case
            if (selectedPath.equals(mRootTreeItem.getText() + "/")) {
                mDirectoryTree.setSelectedItem(mRootTreeItem, false);
            } else {
                boolean good = false;
                for (int level = 1; level < toks.length; level++) {
                    good = false;
                    for (int cItr = 0; cItr < curr.getChildCount(); cItr++) {
                        if (curr.getChild(cItr).getText().equals(toks[level])) {
                            curr = curr.getChild(cItr);
                            good = true;
                        }
                    }
                    if (good == false) {
                        break;
                    }
                }
                if (good) {
                    mDirectoryTree.setSelectedItem(curr, false);
                } else {
                    refreshActive(true);
                }
            }

        }

        checkTagsPanelAttchment();
    }

    private void checkTagsPanelAttchment() {
        if (mRootTreeItem != null && mDirectoryTree != null) {
            if (mDirectoryTree.isAttached() && mRootTreeItem.getChildCount() == 0) {
                detachDirectoryScrollPanel();
            } else if (mRootTreeItem.getChildCount() > 0 && mDirectoryTree.isAttached() == false) {
                reattachDirectoryScrollPanel();
            }
        }
    }

    private boolean changed(TreeItem ti, FileTree ft, int level) {

        if (ti.getChildCount() != ft.getChildren().length) {
            return true;
        }

        if (level != 0) { // don't check name at level 0, just count
            if (ti.getText().equals(ft.getName()) == false) {
                return true;
            }
        }

        for (int i = 0; i < ti.getChildCount(); i++) {
            if (changed(ti.getChild(i), ft.getChildren()[i], level + 1)) {
                return true;
            }
        }
        return false;
    }

    private String reverse(TreeItem curr, int depth) {
        if (curr.getParentItem() == null) {
            return ""; // don't count the root item in the path
        } else {
            return reverse(curr.getParentItem(), depth + 1) + curr.getText()
                    + (depth > 0 ? "/" : "");
        }
    }

    protected void showSettings(int tab) {
        SettingsDialog dlg = new SettingsDialog(mEntireUIRoot, SwarmsBrowser.this, tab);
        dlg.show();
        dlg.setVisible(false);
        dlg.center();
        dlg.setVisible(true);
    }

    private List<String> getExpandedPaths(TreeItem curr) {
        /**
         * If curr is visible, check
         */
        List<String> partial = new ArrayList<String>();

        if (curr.getState()) {
            for (int i = 0; i < curr.getChildCount(); i++) {
                for (String path : getExpandedPaths(curr.getChild(i))) {
                    partial.add(curr.getText() + "/" + path);
                }
            }
            if (partial.size() == 0) {
                partial.add(curr.getText());
            }
        }
        return partial;
    }

    final Label computerNameLabel = new Label();

    final private ClickHandler statusImageClickListener = new ClickHandler() {
        public void onClick(ClickEvent event) {
            if (event.getSource() instanceof TorrentContainingImage) {

                TorrentInfo source = ((TorrentContainingImage) event.getSource()).getTorrent();

                new TorrentErrorDialogBox(source, mEntireUIRoot);

            } else {
                System.err.println("********* Got a click event without a TorrentContainingImage");
            }
        }
    };

    public Panel createHeaderButtons() {
        /**
         * We use a dock panel here so we can group some buttons on the left for
         * nondestructive actions (play, etc), and keep destructive actions on
         * the other side (delete).
         * 
         * DockPanel is much easier to use for this than tables
         */
        DockPanel panel = new DockPanel();

        panel.addStyleName(CSS_BROWSER_HEADER);

        panel.setWidth("100%");

        Button createSwarmButton = new Button(Strings.get(Strings.CREATE_SWARM));

        if (OneSwarmGWT.isRemoteAccess()) {
            createSwarmButton.setEnabled(false);
        }

        createSwarmButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new CreateSwarmDialogBox(mEntireUIRoot);
            }
        });

        Button addDownloadButton = new Button(Strings.get(Strings.ADD_SWARM_URL));

        DOM.setStyleAttribute(addDownloadButton.getElement(), "whiteSpace", "nowrap");

        playButton = new Button(Strings.get(Strings.SWARM_BROWSER_PLAY));
        deleteButton = new Button(Strings.get(Strings.SWARM_DELETE));
        tagButton = new Button(Strings.get(Strings.SWARM_TAGS));

        if (useDebug) {
            debugButton = new Button(msg.swarm_browser_button_debug());
            debugButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    dispatchSwarmAction(Strings.DEBUG, null);
                }
            });
        }

        addDownloadButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                String url = Window.prompt(msg.swarm_browser_add_enter_url(), "");
                if (url == null) { // cancel
                    return;
                }

                if (url.toLowerCase().startsWith("magnet:")
                        || url.toLowerCase().startsWith("oneswarm:")) {

                    int pos = url.indexOf((int) '?');
                    if (pos == -1) {
                        Window.alert(msg.swarm_browser_add_warning_bad_magnet());
                        return;
                    }
                    String query = url.substring(url.indexOf((int) '?'));
                    if (query.length() < 10) {
                        Window.alert(msg.swarm_browser_add_warning_bad_magnet());
                        return;
                    }
                    String[] toks = query.substring(1).split("&");
                    String base32hash = null;
                    for (String param : toks) {
                        String[] kv = param.split("=");
                        if (kv.length == 2) {
                            if (kv[0].equals("xt") && kv[1].startsWith("urn:osih:")) {
                                base32hash = kv[1].substring("urn:osih:".length());
                            }
                        }
                    }

                    OneSwarmRPCClient.getService().getBase64HashesForBase32s(
                            OneSwarmRPCClient.getSessionID(), new String[] { base32hash },
                            new AsyncCallback<String[]>() {
                                public void onFailure(Throwable caught) {
                                    caught.printStackTrace();
                                    OneSwarmGWT.log("Error: " + caught.getMessage());
                                    new ReportableErrorDialogBox(caught.getMessage(), false);
                                }

                                public void onSuccess(String[] result) {
                                    if (result == null) {
                                        return;
                                    }
                                    if (result[0] == null) {
                                        return;
                                    }

                                    for (String s : result) {
                                        History.newItem(EntireUIRoot.SEARCH_HISTORY_TOKEN + "id:"
                                                + s);
                                    }
                                }
                            });
                } else {
                    OneSwarmRPCClient.getService().downloadTorrent(
                            OneSwarmRPCClient.getSessionID(), url, new AsyncCallback<Integer>() {
                                public void onFailure(Throwable caught) {
                                    caught.printStackTrace();
                                }

                                public void onSuccess(Integer result) {
                                    if (result == -1) {
                                        Window.alert(msg.swarm_browser_add_download_error());
                                        return;
                                    }

                                    TorrentDownloaderDialog dlg = new TorrentDownloaderDialog(
                                            mEntireUIRoot, result);
                                    dlg.show();
                                    dlg.setVisible(false);
                                    dlg.center();
                                    dlg.setVisible(true);
                                }
                            });
                }
            }
        });

        tagButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                TagEditorDialog dlg = new TagEditorDialog(mEntireUIRoot, SwarmsBrowser.this
                        .getSelectedSwarms());
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setVisible(true);
            }
        });

        /**
         * We need to keep track of these so we can disable / enable them in the
         * highlighting click listeners
         */
        headerButtons.add(playButton);
        headerButtons.add(deleteButton);
        headerButtons.add(tagButton);

        playButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                String btext = playButton.getText();
                if (btext.equals(Strings.get(Strings.SWARM_BROWSER_PLAY))
                        || btext.equals(msg.swarm_browser_button_download()))
                    dispatchSwarmAction(Strings.SWARM_BROWSER_PLAY, getSelectedSwarms());
                else
                    dispatchSwarmAction(Strings.SWARM_DEFAULT_PLAY, getSelectedSwarms());
            }
        });
        deleteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                dispatchSwarmAction(Strings.SWARM_DELETE, getSelectedSwarms());
            }
        });

        Button settingsButton = new Button(Strings.get(Strings.SETTINGS));
        settingsButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                showSettings(0);
            }
        });

        /**
         * Binding things directly to east/west seems to not pack as tightly as
         * I want, but wrapping each in a panel works.
         */
        HorizontalPanel lhs = new HorizontalPanel();
        lhs.add(createSwarmButton);
        lhs.add(addDownloadButton);
        lhs.add(playButton);
        lhs.add(deleteButton);
        lhs.add(tagButton);
        lhs.add(settingsButton);
        if (useDebug) {
            lhs.add(debugButton);
        }

        moreActions.setVisibleItemCount(1);
        moreActions.addItem(msg.swarm_browser_more_actions());
        // immediately by a call to
        // refresh header buttons
        moreActions.addChangeListener(new ChangeListener() {
            public void onChange(Widget sender) {
                String selected = moreActions.getItemText(moreActions.getSelectedIndex());
                System.out.println("selected: " + selected);
                if (selected.equals(msg.swarm_browser_more_actions_start_swarm())
                        || selected.equals(msg.swarm_browser_more_actions_stop_swarm())) {
                    dispatchSwarmAction(Strings.SWARM_STARTSTOP, getSelectedSwarms());
                    String swapped = msg.swarm_browser_more_actions_stop_swarm();
                    if (selected.equals(msg.swarm_browser_more_actions_stop_swarm())) {
                        swapped = msg.swarm_browser_more_actions_start_swarm();
                    }
                    moreActions.setItemText(moreActions.getSelectedIndex(), swapped);
                } else {
                    for (String action : Strings.SWARM_MORE_ACTIONS_NOSELECTION) {
                        if (selected.equals(Strings.get(action))) {
                            dispatchSwarmAction(action, getSelectedSwarms());
                            moreActions.setSelectedIndex(0);
                            return;
                        }
                    }
                    for (String action : Strings.SWARM_MORE_ACTIONS_SELECTED) {
                        if (selected.equals(Strings.get(action))) {
                            dispatchSwarmAction(action, getSelectedSwarms());
                            moreActions.setSelectedIndex(0);
                            return;
                        }
                    }
                }
                moreActions.setSelectedIndex(0);
            }
        });

        lhs.add(moreActions);
        lhs.setCellVerticalAlignment(moreActions, VerticalPanel.ALIGN_MIDDLE);

        BackendTaskReporter backendReporter = new BackendTaskReporter();
        add(backendReporter);
        this.setCellHorizontalAlignment(backendReporter, HorizontalPanel.ALIGN_LEFT);
        lhs.add(backendReporter);
        lhs.setCellVerticalAlignment(backendReporter, VerticalPanel.ALIGN_MIDDLE);

        lhs.setSpacing(3);

        panel.add(lhs, DockPanel.WEST);

        panel.setCellHorizontalAlignment(lhs, HorizontalPanel.ALIGN_LEFT);
        panel.setCellVerticalAlignment(lhs, VerticalPanel.ALIGN_MIDDLE);

        HorizontalPanel rhs = new HorizontalPanel();
        rhs.setSpacing(3);
        // Label nameLabel = new Label("Computer: ");
        // rhs.add(nameLabel);

        rhs.add(computerNameLabel);
        updateComputerName();
        HTML nameEditAnchor = new HTML("(<a href='#'>" + msg.swarm_browser_change_nick_name()
                + "</a>)");
        nameEditAnchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ComputerNameDialog dlg = new ComputerNameDialog(computerNameLabel.getText(), true,
                        new ComputerNameDialog.NameSetCallback() {
                            public void setName(String newName) {
                                computerNameLabel.setText(newName);
                            }
                        });
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                dlg.setVisible(true);
            }
        });
        rhs.add(nameEditAnchor);

        panel.add(rhs, DockPanel.EAST);

        panel.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);
        panel.setCellVerticalAlignment(rhs, VerticalPanel.ALIGN_MIDDLE);

        refreshHeaderButtons();

        return panel;
    }

    private void updateComputerName() {
        OneSwarmRPCClient.getService().getComputerName(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                    }

                    public void onSuccess(String result) {
                        computerNameLabel.setText(result);
                    }
                });
    }

    public void refreshHeaderButtons() {
        boolean isEnabled = true;

        TorrentInfo[] selected = getSelectedSwarms();
        boolean anyF2F = false;
        if (selected != null) {
            for (TorrentInfo t : selected) {
                if (t.isF2FOnly()) {
                    anyF2F = true;
                    break;
                }
            }
        }

        if (selected == null)
            isEnabled = false;

        for (Button b : headerButtons) {
            b.setEnabled(isEnabled);
        }

        /**
         * We build up a set of new menu items and then compare it to the
         * existing list (updating if necessary). We used to just clear the menu
         * and rebuild, but because this function is called periodically,
         * Firefox would reset the selected menu in a visually annoying way
         * periodically while selected.
         * 
         * ====================================================================
         * =================================================
         */
        List<String> newActions = new ArrayList<String>();

        // moreActions.clear();
        // moreActions.addItem("More actions...");
        newActions.add(msg.swarm_browser_more_actions());
        for (String action : Strings.SWARM_MORE_ACTIONS_NOSELECTION) {
            /**
             * Things we can't do when accessing remotely.
             */
            if (action.equals(Strings.ADD_SWARM_FILE) && OneSwarmGWT.isRemoteAccess()) {
                continue;
            }

            // moreActions.addItem(Strings.get(action));
            newActions.add(Strings.get(action));
        }

        if (selected != null) {
            if (anyF2F == false) {
                for (String action : Strings.SWARM_MORE_ACTIONS_SELECTED) {
                    /**
                     * Things we can't do when accessing remotely.
                     */
                    if (action.equals(Strings.SWARM_DEFAULT_PLAY) && OneSwarmGWT.isRemoteAccess()) {
                        continue;
                    }
                    if (action.equals(Strings.SWARM_STARTSTOP) == false) {
                        // moreActions.addItem(Strings.get(action));
                        newActions.add(Strings.get(action));
                    } else {
                        // moreActions.addItem(selected[0].isStarted() ?
                        // "Stop swarm" : "Start swarm");
                        newActions.add(selected[0].isStarted() ? msg
                                .swarm_browser_more_actions_stop_swarm() : msg
                                .swarm_browser_more_actions_start_swarm());
                    }
                }
            } // if local
            else {
                // something is selected, but it's an f2f swarm. In this case,
                // only copy link is valid
                newActions.add(Strings.get(Strings.SWARM_COPY_MAGNET));
            }
        }

        boolean update = newActions.size() != moreActions.getItemCount();
        for (int i = 0; i < newActions.size() && update == false; i++) {
            update = !newActions.get(i).equals(moreActions.getItemText(i));
        }
        if (update) {
            moreActions.clear();
            for (int i = 0; i < newActions.size(); i++) {
                moreActions.addItem(newActions.get(i));
            }
        }

        /**
         * end menu updating
         * ====================================================
         * =================================================================
         */

        if (selected != null) {
            boolean canPlay = false;
            FileTypeFilter type = FileTypeFilter.match(selected[0].getDefaultMovieName());
            if (type.equals(FileTypeFilter.Videos) && selected[0].getProgress() > 5) // lets
            // pretend
            // 0.5
            // %
            // is
            // enough
            // (
            // basically
            // ,
            // has
            // downloading
            // started
            // ?
            // )
            {
                canPlay = true;
            } else if (type.equals(FileTypeFilter.Audio)
                    && selected[0].getProgress() * selected[0].getTotalSize() >= 4096 * 100) // lets
            // pretend
            // 4
            // MB
            // is
            // enough
            // else if( type.equals(FileTypeFilter.Audio) &&
            // mSelectedSwarm.getProgress() == 1000 )
            {
                canPlay = true;
            } else if (anyF2F) {
                canPlay = true; // play == download
            }

            if (anyF2F) {
                playButton.setText(msg.swarm_browser_button_download());
            } else {
                playButton.setText(msg.swarm_browser_button_play());
            }

            if (canPlay) {
                // can play
                playButton.setEnabled(true);
                if (!anyF2F) {
                    playButton.setText(msg.swarm_browser_button_play());
                }
            } else {
                // can't play
                playButton.setEnabled(false);
            }

            if (anyF2F) {
                deleteButton.setEnabled(false);
                tagButton.setEnabled(false);
            }
        }
    }

    public void removeFile(final TorrentInfo inTorrent) {
        // mMouseOverSwarm = null;
        selectSwarm(null);
        refreshHeaderButtons();

        if (inTorrent.getProgress() < 1000) {
            int i = mTransferringSwarms.indexOf(inTorrent);
            if (i != -1) {
                mTransferringSwarms.remove(inTorrent);
                mProgressBars.remove(i);
            } else {
                System.err
                        .println("torrent had progress < 1000 but no progress bar. not updated somewhere?"
                                + inTorrent.getTorrentID());
            }
        }

        VerticalPanel icon_with_name = (VerticalPanel) torrent_to_panel.get(inTorrent).getWidget();
        torrent_to_panel.remove(inTorrent);
        final PreviewPanel effectPanel = (PreviewPanel) icon_with_name.getParent();
        // Fade fade = filePanel_to_fade.get(icon_with_name);
        // fade.getProperties().setStartOpacity(100);
        // fade.getProperties().setEndOpacity(0);
        // effectPanel.beginEffects();
        // (new Timer() {
        // public void run() {
        filesFlowPanel.remove(effectPanel);
        // }
        // }).schedule((int) (FADE_TIME_SECS * 1000));
    }

    class MaxHeightWithTorrentInfoLabel extends Label {
        int mActualHeight = 0;
        TorrentInfo mTorrent = null;

        public MaxHeightWithTorrentInfoLabel(String fname, TorrentInfo inTorrent) {
            super(fname);
            mTorrent = inTorrent;

            if (inTorrent.isF2FOnly()) {
                this.addStyleName(CSS_F2F_LABEL);
            }
        }

        public int getActualHeight() {
            return mActualHeight == 0 ? 45 : mActualHeight;
        }

        public TorrentInfo getSwarm() {
            return mTorrent;
        }

        protected void onLoad() {
            super.onLoad();
            /**
             * == 0 check since we don't want to do this twice (e.g. when we
             * change filters)
             */
            if (mActualHeight == 0) {
                mActualHeight = Math.min(45, getOffsetHeight());
                setHeight("45px"); // so layout draws correctly, but we'll use
                // getActualHeight for GWTCanvas highlight
            }
        }
    }

    private static class RightClickablePanel extends VerticalPanel {

        List<RightClickHandler> handlers = new LinkedList<RightClickHandler>();

        public RightClickablePanel() {
            super();
            sinkEvents(Event.ONMOUSEUP | Event.ONDBLCLICK | Event.ONCONTEXTMENU);
        }

        public void onBrowserEvent(Event event) {
            GWT.log("onBrowserEvent", null);
            event.stopPropagation();
            event.preventDefault();
            switch (DOM.eventGetType(event)) {
            case Event.ONMOUSEUP:
                if (DOM.eventGetButton(event) == Event.BUTTON_RIGHT) {
                    for (RightClickHandler h : handlers) {
                        h.onRightClick(event);
                    }
                }
                break;
            case Event.ONDBLCLICK:
                break;

            case Event.ONCONTEXTMENU:
                GWT.log("Event.ONCONTEXTMENU", null);
                break;

            default:
                break; // Do nothing
            }// end switch
        }

        public void addRightClickHandler(RightClickHandler handler) {
            handlers.add(handler);
        }

    }

    private static interface RightClickHandler {
        void onRightClick(Event event);
    }

    private class RightClickMenu extends PopupPanel {
        public RightClickMenu() {
            super(true, false);
            setStyleName("rightclick_menu");
            MenuBar popupMenuBar = new MenuBar(true);
            final TorrentInfo[] selected = getSelectedSwarms();

            boolean anyF2F = false;
            boolean anyCompleted = false;
            if (selected != null) {
                for (TorrentInfo t : selected) {
                    if (t.isF2FOnly()) {
                        anyF2F = true;
                    }
                    if (t.getProgress() == 1000) {
                        anyCompleted = true;
                    }
                }
            }

            if (anyF2F) {
                MenuItem downloadItem = new MenuItem(msg.swarm_browser_button_download(), true,
                        new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_BROWSER_PLAY, selected);
                            }
                        });
                downloadItem.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(downloadItem);
            } else {

                if (playButton.isEnabled()) {
                    MenuItem playItem = new MenuItem(msg.swarm_browser_button_play(), true,
                            new Command() {
                                public void execute() {
                                    dispatchSwarmAction(Strings.SWARM_BROWSER_PLAY,
                                            getSelectedSwarms());
                                    hide();
                                }
                            });
                    playItem.addStyleName("rightclick_menu-item");
                    popupMenuBar.addItem(playItem);
                }

                MenuItem defaultPlayItem = new MenuItem(
                        msg.swarm_browser_more_actions_default_app(), true, new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_DEFAULT_PLAY, getSelectedSwarms());
                                hide();
                            }
                        });
                defaultPlayItem.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(defaultPlayItem);

                MenuItem showInFinder = new MenuItem(msg.swarm_browser_more_actions_reveal(), true,
                        new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_REVEAL, getSelectedSwarms());
                                hide();
                            }
                        });
                showInFinder.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(showInFinder);

                popupMenuBar.addSeparator();
                if (!anyCompleted) {
                    MenuItem startStop = new MenuItem(msg.swarm_browser_right_click_start_stop(),
                            true, new Command() {
                                public void execute() {
                                    dispatchSwarmAction(Strings.SWARM_STARTSTOP,
                                            getSelectedSwarms());
                                    hide();
                                }
                            });
                    startStop.addStyleName("rightclick_menu-item");
                    popupMenuBar.addItem(startStop);
                }
                MenuItem showDetails = new MenuItem(msg.swarm_browser_more_actions_swarm_details(),
                        true, new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_DETAILS, getSelectedSwarms());
                                hide();
                            }
                        });
                showDetails.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(showDetails);

                MenuItem copyLink = new MenuItem(msg.swarm_browser_more_actions_copy_magnet(),
                        true, new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_COPY_MAGNET, getSelectedSwarms());
                                hide();
                            }
                        });
                copyLink.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(copyLink);

                MenuItem managePerms = new MenuItem(
                        msg.swarm_browser_more_actions_manage_visibility(), true, new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.MANAGE_PERMS, getSelectedSwarms());
                                hide();
                            }
                        });
                managePerms.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(managePerms);

                popupMenuBar.addSeparator();

                MenuItem delete = new MenuItem(msg.swarm_browser_button_delete(), true,
                        new Command() {
                            public void execute() {
                                dispatchSwarmAction(Strings.SWARM_DELETE, getSelectedSwarms());
                                hide();
                            }
                        });
                delete.addStyleName("rightclick_menu-item");
                popupMenuBar.addItem(delete);
            }

            popupMenuBar.setVisible(true);
            add(popupMenuBar);
        }
    }

    static class TorrentContainingImage extends Image {
        private TorrentInfo torrent;

        public TorrentContainingImage(String url, TorrentInfo torrent) {
            super(url);
            this.torrent = torrent;
        }

        public TorrentInfo getTorrent() {
            return torrent;
        }
    }

    public void addFileIcon(final TorrentInfo inTorrent, boolean force_show) {
        String fname = inTorrent.getName();
        String lineWrapped = fname;
        int MAX_LINE_LEN = 20;
        // int MIN_LINE_LEN = MAX_LINE_LEN - 6;
        int MAX_TOTAL_LEN = 60;
        if (lineWrapped.length() > MAX_LINE_LEN) {
            // cap at 45 char
            lineWrapped = lineWrapped.substring(0, Math.min(lineWrapped.length(), MAX_TOTAL_LEN));

            // hack until FF supports css word-wrap=break-word
            String firstLine = lineWrapped.substring(0, MAX_LINE_LEN);
            if (!firstLine.contains(" ")) {
                lineWrapped = firstLine + " " + lineWrapped.substring(MAX_LINE_LEN);
            }
            // }

        }

        VerticalPanel icon_panel_scratch = null;

        String disableRightClickCookie = Cookies
                .getCookie(UISettingsPanel.COOKIE_DISABLE_RIGHT_CLICK);
        if (disableRightClickCookie == null || "1".equals(disableRightClickCookie)) {
            icon_panel_scratch = new VerticalPanel();
        } else {
            icon_panel_scratch = new RightClickablePanel();
            ((RightClickablePanel) icon_panel_scratch)
                    .addRightClickHandler(new RightClickHandler() {
                        public void onRightClick(final Event event) {
                            selectSwarm(inTorrent, false, false);
                            refreshHeaderButtons();

                            final RightClickMenu menu = new RightClickMenu();
                            menu.setPopupPositionAndShow(new PositionCallback() {
                                public void setPosition(int offsetWidth, int offsetHeight) {
                                    menu.setPopupPosition(event.getClientX(), event.getClientY());
                                }
                            });
                        }
                    });
        }

        final VerticalPanel icon_with_name = icon_panel_scratch;

        icon_with_name.setWidth(ICON_WIDTH + "px");

        final Label fnameLabel = new MaxHeightWithTorrentInfoLabel(lineWrapped, inTorrent);
        String tooltipText = fname;
        tooltipText += " Size: " + StringTools.formatRate(inTorrent.getTotalSize());
        if (inTorrent.isF2FOnly()) {
            tooltipText += " From friend: " + inTorrent.getF2F_nick();
        }
        fnameLabel.setTitle(tooltipText);
        fnameLabel.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        fnameLabel.setWidth((ICON_WIDTH + FNAME_BUFFER) + "px");

        // /fnameLabel.setWidth(ICON_WIDTH+"px");
        // fnameLabel.setWidth("30px");

        final AbsolutePanel fnameAbsolute = new AbsolutePanel();
        fnameAbsolute.add(fnameLabel);

        DOM.setStyleAttribute(fnameLabel.getElement(), "zIndex", "10");

        Image videoPreviewImage = null;
        String hint = "";
        if (inTorrent.isF2FOnly() && inTorrent.getDefaultMovieName() != null) {
            if (FileTypeFilter.match(inTorrent.getDefaultMovieName(), FileTypeFilter.Audio)) {
                hint = "audio";
            } else if (FileTypeFilter.match(inTorrent.getDefaultMovieName(), FileTypeFilter.Videos)) {
                hint = "video";
            }
        }

        videoPreviewImage = new TorrentContainingImage(GWT.getModuleBaseURL() + "image?infohash="
                + inTorrent.getTorrentID() + "&type=" + hint, inTorrent);

        videoPreviewImage.setWidth("128px");
        videoPreviewImage.setHeight("128px");
        if (inTorrent.isF2FOnly()) {
            videoPreviewImage.addStyleName(CSS_PREVIEW_TRANSFERRING);
        }
        final AbsolutePanel overlay = new AbsolutePanel();
        overlay.addStyleName(CSS_VIDEO_PREVIEW_BG);
        overlay.setWidth(ICON_WIDTH + FNAME_BUFFER + "px");
        overlay.setHeight(ICON_HEIGHT + "px");
        overlay.add(videoPreviewImage, FNAME_BUFFER / 2, 0);

        Image statusImage = new TorrentContainingImage(
                inTorrent.getErrorState() == 0 ? ImageConstants.ICON_STARTED
                        : ImageConstants.ICON_WARNING, inTorrent);

        statusImage.addClickHandler(statusImageClickListener);

        statusImage.setWidth("16px");
        statusImage.setHeight("16px");
        /**
         * now always leaving this invisible unless stopped. looks too much like
         * a play icon. confusing. note, we use the DOM method instead of
         * setVisible since the latter interferes with the selection code's use
         * of zIndex, whereas visibility hidden always hides, regardless of
         * zIndex.
         */
        if (inTorrent.getErrorState() == 0) {
            DOM.setStyleAttribute(statusImage.getElement(), "visibility", "hidden");
        }
        overlay.add(statusImage, 0, ICON_HEIGHT - 16);
        statusImage.setTitle("Swarm started");

        // ClickListener selectListener = new ClickListener() {
        // public void onClick(Widget sender) {
        // selectSwarm(inTorrent, true);
        // refreshHeaderButtons();
        // }
        // };

        ClickHandler selectHandler = new ClickHandler() {
            public void onClick(ClickEvent event) {
                selectSwarm(inTorrent, true, event.getNativeEvent().getShiftKey());
                refreshHeaderButtons();
            }
        };

        fnameLabel.addClickHandler(selectHandler);
        videoPreviewImage.addClickHandler(selectHandler);

        if (inTorrent.getProgress() < 1000) {
            overlay.addStyleName(CSS_PREVIEW_TRANSFERRING);
            mTransferringSwarms.add(inTorrent);

            // Label progressBar = new Label("0");
            // fnameAbsolute.add(progressBar, 0, 0);

            ProgressBar pb = new ProgressBar(0.0, 1000.0, inTorrent.getProgress());
            pb.setTextFormatter(new SwarmRateTextFormatter(inTorrent));

            int pb_width = (ICON_WIDTH - (ICON_WIDTH / 5));
            pb.setWidth(pb_width + "px");
            pb.setHeight("15px");
            DOM.setStyleAttribute(pb.getElement(), "zIndex", "100");
            overlay.add(pb, (CELL_WIDTH - pb_width) / 2, ICON_HEIGHT - 20);

            // add(pb);
            mProgressBars.add(pb);
        }

        icon_with_name.add(overlay);
        icon_with_name.add(fnameAbsolute);
        icon_with_name.setSpacing(3);

        icon_with_name.addStyleName(CSS_FILE_ICON);

        PreviewPanel fadePanel = new PreviewPanel();
        fadePanel.add(icon_with_name);
        // Fade fade = new Fade();
        // fade.getProperties().setEffectLength(FADE_TIME_SECS);
        // fade.getProperties().setStartOpacity(0);
        // fade.getProperties().setEndOpacity(100);
        // fadePanel.addEffect(fade);

        /**
         * So we can link UI elements with torrent data
         */
        torrent_to_panel.put(inTorrent, fadePanel);

        // filePanel_to_fade.put(icon_with_name, fade);
        // filesFlowPanel.add(fadePanel);
        // we can't do this if showing transfers -- will crash 8)

        if (force_show) {
            if (mShowingTransfers == false) {
                // sortedFilesPanelInsert(fadePanel);

                filesFlowPanel.add(fadePanel);

                // filesFlowPanel.add(icon_with_name);
            }
            // fadePanel.beginEffects();
        }

        PickupDragController dragController = mEntireUIRoot.getDragController();
        if (dragController != null
                && "0".equals(Cookies.getCookie(UISettingsPanel.COOKIE_DISABLE_DRAG_AND_DROP))) {
            Image moveImage = new Image("images/move_arrow_16x16.png");
            overlay.add(moveImage, overlay.getOffsetWidth() - 16, 0);
            dragController.makeDraggable(videoPreviewImage, moveImage);
            moveImage.setVisible(false);
        }

        if (mSelectedSwarms.contains(inTorrent)) {
            selectSwarm(inTorrent);
        }

    }

    private Image currentMoveIcon = null;

    public void update(int count) {
        // testing explicit signaling to remove polling
        // refreshActive();

        // if( mNextTransferringSwarmsUpdate < System.currentTimeMillis() ) {
        // mNextTransferringSwarmsUpdate = Long.MAX_VALUE;
        // refreshTransferringSwarms();
        // }
        // if( mNextStateSyncUpdate < System.currentTimeMillis() ) {
        // mNextStateSyncUpdate = Long.MAX_VALUE;
        // synchronizeSwarmStateValues();
        // }

        if (mNextPagedUpdate < System.currentTimeMillis()) {
            mNextPagedUpdate = System.currentTimeMillis() + 20 * 1000; // ensure
            // we
            // _eventually_
            // do
            // something
            // again
            // !

            if (mUsingIconView) // the list view will take care of this
            // internally.
            {
                pagedSwarmRefresh();
            }
        }

        if (mNextWarningCheck < System.currentTimeMillis()) {
            OneSwarmRPCClient.getService().checkIfWarning(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<String[]>() {
                        public void onSuccess(String[] result) {
                            for (int j = 0; j < 4; j++) {
                                if (result[4 + (j * 8)].equals("true")) {
                                    StoppedUserDialog dlg = new StoppedUserDialog(mEntireUIRoot,
                                            result[5 + (j * 8)], result[6 + (j * 8)],
                                            result[7 + (j * 8)]);
                                    dlg.show();
                                    dlg.setVisible(false);
                                    dlg.center();
                                    dlg.setVisible(true);
                                } else if (result[(j * 8)].equals("true")) {
                                    WarnUserDialog dlg = new WarnUserDialog(mEntireUIRoot,
                                            result[1 + (j * 8)], result[2 + (j * 8)],
                                            result[3 + (j * 8)]);
                                    dlg.show();
                                    dlg.setVisible(false);
                                    dlg.center();
                                    dlg.setVisible(true);
                                }
                            }
                        }

                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }
                    });
            mNextWarningCheck = System.currentTimeMillis() + 30 * 1000;
        }
        if ((count % 5) == 0) {
            lightweight_refresh_check();
        }

        if (mNextErrorPoll < System.currentTimeMillis()) {
            mNextErrorPoll = System.currentTimeMillis() + 120 * 1000;
            OneSwarmRPCClient.getService().getBackendErrors(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<ArrayList<BackendErrorReport>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(ArrayList<BackendErrorReport> result) {

                            if (result.size() > 0) {
                                OneSwarmGWT.log("Got " + result.size() + " backend errors");
                            }

                            // most recent first
                            Collections.reverse(result);

                            mNextErrorPoll = System.currentTimeMillis() + (5 * 1000);

                            if (result.size() == 0) {
                                return;
                            }
                            StringBuffer concat = new StringBuffer();
                            boolean show_report = false;
                            for (BackendErrorReport r : result) {
                                concat.append(r.getMessage() + "\n");

                                if (r.isShowReportText()) {
                                    show_report = true;
                                }
                            }

                            new ReportableErrorDialogBox(concat.toString(), false, show_report);
                        }
                    });
        }

        updateComputerName();
    }

    /**
     * This checks only the _number_ of download managers as a prelude to decide
     * whether or not we need to do a full refreshActive(). Even though the UI
     * is explicitly notified of most refreshes, watch directories sometimes
     * mean that swarms will be added without UI knowledge
     */
    private void lightweight_refresh_check() {
        OneSwarmRPCClient.getService().getDownloadManagersCount(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        if (result.intValue() != mPreviousFullRefreshTotalSwarmsCount) {
                            System.out
                                    .println("download manager count doesn't match mPreviousFullRefreshTotalSwarmsCount, refreshing ("
                                            + result + " / " + mPreviousFullRefreshTotalSwarmsCount);
                            if (mShowingTransfers == false || mShowingFriends) {
                                SwarmsBrowser.this.refreshActive(false);
                            }
                        } else {
                            // System.out.println("lightweight refresh: " +
                            // result);
                        }
                    }
                });
    }

    private void pagedSwarmRefresh() {
        ArrayList<String> whichOnes = new ArrayList<String>();
        for (int i = 0; i < filesFlowPanel.getWidgetCount(); i++) {
            if (filesFlowPanel.getWidget(i) instanceof PreviewPanel) {
                PreviewPanel p = (PreviewPanel) filesFlowPanel.getWidget(i);
                VerticalPanel fnamePanel = (VerticalPanel) p.getWidget(); // this
                AbsolutePanel fnameAbsolute = (AbsolutePanel) fnamePanel.getWidget(1);
                MaxHeightWithTorrentInfoLabel fnameLabel = (MaxHeightWithTorrentInfoLabel) fnameAbsolute
                        .getWidget(0);

                whichOnes.add(fnameLabel.getSwarm().getTorrentID());
            }
        }

        OneSwarmRPCClient.getService().pagedTorrentStateRefresh(OneSwarmRPCClient.getSessionID(),
                whichOnes, new AsyncCallback<TorrentInfo[]>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(TorrentInfo[] result) {
                        /**
                         * begin by synchronizing the state
                         */
                        Map<String, TorrentInfo> id_to_torrent = new HashMap<String, TorrentInfo>();

                        for (TorrentInfo t : torrent_to_panel.keySet()) {
                            id_to_torrent.put(t.getTorrentID(), t);
                        }

                        for (TorrentInfo t : result) {
                            if (t == null)
                                continue;

                            if (id_to_torrent.get(t.getTorrentID()) != null) {
                                id_to_torrent.get(t.getTorrentID()).setStatus(t.getStatus());
                                id_to_torrent.get(t.getTorrentID()).setProgress(t.getProgress());
                                id_to_torrent.get(t.getTorrentID()).setDownloadRate(
                                        t.getDownloadRate());
                                id_to_torrent.get(t.getTorrentID())
                                        .setDownloaded(t.getDownloaded());

                                id_to_torrent.get(t.getTorrentID()).setSeeders(t.getSeeders());
                                id_to_torrent.get(t.getTorrentID()).setLeechers(t.getLeechers());

                                id_to_torrent.get(t.getTorrentID())
                                        .setErrorState(t.getErrorState());

                                /**
                                 * In case the play button now needs enabled
                                 */
                                if (mSelectedSwarms.size() == 1) {
                                    if (mSelectedSwarms.get(0).getTorrentID()
                                            .equals(t.getTorrentID()))
                                        refreshHeaderButtons();
                                }
                            }
                        }

                        /**
                         * then progress...
                         */
                        gotRefreshOfTransferring(result);

                        mNextPagedUpdate = System.currentTimeMillis() + 1000;
                    }
                });
    }

    private void gotRefreshOfTransferring(TorrentInfo[] swarms) {
        // System.out.println("got refresh of: " + swarms.length +
        // " currently visible swarms.");

        Map<String, TorrentInfo> idToInfo = new HashMap<String, TorrentInfo>();
        for (TorrentInfo info : swarms) {
            if (info != null) {
                idToInfo.put(info.getTorrentID(), info);
            }
        }

        // for (int i = 0; i < mTransferringSwarms.size(); i++) {
        for (int swarmsIndex = 0; swarmsIndex < swarms.length; swarmsIndex++) {
            // TorrentInfo info = mTransferringSwarms.get(i);
            TorrentInfo info = swarms[swarmsIndex];

            if (info == null) {
                // not refresh
                continue;
            }

            int i = mTransferringSwarms.indexOf(info);
            if (i > -1) {
                ProgressBar progressBar = mProgressBars.get(i);

                /**
                 * This is brittle (and used somewhere else) -- TODO: should
                 * wrap this?
                 */
                final VerticalPanel fnamePanel = (VerticalPanel) torrent_to_panel.get(info)
                        .getWidget(); // this
                // is
                // icon_with_name
                // verticalpanel
                AbsolutePanel fnameAbsolute = (AbsolutePanel) fnamePanel.getWidget(1);
                // Label fnameLabel = (Label) fnameAbsolute.getWidget(0);

                // System.out.println("attempting refresh of: " + info);
                // fnameAbsolute.setWidgetPosition(progressBar, 0,
                // -fnameLabel.getOffsetHeight());
                TorrentInfo latest = idToInfo.get(info.getTorrentID());
                if (latest != null) {
                    progressBar.setProgress(latest.getProgress());
                    info.setProgress(latest.getProgress());
                    // System.out.println("progress of: " + latest.getName() +
                    // " " +
                    // latest.getProgress());
                    if (latest.getProgress() == 1000) {
                        // done, remove progress bar and make opaque.
                        int pbi = mTransferringSwarms.indexOf(latest);
                        mTransferringSwarms.remove(latest);
                        ProgressBar pb = mProgressBars.get(pbi);
                        pb.removeFromParent();
                        fnameAbsolute.removeStyleName(CSS_PREVIEW_TRANSFERRING);
                        mProgressBars.remove(pbi);

                        /**
                         * We may also now be able to generate a preview image,
                         * so give that a shot
                         */
                        Image previewImage = (Image) ((AbsolutePanel) fnamePanel.getWidget(0))
                                .getWidget(0);
                        previewImage.setUrl(previewImage.getUrl() + "&refresh");
                        System.out.println("attempting image refresh...: " + previewImage.getUrl());

                        /**
                         * Also, since it might now be playable (and selected)
                         */
                        refreshHeaderButtons();
                    }
                } else {
                    // something finished or was removed --> more
                    // heavyweight
                    System.out.println("something finished or was removed, refreshing active");
                    refreshActive(false);
                }
            }

            // this is not transferring, just update start/stop
            TorrentInfo latest = idToInfo.get(info.getTorrentID());
            info.setStatus(latest.getStatus());
            Image statusImage = getStartStopImageFromEffectPanel(torrent_to_panel.get(info));
            if (statusImage != null) {

                if (info.getErrorState() == 0) {
                    boolean startImage = statusImage.getUrl().endsWith(ImageConstants.ICON_STARTED);
                    if (info.isStarted() && !startImage) {
                        statusImage.setUrl(ImageConstants.ICON_STARTED);
                        DOM.setStyleAttribute(statusImage.getElement(), "visibility", "hidden");
                    } else if (!info.isStarted() && startImage) {
                        statusImage.setUrl(ImageConstants.ICON_STOPPED);
                        DOM.setStyleAttribute(statusImage.getElement(), "visibility", "visible");
                    }
                } else {
                    statusImage.setUrl(ImageConstants.ICON_WARNING);
                    DOM.setStyleAttribute(statusImage.getElement(), "visibility", "visible");
                }

                // no need to show these if downloads are complete
                if (info.getProgress() == 1000 && info.getErrorState() == 0) {
                    DOM.setStyleAttribute(statusImage.getElement(), "visibility", "hidden");
                }
            }
        }
    }

    private Image getStartStopImageFromEffectPanel(PreviewPanel effectPanel) {
        if (effectPanel == null) {
            System.err.println("null effectpanel in startstop image get");
            return null;
        }
        VerticalPanel w = ((VerticalPanel) effectPanel.getWidget());
        if (w == null) {
            System.err.println("null verticalpanel in startstop image get");
            return null;
        }
        AbsolutePanel imageAbsolute = (AbsolutePanel) w.getWidget(0);
        return (Image) imageAbsolute.getWidget(1);
    }

    public void changeFilter(FileTypeFilter filter, boolean just_a_refresh) {

        if (just_a_refresh == false) {
            mFilterPattern = "";
        }

        boolean needReshowControls = mShowingTransfers || mShowingFriends;

        mShowingTransfers = false;
        mShowingFriends = false;
        if (mCurrentFileTypeFilter.equals(filter) == false) {
            System.out.println("changing file filter type, going back to jump zero");
            mJumpList.setSelectedIndex(0); // go back to first page
        }

        mCurrentFileTypeFilter = filter;
        mTotalSwarmsForFilterType = 0;

        if (needReshowControls) {
            DOM.setStyleAttribute(mPagingPanel.getElement(), "display", null);
            // DOM.setStyleAttribute(mPagingPanel.getElement(), "height",
            // mPagingTableOldHeight);

            checkTagsPanelAttchment();

            DOM.setStyleAttribute(mSwarmsPerPagePanel.getElement(), "visibility", "visible");
            DOM.setStyleAttribute(mShowFriendsCheckbox.getElement(), "visibility", "visible");
        }

        transferDetailsTable = null;

        System.out.println("change filter: refreshActive()");
        refreshActive(false); // we checked this above

        // filterDisplayed(mFilterPattern);
        // updateNavigationPanel();
    }

    private void detachDirectoryScrollPanel() {
        this.mDirectoryScroll.removeFromParent();
        this.mTagsFilesSeparator.removeFromParent();
    }

    private void reattachDirectoryScrollPanel() {
        mFoldersAndFiles.insert(mDirectoryScroll, 0);
        mFoldersAndFiles.insert(mTagsFilesSeparator, 1);
        mFoldersAndFiles.setCellWidth(mDirectoryScroll, DIRECTORY_SCROLL_WIDTH + "px");
        mFoldersAndFiles.setCellWidth(mTagsFilesSeparator, "1px");

        com.google.gwt.user.client.Element td = DOM.getParent(mTagsFilesSeparator.getElement());
        DOM.setStyleAttribute(td, "borderRight", "1px solid #d2d2d2");
    }

    // String mPagingTableOldHeight = null;

    protected void showFriends() {
        mShowingFriends = true;
        // mPagingTableOldHeight =
        // DOM.getStyleAttribute(mPagingPanel.getElement(), "height");
        DOM.setStyleAttribute(mPagingPanel.getElement(), "display", "none");
        // DOM.setStyleAttribute(mPagingPanel.getElement(), "height", "0pt");
        if (mDirectoryScroll.isAttached()) {
            mDirectoryScroll.removeFromParent();
        }
        DOM.setStyleAttribute(mSwarmsPerPagePanel.getElement(), "visibility", "hidden");
        DOM.setStyleAttribute(mShowFriendsCheckbox.getElement(), "visibility", "hidden");
        filesFlowPanel.clear();
        selectSwarm(null);
        filesFlowPanel.add(new FriendsDetailsListPanel(SwarmsBrowser.this));
    }

    public void showTransfers() {
        mShowingTransfers = true;

        // mPagingTableOldHeight =
        // DOM.getStyleAttribute(mPagingPanel.getElement(), "height");
        DOM.setStyleAttribute(mPagingPanel.getElement(), "display", "none");
        // DOM.setStyleAttribute(mPagingPanel.getElement(), "height", "0pt");

        if (mDirectoryScroll.isAttached()) {
            detachDirectoryScrollPanel();
        }

        DOM.setStyleAttribute(mSwarmsPerPagePanel.getElement(), "visibility", "hidden");
        DOM.setStyleAttribute(mShowFriendsCheckbox.getElement(), "visibility", "hidden");

        filesFlowPanel.clear();
        /**
         * We don't want the transfers panel to persist unless its visible since
         * it requires polling updates.
         */
        // tableSelectedSwarmChanged(null);
        selectSwarm(null);
        TransferDetailsPanel detailsPanel = new TransferDetailsPanel();
        transferDetailsTable = detailsPanel.getPublicDetailsTable();
        filesFlowPanel.add(detailsPanel);

        // filterDisplayed(mFilterPattern);
        transferDetailsTable.filterDisplayed(mFilterPattern);
    }

    /**
     * This is called by the transfer details table. We need this callback
     * because the filtered count depends on an RPC
     */
    public void updateFilteredCountFromTransferDetails(int count) {
        updateFilteredCount(count);
    }

    // public void tableSelectedSwarmChanged(TorrentInfo inSwarm) {
    // mSelectedSwarm = inSwarm;
    // refreshHeaderButtons();
    // }

    public void refreshFromFilterText(String text) {
        mFilterPattern = text;
        if (mShowingTransfers == false || mShowingFriends) {
            System.out.println("filtering swarms...");
            mJumpList.setSelectedIndex(0);
            changeFilter(mCurrentFileTypeFilter, true);
        } else {
            System.out.println("filtering transfers...");
            transferDetailsTable.filterDisplayed(mFilterPattern);
        }
    }

    public void pageZero() {
        mDirectoryTree.setSelectedItem(null);
        mDirectoryScroll.scrollToLeft();
        mDirectoryScroll.scrollToTop();
        mJumpList.setSelectedIndex(0);
    }

    public void sync_settings() {
        System.out.println("sync settings");
        OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "OneSwarm.ui.double.click", new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        System.out.println("doubleclick action: " + result);
                        if (result >= 0 && result < 3) {
                            mDoubleClickAction = DoubleClickAction.values()[result];
                            System.out.println("doubleclick action str: "
                                    + mDoubleClickAction.getAction());
                        }
                    }
                });

        // in case we enabled/disabled tags
        refreshActive(false);
    }

    public TorrentInfo[] getSelectedSwarms() {
        if (mUsingIconView) {
            if (mSelectedSwarms.size() > 0)
                return mSelectedSwarms.toArray(new TorrentInfo[0]);
            else
                return null;
        } else if (mShowingTransfers == false && mShowingFriends == false) {
            return ((SwarmsListTable) filesFlowPanel.getWidget(0)).getSelectedSwarms();
        } else {
            return null;
        }
    }

    public void selectPreviousSwarm() {
        if (mUsingIconView == false) {
            return;
        }

        if (mSelectedSwarms.size() == 0) {
            if (mSwarmsOrderedByDisplay.length > 0) {
                selectSwarm(mSwarmsOrderedByDisplay[mSwarmsOrderedByDisplay.length - 1]);
            }
        }
        TorrentInfo which = mSelectedSwarms.get(0);
        for (int i = 0; i < mSwarmsOrderedByDisplay.length; i++) {
            if (which.equals(mSwarmsOrderedByDisplay[i])) {
                if (i == 0) {
                    previousPage();
                } else {
                    selectSwarm(mSwarmsOrderedByDisplay[i - 1]);
                }
            }
        }
    }

    public void selectNextSwarm() {
        if (mUsingIconView == false) {
            return;
        }

        if (mSelectedSwarms.size() == 0) {
            if (mSwarmsOrderedByDisplay.length > 0) {
                selectSwarm(mSwarmsOrderedByDisplay[0]);
            }
        }
        TorrentInfo which = mSelectedSwarms.get(0);
        for (int i = 0; i < mSwarmsOrderedByDisplay.length; i++) {
            if (which.equals(mSwarmsOrderedByDisplay[i])) {
                if (i == mSwarmsOrderedByDisplay.length - 1) {
                    nextPage();
                } else {
                    selectSwarm(mSwarmsOrderedByDisplay[i + 1]);
                }
            }
        }
    }

    public void nextPage() {
        nextPreviousPageLinkListener.onClick(new ClickEvent() {
            public Object getSource() {
                return nextLink;
            }
        });
    }

    public void previousPage() {
        nextPreviousPageLinkListener.onClick(new ClickEvent() {
            public Object getSource() {
                return prevLink;
            }
        });
    }

    public void doubleClick() {
        if (this.getSelectedSwarms() == null) {
            return;
        }

        if (getSelectedSwarms().length == 0) {
            return;
        }

        if (OneSwarmGWT.isRemoteAccess() == false) {
            if (mDoubleClickAction.getAction().equals(Strings.SWARM_BROWSER_PLAY)
                    && playButton.isEnabled() == false) {
                dispatchSwarmAction(Strings.SWARM_DEFAULT_PLAY, getSelectedSwarms());
            } else {
                dispatchSwarmAction(mDoubleClickAction.getAction(), getSelectedSwarms());
            }
        } else if (playButton.isEnabled()) {
            /**
             * other actions don't make sense when connected remotely
             */
            dispatchSwarmAction(Strings.SWARM_BROWSER_PLAY, getSelectedSwarms());
        }
    }
}
