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
 * Base class for binary relationships between subjects. Examples are less than,
 * greater than, etc.
 */
public abstract class DependentSubjectValidator extends BuiltInValidator {
  private Subject dependent;

  /**
   * Constructor for <code>DependentSubjectValidator</code>.
   * 
   * @param dependent dependent subject
   */
  public DependentSubjectValidator(Subject dependent) {
    this.dependent = dependent;
  }

  public void checkValid(Subject target, ErrorHandler handler) {
    checkValid(dependent, target, handler);
  }

  /**
   * Handles the validation error.
   */
  public abstract String handleError(String dependentLabel, Subject target);

  /**
   * Is the relationship maintained?
   */
  public abstract boolean isValid(Object dependentValue, Object targetValue);

  protected void checkValid(Subject dependent, Subject target,
      ErrorHandler handler) {

    Object dependentValue = dependent.getValue();
    Object targetValue = target.getValue();

    if (!(dependentValue == null || targetValue == null || isValid(
        dependentValue, targetValue))) {
      String dependentLabel = "'" + dependent.getLabel() + "'";
      String failedDependency = handleError(dependentLabel, target);
      handler.reportError(target, failedDependency);
    }
  }
}
