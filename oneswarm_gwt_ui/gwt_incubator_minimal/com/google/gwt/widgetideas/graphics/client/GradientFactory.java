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

import com.google.gwt.user.client.Element;

/**
 * Radial Gradient for use as a stroke or fill style. Depends on deferred
 * binding implementations.
 * 
 * @deprecated As of GWT 2.3, use {@link com.google.gwt.canvas.client.Canvas} instead.
 */
@Deprecated
public interface GradientFactory {

  CanvasGradient createLinearGradient(double x0, double y0, double x1,
      double y1, Element c);

  CanvasGradient createRadialGradient(double x0, double y0, double r0,
      double x1, double y1, double r1, Element c);
}
