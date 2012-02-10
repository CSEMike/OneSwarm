package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class NewUserSetupPanel extends VerticalPanel implements Updateable {

    private static OSMessages msg = OneSwarmGWT.msg;

    public static final String CSS_WELCOME_BANNER = "os-welcome_banner";
    public static final String CSS_WELCOME_PROMPT = "os-welcome_prompt";
    public static final String CSS_WELCOME_DEFAULTS_BUTTON = "os-welcome_defaults_button";

    private final SwarmsBrowser swarmsBrowser;

    enum State {
        kLoading, kNoFriendsNoCommunity, kNoFriends, kNoOnlineFriends, kFriendsButNotOnline, kFriendsOnline
    }

    private State state = State.kLoading;

    public NewUserSetupPanel(SwarmsBrowser browser) {
        // HTML w = new HTML("<div id=\"" + OneSwarmCss.CSS_NOTHING_SHOWING +
        // "\">" + Strings.get(Strings.NO_FILES_MESSAGE) + "</div>");
        // this.add(w);
        //
        this.swarmsBrowser = browser;

        addStyleName(OneSwarmCss.CSS_NOTHING_SHOWING);
        setWidth("100%");

        updateStatus(msg.loading());
    }

    private void updateStatus(String message) {
        updateStatus(message, true);
    }

    private void updateStatus(String message, boolean show_spinner) {
        clear();

        SimplePanel spacer = new SimplePanel();
        DOM.setStyleAttribute(spacer.getElement(), "height", "75px");
        add(spacer);

        Image spinner = new Image(ImageConstants.PROGRESS_SPINNER);
        spinner.setWidth("16px");
        spinner.setHeight("16px");

        HorizontalPanel status = new HorizontalPanel();
        HTML workingLabel = new HTML(message);
        if (show_spinner) {
            status.add(spinner);
            status.setCellVerticalAlignment(spinner, ALIGN_MIDDLE);
            status.setSpacing(3);
        }
        // workingLabel.addStyleName(CSS_WELCOME_PROMPT);
        status.add(workingLabel);
        status.setCellVerticalAlignment(workingLabel, ALIGN_MIDDLE);
        status.setCellWidth(workingLabel, "375px");
        if (!show_spinner) {
            DOM.setStyleAttribute(workingLabel.getElement(), "textAlign", "center");
        }
        add(status);
        setCellHorizontalAlignment(status, ALIGN_CENTER);

        spacer = new SimplePanel();
        DOM.setStyleAttribute(spacer.getElement(), "height", "150px");
        add(spacer);
    }

    private void firstrun() {
        clear();

        state = State.kNoFriendsNoCommunity;

        Label banner = new Label(msg.swarm_browser_welcome_banner());
        banner.addStyleName(CSS_WELCOME_BANNER);
        add(banner);

        SimplePanel spacer = new SimplePanel();
        DOM.setStyleAttribute(spacer.getElement(), "height", "15px");
        add(spacer);

        Label desc = new Label(msg.swarm_browser_welcome_prompt());
        desc.addStyleName(CSS_WELCOME_PROMPT);
        add(desc);

        this.setCellHorizontalAlignment(desc, ALIGN_CENTER);

        Button defaultsButton = new Button(msg.swarm_browser_welcome_button_default());
        defaultsButton.getElement().setId("useDefaultsLink");
        defaultsButton.addStyleName(CSS_WELCOME_DEFAULTS_BUTTON);
        defaultsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                defaultClicked();
            }
        });

        add(defaultsButton);
        this.setCellHorizontalAlignment(defaultsButton, ALIGN_CENTER);

        Label defaults = new Label(msg.swarm_browser_welcome_defaults());
        add(defaults);
        setCellHorizontalAlignment(defaults, ALIGN_CENTER);

        Hyperlink advanced = new Hyperlink(msg.swarm_browser_welcome_manual_import(), "");
        add(advanced);
        setCellHorizontalAlignment(advanced, ALIGN_CENTER);
        advanced.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                swarmsBrowser.showSettings(1);
            }
        });

        spacer = new SimplePanel();
        DOM.setStyleAttribute(spacer.getElement(), "height", "150px");
        add(spacer);
    }

    @Override
    public void onAttach() {
        super.onAttach();
        OneSwarmGWT.addToUpdateTask(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        OneSwarmGWT.removeFromUpdateTask(this);
    }

    private void defaultClicked() {

        updateStatus(msg.swarm_browser_welcome_configuring());

        OneSwarmRPCClient.getService().applyDefaultSettings(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        String message = caught.getClass().getName() + " / " + caught.toString()
                                + " / " + caught.getMessage();
                        new ReportableErrorDialogBox(message, false);
                    }

                    @Override
                    public void onSuccess(Void result) {
                        state = State.kNoFriends;
                    }
                });
    }

    private long mNextRPC = 0;

    @Override
    public void update(int count) {

        if (mNextRPC < System.currentTimeMillis()) {

            mNextRPC = System.currentTimeMillis() + 10 * 1000;

            // System.out.println("new user rpc");

            OneSwarmRPCClient.getService().getNumberOnlineFriends(OneSwarmRPCClient.getSessionID(),
                    new AsyncCallback<Integer>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            caught.printStackTrace();
                        }

                        @Override
                        public void onSuccess(Integer onlineFriends) {
                            mNextRPC = System.currentTimeMillis() + 10 * 1000;

                            // System.out.println("online friends: " +
                            // onlineFriends);

                            if (onlineFriends == 0) {

                                OneSwarmRPCClient.getService().getNumberFriendsCount(
                                        OneSwarmRPCClient.getSessionID(),
                                        new AsyncCallback<Integer>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                caught.printStackTrace();
                                            }

                                            @Override
                                            public void onSuccess(Integer anyfriends) {

                                                // System.out.println("total friends: "
                                                // + anyfriends);

                                                if (anyfriends == 0) {

                                                    // System.out.println("0 total friends");

                                                    // now also check if there
                                                    // are no community servers
                                                    mNextRPC = System.currentTimeMillis() + 10 * 1000;
                                                    OneSwarmRPCClient
                                                            .getService()
                                                            .getStringListParameterValue(
                                                                    OneSwarmRPCClient
                                                                            .getSessionID(),
                                                                    "oneswarm.community.servers",
                                                                    new AsyncCallback<ArrayList<String>>() {
                                                                        @Override
                                                                        public void onFailure(
                                                                                Throwable caught) {
                                                                        }

                                                                        @Override
                                                                        public void onSuccess(
                                                                                ArrayList<String> result) {
                                                                            if (result.size() == 0) {
                                                                                System.out
                                                                                        .println("no community servers");
                                                                                if (state != State.kNoFriendsNoCommunity) {
                                                                                    firstrun();
                                                                                    state = State.kNoFriendsNoCommunity;
                                                                                }
                                                                            } else if (state != State.kNoFriends) {
                                                                                nofriends();
                                                                                state = State.kNoFriends;
                                                                            }

                                                                            mNextRPC = System
                                                                                    .currentTimeMillis() + 5 * 1000;
                                                                        }
                                                                    });

                                                } else if (state != State.kFriendsButNotOnline) {

                                                    // System.out.println("friends but not online");

                                                    nofriendsonline();
                                                    mNextRPC = System.currentTimeMillis() + 5 * 1000;
                                                    state = State.kFriendsButNotOnline;
                                                }
                                            }
                                        });
                            } else { // onlineFriends > 0
                                if (state != State.kFriendsOnline) {
                                    state = State.kFriendsOnline;
                                    friendsonline();
                                }
                                mNextRPC = System.currentTimeMillis() + 5 * 1000;
                            }
                        }
                    });
        } // if < RPC threshold
    }

    private void friendsonline() {
        updateStatus(msg.swarm_browser_welcome_friends_online(), false);
    }

    private void nofriends() {
        updateStatus(msg.swarm_browser_welcome_no_friends_online_HTML(), false);
    }

    private void nofriendsonline() {
        updateStatus(msg.swarm_browser_welcome_no_friends_online_HTML(), false);
    }
}
