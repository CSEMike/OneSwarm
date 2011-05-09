/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

// TODO: move to dead code

public class AddTorrentURLDialog extends OneSwarmDialogBox implements Updateable {

    /**
	 * 
	 */

    private final static String OK_TEXT = "Add URL";
    private final static String CANCEL_TEXT = "Cancel";
    private final static String REFERRING_TEXT = "Referrer: ";
    private final static String REFERRER_TEXT = "Specify referrer";

    private static final int WIDTH = 400;
    private static final int HEIGHT = 100;

    private final Button okButton = new Button(OK_TEXT);
    private final Button cancelButton = new Button(CANCEL_TEXT);
    private final TextBox urlBox = new TextBox();
    private final TextBox referrerBox = new TextBox();
    private final Label referrerLabel = new Label(REFERRING_TEXT);

    CheckBox specifyReferrer = new CheckBox(REFERRER_TEXT);

    private final Label statusLabel = new Label();

    // private int torrentDownloadId = -1;

    private HorizontalPanel referRow;

    // private EntireUIRoot mUIRoot;

    public AddTorrentURLDialog(EntireUIRoot inRoot) {

        // mUIRoot = inRoot;

        setText("Add Swarm from URL");

        VerticalPanel panel = new VerticalPanel();
        HorizontalPanel urlRow = new HorizontalPanel();
        Label urlLabel = new Label("URL:");
        urlRow.add(urlLabel);
        urlRow.add(urlBox);
        urlBox.setWidth("300px");
        urlRow.setCellHorizontalAlignment(urlBox, HorizontalPanel.ALIGN_RIGHT);
        urlRow.setWidth("100%");
        urlRow.setCellVerticalAlignment(urlLabel, VerticalPanel.ALIGN_MIDDLE);
        urlRow.setCellVerticalAlignment(urlBox, VerticalPanel.ALIGN_MIDDLE);

        referRow = new HorizontalPanel();
        referRow.add(referrerLabel);
        referrerBox.setWidth("300px");
        referRow.add(referrerBox);
        referRow.setCellHorizontalAlignment(referrerBox, HorizontalPanel.ALIGN_RIGHT);
        referRow.setWidth("100%");
        referRow.setCellVerticalAlignment(referrerLabel, VerticalPanel.ALIGN_MIDDLE);
        referRow.setCellVerticalAlignment(referrerBox, VerticalPanel.ALIGN_MIDDLE);

        referRow.setVisible(false);

        urlRow.setSpacing(3);
        referRow.setSpacing(3);

        panel.add(urlRow);
        panel.add(referRow);

        HorizontalPanel buttonPanel = new HorizontalPanel();
        // buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        buttonPanel.setWidth("100%");

        buttonPanel.add(specifyReferrer);
        buttonPanel.setCellHorizontalAlignment(specifyReferrer, HorizontalPanel.ALIGN_LEFT);

        HorizontalPanel rhs = new HorizontalPanel();
        rhs.setSpacing(3);
        okButton.addClickHandler(this);
        cancelButton.addClickHandler(this);
        specifyReferrer.addClickHandler(this);
        rhs.add(cancelButton);
        rhs.add(okButton);
        buttonPanel.add(rhs);
        buttonPanel.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);

        buttonPanel.setCellVerticalAlignment(rhs, VerticalPanel.ALIGN_MIDDLE);
        buttonPanel.setCellVerticalAlignment(specifyReferrer, VerticalPanel.ALIGN_MIDDLE);

        panel.add(buttonPanel);

        panel.setWidth(WIDTH + "px");
        panel.setHeight(HEIGHT + "px");
        setWidget(panel);
    }

    public void onClick(ClickEvent event) {

        if (event.getSource().equals(okButton)) {

            if (urlBox.getText().length() > 0) {
                OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
                okButton.setVisible(false);
                String session = OneSwarmRPCClient.getSessionID();
                service.downloadTorrent(session, urlBox.getText(), new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        error(caught.getMessage());
                    }

                    public void onSuccess(Integer result) {
                        if (result.intValue() == -1) {
                            error("download failed");

                        } else {
                            // torrentDownloadId = ret.intValue();
                            OneSwarmGWT.addToUpdateTask(AddTorrentURLDialog.this);
                            statusLabel.setText("downloading: ");
                            statusLabel.setVisible(true);
                        }
                    }

                    private void error(String message) {
                        OneSwarmGWT.log("got error while downloading torrent file: " + message);
                        Window.alert("got error while downloading torrent file");
                        statusLabel.setText("error");
                        statusLabel.setVisible(true);
                        okButton.setVisible(true);
                    }

                });

            } else {
                Window.alert("invalid url");
            }
        } else if (event.getSource().equals(cancelButton)) {
            hide();
        } else if (event.getSource().equals(specifyReferrer)) {
            referRow.setVisible(!referRow.isVisible());
        } else {
            super.onClick(event);
        }

    }

    public void update(int count) {
        // if (torrentDownloadId != -1) {
        // final String session = OneSwarmRPCClient.getSessionID();
        // OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
        // service.getTorrentDownloadProgress(session, torrentDownloadId, new
        // AsyncCallback() {
        //
        // public void onFailure(Throwable caught) {
        // }
        //
        // public void onSuccess(Object result) {
        // Integer progress = (Integer) result;
        // OneSwarmGWT.log("downloaded: " + progress);
        // statusLabel.setText("downloading: " + progress + "%");
        // if (progress.intValue() == 100) {
        // OneSwarmGWT.log("download finished");
        // OneSwarmUIServiceAsync addTorrentService =
        // OneSwarmRPCClient.getService();
        //
        // List<PermissionsGroup> groups = new ArrayList<PermissionsGroup>();
        // groups.add(new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET));
        // groups.add(new PermissionsGroup(PermissionsGroup.ALL_FRIENDS));
        //
        // addTorrentService.addTorrent(session, torrentDownloadId, null,
        // groups, new AsyncCallback<Boolean>() {
        //
        // public void onFailure(Throwable caught) {
        // OneSwarmGWT.log("Add torrent request failed");
        // }
        //
        // public void onSuccess(Boolean result) {
        // Boolean res = (Boolean) result;
        // if (res.booleanValue()) {
        // OneSwarmGWT.log("Torrent added successfully, refreshing swarms");
        // mUIRoot.refreshSwarms();
        // } else {
        // OneSwarmGWT.log("Add torrent failed");
        // }
        // }
        // });
        // OneSwarmGWT.removeFromUpdateTask(AddTorrentURLDialog.this);
        // hide();
        // }
        // }
        // });
        // }
    }
}