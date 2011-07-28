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
 * <code>ErrorHandler</code> processes error messages and exceptions derived
 * from <code>ValidationException</code>. It is always called directly from
 * <code>Validator</code> subclasses. A default <code>ErrorHandler</code> is
 * supplied by the system if the user does not create one.
 * 
 */
public abstract class ErrorHandler {
  /**
   * Reports that a subject threw a <code>ValidationException</code> during validation.
   *
   * @param subject the subject with the error
   * @param exception the <code>Exception</code> thrown.
   */
  public void reportError(Subject subject, ValidationException exception) {
    reportError(subject, exception.getErrorMessage());
  }

  /**
   * Reports that a subject reported an error during validation.
   *
   * @param subject the subject with the error
   * @param errorMessage the error reported
   */
  public void reportError(Subject subject, String errorMessage) {
    reportError(errorMessage);
    subject.setError(true);
  }

  /**
   * Reports errors.
   */
  public abstract void reportError(String errorMessage);
}
