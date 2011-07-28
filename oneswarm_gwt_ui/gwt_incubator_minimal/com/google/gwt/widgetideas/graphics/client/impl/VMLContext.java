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

import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

/**
 * The VML context abstraction for the Internet Explorer implementation.
 */
public class VMLContext {

  public double arcScaleX;

  public double arcScaleY;

  public double fillAlpha;

  public String fillStyle;

  public CanvasGradientImplIE6 fillGradient;

  public CanvasGradientImplIE6 strokeGradient;

  public double globalAlpha;

  public String globalCompositeOperation;

  public String lineCap;

  public String lineJoin;
  
  public double lineScale;

  public double lineWidth;

  public double[] matrix = new double[9];

  public double miterLimit;

  public double strokeAlpha;

  public String strokeStyle;

  public VMLContext() {

    // load identity matrix
    matrix[0] = 1;
    matrix[1] = 0;
    matrix[2] = 0;
    matrix[3] = 0;
    matrix[4] = 1;
    matrix[5] = 0;
    matrix[6] = 0;
    matrix[7] = 0;
    matrix[8] = 1;

    // init other stuff
    arcScaleX = 1;
    arcScaleY = 1;
    globalAlpha = 1;
    strokeAlpha = 1;
    fillAlpha = 1;
    miterLimit = 10;
    lineWidth = 1;
    lineCap = GWTCanvasImplIE6.BUTT;
    lineScale = 1;
    lineJoin = GWTCanvas.MITER;
    strokeStyle = "#000";
    fillStyle = "#000";
    globalCompositeOperation = GWTCanvasImplIE6.SOURCE_OVER;
  }

  public VMLContext(VMLContext ctx) {

    // copy the matrix
    matrix[0] = ctx.matrix[0];
    matrix[1] = ctx.matrix[1];
    matrix[2] = ctx.matrix[2];
    matrix[3] = ctx.matrix[3];
    matrix[4] = ctx.matrix[4];
    matrix[5] = ctx.matrix[5];
    matrix[6] = ctx.matrix[6];
    matrix[7] = ctx.matrix[7];
    matrix[8] = ctx.matrix[8];

    // copy other stuff
    arcScaleX = ctx.arcScaleX;
    arcScaleY = ctx.arcScaleY;
    globalAlpha = ctx.globalAlpha;
    strokeAlpha = ctx.strokeAlpha;
    fillAlpha = ctx.fillAlpha;
    miterLimit = ctx.miterLimit;
    lineScale = ctx.lineScale;
    lineWidth = ctx.lineWidth;
    lineCap = ctx.lineCap;
    lineJoin = ctx.lineJoin;
    strokeStyle = ctx.strokeStyle;
    fillStyle = ctx.fillStyle;
    fillGradient = ctx.fillGradient;
    strokeGradient = ctx.strokeGradient;
    globalCompositeOperation = ctx.globalCompositeOperation;
  }
}
