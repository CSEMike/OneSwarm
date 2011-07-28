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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * Base class for Key events. The native keyboard events are somewhat a mess
 * (http://www.quirksmode.org/js/keys.html), we do some trivial normalization
 * here, but do not attempt any complex patching, so user be warned.
 * 
 * @deprecated use the com.google.gwt.event.dom.client classes instead
 */
@Deprecated
public abstract class KeyEvent extends DomEvent {

  /**
   * Alt modifier.
   */
  public static final int MODIFIER_ALT = 4;
  /**
   * Control modifier.
   */
  public static final int MODIFIER_CTRL = 2;
  /**
   * Meta modifier.
   */
  public static final int MODIFIER_META = 8;
  /**
   * Shift modifier.
   */
  public static final int MODIFIER_SHIFT = 1;

  /**
   * Constructor.
   * 
   * @param nativeEvent the wrapped native event
   */
  protected KeyEvent(Event nativeEvent) {
    super(nativeEvent);
  }

  /**
   * Gets the key modifiers associated with this event.
   * 
   * @return the modifiers as defined in {@link KeyCodeEvent}.
   */
  public int getKeyModifiers() {
    Event event = getNativeEvent();
    return (DOM.eventGetShiftKey(event) ? MODIFIER_SHIFT : 0)
        | (DOM.eventGetMetaKey(event) ? MODIFIER_META : 0)
        | (DOM.eventGetCtrlKey(event) ? MODIFIER_CTRL : 0)
        | (DOM.eventGetAltKey(event) ? MODIFIER_ALT : 0);
  }

  /**
   * Is the <code>alt</code> key down?
   * 
   * @return whether the alt key is down
   */
  public boolean isAltKeyDown() {
    return getNativeEvent().getAltKey();
  }

  /**
   * Gets the key-repeat state of this event.
   * 
   * @return <code>true</code> if this key event was an auto-repeat
   */
  public boolean isAutoRepeat() {
    return getNativeEvent().getRepeat();
  }

  /**
   * Is the <code>control</code> key down?
   * 
   * @return whether the control key is down
   */
  public boolean isControlKeyDown() {
    return getNativeEvent().getCtrlKey();
  }

  /**
   * Is the <code>shift</code> key down?
   * 
   * @return whether the shift key is down
   */
  public boolean isShiftKeyDown() {
    return getNativeEvent().getShiftKey();
  }
}
