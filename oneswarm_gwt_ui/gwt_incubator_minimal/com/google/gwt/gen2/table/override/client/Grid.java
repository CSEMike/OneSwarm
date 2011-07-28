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
public class Grid extends HTMLTable {
  /**
   * Number of columns in the current grid.
   */
  protected int numColumns;

  /**
   * Number of rows in the current grid.
   */
  protected int numRows;

  /**
   * Constructor for <code>Grid</code>.
   */
  public Grid() {
    super();
    setClearText("&nbsp;");
    setCellFormatter(new CellFormatter());
    setRowFormatter(new RowFormatter());
    setColumnFormatter(new ColumnFormatter());
  }

  /**
   * Constructs a grid with the requested size.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public Grid(int rows, int columns) {
    this();
    resize(rows, columns);
  }

  /**
   * Return number of columns. For grid, row argument is ignored as all grids
   * are rectangular.
   */
  @Override
  public int getCellCount(int row) {
    return numColumns;
  }

  /**
   * Gets the number of columns in this grid.
   * 
   * @return the number of columns
   */
  public int getColumnCount() {
    return numColumns;
  }

  /**
   * Return number of rows.
   */
  @Override
  public int getRowCount() {
    return numRows;
  }

  /**
   * Inserts a new row into the table. If you want to add multiple rows at once,
   * use {@link #resize(int, int)} or {@link #resizeRows(int)} as they are more
   * efficient.
   * 
   * @param beforeRow the index before which the new row will be inserted
   * @return the index of the newly-created row
   * @throws IndexOutOfBoundsException
   */
  @Override
  public int insertRow(int beforeRow) {
    int index = super.insertRow(beforeRow);
    numRows++;
    return index;
  }

  @Override
  public void removeRow(int row) {
    super.removeRow(row);
    numRows--;
  }

  /**
   * Resizes the grid.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public void resize(int rows, int columns) {
    resizeColumns(columns);
    resizeRows(rows);
  }

  /**
   * Resizes the grid to the specified number of columns.
   * 
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public void resizeColumns(int columns) {
    if (numColumns == columns) {
      return;
    }
    if (columns < 0) {
      throw new IndexOutOfBoundsException("Cannot set number of columns to "
          + columns);
    }

    if (numColumns > columns) {
      // Fewer columns. Remove extraneous cells.
      for (int i = 0; i < numRows; i++) {
        for (int j = numColumns - 1; j >= columns; j--) {
          removeCell(i, j);
        }
      }
    } else {
      // More columns. add cells where necessary.
      for (int i = 0; i < numRows; i++) {
        for (int j = numColumns; j < columns; j++) {
          insertCell(i, j);
        }
      }
    }
    numColumns = columns;
  }

  /**
   * Resizes the grid to the specified number of rows.
   * 
   * @param rows the number of rows
   * @throws IndexOutOfBoundsException
   */
  public void resizeRows(int rows) {
    if (numRows == rows) {
      return;
    }
    if (rows < 0) {
      throw new IndexOutOfBoundsException("Cannot set number of rows to "
          + rows);
    }
    if (numRows < rows) {
      Element tr = createRow();
      getBodyElement().appendChild(tr);
      for (int i = numRows + 1; i < rows; i++) {
        getBodyElement().appendChild(tr.cloneNode(true));
      }
      numRows = rows;
    } else {
      while (numRows > rows) {
        // Fewer rows. Remove extraneous ones.
        removeRow(numRows - 1);
      }
    }
  }

  /**
   * Creates a new, empty cell.
   */
  @Override
  protected Element createCell() {
    Element td = super.createCell();

    // Add a non-breaking space to the TD. This ensures that the cell is
    // displayed.
    DOM.setInnerHTML(td, "&nbsp;");
    return td;
  }

  @Override
  protected Element createRow() {
    Element tr = super.createRow();
    for (int i = 0; i < numColumns; i++) {
      tr.appendChild(createCell());
    }   
    return tr;
  }

  /**
   * Checks that a cell is a valid cell in the table.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  @Override
  protected void prepareCell(int row, int column) {
    // Ensure that the indices are not negative.
    prepareRow(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    }

    if (column >= numColumns) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + numColumns);
    }
  }

  /**
   * Checks that the column index is valid.
   * 
   * @param column The column index to be checked
   * @throws IndexOutOfBoundsException if the column is negative
   */
  @Override
  protected void prepareColumn(int column) {
    // Ensure that the indices are not negative.
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    }

    /**
     * Grid does not lazily create cells, so simply ensure that the requested
     * column and column are valid
     */
    if (column >= numColumns) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + numColumns);
    }
  }

  /**
   * Checks that the row index is valid.
   * 
   * @param row The row index to be checked
   * @throws IndexOutOfBoundsException if the row is negative
   */
  @Override
  protected void prepareRow(int row) {
    // Ensure that the indices are not negative.
    if (row < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a row with a negative index: " + row);
    }

    /**
     * Grid does not lazily create cells, so simply ensure that the requested
     * row and column are valid
     */
    if (row >= numRows) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: "
          + numRows);
    }
  }
}
