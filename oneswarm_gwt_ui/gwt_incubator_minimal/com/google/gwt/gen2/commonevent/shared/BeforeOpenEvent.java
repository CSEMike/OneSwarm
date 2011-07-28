/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.gen2.commonevent.shared;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Represents an event that should fire after open. When a tree item receives a
 * request to open for the first time the isFirstTime flag will be set.
 * 
 * @param <T> the type being opened
 */
public class BeforeOpenEvent<T> extends GwtEvent<BeforeOpenHandler<T>> {
  /**
   * Handler type.
   */
  private static Type<BeforeOpenHandler<?>> TYPE;

  /**
   * Fires a beforeOpen event on all registered handlers in the handler manager.
   * If no such handlers exist, this method will do nothing.
   * 
   * @param <T> the target type
   * @param <S> The event source
   * @param source the source of the handlers
   * @param target the target
   */
  public static <T, S extends HasBeforeOpenHandlers<T>> void fire(S source,
      T target, boolean isFirstTime) {
    if (TYPE != null) {
      BeforeOpenEvent<T> event = new BeforeOpenEvent<T>(target, isFirstTime);
      source.fireEvent(event);
    }
  }

  /**
   * Fires a beforeOpen event on all registered handlers in the handler source.
   * If no such handlers exist, this method will do nothing.
   * 
   * @param <T> the target and event type
   * @param source the source of the handlers
   */
  public static <T extends HasBeforeOpenHandlers<T>> void fire(T source,
      boolean isFirstTime) {
    fire(source, source, isFirstTime);
  }

  public static Type<BeforeOpenHandler<?>> getType() {
    if (TYPE == null) {
      TYPE = new Type<BeforeOpenHandler<?>>();
    }
    return TYPE;
  }

  private final T target;

  /**
   * Flag to keep track of the first time @FastTreeItem has been opened.
   */
  private boolean isFirstTime;

  /**
   * Creates a new before open event.
   * 
   * @param target the ui object being opened
   * @param isFirstTime
   */
  protected BeforeOpenEvent(T target, boolean isFirstTime) {
    this.target = target;
    this.isFirstTime = isFirstTime;
  }

  // Because of type erasure, our static type is
  // wild carded, yet the "real" type should use our I param.
  @SuppressWarnings("unchecked")
  @Override
  public final Type<BeforeOpenHandler<T>> getAssociatedType() {
    return (Type) TYPE;
  }

  /**
   * Gets the target.
   * 
   * @return the target
   */
  public T getTarget() {
    return target;
  }

  public boolean isFirstTime() {
    return isFirstTime;
  }

  public void setFirstTime(boolean isFirstTime) {
    this.isFirstTime = isFirstTime;
  }

  @Override
  protected void dispatch(BeforeOpenHandler<T> handler) {
    handler.onBeforeOpen(this);
  }
}
