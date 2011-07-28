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
 * Row renderers can be used to customize the display of data in a table row.
 * 
 * @param <RowType> the type of the row value
 */
public interface RowRenderer<RowType> {
  /**
   * Render the contents of a row as a
   * {@link com.google.gwt.user.client.ui.Widget} or Text of HTML.
   * 
   * @param rowValue the object associated with the row
   * @param view the view used to set the row contents
   */
  void renderRowValue(RowType rowValue, AbstractRowView<RowType> view);
}
