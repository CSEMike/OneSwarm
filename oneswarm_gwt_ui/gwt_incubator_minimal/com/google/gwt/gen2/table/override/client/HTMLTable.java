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
package com.google.gwt.gen2.table.override.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.TableListenerCollection;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class should replace the actual class of the same name.
 * 
 * TODO: Incorporate changes into actual class.
 * 
 * Steps to incorporate: 1. Replace "OverrideDOM." with "DOM." 2. Copy contents
 * to actual HTMLTable class
 */
public abstract class HTMLTable extends Panel implements SourcesTableEvents {
  /**
   * This class contains methods used to format a table's cells.
   */
  public class CellFormatter {
    /**
     * Adds a style to the specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     */
    public void addStyleName(int row, int column, String styleName) {
      prepareCell(row, column);
      Element td = getRawElement(row, column);
      UIObject.setStyleName(td, styleName, true);
    }

    /**
     * Gets the TD element representing the specified cell.
     * 
     * @param row the row of the cell to be retrieved
     * @param column the column of the cell to be retrieved
     * @return the column's TD element
     * @throws IndexOutOfBoundsException
     */
    public Element getElement(int row, int column) {
      checkCellBounds(row, column);
      return getRawElement(row, column);
    }

    /**
     * Gets the style of a specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @see UIObject#getStyleName()
     * @return returns the style name
     * @throws IndexOutOfBoundsException
     */
    public String getStyleName(int row, int column) {
      return UIObject.getStyleName(getElement(row, column));
    }

    /**
     * Gets the primary style of a specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @see UIObject#getStylePrimaryName()
     * @return returns the style name
     * @throws IndexOutOfBoundsException
     */
    public String getStylePrimaryName(int row, int column) {
      return UIObject.getStylePrimaryName(getElement(row, column));
    }

    /**
     * Determines whether or not this cell is visible.
     * 
     * @param row the row of the cell whose visibility is to be set
     * @param column the column of the cell whose visibility is to be set
     * @return <code>true</code> if the object is visible
     */
    public boolean isVisible(int row, int column) {
      Element e = getElement(row, column);
      return UIObject.isVisible(e);
    }

    /**
     * Removes a style from the specified cell.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int row, int column, String styleName) {
      checkCellBounds(row, column);
      Element td = getRawElement(row, column);
      UIObject.setStyleName(td, styleName, false);
    }

    /**
     * Sets the horizontal and vertical alignment of the specified cell's
     * contents.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param hAlign the cell's new horizontal alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasHorizontalAlignment}
     * @param vAlign the cell's new vertical alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasVerticalAlignment}
     * @throws IndexOutOfBoundsException
     */
    public void setAlignment(int row, int column, HorizontalAlignmentConstant hAlign,
        VerticalAlignmentConstant vAlign) {
      setHorizontalAlignment(row, column, hAlign);
      setVerticalAlignment(row, column, vAlign);
    }

    /**
     * Sets the height of the specified cell.
     * 
     * @param row the row of the cell whose height is to be set
     * @param column the cell whose height is to be set
     * @param height the cell's new height, in CSS units
     * @throws IndexOutOfBoundsException
     */
    public void setHeight(int row, int column, String height) {
      prepareCell(row, column);
      Element elem = getRawElement(row, column);
      DOM.setElementProperty(elem, "height", height);
    }

    /**
     * Sets the horizontal alignment of the specified cell.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param align the cell's new horizontal alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasHorizontalAlignment}.
     * @throws IndexOutOfBoundsException
     */
    public void setHorizontalAlignment(int row, int column, HorizontalAlignmentConstant align) {
      prepareCell(row, column);
      Element elem = getRawElement(row, column);
      DOM.setElementProperty(elem, "align", align.getTextAlignString());
    }

    /**
     * Sets the style name associated with the specified cell.
     * 
     * @param row the row of the cell whose style name is to be set
     * @param column the column of the cell whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int row, int column, String styleName) {
      prepareCell(row, column);
      UIObject.setStyleName(getRawElement(row, column), styleName);
    }

    /**
     * Sets the primary style name associated with the specified cell.
     * 
     * @param row the row of the cell whose style name is to be set
     * @param column the column of the cell whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStylePrimaryName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStylePrimaryName(int row, int column, String styleName) {
      UIObject.setStylePrimaryName(getRawElement(row, column), styleName);
    }

    /**
     * Sets the vertical alignment of the specified cell.
     * 
     * @param row the row of the cell whose alignment is to be set
     * @param column the cell whose alignment is to be set
     * @param align the cell's new vertical alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasVerticalAlignment}.
     * @throws IndexOutOfBoundsException
     */
    public void setVerticalAlignment(int row, int column, VerticalAlignmentConstant align) {
      prepareCell(row, column);
      DOM.setStyleAttribute(getRawElement(row, column), "verticalAlign",
          align.getVerticalAlignString());
    }

    /**
     * Sets whether this cell is visible via the display style property. The
     * other cells in the row will all shift left to fill the cell's space. So,
     * for example a table with (0,1,2) will become (1,2) if cell 1 is hidden.
     * 
     * @param row the row of the cell whose visibility is to be set
     * @param column the column of the cell whose visibility is to be set
     * @param visible <code>true</code> to show the cell, <code>false</code>
     *          to hide it
     */
    public void setVisible(int row, int column, boolean visible) {
      Element e = ensureElement(row, column);
      UIObject.setVisible(e, visible);
    }

    /**
     * Sets the width of the specified cell.
     * 
     * @param row the row of the cell whose width is to be set
     * @param column the cell whose width is to be set
     * @param width the cell's new width, in CSS units
     * @throws IndexOutOfBoundsException
     */
    public void setWidth(int row, int column, String width) {
      // Give the subclass a chance to prepare the cell.
      prepareCell(row, column);
      DOM.setElementProperty(getRawElement(row, column), "width", width);
    }

    /**
     * Sets whether the specified cell will allow word wrapping of its contents.
     * 
     * @param row the row of the cell whose word-wrap is to be set
     * @param column the cell whose word-wrap is to be set
     * @param wrap <code>false </code> to disable word wrapping in this cell
     * @throws IndexOutOfBoundsException
     */
    public void setWordWrap(int row, int column, boolean wrap) {
      prepareCell(row, column);
      String wrapValue = wrap ? "" : "nowrap";
      DOM.setStyleAttribute(getElement(row, column), "whiteSpace", wrapValue);
    }

    /**
     * Gets the element associated with a cell. If it does not exist and the
     * subtype allows creation of elements, creates it.
     * 
     * @param row the cell's row
     * @param column the cell's column
     * @return the cell's element
     * @throws IndexOutOfBoundsException
     */
    protected Element ensureElement(int row, int column) {
      prepareCell(row, column);
      return getRawElement(row, column);
    }

    /**
     * Convenience methods to get an attribute on a cell.
     * 
     * @param row cell's row
     * @param column cell's column
     * @param attr attribute to get
     * @return the attribute's value
     * @throws IndexOutOfBoundsException
     */
    protected String getAttr(int row, int column, String attr) {
      Element elem = getElement(row, column);
      return DOM.getElementAttribute(elem, attr);
    }

    /**
     * Gets the TD element representing the specified cell unsafely (meaning
     * that it doesn't ensure that the row and column are valid).
     * 
     * @param row the row of the cell to be retrieved
     * @param column the column of the cell to be retrieved
     * @return the column's TD element
     */
    protected Element getRawElement(int row, int column) {
      return getCellElement(bodyElem, row, column);
    }

    /**
     * Convenience methods to set an attribute on a cell.
     * 
     * @param row cell's row
     * @param column cell's column
     * @param attrName attribute to set
     * @param value value to set
     * @throws IndexOutOfBoundsException
     */
    protected void setAttr(int row, int column, String attrName, String value) {
      Element elem = ensureElement(row, column);
      DOM.setElementAttribute(elem, attrName, value);
    }

    /**
     * Native method to get a cell's element.
     * 
     * @param table the table element
     * @param row the row of the cell
     * @param col the column of the cell
     * @return the element
     */
    private native Element getCellElement(Element table, int row, int col) /*-{
        var out = table.rows[row].cells[col];
        return (out == null ? null : out);
        }-*/;
  }

  /**
   * This class contains methods used to format a table's columns. It is limited
   * by the support cross-browser HTML support for column formatting.
   */
  public class ColumnFormatter {
    protected Element columnGroup;

    /**
     * Adds a style to the specified column.
     * 
     * @param col the col to which the style will be added
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void addStyleName(int col, String styleName) {
      UIObject.setStyleName(ensureColumn(col), styleName, true);
    }

    /**
     * Gets the style of the specified column.
     * 
     * @param column the column to be queried
     * @return the style name
     * @see UIObject#getStyleName()
     * @throws IndexOutOfBoundsException
     */
    public String getStyleName(int column) {
      return UIObject.getStyleName(ensureColumn(column));
    }

    /**
     * Gets the primary style of the specified column.
     * 
     * @param column the column to be queried
     * @return the style name
     * @see UIObject#getStylePrimaryName()
     * @throws IndexOutOfBoundsException
     */
    public String getStylePrimaryName(int column) {
      return UIObject.getStylePrimaryName(ensureColumn(column));
    }

    /**
     * Removes a style from the specified column.
     * 
     * @param column the column from which the style will be removed
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int column, String styleName) {
      UIObject.setStyleName(ensureColumn(column), styleName, false);
    }

    /**
     * Sets the style name associated with the specified column.
     * 
     * @param column the column whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int column, String styleName) {
      UIObject.setStyleName(ensureColumn(column), styleName);
    }

    /**
     * Sets the primary style name associated with the specified column.
     * 
     * @param column the column whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStylePrimaryName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStylePrimaryName(int column, String styleName) {
      UIObject.setStylePrimaryName(ensureColumn(column), styleName);
    }

    /**
     * Sets the width of the specified column.
     * 
     * @param column the column of the cell whose width is to be set
     * @param width the cell's new width, in percentage or pixel units
     * @throws IndexOutOfBoundsException
     */
    public void setWidth(int column, String width) {
      DOM.setElementProperty(ensureColumn(column), "width", width);
    }

    private Element ensureColumn(int col) {
      prepareColumn(col);
      prepareColumnGroup();

      int num = DOM.getChildCount(columnGroup);
      if (num <= col) {
        Element colElement = null;
        for (int i = num; i <= col; i++) {
          colElement = DOM.createElement("col");
          DOM.appendChild(columnGroup, colElement);
        }
        return colElement;
      }
      return DOM.getChild(columnGroup, col);
    }

    /**
     * Prepare the colgroup tag for the first time, guarenteeing that it exists
     * and has at least one col tag in it. This method corrects a Mozilla issue
     * where the col tag will affect the wrong column if a col tag doesn't exist
     * when the element is attached to the page.
     */
    private void prepareColumnGroup() {
      if (columnGroup == null) {
        columnGroup = DOM.createElement("colgroup");
        DOM.insertChild(tableElem, columnGroup, 0);
        DOM.appendChild(columnGroup, DOM.createElement("col"));
      }
    }
  }

  /**
   * This class contains methods used to format a table's rows.
   */
  public class RowFormatter {

    /**
     * Adds a style to the specified row.
     * 
     * @param row the row to which the style will be added
     * @param styleName the style name to be added
     * @see UIObject#addStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void addStyleName(int row, String styleName) {
      UIObject.setStyleName(ensureElement(row), styleName, true);
    }

    /**
     * Gets the TR element representing the specified row.
     * 
     * @param row the row whose TR element is to be retrieved
     * @return the row's TR element
     * @throws IndexOutOfBoundsException
     */
    public Element getElement(int row) {
      checkRowBounds(row);
      return getRawElement(row);
    }

    /**
     * Gets the style of the specified row.
     * 
     * @param row the row to be queried
     * @return the style name
     * @see UIObject#getStyleName()
     * @throws IndexOutOfBoundsException
     */
    public String getStyleName(int row) {
      return UIObject.getStyleName(getElement(row));
    }

    /**
     * Gets the primary style of the specified row.
     * 
     * @param row the row to be queried
     * @return the style name
     * @see UIObject#getStylePrimaryName()
     * @throws IndexOutOfBoundsException
     */
    public String getStylePrimaryName(int row) {
      return UIObject.getStylePrimaryName(getElement(row));
    }

    /**
     * Determines whether or not this row is visible via the display style
     * attribute.
     * 
     * @param row the row whose visibility is to be set
     * @return <code>true</code> if the row is visible
     */
    public boolean isVisible(int row) {
      Element e = getElement(row);
      return UIObject.isVisible(e);
    }

    /**
     * Removes a style from the specified row.
     * 
     * @param row the row from which the style will be removed
     * @param styleName the style name to be removed
     * @see UIObject#removeStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void removeStyleName(int row, String styleName) {
      UIObject.setStyleName(ensureElement(row), styleName, false);
    }

    /**
     * Sets the style name associated with the specified row.
     * 
     * @param row the row whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStyleName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStyleName(int row, String styleName) {
      UIObject.setStyleName(ensureElement(row), styleName);
    }

    /**
     * Sets the primary style name associated with the specified row.
     * 
     * @param row the row whose style name is to be set
     * @param styleName the new style name
     * @see UIObject#setStylePrimaryName(String)
     * @throws IndexOutOfBoundsException
     */
    public void setStylePrimaryName(int row, String styleName) {
      UIObject.setStylePrimaryName(ensureElement(row), styleName);
    }

    /**
     * Sets the vertical alignment of the specified row.
     * 
     * @param row the row whose alignment is to be set
     * @param align the row's new vertical alignment as specified in
     *          {@link com.google.gwt.user.client.ui.HasVerticalAlignment}
     * @throws IndexOutOfBoundsException
     */
    public void setVerticalAlign(int row, VerticalAlignmentConstant align) {
      DOM.setStyleAttribute(ensureElement(row), "verticalAlign", align.getVerticalAlignString());
    }

    /**
     * Sets whether this row is visible.
     * 
     * @param row the row whose visibility is to be set
     * @param visible <code>true</code> to show the row, <code>false</code>
     *          to hide it
     */
    public void setVisible(int row, boolean visible) {
      Element e = ensureElement(row);
      UIObject.setVisible(e, visible);
    }

    /**
     * Ensure the TR element representing the specified row exists for
     * subclasses that allow dynamic addition of elements.
     * 
     * @param row the row whose TR element is to be retrieved
     * @return the row's TR element
     * @throws IndexOutOfBoundsException
     */
    protected Element ensureElement(int row) {
      prepareRow(row);
      return getRawElement(row);
    }

    /**
     * Unsafe method to get a row element.
     * 
     * @param row the row to get
     * @return the row element
     */
    protected Element getRawElement(int row) {
      return getRow(bodyElem, row);
    }

    protected native Element getRow(Element elem, int row) /*-{
        return elem.rows[row] || null;
        }-*/;

    /**
     * Convenience methods to set an attribute on a row.
     * 
     * @param row cell's row
     * @param attrName attribute to set
     * @param value value to set
     * @throws IndexOutOfBoundsException
     */
    protected void setAttr(int row, String attrName, String value) {
      Element elem = ensureElement(row);
      DOM.setElementAttribute(elem, attrName, value);
    }
  }

  /**
   * Creates a mapping from elements to their associated widgets.
   */
  protected static class WidgetMapper {

    private static class FreeNode {
      int index;
      FreeNode next;

      public FreeNode(int index, FreeNode next) {
        this.index = index;
        this.next = next;
      }
    }

    private static native void clearWidgetIndex(Element elem) /*-{
        elem["__widgetID"] = null;
        }-*/;

    private static native int getWidgetIndex(Element elem) /*-{
        var index = elem["__widgetID"];
        return (index == null) ? -1 : index;
        }-*/;

    private static native void setWidgetIndex(Element elem, int index) /*-{
        elem["__widgetID"] = index;
        }-*/;

    private FreeNode freeList = null;

    private final ArrayList widgetList = new ArrayList();

    /**
     * Returns the widget associated with the given element.
     * 
     * @param elem widget's element
     * @return the widget
     */
    public Widget getWidget(Element elem) {
      int index = getWidgetIndex(elem);
      if (index < 0) {
        return null;
      }
      return (Widget) widgetList.get(index);
    }

    /**
     * Adds the Widget.
     * 
     * @param widget widget to add
     */
    public void putWidget(Widget widget) {
      int index;
      if (freeList == null) {
        index = widgetList.size();
        widgetList.add(widget);
      } else {
        index = freeList.index;
        widgetList.set(index, widget);
        freeList = freeList.next;
      }
      setWidgetIndex(widget.getElement(), index);
    }

    /**
     * Remove the widget associated with the given element.
     * 
     * @param elem the widget's element
     */
    public void removeWidgetByElement(Element elem) {
      int index = getWidgetIndex(elem);
      removeImpl(elem, index);
    }

    /**
     * Creates an iterator of widgets.
     * 
     * @return the iterator
     */
    public Iterator widgetIterator() {
      // TODO: look at using the WidgetIterators class!
      return new Iterator() {
        int lastIndex = -1;
        int nextIndex = -1;
        {
          findNext();
        }

        public boolean hasNext() {
          return nextIndex < widgetList.size();
        }

        public Object next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          Object result = widgetList.get(nextIndex);
          lastIndex = nextIndex;
          findNext();
          return result;
        }

        public void remove() {
          if (lastIndex < 0) {
            throw new IllegalStateException();
          }
          Widget w = (Widget) widgetList.get(lastIndex);
          assert (w.getParent() instanceof HTMLTable);
          w.removeFromParent();
          lastIndex = -1;
        }

        private void findNext() {
          while (++nextIndex < widgetList.size()) {
            if (widgetList.get(nextIndex) != null) {
              return;
            }
          }
        }
      };
    }

    private void removeImpl(Element elem, int index) {
      clearWidgetIndex(elem);
      widgetList.set(index, null);
      freeList = new FreeNode(index, freeList);
    }
  }

  /**
   * Table's body.
   */
  private Element bodyElem;

  /**
   * Current cell formatter.
   */
  private CellFormatter cellFormatter;

  /**
   * The text to insert into cleared cells.
   */
  private String clearText = "";

  /**
   * Column Formatter.
   */
  private ColumnFormatter columnFormatter;

  /**
   * Current row formatter.
   */
  private RowFormatter rowFormatter;

  /**
   * Table element.
   */
  private final Element tableElem;

  /**
   * Current table listener.
   */
  private TableListenerCollection tableListeners;

  private WidgetMapper widgetMap = new WidgetMapper();

  /**
   * Create a new empty HTML Table.
   */
  public HTMLTable() {
    super();
    tableElem = DOM.createTable();
    bodyElem = DOM.createTBody();
    DOM.appendChild(tableElem, bodyElem);
    setElement(tableElem);
    sinkEvents(Event.ONCLICK);
  }

  /**
   * Adds a listener to the current table.
   * 
   * @param listener listener to add
   */
  public void addTableListener(TableListener listener) {
    if (tableListeners == null) {
      tableListeners = new TableListenerCollection();
    }
    tableListeners.add(listener);
  }

  /**
   * Removes all widgets from this table, but does not remove other HTML or text
   * contents of cells.
   */
  public void clear() {
    for (int row = 0; row < getRowCount(); ++row) {
      for (int col = 0; col < getCellCount(row); ++col) {
        Widget child = getWidgetImpl(row, col);
        if (child != null) {
          remove(child);
        }
      }
    }
  }

  /**
   * Removes all widgets from this table, including other HTML or text contents
   * of cells.
   */
  public void clearAll() {
    for (int row = 0; row < getRowCount(); row++) {
      for (int col = 0; col < getCellCount(row); col++) {
        Element td = cellFormatter.getRawElement(row, col);
        internalClearCell(td, true);
      }
    }
  }

  /**
   * Clears the given row and column. If it contains a Widget, it will be
   * removed from the table. If not, its contents will simply be cleared.
   * 
   * @param row the widget's column
   * @param column the widget's column
   * @return true if a widget was removed
   * @throws IndexOutOfBoundsException
   */
  public boolean clearCell(int row, int column) {
    Element td = getCellFormatter().getElement(row, column);
    return internalClearCell(td, true);
  }

  /**
   * Gets the number of cells in a given row.
   * 
   * @param row the row whose cells are to be counted
   * @return the number of cells present in the row
   */
  public abstract int getCellCount(int row);

  /**
   * Gets the {@link CellFormatter} associated with this table. Use casting to
   * get subclass-specific functionality
   * 
   * @return this table's cell formatter
   */
  public CellFormatter getCellFormatter() {
    return cellFormatter;
  }

  /**
   * Gets the amount of padding that is added around all cells.
   * 
   * @return the cell padding, in pixels
   */
  public int getCellPadding() {
    return DOM.getElementPropertyInt(tableElem, "cellPadding");
  }

  /**
   * Gets the amount of spacing that is added around all cells.
   * 
   * @return the cell spacing, in pixels
   */
  public int getCellSpacing() {
    return DOM.getElementPropertyInt(tableElem, "cellSpacing");
  }

  /**
   * Gets the column formatter.
   * 
   * @return the column formatter
   */
  public ColumnFormatter getColumnFormatter() {
    return columnFormatter;
  }

  /**
   * Determines the TD associated with the specified event.
   * 
   * @param event the event to be queried
   * @return the TD associated with the event, or <code>null</code> if none is
   *         found.
   */
  public Element getEventTargetCell(Event event) {
    Element td = DOM.eventGetTarget(event);
    for (; td != null; td = DOM.getParent(td)) {
      // If it's a TD, it might be the one we're looking for.
      if ("td".equalsIgnoreCase(td.getPropertyString("tagName"))) {
        // Make sure it's directly a part of this table before returning
        // it.
        Element tr = DOM.getParent(td);
        Element body = DOM.getParent(tr);
        if (DOM.compare(body, bodyElem)) {
          return td;
        }
      }
      // If we run into this table's body, we're out of options.
      if (DOM.compare(td, bodyElem)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Determines the TR associated with the specified event.
   * 
   * @param event the event to be queried
   * @return the TR associated with the event, or <code>null</code> if none is
   *         found.
   */
  public Element getEventTargetRow(Event event) {
    Element tr = DOM.eventGetTarget(event);
    for (; tr != null; tr = DOM.getParent(tr)) {
      // If it's a TD, it might be the one we're looking for.
      if ("tr".equalsIgnoreCase(tr.getPropertyString("tagName"))) {
        // Make sure it's directly a part of this table before returning
        // it.
        Element body = DOM.getParent(tr);
        if (DOM.compare(body, bodyElem)) {
          return tr;
        }
      }
      // If we run into this table's body, we're out of options.
      if (DOM.compare(tr, bodyElem)) {
        return null;
      }
    }
    return null;
  }

  /**
   * Gets the HTML contents of the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's HTML contents
   * @throws IndexOutOfBoundsException
   */
  public String getHTML(int row, int column) {
    return DOM.getInnerHTML(cellFormatter.getElement(row, column));
  }

  /**
   * Gets the number of rows present in this table.
   * 
   * @return the table's row count
   */
  public abstract int getRowCount();

  /**
   * Gets the RowFormatter associated with this table.
   * 
   * @return the table's row formatter
   */
  public RowFormatter getRowFormatter() {
    return rowFormatter;
  }

  /**
   * Gets the text within the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the cell's text contents
   * @throws IndexOutOfBoundsException
   */
  public String getText(int row, int column) {
    return DOM.getInnerText(cellFormatter.getElement(row, column));
  }

  /**
   * Gets the widget in the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the widget in the specified cell, or <code>null</code> if none is
   *         present
   * @throws IndexOutOfBoundsException
   */
  public Widget getWidget(int row, int column) {
    checkCellBounds(row, column);
    return getWidgetImpl(row, column);
  }

  /**
   * Determines whether the specified cell exists.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return <code>true</code> if the specified cell exists
   */
  public boolean isCellPresent(int row, int column) {
    if ((row >= getRowCount()) || (row < 0)) {
      return false;
    }
    if ((column < 0) || (column >= getCellCount(row))) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Returns an iterator containing all the widgets in this table.
   * 
   * @return the iterator
   */
  public Iterator<Widget> iterator() {
    return widgetMap.widgetIterator();
  }

  /**
   * Method to process events generated from the browser.
   * 
   * @param event the generated event
   */
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        if (tableListeners != null) {
          // Find out which cell was actually clicked.
          Element td = getEventTargetCell(event);
          if (td == null) {
            return;
          }
          Element tr = DOM.getParent(td);
          int row = getRowIndex(tr);
          int column = getCellIndex(tr, td);
          if (column >= 0) {
            // Fire the event.
            tableListeners.fireCellClicked(this, row, column);
          }
        }
        break;
      }
      default: {
        // Do nothing
      }
    }
  }

  /**
   * Remove the specified widget from the table.
   * 
   * @param widget widget to remove
   * @return was the widget removed from the table.
   */
  public boolean remove(Widget widget) {
    // Validate.
    if (widget.getParent() != this) {
      return false;
    }

    // Orphan.
    orphan(widget);

    // Physical detach.
    Element elem = widget.getElement();
    DOM.removeChild(DOM.getParent(elem), elem);

    // Logical detach.
    widgetMap.removeWidgetByElement(elem);
    return true;
  }

  /**
   * Removes the specified table listener.
   * 
   * @param listener listener to remove
   */
  public void removeTableListener(TableListener listener) {
    if (tableListeners != null) {
      tableListeners.remove(listener);
    }
  }

  /**
   * Sets the width of the table's border. This border is displayed around all
   * cells in the table.
   * 
   * @param width the width of the border, in pixels
   */
  public void setBorderWidth(int width) {
    DOM.setElementProperty(tableElem, "border", "" + width);
  }

  /**
   * Sets the amount of padding to be added around all cells.
   * 
   * @param padding the cell padding, in pixels
   */
  public void setCellPadding(int padding) {
    DOM.setElementPropertyInt(tableElem, "cellPadding", padding);
  }

  /**
   * Sets the amount of spacing to be added around all cells.
   * 
   * @param spacing the cell spacing, in pixels
   */
  public void setCellSpacing(int spacing) {
    DOM.setElementPropertyInt(tableElem, "cellSpacing", spacing);
  }

  /**
   * Sets the element within the specified cell.
   * <p>
   * Inherited implementations may either throw IndexOutOfBounds exception if
   * the cell does not exist, or allocate a new cell to store the content.
   * </p>
   * <p>
   * FlexTable will automatically allocate the cell at the correct location and
   * then set the widget. Grid will set the widget if and only if the cell is
   * within the Grid's bounding box.
   * </p>
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  public void setElement(int row, int column, Element element) {
    prepareCell(row, column);
    if (element != null) {

      // Removes any existing widget.
      Element td = cleanCell(row, column, true);

      // Physical attach.
      DOM.appendChild(td, element);
    }
  }

  /**
   * Sets the HTML contents of the specified cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @param html the cell's HTML contents
   * @throws IndexOutOfBoundsException
   */
  public void setHTML(int row, int column, String html) {
    prepareCell(row, column);
    Element td = cleanCell(row, column, html == null);
    if (html != null) {
      DOM.setInnerHTML(td, html);
    }
  }

  /**
   * Sets the text within the specified cell.
   * 
   * @param row the cell's row
   * @param column cell's column
   * @param text the cell's text contents
   * @throws IndexOutOfBoundsException
   */
  public void setText(int row, int column, String text) {
    prepareCell(row, column);
    Element td = cleanCell(row, column, text == null);
    if (text != null) {
      DOM.setInnerText(td, text);
    }
  }

  /**
   * Sets the widget within the specified cell.
   * <p>
   * Inherited implementations may either throw IndexOutOfBounds exception if
   * the cell does not exist, or allocate a new cell to store the content.
   * </p>
   * <p>
   * FlexTable will automatically allocate the cell at the correct location and
   * then set the widget. Grid will set the widget if and only if the cell is
   * within the Grid's bounding box.
   * </p>
   * 
   * @param widget The widget to be added
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  public void setWidget(int row, int column, Widget widget) {
    prepareCell(row, column);
    if (widget != null) {
      widget.removeFromParent();

      // Removes any existing widget.
      Element td = cleanCell(row, column, true);
      DOM.setInnerHTML(td, "");

      // Logical attach.
      widgetMap.putWidget(widget);

      // Physical attach.
      DOM.appendChild(td, widget.getElement());

      adopt(widget);
    }
  }

  /**
   * Bounds checks that the cell exists at the specified location.
   * 
   * @param row cell's row
   * @param column cell's column
   * @throws IndexOutOfBoundsException
   */
  protected void checkCellBounds(int row, int column) {
    checkRowBounds(row);
    if (column < 0) {
      throw new IndexOutOfBoundsException("Column " + column + " must be non-negative: " + column);
    }
    int cellSize = getCellCount(row);
    if (cellSize <= column) {
      throw new IndexOutOfBoundsException("Column index: " + column + ", Column size: "
          + getCellCount(row));
    }
  }

  /**
   * Checks that the row is within the correct bounds.
   * 
   * @param row row index to check
   * @throws IndexOutOfBoundsException
   */
  protected void checkRowBounds(int row) {
    int rowSize = getRowCount();
    if ((row >= rowSize) || (row < 0)) {
      throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: " + rowSize);
    }
  }

  /**
   * Removes any widgets, text, and HTML within the cell. This method assumes
   * that the requested cell already exists.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @param clearInnerHTML should the cell's inner html be cleared?
   * @return element that has been cleaned
   */
  protected Element cleanCell(int row, int column, boolean clearInnerHTML) {
    // Clear whatever is in the cell.
    Element td = getCellFormatter().getRawElement(row, column);
    internalClearCell(td, clearInnerHTML);
    return td;
  }

  /**
   * Creates a new cell. Override this method if the cell should have initial
   * contents.
   * 
   * @return the newly created TD
   */
  protected Element createCell() {
    return OverrideDOM.createTD();
  }

  /**
   * Creates a new row. Override this method if the row should have initial
   * contents.
   * 
   * @return the newly created TD
   */
  protected Element createRow() {
    return DOM.createTR();
  }

  /**
   * Gets the table's TBODY element.
   * 
   * @return the TBODY element
   */
  protected Element getBodyElement() {
    return bodyElem;
  }

  /**
   * @param rowElem the row element
   * @param cellElem the cell element
   * @return the index of a cell in the row
   */
  protected int getCellIndex(Element rowElem, Element cellElem) {
    return DOM.getChildIndex(rowElem, cellElem);
  }

  /**
   * Directly ask the underlying DOM what the cell count on the given row is.
   * 
   * @param tableBody the element
   * @param row the row
   * @return number of columns in the row
   */
  protected native int getDOMCellCount(Element tableBody, int row) /*-{
    return tableBody.rows[row].cells.length;
    }-*/;

  /**
   * Directly ask the underlying DOM what the cell count on the given row is.
   * 
   * @param row the row
   * @return number of columns in the row
   */
  protected int getDOMCellCount(int row) {
    return getDOMCellCount(bodyElem, row);
  }

  /**
   * Directly ask the underlying DOM what the row count is.
   * 
   * @return Returns the number of rows in the table
   */
  protected int getDOMRowCount() {
    return getDOMRowCount(bodyElem);
  }

  protected native int getDOMRowCount(Element elem) /*-{
    return elem.rows.length;
  }-*/;

  /**
   * @param rowElem the row element
   * @return the index of a row
   */
  protected int getRowIndex(Element rowElem) {
    return OverrideDOM.getRowIndex(rowElem);
  }

  /**
   * Returns the widgetMap.
   * 
   * @return the widget map
   */
  protected WidgetMapper getWidgetMap() {
    return widgetMap;
  }

  /**
   * Inserts a new cell into the specified row.
   * 
   * @param row the row into which the new cell will be inserted
   * @param column the column before which the cell will be inserted
   * @return the new element
   * @throws IndexOutOfBoundsException
   */
  protected Element insertCell(int row, int column) {
    Element tr = rowFormatter.getElement(row);
    Element td = createCell();
    Element beforeTd = cellFormatter.getRawElement(row, column);
    tr.insertBefore(td, beforeTd);
    return td;
  }

  /**
   * Inserts a number of cells before the specified cell.
   * 
   * @param row the row into which the new cells will be inserted
   * @param column the column before which the new cells will be inserted
   * @param count number of cells to be inserted
   * @throws IndexOutOfBoundsException
   */
  protected void insertCells(int row, int column, int count) {
    Element tr = rowFormatter.getRawElement(row);
    Element beforeTd = cellFormatter.getRawElement(row, column);
    for (int i = column; i < column + count; i++) {
      Element td = createCell();
      tr.insertBefore(td, beforeTd);
    }
  }

  /**
   * Inserts a new row into the table.
   * 
   * @param beforeRow the index before which the new row will be inserted
   * @return the index of the newly-created row
   * @throws IndexOutOfBoundsException
   */
  protected int insertRow(int beforeRow) {
    // Specifically allow the row count as an insert position.
    if (beforeRow != getRowCount()) {
      checkRowBounds(beforeRow);
    }
    Element tr = createRow();
    Element beforeRowElem = rowFormatter.getRawElement(beforeRow);
    bodyElem.insertBefore(tr, beforeRowElem);
    return beforeRow;
  }

  /**
   * Does actual clearing, used by clearCell and cleanCell. All HTMLTable
   * methods should use internalClearCell rather than clearCell, as clearCell
   * may be overridden in subclasses to format an empty cell.
   * 
   * @param td element to clear
   * @param clearInnerHTML should the cell's inner html be cleared?
   * @return returns whether a widget was cleared
   */
  protected boolean internalClearCell(Element td, boolean clearInnerHTML) {
    Element maybeChild = DOM.getFirstChild(td);
    Widget widget = null;
    if (maybeChild != null) {
      widget = widgetMap.getWidget(maybeChild);
    }
    if (widget != null) {
      // If there is a widget, remove it.
      remove(widget);
      return true;
    } else {
      // Otherwise, simply clear whatever text and/or HTML may be there.
      if (clearInnerHTML) {
        DOM.setInnerHTML(td, clearText);
      }
      return false;
    }
  }

  /**
   * Subclasses must implement this method. It allows them to decide what to do
   * just before a cell is accessed. If the cell already exists, this method
   * must do nothing. Otherwise, a subclass must either ensure that the cell
   * exists or throw an {@link IndexOutOfBoundsException}.
   * 
   * @param row the cell's row
   * @param column the cell's column
   */
  protected abstract void prepareCell(int row, int column);

  /**
   * Subclasses can implement this method. It allows them to decide what to do
   * just before a column is accessed. For classes, such as
   * <code>FlexTable</code>, that do not have a concept of a global column
   * length can ignore this method.
   * 
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  protected void prepareColumn(int column) {
    // By default, do nothing.
  }

  /**
   * Subclasses must implement this method. If the row already exists, this
   * method must do nothing. Otherwise, a subclass must either ensure that the
   * row exists or throw an {@link IndexOutOfBoundsException}.
   * 
   * @param row the cell's row
   */
  protected abstract void prepareRow(int row);

  /**
   * Removes the specified cell from the table.
   * 
   * @param row the row of the cell to remove
   * @param column the column of cell to remove
   * @throws IndexOutOfBoundsException
   */
  protected void removeCell(int row, int column) {
    checkCellBounds(row, column);
    Element td = cleanCell(row, column, false);
    Element tr = rowFormatter.getRawElement(row);
    DOM.removeChild(tr, td);
  }

  /**
   * Removes the specified row from the table.
   * 
   * @param row the index of the row to be removed
   * @throws IndexOutOfBoundsException
   */
  protected void removeRow(int row) {
    checkRowBounds(row);
    int columnCount = getCellCount(row);
    for (int column = 0; column < columnCount; ++column) {
      cleanCell(row, column, false);
    }
    DOM.removeChild(bodyElem, rowFormatter.getRawElement(row));
  }

  protected void setBodyElement(Element element) {
    // Must free any existing widgets from the current DOM first.
    if (this.bodyElem != null) {
      clearOnlyWidgets();
    }
    this.bodyElem = element;
  }

  /**
   * Sets the table's CellFormatter.
   * 
   * @param cellFormatter the table's cell formatter
   */
  protected void setCellFormatter(CellFormatter cellFormatter) {
    this.cellFormatter = cellFormatter;
  }

  /**
   * Set the string to insert into cells when they are cleared. By default, an
   * empty string is used, but a &nbsp; or line break may also be used.
   * 
   * @param clearText the default clear text
   */
  protected void setClearText(String clearText) {
    this.clearText = clearText;
  }

  protected void setColumnFormatter(ColumnFormatter formatter) {
    columnFormatter = formatter;
    columnFormatter.prepareColumnGroup();
  }

  /**
   * Sets the table's RowFormatter.
   * 
   * @param rowFormatter the table's row formatter
   */
  protected void setRowFormatter(RowFormatter rowFormatter) {
    this.rowFormatter = rowFormatter;
  }

  /**
   * Clears the widgets from the table without actually modifying the underlying
   * HTMLTable.
   */
  private void clearOnlyWidgets() {
    Iterator widgets = iterator();
    while (widgets.hasNext()) {
      orphan((Widget) widgets.next());
    }
    widgetMap = new WidgetMapper();
  }

  /**
   * Gets the Widget associated with the given cell.
   * 
   * @param row the cell's row
   * @param column the cell's column
   * @return the widget
   */
  private Widget getWidgetImpl(int row, int column) {
    Element e = cellFormatter.getRawElement(row, column);
    Element child = DOM.getFirstChild(e);
    if (child == null) {
      return null;
    } else {
      return widgetMap.getWidget(child);
    }
  }

  /**
   * Sets the widget of a cell without clearing the cell.
   * 
   * @param widget The widget to be added
   * @param row the cell's row
   * @param column the cell's column
   * @throws IndexOutOfBoundsException
   */
  @SuppressWarnings("unused")
  private void setWidgetRaw(int row, int column, Widget widget) {
    if (widget != null) {
      widget.removeFromParent();

      // Logical attach.
      widgetMap.putWidget(widget);

      // Physical attach.
      Element td = getCellFormatter().getRawElement(row, column);
      DOM.appendChild(td, widget.getElement());

      adopt(widget);
    }
  }
}
