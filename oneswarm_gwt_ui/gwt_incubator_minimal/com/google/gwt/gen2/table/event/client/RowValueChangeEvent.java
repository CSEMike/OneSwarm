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
 * 
 * @param <RowType> the data type of the row values
 */
public class RowValueChangeEvent<RowType> extends AbstractEvent {
  /**
   * Event Key for {@link RowValueChangeEvent}.
   */
  public static final Type<RowValueChangeEvent, RowValueChangeHandler> TYPE = new Type<RowValueChangeEvent, RowValueChangeHandler>() {
    @Override
    protected void fire(RowValueChangeHandler handler, RowValueChangeEvent event) {
      handler.onRowValueChange(event);
    }
  };

  /**
   * The new row value.
   */
  private RowType rowValue;

  /**
   * The index of the row.
   */
  private int rowIndex;

  /**
   * Construct a new {@link RowValueChangeEvent}.
   * 
   * @param rowIndex the index of the removed row
   * @param rowValue the new row value
   */
  public RowValueChangeEvent(int rowIndex, RowType rowValue) {
    this.rowIndex = rowIndex;
    this.rowValue = rowValue;
  }

  /**
   * @return the index of the row
   */
  public int getRowIndex() {
    return rowIndex;
  }

  /**
   * @return the row value
   */
  public RowType getRowValue() {
    return rowValue;
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
