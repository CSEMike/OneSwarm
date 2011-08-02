package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;

class SharedServicesTable extends ServicesTable<SharedServiceDTO> {

    public SharedServicesTable() {
        setWidth("100%");
        Label label = new Label("Shared Services:");
        label.addStyleName("os-service-share-caption");
        add(label);
        serviceTable = new CellTable<SharedServiceDTO>();
        serviceTable.setWidth("100%");

        addNameColumn();

        addSearchKeyColumn();

        addAddressColumn();

        addPortColumn();

        addDeleteColumn();

        add(serviceTable);
    }

    protected void addAddressColumn() {
        EditColumn<SharedServiceDTO> addressColumn = new EditColumn<SharedServiceDTO>(false) {
            @Override
            public String getValue(SharedServiceDTO service) {
                SharedServiceDTO sservice = (SharedServiceDTO) service;
                return sservice.address;
            }

            @Override
            public void update(int index, SharedServiceDTO service, String value) {
                SharedServiceDTO sservice = (SharedServiceDTO) service;
                sservice.address = value;
                serviceTable.redraw();
            }
        };
        serviceTable.addColumn(addressColumn, "Address");
    }

    @Override
    protected SharedServiceDTO getNewEntry() {
        return new SharedServiceDTO(true);
    }
}