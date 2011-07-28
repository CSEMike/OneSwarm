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
 */package com.google.gwt.gen2.event.logical.shared;

import com.google.gwt.gen2.event.shared.HandlerRegistration;

/**
 * An event source that implements this interface is a public source of
 * {@link HighlightEvent} events.
 * 
 * @param <Value> The value associated with the highlight event
 * @deprecated use the com.google.gwt.event.logical.shared classes instead
 */
@Deprecated
public interface HasHighlightHandlers<Value> {
  /**
   * Adds a {@link HighlightEvent} handler.
   * 
   * @param handler the handler
   * @return the handler registration
   */
  HandlerRegistration addHighlightHandler(HighlightHandler<Value> handler);
}
