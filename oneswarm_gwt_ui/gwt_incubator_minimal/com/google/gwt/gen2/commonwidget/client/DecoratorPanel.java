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
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A panel which wraps its child element in the supplied decorator.
 */
public class DecoratorPanel extends SimplePanel {
  final Element container;
  final Element elem;

  /**
   * Creates a new decorator panel.
   */
  public DecoratorPanel(Decorator decorator) {
    super((Element) null);
    container = DOM.createDiv();
    elem = decorator.wrapElement(container);
    setElement(elem);
  }

  /**
   * Creates a new decorator panel.
   */
  public DecoratorPanel(Widget widget, Decorator decorator) {
    this(decorator);
    setWidget(widget);
  }

  @Override
  protected com.google.gwt.user.client.Element getContainerElement() {
    return (com.google.gwt.user.client.Element) container;
  }

}
