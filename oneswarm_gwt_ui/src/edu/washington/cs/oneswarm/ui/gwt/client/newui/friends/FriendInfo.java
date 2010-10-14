package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCommunityServer;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils.SortableDateColumn;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils.SortableDouble;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils.SortableSizeColumn;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class FriendInfo {

	private FriendInfoLite friend = null;
	private Map<String, Widget> datamap = new HashMap<String, Widget>();

	public FriendInfo(FriendInfoLite friend) {
		this.friend = friend;
		if (this.friend != null) {
			refresh();
		}
	}

	public FriendInfoLite getFriendInfoLite() {
		return friend;
	}

	// private String appleLikeDate(Date date) {
	// if (date == null) {
	// return "Never";
	// }
	// int secAgo = (int) (((new Date()).getTime() - date.getTime()) / 1000);
	// int minAgo = secAgo / 60;
	// int hoursAgo = minAgo / 60;
	// int daysAgo = hoursAgo / 24;
	// int monthsAgo = daysAgo / 31;
	// if (secAgo < 60)
	// return secAgo + " seconds ago";
	// if (minAgo == 1)
	// return "1 minute ago";
	// if (minAgo < 60)
	// return minAgo + " minutes ago";
	// if (hoursAgo == 1)
	// return "1 hour ago";
	// if (hoursAgo < 24)
	// return hoursAgo + " hours ago";
	// if (daysAgo == 1)
	// return "yesterday";
	// if (daysAgo < 62)
	// return daysAgo + " days ago";
	// if (monthsAgo < 24)
	// return (monthsAgo) + " months ago";
	// else
	// return "a long time ago in a galaxy far far away";
	// }

	public void refresh() {
		// System.out.println("Updated Friend Stats" + " " + friend.getName());
		long downloadedTotal = friend.getDownloadedTotal();
		long uploadedTotal = friend.getUploadedTotal();
		double upDownRatio = 1;
		if (uploadedTotal > 0 && downloadedTotal > 0) {
			upDownRatio = (int) (((100.0 * downloadedTotal) / uploadedTotal)) / 100.0;
		}
		String name = friend.getName();
		String status = "";
		switch (friend.getStatus()) {
		case FriendInfoLite.STATUS_CONNECTING:
			status = "Connecting...";
			break;
		case FriendInfoLite.STATUS_HANDSHAKING:
			status = "Handshaking";
			break;
		case FriendInfoLite.STATUS_ONLINE:
			status = "Connected";
			break;
		case FriendInfoLite.STATUS_OFFLINE:
			if (friend.getLastConnectedDate() == null) {
				status = "Unknown (never connected)";
			} else {
				status = "Disconnected";
			}
			break;
		default:
			break;
		}
		Date lastConnectedDate = friend.getLastConnectedDate();
		if (friend.isConnected()) {
			lastConnectedDate = new Date();
		}
		String lastConnect = "";
		if (lastConnectedDate != null) {
			lastConnect = StringTools.formatDateAppleLike(lastConnectedDate, false);
		} else {
			lastConnect = "never";
		}

		String lastConnectedIp = "";
		if (lastConnect.equals("never")) {
			lastConnectedIp = "Never Connected";
		} else {
			lastConnectedIp = friend.getLastConnectIp() + ":" + friend.getLastConnectPort();
		}
		Date dateAdded = friend.getDateAdded();
		// Date date = "";
		// if (dateAdded == null) {
		// //date = "Unknown";
		// } else {
		// date = dateAdded.toGMTString();
		// }
		String chatallowstring = "";
		String limitedstring = "";
		if (friend.isAllowChat()) {
			chatallowstring = "Yes";
		} else {
			chatallowstring = "No";
		}
		if (!friend.isCanSeeFileList()) {
			limitedstring = "Yes";
		} else {
			limitedstring = "No";
		}
		// Calendar dateAddedCalendar = Calendar.getInstance()
		// dateAddedCalendar.setTime(dateAdded);
		// String dateAddedString = dateAddedCalendar.get(Calendar.MONTH) + "/"
		// + dateAddedCalendar.get(Calendar.DAY_OF_MONTH) + "/" +
		// dateAddedCalendar.get(Calendar.YEAR)
		Label nameLabel = new Label(name);
		nameLabel.setStylePrimaryName("gwt-nameStyle");
		ClickHandler handler = new ClickHandler() {
			public void onClick(ClickEvent event) {
				FriendPropertiesDialog dlg = new FriendPropertiesDialog(friend, false);
				dlg.show();
				dlg.setVisible(false);
				dlg.center();
				dlg.setVisible(true);
			}
		};
		nameLabel.addClickHandler(handler);
		// HorizontalPanel IconsAndName = new HorizontalPanel();

		SortableNamePanel namePanel = new SortableNamePanel(friend, nameLabel);
		Label l;
		datamap.put("Deleted", new Label(friend.isBlocked() + ""));
		datamap.put("Status", new Label(status));
		datamap.put("Last Connected", lastConnectedDate == null ? new Label(lastConnect) : new SortableDateColumn(lastConnectedDate, SortableDateColumn.FormatStyle.APPLE));
		datamap.put("Last Connected IP", new Label(lastConnectedIp));
		datamap.put("Share Ratio", new SortableDouble(upDownRatio));
		datamap.put("Uploaded", new SortableSizeColumn(uploadedTotal));
		datamap.put("Downloaded", new SortableSizeColumn(downloadedTotal));
		datamap.put("Limited", new Label(limitedstring));
		datamap.put("Name", namePanel);
		datamap.put("Date Added", new SortableDateColumn(dateAdded, SortableDateColumn.FormatStyle.NOTIME));
		datamap.put("Chat Allowed", new Label(chatallowstring));
		String sourceStr = friend.getSource().startsWith(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME) ? friend.getSource().substring(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME.length() + 1) : friend.getSource();
		l = new Label(sourceStr);
		l.setTitle(sourceStr); // tooltip
		datamap.put("Source", l);
	}

	class SortableNamePanel extends HorizontalPanel implements Comparable<SortableNamePanel> {
		String name;

		public SortableNamePanel(FriendInfoLite f, Label s) {
			super();
			if (f.isAllowChat()) {
				Image statusImage = new Image("images/icons/chat.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			} else {
				Image statusImage = new Image("images/spacer.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			}
			if (!f.isCanSeeFileList()) {
				Image statusImage = new Image("images/friend_limited.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			} else {
				Image statusImage = new Image("images/spacer.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			}
			if (f.isBlocked()) {
				Image statusImage = new Image("images/friend_blocked.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			} else {
				Image statusImage = new Image("images/spacer.png");
				statusImage.setPixelSize(12, 12);
				this.add(statusImage);
			}
			this.add(s);
			name = s.getText();
		}

		public int compareTo(SortableNamePanel s) {
			return this.name.toLowerCase().compareTo(s.name.toLowerCase());
		}

		public FriendInfo getFriendInfo() {
			return FriendInfo.this;
		}
	}

	public Map<String, Widget> GetFriendTableData() {
		return datamap;
	}

}
