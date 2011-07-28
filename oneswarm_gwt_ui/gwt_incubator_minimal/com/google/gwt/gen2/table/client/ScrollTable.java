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

import com.google.gwt.gen2.table.client.ColumnResizer.ColumnWidthInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A static version of the {@link AbstractScrollTable} that combines a header,
 * data, and footer section.
 */
public class ScrollTable extends AbstractScrollTable {
  /**
   * A mapping of column indexes to their associated {@link ColumnWidthInfo}.
   */
  private Map<Integer, ColumnWidthInfo> columnWidthInfos = new HashMap<Integer, ColumnWidthInfo>();

  /**
   * A set of untruncatable column indexes.
   */
  private Set<Integer> untruncatableColumns = new HashSet<Integer>();

  /**
   * A set of untruncatable footer column indexes.
   */
  private Set<Integer> untruncatableFooters = new HashSet<Integer>();

  /**
   * A set of untruncatable header column indexes.
   */
  private Set<Integer> untruncatableHeaders = new HashSet<Integer>();

  /**
   * A set of unsortable column indexes.
   */
  private Set<Integer> unsortableColumns = new HashSet<Integer>();

  /**
   * Construct a new {@link ScrollTable}.
   * 
   * @param dataTable the data table
   * @param headerTable the header table
   */
  public ScrollTable(FixedWidthGrid dataTable, FixedWidthFlexTable headerTable) {
    super(dataTable, headerTable);
  }

  /**
   * Construct a new {@link ScrollTable}.
   * 
   * @param dataTable the data table
   * @param headerTable the header table
   * @param images the images to use in the table
   */
  public ScrollTable(FixedWidthGrid dataTable, FixedWidthFlexTable headerTable,
      ScrollTableImages images) {
    super(dataTable, headerTable, images);
  }

  @Override
  public int getMaximumColumnWidth(int column) {
    return getColumnWidthInfo(column).getMaximumWidth();
  }

  @Override
  public int getMinimumColumnWidth(int column) {
    return getColumnWidthInfo(column).getMinimumWidth();
  }

  @Override
  public int getPreferredColumnWidth(int column) {
    return getColumnWidthInfo(column).getPreferredWidth();
  }

  @Override
  public boolean isColumnSortable(int column) {
    return (getSortPolicy() != SortPolicy.DISABLED && !unsortableColumns.contains(column));
  }

  @Override
  public boolean isColumnTruncatable(int column) {
    return !untruncatableColumns.contains(column);
  }

  @Override
  public boolean isFooterColumnTruncatable(int column) {
    return !untruncatableFooters.contains(column);
  }

  @Override
  public boolean isHeaderColumnTruncatable(int column) {
    return !untruncatableHeaders.contains(column);
  }

  /**
   * Enable or disable sorting on a specific column. All columns are sortable by
   * default. Use {@link #setSortPolicy(SortPolicy)} to disable sorting on all
   * columns.
   * 
   * @param column the index of the column
   * @param sortable true to enable sorting for this column, false to disable
   */
  public void setColumnSortable(int column, boolean sortable) {
    if (sortable) {
      unsortableColumns.remove(column);
    } else {
      unsortableColumns.add(column);
    }
  }

  /**
   * Enable or disable truncation on a specific column. When enabled, the column
   * width will be adjusted to fit the content. All columns are truncatable by
   * default.
   * 
   * @param column the index of the column
   * @param truncatable true to enable truncation, false to disable
   */
  public void setColumnTruncatable(int column, boolean truncatable) {
    if (truncatable) {
      untruncatableColumns.remove(column);
    } else {
      untruncatableColumns.add(column);
    }
  }

  /**
   * Enable or disable truncation on a specific footer column. When enabled, the
   * column width will be adjusted to fit the content. All columns are
   * truncatable by default.
   * 
   * @param column the index of the column
   * @param truncatable true to enable truncation, false to disable
   */
  public void setFooterColumnTruncatable(int column, boolean truncatable) {
    if (truncatable) {
      untruncatableFooters.remove(column);
    } else {
      untruncatableFooters.add(column);
    }
  }

  /**
   * Enable or disable truncation on a specific header column. When enabled, the
   * column width will be adjusted to fit the content. All columns are
   * truncatable by default.
   * 
   * @param column the index of the column
   * @param truncatable true to enable truncation, false to disable
   */
  public void setHeaderColumnTruncatable(int column, boolean truncatable) {
    if (truncatable) {
      untruncatableHeaders.remove(column);
    } else {
      untruncatableHeaders.add(column);
    }
  }

  /**
   * Set the maximum width of the column.
   * 
   * @param column the column index
   * @param maxWidth the maximum width
   */
  public void setMaximumColumnWidth(int column, int maxWidth) {
    getColumnWidthInfo(column).setMaximumWidth(maxWidth);
  }

  /**
   * Set the minimum width of the column.
   * 
   * @param column the column index
   * @param minWidth the minimum width
   */
  public void setMinimumColumnWidth(int column, int minWidth) {
    minWidth = Math.max(minWidth, FixedWidthGrid.MIN_COLUMN_WIDTH);
    getColumnWidthInfo(column).setMinimumWidth(minWidth);
  }

  /**
   * Set the preferred width of a column. The table will attempt maintain the
   * preferred width of the column. If it cannot, the preferred widths will
   * serve as relative weights when distributing available width.
   * 
   * @param column the column index
   * @param preferredWidth the preferred width
   */
  public void setPreferredColumnWidth(int column, int preferredWidth) {
    getColumnWidthInfo(column).setPreferredWidth(preferredWidth);
  }

  /**
   * Get info about the width of a column.
   * 
   * @param column the column index
   * @return the info about the column width
   */
  private ColumnWidthInfo getColumnWidthInfo(int column) {
    int curWidth = getColumnWidth(column);
    ColumnWidthInfo info = columnWidthInfos.get(new Integer(column));
    if (info == null) {
      info = new ColumnWidthInfo(FixedWidthGrid.MIN_COLUMN_WIDTH, -1,
          FixedWidthGrid.DEFAULT_COLUMN_WIDTH, curWidth);
      columnWidthInfos.put(new Integer(column), info);
    } else {
      info.setCurrentWidth(curWidth);
    }
    return info;
  }
}
