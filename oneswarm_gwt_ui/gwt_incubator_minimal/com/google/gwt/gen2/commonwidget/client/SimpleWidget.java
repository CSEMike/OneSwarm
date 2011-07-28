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

package com.google.gwt.gen2.commonwidget.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.gen2.commonwidget.client.impl.StandardCssImpl;
import com.google.gwt.gen2.event.dom.client.ClickEvent;
import com.google.gwt.gen2.event.dom.client.ClickHandler;
import com.google.gwt.gen2.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.gen2.event.dom.client.HasClickHandlers;
import com.google.gwt.gen2.event.dom.client.MouseDownEvent;
import com.google.gwt.gen2.event.dom.client.MouseDownHandler;
import com.google.gwt.gen2.event.dom.client.MouseMoveEvent;
import com.google.gwt.gen2.event.dom.client.MouseMoveHandler;
import com.google.gwt.gen2.event.dom.client.MouseOutEvent;
import com.google.gwt.gen2.event.dom.client.MouseOutHandler;
import com.google.gwt.gen2.event.dom.client.MouseOverEvent;
import com.google.gwt.gen2.event.dom.client.MouseOverHandler;
import com.google.gwt.gen2.event.dom.client.MouseUpEvent;
import com.google.gwt.gen2.event.dom.client.MouseUpHandler;
import com.google.gwt.gen2.event.dom.client.MouseWheelEvent;
import com.google.gwt.gen2.event.dom.client.MouseWheelHandler;
import com.google.gwt.gen2.event.shared.HandlerRegistration;
import com.google.gwt.gen2.widgetbase.client.Gen2Widget;
import com.google.gwt.gen2.widgetbase.client.WidgetCss;
import com.google.gwt.user.client.DOM;

/**
 * Simple widget which wraps a single element and adds mouse and click events to
 * it.
 */
public class SimpleWidget extends Gen2Widget implements HasAllMouseHandlers,
    HasClickHandlers {

  /**
   * Css for simple widget.
   */
  public static interface Css extends WidgetCss {
    /**
     * Base name.
     */
    @ClassName("gwt-SimpleWidget")
    String simpleWidget();
  }

  /**
   * Standard Css for simple widget.
   */
  public static Css DEFAULT_CSS;
  private static Element translater;

  /**
   * Create the css for this widget with the given base name.
   */
  public static Css createCss(final String styleName) {
    class Standard extends StandardCssImpl implements Css {
      Standard() {
        super(styleName, "simpleWidget");
      }

      public String simpleWidget() {
        return getWidgetStyleName();
      }
    }
    return new Standard();
  }

  private static Element convertHtml(String html) {
    if (translater == null) {
      translater = DOM.createDiv();
    }
    translater.setInnerHTML(html);
    assert (translater.getChildNodes().getLength() == 1);
    Element e = translater.getFirstChildElement();
    translater.removeChild(e);
    return e;
  }

  private static Css ensureDefaultCss() {
    if (DEFAULT_CSS == null) {
      DEFAULT_CSS = createCss("gwt-SimpleWidget");
    }
    return DEFAULT_CSS;
  }

  /**
   * Constructor. The element passed in should not be attached to a parent and
   * should support mouse events and clicks.
   */
  public SimpleWidget(Element e) {
    this(e, ensureDefaultCss());
  }

  /**
   * Constructor. The element passed in should not be attached to a parent and
   * should support mouse events and clicks.
   */
  public SimpleWidget(Element e, Css css) {
    assert (e != null);
    assert (css != null);
    assert (e.getParentElement() == null);
    setElement(e);
    setStyleName(e, css.simpleWidget());
  }

  /**
   * Constructor. The html passed in to this constructor should represent a
   * single element and its children. The element, or one of its children,
   * should support click and mouse events.
   */
  public SimpleWidget(String html) {
    this(convertHtml(html), ensureDefaultCss());
  }
  


  /**
   * Constructor. The html passed in to this constructor should represent a
   * single element and its children. The element, or one of its children,
   * should support click and mouse events.
   */
  public SimpleWidget(String html, Css css) {
    this(convertHtml(html), css);
  }

  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(ClickEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(MouseDownEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(MouseMoveEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(MouseOutEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(MouseOverEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(MouseUpEvent.TYPE, handler);
  }

  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(MouseWheelEvent.TYPE, handler);
  }
}
