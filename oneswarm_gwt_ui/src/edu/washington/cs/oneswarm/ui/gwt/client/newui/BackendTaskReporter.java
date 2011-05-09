package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;

public class BackendTaskReporter extends HorizontalPanel implements Updateable {
    Label shortStatus = new Label("...");

    public static final String CSS_BACKEND_TASK = "os-backend_task";

    public BackendTaskReporter() {
        super();

        shortStatus.addStyleName(CSS_BACKEND_TASK);

        Image spin = new Image(ImageConstants.PROGRESS_SPINNER);
        spin.setWidth("16px");
        spin.setHeight("16px");
        add(spin);
        add(shortStatus);

        this.setSpacing(3);

        this.setCellVerticalAlignment(spin, VerticalPanel.ALIGN_MIDDLE);
        this.setCellVerticalAlignment(shortStatus, VerticalPanel.ALIGN_MIDDLE);

        shortStatus.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                BackendTask[] tasks = mLatestTasks;
                if (tasks.length > 0) {
                    BackendTasksDialog dlg = new BackendTasksDialog(tasks);
                    dlg.show();
                    dlg.setVisible(false);
                    dlg.center();
                    dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
                    dlg.setVisible(true);
                }
            }
        });

        hide();

        OneSwarmGWT.addToUpdateTask(this);
    }

    public void onDetach() {
        super.onDetach();

        OneSwarmGWT.removeFromUpdateTask(this);
    }

    BackendTask[] mLatestTasks = null;

    private boolean mVisible = false;

    public void update(int count) {
        /**
         * fewer updonates when we're just checking for status (rather than
         * updating it)
         */
        if (mVisible == false && (count % 2) != 0)
            return;

        // OneSwarmGWT.log("Queuing RPC");
        OneSwarmRPCClient.getService().getBackendTasks(OneSwarmRPCClient.getSessionID(),
                new AsyncCallback<BackendTask[]>() {
                    public void onFailure(Throwable caught) {
                        caught.printStackTrace();
                        OneSwarmGWT.log("Error on BackendTasks RPC: " + caught.toString());
                    }

                    public void onSuccess(BackendTask[] result) {
                        // OneSwarmGWT.log("Backend tasks: " + result.length);
                        mLatestTasks = result;
                        refreshUI();
                    }
                });
    }

    private void hide() {
        mVisible = false;
        DOM.setStyleAttribute(this.getElement(), "visibility", "hidden");
    }

    private void show() {
        mVisible = true;
        DOM.setStyleAttribute(this.getElement(), "visibility", "visible");
    }

    private void refreshUI() {
        if (mLatestTasks.length == 0) {
            hide();
        } else if (mLatestTasks.length == 1) {
            show();

            shortStatus.setText(mLatestTasks[0].getShortname() + " "
                    + mLatestTasks[0].getProgress());
        } else // > 1
        {
            show();
            shortStatus.setText(mLatestTasks.length + " tasks...");
        }
    }
}
