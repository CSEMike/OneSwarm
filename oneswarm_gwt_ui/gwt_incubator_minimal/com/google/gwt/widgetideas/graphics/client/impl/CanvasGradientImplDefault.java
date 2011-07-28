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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.widgetideas.graphics.client.CanvasGradient;
import com.google.gwt.widgetideas.graphics.client.Color;

/**
 * Native canvas gradients.
 */
public class CanvasGradientImplDefault extends CanvasGradient {
  
  private JavaScriptObject nativeGradient;
  
  public void addColorStop(double offset, Color color) {
    addNativeColorStop(offset,color.toString());
  }
  
  protected JavaScriptObject getObject() {
    return nativeGradient;
  }
  private native void addNativeColorStop(double offset, String color) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.CanvasGradientImplDefault::nativeGradient).addColorStop(offset,color);
  }-*/;

  @SuppressWarnings("unused")
  private void setNativeGradient(JavaScriptObject grad) {
    this.nativeGradient = grad;
  }
}
