package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SpeedTestPanel.SpeedTestCallback;

class SpeedLimitsPanel extends SettingsPanel {
    TextBox ul_speed_box = new TextBox();
    TextBox dl_speed_box = new TextBox();
    final Label warning = new Label("");
    private static final String WARNING_STYLE = "limit_exceeded_warning";

    public SpeedLimitsPanel() {
        super();

        Grid g = new Grid(2, 4);

        ul_speed_box.setText("...");
        dl_speed_box.setText("...");

        Label l = new Label(msg.settings_net_upload_limit());
        g.setWidget(0, 0, l);
        g.setWidget(0, 1, ul_speed_box);
        Button speedTestBotton = new Button(msg.speed_test_button());
        speedTestBotton.addStyleName(OneSwarmCss.SMALL_BUTTON);
        speedTestBotton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                final OneSwarmDialogBox dlg = new OneSwarmDialogBox(false, true, false);
                dlg.setText(msg.speed_test_window_title());
                dlg.setWidget(new SpeedTestPanel(new SpeedTestCallback() {
                    public void speedTestCompleted(double rate) {
                        if (rate > 1000) {
                            int rateInKb = (int) Math.round(rate / 1000);
                            ul_speed_box.setText(rateInKb + "");
                        }
                        dlg.hide();
                    }

                    public void speedTestCanceled() {
                        dlg.hide();
                    }
                }));
                dlg.center();
                dlg.show();
            }
        });
        g.setWidget(0, 3, speedTestBotton);
        ul_speed_box.setWidth("55px");

        l = new Label(msg.settings_net_download_limit());
        g.setWidget(1, 0, l);
        g.setWidget(1, 1, dl_speed_box);

        dl_speed_box.setWidth("55px");
        g.setWidget(0, 2, new Label(msg.settings_net_zero_unlimited()));
        g.setWidget(1, 2, new Label(msg.settings_net_zero_unlimited()));
        this.add(g);
        this.add(warning);
        this.setWidth("100%");

        OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "Max Download Speed KBs", new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        dl_speed_box.setText(result.toString());
                        if (ul_speed_box.getText().equals("...") == false) {
                            loadNotify();
                        }
                    }
                });

        OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "Max Upload Speed KBs", new AsyncCallback<Integer>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Integer result) {
                        ul_speed_box.setText(result.toString());
                        if (dl_speed_box.getText().equals("...") == false) {
                            loadNotify();
                        }
                    }
                });

        OneSwarmRPCClient.getService().getStopped(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<Boolean>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Boolean result) {
                        if (result) {
                            warning.setText("Transfer Limit Exceeded! Reset your usage limits to reset data rates");
                            warning.setStyleName(WARNING_STYLE);
                            ul_speed_box.setEnabled(false);
                            dl_speed_box.setEnabled(false);
                        } else {
                            warning.setText("");
                            ul_speed_box.setEnabled(true);
                            dl_speed_box.setEnabled(true);
                        }
                    }
                });
    }

    public void sync() {
        System.out.println("attempting to sync ul/dl speeds");

        OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "Max Upload Speed KBs", Integer.parseInt(ul_speed_box.getText()),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("success for UL sync");
                    }
                });

        OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(),
                "Max Download Speed KBs", Integer.parseInt(dl_speed_box.getText()),
                new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                    }

                    public void onSuccess(Void result) {
                        System.out.println("success for DL sync");
                    }
                });
    }

    final String RANGE = msg.settings_net_limit_range_error();

    String validData() {
        try {
            int ul = Integer.parseInt(ul_speed_box.getText());
            int dl = Integer.parseInt(dl_speed_box.getText());

            if (dl < 0 || ul < 0) {
                throw new Exception(RANGE);
            }
        } catch (NumberFormatException e) {
            return RANGE;
        } catch (Exception e) {
            return e.toString();
        }
        return null;
    }
}
