/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.gen2.commonwidget.client.impl;

import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.KeyboardListener;

/**
 * This class contains some of the common code needed to provide good keyboard
 * support.
 * 
 */
public class KeyboardSupportImpl {
  private static final int OTHER_KEY_UP = 63232;

  private static final int OTHER_KEY_DOWN = 63233;

  private static final int OTHER_KEY_LEFT = 63234;

  private static final int OTHER_KEY_RIGHT = 63235;

  /**
   * Does this event have modifiers?
   */
  public static boolean hasModifiers(Event event) {
    boolean alt = event.getAltKey();
    boolean ctrl = event.getCtrlKey();
    boolean meta = event.getMetaKey();
    boolean shift = event.getShiftKey();

    return alt || ctrl || meta || shift;
  }

  /**
   * Is this an arrow key.
   */
  public static boolean isArrowKey(int code) {
    switch (code) {
      case OTHER_KEY_DOWN:
      case OTHER_KEY_RIGHT:
      case OTHER_KEY_UP:
      case OTHER_KEY_LEFT:
      case KeyboardListener.KEY_DOWN:
      case KeyboardListener.KEY_RIGHT:
      case KeyboardListener.KEY_UP:
      case KeyboardListener.KEY_LEFT:
        return true;
      default:
        return false;
    }
  }

  /**
   * Normalized key codes. Also switches KEY_RIGHT and KEY_LEFT in RTL
   * languages.
   */
  public static int standardizeKeycode(int code) {
    switch (code) {
      case OTHER_KEY_DOWN:
        code = KeyboardListener.KEY_DOWN;
        break;
      case OTHER_KEY_RIGHT:
        code = KeyboardListener.KEY_RIGHT;
        break;
      case OTHER_KEY_UP:
        code = KeyboardListener.KEY_UP;
        break;
      case OTHER_KEY_LEFT:
        code = KeyboardListener.KEY_LEFT;
        break;
    }
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      if (code == KeyboardListener.KEY_RIGHT) {
        code = KeyboardListener.KEY_LEFT;
      } else if (code == KeyboardListener.KEY_LEFT) {
        code = KeyboardListener.KEY_RIGHT;
      }
    }
    return code;
  }

}
