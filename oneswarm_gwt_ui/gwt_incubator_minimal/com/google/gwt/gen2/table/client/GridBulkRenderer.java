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
package com.google.gwt.gen2.table.client;

import com.google.gwt.gen2.table.override.client.Grid;

/**
 * Allows bulk rendering of {@link Grid}s.
 * <p>
 * Must use the {@link Grid} in the overrides package.
 * </p>
 * 
 * @param <RowType> the data type of the row values
 */
public class GridBulkRenderer<RowType> extends TableBulkRenderer<RowType> {
  /**
   * Construct a new {@link GridBulkRenderer}.
   * 
   * @param grid {@link Grid} to be be bulk rendered
   * @param tableDef the table definition that should be used during rendering
   */
  public GridBulkRenderer(Grid grid, TableDefinition<RowType> tableDef) {
    super(grid, tableDef);
    init(grid);
  }

  /**
   * Construct a new {@link GridBulkRenderer}.
   * 
   * @param grid {@link Grid} to be be bulk rendered
   * @param sourceTableDef the external source of the table definition
   */
  public GridBulkRenderer(Grid grid, HasTableDefinition<RowType> sourceTableDef) {
    super(grid, sourceTableDef);
    init(grid);
  }

  @Override
  protected void renderRows(String rawHTMLTable) {
    super.renderRows(rawHTMLTable);
    setGridDimensions((Grid) getTable());
  }

  /**
   * Short term hack to set protected row and columns.
   */
  native void setGridDimensions(Grid table) /*-{
    var numRows =  table.@com.google.gwt.gen2.table.override.client.HTMLTable::getDOMRowCount()();
    table.@com.google.gwt.gen2.table.override.client.Grid::numRows = numRows;
    var cellCount = 0;
    if (numRows > 0) {
      cellCount =
        table.@com.google.gwt.gen2.table.override.client.HTMLTable::getDOMCellCount(I)(0);
    }
    table.@com.google.gwt.gen2.table.override.client.Grid::numColumns = cellCount;
  }-*/;

  private void init(Grid grid) {
    if (grid instanceof FixedWidthGrid
        && (!(this instanceof FixedWidthGridBulkRenderer))) {
      throw new UnsupportedOperationException(
          "Must use a FixedWidthGridBulkLoader to bulk load a fixed grid");
    }
    if (grid instanceof SelectionGrid
        && (!(this instanceof SelectionGridBulkRenderer))) {
      throw new UnsupportedOperationException(
          "Must use a SelectionGridBulkLoader to bulk load a selection grid");
    }
  }
}
