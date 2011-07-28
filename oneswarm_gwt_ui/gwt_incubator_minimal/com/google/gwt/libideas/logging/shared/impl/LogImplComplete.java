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

import com.google.gwt.libideas.logging.shared.Level;

/**
 * Empty {@link com.google.gwt.libideas.logging.shared.Log} implementation.
 * 
 */
public class LogImplComplete extends LogImplSome {

  public void config(String msg, String category) {
    getReal().config(msg, category);
  }

  public void fine(String msg, String category) {
    getReal().fine(msg, category);
  }

  public void finer(String msg, String category) {
    getReal().finer(msg, category);
  }

  public void finest(String msg, String category) {
    getReal().finest(msg, category);
  }

  public void info(String msg, String category) {
    getReal().info(msg, category);
  }

  public boolean isLoggingMinimal() {
    return false;
  }

  public void log(String msg, Level level, String category, Throwable t) {
    getReal().log(level, msg, category, t);
  }

  public void severe(String msg, String category, Throwable t) {
    getReal().severe(msg, category, t);
  }

  public void warning(String msg, String category, Throwable t) {
    getReal().warning(msg, category, t);
  }

}
