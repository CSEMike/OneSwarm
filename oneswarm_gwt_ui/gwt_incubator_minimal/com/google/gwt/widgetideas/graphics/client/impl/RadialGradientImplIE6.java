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
 *  IE6 deferred binding of Gradient Factory will create instances of this class
 *  for RadialGradients.
 */
public class RadialGradientImplIE6 extends CanvasGradientImplIE6 {
    
  public RadialGradientImplIE6(double x0, double y0, double r0, double x1,
      double y1, double r1) {
    super(x0,y0,x1,y1);
    startRad = r0;
    endRad = r1;
    type = "gradientradial";
  }

  public CanvasGradientImplIE6 cloneGradient() {
    RadialGradientImplIE6 newGrad = new RadialGradientImplIE6(startX,startY,startRad,endX,endY,endRad);
    newGrad.startX = this.startX;
    newGrad.startY = this.startY;
    newGrad.startRad = this.startRad;
    newGrad.endX = this.endX;
    newGrad.endY = this.endY;
    newGrad.endRad = this.endRad;
    
    ColorStop[] cStops = colorStops.toArray(new ColorStop[0]);
    
    for (int i = 0; i < cStops.length; i++) { 
      newGrad.colorStops.add(cStops[i].cloneColorStop());
    }
    return newGrad;
  }

}
