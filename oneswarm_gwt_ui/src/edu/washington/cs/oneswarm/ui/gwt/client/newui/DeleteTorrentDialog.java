package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class DeleteTorrentDialog extends OneSwarmDialogBox {

	// private FlexTable panel;
	//
	// private Button deleteDataButton = new Button("Delete Data");
	//
	// private Button deleteDataAndFromListButton = new Button(
	// "Delete From Library");
	//
	// private Button deleteCompletelyButton = new Button("Delete Completely");

	private TorrentInfo[] torrentInfo;

	EntireUIRoot mRoot = null;

	Button okButton = new Button("Delete");
	Button cancelButton = new Button("Cancel");

	RadioButton swarmOnlyRadioButton = new RadioButton("deleteOptions", Strings.get(Strings.DELETE_DIALOG_SWARM_ONLY));
	RadioButton everythingRadioButton = new RadioButton("deleteOptions", Strings.get(Strings.DELETE_DIALOG_ALL));

	public DeleteTorrentDialog(TorrentInfo[] torrentInfo, EntireUIRoot inRoot) {
		super(false, true, true);

		mRoot = inRoot;

		this.torrentInfo = torrentInfo;
		setText("Remove: " + (torrentInfo.length > 1 ? torrentInfo.length + " swarms" : StringTools.truncate(torrentInfo[0].getName(), 25, true)));

		this.setWidth("250px");
		this.setHeight("100px");

		DockPanel panel = new DockPanel();

		Label promptLabel = new Label(Strings.get(Strings.DELETE_DIALOG_SELECT_PROMPT));
		promptLabel.setHeight("20px");

		panel.add(promptLabel, DockPanel.NORTH);

		VerticalPanel radioButtons = new VerticalPanel();

		radioButtons.add(swarmOnlyRadioButton);
		radioButtons.add(everythingRadioButton);

		swarmOnlyRadioButton.setValue(true);

		HorizontalPanel bottom = new HorizontalPanel();

		bottom.add(cancelButton);
		bottom.add(okButton);

		okButton.addClickHandler(this);
		cancelButton.addClickHandler(this);

		bottom.setWidth("100%");
		bottom.setSpacing(3);

		panel.add(radioButtons, DockPanel.CENTER);
		panel.add(bottom, DockPanel.SOUTH);

		// panel = new FlexTable();
		//
		// deleteDataButton.addClickListener(this);
		// deleteDataAndFromListButton.addClickListener(this);
		// deleteCompletelyButton.addClickListener(this);
		//
		// panel.setWidget(0, 0, new Label(
		// "Only delete data (to free up hard disk space)", true));
		// panel.setWidget(0, 1, deleteDataButton);
		//
		// panel
		// .setWidget(
		// 1,
		// 0,
		// new Label(
		// "Delete data and from the library (will still show up when searching)",
		// true));
		// panel.setWidget(1, 1, deleteDataAndFromListButton);
		//
		// panel.setWidget(2, 0, new Label("Delete completely", true));
		// panel.setWidget(2, 1, deleteCompletelyButton);

		// panel.setWidget(3, 0, new Label(""));
		// panel.setWidget(4, 1, cancelButton);
		//		

		setWidget(panel);
	}

	public void onClick(ClickEvent event) {

		if (event.getSource().equals(okButton)) {
			if (swarmOnlyRadioButton.getValue()) {
				stopSharingKeepData();
			} else {
				assert everythingRadioButton.getValue() == true : "radio button sanity violation";

				deleteCompletely();
			}

			hide();
		} else if (event.getSource().equals(cancelButton)) {
			hide();
		} else {
			super.onClick(event);
		}
	}

	private void refreshRoot() {
		System.out.println("refreshRoot() -> refreshSwarms()");
		mRoot.refreshSwarms();
	}

	AsyncCallback<ReportableException> callback = new AsyncCallback<ReportableException>() {
		public void onFailure(Throwable caught) {
			caught.printStackTrace();
		}

		public void onSuccess(ReportableException result) {
			hide();
			if (result != null)
				new ReportableErrorDialogBox(result, false);
			else
				refreshRoot();
		}
	};

	private void deleteCompletely() {
		System.out.println("Delete completely");

		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		String[] torrentIDs = new String[torrentInfo.length];
		for (int i = 0; i < torrentInfo.length; i++) {
			torrentIDs[i] = torrentInfo[i].getTorrentID();
		}

		service.deleteCompletely(OneSwarmRPCClient.getSessionID(), torrentIDs, callback);
	}

	private void stopSharingKeepData() {

		System.out.println("stopSharingKeepData");

		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

		String[] torrentIDs = new String[torrentInfo.length];
		for (int i = 0; i < torrentInfo.length; i++) {
			torrentIDs[i] = torrentInfo[i].getTorrentID();
		}

		service.deleteFromShareKeepData(OneSwarmRPCClient.getSessionID(), torrentIDs, callback);
	}
}
