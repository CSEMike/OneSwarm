package edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details;

import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class TransferDetailsPanel extends SimplePanel {
	private static OSMessages msg = OneSwarmGWT.msg;

	public static final String CSS_XFER_DETAILS_TABS = "os-xfer_details_tabs";

	DecoratedTabPanel tabs = null;

	TransferDetailsTable publicDetails;
	TransferDetailsTable f2fDetails;

	public TransferDetailsPanel() {
		publicDetails = new TransferDetailsTable(TransferDetailsTable.Type.PUBLIC);

		tabs = new DecoratedTabPanel();
		tabs.setWidth("99%");
		tabs.addStyleName(CSS_XFER_DETAILS_TABS);

		tabs.add(publicDetails, msg.transfers_tab_public());

		f2fDetails = new TransferDetailsTable(TransferDetailsTable.Type.F2F);
		tabs.add(f2fDetails, msg.transfers_tab_f2f());

		// tabs.add(friendsTransfers, "Friends");

		F2FForwardsDetails friendsForwarding = new F2FForwardsDetails();
		tabs.add(friendsForwarding, msg.transfers_tab_forwarding());

		tabs.selectTab(1);

//		tabs.addTabListener(new TabListener() {
//			public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
//				return true;
//			}
//
//			public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
//				if (tabIndex == 0) {
//					TransferDetailsTable.getBrowserParent(this_shadow).tableSelectedSwarmChanged(publicDetails.getSelectedSwarm());
//				} else if (tabIndex == 1) {
//					TransferDetailsTable.getBrowserParent(this_shadow).tableSelectedSwarmChanged(f2fDetails.getSelectedSwarm());
//				} else if (tabIndex == 2) {
//					TransferDetailsTable.getBrowserParent(this_shadow).tableSelectedSwarmChanged(null);
//				} else {
//
//					System.err.println("need to update tab listener if adding more tabs to transfer details");
//				}
//			}
//		});

		setWidget(tabs);
	}

	public TransferDetailsTable getPublicDetailsTable() {
		return publicDetails;
	}
}
