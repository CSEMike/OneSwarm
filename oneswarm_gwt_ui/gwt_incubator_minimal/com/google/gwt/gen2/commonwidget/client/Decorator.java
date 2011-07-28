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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.UIObject;

/**
 * A decorator is used to allow subclasses to customize the html wrappers used
 * in their super classes.
 */
public class Decorator {

  /**
   * The default decorator. Simply returns the element/text input as its output.
   */
  public static final Decorator DEFAULT = new Decorator() {
    @Override
    public void wrapElementInPlace(Element element) {
    }

    @Override
    String wrapHTMLImpl(String html) {
      return html;
    }

    @Override
    Element wrapImpl(Node e) {
      return (Element) e;
    }
  };

  private static Element placeHolderDiv;
  private static Element hiddenDiv;
  static {
    if (GWT.isClient()) {
      hiddenDiv = DOM.createDiv();
      UIObject.setVisible(hiddenDiv, false);
      RootPanel.getBodyElement().appendChild(hiddenDiv);
      placeHolderDiv = DOM.createDiv();
    }
  }

  /**
   * Creates a div-based decorator with the given class name.
   * 
   * @param className the div's class name
   * @return the created decorator.
   */
  public static Decorator createDivDecorator(String className) {
    Element e = DOM.createDiv();
    e.setClassName(className);
    return new Decorator(e, e);
  } 
  private Element templateRoot;

  private String decoratorId = DOM.createUniqueId();

  /**
   * Creates a new decorator.
   * 
   * @param templateRoot root
   * @param templateAttach to attach
   */
  public Decorator(Element templateRoot, Element templateAttach) {
    setTemplateElements(templateRoot, templateAttach);
  }

  // Used for default decorator.
  private Decorator() {
  }

  /**
   * Returns a wrapped element.
   */
  public final Element wrapElement(Element element) {
    return wrapImpl(element);
  }

  /**
   * Wraps an element in place.
   */
  public void wrapElementInPlace(Element element) {
    Element parent = element.getParentElement();
    parent.replaceChild(placeHolderDiv, element);
    Element newChild = wrapImpl(element);
    parent.replaceChild(newChild, placeHolderDiv);
  }

  /**
   * Wraps the given string in the decorated html.
   * <p>
   * Note, current implementation does multiple conversions between html and
   * strings so is neither safe nor efficient.
   * 
   * @param html html to be wrapped.
   * @return wrapped html
   */
  public final String wrapHTML(String html) {
    return wrapHTMLImpl(html);
  }

  String wrapHTMLImpl(String html) {
    hiddenDiv.setInnerHTML(html);
    Node e = hiddenDiv.getFirstChild();
    assert (e != null) : html + " does not return a inner child";
    Element result = wrapImpl(e);
    return DOM.toString((com.google.gwt.user.client.Element) result);
  }

  Element wrapImpl(Node node) {
    Element nextRoot = (Element) templateRoot.cloneNode(true);
    hiddenDiv.appendChild(nextRoot);
    Element nextAttach = DOM.getElementById(decoratorId);
    nextAttach.removeAttribute("id");
    nextAttach.appendChild(node);

    // clear the hidden div.
    Node removed = hiddenDiv.removeChild(nextRoot);
    assert (removed != null);
    return nextRoot;
  }

  private void setTemplateElements(Element root, Element attachTo) {
    this.templateRoot = root;
    assert (root.isOrHasChild(attachTo));
    assert (root.getParentElement() == null);
    attachTo.setId(decoratorId);
  }
}
