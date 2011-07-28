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

import com.google.gwt.gen2.table.client.TableDefinition.AbstractRowView;

/**
 * The default {@link RowRenderer} used by the {@link DefaultTableDefinition}
 * when the user does not specify one.
 * 
 * @param <RowType> the type of the row value
 */
public class DefaultRowRenderer<RowType> implements RowRenderer<RowType> {
  /**
   * The alternating colors to apply to the rows.
   */
  private String[] rowColors;

  /**
   * Construct a new {@link DefaultRowRenderer}.
   */
  public DefaultRowRenderer() {
    this(null);
  }

  /**
   * Construct a new {@link DefaultRowRenderer}.
   * 
   * @param rowColors an array of alternating colors to apply to the rows
   */
  public DefaultRowRenderer(String[] rowColors) {
    this.rowColors = rowColors;
  }

  public void renderRowValue(RowType rowValue, AbstractRowView<RowType> view) {
    if (rowColors != null) {
      int index = view.getRowIndex() % rowColors.length;
      view.setStyleAttribute("background", rowColors[index]);
    }
  }
}
