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
 */package com.google.gwt.gen2.event.dom.client;

import com.google.gwt.gen2.event.shared.HandlerRegistration;

/**
 * A widget that implements this interface is a public source of
 * {@link LoseCaptureEvent} events.
 * 
 * @deprecated use the com.google.gwt.event.dom.client classes instead
 */
@Deprecated
public interface HasLoseCaptureHandlers {
  /**
   * Adds a {@link LoseCaptureEvent} handler.
   * 
   * @param handler the lose capture handler
   * @return {@link HandlerRegistration} used to remove this handler
   */
  HandlerRegistration addLoseCaptureHandler(LoseCaptureHandler handler);
}
