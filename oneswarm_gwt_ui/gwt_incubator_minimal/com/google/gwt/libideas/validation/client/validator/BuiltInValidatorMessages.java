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

import com.google.gwt.i18n.client.Messages;

/**
 * Some common error messages.
 */
public interface BuiltInValidatorMessages extends Messages {

  /**
   * The text message to use for generic badly formatted values.
   */
  String badFormat(String subject, String target, String format);

  /**
   * The text message to use for values that are not greater than the other
   * value.
   */
  String greaterThan(String e, Object target, Object max);

  /**
   * The text message to use for values that are not less than the other value.
   */
  String lessThan(String question, Object target, Object min);

  /**
   * The text message to use for non-phone number values.
   */
  String phone(String fieldName, String answer);

  /**
   * The text message to use for non-ssn values.
   */
  String ssn(String fieldName, String answer);
}
