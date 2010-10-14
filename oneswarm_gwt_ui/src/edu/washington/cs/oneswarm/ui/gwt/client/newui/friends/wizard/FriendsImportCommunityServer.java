package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;

public class FriendsImportCommunityServer extends VerticalPanel implements Updateable {
	public static final String FRIEND_NETWORK_COMMUNITY_NAME = "Community_server";
	private static OSMessages msg = OneSwarmGWT.msg;

	private final FriendsImportCallback callback;

	private Label statusLabel;

	private CommunityServerAddPanel addPanel;

	private Integer mTaskID = null;

	public static final int WIDTH = 400;

	public FriendsImportCommunityServer(final FriendsImportCallback callback, boolean notification, CommunityRecord toAdd) {
		this.callback = callback;
		if (callback == null) {
			new RuntimeException("tried to create friend import without callback!").printStackTrace();
		}

		final FriendsImportCommunityServer this_s = this;
		if (notification) {
			this.add(new Label(msg.loading()));
			OneSwarmRPCClient.getService().getPendingCommunityFriendImports(OneSwarmRPCClient.getSessionID(), new AsyncCallback<FriendList>() {
				public void onFailure(Throwable caught) {
					this_s.clear();
					this_s.add(new Label(msg.add_friends_community_error(caught.toString())));
				}

				public void onSuccess(FriendList result) {
					callback.connectSuccesful(result.getFriendList(), true);
				}
			});
			return;
		}

		// OneSwarmGWT.addToUpdateTask(this);

		setWidth(WIDTH + "px");

		addPanel = toAdd == null ? new CommunityServerAddPanel() : new CommunityServerAddPanel(toAdd);
		add(addPanel);
		HorizontalPanel rhs = new HorizontalPanel();
		final Button saveButton = new Button(msg.button_subscribe());
		Button cancelButton = new Button(msg.button_cancel());
		rhs.add(cancelButton);
		rhs.add(saveButton);
		rhs.setSpacing(3);
		// rhs.setWidth("100%");
		com.google.gwt.user.client.ui.Widget hrule = new SimplePanel();
		hrule.addStyleName(SettingsDialog.CSS_HRULE);

		HorizontalPanel bottom = new HorizontalPanel();
		bottom.setWidth("100%");
		statusLabel = new Label("");
		bottom.add(statusLabel);
		bottom.setCellVerticalAlignment(statusLabel, VerticalPanel.ALIGN_MIDDLE);
		statusLabel.setVisible(false);
		bottom.add(rhs);
		this.add(hrule);
		this.add(bottom);
		bottom.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);

		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				callback.cancel();
			}
		});

		saveButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {

				saveButton.setEnabled(false);
				addPanel.disableStuff();

				statusLabel.setText(msg.add_friends_community_contacting_server());
				statusLabel.setVisible(true);
				CommunityRecord cr = new CommunityRecord(addPanel);
				System.out.println("saving: " + cr);
				OneSwarmRPCClient.getService().pollCommunityServer(OneSwarmRPCClient.getSessionID(), cr, new AsyncCallback<Integer>() {

					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(Integer result) {
						OneSwarmGWT.addToUpdateTask(FriendsImportCommunityServer.this);
						mTaskID = result;
					}
				});

			}
		});
	}

	public void onDetach() {
		super.onDetach();
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	public void update(int n) {
		if (mTaskID != null && (n % 2) == 0) {
			OneSwarmRPCClient.getService().getBackendTask(OneSwarmRPCClient.getSessionID(), mTaskID.intValue(), new AsyncCallback<BackendTask>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(BackendTask result) {

					if (result == null) {
						statusLabel.setText(msg.add_friends_community_error_task_disappeared());
						OneSwarmGWT.removeFromUpdateTask(FriendsImportCommunityServer.this);
						return;
					}

					System.out.println("refreshing backend task: " + mTaskID);
					statusLabel.setText(result.getSummary());

					if (result.getProgress().equals("100") && result.isGood()) {
						FriendInfoLite[] flist = ((FriendList) result.getResult()).getFriendList();
						System.out.println("got " + flist.length + " from server");
						for (FriendInfoLite f : flist) {
							// f.setGroup(addPanel.getGroup());
							System.out.println(f.getName() + " / " + f.getGroup());
						}
						callback.connectSuccesful(flist, true);
						OneSwarmGWT.removeFromUpdateTask(FriendsImportCommunityServer.this);

						// this is now done on the server after successful
						// refresh -- we need the server-side info about
						// capabilities to do this.
						// /**
						// * Append this to the list of servers that we should
						// * refresh
						// */
						// OneSwarmRPCClient.getService().getStringListParameterValue(OneSwarmRPCClient.getSessionID(),
						// "oneswarm.community.servers", new
						// AsyncCallback<List<String>>() {
						// public void onFailure(Throwable caught) {
						// caught.printStackTrace();
						// }
						//
						// public void onSuccess(List<String> result) {
						// for (int i = 0; i < result.size() / 5; i++) {
						// CommunityRecord rec = new CommunityRecord(result, i *
						// 5);
						// if (addPanel.getURL().equals(rec.url)) {
						// System.out.println("skipping addition of duplicate community server");
						// return; // skip duplicate
						// }
						// }
						// for (String t : (new
						// CommunityRecord(addPanel)).toTokens()) {
						// result.add(t);
						// }
						//
						// OneSwarmRPCClient.getService().setStringListParameterValue(OneSwarmRPCClient.getSessionID(),
						// "oneswarm.community.servers", result, new
						// AsyncCallback<Void>() {
						// public void onFailure(Throwable caught) {
						// caught.printStackTrace();
						// }
						//
						// public void onSuccess(Void result) {
						// System.out.println("saved community servers successfully");
						// }
						// });
						// }
						// });
					}
				}
			});
		}
	}
}
