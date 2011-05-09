package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.swarms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ResizePolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollPolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.SortPolicy;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.gen2.table.event.client.TableEvent.Row;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.GroupsListSorter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipList;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipListListener;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.SwarmPermissionsDialog;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class SwarmPermissionsTab extends SimplePanel {
    private static OSMessages msg = OneSwarmGWT.msg;

    private TorrentInfo[] mTorrents;
    private Widget swarmList;

    private VerticalPanel mCurrentRHS = null;

    final HorizontalPanel mainPanel = new HorizontalPanel();

    public SwarmPermissionsTab(TorrentInfo[] torrents, int inSelected) {
        super();

        mTorrents = torrents;

        mainPanel.setWidth("100%");
        mainPanel.setHeight("400px");

        swarmList = createSwarmList();

        mainPanel.add(swarmList);

        mainPanel.setCellWidth(swarmList, "200px");

        swarmList.addStyleName(TorrentInfoHeaderPanel.CSS_TORRENT_HEADER_SUB);

        changeSelectedSwarm(mTorrents[inSelected]);

        this.setWidget(mainPanel);
    }

    private Widget createSwarmList() {

        FixedWidthGrid table = new FixedWidthGrid(mTorrents.length, 1);
        FixedWidthFlexTable filesHeader = new FixedWidthFlexTable();
        filesHeader.setText(0, 0, msg.visibility_swarm());
        filesHeader.setColumnWidth(0, 200 - 23);
        ScrollTable fileTable = new ScrollTable(table, filesHeader);
        fileTable.setResizePolicy(ResizePolicy.FIXED_WIDTH);
        fileTable.setScrollPolicy(ScrollPolicy.BOTH);
        fileTable.setHeight("400px");

        table.setColumnWidth(0, 200 - 23);

        // fileTable.setSortingEnabled(false);
        fileTable.setSortPolicy(SortPolicy.DISABLED);

        for (int i = 0; i < mTorrents.length; i++) {
            Label l = new Label(mTorrents[i].getName());
            l.setWordWrap(true);
            table.setWidget(i, 0, l);
        }

        table.setSelectionPolicy(SelectionPolicy.ONE_ROW);

        table.addRowSelectionHandler(new RowSelectionHandler() {
            public void onRowSelection(RowSelectionEvent event) {
                Set<Row> selectedRows = event.getSelectedRows();
                if (selectedRows.size() > 0) {
                    int firstRow = selectedRows.toArray(new Row[0])[0].getRowIndex();
                    changeSelectedSwarm(mTorrents[firstRow]);
                }
            }
        });

        return fileTable;
    }

    private void changeSelectedSwarm(final TorrentInfo inSwarm) {
        TorrentInfoHeaderPanel header = new TorrentInfoHeaderPanel(inSwarm);
        header.setWidth("100%");
        mCurrentRHS = new VerticalPanel();
        mCurrentRHS.add(header);

        if (mainPanel.getWidgetCount() == 2) {
            mainPanel.remove(1);
            mainPanel.add(new Label(msg.loading()));
        }

        /**
         * painful chaining of all of these...
         */
        OneSwarmRPCClient.getService().getAllGroups(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<ArrayList<PermissionsGroup>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(ArrayList<PermissionsGroup> result) {
                        final List<PermissionsGroup> all_groups = result;

                        if (all_groups != null) {
                            System.out.println("got all groups: " + result.size());

                            OneSwarmRPCClient.getService().getGroupsForSwarm(
                                    OneSwarmRPCClient.getSessionID(), inSwarm,
                                    new AsyncCallback<ArrayList<PermissionsGroup>>() {
                                        public void onFailure(Throwable caught) {
                                            caught.printStackTrace();
                                        }

                                        public void onSuccess(ArrayList<PermissionsGroup> result) {
                                            changeSelectedSwarmUI(inSwarm, all_groups, result);
                                        }
                                    });
                        } else {
                            System.err.println("all groups is null!");
                            (new Exception()).printStackTrace();
                        }
                    }
                });
    }

    boolean all_friends_added = false;

    private void changeSelectedSwarmUI(final TorrentInfo inSwarm,
            final List<PermissionsGroup> all_groups, List<PermissionsGroup> swarm_groups) {
        // remove loading label
        if (mainPanel.getWidgetCount() == 2) {
            mainPanel.remove(1);
        }

        List<PermissionsGroup> all_sub_shared = new ArrayList<PermissionsGroup>();
        for (int i = 0; i < all_groups.size(); i++) {
            if (!swarm_groups.contains(all_groups.get(i))) {
                all_sub_shared.add(all_groups.get(i));
                System.out.println("available to add has: " + all_groups.get(i));
            }
        }

        final MembershipList<PermissionsGroup> available_groups = new MembershipList<PermissionsGroup>(
                msg.visibility_group_available_groups(), true, all_groups, swarm_groups, true,
                new GroupsListSorter());
        final MembershipList<PermissionsGroup> sharing_with_groups = new MembershipList<PermissionsGroup>(
                msg.visibility_sharing_with(), false, all_groups, all_sub_shared, true,
                new GroupsListSorter());

        final PermissionsGroup public_net = new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET);
        final PermissionsGroup all_friends = new PermissionsGroup(PermissionsGroup.ALL_FRIENDS);
        Set<PermissionsGroup> hs = new HashSet<PermissionsGroup>();
        hs.add(public_net);

        all_friends_added = false;
        for (PermissionsGroup g : swarm_groups) {
            if (g.isAllFriends()) {
                all_friends_added = true;
            }
        }

        HorizontalPanel hp = new HorizontalPanel() {
            public void onDetach() {
                System.out.println("attempting to synchronize permissions for: "
                        + inSwarm.getName());

                ArrayList<PermissionsGroup> groupsList = sharing_with_groups.getMembers();

                OneSwarmRPCClient.getService().setGroupsForSwarm(OneSwarmRPCClient.getSessionID(),
                        inSwarm, groupsList, new AsyncCallback<ReportableException>() {

                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(ReportableException result) {
                                if (result != null) {
                                    new ReportableErrorDialogBox(result, false);
                                } else {
                                    System.out.println("sync'd successfully");
                                }
                            }
                        });
            }
        };

        for (PermissionsGroup g : swarm_groups) {
            System.out.println("swarm groups has: " + g.toString());
        }

        available_groups.addListener(new MembershipListListener<PermissionsGroup>() {
            public void objectEvent(MembershipList<PermissionsGroup> list, PermissionsGroup inObject) {
                System.out.println("groups remove event: " + inObject);
                sharing_with_groups.restoreExcluded(inObject);

                /**
                 * Here we are removing some group from available groups and
                 * adding to sharing with. If we're adding something that's NOT
                 * the 'public' group and we're in 'share with all friends'
                 * mode, we need to remove all the more specific groups.
                 */
                if (all_friends_added && inObject.isPublicInternet() == false) {
                    System.out.println("adding specific group when all friends is present");
                    all_friends_added = false;
                    sharing_with_groups.addExcluded(all_friends);
                    available_groups.restoreExcluded(all_friends);
                }
                /**
                 * Here we are adding the 'all friends' object, so we need to
                 * remove all the previous, more specific groups.
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
            public void objectEvent(MembershipList<PermissionsGroup> list, PermissionsGroup inObject) {
                System.out.println("permitted remove event: " + inObject);
                available_groups.restoreExcluded(inObject);
            }
        });

        // permitted_vp.setWidth("100%");
        // groups.setWidth("");
        hp.setWidth("100%");

        hp.add(sharing_with_groups);
        available_groups.setWidth("100%");
        hp.add(available_groups);
        hp.setSpacing(3);
        hp.setCellWidth(available_groups, "49%");
        hp.setCellWidth(sharing_with_groups, "49%");

        hp.setWidth("100%");

        hp.setCellHorizontalAlignment(sharing_with_groups, HorizontalPanel.ALIGN_LEFT);
        hp.setCellHorizontalAlignment(available_groups, HorizontalPanel.ALIGN_RIGHT);

        mCurrentRHS.add(hp);
        mCurrentRHS.setWidth((SwarmPermissionsDialog.WIDTH - 200) + "px");

        // mCurrentRHS.setWidth("");

        mainPanel.add(mCurrentRHS);
    }

}
