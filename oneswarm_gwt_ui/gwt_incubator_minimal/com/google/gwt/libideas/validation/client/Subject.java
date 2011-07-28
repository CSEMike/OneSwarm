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
 * A <code>Subject</code> is, logically enough, an object that can be the
 * subject of a validation. In order to provide the least constraint on the
 * parent data-model, <code> Subject </code> is represented by a simple
 * interface. For example, is a user wishes to use validations on their personal
 * <code>TextField</code> object, the <code>TextField</code> object would
 * implement <code>Subject</code>.
 * 
 * 
 */
public interface Subject {
  /**
   * Returns whether the subject already has an error.
   * 
   * @return is the subject in an error state
   */
  boolean getError();

  /**
   * Gets the current label.
   * 
   * @return the current label
   */
  String getLabel();

  /**
   * Gets the current value.
   * 
   * @return the current value
   */
  Object getValue();

  /**
   * Sets a user error condition for the subject.
   * 
   * @param hasError is the subject in an error state
   */
  void setError(boolean hasError);

  /**
   * Sets the current value.
   */
  void setValue(Object value);
}
