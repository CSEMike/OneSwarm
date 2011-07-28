/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.gen2.table.override.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * This class should replace the actual class of the same name.
 * 
 * TODO: Incorporate changes into actual class.
 */
public class FlexTable extends HTMLTable {

  /**
   * FlexTable-specific implementation of {@link HTMLTable.CellFormatter}. The
   * formatter retrieved from {@link HTMLTable#getCellFormatter()} may be cast
   * to this class.
   */
  public class FlexCellFormatter extends CellFormatter {

    /**
     * Gets the column span for the given cell. This is the number of logical
     * columns covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's column span
     * @throws IndexOutOfBoundsException
     */
    public int getColSpan(int row, int column) {
      return DOM.getElementPropertyInt(getElement(row, column), "colSpan");
    }

    /**
     * Gets the row span for the given cell. This is the number of logical rows
     * covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's row span
     * @throws IndexOutOfBoundsException
     */
    public int getRowSpan(int row, int column) {
      return DOM.getElementPropertyInt(getElement(row, column), "rowSpan");
    }

    /**
     * Sets the column span for the given cell. This is the number of logical
     * columns covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param colSpan the cell's column span
     * @throws IndexOutOfBoundsException
     */
    public void setColSpan(int row, int column, int colSpan) {
      DOM.setElementPropertyInt(ensureElement(row, column), "colSpan", colSpan);
    }

    /**
     * Sets the row span for the given cell. This is the number of logical rows
     * covered by the cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param rowSpan the cell's row span
     * @throws IndexOutOfBoundsException
     */
    public void setRowSpan(int row, int column, int rowSpan) {
      DOM.setElementPropertyInt(ensureElement(row, column), "rowSpan", rowSpan);
    }
  }

  /**
   * Add cells to the specified row.
   * 
   * @param table the table to affect
   * @param row the row to affect
   * @param num the number of cells to add
   */
  private static native void addCells(Element table, int row, int num)/*-{
        var rowElem = table.rows[row];
        for(var i = 0; i < num; i++){
          var cell = $doc.createElement("td");
          cell['colSpan'] = 1;
          cell['rowSpan'] = 1;
          rowElem.appendChild(cell);  
        }
      }-*/;

  public FlexTable() {
    super();
    setCellFormatter(new FlexCellFormatter());
    setRowFormatter(new RowFormatter());
    setColumnFormatter(new ColumnFormatter());
  }

  /**
   * Appends a cell to the specified row.
   * 
   * @param row the row to which the new cell will be added
   * @throws IndexOutOfBoundsException
   */
  public void addCell(int row) {
    insertCell(row, getCellCount(row));
  }

  /**
   * Gets the number of cells on a given row.
   * 
   * @param row the row whose cells are to be counted
   * @return the number of cells present
   * @throws IndexOutOfBoundsException
   */
  @Override
  public int getCellCount(int row) {
    checkRowBounds(row);
    return getDOMCellCount(row);
  }

  /**
   * Gets the overall column index of this cell as if this table were a grid, as
   * determined by the rowspan and colspan of other cells in the table.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's column index
   * @throws IndexOutOfBoundsException
   */
  public int getColumnIndex(int row, int column) {
    checkCellBounds(row, column);
    return getRawColumnIndex(row, column);
  }

  /**
   * Explicitly gets the {@link FlexCellFormatter}. The results of
   * {@link HTMLTable#getCellFormatter()} may also be downcast to a
   * {@link FlexCellFormatter}.
   * 
   * @return the FlexTable's cell formatter
   */
  public FlexCellFormatter getFlexCellFormatter() {
    return (FlexCellFormatter) getCellFormatter();
  }

  /**
   * Gets the number of rows.
   * 
   * @return number of rows
   */
  @Override
  public int getRowCount() {
    return getDOMRowCount();
  }

  /**
   * Inserts a cell into the FlexTable.
   * 
   * @param beforeRow the cell's row
   * @param beforeColumn the cell's column
   * @return the element
   */
  @Override
  public Element insertCell(int beforeRow, int beforeColumn) {
    return super.insertCell(beforeRow, beforeColumn);
  }

  /**
   * Inserts a row into the FlexTable.
   * 
   * @param beforeRow the row to insert
   */
  @Override
  public int insertRow(int beforeRow) {
    return super.insertRow(beforeRow);
  }

  /**
   * Removes the specified cell from the table.
   * 
   * @param row the row of the cell to remove
   * @param column the column of cell to remove
   * @throws IndexOutOfBoundsException
   */
  @Override
  public void removeCell(int row, int column) {
    super.removeCell(row, column);
  }

  /**
   * Removes a number of cells from a row in the table.
   * 
   * @param row the row of the cells to be removed
   * @param column the column of the first cell to be removed
   * @param num the number of cells to be removed
   * @throws IndexOutOfBoundsException
   */
  public void removeCells(int row, int column, int num) {
    for (int i = 0; i < num; i++) {
      removeCell(row, column);
    }
  }

  @Override
  public void removeRow(int row) {
    super.removeRow(row);
  }

  /**
   * Add cells to the specified row.
   * 
   * @param row the row to affect
   * @param num the number of cells to add
   */
  protected void addCells(int row, int num) {
    addCells(getBodyElement(), row, num);
  }

  /**
   * Ensure that the cell exists.
   * 
   * @param row the row to prepare.
   * @param column the column to prepare.
   * @throws IndexOutOfBoundsException if the row is negative
   */
  @Override
  protected void prepareCell(int row, int column) {
    prepareRow(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot create a column with a negative index: " + column);
    }

    // Ensure that the requested column exists.
    int cellCount = getCellCount(row);
    int required = column + 1 - cellCount;
    if (required > 0) {
      addCells(row, required);
    }
  }

  /**
   * Ensure that the row exists.
   * 
   * @param row The row to prepare.
   * @throws IndexOutOfBoundsException if the row is negative
   */
  @Override
  protected void prepareRow(int row) {
    if (row < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot create a row with a negative index: " + row);
    }

    // Ensure that the requested row exists.
    int rowCount = getRowCount();
    for (int i = rowCount; i <= row; i++) {
      insertRow(i);
    }
  }

  /**
   * Gets the overall column index of this cell as if this table were a grid, as
   * determined by the rowspan and colspan of other cells in the table. This
   * helper method doesn't check the bounds on every recursive call.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's column index
   * @throws IndexOutOfBoundsException
   */
  private int getRawColumnIndex(int row, int column) {
    // Get cells before me in my row
    FlexCellFormatter formatter = getFlexCellFormatter();
    int columnIndex = 0;
    for (int curCell = 0; curCell < column; curCell++) {
      columnIndex += formatter.getColSpan(row, curCell);
    }

    // Get cells that span down into my row
    int numCells = 0;
    for (int curRow = 0; curRow < row; curRow++) {
      numCells = getCellCount(curRow);
      for (int curCell = 0; curCell < numCells; curCell++) {
        if ((curRow + formatter.getRowSpan(curRow, curCell) - 1) >= row) {
          if (getRawColumnIndex(curRow, curCell) <= columnIndex) {
            columnIndex += formatter.getColSpan(curRow, curCell);
          }
        }
      }
    }

    return columnIndex;
  }
}
