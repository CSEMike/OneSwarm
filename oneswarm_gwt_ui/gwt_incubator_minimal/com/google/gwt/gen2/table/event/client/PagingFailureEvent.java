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

package com.google.gwt.gen2.table.event.client;

import com.google.gwt.gen2.event.shared.AbstractEvent;

/**
 * Logical event fired when a page fails to load.
 */
public class PagingFailureEvent extends AbstractEvent {
  /**
   * Event Key for {@link PagingFailureEvent}.
   */
  public static final Type<PagingFailureEvent, PagingFailureHandler> TYPE = new Type<PagingFailureEvent, PagingFailureHandler>() {
    @Override
    protected void fire(PagingFailureHandler handler, PagingFailureEvent event) {
      handler.onPagingFailure(event);
    }
  };

  /**
   * The exception that caused the failure.
   */
  private Throwable exception;

  /**
   * Construct a new {@link PagingFailureEvent}.
   * 
   * @param exception the exception that caused the event
   */
  public PagingFailureEvent(Throwable exception) {
    this.exception = exception;
  }

  /**
   * @return the exception that caused the failure
   */
  public Throwable getException() {
    return exception;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
