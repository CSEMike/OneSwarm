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

/**
 * Cell editors provide a mechanism to edit cells.
 * 
 * @param <ColType> the data type of the column
 */
public interface CellEditor<ColType> {
  /**
   * Callback for {@link CellEditor}. The callback will be used when the user
   * finishes editing the cell.
   * 
   * @param <ColType> the data type of the column
   */
  public static interface Callback<ColType> {
    /**
     * Use this callback to return a new row value to the table.
     * 
     * @param cellEditInfo information about the source of the edit request
     * @param cellValue the new value to associated with the cell
     */
    void onComplete(CellEditInfo cellEditInfo, ColType cellValue);

    /**
     * Use this callback to cancel the edit request.
     * 
     * @param cellEditInfo information about the source of the edit request
     */
    void onCancel(CellEditInfo cellEditInfo);
  }

  /**
   * The information about the cell to edit.
   */
  public static class CellEditInfo {
    /**
     * The cell index.
     */
    private int cellIndex;

    /**
     * The row index.
     */
    private int rowIndex;

    /**
     * The table that triggered the editor.
     */
    private HTMLTable table;

    /**
     * Construct a new {@link CellEditInfo}.
     * 
     * @param table the table that opened the editor
     * @param rowIndex the row index
     * @param cellIndex the cell index
     */
    public CellEditInfo(HTMLTable table, int rowIndex, int cellIndex) {
      this.table = table;
      this.rowIndex = rowIndex;
      this.cellIndex = cellIndex;
    }

    /**
     * @return the cell index
     */
    public int getCellIndex() {
      return cellIndex;
    }

    /**
     * @return the row index
     */
    public int getRowIndex() {
      return rowIndex;
    }

    /**
     * @return the table that opened the editor
     */
    public HTMLTable getTable() {
      return table;
    }
  }

  /**
   * Handle a request to edit a cell.
   * 
   * @param cellEditInfo information about the source of the edit request
   * @param cellValue the value in the cell to edit
   * @param callback callback used when editing is complete
   */
  void editCell(CellEditInfo cellEditInfo, ColType cellValue,
      Callback<ColType> callback);
}
