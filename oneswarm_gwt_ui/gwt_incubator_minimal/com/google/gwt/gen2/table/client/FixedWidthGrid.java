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

import com.google.gwt.gen2.table.client.FixedWidthTableImpl.IdealColumnWidthInfo;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Map;

/**
 * A variation of the {@link com.google.gwt.gen2.table.override.client.Grid}
 * that resizes columns using a fixed table width.
 */
public class FixedWidthGrid extends SortableGrid {
  /**
   * This class contains methods used to format a table's cells.
   */
  public class FixedWidthGridCellFormatter extends SelectionGridCellFormatter {
    @Override
    public void setWidth(int row, int column, String width) {
      throw new UnsupportedOperationException("setWidth is not supported.  "
          + "Use FixedWidthGrid.setColumnWidth(int, int) instead.");
    }

    @Override
    protected Element getRawElement(int row, int column) {
      return super.getRawElement(row + 1, column);
    }
  }

  /**
   * This class contains methods used to format a table's columns.
   */
  public class FixedWidthGridColumnFormatter extends ColumnFormatter {
    @Override
    public void setWidth(int column, String width) {
      throw new UnsupportedOperationException("setWidth is not supported.  "
          + "Use FixedWidthdGrid.setColumnWidth(int, int) instead.");
    }
  }

  /**
   * This class contains methods used to format a table's rows.
   */
  public class FixedWidthGridRowFormatter extends SelectionGridRowFormatter {
    @Override
    protected Element getRawElement(int row) {
      return super.getRawElement(row + 1);
    }
  }

  /**
   * The default width of a column in pixels.
   */
  public static final int DEFAULT_COLUMN_WIDTH = 80;

  /**
   * The minimum width of any column.
   */
  public static final int MIN_COLUMN_WIDTH = 1;

  /**
   * A mapping of column indexes to their widths in pixels.
   */
  private Map<Integer, Integer> colWidths = new HashMap<Integer, Integer>();

  /**
   * The hidden, ghost row used for sizing the columns.
   */
  private Element ghostRow = null;

  /**
   * The ideal widths of all columns (that are available).
   */
  private int[] idealWidths;

  /**
   * Info used to calculate ideal column width.
   */
  private IdealColumnWidthInfo idealColumnWidthInfo;

  /**
   * Constructor.
   */
  public FixedWidthGrid() {
    super();
    setClearText("&nbsp;");

    // Setup the table element
    Element tableElem = getElement();
    DOM.setStyleAttribute(tableElem, "tableLayout", "fixed");
    DOM.setStyleAttribute(tableElem, "width", "0px");

    // Replace the formatters
    setRowFormatter(new FixedWidthGridRowFormatter());
    setCellFormatter(new FixedWidthGridCellFormatter());
    setColumnFormatter(new FixedWidthGridColumnFormatter());

    // Create the ghost row for sizing
    ghostRow = FixedWidthTableImpl.get().createGhostRow();
    DOM.insertChild(getBodyElement(), ghostRow, 0);

    // Sink highlight and selection events
    sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEDOWN | Event.ONCLICK);
  }

  /**
   * Constructs a {@link FixedWidthGrid} with the requested size.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public FixedWidthGrid(int rows, int columns) {
    this();
    resize(rows, columns);
  }

  @Override
  public void clear() {
    super.clear();
    clearIdealWidths();
  }

  /**
   * Return the column width for a given column index. If a width has not been
   * assigned, the default width is returned.
   * 
   * @param column the column index
   * @return the column width in pixels
   */
  public int getColumnWidth(int column) {
    Integer colWidth = colWidths.get(new Integer(column));
    if (colWidth == null) {
      return DEFAULT_COLUMN_WIDTH;
    } else {
      return colWidth.intValue();
    }
  }

  /**
   * <p>
   * Calculate the ideal width required to tightly wrap the specified column. If
   * the ideal column width cannot be calculated (eg. if the table is not
   * attached), -1 is returned.
   * </p>
   * <p>
   * Note that this method requires an expensive operation whenever the content
   * of the table is changed, so you should only call it after you've completely
   * modified the contents of your table.
   * </p>
   * 
   * @return the ideal column width, or -1 if it is not applicable
   */
  public int getIdealColumnWidth(int column) {
    maybeRecalculateIdealColumnWidths();
    if (idealWidths.length > column) {
      return idealWidths[column];
    }
    return -1;
  }

  @Override
  public boolean remove(Widget widget) {
    if (super.remove(widget)) {
      clearIdealWidths();
      return true;
    }
    return false;
  }

  @Override
  public void removeRow(int row) {
    super.removeRow(row);
    clearIdealWidths();
  }

  @Override
  public void resizeColumns(int columns) {
    super.resizeColumns(columns);
    updateGhostRow();
    clearIdealWidths();
  }

  @Override
  public void resizeRows(int rows) {
    super.resizeRows(rows);
    clearIdealWidths();
  }

  @Override
  public void setCellPadding(int padding) {
    super.setCellPadding(padding);

    // Reset the width of all columns
    for (Map.Entry<Integer, Integer> entry : colWidths.entrySet()) {
      setColumnWidth(entry.getKey(), entry.getValue());
    }
    if (getSelectionPolicy().hasInputColumn()) {
      setColumnWidthImpl(-1, getInputColumnWidth());
    }
  }

  @Override
  public void setCellSpacing(int spacing) {
    super.setCellSpacing(spacing);

    // Reset the width of all columns
    for (Map.Entry<Integer, Integer> entry : colWidths.entrySet()) {
      setColumnWidth(entry.getKey(), entry.getValue());
    }
    if (getSelectionPolicy().hasInputColumn()) {
      setColumnWidthImpl(-1, getInputColumnWidth());
    }
  }

  /**
   * Set the width of a column.
   * 
   * @param column the index of the column
   * @param width the width in pixels
   * @throws IndexOutOfBoundsException
   */
  public void setColumnWidth(int column, int width) {
    // Ensure that the indices are not negative.
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    }

    // Add the width to the map
    width = Math.max(MIN_COLUMN_WIDTH, width);
    colWidths.put(new Integer(column), new Integer(width));

    // Update the cell width if possible
    if (column >= numColumns) {
      return;
    }

    // Set the actual column width
    setColumnWidthImpl(column, width);
  }

  @Override
  public void setHTML(int row, int column, String html) {
    super.setHTML(row, column, html);
    clearIdealWidths();
  }

  @Override
  public void setSelectionPolicy(SelectionPolicy selectionPolicy) {
    // Update the input column in the ghost row
    if (selectionPolicy.hasInputColumn()
        && !getSelectionPolicy().hasInputColumn()) {
      // Add ghost input column
      Element tr = getGhostRow();
      Element td = FixedWidthTableImpl.get().createGhostCell(null);
      tr.insertBefore(td, tr.getFirstChildElement());
      super.setSelectionPolicy(selectionPolicy);
      setColumnWidthImpl(-1, getInputColumnWidth());
    } else if (!selectionPolicy.hasInputColumn()
        && getSelectionPolicy().hasInputColumn()) {
      // Remove ghost input column
      Element tr = getGhostRow();
      tr.removeChild(tr.getFirstChildElement());
      super.setSelectionPolicy(selectionPolicy);
    } else {
      super.setSelectionPolicy(selectionPolicy);
    }
  }

  @Override
  public void setText(int row, int column, String text) {
    super.setText(row, column, text);
    clearIdealWidths();
  }

  @Override
  public void setWidget(int row, int column, Widget widget) {
    super.setWidget(row, column, widget);
    clearIdealWidths();
  }

  @Override
  protected int getDOMCellCount(int row) {
    return super.getDOMCellCount(row + 1);
  }

  @Override
  protected int getDOMRowCount() {
    return super.getDOMRowCount() - 1;
  }

  /**
   * Explicitly gets the {@link FixedWidthGridCellFormatter}. The results of
   * {@link com.google.gwt.user.client.ui.HTMLTable#getCellFormatter()} may also
   * be downcast to a {@link FixedWidthGridCellFormatter}.
   * 
   * @return the {@link FixedWidthGrid}'s cell formatter
   */
  protected FixedWidthGridCellFormatter getFixedWidthGridCellFormatter() {
    return (FixedWidthGridCellFormatter) getCellFormatter();
  }

  /**
   * Explicitly gets the {@link FixedWidthGridRowFormatter}. The results of
   * {@link com.google.gwt.user.client.ui.HTMLTable#getCellFormatter()} may also
   * be downcast to a {@link FixedWidthGridRowFormatter}.
   * 
   * @return the {@link FixedWidthGrid}'s cell formatter
   */
  protected FixedWidthGridRowFormatter getFixedWidthGridRowFormatter() {
    return (FixedWidthGridRowFormatter) getRowFormatter();
  }

  /**
   * @return the number of columns in the ghost row
   */
  protected int getGhostColumnCount() {
    return super.getDOMCellCount(0);
  }

  /**
   * @return the ghost row element
   */
  protected Element getGhostRow() {
    return ghostRow;
  }

  /**
   * Get the width of the input column used in the current
   * {@link SelectionGrid.SelectionPolicy}.
   * 
   * @return the width of the input element
   */
  protected int getInputColumnWidth() {
    return 30;
  }

  @Override
  protected int getRowIndex(Element rowElem) {
    int rowIndex = super.getRowIndex(rowElem);
    if (rowIndex < 0) {
      return rowIndex;
    }
    return rowIndex - 1;
  }

  @Override
  protected boolean internalClearCell(Element td, boolean clearInnerHTML) {
    clearIdealWidths();
    return super.internalClearCell(td, clearInnerHTML);
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    clearIdealWidths();
  }

  /**
   * Recalculate the ideal column widths of each column in the data table.
   */
  protected void recalculateIdealColumnWidths() {
    // We need at least one cell to do any calculations
    int columnCount = getColumnCount();
    if (!isAttached() || getRowCount() == 0 || columnCount < 1) {
      idealWidths = new int[0];
      return;
    }

    recalculateIdealColumnWidthsSetup();
    recalculateIdealColumnWidthsImpl();
    recalculateIdealColumnWidthsTeardown();
  }

  /**
   * Sets the ghost row variable. This does not change the underlying structure
   * of the table.
   * 
   * @param ghostRow the new ghost row
   */
  protected void setGhostRow(Element ghostRow) {
    this.ghostRow = ghostRow;
  }

  /**
   * Add or remove ghost cells when the table size changes.
   */
  protected void updateGhostRow() {
    int numGhosts = getGhostColumnCount();
    if (numColumns > numGhosts) {
      // Add ghosts as needed
      for (int i = numGhosts; i < numColumns; i++) {
        Element td = FixedWidthTableImpl.get().createGhostCell(null);
        DOM.appendChild(ghostRow, td);
        setColumnWidth(i, getColumnWidth(i));
      }
    } else if (numColumns < numGhosts) {
      int cellsToRemove = numGhosts - numColumns;
      for (int i = 0; i < cellsToRemove; i++) {
        Element td = getGhostCellElement(numColumns);
        DOM.removeChild(ghostRow, td);
      }
    }
  }

  @Override
  void applySort(Element[] trElems) {
    // Move the rows to their new positions
    Element bodyElem = getBodyElement();
    for (int i = trElems.length - 1; i >= 0; i--) {
      if (trElems[i] != null) {
        DOM.removeChild(bodyElem, trElems[i]);
        // Need to insert below the ghost row
        DOM.insertChild(bodyElem, trElems[i], 1);
      }
    }
  }

  /**
   * Clear the idealWidths field when the ideal widths change.
   */
  void clearIdealWidths() {
    idealWidths = null;
  }

  /**
   * @return true if the ideal column widths have already been calculated
   */
  boolean isIdealColumnWidthsCalculated() {
    return idealWidths != null;
  }

  /**
   * Recalculate the ideal column widths of each column in the data table. This
   * method assumes that the tableLayout has already been changed.
   */
  void recalculateIdealColumnWidthsImpl() {
    idealWidths = FixedWidthTableImpl.get().recalculateIdealColumnWidths(
        idealColumnWidthInfo);
  }

  /**
   * Setup to recalculate column widths.
   */
  void recalculateIdealColumnWidthsSetup() {
    int offset = 0;
    if (getSelectionPolicy().hasInputColumn()) {
      offset++;
    }
    idealColumnWidthInfo = FixedWidthTableImpl.get().recalculateIdealColumnWidthsSetup(
        this, getColumnCount(), offset);
  }

  /**
   * Tear down after recalculating column widths.
   */
  void recalculateIdealColumnWidthsTeardown() {
    FixedWidthTableImpl.get().recalculateIdealColumnWidthsTeardown(
        idealColumnWidthInfo);
    idealColumnWidthInfo = null;
  }

  /**
   * Returns a cell in the ghost row.
   * 
   * @param column the cell's column
   * @return the ghost cell
   */
  private Element getGhostCellElement(int column) {
    if (getSelectionPolicy().hasInputColumn()) {
      column++;
    }
    return FixedWidthTableImpl.get().getGhostCell(ghostRow, column);
  }

  /**
   * Recalculate the ideal column widths of each column in the data table if
   * they have changed since the last calculation.
   */
  private void maybeRecalculateIdealColumnWidths() {
    if (idealWidths == null) {
      recalculateIdealColumnWidths();
    }
  }

  /**
   * Set the width of a column.
   * 
   * @param column the index of the column
   * @param width the width in pixels
   */
  private void setColumnWidthImpl(int column, int width) {
    if (getSelectionPolicy().hasInputColumn()) {
      column++;
    }
    FixedWidthTableImpl.get().setColumnWidth(this, ghostRow, column, width);
  }
}
