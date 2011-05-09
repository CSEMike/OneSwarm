package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class Footer extends HorizontalPanel {
    private static final String CSS_MAIN_FOOTER = "os-main_footer";
    private String mVersionString = "checking version...";

    protected static OSMessages msg = OneSwarmGWT.msg;

    final HTML versionLabel = new HTML();

    public Footer() {
        super();
        addStyleName(CSS_MAIN_FOOTER);

        OneSwarmRPCClient.getService().getVersion(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                        onSuccess("");
                    }

                    public void onSuccess(String result) {
                        mVersionString = result;
                        refreshLabel();
                    }
                });

        add(versionLabel);
        refreshLabel();
    }

    /*
     * Only if we've loaded it
     */
    public String getVersion() {
        return mVersionString;
    }

    private void refreshLabel() {
        versionLabel
                .setHTML("<center><small><a href=\"http://oneswarm.cs.washington.edu/\">OneSwarm</a>: "
                        + mVersionString
                        + " | <a href=\"http://forum.oneswarm.org/\">"
                        + msg.footer_forum()
                        + "</a>"
                        + " | <a href=\"http://wiki.oneswarm.org/\"><b>"
                        + msg.footer_help()
                        + "</b></a></small></center>");
    }
}
