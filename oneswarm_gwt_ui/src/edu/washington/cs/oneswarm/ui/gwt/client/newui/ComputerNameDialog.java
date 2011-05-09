package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

public class ComputerNameDialog extends OneSwarmDialogBox {

    public static final int WIDTH = 400;
    private VerticalPanel mainPanel;
    private NameSetCallback callback;

    public interface NameSetCallback {
        public void setName(String newName);
    };

    public ComputerNameDialog(String initialName, boolean modal, NameSetCallback nameSetCallback) {
        super(false, modal, true);

        this.callback = nameSetCallback;

        setText(msg.settings_computer_name_header());

        Label selectLabel = new Label(msg.settings_computer_name_msg());
        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");

        mainPanel = new VerticalPanel();
        mainPanel.setWidth(WIDTH + "px");

        mainPanel.add(selectLabel);

        HorizontalPanel p = new HorizontalPanel();
        mainPanel.add(p);
        Label nameLabel = new Label(msg.settings_computer_name());
        p.add(nameLabel);
        p.setSpacing(3);
        p.setCellVerticalAlignment(nameLabel, VerticalPanel.ALIGN_MIDDLE);

        final TextBox computerNameBox = new TextBox();
        computerNameBox.setText(initialName);
        p.add(computerNameBox);

        p = new HorizontalPanel();
        p.setSpacing(0);
        Button okButton = new Button(msg.button_save());
        Button cancelButton = new Button(msg.button_cancel());

        p.add(cancelButton);
        p.add(okButton);

        okButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {

                if (computerNameBox.getText() == null) {
                    Window.alert(msg.settings_computer_name_problem_no_name());
                    return;
                }

                if (computerNameBox.getText().length() == 0) {
                    Window.alert(msg.settings_computer_name_problem_no_name());
                    return;
                }

                if (computerNameBox.getText().length() > 64) {
                    Window.alert(msg.settings_computer_name_problem_long_name());
                    return;
                }

                OneSwarmRPCClient.getService().setComputerName(
                        OneSwarmRPCClient.getSessionID(),
                        computerNameBox.getText().substring(0,
                                Math.min(computerNameBox.getText().length(), 64)),
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(Void result) {
                                callback.setName(computerNameBox.getText());
                            }
                        });

                hide();
            }
        });

        cancelButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ComputerNameDialog.this.hide();
            }
        });
        p.setSpacing(3);
        mainPanel.add(p);
        mainPanel.setCellHorizontalAlignment(p, HorizontalPanel.ALIGN_RIGHT);

        mainPanel.setSpacing(0);

        setWidget(mainPanel);
    }

    // OneSwarmRPCClient.getService().getSelf(OneSwarmRPCClient.getSessionID(),
    // new AsyncCallback<FriendInfoLite>() {
    // public void onFailure(Throwable caught) {
    // }
    //
    // public void onSuccess(FriendInfoLite result) {

}
