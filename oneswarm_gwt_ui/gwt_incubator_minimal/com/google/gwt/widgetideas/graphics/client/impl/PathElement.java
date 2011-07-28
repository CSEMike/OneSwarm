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
 * Simple Path String generator class. This is used only by the IE6 deferred
 * binding implementation.
 */
public class PathElement {

  /**
   * Path Constants.
   */
  public static final String ARC_ANTI_CLOCK = " at";
  public static final String ARC_CLOCK = " wa";
  public static final String CLOSE = " x";
  public static final String CUBIC = " c";
  public static final String END = " e";
  public static final String LINETO = " l";
  public static final String MOVETO = " m";

  public static String arc(double x, double y, double radius,
      double startAngle, double endAngle, boolean antiClockwise,
      GWTCanvasImplIE6 canvas) {
    double fullArc = Math.PI * 2;

    // HTML 5 canvas spec treats an arc that subtends more than 360 degrees as a
    // full circle. It does not specify where the path should end up afterwards.
    // It also does not specify what should happen with negative angles.
    if (Math.abs(endAngle - startAngle) > fullArc) {
      // Draw a full circle.
      return arcImpl(x, y, radius, 0, fullArc, antiClockwise, canvas);
    } else {
      return arcImpl(x, y, radius, startAngle, endAngle, antiClockwise, canvas);
    }
  }

  public static String bezierCurveTo(double c1x, double c1y, double c2x,
      double c2y, double x, double y, GWTCanvasImplIE6 canvas) {
    double[] matrix = canvas.matrix;
    return CUBIC + canvas.getCoordX(matrix, c1x, c1y) + ","
        + canvas.getCoordY(matrix, c1x, c1y) + ","
        + canvas.getCoordX(matrix, c2x, c2y) + ","
        + canvas.getCoordY(matrix, c2x, c2y) + ","
        + canvas.getCoordX(matrix, x, y) + "," + canvas.getCoordY(matrix, x, y);
  }

  public static String closePath() {
    return CLOSE;
  }

  public static String lineTo(double x, double y, GWTCanvasImplIE6 canvas) {
    double[] matrix = canvas.matrix;
    return LINETO + canvas.getCoordX(matrix, x, y) + ","
        + canvas.getCoordY(matrix, x, y);
  }

  public static String moveTo(double x, double y, GWTCanvasImplIE6 canvas) {
    double[] matrix = canvas.matrix;
    return MOVETO + canvas.getCoordX(matrix, x, y) + ","
        + canvas.getCoordY(matrix, x, y);
  }

  private static String arcImpl(double x, double y, double radius,
      double startAngle, double endAngle, boolean antiClockwise,
      GWTCanvasImplIE6 canvas) {
    String arcType = antiClockwise ? ARC_ANTI_CLOCK : ARC_CLOCK;
    double[] matrix = canvas.matrix;
    VMLContext context = canvas.context;

    double ar;
    double endX;
    double endY;
    double startX;
    double startY;

    ar = radius * GWTCanvasImplIE6.subPixlFactor;
    startX = (x + Math.cos(startAngle)
        * (ar - GWTCanvasImplIE6.subPixelFactorHalf));
    startY = (y + Math.sin(startAngle)
        * (ar - GWTCanvasImplIE6.subPixelFactorHalf));
    endX = (x + Math.cos(endAngle) * (ar - GWTCanvasImplIE6.subPixelFactorHalf));
    endY = (y + Math.sin(endAngle) * (ar - GWTCanvasImplIE6.subPixelFactorHalf));

    double cx = canvas.getCoordX(matrix, x, y);
    double cy = canvas.getCoordY(matrix, x, y);
    double arcX = (context.arcScaleX * ar);
    double arcY = (context.arcScaleY * ar);

    return arcType + GWTCanvasImplIE6.doubleToFlooredInt(cx - arcX + 0.5f)
        + "," + GWTCanvasImplIE6.doubleToFlooredInt(cy + arcY + 0.5f) + " "
        + GWTCanvasImplIE6.doubleToFlooredInt(cx + arcX + 0.5f) + ","
        + GWTCanvasImplIE6.doubleToFlooredInt(cy - arcY + 0.5f) + " "
        + canvas.getCoordX(matrix, startX, startY) + ","
        + canvas.getCoordY(matrix, startX, startY) + " "
        + canvas.getCoordX(matrix, endX, endY) + ","
        + canvas.getCoordY(matrix, endX, endY);
  }
}