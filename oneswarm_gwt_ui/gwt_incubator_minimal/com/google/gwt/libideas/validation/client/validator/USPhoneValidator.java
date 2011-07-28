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

package com.google.gwt.libideas.validation.client.validator;

import com.google.gwt.libideas.validation.client.ErrorHandler;
import com.google.gwt.libideas.validation.client.Subject;

/**
 * United States Phone validator. Modifies the output if its "close enough";
 */
public class USPhoneValidator extends RegExValidator {
  static char[] PHONE_FILLERS = {'(', ')', '-', ' '};
  static char[] NUMBERS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

  private static boolean inArray(char a, char[] array) {
    for (int i = 0; i < array.length; i++) {
      if (a == array[i]) {
        return true;
      }
    }
    return false;
  }

  public USPhoneValidator() {
    super("[(][0-9]{3}[)] [0-9]{3}-[0-9]{4}");
  }

  public void checkValid(Subject subject, ErrorHandler handler) {
    String s = subject.getValue().toString();
    if (!s.matches(regEx)) {
      if (!graunchable(s, subject)) {
        handler.reportError(subject, createErrorMessage(subject.getLabel(), s));
      }
    }
  }

  public String createErrorMessage(String fieldName, String answer) {
    return getMessages().phone(fieldName, answer);
  }

  /**
   * See if the phone number can be graunched into shape.
   */
  private boolean graunchable(String s, Subject subject) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      char cur = s.charAt(i);
      if (i == 0 && cur == '1') {
        continue;
      } else if (inArray(cur, PHONE_FILLERS)) {
        continue;
      } else if (inArray(cur, NUMBERS)) {
        buf.append(cur);
      } else {
        return false;
      }
    }
    if (buf.length() != 10) {
      return false;
    } else {
      buf.insert(0, "(");
      buf.insert(4, ") ");
      buf.insert(9, "-");
      subject.setValue(buf.toString());
      return true;
    }
  }
}
