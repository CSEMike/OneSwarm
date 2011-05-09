package edu.washington.cs.oneswarm.ui.gwt.client.newui.creation;

import java.util.ArrayList;

import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;

public interface ApriorPermissionsCallback {
    public void permissionsDefined(ArrayList<PermissionsGroup> permitted_groups);

    public void cancelled();
}
