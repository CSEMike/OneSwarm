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

import com.google.gwt.widgetideas.graphics.client.CanvasGradient;
import com.google.gwt.widgetideas.graphics.client.Color;

import java.util.ArrayList;

/**
 * Gradients for IE6 implementation need some extra meta info.
 */
public abstract class CanvasGradientImplIE6 extends CanvasGradient {
  public String type;
  
  public double startX;
  public double startY;
  public double endX;
  public double endY;
  public double startRad;
  public double endRad;
  public int angle;
  public double length;
  
  public ArrayList<ColorStop> colorStops;

  public CanvasGradientImplIE6(double x0, double y0, double x1, double y1) {
    startX = x0;
    startY = y0;
    endX = x1;
    endY = y1;
    startRad = 0;
    endRad = 0;
    double dx = x1 - x0;
    double dy = y1 - y0;
    length = (double) Math.sqrt((dx * dx) + (dy * dy));
    angle = (int) (Math.atan(dx / dy) * 180 / Math.PI) + 180;
   
    colorStops = new ArrayList<ColorStop>();
  }
  
  public void addColorStop(double offset, Color color) { 
    ColorStop newColorStop = new ColorStop(offset, color);
    for (int i = 0; i < colorStops.size(); i++) {
      ColorStop cs = colorStops.get(i);
      if (offset < cs.offset) {
        colorStops.add(i, newColorStop);
        return;
      }
    }
    colorStops.add(newColorStop);
  }

  /**
   * Creates an equivalent copy of this Gradient object.
   * 
   * @return returns an equivalent copy of this gradient object
   */
  public abstract CanvasGradientImplIE6 cloneGradient();
}
