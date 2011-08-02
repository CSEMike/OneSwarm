package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.rpc.ServiceDTO;

abstract class ServicesTable<T extends ServiceDTO> extends VerticalPanel {
    protected ArrayList<T> services;
    protected CellTable<T> serviceTable;
    private SettingsPanel syncHandler;

    public ArrayList<T> getServices() {
        return services;
    }

    public void setSyncHandler(SettingsPanel syncHandler) {
        this.syncHandler = syncHandler;
    }

    protected void addDeleteColumn() {
        Column<T, String> deleteColumn = new Column<T, String>(new ButtonCell()) {
            @Override
            public String getValue(T service) {
                if (service.isDummy()) {
                    return "add";
                }
                return "delete";
            }
        };
        deleteColumn.setFieldUpdater(new FieldUpdater<T, String>() {
            @Override
            public void update(int index, T service, String value) {
                if (service.isDummy()) {
                    try {
                        service.validate();
                        service.setDummy(false);
                        services.add(getNewEntry());
                        serviceTable.setRowData(services);
                        serviceTable.redraw();
                        syncHandler.sync();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        Window.alert("Please fill out the table row completely before adding.\n\nGot error: "
                                + e.getMessage());
                    }
                } else {
                    if (Window.confirm("Are you sure you want to remove service: "
                            + service.getName())) {
                        services.remove(index);
                        serviceTable.setRowData(services);
                        serviceTable.redraw();
                        syncHandler.sync();
                    }
                }
            }
        });
        serviceTable.addColumn(deleteColumn);
    }

    protected abstract T getNewEntry();

    public void setTableData(ArrayList<T> services) {
        this.services = services;
        serviceTable.setRowData(services);
    }

    protected void addPortColumn() {
        EditColumn<T> portColumn = new EditColumn<T>(true) {
            @Override
            public String getValue(T service) {
                if (service.getPort() == 0) {
                    return "";
                }

                return service.getPort() + "";
            }

            @Override
            public void update(final int index, final T service, String value) {
                service.setPort(value);
                serviceTable.redraw();
            }
        };
        serviceTable.addColumn(portColumn, "Local port");
    }

    protected void addSearchKeyColumn() {
        EditColumn<T> searchKeyColumn = new EditColumn<T>(false) {
            @Override
            public String getValue(T service) {
                return service.getSearchKey();
            }

            @Override
            public void update(int index, T service, String value) {
                service.setSearchKey(value);
                serviceTable.redraw();
            }
        };
        serviceTable.addColumn(searchKeyColumn, "Search Key");
    }

    protected void addNameColumn() {
        EditColumn<T> nameColumn = new EditColumn<T>(false) {
            @Override
            public String getValue(T service) {
                return service.getName();
            }

            @Override
            public void update(int index, T service, String value) {
                service.setName(value);
                serviceTable.redraw();
            }
        };
        serviceTable.addColumn(nameColumn, "Name");
    }
}