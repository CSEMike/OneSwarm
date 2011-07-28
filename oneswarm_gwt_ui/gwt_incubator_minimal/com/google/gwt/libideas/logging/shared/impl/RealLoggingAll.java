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

package com.google.gwt.libideas.logging.shared.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.libideas.logging.client.ConsoleLogHandler;
import com.google.gwt.libideas.logging.client.FireBugLogHandler;
import com.google.gwt.libideas.logging.client.GWTLogHandler;
import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.libideas.logging.shared.Log;
import com.google.gwt.user.client.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * Real logging with runtime level support.
 * 
 */
public class RealLoggingAll extends AbstractRealLogging {

  /**
   * Copy of Windows.Location from 1.5.
   */
  public static class Location {
    private static Map paramMap;

    /**
     * Gets the URL's parameter of the specified name.
     * 
     * @param name the name of the URL's parameter
     * @return the value of the URL's parameter
     */
    public static String getParameter(String name) {
      return (String) ensureParameterMap().get(name);
    }

    /**
     * Gets the URL's query string.
     * 
     * @return the URL's query string
     */
    public static native String getQueryString() /*-{
      return $wnd.location.search;
    }-*/;

    private static Map ensureParameterMap() {
      if (paramMap == null) {
        paramMap = new HashMap();
        String queryString = getQueryString();
        if (queryString != null && queryString.length() > 1) {
          String qs = queryString.substring(1);
          String[] kvPair = qs.split("&");
          for (int i = 0; i < kvPair.length; i++) {
            String[] kv = kvPair[i].split("=");
            if (kv.length > 1) {
              paramMap.put(kv[0], URL.decode(kv[1]));
            } else {
              paramMap.put(kv[0], "");
            }
          }
        }
      }
      return paramMap;
    }

    private Location() {
    }
  }

  public static AbstractRealLogging real = new RealLoggingAll();

  static {
    readLevel();
    if (GWT.isScript()) {
      FireBugLogHandler f = new FireBugLogHandler();
      if (f.isSupported()) {
        Log.addLogHandler(f);
      } else {
        ConsoleLogHandler handler = new ConsoleLogHandler();
        if (handler.isSupported()) {
          Log.addLogHandler(handler);
        } else {
          // TODO(ecc) create/import popup window manager.
        }
      }
 
    } else {
      Log.addLogHandler(new GWTLogHandler());
    }
  }

  private static void readLevel() {
    String param = Location.getParameter("logLevel");
    if (param != null) {
      try {
        Level l = Log.parseLevel(param);
        real.setDefaultLevel(l);
      } catch (IllegalArgumentException argument) {
        Window.alert(param
            + " is an illegal arguement for debugLevel. We are ignoring it, use 'SEVERE', 'WARNING', 'CONFIG', 'FINE',etc instead.");
      }
    }
  }

  RealLoggingAll() {
    real = this;
    initializeLevels();
  }

}
