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

import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;

/**
 * An {@link InlineCellEditor} that can be used to edit {@link String Strings}.
 */
public class TextCellEditor extends InlineCellEditor<String> {
  /**
   * The text field used in this editor.
   */
  private TextBoxBase textBox;

  /**
   * Construct a new {@link TextCellEditor} using a normal {@link TextBox}.
   */
  public TextCellEditor() {
    this(new TextBox());
  }

  /**
   * Construct a new {@link TextCellEditor} using the specified {@link TextBox}.
   * 
   * @param textBox the text box to use
   */
  public TextCellEditor(TextBoxBase textBox) {
    super(textBox);
    this.textBox = textBox;
  }

  /**
   * Construct a new {@link TextCellEditor} using the specified {@link TextBox}
   * and images.
   * 
   * @param textBox the text box to use
   * @param images the images to use for the accept/cancel buttons
   */
  public TextCellEditor(TextBoxBase textBox, InlineCellEditorImages images) {
    super(textBox, images);
    this.textBox = textBox;
  }

  @Override
  public void editCell(CellEditInfo cellEditInfo, String cellValue,
      Callback<String> callback) {
    super.editCell(cellEditInfo, cellValue, callback);
    textBox.setFocus(true);
  }

  /**
   * @return the text box used in the editor
   */
  protected TextBoxBase getTextBox() {
    return textBox;
  }

  @Override
  protected String getValue() {
    return textBox.getText();
  }

  @Override
  protected void setValue(String cellValue) {
    if (cellValue == null) {
      cellValue = "";
    }
    textBox.setText(cellValue);
  }
}
