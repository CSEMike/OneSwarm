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

import com.google.gwt.core.client.Duration;
import com.google.gwt.dom.client.Document;
import com.google.gwt.gen2.table.client.TableDefinition.AbstractCellView;
import com.google.gwt.gen2.table.client.TableDefinition.AbstractRowView;
import com.google.gwt.gen2.table.client.TableModelHelper.Request;
import com.google.gwt.gen2.table.client.TableModelHelper.Response;
import com.google.gwt.gen2.table.override.client.HTMLTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Enables bulk rendering of tables. Each subclass that needs special handling
 * for bulk rendering should have its own bulk renderer.
 * 
 * @param <RowType> the data type of the row values
 */
public abstract class TableBulkRenderer<RowType> implements
    HasTableDefinition<RowType> {
  /**
   * A simple class that keeps track of a widget that needs to be add to the
   * table after it finishes loading.
   */
  private static class DelayedWidget {
    public int cellIndex;
    public int rowIndex;
    public Widget widget;

    /**
     * Construct a new {@link DelayedWidget}.
     */
    public DelayedWidget(int rowIndex, int cellIndex, Widget widget) {
      this.rowIndex = rowIndex;
      this.cellIndex = cellIndex;
      this.widget = widget;
    }
  }

  /**
   * A custom {@link AbstractCellView} used by the {@link TableBulkRenderer}.
   * 
   * @param <RowType> the data type of the row values
   */
  protected static class BulkCellView<RowType> extends
      AbstractCellView<RowType> {
    /**
     * A {@link StringBuffer} used to assemble the HTML of the table.
     */
    private StringBuffer buffer = null;

    /**
     * An element used to convert text to escaped html.
     */
    private Element htmlCleaner = Document.get().createDivElement().cast();

    /**
     * The horizontal alignment to apply to the current cell.
     */
    private HorizontalAlignmentConstant curCellHorizontalAlign = null;

    /**
     * The html string to add to the cell.
     */
    private String curCellHtml = null;

    /**
     * The widget to add to the current cell.
     */
    private Widget curCellWidget = null;

    /**
     * The style attributes to apply to the current cell.
     */
    private Map<String, String> curCellStyles = new HashMap<String, String>();

    /**
     * The style name to be applied to the current cell.
     */
    private String curCellStyleName = null;

    /**
     * The vertical alignment to apply to the current cell.
     */
    private VerticalAlignmentConstant curCellVerticalAlign = null;

    /**
     * The {@link Widget Widgets} that need to be added to the table after it
     * has finished loading.
     */
    private List<DelayedWidget> delayedWidgets = new ArrayList<DelayedWidget>();

    /**
     * Construct a new {@link TableBulkRenderer.BulkCellView}.
     * 
     * @param bulkRenderer the renderer
     */
    public BulkCellView(TableBulkRenderer<RowType> bulkRenderer) {
      super((bulkRenderer.source == null) ? bulkRenderer : bulkRenderer.source);
    }

    @Override
    public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
      curCellHorizontalAlign = align;
    }

    @Override
    public void setHTML(String html) {
      curCellWidget = null;
      curCellHtml = html;
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      curCellStyles.put(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      curCellStyleName = stylename;
    }

    @Override
    public void setText(String text) {
      htmlCleaner.setInnerText(text);
      setHTML(htmlCleaner.getInnerHTML());
    }

    @Override
    public void setVerticalAlignment(VerticalAlignmentConstant align) {
      curCellVerticalAlign = align;
    }

    @Override
    public void setWidget(Widget widget) {
      curCellHtml = null;
      curCellWidget = widget;
    }

    protected StringBuffer getStringBuffer() {
      return buffer;
    }

    @Override
    protected void renderRowValue(RowType rowValue, ColumnDefinition columnDef) {
      curCellHtml = null;
      curCellWidget = null;
      curCellStyleName = null;
      curCellHorizontalAlign = null;
      curCellVerticalAlign = null;
      curCellStyles.clear();
      super.renderRowValue(rowValue, columnDef);

      // Save the widget until rendering is complete
      if (curCellWidget != null) {
        int row = getRowIndex();
        int cell = getCellIndex();
        delayedWidgets.add(new DelayedWidget(row, cell, curCellWidget));
      }

      // Add the open tag
      buffer.append("<td");
      if (curCellHorizontalAlign != null) {
        buffer.append(" align=\"");
        buffer.append(curCellHorizontalAlign.getTextAlignString());
        buffer.append("\"");
      }
      if (curCellVerticalAlign != null) {
        curCellStyles.put("verticalAlign",
            curCellVerticalAlign.getVerticalAlignString());
      }
      if (curCellStyleName != null) {
        buffer.append(" class=\"");
        buffer.append(curCellStyleName);
        buffer.append("\"");
      }
      if (curCellStyles.size() > 0) {
        buffer.append(" style=\"");
        for (Map.Entry<String, String> entry : curCellStyles.entrySet()) {
          buffer.append(entry.getKey());
          buffer.append(":");
          buffer.append(entry.getValue());
          buffer.append(";");
        }
        buffer.append("\"");
      }
      buffer.append(">");

      // Add contents
      if (curCellHtml != null) {
        buffer.append(curCellHtml);
      }

      // Add close tag
      buffer.append("</td>");
    }

    /**
     * Visible for testing.
     * 
     * @return the cell's html contents.
     */
    String getHtml() {
      return curCellHtml;
    }
  }

  /**
   * A custom {@link AbstractRowView} used by the {@link PagingScrollTable}.
   * 
   * @param <RowType> the type of the row values
   */
  protected static class BulkRowView<RowType> extends AbstractRowView<RowType> {
    /**
     * A {@link StringBuffer} used to assemble the HTML of the table.
     */
    private StringBuffer buffer;

    /**
     * The bulk renderer doing the rendering.
     */
    private TableBulkRenderer<RowType> bulkRenderer;

    /**
     * The view of the cell.
     */
    private BulkCellView<RowType> cellView;

    /**
     * The style attributes to apply to the current cell.
     */
    private Map<String, String> curRowStyles = new HashMap<String, String>();

    /**
     * The style name to be applied to the current cell.
     */
    private String curRowStyleName = null;

    /**
     * The {@link RenderingOptions} to apply to the table.
     */
    private RenderingOptions options;

    /**
     * The current row index.
     */
    private int rowIndex = 0;

    /**
     * Construct a new {@link TableBulkRenderer.BulkRowView}.
     * 
     * @param cellView the view of the cell
     * @param bulkRenderer the renderer
     * @param options the {@link RenderingOptions} to apply to the table
     */
    public BulkRowView(BulkCellView<RowType> cellView,
        TableBulkRenderer<RowType> bulkRenderer, RenderingOptions options) {
      super(cellView);
      this.bulkRenderer = bulkRenderer;
      this.cellView = cellView;
      this.options = options;

      // Create a string buffer to assemble the table
      buffer = new StringBuffer();
      cellView.buffer = buffer;
    }

    @Override
    public void setStyleAttribute(String attr, String value) {
      curRowStyles.put(attr, value);
    }

    @Override
    public void setStyleName(String stylename) {
      curRowStyleName = stylename;
    }

    protected StringBuffer getStringBuffer() {
      return buffer;
    }

    @Override
    protected void renderRowImpl(int rowIndex, RowType rowValue,
        RowRenderer<RowType> rowRenderer,
        List<ColumnDefinition<RowType, ?>> visibleColumns) {
      super.renderRowImpl(rowIndex, rowValue, rowRenderer, visibleColumns);
      buffer.append("</tr>");
    }

    @Override
    protected void renderRowsImpl(int startRowIndex,
        final Iterator<RowType> rowValues,
        final RowRenderer<RowType> rowRenderer,
        final List<ColumnDefinition<RowType, ?>> visibleColumns) {
      // Create the table
      buffer.append("<table><tbody>");
      if (options.headerRow != null) {
        buffer.append(options.headerRow);
      }

      // Reset the row index
      rowIndex = startRowIndex;
      final int myStamp = ++bulkRenderer.requestStamp;

      // Use an incremental command to render rows in increments
      class RenderTableCommand implements IncrementalCommand {
        public boolean execute() {
          // Poor man's cancel() event.
          if (myStamp != bulkRenderer.requestStamp) {
            return false;
          }
          int checkRow = ROWS_PER_TIME_CHECK;
          double endSlice = Duration.currentTimeMillis() + TIME_SLICE;

          // Loop through the rows
          while (rowValues.hasNext()) {
            // Check if we've exceed the time slice
            if (options.syncCall == false && --checkRow == 0) {
              checkRow = ROWS_PER_TIME_CHECK;
              double time = Duration.currentTimeMillis();
              if (time > endSlice) {
                return true;
              }
            }

            // Render a single row and increment the row index
            renderRowImpl(rowIndex, rowValues.next(), rowRenderer,
                visibleColumns);
            rowIndex++;
          }

          // Add the footer row
          if (options.footerRow != null) {
            buffer.append(options.footerRow);
          }

          // Finish rendering the table
          buffer.append("</tbody></table>");
          bulkRenderer.renderRows(buffer.toString());

          // Add widgets into the table
          for (DelayedWidget dw : cellView.delayedWidgets) {
            bulkRenderer.setWidgetRaw(bulkRenderer.getTable(), dw.rowIndex,
                dw.cellIndex, dw.widget);
          }

          // Trigger the callback
          if (options.callback != null) {
            options.callback.onRendered();
          }
          return false;
        }
      }

      // Fire the incremental command
      RenderTableCommand renderTable = new RenderTableCommand();
      if (renderTable.execute()) {
        DeferredCommand.addCommand(renderTable);
      }
    }

    @Override
    protected void renderRowValue(RowType rowValue,
        RowRenderer<RowType> rowRenderer) {
      curRowStyleName = null;
      curRowStyles.clear();
      super.renderRowValue(rowValue, rowRenderer);

      // Add the open tag
      buffer.append("<tr");
      if (curRowStyleName != null) {
        buffer.append(" class=\"");
        buffer.append(curRowStyleName);
        buffer.append("\"");
      }
      if (curRowStyles.size() > 0) {
        buffer.append(" style=\"");
        for (Map.Entry<String, String> entry : curRowStyles.entrySet()) {
          buffer.append(entry.getKey());
          buffer.append(":");
          buffer.append(entry.getValue());
          buffer.append(";");
        }
        buffer.append("\"");
      }
      buffer.append(">");
    }
  }

  /**
   * Convenience class used to specify rendering options for the table.
   */
  protected static class RenderingOptions {
    public int startRow = 0;
    public int numRows = MutableTableModel.ALL_ROWS;
    public boolean syncCall = false;
    public String headerRow = null;
    public String footerRow = null;
    public RendererCallback callback = null;
  }

  /**
   * Time slice in milliseconds that the construction of the string can take
   * before flushing the event cue.
   */
  public static int TIME_SLICE = 1000;

  /**
   * How many rows should be processed before time is checked and the event loop
   * is potentially flushed.
   */
  // TODO check how long time check takes to see if this guard is worth having.
  public static int ROWS_PER_TIME_CHECK = 10;

  /**
   * Scratch Element used to render table and row strings.
   */
  private static Element WRAPPER_DIV;

  /**
   * Stamp used to detect when a request has been orphaned.
   */
  private int requestStamp = 0;

  /**
   * The external source of the rendering requests.
   */
  private HasTableDefinition<RowType> source = null;

  /**
   * Table to be bulk rendered.
   */
  private final HTMLTable table;

  /**
   * The definition of the columns.
   */
  private TableDefinition<RowType> tableDefinition;

  /**
   * Constructor for the bulk renderer.
   * 
   * @param table the table to be bulk rendered
   * @param tableDefinition the renderer that should be used during bulk
   *          rendering
   */
  public TableBulkRenderer(HTMLTable table,
      TableDefinition<RowType> tableDefinition) {
    this.table = table;
    this.tableDefinition = tableDefinition;
  }

  /**
   * Constructor for the bulk renderer.
   * 
   * @param table the table to be bulk rendered
   * @param sourceTableDef the external source of the table definition
   */
  public TableBulkRenderer(HTMLTable table,
      HasTableDefinition<RowType> sourceTableDef) {
    this(table, sourceTableDef.getTableDefinition());
    this.source = sourceTableDef;
  }

  public TableDefinition<RowType> getTableDefinition() {
    return (source == null) ? tableDefinition : source.getTableDefinition();
  }

  /**
   * Removes all rows in the current table replaces them with the rows provided.
   * <p>
   * This method should only be used when the number of rows is known and of
   * reasonable size, therefore this call is synchronous by default.
   * </p>
   * 
   * @param rows {@link Iterable} of row values
   */
  public final void renderRows(Iterable<RowType> rows) {
    renderRows(rows, null);
  }

  /**
   * Removes all rows in the current table replaces them with the rows provided.
   * <p>
   * This method should only be used when the number of rows is known and of
   * reasonable size, therefore this call is synchronous by default.
   * </p>
   * 
   * @param rows {@link Iterable} of row values
   * @param callback callback to be called after the rows are rendered
   */
  public final void renderRows(Iterable<RowType> rows, RendererCallback callback) {
    IterableTableModel<RowType> tableModel = new IterableTableModel<RowType>(
        rows);
    RenderingOptions options = createRenderingOptions();
    options.syncCall = true;
    options.callback = callback;
    renderRows(tableModel, options);
  }

  /**
   * Removes all rows in the current table replaces them with the rows supplied
   * by the iterator. Each element of the rows iterator is an {@link Iterator}
   * which represents a single row.
   * 
   * @param rows iterator of row values
   * @param callback callback to be called after the rows are rendered
   */
  public final void renderRows(Iterator<RowType> rows, RendererCallback callback) {
    RenderingOptions options = createRenderingOptions();
    options.callback = callback;
    renderRows(rows, options);
  }

  /**
   * Removes all rows in the current table and replaces them with the rows
   * supplied by the provided {@link MutableTableModel}.
   * 
   * @param tableModel the table data
   * @param startRow the tableModel's start row index
   * @param numRows the number of rows to request from the tableModel -1
   *          indicates all of them *
   * @param callback callback to call after the table is finished being rendered
   */
  public final void renderRows(MutableTableModel<RowType> tableModel,
      int startRow, int numRows, RendererCallback callback) {
    RenderingOptions options = createRenderingOptions();
    options.startRow = startRow;
    options.numRows = numRows;
    options.callback = callback;
    renderRows(tableModel, options);
  }

  /**
   * Removes all rows in the current table and replaces them with the rows
   * supplied by the provided {@link MutableTableModel}.
   * 
   * @param tableModel the table model
   * @param callback callback to call after the table is finished being rendered
   */
  public final void renderRows(MutableTableModel<RowType> tableModel,
      RendererCallback callback) {
    renderRows(tableModel, 0, MutableTableModel.ALL_ROWS, callback);
  }

  /**
   * Creates the rendering options associated with this renderer.
   * 
   * @return the rendering options
   */
  protected RenderingOptions createRenderingOptions() {
    return new RenderingOptions();
  }

  /**
   * Create an {@link AbstractRowView} that can be used to render the table.
   * 
   * @param options the {@link RenderingOptions}
   * @return the row view
   */
  protected AbstractRowView<RowType> createRowView(
      final RenderingOptions options) {
    BulkCellView<RowType> cellView = new BulkCellView<RowType>(this);
    return new BulkRowView<RowType>(cellView, this, options);
  }

  /**
   * Gets the table.
   * 
   * @returns the current html table.
   */
  protected HTMLTable getTable() {
    return table;
  }

  /**
   * Work horse protected rendering method.
   * 
   * @param rows Iterator of row iterators
   * @param options rendering options for this table
   */
  protected void renderRows(final Iterator<RowType> rows,
      final RenderingOptions options) {
    getTableDefinition().renderRows(0, rows, createRowView(options));
  }

  /**
   * Render rows using a table model.
   * 
   * @param tableModel table model
   * @param options options
   */
  protected final void renderRows(TableModel<RowType> tableModel,
      final RenderingOptions options) {

    // Create a callback to handle the request
    TableModel.Callback<RowType> requestCallback = new TableModel.Callback<RowType>() {
      public void onFailure(Throwable caught) {
      }

      public void onRowsReady(Request request, final Response<RowType> response) {
        final Iterator<RowType> rows = response.getRowValues();
        renderRows(rows, options);
      }
    };

    tableModel.requestRows(new Request(options.startRow, options.numRows),
        requestCallback);
  }

  protected void renderRows(String rawHTMLTable) {
    DOM.setInnerHTML(getWrapperDiv(), rawHTMLTable);
    Element tableElement = DOM.getFirstChild(getWrapperDiv());
    Element newBody = replaceBodyElement(table.getElement(), tableElement);
    setBodyElement(table, newBody);
  }

  private Element getWrapperDiv() {
    if (WRAPPER_DIV == null) {
      WRAPPER_DIV = DOM.createElement("div");
    }
    return WRAPPER_DIV;
  }

  /**
   * Replace a table's body element with the body element from another table.
   * 
   * @param table the table element who's body will be replaced
   * @param thatBody the table element with the donor body
   */
  private native Element replaceBodyElement(Element table, Element thatBody)
  /*-{
    table.removeChild(table.tBodies[0]);
    var thatChild = thatBody.tBodies[0];
    table.appendChild(thatChild);
    return thatChild;
  }-*/;

  /**
   * Short term hack to get protected setBodyElement.
   */
  private native void setBodyElement(HTMLTable table, Element newBody)
  /*-{
    table.@com.google.gwt.gen2.table.override.client.HTMLTable::setBodyElement(Lcom/google/gwt/user/client/Element;)(newBody);
  }-*/;

  /**
   * Set a widget without clearing its contents.
   */
  private native void setWidgetRaw(HTMLTable table, int row, int cell,
      Widget widget)
  /*-{
    table.@com.google.gwt.gen2.table.override.client.HTMLTable::setWidgetRaw(IILcom/google/gwt/user/client/ui/Widget;)(row, cell, widget);
  }-*/;
}
