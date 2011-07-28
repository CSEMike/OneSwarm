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

import com.google.gwt.libideas.validation.client.Subject;
import com.google.gwt.libideas.validation.client.ValidationException;
import com.google.gwt.libideas.validation.client.Validator;

import java.util.Date;

/**
 * Validator with some built in error messages.
 */
public abstract class BuiltInValidator extends Validator {

  /**
   * Build in messages for this validator. Validators should never setup their
   * own messages as it may be web based or server based.
   */
  private static BuiltInValidatorMessages msgs;

  /**
   * Gets the build in error messages.
   */
  public static BuiltInValidatorMessages getMessages() {
    return msgs;
  }

  /**
   * Sets the build in error messages.
   */
  public static void setMessages(BuiltInValidatorMessages msgs) {
    BuiltInValidator.msgs = msgs;
  }

  /**
   * Convert strings to dates.
   */
  public Date getDate(Subject v) throws ValidationException {
    Object answer = v.getValue();
    if (answer instanceof Date || answer == null) {
      return (Date) answer;
    } else if (answer instanceof String) {
      if (((String) answer).trim().length() == 0) {
        return null;
      }
      try {
        return new Date((String) answer);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(getMessages().badFormat(v.getLabel(),
            v.getValue().toString(), "9/99/9999"));
      }
    } else {
      throw new ValidationException(getMessages().badFormat(v.getLabel(),
          v.getValue().toString(), "9/99/9999"));
    }
  }

}
