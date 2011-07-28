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

package com.google.gwt.gen2.commonwidget.client.impl;

import com.google.gwt.dom.client.Element;
import com.google.gwt.gen2.table.override.client.ElementMapper;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.UIObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Highlighting, selectable cell grid. Base class for {@link CustomListBox} and
 * (eventually) Calendar view.
 * 
 * @param <ValueType> type of value in grid.
 */
@SuppressWarnings("unchecked")
public abstract class CellGridImpl<ValueType> extends
    com.google.gwt.gen2.table.override.client.Grid {

  /**
   * Cell type.
   * 
   */
  protected abstract class Cell extends UIObject {
    private boolean enabled = true;
    private ValueType value;
    private int index;

    /**
     * Create a cell grid.
     */
    public Cell(Element elem, ValueType value) {
      this.value = value;
      Cell current = (Cell) this;
      index = cellList.size();
      cellList.add(current);

      setElement(elem);
      elementToCell.put(current);
    }

    public ValueType getValue() {
      return value;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isHighlighted() {
      return this == highlightedCell;
    }

    public boolean isSelected() {
      return selectedCell == this;
    }

    public final void setEnabled(boolean enabled) {
      this.enabled = enabled;
      onEnabled(enabled);
    }

    public void verticalNavigation(int keyCode) {
      switch (keyCode) {
        case KeyboardListener.KEY_UP:
          setHighlighted(previousItem());
          break;
        case KeyboardListener.KEY_DOWN:
          setHighlighted(nextItem());
          break;
        case KeyboardListener.KEY_ESCAPE:
          // Figure out new event for this.
          break;
        case KeyboardListener.KEY_ENTER:
          setSelected((Cell) this);
          break;
      }
    }

    protected Cell nextItem() {
      if (index == getLastIndex()) {
        return cellList.get(0);
      } else {
        return cellList.get(index + 1);
      }
    }

    protected void onEnabled(boolean enabled) {
      updateStyle();
    }

    protected void onHighlighted(boolean highlighted) {
      updateStyle();
    }

    protected void onSelected(boolean selected) {
      updateStyle();
    }

    protected Cell previousItem() {
      if (index != 0) {
        return cellList.get(index - 1);
      } else {
        return cellList.get(getLastIndex());
      }
    }

    protected abstract void updateStyle();

    private int getLastIndex() {
      return cellList.size() - 1;
    }
  }

  private Cell highlightedCell;

  private Cell selectedCell;
  private ElementMapper<Cell> elementToCell = new ElementMapper<Cell>();
  private ArrayList<Cell> cellList = new ArrayList<Cell>();

  protected CellGridImpl() {
    setCellPadding(0);
    setCellSpacing(0);
    setBorderWidth(0);
    sinkEvents(Event.ONCLICK | Event.ONMOUSEOVER | Event.ONMOUSEOUT);
  }

  public Cell getCell(Element element) {
    return elementToCell.get((com.google.gwt.user.client.Element) element);
  }

  public Cell getCell(Event e) {
    // Find out which cell was actually clicked.
    Element td = getEventTargetCell(e);
    return td != null
        ? elementToCell.get((com.google.gwt.user.client.Element) td) : null;
  }

  public Cell getCell(int i) {
    return cellList.get(i);
  }

  public Iterator getCells() {
    return cellList.iterator();
  }

  public Cell getHighlightedCell() {
    return highlightedCell;
  }

  public int getNumCells() {
    return cellList.size();
  }

  public Cell getSelectedCell() {
    return selectedCell;
  }

  public ValueType getSelectedValue() {
    return getValue(selectedCell);
  }

  public ValueType getValue(Cell cell) {
    // ValueType == Object, so cast should be removed in manager code.
    return (ValueType) (cell == null ? null : cell.getValue());
  }

  @Override
  public void onBrowserEvent(Event event) {

    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        Cell cell = getCell(event);
        if (isActive(cell)) {
          setSelected(cell);
        }
        break;
      }
      case Event.ONMOUSEOUT: {
        Element e = DOM.eventGetFromElement(event);
        if (e != null) {
          Cell cell = elementToCell.get((com.google.gwt.user.client.Element) e);
          if (cell == highlightedCell) {
            setHighlighted(null);
          }
        }
        break;
      }
      case Event.ONMOUSEOVER: {
        Element e = DOM.eventGetToElement(event);
        if (e != null) {
          Cell cell = elementToCell.get((com.google.gwt.user.client.Element) e);
          if (isActive(cell)) {
            setHighlighted(cell);
          }
        }
        break;
      }
      case Event.ONKEYDOWN: {
        onKeyDown(highlightedCell, event);
        break;
      }
    }
  }

  @Override
  public void onUnload() {
    setHighlighted(null);
  }

  public final void setHighlighted(Cell nextHighlighted) {
    if (nextHighlighted == highlightedCell) {
      return;
    }
    Cell oldHighlighted = highlightedCell;
    highlightedCell = nextHighlighted;
    if (oldHighlighted != null) {
      oldHighlighted.onHighlighted(false);
    }
    if (highlightedCell != null) {
      highlightedCell.onHighlighted(true);
    }
  }

  public final void setSelected(Cell cell) {
    Cell last = getSelectedCell();
    selectedCell = cell;

    if (last != null) {
      last.onSelected(false);
    }
    if (selectedCell != null) {
      selectedCell.onSelected(true);
    }
    onSelected(last, selectedCell);
  }

  protected void onKeyDown(Cell lastHighlighted, Event event) {
    if (KeyboardSupportImpl.hasModifiers(event)) {
      return;
    }
    int keyCode = event.getKeyCode();
    if (lastHighlighted == null) {
      if (keyCode == KeyboardListener.KEY_DOWN && cellList.size() > 0) {
        setHighlighted(cellList.get(0));
      }
    } else {
      lastHighlighted.verticalNavigation(keyCode);
    }
  }

  protected abstract void onSelected(Cell lastSelected, Cell cell);

  private boolean isActive(Cell cell) {
    return cell != null && cell.isEnabled();
  }
}
