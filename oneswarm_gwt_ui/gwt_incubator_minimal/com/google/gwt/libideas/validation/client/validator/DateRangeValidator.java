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
import com.google.gwt.libideas.validation.client.ValidationException;

import java.util.Date;

/**
 * Validator to check the range of a date.
 */
public class DateRangeValidator extends BuiltInValidator {
  private Date max;
  private Date min;

  /**
   * Default Constructor used for serialization.
   */
  public DateRangeValidator() {
  }

  /**
   * Adds a min/max that the DataQuestion must conform to.
   * 
   * @param min the earliest valid date
   * @param max the latest valid date
   */
  public DateRangeValidator(Date min, Date max) {
    this.min = min;
    this.max = max;
  }

  /**
   * Check whether the Date is within the correct range.
   */
  public void checkValid(Subject e, ErrorHandler handler) {
    try {
      Date t = getDate(e);
      if (t != null) {
        if (min != null && t.before(min)) {
          throw new ValidationException(getMessages().lessThan(e.getLabel(), t,
              min));
        } else if (max != null && t.after(max)) {
          throw new ValidationException(getMessages().greaterThan(e.getLabel(),
              t, max));
        }
      }
    } catch (ValidationException validationException) {
      handler.reportError(e, validationException);
    }
  }
}
