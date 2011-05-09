package edu.washington.cs.oneswarm.ui.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIService;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;

public class OneSwarmRPCClient {

    private static String sessionID;

    public static void setSessionID(String _sessionID) {
        sessionID = _sessionID;
    }

    public static String getSessionID() {
        return sessionID;
    }

    public static OneSwarmUIServiceAsync getService() {

        OneSwarmUIServiceAsync service = (OneSwarmUIServiceAsync) GWT
                .create(OneSwarmUIService.class);
        ServiceDefTarget endpoint = (ServiceDefTarget) service;
        String moduleRelativeURL = GWT.getModuleBaseURL() + "OneSwarmGWT";

        endpoint.setServiceEntryPoint(moduleRelativeURL);
        return service;
    }

}
