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

/**
 * Handler interface for all Table events.
 */
public interface TableEvent {
  /**
   * Information about the cell that is being highlighted.
   */
  public static class Cell {
    private int cellIndex;
    private int rowIndex;

    /**
     * Construct a new Cell.
     * 
     * @param rowIndex the index of the highlighted row
     * @param cellIndex the index of the highlighted cell
     */
    public Cell(int rowIndex, int cellIndex) {
      this.cellIndex = cellIndex;
      this.rowIndex = rowIndex;
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
  }
  
  /**
   * Information about the row that is being highlighted.
   */
  public static class Row implements Comparable<Row> {
    private int rowIndex;

    /**
     * Construct a new Row.
     * 
     * @param rowIndex the index of the highlighted row
     */
    public Row(int rowIndex) {
      this.rowIndex = rowIndex;
    }

    public int compareTo(Row o) {
      if (o == null) {
        return 1;
      } else {
        return rowIndex - o.getRowIndex();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Row) {
        return compareTo((Row) o) == 0;
      }
      return false;
    }

    /**
     * @return the row index
     */
    public int getRowIndex() {
      return rowIndex;
    }
  }
}
