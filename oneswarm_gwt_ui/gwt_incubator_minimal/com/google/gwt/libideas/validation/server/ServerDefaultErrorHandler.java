/*
 * Copyright 2006 Google Inc.
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

package com.google.gwt.libideas.validation.server;

import com.google.gwt.libideas.validation.client.ErrorHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Default Server side error handler. As we do not impose a logging system upon
 * the user, the error handler keeps tracks of messages and dumps them to the
 * user when requested.
 */

public class ServerDefaultErrorHandler extends ErrorHandler {
  PrintWriter writer;
  StringWriter accum;

  public ServerDefaultErrorHandler() {
    accum = new StringWriter();
    writer = new PrintWriter(accum);
  }

  public StringBuffer dumpResults() {
    StringBuffer results = accum.getBuffer();
    accum = new StringWriter();
    writer = new PrintWriter(accum);
    return results;
  }

  public void reportError(String s) {
    writer.println(s);
  }

  public String toString() {
    return "ServerDefaultErrorHandler";
  }
}
