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

import com.google.gwt.libideas.validation.client.AbstractValidatorController;
import com.google.gwt.libideas.validation.client.validator.BuiltInValidator;

/**
 * Additional methods/functionality available on the server for the validation
 * system.
 */
public class ServerValidatorController extends AbstractValidatorController {
  static {
    setupOnServer();
  }

  /**
   * Method to call to force cinit without creating new instance.
   */
  public static void init() {
  }

  private static void setupOnServer() {
    setDefaultErrorHandler(new ServerDefaultErrorHandler());
    BuiltInValidator.setMessages(new ServerBuiltInValidatorMessages());
  }

}
