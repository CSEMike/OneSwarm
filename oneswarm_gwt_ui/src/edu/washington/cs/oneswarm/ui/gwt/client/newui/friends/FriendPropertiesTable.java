package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public class FriendPropertiesTable extends Composite {

    private final List<FriendPropertiesPanel> panels;
    private final boolean forceUpdate;

    public FriendPropertiesTable(FriendInfoLite[] friends, boolean forceUpdate, boolean showSkip) {
        this.forceUpdate = forceUpdate;

        VerticalPanel mainPanel = new VerticalPanel();
        ScrollPanel scroll = new ScrollPanel(mainPanel);
        scroll.setWidth("100%");
        mainPanel.setWidth("100%");
        if (friends.length == 0) {
            mainPanel.add(new Label("No new friends found"));
        }
        panels = new ArrayList<FriendPropertiesPanel>();

        Arrays.sort(friends);
        // HorizontalPanel labelPanel = new HorizontalPanel();
        // Label nameLabel = new Label("Friend nick");
        // nameLabel.setWidth("250px");
        // labelPanel.add(nameLabel);
        // Label blockLabel = new Label("Block");
        // blockLabel.setWidth("50px");
        int addCount = 0;
        for (int i = 0; i < friends.length; i++) {
            if (friends[i].getStatus() != FriendInfoLite.STATUS_TO_BE_DELETED) {
                FriendPropertiesPanel fp = new FriendPropertiesPanel(friends[i], showSkip);
                mainPanel.add(fp);
                panels.add(fp);
                addCount++;
            }
        }

        if (showSkip && addCount > 0) {
            HorizontalPanel buttonPanel = new HorizontalPanel();
            final Button selectAll = new Button("Select All");
            final Button selectNone = new Button("Select None");
            selectAll.addStyleName(OneSwarmCss.SMALL_BUTTON);
            selectNone.addStyleName(OneSwarmCss.SMALL_BUTTON);
            ClickListener selectAllNoneListener = new ClickListener() {
                public void onClick(Widget sender) {
                    boolean val = sender.equals(selectAll);
                    for (FriendPropertiesPanel p : panels) {
                        p.setChecked(val);
                    }
                }
            };
            selectAll.addClickListener(selectAllNoneListener);
            selectNone.addClickListener(selectAllNoneListener);
            buttonPanel.add(selectAll);
            buttonPanel.add(selectNone);
            mainPanel.add(buttonPanel);
        }

        this.initWidget(scroll);
    }

    public void saveChanges(boolean reallySkip) {
        for (FriendPropertiesPanel fp : panels) {
            fp.saveChanges(null, forceUpdate, reallySkip);
        }
    }
}
