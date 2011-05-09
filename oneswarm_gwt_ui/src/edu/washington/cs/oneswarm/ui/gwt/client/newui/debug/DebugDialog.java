package edu.washington.cs.oneswarm.ui.gwt.client.newui.debug;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

public class DebugDialog extends OneSwarmDialogBox {

    public DebugDialog() {
        super(false, true, true);

        this.setText("Debug");

        VerticalPanel vp = new VerticalPanel();

        FlowPanel buttons = new FlowPanel();

        final TextArea ta = new TextArea();
        ta.setStyleName("os-connectionLog");
        ta.setReadOnly(true);
        ta.getElement().setAttribute("WRAP", "OFF");

        ta.setVisibleLines(35);
        vp.setHeight("100%");
        vp.setWidth("100%");

        buttons.setWidth("700px");

        ClickListener cl = new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                System.out.println("click: " + ((Button) sender).getText());
                OneSwarmRPCClient.getService().debug(OneSwarmRPCClient.getSessionID(),
                        ((Button) sender).getText(), new AsyncCallback<String>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            @Override
                            public void onSuccess(String result) {
                                System.out.println("debug: " + result);
                                if (result != null) {
                                    ta.setText(result);
                                } else {
                                    ta.setText("no result (check server side stdout)");
                                }
                            }
                        });
            }
        };

        ClickListener cl_param = new ClickListener() {
            @Override
            public void onClick(Widget sender) {
                System.out.println("click: " + ((Button) sender).getText());
                String param = Window.prompt("param?:", "");
                OneSwarmRPCClient.getService().debug(OneSwarmRPCClient.getSessionID(),
                        ((Button) sender).getText() + " " + param, new AsyncCallback<String>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                caught.printStackTrace();
                            }

                            @Override
                            public void onSuccess(String result) {
                                System.out.println("debug: " + result);
                                if (result != null) {
                                    ta.setText(result);
                                } else {
                                    ta.setText("no result (check server side stdout)");
                                }
                            }
                        });
            }
        };
        // new Button("remove all largest-file-audio swarms")
        Button[] buttonarr = new Button[] { new Button("friend ids"), new Button("friend logs"),
                new Button("DL managers"), new Button("friend files"), new Button("dht"),
                new Button("check_async_output"), new Button("reshare unseen with all friends"),
                new Button("f2f debug"), new Button("dump-messages"),
                new Button("disconnect all friends"), new Button("force connect to all friends"),
                new Button("id3"), new Button("bind_audio"), new Button("autotag_music"),
                new Button("Remote Access"), new Button("refresh_community_servers"),
                new Button("ffmpeg"), new Button("searches"), new Button("locks"),
                new Button("queue lengths"), new Button("rpc profiling"),
                new Button("backendtask"), new Button("error_dlog"), new Button("threads"),
                new Button("reload_logging"), new Button("reshare_with_all_friends"),
                new Button("republish_location") };

        for (Button b : buttonarr) {
            b.addClickListener(cl);

            buttons.add(b);
            // b.setWidth("100%");
        }

        Button[] paramButtons = new Button[] { new Button("dht_lookup"), new Button("dht_put") };
        for (Button b : paramButtons) {
            b.addClickListener(cl_param);

            buttons.add(b);
            // b.setWidth("100%");
        }

        vp.add(buttons);

        ta.setWidth("98%");
        vp.add(ta);

        this.setWidget(vp);
        this.setWidth("800px");
        // this.setHeight("800px");
    }
}
