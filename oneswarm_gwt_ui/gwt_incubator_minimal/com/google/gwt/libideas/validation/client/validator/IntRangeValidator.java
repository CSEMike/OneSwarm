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
 * Validate that an IntegerField is in a certain range.
 */
public class IntRangeValidator extends BuiltInValidator {
  private int max;
  private int min;

  /**
   * Default Constructor used for serialization.
   */
  public IntRangeValidator() {
  }

  /**
   * Constructor for <code>IntRangeValidator</code>.
   * 
   * @param min
   * @param max
   */
  public IntRangeValidator(int min, int max) {
    this.max = max;
    this.min = min;
  }

  /**
   * Ensure the subject's value falls between min and max.
   */
  public void checkValid(Subject trigger, ErrorHandler handler) {
    Integer value = (Integer) trigger.getValue();
    int val = value.intValue();
    if (val < min) {
      handler.reportError(trigger, getMessages().lessThan(trigger.getLabel(),
          value, min + ""));
    } else if (val > max) {
      handler.reportError(trigger, getMessages().greaterThan(
          trigger.getLabel(), value, max + ""));
    }
  }
}
