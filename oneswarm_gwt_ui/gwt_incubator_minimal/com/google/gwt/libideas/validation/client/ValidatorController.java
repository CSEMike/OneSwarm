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

import com.google.gwt.core.client.GWT;
import com.google.gwt.libideas.validation.client.validator.BuiltInValidator;
import com.google.gwt.libideas.validation.client.validator.BuiltInValidatorMessages;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Allows users to include Validators in their client or server side code. The
 * <code>ValidatorController</code> class serves as the bookkeeper for the
 * validation system. Each <code>ValidatorController</code> is composed of
 * <code>Validators</code>, <code>Subjects</code>, and a single
 * <code>ErrorHandler</code>. When the <code>ValidatorController</code> is
 * validated, it takes the cross-product of (Validator,Subject, ErrorHandler)
 * and calls <code>Validator.validate</code> on each (Subject,ErrorHandler)
 * pair.
 */
public class ValidatorController extends AbstractValidatorController implements
    ClickListener, FocusListener, ChangeListener {

  protected static boolean setup = false;
  static {
    setupInBrowser();
  }

  /**
   * Creates a new ValidatorController for the given text box, which is checked
   * when the text box losses focus.
   */
  public static ValidatorController addAsFocusListener(TextBox box,
      Validator validator) {
    DefaultTextBoxSubject target = new DefaultTextBoxSubject(box);
    ValidatorController v = new ValidatorController(target, validator);
    box.addFocusListener(v);
    return v;
  }

  /**
   * 
   * @return
   */
  private static boolean inGWTModule() {
    try {
      GWT.getModuleName();
      // This is actual a UnsatifiedLinkError, but GWT does not have that class.
    } catch (Error l) {
      return false;
    }
    return true;
  }

  private static void setupInBrowser() {
    if (setup == false) {
      setup = true;
      setDefaultErrorHandler(new DefaultErrorHandler());
      BuiltInValidator.setMessages((BuiltInValidatorMessages) GWT.create(BuiltInValidatorMessages.class));
    }
  }

  /**
   * Constructor for <code>ValidatorController</code>.
   */
  public ValidatorController() {
    // Default constructor.
  }

  /**
   * Constructor for <code>ValidatorController</code>.
   */
  public ValidatorController(Subject subject) {
    addSubject(subject);
  }

  /**
   * Constructor for <code>ValidatorController</code>.
   */
  public ValidatorController(Subject subject, Validator validator) {
    addSubject(subject);
    addValidator(validator);
  }

  /**
   * Constructor for <code>ValidatorController</code>.
   */
  public ValidatorController(Subject subject, Validator validator,
      ErrorHandler errorHandler) {
    this(subject, validator);
    setErrorHandler(errorHandler);
  }

  public void onChange(Widget sender) {
    validate();
  }

  public void onClick(Widget sender) {
    validate();
  }

  public void onFocus(Widget sender) {
    // Do nothing
  }

  public void onLostFocus(Widget sender) {
    validate();
  }

}
