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
 * Logical event fired immediately when a new page is requested.
 */
public class PageChangeEvent extends AbstractEvent {
  /**
   * Event Key for {@link PageChangeEvent}.
   */
  public static final Type<PageChangeEvent, PageChangeHandler> TYPE = new Type<PageChangeEvent, PageChangeHandler>() {
    @Override
    protected void fire(PageChangeHandler handler, PageChangeEvent event) {
      handler.onPageChange(event);
    }
  };

  /**
   * The new page.
   */
  private int newPage;

  /**
   * The previous page.
   */
  private int oldPage;

  /**
   * Construct a new {@link PageChangeEvent}.
   * 
   * @param oldPage the previous page
   * @param newPage the page that was requested
   */
  public PageChangeEvent(int oldPage, int newPage) {
    this.oldPage = oldPage;
    this.newPage = newPage;
  }

  /**
   * @return the new page that was requested
   */
  public int getNewPage() {
    return newPage;
  }

  /**
   * @return the old page
   */
  public int getOldPage() {
    return oldPage;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
