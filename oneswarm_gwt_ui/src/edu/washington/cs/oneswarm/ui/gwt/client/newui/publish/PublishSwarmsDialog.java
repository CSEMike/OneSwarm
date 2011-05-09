package edu.washington.cs.oneswarm.ui.gwt.client.newui.publish;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ImageConstants;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class PublishSwarmsDialog extends OneSwarmDialogBox implements ClickHandler {

    static final int WIDTH = 450;
    // static final int HEIGHT = 200;

    VerticalPanel mainPanel = new VerticalPanel();

    Button cancelButton = new Button(msg.button_cancel());
    Button publishButton = new Button(msg.button_publish());

    Label statusLabel = new Label();

    private HorizontalPanel status_hp;
    private TorrentInfo[] mSwarms;

    private ListBox serversPopup = new ListBox();

    private List<CommunityRecord> menuRecs = new ArrayList<CommunityRecord>();

    private List<PublishSwarmInfoPanel> infoPanels;

    public PublishSwarmsDialog(EntireUIRoot inRoot, TorrentInfo[] infos) {
        super(false, true, true);

        mSwarms = infos;
        infoPanels = new ArrayList<PublishSwarmInfoPanel>(infos.length);

        setText(msg.publish_title());

        Label selectLabel = new Label(msg.publish_help());
        selectLabel.addStyleName(CSS_DIALOG_HEADER);
        selectLabel.setWidth(WIDTH + "px");
        mainPanel.add(selectLabel);
        mainPanel.setCellVerticalAlignment(selectLabel, VerticalPanel.ALIGN_TOP);

        mainPanel.setWidth(WIDTH + "px");
        // mainPanel.setHeight(HEIGHT+"px");

        serversPopup.addItem("...");

        mainPanel.add(serversPopup);

        HorizontalPanel buttons_hp = new HorizontalPanel();
        buttons_hp.add(cancelButton);
        buttons_hp.add(publishButton);
        buttons_hp.setSpacing(3);

        cancelButton.setEnabled(false);
        publishButton.setEnabled(false);

        cancelButton.addClickHandler(this);
        publishButton.addClickHandler(this);

        status_hp = new HorizontalPanel();
        status_hp.add(new Image(ImageConstants.PROGRESS_SPINNER));
        status_hp.add(statusLabel);

        status_hp.setCellVerticalAlignment(statusLabel, VerticalPanel.ALIGN_MIDDLE);
        status_hp.setVisible(false);

        HorizontalPanel status_and_buttons = new HorizontalPanel();
        status_and_buttons.add(status_hp);
        status_and_buttons.add(buttons_hp);

        status_and_buttons.setCellVerticalAlignment(status_hp, VerticalPanel.ALIGN_MIDDLE);
        status_and_buttons.setCellHorizontalAlignment(buttons_hp, HorizontalPanel.ALIGN_RIGHT);
        status_and_buttons.setWidth("100%");

        for (TorrentInfo s : mSwarms) {
            DisclosurePanel p = new DisclosurePanel(s.getName(), true);
            PublishSwarmInfoPanel siPanel = new PublishSwarmInfoPanel(s);
            infoPanels.add(siPanel);
            p.add(siPanel);
            if (mSwarms.length <= 4) {
                p.setOpen(true);
            } else {
                p.setOpen(false);
            }
            mainPanel.add(p);
        }

        com.google.gwt.user.client.ui.Widget hrule = new SimplePanel();
        hrule.addStyleName(SettingsDialog.CSS_HRULE);
        mainPanel.add(hrule);
        mainPanel.add(status_and_buttons);

        setWidget(mainPanel);

        final ChangeHandler serversChangeHandler = new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                for (PublishSwarmInfoPanel p : infoPanels) {
                    p.updateCategories(null);
                }

                /**
                 * hack attack for backwards compatibility with old community
                 * servers
                 */
                CommunityRecord selected = menuRecs.get(serversPopup.getSelectedIndex());
                OneSwarmRPCClient.getService().getCategoriesForCommunityServer(
                        OneSwarmRPCClient.getSessionID(), selected,
                        new AsyncCallback<ArrayList<String>>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(ArrayList<String> result) {
                                for (PublishSwarmInfoPanel p : infoPanels) {
                                    p.updateCategories(result);
                                }
                            }
                        });
            }
        };

        OneSwarmRPCClient.getService().getStringListParameterValue(
                OneSwarmRPCClient.getSessionID(), "oneswarm.community.servers",
                new AsyncCallback<ArrayList<String>>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(ArrayList<String> result) {

                        serversPopup.clear();

                        for (int i = 0; i < result.size() / 5; i++) {
                            CommunityRecord candidate = new CommunityRecord(result, 5 * i);

                            if (candidate.getSupports_publish() != null) {
                                menuRecs.add(candidate);
                                StringBuilder name = new StringBuilder();
                                if (candidate.getServer_name() != null) {
                                    name.append(candidate.getServer_name());
                                    name.append(" - ");
                                }
                                name.append(StringTools.truncate(candidate.getUrl(), 64, true));
                                serversPopup.addItem(StringTools.truncate(name.toString(), 64, true));
                            }
                        }

                        if (serversPopup.getItemCount() == 0) {
                            Window.alert(msg.publish_no_servers());
                            hide();
                            return;
                        }

                        serversChangeHandler.onChange(null);

                        cancelButton.setEnabled(true);
                        publishButton.setEnabled(true);
                    }
                });

        serversPopup.addChangeHandler(serversChangeHandler);
    }

    public void onClick(ClickEvent event) {
        if (event.getSource().equals(cancelButton)) {
            hide();
        } else if (event.getSource().equals(publishButton)) {

            String[] previewPaths = new String[mSwarms.length];
            String[] comments = new String[mSwarms.length];
            String[] categories = new String[mSwarms.length];

            for (int i = 0; i < mSwarms.length; i++) {
                previewPaths[i] = infoPanels.get(i).getPreviewPath();
                comments[i] = infoPanels.get(i).getDescription();
                categories[i] = infoPanels.get(i).getCategory();
            }

            statusLabel.setText("");
            status_hp.setVisible(true);

            OneSwarmRPCClient.getService().publishSwarms(OneSwarmRPCClient.getSessionID(), mSwarms,
                    previewPaths, comments, categories,
                    menuRecs.get(serversPopup.getSelectedIndex()),
                    new AsyncCallback<BackendTask>() {
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        public void onSuccess(BackendTask result) {
                            hide();
                        }
                    });

        } else {
            super.hide();
        }
    }
}
