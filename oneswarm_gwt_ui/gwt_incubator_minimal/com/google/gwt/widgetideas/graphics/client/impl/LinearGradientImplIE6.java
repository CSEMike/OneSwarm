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

/**
 * IE6 implementation of Linear Gradient. This is instantiated by IE6 deferred binding
 * of GradientFactory.
 */
public class LinearGradientImplIE6 extends CanvasGradientImplIE6 {
  
  public LinearGradientImplIE6(double x0, double y0, double x1, double y1) {
    super(x0,y0,x1,y1);
    type = "gradient";
  }

  public CanvasGradientImplIE6 cloneGradient() {
    LinearGradientImplIE6 newGrad = new LinearGradientImplIE6(startX,startY,endX,endY);
    newGrad.startX = this.startX;
    newGrad.startY = this.startY;
    newGrad.endX = this.endX;
    newGrad.endY = this.endY;
    
    ColorStop[] cStops = colorStops.toArray(new ColorStop[0]);
    
    for (int i = 0; i < cStops.length; i++) { 
      newGrad.colorStops.add(cStops[i].cloneColorStop());
    }
    return newGrad;
  }

}
