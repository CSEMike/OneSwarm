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

import com.google.gwt.libideas.resources.client.CssResource;

/**
 * An impl class that provides basic functionally to support supporting css
 * resource as well as standard css.
 * 
 * 
 */
public class StandardCssImpl implements CssResource {
  private String baseName;
  private String widgetName;

  public StandardCssImpl(String widgetName, String baseName) {
    this.widgetName = widgetName;
    this.baseName = baseName;
  }

  public String getBaseStyleName() {
    return baseName;
  }

  // Bogus impl of css resources getName.
  public String getName() {
    return null;
  }

  // Bogus impl of css resources getText.
  public String getText() {
    return null;
  }

  public String getWidgetStyleName() {
    return widgetName;
  }

  /**
   * Prepends the base name to the given style.
   * 
   * @param style style name
   * @return style name
   */
  protected String wrap(String styleName) {
    return baseName + styleName;
  }
}