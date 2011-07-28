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

package com.google.gwt.libideas.logging.server.impl;

import com.google.gwt.libideas.logging.shared.LogHandler;
import com.google.gwt.libideas.logging.shared.Level;
import com.google.gwt.libideas.logging.shared.impl.LogImplComplete;

import java.util.logging.Logger;

/**
 * {@link LogImpl} class used to support Java Logging.
 * 
 */
public class LogImplJavaLogging extends LogImplComplete {

  Logger gwt = Logger.getLogger("gwt");

  public boolean addLogHandler(LogHandler handler) {
    throw new UnsupportedOperationException(
        "Cannot add GWT Handlers to java logging, as the reason you are using this class is to use the java logging handlers");
  }

  public Level convertToGWTLevel(java.util.logging.Level level) {
    // TODO this information should be cashed
    return new Level(level.getName(), level.intValue());
  }

  public java.util.logging.Level convertToLoggingLevel(Level level) {
    return java.util.logging.Level.parse(level.getName());
  }

  public Level getDefaultLevel() {
    return convertToGWTLevel(gwt.getLevel());
  }

  public boolean isLoggingSupported() {
    return true;
  }

  public void log(Level level, String msg, String category) {
    Logger logger;
    if (category == null) {
      logger = gwt;
    } else {
      logger = Logger.getLogger("gwt." + category);
    }

    logger.log(convertToLoggingLevel(level), msg);
  }

  public void setDefaultLevel(Level level) {
    gwt.setLevel(convertToLoggingLevel(level));
  }

}
