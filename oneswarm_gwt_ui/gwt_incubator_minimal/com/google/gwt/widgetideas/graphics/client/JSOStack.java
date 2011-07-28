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
package com.google.gwt.widgetideas.graphics.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * JSO Overlay for an array. Treated as a Stack.
 * 
 * @param <T>
 */
public class JSOStack<T> extends JavaScriptObject {

  /*
   * Pre-initialized JSOStack to be used for transient string concatenations
   * without having to instantiate a new object. JavaScript is single threaded
   * so we don't have to worry about races.
   */
  private static JSOStack<String> scratch = create();

  public static native <M> JSOStack<M> create() /*-{
     return [];
   }-*/;

  public static JSOStack<String> getScratchArray() {
    scratch.clear();
    return scratch;
  }

  protected JSOStack() {
  }

  public final native void clear() /*-{
     this.length = 0;
     this._minX = this._minY = this._maxX = this._maxY = null;
   }-*/;

  public final native double getMaxCoordX() /*-{
     return this._maxX;
   }-*/;

  public final native double getMaxCoordY() /*-{
     return this._maxY;
   }-*/;

  public final native double getMinCoordX() /*-{
   return this._minX;
  }-*/;

  public final native double getMinCoordY() /*-{
   return this._minY;
  }-*/;

  public final native boolean isEmpty() /*-{
     return (this.length == 0);
   }-*/;

  public final native String join() /*-{
     return this.join("");
   }-*/;

  public final native void logCoordX(double coordX) /*-{
     if (!this._minX) {
       this._minX = coordX;
       this._maxX = coordX;
     } else {
       if (this._minX > coordX) {
         this._minX = coordX;
       } else {
         if (this._maxX < coordX) {
           this._maxX = coordX;
         }
       }
     }
   }-*/;

  public final native void logCoordY(double coordY) /*-{
     if (!this._minY) {
       this._minY = coordY;
       this._maxY = coordY;
     } else {
       if (this._minY > coordY) {
         this._minY = coordY;
       } else {
         if (this._maxY < coordY) {
           this._maxY = coordY;
         }
       }
     }
   }-*/;

  public final native T pop() /*-{
     return this.pop();
   }-*/;

  /*
   * For backwards compatibility with IE5 and because this is marginally faster
   * than push() in IE6.
   */
  public final native void push(T pathStr) /*-{
     this[this.length] = pathStr;
   }-*/;

  public final native int size() /*-{
     return this.length;
   }-*/;
}
