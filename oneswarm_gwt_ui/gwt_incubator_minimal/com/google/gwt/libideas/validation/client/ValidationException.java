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
 * Validation Exception. Used in helper methods to alert the current
 * <code>Validator</code> to report a user error.
 * 
 */
public class ValidationException extends Exception {

  private String errorMessage;

  /**
   * Constructor for <code>ValidationException</code>
   * 
   * @param errorMessage the error message for this exception
   */
  public ValidationException(String errorMessage) {
    setErrorMessage(errorMessage);
  }

  /**
   * Gets the error message for this exception.
   * 
   * @return the error message for this exception
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Sets the error message for this Exception.
   * 
   * @param errorMessage the error message to set
   */
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
