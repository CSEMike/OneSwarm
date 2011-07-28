/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.gen2.table.override.client.FlexTable;

/**
 * A helper class to enable bulk loading of {@link FlexTable}. Once we have 1.5
 * support it will extend TableBulkLoader<FlexTable>.
 * <p>
 * Important note: Must use {@link FlexTable} in overrides package NOT the
 * standard 1.4 FlexTable.
 * 
 * @param <RowType> the data type of the row values
 */
public class FlexTableBulkRenderer<RowType> extends TableBulkRenderer<RowType> {
  /**
   * Construct a new {@link FlexTableBulkRenderer}.
   * 
   * @param table the table to be bulk loaded
   * @param tableDef the table definition that should be used during rendering
   */
  public FlexTableBulkRenderer(FlexTable table,
      TableDefinition<RowType> tableDef) {
    super(table, tableDef);
  }

  /**
   * Construct a new {@link FlexTableBulkRenderer}.
   * 
   * @param table the table to be bulk rendered
   * @param sourceTableDef the external source of the table definition
   */
  public FlexTableBulkRenderer(FlexTable table,
      HasTableDefinition<RowType> sourceTableDef) {
    super(table, sourceTableDef);
  }
}
