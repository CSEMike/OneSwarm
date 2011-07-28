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

import com.google.gwt.user.client.Event;

/**
 * Represents a native blur event.
 * 
 * @deprecated use the com.google.gwt.event.dom.client classes instead
 */
@Deprecated
public class BlurEvent extends DomEvent {

  /**
   * Event type for blur events. Represents the meta-data associated with this
   * event.
   */
  public static final Type<BlurEvent, BlurHandler> TYPE = new Type<BlurEvent, BlurHandler>(
      Event.ONBLUR) {
    @Override
    public void fire(BlurHandler handler, BlurEvent event) {
      handler.onBlur(event);
    }

    @Override
    BlurEvent wrap(Event nativeEvent) {
      return new BlurEvent(nativeEvent);
    }
  };

  /**
   * Constructor.
   * 
   * @param nativeEvent the native event object
   */
  public BlurEvent(Event nativeEvent) {
    super(nativeEvent);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }

}
