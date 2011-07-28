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

import com.google.gwt.gen2.event.logical.shared.HighlightEvent;
import com.google.gwt.gen2.table.event.client.TableEvent.Row;

/**
 * Logical event fired when a row is unhighlighted.
 */
public class RowUnhighlightEvent extends HighlightEvent<Row> {
  /**
   * Event Key for {@link RowUnhighlightEvent}.
   */
  public static final Type<RowUnhighlightEvent, RowUnhighlightHandler> TYPE = new Type<RowUnhighlightEvent, RowUnhighlightHandler>() {
    @Override
    protected void fire(RowUnhighlightHandler handler, RowUnhighlightEvent event) {
      handler.onRowUnhighlight(event);
    }
  };

  /**
   * Construct a new {@link RowUnhighlightEvent}.
   * 
   * @param rowIndex the index of the highlighted row
   */
  public RowUnhighlightEvent(int rowIndex) {
    this(new Row(rowIndex));
  }

  /**
   * Construct a new {@link RowUnhighlightEvent}.
   * 
   * @param row the cell being highlighted
   */
  public RowUnhighlightEvent(Row row) {
    super(row);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
