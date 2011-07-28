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

import com.google.gwt.libideas.validation.client.validator.BuiltInValidatorMessages;

/**
 * Server side implementation of <code>BuiltInValidatorMessages</code>.
 */
public class ServerBuiltInValidatorMessages implements BuiltInValidatorMessages {
  public String badFormat(String arg0, String arg1, String format) {
    return "" + arg0 + " cannot be set to  " + arg1
        + ", must be formatted like '" + format + "'";
  }

  public String greaterThan(String arg0, Object arg1, Object arg2) {
    return "" + arg0 + ": " + arg1 + " must be less than " + arg2 + "";
  }

  public String lessThan(String arg0, Object arg1, Object arg2) {
    return "" + arg0 + ": " + arg1 + " must be greater than " + arg2 + " ";
  }

  public String phone(String arg0, String arg1) {
    return "" + arg0 + ": " + arg1 + " is a bad phone number.";
  }

  public String ssn(String arg0, String arg1) {
    return "" + arg0 + ": " + arg1 + " is a bad social security number.";
  }

}
