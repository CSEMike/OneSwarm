package edu.washington.cs.oneswarm.ui.gwt.client.filebrowser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollPolicy;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.creation.AprioriPermissionsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.GroupsListSorter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipList;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipListListener;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class TorrentDownloaderDialog extends OneSwarmDialogBox implements Updateable {

    public static final String CSS_F2F_TABS = "os-f2f_tabs";

    private static int WIDTH = 400;

    private DockPanel bottom = new DockPanel();

    ClickListener cancelListener = new ClickListener() {
        public void onClick(Widget sender) {
            hide();
        }
    };
    private final int channelId;
    private List<CheckBox> checkboxes = new ArrayList<CheckBox>();
    boolean checkEverything = false;
    private FileListLite[] collection = new FileListLite[0];
    private boolean doneTorrentDownload = false;

    private Button downloadButton = new Button(msg.swarm_browser_button_download());

    ClickListener downloadClickListener = new ClickListener() {
        public void onClick(Widget sender) {

            downloadButton.setEnabled(false);
            downloadTop.setEnabled(false);
            for (CheckBox c : checkboxes) {
                c.setEnabled(false);
            }
            startTorrentDownload();
            if (mToClose != null) {
                mToClose.hide();
            }
        }
    };
    private boolean downloadInOrder = false;

    private Button downloadTop = new Button(msg.swarm_browser_button_download());

    private ClickListener enableDisableDLButton = new ClickListener() {
        public void onClick(Widget sender) {
            if (friendId == 0) {
                downloadButton.setEnabled(false);
                downloadTop.setEnabled(false);
                return; // we can never download from ourself
            }

            // if nothing is selected, we can't download
            boolean good = false;
            for (CheckBox c : checkboxes) {
                if (c.isChecked() == true) {
                    good = true;
                    break;
                }
            }
            if (good) {
                downloadButton.setEnabled(true);
                downloadTop.setEnabled(true);
            } else {
                downloadButton.setEnabled(false);
                downloadTop.setEnabled(false);
            }
        }
    };
    // private FileListLite file = null;
    // private final boolean filesAvailable;

    private final int friendId;
    private boolean groupsLoaded = false;

    private InfoPanel infoPanel;

    private String mCustomSavePath = null;

    private final int metaInfoLengthHint;
    private Long[] mFriendsDelays;
    private String[] mFromFriends;
    boolean mFromURL = false;

    private EntireUIRoot mRoot = null;;

    private DecoratedTabPanel mTabs;

    private OneSwarmDialogBox mToClose = null;;

    PermissionsPanel permissionsPanel = null;

    private Button selectAllTop = new Button(msg.button_select_all());
    private Button selectNoneTop = new Button(msg.button_select_none());
    private Button cancelTop = new Button(msg.button_cancel());

    private Button selectAllButton = new Button(msg.button_select_all());
    private Button selectNoneButton = new Button(msg.button_select_none());

    private FilesPanel filesPanel = new FilesPanel();

    ClickListener selectAllListener = new ClickListener() {
        public void onClick(Widget arg0) {
            for (CheckBox c : checkboxes) {
                c.setChecked(true);
            }
        }
    };

    private FileListLite selectedFile = null;

    ClickListener selectNoneListener = new ClickListener() {
        public void onClick(Widget arg0) {
            for (CheckBox c : checkboxes) {
                c.setChecked(false);
            }
        }
    };

    private Label statusLabel = new Label("Getting info: sending request");

    private int torrentDownloadId = -1;

    private final static String IN_ORDER_DOWNLOAD_COOKIE = "in_order_default";

    public TorrentDownloaderDialog(EntireUIRoot inRoot, int inDownloadID) {

        mRoot = inRoot;
        /**
         * friend state that's ignored...
         */
        friendId = -1;
        channelId = -1;
        // filesAvailable = false;
        metaInfoLengthHint = -1;

        this.setText("Downloading...");

        downloadButton.setEnabled(false);
        downloadTop.setEnabled(false);

        mFromURL = true;
        torrentDownloadId = inDownloadID;

        /**
         * torrents downloaded from URLs are public...
         */
        permissionsPanel = new PermissionsPanel();

        initUI();

        OneSwarmGWT.addToUpdateTask(this);
    }

    /**
     * lazy wrapper for channel id=0 and length hint = 0
     */
    public TorrentDownloaderDialog(EntireUIRoot inRoot, OneSwarmDialogBox toCloseIfDownload,
            int friendId, FileListLite selectedFile, boolean filesAvailable) {
        this(inRoot, toCloseIfDownload, friendId, 0, 0, selectedFile, filesAvailable, null, null);
    }

    public TorrentDownloaderDialog(EntireUIRoot inRoot, OneSwarmDialogBox toCloseIfDownload,
            int friendId, int channelId, int metaInfoLengthHint, FileListLite selectFile,
            boolean filesAvailable, String[] inFromFriends, Long[] inDelays) {
        super(false, false, true);

        this.setText(msg.torrent_download_details() + ": "
                + StringTools.truncate(selectFile.getCollectionName(), 45, true));
        this.selectedFile = selectFile;
        mRoot = inRoot;
        mToClose = toCloseIfDownload;

        mFromFriends = inFromFriends;
        mFriendsDelays = inDelays;

        permissionsPanel = new PermissionsPanel();
        /*
         * wait with init until after torrent is downloaded
         */
        this.friendId = friendId;
        this.channelId = channelId;
        this.metaInfoLengthHint = metaInfoLengthHint;
        // this.filesAvailable = filesAvailable;
        downloadButton.setEnabled(false);
        downloadTop.setEnabled(false);
        initUI();
        // if (filesAvailable) {
        // this.updateCollection(friendId, true);
        // }
        if (friendId != 0) {
            startMetaInfoDownload();
        } else {
            completed();
        }
    }

    private void completed() {
        bottom.remove(statusLabel);
        // don't allow downloading own stuff
        if (friendId != 0) {
            doneTorrentDownload = true;

            if (groupsLoaded && doneTorrentDownload) {
                downloadButton.setEnabled(true);
                downloadTop.setEnabled(true);
            }
        }

        selectNoneTop.addClickListener(selectNoneListener);
        selectNoneTop.addClickListener(enableDisableDLButton);

        selectNoneButton.addClickListener(selectNoneListener);
        selectNoneButton.addClickListener(enableDisableDLButton);

        selectAllTop.addClickListener(selectAllListener);
        selectAllTop.addClickListener(enableDisableDLButton);

        selectAllButton.addClickListener(selectAllListener);
        selectAllButton.addClickListener(enableDisableDLButton);

        HorizontalPanel f = new HorizontalPanel();
        f.setSpacing(3);
        f.add(selectAllButton);
        f.add(selectNoneButton);
        bottom.add(f, DockPanel.WEST);
        bottom.setCellVerticalAlignment(f, DockPanel.ALIGN_MIDDLE);
        bottom.setCellHorizontalAlignment(f, DockPanel.ALIGN_LEFT);
    }

    private ScrollTable createSourcesTable() {
        int[] widths = new int[] { WIDTH - 100, 100 };
        String[] cols = new String[] { "Friend", "Delay" };

        final FixedWidthGrid data = new FixedWidthGrid(0, cols.length);
        FixedWidthFlexTable header = new FixedWidthFlexTable();

        for (int i = 0; i < cols.length; i++) {
            header.setText(0, i, cols[i]);

            header.setColumnWidth(i, widths[i]);
            data.setColumnWidth(i, widths[i]);
        }

        data.setSelectionPolicy(SelectionPolicy.MULTI_ROW);

        ScrollTable table = new ScrollTable(data, header);
        table.setScrollPolicy(ScrollPolicy.DISABLED);
        table.setResizePolicy(ScrollTable.ResizePolicy.FILL_WIDTH);

        if (table.getDataTable().getRowCount() != mFromFriends.length) {
            table.getDataTable().resize(mFromFriends.length, 2);
        }
        for (int i = 0; i < mFromFriends.length; i++) {
            System.out.println("source: " + mFromFriends[i]);
            data.setText(i, 0, mFromFriends[i]);
            data.setText(i, 1, (mFriendsDelays == null ? "Unknown" : mFriendsDelays[i].toString()
                    + " ms"));
        }

        return table;
    }

    public void everythingChecked() {
        checkEverything = true;
    }

    public void hide() {
        OneSwarmGWT.removeFromUpdateTask(TorrentDownloaderDialog.this);
        OneSwarmGWT.log("removing from update task");
        super.hide();
    }

    private void initUI() {
        final VerticalPanel mainPanel = new VerticalPanel();

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(3);
        Button cancelButton = new Button(msg.button_cancel());
        cancelTop.addClickListener(cancelListener);
        cancelButton.addClickListener(cancelListener);
        // cancelButton.setWidth("100px");

        buttonPanel.add(cancelButton);

        // okButton.setWidth("100px");
        downloadTop.addClickListener(downloadClickListener);
        downloadButton.addClickListener(downloadClickListener);
        buttonPanel.add(downloadButton);

        buttonPanel.setCellHorizontalAlignment(downloadButton, HorizontalPanel.ALIGN_RIGHT);
        buttonPanel.setCellHorizontalAlignment(cancelButton, HorizontalPanel.ALIGN_RIGHT);
        buttonPanel.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_LEFT);

        bottom.setWidth("100%");
        bottom.add(buttonPanel, DockPanel.EAST);
        bottom.setCellHorizontalAlignment(buttonPanel, HorizontalPanel.ALIGN_RIGHT);

        bottom.add(statusLabel, DockPanel.WEST);
        bottom.setCellVerticalAlignment(statusLabel, VerticalPanel.ALIGN_MIDDLE);
        bottom.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_LEFT);

        VerticalPanel sourcesPanel = new VerticalPanel();

        if (mFromFriends == null) {
            sourcesPanel.add(new Label(msg.torrent_download_sources_not_available()));
        } else {
            // sourcesPanel.add(new Label(mFromFriends.length + " sources:"));
            sourcesPanel.add(createSourcesTable());
        }

        infoPanel = new InfoPanel(selectedFile, mFromFriends);

        mTabs = new DecoratedTabPanel();
        mTabs.addStyleName(CSS_F2F_TABS);
        mTabs.add(infoPanel, msg.torrent_download_tab_general_info());
        mTabs.add(filesPanel, msg.settings_tab_files());
        if (mFromFriends != null) {
            mTabs.add(sourcesPanel, msg.torrent_download_tab_sources());
        }
        mTabs.add(permissionsPanel, msg.torrent_download_tab_visibility());
        mTabs.setWidth(WIDTH + "px");
        mTabs.addTabListener(new TabListener() {

            public boolean onBeforeTabSelected(SourcesTabEvents arg0, int tabIndex) {
                if (tabIndex != 0) {
                    return doneTorrentDownload;
                }
                return true;
            }

            public void onTabSelected(SourcesTabEvents arg0, int tabIndex) {
                if (tabIndex == 1) {
                    selectAllButton.setVisible(true);
                    selectNoneButton.setVisible(true);
                } else {
                    selectAllButton.setVisible(false);
                    selectNoneButton.setVisible(false);
                }

                if (tabIndex == 3) {
                    mainPanel.setWidth(AprioriPermissionsDialog.WIDTH + "px");
                    mainPanel.setHeight(AprioriPermissionsDialog.HEIGHT + "px");
                    // selectLabel.setWidth(AprioriPermissionsDialog.WIDTH +
                    // "px");
                } else {
                    // selectLabel.setWidth(WIDTH + "px");
                    mainPanel.setWidth(WIDTH + "px");
                    mainPanel.setHeight("");
                }
            }
        });
        mainPanel.add(mTabs);

        mainPanel.add(bottom);

        mainPanel.setWidth(WIDTH + "px");

        mTabs.selectTab(0);

        this.setWidget(mainPanel);
    }

    private void startMetaInfoDownload() {

        List<FileListLite> filesToDownload = new ArrayList<FileListLite>();

        for (int i = 0; i < collection.length; i++) {
            if (checkboxes.get(i).isChecked()) {
                filesToDownload.add(collection[i]);
            }
        }

        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<Integer> cb = new AsyncCallback<Integer>() {
            public void onFailure(Throwable caught) {
                // well, do nothing...
                OneSwarmGWT.log("error " + caught.getMessage());
            }

            public void onSuccess(Integer result) {
                OneSwarmGWT.log("added download, res=" + result);
                torrentDownloadId = result;
            }
        };
        service.downloadTorrent(session, friendId, channelId, selectedFile.getCollectionId(),
                metaInfoLengthHint, cb);
        OneSwarmGWT.addToUpdateTask(this);
    }

    private void startTorrentDownload() {

        List<FileListLite> filesToDownload = new ArrayList<FileListLite>();

        for (int i = 0; i < collection.length; i++) {
            if (checkboxes.get(i).isChecked()) {
                filesToDownload.add(collection[i]);
            }
        }

        final String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync addTorrentService = OneSwarmRPCClient.getService();

        ArrayList<PermissionsGroup> permGroups = permissionsPanel.sharing_with_groups.getMembers();

        if (permGroups.size() == 0)
            System.out.println("no permitted groups!");

        System.out.println("permitted:");
        for (PermissionsGroup g : permGroups)
            System.out.println("adding, permitted: " + g);
        System.out.println("end permitted");
        boolean noStream = !downloadInOrder;
        System.out.println("starting download, nostream=" + noStream);
        addTorrentService.addTorrent(session, torrentDownloadId,
                filesToDownload.toArray(new FileListLite[0]), permGroups, mCustomSavePath,
                noStream, new AsyncCallback<Boolean>() {

                    public void onFailure(Throwable caught) {
                        OneSwarmGWT.log("Add torrent request failed");
                        caught.printStackTrace();
                    }

                    public void onSuccess(Boolean result) {
                        Boolean res = (Boolean) result;
                        if (res.booleanValue()) {
                            // OneSwarmGWT.log("Torrent added successfully,
                            // attempting
                            // front-end refresh");
                            mRoot.refreshSwarms();
                            System.out.println("add torrent succeeded");
                        } else {
                            OneSwarmGWT.log("Add torrent failed");
                        }
                    }
                });

        hide();
    }

    private void update() {
        filesPanel.update();
    }

    public void update(int count) {

        if (torrentDownloadId != -1) {
            final String session = OneSwarmRPCClient.getSessionID();
            OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
            service.getTorrentDownloadProgress(session, torrentDownloadId,
                    new AsyncCallback<Integer>() {

                        public void onFailure(Throwable caught) {
                        }

                        public void onSuccess(Integer result) {
                            Integer progress = (Integer) result;
                            // OneSwarmGWT.log("downloaded: " + progress);
                            statusLabel.setText("Getting info: " + progress + "%");
                            if (progress.intValue() == 100) {
                                OneSwarmGWT.log("download finished");
                                completed();
                                updateCollection(friendId, false);
                                OneSwarmGWT.removeFromUpdateTask(TorrentDownloaderDialog.this);
                            } else if (progress.intValue() == -2) {
                                statusLabel.setText("Sorry, an error occurred.");
                            }
                        }
                    });
        }
    }

    private void updateCollection(final int friendId, boolean filesAvailable) {
        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<FileListLite[]> cb = new AsyncCallback<FileListLite[]>() {
            public void onFailure(Throwable caught) {
                // well, do nothing...
                OneSwarmGWT.log("error " + caught.getMessage());
            }

            public void onSuccess(final FileListLite[] result) {
                OneSwarmGWT.log("got list of files in torrent, len=" + result.length);
                collection = result;

                if (mFromURL) {
                    OneSwarmRPCClient.getService().getTorrentName(OneSwarmRPCClient.getSessionID(),
                            torrentDownloadId, new AsyncCallback<String>() {
                                public void onFailure(Throwable caught) {
                                    caught.printStackTrace();
                                }

                                public void onSuccess(String name) {

                                    TorrentDownloaderDialog.this.setText(msg
                                            .torrent_download_details() + ": " + name);

                                    infoPanel.updateName(name);
                                    long sum = 0;
                                    System.out.println("got file list, size: " + result.length);
                                    for (FileListLite f : result) {
                                        sum += f.getFileSize();
                                    }
                                    infoPanel.updateSize(sum);
                                    infoPanel.updateFilesCount(result.length);

                                    filesPanel.update();

                                    /*
                                     * init the permissions, this was from a url
                                     * so enable public by default
                                     */
                                    permissionsPanel.init(true);
                                }
                            });
                } else {
                    // check if we should set the default permissions to only
                    // the source friend
                    if (result.length > 0 && result[0].isOneSwarmNoShare()) {
                        /*
                         * aha, this is a noShare torrent, set the permissions
                         * to the source friend only
                         */
                        OneSwarmRPCClient.getService().getFriends(OneSwarmRPCClient.getSessionID(),
                                0, true, true, new AsyncCallback<FriendList>() {
                                    public void onFailure(Throwable caught) {
                                    }

                                    public void onSuccess(FriendList result) {

                                        for (FriendInfoLite friend : result.getFriendList()) {
                                            if (friend.getConnectionId() == friendId) {
                                                permissionsPanel.init(friend);
                                                return;
                                            }
                                        }
                                    }
                                });
                    } else {
                        permissionsPanel.init(false);
                    }
                    update();
                } // if fromURL (else)

                /**
                 * If this archive has multiple files -- we'll now only select
                 * one. Since users might not really prefer this (especially in
                 * the case of photo / music archives) -- show them a list of
                 * files so they know what's coming by default
                 */
                if (result.length > 1) {
                    mTabs.selectTab(1);
                }
            }
        };

        if (filesAvailable) {
            if (mFromURL == false) {
                service.getFileList(session, friendId, "id:" + selectedFile.getCollectionId(), 0,
                        1000, 24 * 60 * 60 * 1000, cb);
            } else {
                service.getTorrentFiles(session, torrentDownloadId, cb);
            }
        } else {
            service.getTorrentFiles(session, torrentDownloadId, cb);
        }

    }

    class FilesPanel extends VerticalPanel {

        private ScrollTable fileTable;

        private FixedWidthGrid mFilesData = null;

        private Label selectLabel;
        private HorizontalPanel topButtons;

        public FilesPanel() {
            String[] dl_cols = new String[] { "DL", "Name", "Size" };
            FixedWidthFlexTable filesHeader = new FixedWidthFlexTable();

            int[] widths = new int[] { 20, 300, 70 };

            mFilesData = new FixedWidthGrid(0, dl_cols.length);

            for (int i = 0; i < dl_cols.length; i++) {
                filesHeader.setText(0, i, dl_cols[i]);

                filesHeader.setColumnWidth(i, widths[i]);
                mFilesData.setColumnWidth(i, widths[i]);
            }

            mFilesData.setSelectionPolicy(SelectionPolicy.MULTI_ROW);

            fileTable = new ScrollTable(mFilesData, filesHeader);
            fileTable.setScrollPolicy(ScrollPolicy.DISABLED);
            fileTable.setResizePolicy(ScrollTable.ResizePolicy.FILL_WIDTH);
            fileTable.setScrollPolicy(ScrollPolicy.DISABLED);

            mFilesData.addTableListener(new TableListener() {
                public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
                    CheckBox cb = (CheckBox) (mFilesData.getWidget(row, 0));
                    cb.setChecked(!cb.isChecked());
                    TorrentDownloaderDialog.this.enableDisableDLButton.onClick(null); // updates
                    // the
                    // 'download'
                    // button
                }
            });

            final VerticalPanel mainPanel = new VerticalPanel();

            selectLabel = new Label(msg.torrent_download_header());
            selectLabel.addStyleName(CSS_DIALOG_HEADER);
            selectLabel.setWidth(WIDTH + "px");
            mainPanel.add(selectLabel);

            selectAllTop.addStyleName(OneSwarmCss.SMALL_BUTTON);
            selectNoneTop.addStyleName(OneSwarmCss.SMALL_BUTTON);
            cancelTop.addStyleName(OneSwarmCss.SMALL_BUTTON);
            downloadTop.addStyleName(OneSwarmCss.SMALL_BUTTON);
            HorizontalPanel lhs = new HorizontalPanel(), rhs = new HorizontalPanel();
            lhs.add(selectAllTop);
            lhs.add(selectNoneTop);
            lhs.setSpacing(3);
            rhs.add(cancelTop);
            rhs.add(downloadTop);
            rhs.setSpacing(3);
            topButtons = new HorizontalPanel();
            topButtons.add(lhs);
            topButtons.add(rhs);
            topButtons.setWidth("100%");
            topButtons.setCellHorizontalAlignment(lhs, HorizontalPanel.ALIGN_LEFT);
            topButtons.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);

            // will show if we have too many items
            topButtons.setVisible(false);
            mainPanel.add(topButtons);

            super.add(mainPanel);
            super.add(fileTable);
            // panel.add(statusLabel);
        }

        private void update() {
            // OneSwarmGWT.log("running update");
            fileTable.setVisible(false);
            mFilesData.clear();
            checkboxes.clear();

            long sum = 0;

            if (collection.length > 20) {
                topButtons.setVisible(true);
            }

            if (mFilesData.getRowCount() != collection.length) {
                mFilesData.resize(collection.length, 3);
            }
            for (int i = 0; i < collection.length; i++) {
                FileListLite f = collection[i];
                final CheckBox b = new CheckBox();
                b.addClickListener(enableDisableDLButton);

                b.addClickListener(new ClickListener() {
                    public void onClick(Widget sender) {
                        b.setChecked(!b.isChecked());
                    }
                });
                /**
                 * only selected file checked by default now
                 */
                if (checkEverything || selectedFile == null
                        || f.getFileName().equals(selectedFile.getFileName())) {
                    b.setChecked(true);
                } else {
                    b.setChecked(false);
                }
                checkboxes.add(b);
                mFilesData.setWidget(i, 0, b);
                mFilesData.setText(i, 1, f.getFileName());
                mFilesData.setText(i, 2, StringTools.formatRate(f.getFileSize()));

                sum += collection[i].getFileSize();

                // OneSwarmGWT.log("adding: " + f.toString());
            }

            infoPanel.updateSize(sum);
            infoPanel.updateFilesCount(collection.length);

            fileTable.setVisible(true);

        }
    }

    private class InfoPanel extends VerticalPanel {

        private Anchor filesLabel;
        private Label locationLabel;
        private Grid mGrid;
        private Label sizeLabel;
        private Widget sourcesLabel;

        public InfoPanel(FileListLite fileListLite, String[] sources) {

            mGrid = new Grid(sources != null ? 5 : 4, 2);
            int row = 0;
            mGrid.setWidget(row, 0, new Label("Swarm:"));
            if (fileListLite != null) {
                mGrid.setWidget(row, 1, new Label(fileListLite.getCollectionName()));
            } else {
                mGrid.setWidget(row, 1, new Label("loading..."));
            }

            row++;
            mGrid.setWidget(row, 0, new Label("Files:"));
            if (fileListLite != null) {
                filesLabel = new Anchor("" + fileListLite.getTotalFilesInGroup());
            } else {
                filesLabel = new Anchor("loading...");
            }
            filesLabel.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    // System.out.println("files anchor selected");
                    mTabs.selectTab(1);
                }
            });
            mGrid.setWidget(row, 1, filesLabel);

            row++;
            mGrid.setWidget(row, 0, new Label("Size:"));
            sizeLabel = new Label("loading...");
            mGrid.setWidget(row, 1, sizeLabel);

            row++;

            mGrid.setWidget(row, 0, new Label("Save to:"));
            HorizontalPanel locationPanel = new HorizontalPanel();
            locationLabel = new Label("(default)");
            locationPanel.add(locationLabel);
            final Button locationButton = new Button("Browse");
            if (OneSwarmGWT.isRemoteAccess()) {
                locationButton.setEnabled(false);
            }
            locationButton.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    locationButton.setEnabled(false);
                    OneSwarmRPCClient.getService().selectFileOrDirectory(
                            OneSwarmRPCClient.getSessionID(), true, new AsyncCallback<String>() {
                                public void onFailure(Throwable caught) {
                                    System.err.println(caught.toString());
                                }

                                public void onSuccess(String result) {
                                    locationButton.setEnabled(true);
                                    if (result != null) {
                                        mCustomSavePath = result;
                                        locationLabel.setText(StringTools
                                                .truncate(result, 50, true));
                                    }
                                }
                            });
                }
            });
            locationButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
            locationPanel.add(locationButton);
            locationPanel.setCellVerticalAlignment(locationLabel, VerticalPanel.ALIGN_MIDDLE);
            locationPanel.setSpacing(3);
            mGrid.setWidget(row, 1, locationPanel);

            if (sources != null) {
                row++;

                mGrid.setWidget(row, 0, new Label("Sources:"));
                if (sources == null) {
                    sourcesLabel = new Label("(unknown)");
                } else {
                    Anchor a = new Anchor("" + sources.length);
                    a.addClickListener(new ClickListener() {
                        public void onClick(Widget sender) {
                            mTabs.selectTab(2);
                        }
                    });
                    sourcesLabel = a;
                }
                mGrid.setWidget(row, 1, sourcesLabel);
            }
            super.add(mGrid);

            HorizontalPanel streamCheckPanel = new HorizontalPanel();
            // streamCheckPanel.setSpacing(7);
            // streamCheckPanel.setWidth("100%");

            /*
             * figure out if we should default to in order download
             */
            // first check for the cookie
            if (Cookies.getCookie(IN_ORDER_DOWNLOAD_COOKIE) != null) {
                downloadInOrder = Boolean.parseBoolean(Cookies.getCookie(IN_ORDER_DOWNLOAD_COOKIE));
            } else {
                /*
                 * else, set it to stream for media files and not to stream for
                 * other
                 */
                if (selectedFile != null
                        && OneSwarmConstants.InOrderType.getType(selectedFile.getFileName()) != null) {
                    downloadInOrder = true;
                }
            }

            final CheckBox streamCheck = new CheckBox("Streaming download");
            streamCheck.setValue(downloadInOrder);
            streamCheck.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent sender) {
                    downloadInOrder = streamCheck.getValue();
                    Cookies.setCookie(IN_ORDER_DOWNLOAD_COOKIE, downloadInOrder + "",
                            OneSwarmConstants.TEN_YEARS_FROM_NOW);
                }
            });

            streamCheckPanel.add(streamCheck);
            HelpButton help = new HelpButton(
                    "Streaming downloads files in-order so you can play them during transfer. But, downloading out-of-order may improve download speeds.");
            streamCheckPanel.add(help);
            streamCheckPanel.setCellWidth(help, "35px");
            streamCheckPanel.setCellHorizontalAlignment(help, ALIGN_CENTER);
            super.add(streamCheckPanel);

        }

        public void updateFilesCount(int inCount) {
            filesLabel.setText(inCount + "");
        }

        public void updateName(String inName) {
            mGrid.setWidget(0, 1, new Label(inName));
        }

        public void updateSize(long inSize) {
            // mGrid.setWidget(2, 1, new Label("" +
            // Strings.formatRate(inSize)));
            sizeLabel.setText(StringTools.formatRate(inSize));
        }
    }

    class PermissionsPanel extends VerticalPanel {

        boolean all_friends_added = true;

        List<PermissionsGroup> initial_groups = null;
        VerticalPanel mainPanel = this;

        private MembershipList<PermissionsGroup> sharing_with_groups;

        public PermissionsPanel() {
            super();
            add(new Label(msg.loading()));
        }

        public void init(boolean includePublic) {
            this.init(includePublic, null);
        }

        private void init(final boolean includePublic, final FriendInfoLite friendOnly) {
            OneSwarmRPCClient.getService().getAllGroups(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<ArrayList<PermissionsGroup>>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                            hide();
                        }

                        public void onSuccess(ArrayList<PermissionsGroup> result) {

                            initial_groups = new ArrayList<PermissionsGroup>();

                            /*
                             * set the default permissions
                             */
                            if (friendOnly != null) {
                                String friendPublicKey = friendOnly.getPublicKey();
                                for (PermissionsGroup permissionsGroup : result) {
                                    if (permissionsGroup.isUserGroup()
                                            && permissionsGroup.containsUser(friendPublicKey)) {
                                        initial_groups.add(permissionsGroup);
                                        break;
                                    }
                                }
                            } else {
                                initial_groups.add(new PermissionsGroup(
                                        PermissionsGroup.ALL_FRIENDS));
                                if (includePublic) {
                                    initial_groups.add(new PermissionsGroup(
                                            PermissionsGroup.PUBLIC_INTERNET));
                                }
                            }

                            initThisUI(result);
                            groupsLoaded = true;

                            if (groupsLoaded && doneTorrentDownload) {
                                downloadButton.setEnabled(true);
                                downloadTop.setEnabled(true);
                            }
                        }
                    });
        }

        public void init(FriendInfoLite friendOnly) {
            this.init(false, friendOnly);
        }

        private void initThisUI(final List<PermissionsGroup> all_groups) {
            mainPanel.remove(0);

            VerticalPanel bottomPane = new VerticalPanel();

            bottomPane.setWidth(AprioriPermissionsDialog.WIDTH + "px");

            List<PermissionsGroup> swarm_groups = new ArrayList<PermissionsGroup>();
            for (PermissionsGroup g : initial_groups) {
                swarm_groups.add(g);
            }

            List<PermissionsGroup> all_sub_shared = new ArrayList<PermissionsGroup>();
            for (PermissionsGroup g : all_groups) {
                if (!swarm_groups.contains(g)) {
                    all_sub_shared.add(g);
                }
            }

            final MembershipList<PermissionsGroup> available_groups = new MembershipList<PermissionsGroup>(
                    "Available groups:", true, all_groups, swarm_groups, true,
                    new GroupsListSorter());
            sharing_with_groups = new MembershipList<PermissionsGroup>("Will share with:", false,
                    all_groups, all_sub_shared, true);

            final PermissionsGroup public_net = new PermissionsGroup(
                    PermissionsGroup.PUBLIC_INTERNET);
            final PermissionsGroup all_friends = new PermissionsGroup(PermissionsGroup.ALL_FRIENDS);
            Set<PermissionsGroup> hs = new HashSet<PermissionsGroup>();
            hs.add(public_net);

            all_friends_added = false;
            for (PermissionsGroup g : swarm_groups) {
                if (g.isAllFriends()) {
                    all_friends_added = true;
                }
            }

            available_groups.addListener(new MembershipListListener<PermissionsGroup>() {
                public void objectEvent(MembershipList<PermissionsGroup> list,
                        PermissionsGroup inObject) {
                    System.out.println("groups remove event: " + inObject);
                    sharing_with_groups.restoreExcluded(inObject);

                    /**
                     * Here we are removing some group from available groups and
                     * adding to sharing with. If we're adding something that's
                     * NOT the 'public' group and we're in 'share with all
                     * friends' mode, we need to remove all the more specific
                     * groups.
                     */
                    if (all_friends_added && inObject.isPublicInternet() == false) {
                        System.out.println("adding specific group when all friends is present");
                        all_friends_added = false;
                        sharing_with_groups.addExcluded(all_friends);
                        available_groups.restoreExcluded(all_friends);
                    }
                    /**
                     * Here we are adding the 'all friends' object, so we need
                     * to remove all the previous, more specific groups.
                     */
                    else if (inObject.isAllFriends()) {
                        all_friends_added = true;
                        // need to remove anything more specific than this.
                        for (PermissionsGroup g : all_groups) {
                            if (g.isPublicInternet() == false && g.isAllFriends() == false) {
                                sharing_with_groups.addExcluded(g);
                                available_groups.restoreExcluded(g);
                            }
                        }
                    }
                }
            });

            sharing_with_groups.addListener(new MembershipListListener<PermissionsGroup>() {
                public void objectEvent(MembershipList<PermissionsGroup> list,
                        PermissionsGroup inObject) {
                    System.out.println("permitted remove event: " + inObject);
                    available_groups.restoreExcluded(inObject);
                }
            });

            HorizontalPanel hp = new HorizontalPanel();
            hp.setWidth("100%");

            hp.add(sharing_with_groups);
            available_groups.setWidth("100%");
            hp.add(available_groups);
            hp.setSpacing(3);
            hp.setCellWidth(available_groups, "49%");
            hp.setCellWidth(sharing_with_groups, "49%");

            hp.setCellHorizontalAlignment(sharing_with_groups, HorizontalPanel.ALIGN_LEFT);
            hp.setCellHorizontalAlignment(available_groups, HorizontalPanel.ALIGN_RIGHT);

            bottomPane.add(hp);

            mainPanel.add(bottomPane);
        }
    }
}
