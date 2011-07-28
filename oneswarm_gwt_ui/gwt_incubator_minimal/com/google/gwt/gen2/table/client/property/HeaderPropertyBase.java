/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.gen2.table.client.property;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class that allows for headers or footers on a column. The 0th index
 * should always refer to the row closest to the data table.
 */
class HeaderPropertyBase extends ColumnProperty {
  private int headerCount;
  private Map<Integer, Object> headers = new HashMap<Integer, Object>();
  private boolean isDynamic;

  /**
   * If the headers are dymanic, they will be re-rendered on every page load.
   * Set this to true if your headers depend on the data.
   * 
   * @return true if the headers are dynamic.
   */
  public boolean isDynamic() {
    return isDynamic;
  }

  /**
   * Set whether or not the headers are dynamically generated and should be
   * refreshed on every page load.
   * 
   * @param isDynamic true if the headers are dynamic
   */
  public void setDynamic(boolean isDynamic) {
    this.isDynamic = isDynamic;
  }

  /**
   * Get the header at the given row index.
   * 
   * @param row the row index from the bottom
   * @return the header for the given row
   */
  Object getHeader(int row) {
    return headers.get(new Integer(row));
  }

  /**
   * Get the header at the given row and column index. Override this method if
   * your header includes dynamic content that depends on the column index.
   * 
   * @param row the row index from the bottom
   * @param column the column index at runtime
   * @return the header for the given row
   */
  Object getHeader(int row, int column) {
    return getHeader(row);
  }

  /**
   * @return get the number of headers above the column
   */
  int getHeaderCount() {
    return headerCount;
  }

  /**
   * Remove the header above this column for the specified row.
   * 
   * @param row the row index from the bottom
   */
  void removeHeader(int row) {
    headers.remove(new Integer(row));
  }

  /**
   * Set the header above this column. The row index starts with the bottom row,
   * which is reverse of a normal table. The headerCount will automatically be
   * increased to accommodate the row.
   * 
   * @param row the row index from the bottom
   * @param header the header
   */
  void setHeader(int row, Object header) {
    headers.put(new Integer(row), header);
    headerCount = Math.max(headerCount, row + 1);
  }

  /**
   * Set the number of headers above the column.
   * 
   * @param headerCount the number of headers
   */
  void setHeaderCount(int headerCount) {
    this.headerCount = headerCount;
  }
}
