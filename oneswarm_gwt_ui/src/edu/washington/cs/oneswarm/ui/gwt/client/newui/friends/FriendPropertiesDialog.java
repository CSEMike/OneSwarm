package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendPropertiesDialog extends OneSwarmDialogBox {
	private final FriendPropertiesPanel mainPanel;
	private final FriendListPanel parent;
	private String initialGroup;

	public FriendPropertiesDialog(final FriendInfoLite friend, boolean useDebug) {
		this(null, friend, useDebug);
	}

	public FriendPropertiesDialog(FriendListPanel parent, final FriendInfoLite friend, boolean useDebug) {
		super();
		this.parent = parent;
		
		this.initialGroup = friend.getGroup();
		
		setText("Edit Friend: " + friend.getName());
		mainPanel = new FriendPropertiesPanel(friend, this, useDebug);
		setWidget(mainPanel);
	}

	public void saveFriend() {
		if (mainPanel != null) {
			mainPanel.saveChanges(null, true, false);
		}
	}

	public void hide() {
		mainPanel.stopUpdates();
		super.hide();
		if (parent != null) {
			boolean force = !initialGroup.equals(mainPanel.getGroup());
			parent.updateUI(force);
		}
	}

}
