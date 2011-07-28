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

package com.google.gwt.libideas.logging.server;

import com.google.gwt.libideas.logging.client.RemoteLoggingService;
import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.libideas.logging.shared.Log;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * Remote logging implementation.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class RemoteLoggingServiceImpl extends RemoteServiceServlet implements
    RemoteLoggingService {
  {
    try {
      ServerLogManager.init();
    } catch (Throwable e) {
      System.err.println(e);
    }
  }

  public final void publish(String message, Level level, String category,
      Throwable ex) {
    try {
      Log.log(message, level, category, ex);
    } catch (RuntimeException e) {
      System.err.println("Failed to log message due to " + e.toString());
      e.printStackTrace();
    }
  }
}
