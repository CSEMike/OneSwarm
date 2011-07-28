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
package com.google.gwt.gen2.event.dom.client;

import com.google.gwt.gen2.event.shared.EventHandler;
import com.google.gwt.user.client.Event;

/**
 * Abstract class representing mouse events.
 * 
 * @param <T> event handler type
 * @deprecated use the com.google.gwt.event.dom.client classes instead
 */
@Deprecated
public abstract class MouseEvent<T extends EventHandler> extends DomEvent {

  /**
   * The native Dom button codes.
   */
  public enum Button {
    /**
     * Left button.
     */
    LEFT,
    /**
     * Middle button.
     */
    MIDDLE,
    /**
     * Right button.
     */
    RIGHT;

    /**
     * Gets the button codes.
     * 
     * @param buttonCode the button code
     * @return which button
     */
    public static Button valueOf(int buttonCode) {
      switch (buttonCode) {
        case Event.BUTTON_LEFT:
          return LEFT;
        case Event.BUTTON_RIGHT:
          return RIGHT;
        case Event.BUTTON_MIDDLE:
          return MIDDLE;
        default:
          throw new IllegalStateException("Unknown button code " + buttonCode);
      }
    }
  }

  MouseEvent(Event e) {
    super(e);
  }

  /**
   * Gets the button value.
   * 
   * @return gets the button value.
   */
  public Button getButton() {
    return Button.valueOf(getNativeEvent().getButton());
  }

  /**
   * Gets the mouse x-position within the browser window's client area.
   * 
   * @return the mouse x-position
   */
  public int getClientX() {
    return getNativeEvent().getClientX();
  }

  /**
   * Gets the mouse y-position within the browser window's client area.
   * 
   * @return the mouse y-position
   */
  public int getClientY() {
    return getNativeEvent().getClientY();
  }

  /**
   * Gets the mouse x-position on the user's display.
   * 
   * @return the mouse x-position
   */
  public int getScreenX() {
    return getNativeEvent().getScreenX();
  }

  /**
   * Gets the mouse y-position on the user's display.
   * 
   * @return the mouse y-position
   */
  public int getScreenY() {
    return getNativeEvent().getScreenY();
  }

  /**
   * Is <code>alt</code> key down.
   * 
   * @return whether the alt key is down
   */
  public boolean isAltKeyDown() {
    return getNativeEvent().getAltKey();
  }

  /**
   * Is <code>control</code> key down.
   * 
   * @return whether the control key is down
   */
  public boolean isControlKeyDown() {
    return getNativeEvent().getCtrlKey();
  }

  /**
   * Is <code>meta</code> key down.
   * 
   * @return whether the meta key is down
   */
  public boolean isMetaKeyDown() {
    return getNativeEvent().getMetaKey();
  }

  /**
   * Is <code>shift</code> key down.
   * 
   * @return whether the shift key is down
   */
  public boolean isShiftKeyDown() {
    return getNativeEvent().getShiftKey();
  }
}