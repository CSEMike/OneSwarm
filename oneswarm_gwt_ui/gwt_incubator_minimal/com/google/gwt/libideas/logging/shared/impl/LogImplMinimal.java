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

/**
 * Minimal logging. Often used in a deployed system. Only non intrusive handlers
 * should be added when the logging system is in minimal mode. Also, only severe
 * error messages are included.
 * 
 */
public class LogImplMinimal extends LogImplSome {

  public void config(String msg, String category) {
  }

  public void fine(String msg, String category) {
  }

  public void finer(String msg, String category) {
  }

  public void finest(String msg, String category) {
  }

  public void info(String msg, String category) {
  }

  public boolean isLoggingMinimal() {
    return true;
  }

  public void severe(String msg, String category, Throwable t) {
    getReal().severe(msg, category, t);
  }

  public void warning(String msg, String category, Throwable t) {
  }

}
