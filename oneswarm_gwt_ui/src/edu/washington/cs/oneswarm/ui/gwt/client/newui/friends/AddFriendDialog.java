/**
 * Copyright (C) 2008 Tomas Isdal
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Created Jul 10, 2008 by isdal
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.Date;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

class AddFriendDialog extends OneSwarmDialogBox implements ClickHandler, ChangeHandler,
        KeyUpHandler {

    private final VerticalPanel mainPanel = new VerticalPanel();
    private final TextBox nickBox = new TextBox();
    private final TextArea publicKeyArea = new TextArea();
    private final Button okButton = new Button("Add friend");
    private final Button cancelButton = new Button("Cancel");
    private final static int WIDTH = 430;

    private final FriendListPanel parent;
    private Label lengthLabel;

    public AddFriendDialog(FriendListPanel parent) {
        this.parent = parent;
        this.setText("Add friend using public key");

        Label selectLabel = new Label(
                "Enter your friend's nickname and public key into the fields below.");
        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");
        mainPanel.add(selectLabel);

        // nickname
        HorizontalPanel nickPanel = new HorizontalPanel();
        nickPanel.setWidth("100%");
        Label nickLabel = new Label("Nickname: ");
        nickPanel.add(nickLabel);
        nickPanel.add(nickBox);
        nickPanel.setSpacing(3);
        nickBox.setWidth("300px");
        nickPanel.setCellHorizontalAlignment(nickBox, HorizontalPanel.ALIGN_RIGHT);
        nickPanel.setCellVerticalAlignment(nickLabel, VerticalPanel.ALIGN_MIDDLE);
        mainPanel.add(nickPanel);

        // public key
        publicKeyArea.setVisibleLines(7);
        publicKeyArea.setWidth(WIDTH - 20 + "px");
        publicKeyArea.addChangeHandler(this);
        publicKeyArea.addKeyUpHandler(this);
        mainPanel.add(publicKeyArea);
        mainPanel.setCellHorizontalAlignment(publicKeyArea, HorizontalPanel.ALIGN_CENTER);

        // confirm
        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.setSpacing(5);
        lengthLabel = new Label("0 / 216");
        // buttonPanel.add(lengthLabel); // skip this for now
        buttonPanel.setCellHorizontalAlignment(lengthLabel, HorizontalPanel.ALIGN_LEFT);
        buttonPanel.setCellVerticalAlignment(lengthLabel, HorizontalPanel.ALIGN_MIDDLE);
        buttonPanel.setWidth("100%");

        // okButton.setEnabled(false);

        HorizontalPanel rhs = new HorizontalPanel();
        rhs.add(okButton);
        okButton.addClickHandler(this);
        rhs.add(cancelButton);
        cancelButton.addClickHandler(this);
        rhs.setSpacing(3);
        buttonPanel.add(rhs);
        buttonPanel.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);

        mainPanel.add(buttonPanel);
        mainPanel.setCellHorizontalAlignment(buttonPanel, VerticalPanel.ALIGN_RIGHT);
        mainPanel.setWidth(WIDTH + "px");
        setWidget(mainPanel);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            addFriend(nickBox.getText(), publicKeyArea.getText());
        } else if (event.getSource().equals(cancelButton)) {
            hide();
        } else {
            super.onClick(event);
        }
    }

    private void addFriend(String nick, String publicKey) {
        final FriendInfoLite friend = new FriendInfoLite();
        friend.setBlocked(false);
        friend.setCanSeeFileList(true);
        friend.setPublicKey(publicKeyArea.getText());
        friend.setName(nickBox.getText());
        friend.setSource("Manual");
        friend.setConnectionId(-1);
        friend.setDateAdded(new Date());

        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<Void> callback = new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                OneSwarmGWT.log("added friend " + result);
                parent.updateUI();
                createPopup();
            }

            private void createPopup() {

                FriendPropertiesDialog dlg = new FriendPropertiesDialog(friend, false);
                dlg.show();
                dlg.setVisible(false);
                dlg.center();
                dlg.setVisible(true);
                dlg.saveFriend();
                hide();
            }

            public void onFailure(Throwable caught) {
                String error = caught.getMessage();
                if (error.equals("Friend already in friend list")) {
                    createPopup();
                } else {
                    Window.alert("Problem adding Friend: " + "\n" + error);
                }
            }
        };
        service.addFriend(session, friend, false, callback);

    }

    public void onChange(ChangeEvent sender) {
        wordWrap();
    }

    private void wordWrap() {
        // some word wrapping would be nice here...
        String text = publicKeyArea.getText();
        text = text.replaceAll("\\s+", "");
        int pos = 0;
        int len = text.length();

        StringBuilder b = new StringBuilder();
        while (pos < len) {
            int maxlen = Math.min(45, text.length() - pos);
            b.append(text.substring(pos, pos + maxlen));
            b.append("\n");
            pos += maxlen;
        }

        publicKeyArea.setText(b.toString());
    }

    public void onKeyUp(KeyUpEvent event) {
        wordWrap();
    }
}