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

import com.google.gwt.gen2.table.client.TableModelHelper.Request;
import com.google.gwt.gen2.table.client.TableModelHelper.Response;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A {@link ClientTableModel} that uses a 2D {@link List} of Objects as its
 * source of data.
 */
public class ListTableModel extends MutableTableModel<List<Object>> {
  /**
   * An {@link Iterator} over the requested rows.
   */
  private class RowIterator implements Iterator<List<Object>> {
    private int curRow;
    private int lastRow;

    public RowIterator(Request request) {
      curRow = request.getStartRow() - 1;
      lastRow = Math.min(rowValues.size() - 1, curRow + request.getNumRows());
    }

    public boolean hasNext() {
      return curRow < lastRow;
    }

    public List<Object> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      curRow++;
      return rowValues.get(new Integer(curRow));
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * The values associated with each cell.
   */
  private List<List<Object>> rowValues;

  /**
   * Construct a new {@link ListTableModel}.
   * 
   * @param rows the data that this model feeds from
   */
  public ListTableModel(List<List<Object>> rows) {
    this.rowValues = rows;
    setRowCount(rows.size());
  }

  @Override
  public void requestRows(Request request, Callback<List<Object>> callback) {
    final RowIterator it = new RowIterator(request);
    Response<List<Object>> response = new Response<List<Object>>() {
      @Override
      public Iterator<List<Object>> getRowValues() {
        return it;
      }
    };
    callback.onRowsReady(request, response);
  }

  @Override
  protected boolean onRowInserted(int beforeRow) {
    if (beforeRow < rowValues.size()) {
      rowValues.add(beforeRow, null);
    }
    return true;
  }

  @Override
  protected boolean onRowRemoved(int row) {
    if (row < rowValues.size()) {
      rowValues.remove(row);
    }
    return true;
  }

  @Override
  protected boolean onSetRowValue(int row, List<Object> rowValue) {
    // Expand to fit row
    for (int i = rowValues.size(); i <= row; i++) {
      rowValues.add(null);
    }

    // Set the new row value
    rowValues.set(row, rowValue);
    return true;
  }

  /**
   * Get the value at a given cell. This is used for testing.
   * 
   * @param rowIndex the index of the row
   * @param cellIndex the index of the cell
   * @return the cell value, or null if it does not exist
   */
  Object getCellValue(int rowIndex, int cellIndex) {
    // Row does not exist
    if (rowIndex >= rowValues.size()) {
      return null;
    }

    // Get the cell value from the row
    List<Object> rowList = rowValues.get(rowIndex);
    if (rowList != null && rowList.size() > cellIndex) {
      return rowList.get(cellIndex);
    }
    return null;
  }
}
