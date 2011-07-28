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
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * An {@link InlineCellEditor} that allows the user to select a {@link String}
 * from a list of {@link RadioButton RadioButtons}.
 * 
 * @param <ColType> the data type of the column
 */
public class RadioCellEditor<ColType> extends InlineCellEditor<ColType> {
  /**
   * A mapping of {@link RadioButton RadioButtons} to the value for the buttons.
   */
  private Map<RadioButton, ColType> radioMap = new HashMap<RadioButton, ColType>();

  /**
   * The vertical panel used to layout the contents.
   */
  private VerticalPanel vpanel = new VerticalPanel();

  /**
   * Construct a new {@link RadioCellEditor}.
   */
  public RadioCellEditor() {
    this(GWT.<InlineCellEditorImages> create(InlineCellEditorImages.class));
  }

  /**
   * Construct a new {@link RadioCellEditor} with the specified images.
   * 
   * @param images the images to use for the accept/cancel buttons
   */
  public RadioCellEditor(InlineCellEditorImages images) {
    this(new VerticalPanel(), images);
  }

  /**
   * Constructor.
   * 
   * @param images the images to use for the accept/cancel buttons
   */
  private RadioCellEditor(VerticalPanel vPanel, InlineCellEditorImages images) {
    super(vPanel, images);
    this.vpanel = vPanel;
  }

  /**
   * Add a {@link RadioButton} to the editor.
   * 
   * @param radio the radio button to add
   * @param value the value associated with the {@link RadioButton}
   */
  public void addRadioButton(RadioButton radio, ColType value) {
    vpanel.add(radio);
    radioMap.put(radio, value);
  }

  @Override
  public void editCell(CellEditInfo cellEditInfo, ColType cellValue,
      Callback<ColType> callback) {
    super.editCell(cellEditInfo, cellValue, callback);
    Iterator<Widget> it = vpanel.iterator();
    while (it.hasNext()) {
      RadioButton radio = (RadioButton) it.next();
      if (radio.getValue()) {
        radio.setFocus(true);
        return;
      }
    }
  }

  /**
   * Insert a {@link RadioButton} at the specified index.
   * 
   * @param radio the radio button to add
   * @param value the value associated with the {@link RadioButton}
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insertRadioButton(RadioButton radio, ColType value,
      int beforeIndex) throws IndexOutOfBoundsException {
    vpanel.insert(radio, beforeIndex);
    radioMap.put(radio, value);
  }

  /**
   * Remove a {@link RadioButton} from the editor.
   * 
   * @param radio the radio button to remove
   */
  public void removeRadioButton(RadioButton radio) {
    vpanel.remove(radio);
    radioMap.remove(radio);
  }

  @Override
  protected ColType getValue() {
    Iterator<Widget> it = vpanel.iterator();
    while (it.hasNext()) {
      RadioButton radio = (RadioButton) it.next();
      if (radio.getValue()) {
        return radioMap.get(radio);
      }
    }
    return null;
  }

  @Override
  protected void setValue(ColType cellValue) {
    for (Map.Entry<RadioButton, ColType> entry : radioMap.entrySet()) {
      if (entry.getValue().equals(cellValue)) {
        entry.getKey().setValue(true);
      } else {
        entry.getKey().setValue(false);
      }
    }
  }
}
