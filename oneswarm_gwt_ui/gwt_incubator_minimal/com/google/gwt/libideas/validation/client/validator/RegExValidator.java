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
 * Regular expression validator.
 */
public abstract class RegExValidator extends BuiltInValidator {

  /**
   * Social Security Validator.
   */
  public static final RegExValidator SSN = new RegExValidator(
      "[0-9]{3}-[0-9]{2}-[0-9]{4}") {
    public String createErrorMessage(String fieldName, String answer) {
      return getMessages().ssn(fieldName, answer);
    }
  };

  protected String regEx;

  /**
   * Constructor for <code>RegExValidator</code>.
   * 
   * @param regEx Regular Expression string.  Note that the string must be a 
   * valid Javascript regular expression.
   */
  public RegExValidator(String regEx) {
    this.regEx = regEx;
    // Check for bad regular expressions.
    "".matches(regEx);
  }

  public void checkValid(Subject element, ErrorHandler handler) {
    String s = element.getValue().toString().trim();
    if ((s.length() > 0) && !s.matches(regEx)) {
      handler.reportError(element, createErrorMessage(element.getLabel(), s));
    }
  }

  /**
   * Synthesizes the error message associated with this validator.
   * 
   * @param subjectLabel subject's label
   * @param answer the wrong answer supplied by the user
   * @return the error message
   */
  public abstract String createErrorMessage(String subjectLabel, String answer);

}
