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

/**
 * Abstract Class representing a color gradient.
 * Color gradients are comprised of multiple colors located at color stops.
 * 
 * @deprecated As of GWT 2.3, use {@link com.google.gwt.canvas.client.Canvas} instead.
 */
@Deprecated
public abstract class CanvasGradient {
  
  /**
   * Adds a color stop to the gradient.
   * 
   * @param offset the offset at which to apply the colorstop
   * @param color the color of the color stop
   */
  public abstract void addColorStop(double offset, Color color);
}
