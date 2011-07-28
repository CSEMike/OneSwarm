/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.libideas.logging.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.libideas.logging.shared.LogHandler;
import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.libideas.logging.shared.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Handler to publish messages over RPC.
 * <p>
 * Note:
 * </p>
 * While this class ended up sharing no code with gwt-logs remote service API
 * due to naming/functionality differences, its design is modeled using the same
 * pattern as the gwt-logs <code>RemoteLogger</code>.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class RemoteLogHandler extends LogHandler {
  class DefaultCallback implements AsyncCallback {

    public void onFailure(Throwable caught) {
      Log.removeLogHandler(RemoteLogHandler.this);
      Log.severe(
          "Remote logging failed,  remote handler is now removed as a valid handler",
          CATEGORY, caught);
    }

    public void onSuccess(Object result) {
      Log.finest("Remote logging message acknowledged", CATEGORY);
    }
  }

  private static final String CATEGORY = "gwt.logging.RemoteLoggingHandler";

  private RemoteLoggingServiceAsync service;

  private AsyncCallback callback;

  public RemoteLogHandler() {
    this((RemoteLoggingServiceAsync) GWT.create(RemoteLoggingService.class));
  }

  public RemoteLogHandler(RemoteLoggingServiceAsync service) {
    ServiceDefTarget target = (ServiceDefTarget) service;
    target.setServiceEntryPoint(GWT.getModuleBaseURL() + "logging");
    this.service = service;

    this.callback = new DefaultCallback();
  }

  public void publish(String message, Level level, String category, Throwable e) {
    // Don't log messages about myself.
    if (category == CATEGORY) {
      return;
    }
    service.publish(message, level, category, e, callback);
  }

  public void setCallBack(AsyncCallback callback) {
    this.callback = callback;
  }
}
