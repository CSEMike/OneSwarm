package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public class KeywordFilterSettingsPanel extends SettingsPanel implements ClickHandler {
    Button addButton = new Button(msg.button_add());
    Button removeButton = new Button(msg.button_remove());
    ListBox currentList = new ListBox();

    List<String> currentKeywords = new ArrayList<String>();

    public KeywordFilterSettingsPanel() {
        HorizontalPanel topPanel = new HorizontalPanel();
        add(topPanel);
        setCellHorizontalAlignment(topPanel, VerticalPanel.ALIGN_LEFT);

        topPanel.setWidth("100%");
        currentList.setVisibleItemCount(4);

        addButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        removeButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

        currentList.setWidth("100%");
        currentList.addItem(msg.loading());

        addButton.addClickHandler(this);
        removeButton.addClickHandler(this);

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(currentList);
        hp.setCellWidth(currentList, "300px");
        // currentList.setWidth("300px");
        hp.setSpacing(3);
        VerticalPanel vp = new VerticalPanel();
        removeButton.setWidth("75px");
        addButton.setWidth("75px");
        vp.setSpacing(3);
        vp.add(addButton);
        vp.add(removeButton);

        addButton.setEnabled(false);
        removeButton.setEnabled(false);

        hp.add(vp);
        hp.setCellVerticalAlignment(vp, HorizontalPanel.ALIGN_MIDDLE);

        add(hp);
        this.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_CENTER);

        OneSwarmRPCClient.getService().getStringListParameterValue(
                OneSwarmRPCClient.getSessionID(), "oneswarm.search.filter.keywords",
                new AsyncCallback<ArrayList<String>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                        System.err.println("oneswarm.community.servers settings get failed! "
                                + caught.toString());
                    }

                    public void onSuccess(ArrayList<String> result) {

                        for (String s : result) {
                            currentKeywords.add(s);
                        }

                        refreshUI();

                        addButton.setEnabled(true);
                        removeButton.setEnabled(true);

                        loadNotify();
                    }
                });
    }

    public void refreshUI() {
        currentList.clear();
        Set<String> existing = new HashSet<String>();
        for (String keyword : currentKeywords) {

            if (existing.contains(keyword)) {
                System.out.println("skipping duplicate keyword: " + keyword);
                continue;
            }

            currentList.addItem(keyword);
            existing.add(keyword);
        }
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(addButton)) {
            String neu = Window.prompt(msg.search_filter_add_prompt(), "");
            if (neu != null) {
                if (neu.length() >= 3) {
                    currentKeywords.add(neu + " ("
                            + msg.add_friends_invite_create_delivery_manual() + ")");
                    refreshUI();
                } else {
                    Window.alert(msg.search_filter_length_error());
                }
            }
        } else if (event.getSource().equals(removeButton)) {
            int index = currentList.getSelectedIndex();
            if (index >= 0) {
                String v = currentList.getItemText(index);
                currentList.removeItem(index);
                currentKeywords.remove(v);
                refreshUI();
            }
        }
    }

    public void sync() {
        ArrayList<String> params = new ArrayList<String>();
        for (String keyword : currentKeywords) {
            params.add(keyword);
        }

        OneSwarmRPCClient.getService().setStringListParameterValue(
                OneSwarmRPCClient.getSessionID(), "oneswarm.search.filter.keywords", params,
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("saved search filter keywords successfully");
                    }
                });
    }

    String validData() {
        return null;
    }
}
