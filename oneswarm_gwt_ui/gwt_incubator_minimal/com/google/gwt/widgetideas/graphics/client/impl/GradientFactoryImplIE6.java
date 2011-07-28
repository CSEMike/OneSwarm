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
import com.google.gwt.widgetideas.graphics.client.CanvasGradient;
import com.google.gwt.widgetideas.graphics.client.GradientFactory;

/**
 * Deferred binding implementation of GradientFactory for IE6.
 */
public class GradientFactoryImplIE6 implements GradientFactory {

  public CanvasGradient createLinearGradient(double x0, double y0, double x1,
      double y1, Element c) {
    return new LinearGradientImplIE6(x0,y0,x1,y1);
  } 

  public CanvasGradient createRadialGradient(double x0, double y0, double r0,
      double x1, double y1, double r1, Element c) {
    return new RadialGradientImplIE6(x0,y0,r0,x1,y1,r1);
  }
}
