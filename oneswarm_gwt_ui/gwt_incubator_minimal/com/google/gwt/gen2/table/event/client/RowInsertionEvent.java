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
 * Logical event fired when a row is inserted.
 */
public class RowInsertionEvent extends AbstractEvent {
  /**
   * Event Key for {@link RowInsertionEvent}.
   */
  public static final Type<RowInsertionEvent, RowInsertionHandler> TYPE = new Type<RowInsertionEvent, RowInsertionHandler>() {
    @Override
    protected void fire(RowInsertionHandler handler, RowInsertionEvent event) {
      handler.onRowInsertion(event);
    }
  };

  /**
   * The index of the new row.
   */
  private int rowIndex;

  /**
   * Construct a new {@link RowInsertionEvent}.
   * 
   * @param rowIndex the index of the new row
   */
  public RowInsertionEvent(int rowIndex) {
    this.rowIndex = rowIndex;
  }

  /**
   * @return the index of the inserted row
   */
  public int getRowIndex() {
    return rowIndex;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
