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

package com.google.gwt.libideas.validation.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class to associate validators with subjects and error handlers.
 * 
 */
public class AbstractValidatorController {

  /**
   * Default error handler.
   * 
   */

  protected static ErrorHandler defaultErrorHandler;

  /**
   * Gets the default error handler.
   * 
   * @return the default error handler.
   */
  public static ErrorHandler getDefaultErrorHandler() {
    if (defaultErrorHandler == null) {
      throw new NullPointerException("No default error handler is installed.");
    }
    return defaultErrorHandler;
  }

  /**
   * Sets the default error handler.
   */
  protected static void setDefaultErrorHandler(ErrorHandler errorHandler) {
    AbstractValidatorController.defaultErrorHandler = errorHandler;
  }

  private ErrorHandler errorHandler = defaultErrorHandler;
  private List children = null;
  private List subjects = new ArrayList();
  private List validators = new ArrayList();

  /**
   * Adds a child <code>ValidatorController</code>.
   */
  public void addChild(ValidatorController validation) {
    if (children == null) {
      children = new ArrayList();
    }
    children.add(validation);
  }

  /**
   * Adds a subject.
   */
  public void addSubject(Subject validatable) {
    subjects.add(validatable);
  }

  /**
   * Adds a validator.
   */
  public void addValidator(Validator validator) {
    validators.add(validator);
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public void setErrorHandler(ErrorHandler handler) {
    this.errorHandler = handler;
  }

  /**
   * Validates all subjects using all listed validators. Then delegates call to
   * validate to children.
   */
  public final void validate() {
    for (int i = 0; i < validators.size(); i++) {
      Validator validator = (Validator) validators.get(i);
      Iterator iter = subjects.iterator();
      while (iter.hasNext()) {
        Subject subject = (Subject) iter.next();
        if (subject.getValue() != null) {
          validator.checkValid(subject, getErrorHandler());
        }
      }
    }
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        ValidatorController v = (ValidatorController) children.get(i);
        v.validate();
      }
    }
  }
}
