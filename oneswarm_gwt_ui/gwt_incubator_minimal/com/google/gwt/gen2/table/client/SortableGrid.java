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

import com.google.gwt.gen2.event.shared.HandlerRegistration;
import com.google.gwt.gen2.table.client.TableModelHelper.ColumnSortInfo;
import com.google.gwt.gen2.table.client.TableModelHelper.ColumnSortList;
import com.google.gwt.gen2.table.event.client.ColumnSortEvent;
import com.google.gwt.gen2.table.event.client.ColumnSortHandler;
import com.google.gwt.gen2.table.event.client.HasColumnSortHandlers;
import com.google.gwt.gen2.table.override.client.OverrideDOM;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * A variation of the {@link com.google.gwt.gen2.table.override.client.Grid}
 * that supports sorting and row movement.
 */
public class SortableGrid extends SelectionGrid implements
    HasColumnSortHandlers {
  /**
   * The column sorter defines an algorithm to sort columns.
   */
  public abstract static class ColumnSorter {
    /**
     * Override this method to implement a custom column sorting algorithm.
     * 
     * @param grid the grid that the sorting will be applied to
     * @param sortList the list of columns to sort by
     * @param callback the callback object when sorting is complete
     */
    public abstract void onSortColumn(SortableGrid grid,
        ColumnSortList sortList, SortableGrid.ColumnSorterCallback callback);
  }

  /**
   * The default {@link ColumnSorter} used if no other {@link ColumnSorter} is
   * specified. This column sorted uses a quicksort algorithm to sort columns.
   */
  private static class DefaultColumnSorter extends ColumnSorter {
    @Override
    public void onSortColumn(SortableGrid grid, ColumnSortList sortList,
        SortableGrid.ColumnSorterCallback callback) {
      // Get the primary column and sort order
      int column = sortList.getPrimaryColumn();
      boolean ascending = sortList.isPrimaryAscending();

      // Get all of the cell elements
      SelectionGridCellFormatter formatter = grid.getSelectionGridCellFormatter();
      int rowCount = grid.getRowCount();
      List<Element> tdElems = new ArrayList<Element>(rowCount);
      for (int i = 0; i < rowCount; i++) {
        tdElems.add(formatter.getRawElement(i, column));
      }

      // Sort the cell elements
      if (ascending) {
        Collections.sort(tdElems, new Comparator<Element>() {
          public int compare(Element o1, Element o2) {
            return o1.getInnerText().compareTo(o2.getInnerText());
          }
        });
      } else {
        Collections.sort(tdElems, new Comparator<Element>() {
          public int compare(Element o1, Element o2) {
            return o2.getInnerText().compareTo(o1.getInnerText());
          }
        });
      }

      // Convert tdElems to trElems, reversing if needed
      Element[] trElems = new Element[rowCount];
      for (int i = 0; i < rowCount; i++) {
        trElems[i] = DOM.getParent(tdElems.get(i));
      }

      // Use the callback to complete the sorting
      callback.onSortingComplete(trElems);
    }
  }

  /**
   * Callback that is called when the row sorting is complete.
   */
  public class ColumnSorterCallback {
    /**
     * An array of the tr elements that should be reselected.
     */
    private Element[] selectedRows;

    /**
     * Construct a new {@link ColumnSorterCallback}.
     */
    protected ColumnSorterCallback(Element[] selectedRows) {
      this.selectedRows = selectedRows;
    }

    /**
     * Set the order of all rows after a column sort request.
     * 
     * This method takes an array of the row indexes in this Grid, ordered
     * according to their new positions in the Grid.
     * 
     * @param trIndexes the row index in their new order
     */
    public void onSortingComplete(int[] trIndexes) {
      // Convert indexes to row elements
      SelectionGridRowFormatter formatter = getSelectionGridRowFormatter();
      Element[] trElems = new Element[trIndexes.length];
      for (int i = 0; i < trElems.length; i++) {
        trElems[i] = formatter.getRawElement(trIndexes[i]);
      }

      // Call main callback method
      onSortingComplete(trElems);
    }

    /**
     * Set the order of all rows after a column sort request.
     * 
     * This method takes an array of the row elements in this Grid, ordered
     * according to their new positions in the Grid.
     * 
     * @param trElems the row elements in their new order
     */
    public void onSortingComplete(Element[] trElems) {
      // Move the rows to their new positions
      applySort(trElems);

      // Fire the listeners
      onSortingComplete();
    }

    /**
     * Trigger the callback, but do not change the ordering of the rows.
     * 
     * Use this method if your {@link ColumnSorter} rearranges the columns
     * manually.
     */
    public void onSortingComplete() {
      // Reselect things that need reselecting
      for (int i = 0; i < selectedRows.length; i++) {
        int rowIndex = getRowIndex(selectedRows[i]);
        if (rowIndex >= 0) {
          selectRow(rowIndex, false);
        }
      }

      fireColumnSorted();
    }
  }

  /**
   * The class used to do column sorting.
   */
  private ColumnSorter columnSorter = null;

  /**
   * Information about the sorted columns.
   */
  private ColumnSortList columnSortList = new ColumnSortList();

  /**
   * Constructor.
   */
  public SortableGrid() {
    super();
  }

  /**
   * Constructs a {@link SortableGrid} with the requested size.
   * 
   * @param rows the number of rows
   * @param columns the number of columns
   * @throws IndexOutOfBoundsException
   */
  public SortableGrid(int rows, int columns) {
    this();
    resize(rows, columns);
  }

  public HandlerRegistration addColumnSortHandler(ColumnSortHandler handler) {
    return addHandler(ColumnSortEvent.TYPE, handler);
  }

  /**
   * @return the column sorter used to sort columns
   */
  public ColumnSorter getColumnSorter() {
    return getColumnSorter(false);
  }

  /**
   * @return the {@link ColumnSortList} of previously sorted columns
   */
  public ColumnSortList getColumnSortList() {
    return columnSortList;
  }

  /**
   * Move a row up (relative to the screen) one index to a lesser index.
   * 
   * @param row the row index to move
   * @throws IndexOutOfBoundsException
   */
  public void moveRowDown(int row) {
    swapRows(row, row + 1);
  }

  /**
   * Move a row down (relative to the screen) one index to a greater index.
   * 
   * @param row the row index to move
   * @throws IndexOutOfBoundsException
   */
  public void moveRowUp(int row) {
    swapRows(row, row - 1);
  }

  /**
   * Completely reverse the order of all rows in the table.
   */
  public void reverseRows() {
    int lastRow = numRows - 1;
    for (int i = 0; i < numRows / 2; i++) {
      swapRowsRaw(i, lastRow);
      lastRow--;
    }

    // Set the column sorting as reversed
    for (ColumnSortInfo sortInfo : columnSortList) {
      sortInfo.setAscending(!sortInfo.isAscending());
    }
    fireColumnSorted();
  }

  /**
   * Set the {@link ColumnSorter}.
   * 
   * @param sorter the new {@link ColumnSorter}
   */
  public void setColumnSorter(ColumnSorter sorter) {
    this.columnSorter = sorter;
  }

  /**
   * Set the current {@link ColumnSortList} and fire an event.
   * 
   * @param columnSortList the new {@link ColumnSortList}
   */
  public void setColumnSortList(ColumnSortList columnSortList) {
    setColumnSortList(columnSortList, true);
  }

  /**
   * Set the current {@link ColumnSortList} and optionally fire an event.
   * 
   * @param columnSortList the new {@link ColumnSortList}
   * @param fireEvents true to trigger the onSort event
   */
  public void setColumnSortList(ColumnSortList columnSortList,
      boolean fireEvents) {
    assert columnSortList != null : "columnSortList cannot be null";
    this.columnSortList = columnSortList;
    if (fireEvents) {
      fireColumnSorted();
    }
  }

  /**
   * Sort the grid according to the specified column. If the column is already
   * sorted, reverse sort it.
   * 
   * @param column the column to sort
   * @throws IndexOutOfBoundsException
   */
  public void sortColumn(int column) {
    if (column == columnSortList.getPrimaryColumn()) {
      sortColumn(column, !columnSortList.isPrimaryAscending());
    } else {
      sortColumn(column, true);
    }
  }

  /**
   * Sort the grid according to the specified column.
   * 
   * @param column the column to sort
   * @param ascending sort the column in ascending order
   * @throws IndexOutOfBoundsException
   */
  public void sortColumn(int column, boolean ascending) {
    // Verify the column bounds
    if (column < 0) {
      throw new IndexOutOfBoundsException(
          "Cannot access a column with a negative index: " + column);
    } else if (column >= numColumns) {
      throw new IndexOutOfBoundsException("Column index: " + column
          + ", Column size: " + numColumns);
    }

    // Add the sorting to the list of sorted columns
    columnSortList.add(new ColumnSortInfo(column, ascending));

    // Use the onSort method to actually sort the column
    Element[] selectedRows = getSelectedRowsMap().values().toArray(
        new Element[0]);
    deselectAllRows();
    getColumnSorter(true).onSortColumn(this, columnSortList,
        new SortableGrid.ColumnSorterCallback(selectedRows));
  }

  /**
   * Swap the positions of two rows.
   * 
   * @param row1 the first row to swap
   * @param row2 the second row to swap
   * @throws IndexOutOfBoundsException
   */
  public void swapRows(int row1, int row2) {
    checkRowBounds(row1);
    checkRowBounds(row2);
    swapRowsRaw(row1, row2);
  }

  /**
   * Fire column sorted event to listeners.
   */
  protected void fireColumnSorted() {
    fireEvent(new ColumnSortEvent(columnSortList));
  }

  /**
   * Get the {@link ColumnSorter}. Optionally create a
   * {@link DefaultColumnSorter} if the user hasn't specified one already.
   * 
   * @param createAsNeeded create a default sorter if needed
   * @return the column sorter
   */
  protected ColumnSorter getColumnSorter(boolean createAsNeeded) {
    if ((columnSorter == null) && createAsNeeded) {
      columnSorter = new DefaultColumnSorter();
    }
    return columnSorter;
  }

  /**
   * Swap two rows without checking the cell bounds.
   * 
   * @param row1 the first row to swap
   * @param row2 the second row to swap
   */
  protected void swapRowsRaw(int row1, int row2) {
    Element tbody = getBodyElement();
    if (row1 == row2 + 1) {
      // Just move row1 up one
      Element tr = getSelectionGridRowFormatter().getRawElement(row1);
      int index = OverrideDOM.getRowIndex(tr);
      DOM.removeChild(tbody, tr);
      DOM.insertChild(tbody, tr, index - 1);
    } else if (row2 == row1 + 1) {
      // Just move row2 up one
      Element tr = getSelectionGridRowFormatter().getRawElement(row2);
      int index = OverrideDOM.getRowIndex(tr);
      DOM.removeChild(tbody, tr);
      DOM.insertChild(tbody, tr, index - 1);
    } else if (row1 == row2) {
      // Do nothing if rows are the same
      return;
    } else {
      // Remove both rows
      Element tr1 = getSelectionGridRowFormatter().getRawElement(row1);
      Element tr2 = getSelectionGridRowFormatter().getRawElement(row2);
      int index1 = OverrideDOM.getRowIndex(tr1);
      int index2 = OverrideDOM.getRowIndex(tr2);
      DOM.removeChild(tbody, tr1);
      DOM.removeChild(tbody, tr2);

      // Reinsert them into the table
      if (row1 > row2) {
        DOM.insertChild(tbody, tr1, index2);
        DOM.insertChild(tbody, tr2, index1);
      } else if (row1 < row2) {
        DOM.insertChild(tbody, tr2, index1);
        DOM.insertChild(tbody, tr1, index2);
      }
    }

    // Update the selected rows table
    Map<Integer, Element> selectedRows = getSelectedRowsMap();
    Element tr1 = selectedRows.remove(new Integer(row1));
    Element tr2 = selectedRows.remove(new Integer(row2));
    if (tr1 != null) {
      selectedRows.put(new Integer(row2), tr1);
    }
    if (tr2 != null) {
      selectedRows.put(new Integer(row1), tr2);
    }
  }

  /**
   * Set the order of all rows after a column sort request.
   * 
   * This method takes an array of the row elements in this Grid, ordered
   * according to their new positions in the Grid.
   * 
   * @param trElems the row elements in their new order
   */
  void applySort(Element[] trElems) {
    // Move the rows to their new positions
    Element bodyElem = getBodyElement();
    for (int i = trElems.length - 1; i >= 0; i--) {
      if (trElems[i] != null) {
        DOM.removeChild(bodyElem, trElems[i]);
        DOM.insertChild(bodyElem, trElems[i], 0);
      }
    }
  }
}
