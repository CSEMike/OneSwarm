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

import java.util.Iterator;

/**
 * Root impl used when some logging is instantiated.
 */
public abstract class LogImplSome extends LogImpl {

  public boolean addLogHandler(LogHandler handler) {
    return getReal().addLogHandler(handler);
  }

  public void clearLogHandlers() {
    getReal().clearLogHandlers();
  }

  public Level getDefaultLevel() {
    return getReal().getLevel();
  }

  public boolean isLoggable(Level level) {
    return getReal().isLoggable(level);
  }

  public boolean isLoggingMinimal() {
    return getReal().isLoggingMinimal();
  }

  public boolean isLoggingSupported() {
    return true;
  }

  public Iterator levelIterator() {
    return getReal().levelIterator();
  }

  public void log(String msg, Level level, String category, Throwable e) {
    getReal().log(level, msg, category, e);
  }

  public Level parse(String levelName) {
    return getReal().parse(levelName);
  }

  public void registerLevel(Level level) {
    getReal().registerLevel(level);
  }

  public void removeLogHandler(LogHandler handler) {
    getReal().removeLogHandler(handler);
  }
 
  public void setDefaultLevel(Level level) {
    getReal().setDefaultLevel(level);
  }

  public void setLevel(String category, Level level) {
    getReal().setLevel(category, level);
  }

  public String[] splitCategory(String category) {
    return getReal().splitCategory(category);
  }

  public String toString() {
    return getReal().toString();
  }

  protected AbstractRealLogging getReal() {
    return RealLoggingSome.real;
  }

  
  
}
