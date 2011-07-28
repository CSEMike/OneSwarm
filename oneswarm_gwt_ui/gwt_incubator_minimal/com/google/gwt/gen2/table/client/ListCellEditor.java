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
import com.google.gwt.user.client.ui.ListBox;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link InlineCellEditor} that allows the user to select a {@link String}
 * from a drop down {@link ListBox}.
 * 
 * @param <ColType> the data type of the column
 */
public class ListCellEditor<ColType> extends InlineCellEditor<ColType> {
  /**
   * The list box of options.
   */
  private ListBox listBox;

  /**
   * A list of item values that cooresponds to the indexes in the
   * {@link ListBox}.
   */
  private List<ColType> itemValues = new ArrayList<ColType>();

  /**
   * Construct a new {@link ListCellEditor}.
   */
  public ListCellEditor() {
    this(new ListBox(),
        GWT.<InlineCellEditorImages> create(InlineCellEditorImages.class));
  }

  /**
   * Construct a new {@link ListCellEditor} using the specified images.
   * 
   * @param images the images to use for the accept/cancel buttons
   */
  public ListCellEditor(InlineCellEditorImages images) {
    this(new ListBox(), images);
  }

  /**
   * Construct a new {@link ListCellEditor} using the specified {@link ListBox}
   * and images.
   * 
   * @param listBox the {@link ListBox} to use
   * @param images the images to use for the accept/cancel buttons
   */
  protected ListCellEditor(ListBox listBox, InlineCellEditorImages images) {
    super(listBox, images);
    this.listBox = listBox;
  }

  /**
   * Adds an item to the {@link ListBox} in the editor.
   * 
   * @param item the text of the item to be added
   * @param value the value associated with the item
   */
  public void addItem(String item, ColType value) {
    listBox.addItem(item);
    itemValues.add(value);
  }

  @Override
  public void editCell(CellEditInfo cellEditInfo, ColType cellValue,
      Callback<ColType> callback) {
    super.editCell(cellEditInfo, cellValue, callback);
    listBox.setFocus(true);
  }

  /**
   * Inserts an item into the {@link ListBox} in the editor.
   * 
   * @param item the text of the item to be inserted
   * @param index the index at which to insert it
   * @param value the value associated with the item
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void insertItem(String item, int index, ColType value)
      throws IndexOutOfBoundsException {
    if (index < 0 || index >= listBox.getItemCount()) {
      throw new IndexOutOfBoundsException();
    }
    listBox.insertItem(item, index);
    itemValues.add(index, value);
  }

  /**
   * Removes the item at the specified index.
   * 
   * @param index the index of the item to be removed
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public void removeItem(int index) throws IndexOutOfBoundsException {
    listBox.removeItem(index);
    itemValues.remove(index);
  }

  @Override
  protected ColType getValue() {
    int index = listBox.getSelectedIndex();
    if (index < 0) {
      return null;
    } else {
      return itemValues.get(index);
    }
  }

  @Override
  protected void setValue(ColType cellValue) {
    listBox.setSelectedIndex(Math.max(0, itemValues.indexOf(cellValue)));
  }
}
