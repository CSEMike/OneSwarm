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
 * Logical event fired when the number of pages changes.
 */
public class PageCountChangeEvent extends AbstractEvent {
  /**
   * Event Key for {@link PageCountChangeEvent}.
   */
  public static final Type<PageCountChangeEvent, PageCountChangeHandler> TYPE = new Type<PageCountChangeEvent, PageCountChangeHandler>() {
    @Override
    protected void fire(PageCountChangeHandler handler, PageCountChangeEvent event) {
      handler.onPageCountChange(event);
    }
  };

  /**
   * The new page count.
   */
  private int newPageCount;

  /**
   * The previous page count.
   */
  private int oldPageCount;

  /**
   * Construct a new {@link PageCountChangeEvent}.
   * 
   * @param oldPageCount the previous page
   * @param newPageCount the page that was requested
   */
  public PageCountChangeEvent(int oldPageCount, int newPageCount) {
    this.oldPageCount = oldPageCount;
    this.newPageCount = newPageCount;
  }

  /**
   * @return the new page count
   */
  public int getNewPageCount() {
    return newPageCount;
  }

  /**
   * @return the old page count
   */
  public int getOldPageCount() {
    return oldPageCount;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
