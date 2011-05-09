package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class TorrentErrorDialogBox extends OneSwarmDialogBox {
    static final int WIDTH = 350;
    // static final int HEIGHT = 250;

    private static OSMessages msg = OneSwarmGWT.msg;
    VerticalPanel mainPanel = new VerticalPanel();

    public static final String CSS_ERROR_TEXT = "os-error_text";

    Button dismissButton = new Button(msg.button_dismiss());

    Button fixButton = new Button(msg.button_fix());
    Button fixAllButton = new Button(msg.button_fix_all());

    private TorrentInfo mTorrent;

    private EntireUIRoot mRoot;

    public TorrentErrorDialogBox(final TorrentInfo inTorrent, EntireUIRoot inRoot) {
        super(false, false, true);

        mTorrent = inTorrent;
        mRoot = inRoot;

        setText(msg.torrent_error_dialog_warning() + ": "
                + StringTools.truncate(inTorrent.getName(), 32, true));

        mainPanel.setWidth(WIDTH + "px");
        // mainPanel.setHeight(HEIGHT+"px");

        final TextArea errorText = new TextArea();

        errorText.setText(msg.torrent_error_dialog_text());

        errorText.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown(KeyDownEvent event) {
                errorText.cancelKey();
            }
        });

        errorText.setWidth(WIDTH + "px");
        errorText.setVisibleLines(8);
        errorText.addStyleName(CSS_ERROR_TEXT);

        mainPanel.add(errorText);

        HorizontalPanel status_and_button = new HorizontalPanel();
        // status_and_button.setWidth("100%");

        status_and_button.add(fixButton);
        status_and_button.add(fixAllButton);
        status_and_button.add(dismissButton);
        status_and_button.setSpacing(3);

        dismissButton.addClickHandler(this);
        fixButton.addClickHandler(this);
        fixAllButton.addClickHandler(this);

        mainPanel.add(status_and_button);

        mainPanel.setCellHorizontalAlignment(status_and_button, HorizontalPanel.ALIGN_RIGHT);

        setWidget(mainPanel);

        show();
        setVisible(false);
        center();
        setVisible(true);
    }

    public void onClick(ClickEvent event) {

        Object source = event.getSource();

        if (source.equals(dismissButton)) {
            hide();
        } else if (source.equals(fixButton)) {
            OneSwarmRPCClient.getService().fixPermissions(OneSwarmRPCClient.getSessionID(),
                    mTorrent, false, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            if (caught instanceof OneSwarmException) {
                                new ReportableErrorDialogBox((OneSwarmException) caught, false);
                            } else {
                                new ReportableErrorDialogBox(caught.toString(), false);
                            }
                        }

                        public void onSuccess(Void result) {
                            mRoot.refreshSwarms();
                        }
                    });
        } else if (source.equals(fixAllButton)) {
            OneSwarmRPCClient.getService().fixPermissions(OneSwarmRPCClient.getSessionID(),
                    mTorrent, true, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            if (caught instanceof OneSwarmException) {
                                new ReportableErrorDialogBox((OneSwarmException) caught, false);
                            } else {
                                new ReportableErrorDialogBox(caught.toString(), false);
                            }
                        }

                        public void onSuccess(Void result) {
                            mRoot.refreshSwarms();
                        }
                    });
        }

        super.onClick(event);
    }
}
