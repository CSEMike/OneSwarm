package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceDTO;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;

public class ServiceSharingSettingsPanel extends SettingsPanel {
    // protected final static OSMessages msg = OneSwarmGWT.msg;
    public static final int WIDTH = 400;
    private boolean ready_save = false;

    public boolean isReadyToSave() {
        return ready_save;
    }

    public void loadNotify() {
        ready_save = true;
    }

    public void sync() {
        service.saveClientServices(clientTable.getServices(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(Void result) {
            }
        });

        service.saveSharedServices(sharedTable.getServices(), new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(Void result) {

            }
        });
    }

    public String validData() {
        return ready_save ? null : "services not loaded completely, save not possible";
    }

    OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();

    int loadCount = 0;

    private void partialLoad() {
        loadCount++;
        if (loadCount == 2) {
            loadNotify();
        }
    }

    private final ClientServicesTable clientTable;
    private final SharedServicesTable sharedTable;

    public ServiceSharingSettingsPanel() {
        clientTable = new ClientServicesTable();
        clientTable.setSyncHandler(this);
        add(clientTable);
        setWidth(ServiceSharingSettingsPanel.WIDTH + "px");
        service.getClientServices(new AsyncCallback<ArrayList<ClientServiceDTO>>() {
            @Override
            public void onSuccess(ArrayList<ClientServiceDTO> result) {
                result.add(new ClientServiceDTO(true));
                clientTable.setTableData(result);
                partialLoad();
            }

            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();
            }
        });
        sharedTable = new SharedServicesTable();
        sharedTable.setSyncHandler(this);
        add(sharedTable);
        service.getSharedServices(new AsyncCallback<ArrayList<SharedServiceDTO>>() {
            @Override
            public void onSuccess(ArrayList<SharedServiceDTO> result) {
                result.add(new SharedServiceDTO(true));
                sharedTable.setTableData(result);
                partialLoad();
            }

            @Override
            public void onFailure(Throwable caught) {
                caught.printStackTrace();

            }
        });
    }
}