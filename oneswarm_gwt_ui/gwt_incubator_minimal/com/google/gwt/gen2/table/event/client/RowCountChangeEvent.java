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
 * Logical event fired when the number of rows changes.
 */
public class RowCountChangeEvent extends AbstractEvent {
  /**
   * Event Key for {@link RowCountChangeEvent}.
   */
  public static final Type<RowCountChangeEvent, RowCountChangeHandler> TYPE = new Type<RowCountChangeEvent, RowCountChangeHandler>() {
    @Override
    protected void fire(RowCountChangeHandler handler, RowCountChangeEvent event) {
      handler.onRowCountChange(event);
    }
  };

  /**
   * The new row count.
   */
  private int newRowCount;

  /**
   * The previous row count.
   */
  private int oldRowCount;

  /**
   * Construct a new {@link RowCountChangeEvent}.
   * 
   * @param oldRowCount the previous page
   * @param newRowCount the page that was requested
   */
  public RowCountChangeEvent(int oldRowCount, int newRowCount) {
    this.oldRowCount = oldRowCount;
    this.newRowCount = newRowCount;
  }

  /**
   * @return the new row count
   */
  public int getNewRowCount() {
    return newRowCount;
  }

  /**
   * @return the old row count
   */
  public int getOldRowCount() {
    return oldRowCount;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
