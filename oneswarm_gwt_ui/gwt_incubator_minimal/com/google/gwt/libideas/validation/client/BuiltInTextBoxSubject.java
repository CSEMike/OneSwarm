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

import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple default subject for a <code>TextBox</code>.
 */
public class BuiltInTextBoxSubject implements Subject {

  /**
   * Style used to indicate an error in a text box.
   */
  private static String errorStyle = "gwt-TextBox-error";

  /**
   * Override the default error style.
   */
  public static void setErrorStyleName(String styleName) {
    errorStyle = styleName;
  }

  /**
   * Saves the current text box style.
   */
  String textBoxStyle;

  /**
   * Target text box.
   */
  private final TextBox box;

  /**
   * Constructor for <code>BuiltInTextBoxSubject</code>.
   */
  public BuiltInTextBoxSubject(final TextBox box) {
    this.box = box;
    textBoxStyle = box.getStyleName();
    box.addKeyboardListener(new KeyboardListenerAdapter() {
      public void onKeyDown(Widget sender, char keyCode, int modifiers) {
        String curStyle = box.getStyleName();
        if (errorStyle.equals(curStyle)) {
          box.setStyleName(textBoxStyle);
        }
      }
    });
  }

  public boolean getError() {
    return box.getStyleName().contains(errorStyle);
  }
  
  /**
   * Gets the title of the text box.
   */
  public String getLabel() {
    return box.getTitle();
  }

  /**
   * Get the current text in the text box.
   */
  public Object getValue() {
    return box.getText();
  }

  /**
   * Sets the error status.
   */
  public void setError(boolean hasError) {
    if (hasError) {
      String curStyle = box.getStyleName();
      if (!curStyle.equals(errorStyle)) {
        if (!curStyle.equals(textBoxStyle)) {
          textBoxStyle = curStyle;
        }
        box.setStyleName(errorStyle);
      }
    } else {
      box.setStyleName(textBoxStyle);
    }
  }

  /**
   * Sets the answer for this text box.
   */
  public void setValue(Object answer) {
    box.setText((String) answer);
  }

}
