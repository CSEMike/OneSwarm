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
import com.google.gwt.gen2.table.event.client.TableEvent.Cell;

/**
 * Logical event fired when a cell is highlighted.
 */
public class CellHighlightEvent extends HighlightEvent<Cell> {
  /**
   * Event Key for {@link CellHighlightEvent}.
   */
  public static final Type<CellHighlightEvent, CellHighlightHandler> TYPE = new Type<CellHighlightEvent, CellHighlightHandler>() {
    @Override
    protected void fire(CellHighlightHandler handler, CellHighlightEvent event) {
      handler.onCellHighlight(event);
    }
  };

  /**
   * Construct a new {@link CellHighlightEvent}.
   * 
   * @param rowIndex the index of the highlighted row
   * @param cellIndex the index of the highlighted cell
   */
  public CellHighlightEvent(int rowIndex, int cellIndex) {
    this(new Cell(rowIndex, cellIndex));
  }

  /**
   * Construct a new {@link CellHighlightEvent}.
   * 
   * @param cell the cell being highlighted
   */
  public CellHighlightEvent(Cell cell) {
    super(cell);
  }

  @Override
  protected Type getType() {
    return TYPE;
  }
}
