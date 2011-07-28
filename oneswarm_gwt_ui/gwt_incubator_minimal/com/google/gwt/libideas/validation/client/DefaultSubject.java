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
 * Default version of subject which stores a label and value for the subject.
 */
public class DefaultSubject implements Subject {
  String label;
  Object value;
  boolean hasError;

  /**
   * Constructor for <code>DefaultSubject</code>.
   * 
   * @param label
   * @param answer
   */
  public DefaultSubject(String label, Object answer) {
    this.label = label;
    this.value = answer;
  }

  public boolean getError() {
    return hasError;
  }

  public String getLabel() {
    return label;
  }

  public Object getValue() {
    return value;
  }

  public void setError(boolean hasError) {
    this.hasError = hasError;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public String toString() {
    return this.label + ":" + this.value;
  }
}
