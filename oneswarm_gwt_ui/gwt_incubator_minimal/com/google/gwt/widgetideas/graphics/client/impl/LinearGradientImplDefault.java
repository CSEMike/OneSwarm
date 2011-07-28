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
package com.google.gwt.widgetideas.graphics.client.impl;

import com.google.gwt.user.client.Element;

/**
 * Default deferred binding of GradientFactory will create instances of this class.
 * This corresponds to a LinearGradient for stroke or fill styles.
 */
public class LinearGradientImplDefault extends CanvasGradientImplDefault {
  
  public LinearGradientImplDefault(double x0, double y0, double x1, double y1, Element c) {
    createNativeGradientObject(x0,y0,x1,y1,c);
  }

  private native void createNativeGradientObject(double x0, double y0, double x1, double y1, Element c) /*-{
    var ctx = c.getContext('2d');
    var gradient = ctx.createLinearGradient(x0,y0,x1,y1);
    this.@com.google.gwt.widgetideas.graphics.client.impl.CanvasGradientImplDefault::setNativeGradient(Lcom/google/gwt/core/client/JavaScriptObject;)(gradient);
  }-*/;

}
