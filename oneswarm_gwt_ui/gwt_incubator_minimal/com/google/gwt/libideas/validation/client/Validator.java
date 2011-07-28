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

/**
 * A <code>Validator</code> acts upon <code>Subjects</code> For example, the
 * user might extend <code>Validator</code> to create a
 * <code>LastNameValidator</code>, which checks if the subject object's
 * answer is a legitimate last name.
 * 
 */
public abstract class Validator {

  /**
   * Checks to see if the subject is valid.
   * 
   * @param subject the item to check
   */
  public final void checkValid(Subject subject) {
    checkValid(subject, ValidatorController.getDefaultErrorHandler());
  }

  /**
   * Checks to see if the subject is valid, using a specified error handler.
   * 
   * @param subject the item to check
   * @param handler the <code>ErrorHandler</code> to use in the event of an error
   */
  public abstract void checkValid(Subject element, ErrorHandler handler);
}