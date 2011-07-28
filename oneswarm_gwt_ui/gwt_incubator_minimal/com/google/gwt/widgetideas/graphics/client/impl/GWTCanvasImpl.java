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

import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.Element;
import com.google.gwt.widgetideas.graphics.client.CanvasGradient;

/**
 * Common interface for all Deferred binding implementations of 
 * GWTCanvas.
 *
 * @deprecated As of GWT 2.3, use {@link com.google.gwt.canvas.client.Canvas} instead.
 */
@Deprecated
public interface GWTCanvasImpl {
  
  void arc(double x, double y, double radius, double startAngle, 
      double endAngle, boolean antiClockwise);

  void beginPath();
  
  void clear(int width, int height);
  
  void closePath();
  
  Element createElement();
  
  void cubicCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, 
      double x, double y);
  
  void drawImage(ImageElement img, double sourceX, double sourceY, double sourceWidth,
      double sourceHeight, double destX, double destY, double destWidth, double destHeight);
  
  void fill();
  
  void fillRect(double startX, double startY, double width, double height);
 
  double getGlobalAlpha();
  
  String getGlobalCompositeOperation();
  
  String getLineCap();
  
  String getLineJoin();
  
  double getLineWidth();
  
  double getMiterLimit();
  
  void lineTo(double x, double y);
  
  void moveTo(double x, double y);
  
  void quadraticCurveTo(double cpx, double cpy, double x, double y);
  
  void rect(double x, double y, double width, double height);
  
  void restoreContext();
  
  void rotate(double angle);
  
  void saveContext();
  
  void scale(double x, double y);
  
  void setBackgroundColor(Element element, String color);
  
  void setCoordHeight(Element elem, int height);
  
  void setCoordWidth(Element elem, int width);
 
  void setFillStyle(CanvasGradient gradient);
  
  void setFillStyle(String colorStr);
  
  void setGlobalAlpha(double alpha);
  
  void setGlobalCompositeOperation(String globalCompositeOperation);

  void setLineCap(String lineCap);

  void setLineJoin(String lineJoin);

  void setLineWidth(double width);

  void setMiterLimit(double miterLimit);

  void setPixelHeight(Element elem, int height);

  void setPixelWidth(Element elem, int width);

  void setStrokeStyle(CanvasGradient gradient);

  void setStrokeStyle(String colorStr);

  void stroke();

  void strokeRect(double startX, double startY, double width, double height);

  void transform(double m11, double m12, double m21,double m22, 
      double dx, double dy);

  void translate(double x, double y);
}
