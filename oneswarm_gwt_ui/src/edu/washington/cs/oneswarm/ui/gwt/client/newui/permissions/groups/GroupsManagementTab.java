package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.groups;

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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipList;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipListListener;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.SwarmPermissionsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.swarms.TorrentInfoHeaderPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;

public class GroupsManagementTab extends SimplePanel {
	private static OSMessages msg = OneSwarmGWT.msg;
	private List<PermissionsGroup> mGroups;

	private Set<FriendInfoLite> all_friends;

	// this includes friend names
	private Set<String> excluded_special_group_names = new HashSet<String>();

	private ScrollTable groupListTable;

	private VerticalPanel mCurrentRHS = null;

	final HorizontalPanel mainPanel = new HorizontalPanel();

	final Label NO_GROUPS_LABEL = new Label(msg.visibility_group_create());

	public GroupsManagementTab() {
		super();

		mainPanel.setWidth("100%");
		mainPanel.setHeight("400px");

		mainPanel.add(new Label(msg.loading()));

		OneSwarmRPCClient.getService().getFriends(OneSwarmRPCClient.getSessionID(), 0, true, false, new AsyncCallback<FriendList>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(FriendList result) {
				all_friends = new HashSet<FriendInfoLite>();
				for (FriendInfoLite f : result.getFriendList()) {
					if( f.isCanSeeFileList() ) {
						all_friends.add(f);
					}
				}

				OneSwarmRPCClient.getService().getAllGroups(OneSwarmRPCClient.getSessionID(), new AsyncCallback<ArrayList<PermissionsGroup>>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(ArrayList<PermissionsGroup> result) {

						mGroups = new ArrayList<PermissionsGroup>();
						/**
						 * We should only operate on created groups, i.e., not
						 * single-user wrappers or all friends or public 'net
						 * groups.
						 */
						for (PermissionsGroup g : result) {
							if (g.isSpecial() == false) {
								mGroups.add(g);
							}
							excluded_special_group_names.add(g.getName());
						}
						initUI();
					}
				});

			}
		});

		this.setWidget(mainPanel);
	}

	private void initUI() {
		mainPanel.clear();

		Widget groupList = createGroupList();

		mainPanel.add(groupList);

		mainPanel.setCellWidth(groupList, "200px");

		groupList.addStyleName(TorrentInfoHeaderPanel.CSS_TORRENT_HEADER_SUB);

		if (mGroups.size() > 0) {
			groupListTable.getDataTable().selectRow(0, true);
			// changeSelectedGroup(mGroups.get(0));
		} else {
			mainPanel.add(NO_GROUPS_LABEL);
			mainPanel.setCellVerticalAlignment(NO_GROUPS_LABEL, VerticalPanel.ALIGN_MIDDLE);
		}
	}

	String removing = null;

	private GroupInfoHeaderPanel mCurrentHeader;

	private Widget createGroupList() {

		VerticalPanel vp = new VerticalPanel();

		final FixedWidthGrid table = new FixedWidthGrid(0, 1);
		FixedWidthFlexTable filesHeader = new FixedWidthFlexTable();
		filesHeader.setText(0, 0, msg.visibility_group());
		filesHeader.setColumnWidth(0, 200 - 23);
		groupListTable = new ScrollTable(table, filesHeader);
		groupListTable.setResizePolicy(ResizePolicy.FIXED_WIDTH);
		groupListTable.setScrollPolicy(ScrollPolicy.BOTH);
		groupListTable.setHeight("380px");
		
		table.resizeRows(mGroups.size());

		vp.setHeight("400px");

		table.setColumnWidth(0, 200 - 23);

		groupListTable.setSortPolicy(SortPolicy.DISABLED);

		for (int i = 0; i < mGroups.size(); i++) {
			Label l = new Label(mGroups.get(i).getName());
			l.addStyleName(OneSwarmCss.CLICKABLE);
			l.setWordWrap(true);
			table.setWidget(i, 0, l);
		}

		table.setSelectionPolicy(SelectionPolicy.ONE_ROW);

		table.addRowSelectionHandler(new RowSelectionHandler() {

			public void onRowSelection(RowSelectionEvent event) {
				final Set<Row> selectedRows = event.getSelectedRows();
				int firstSelected = Integer.MAX_VALUE;
				for (Row row : selectedRows) {
					if (row.getRowIndex() < firstSelected) {
						firstSelected = row.getRowIndex();
					}
				}
				if( firstSelected < Integer.MAX_VALUE ) {
					changeSelectedGroup(mGroups.get(firstSelected));
				}
			}
		});

		HorizontalPanel buttons_hp = new HorizontalPanel();
		Button add_button = new Button(msg.button_add());
		add_button.addStyleName(OneSwarmCss.SMALL_BUTTON);
		buttons_hp.add(add_button);
		buttons_hp.setCellHorizontalAlignment(add_button, HorizontalPanel.ALIGN_LEFT);
		final Button remove_button = new Button(msg.button_remove());
		remove_button.addStyleName(OneSwarmCss.SMALL_BUTTON);
		buttons_hp.add(remove_button);
		buttons_hp.setCellHorizontalAlignment(remove_button, HorizontalPanel.ALIGN_LEFT);

		add_button.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				String name = null;
				while (true) {
					name = Window.prompt(msg.visibility_group_enter_name(), msg.visibility_group_default_name());
					if (name == null) {
						break;
					}
					if (excluded_special_group_names.contains(name)) {
						Window.alert(msg.visibility_group_name_used());
					} else {
						break;
					}
				}

				if (name != null) {
					PermissionsGroup newGroup = new PermissionsGroup(name, new String[] {}, false, 0);
					OneSwarmRPCClient.getService().updateGroupMembership(OneSwarmRPCClient.getSessionID(), newGroup, new ArrayList<FriendInfoLite>(), new AsyncCallback<PermissionsGroup>() {
						public void onFailure(Throwable caught) {
							if( caught instanceof OneSwarmException ) { 
								new ReportableErrorDialogBox((OneSwarmException)caught, false);
							}
							System.err.println(caught.toString());
						}

						public void onSuccess(PermissionsGroup result) {
							table.insertRow(0);
							mGroups.add(0, result);
							Label l = new Label(mGroups.get(0).getName());
							l.setWordWrap(true);
							table.setWidget(0, 0, l);
							table.selectRow(0, true);
						}
					});
				}
			}
		});

		remove_button.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {

				if (table.getSelectedRows().size() == 0)
					return;

				final int index = table.getSelectedRows().toArray(new Integer[0])[0];
				final PermissionsGroup selected = mGroups.get(index);

				if (Window.confirm(msg.visibility_group_remove(selected.getName()))) {
					remove_button.setEnabled(false);
					OneSwarmRPCClient.getService().removeGroup(OneSwarmRPCClient.getSessionID(), selected.getGroupID(), new AsyncCallback<ReportableException>() {
						public void onFailure(Throwable caught) {
							caught.printStackTrace();
						}

						public void onSuccess(ReportableException result) {
							if (result != null) {
								new ReportableErrorDialogBox(result, false);
							} else {
								mGroups.remove(index);
								table.removeRow(index);
								remove_button.setEnabled(true);

								removing = selected.getName();

								if (mGroups.size() > 0) {
									groupListTable.getDataTable().selectRow(0, true);
								} else {
									mainPanel.remove(1);
									mainPanel.add(NO_GROUPS_LABEL);
									mainPanel.setCellVerticalAlignment(NO_GROUPS_LABEL, VerticalPanel.ALIGN_MIDDLE);
								}

								// System.out.println("removed group success: "
								// + selected.getName());
							}
						}
					});
				}
			}
		});

		buttons_hp.setSpacing(2);

		// buttons_hp.setWidth("100%");
		vp.setWidth("100%");

		vp.add(groupListTable);
		vp.add(buttons_hp);

		vp.setCellHorizontalAlignment(buttons_hp, HorizontalPanel.ALIGN_LEFT);

		return vp;
	}

	private void changeSelectedGroup(final PermissionsGroup inGroup) {
		if (mCurrentHeader != null) {
			mCurrentHeader.removeFromParent();
		}

		mCurrentHeader = new GroupInfoHeaderPanel(inGroup);
		mCurrentHeader.setWidth("100%");
		mCurrentRHS = new VerticalPanel();
		mCurrentRHS.add(mCurrentHeader);

		if (mainPanel.getWidgetCount() == 2) {
			mainPanel.remove(1);
			mainPanel.add(new Label(msg.loading()));
		}

		/**
		 * get details for this group
		 */
		OneSwarmRPCClient.getService().getFriendsForGroup(OneSwarmRPCClient.getSessionID(), inGroup, new AsyncCallback<ArrayList<FriendInfoLite>>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(ArrayList<FriendInfoLite> result) {
				changeSelectedGroupUI(inGroup, result);
			}
		});
	}

	boolean all_friends_added = false;

	private void changeSelectedGroupUI(final PermissionsGroup inGroup, List<FriendInfoLite> group_friends) {
		// remove loading label
		if (mainPanel.getWidgetCount() == 2) {
			mainPanel.remove(1);
		}

		List<FriendInfoLite> all_sub_shared = new ArrayList<FriendInfoLite>();
		FriendInfoLite[] all_friends_arr = all_friends.toArray(new FriendInfoLite[0]);
		List<FriendInfoLite> all_friends_list = new ArrayList<FriendInfoLite>();
		for (FriendInfoLite f : all_friends) {
			all_friends_list.add(f);
		}

		for (int i = 0; i < all_friends.size(); i++) {
			if (!group_friends.contains(all_friends_arr[i])) {
				all_sub_shared.add(all_friends_arr[i]);
				// System.out.println("available to add has: " +
				// all_friends_arr[i]);
			}
		}

		final MembershipList<FriendInfoLite> available_friends = new MembershipList<FriendInfoLite>(msg.visibility_group_available_friends(), true, all_friends_list, group_friends, true);
		final MembershipList<FriendInfoLite> group_member_friends = new MembershipList<FriendInfoLite>(msg.visibility_group_members(), false, all_friends_list, all_sub_shared, true);

		HorizontalPanel hp = new HorizontalPanel() {
			public void onDetach() {
				// System.out.println("attempting to synchronize group membership for: "
				// + inGroup.getName());

				/*
				 * This will happen once right after we remove something when we
				 * remove its panel
				 */
				if (removing != null) {
					if (inGroup.getName().equals(removing)) {
						removing = null;
						return;
					}
				}

				ArrayList<FriendInfoLite> groupsList = group_member_friends.getMembers();

				OneSwarmRPCClient.getService().updateGroupMembership(OneSwarmRPCClient.getSessionID(), inGroup, groupsList, new AsyncCallback<PermissionsGroup>() {

					public void onFailure(Throwable caught) {
						if( caught instanceof OneSwarmException ) { 
							new ReportableErrorDialogBox((OneSwarmException)caught, false);
						}
						System.err.println(caught.toString());
					}

					public void onSuccess(PermissionsGroup result) {
							// System.out.println("group membership sync'd successfully: "
							// + inGroup.getName());
					}
				});
				// System.out.println("(would have called sync if live)");
			}
		};

		// for (FriendInfoLite f : group_friends) {
		// System.out.println("group friends has: " + f.getName());
		// }

		available_friends.addListener(new MembershipListListener<FriendInfoLite>() {
			public void objectEvent(MembershipList<FriendInfoLite> list, FriendInfoLite inObject) {
				// System.out.println("groups remove event: " + inObject);
				group_member_friends.restoreExcluded(inObject);
				if (mCurrentHeader != null) {
					mCurrentHeader.updateFriendCount(group_member_friends.getMembers().size());
				}
			}
		});

		group_member_friends.addListener(new MembershipListListener<FriendInfoLite>() {
			public void objectEvent(MembershipList<FriendInfoLite> list, FriendInfoLite inObject) {
				// System.out.println("permitted remove event: " + inObject);
				available_friends.restoreExcluded(inObject);
				if (mCurrentHeader != null) {
					mCurrentHeader.updateFriendCount(group_member_friends.getMembers().size());
				}
			}
		});

		// permitted_vp.setWidth("100%");
		// groups.setWidth("");
		hp.setWidth("100%");

		hp.add(group_member_friends);
		available_friends.setWidth("100%");
		hp.add(available_friends);
		hp.setSpacing(3);
		hp.setCellWidth(available_friends, "49%");
		hp.setCellWidth(group_member_friends, "49%");

		hp.setWidth("100%");

		hp.setCellHorizontalAlignment(group_member_friends, HorizontalPanel.ALIGN_LEFT);
		hp.setCellHorizontalAlignment(available_friends, HorizontalPanel.ALIGN_RIGHT);

		mCurrentRHS.add(hp);
		mCurrentRHS.setWidth((SwarmPermissionsDialog.WIDTH - 200) + "px");

		// mCurrentRHS.setWidth("");

		mainPanel.add(mCurrentRHS);
	}

}
