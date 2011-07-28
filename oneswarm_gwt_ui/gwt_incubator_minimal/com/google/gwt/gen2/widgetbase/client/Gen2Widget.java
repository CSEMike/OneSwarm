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

package com.google.gwt.gen2.widgetbase.client;

import com.google.gwt.gen2.event.dom.client.DomEvent;
import com.google.gwt.gen2.event.shared.AbstractEvent;
import com.google.gwt.gen2.event.shared.EventHandler;
import com.google.gwt.gen2.event.shared.HandlerManager;
import com.google.gwt.gen2.event.shared.HandlerRegistration;
import com.google.gwt.gen2.event.shared.HasHandlerManager;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

/**
 * All top-level incubator widgets should extend {@link Gen2Widget}.
 * {@link Gen2Widget} will include all the extra handler and styling support
 * needed by gen2 widgets.
 * 
 * @deprecated gen2Widget's functionality has been subsumed by the 1.6 Widget
 *             functionality
 */
@Deprecated
public abstract class Gen2Widget extends Widget implements HasHandlerManager {

  private HandlerManager handlerManager;

  /**
   * Returns this widget's {@link HandlerManager} used for event management.
   * 
   * @return the handler manager
   */
  public final HandlerManager getHandlerManager() {
    if (handlerManager == null) {
      handlerManager = createLegacyHandlerManager();
    }
    return handlerManager;
  }

  @Override
  public void onBrowserEvent(Event nativeEvent) {
    super.onBrowserEvent(nativeEvent);
    if (handlerManager != null) {
      DomEvent.fireNativeEvent(nativeEvent, handlerManager);
    }
  }

  @Override
  public void setStyleName(String name) {
    setStylePrimaryName(name);
  }

  // This code can probably not be compiled out, but this toString is
  // sufficiently more useful the cost seems worth it.
  @Override
  public String toString() {
    String accum = this.getStylePrimaryName() + " widget";
    String id = this.getElement().getId();
    if (id != null && id.trim().length() > 0) {
      accum += "id " + id;
    }
    return accum;
  }

  /**
   * Adds a native event handler to the widget and sinks the corresponding
   * native event. If you do not wish to sink the native event, use the
   * addHandler method instead.
   * 
   * @param <HandlerType> the type of handler to add
   * @param key the event key
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  protected <HandlerType extends EventHandler> HandlerRegistration addDomHandler(
      DomEvent.Type<?, HandlerType> key, final HandlerType handler) {
    sinkEvents(key.getNativeEventType());
    return addHandler(key, handler);
  }

  /**
   * Adds a handler.
   * 
   * @param <HandlerType> the type of handler to add
   * @param type the event type
   * @param handler the handler
   * @return {@link HandlerRegistration} used to remove the handler
   */
  protected <HandlerType extends EventHandler> HandlerRegistration addHandler(
      AbstractEvent.Type<?, HandlerType> type, final HandlerType handler) {
    return getHandlerManager().addHandler(type, handler);
  }

  /**
   * <p>
   * Creates the {@link HandlerManager} used by this widget for event
   * management.
   * </p>
   * <p>
   * Note that {@link Gen2Widget} is deprecated and should not be used. The
   * HandlerManager has been moved to GWT trunk and included in {@link Widget},
   * so Gen2Widget contains both the deprecated {@link HandlerManager} and the
   * non-deprecated {@link com.google.gwt.event.shared.HandlerManager}. 
   * </p>
   * 
   * @return the handler manager
   * @see #createHandlerManager()
   */
  protected HandlerManager createLegacyHandlerManager() {
    return new HandlerManager(this);
  }

  /**
   * Fires an event.
   * 
   * @param event the event
   */
  protected void fireEvent(AbstractEvent event) {
    if (handlerManager != null) {
      handlerManager.fireEvent(event);
    }
  }

  /**
   * Is the event handled by one or more handlers?
   * 
   * @param type event type
   * @return does this event type have a current handler
   */
  protected final boolean isEventHandled(AbstractEvent.Type type) {
    return handlerManager == null ? false : handlerManager.isEventHandled(type);
  }

  /**
   * Removes the given handler from the specified event type. Normally,
   * applications should call {@link HandlerRegistration#removeHandler()}
   * instead.
   * 
   * @param <HandlerType> handler type
   * 
   * @param type the event type
   * @param handler the handler
   */
  protected <HandlerType extends EventHandler> void removeHandler(
      AbstractEvent.Type<?, HandlerType> type, final HandlerType handler) {
    if (handlerManager == null) {
      handlerManager = new HandlerManager(this);
    }
    handlerManager.removeHandler(type, handler);
  }
}
