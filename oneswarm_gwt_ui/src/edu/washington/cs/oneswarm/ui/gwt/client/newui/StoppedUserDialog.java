package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;

public class StoppedUserDialog extends OneSwarmDialogBox {

    EntireUIRoot mRoot = null;

    Button okButton = new Button("Ok");
    Button changeLimitButton = new Button("Reset Limit");
    private String limittype;

    public StoppedUserDialog(EntireUIRoot inRoot, String limitType, String limitNumber,
            String currentNumber) {
        super();
        limittype = limitType;
        mRoot = inRoot;

        setText("Transfer Limit Exceeded");

        this.setWidth("310px");
        this.setHeight("200" + "px");
        DockPanel panel = new DockPanel();
        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        panel.setSize("310px", "200px");
        Label promptLabel = new Label(
                "Warning! You have exceeded your Transfer Limit and your torrents have been automatically stopped."
                        + "\n");
        promptLabel.setHeight("30px");
        Label explanationLabel = new Label(
                "Your "
                        + limitType
                        + " limit is set to "
                        + limitNumber
                        + "\n"
                        + "Your current "
                        + limitType
                        + " transfer is "
                        + currentNumber
                        + "\n"
                        + "\n"
                        + "If you want to restart your torrents, press the 'Reset Limit' button below or change your limit setting manually in the Settings menu");

        panel.add(promptLabel, DockPanel.NORTH);
        panel.add(explanationLabel, DockPanel.CENTER);
        HorizontalPanel bottom = new HorizontalPanel();
        bottom.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        okButton.setSize("130px", "28px");
        changeLimitButton.setSize("130px", "28px");
        bottom.add(changeLimitButton);
        bottom.add(okButton);

        okButton.addClickHandler(this);
        changeLimitButton.addClickHandler(this);

        bottom.setWidth("100%");
        bottom.setSpacing(3);

        panel.add(bottom, DockPanel.SOUTH);

        setWidget(panel);
    }

    public void onClick(ClickEvent event) {

        if (event.getSource().equals(okButton)) {
            hide();
        } else if (event.getSource().equals(changeLimitButton)) {
            OneSwarmRPCClient.getService().resetLimit(OneSwarmRPCClient.getSessionID(), limittype,
                    new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(String result) {
                            System.out.println("Reset button pressed");
                        }
                    });
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
}
