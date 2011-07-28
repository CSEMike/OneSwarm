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

import com.google.gwt.gen2.table.override.client.HTMLTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.Iterator;

/**
 * Helper class to bulk load {@link FixedWidthGrid} tables.
 * 
 * @param <RowType> the data type of the row values
 */
public class FixedWidthGridBulkRenderer<RowType> extends
    SelectionGridBulkRenderer<RowType> {
  /**
   * Constructor. Takes in the number of columns in the table to allow efficient
   * creation of the header row.
   * 
   * @param grid {@link FixedWidthGrid} to be be bulk rendered
   * @param tableDef the table definition that should be used during rendering
   */
  public FixedWidthGridBulkRenderer(FixedWidthGrid grid,
      TableDefinition<RowType> tableDef) {
    super(grid, tableDef);
  }

  /**
   * Constructor. Takes in the number of columns in the table to allow efficient
   * creation of the header row.
   * 
   * @param grid {@link FixedWidthGrid} to be be bulk rendered
   * @param sourceTableDef the external source of the table definition
   */
  public FixedWidthGridBulkRenderer(FixedWidthGrid grid,
      HasTableDefinition<RowType> sourceTableDef) {
    super(grid, sourceTableDef);
  }

  /**
   * Gets the new ghost element from the table.
   * 
   * @param table the table
   * @return the new ghost row
   */
  protected native Element getBulkLoadedGhostRow(HTMLTable table)
  /*-{
    return table.@com.google.gwt.gen2.table.override.client.HTMLTable::getBodyElement()(table).rows[0];
  }-*/;

  @Override
  protected void renderRows(Iterator<RowType> iterator,
      final RenderingOptions options) {
    FixedWidthGrid table = (FixedWidthGrid) super.getTable();
    options.headerRow = DOM.toString(table.getGhostRow());
    super.renderRows(iterator, options);
  }

  @Override
  protected void renderRows(String rawHTMLTable) {
    super.renderRows(rawHTMLTable);

    // Update the ghost row variable after the num columns has been set
    Element newGhostRow = getBulkLoadedGhostRow(getTable());
    FixedWidthGrid grid = (FixedWidthGrid) getTable();
    grid.setGhostRow(newGhostRow);
    grid.updateGhostRow();
  }
}
