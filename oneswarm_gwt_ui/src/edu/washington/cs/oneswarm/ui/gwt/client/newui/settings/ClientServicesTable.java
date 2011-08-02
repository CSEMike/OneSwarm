package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceDTO;

public class ClientServicesTable extends ServicesTable<ClientServiceDTO> {

    public ClientServicesTable() {
        setWidth("100%");
        Label label = new Label("Service Subscriptions:");
        label.addStyleName("os-service-share-caption");
        add(label);
        serviceTable = new CellTable<ClientServiceDTO>();
        serviceTable.setWidth("100%");

        addNameColumn();

        addSearchKeyColumn();

        addPortColumn();

        addDeleteColumn();

        add(serviceTable);
    }

    @Override
    protected ClientServiceDTO getNewEntry() {
        return new ClientServiceDTO(true);
    }

}
