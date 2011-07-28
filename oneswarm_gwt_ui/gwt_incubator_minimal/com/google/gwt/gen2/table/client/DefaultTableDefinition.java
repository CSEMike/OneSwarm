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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A collection of {@link ColumnDefinition ColumnDefinitions} that define a
 * table.
 * 
 * @param <RowType> the type of the row values
 */
public class DefaultTableDefinition<RowType> implements
    TableDefinition<RowType> {
  /**
   * The default {@link RowRenderer} to use when the
   * {@link DefaultTableDefinition} does not specify one.
   */
  private static final RowRenderer DEFAULT_ROW_RENDERER = new DefaultRowRenderer();

  /**
   * The ordered list of {@link ColumnDefinition ColumnDefinitions} used by this
   * renderer.
   */
  private List<ColumnDefinition<RowType, ?>> columnDefs;

  /**
   * A list of visible columns.
   */
  private Set<ColumnDefinition<RowType, ?>> hiddenColumnDefs;

  /**
   * The renderer used to render rows.
   */
  private RowRenderer<RowType> rowRenderer = DEFAULT_ROW_RENDERER;

  /**
   * Create a new {@link TableDefinition}.
   */
  public DefaultTableDefinition() {
    this(new ArrayList<ColumnDefinition<RowType, ?>>());
  }

  /**
   * Create a new {@link TableDefinition} with a list of
   * {@link ColumnDefinition ColumnDefinitions}.
   * 
   * @param columnDefs the {@link ColumnDefinition ColumnDefinitions} to render
   */
  public DefaultTableDefinition(List<ColumnDefinition<RowType, ?>> columnDefs) {
    this.columnDefs = columnDefs;
    hiddenColumnDefs = new HashSet<ColumnDefinition<RowType, ?>>();
  }

  /**
   * Add a {@link ColumnDefinition}.
   * 
   * @param columnDef the {@link ColumnDefinition} to add
   */
  public void addColumnDefinition(ColumnDefinition<RowType, ?> columnDef) {
    columnDefs.add(columnDef);
  }

  /**
   * Insert a {@link ColumnDefinition} at a specific index.
   * 
   * @param index the index to place the {@link ColumnDefinition}
   * @param columnDef the {@link ColumnDefinition} to add
   */
  public void addColumnDefinition(int index,
      ColumnDefinition<RowType, ?> columnDef) {
    columnDefs.add(index, columnDef);
  }

  /**
   * Get the {@link ColumnDefinition} for a given column.
   * 
   * @param column the column index
   * @return the {@link ColumnDefinition} for the column
   * @throws IndexOutOfBoundsException if the column is not defined
   */
  public ColumnDefinition<RowType, ?> getColumnDefinition(int column)
      throws IndexOutOfBoundsException {
    return columnDefs.get(column);
  }

  /**
   * @return the number of {@link ColumnDefinition ColumnDefinitions}.
   */
  public int getColumnDefinitionCount() {
    return columnDefs.size();
  }

  public RowRenderer<RowType> getRowRenderer() {
    return rowRenderer;
  }

  public List<ColumnDefinition<RowType, ?>> getVisibleColumnDefinitions() {
    List<ColumnDefinition<RowType, ?>> visibleColumns = new ArrayList<ColumnDefinition<RowType, ?>>();
    for (ColumnDefinition<RowType, ?> columnDef : columnDefs) {
      if (isColumnVisible(columnDef)) {
        visibleColumns.add(columnDef);
      }
    }
    return visibleColumns;
  }

  /**
   * Check if a column is visible or not.
   * 
   * @param colDef the {@link ColumnDefinition}
   * @return true if visible, false if hidden
   */
  public boolean isColumnVisible(ColumnDefinition<RowType, ?> colDef) {
    return !hiddenColumnDefs.contains(colDef);
  }

  /**
   * Remove a {@link ColumnDefinition}.
   * 
   * @param columnDef the {@link ColumnDefinition} to remove
   */
  public void removeColumnDefinition(ColumnDefinition<RowType, ?> columnDef) {
    columnDefs.remove(columnDef);
  }

  public void renderRows(int startRowIndex, Iterator<RowType> rowValues,
      AbstractRowView<RowType> view) {
    List<ColumnDefinition<RowType, ?>> visibleColumns = getVisibleColumnDefinitions();
    view.renderRowsImpl(startRowIndex, rowValues, rowRenderer, visibleColumns);
  }

  /**
   * Hide or show a column.
   * 
   * @param colDef the {@link ColumnDefinition}
   * @param visible true to show it, false to hide
   */
  public void setColumnVisible(ColumnDefinition<RowType, ?> colDef,
      boolean visible) {
    if (visible) {
      hiddenColumnDefs.remove(colDef);
    } else {
      hiddenColumnDefs.add(colDef);
    }
  }

  /**
   * Set the {@link RowRenderer} used to render rows.
   */
  public void setRowRenderer(RowRenderer<RowType> rowRenderer) {
    assert rowRenderer != null : "rowRenderer cannot be null";
    this.rowRenderer = rowRenderer;
  }
}
