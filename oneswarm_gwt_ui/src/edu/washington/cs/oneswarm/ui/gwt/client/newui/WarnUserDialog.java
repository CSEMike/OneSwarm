package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;

public class WarnUserDialog extends OneSwarmDialogBox {

    EntireUIRoot mRoot = null;

    Button okButton = new Button("Ok");
    Button changeLimitButton = new Button("Change Limit");

    public WarnUserDialog(EntireUIRoot inRoot, String limitType, String limitNumber,
            String currentNumber) {
        super();

        mRoot = inRoot;

        setText("Approaching Transfer Limit");

        this.setWidth("310px");
        this.setHeight("150" + "px");

        DockPanel panel = new DockPanel();

        Label promptLabel = new Label("Warning! You are Approaching Your Transfer Limit");
        promptLabel.setHeight("20px");
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
                        + "If you want to prevent your downloads from being automatically stopped once the limit is hit, please change your limit settings in the Data Usage section");

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
            SettingsDialog dlg = new SettingsDialog(EntireUIRoot.getRoot(this.mRoot),
                    this.mRoot.swarmFileBrowser, 1);
            dlg.show();
            dlg.setVisible(false);
            dlg.center();
            dlg.setVisible(true);
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
