package edu.washington.cs.oneswarm.ui.gwt.client.newui.publish;

import java.util.List;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.fileDialog.FileBrowser;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class PublishSwarmInfoPanel extends VerticalPanel {

    protected static OSMessages msg = OneSwarmGWT.msg;

    String mPreviewPath = null;
    TextArea descriptionBox = new TextArea();
    ListBox categoriesListBox = new ListBox();

    public PublishSwarmInfoPanel(TorrentInfo info) {

        HorizontalPanel hp = new HorizontalPanel();

        VerticalPanel lhs = new VerticalPanel();

        Button includePreview = new Button(msg.button_select_preview());
        includePreview.addStyleName(OneSwarmCss.SMALL_BUTTON);

        final Image previewImage = new Image(GWT.getModuleBaseURL() + "image?infohash="
                + info.getTorrentID());

        previewImage.addErrorHandler(new ErrorHandler() {
            public void onError(ErrorEvent event) {
                mPreviewPath = null;
                previewImage.setVisible(false);

                Window.alert(msg.publish_preview_image_error());
            }
        });

        includePreview.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
            	FileBrowser dialog = new FileBrowser(
                        OneSwarmRPCClient.getSessionID(), false, new AsyncCallback<String>() {
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            public void onSuccess(String result) {
                                if (result == null) {
                                    return;
                                }

                                mPreviewPath = result;
                                previewImage.setUrl(GWT.getModuleBaseURL() + "image?path="
                                        + URL.encode(result) + "&scale=128");
                                previewImage.setVisible(true);
                            }
                        });
            	dialog.show();
            }
        });
        lhs.add(previewImage);
        lhs.add(includePreview);
        lhs.setCellHorizontalAlignment(includePreview, HorizontalPanel.ALIGN_CENTER);
        lhs.setCellHorizontalAlignment(previewImage, ALIGN_CENTER);

        VerticalPanel rhs = new VerticalPanel();

        descriptionBox.setVisibleLines(5);
        descriptionBox.setWidth("280px");

        HorizontalPanel categoryHP = new HorizontalPanel();
        Label l = new Label(msg.publish_category_label() + ":");
        categoryHP.add(l);
        categoryHP.add(categoriesListBox);
        categoryHP.setCellVerticalAlignment(l, HorizontalPanel.ALIGN_MIDDLE);

        categoriesListBox.setEnabled(false);

        rhs.add(categoryHP);
        rhs.add(new Label(info.getName()));
        rhs.add(new Label(info.getNumFiles() + " " + msg.publish_files_label()));
        rhs.add(new Label(msg.publish_size_label() + ": "
                + StringTools.formatRate(info.getTotalSize(), "")));

        descriptionBox.setText(msg.publish_comment_label());

        rhs.add(descriptionBox);

        if (OneSwarmGWT.isRemoteAccess() == false) {
            hp.add(lhs);
        }
        hp.add(rhs);

        hp.setCellVerticalAlignment(rhs, ALIGN_MIDDLE);

        lhs.setSpacing(3);

        add(hp);
    }

    public void updateCategories(List<String> categories) {
        categoriesListBox.clear();
        if (categories == null) {
            categoriesListBox.setEnabled(false);
            categoriesListBox.addItem(msg.publish_no_categories());
            return;
        }
        if (categories.size() == 0) {
            categoriesListBox.setEnabled(false);
            categoriesListBox.addItem(msg.publish_no_categories());
            return;
        }

        for (String s : categories) {
            categoriesListBox.addItem(s);
        }

        categoriesListBox.setEnabled(true);
    }

    public String getDescription() {
        if (descriptionBox.getText().equals(msg.publish_comment_label())) {
            return "";
        } else {
            return descriptionBox.getText();
        }
    }

    public String getPreviewPath() {
        return mPreviewPath;
    }

    public String getCategory() {
        if (categoriesListBox.isEnabled() == false) {
            return null;
        }
        return categoriesListBox.getItemText(categoriesListBox.getSelectedIndex());
    }

}
