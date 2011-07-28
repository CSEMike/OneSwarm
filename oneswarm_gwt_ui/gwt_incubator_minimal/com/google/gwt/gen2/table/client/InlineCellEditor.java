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
import com.google.gwt.gen2.table.override.client.HTMLTable;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ImageBundle;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;

/**
 * An abstract representation of an editor used to edit the contents of a cell.
 * 
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-InlineCellEditor</dt>
 * <dd>applied to the entire widget</dd>
 * <dt>.gwt-InlineCellEditor .accept</dt>
 * <dd>applied to the accept image</dd>
 * <dt>.gwt-InlineCellEditor .cancel</dt>
 * <dd>applied to the cancel image</dd>
 * </dl>
 * 
 * @param <ColType> the data type of the column
 */
public abstract class InlineCellEditor<ColType> extends PopupPanel implements
    CellEditor<ColType> {
  /**
   * An {@link ImageBundle} that provides images for {@link InlineCellEditor}.
   */
  public static interface InlineCellEditorImages extends ImageBundle {
    /**
     * An image used to fill the available width.
     * 
     * @return a prototype of this image
     */
    AbstractImagePrototype cellEditorAccept();

    /**
     * An image indicating that a column is sorted in ascending order.
     * 
     * @return a prototype of this image
     */
    AbstractImagePrototype cellEditorCancel();
  }

  /**
   * <code>ClickDecoratorPanel</code> decorates any widget with the minimal
   * amount of machinery to receive clicks for delegation to the parent.
   */
  private static final class ClickDecoratorPanel extends SimplePanel {
    public ClickDecoratorPanel(Widget child, ClickHandler delegate) {
      setWidget(child);
      addDomHandler(delegate, ClickEvent.getType());
    }
  }

  /**
   * Default style name.
   */
  public static final String DEFAULT_STYLENAME = "gwt-InlineCellEditor";

  /**
   * The click listener used to accept.
   */
  private ClickHandler cancelHandler = new ClickHandler() {
    public void onClick(ClickEvent event) {
      cancel();
    }
  };

  /**
   * The click listener used to accept.
   */
  private ClickHandler acceptHandler = new ClickHandler() {
    public void onClick(ClickEvent event) {
      accept();
    }
  };

  /**
   * The current {@link CellEditor.Callback}.
   */
  private Callback<ColType> curCallback = null;

  /**
   * The current {@link CellEditor.CellEditInfo}.
   */
  private CellEditInfo curCellEditInfo = null;

  /**
   * The main grid used for layout.
   */
  private FlexTable layoutTable;

  /**
   * Construct a new {@link InlineCellEditor}.
   * 
   * @param content the {@link Widget} used to edit
   */
  protected InlineCellEditor(Widget content) {
    this(content,
        GWT.<InlineCellEditorImages> create(InlineCellEditorImages.class));
  }

  /**
   * Construct a new {@link InlineCellEditor} with the specified images.
   * 
   * @param content the {@link Widget} used to edit
   * @param images the images to use for the accept/cancel buttons
   */
  protected InlineCellEditor(Widget content, InlineCellEditorImages images) {
    super(true, true);
    setStylePrimaryName(DEFAULT_STYLENAME);

    // Wrap contents in a table
    layoutTable = new FlexTable();
    FlexCellFormatter formatter = layoutTable.getFlexCellFormatter();
    layoutTable.setCellSpacing(0);
    setWidget(layoutTable);

    // Add a label
    setLabel("");
    formatter.setColSpan(0, 0, 3);

    // Add content widget
    layoutTable.setWidget(1, 0, content);

    // Add accept and cancel buttons
    setAcceptWidget(images.cellEditorAccept().createImage());
    setCancelWidget(images.cellEditorCancel().createImage());
  }

  public void editCell(CellEditInfo cellEditInfo, ColType cellValue,
      Callback<ColType> callback) {
    // Save the current values
    curCallback = callback;
    curCellEditInfo = cellEditInfo;

    // Get the info about the cell
    HTMLTable table = curCellEditInfo.getTable();
    int row = curCellEditInfo.getRowIndex();
    int cell = curCellEditInfo.getCellIndex();

    // Get the location of the cell
    Element cellElem = table.getCellFormatter().getElement(row, cell);
    int top = DOM.getAbsoluteTop(cellElem) + getOffsetTop();
    int left = DOM.getAbsoluteLeft(cellElem) + getOffsetLeft();
    setPopupPosition(left, top);

    // Set the current value
    setValue(cellValue);

    // Show the editor
    show();
  }

  /**
   * @return the label text
   */
  public String getLabel() {
    return layoutTable.getHTML(0, 0);
  }

  /**
   * Set the label for this cell editor.
   * 
   * @param label the new label
   */
  public void setLabel(String label) {
    layoutTable.setHTML(0, 0, label);
  }

  /**
   * Accept the contents of the cell editor as the new cell value.
   */
  protected void accept() {
    // Check if we are ready to accept
    if (!onAccept()) {
      return;
    }

    // Get the value before hiding the editor
    ColType cellValue = getValue();

    // Hide the editor
    hide();

    // Send the new cell value to the callback
    curCallback.onComplete(curCellEditInfo, cellValue);
    curCallback = null;
    curCellEditInfo = null;
  }

  /**
   * Cancel the cell edit.
   */
  protected void cancel() {
    // Fire the event
    if (!onCancel()) {
      return;
    }

    // Hide the popup
    hide();

    // Call the callback
    if (curCallback != null) {
      curCallback.onCancel(curCellEditInfo);
      curCellEditInfo = null;
      curCallback = null;
    }
  }

  /**
   * @return the Widget that is used to accept the current value.
   */
  protected Widget getAcceptWidget() {
    ClickDecoratorPanel clickPanel = (ClickDecoratorPanel) layoutTable.getWidget(
        1, 1);
    return clickPanel.getWidget();
  }

  /**
   * @return the Widget that is used to cancel editing.
   */
  protected Widget getCancelWidget() {
    ClickDecoratorPanel clickPanel = (ClickDecoratorPanel) layoutTable.getWidget(
        1, 2);
    return clickPanel.getWidget();
  }

  /**
   * @return the content widget
   */
  protected Widget getContentWidget() {
    return layoutTable.getWidget(1, 0);
  }

  /**
   * Get the additional number of pixels to offset this cell editor from the top
   * left corner of the cell. Override this method to shift the editor left or
   * right.
   * 
   * @return the additional left offset in pixels
   */
  protected int getOffsetLeft() {
    return 0;
  }

  /**
   * Get the additional number of pixels to offset this cell editor from the top
   * left corner of the cell. Override this method to shift the editor up or
   * down.
   * 
   * @return the additional top offset in pixels
   */
  protected int getOffsetTop() {
    return 0;
  }

  /**
   * Get the new cell value from the editor.
   * 
   * @return the new cell value
   */
  protected abstract ColType getValue();

  /**
   * Called before an accept takes place.
   * 
   * @return true to allow the accept, false to prevent it
   */
  protected boolean onAccept() {
    return true;
  }

  /**
   * Called before a cancel takes place.
   * 
   * @return true to allow the cancel, false to prevent it
   */
  protected boolean onCancel() {
    return true;
  }

  /**
   * Set the Widget that is used to accept the current value.
   * 
   * @param w the widget
   */
  protected void setAcceptWidget(Widget w) {
    ClickDecoratorPanel clickPanel = new ClickDecoratorPanel(w, acceptHandler);
    clickPanel.setStyleName("accept");
    layoutTable.setWidget(1, 1, clickPanel);
  }

  /**
   * Set the Widget that is used to cancel editing.
   * 
   * @param w the widget
   */
  protected void setCancelWidget(Widget w) {
    ClickDecoratorPanel clickPanel = new ClickDecoratorPanel(w, cancelHandler);
    clickPanel.setStyleName("cancel");
    layoutTable.setWidget(1, 2, clickPanel);
  }

  /**
   * Set the cell value in the editor.
   * 
   * @param cellValue the value in the cell
   */
  protected abstract void setValue(ColType cellValue);
}
