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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

class ShowMyPublicKeyDialog extends OneSwarmDialogBox {

    private final VerticalPanel mainPanel = new VerticalPanel();
    private final Button okButton = new Button("Close");
    private final TextArea publicKeyArea = new TextArea();
    private final static int WIDTH = 375;
    private final static int HEIGHT = 140;

    public ShowMyPublicKeyDialog() {
        this.setText("Local Public Key");
        mainPanel.setWidth(WIDTH + "px");
        mainPanel.setHeight(HEIGHT + "px");

        // public key
        publicKeyArea.setWidth(WIDTH + "px");
        publicKeyArea.setVisibleLines(7);
        mainPanel.add(publicKeyArea);

        okButton.addClickHandler(this);
        okButton.setWidth("100px");
        mainPanel.add(okButton);
        mainPanel.setCellHorizontalAlignment(okButton, HorizontalPanel.ALIGN_CENTER);
        mainPanel.setWidth(WIDTH + "px");
        mainPanel.setHeight(HEIGHT + "px");
        setWidget(mainPanel);

        this.update();
    }

    private void update() {
        String session = OneSwarmRPCClient.getSessionID();
        OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

        AsyncCallback<String> callback = new AsyncCallback<String>() {
            public void onSuccess(String result) {
                publicKeyArea.setText(result);
                wordWrap();
            }

            public void onFailure(Throwable caught) {
                // well, do nothing...
                OneSwarmGWT.log("error " + caught.getMessage());
            }
        };
        service.getMyPublicKey(session, callback);
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

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(okButton)) {
            hide();
        } else {
            super.onClick(event);
        }
    }
}