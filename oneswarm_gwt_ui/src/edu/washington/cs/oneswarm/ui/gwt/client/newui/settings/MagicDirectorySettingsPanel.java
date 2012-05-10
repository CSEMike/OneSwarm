package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.List;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.fileDialog.FileBrowser;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;

public class MagicDirectorySettingsPanel extends SettingsPanel implements ClickHandler {
    ListBox currentList = new ListBox();
    Button scanButton = new Button(msg.button_scan_now());
    Button addButton = new Button(msg.button_add());
    Button removeButton = new Button(msg.button_remove());
    Button propertiesButton = new Button(msg.button_properties());

    final List<MagicPath> watchDirs = new ArrayList<MagicPath>();
    ListBox timerListBox = new ListBox();

    boolean intervalLoaded = false, dirsLoaded = false;

    public MagicDirectorySettingsPanel() {
        this.setWidth("100%");

        HorizontalPanel topPanel = new HorizontalPanel();
        add(topPanel);
        setCellHorizontalAlignment(topPanel, VerticalPanel.ALIGN_LEFT);

        topPanel.setWidth("100%");
        Label label = new Label(msg.settings_files_watch_dir());
        label.setHorizontalAlignment(Label.ALIGN_LEFT);
        label.setWidth("100%");
        topPanel.add(label);
        topPanel.setCellWidth(label, "300px");
        topPanel.setCellHorizontalAlignment(label, HorizontalPanel.ALIGN_LEFT);
        /*
         * set the button invisible until we figure out how to trigger a check
         */
        scanButton.setVisible(false);
        scanButton.setWidth("75px");
        scanButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        topPanel.add(scanButton);
        topPanel.setSpacing(3);
        topPanel.setCellHorizontalAlignment(scanButton, HorizontalPanel.ALIGN_CENTER);

        currentList.setVisibleItemCount(3);

        addButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        if (OneSwarmGWT.isRemoteAccess() == true) {
            addButton.setEnabled(false);
        }
        removeButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        propertiesButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

        currentList.setWidth("100%");

        currentList.addItem(msg.loading());

        if (OneSwarmGWT.isRemoteAccess() == false) {
            addButton.addClickHandler(this);
        }
        removeButton.addClickHandler(this);
        propertiesButton.addClickHandler(this);

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(currentList);
        hp.setCellWidth(currentList, "300px");
        // currentList.setWidth("300px");
        hp.setSpacing(3);
        VerticalPanel vp = new VerticalPanel();
        removeButton.setWidth("75px");
        addButton.setWidth("75px");
        propertiesButton.setWidth("75px");
        vp.setSpacing(3);
        vp.add(addButton);
        vp.add(removeButton);
        vp.add(propertiesButton);
        hp.add(vp);
        hp.setCellVerticalAlignment(vp, HorizontalPanel.ALIGN_MIDDLE);

        add(hp);
        this.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_CENTER);

        Label l = new Label(msg.settings_files_refresh_interval() + ":");
        hp = new HorizontalPanel();
        hp.setSpacing(3);
        timerListBox.addItem(msg.settings_files_automatic());
        for (int i : new int[] { 1, 5, 15, 60 }) {
            timerListBox.addItem(i + " " + msg.settings_files_minutes());
        }
        hp.add(l);
        hp.add(timerListBox);
        hp.setCellVerticalAlignment(l, ALIGN_MIDDLE);
        hp.setCellVerticalAlignment(timerListBox, ALIGN_MIDDLE);

        timerListBox.setEnabled(false);

        add(hp);

        OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "oneswarm.watchdir.refresh.interval", new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        if (result == null) {
                            result = new Integer(0);
                        }

                        if (result.intValue() == 0) {
                            timerListBox.setSelectedIndex(0);
                        } else { // find it, if not there, add at end
                            try {
                                String match = result.toString();
                                boolean found = false;
                                for (int i = 0; i < timerListBox.getItemCount(); i++) {
                                    if (timerListBox.getItemText(i).split("\\s+")[0].equals(match)) {
                                        timerListBox.setSelectedIndex(i);
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    timerListBox.addItem(result + " "
                                            + msg.settings_files_minutes());
                                    timerListBox.setSelectedIndex(timerListBox.getItemCount() - 1);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        timerListBox.setEnabled(true);

                        intervalLoaded = true;
                        if (dirsLoaded) {
                            loadNotify();
                        }
                    }
                });

        // System.out.println("get settings rpc");
        OneSwarmRPCClient.getService().getStringListParameterValue(
                OneSwarmRPCClient.getSessionID(), "Magic Watch Directories",
                new AsyncCallback<ArrayList<String>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(ArrayList<String> result) {
                        // System.out.println("got watch dir result");
                        watchDirs.clear();
                        if (result != null) {
                            for (int i = 0; i < result.size(); i++) {
                                if (result.get(i).length() > 0) {
                                    try {
                                        watchDirs.add(new MagicPath(result.get(i)));
                                    } catch (MagicPathParseException e) {
                                        System.err.println(e.toString() + " on " + result.get(i));
                                        watchDirs.clear();
                                        break;
                                    }
                                }
                            }
                        }
                        refreshList();
                        dirsLoaded = true;
                        if (intervalLoaded) {
                            loadNotify();
                        }
                    }
                });
    }

    private void refreshList() {
        currentList.clear();
        for (MagicPath d : watchDirs) {
            System.out.println("refresh with path: " + d.toString());
            currentList.addItem(d.getPath());
        }
        System.out.println("current list size: " + watchDirs.size());
    }

    public void sync() {
        System.out.println("attempting to sync magic dir settings");

        ArrayList<String> converted = new ArrayList<String>();
        for (MagicPath p : watchDirs) {
            converted.add(p.toString());
        }

        OneSwarmRPCClient.getService().setStringListParameterValue(
                OneSwarmRPCClient.getSessionID(), "Magic Watch Directories", converted,
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void foo) {
                        System.out.println("backend ack for magic dir settings sync");
                    }
                });

        int refreshInterval = 0;
        try {
            if (timerListBox.getSelectedIndex() > 0) {
                refreshInterval = Integer.parseInt(timerListBox.getItemText(
                        timerListBox.getSelectedIndex()).split("\\s+")[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "oneswarm.watchdir.refresh.interval", refreshInterval, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("backend ack for interval sync");
                    }
                });
    }

    public void onClick(ClickEvent event) {
        Object sender = event.getSource();
        if (sender.equals(addButton)) {
            addButton.setEnabled(false);
            FileBrowser dialog = new FileBrowser(OneSwarmRPCClient.getSessionID(),
                    true, new AsyncCallback<String>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(final String selectedPath) {
                            if (selectedPath == null) {
                                addButton.setEnabled(true);
                                return;
                            }

                            // skip dups
                            if (watchDirs.contains(selectedPath) == false) {
                                MagicTypeSelectionDialog dlg = new MagicTypeSelectionDialog(
                                        MagicWatchType.Magic,
                                        new MagicTypeSelectionDialog.Callback() {
                                            public void done(boolean cancelled, MagicWatchType which) {
                                                if (!cancelled) {
                                                    MagicPath mpath = new MagicPath(selectedPath,
                                                            which);
                                                    watchDirs.add(mpath);
                                                    refreshList();
                                                }
                                            }
                                        });

                                dlg.show();
                                dlg.setVisible(false);
                                dlg.center();
                                dlg.setPopupPosition(dlg.getPopupLeft(),
                                        Window.getScrollTop() + 125);
                                dlg.setVisible(true);
                            }
                            addButton.setEnabled(true);
                        }
                    });
            dialog.show();
        } else if (sender.equals(removeButton)) {
            if (currentList.getSelectedIndex() != -1) {
                watchDirs.remove(currentList.getSelectedIndex());
                refreshList();
            }
        } else if (sender.equals(propertiesButton)) {
            if (currentList.getSelectedIndex() == -1 || isReadyToSave() == false) {
                return;
            }

            MagicTypeSelectionDialog dlg = new MagicTypeSelectionDialog(watchDirs.get(currentList
                    .getSelectedIndex()).mType, new MagicTypeSelectionDialog.Callback() {
                public void done(boolean cancelled, MagicWatchType which) {
                    if (!cancelled) {
                        watchDirs.get(currentList.getSelectedIndex()).mType = which;
                    }
                }
            });

            dlg.show();
            dlg.setVisible(false);
            dlg.center();
            dlg.setPopupPosition(dlg.getPopupLeft(), dlg.getPopupTop() + 20);
            dlg.setVisible(true);
        }
    }

    String validData() {
        return null; // should be a valid directory path since it was chosen
        // using the directory chooser
    }
}
