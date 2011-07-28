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

import com.google.gwt.libideas.logging.server.impl.LogImplBasic;
import com.google.gwt.libideas.logging.server.impl.LogImplJavaLogging;
import com.google.gwt.libideas.logging.shared.Log;
import com.google.gwt.libideas.logging.shared.impl.LogImpl;

/**
 * A convenience class to enable shared code to use logging while on the server.
 * 
 * @deprecated use the com.google.gwt.gen2.logging classes instead
 */
@Deprecated
public class ServerLogManager extends Log {

  /**
   * Make sure that the ServerLogManager is initialized.
   @deprecated use the com.google.gwt.gen2.logging classes instead */ @Deprecated public static void init() {
    if (Log.getLogImpl() == null) {
      useJavaLogging();
    }
  }

  /**
   * A basic implementation of server logging. Here you can add ArrayList and
   * System handlers.
   @deprecated use the com.google.gwt.gen2.logging classes instead */ @Deprecated public static void useBasicLogging() {
    Log.initLogImpl(new LogImplBasic());
  }

  /**
   * Adaptor to use java logging. Each category is mapped to a logger. All such
   * loggers are children of the gwt logger.
   @deprecated use the com.google.gwt.gen2.logging classes instead */ @Deprecated public static void useJavaLogging() {
    Log.initLogImpl(new LogImplJavaLogging());
  }

  /**
   * For non built-in logging libraries, add the appropriate LogImpl class here.
   * For example, here is where a Log4J {@link LogImpl} adaptor would go.
   */
  public static void useLibrary(LogImpl libraryAdaptor) {
    Log.initLogImpl(libraryAdaptor);
  }
}
