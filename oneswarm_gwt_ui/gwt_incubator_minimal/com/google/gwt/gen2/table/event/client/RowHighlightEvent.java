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
 * Logical event fired when a row is highlighted.
 */
public class RowHighlightEvent extends HighlightEvent<Row> {
  /**
   * Event Key for {@link RowHighlightEvent}.
   */
  public static final Type<RowHighlightEvent, RowHighlightHandler> TYPE = new Type<RowHighlightEvent, RowHighlightHandler>() {
    @Override
    protected void fire(RowHighlightHandler handler, RowHighlightEvent event) {
      handler.onRowHighlight(event);
    }
  };

  /**
   * Construct a new {@link RowHighlightEvent}.
   * 
   * @param rowIndex the index of the highlighted row
   */
  public RowHighlightEvent(int rowIndex) {
    this(new Row(rowIndex));
  }

  /**
   * Construct a new {@link RowHighlightEvent}.
   * 
   * @param row the row being highlighted
   */
  public RowHighlightEvent(Row row) {
    super(row);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
