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
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.widgetideas.graphics.client.CanvasGradient;

/**
 * Deferred binding implementation of GWTCanvas.
 */
public class GWTCanvasImplDefault implements GWTCanvasImpl {

  @SuppressWarnings("unused")
  private JavaScriptObject canvasContext = null;
  
  public native void arc(double x, double y, double radius, double startAngle,
      double endAngle, boolean antiClockwise) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).arc(x,y,radius,startAngle,endAngle,antiClockwise);
  }-*/;
  
  public native void beginPath() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).beginPath();
  }-*/;
  
  public void clear(int width, int height) {
    clearRect(0,0,width,height);
  }
  
  public native void closePath() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).closePath();
  }-*/;
  
  public native Element createElement() /*-{
    var e = $doc.createElement("CANVAS");
    this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::setCanvasContext(Lcom/google/gwt/core/client/JavaScriptObject;)(e.getContext('2d'));
    return e;
  }-*/;
  
  public native void cubicCurveTo(double cp1x, double cp1y, double cp2x, double cp2y,
      double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).bezierCurveTo(cp1x,cp1y,cp2x,cp2y,x,y);
  }-*/;
  
  public native void drawImage(ImageElement img, double sourceX, double sourceY, double sourceWidth,
      double sourceHeight, double destX, double destY, double destWidth, double destHeight) /*-{
     
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).drawImage(img,sourceX,sourceY,sourceWidth,sourceHeight,destX,destY,destWidth,destHeight);
    
  }-*/;

  public native void fill() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).fill();
  }-*/;

  public native void fillRect(double startX, double startY, double width, double height) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).fillRect(startX,startY,width,height);
  }-*/;

  public native double getGlobalAlpha() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).globalAlpha;
  }-*/;

  public native String getGlobalCompositeOperation() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).globalCompositeOperation;
  }-*/;

  public int getHeight(Element elem) {
    return DOM.getElementPropertyInt(elem, "height");
  }

  public native String getLineCap() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineCap;
  }-*/;

  public native String getLineJoin() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineJoin;
  }-*/;

  public native double getLineWidth() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineWidth;
  }-*/;

  public native double getMiterLimit() /*-{
    return (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).miterLimit;
  }-*/;

  public int getWidth(Element elem) {
    return DOM.getElementPropertyInt(elem, "width");
  }

  public native void lineTo(double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineTo(x,y);
  }-*/;

  public native void moveTo(double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).moveTo(x,y);
  }-*/;

  public native void quadraticCurveTo(double cpx, double cpy, double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).quadraticCurveTo(cpx,cpy,x,y);
  }-*/;

  public native void rect(double x, double y, double width, double height) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).rect(x,y,width,height);
  }-*/;

  public native void restoreContext() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).restore();
  }-*/;
   
  public native void rotate(double angle) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).rotate(angle);
  }-*/;
  
  public native void saveContext() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).save();
  }-*/;
  
  public native void scale(double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).scale(x,y);
  }-*/;

  public void setBackgroundColor(Element element, String color) {
    DOM.setStyleAttribute(element, "backgroundColor", color);
  }

  public void setCoordHeight(Element elem, int height) {
    DOM.setElementProperty(elem, "height", String.valueOf(height));
  }

  public void setCoordWidth(Element elem, int width) {
    DOM.setElementProperty(elem,"width", String.valueOf(width));
  }
  
  public void setFillStyle(CanvasGradient gradient) {
    setFillStyle((CanvasGradientImplDefault) gradient);
  }

  public native void setFillStyle(String colorStr) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).fillStyle = colorStr;
  }-*/;

  public native void setGlobalAlpha(double alpha) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).globalAlpha = alpha;
  }-*/;

  public native void setGlobalCompositeOperation(String globalCompositeOperation) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).globalCompositeOperation = globalCompositeOperation;    
  }-*/;

  public native void setLineCap(String lineCap) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineCap = lineCap;
  }-*/;

  public native void setLineJoin(String lineJoin) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineJoin = lineJoin;
  }-*/;

  public native void setLineWidth(double width) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).lineWidth = width;
  }-*/;
  
  public native void setMiterLimit(double miterLimit) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).miterLimit = miterLimit;
  }-*/;

  public void setPixelHeight(Element elem, int height) {
    DOM.setStyleAttribute(elem, "height", height + "px");
  }

  public void setPixelWidth(Element elem, int width) {
    DOM.setStyleAttribute(elem, "width", width + "px");
  }

  public void setStrokeStyle(CanvasGradient gradient) {
    setStrokeStyle((CanvasGradientImplDefault) gradient);
  }

  public native void setStrokeStyle(String colorStr) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).strokeStyle = colorStr;
  }-*/;

  public native void stroke() /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).stroke();
  }-*/;

  public native void strokeRect(double startX, double startY, double width, double height) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).strokeRect(startX,startY,width,height);
  }-*/;

  public native void transform(double m11, double m12, double m21, double m22, double dx,
      double dy) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).transform(m11,m12,m21,m22,dx,dy);
  }-*/;

  public native void translate(double x, double y) /*-{
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).translate(x,y);
  }-*/;

  private native void clearRect(double startX, double startY, double width, double height) /*-{   
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).clearRect(startX,startY,width,height);
  }-*/;

  @SuppressWarnings("unused")
  private void setCanvasContext(JavaScriptObject ctx) {
    this.canvasContext = ctx;
  }

  private native void setFillStyle(CanvasGradientImplDefault gradient) /*-{ 
    var gradObj = gradient.@com.google.gwt.widgetideas.graphics.client.impl.CanvasGradientImplDefault::getObject()();
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).fillStyle = gradObj; 
  }-*/;

  private native void setStrokeStyle(CanvasGradientImplDefault gradient) /*-{
    var gradObj = gradient.@com.google.gwt.widgetideas.graphics.client.impl.CanvasGradientImplDefault::getObject()();
    (this.@com.google.gwt.widgetideas.graphics.client.impl.GWTCanvasImplDefault::canvasContext).strokeStyle = gradObj; 
  }-*/;;
}
