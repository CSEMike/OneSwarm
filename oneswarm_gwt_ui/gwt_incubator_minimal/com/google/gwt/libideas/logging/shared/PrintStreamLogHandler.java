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

package com.google.gwt.libideas.logging.shared;

import com.google.gwt.core.client.GWT;

import java.io.PrintStream;

/**
 * Handler to print out to the error console. Only works in hosted mode.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class PrintStreamLogHandler extends LogHandler {

  PrintStream stream;

  public PrintStreamLogHandler(PrintStream stream) {
    this.stream = stream;
  }

  public boolean isSupported() {
    return !GWT.isScript();
  }

  public void publish(String string, Level level, String category, Throwable e) {
    stream.println(format(string, level, category, e));
  }
}
