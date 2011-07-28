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

import com.google.gwt.gen2.table.override.client.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PreloadedTable} allow users to configure a table with HTML cell data
 * before rendering the table. This can lead to faster performance for large
 * tables.
 */
public class PreloadedTable extends FlexTable {
  List<List<Object>> rows = new ArrayList<List<Object>>();
  boolean isFilledIn = false;

  /*
   * As often users manipulate a single row at a time, caching the current row
   * and index for speed;
   */
  List<Object> curRow;

  int curIndex = -1;

  /**
   * The maximum number of columns in any row.
   */
  private int maxColumn = 0;

  @Override
  public void setHTML(int row, int column, String html) {
    ensureLoaded();
    super.setHTML(row, column, html);
  }

  /**
   * Sets cells HTML that will be included in the table upon initialization.
   * 
   * @param row row
   * @param column column
   * @param html html to set
   */
  public void setPendingHTML(int row, int column, String html) {
    if (isFilledIn) {
      throw new UnsupportedOperationException(
          "Cannot call setPendingHTML after either setWidget/setText/setHTML has been called or the table has been attached to the DOM");
    }
    List<Object> l = ensureRow(row);
    ensureCell(l, column, html);
    maxColumn = Math.max(maxColumn, column);
  }

  @Override
  public void setText(int row, int column, String text) {
    ensureLoaded();
    super.setText(row, column, text);
  }

  @Override
  public void setWidget(int row, int column, Widget widget) {
    ensureLoaded();
    super.setWidget(row, column, widget);
  }

  @Override
  protected void onLoad() {
    ensureLoaded();
  }

  private void ensureCell(List<Object> row, int column, String value) {
    if (column == row.size()) {
      row.add(value);
    } else {
      for (int i = row.size(); i < column + 1; i++) {
        row.add("");
      }
      row.set(column, value);
    }
  }

  private void ensureLoaded() {
    if (!isFilledIn) {
      isFilledIn = true;
      DefaultTableDefinition<List<Object>> tableDef = new DefaultTableDefinition<List<Object>>();
      for (int i = 0; i < maxColumn; i++) {
        tableDef.addColumnDefinition(new ListColumnDefinition<Object>(i));
      }
      new FlexTableBulkRenderer<List<Object>>(this, tableDef).renderRows(rows,
          null);
    }
  }

  private List<Object> ensureRow(int row) {
    if (row == curIndex) {
      return curRow;
    }
    for (int i = rows.size(); i < row + 1; i++) {
      curRow = new ArrayList<Object>();
      curRow.add("");
      rows.add(curRow);
    }
    curRow = rows.get(row);
    curIndex = row;
    return rows.get(row);
  }
}
