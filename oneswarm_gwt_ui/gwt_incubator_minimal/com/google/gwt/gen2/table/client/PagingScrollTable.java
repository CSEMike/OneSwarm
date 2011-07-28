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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.event.shared.HandlerRegistration;
import com.google.gwt.gen2.table.client.CellEditor.CellEditInfo;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.gen2.table.client.SortableGrid.ColumnSorter;
import com.google.gwt.gen2.table.client.SortableGrid.ColumnSorterCallback;
import com.google.gwt.gen2.table.client.TableDefinition.AbstractCellView;
import com.google.gwt.gen2.table.client.TableDefinition.AbstractRowView;
import com.google.gwt.gen2.table.client.TableModel.Callback;
import com.google.gwt.gen2.table.client.TableModelHelper.ColumnSortList;
import com.google.gwt.gen2.table.client.TableModelHelper.Request;
import com.google.gwt.gen2.table.client.TableModelHelper.Response;
import com.google.gwt.gen2.table.client.property.FooterProperty;
import com.google.gwt.gen2.table.client.property.HeaderProperty;
import com.google.gwt.gen2.table.client.property.MaximumWidthProperty;
import com.google.gwt.gen2.table.client.property.MinimumWidthProperty;
import com.google.gwt.gen2.table.client.property.PreferredWidthProperty;
import com.google.gwt.gen2.table.client.property.SortableProperty;
import com.google.gwt.gen2.table.client.property.TruncationProperty;
import com.google.gwt.gen2.table.event.client.HasPageChangeHandlers;
import com.google.gwt.gen2.table.event.client.HasPageCountChangeHandlers;
import com.google.gwt.gen2.table.event.client.HasPageLoadHandlers;
import com.google.gwt.gen2.table.event.client.HasPagingFailureHandlers;
import com.google.gwt.gen2.table.event.client.HasRowInsertionHandlers;
import com.google.gwt.gen2.table.event.client.HasRowRemovalHandlers;
import com.google.gwt.gen2.table.event.client.HasRowValueChangeHandlers;
import com.google.gwt.gen2.table.event.client.PageChangeEvent;
import com.google.gwt.gen2.table.event.client.PageChangeHandler;
import com.google.gwt.gen2.table.event.client.PageCountChangeEvent;
import com.google.gwt.gen2.table.event.client.PageCountChangeHandler;
import com.google.gwt.gen2.table.event.client.PageLoadEvent;
import com.google.gwt.gen2.table.event.client.PageLoadHandler;
import com.google.gwt.gen2.table.event.client.PagingFailureEvent;
import com.google.gwt.gen2.table.event.client.PagingFailureHandler;
import com.google.gwt.gen2.table.event.client.RowCountChangeEvent;
import com.google.gwt.gen2.table.event.client.RowCountChangeHandler;
import com.google.gwt.gen2.table.event.client.RowInsertionEvent;
import com.google.gwt.gen2.table.event.client.RowInsertionHandler;
import com.google.gwt.gen2.table.event.client.RowRemovalEvent;
import com.google.gwt.gen2.table.event.client.RowRemovalHandler;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.gen2.table.event.client.RowValueChangeEvent;
import com.google.gwt.gen2.table.event.client.RowValueChangeHandler;
import com.google.gwt.gen2.table.event.client.TableEvent.Row;
import com.google.gwt.gen2.table.override.client.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An {@link AbstractScrollTable} that acts as a view for an underlying
 * {@link MutableTableModel}.
 * 
 * @param <RowType> the data type of the row values
 */
public class PagingScrollTable<RowType> extends AbstractScrollTable implements
    HasTableDefinition<RowType>, HasPageCountChangeHandlers,
    HasPageLoadHandlers, HasPageChangeHandlers, HasPagingFailureHandlers {
  /**
   * A custom {@link AbstractCellView} used by the {@link PagingScrollTable}.
   * 
   * @param <RowType> the type of the row values
   */
  protected static class PagingScrollTableCellView<RowType> extends
      AbstractCellView<RowType> {
    private PagingScrollTable<RowType> table;

    public PagingScrollTableCellView(PagingScrollTable<RowType> table) {
      super(table);
      this.table = table;
    }

    @Override
    public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
      table.getDataTable().getCellFormatter().setHorizontalAlignment(
          getRowIndex(), getCellIndex(), align);
    }

    @Override
    public void setHTML(String html) {
      table.getDataTable().setHTML(getRowIndex(), getCellIndex(), html);
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      table.getDataTable().getFixedWidthGridCellFormatter().getRawElement(
          getRowIndex(), getCellIndex()).getStyle().setProperty(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      table.getDataTable().getCellFormatter().setStyleName(getRowIndex(),
          getCellIndex(), stylename);
    }

    @Override
    public void setText(String text) {
      table.getDataTable().setText(getRowIndex(), getCellIndex(), text);
    }

    @Override
    public void setVerticalAlignment(VerticalAlignmentConstant align) {
      table.getDataTable().getCellFormatter().setVerticalAlignment(
          getRowIndex(), getCellIndex(), align);
    }

    @Override
    public void setWidget(Widget widget) {
      table.getDataTable().setWidget(getRowIndex(), getCellIndex(), widget);
    }
  }

  /**
   * A custom {@link AbstractRowView} used by the {@link PagingScrollTable}.
   * 
   * @param <RowType> the type of the row values
   */
  protected static class PagingScrollTableRowView<RowType> extends
      AbstractRowView<RowType> {
    private PagingScrollTable<RowType> table;

    public PagingScrollTableRowView(PagingScrollTable<RowType> table) {
      super(new PagingScrollTableCellView<RowType>(table));
      this.table = table;
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      table.getDataTable().getFixedWidthGridRowFormatter().getRawElement(
          getRowIndex()).getStyle().setProperty(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      // If the row is selected, add the selected style name back
      if (table.getDataTable().isRowSelected(getRowIndex())) {
        stylename += " selected";
      }
      table.getDataTable().getRowFormatter().setStyleName(getRowIndex(),
          stylename);
    }
  }

  /**
   * Information about a column header.
   */
  private static class ColumnHeaderInfo {
    private int rowSpan = 1;
    private Object header;

    public ColumnHeaderInfo(Object header) {
      this.header = (header == null) ? "" : header;
    }

    public ColumnHeaderInfo(Object header, int rowSpan) {
      this.header = (header == null) ? "&nbsp;" : header;
      this.rowSpan = rowSpan;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      if (o instanceof ColumnHeaderInfo) {
        ColumnHeaderInfo info = (ColumnHeaderInfo) o;
        return (rowSpan == info.rowSpan) && header.equals(info.header);
      }
      return false;
    }

    public Object getHeader() {
      return header;
    }

    public int getRowSpan() {
      return rowSpan;
    }

    public void incrementRowSpan() {
      rowSpan++;
    }
  }

  /**
   * An iterator over the visible rows in an iterator over many rows.
   */
  private class VisibleRowsIterator implements Iterator<RowType> {
    /**
     * The iterator of row data.
     */
    private Iterator<RowType> rows;

    /**
     * The current row of the rows iterator.
     */
    private int curRow;

    /**
     * The last visible row in the grid.
     */
    private int lastVisibleRow;

    /**
     * Constructor.
     * 
     * @param rows the iterator over row data
     * @param firstRow the first absolute row of the rows iterator
     * @param firstVisibleRow the first visible row in this grid
     * @param lastVisibleRow the last visible row in this grid
     */
    public VisibleRowsIterator(Iterator<RowType> rows, int firstRow,
        int firstVisibleRow, int lastVisibleRow) {
      this.curRow = firstRow;
      this.lastVisibleRow = lastVisibleRow;

      // Iterate up to the first row
      while (curRow < firstVisibleRow && rows.hasNext()) {
        rows.next();
        curRow++;
      }
      this.rows = rows;
    }

    public boolean hasNext() {
      return (curRow <= lastVisibleRow && rows.hasNext());
    }

    public RowType next() {
      // Check that the next row exists
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return rows.next();
    }

    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }

  /**
   * The bulk render used to render the contents of this table.
   */
  private FixedWidthGridBulkRenderer<RowType> bulkRenderer = null;

  /**
   * The wrapper around the empty table widget.
   */
  private SimplePanel emptyTableWidgetWrapper = new SimplePanel();

  /**
   * The definition of the columns in the table.
   */
  private TableDefinition<RowType> tableDefinition = null;

  /**
   * The current visible page.
   */
  private int currentPage = -1;

  /**
   * The last request that was sent to the {@link TableModel}.
   */
  private Request lastRequest = null;

  /**
   * A boolean indicating that cross page selection is enabled.
   */
  private boolean isCrossPageSelectionEnabled;

  /**
   * The set of selected row values.
   */
  private Set<RowType> selectedRowValues = new HashSet<RowType>();

  /**
   * A boolean indicating that the footer should be generated automatically.
   */
  private boolean isFooterGenerated;

  /**
   * A boolean indicating that the header should be generated automatically.
   */
  private boolean isHeaderGenerated;

  /**
   * A boolean indicating that the page is currently being loaded.
   */
  private boolean isPageLoading;

  /**
   * The old page count, used to detect when the number of pages changes.
   */
  private int oldPageCount;

  /**
   * The number of rows per page. If the number of rows per page is equal to the
   * number of rows, paging is disabled because only one page exists.
   */
  private int pageSize = 0;

  /**
   * The callback that handles page requests.
   */
  private Callback<RowType> pagingCallback = new Callback<RowType>() {
    public void onFailure(Throwable caught) {
      isPageLoading = false;
      fireEvent(new PagingFailureEvent(caught));
    }

    public void onRowsReady(Request request, Response<RowType> response) {
      if (lastRequest == request) {
        setData(request.getStartRow(), response.getRowValues());
        lastRequest = null;
      }
    }
  };

  /**
   * The values associated with each row. This is an optional list of data that
   * ties the visible content in each row to an underlying object.
   */
  private List<RowType> rowValues = new ArrayList<RowType>();

  /**
   * The view of this table.
   */
  private AbstractRowView<RowType> rowView = new PagingScrollTableRowView<RowType>(
      this);

  /**
   * The {@link CheckBox} used to select all rows.
   */
  private Widget selectAllWidget;

  /**
   * The underlying table model.
   */
  private TableModel<RowType> tableModel;

  /**
   * The {@link RendererCallback} used when table rendering completes.
   */
  private RendererCallback tableRendererCallback = new RendererCallback() {
    public void onRendered() {
      onDataTableRendered();
    }
  };

  /**
   * The columns that are currently visible.
   */
  private List<ColumnDefinition<RowType, ?>> visibleColumns = new ArrayList<ColumnDefinition<RowType, ?>>();

  /**
   * The boolean indicating that the header tables are obsolete.
   */
  private boolean headersObsolete;

  /**
   * Construct a new {@link PagingScrollTable}.
   * 
   * @param tableModel the underlying table model
   * @param tableDefinition the column definitions
   */
  public PagingScrollTable(TableModel<RowType> tableModel,
      TableDefinition<RowType> tableDefinition) {
    this(tableModel, new FixedWidthGrid(), new FixedWidthFlexTable(),
        tableDefinition);
    isHeaderGenerated = true;
    isFooterGenerated = true;
  }

  /**
   * Construct a new {@link PagingScrollTable}.
   * 
   * @param tableModel the underlying table model
   * @param dataTable the table used to display data
   * @param headerTable the header table
   * @param tableDefinition the column definitions
   */
  public PagingScrollTable(TableModel<RowType> tableModel,
      FixedWidthGrid dataTable, FixedWidthFlexTable headerTable,
      TableDefinition<RowType> tableDefinition) {
    this(tableModel, dataTable, headerTable, tableDefinition,
        GWT.<ScrollTableImages> create(ScrollTableImages.class));
  }

  /**
   * Construct a new {@link PagingScrollTable} with custom images.
   * 
   * @param tableModel the underlying table model
   * @param dataTable the table used to display data
   * @param headerTable the header table
   * @param tableDefinition the column definitions
   * @param images the images to use in the table
   */
  public PagingScrollTable(TableModel<RowType> tableModel,
      FixedWidthGrid dataTable, FixedWidthFlexTable headerTable,
      TableDefinition<RowType> tableDefinition, ScrollTableImages images) {
    super(dataTable, headerTable, images);
    this.tableModel = tableModel;
    setTableDefinition(tableDefinition);
    refreshVisibleColumnDefinitions();
    oldPageCount = getPageCount();

    // Setup the empty table widget wrapper
    emptyTableWidgetWrapper.getElement().getStyle().setProperty("width", "100%");
    emptyTableWidgetWrapper.getElement().getStyle().setProperty("overflow",
        "hidden");
    emptyTableWidgetWrapper.getElement().getStyle().setPropertyPx("border", 0);
    emptyTableWidgetWrapper.getElement().getStyle().setPropertyPx("margin", 0);
    emptyTableWidgetWrapper.getElement().getStyle().setPropertyPx("padding", 0);
    insert(emptyTableWidgetWrapper, getAbsoluteElement(), 2, true);
    setEmptyTableWidgetVisible(false);

    // Listen to table model events
    tableModel.addRowCountChangeHandler(new RowCountChangeHandler() {
      public void onRowCountChange(RowCountChangeEvent event) {
        int pageCount = getPageCount();
        if (pageCount != oldPageCount) {
          fireEvent(new PageCountChangeEvent(oldPageCount, pageCount));
          oldPageCount = pageCount;
        }
      }
    });
    if (tableModel instanceof HasRowInsertionHandlers) {
      ((HasRowInsertionHandlers) tableModel).addRowInsertionHandler(new RowInsertionHandler() {
        public void onRowInsertion(RowInsertionEvent event) {
          insertAbsoluteRow(event.getRowIndex());
        }
      });
    }
    if (tableModel instanceof HasRowRemovalHandlers) {
      ((HasRowRemovalHandlers) tableModel).addRowRemovalHandler(new RowRemovalHandler() {
        public void onRowRemoval(RowRemovalEvent event) {
          removeAbsoluteRow(event.getRowIndex());
        }
      });
    }
    if (tableModel instanceof HasRowValueChangeHandlers) {
      ((HasRowValueChangeHandlers<RowType>) tableModel).addRowValueChangeHandler(new RowValueChangeHandler<RowType>() {
        public void onRowValueChange(RowValueChangeEvent<RowType> event) {
          int rowIndex = event.getRowIndex();
          if (rowIndex < getAbsoluteFirstRowIndex()
              || rowIndex > getAbsoluteLastRowIndex()) {
            return;
          }
          setRowValue(rowIndex - getAbsoluteFirstRowIndex(),
              event.getRowValue());
        }
      });
    }

    // Listen for cell click events
    dataTable.addTableListener(new TableListener() {
      public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
        editCell(row, cell);
      }
    });

    // Override the column sorter
    if (dataTable.getColumnSorter() == null) {
      ColumnSorter sorter = new ColumnSorter() {
        @Override
        public void onSortColumn(SortableGrid grid, ColumnSortList sortList,
            ColumnSorterCallback callback) {
          reloadPage();
          callback.onSortingComplete();
        }
      };
      dataTable.setColumnSorter(sorter);
    }

    // Listen for selection events
    dataTable.addRowSelectionHandler(new RowSelectionHandler() {
      public void onRowSelection(RowSelectionEvent event) {
        if (isPageLoading) {
          return;
        }
        Set<Row> deselected = event.getDeselectedRows();
        for (Row row : deselected) {
          selectedRowValues.remove(getRowValue(row.getRowIndex()));
        }
        Set<Row> selected = event.getSelectedRows();
        for (Row row : selected) {
          selectedRowValues.add(getRowValue(row.getRowIndex()));
        }
      }
    });
  }

  public HandlerRegistration addPageChangeHandler(PageChangeHandler handler) {
    return addHandler(PageChangeEvent.TYPE, handler);
  }

  public HandlerRegistration addPageCountChangeHandler(
      PageCountChangeHandler handler) {
    return addHandler(PageCountChangeEvent.TYPE, handler);
  }

  public HandlerRegistration addPageLoadHandler(PageLoadHandler handler) {
    return addHandler(PageLoadEvent.TYPE, handler);
  }

  public HandlerRegistration addPagingFailureHandler(
      PagingFailureHandler handler) {
    return addHandler(PagingFailureEvent.TYPE, handler);
  }

  /**
   * @return the absolute index of the first visible row
   */
  public int getAbsoluteFirstRowIndex() {
    return currentPage * pageSize;
  }

  /**
   * @return the absolute index of the last visible row
   */
  public int getAbsoluteLastRowIndex() {
    if (tableModel.getRowCount() < 0) {
      // Unknown row count, so just return based on current page
      return (currentPage + 1) * pageSize - 1;
    } else if (pageSize == 0) {
      // Only one page, so return row count
      return tableModel.getRowCount() - 1;
    }
    return Math.min(tableModel.getRowCount(), (currentPage + 1) * pageSize) - 1;
  }

  /**
   * @return the current page
   */
  public int getCurrentPage() {
    return currentPage;
  }

  /**
   * @return the widget displayed when the data table is empty
   */
  public Widget getEmptyTableWidget() {
    return emptyTableWidgetWrapper.getWidget();
  }

  @Override
  public int getMaximumColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return -1;
    }
    return colDef.getColumnProperty(MaximumWidthProperty.TYPE).getMaximumColumnWidth();
  }

  @Override
  public int getMinimumColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return FixedWidthGrid.MIN_COLUMN_WIDTH;
    }
    int minWidth = colDef.getColumnProperty(MinimumWidthProperty.TYPE).getMinimumColumnWidth();
    return Math.max(FixedWidthGrid.MIN_COLUMN_WIDTH, minWidth);
  }

  /**
   * @return the number of pages, or -1 if not known
   */
  public int getPageCount() {
    if (pageSize < 1) {
      return 1;
    } else {
      int numDataRows = tableModel.getRowCount();
      if (numDataRows < 0) {
        return -1;
      }
      return (int) Math.ceil(numDataRows / (pageSize + 0.0));
    }
  }

  /**
   * @return the number of rows per page
   */
  public int getPageSize() {
    return pageSize;
  }

  @Override
  public int getPreferredColumnWidth(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return FixedWidthGrid.DEFAULT_COLUMN_WIDTH;
    }
    return colDef.getColumnProperty(PreferredWidthProperty.TYPE).getPreferredColumnWidth();
  }

  /**
   * Get the value associated with a row.
   * 
   * @param row the row index
   * @return the value associated with the row
   */
  public RowType getRowValue(int row) {
    if (rowValues.size() <= row) {
      return null;
    }
    return rowValues.get(row);
  }

  /**
   * Get the selected row values. If cross page selection is enabled, this will
   * include row values selected on all pages.
   * 
   * @return the selected row values
   * @see #setCrossPageSelectionEnabled(boolean)
   */
  public Set<RowType> getSelectedRowValues() {
    return selectedRowValues;
  }

  public TableDefinition<RowType> getTableDefinition() {
    return tableDefinition;
  }

  /**
   * @return the table model
   */
  public TableModel<RowType> getTableModel() {
    return tableModel;
  }

  /**
   * Go to the first page.
   */
  public void gotoFirstPage() {
    gotoPage(0, false);
  }

  /**
   * Go to the last page. If the number of pages is not known, this method is
   * ignored.
   */
  public void gotoLastPage() {
    if (getPageCount() >= 0) {
      gotoPage(getPageCount(), false);
    }
  }

  /**
   * Go to the next page.
   */
  public void gotoNextPage() {
    gotoPage(currentPage + 1, false);
  }

  /**
   * Set the current page. If the page is out of bounds, it will be
   * automatically set to zero or the last page without throwing any errors.
   * 
   * @param page the page
   * @param forced reload the page even if it is already loaded
   */
  public void gotoPage(int page, boolean forced) {
    int oldPage = currentPage;
    int numPages = getPageCount();
    if (numPages >= 0) {
      currentPage = Math.max(0, Math.min(page, numPages - 1));
    } else {
      currentPage = page;
    }

    if (currentPage != oldPage || forced) {
      isPageLoading = true;

      // Deselect rows when switching pages
      FixedWidthGrid dataTable = getDataTable();
      dataTable.deselectAllRows();
      if (!isCrossPageSelectionEnabled) {
        selectedRowValues = new HashSet<RowType>();
      }

      // Fire listeners
      fireEvent(new PageChangeEvent(oldPage, currentPage));

      // Clear out existing data if we aren't bulk rendering
      if (bulkRenderer == null) {
        int rowCount = getAbsoluteLastRowIndex() - getAbsoluteFirstRowIndex()
            + 1;
        if (rowCount != dataTable.getRowCount()) {
          dataTable.resizeRows(rowCount);
        }
        dataTable.clearAll();
      }

      // Request the new data from the table model
      int firstRow = getAbsoluteFirstRowIndex();
      int lastRow = pageSize == 0 ? tableModel.getRowCount() : pageSize;
      lastRequest = new Request(firstRow, lastRow,
          dataTable.getColumnSortList());
      tableModel.requestRows(lastRequest, pagingCallback);
    }
  }

  /**
   * Go to the previous page.
   */
  public void gotoPreviousPage() {
    gotoPage(currentPage - 1, false);
  }

  @Override
  public boolean isColumnSortable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    if (getSortPolicy() == SortPolicy.DISABLED) {
      return false;
    }
    return colDef.getColumnProperty(SortableProperty.TYPE).isColumnSortable();
  }

  @Override
  public boolean isColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isColumnTruncatable();
  }

  /**
   * @return true if cross page selection is enabled
   */
  public boolean isCrossPageSelectionEnabled() {
    return isCrossPageSelectionEnabled;
  }

  @Override
  public boolean isFooterColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isFooterTruncatable();
  }

  /**
   * @return true if the footer table is automatically generated
   */
  public boolean isFooterGenerated() {
    return isFooterGenerated;
  }

  @Override
  public boolean isHeaderColumnTruncatable(int column) {
    ColumnDefinition<RowType, ?> colDef = getColumnDefinition(column);
    if (colDef == null) {
      return true;
    }
    return colDef.getColumnProperty(TruncationProperty.TYPE).isHeaderTruncatable();
  }

  /**
   * @return true if the header table is automatically generated
   */
  public boolean isHeaderGenerated() {
    return isHeaderGenerated;
  }

  /**
   * @return true if a page load is pending
   */
  public boolean isPageLoading() {
    return isPageLoading;
  }

  /**
   * Reload the current page.
   */
  public void reloadPage() {
    if (currentPage >= 0) {
      gotoPage(currentPage, true);
    } else {
      gotoPage(0, true);
    }
  }

  /**
   * Set the {@link FixedWidthGridBulkRenderer} used to render the data table.
   * 
   * @param bulkRenderer the table renderer
   */
  public void setBulkRenderer(FixedWidthGridBulkRenderer<RowType> bulkRenderer) {
    this.bulkRenderer = bulkRenderer;
  }

  /**
   * Enable or disable cross page selection. When enabled, row value selections
   * are maintained across page loads. Selections are remembered by type (not by
   * row index), so row values can move around and still maintain their
   * selection.
   * 
   * @param enabled true to enable, false to disable
   */
  public void setCrossPageSelectionEnabled(boolean enabled) {
    if (isCrossPageSelectionEnabled != enabled) {
      this.isCrossPageSelectionEnabled = enabled;

      // Reselected only the rows on this page
      if (!enabled) {
        selectedRowValues = new HashSet<RowType>();
        Set<Integer> selectedRows = getDataTable().getSelectedRows();
        for (Integer selectedRow : selectedRows) {
          selectedRowValues.add(getRowValue(selectedRow));
        }
      }
    }
  }

  /**
   * Set the {@link Widget} that will be displayed in place of the data table
   * when the data table has no data to display.
   * 
   * @param emptyTableWidget the widget to display when the data table is empty
   */
  public void setEmptyTableWidget(Widget emptyTableWidget) {
    emptyTableWidgetWrapper.setWidget(emptyTableWidget);
  }

  /**
   * Set whether or not the footer table should be automatically generated.
   * 
   * @param isGenerated true to enable, false to disable
   */
  public void setFooterGenerated(boolean isGenerated) {
    this.isFooterGenerated = isGenerated;
    if (isGenerated) {
      refreshFooterTable();
    }
  }

  /**
   * Set whether or not the header table should be automatically generated.
   * 
   * @param isGenerated true to enable, false to disable
   */
  public void setHeaderGenerated(boolean isGenerated) {
    this.isHeaderGenerated = isGenerated;
    if (isGenerated) {
      refreshHeaderTable();
    }
  }

  /**
   * Set the number of rows per page.
   * 
   * By default, the page size is zero, which indicates that all rows should be
   * shown on the page.
   * 
   * @param pageSize the number of rows per page
   */
  public void setPageSize(int pageSize) {
    pageSize = Math.max(0, pageSize);
    this.pageSize = pageSize;

    int pageCount = getPageCount();
    if (pageCount != oldPageCount) {
      fireEvent(new PageCountChangeEvent(oldPageCount, pageCount));
      oldPageCount = pageCount;
    }

    // Reset the page
    if (currentPage >= 0) {
      gotoPage(currentPage, true);
    }
  }

  /**
   * Associate a row in the table with a value.
   * 
   * @param row the row index
   * @param value the value to associate
   */
  public void setRowValue(int row, RowType value) {
    // Make sure the list can fit the row
    for (int i = rowValues.size(); i <= row; i++) {
      rowValues.add(null);
    }

    // Set the row value
    rowValues.set(row, value);

    // Render the new row value
    refreshRow(row);
  }

  /**
   * Set the {@link TableDefinition} used to define the columns.
   * 
   * @param tableDefinition the new table definition.
   */
  public void setTableDefinition(TableDefinition<RowType> tableDefinition) {
    assert tableDefinition != null : "tableDefinition cannot be null";
    this.tableDefinition = tableDefinition;
  }

  /**
   * Invoke the cell editor on a cell, if one is set. If a cell editor is not
   * specified, this method has no effect.
   */
  protected void editCell(int row, int column) {
    // Get the cell editor
    final ColumnDefinition colDef = getColumnDefinition(column);
    if (colDef == null) {
      return;
    }
    CellEditor cellEditor = colDef.getCellEditor();
    if (cellEditor == null) {
      return;
    }

    // Forward the request to the cell editor
    final RowType rowValue = getRowValue(row);
    CellEditInfo editInfo = new CellEditInfo(getDataTable(), row, column);
    cellEditor.editCell(editInfo, colDef.getCellValue(rowValue),
        new CellEditor.Callback() {
          public void onCancel(CellEditInfo cellEditInfo) {
          }

          public void onComplete(CellEditInfo cellEditInfo, Object cellValue) {
            colDef.setCellValue(rowValue, cellValue);
            if (tableModel instanceof MutableTableModel) {
              int row = getAbsoluteFirstRowIndex() + cellEditInfo.getRowIndex();
              ((MutableTableModel<RowType>) tableModel).setRowValue(row,
                  rowValue);
            } else {
              refreshRow(cellEditInfo.getRowIndex());
            }
          }
        });
  }

  /**
   * Get the {@link ColumnDefinition} currently associated with a column.
   * 
   * @param colIndex the index of the column
   * @return the {@link ColumnDefinition} associated with the column, or null
   */
  protected ColumnDefinition<RowType, ?> getColumnDefinition(int colIndex) {
    if (colIndex < visibleColumns.size()) {
      return visibleColumns.get(colIndex);
    }
    return null;
  }

  /**
   * @return the index of the first visible row
   * @deprecated use {@link #getAbsoluteFirstRowIndex()} instead
   */
  @Deprecated
  protected int getFirstRow() {
    return getAbsoluteFirstRowIndex();
  }

  /**
   * @return the index of the last visible row
   * @deprecated use {@link #getAbsoluteLastRowIndex()} instead
   */
  @Deprecated
  protected int getLastRow() {
    return getAbsoluteLastRowIndex();
  }

  /**
   * Get the list of row values associated with the table.
   * 
   * @return the list of row value
   */
  protected List<RowType> getRowValues() {
    return rowValues;
  }

  /**
   * @return the header widget used to select all rows
   */
  protected Widget getSelectAllWidget() {
    if (selectAllWidget == null) {
      final CheckBox box = new CheckBox();
      selectAllWidget = box;
      box.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          if (box.getValue()) {
            getDataTable().selectAllRows();
          } else {
            getDataTable().deselectAllRows();
          }
        }
      });
    }
    return selectAllWidget;
  }

  /**
   * @return the list of current visible column definitions
   */
  protected List<ColumnDefinition<RowType, ?>> getVisibleColumnDefinitions() {
    return visibleColumns;
  }

  /**
   * Insert a row into the table relative to the total number of rows.
   * 
   * @param beforeRow the row index
   */
  protected void insertAbsoluteRow(int beforeRow) {
    // Physically insert the row
    int lastRow = getAbsoluteLastRowIndex() + 1;
    if (beforeRow <= lastRow) {
      int firstRow = getAbsoluteFirstRowIndex();
      if (beforeRow >= firstRow) {
        // Insert row in the middle of the page
        getDataTable().insertRow(beforeRow - firstRow);
      } else {
        // Insert zero row because row is before this page
        getDataTable().insertRow(0);
      }
      if (getDataTable().getRowCount() > pageSize) {
        getDataTable().removeRow(pageSize);
      }
    }
  }

  /**
   * Called when the data table has finished rendering.
   */
  protected void onDataTableRendered() {
    // Refresh the headers if needed
    if (headersObsolete) {
      refreshHeaderTable();
      refreshFooterTable();
      headersObsolete = false;
    }

    // Select rows
    FixedWidthGrid dataTable = getDataTable();
    int rowCount = dataTable.getRowCount();
    for (int i = 0; i < rowCount; i++) {
      if (selectedRowValues.contains(getRowValue(i))) {
        dataTable.selectRow(i, false);
      }
    }

    // Update the UI of the table
    dataTable.clearIdealWidths();
    redraw();
    isPageLoading = false;
    fireEvent(new PageLoadEvent(currentPage));
  }

  /**
   * Update the footer table based on the new {@link ColumnDefinition}.
   */
  protected void refreshFooterTable() {
    if (!isFooterGenerated) {
      return;
    }

    // Generate the list of lists of ColumnHeaderInfo.
    List<List<ColumnHeaderInfo>> allInfos = new ArrayList<List<ColumnHeaderInfo>>();
    int columnCount = visibleColumns.size();
    int footerCounts[] = new int[columnCount];
    int maxFooterCount = 0;
    for (int col = 0; col < columnCount; col++) {
      // Get the header property.
      ColumnDefinition<RowType, ?> colDef = visibleColumns.get(col);
      FooterProperty prop = colDef.getColumnProperty(FooterProperty.TYPE);
      int footerCount = prop.getFooterCount();
      footerCounts[col] = footerCount;
      maxFooterCount = Math.max(maxFooterCount, footerCount);

      // Add each ColumnHeaderInfo
      List<ColumnHeaderInfo> infos = new ArrayList<ColumnHeaderInfo>();
      ColumnHeaderInfo prev = null;
      for (int row = 0; row < footerCount; row++) {
        Object footer = prop.getFooter(row, col);
        if (prev != null && prev.header.equals(footer)) {
          prev.incrementRowSpan();
        } else {
          prev = new ColumnHeaderInfo(footer);
          infos.add(prev);
        }
      }
      allInfos.add(infos);
    }

    // Return early if there is no footer
    if (maxFooterCount == 0) {
      return;
    }

    // Fill in missing rows
    for (int col = 0; col < columnCount; col++) {
      int footerCount = footerCounts[col];
      if (footerCount < maxFooterCount) {
        allInfos.get(col).add(
            new ColumnHeaderInfo(null, maxFooterCount - footerCount));
      }
    }

    // Ensure that we have a footer table
    if (getFooterTable() == null) {
      setFooterTable(new FixedWidthFlexTable());
    }

    // Refresh the table
    refreshHeaderTable(getFooterTable(), allInfos, false);
  }

  /**
   * Update the header table based on the new {@link ColumnDefinition}.
   */
  protected void refreshHeaderTable() {
    if (!isHeaderGenerated) {
      return;
    }

    // Generate the list of lists of ColumnHeaderInfo.
    List<List<ColumnHeaderInfo>> allInfos = new ArrayList<List<ColumnHeaderInfo>>();
    int columnCount = visibleColumns.size();
    int headerCounts[] = new int[columnCount];
    int maxHeaderCount = 0;
    for (int col = 0; col < columnCount; col++) {
      // Get the header property.
      ColumnDefinition<RowType, ?> colDef = visibleColumns.get(col);
      HeaderProperty prop = colDef.getColumnProperty(HeaderProperty.TYPE);
      int headerCount = prop.getHeaderCount();
      headerCounts[col] = headerCount;
      maxHeaderCount = Math.max(maxHeaderCount, headerCount);

      // Add each ColumnHeaderInfo
      List<ColumnHeaderInfo> infos = new ArrayList<ColumnHeaderInfo>();
      ColumnHeaderInfo prev = null;
      for (int row = 0; row < headerCount; row++) {
        Object header = prop.getHeader(row, col);
        if (prev != null && prev.header.equals(header)) {
          prev.incrementRowSpan();
        } else {
          prev = new ColumnHeaderInfo(header);
          infos.add(0, prev);
        }
      }
      allInfos.add(infos);
    }

    // Return early if there is no header
    if (maxHeaderCount == 0) {
      return;
    }

    // Fill in missing rows
    for (int col = 0; col < columnCount; col++) {
      int headerCount = headerCounts[col];
      if (headerCount < maxHeaderCount) {
        allInfos.get(col).add(0,
            new ColumnHeaderInfo(null, maxHeaderCount - headerCount));
      }
    }

    // Refresh the table
    refreshHeaderTable(getHeaderTable(), allInfos, true);
  }

  /**
   * Refresh the list of the currently visible column definitions based on the
   * {@link TableDefinition}.
   */
  protected void refreshVisibleColumnDefinitions() {
    List<ColumnDefinition<RowType, ?>> colDefs = new ArrayList<ColumnDefinition<RowType, ?>>(
        tableDefinition.getVisibleColumnDefinitions());
    if (!colDefs.equals(visibleColumns)) {
      visibleColumns = colDefs;
      headersObsolete = true;
    } else {
      // Check if any of the headers are dynamic
      for (ColumnDefinition<RowType, ?> colDef : colDefs) {
        if (colDef.getColumnProperty(HeaderProperty.TYPE).isDynamic()
            || colDef.getColumnProperty(FooterProperty.TYPE).isDynamic()) {
          headersObsolete = true;
          return;
        }
      }
    }
  }

  /**
   * Remove a row from the table relative to the total number of rows.
   * 
   * @param row the row index
   */
  protected void removeAbsoluteRow(int row) {
    // Physically remove the row if it is in the middle of the data table
    int firstRow = getAbsoluteFirstRowIndex();
    int lastRow = getAbsoluteLastRowIndex();
    if (row <= lastRow && row >= firstRow) {
      FixedWidthGrid dataTable = getDataTable();
      int relativeRow = row - firstRow;
      if (relativeRow < dataTable.getRowCount()) {
        dataTable.removeRow(relativeRow);
      }
    }
  }

  /**
   * Set a block of data. This method is used when responding to data requests.
   * 
   * This method takes an iterator of iterators, where each iterator represents
   * one row of data starting with the first row.
   * 
   * @param firstRow the row index that the rows iterator starts with
   * @param rows the values associated with each row
   */
  protected void setData(int firstRow, Iterator<RowType> rows) {
    getDataTable().deselectAllRows();
    rowValues = new ArrayList<RowType>();
    if (rows != null && rows.hasNext()) {
      setEmptyTableWidgetVisible(false);

      // Get an iterator over the visible rows
      int firstVisibleRow = getAbsoluteFirstRowIndex();
      int lastVisibleRow = getAbsoluteLastRowIndex();
      Iterator<RowType> visibleIter = new VisibleRowsIterator(rows, firstRow,
          firstVisibleRow, lastVisibleRow);

      // Set the row values
      while (visibleIter.hasNext()) {
        rowValues.add(visibleIter.next());
      }

      // Copy the visible column definitions
      refreshVisibleColumnDefinitions();

      // Render using the bulk renderer
      if (bulkRenderer != null) {
        bulkRenderer.renderRows(rowValues.iterator(), tableRendererCallback);
        return;
      }

      // Get rid of unneeded rows and columns
      int rowCount = rowValues.size();
      int colCount = visibleColumns.size();
      getDataTable().resize(rowCount, colCount);

      // Render the rows
      tableDefinition.renderRows(0, rowValues.iterator(), rowView);
    } else {
      setEmptyTableWidgetVisible(true);
    }

    // Fire page loaded event
    onDataTableRendered();
  }

  /**
   * Set whether or not the empty table widget is visible.
   * 
   * @param visible true to show the empty table widget
   */
  protected void setEmptyTableWidgetVisible(boolean visible) {
    emptyTableWidgetWrapper.setVisible(visible);
    if (visible) {
      getDataWrapper().getStyle().setProperty("display", "none");
    } else {
      getDataWrapper().getStyle().setProperty("display", "");
    }
  }

  /**
   * Update the header or footer tables based on the new
   * {@link ColumnDefinition}.
   * 
   * @param table the header or footer table
   * @param allInfos the header info
   * @param isHeader false if refreshing the footer table
   */
  private void refreshHeaderTable(FixedWidthFlexTable table,
      List<List<ColumnHeaderInfo>> allInfos, boolean isHeader) {
    // Return if we have no column definitions.
    if (visibleColumns == null) {
      return;
    }

    // Reset the header table.
    int rowCount = table.getRowCount();
    for (int i = 0; i < rowCount; i++) {
      table.removeRow(0);
    }

    // Generate the header table
    int columnCount = allInfos.size();
    FlexCellFormatter formatter = table.getFlexCellFormatter();
    List<ColumnHeaderInfo> prevInfos = null;
    for (int col = 0; col < columnCount; col++) {
      List<ColumnHeaderInfo> infos = allInfos.get(col);
      int row = 0;
      for (ColumnHeaderInfo info : infos) {
        // Get the actual row and cell index
        int rowSpan = info.getRowSpan();
        int cell = 0;
        if (table.getRowCount() > row) {
          cell = table.getCellCount(row);
        }

        // Compare to the cell in the previous column
        if (prevInfos != null) {
          boolean headerAdded = false;
          int prevRow = 0;
          for (ColumnHeaderInfo prevInfo : prevInfos) {
            // Increase the colSpan of the previous cell
            if (prevRow == row && info.equals(prevInfo)) {
              int colSpan = formatter.getColSpan(row, cell - 1);
              formatter.setColSpan(row, cell - 1, colSpan + 1);
              headerAdded = true;
              break;
            }
            prevRow += prevInfo.getRowSpan();
          }

          if (headerAdded) {
            row += rowSpan;
            continue;
          }
        }

        // Set the new header
        Object header = info.getHeader();
        if (header instanceof Widget) {
          table.setWidget(row, cell, (Widget) header);
        } else {
          table.setHTML(row, cell, header.toString());
        }

        // Update the rowSpan
        if (rowSpan > 1) {
          formatter.setRowSpan(row, cell, rowSpan);
        }

        // Increment the row
        row += rowSpan;
      }

      // Increment the previous info
      prevInfos = infos;
    }

    // Insert the checkbox column
    SelectionPolicy selectionPolicy = getDataTable().getSelectionPolicy();
    if (selectionPolicy.hasInputColumn()) {
      // Get the select all box
      Widget box = null;
      if (isHeader
          && getDataTable().getSelectionPolicy() == SelectionPolicy.CHECKBOX) {
        box = getSelectAllWidget();
      }

      // Add the offset column
      table.insertCell(0, 0);
      if (box != null) {
        table.setWidget(0, 0, box);
      } else {
        table.setHTML(0, 0, "&nbsp;");
      }
      formatter.setRowSpan(0, 0, table.getRowCount());
      formatter.setHorizontalAlignment(0, 0,
          HasHorizontalAlignment.ALIGN_CENTER);
      table.setColumnWidth(0, getDataTable().getInputColumnWidth());
    }
  }

  /**
   * Refresh a single row in the table.
   * 
   * @param rowIndex the index of the row
   */
  private void refreshRow(int rowIndex) {
    final RowType rowValue = getRowValue(rowIndex);
    Iterator<RowType> singleIterator = new Iterator<RowType>() {
      private boolean nextCalled = false;

      public boolean hasNext() {
        return !nextCalled;
      }

      public RowType next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        nextCalled = true;
        return rowValue;
      }

      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
    tableDefinition.renderRows(rowIndex, singleIterator, rowView);
  }
}
