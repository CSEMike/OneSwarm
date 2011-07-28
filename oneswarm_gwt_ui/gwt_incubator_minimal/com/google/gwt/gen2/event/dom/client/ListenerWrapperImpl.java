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

import com.google.gwt.gen2.event.shared.AbstractEvent;
import com.google.gwt.gen2.event.shared.EventHandler;
import com.google.gwt.gen2.event.shared.HandlerManager;
import com.google.gwt.gen2.event.shared.HasHandlerManager;
import com.google.gwt.gen2.event.shared.AbstractEvent.Type;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;

import java.util.EventListener;

/**
 * legacy listener support for dom events. Will eventually be moved into
 * user.ui.
 * 
 * @param <ListenerType> listener type
 * 
 */

abstract class ListenerWrapperImpl<ListenerType> implements EventHandler {

  static class Click extends ListenerWrapperImpl<ClickListener> implements
      ClickHandler {

    public static void removeWrappedListener(HasHandlerManager eventSource,
        ClickListener listener) {
      removeWrappedListene(eventSource, listener, ClickEvent.TYPE);
    }

    protected Click(ClickListener listener) {
      super(listener);
    }

    public void onClick(ClickEvent event) {
      listener.onClick(source(event));
    }
  }

  /*
   * Handler wrapper for {@link FocusListener}.
   */
  static class Focus extends ListenerWrapperImpl<FocusListener> implements
      FocusHandler, BlurHandler {

    public Focus(FocusListener listener) {
      super(listener);
    }

    public void onBlur(BlurEvent event) {
      listener.onLostFocus(source(event));
    }

    public void onFocus(FocusEvent event) {
      listener.onFocus(source(event));
    }
  }

  static class Keyboard extends ListenerWrapperImpl<KeyboardListener> implements
      KeyDownHandler, KeyUpHandler, KeyPressHandler {

    public static void removeWrappedListener(HasHandlerManager eventSource,
        KeyboardListener listener) {
      ListenerWrapperImpl.removeWrappedListene(eventSource, listener,
          KeyDownEvent.TYPE, KeyUpEvent.TYPE, KeyPressEvent.TYPE);
    }

    public Keyboard(KeyboardListener listener) {
      super(listener);
    }

    public void onKeyDown(KeyDownEvent event) {
      listener.onKeyDown(source(event), (char) event.getKeyCode(),
          event.getKeyModifiers());
    }

    public void onKeyPress(KeyPressEvent event) {
      listener.onKeyPress(source(event),
          (char) event.getNativeEvent().getKeyCode(), event.getKeyModifiers());
    }

    public void onKeyUp(KeyUpEvent event) {
      source(event);
      listener.onKeyUp(source(event), (char) event.getKeyCode(),
          event.getKeyModifiers());
    }
  }

  static class Mouse extends ListenerWrapperImpl<MouseListener> implements
      MouseDownHandler, MouseUpHandler, MouseOutHandler, MouseOverHandler,
      MouseMoveHandler {

    public static <EventSourceType extends HasHandlerManager & HasMouseDownHandlers & HasMouseUpHandlers & HasMouseOutHandlers & HasMouseOverHandlers & HasMouseMoveHandlers, HandlerType extends MouseDownHandler & MouseUpHandler & MouseOutHandler & MouseOverHandler & MouseMoveHandler> void addHandlers(
        EventSourceType source, HandlerType handlers) {
      source.addMouseDownHandler(handlers);
      source.addMouseUpHandler(handlers);
      source.addMouseOutHandler(handlers);
      source.addMouseOverHandler(handlers);
      source.addMouseMoveHandler(handlers);
    }

    public static void remove(HasHandlerManager eventSource,
        MouseListener listener) {
      removeWrappedListene(eventSource, listener, MouseDownEvent.TYPE,
          MouseUpEvent.TYPE, MouseOverEvent.TYPE, MouseOutEvent.TYPE);
    }

    protected Mouse(MouseListener listener) {
      super(listener);
    }

    public void onMouseDown(MouseDownEvent event) {
      listener.onMouseDown(source(event), event.getClientX(),
          event.getScreenY());
    }

    public void onMouseMove(MouseMoveEvent event) {
      listener.onMouseMove(source(event), event.getClientX(),
          event.getClientY());
    }

    public void onMouseOut(MouseOutEvent event) {
      listener.onMouseLeave(source(event));
    }

    public void onMouseOver(MouseOverEvent event) {
      listener.onMouseEnter(source(event));
    }

    public void onMouseUp(MouseUpEvent event) {
      listener.onMouseUp(source(event), event.getClientX(), event.getClientY());
    }
  }

  protected static void removeWrappedListene(HasHandlerManager eventSource,
      EventListener listener, Type... keys) {
    HandlerManager manager = eventSource.getHandlerManager();
    for (Type key : keys) {
      int handlerCount = manager.getHandlerCount(key);
      for (int i = 0; i < handlerCount; i++) {
        EventHandler handler = manager.getHandler(key, i);
        if (handler instanceof ListenerWrapperImpl
            && ((ListenerWrapperImpl) handler).listener.equals(listener)) {
          manager.removeHandler(key, handler);
        }
      }
    }
  }

  protected final ListenerType listener;

  ListenerWrapperImpl(ListenerType listener) {
    this.listener = listener;
  }

  Widget source(AbstractEvent e) {
    return (Widget) e.getSource();
  }

}
