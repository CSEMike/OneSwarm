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

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.Iterator;
import java.util.List;

/**
 * A collection of {@link ColumnDefinition ColumnDefinitions} that define a
 * table.
 * 
 * @param <RowType> the type of the row values
 */
public interface TableDefinition<RowType> {
  /**
   * A hook into where the rendered cell will be displayed.
   * 
   * @param <RowType> the type of the row values
   */
  public abstract static class AbstractCellView<RowType> {
    private int cellIndex = 0;
    private int rowIndex = 0;
    private HasTableDefinition<RowType> source;

    /**
     * Construct a new {@link TableDefinition.AbstractCellView}.
     * 
     * @param sourceTableDef the {@link HasTableDefinition} that defined the
     *          cell view
     */
    public AbstractCellView(HasTableDefinition<RowType> sourceTableDef) {
      this.source = sourceTableDef;
    }

    /**
     * @return the actual cell index being rendered
     */
    public int getCellIndex() {
      return cellIndex;
    }

    /**
     * @return the actual row index being rendered
     */
    public int getRowIndex() {
      return rowIndex;
    }

    /**
     * @return the source of the {@link TableDefinition}
     */
    public HasTableDefinition<RowType> getSourceTableDefinition() {
      return source;
    }

    /**
     * Sets the horizontal alignment of the specified cell.
     * 
     * @param align the cell's new horizontal alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasHorizontalAlignment}.
     */
    public abstract void setHorizontalAlignment(
        HorizontalAlignmentConstant align);

    /**
     * Set the html string in the current cell.
     * 
     * @param html the html string to set
     */
    public abstract void setHTML(String html);

    /**
     * Sets an attribute on the cell's style.
     * 
     * @param attr the name of the style attribute to be set
     * @param value the style attribute's new value
     */
    public abstract void setStyleAttribute(String attr, String value);

    /**
     * Set the style name of the cell, which can be a space delimited list of
     * style names.
     */
    public abstract void setStyleName(String stylename);

    /**
     * Set the text string in the current cell.
     * 
     * @param text the text string to set
     */
    public abstract void setText(String text);

    /**
     * Sets the vertical alignment of the specified cell.
     * 
     * @param align the cell's new vertical alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasVerticalAlignment}.
     */
    public abstract void setVerticalAlignment(VerticalAlignmentConstant align);

    /**
     * Set the {@link Widget} in the current cell.
     * 
     * @param widget the widget to set
     */
    public abstract void setWidget(Widget widget);

    /**
     * Render a single cell.
     * 
     * @param rowIndex the index of the row
     * @param cellIndex the index of the cell
     * @param rowValue the row value associated with the row
     * @param columnDef the {@link ColumnDefinition} associated with the column
     */
    protected void renderCellImpl(int rowIndex, int cellIndex,
        RowType rowValue, ColumnDefinition<RowType, ?> columnDef) {
      this.rowIndex = rowIndex;
      this.cellIndex = cellIndex;
      renderRowValue(rowValue, columnDef);
    }

    /**
     * Render a row value into a cell.
     * 
     * @param rowValue the row value associated with the row
     * @param columnDef the {@link ColumnDefinition} associated with the column
     */
    protected void renderRowValue(RowType rowValue, ColumnDefinition columnDef) {
      columnDef.getCellRenderer().renderRowValue(rowValue, columnDef, this);
    }
  }

  /**
   * A hook into where the rendered row will be displayed.
   * 
   * @param <RowType> the type of the row values
   */
  public abstract static class AbstractRowView<RowType> {
    private int rowIndex = 0;
    private AbstractCellView<RowType> cellView;

    /**
     * Construct a new {@link TableDefinition.AbstractRowView}.
     * 
     * @param cellView the view of the cell
     */
    public AbstractRowView(AbstractCellView<RowType> cellView) {
      this.cellView = cellView;
    }

    /**
     * @return the actual row index being rendered
     */
    public int getRowIndex() {
      return rowIndex;
    }

    /**
     * @return the source of the {@link TableDefinition}
     */
    public HasTableDefinition<RowType> getSourceTableDefinition() {
      return cellView.getSourceTableDefinition();
    }

    /**
     * Sets an attribute on the cell's style.
     * 
     * @param attr the name of the style attribute to be set
     * @param value the style attribute's new value
     */
    public abstract void setStyleAttribute(String attr, String value);

    /**
     * Set the style name of the cell, which can be a space delimited list of
     * style names.
     */
    public abstract void setStyleName(String stylename);

    /**
     * Render all of the cells in a single row.
     * 
     * @param rowIndex the index of the row
     * @param rowValue the row value associated with the row
     * @param rowRenderer the renderer used to render the rows
     * @param visibleColumns the list of visible {@link ColumnDefinition}
     */
    protected void renderRowImpl(int rowIndex, RowType rowValue,
        RowRenderer<RowType> rowRenderer,
        List<ColumnDefinition<RowType, ?>> visibleColumns) {
      this.rowIndex = rowIndex;
      renderRowValue(rowValue, rowRenderer);
      int numColumns = visibleColumns.size();
      for (int i = 0; i < numColumns; i++) {
        cellView.renderCellImpl(rowIndex, i, rowValue, visibleColumns.get(i));
      }
    }

    /**
     * Render the rows in the table given the list of visible
     * {@link ColumnDefinition ColumnDefinitions}.
     * 
     * @param startRowIndex the index of the first row to render
     * @param rowValues the values associated with each row
     * @param rowRenderer the renderer used to render the rows
     * @param visibleColumns the list of visible {@link ColumnDefinition}
     */
    protected void renderRowsImpl(int startRowIndex,
        Iterator<RowType> rowValues, RowRenderer<RowType> rowRenderer,
        List<ColumnDefinition<RowType, ?>> visibleColumns) {
      int curRow = startRowIndex;
      while (rowValues.hasNext()) {
        renderRowImpl(curRow, rowValues.next(), rowRenderer, visibleColumns);
        curRow++;
      }
    }

    /**
     * Render a row value into a row.
     * 
     * @param rowValue the row value associated with the row
     * @param rowRenderer the renderer used to render the rows
     */
    protected void renderRowValue(RowType rowValue,
        RowRenderer<RowType> rowRenderer) {
      rowRenderer.renderRowValue(rowValue, this);
    }
  }

  /**
   * Get the {@link RowRenderer} used to render rows. The return value should
   * not be null.
   * 
   * @return the row renderer
   */
  RowRenderer<RowType> getRowRenderer();

  /**
   * Get a list of the visible {@link ColumnDefinition ColumnDefinitions}.
   * 
   * @return the visible {@link ColumnDefinition ColumnDefinitions}
   */
  List<ColumnDefinition<RowType, ?>> getVisibleColumnDefinitions();

  /**
   * Iterator over the row values, rendering every cell in every row.
   * 
   * @param startRowIndex the index of the first row to render
   * @param rowValues the values associated with the rows
   * @param view the view used to render the rows and cells
   */
  void renderRows(int startRowIndex, Iterator<RowType> rowValues,
      AbstractRowView<RowType> view);
}
