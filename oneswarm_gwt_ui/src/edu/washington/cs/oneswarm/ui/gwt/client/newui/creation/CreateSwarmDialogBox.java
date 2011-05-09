package edu.washington.cs.oneswarm.ui.gwt.client.newui.creation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.TreeListener;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.FileTreePanel;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicDirectorySettingsPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;

public class CreateSwarmDialogBox extends OneSwarmDialogBox {

    public static final String[] DEFAULT_TRACKERS = new String[] { "http://tracker.openbittorrent.com:80/announce" };
    public static final String CUSTOM_TRACKER_ITEM = msg.create_swarm_custom_tracker();

    public static final String NOTHING = msg.create_swarm_nothing_selected();

    static final int WIDTH = 500;
    static final int HEIGHT = 200;

    static final int MAX_PATH_DISPLAYED = 50;

    final CreateSwarmDialogBox this_shadow = this;

    final Button addButton = new Button(msg.button_share());
    final Button cancelButton = new Button(msg.button_cancel());
    final Button chooseButton = new Button(msg.button_choose_file());

    final Button permissionsButton = new Button(msg.create_swarm_visibility());

    final RadioButton fileRadioButton = new RadioButton("fileOrDir", msg.create_swarm_file());
    final RadioButton dirRadioButton = new RadioButton("fileOrDir", msg.create_swarm_dir());
    final RadioButton multDirRadioButton = new RadioButton("fileOrDir", msg.create_swarm_mulitple());

    FileTreePanel multiDirFileTree;
    final Panel multiDirPanel = new VerticalPanel();

    VerticalPanel mainPanelShare = null;
    MagicDirectorySettingsPanel mainPanelWatch = null;
    final VerticalPanel mainTabPanel = new VerticalPanel();

    final ListBox defaultTrackersListBox = new ListBox();

    final Label pathLabel = new Label(NOTHING);

    // final CheckBox startSeedingCheckBox = new
    // CheckBox("Start seeding immediately");
    // final CheckBox f2fOnlyCheckBox = new CheckBox("Only share with friends");
    final CheckBox sharePubliclyCheckBox = new CheckBox(msg.create_swarm_share_public());
    final CheckBox shareF2FCheckBox = new CheckBox(msg.create_swarm_share_with_friends());

    String mPath = null;

    EntireUIRoot mRoot = null;

    private boolean useDirChooser = false;
    // private int multiTorrentNum = 0;
    private Label selectLabel;

    private DecoratedTabPanel mTabs;

    ArrayList<PermissionsGroup> permitted_groups = new ArrayList<PermissionsGroup>();

    ClickHandler permissionsClickListener = null;

    public CreateSwarmDialogBox(EntireUIRoot inRoot) {
        super(false, true, true);

        mRoot = inRoot;
        setText(msg.create_swarm_share_files());
        mainTabPanel.setWidth(WIDTH + "px");
        mainTabPanel.setHeight(HEIGHT + "px");

        GWT.runAsync(new RunAsyncCallback() {
            public void onFailure(Throwable reason) {
                Window.alert("Failed to load create swarms dialog javascript: " + reason.toString());
            }

            public void onSuccess() {
                if (mRoot == null) {
                    return;
                }
                CreateSwarmDialogBox dlg = onInitialized();
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setVisible(true);
            }
        });
    }

    protected CreateSwarmDialogBox onInitialized() {
        permissionsClickListener = new ClickHandler() {
            public void onClick(ClickEvent event) {
                OneSwarmDialogBox dlg = new AprioriPermissionsDialog(msg.create_swarm(),
                        msg.create_swarm_visiblity_msg(), permitted_groups,
                        new ApriorPermissionsCallback() {
                            public void cancelled() {
                            }

                            public void permissionsDefined(
                                    ArrayList<PermissionsGroup> permitted_groups) {
                                reflect_advanced_permissions(permitted_groups);
                            }
                        });

                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                dlg.setVisible(true);
            }
        };

        selectLabel = new Label(msg.create_swarm_share_manual_msg());
        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");
        mainTabPanel.add(selectLabel);
        mainTabPanel.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);

        mainPanelShare = createSharePanel();
        mainPanelWatch = new MagicDirectorySettingsPanel();

        mTabs = new DecoratedTabPanel();
        mTabs.addStyleName(TorrentDownloaderDialog.CSS_F2F_TABS);
        mTabs.add(mainPanelShare, msg.create_swarm_share_tab_manual());
        mTabs.add(mainPanelWatch, msg.create_swarm_share_tab_automatic());
        mTabs.setWidth(WIDTH + "px");
        mTabs.setHeight("100%");
        mTabs.addTabListener(new TabListener() {
            public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
                return true;
            }

            public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
                switch (tabIndex) {
                case 0:
                    selectLabel.setText(msg.create_swarm_share_manual_msg());
                    mTabs.setWidth(WIDTH + "px");
                    mainTabPanel.setWidth(WIDTH + "px");
                    if (selectLabel.isAttached()) {
                        selectLabel.setWidth(mainTabPanel.getOffsetWidth() + "px");
                    }
                    break;

                case 1:
                    selectLabel.setText(msg.create_swarm_share_automatic_msg());
                    mTabs.setWidth("350px");
                    mainTabPanel.setWidth("350px");
                    if (selectLabel.isAttached()) {
                        selectLabel.setWidth(mainTabPanel.getOffsetWidth() + "px");
                    }
                    break;
                }
            }
        });

        mTabs.setHeight(HEIGHT - 40 + "px");

        mTabs.selectTab(1);

        mainTabPanel.add(mTabs);
        mainTabPanel.setCellVerticalAlignment(mTabs, VerticalPanel.ALIGN_TOP);

        setWidget(mainTabPanel);

        return this;
    }

    public void onDetach() {
        super.onDetach();

        if (mainPanelWatch.isReadyToSave()) {
            mainPanelWatch.sync();
        }
    }

    private VerticalPanel createSharePanel() {

        VerticalPanel mainPanel = new VerticalPanel();

        mainPanel.setSpacing(3);

        HorizontalPanel radiosPanel = new HorizontalPanel();
        radiosPanel.setSpacing(5);
        // radiosPanel.setWidth("100%");
        fileRadioButton.setValue(true);
        radiosPanel.add(fileRadioButton);
        radiosPanel.add(dirRadioButton);
        radiosPanel.add(multDirRadioButton);
        HelpButton multiHelp = new HelpButton(msg.create_swarm_multiple_help());
        radiosPanel.add(multiHelp);

        mainPanel.add(radiosPanel);

        fileRadioButton.addClickHandler(this);
        dirRadioButton.addClickHandler(this);
        multDirRadioButton.addClickHandler(this);

        chooseButton.addStyleDependentName(OneSwarmCss.SMALL_BUTTON);

        HorizontalPanel choosePathHP = new HorizontalPanel();
        choosePathHP.setWidth("100%");
        choosePathHP.add(chooseButton);
        choosePathHP.setCellWidth(chooseButton, "150px");
        chooseButton.addClickHandler(this);
        choosePathHP.add(pathLabel);
        pathLabel.setWordWrap(false);
        pathLabel.setWidth("245px");
        choosePathHP.setCellVerticalAlignment(pathLabel, VerticalPanel.ALIGN_MIDDLE);

        mainPanel.add(choosePathHP);

        multiDirPanel.setVisible(false);
        mainPanel.add(multiDirPanel);

        HorizontalPanel simple_perms_hp = new HorizontalPanel();
        simple_perms_hp.add(shareF2FCheckBox);
        simple_perms_hp.add(sharePubliclyCheckBox);

        simple_perms_hp.setWidth("75%");

        simple_perms_hp.setCellHorizontalAlignment(shareF2FCheckBox, HorizontalPanel.ALIGN_LEFT);
        simple_perms_hp.setCellHorizontalAlignment(sharePubliclyCheckBox,
                HorizontalPanel.ALIGN_LEFT);
        mainPanel.add(simple_perms_hp);

        // VerticalPanel radiosPath = new VerticalPanel();
        // //radiosPath.add(radiosPanel);
        // radiosPath.add(choosePathHP);
        // centerPanel.add(radiosPath, DockPanel.NORTH);
        //
        /***********************************************************************
         * Tracker details
         */
        HorizontalPanel trackerPanel = new HorizontalPanel();

        Label trackerSelectLabel = new Label(msg.create_swarm_trackers());
        trackerPanel.add(trackerSelectLabel);
        trackerPanel.setCellVerticalAlignment(trackerSelectLabel, VerticalPanel.ALIGN_MIDDLE);
        for (String trackerURL : DEFAULT_TRACKERS) {
            defaultTrackersListBox.addItem(trackerURL);
        }
        defaultTrackersListBox.addItem(CUSTOM_TRACKER_ITEM);
        defaultTrackersListBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                if (defaultTrackersListBox.getItemText(defaultTrackersListBox.getSelectedIndex())
                        .equals(CUSTOM_TRACKER_ITEM)) {
                    String tracker = Window.prompt(msg.create_swarm_trackers_msg(),
                            "http://tracker.oneswarm.net:6969/announce");
                    if (tracker != null) {
                        // since there's no way to insert other than at the end,
                        // first remove the end, then insert, then reinsert
                        // custom item
                        defaultTrackersListBox.removeItem(defaultTrackersListBox.getItemCount() - 1);
                        defaultTrackersListBox.addItem(tracker);
                        defaultTrackersListBox.setItemSelected(
                                defaultTrackersListBox.getItemCount() - 1, true);

                        defaultTrackersListBox.addItem(CUSTOM_TRACKER_ITEM);
                    } else {
                        // don't let this actually stay a selected item since
                        // this isn't really a tracker url.
                        defaultTrackersListBox.setSelectedIndex(0);
                    }
                }
            }
        });

        trackerPanel.add(defaultTrackersListBox);

        mainPanel.add(trackerPanel);

        HorizontalPanel buttonsHP = new HorizontalPanel();
        HorizontalPanel lhs = new HorizontalPanel(), rhs = new HorizontalPanel();
        lhs.add(permissionsButton);
        rhs.add(cancelButton);
        rhs.add(addButton);
        lhs.setSpacing(3);
        rhs.setSpacing(3);
        buttonsHP.setWidth("100%");
        buttonsHP.add(lhs);
        buttonsHP.add(rhs);
        buttonsHP.setCellHorizontalAlignment(lhs, HorizontalPanel.ALIGN_LEFT);
        buttonsHP.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);

        permissionsButton.addClickHandler(permissionsClickListener);

        mainPanel.add(buttonsHP);
        mainPanel.setCellVerticalAlignment(buttonsHP, VerticalPanel.ALIGN_BOTTOM);
        mainPanel.setCellHorizontalAlignment(buttonsHP, HorizontalPanel.ALIGN_RIGHT);

        addButton.setEnabled(false);

        addButton.addClickHandler(this);
        cancelButton.addClickHandler(this);

        // buttonsHP.setSpacing(10);

        shareF2FCheckBox.setValue(true);
        setTrackerStuff(true);
        sharePubliclyCheckBox.setValue(false);

        if (shareF2FCheckBox.getValue()) {
            permitted_groups.add(new PermissionsGroup(PermissionsGroup.ALL_FRIENDS));
        }
        if (sharePubliclyCheckBox.getValue()) {
            permitted_groups.add(new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET));
        }

        shareF2FCheckBox.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (shareF2FCheckBox.getValue() == false) {
                    for (PermissionsGroup g : permitted_groups) {
                        if (g.isAllFriends() == false && g.isPublicInternet() == false) {
                            if (Window.confirm(msg.create_swarm_visibility_clear()) == false) {
                                shareF2FCheckBox.setValue(true);
                                return;
                            } else {
                                break;
                            }
                        }
                    }
                }

                /**
                 * Need to remove any F2F groups we've added...
                 */
                if (shareF2FCheckBox.getValue() == false) {
                    for (int curr = permitted_groups.size() - 1; curr >= 0; curr--) {
                        PermissionsGroup g = permitted_groups.get(curr);
                        if (g.isPublicInternet() == false)
                            permitted_groups.remove(curr);
                    }
                }

                if (sharePubliclyCheckBox.getValue() == false) {
                    setTrackerStuff(true);
                }
            }
        });
        final PermissionsGroup publicGroup = new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET);
        sharePubliclyCheckBox.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (sharePubliclyCheckBox.getValue()) {
                    permitted_groups.add(publicGroup);
                    setTrackerStuff(false);
                } else {
                    permitted_groups.remove(publicGroup);
                    setTrackerStuff(true);
                }
            }
        });

        return mainPanel;
    }

    private void reflect_advanced_permissions(ArrayList<PermissionsGroup> inGroups) {
        sharePubliclyCheckBox.setValue(false);
        boolean sharePublic = inGroups.contains(new PermissionsGroup(
                PermissionsGroup.PUBLIC_INTERNET));
        if (sharePublic) {
            sharePubliclyCheckBox.setValue(true);
        }
        shareF2FCheckBox.setValue(false);
        for (PermissionsGroup g : inGroups) {
            if (g.isPublicInternet() == false)
                shareF2FCheckBox.setValue(true);
        }

        permitted_groups = inGroups;

        if (shareF2FCheckBox.getValue() && !sharePublic) {
            setTrackerStuff(true);
        } else {
            setTrackerStuff(!sharePublic);
        }
    }

    private void setTrackerStuff(boolean f2fOnly) {
        if (f2fOnly) {
            defaultTrackersListBox.setEnabled(false);
            defaultTrackersListBox.setItemText(1, "http://tracker.not.needed.invalid/announce");
            defaultTrackersListBox.setItemSelected(1, true);
        } else {
            defaultTrackersListBox.setEnabled(true);
            defaultTrackersListBox.setItemText(1, CUSTOM_TRACKER_ITEM);
            defaultTrackersListBox.setSelectedIndex(0);
        }
    }

    private void clearPath() {
        pathLabel.setText(NOTHING);
        addButton.setEnabled(false);

        if (!multDirRadioButton.getValue()) {
            addButton.setText(msg.create_swarm());
            if (multiDirFileTree != null) {
                multiDirPanel.remove(multiDirFileTree);
                multiDirPanel.setVisible(false);
                multiDirFileTree = null;

                resize();
            }
        } else {
            addButton.setText(msg.create_swarms());
        }
    }

    public void resize() {
        if (multiDirFileTree == null) {
            System.out.println("got resize: (no multiDirFileTree)");
            selectLabel.setWidth(WIDTH + "px");
        } else {
            System.out.println("got resize: " + multiDirFileTree.getOffsetWidth());
            selectLabel.setWidth(Math.max(WIDTH, multiDirFileTree.getOffsetWidth()) + "px");
        }
    }

    private void setPath(String inPath) {
        mPath = inPath;
        if (inPath == null) {
            pathLabel.setText(NOTHING);
            addButton.setEnabled(false);
        } else {
            // pathLabel.setText(inPath);

            /**
             * We need to truncate this string to make sure that it doesn't
             * overflow...
             */
            String truncated = inPath.substring(Math.max(0, inPath.length() - MAX_PATH_DISPLAYED),
                    inPath.length());
            if (truncated.length() == MAX_PATH_DISPLAYED)
                truncated = "..." + truncated;
            pathLabel.setText(truncated);

            addButton.setEnabled(true);
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(cancelButton)) {
            hide();
        } else if (event.getSource().equals(addButton)) {
            if (multDirRadioButton.getValue()) {
                if (multiDirFileTree.getSelected().size() == 0) {
                    Window.alert(msg.create_swarm_share_warning_select_one_file());
                    return;
                }
            }

            if (multiDirFileTree == null) {
                ArrayList<String> path = new ArrayList<String>();
                path.add(mPath);
                createSwarm(path);
            } else {
                ArrayList<String> paths = multiDirFileTree.getSelected();
                // multiTorrentNum = paths.size();
                addButton.setText(0 + "%");
                createSwarm(paths);
            }

            hide();

        } else if (event.getSource().equals(dirRadioButton)) {
            chooseButton.setText(msg.create_swarm_choose_directory());
            useDirChooser = true;
            clearPath();
        } else if (event.getSource().equals(fileRadioButton)) {
            chooseButton.setText(msg.create_swarm_choose_file());
            useDirChooser = false;
            clearPath();
        } else if (event.getSource().equals(multDirRadioButton)) {
            chooseButton.setText(msg.create_swarm_choose_directory());
            useDirChooser = true;
            clearPath();
        } else if (event.getSource().equals(chooseButton)) {

            chooseButton.setEnabled(false);
            /**
             * Now, a total hack to get choose file / directory. We actually
             * need to do this through RPC since there's no way to get a choose
             * directory dialog box in GWT or HTML in general.
             */
            OneSwarmGWT.log("select file/dir called");
            OneSwarmRPCClient.getService().selectFileOrDirectory(OneSwarmRPCClient.getSessionID(),
                    useDirChooser, new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(String path) {

                            if (path != null) {
                                /**
                                 * In case we select back-to-back 'multiple'
                                 * directories, we clear the last one first.
                                 */
                                if (multiDirFileTree != null) {
                                    multiDirPanel.remove(multiDirFileTree);
                                    multiDirPanel.setVisible(false);
                                    multiDirFileTree = null;
                                    resize();
                                }
                            }

                            setPath(path);
                            chooseButton.setEnabled(true);

                            if (multDirRadioButton.getValue() && path != null) {
                                multiDirFileTree = new FileTreePanel(path);
                                multiDirFileTree.t.addTreeListener(new TreeListener() {
                                    public void onTreeItemSelected(TreeItem item) {
                                    }

                                    public void onTreeItemStateChanged(TreeItem item) {
                                        resize();
                                    }
                                });
                                multiDirPanel.add(multiDirFileTree);
                                multiDirPanel.setVisible(true);
                                System.out.println(multiDirPanel.getOffsetWidth()
                                        + " multidirpanel width");
                            }
                        }
                    });
        } else {
            super.onClick(event);
        }
    }

    private void createSwarm(final ArrayList<String> paths) {
        if (paths.size() > 0) {

            // System.out.println("createSwarmFromLocalFileSystemPath with peritted_groups: "
            // );
            // for( PermissionsGroup s : permitted_groups )
            // {
            // System.out.println(s.getName());
            // }

            OneSwarmRPCClient.getService().createSwarmFromLocalFileSystemPath(
                    OneSwarmRPCClient.getSessionID(), mPath, paths, true,
                    defaultTrackersListBox.getItemText(defaultTrackersListBox.getSelectedIndex()),
                    permitted_groups, new AsyncCallback<Boolean>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(Boolean result) {

                            if (result == false) {
                                System.err.println("swarm creation error");
                            } else {
                                hide();

                                /**
                                 * This will be picked up by the new background
                                 * task manager
                                 */
                                // CreateSwarmProgressDialog dlg = new
                                // CreateSwarmProgressDialog(mRoot);
                                // dlg.show();
                                // dlg.setVisible(false);
                                // dlg.center();
                                // dlg.setVisible(true);
                            }
                        }
                    });
        }
    }
}
