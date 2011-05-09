package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendInfo.SortableNamePanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details.TransferColumnSorter;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;

public class FriendsDetailsTable extends ScrollTable {

    interface SelectionCallback {
        public void deselectedAll();

        public void somethingSelected();
    }

    interface RefreshCallback {
        public void refreshed(FriendList result);
    }

    public static class HeaderWithWidth {
        public String name;
        public int width;
        public boolean center;

        public HeaderWithWidth(String name, int width) {
            this(name, width, false);
        }

        public HeaderWithWidth(String name, int width, boolean center) {
            this.name = name;
            this.width = width;
            this.center = center;
        }

    }

    private static final HeaderWithWidth[] COLUMNS = new HeaderWithWidth[] {
            new HeaderWithWidth("", 15),

            new HeaderWithWidth("Name", 80, true), new HeaderWithWidth("Last connect", 40),

            new HeaderWithWidth("Ratio", 25), new HeaderWithWidth("Received", 30),

            new HeaderWithWidth("Sent", 30), new HeaderWithWidth("Limited", 25),

            new HeaderWithWidth("Allow chat", 35), new HeaderWithWidth("Date added", 38),

            new HeaderWithWidth("Source", 55) };

    // private SwarmsBrowser mSwarmsBrowser;
    private FixedWidthGrid mData = null;
    private FixedWidthFlexTable mHeader = null;
    // private long mNextUpdate = System.currentTimeMillis();
    // private FriendInfoLite[] friends;
    // private ArrayList<FriendInfo> friendinfos = new ArrayList<FriendInfo>();
    // private ArrayList<FrendInfoEncapsulatingCheckBox> selectors = new
    // ArrayList<FrendInfoEncapsulatingCheckBox>();
    // private boolean firstexecution = true;
    private Map<String, Integer> newFriendRequestCounts = new HashMap<String, Integer>();
    // private boolean noremoveoperation = true;
    // private int currentwidget = 0;
    private List<SelectionCallback> mSelectionCallbacks = new ArrayList<SelectionCallback>();
    private RefreshCallback mRefreshCallback;
    private boolean mShowBlocked = false;

    public FriendsDetailsTable(SwarmsBrowser swarmbrowser, RefreshCallback refreshCallback) {
        super(new FixedWidthGrid(0, COLUMNS.length - 1) {
            @Override
            protected int getInputColumnWidth() {
                return COLUMNS[0].width;
            }
        }, new FixedWidthFlexTable());
        // mSwarmsBrowser = swarmbrowser;
        mData = getDataTable();
        mHeader = getHeaderTable();

        // mData.addSortableColumnsListener(new SortableColumnsListener(){
        // public void onColumnSorted(ColumnSortList sortList) {
        // sortList.getPrimaryColumn();
        // }});

        setScrollPolicy(ScrollPolicy.DISABLED);
        mData.setSelectionPolicy(SelectionPolicy.CHECKBOX);
        setResizePolicy(ResizePolicy.FILL_WIDTH);
        setupHeader();
        mData.setColumnSorter(new TransferColumnSorter());

        mRefreshCallback = refreshCallback;

        mHeader.setWidth("100%");
        mData.setWidth("100%");

        mData.addTableListener(new TableListener() {
            public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
                if (mData.isRowSelected(row)) {
                    mData.deselectRow(row);
                } else {
                    mData.selectRow(row, false);
                }
                selectionChangedCheckIfListenersShouldBeNotified();
            }
        });

        mData.addRowSelectionHandler(new RowSelectionHandler() {
            public void onRowSelection(RowSelectionEvent event) {
                selectionChangedCheckIfListenersShouldBeNotified();
            }
        });

        resizeTable();
        refresh();

    }

    private void selectionChangedCheckIfListenersShouldBeNotified() {
        Set<Integer> selectedRows = mData.getSelectedRows();
        if (selectedRows.size() > 0) {
            for (SelectionCallback c : mSelectionCallbacks) {
                c.somethingSelected();
            }
        } else {
            for (SelectionCallback c : mSelectionCallbacks) {
                c.deselectedAll();
            }
        }
    }

    /*
     * resize the table but keep the checkboxes aligned
     */
    private void resizeTable() {
        for (int i = 0; i < COLUMNS.length; i++) {
            mHeader.setColumnWidth(i, COLUMNS[i].width);
            if (i < COLUMNS.length - 1) {
                mData.setColumnWidth(i, COLUMNS[i + 1].width);
            }
        }
        fillWidth();
        redraw();
    }

    private void setupHeader() {
        for (int i = 0; i < COLUMNS.length; i++) {
            mHeader.setText(0, i, COLUMNS[i].name);
            if (COLUMNS[i].center) {
                mHeader.getCellFormatter().setHorizontalAlignment(0, i,
                        HorizontalPanel.ALIGN_CENTER);
            }
        }
    }

    public void onDetach() {
        super.onDetach();
        // int size = friendinfos.size();
        // for (int i = 0;i < size;i++) {
        // friendinfos.get(i);
        // }
        // for (int j = 0;j < size;j++) {
        // friendinfos.remove(0);
        // }
        // friends = null;
    }

    // public void onAttach() {
    // super.onAttach();
    // }

    public void selectall() {
        mData.selectAllRows();
        selectionChangedCheckIfListenersShouldBeNotified();
    }

    public void deselectall() {
        mData.deselectAllRows();
        selectionChangedCheckIfListenersShouldBeNotified();
    }

    public void forceConnect() {
        // for (int i = 0;i < friends.length;i++) {
        // if (selectors.get(i).getValue()) {

        OneSwarmRPCClient.getService().connectToFriends(OneSwarmRPCClient.getSessionID(),
                this.getSelectedFriends().toArray(new FriendInfoLite[0]),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        OneSwarmGWT.log("connect to friend: got error");
                    }

                    public void onSuccess(Void result) {
                        refresh();
                        OneSwarmGWT.log("connection attempt initiated");
                    }
                });
    }

    private List<FriendInfoLite> getSelectedFriends() {
        List<FriendInfoLite> out = new ArrayList<FriendInfoLite>();

        for (int i = 0; i < mData.getRowCount(); i++) {
            SortableNamePanel cb = (SortableNamePanel) mData.getWidget(i, 0);
            if (mData.isRowSelected(i)) {
                out.add(cb.getFriendInfo().getFriendInfoLite());
            }
        }

        // for( int i=0; i<friends.length; i++ ) {
        // if( selectors.get(i).getValue() ) {
        // out.add(friends[i]);
        // }
        // }
        return out;
    }

    public void blockClicked() {

        List<FriendInfoLite> selected = getSelectedFriends();
        Boolean shouldBlock = null;
        for (FriendInfoLite f : selected) {
            if (shouldBlock == null) {
                shouldBlock = !f.isBlocked();
            }
            System.out.println("setting " + f.getName() + " blocked? " + shouldBlock);
            f.setBlocked(shouldBlock);
        }

        OneSwarmRPCClient.getService().setFriendsSettings(OneSwarmRPCClient.getSessionID(),
                selected.toArray(new FriendInfoLite[0]), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("success");
                        refresh();
                    }
                });
    }

    public void removeClicked() {

        FriendInfoLite[] selectedArr = getSelectedFriends().toArray(new FriendInfoLite[0]);

        String prompt = "Are you sure you want to permanently delete ";
        if (selectedArr.length < 5) {
            for (int i = 0; i < selectedArr.length; i++) {
                prompt += selectedArr[i].getName() + " ";
            }
            prompt += "?";
        } else {
            prompt += selectedArr.length + " friends?";
        }

        if (Window.confirm(prompt)) {

            OneSwarmRPCClient.getService().deleteFriends(OneSwarmRPCClient.getSessionID(),
                    selectedArr, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            OneSwarmGWT.log("delete friend: got error");
                        }

                        public void onSuccess(Void result) {
                            OneSwarmGWT.log("friend deleted");
                            // noremoveoperation = false;
                            refresh();
                            deselectall();
                        }
                    });
        }

        // for (int i = 0;i < friends.length;i++) {
        // if (selectors.get(i).getValue()) {
        // if (friends[i].isBlocked()) {
        // OneSwarmRPCClient.getService().deleteFriend(OneSwarmRPCClient.getSessionID(),
        // friends[i], new AsyncCallback<Void>() {
        // public void onFailure(Throwable caught) {
        // OneSwarmGWT.log("delete friend: got error");
        // }
        //
        // public void onSuccess(Void result) {
        // OneSwarmGWT.log("friend deleted");
        // noremoveoperation = false;
        // refresh();
        // deselectall();
        // }
        // });
        // } else {
        // friends[i].setBlocked(true);
        // OneSwarmRPCClient.getService().setFriendSettings(OneSwarmRPCClient.getSessionID(),
        // friends[i], new AsyncCallback<Void>(){
        // public void onFailure(Throwable caught) {
        // caught.printStackTrace();
        // }
        //
        // public void onSuccess(Void result) {
        // refresh();
        // }
        // });
        // }
        // }
        // }
    }

    public void addFriends() {
        OneSwarmDialogBox dlg = new FriendsImportWizard(newFriendRequestCounts);
        dlg.show();
        dlg.setVisible(false);
        dlg.center();
        dlg.setPopupPosition(dlg.getPopupLeft(), Math.max(40, dlg.getPopupTop() - 200));
        dlg.setVisible(true);
    }

    // public void undelete() {
    // for (int i = 0;i < friends.length;i++) {
    // if (selectors.get(i).getValue()) {
    // friends[i].setBlocked(false);
    // OneSwarmRPCClient.getService().setFriendsSettings(OneSwarmRPCClient.getSessionID(),
    // friends[i], new AsyncCallback<Void>(){
    // public void onFailure(Throwable caught) {
    // caught.printStackTrace();
    // }
    //
    // public void onSuccess(Void result) {
    // refresh();
    // }
    // });
    // }
    // }
    // }

    public void swaplimited() {

        Boolean SetOrUnset = null;
        // List<FriendInfoLite> toUpdate = new ArrayList<FriendInfoLite>();
        // for (int i = 0; i<friends.length;i++) {
        // if (selectors.get(i).getValue()) {
        // if (SetOrUnset == null) {
        // SetOrUnset = !friends[i].isCanSeeFileList();
        // }
        // friends[i].setCanSeeFileList(SetOrUnset);
        // toUpdate.add(friends[i]);
        // }
        // }

        List<FriendInfoLite> selected = this.getSelectedFriends();
        for (FriendInfoLite f : selected) {
            if (SetOrUnset == null) {
                SetOrUnset = !f.isCanSeeFileList();
            }
            f.setCanSeeFileList(SetOrUnset);
            f.setRequestFileList(SetOrUnset);
        }

        OneSwarmRPCClient.getService().setFriendsSettings(OneSwarmRPCClient.getSessionID(),
                selected.toArray(new FriendInfoLite[0]), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        refresh();
                    }
                });

        // Boolean SetOrUnset = null;
        // for (int i = 0;i < friends.length;i++) {
        // if (selectors.get(i).getValue()) {
        // if (SetOrUnset == null) {
        // SetOrUnset = !friends[i].isCanSeeFileList();
        // }
        // friends[i].setCanSeeFileList(SetOrUnset);
        // OneSwarmRPCClient.getService().setFriendsSettings(OneSwarmRPCClient.getSessionID(),
        // selected, new AsyncCallback<Void>(){
        // public void onFailure(Throwable caught) {
        // caught.printStackTrace();
        // }
        //
        // public void onSuccess(Void result) {
        // refresh();
        // }
        // });
        // }
        // }
    }

    public void swapchat() {
        Boolean SetOrUnset = null;
        // List<FriendInfoLite> toUpdate = new ArrayList<FriendInfoLite>();

        List<FriendInfoLite> selected = this.getSelectedFriends();

        // for (int i = 0;i < friends.length;i++) {
        // if (selectors.get(i).getValue()) {
        // if (SetOrUnset == null) {
        // SetOrUnset = !friends[i].isAllowChat();
        // }
        // friends[i].setAllowChat(SetOrUnset);
        // toUpdate.add(friends[i]);
        // }
        // }

        for (FriendInfoLite f : selected) {
            if (SetOrUnset == null) {
                SetOrUnset = !f.isAllowChat();
            }
            f.setAllowChat(SetOrUnset);
        }

        OneSwarmRPCClient.getService().setFriendsSettings(OneSwarmRPCClient.getSessionID(),
                selected.toArray(new FriendInfoLite[0]), new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("successful refresh");
                        refresh();
                    }
                });
    }

    public void refresh() {
        OneSwarmRPCClient.getService().getNewFriendsCountsFromAutoCheck(
                OneSwarmRPCClient.getSessionID(), new AsyncCallback<HashMap<String, Integer>>() {
                    public void onSuccess(HashMap<String, Integer> result) {
                        newFriendRequestCounts = result;
                    }

                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }
                });

        OneSwarmRPCClient.getService().getFriends(OneSwarmRPCClient.getSessionID(), 0, true, true,
                new AsyncCallback<FriendList>() {
                    public void onSuccess(FriendList result) {

                        if (mRefreshCallback != null) {
                            mRefreshCallback.refreshed(result);
                        }

                        int oldSortCol = mData.getColumnSortList().getPrimaryColumn();

                        FriendInfoLite[] friendlist = result.getFriendList();

                        /**
                         * If there are no friends we won't be showing the table
                         * anyway
                         */
                        if (friendlist.length == 0) {
                            return;
                        }

                        System.out.println("Showing blocked friends?: " + mShowBlocked);
                        int rowoffset = 0;

                        int rows = 0;
                        for (int i = 0; i < friendlist.length; i++) {
                            if (mShowBlocked || friendlist[i].isBlocked() == false) {
                                rows++;
                            }
                        }

                        System.out.println("got: " + rows + " rows");

                        mData.resizeRows(rows);

                        for (int i = 0; i < friendlist.length; i++) {
                            // System.out.println("INSIDE FOR LOOP: " + i);
                            FriendInfo finfo = new FriendInfo(friendlist[i]);
                            // FriendInfo finfo =
                            // ((FrendInfoEncapsulatingCheckBox)mData.getWidget(i,0)).getFriendInfo();
                            Map<String, Widget> frienddata = finfo.GetFriendTableData();

                            if (mShowBlocked
                                    || ((Label) (frienddata.get("Deleted"))).getText().equals(
                                            "false")) {
                                SortableNamePanel sortableNamePanel = (SortableNamePanel) frienddata
                                        .get("Name");
                                // if (mData.getRowCount() >= rowoffset) {
                                // mData.resizeRows(rowoffset + 1);
                                // }
                                mData.setWidget(rowoffset, 0, sortableNamePanel);
                                mData.setWidget(rowoffset, 1, frienddata.get("Last Connected"));
                                mData.setWidget(rowoffset, 2, frienddata.get("Share Ratio"));
                                mData.setWidget(rowoffset, 3, frienddata.get("Downloaded"));
                                mData.setWidget(rowoffset, 4, frienddata.get("Uploaded"));
                                mData.setWidget(rowoffset, 5, frienddata.get("Limited"));
                                mData.setWidget(rowoffset, 6, frienddata.get("Chat Allowed"));
                                mData.setWidget(rowoffset, 7, frienddata.get("Date Added"));
                                // mData.setWidget(rowoffset, 9,
                                // frienddata.get("Last Connected IP"));
                                mData.setWidget(rowoffset, 8, frienddata.get("Source"));

                                rowoffset++;
                            }
                        }

                        // noremoveoperation = true;

                        if (oldSortCol != -1) {
                            mData.sortColumn(oldSortCol);
                        }
                        resizeTable();

                    }

                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }
                });
    }

    public void addSelectionCallback(SelectionCallback selectionCallback) {
        mSelectionCallbacks.add(selectionCallback);
    }

    public void selectNeverConnected() {
        for (int row = 0; row < mData.getRowCount(); row++) {
            SortableNamePanel cb = (SortableNamePanel) mData.getWidget(row, 0);
            FriendInfoLite flite = cb.getFriendInfo().getFriendInfoLite();

            if (flite.getLastConnectedDate() == null) {
                mData.selectRow(row, false);
            } else {
                mData.deselectRow(row);
            }
        }
        selectionChangedCheckIfListenersShouldBeNotified();
    }

    public void setShowBlocked(boolean value) {
        mShowBlocked = value;
    }
}
