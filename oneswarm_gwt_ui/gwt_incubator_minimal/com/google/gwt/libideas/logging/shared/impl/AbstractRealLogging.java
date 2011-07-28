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

import com.google.gwt.libideas.logging.shared.LogHandler;
import com.google.gwt.libideas.logging.shared.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Abstract base class for logging.
 */
public abstract class AbstractRealLogging {

  /**
   * Registered levels.
   */
  HashMap levels = new HashMap();
  HashMap categoryLevels = null;
  private ArrayList handlers = new ArrayList();
  private Level globalLevel;

  public boolean addLogHandler(LogHandler handler) {
    if (handler.isSupported()) {
      handlers.add(handler);
      return true;
    } else {
      return false;
    }
  }

  public void clearLogHandlers() {
    handlers.clear();
  }

  public void config(String msg, String category) {
    log(Level.CONFIG, msg, category);
  }

  public void fine(String msg, String category) {
    log(Level.FINE, msg, category);
  }

  public void finer(String msg, String category) {
    log(Level.FINER, msg, category);
  }

  public void finest(String msg, String category) {
    log(Level.FINEST, msg, category);
  }

  public Level getLevel() {
    if (globalLevel == null) {
      globalLevel = Level.CONFIG;
    }
    return globalLevel;
  }

  public void info(String msg, String category) {
    log(Level.INFO, msg, category);
  }

  public boolean isLoggable(Level level) {
    return allows(getLevel(), level);
  }

  public boolean isLoggingMinimal() {
    return false;
  }

  public boolean isLoggingSupported() {
    return true;
  }

  public Iterator levelIterator() {
    ArrayList accum = new ArrayList();
    accum.addAll(levels.values());
    Collections.sort(accum);
    return accum.iterator();
  }

  public void log(Level level, String msg, String category, Throwable e) {
    if (isLoggable(level, category)) {
      for (int i = 0; i < handlers.size(); i++) {
        LogHandler handler = (LogHandler) handlers.get(i);
        if (allows(handler.getLevel(), level)) {
          handler.publish(msg, level, category, e);
        }
      }
    }
  }

  public Level parse(String levelName) {
    Level value = null;
    if (levels != null) {
      value = (Level) levels.get(levelName);
    }
    if (value == null) {
      throw new IllegalArgumentException(levelName + " is not a known Level");
    }
    return value;
  }

  public void registerLevel(Level level) {
    levels.put(level.getName(), level);
  }

  public void removeLogHandler(LogHandler handler) {
    handlers.remove(handler);
    handler.hideHandler();
  }

  public void setDefaultLevel(Level level) {
    if (level == null) {
      throw new IllegalArgumentException("Cannot set global level to null");
    }
    globalLevel = level;
  }

  public void setLevel(String category, Level level) {
    if (categoryLevels == null) {
      categoryLevels = new HashMap();
    }
    categoryLevels.put(category, level);
  }

  public void severe(String msg, String category, Throwable e) {
    log(Level.SEVERE, msg, category, e);
  }

  /**
   * Splits the category into its component pieces with "." as the separator.
   */
  public String[] splitCategory(String category) {
    if (category == null) {
      return new String[0];
    } else {
      return category.split("[.]");
    }
  }

  public void warning(String msg, String category, Throwable t) {
    log(Level.WARNING, msg, category, t);
  }

  protected boolean allows(Level parent, Level child) {
    return parent.intValue() <= child.intValue();
  }

  protected Level getLevelForCategory(String category) {
    Level obj = (Level) categoryLevels.get(category);
    if (obj != null) {
      return obj;
    }
    int index = category.lastIndexOf(".");
    if (index == -1) {
      return globalLevel;
    }
    return getLevelForCategory(category.substring(0, index));
  }

  protected void initializeLevels() {
    Level.OFF = new Level("OFF", Integer.MAX_VALUE, true);
    Level.SEVERE = new Level("SEVERE", 1000);
    Level.WARNING = new Level("WARNING", 900);
    Level.INFO = new Level("INFO", 800);
    Level.CONFIG = new Level("CONFIG", 700);
    Level.FINE = new Level("FINE", 500);
    Level.FINER = new Level("FINER", 400);
    Level.FINEST = new Level("FINEST", 300);
    Level.ALL = new Level("ALL", Integer.MIN_VALUE, true);
  }

  protected boolean isLoggable(Level level, String category) {
    if (category == null || categoryLevels == null) {
      return isLoggable(level);
    }
    return allows(getLevelForCategory(category), level);
  }

  private void log(Level level, String message, String category) {
    log(level, message, category, null);
  }

}
